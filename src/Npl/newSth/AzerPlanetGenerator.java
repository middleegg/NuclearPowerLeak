package Npl.newSth;

import arc.func.Boolf;
import arc.func.Cons;
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
import mindustry.world.meta.*;
import Npl.content.*;

import static mindustry.Vars.*;

public class AzerPlanetGenerator extends PlanetGenerator {
    static {
        PlanetDialog.debugSelect = false;
    }

    // ===== 白色替换接口 =====
    public @Nullable Color replaceColor = null;
    public float whiteThreshold = 0.8f;

    // ===== Holly 废墟群系参数 =====
    public static float hollyFreq1 = 0.05f;
    public static float hollyFreq2 = 0.07f;
    public static float hollyNoiseScl = 5.5f;
    public static float hollyDstMul = 0.85f;
    public static float hollyBandThresh = 0.15f;

    // ===== 其他参数 =====
    public float octaves = 14, persistence = 0.7f, heightScl = 0.9f, heightPow = 3f, heightMult = 1.6f;
    public static float airThresh = 0.13f, airScl = 14;
    public static float baseRadius = 0.65f;

    // ===== 地形二维表（懒加载，避免 envBlocks 尚未初始化时出现 NPE）=====
    Block[][] terrains;
    Block[][] terrains(){
        if(terrains == null){
            Block
                    ds = envBlocks.desert,
                    ys = envBlocks.yellowStone,
                    bs = envBlocks.brownStone,
                    rb = envBlocks.rubberFloor;
            terrains = new Block[][]{
                    // ===== 25行 × 9列，分散分布，避免带状 =====
                    // 每行都混合多种地板，desert 占主体

                    // 行 0-5：低海拔，desert 为主，
                    {ds, ds, ds, ds, ds, ds, ds, ds, ds},
                    {ds, ds, ds, ds, ds, ds, ds, ds, ds},
                    {ds, ds, ds, ds, ds, ds, ds, ds, ds},
                    {ds, ds, ds, ds, ds, ds, ds, ds, ds},
                    {ds, ds, ds, ds, ds, ds, ds, ds, ds},
                    {ds, ds, ds, ds, ds, ds, ds, ds, ds},

                    // 行 6-12：中低海拔，desert 仍为主，yellowStone/brownStone 增多
                    {ds, ys, ds, ds, ds, ds, ys, ds, ds},
                    {ds, ds, ds, ys, ds, ds, ds, ds, ds},
                    {ys, ds, ds, ds, ds, ds, ys, ds, ds},
                    {ds, ds, ys, ds, ds, ds, ds, ys, ds},
                    {ds, ds, ds, ds, ds, ys, ds, ds, ds},
                    {ds, ds, ds, ds, ys, ds, ds, ds, ds},
                    {ys, ds, ds, ds, ds, ds, ds, ys, ds},

                    // 行 13-17：中海拔，三种地板混合，desert 仍略多
                    {ds, ys, bs, ds, ds, ys, bs, ds, ds},
                    {ys, bs, ds, ds, ys, ds, bs, ds, ds},
                    {ds, bs, ys, ds, bs, ds, ys, ds, bs},
                    {ys, ds, ds, bs, ds, ys, ds, bs, bs},
                    {ds, bs, ds, ys, bs, ds, ds, bs, ds},

                    // 行 18-21：中高海拔，yellowStone/brownStone 增多，desert 减少
                    {ys, bs, ds, bs, ys, bs, ds, ys, bs},
                    {bs, ys, bs, ds, bs, ys, bs, bs, ys},
                    {bs, bs, ys, bs, rb, ds, bs, ys, ds},
                    {bs, rb, ds, bs, ys, rb, bs, ds, ys},

                    // 行 22-24：高海拔，rubberFloor 适度（~7%）
                    {rb, ds, ds, rb, ds, ds, ds, bs, ds},
                    {bs, ds, ds, ys, bs, ds, bs, ds, ds},
                    {rb, ds, rb, ys, ds, bs, ds, bs, ds},
            };
        }
        return terrains;
    }

    {
        baseSeed = 2;
        defaultLoadout = Loadouts.basicBastion;
    }
    {
        baseSeed = 1;
    }

    public AzerPlanetGenerator() {
    }

    // ========= 纬度 / 噪声 =========

    public float getLatitude(Vec3 position) {
        return Math.abs(Mathf.atan2(position.y, Mathf.sqrt(Mathf.sqr(position.x) + Mathf.sqr(position.z))) * Mathf.radDeg - 90f);
    }

    public float getRawNoise(Vec3 position) {
        return Simplex.noise3d(seed + 321, octaves, 0.42f, 1f / heightScl, position.x, position.y, position.z) * 1.4f;
    }

    public float getTerrainNoise(Vec3 position) {
        float temp = Mathf.clamp(Math.abs(position.y * 2f));
        float tnoise = Simplex.noise3d(seed + 192, 4, 0.85f, 2.8f, position.x, position.y + 999f, position.z);
        return Mathf.lerp(temp, (tnoise + 1f) / 2f, 0.5f);
    }

    public float getColorNoise(Vec3 position) {
        return 1f + (Simplex.noise3d(seed + 1, 6, 0.72f, 0.2f, position.x, position.y, position.z) * 0.3f - 0.15f);
    }

    public float getRawHeight(Vec3 position) {
        return (float) Math.pow(Interp.reverse.apply(Mathf.clamp(Math.abs(getRawNoise(position) - 0.645f) * 1.2f)) * 0.895f, 1.2f) + 0.15f;
    }

    // ========= terrains 表查询 =========

    public Block getFloor(Vec3 position) {
        Block[][] ts = terrains();
        int size = ts.length;
        float rowF = Mathf.clamp(getRawNoise(position) * size, 0f, size - 1f);
        int row = Mathf.round(rowF);
        Block[] r = ts[row];
        float colF = Mathf.clamp(getTerrainNoise(position) * r.length, 0f, r.length - 1f);
        int col = Mathf.round(colF);
        return r[col];
    }

    public void sampleRowColor(Vec3 position, int row, Color out) {
        Block[] r = terrains()[row];
        float colF = Mathf.clamp(getTerrainNoise(position) * r.length, 0f, r.length - 1f);
        int c0 = (int) colF;
        int c1 = Math.min(c0 + 1, r.length - 1);
        float ct = Interp.smooth.apply(colF - c0);
        out.set(r[c0].mapColor).lerp(r[c1].mapColor, ct);
    }

    public void getFloorColor(Vec3 position, Color out) {
        Block[][] ts = terrains();
        int size = ts.length;
        float rowF = Mathf.clamp(getRawNoise(position) * size, 0f, size - 1f);
        int r0 = (int) rowF;
        int r1 = Math.min(r0 + 1, size - 1);
        float rt = Interp.smooth.apply(rowF - r0);
        sampleRowColor(position, r0, Tmp.c1);
        sampleRowColor(position, r1, Tmp.c2);
        out.set(Tmp.c1).lerp(Tmp.c2, rt);
    }

    public int getDensity(Vec3 position) {
        return Mathf.clamp((int) (Simplex.noise3d(seed + 111, 12, 0.42f, 8.7f, position.x, position.y, position.z) * 4f - 1f), 0, 3);
    }

    @Override
    public float getSizeScl() {
        return 2000 * 1.07f * 6f / 5f;
    }

    float rawHeight(Vec3 position) {
        return Simplex.noise3d(seed, octaves, persistence, 1f / heightScl, 10f + position.x, 10f + position.y, 10f + position.z);
    }

    @Override
    public float getHeight(Vec3 position) {
        float height = getRawHeight(position);
        return Math.max(height, 0.15f) * 0.85f + 0.1f;
    }

    @Override
    public void getColor(Vec3 position, Color out) {
        getFloorColor(position, out);
        out.mul(getColorNoise(position));
        if (replaceColor != null && out.r >= whiteThreshold && out.g >= whiteThreshold && out.b >= whiteThreshold) {
            float a = out.a;
            out.set(replaceColor);
            out.a = a;
        }
    }

    @Override
    public void genTile(Vec3 position, TileGen tile) {
        tile.floor = getFloor(position);
        tile.block = tile.floor.asFloor().wall;

        if (Ridged.noise3d(seed + 124, position.x, position.y, position.z, 4, 12.92f) > -0.45) tile.block = Blocks.air;
        if (Ridged.noise3d(seed + 1, position.x, position.y, position.z, 2, airScl) > airThresh) tile.block = Blocks.air;
    }

    // ========= generate(Tiles, Sector, WorldParams)：Midantha 同款覆盖版 =========

    public void generate(Tiles tiles, Sector sec, WorldParams params) {
        this.tiles = tiles;
        this.seed = params.seedOffset + baseSeed;
        this.sector = sec;
        this.width = tiles.width;
        this.height = tiles.height;
        this.rand.setSeed(sec.id + params.seedOffset + baseSeed);

        TileGen gen = new TileGen();
        Vec3 pos = new Vec3();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                gen.reset();
                pos.set(sector.rect.project(x / (float) tiles.width, y / (float) tiles.height));
                genTile(pos, gen);
                Tile tile = new Tile(x, y, gen.floor, gen.overlay, gen.block);
                tiles.set(x, y, tile);
            }
        }

        generate(tiles, params);
    }

    // ========= 扇区判断 =========

    @Override
    public boolean allowAcceleratorLanding(Sector sector) {
        return super.allowAcceleratorLanding(sector) && isLandSector(sector);
    }

    @Override
    public boolean allowLanding(Sector sector) {
        return true;
    }

    public boolean isLandSector(Sector sector) {
        if (sector == null) return true;
        int land = 0;
        if (getHeight(sector.tile.v) > 0.3f) land++;
        for (PlanetGrid.Corner corner : sector.tile.corners) {
            if (getHeight(corner.v) > 0.3f) land += 5;
        }
        return land > 5;
    }

    // ========= 矿簇辅助 =========

    public void clusterOre(Block dest, Block src, float i, float thresh) {
        pass((x, y) -> {
            if (floor != src) return;
            if (Math.abs(0.5f - noise(x, y + i * 999, 2, 0.7f, 40f + i * 2f)) > 0.26f * thresh &&
                    Math.abs(0.5f - noise(x, y - i * 999, 1, 1f, 30f + i * 4f)) > 0.37f * thresh) {
                ore = dest;
            }
        });
    }

    public void clusterWallOre(Block src, Block dest, float i, float thresh) {
        boolean overlay = dest.isOverlay();
        pass((x, y) -> {
            if (block != src) return;
            boolean empty = false;
            for (Point2 p : Geometry.d8) {
                Tile other = tiles.get(x + p.x, y + p.y);
                if (other != null && other.block() == Blocks.air) {
                    empty = true;
                    break;
                }
            }
            if (!empty) return;
            if (Math.abs(0.5f - noise(x, y + i * 999, 2, 0.7f, 40f + i * 2f)) > 0.26f * thresh &&
                    Math.abs(0.5f - noise(x, y - i * 999, 1, 1f, 30f + i * 4f)) > 0.37f * thresh) {
                if (overlay) {
                    tiles.getn(x, y).setOverlay(dest);
                } else {
                    block = dest;
                }
            }
        });
    }

    // ========= Holly 圣殿废墟生成 =========

    /**
     * 随机寻找空旷处生成破损矩形圣殿废墟。
     * @param count    尝试次数
     * @param minSize  矩形最小边长
     * @param maxSize  矩形最大边长
     */
    public void generateHollyRuins(int count, int minSize, int maxSize){
        int attempts = 0;
        int placed = 0;
        int target = 4; // 期望生成的废墟数量
        while(attempts < count && placed < target){
            attempts++;
            int cx = rand.random(20, width - 20);
            int cy = rand.random(20, height - 20);
            int size = rand.random(minSize, maxSize);
            int half = size / 2;

            // 1. 检查空间：范围内需足够空旷（block == air 比例 >= 0.7）
            int total = 0, empty = 0;
            for(int dx = -half - 2; dx <= half + 2; dx++){
                for(int dy = -half - 2; dy <= half + 2; dy++){
                    Tile tile = tiles.get(cx + dx, cy + dy);
                    if(tile == null) continue;
                    total++;
                    if(!tile.block().solid) empty++;
                }
            }
            if(total == 0 || (float)empty / total < 0.7f) continue;

            // 2. 生成破损矩形圣殿
            placeHollyRuin(cx, cy, size);
            placed++;
        }
    }

    /**
     * 在 (cx, cy) 中心生成一个破损矩形圣殿废墟。
     * 外围边缘部分格放 hollyWall（破损：约 65% 放墙，35% 留空），
     * 四角更易破损（40% 放墙），内部填 hollyFloor，中心 3x3 为 altar。
     */
    public void placeHollyRuin(int cx, int cy, int size){
        int half = size / 2;
        for(int dx = -half; dx <= half; dx++){
            for(int dy = -half; dy <= half; dy++){
                int wx = cx + dx, wy = cy + dy;
                if(!Structs.inBounds(wx, wy, width, height)) continue;
                Tile tile = tiles.getn(wx, wy);

                boolean edge = (dx == -half || dx == half || dy == -half || dy == half);
                boolean corner = (Math.abs(dx) == half) && (Math.abs(dy) == half);
                boolean innerEdge = (Math.abs(dx) == half - 1 || Math.abs(dy) == half - 1)
                        && !(Math.abs(dx) == half || Math.abs(dy) == half);

                // 中心 3x3：altar（圣殿核心）
                if(Math.abs(dx) <= 1 && Math.abs(dy) <= 1){
                    tile.setFloor(envBlocks.altar.asFloor());
                    tile.setBlock(Blocks.air);
                    continue;
                }

                if(edge){
                    // 外围边缘：破损判定（四角 40% 放墙，普通边缘 65% 放墙）
                    float wallChance = corner ? 0.4f : 0.65f;
                    if(rand.chance(wallChance)){
                        tile.setBlock(envBlocks.hollyWall);
                        tile.setFloor(envBlocks.hollyFloor.asFloor());
                    }else{
                        // 破损缺口：不放墙，地板仍是 hollyFloor
                        tile.setFloor(envBlocks.hollyFloor.asFloor());
                        if(tile.block().solid) tile.setBlock(Blocks.air);
                    }
                }else if(innerEdge){
                    // 内圈：零星残墙（20%）
                    if(rand.chance(0.2f)){
                        tile.setBlock(envBlocks.hollyWall);
                    }else{
                        if(tile.block().solid) tile.setBlock(Blocks.air);
                    }
                    tile.setFloor(envBlocks.hollyFloor.asFloor());
                }else{
                    // 内部：hollyFloor + 清空
                    tile.setFloor(envBlocks.hollyFloor.asFloor());
                    if(tile.block().solid) tile.setBlock(Blocks.air);
                }
            }
        }

        // 在废墟内部随机放置 1~3 个 glossy（altar/内部/边缘缺口 都可以）
        int glossyCount = rand.random(1, 3);
        int tries = 0;
        while(glossyCount > 0 && tries < 50){
            tries++;
            int gdx = rand.random(-half + 1, half - 1);
            int gdy = rand.random(-half + 1, half - 1);
            int gx = cx + gdx, gy = cy + gdy;
            if(!Structs.inBounds(gx, gy, width, height)) continue;
            Tile gt = tiles.getn(gx, gy);
            // 避开 altar 中心 3x3 和 hollyWall 实心格
            if(Math.abs(gdx) <= 1 && Math.abs(gdy) <= 1) continue;
            if(gt.block().solid) continue;
            // 已经是 glossy 就跳过
            if(gt.floor() == envBlocks.glossy) continue;
            gt.setFloor((Floor)envBlocks.glossy);
            glossyCount--;
        }
    }

    // ========= 辅助方法 =========

    public boolean isOnLine(int x, int y, int s, int o) {
        int spacing = 102;
        int n1 = (spacing + s + o) % spacing;
        int n2 = (spacing + s - o) % spacing;
        return x % spacing == n1 || x % spacing == n2 || y % spacing == n1 || y % spacing == n2;
    }

    public void drawPoint(int cx, int cy, int rad, Cons<Tile> cons) {
        drawPoint(cx, cy, rad, tile -> true, cons);
    }

    public void drawPoint(int cx, int cy, int rad, Boolf<Tile> bool, Cons<Tile> cons) {
        for (int x = -rad; x <= rad; x++) {
            for (int y = -rad; y <= rad; y++) {
                int wx = cx + x, wy = cy + y;
                if (Structs.inBounds(wx, wy, width, height)) {
                    Tile tile = tiles.get(wx, wy);
                    if (bool.get(tile)) {
                        cons.get(tiles.getn(wx, wy));
                    }
                }
            }
        }
    }

    // ========= protected void generate()：主流程 =========

    @Override
    protected void generate() {
        distort(6, 12);
        median(3);

        // scatter：desert / yellowStone / brownStone 错落分布
        //scatter(envBlocks.desert, envBlocks.yellowStone, 0.38f);
        //scatter(envBlocks.yellowStone, envBlocks.brownStone, 0.4f);
        //scatter(envBlocks.brownStone, envBlocks.yellowStone, 0.3f);

        // 墙体：cells + 噪声按 Floor 生成对应 Wall
        cells(4);
        pass((x, y) -> {
            if (floor == envBlocks.desert && noise(x, y, 3, 0.4f, 13f, 1f) > 0.59f) {
                block = envBlocks.desertWall;
            }
            if (floor == envBlocks.yellowStone && noise(x, y, 3, 0.45f, 14f, 1f) > 0.62f) {
                block = envBlocks.yellowStoneWall;
            }
            if (floor == envBlocks.brownStone && noise(x, y, 3, 0.48f, 12f, 1f) > 0.55f) {
                block = envBlocks.brownStoneWall;
            }
            if (floor == envBlocks.rubberFloor && noise(x, y, 3, 0.48f, 12f, 1f) > 0.2f) {
                block = envBlocks.rubberTree;
            }
        });

        decoration(0.025f);
        distort(4, 4);

        // 出生点 + 核心位置
        float length = width / 4f;
        Vec2 trns = Tmp.v1.trns(rand.random(360f), length);
        int
                spawnX = (int) (trns.x + width / 2f), spawnY = (int) (trns.y + height / 2f),
                endX = (int) (-trns.x + width / 2f), endY = (int) (-trns.y + height / 2f);
        float maxd = Mathf.dst(width / 2f, height / 2f);

        erase(spawnX, spawnY, 35);
        brush(pathfind(spawnX, spawnY, endX, endY, tile -> (tile.solid() ? 300f : 0f) + maxd - tile.dst(width / 2f, height / 2f) / 10f, Astar.manhattan), 9);
        erase(endX, endY, 21);

        // blend / distort / median 微调
        median(2, 0.6, envBlocks.desert);
        blend(envBlocks.yellowStone, envBlocks.brownStone, 4);
        blend(envBlocks.desert, envBlocks.yellowStone, 4);
        distort(10f, 12f);
        distort(5f, 7f);
        median(2, 0.6, envBlocks.brownStone);
        median(3, 0.6, envBlocks.rubberFloor);

        pass((x, y) -> {
            if (noise(x, y + 600 + x, 5, 0.86f, 60f, 1f) < 0.41f && floor == envBlocks.brownStone) {
                floor = envBlocks.yellowStone;
            }
            if (floor == envBlocks.brownStone && Mathf.within(x, y, spawnX, spawnY, 30f + noise(x, y, 2, 0.8f, 9f, 15f))) {
                floor = envBlocks.rubberFloor;
            }
            // spawn 周围保持原样（holly 废墟由 generateHollyRuins 随机放置，不围绕出生点）
            if ((floor == envBlocks.desert || floor == envBlocks.yellowStone) && block.isStatic()) {
                block = envBlocks.desertWall;
            }
            float max = 0;
            for (Point2 p : Geometry.d8) {
                max = Math.max(max, world.getDarkness(x + p.x, y + p.y));
            }
            if (max > 0) {
                block = floor.asFloor().wall;
                if (block == Blocks.air) block = envBlocks.desertWall;
            }
            if (floor == envBlocks.yellowStone && noise(x + 78 + y, y, 3, 0.8f, 6f, 1f) > 0.44f) {
                floor = envBlocks.desert;
            }
            if (floor == envBlocks.brownStone && noise(x + 78 - y, y, 4, 0.73f, 19f, 1f) > 0.63f) {
                floor = envBlocks.desert;
            }
        });

        inverseFloodFill(tiles.getn(spawnX, spawnY));
        blend(envBlocks.brownStone, envBlocks.rubberFloor, 4);

        erase(endX, endY, 6);
        tiles.getn(endX, endY).setOverlay(Blocks.spawn);

        // ===== Holly 圣殿废墟：随机找空旷处生成破损矩形 =====
        generateHollyRuins(10, 14, 22);

        // ===== 矿物生成（占比：bigIron > coal > sulFurFrag > frailPolyester > pumice）=====
        // thresh 越小 → 生成越多；i 为噪声偏移种子，确保各矿不重叠

        // 1. bigIron 铁矿（最多）：覆盖 desert / yellowStone / brownStone 三种地表 + 对应墙
        clusterOre(envBlocks.bigIronOre, envBlocks.desert,      6f,  0.78f);
        clusterOre(envBlocks.bigIronOre, envBlocks.yellowStone, 7f,  0.80f);
        clusterOre(envBlocks.bigIronOre, envBlocks.brownStone,  8f,  0.80f);
        clusterWallOre(envBlocks.desertWall,       envBlocks.bigIronWores, 9f,  0.80f);
        clusterWallOre(envBlocks.yellowStoneWall,  envBlocks.bigIronWores, 10f, 0.82f);
        clusterWallOre(envBlocks.brownStoneWall,   envBlocks.bigIronWores, 11f, 0.84f);

        // 2. Coal 煤矿（次多）：覆盖 desert / yellowStone / brownStone
        clusterOre(envBlocks.coalHill, envBlocks.desert,      4.5f, 0.85f);
        clusterOre(envBlocks.coalHill, envBlocks.yellowStone, 5.5f, 0.87f);
        clusterOre(envBlocks.coalHill, envBlocks.brownStone,  6.5f, 0.87f);
        clusterWallOre(envBlocks.desertWall,       envBlocks.coalHillOre, 7f,  0.87f);
        clusterWallOre(envBlocks.yellowStoneWall,  envBlocks.coalHillOre, 8f,  0.89f);
        clusterWallOre(envBlocks.brownStoneWall,   envBlocks.coalHillOre, 12f, 0.90f);

        // 3. sulFurFrag 硫矿（中等）：只在 yellowStone / brownStone 生成
        clusterOre(envBlocks.sulfurFragOre, envBlocks.yellowStone, 13f, 0.92f);
        clusterOre(envBlocks.sulfurFragOre, envBlocks.brownStone,  14f, 0.92f);
        clusterWallOre(envBlocks.yellowStoneWall,  envBlocks.sulfurFragWores, 15f, 0.93f);
        clusterWallOre(envBlocks.brownStoneWall,   envBlocks.sulfurFragWores, 16f, 0.93f);

        // 4. frailPolyester 脆聚酯矿（较少）：只在 brownStone 生成
        clusterOre(envBlocks.frailPolyesterOre, envBlocks.brownStone, 17f, 0.95f);
        clusterWallOre(envBlocks.brownStoneWall, envBlocks.frailPolyesterWores, 18f, 0.95f);

        // 5. pumice 浮岩矿（最少）：稀疏分布
        clusterOre(envBlocks.pumiceMegaOre, envBlocks.brownStone, 19f, 0.98f);
        clusterWallOre(envBlocks.brownStoneWall, envBlocks.pumiceMegaWores, 20f, 0.98f);

        // 水晶矿：uranCrystalWall 作为 block（非 ore，避免 ClassCast）
        //pass((x, y) -> {
        //    if (!nearWall(x, y)) return;
        //    if (noise(x + 999, y + 600 - x, 4, 0.63f, 45f, 1f) < 0.27f && floor == envBlocks.crystalCune) {
        //        block = envBlocks.uranCrystalWall;
        //    }
        //    if (block == Blocks.air && floor == envBlocks.crystalCune && rand.chance(0.09) && nearWall(x, y)
        //            && !near(x, y, 4, envBlocks.uranCrystalWall)) {
        //        block = envBlocks.uranCrystalWall;
        //    }
        //    if (block == envBlocks.yellowStoneWall && rand.chance(0.23) && nearAir(x, y) && !near(x, y, 3, envBlocks.uranCrystalWall)) {
        //        block = envBlocks.uranCrystalWall;
        //    }
        //    if (block == envBlocks.desertWall && rand.chance(0.3) && nearAir(x, y) && !near(x, y, 3, envBlocks.uranCrystalWall)) {
        //        block = envBlocks.uranCrystalWall;
        //    }
        //});

        // 清理矿附近的树
        pass((x, y) -> {
            if ((ore instanceof Floor && ore.asFloor().wallOre) || block.itemDrop != null || (block == Blocks.air && ore != Blocks.air)) {
                removeWall(x, y, 3, b -> b == envBlocks.hollyTree || b == envBlocks.rubberTree);
            }
        });

        trimDark();

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
}
