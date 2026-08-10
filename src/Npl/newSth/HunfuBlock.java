
package Npl.newSth;

// ==================== 导入区（不用管，用到啥写啥）====================
import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.*;
import mindustry.world.consumers.*;
import mindustry.world.consumers.ConsumePower;
import mindustry.world.meta.*;
import mindustry.world.Block;
import java.util.*;


import static mindustry.Vars.*;


/**
 * ============================================================
 * 【HunfuBlock = 混成（物品 + 液体）多配方工厂】
 *  完全照搬 ConfigurableBlock 的物品配方架构，在此基础上新增：
 *    1. Plan 支持 inLiquid（液体原料：水 × 10）、outLiquid（液体产物：炉渣液 × 5）
 *    2. 液体容量数组、getMaximumAccepted(Liquid)、acceptLiquid 判定
 *    3. shouldConsume 双检查：物品 + 液体的「原料够不够、产物塞不塞得下」
 *    4. 进度满时：同时扣物品/液体原料，同时加物品/液体产物，同时对产物做 dump（物品）+ dumpLiquid（液体）
 *
 *  关于「荷载」（Payload）的说明：
 *    Mindustry 159.6 没有 Payload / UnlocateContent 类（项目记忆明确约束），
 *    所以「荷载」按「物品 + 液体 双承载输入/输出」实现，后续版本升级了再把 PayloadStack 加进 Plan 即可。
 * ============================================================
 */
public class HunfuBlock extends Block {

    // ========== 字段区：本方块"整体级别的配置" ==========
    /** 每种物品在这个方块仓里的最大存放量（按物品ID索引的数组） */
    public int[] capacities = {};
    /** 每种液体在这个方块仓里的最大存放量（按液体ID索引的数组） */
    public float[] liquidCapacities = {};

    /** 【核心】配方列表！每个 Plan 就代表一种配方（输入啥、输出啥、做多久） */
    public Seq<Plan> plans = new Seq<>(4);

    /** UI 面板里配方选择网格默认显示几行（可以在方块定义时改）*/
    public int selectionRows = 2;
    /** UI 面板里配方选择网格默认显示几列 */
    public int selectionColumns = 4;

    // ========== 构造器：方块注册阶段调用 ==========
    public HunfuBlock(String name) {
        super(name);
        // ================= 基本属性（方块的"硬件参数"）=================
        update = true;       // 每 tick 需要执行 updateTile()
        solid = true;        // 实体方块
        hasItems = true;     // 有物品仓（物品原料/产物）
        hasPower = true;     // 需要电力
        hasLiquids = true;   // ★ 有液体仓（液体原料/产物）——混成工厂必备
        consumesPower = true;// 真正消耗电
        size = 2;
        health = 100;
        rotate = false;      // 不要方向
        configurable = true; // 点击弹出配方面板
        itemCapacity = 30;   // 初始默认容量（initCapacities() 会根据配方重新算）
        liquidCapacity = 30f;// ★ 初始默认液体容量（同样 initCapacities() 会重算）
        // ================= 三种切换配方的方式（配置通道）=================
        // ① 整数索引：逻辑处理器用
        config(Integer.class, (HunfuBuild build, Integer i) -> {
            if (!configurable) return;
            if (build.currentPlan == i) return;
            build.currentPlan = i < 0 || i >= plans.size ? -1 : i;
            build.progress = 0;
            // 切配方：清空物品仓 + 倒空液体仓！（防止旧配方的铜/水堵了新配方）
            build.dump();
            for (Liquid l : Vars.content.liquids()) build.dumpLiquid(l);
        });
        // ② 物品对象：UI 配方图标点选
        config(Item.class, (HunfuBuild build, Item item) -> {
            if (!configurable) return;
            int next = plans.indexOf(p -> p.outItem != null && p.outItem.length > 0
                    && p.outItem[0] != null && p.outItem[0].item == item);
            if (build.currentPlan == next) return;
            build.currentPlan = next;
            build.progress = 0;
            build.dump();
            for (Liquid l : Vars.content.liquids()) build.dumpLiquid(l);
        });
        // ③ 清空：停止生产
        configClear((HunfuBuild build) -> {
            build.currentPlan = -1;
            build.progress = 0;
        });
    }

    // =================================================================
    // init / afterPatch / initCapacities
    // =================================================================
    @Override
    public void init() {
        initCapacities();
        super.init();
    }
    @Override
    public void afterPatch() {
        initCapacities();
        super.afterPatch();
    }

    /**
     * 【容量自动计算】：根据所有配方推导「每种物品 / 每种液体最多能放仓里多少」。
     * 混成工厂需要同时计算物品（capacities[]）和液体（liquidCapacities[]）两套容量。
     */
    public void initCapacities() {
        // ── 物品容量 ──
        capacities = new int[Vars.content.items().size];
        int maxItem = 0;
        // ── 液体容量 ──
        liquidCapacities = new float[Vars.content.liquids().size];
        float maxLiquid = 0f;

        for (Plan plan : plans) {
            if (plan == null) continue;
            // 扫描物品原料
            if (plan.requirements != null) for (ItemStack stack : plan.requirements)
                if (stack != null && stack.amount > maxItem) maxItem = stack.amount;
            // 扫描物品产物
            if (plan.outItem != null) for (ItemStack stack : plan.outItem)
                if (stack != null && stack.amount > maxItem) maxItem = stack.amount;
            // 扫描液体原料
            if (plan.inLiquid != null) for (LiquidStack stack : plan.inLiquid)
                if (stack != null && stack.amount > maxLiquid) maxLiquid = stack.amount;
            // 扫描液体产物
            if (plan.outLiquid != null) for (LiquidStack stack : plan.outLiquid)
                if (stack != null && stack.amount > maxLiquid) maxLiquid = stack.amount;
        }

        // 放大 10 倍作为统一容量：囤 10 轮的量不堵
        int unifiedItem = Math.max(1, maxItem * 10);
        Arrays.fill(capacities, unifiedItem);
        float unifiedLiquid = Math.max(1f, maxLiquid * 10f);
        Arrays.fill(liquidCapacities, unifiedLiquid);
    }

    /** 游戏内容总数变化时（Mod 热加载），容量数组扩容防止越界 */
    @Override
    public void checkContentArrayCapacity(int items, int liquids) {
        super.checkContentArrayCapacity(items, liquids);
        if (capacities.length != items) capacities = Arrays.copyOf(capacities, items);
        if (liquidCapacities.length != liquids) liquidCapacities = Arrays.copyOf(liquidCapacities, liquids);
    }
    // =================================================================
    // UI 相关：进度条 / 图标 / 详情面板 / 候选列表
    // =================================================================
    @Override
    public void setBars() {
        super.setBars();
        addBar("progress",
                (HunfuBuild e) -> new Bar(
                        "bar.progress",
                        Pal.ammo,
                        e::fraction
                ));
    }
    /** Block 类有 outputsItems() 覆写槽：声明会输出物品，传送带给下游时才会问我要 */
    @Override
    public boolean outputsItems()   { return true; }
    /** ★ 混成工厂：声明会输出液体
     *  ❗ 注意：Block 类没有 outputsLiquids() 的覆写槽（不像 outputsItems 有 @Override），
     *     所以这里只是普通方法，hasLiquids=true + Building.acceptLiquid() 判定才是真正让液体出入仓生效的关键 */
    public boolean outputsLiquids() { return true; }

    @Override
    public TextureRegion[] icons() {
        Seq<TextureRegion> result = new Seq<>();
        if (region != null) result.add(region);
        return result.toArray(TextureRegion.class);
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {
        Draw.rect(region, plan.drawx(), plan.drawy());
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.remove(Stat.itemCapacity);
        // 把液体容量也从默认里移除（我们用的是自定义 liquidCapacities 数组）
        stats.remove(Stat.liquidCapacity);

        // 所有配方展示：物品/液体都显示
        stats.add(Stat.output, table -> {
            table.row();
            for (Plan plan : plans) {
                if (plan == null) continue;
                // 找"配方代表图标"：优先第一个物品输出，没有就找第一个液体输出
                ItemStack firstOutItem = (plan.outItem   != null && plan.outItem.length   > 0) ? plan.outItem[0]   : null;
                LiquidStack firstOutLiq = (plan.outLiquid != null && plan.outLiquid.length > 0) ? plan.outLiquid[0] : null;
                if ((firstOutItem == null || firstOutItem.item == null) &&
                    (firstOutLiq == null || firstOutLiq.liquid == null)) continue;

                table.table(Styles.grayPanel, t -> {
                    // ── 左：配方代表图标 ──
                    t.table(icons -> {
                        icons.left();
                        if (firstOutItem != null && firstOutItem.item != null) {
                            // 有物品输出 → 用物品图标当代表
                            icons.image(firstOutItem.item.uiIcon).size(40).pad(10f).scaling(Scaling.fit)
                                    .with(i -> StatValues.withTooltip(i, firstOutItem.item));
                        } else if (firstOutLiq != null && firstOutLiq.liquid != null) {
                            // 没物品输出只有液体 → 用液体 uiIcon 当代表
                            icons.image(firstOutLiq.liquid.uiIcon).size(40).pad(10f).scaling(Scaling.fit)
                                    .with(i -> StatValues.withTooltip(i, firstOutLiq.liquid));
                        }
                        // 显示"总产物数量：还有 N 种"
                        int outCount = 0;
                        if (plan.outItem   != null) outCount += plan.outItem.length;
                        if (plan.outLiquid != null) outCount += plan.outLiquid.length;
                        if (outCount > 1) icons.add(" +" + (outCount - 1)).color(Color.lightGray).padLeft(2f);
                    }).left();

                    // ── 中：名字 + 耗时 ──
                    t.table(info -> {
                        if (firstOutItem != null && firstOutItem.item != null)
                            info.add(firstOutItem.item.localizedName).left();
                        else if (firstOutLiq != null && firstOutLiq.liquid != null)
                            info.add(firstOutLiq.liquid.localizedName).left();
                        int inCount = 0, outC = 0;
                        if (plan.requirements != null) inCount += plan.requirements.length;
                        if (plan.inLiquid     != null) inCount += plan.inLiquid.length;
                        if (plan.outItem      != null) outC    += plan.outItem.length;
                        if (plan.outLiquid    != null) outC    += plan.outLiquid.length;
                        if (inCount > 1 || outC > 1)
                            info.add(" (物" + inCount + "/液" + outC + ")").color(Color.lightGray).padLeft(4f);
                        info.row();
                        info.add(Strings.autoFixed(plan.time / 60f, 1) + " " + Core.bundle.get("unit.seconds"))
                                .color(Color.lightGray);
                    }).left().padLeft(10f);

                    // ── 右：所有原料（物品+液体，每行 4 个）──
                    t.table(req -> {
                        req.right();
                        int idx = 0;
                        // 先物品原料
                        if (plan.requirements != null) for (ItemStack stack : plan.requirements) {
                            if (idx++ % 4 == 0) req.row();
                            req.add(StatValues.displayItem(stack.item, stack.amount, plan.time, true)).pad(5);
                        }
                        // 再液体原料
                        if (plan.inLiquid != null) for (LiquidStack stack : plan.inLiquid) {
                            if (idx++ % 4 == 0) req.row();
                            // StatValues.displayLiquid 只有 3 个参数：(Liquid, float, boolean)
                            //   没有 plan.time 那个参数（和 displayItem 的 4 参数版签名不一样！之前多传了 plan.time → 编译报错）
                            //   boolean 第三个参数 = 是否显示数量标签
                            req.add(StatValues.displayLiquid(stack.liquid, stack.amount, true)).pad(5);
                        }
                    }).right().grow().pad(10f);
                }).growX().pad(5);
                table.row();
            }
        });
    }

    @Override
    public void getPlanConfigs(Seq<UnlockableContent> options) {
        for (Plan plan : plans) {
            if (plan == null) continue;
            // 配方代表：优先物品输出，其次液体输出
            if (plan.outItem != null && plan.outItem.length > 0 && plan.outItem[0] != null && plan.outItem[0].item != null) {
                Item it = plan.outItem[0].item;
                if (it.unlockedNow()) options.add(it);
            } else if (plan.outLiquid != null && plan.outLiquid.length > 0 && plan.outLiquid[0] != null && plan.outLiquid[0].liquid != null) {
                Liquid lq = plan.outLiquid[0].liquid;
                if (lq.unlockedNow()) options.add(lq);
            }
        }
    }

    // =================================================================
    // 【Plan 内部类】= 一份配方（混成版：物品 + 液体同时参与）
    // =================================================================
    public static class Plan {
        public ItemStack[]   outItem;        // 物品产物（数组，支持多种）
        public ItemStack[]   requirements;   // 物品原料
        public LiquidStack[] outLiquid;      // ★ 液体产物（新增：比如炉渣液×5）
        public LiquidStack[] inLiquid;       // ★ 液体原料（新增：比如水×10）
        public float         time;           // 做一轮耗时（tick）

        /** 全参构造器：物品出/入 + 液体出/入 + 耗时 */
        public Plan(ItemStack[] outItem, float time, ItemStack[] requirements,
                    LiquidStack[] outLiquid, LiquidStack[] inLiquid) {
            this.outItem      = (outItem      != null) ? outItem      : new ItemStack[0];
            this.requirements = (requirements != null) ? requirements : new ItemStack[0];
            this.outLiquid    = (outLiquid    != null) ? outLiquid    : new LiquidStack[0];
            this.inLiquid     = (inLiquid     != null) ? inLiquid     : new LiquidStack[0];
            this.time         = time;
        }
        /** 兼容老版：只有物品（液体传 null 即可）*/
        public Plan(ItemStack[] outItem, float time, ItemStack[] requirements) {
            this(outItem, time, requirements, null, null);
        }
        Plan(){
            this.outItem      = new ItemStack[0];
            this.requirements = new ItemStack[0];
            this.outLiquid    = new LiquidStack[0];
            this.inLiquid     = new LiquidStack[0];
            this.time         = time;
        }
    }

    // =================================================================
    // 【HunfuBuild 内部类】= 地图上放下去的那个方块实例（混成版）
    //   完全照搬 ConfigurableBlock.UnitFactoryBuild，
    //   新增：液体 acceptLiquid / 液体 shouldConsume 检查 / 扣液体加液体
    // =================================================================
    public class HunfuBuild extends Building {

        // ========== 实例级字段 ==========
        public int   currentPlan = -1;  // 配方索引（-1 = 不生产）
        public float progress    = 0f;  // 当前配方进度

        // ========== 小工具方法 ==========
        public float fraction() {
            if (currentPlan == -1 || currentPlan >= plans.size) return 0;
            Plan p = plans.get(currentPlan);
            return p == null ? 0 : progress / p.time;
        }

        @Override
        public void created() {
            if (currentPlan == -1) {
                for (int i = 0; i < plans.size; i++) {
                    Plan p = plans.get(i);
                    boolean hasOut = (p.outItem   != null && p.outItem.length   > 0 && p.outItem[0]   != null && p.outItem[0].item   != null && p.outItem[0].item.unlockedNow())
                                  || (p.outLiquid != null && p.outLiquid.length > 0 && p.outLiquid[0] != null && p.outLiquid[0].liquid != null && p.outLiquid[0].liquid.unlockedNow());
                    if (hasOut) { currentPlan = i; break; }
                }
                if (currentPlan == -1 && plans.size > 0) currentPlan = 0;
            }
        }

        @Override
        public void drawSelect() {
            super.drawSelect();
            if (plans.size > 1 && currentPlan != -1 && currentPlan < plans.size) {
                Plan p = plans.get(currentPlan);
                if (p != null) {
                    // 优先画第一个物品输出；没有就画第一个液体输出
                    if (p.outItem != null && p.outItem.length > 0 && p.outItem[0] != null && p.outItem[0].item != null) {
                        drawItemSelection(p.outItem[0].item);
                    } else if (p.outLiquid != null && p.outLiquid.length > 0 && p.outLiquid[0] != null && p.outLiquid[0].liquid != null) {
                        // 液体高亮：用 Mindustry 自带的液体选择绘制（其实就是液体条）
                    }
                }
            }
        }

        @Override
        public boolean acceptPayload(Building source, mindustry.world.blocks.payloads.Payload payload) { return false; }

        /** 【acceptLiquid】要不要收某个液体？只收当前配方的 inLiquid（液体原料），其他一律拒收 */
        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            if (currentPlan == -1 || currentPlan >= plans.size) return false;
            Plan plan = plans.get(currentPlan);
            if (plan == null || plan.inLiquid == null) return false;
            for (LiquidStack stack : plan.inLiquid) {
                if (stack != null && stack.liquid == liquid
                        && liquids.get(liquid) < getMaximumAccepted(liquid)) {
                    return true;
                }
            }
            return false;  // 其它液体（比如我们自己产的炉渣液）绝对不收回来
        }

        @Override
        public Object senseObject(LAccess sensor) {
            if (sensor == LAccess.config) {
                if (currentPlan == -1 || currentPlan >= plans.size) return null;
                Plan p = plans.get(currentPlan);
                if (p == null) return null;
                // 优先物品输出，没有就液体输出
                if (p.outItem != null && p.outItem.length > 0 && p.outItem[0] != null) return p.outItem[0].item;
                if (p.outLiquid != null && p.outLiquid.length > 0 && p.outLiquid[0] != null) return p.outLiquid[0].liquid;
                return null;
            }
            return super.senseObject(sensor);
        }

        @Override
        public double sense(LAccess sensor) {
            if (sensor == LAccess.progress)     return Mathf.clamp(fraction());
            if (sensor == LAccess.itemCapacity) return itemCapacity;
            if (sensor == LAccess.liquidCapacity) return liquidCapacity;
            return super.sense(sensor);
        }

        @Override
        public void display(Table table) {
            super.display(table);
            TextureRegionDrawable reg = new TextureRegionDrawable();
            table.row();
            table.table(t -> {
                t.left();
                t.image().update(i -> {
                    if (currentPlan == -1 || currentPlan >= plans.size) {
                        i.setDrawable(Icon.cancel); i.setColor(Color.lightGray);
                    } else {
                        Plan p = plans.get(currentPlan);
                        if (p == null) { i.setDrawable(Icon.cancel); i.setColor(Color.lightGray); return; }
                        if (p.outItem != null && p.outItem.length > 0 && p.outItem[0] != null && p.outItem[0].item != null) {
                            i.setDrawable(reg.set(p.outItem[0].item.uiIcon));
                            i.setColor(Color.white);
                        } else if (p.outLiquid != null && p.outLiquid.length > 0 && p.outLiquid[0] != null && p.outLiquid[0].liquid != null) {
                            i.setDrawable(reg.set(p.outLiquid[0].liquid.uiIcon));
                            i.setColor(Color.white);
                        } else {
                            i.setDrawable(Icon.cancel); i.setColor(Color.lightGray);
                        }
                    }
                    i.setScaling(Scaling.fit);
                }).size(32).padBottom(-4).padRight(2);
                t.label(() -> {
                    if (currentPlan == -1 || currentPlan >= plans.size) return "@none";
                    Plan p = plans.get(currentPlan);
                    if (p == null) return "@none";
                    if (p.outItem != null && p.outItem.length > 0 && p.outItem[0] != null && p.outItem[0].item != null)
                        return p.outItem[0].item.localizedName;
                    if (p.outLiquid != null && p.outLiquid.length > 0 && p.outLiquid[0] != null && p.outLiquid[0].liquid != null)
                        return p.outLiquid[0].liquid.localizedName;
                    return "@none";
                }).wrap().width(230f).color(Color.lightGray);
            }).left();
        }

        @Override
        public Object config() { return currentPlan; }

        @Override
        public void draw() {
            Draw.rect(region, x, y);
        }

        // ========== 配置面板 UI ==========
        @Override
        public void buildConfiguration(Table table) {
            // 候选列表：所有配方的"第一个输出"（先物品，没有就液体）
            Seq<UnlockableContent> candidates = new Seq<>();
            for (Plan p : plans) {
                if (p == null) continue;
                if (p.outItem != null && p.outItem.length > 0 && p.outItem[0] != null
                        && p.outItem[0].item != null && p.outItem[0].item.unlockedNow()) {
                    candidates.add(p.outItem[0].item);
                } else if (p.outLiquid != null && p.outLiquid.length > 0 && p.outLiquid[0] != null
                        && p.outLiquid[0].liquid != null && p.outLiquid[0].liquid.unlockedNow()) {
                    candidates.add(p.outLiquid[0].liquid);
                }
            }
            if (candidates.any()) {
                // ★ 有液体和物品混合时：退化为手写按钮网格（ItemSelection 只支持 Item）
                // 每行 selectionColumns 个
                int idx = 0;
                for (UnlockableContent uc : candidates) {
                    if (idx % selectionColumns == 0) table.row();
                    Item item = (uc instanceof Item it) ? it : null;
                    Liquid liq = (uc instanceof Liquid lq) ? lq : null;
                    ImageButton b = table.button(Tex.whiteui, Styles.squareTogglei, () -> {
                        if (item != null) configure(item);
                        else if (liq != null) {
                            // 液体当选代表：找第一个「第一个 outLiquid 就是这个液体」的配方
                            // ⚠️ 之前写了一段 p.outItem[0].item == liq 比较（Liquid vs Item），不同类型永远不等还报错
                            int pi = plans.indexOf(p ->
                                    p.outLiquid != null && p.outLiquid.length > 0
                                            && p.outLiquid[0] != null && p.outLiquid[0].liquid == liq);
                            if (pi >= 0) configure(pi);
                        }
                    }).size(50f).get();
                    // 图标：物品/液体各自 uiIcon
                    b.clearChildren();
                    if (item != null) b.image(item.uiIcon).scaling(Scaling.fit).size(34);
                    else if (liq != null) b.image(liq.uiIcon).scaling(Scaling.fit).size(34);
                    b.update(() -> {
                        if (currentPlan == -1 || currentPlan >= plans.size) { b.setChecked(false); return; }
                        Plan p = plans.get(currentPlan);
                        if (p == null) { b.setChecked(false); return; }
                        if (item != null) b.setChecked(p.outItem != null && p.outItem.length > 0
                                && p.outItem[0] != null && p.outItem[0].item == item);
                        else if (liq != null) b.setChecked(
                                // outLiquid[0].liquid == liq，不要和 outItem 比较（类型不同）
                                p.outLiquid != null && p.outLiquid.length > 0
                                        && p.outLiquid[0] != null && p.outLiquid[0].liquid == liq);
                    });
                    table.add().pad(2);
                    idx++;
                }
            } else {
                table.table(Styles.black3, t -> t.add("@none").color(Color.lightGray));
            }
        }

        // ========== shouldConsume：能不能开工？（物品 + 液体双检查）==========
        @Override
        public boolean shouldConsume() {
            if (currentPlan == -1 || currentPlan >= plans.size) return false;
            if (!enabled) return false;
            Plan plan = plans.get(currentPlan);
            if (plan == null) return false;
            boolean hasAnyOut = (plan.outItem != null && plan.outItem.length > 0)
                             || (plan.outLiquid != null && plan.outLiquid.length > 0);
            if (!hasAnyOut) return false;

            // ── 检查 1：所有物品产物容量 ──
            if (plan.outItem != null) for (ItemStack s : plan.outItem) {
                if (s == null || s.item == null) continue;
                int willHave = items.get(s.item) + s.amount;
                if (willHave > getMaximumAccepted(s.item)) return false;
            }
            // ── 检查 2：所有液体产物容量 ──（★ 混成工厂新增）
            if (plan.outLiquid != null) for (LiquidStack s : plan.outLiquid) {
                if (s == null || s.liquid == null) continue;
                float willHave = liquids.get(s.liquid) + s.amount;
                if (willHave > getMaximumAccepted(s.liquid)) return false;
            }
            // ── 检查 3：物品原料够不够 ──
            if (plan.requirements != null) for (ItemStack s : plan.requirements) {
                if (s == null || s.item == null) continue;
                if (items.get(s.item) < s.amount) return false;
            }
            // ── 检查 4：液体原料够不够 ──（★ 混成工厂新增）
            if (plan.inLiquid != null) for (LiquidStack s : plan.inLiquid) {
                if (s == null || s.liquid == null) continue;
                if (liquids.get(s.liquid) < s.amount) return false;
            }
            return true;
        }

        @Override
        public BlockStatus status() { return super.status(); }

        /** 某物品最大容量：读自定义 capacities 数组——Building 类对 Item 版本有覆写槽 */
        @Override
        public int getMaximumAccepted(Item item) {
            if (item == null || item.id >= capacities.length) return 0;
            return capacities[item.id];
        }
        /** ★ 混成工厂新增：某液体最大容量：读自定义 liquidCapacities 数组
         *  ❗ Building 类没有对 Liquid 版本的 getMaximumAccepted 覆写槽（只有 Item 版有）
         *     所以这里只是普通方法（不要 @Override），真正限制液体量的是 Building.addLiquid/removeLiquid
         *     会问 Liquids Module 的容量上限，我们这里只供 shouldConsume 自查容量用 */
        public float getMaximumAccepted(Liquid liquid) {
            if (liquid == null || liquid.id >= liquidCapacities.length) return 0f;
            return liquidCapacities[liquid.id];
        }

        /** acceptItem：只收当前配方的物品原料 */
        @Override
        public boolean acceptItem(Building source, Item item) {
            if (currentPlan == -1 || currentPlan >= plans.size) return false;
            Plan plan = plans.get(currentPlan);
            if (plan == null || plan.requirements == null) return false;
            for (ItemStack stack : plan.requirements) {
                if (stack != null && stack.item == item
                        && items.get(item) < getMaximumAccepted(item)) {
                    return true;
                }
            }
            return false;
        }

        // =================================================================
        // ✅✅✅ 核心方法：updateTile() 每 tick（物品 + 液体双流程）
        // =================================================================
        @Override
        public void updateTile() {
            // 前置：越界/坏配方
            if (!configurable) currentPlan = 0;
            if (currentPlan < 0 || currentPlan >= plans.size) { currentPlan = -1; return; }
            Plan plan = plans.get(currentPlan);
            boolean hasAnyOut = (plan != null) && (
                    (plan.outItem != null && plan.outItem.length > 0) ||
                    (plan.outLiquid != null && plan.outLiquid.length > 0));
            if (plan == null || !hasAnyOut) { currentPlan = -1; return; }

            // Step 1：进度累积
            if (efficiency > 0) {
                progress += edelta();
            }

            // Step 2：进度满 → 扣原料 + 出产物
            if (progress >= plan.time) {
                // ① 检查：物品产物容量
                boolean allOutOk = true;
                if (plan.outItem != null) for (ItemStack s : plan.outItem) {
                    if (s == null || s.item == null) continue;
                    int willHave = items.get(s.item) + s.amount;
                    if (willHave > getMaximumAccepted(s.item)) { allOutOk = false; break; }
                }
                // ② 检查：液体产物容量（★ 混成工厂新增）
                if (allOutOk && plan.outLiquid != null) for (LiquidStack s : plan.outLiquid) {
                    if (s == null || s.liquid == null) continue;
                    float willHave = liquids.get(s.liquid) + s.amount;
                    if (willHave > getMaximumAccepted(s.liquid)) { allOutOk = false; break; }
                }
                // ③ 检查：物品原料
                boolean allInOk = true;
                if (plan.requirements != null) for (ItemStack s : plan.requirements) {
                    if (s == null || s.item == null) continue;
                    if (items.get(s.item) < s.amount) { allInOk = false; break; }
                }
                // ④ 检查：液体原料（★ 混成工厂新增）
                if (allInOk && plan.inLiquid != null) for (LiquidStack s : plan.inLiquid) {
                    if (s == null || s.liquid == null) continue;
                    if (liquids.get(s.liquid) < s.amount) { allInOk = false; break; }
                }

                if (allOutOk && allInOk) {
                    // ==========================================
                    // ★ 扣原料：物品 items.remove + 液体 liquids.remove
                    // ==========================================
                    if (plan.requirements != null) for (ItemStack s : plan.requirements) {
                        if (s == null || s.item == null || s.amount <= 0) continue;
                        items.remove(s.item, s.amount);
                    }
                    if (plan.inLiquid != null) for (LiquidStack s : plan.inLiquid) {
                        if (s == null || s.liquid == null || s.amount <= 0) continue;
                        liquids.remove(s.liquid, s.amount);
                    }
                    // ==========================================
                    // ★ 出产物：物品（add + dump×N）+ 液体（add + dumpLiquid）
                    // ==========================================
                    // ① 物品产物：入仓 + 一份一份 dump
                    if (plan.outItem != null) for (ItemStack s : plan.outItem) {
                        if (s == null || s.item == null || s.amount <= 0) continue;
                        items.add(s.item, s.amount);
                        for (int i = 0; i < s.amount; i++) dump(s.item);
                    }
                    // ② 液体产物：入液仓 + dumpLiquid 推下游管道（★ 混成工厂新增）
                    if (plan.outLiquid != null) for (LiquidStack s : plan.outLiquid) {
                        if (s == null || s.liquid == null || s.amount <= 0f) continue;
                        liquids.add(s.liquid, s.amount);
                        // dumpLiquid(liquid) 也是一次推一份，amount 多少就推几次
                        int times = (int)Math.ceil(s.amount);
                        for (int i = 0; i < times; i++) dumpLiquid(s.liquid);
                    }

                    progress -= plan.time;
                } else {
                    progress = Math.min(progress, plan.time - 0.001f);
                }
            }
            if (progress < 0) progress = 0;

            // ── 兜底：每个 tick 末再扫一遍当前配方的所有产物（物品+液体），把仓里剩余的再推一遍
            if (currentPlan != -1 && currentPlan < plans.size) {
                Plan lp = plans.get(currentPlan);
                if (lp != null) {
                    // 物品兜底
                    if (lp.outItem != null) for (ItemStack s : lp.outItem) {
                        if (s == null || s.item == null) continue;
                        int left = items.get(s.item);
                        if (left <= 0) continue;
                        for (int i = 0; i < left; i++) dump(s.item);
                    }
                    // 液体兜底（★ 混成工厂新增）
                    if (lp.outLiquid != null) for (LiquidStack s : lp.outLiquid) {
                        if (s == null || s.liquid == null) continue;
                        float left = liquids.get(s.liquid);
                        if (left <= 0f) continue;
                        int times = (int)Math.ceil(left);
                        for (int i = 0; i < times; i++) dumpLiquid(s.liquid);
                    }
                }
            }
        }

        // =================================================================
        // 存档读写：物品 + 液体完全由父类 Building 已经处理，
        // 我们自己只存 progress + currentPlan（和 ConfigurableBlock 一致）
        // =================================================================
        @Override
        public byte version() { return 1; }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.f(progress);      // ① float 4B
            write.s(currentPlan);   // ② short 2B
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            progress = read.f();         // ① float 4B
            currentPlan = read.s();      // ② short 2B
        }
    }
}
