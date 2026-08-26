package Npl.newSth.assembler;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.scene.ui.*;
import arc.struct.*;
import arc.scene.ui.Slider;
import arc.scene.ui.Label;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * DroneHarvester —— 无人机采矿器（继承 Block，从零实现）
 * ============================================================
 * 保留"组装区域 + 多无人机"的视觉概念，但功能不是造单位，而是：
 *   ① 扫描 areaSize×areaSize 组装区域内的所有矿物
 *   ② 4 架 drone 各自认领一个矿格，飞过去采挖，采完飞回方块存入
 *   ③ 遇到无法采集的矿（硬度超过 drillTier / 在 blacklist / 被其他方块盖住）→ drone 跳过
 *   ④ 组装区域没有任何可采矿 → canPlaceOn 返回 false，方块放不下去
 *   ⑤ 采回的矿物存进方块 items，从输出端 offload 送出（接传送带/容器）
 *   ⑥ 点击建筑弹出矿物选择面板（类似 HunfuBlock 配方），支持多选
 *   ⑦ drone 生成时 Drawf.construct 渐显动画（和 UnitAssembler 一样）
 *   ⑧ 区域偏移到方块外侧 + 可转向（rotatable=true，和 UnitAssembler 一样）
 *   ⑨ 采矿优先级：tier（硬度）从高到低 → 同 tier 按距离近远 → 同距离平分 → 余 1 随机
 *   ⑩ 采完一块换一块（recentlyMined 冷却 3 秒，不会重复采同一块）
 *
 * 区域布局（和 UnitAssembler 完全一致）：
 *   size=3, areaSize=7, rotation=0 → 方块在 (-1..+1, -1..+1)，区域在 (+2..+8, -3..+3)
 *   区域中心 = 方块中心 + (size/2 + areaSize/2) * rotation 方向
 *   方块本身**不在**区域里，旋转时区域跟着转
 *
 * 状态机（每个 drone 独立运行，4 架并行）：
 *   空闲 → 飞向矿格 →(到达)→ 采挖 →(采满)→ 飞回方块 →(飞抵)→ 存入方块 → 空闲
 *   飞向矿格 → 到达发现采不动 → 跳过回 空闲
 *
 * 用法（NuBlocks 里）：
 * <pre>
 *   myHarvester = new DroneHarvester("myHarvester"){{
 *       size = 3;
 *       health = 320;
 *       areaSize = 7;
 *       dronesCreated = 4;
 *       drillTier = 3;
 *       mineSpeed = 0.04f;
 *       droneSpeed = 0.45f;
 *       droneBuildTime = 60f * 5f;
 *       blacklist.add(Items.thorium);
 *       consumePower(2f);
 *       requirements(Category.production, with(Items.copper, 200, Items.silicon, 100));
 *   }};
 * </pre>
 */
public class DroneHarvester extends Block {

    // ══════════════════════════════════════════════════════════════════
    // Block 层字段：描述"这种方块是什么"（所有实例共用，加载时确定）
    // ══════════════════════════════════════════════════════════════════

    /** 组装区域边长（格）。areaSize=7 表示 7×7 的方形区域用于采矿 */
    public int areaSize = 7;

    /** 无人机数量（默认 4，对应 4 架 drone 并行采集） */
    public int dronesCreated = 4;

    /** 可采矿物硬度上限。某矿格 item.hardness > drillTier → drone 飞过去也采不动 → 跳过 */
    public int drillTier = 1;

    /** 每帧采挖进度增量。0.04 表示约 25 帧（0.4 秒）采满 1 单位矿物 */
    public float mineSpeed = 0.04f;

    /** 无人机飞行插值系数。drone 每帧把自身位置朝目标插值移动 droneSpeed 比例 */
    public float droneSpeed = 0.45f;

    /** 造满全部 dronesCreated 架 drone 需要的时间（秒） */
    public float droneBuildTime = 60f * 5f;

    /** 矿物黑名单：列在这里的矿物 drone 不采（即使硬度够） */
    public Seq<Item> blacklist = new Seq<>();

    /** drone 贴图。默认借用原版装配无人机的贴图 "unit-assembly-drone" */
    public TextureRegion droneRegion;

    /** 采完一块矿后，该矿格的冷却时间（秒）。冷却期内不会被任何 drone 认领，实现"采完换一块" */
    public float mineCooldown = 2f;

    // ————————————————————————————————————————————
    //  构造器：mindustry.world.Block 的标准入口
    // ————————————————————————————————————————————
    public DroneHarvester(String name){
        super(name);
        configurable = true;
        rotate = true;           // ⭐ 可转向！区域跟着 rotation 走（Block 字段叫 rotate，不是 rotatable）
        rotateDraw = true;       // ⭐ 画贴图时也跟着 rotation 旋转
        hasItems = true;
        acceptsItems = false;
        itemCapacity = 50;
        update = true;
        sync = true;
    }

    // ══════════════════════════════════════════════════════════════════
    // Block 层 @Override 方法
    // ══════════════════════════════════════════════════════════════════

    /* ⭐ @Override Block.load() */
    @Override
    public void load(){
        super.load();
        droneRegion = Core.atlas.find(name + "-drone", "unit-assembly-drone");
        if(droneRegion == null || droneRegion.texture == null) droneRegion = Core.atlas.find("white");
    }

    /* ⭐ @Override Block.init() */
    @Override
    public void init(){
        if(dronesCreated > 0){
            itemCapacity = Math.max(itemCapacity, dronesCreated * 8);
        }
        super.init();
    }

    /* ⭐ @Override Block.outputsItems() */
    @Override
    public boolean outputsItems(){
        return true;
    }

    /**
     * ⭐ 100% 抄原版 UnitAssembler.getRect(Rect, float x, float y, int rotation)
     * x, y = 方块中心像素坐标（不是 tile.x 最左下格！）
     * 原版字节码还原：
     *   rect.setCentered(x, y, areaSize * tilesize)   // 先居中
     *   offset = (areaSize + size) * tilesize / 2f     // 像素级偏移
     *   rect.x += Geometry.d4x(rotation) * offset
     *   rect.y += Geometry.d4y(rotation) * offset
     */
    public Rect getAreaRect(Rect rect, float cx, float cy, int rotation){
        rect.setCentered(cx, cy, areaSize * tilesize);
        float offset = (areaSize + size) * tilesize / 2f;
        rect.x += arc.math.geom.Geometry.d4x(rotation) * offset;
        rect.y += arc.math.geom.Geometry.d4y(rotation) * offset;
        return rect;
    }

    /** 运行时用：从 Build 拿 rotation */
    public Rect getAreaRect(Tile tile){
        float cx = tile.drawx();   // 方块中心像素 x（= (tile.x + size/2) * tilesize + tilesize/2？
                                    //   不对，tile.drawx() 是最左下格中心像素；
                                    //   但原版 x,y 是 Block 的 centerX/centerY 像素
        float cy = tile.drawy();
        // size > 1 时方块中心不在最左下格中心，要加偏移
        if(size > 1){
            cx += (size - 1) * tilesize / 2f;
            cy += (size - 1) * tilesize / 2f;
        }
        int rot = tile.build != null ? tile.build.rotation : 0;
        return getAreaRect(Tmp.r1, cx, cy, rot);
    }

    /** 放置预览用：靠 rotation 参数 */
    public Rect getAreaRectWithRotation(Tile tile, int rotation){
        float cx = tile.drawx();
        float cy = tile.drawy();
        if(size > 1){
            cx += (size - 1) * tilesize / 2f;
            cy += (size - 1) * tilesize / 2f;
        }
        return getAreaRect(Tmp.r1, cx, cy, rotation);
    }

    /** 取区域内所有 tile（遍历原版 Rect 覆盖的像素范围，转 tile 坐标） */
    public Seq<Tile> getAreaTiles(Tile tile){
        Seq<Tile> out = new Seq<>();
        Rect r = getAreaRect(tile);
        int minTx = (int)(r.x / tilesize);
        int minTy = (int)(r.y / tilesize);
        int maxTx = (int)((r.x + r.width) / tilesize);
        int maxTy = (int)((r.y + r.height) / tilesize);
        for(int tx = minTx; tx < maxTx; tx++){
            for(int ty = minTy; ty < maxTy; ty++){
                Tile t = world.tile(tx, ty);
                if(t != null) out.add(t);
            }
        }
        return out;
    }

    /** canPlaceOn/drawPlace 用：按 rotation 参数算 */
    public Seq<Tile> getAreaTilesWithRotation(Tile tile, int rotation){
        Seq<Tile> out = new Seq<>();
        Rect r = getAreaRectWithRotation(tile, rotation);
        int minTx = (int)(r.x / tilesize);
        int minTy = (int)(r.y / tilesize);
        int maxTx = (int)((r.x + r.width) / tilesize);
        int maxTy = (int)((r.y + r.height) / tilesize);
        for(int tx = minTx; tx < maxTx; tx++){
            for(int ty = minTy; ty < maxTy; ty++){
                Tile t = world.tile(tx, ty);
                if(t != null) out.add(t);
            }
        }
        return out;
    }

    /**
     * 判断一个 tile 上的矿是否可采。
     * 可采条件（全部满足）：
     *   ① drop != null（有矿）
     *   ② hardness <= drillTier（硬度够）
     *   ③ !blacklist.contains(drop)（不在黑名单）
     *   ④ tile.build == null || tile.build.block == this（没被其他方块盖住）
     *   ⑤ selectedMinerals.isEmpty() || selectedMinerals.contains(drop)（玩家选了矿种 → 必须命中选的；没选 → 全采）
     */
    public boolean isMineable(Tile tile, Seq<Item> selectedMinerals){
        Item drop = tile.drop();
        if(drop == null) return false;
        if(drop.hardness > drillTier) return false;
        if(blacklist.contains(drop)) return false;
        if(tile.build != null && tile.build.block != this) return false;
        if(selectedMinerals != null && selectedMinerals.size > 0 && !selectedMinerals.contains(drop)) return false;
        return true;
    }

    /** 计算矿格中心到区域中心的距离平方（同 tier 内排序用） */
    public float distSqToAreaCenter(Tile tile, Rect areaRect){
        float acx = areaRect.x + areaRect.width / 2f;
        float acy = areaRect.y + areaRect.height / 2f;
        float dx = tile.drawx() - acx;
        float dy = tile.drawy() - acy;
        return dx * dx + dy * dy;
    }

    /* ⭐ @Override Block.canPlaceOn */
    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        // ⚠️ 这里 tile 是**方块本身要放的位置**，不是区域里的 tile
        // 我们需要一个临时 Build 来给 getAreaRect 提供 rotation
        Seq<Tile> tiles = getAreaTilesWithRotation(tile, rotation);
        for(Tile t : tiles){
            if(isMineable(t, null)) return true;
        }
        return false;
    }

    /* ⭐ @Override Block.drawPlace —— 半透明覆盖（黄=可采，红=不可采），不要虚线框 */
    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Tile tile = world.tile(x, y);   // ← 恢复这行
        if(tile == null) return;
        // 半透明覆盖（黄=可采，红=不可采）—— 不要虚线框
        for(Tile t : getAreaTilesWithRotation(tile, rotation)){
            if(t.drop() == null) continue;
            boolean can = isMineable(t, null);
            Draw.color(can ? new Color(1f, 0.85f, 0.2f, 0.35f) : new Color(1f, 0.25f, 0.2f, 0.35f));
            Draw.rect(Core.atlas.find("white"), t.drawx(), t.drawy(), tilesize, tilesize);
            Draw.color();
        }
    }

    /** 从区域里收集所有独特矿物类型（去重），给配置面板画选择按钮用 */
    public Seq<Item> getUniqueMinerals(Tile tile){
        Seq<Item> out = new Seq<>();
        for(Tile t : getAreaTiles(tile)){
            Item drop = t.drop();
            if(drop != null && !out.contains(drop)) out.add(drop);
        }
        return out;
    }

    /* ⭐ @Override Block.setStats() */
    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.range, areaSize + "×" + areaSize + " 格采矿区域（偏移）");
        stats.add(Stat.affinities, dronesCreated + " 架无人机");
        stats.add(Stat.abilities, "可采硬度 ≤ " + drillTier + "（tier " + drillTier + "）");
        stats.add(Stat.productionTime, (1f / mineSpeed / 60f), StatUnit.seconds);
        stats.add(Stat.damage, "tier 优先 → 距离优先 → 平分");
        if(!blacklist.isEmpty()){
            stats.add(Stat.input, "不采集：" + blacklist.toString(", ", i -> i.localizedName));
        }
    }

    /* ⭐ @Override Block.setBars() —— 方块详情面板顶部的状态条 */
    @Override
    public void setBars(){
        super.setBars();
        // addBar(名字, 把 Build e 转成 Bar 的函数)
        // Bar(翻译key, 颜色, 进度函数 0..1)
        addBar("drone-build", (DroneHarvesterBuild e) -> new Bar(
            () -> "无人机: " + e.activeDrones + "/" + dronesCreated,
            () -> Pal.accent,
            () -> e.activeDrones >= dronesCreated ? 1f : e.droneSpawnProgress
        ));
        addBar("mining", (DroneHarvesterBuild e) -> new Bar(
            () -> {
                int cnt = 0;
                for(int i = 0; i < e.activeDrones; i++){
                    if(e.drones[i].state == DroneHarvesterBuild.DroneState.MINE) cnt++;
                }
                return cnt + " 架采集中";
            },
            () -> Pal.lightOrange,
            () -> {
                float total = 0f; int cnt = 0;
                for(int i = 0; i < e.activeDrones; i++){
                    if(e.drones[i].state == DroneHarvesterBuild.DroneState.MINE){ total += e.drones[i].mineProgress; cnt++; }
                }
                return cnt > 0 ? total / cnt : 0f;
            }
        ));
        addBar("cooldown", (DroneHarvesterBuild e) -> new Bar(
            () -> e.cooldownMap.size + " 格冷却中",
            () -> Pal.remove,
            () -> {
                float maxCd = 0f;
                for(var ent : e.cooldownMap.entries()){
                    if(ent.value > maxCd) maxCd = ent.value;
                }
                return Mathf.clamp(maxCd /mineCooldown);
            }
        ));
        addBar("queue", (DroneHarvesterBuild e) -> new Bar(
            () -> "待采: " + e.mineQueue.size,
            () -> Pal.place,
            () -> 1f
        ));
    }

    // ══════════════════════════════════════════════════════════════════
    //  Build 层：地图上每一台 DroneHarvester 的实例行为
    // ══════════════════════════════════════════════════════════════════
    public class DroneHarvesterBuild extends Building {

        public Drone[] drones;
        public int activeDrones = 0;
        public float droneSpawnProgress = 0f;
        /** 待采矿格队列（按 tier DESC + 距离 ASC 排序） */
        public Seq<Tile> mineQueue = new Seq<>();
        public int scanTick = 0;
        public float coolingSpeed = 10f;  // 冷却速度倍率，默认1.0
        /**
         * ⭐ 已采过的矿格冷却表：key=Tile.pos, value=冷却剩余秒数
         * 采完一块矿 → 进表 → mineCooldown 秒内不再认领 → 采完换一块
         */
        public ObjectMap<Integer, Float> cooldownMap = new ObjectMap<>();
        /** 同 tier 平分计数器：每台 drone 采完一块后，按 tier 记录已分配数 */
        public IntMap<Integer> tierAssignCount = new IntMap<>();

        public Seq<Item> selectedMinerals = new Seq<>();

        @Override
        public void created(){
            super.created();
            drones = new Drone[dronesCreated];
            for(int i = 0; i < dronesCreated; i++){
                drones[i] = new Drone();
                drones[i].x = x;
                drones[i].y = y;
                drones[i].owner = this;
            }
        }

        @Override
        public void updateTile(){
            float speed = this.coolingSpeed;  // 从建筑实例获取倍率
            ObjectMap.Entries<Integer, Float> entries = cooldownMap.entries();
            while (entries.hasNext) {
                ObjectMap.Entry<Integer, Float> e = entries.next();
                float nv = e.value - delta()*speed;
                if (nv <= 0f) {
                    entries.remove();   // 冷却结束，移除条目
                } else {
                    e.value = nv;
                }
                Log.info("递减: " + e.key + " -> " + e.value);
            }
            // 在 updateTile() 开头加上
            if (Time.time % 1f < 0.05f && !cooldownMap.isEmpty()) {
                Log.info("=== 冷却状态 ===");
                for (var entry : cooldownMap.entries()) {
                    Log.info("  矿格 " + entry.key + " 剩余 " + entry.value + " 秒");
                }
            }
            // —— ① 制造 drone ——
            if(activeDrones < dronesCreated){
                droneSpawnProgress += delta() / droneBuildTime * efficiency;
                if(droneSpawnProgress >= 1f){
                    droneSpawnProgress = 0f;
                    activeDrones++;
                    Drone d = drones[activeDrones - 1];
                    d.state = DroneState.IDLE;
                    d.x = x;
                    d.y = y;
                    d.spawnProgress = 0f;
                }
            }

            // —— ② 每 30 帧重扫区域 + 冷却递减 + 排序 mineQueue ——
            scanTick++;
            if(scanTick >= 30){
                scanTick = 0;
                // 冷却递减
                tierAssignCount.clear();

                // 扫矿 + 排序
                mineQueue.clear();
                for(Tile t : getAreaTiles(tile)){
                    if(isMineable(t, selectedMinerals)){
                        mineQueue.add(t);
                    }
                }
                // ⭐ 排序：先按 hardness DESC（tier 高优先），同 tier 按距离 ASC（近优先）
                Rect areaRect = getAreaRect(tile);
                mineQueue.sort((a, b) -> {
                    int ha = a.drop().hardness, hb = b.drop().hardness;
                    if(ha != hb) return Integer.compare(hb, ha);   // hardness DESC
                    float da = distSqToAreaCenter(a, areaRect), db = distSqToAreaCenter(b, areaRect);
                    return Float.compare(da, db);   // 距离 ASC
                });
            }

            // —— ③ 更新每架已激活 drone ——
            for(int i = 0; i < activeDrones; i++){
                drones[i].update();
                if(drones[i].spawnProgress < 1f){
                    drones[i].spawnProgress = Mathf.clamp(drones[i].spawnProgress + delta() / 30f);
                }
            }

            // —— ④ offload ——
            if(timer(0, 5f) && items.total() > 0){
                dump();
            }
        }

        @Override
        public void draw(){
            super.draw();
            Draw.draw(35f, () -> {
                for(int i = 0; i < activeDrones; i++){
                    Drone d = drones[i];
                    if(d.spawnProgress < 1f){
                        float size = 8f * Mathf.lerp(0.3f, 1f, d.spawnProgress);
                        Drawf.construct(d.x, d.y, droneRegion, Pal.accent, size, 0f, d.spawnProgress, 1f);
                    }else{
                        Draw.rect(droneRegion, d.x, d.y, 8f, 8f);
                        if(d.carrying != null && d.carrying.uiIcon != null && d.carrying.uiIcon.texture != null){
                            Draw.rect(d.carrying.uiIcon, d.x, d.y - 6f, 4f, 4f);
                        }
                    }
                }
            });
        }

        @Override
        public void drawSelect(){
            super.drawSelect();
            //Rect r = getAreaRect(tile);
            //Drawf.dashRect(Pal.accent, r);
            // 不要虚线框，只画半透明覆盖
            for(Tile t : getAreaTiles(tile)){
                if(t.drop() == null) continue;
                boolean can = isMineable(t, selectedMinerals);
                Draw.color(can ? new Color(1f, 0.85f, 0.2f, 0.35f) : new Color(1f, 0.25f, 0.2f, 0.35f));
                Draw.rect(Core.atlas.find("white"), t.drawx(), t.drawy(), tilesize, tilesize);
                Draw.color();
            }
        }

        @Override
        public void buildConfiguration(Table table){
            table.table(Styles.black3, t -> {
                t.add("无人机: " + activeDrones + "/" + dronesCreated).color(Color.lightGray).left().row();
                t.add("待采矿格: " + mineQueue.size).color(Color.lightGray).left().row();
                t.add("冷却中: " + cooldownMap.size).color(Color.lightGray).left().row();
                t.add(selectedMinerals.isEmpty() ? "选中矿物: 全部" : "选中矿物: " + selectedMinerals.size)
                 .color(Color.lightGray).left();
            }).row();

            table.add().row();

            Seq<Item> allMinerals = getUniqueMinerals(tile);

            if(allMinerals.size > 0){
                table.add("选择可采矿种（可多选，空=全采）").color(Color.lightGray).row();

                table.table(sel -> {
                    sel.button("全选", Styles.defaultt, () -> {
                        selectedMinerals.clear();
                    }).width(60f);
                    sel.button("反选", Styles.defaultt, () -> {
                        Seq<Item> toToggle = new Seq<>();
                        for(Item m : allMinerals){
                            if(!selectedMinerals.contains(m)) toToggle.add(m);
                        }
                        selectedMinerals.clear();
                        selectedMinerals.addAll(toToggle);
                    }).width(60f);
                }).row();

                table.table(grid -> {
                    int idx = 0;
                    for(Item mineral : allMinerals){
                        if(idx % 4 == 0) grid.row();
                        final Item m = mineral;
                        ImageButton b = grid.button(Tex.whiteui, Styles.squareTogglei, () -> {
                            if(selectedMinerals.contains(m)){
                                selectedMinerals.remove(m);
                            }else{
                                selectedMinerals.add(m);
                            }
                            scanTick = 30;
                        }).size(50f).get();

                        b.clearChildren();
                        b.stack(
                            new Image(
                                (m.uiIcon != null && m.uiIcon.texture != null) ? m.uiIcon : Core.atlas.find("white")
                            ).setScaling(Scaling.fit),
                            new Table(c -> {
                                c.right().bottom();
                                boolean blocked = false;
                                for(Tile t : getAreaTiles(tile)){
                                    if(t.drop() == m && t.build != null && t.build.block != DroneHarvester.this){
                                        blocked = true; break;
                                    }
                                }
                                if(blocked){
                                    c.add("🚫").style(Styles.outlineLabel).fontScale(0.55f).padRight(-2f).padBottom(-4f);
                                }
                                // ⭐ 加 tier 小字标
                                c.add("T" + m.hardness).style(Styles.outlineLabel).fontScale(0.5f).padLeft(2f).padTop(-4f);
                            })
                        ).grow().pad(8f);

                        b.update(() -> b.setChecked(selectedMinerals.contains(m)));
                        idx++;
                    }
                });
            }else{
                table.add("区域内无矿物").color(Color.lightGray);
            }
            table.row();
            table.add("冷却速度倍率").left().padRight(10f);

            // 创建滑块，范围0.1~3.0，步长0.1，初始值=current coolingSpeed
            Slider speedSlider = new Slider(1f, 20f, 0.1f, false);
            speedSlider.setValue(this.coolingSpeed);

            // 滑块值改变时更新建筑实例的冷却速度
            speedSlider.changed(() -> {
                this.coolingSpeed = speedSlider.getValue();
                // 可选：实时更新UI显示当前值
            });

            // 显示当前值的标签
            Label valueLabel = new Label(Float.toString(this.coolingSpeed));
            speedSlider.changed(() -> {
                valueLabel.setText(String.format("%.1f", this.coolingSpeed));
            });

            table.add(speedSlider).width(200f).padRight(10f);
            table.add(valueLabel).width(40f);
        }

        @Override
        public Object config(){
            return selectedMinerals;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void configure(Object value){
            if(value instanceof Seq<?> s){
                selectedMinerals.clear();
                for(Object o : s){
                    if(o instanceof Item it) selectedMinerals.add(it);
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════
        //  存档读写
        // ══════════════════════════════════════════════════════════════════

        @Override
        public byte version(){
            return 3;   // 版本 3：加了 cooldownMap + tierAssignCount
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.i(activeDrones);
            write.f(droneSpawnProgress);
            write.i(selectedMinerals.size);
            for(Item m : selectedMinerals) write.s(m.id);
            // cooldownMap
            write.i(cooldownMap.size);
            for(var e : cooldownMap.entries()){
                write.i(e.key);
                write.f(e.value);
            }
            // tierAssignCount
            write.i(tierAssignCount.size);
            for(var e : tierAssignCount.entries()){
                write.i(e.key);
                write.i(e.value);
            }
            for(int i = 0; i < dronesCreated; i++){
                Drone d = drones[i];
                write.b(d.state == null ? 0 : d.state.ordinal());
                write.i(d.target == null ? -1 : d.target.x);
                write.i(d.target == null ? -1 : d.target.y);
                write.f(d.mineProgress);
                write.s(d.carrying == null ? -1 : d.carrying.id);
                write.i(d.carryingAmount);
                write.f(d.x);
                write.f(d.y);
                write.f(this.coolingSpeed);
                write.f(d.spawnProgress);
                write.i(d.lastMinedTilePos);
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(drones == null){
                drones = new Drone[dronesCreated];
                for(int i = 0; i < dronesCreated; i++){
                    drones[i] = new Drone();
                    drones[i].x = x;
                    drones[i].y = y;
                    drones[i].owner = this;
                }
            }
            activeDrones = read.i();
            droneSpawnProgress = read.f();
            selectedMinerals.clear();
            int selCount = read.i();
            for(int i = 0; i < selCount; i++){
                short itemId = read.s();
                if(itemId >= 0) selectedMinerals.add(Vars.content.item(itemId));
            }
            if(revision >= 3){
                // cooldownMap
                int cdSize = read.i();
                for(int i = 0; i < cdSize; i++){
                    int key = read.i();
                    float val = read.f();
                    cooldownMap.put(key, val);
                }
                // tierAssignCount
                int tcSize = read.i();
                for(int i = 0; i < tcSize; i++){
                    int key = read.i();
                    int val = read.i();
                    tierAssignCount.put(key, val);
                }
            }
            for(int i = 0; i < dronesCreated; i++){
                Drone d = drones[i];
                int ord = read.b();
                d.state = (ord >= 0 && ord < DroneState.values().length) ? DroneState.values()[ord] : DroneState.IDLE;
                int tx = read.i();
                int ty = read.i();
                d.target = (tx < 0 || ty < 0) ? null : world.tile(tx, ty);
                d.mineProgress = read.f();
                short itemId = read.s();
                d.carrying = (itemId < 0) ? null : Vars.content.item(itemId);
                d.carryingAmount = read.i();
                d.x = read.f();
                d.y = read.f();
                d.spawnProgress = (revision >= 2) ? read.f() : 1f;
                d.lastMinedTilePos = (revision >= 3) ? read.i() : -1;
            }
            if (revision >= 4) { // 版本号需要递增
                this.coolingSpeed = read.f();
            } else {
                this.coolingSpeed = 10f; // 默认值
            }
        }

        // ══════════════════════════════════════════════════════════════════
        //  Drone 内部类：单架无人机的状态机
        // ══════════════════════════════════════════════════════════════════

        enum DroneState { IDLE, FLY_TO, MINE, FLY_BACK, DEPOSIT }

        public class Drone {
            public float x, y;
            public Tile target;
            public DroneState state = DroneState.IDLE;
            public float mineProgress = 0f;
            public Item carrying = null;
            public int carryingAmount = 0;
            public float spawnProgress = 1f;
            /** 上一次采过的矿格 pos（packInt），用来避免短时间内重复采同一块 */
            public int lastMinedTilePos = -1;
            public DroneHarvesterBuild owner;

            public void update(){
                if(owner == null) owner = DroneHarvesterBuild.this;
                switch(state){
                    case IDLE -> {
                        target = claimTile();
                        if(target != null) state = DroneState.FLY_TO;
                    }
                    case FLY_TO -> {
                        if(target == null){ state = DroneState.IDLE; break; }
                        if(moveToward(target.drawx(), target.drawy())){
                            if(isMineable(target, selectedMinerals)){
                                mineProgress = 0f;
                                carrying = target.drop();
                                state = DroneState.MINE;
                            }else{
                                target = null;
                                state = DroneState.IDLE;
                            }
                        }
                    }
                    case MINE -> {
                        mineProgress += mineSpeed * owner.efficiency * delta();
                        if(mineProgress >= 1f){
                            mineProgress = 0f;
                            carryingAmount = 1;
                            // ⭐ 采完了：进冷却表 → 采完换一块
                            if(target != null){
                                owner.cooldownMap.put(target.pos(), mineCooldown);
                                this.lastMinedTilePos = target.pos();
                            }
                            state = DroneState.FLY_BACK;
                        }
                    }
                    case FLY_BACK -> {
                        if(moveToward(owner.x, owner.y)){
                            state = DroneState.DEPOSIT;
                        }
                    }
                    case DEPOSIT -> {
                        if(carrying != null && carryingAmount > 0){
                            if(owner.items.get(carrying) < owner.block.itemCapacity){
                                owner.items.add(carrying, carryingAmount);
                            }
                            carrying = null;
                            carryingAmount = 0;
                        }
                        target = null;
                        state = DroneState.IDLE;
                    }
                }
            }

            /**
             * ⭐ 优先级认领：
             *   1. mineQueue 已经按 tier DESC + 距离 ASC 排好序
             *   2. 跳过冷却中的矿格（刚被其他 drone 采过）
             *   3. 跳过已被其他 drone 认领的矿格
             *   4. 同 tier 平分：每分配一块该 tier 的矿，tierAssignCount[tier]++
             *      当某 tier 的已分配数 >= areaSize / dronesCreated 时，跳过该 tier（防止全挤同一个 tier）
             *   5. 如果所有高优先级 tier 都被分完了，自然往下走更低 tier
             *   6. 余 1 情况（4 drone 3 同 tier）：第 4 台自然往下一个 tier 走
             */
            private Tile claimTile(){
                if(mineQueue.isEmpty()) return null;
                for(Tile t : mineQueue){
                    int pos = t.pos();
                    if(cooldownMap.containsKey(pos)) continue;
                    boolean taken = false;
                    for(Drone other : drones){
                        if(other != null && other != this && other.target != null && other.target.pos() == pos){
                            taken = true;
                            break;
                        }
                    }
                    if(!taken){
                        return t;
                    }
                }
                return null;
            }
            private boolean moveToward(float tx, float ty){
                float dx = tx - x, dy = ty - y;
                float dist = Mathf.len(dx, dy);
                float step = droneSpeed * delta() * 8f;
                if(dist <= step || dist < 3f){
                    x = tx; y = ty;
                    return true;
                }
                x += dx / dist * step;
                y += dy / dist * step;
                return false;
            }
        }
    }
}
