package Npl.newSth;

import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.ai.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.graphics.g3d.PlanetGrid;
import mindustry.maps.generators.*;
import mindustry.type.Sector;
import mindustry.ui.dialogs.PlanetDialog;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import Npl.content.*;

import static mindustry.Vars.*;

/**
 * 阿泽厄尔（Azer）星球生成器。
 *
 * 结构总览：
 * ─ 参数区     所有可调参数按功能分组，generate() 只引用这里的参数
 * ─ 星球外观   HexMesher 接口实现（星球网格的高度与颜色，供 NoiseMesh / HexMesh 使用）
 * ─ 噪声       noise() 覆盖 + 地图生成各阶段共用的噪声入口
 * ─ 主流程     generate() 十个阶段，每个阶段一个方法
 * ─ 结构生成   陨石坑与 Holly 圣殿废墟
 *
 * 噪声约定：
 * ─ noise(x, y, ...) 接收原始格坐标，内部投影到星球表面（与原版一致）
 * ─ scl 是波长：想要"大块"地形就调大 scl，想要细碎就调小
 * ─ Simplex 返回 [0,1]，Ridged 返回 [-1, ~0.25]（脊线在 +0.25 附近）
 */
public class AzerPlanetGenerator extends PlanetGenerator {
    static {
        PlanetDialog.debugSelect = false;
    }

    {
        baseSeed = 1;
        defaultLoadout = Loadouts.basicBastion;
    }

    // ==================== 参数区 ====================

    // ---- 星球外观（NoiseMesh 网格用）----
    /** 地形基础噪声波长 */
    public float heightScl = 0.9f;
    /** genTile 静态墙开洞的 Ridged 参数 */
    public static float airThresh = 0.13f, airScl = 14;

    // ---- 群系分区 ----
    public static float biomeNoiseScl = 300f;   // 群系噪声波长（大 = 连贯大块群系）
    public static float desertThresh = 0.33f;   // bNoise < 此值 → 沙漠群系
    public static float rockThresh = 0.66f;     // bNoise < 此值 → 岩石群系，否则 → 圣殿群系
    public static float altitudeScl = 70f;      // 群系内部海拔噪声波长

    // ---- 河流 / 湖泊 ----
    public static float riverScl = 55f;         // Ridged 河流噪声波长
    public static float riverThresh = 0.17f;    // 河流阈值（越大河道越窄）
    public static float lakeScl = 150f;         // 湖泊噪声波长
    public static float lakeThresh = 0.62f;     // 湖泊阈值（越大湖越少）

    // ---- 山体墙 ----
    public static float wallScl = 36f;          // 墙体噪声波长（大 = 大块山体）
    public static float wallThresh = 0.55f;     // 成墙阈值（越大墙越少）

    // ---- 陨石坑 ----
    public static int craterCount = 5;
    public static int craterMinRadius = 8, craterMaxRadius = 18;
    public static float craterOreChance = 0.12f;    // 内环矿石概率
    public static float craterCrystalChance = 0.3f; // 中心铀晶体墙概率

    // ---- Holly 圣殿废墟 ----
    public static int hollyRuinTries = 10;      // 尝试次数
    public static int hollyRuinTarget = 4;      // 期望生成数量
    public static int hollyRuinMinSize = 14, hollyRuinMaxSize = 22;

    // ---- 白色替换接口（预览工具用）----
    public @Nullable Color replaceColor = null;
    public float whiteThreshold = 0.8f;

    // 出生点 / 敌方空投点（setupSpawns 计算后供收尾阶段使用）
    protected int spawnX, spawnY, endX, endY;

    // ==================== 星球外观（HexMesher）====================

    /** 地形基础噪声 [0, 1.4]，驱动星球网格起伏与地板分层。 */
    public float getRawNoise(Vec3 position) {
        return Simplex.noise3d(seed + 321, 4, 0.5f, 1f / heightScl, position.x, position.y, position.z) * 1.4f;
    }

    /** 纬度与噪声混合 [0, 1]，用于模糊高度边界。 */
    public float getTerrainNoise(Vec3 position) {
        float temp = Mathf.clamp(Math.abs(position.y * 2f));
        float tnoise = Simplex.noise3d(seed + 192, 2, 0.6f, 6f, position.x, position.y + 999f, position.z);
        return Mathf.lerp(temp, (tnoise + 1f) / 2f, 0.5f);
    }

    /** 颜色明暗抖动系数 [0.85, 1.15]。 */
    public float getColorNoise(Vec3 position) {
        return 1f + (Simplex.noise3d(seed + 1, 6, 0.72f, 0.2f, position.x, position.y, position.z) * 0.3f - 0.15f);
    }

    /** 星球网格高度 [0.15, ~1]。 */
    public float getRawHeight(Vec3 position) {
        return (float) Math.pow(Interp.reverse.apply(Mathf.clamp(Math.abs(getRawNoise(position) - 0.645f) * 1.2f)) * 0.895f, 1.2f) + 0.15f;
    }

    /** 按高度 + 地形噪声选择地板（genTile 用）。 */
    public Block getFloor(Vec3 position) {
        float h = getRawNoise(position) + (getTerrainNoise(position) - 0.5f) * 0.15f;

        // TODO：低海拔血液污染占位 —— h < 0.2 且 tnoise 命中 → envBlocks.bloodFloor
        // TODO：高海拔荧光污染占位 —— h > 0.75 且 tnoise 命中 → envBlocks.fluorFloor

        if (h < 0.3f) return envBlocks.desert;
        if (h < 0.5f) return envBlocks.yellowStone;
        if (h < 0.7f) return envBlocks.brownStone;
        return envBlocks.rubberFloor;
    }

    /** 地板颜色，相邻高度带之间平滑过渡（星球网格配色）。 */
    public void getFloorColor(Vec3 position, Color out) {
        float h = getRawNoise(position) + (getTerrainNoise(position) - 0.5f) * 0.15f;

        if (h < 0.3f) {
            out.set(envBlocks.desert.mapColor).lerp(envBlocks.yellowStone.mapColor,
                    Interp.smooth.apply(Mathf.clamp(h / 0.3f, 0f, 1f)));
        } else if (h < 0.5f) {
            out.set(envBlocks.yellowStone.mapColor).lerp(envBlocks.brownStone.mapColor,
                    Interp.smooth.apply(Mathf.clamp((h - 0.3f) / 0.2f, 0f, 1f)));
        } else if (h < 0.7f) {
            out.set(envBlocks.brownStone.mapColor).lerp(envBlocks.rubberFloor.mapColor,
                    Interp.smooth.apply(Mathf.clamp((h - 0.5f) / 0.2f, 0f, 1f)));
        } else {
            out.set(envBlocks.rubberFloor.mapColor);
        }

        // TODO：污染色彩叠加占位 —— 低海拔混入暗红，高海拔混入荧光绿
    }

    @Override
    public float getSizeScl() {
        return 2000 * 1.07f * 6f / 5f;
    }

    @Override
    public float getHeight(Vec3 position) {
        return Math.max(getRawHeight(position), 0.15f) * 0.85f + 0.1f;
    }

    @Override
    public void getColor(Vec3 position, Color out) {
        getFloorColor(position, out);
        out.mul(getColorNoise(position));

        // 白色替换接口：接近纯白的像素替换为 replaceColor
        if (replaceColor != null && out.r >= whiteThreshold && out.g >= whiteThreshold && out.b >= whiteThreshold) {
            float a = out.a;
            out.set(replaceColor);
            out.a = a;
        }
    }

    @Override
    public boolean isEmissive() {
        return true;
    }

    @Override
    public void genTile(Vec3 position, TileGen tile) {
        tile.floor = getFloor(position);
        tile.block = tile.floor.asFloor().wall;

        // Ridged 噪声在静态墙上开洞，避免整片实心
        if (Ridged.noise3d(seed + 124, position.x, position.y, position.z, 4, 12.92f) > -0.45f) tile.block = Blocks.air;
        if (Ridged.noise3d(seed + 1, position.x, position.y, position.z, 2, airScl) > airThresh) tile.block = Blocks.air;
    }

    // ==================== 噪声 ====================

    /**
     * 所有 pass() 阶段共用的噪声入口。
     * 覆盖原因：基类固定用种子 0，全星球每个扇区都是同一片噪声场；
     * 这里改用本扇区种子（Serpulo 同款做法），让每个扇区地形各不相同。
     */
    @Override
    protected float noise(float x, float y, double octaves, double falloff, double scl, double mag) {
        Vec3 v = sector.rect.project(x, y);
        return Simplex.noise3d(seed, octaves, falloff, 1f / scl, v.x, v.y, v.z) * (float) mag;
    }

    // ==================== 主流程 ====================

    @Override
    protected void generate() {
        smoothBase();                                                             // 1. 基础平滑
        partitionBiomes();                                                        // 2. 群系分区
        carveWaters();                                                            // 3. 河流与湖泊
        placeWalls();                                                             // 4. 山体墙
        polishTerrain();                                                          // 5. 边缘平滑
        generateCraters(craterCount, craterMinRadius, craterMaxRadius);           // 6. 陨石坑
        setupSpawns();                                                            // 7. 出生点与通路
        generateHollyRuins(hollyRuinTries, hollyRuinMinSize, hollyRuinMaxSize);   // 8. 圣殿废墟
        generateOres();                                                           // 9. 矿物
        finish();                                                                 // 10. 收尾
    }

    /** 阶段 1：元胞自动机 + 扭曲 + 中值滤波，抹平 genTile 的逐格噪声碎块。 */
    private void smoothBase() {
        cells(4);
        distort(6, 12);
        median(3);
    }

    /** 阶段 2：大尺度噪声把地图分成三大群系，群系内部再按海拔细分地板。 */
    private void partitionBiomes() {
        pass((x, y) -> {
            float bNoise = noise(x + 500, y + 500, 2, 0.5f, biomeNoiseScl, 1f);
            float alt = noise(x, y, 2, 0.6f, altitudeScl, 1f);

            if (bNoise < desertThresh) {
                // 沙漠群系：desert 为主
                if (alt < 0.3f) floor = envBlocks.desert;
                else if (alt < 0.55f) floor = envBlocks.yellowStone;
                else floor = envBlocks.brownStone;
            } else if (bNoise < rockThresh) {
                // 岩石群系：yellowStone / brownStone 为主，高处橡胶林
                if (alt < 0.35f) floor = envBlocks.yellowStone;
                else if (alt < 0.6f) floor = envBlocks.brownStone;
                else floor = envBlocks.rubberFloor;
            } else {
                // 圣殿群系：混入 hollyFloor
                if (alt < 0.3f) floor = envBlocks.desert;
                else if (alt < 0.45f) floor = envBlocks.hollyFloor;
                else if (alt < 0.65f) floor = envBlocks.yellowStone;
                else floor = envBlocks.brownStone;
            }
        });
    }

    /**
     * 阶段 3：河流与湖泊。
     * 河流 = Ridged 噪声的脊线（Serpulo 同款做法），两个关键点：
     * ─ 必须用原始格坐标投影 project(x, y)：归一化坐标会让噪声在整张图上近乎恒定，河流完全消失
     * ─ 必须用单阈值：原来的区间阈值 (0.15~0.22) 会在扰动为正时把河心切掉
     */
    private void carveWaters() {
        pass((x, y) -> {
            Vec3 v = sector.rect.project(x, y);

            // 每扇区独立的高频扰动，让河道宽度自然变化、断续有致
            float rr = Simplex.noise2d(sector.id, 2, 0.6f, 1f / 7f, x, y) * 0.1f;
            float river = Ridged.noise3d(2, v.x, v.y, v.z, 1, 1f / riverScl) + rr;

            // 湖泊：大尺度低频噪声命中且处于低洼处
            float lake = noise(x + 300, y + 300, 2, 0.5f, lakeScl, 1f);
            boolean isLake = lake > lakeThresh && noise(x, y, 2, 0.6f, altitudeScl, 1f) < 0.22f;

            if (river > riverThresh || isLake) {
                floor = envBlocks.lossLiquid;
            }
        });
    }

    /** 阶段 4：山体墙。所有群系共用一面噪声场，墙体跨群系连成大块。 */
    private void placeWalls() {
        pass((x, y) -> {
            float wall = noise(x, y, 2, 0.4f, wallScl, 1f);

            if (floor == envBlocks.desert && wall > wallThresh) {
                block = envBlocks.desertWall;
            } else if (floor == envBlocks.yellowStone && wall > wallThresh) {
                block = envBlocks.yellowStoneWall;
            } else if (floor == envBlocks.brownStone && wall > wallThresh) {
                block = envBlocks.brownStoneWall;
            } else if (floor == envBlocks.hollyFloor) {
                if (wall > 0.5f) block = envBlocks.hollyWall;
                else if (noise(x, y + 50, 2, 0.4f, wallScl, 1f) > 0.6f) block = envBlocks.glossy;
            }

            // 水面不立墙
            if (floor == envBlocks.lossLiquid) {
                block = Blocks.air;
            }
        });
    }

    /** 阶段 5：中值滤波 + 边界混合 + 扭曲，让群系边界自然过渡。 */
    private void polishTerrain() {
        median(2, 0.6, envBlocks.desert);
        blend(envBlocks.yellowStone, envBlocks.brownStone, 4);
        blend(envBlocks.desert, envBlocks.yellowStone, 4);
        distort(10f, 12f);
        distort(5f, 7f);
        median(2, 0.6, envBlocks.brownStone);
        median(3, 0.6, envBlocks.rubberFloor);

        pass((x, y) -> {
            // brownStone 区域穿插 yellowStone 条带，打破单调
            if (noise(x, y + 600 + x, 2, 0.6f, 80f, 1f) < 0.41f && floor == envBlocks.brownStone) {
                floor = envBlocks.yellowStone;
            }

            // 群系重分配后残留的静态墙 → 统一换成当前地板对应的墙
            if (block.isStatic()) {
                if (floor == envBlocks.desert) block = envBlocks.desertWall;
                else if (floor == envBlocks.yellowStone) block = envBlocks.yellowStoneWall;
                else if (floor == envBlocks.brownStone) block = envBlocks.brownStoneWall;
            }

            // 液体区域上方不保留墙体
            if (floor == envBlocks.lossLiquid) {
                block = Blocks.air;
            }
        });
    }

    /** 阶段 7：确定出生点与敌方空投点，清出通路，保证两点都在陆地上。 */
    private void setupSpawns() {
        float length = width / 4f;
        Vec2 trns = Tmp.v1.trns(rand.random(360f), length);
        spawnX = (int) (trns.x + width / 2f);
        spawnY = (int) (trns.y + height / 2f);
        endX = (int) (-trns.x + width / 2f);
        endY = (int) (-trns.y + height / 2f);
        float maxd = Mathf.dst(width / 2f, height / 2f);

        // 河流可能正好穿过出生点/空投点，先填成陆地
        toLand(spawnX, spawnY, 16);
        toLand(endX, endY, 8);

        erase(spawnX, spawnY, 35);
        brush(pathfind(spawnX, spawnY, endX, endY,
                tile -> (tile.solid() ? 300f : 0f) + maxd - tile.dst(width / 2f, height / 2f) / 10f,
                Astar.manhattan), 9);
        erase(endX, endY, 21);

        // 封死无法从出生点到达的孤岛区域
        inverseFloodFill(tiles.getn(spawnX, spawnY));

        blend(envBlocks.brownStone, envBlocks.rubberFloor, 4);

        erase(endX, endY, 6);
        tiles.getn(endX, endY).setOverlay(Blocks.spawn);
    }

    /** 把 (cx, cy) 附近半径 rad 内的液体填为沙漠。 */
    private void toLand(int cx, int cy, int rad) {
        for (int x = -rad; x <= rad; x++) {
            for (int y = -rad; y <= rad; y++) {
                Tile tile = tiles.get(cx + x, cy + y);
                if (tile != null && Mathf.within(x, y, rad) && tile.floor().isLiquid) {
                    tile.setFloor(envBlocks.desert.asFloor());
                }
            }
        }
    }

    /** 阶段 9：矿物。地表矿用继承的 ore()，墙壁矿用 wallOreCluster()，数量从多到少。 */
    private void generateOres() {
        // 1. bigIron（最多）
        ore(envBlocks.bigIronOre, envBlocks.desert, 6f, 0.78f);
        ore(envBlocks.bigIronOre, envBlocks.yellowStone, 7f, 0.80f);
        ore(envBlocks.bigIronOre, envBlocks.brownStone, 8f, 0.80f);
        wallOreCluster(envBlocks.desertWall, envBlocks.bigIronWores, 9f, 0.80f);
        wallOreCluster(envBlocks.yellowStoneWall, envBlocks.bigIronWores, 10f, 0.82f);
        wallOreCluster(envBlocks.brownStoneWall, envBlocks.bigIronWores, 11f, 0.84f);

        // 2. 煤（次多）
        ore(envBlocks.coalHill, envBlocks.desert, 4.5f, 0.85f);
        ore(envBlocks.coalHill, envBlocks.yellowStone, 5.5f, 0.87f);
        ore(envBlocks.coalHill, envBlocks.brownStone, 6.5f, 0.87f);
        wallOreCluster(envBlocks.desertWall, envBlocks.coalHillOre, 7f, 0.87f);
        wallOreCluster(envBlocks.yellowStoneWall, envBlocks.coalHillOre, 8f, 0.89f);
        wallOreCluster(envBlocks.brownStoneWall, envBlocks.coalHillOre, 12f, 0.90f);

        // 3. 硫（中等）
        ore(envBlocks.sulfurFragOre, envBlocks.yellowStone, 13f, 0.92f);
        ore(envBlocks.sulfurFragOre, envBlocks.brownStone, 14f, 0.92f);
        wallOreCluster(envBlocks.yellowStoneWall, envBlocks.sulfurFragWores, 15f, 0.93f);
        wallOreCluster(envBlocks.brownStoneWall, envBlocks.sulfurFragWores, 16f, 0.93f);

        // 4. 聚酯（较少）
        ore(envBlocks.frailPolyesterOre, envBlocks.brownStone, 17f, 0.95f);
        wallOreCluster(envBlocks.brownStoneWall, envBlocks.frailPolyesterWores, 18f, 0.95f);

        // 5. 浮岩（最少）
        ore(envBlocks.pumiceMegaOre, envBlocks.brownStone, 19f, 0.98f);
        wallOreCluster(envBlocks.brownStoneWall, envBlocks.pumiceMegaWores, 20f, 0.98f);
    }

    /**
     * 在 src 墙上生成 dest 墙壁矿。
     * 墙壁矿是带 wallOre 标记的 overlay（OreBlock），必须用 ore = dest 挂到墙上渲染：
     * 直接 setOverlay 会被 pass() 结尾的回写覆盖，直接 setBlock 则会拆掉墙体。
     */
    public void wallOreCluster(Block src, Block dest, float i, float thresh) {
        pass((x, y) -> {
            if (block != src || !nearAir(x, y)) return;

            if (Math.abs(0.5f - noise(x, y + i * 999, 2, 0.7f, 40f + i * 2f)) > 0.26f * thresh &&
                    Math.abs(0.5f - noise(x, y - i * 999, 1, 1f, 30f + i * 4f)) > 0.37f * thresh) {
                ore = dest;
            }
        });
    }

    /** 阶段 10：清理、装饰、规则与空投仓。 */
    private void finish() {
        // 清理矿附近的树，避免遮挡
        pass((x, y) -> {
            if ((ore instanceof Floor && ore.asFloor().wallOre) || block.itemDrop != null || (block == Blocks.air && ore != Blocks.air)) {
                removeWall(x, y, 3, b -> b == envBlocks.hollyTree || b == envBlocks.rubberTree);
            }
        });

        trimDark();

        // 清掉落在液体上的地表矿 overlay
        for (Tile tile : tiles) {
            if (tile.overlay().needsSurface && !tile.floor().hasSurface()) {
                tile.setOverlay(Blocks.air);
            }
        }

        decoration(0.017f);

        state.rules.env = sector.planet.defaultEnv;
        state.rules.placeRangeCheck = true;
        Schematics.placeLaunchLoadout(spawnX, spawnY);
        state.rules.waves = false;
        state.rules.hideSpawns = false;
    }

    // ==================== 结构生成 ====================

    /**
     * 阶段 6：生成若干陨石坑（凹陷地形 + 辐射矿石）。
     * 中心放 thalliumOre + uranCrystalWall，内环下沉为 brownStone 并散落矿石，外环与地形融合。
     */
    public void generateCraters(int count, int minRadius, int maxRadius) {
        for (int i = 0; i < count; i++) {
            int cx = rand.random(20, width - 20);
            int cy = rand.random(20, height - 20);
            int radius = rand.random(minRadius, maxRadius);

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    int x = cx + dx, y = cy + dy;
                    if (!Structs.inBounds(x, y, width, height)) continue;

                    float dist = Mathf.sqrt(dx * dx + dy * dy) / radius;
                    if (dist > 1f) continue;

                    Tile tile = tiles.get(x, y);
                    if (tile.floor().isLiquid) continue;

                    if (dist < 0.15f) {
                        // 中心：辐射矿石 + 可能的铀晶体墙
                        tile.setOverlay(envBlocks.thalliumOre);
                        if (rand.chance(craterCrystalChance)) {
                            tile.setBlock(envBlocks.uranCrystalWall);
                        }
                    } else if (dist < 0.55f) {
                        // 内环：下沉为 brownStone，散落矿石
                        tile.setFloor(envBlocks.brownStone.asFloor());
                        if (tile.block().solid) tile.setBlock(Blocks.air);
                        if (rand.chance(craterOreChance)) {
                            tile.setOverlay(envBlocks.thalliumOre);
                        }
                    } else if (dist < 0.85f) {
                        // 外环：与周围地形融合
                        if (tile.floor() == envBlocks.desert) {
                            tile.setFloor(envBlocks.yellowStone.asFloor());
                        }
                        if (tile.block().solid && rand.chance(0.5f)) {
                            tile.setBlock(Blocks.air);
                        }
                    }
                    // 0.85~1.0：边缘保持不变，自然过渡
                }
            }
        }
    }

    /**
     * 阶段 8：随机寻找空旷处生成破损矩形圣殿废墟。
     * @param tries    尝试次数
     * @param minSize  矩形最小边长
     * @param maxSize  矩形最大边长
     */
    public void generateHollyRuins(int tries, int minSize, int maxSize) {
        int placed = 0;
        for (int attempt = 0; attempt < tries && placed < hollyRuinTarget; attempt++) {
            int cx = rand.random(20, width - 20);
            int cy = rand.random(20, height - 20);
            int size = rand.random(minSize, maxSize);
            int half = size / 2;

            // 空间检查：范围内非实心占比需 >= 0.7
            int total = 0, empty = 0;
            for (int dx = -half - 2; dx <= half + 2; dx++) {
                for (int dy = -half - 2; dy <= half + 2; dy++) {
                    Tile tile = tiles.get(cx + dx, cy + dy);
                    if (tile == null) continue;
                    total++;
                    if (!tile.block().solid) empty++;
                }
            }
            if (total == 0 || (float) empty / total < 0.7f) continue;

            placeHollyRuin(cx, cy, size);
            placed++;
        }
    }

    /**
     * 在 (cx, cy) 生成一个破损矩形圣殿：
     * 外围残墙（四角 40% 放墙，普通边缘 65%），内圈零星残墙（20%），
     * 内部 hollyFloor，中心 3x3 为 altar，随机点缀 1~3 个 glossy 蒸汽口。
     */
    public void placeHollyRuin(int cx, int cy, int size) {
        int half = size / 2;
        for (int dx = -half; dx <= half; dx++) {
            for (int dy = -half; dy <= half; dy++) {
                int wx = cx + dx, wy = cy + dy;
                if (!Structs.inBounds(wx, wy, width, height)) continue;
                Tile tile = tiles.getn(wx, wy);

                boolean edge = (dx == -half || dx == half || dy == -half || dy == half);
                boolean corner = Math.abs(dx) == half && Math.abs(dy) == half;
                boolean innerEdge = (Math.abs(dx) == half - 1 || Math.abs(dy) == half - 1)
                        && !(Math.abs(dx) == half || Math.abs(dy) == half);

                // 中心 3x3：altar（圣殿核心）
                if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) {
                    tile.setFloor(envBlocks.altar.asFloor());
                    tile.setBlock(Blocks.air);
                    continue;
                }

                if (edge) {
                    if (rand.chance(corner ? 0.4f : 0.65f)) {
                        tile.setBlock(envBlocks.hollyWall);
                    } else if (tile.block().solid) {
                        tile.setBlock(Blocks.air);
                    }
                    tile.setFloor(envBlocks.hollyFloor.asFloor());
                } else if (innerEdge) {
                    if (rand.chance(0.2f)) {
                        tile.setBlock(envBlocks.hollyWall);
                    } else if (tile.block().solid) {
                        tile.setBlock(Blocks.air);
                    }
                    tile.setFloor(envBlocks.hollyFloor.asFloor());
                } else {
                    tile.setFloor(envBlocks.hollyFloor.asFloor());
                    if (tile.block().solid) tile.setBlock(Blocks.air);
                }
            }
        }

        // 内部随机放 glossy 蒸汽口（避开 altar 中心与残墙）
        int glossyCount = rand.random(1, 3);
        int tries = 0;
        while (glossyCount > 0 && tries < 50) {
            tries++;
            int gdx = rand.random(-half + 1, half - 1);
            int gdy = rand.random(-half + 1, half - 1);
            int gx = cx + gdx, gy = cy + gdy;
            if (!Structs.inBounds(gx, gy, width, height)) continue;
            Tile gt = tiles.getn(gx, gy);
            if (Math.abs(gdx) <= 1 && Math.abs(gdy) <= 1) continue;
            if (gt.block().solid) continue;
            if (gt.floor() == envBlocks.glossy) continue;
            gt.setFloor(envBlocks.glossy.asFloor());
            glossyCount--;
        }
    }

    // ==================== 扇区规则 ====================

    @Override
    public boolean allowAcceleratorLanding(Sector sector) {
        return super.allowAcceleratorLanding(sector) && isLandSector(sector);
    }

    @Override
    public boolean allowLanding(Sector sector) {
        return true;
    }

    /** 判断扇区是否以陆地为主（中心 + 各角高度达标）。 */
    public boolean isLandSector(Sector sector) {
        if (sector == null) return true;
        int land = 0;
        if (getHeight(sector.tile.v) > 0.3f) land++;
        for (PlanetGrid.Corner corner : sector.tile.corners) {
            if (getHeight(corner.v) > 0.3f) land += 5;
        }
        return land > 5;
    }
}
