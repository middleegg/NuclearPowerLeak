package Npl.newSth;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.struct.IntMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import arc.util.noise.Simplex;
import mindustry.content.Blocks;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.world.Block;
import mindustry.world.Tile;

import java.lang.reflect.Field;

import static mindustry.Vars.*;

/**
 * 无限世界系统 —— Chunk 流式加载方案
 * ================================================================
 * 核心思想：
 *   1. 维护一个"虚拟无限"的世界坐标空间，按 CHUNK_SIZE 分块
 *   2. 实际 World.tiles 只保留一个固定大小的"活动窗口"（WINDOW x WINDOW 瓦片）
 *   3. 玩家始终在窗口中心附近；当玩家接近边缘时，窗口整体"平移"
 *      - 丢弃远离玩家一侧的 Chunk
 *      - 在新暴露的一侧生成新的 Chunk
 *      - 所有 Tile / Building / Unit / Bullet 的坐标同步平移
 *   4. 地形基于 Simplex 噪声程序化生成，保证每个 (cx, cy) Chunk 内容稳定
 *
 * 坐标约定：
 *   - 虚拟瓦片坐标 (vx, vy)：无限整数空间中的真实坐标
 *   - 窗口内坐标 (wx, wy)：World.tiles 数组下标，范围 [0, WINDOW)
 *   - 偏移量 (offsetX, offsetY)：窗口左下角对应的虚拟瓦片坐标
 *   - 转换：wx = vx - offsetX,  wy = vy - offsetY
 *
 * 限制：
 *   - 窗口一次只平移整数个 Chunk，避免 Tile 坐标出现非对齐问题
 *   - 单位不做持久化（卸载的 Chunk 里的敌方单位直接清除，友方单位跟随窗口）
 *   - 建筑需要持久化（序列化到磁盘），当前版本先保留在内存 Map 中
 */
public class InfiniteWorldSystem {

    // ==================== 配置参数 ====================

    /** 单个 Chunk 的边长（瓦片）。Mindustry 原版区块单位，建议 32。 */
    public static final int CHUNK_SIZE = 32;

    /** 活动窗口的边长（瓦片）。必须是 CHUNK_SIZE 的整数倍。
     *  注意：如果 Tile.x/y 是 byte，此值不要超过 127；int 则可更大。 */
    public static final int WINDOW = 160; // 5x5 Chunk

    /** 玩家距离窗口边缘小于此值（瓦片）时触发平移。应 >= CHUNK_SIZE。 */
    public static final int EDGE_THRESHOLD = CHUNK_SIZE * 2;

    /** 世界种子，影响地形生成。 */
    public static int worldSeed = 12345;

    // ==================== 运行时状态 ====================

    private static boolean inited = false;
    private static boolean enabled = false;

    /** 窗口左下角对应的虚拟瓦片坐标。 */
    private static int offsetX = 0, offsetY = 0;

    /** 已加载的 Chunk 缓存（虚拟坐标 -> Chunk）。用于平移时复用数据。 */
    private static final IntMap<Chunk> loadedChunks = new IntMap<>();

    /** 卸载但未持久化的 Chunk（虚拟坐标 -> Chunk）。
     *  长期应写入磁盘，当前版本留内存。 */
    private static final IntMap<Chunk> unloadedChunks = new IntMap<>();

    // ==================== 反射字段缓存 ====================

    private static Field worldTilesField;       // World.tiles → Tiles 对象
    private static Field tilesArrayField;       // Tiles.array → Tile[] 一维数组
    private static Field worldTileChangesField;
    private static Field worldWidthField, worldHeightField;
    private static Field tileXField, tileYField;

    // ==================== 初始化 ====================

    /** 在 ClientLoadEvent 中调用一次。 */
    public static void init() {
        if (inited) return;
        inited = true;

        initReflection();

        // 每帧检测玩家是否需要触发窗口平移，以及 F12 启动无限世界
        Events.run(Trigger.update, () -> {
            // F12 启动无限世界（调试用）
            if (Core.input.keyTap(KeyCode.f12)) {
                launchInfiniteWorld();
            }
            update();
        });

        // 世界加载时重置偏移
        Events.on(WorldLoadEvent.class, e -> {
            offsetX = 0;
            offsetY = 0;
            loadedChunks.clear();
            unloadedChunks.clear();
        });
    }

    /** 启用无限世界模式（需在 world.loadMap 之后调用）。 */
    public static void enable() {
        if (!inited) {
            Log.err("[InfiniteWorld] 请先调用 init()");
            return;
        }
        if (world == null || worldWidth() == 0) {
            Log.err("[InfiniteWorld] world 未加载");
            return;
        }
        initReflection();
        enabled = true;
        offsetX = 0;
        offsetY = 0;
        Log.info("[InfiniteWorld] 已启用，窗口大小 " + WINDOW + "x" + WINDOW);
    }

    public static void disable() {
        enabled = false;
    }

    // ==================== 启动无限世界 ====================

    /**
     * 直接调整 World 大小并填充程序化地形，然后启用无限世界模式。
     * 绕过 Map 类，避免构造函数/序列化校验问题。
     */
    public static void launchInfiniteWorld() {
        try {
            int W = WINDOW, H = WINDOW;

            // 1. 创建 rules（沙盒模式，无限资源，方便测试）
            mindustry.game.Rules rules = new mindustry.game.Rules();
            rules.infiniteResources = true;
            rules.waves = false;
            rules.attackMode = false;

            // 2. 调整世界大小（创建 W×H 的 Tile 网格）
            world.resize(W, H);

            // 3. 获取 Tiles 内部一维数组
            Tile[] tiles = getTileArray();
            if (tiles == null) {
                Log.err("[InfiniteWorld] 无法获取 tiles 数组");
                return;
            }

            // 4. 填充地形，确保没有 null tile
            int nullCount = 0;
            for (int x = 0; x < W; x++) {
                for (int y = 0; y < H; y++) {
                    int idx = tileIndex(x, y, W);
                    Tile tile = tiles[idx];
                    if (tile == null) {
                        tile = new Tile(x, y);
                        tiles[idx] = tile;
                        nullCount++;
                    } else {
                        // 确保 Tile 坐标正确
                        setTileXY(tile, x, y);
                    }
                    generateTileContent(tile, x, y);
                }
            }
            if (nullCount > 0) {
                Log.info("[InfiniteWorld] 补建 " + nullCount + " 个 null Tile");
            }

            // 5. 设置游戏状态
            state.rules = rules;
            state.set(mindustry.core.GameState.State.playing);

            // 6. 触发瓦片变化刷新
            bumpTileChanges();

            // 7. 启用无限世界
            enable();

            Log.info("[InfiniteWorld] 无限世界已启动！窗口 " + W + "x" + H);
        } catch (Exception e) {
            Log.err("[InfiniteWorld] 启动失败", e);
        }
    }

    // ==================== 确定性结构生成 ====================

    /** 网格化特征的间距（瓦片）。每个网格点可能放置陨石坑/湖泊等。 */
    private static final int FEATURE_GRID = 96;
    /** 陨石坑最大半径，决定一个网格点需要检查周围多少格。 */
    private static final int MAX_CRATER_RADIUS = 40;
    /** 湖泊最大半径。 */
    private static final int MAX_LAKE_RADIUS = 35;

    /**
     * 基于坐标的确定性哈希，返回 [0,1) 伪随机值。
     * 相同 (x,y,seed) 永远返回相同值，保证跨区块一致。
     */
    private static float hash2D(int x, int y, int salt) {
        int h = x * 374761393 + y * 668265263 + salt * 2147483647;
        h = (h ^ (h >> 13)) * 1274126177;
        h = h ^ (h >> 16);
        return ((h & 0x7FFFFFFF) % 100000) / 100000f;
    }

    /**
     * 检查点 (vx, vy) 是否落在某个陨石坑内。
     * 返回陨石坑信息（半径、到中心的归一化距离），不在则返回 null。
     * 确定性：相同坐标永远得到相同结果。
     */
    private static CraterInfo getCraterAt(int vx, int vy) {
        // 检查周围网格点（半径覆盖范围内）
        int range = MAX_CRATER_RADIUS / FEATURE_GRID + 2;
        int gx0 = vx / FEATURE_GRID;
        int gy0 = vy / FEATURE_GRID;

        for (int gx = gx0 - range; gx <= gx0 + range; gx++) {
            for (int gy = gy0 - range; gy <= gy0 + range; gy++) {
                // 该网格点是否有陨石坑
                float chance = hash2D(gx, gy, worldSeed + 101);
                if (chance > 0.22f) continue; // 约 22% 的网格点有坑

                // 坑心位置（在网格内随机偏移）
                int cx = gx * FEATURE_GRID + (int) (hash2D(gx, gy, worldSeed + 102) * FEATURE_GRID);
                int cy = gy * FEATURE_GRID + (int) (hash2D(gx, gy, worldSeed + 103) * FEATURE_GRID);
                int radius = 8 + (int) (hash2D(gx, gy, worldSeed + 104) * (MAX_CRATER_RADIUS - 8));

                int dx = vx - cx, dy = vy - cy;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist <= radius) {
                    return new CraterInfo(cx, cy, radius, dist / radius);
                }
            }
        }
        return null;
    }

    /** 检查点是否落在湖泊内，返回归一化距离，否则返回 -1。 */
    private static float getLakeAt(int vx, int vy) {
        int range = MAX_LAKE_RADIUS / FEATURE_GRID + 2;
        int gx0 = vx / FEATURE_GRID;
        int gy0 = vy / FEATURE_GRID;

        for (int gx = gx0 - range; gx <= gx0 + range; gx++) {
            for (int gy = gy0 - range; gy <= gy0 + range; gy++) {
                float chance = hash2D(gx, gy, worldSeed + 201);
                if (chance > 0.15f) continue; // 约 15% 网格点有湖

                int cx = gx * FEATURE_GRID + (int) (hash2D(gx, gy, worldSeed + 202) * FEATURE_GRID);
                int cy = gy * FEATURE_GRID + (int) (hash2D(gx, gy, worldSeed + 203) * FEATURE_GRID);
                int radius = 10 + (int) (hash2D(gx, gy, worldSeed + 204) * (MAX_LAKE_RADIUS - 10));

                int dx = vx - cx, dy = vy - cy;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist <= radius) {
                    return dist / radius;
                }
            }
        }
        return -1f;
    }

    private static class CraterInfo {
        int cx, cy, radius;
        float normDist; // 0=中心, 1=边缘
        CraterInfo(int cx, int cy, int radius, float normDist) {
            this.cx = cx; this.cy = cy; this.radius = radius; this.normDist = normDist;
        }
    }

    /**
     * 在已有 Tile 上设置地形内容（地板/方块/覆盖层）。
     * 多层噪声 + 网格化确定性结构（陨石坑/湖泊），模拟 AzerPlanetGenerator。
     * 相同 (vx,vy) 永远生成相同地形。
     */
    private static void generateTileContent(Tile tile, int vx, int vy) {
        if (tile == null) return;

        // ---- 基础噪声 ----
        float height      = Simplex.noise2d(worldSeed,        4, 0.5f, 1f / 90f,  vx, vy);
        float moisture    = Simplex.noise2d(worldSeed + 7777, 3, 0.5f, 1f / 120f, vx, vy);
        float temperature = Simplex.noise2d(worldSeed + 4242, 3, 0.5f, 1f / 150f, vx, vy);

        // 山脉 ridged 噪声
        float mountain = 1f - Math.abs(Simplex.noise2d(worldSeed + 999, 4, 0.5f, 1f / 110f, vx, vy));

        // 河流噪声
        float river = Simplex.noise2d(worldSeed + 31337, 3, 0.6f, 1f / 60f, vx, vy);

        Block floor = Blocks.stone;
        Block block = Blocks.air;
        Block overlay = Blocks.air;
        boolean isWater = false;

        // ---- 1. 高度分层（基础地形）----
        if (height < 0.28f) {
            floor = Blocks.deepwater; isWater = true;
        } else if (height < 0.36f) {
            floor = Blocks.water; isWater = true;
        } else if (height < 0.43f) {
            floor = Blocks.sand;
        } else if (mountain > 0.72f) {
            floor = Blocks.stone;
            if (mountain > 0.85f) {
                block = Blocks.stoneWall;
                if (temperature < 0.35f) {
                    floor = Blocks.snow;
                    if (mountain > 0.88f) block = Blocks.snowWall;
                }
            }
        } else if (height < 0.72f) {
            if (temperature < 0.3f) {
                floor = Blocks.snow;
                if (mountain > 0.6f) block = Blocks.snowWall;
            } else if (moisture > 0.6f) {
                floor = Blocks.grass;
            } else if (moisture < 0.35f && temperature > 0.55f) {
                floor = Blocks.sand;
                if (mountain > 0.55f) block = Blocks.sandWall;
            } else {
                floor = Blocks.dirt;
            }
        } else {
            floor = Blocks.stone;
            if (mountain > 0.6f) block = Blocks.stoneWall;
        }

        // ---- 2. 河流 carving ----
        if (!isWater && block == Blocks.air && mountain < 0.55f) {
            float riverVal = Math.abs(river - 0.5f) * 2f;
            if (riverVal < 0.04f) {
                floor = Blocks.water; isWater = true; block = Blocks.air;
            } else if (riverVal < 0.08f) {
                floor = Blocks.sand;
            }
        }

        // ---- 3. 湖泊（确定性圆形水域）----
        if (!isWater) {
            float lakeDist = getLakeAt(vx, vy);
            if (lakeDist >= 0f) {
                if (lakeDist < 0.8f) {
                    floor = Blocks.water; isWater = true; block = Blocks.air; overlay = Blocks.air;
                } else {
                    floor = Blocks.sand; // 湖岸
                }
            }
        }

        // ---- 4. 陨石坑（确定性圆形凹陷）----
        if (!isWater) {
            CraterInfo crater = getCraterAt(vx, vy);
            if (crater != null) {
                if (crater.normDist < 0.18f) {
                    // 中心：特殊矿石
                    floor = Blocks.stone;
                    block = Blocks.air;
                    overlay = Blocks.oreThorium;
                } else if (crater.normDist < 0.6f) {
                    // 内环：下沉裸岩
                    floor = Blocks.stone;
                    if (block.solid) block = Blocks.air;
                    float oreR = hash2D(vx, vy, worldSeed + 305);
                    if (oreR > 0.6f) overlay = Blocks.oreCopper;
                    else if (oreR > 0.45f) overlay = Blocks.oreLead;
                    else if (oreR > 0.35f) overlay = Blocks.oreCoal;
                } else if (crater.normDist < 0.85f) {
                    // 外环：碎石
                    if (block.solid) block = Blocks.air;
                }
                // 边缘保留原地形
            }
        }

        // ---- 5. 矿物分布（仅非水、非墙）----
        if (!isWater && block == Blocks.air && overlay == Blocks.air) {
            float ore = Simplex.noise2d(worldSeed + 3333, 2, 0.5f, 1f / 28f, vx, vy);
            if (floor == Blocks.stone) {
                if (ore > 0.78f)      overlay = Blocks.oreCopper;
                else if (ore > 0.72f) overlay = Blocks.oreLead;
                else if (ore > 0.68f) overlay = Blocks.oreCoal;
                else if (ore > 0.65f) overlay = Blocks.oreTitanium;
                else if (ore > 0.63f && mountain > 0.6f) overlay = Blocks.oreThorium;
            } else if (floor == Blocks.sand || floor == Blocks.dirt) {
                if (ore > 0.76f)      overlay = Blocks.oreCopper;
                else if (ore > 0.7f)  overlay = Blocks.oreLead;
            } else if (floor == Blocks.grass) {
                if (ore > 0.78f) overlay = Blocks.oreCoal;
            } else if (floor == Blocks.snow) {
                if (ore > 0.74f) overlay = Blocks.oreTitanium;
            }
        }

        tile.setFloor(floor.asFloor());
        tile.setBlock(block);
        tile.setOverlay(overlay);
    }

    /**
     * 创建 Map 对象。
     * 160.3 版本 Map 的可用构造函数：
     *   Map(StringMap)
     *   Map(Fi, int, int, StringMap, boolean)
     *   Map(Fi, int, int, StringMap, boolean, int)
     *   Map(Fi, int, int, StringMap, boolean, int, int)
     * 这里用最简单的 Map(StringMap) 创建，再用反射填充 width/height/tiles/rules。
     */
    private static Object createMap(Class<?> mapClass, String name, String author, String desc,
                                    int width, int height, mindustry.world.Tile[][] tiles,
                                    mindustry.game.Rules rules) throws Exception {
        // 1. 用 StringMap 构造函数创建空 Map
        arc.struct.StringMap meta = new arc.struct.StringMap();
        meta.put("name", name);
        meta.put("author", author);
        meta.put("description", desc);
        Object map = mapClass.getDeclaredConstructor(arc.struct.StringMap.class).newInstance(meta);

        // 2. 用反射设置 width / height / tiles / rules
        trySetField(map, "width", width);
        trySetField(map, "height", height);

        try {
            Field tilesField = mapClass.getDeclaredField("tiles");
            tilesField.setAccessible(true);
            tilesField.set(map, tiles);
        } catch (Exception e) {
            Log.err("[InfiniteWorld] 设置 Map.tiles 失败", e);
        }

        try {
            Field rulesField = mapClass.getDeclaredField("rules");
            rulesField.setAccessible(true);
            rulesField.set(map, rules);
        } catch (Exception e) {
            Log.err("[InfiniteWorld] 设置 Map.rules 失败", e);
        }

        return map;
    }

    private static void trySetField(Object obj, String name, Object value) {
        try {
            Field f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (ReflectiveOperationException ignored) {
            // 字段不存在或访问失败，跳过
        }
    }

    // ==================== 反射初始化 ====================

    private static void initReflection() {
        if (world == null) {
            Log.err("[InfiniteWorld] world 为 null，反射初始化延后");
            return;
        }
        try {
            Class<?> worldClass = world.getClass();
            worldTilesField = worldClass.getDeclaredField("tiles");
            worldTilesField.setAccessible(true);

            worldTileChangesField = worldClass.getDeclaredField("tileChanges");
            worldTileChangesField.setAccessible(true);

            // width/height 可能是字段也可能是方法，字段获取失败时静默（运行时用方法）
            try {
                worldWidthField = worldClass.getDeclaredField("width");
                worldWidthField.setAccessible(true);
            } catch (NoSuchFieldException ignored) {
                // width 可能是方法而非字段，运行时使用 getter
            }
            try {
                worldHeightField = worldClass.getDeclaredField("height");
                worldHeightField.setAccessible(true);
            } catch (NoSuchFieldException ignored) {
                // height 可能是方法而非字段，运行时使用 getter
            }

            tileXField = Tile.class.getDeclaredField("x");
            tileXField.setAccessible(true);
            tileYField = Tile.class.getDeclaredField("y");
            tileYField.setAccessible(true);

            // 获取 Tiles 内部的 Tile[] 一维数组字段
            Object tilesObj = worldTilesField.get(world);
            if (tilesObj != null) {
                tilesArrayField = findTileArrayField(tilesObj);
                if (tilesArrayField == null) {
                    Log.err("[InfiniteWorld] 无法找到 Tiles 内部数组字段");
                } else {
                    Log.info("[InfiniteWorld] Tiles 内部数组字段: " + tilesArrayField.getName());
                }
            }
        } catch (Exception e) {
            Log.err("[InfiniteWorld] 反射初始化失败", e);
        }
    }

    /** 扫描对象所有字段，找到 Tile[] 类型的字段。 */
    private static Field findTileArrayField(Object obj) {
        for (Field f : obj.getClass().getDeclaredFields()) {
            try {
                f.setAccessible(true);
                Object test = f.get(obj);
                if (test instanceof Tile[]) return f;
            } catch (IllegalAccessException ignored) {
                // 字段不可访问，继续尝试下一个
            }
        }
        return null;
    }

    /** 获取 World.tiles 内部的 Tile[] 一维数组。 */
    private static Tile[] getTileArray() {
        try {
            Object tilesObj = worldTilesField.get(world);
            if (tilesObj == null) return null;
            if (tilesObj instanceof Tile[]) return (Tile[]) tilesObj;

            if (tilesArrayField == null) {
                tilesArrayField = findTileArrayField(tilesObj);
            }
            if (tilesArrayField != null) {
                return (Tile[]) tilesArrayField.get(tilesObj);
            }
            Log.err("[InfiniteWorld] 无法找到 Tiles 内部数组字段");
        } catch (Exception e) {
            Log.err("[InfiniteWorld] 获取 tiles 数组失败", e);
        }
        return null;
    }

    /** 一维数组索引：y * width + x */
    private static int tileIndex(int x, int y, int width) {
        return y * width + x;
    }

    private static int worldWidth() {
        try {
            return worldWidthField.getInt(world);
        } catch (Exception e) {
            // 字段不存在，尝试方法 world.width()
            try {
                return (int) world.getClass().getMethod("width").invoke(world);
            } catch (Exception e2) {
                return WINDOW;
            }
        }
    }

    private static int worldHeight() {
        try {
            return worldHeightField.getInt(world);
        } catch (Exception e) {
            try {
                return (int) world.getClass().getMethod("height").invoke(world);
            } catch (Exception e2) {
                return WINDOW;
            }
        }
    }

    // ==================== 主更新 ====================

    private static void update() {
        if (!enabled || world == null || player == null || player.unit() == null) return;

        Unit u = player.unit();
        // 玩家在窗口内的瓦片坐标
        int px = (int) (u.x / tilesize);
        int py = (int) (u.y / tilesize);

        // 计算需要平移的方向（以 Chunk 为单位）
        int shiftX = 0, shiftY = 0;

        if (px < EDGE_THRESHOLD) {
            // 玩家太靠左，窗口向左移（offsetX 减小），数据向右移
            shiftX = -Math.max(1, (EDGE_THRESHOLD - px) / CHUNK_SIZE + 1);
        } else if (px > WINDOW - EDGE_THRESHOLD) {
            shiftX = Math.max(1, (px - (WINDOW - EDGE_THRESHOLD)) / CHUNK_SIZE + 1);
        }

        if (py < EDGE_THRESHOLD) {
            shiftY = -Math.max(1, (EDGE_THRESHOLD - py) / CHUNK_SIZE + 1);
        } else if (py > WINDOW - EDGE_THRESHOLD) {
            shiftY = Math.max(1, (py - (WINDOW - EDGE_THRESHOLD)) / CHUNK_SIZE + 1);
        }

        if (shiftX != 0 || shiftY != 0) {
            shiftWindow(shiftX, shiftY);
        }
    }

    // ==================== 窗口平移核心 ====================

    /**
     * 平移窗口。
     * @param dx 虚拟坐标偏移变化量（瓦片）。正数 = 窗口向右移，数据向左移
     * @param dy 虚拟坐标偏移变化量（瓦片）
     */
    private static void shiftWindow(int dx, int dy) {
        if (dx == 0 && dy == 0) return;

        long t0 = Time.millis();
        Log.info("[InfiniteWorld] 平移窗口 dx=" + dx + " dy=" + dy);

        try {
            Tile[] oldTiles = getTileArray();
            if (oldTiles == null) {
                Log.err("[InfiniteWorld] shiftWindow: tiles 数组为 null");
                return;
            }
            int W = worldWidth();
            int H = worldHeight();

            // 归一化
            dx = Mathf.clamp(dx, -W + CHUNK_SIZE, W - CHUNK_SIZE);
            dy = Mathf.clamp(dy, -H + CHUNK_SIZE, H - CHUNK_SIZE);

            Tile[] newTiles = new Tile[W * H];

            // 1. 拷贝保留区域的 Tile 对象
            for (int x = 0; x < W; x++) {
                for (int y = 0; y < H; y++) {
                    int srcX = x + dx;
                    int srcY = y + dy;
                    if (srcX >= 0 && srcX < W && srcY >= 0 && srcY < H) {
                        Tile t = oldTiles[tileIndex(srcX, srcY, W)];
                        if (t != null) {
                            setTileXY(t, x, y);
                            newTiles[tileIndex(x, y, W)] = t;
                        }
                    }
                }
            }

            // 2. 在新暴露的区域生成地形
            generateNewArea(newTiles, dx, dy, W, H);

            // 3. 替换 Tiles 内部数组
            Object tilesObj = worldTilesField.get(world);
            if (tilesArrayField != null && tilesObj != null) {
                tilesArrayField.set(tilesObj, newTiles);
            } else {
                Log.err("[InfiniteWorld] 无法写回 tiles 数组");
                return;
            }

            // 4. 更新偏移量
            offsetX += dx;
            offsetY += dy;

            // 5. 同步所有实体坐标（像素级）
            float shiftPx = -dx * tilesize;
            float shiftPy = -dy * tilesize;
            syncEntityPositions(shiftPx, shiftPy);

            // 6. 触发瓦片变化
            bumpTileChanges();

            // 7. 重建空间索引
            rebuildBuildIndex();

            long dt = Time.millis() - t0;
            Log.info("[InfiniteWorld] 平移完成，耗时 " + dt + "ms, 新偏移 (" + offsetX + "," + offsetY + ")");

        } catch (Exception e) {
            Log.err("[InfiniteWorld] 平移失败", e);
        }
    }

    /** 在平移后新暴露的空白区域生成地形。 */
    private static void generateNewArea(Tile[] newTiles, int dx, int dy, int W, int H) {
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                int idx = tileIndex(x, y, W);
                if (newTiles[idx] != null) continue;

                // 虚拟坐标
                int vx = x + offsetX + dx;
                int vy = y + offsetY + dy;

                Tile t = createTile(x, y, vx, vy);
                newTiles[idx] = t;
            }
        }
    }

    // ==================== Tile 创建与地形生成 ====================

    /** 创建一个 Tile 并根据虚拟坐标生成地形。 */
    private static Tile createTile(int wx, int wy, int vx, int vy) {
        Tile tile = new Tile(wx, wy);
        generateTileContent(tile, vx, vy);
        return tile;
    }

    // ==================== 实体坐标同步 ====================

    /** 平移所有单位、子弹、建筑的像素坐标。 */
    private static void syncEntityPositions(float dx, float dy) {
        if (Groups.unit != null) {
            Groups.unit.each(u -> {
                if (u != null) {
                    u.x += dx;
                    u.y += dy;
                }
            });
        }
        if (Groups.bullet != null) {
            Groups.bullet.each(b -> {
                if (b != null) {
                    b.x += dx;
                    b.y += dy;
                }
            });
        }
        if (Groups.build != null) {
            Groups.build.each(b -> {
                if (b != null) {
                    b.x += dx;
                    b.y += dy;
                }
            });
        }
        // 特效组
        if (Groups.effect != null) {
            Groups.effect.each(e -> {
                if (e != null) {
                    e.x += dx;
                    e.y += dy;
                }
            });
        }
        // 玩家自身
        if (player != null && player.unit() != null) {
            // player.unit() 已在 Groups.unit 中处理
        }
    }

    /** 重建 Groups.build 的空间索引（平移后 QuadTree 需要重新插入）。 */
    private static void rebuildBuildIndex() {
        try {
            if (Groups.build == null) return;
            // Mindustry 的 QuadTreeGroup 有 clear() + rebuild 机制
            // 但直接重建可能有副作用，这里先尝试调用 tree() 后手动触发更新
            // 如果 Groups.build 有 rebuild 方法则调用，否则跳过
            java.lang.reflect.Method m = Groups.build.getClass().getMethod("rebuild");
            m.invoke(Groups.build);
        } catch (NoSuchMethodException nsme) {
            // 没有 rebuild 方法，尝试 clear + 重新 add
            try {
                Seq<Building> tmp = new Seq<>();
                Groups.build.each(tmp::add);
                Groups.build.clear();
                for (Building b : tmp) Groups.build.add(b);
            } catch (Exception e) {
                Log.err("[InfiniteWorld] 重建建筑索引失败", e);
            }
        } catch (Exception e) {
            Log.err("[InfiniteWorld] 重建建筑索引失败", e);
        }
    }

    // ==================== 反射辅助 ====================

    private static void setTileXY(Tile tile, int x, int y) {
        try {
            tileXField.set(tile, (byte) x);
            tileYField.set(tile, (byte) y);
        } catch (Exception e) {
            // 如果 x/y 不是 byte，尝试 int
            try {
                tileXField.set(tile, x);
                tileYField.set(tile, y);
            } catch (Exception e2) {
                Log.err("[InfiniteWorld] 设置 Tile 坐标失败", e2);
            }
        }
    }

    private static void bumpTileChanges() {
        try {
            int cur = worldTileChangesField.getInt(world);
            worldTileChangesField.setInt(world, cur + 1);
        } catch (Exception e) {
            Log.err("[InfiniteWorld] bump tileChanges 失败", e);
        }
    }

    // ==================== 坐标转换工具 ====================

    /** 虚拟瓦片坐标 -> 窗口内坐标。 */
    public static int virtualToWindowX(int vx) {
        return vx - offsetX;
    }

    public static int virtualToWindowY(int vy) {
        return vy - offsetY;
    }

    /** 窗口内坐标 -> 虚拟瓦片坐标。 */
    public static int windowToVirtualX(int wx) {
        return wx + offsetX;
    }

    public static int windowToVirtualY(int wy) {
        return wy + offsetY;
    }

    /** 获取虚拟坐标处的瓦片（如果在窗口内）。 */
    public static Tile tileAtVirtual(int vx, int vy) {
        int wx = vx - offsetX;
        int wy = vy - offsetY;
        if (wx < 0 || wx >= worldWidth() || wy < 0 || wy >= worldHeight()) return null;
        return world.tile(wx, wy);
    }

    // ==================== Chunk 数据结构 ====================

    /** 单个 Chunk 的数据。 */
    public static class Chunk {
        public final int cx, cy; // Chunk 坐标（虚拟瓦片 / CHUNK_SIZE）
        public Tile[][] tiles;   // CHUNK_SIZE x CHUNK_SIZE

        public Chunk(int cx, int cy) {
            this.cx = cx;
            this.cy = cy;
        }

        public int key() {
            return key(cx, cy);
        }

        public static int key(int cx, int cy) {
            // 简单的坐标编码，适用于坐标范围不大的情况
            return cx * 100000 + cy;
        }
    }

    // ==================== 调试 ====================

    public static int getOffsetX() { return offsetX; }
    public static int getOffsetY() { return offsetY; }
    public static boolean isEnabled() { return enabled; }
}
