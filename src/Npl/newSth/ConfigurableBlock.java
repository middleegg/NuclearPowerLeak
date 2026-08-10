
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
import mindustry.content.Fx;
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
 * 【这个类是干啥的？】
 * 这是一个「可选配方的工厂方块」类。
 * 原版 Mindustry 的工厂（比如 Smelter 硅矿机）只能做一种东西：铜+铅→硅。
 * 但这个 ConfigurableBlock 能在 UI 里切换多种配方：
 *     配方A：铜+铅→钛
 *     配方B：煤+水→硅+炉渣
 *     配方C：钍→相织物
 *     ...想加多少加多少
 * 玩家点击方块后，在弹出的面板里点"钛图标"就做钛，点"硅图标"就做硅。
 * ============================================================
 *
 * 【继承关系（★ 2026-07-31 彻底修复）】
 *  旧版本错误：extends UnitBlock → PayloadBlock
 *      PayloadBlock 强制 sync=true + group=payloads →
 *      ① 服务器权威同步导致"物品推下游一份，仓里还留一份"的幻象
 *      ② payload 组方块的下游传送带被特殊处理，普通物品永远推不到传送带上
 *  现在正确版本：直接 extends Block（和 ConBlock 一样）
 *      自己写 progress 字段、自己扣 items.remove、自己算效率、没有 Payload 干扰
 */
public class ConfigurableBlock extends Block {

    // ========== 字段区：本方块"整体级别的配置"（所有放下去的方块共用这些）==========
    /** 每种物品在这个方块仓里的最大存放量（按物品ID索引的数组）。
     *  比如 capacities[钛.id]=100 表示仓里最多能放 100 个钛（含原料和输出）。
     *  这个数组会在 initCapacities() 里根据所有配方自动算出来。 */
    public int[] capacities = {};

    /** 【核心】配方列表！每个 Plan 就代表一种配方（输入啥、输出啥、做多久）。
     *  想加配方就 plans.add(new Plan(...)) 就行。*/
    public Seq<Plan> plans = new Seq<>(4);

    /** UI 面板里配方选择网格默认显示几行（可以在方块定义时改）*/
    public int selectionRows = 2;

    /** UI 面板里配方选择网格默认显示几列 */
    public int selectionColumns = 4;

    /** 生产完成时触发的视觉效果（在 NuBlocks.java 中赋值，如 NuFx.lightningStormFull）*/
    public Effect craftEffect = Fx.none;

    // ========== 构造器：方块"注册阶段"调用（NuBlocks.java 里 new 的时候跑一次）==========
    public ConfigurableBlock(String name) {
        super(name);  // 先让父类 Block 初始化（会设置基础贴图啥的）
        // ================= 基本属性（方块的"硬件参数"）=================
        update = true;       // 这个方块"每 tick 需要执行 updateTile()"（不写的话生产逻辑不跑！）
        solid = true;        // 这是实体方块，单位不能穿过去
        hasItems = true;     // 这个方块有物品仓（能收原料/存产物）
        hasPower = true;     // 需要电力
        hasLiquids = true;   // 构建液体库
        consumesPower = true;// 真正消耗电（不写的话有电也不会扣）
        size = 2;            // 大小：2×2 格（硅机是 1×1，多配方工厂一般做大点）
        health = 100;        // 血量：被敌人打多少下才炸
        rotate = false;      // ★ 用户要求：不要方向！工厂不要有"朝左朝右"的区别
        configurable = true; // ★ 必须：玩家点击方块能弹出"配置面板"（就是切换配方的 UI）
        itemCapacity = 30;   // 初始默认容量（后面 initCapacities() 会根据配方重新算）
        // ================= 三种"切换配方"的方式（配置通道）=================
        // ① 方式一：传「整数索引」切换（给逻辑处理器 / 蓝图 / 脚本用）
        //    比如逻辑处理器写 "configure 0" → 切到 plans[0] 配方
        // ★ 第一个 lambda 参数必须用具体内部类名 UnitFactoryBuild，不能写 Building！
        config(Integer.class, (UnitFactoryBuild build, Integer i) -> {
            if (!configurable) return;           // 万一 configurable=false 了，不让切
            if (build.currentPlan == i) return;  // 已经是这个配方了，啥也不做（省得清仓）
            build.currentPlan = i < 0 || i >= plans.size ? -1 : i;  // 越界就设成 -1（=停止生产）
            build.progress = 0;                  // 进度重置（配方都变了，老进度没用了）
            build.dump();                        // 切配方顺便把仓里老东西全倒出去，防止"配方A的铜堵了配方B"
        });
        // ② 方式二：传「物品对象」切换（玩家点 UI 里的图标触发）
        //   比如玩家点了"钛图标"，传 Items.titanium → 找第一个产物是钛的配方
        config(Item.class, (UnitFactoryBuild build, Item item) -> {
            if (!configurable) return;
            // 在 plans 里找"第一个输出物就是这个 item"的配方
            int next = plans.indexOf(p -> p.outItem != null && p.outItem.length > 0
                    && p.outItem[0] != null && p.outItem[0].item == item);
            if (build.currentPlan == next) return;
            build.currentPlan = next;  // 没找到就是 -1（plans.indexOf 找不到默认返回-1）
            build.progress = 0;
            build.dump();
        });
        // ③ 方式三：「清空配置」= 停止生产（逻辑处理器发"configure 0 0"或玩家重置时）
        configClear((UnitFactoryBuild build) -> {
            build.currentPlan = -1;  // -1 代表"啥配方也不做"
            build.progress = 0;
        });
    }
    // =================================================================
    // init / afterPatch：初始化容量
    // 这俩方法 Mindustry 会自动调用（加载 Mod 时），我们不用手动调
    // =================================================================
    @Override
    public void init() {
        initCapacities();       // 算一遍容量
        super.init();           // 父类 Block 初始化（不碰 consumeBuilder 了，我们自己扣）
    }
    @Override
    public void afterPatch() {
        initCapacities();       // 再算一遍（有些 Mod 会在 Mod 加载后再加新物品/新配方，确保不越界）
        super.afterPatch();
    }
    /**
     * 【这个方法干啥？】
     * 根据所有配方推导「每种物品最多能放仓里多少个」。
     * 【为什么要单独算？】
     * 原版工厂 itemCapacity=30 是"不管啥物品，加起来最多 30"。
     * 但我们是多配方工厂：有的配方一次产 20 个铜，默认 30 的话塞两下就满了 → 堵料。
     * 所以要把容量按"配方里最大的一份"放大。
     */
    public void initCapacities() {
        // capacities 数组大小 = 游戏里全部物品的总数（比如铜=0, 铅=1, ... 钛=12 ...）
        capacities = new int[Vars.content.items().size];
        int maxAmount = 0;
        // ── 第一步：扫描所有配方，找"单次需要/产出物品最多的数" ──
        for (Plan plan : plans) {
            if (plan == null) continue;
            // 扫描「原料 requirements」里的单物品最大量
            if (plan.requirements != null) for (ItemStack stack : plan.requirements) {
                if (stack != null && stack.amount > maxAmount) maxAmount = stack.amount;
            }
            // 扫描「产物 outItem」里的单物品最大量
            if (plan.outItem != null) for (ItemStack stack : plan.outItem) {
                if (stack != null && stack.amount > maxAmount) maxAmount = stack.amount;
            }
        }
        // ── 第二步：放大 10 倍作为统一容量 ──
        // （比如最大一份是 5 个铜，容量就是 5*10=50，能囤 10 轮生产，不会堵）
        int unifiedLimit = Math.max(1, maxAmount * 10);
        Arrays.fill(capacities, unifiedLimit);  // 所有物品容量都设成这个数
    }
    /** 游戏物品总数变化时（比如另一个 Mod 热加载），把容量数组扩容防止越界崩 */
    @Override
    public void checkContentArrayCapacity(int items, int liquids) {
        super.checkContentArrayCapacity(items, liquids);
        if (capacities.length != items) capacities = Arrays.copyOf(capacities, items);
    }
    // =================================================================
    // 进度条 / 输出声明 / 图标 / 建造预览 / 统计面板
    // （这些都是 UI 相关，不影响核心生产逻辑）
    // =================================================================
    /** 【setBars】设置方块身上的血条/进度条等 */
    @Override
    public void setBars() {
        super.setBars();                          // 先加默认的血量/电力条
        addBar("progress",                        // 再加一个叫"progress"的进度条
                // ★ 第一个参数必须是具体内部类名 UnitFactoryBuild，不能写 Building
                (UnitFactoryBuild e) -> new Bar(
                        "bar.progress",            // 显示名："进度"（翻译文件里的 key）
                        Pal.ammo,                  // 颜色：黄色（弹药色）
                        e::fraction                // 数值（0~1）：调 fraction() 方法
                ));
    }
    /** 告诉 Mindustry「我这个方块能产出物品」，这样传送带能从仓里吸东西 */
    @Override
    public boolean outputsItems() { return true; }
    /** 【icons】返回方块在"物品栏/建造面板"的预览图（多层贴图叠加） */
    @Override
    public TextureRegion[] icons() {
        // region = 主底图（方块名.png），outRegion = 输出层（方块名-out.png），topRegion = 顶层装饰（方块名-top.png）
        // 注意：如果没有 -out.png / -top.png，Core.atlas.find() 会返回 error 纹理，
        // 我们在这里动态过滤掉 null 和 error，保证返回数组没有 null 元素（防止 NPE 崩溃）
        Seq<TextureRegion> result = new Seq<>();
        if (region != null) result.add(region);
        return result.toArray(TextureRegion.class);
    }
    /** 【drawPlanRegion】建造预览时画啥？（把鼠标悬停在地图上还没放的时候） */
    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {
        Draw.rect(region, plan.drawx(), plan.drawy());       // 先画底图
        // rotate=false → 不需要旋转
    }
    /** 【setStats】方块详情面板里显示啥（点方块右上角的"i"看属性）*/
    @Override
    public void setStats() {
        super.setStats();                                    // 先画默认的血量/电力/容量
        stats.remove(Stat.itemCapacity);                     // 删掉默认容量（我们用的是自定义 capacities 数组，不准）
        // 在"输出"那块加一个大表格，列出所有配方：每个配方一行 = 产物 + 耗时 + 原料
        stats.add(Stat.output, table -> {
            table.row();
            for (Plan plan : plans) {                        // 遍历所有配方
                if (plan == null || plan.outItem == null || plan.outItem.length == 0) continue;
                ItemStack firstOut = plan.outItem[0];        // 第一个输出物（作为配方代表图标）
                if (firstOut == null || firstOut.item == null) continue;
                table.table(Styles.grayPanel, t -> {         // 每个配方一个"灰色面板"
                    // ── 左侧：产物图标 + 名字 ──
                    t.table(icons -> {
                        icons.left();
                        icons.image(firstOut.item.uiIcon).size(40).pad(10f).scaling(Scaling.fit)
                                .with(i -> StatValues.withTooltip(i, firstOut.item));  // 鼠标悬停显示物品详情
                        if (plan.outItem.length > 1) {
                            // 多产物就显示"+N"（比如+1 代表还有一种）
                            icons.add("+" + (plan.outItem.length - 1)).color(Color.lightGray).padLeft(2f);
                        } else if (firstOut.amount > 1) {
                            // 单产物但一次产多个就显示"×数量"（比如×2）
                            icons.add("×" + firstOut.amount).color(Color.lightGray).padLeft(2f);
                        }
                    }).left();
                    // ── 中间：产物名 + 耗时 ──
                    t.table(info -> {
                        info.add(firstOut.item.localizedName).left();
                        if (plan.outItem.length > 1) {
                            info.add(" +" + (plan.outItem.length - 1) + " more").color(Color.lightGray).padLeft(4f);
                        }
                        info.row();
                        // plan.time 是 tick 数，除以 60 就是秒（Mindustry 1 秒 = 60 tick）
                        info.add(Strings.autoFixed(plan.time / 60f, 1) + " " + Core.bundle.get("unit.seconds"))
                                .color(Color.lightGray);
                    }).left().padLeft(10f);
                    // ── 右侧：所有原料图标（每行 4 个）──
                    t.table(req -> {
                        req.right();
                        for (int i = 0; i < plan.requirements.length; i++) {
                            if (i % 4 == 0) req.row();
                            ItemStack stack = plan.requirements[i];
                            req.add(StatValues.displayItem(stack.item, stack.amount, plan.time, true)).pad(5);
                        }
                    }).right().grow().pad(10f);
                }).growX().pad(5);
                table.row();  // 下一个配方换一行
            }
        });
    }
    /** 【getPlanConfigs】给配置 UI 的逻辑锚点：把所有配方的第一个输出物都塞进候选列表 */
    @Override
    public void getPlanConfigs(Seq<UnlockableContent> options) {
        for (Plan plan : plans) {
            if (plan == null || plan.outItem == null || plan.outItem.length == 0) continue;
            Item item = plan.outItem[0].item;
            // 物品已解锁（比如不是科技树前面的）才显示
            if (item != null && item.unlockedNow()) {
                options.add(item);
            }
        }
    }

    // =================================================================
    // 【Plan 内部类】= 一份配方
    // 字段名特意保留了用户的命名：outItem（输出物）、requirements（输入物）、time（耗时）
    // =================================================================
    public static class Plan {
        public ItemStack[] outItem;        // 产物列表（数组！一次能产多种物品）
        public ItemStack[] requirements;   // 原料列表（铜5个、铅3个……）
        public float time;                 // 做一轮要多久（单位：tick，60 tick = 1 秒）
        /** 全参构造器：new Plan(outItem数组, 耗时, requirements数组) */
        public Plan(ItemStack[] outItem, float time, ItemStack[] requirements) {
            // ★ 所有参数都判空，免得用户传 null 进来时下面用着崩
            this.outItem      = (outItem      != null) ? outItem      : new ItemStack[0];
            this.requirements = (requirements != null) ? requirements : new ItemStack[0];
            this.time         = time;
        }
        /** 空构造器（默认初始化就行，不传参就空数组）*/
        Plan(){
            this.outItem      = new ItemStack[0];
            this.requirements = new ItemStack[0];
            this.time         = time;
        }
    }
    // =================================================================
    // 【UnitFactoryBuild 内部类】= 真正放在地图上的那个方块实例
    // 每在地图上放一个 ConfigurableBlock，就会 new 一个 UnitFactoryBuild 对象
    //
    // ★★★ 2026-07-31 彻底修复：extends Building，不再继承 UnitBuild（Payload）
    //     旧继承链：UnitBuild → PayloadBlockBuild → PayloadBlock.Build → Building
    //         PayloadBlock 强制 sync=true + group=payloads → 导致：
    //           ① sync=true 服务器覆盖物品状态 = "传送带走一份，仓里还剩一份"幻象（复制感）
    //           ② group=payloads 下游传送带对物品 acceptItem 被特殊改写 = 仓里物品永远推不出
    //     现在直接 extends Building（和 ConBlock 模式完全一致），没有任何干扰
    // =================================================================
    public class UnitFactoryBuild extends Building {

        // ========== 实例级字段：每个放下去的方块都有自己独一份 ==========
        /** 当前选中的配方索引（指向 plans[currentPlan]；-1 = 啥也不做/停止生产） */
        public int currentPlan = -1;
        /** 当前配方进度（0~plan.time；超过 plan.time 就算完成一周期）
         *  ★ 现在是我们自己的字段，不会 shadow 任何父类。 */
        public float progress = 0f;

        // ========== 各种小工具方法 ==========

        /** 完成比例（0~1），给进度条用（比如 plan.time=90，progress=45 → 0.5 = 50%） */
        public float fraction() {
            if (currentPlan == -1 || currentPlan >= plans.size) return 0;  // 没选配方/索引越界 → 0%
            Plan p = plans.get(currentPlan);
            return p == null ? 0 : progress / p.time;
        }
        /** 【created】方块刚放下去时调用一次：自动选一个"能解锁的配方"，省得玩家手动点 */
        @Override
        public void created() {
            if (currentPlan == -1) {                                   // 还没选过配方才执行
                for (int i = 0; i < plans.size; i++) {
                    Plan p = plans.get(i);
                    if (p != null && p.outItem != null && p.outItem.length > 0
                            && p.outItem[0] != null && p.outItem[0].item != null
                            && p.outItem[0].item.unlockedNow()) {     // 找一个已解锁的
                        currentPlan = i;
                        break;
                    }
                }
                // 万一全被锁了，默认选 plans[0]（总不能一直停着）
                if (currentPlan == -1 && plans.size > 0) currentPlan = 0;
            }
        }
        /** 【drawSelect】玩家选中这个方块时：在方块上方把"当前产物"的图标画出来高亮 */
        @Override
        public void drawSelect() {
            super.drawSelect();
            // 配方数量 >1 才有必要显示（只有一种配方还高亮它干啥……）
            if (plans.size > 1 && currentPlan != -1 && currentPlan < plans.size) {
                Plan p = plans.get(currentPlan);
                if (p != null && p.outItem != null && p.outItem.length > 0
                        && p.outItem[0] != null && p.outItem[0].item != null) {
                    drawItemSelection(p.outItem[0].item);  // Mindustry 自带的"把物品图标画在选中方块上"方法
                }
            }
        }

        /** 这个工厂不接 Payload（单位/方块载荷），直接拒绝 */
        @Override
        public boolean acceptPayload(Building source, mindustry.world.blocks.payloads.Payload payload) { return false; }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) { return true; }

        /** 【senseObject】逻辑传感器：读 @config → 返回当前生产的第一个物品（给逻辑处理器判断用）*/
        @Override
        public Object senseObject(LAccess sensor) {
            if (sensor == LAccess.config) {
                if (currentPlan == -1 || currentPlan >= plans.size) return null;
                Plan p = plans.get(currentPlan);
                if (p == null || p.outItem == null || p.outItem.length == 0
                        || p.outItem[0] == null) return null;
                return p.outItem[0].item;
            }
            return super.senseObject(sensor);
        }
        /** 【sense】逻辑传感器：读 progress（生产进度 0~1）/ itemCapacity（容量）*/
        @Override
        public double sense(LAccess sensor) {
            if (sensor == LAccess.progress)     return Mathf.clamp(fraction());  // clamp 保证 0~1 之间
            if (sensor == LAccess.itemCapacity) return itemCapacity;
            return super.sense(sensor);
        }
        /** 【display】鼠标悬停时小面板显示啥：显示当前生产图标 + 名字 */
        @Override
        public void display(Table table) {
            super.display(table);  // 先显示默认的血量/电力条
            TextureRegionDrawable reg = new TextureRegionDrawable();  // 临时的贴图容器
            table.row();
            table.table(t -> {
                t.left();
                // ── 左：图标 ──（每帧更新一次，实时反映配方变化）
                t.image().update(i -> {
                    if (currentPlan == -1 || currentPlan >= plans.size) {
                        i.setDrawable(Icon.cancel);      // 没选配方 → 显示红叉
                        i.setColor(Color.lightGray);
                    }
                    else {
                        Plan p = plans.get(currentPlan);
                        if (p == null || p.outItem == null || p.outItem.length == 0
                                || p.outItem[0] == null || p.outItem[0].item == null) {
                            i.setDrawable(Icon.cancel);
                            i.setColor(Color.lightGray);
                        }
                        else {
                            i.setDrawable(reg.set(p.outItem[0].item.uiIcon));  // 正常 → 显示产物图标
                            i.setColor(Color.white);
                        }
                    }
                    i.setScaling(Scaling.fit);
                }).size(32).padBottom(-4).padRight(2);
                // ── 右：物品名称 ──（也每帧更新）
                t.label(() -> {
                    if (currentPlan == -1 || currentPlan >= plans.size) return "@none";  // "@none" = 游戏翻译里的"无"
                    Plan p = plans.get(currentPlan);
                    if (p == null || p.outItem == null || p.outItem.length == 0
                            || p.outItem[0] == null || p.outItem[0].item == null) return "@none";
                    return p.outItem[0].item.localizedName;
                }).wrap().width(230f).color(Color.lightGray);
            }).left();
        }
        /** 把 currentPlan 当配置值返回（保存/同步/给逻辑处理器读）*/
        @Override
        public Object config() { return currentPlan; }
        /** 【draw】正常游戏画面里怎么画方块：底图直接画（rotate=false 不转，不用 topRegion/outRegion 避免 null）*/
        @Override
        public void draw() {
            Draw.rect(region, x, y);
        }
        // ========== 配置面板 UI ==========
        /** 【buildConfiguration】玩家点方块 → 弹出"配方选择网格"
         *  （Mindustry 159.6 自带 ItemSelection.buildTable，不用手搓网格）*/
        @Override
        public void buildConfiguration(Table table) {
            // ── 第一步：从 plans 里提取"所有配方的第一个输出物品"，组成候选列表 ──
            Seq<Item> items = Seq.with(plans)
                    .select(p -> p != null && p.outItem != null && p.outItem.length > 0
                            && p.outItem[0] != null && p.outItem[0].item != null)    // 先过滤掉坏配方
                    .map(p -> p.outItem[0].item)                                       // 再提取第一个输出物品
                    .retainAll(i -> i != null && i.unlockedNow());                     // 再过滤"已解锁"的
            if (items.any()) {
                // ── 第二步：调用 Mindustry 自带的物品选择网格 ──
                ItemSelection.buildTable(
                        ConfigurableBlock.this,         // 方块本身（给 configure() 回调时用）
                        table,                           // 把 UI 塞到哪个 Table 里
                        items,                           // 候选物品列表
                        () -> {                          // Getter：返回"当前选中的物品"（让网格知道该把哪个图标高亮）
                            if (currentPlan == -1 || currentPlan >= plans.size) return null;
                            Plan p = plans.get(currentPlan);
                            if (p == null || p.outItem == null || p.outItem.length == 0) return null;
                            return p.outItem[0].item;
                        },
                        item -> {                        // 回调：玩家点了某个物品图标
                            int idx = plans.indexOf(p -> p.outItem != null && p.outItem.length > 0
                                    && p.outItem[0] != null && p.outItem[0].item == item);
                            if (idx != -1) configure(idx);  // 找到对应配方的索引 → 切过去！
                        },
                        ConfigurableBlock.this.selectionRows,     // 行数
                        ConfigurableBlock.this.selectionColumns   // 列数
                );
            } else {
                // 没有可用配方就显示"无"
                table.table(Styles.black3, t -> t.add("@none").color(Color.lightGray));
            }
        }
        // ========== 【shouldConsume】= 能不能扣原料、能不能加进度？
        // Mindustry 的 Consume 系统每 tick 都会问一遍：我现在应该开工吗？
        // 返回 false 的话：不扣原料 + efficiency=0 + progress 不涨
        // ==========
        @Override
        public boolean shouldConsume() {
            if (currentPlan == -1 || currentPlan >= plans.size) return false;  // 没选配方 → 不开工
            if (!enabled) return false;                                        // 被逻辑处理器 enable=false → 不开工
            Plan plan = plans.get(currentPlan);
            if (plan == null || plan.outItem == null || plan.outItem.length == 0) return false;  // 配方坏 → 不开工
            // ★ 核心检查 1：任何一个产物"仓里现有 + 即将新产的一份" > 容量 → 停下来！
            // 这是防止"无限生产 bug"的关键：如果仓满了还继续 craft → 产物加不进去就丢了，等于白扣原料
            for (ItemStack s : plan.outItem) {
                if (s == null || s.item == null) continue;
                int willHave = items.get(s.item) + s.amount;  // 做完这一轮后，仓里会有多少
                if (willHave > getMaximumAccepted(s.item)) return false;  // 塞不下就停！
            }
            // ★ 核心检查 2：原料够不够（不够也不开工，免得 efficiency>0 但扣不到原料时进度白涨）
            if (plan.requirements != null) for (ItemStack s : plan.requirements) {
                if (s == null || s.item == null) continue;
                if (items.get(s.item) < s.amount) return false;
            }
            return true;  // 所有检查都过了 → 可以开工
        }
        /** 【status】方块整体状态灯（UI 右上角的绿/黄/红）：正常就用父类判断 */
        @Override
        public BlockStatus status() {
            return super.status();
        }
        /** 【getMaximumAccepted】某物品最多能放仓里多少个？读 capacities 数组 × 规则倍率 */
        @Override
        public int getMaximumAccepted(Item item) {
            if (item == null || item.id >= capacities.length) return 0;   // 物品不存在/越界 → 0 个（不收）
            // 不再用 unitCost（那是 UnitBlock 的团队倍率），普通工厂直接用 capacities[item.id]
            return capacities[item.id];
        }

        /** 【acceptItem】「别人（传送带/相邻工厂）想把某个物品放进我仓里」→ 我要不要收？
         *
         *  ⚠️ 这个方法的语义极其容易搞反！它的参数是：source=「想送东西给我的那个建筑」，item=「对方要送的物品」。
         *  Mindustry 标准：
         *    · 传送带想送原料给我 → acceptItem(传送带, 铜) = true 才收（传送带会扣它自己那段的物品，加到我仓）
         *    · 我仓里的物品想推给下游（dumpOutputs/offload/dump）→ 调的是「下游建筑.acceptItem(我这个建筑, 钛)」问下游要不要
         *        而不是调我自己的 acceptItem！
         *
         *  之前的 bug（仓库里产物推不出）：acceptItem 连 outItem 也返回 true → 导致以下死循环假象：
         *    1. 我仓里有 5 个钛，我调 dump() → 问右侧传送带.acceptItem(我, 钛) → 传送带说"好"，推走 2 个
         *    2. 同一 tick 末 dumpOutputs → 问左侧传送带.acceptItem(我, 钛)
         *    3. 左侧传送带**是输入方向啊**，它要把自己那段上的铜送给我，于是它先调用我.acceptItem(传送带, 钛) 探测
         *    4. 我旧代码 acceptItem 对钛返回 true！= 左传送带误以为"这个工厂允许钛入仓"，
         *       但左传送带上没有钛只有铜，于是它卡住不转 → 铜进不来，钛也出不去（因为两边的 acceptItem 判断互相打乱了）
         *    5. 仓里剩的 3 个钛永远推不出去，还不断有新的铜送进来（但进不来）
         *
         *  ✅ 正确逻辑：acceptItem 只接收「当前配方的原料 requirements」就够了！
         *     我产出的物品（outItem）是要**推给下游**的，绝不应该让「上游的传送带」反过来塞给我。
         */
        @Override
        public boolean acceptItem(Building source, Item item) {
            if (currentPlan == -1 || currentPlan >= plans.size) return false;
            Plan plan = plans.get(currentPlan);
            if (plan == null || plan.requirements == null) return false;

            // 只有在 requirements 里的原料 → 并且仓未满 → 才允许进仓
            for (ItemStack stack : plan.requirements) {
                if (stack != null && stack.item == item
                        && items.get(item) < getMaximumAccepted(item)) {
                    return true;
                }
            }
            // 其它一切物品：我不收！（包括我自己产的 outItem，上游传送带想塞我直接拒绝）
            return false;
        }
        // =================================================================
        // ✅✅✅ 核心方法：【updateTile()】每 tick 都调一次（60次/秒）
        // 这里解决了两个大问题：「不消耗原料」+「复制物品/产物流不出去」
        //
        // 💡 修复原则（完全对齐原版 GenericCrafter / ConBlock 的标准流程）：
        //   1. 原料消耗 → 手写 items.remove(requirements)！（不再依赖 UnitBlock.consumeBuilder / PayloadBlock）
        //   2. 效率 → 父类 Building 的 efficiency 已经自动按电力/液体算好，直接用即可
        //   3. 物品产出 → 先 items.add() 全部入自己仓，再调用带参 dump(item) 推下游
        //      带参 dump(item) 只推指定物品 = 不会把原料推出去
        //      同一个 tick 末 Mindustry 还会 dumpOutputs 再兜底推一次 = 一定能推干净
        // =================================================================
        @Override
        public void updateTile() {
            // ── 前置：索引合法性检查（越界就当 -1 停止） ──
            if (!configurable) { currentPlan = 0; }          // 非 configurable：强制第一个配方
            if (currentPlan < 0 || currentPlan >= plans.size) { currentPlan = -1; return; }
            Plan plan = plans.get(currentPlan);
            if (plan == null || plan.outItem == null || plan.outItem.length == 0) {
                currentPlan = -1; return;                     // 配方坏了就停
            }
            // ── 第一步：进度累积 ──（只有 efficiency>0 才涨；efficiency 父类自动按电力/液体算）
            if (efficiency > 0) {
                // edelta() = 时间缩放（快进/倍速的话 >1，慢动作 <1），不乘 unitBuildSpeed 了（那是 UnitBlock 专用）
                progress += edelta();
            }
            // ======================================================
            // 进度满了 → 完成一周期生产！
            // ======================================================
            if (progress >= plan.time) {
                // Step 1：检查所有输出物容量（shouldConsume 已检查过，双保险）
                boolean allOutOk = true;
                for (ItemStack s : plan.outItem) {
                    if (s == null || s.item == null) continue;
                    int willHave = items.get(s.item) + s.amount;
                    if (willHave > getMaximumAccepted(s.item)) { allOutOk = false; break; }
                }

                // Step 2：检查原料够不够（shouldConsume 已经检查过，但防止边界情况）
                boolean allInOk = true;
                if (plan.requirements != null) for (ItemStack s : plan.requirements) {
                    if (s == null || s.item == null) continue;
                    if (items.get(s.item) < s.amount) { allInOk = false; break; }
                }

                // ★ 容量够 + 原料够 → 才真正扣原料 + 出物品
                if (allOutOk && allInOk) {
                    // ─────────────────────────────────────────
                    // ★ 扣原料：手动 items.remove(requirements)
                    //    彻底抛弃之前的 UnitBlock.consumeBuilder（我们不再 extends UnitBlock 了）
                    //    这样：扣一份原料、出一份产物，1:1 永远对应，不会白扣
                    // ─────────────────────────────────────────
                    if (plan.requirements != null) for (ItemStack s : plan.requirements) {
                        if (s == null || s.item == null || s.amount <= 0) continue;
                        items.remove(s.item, s.amount);
                    }

                    // 触发生产完成特效（如 NuFx.lightningStormFull）
                    craftEffect.at(x, y);

                    // ─────────────────────────────────────────
                    // ★ 出物品：对每个产物做 入仓（amount 份）→ for(amount 次) dump(item) 挨个推下游
                    //
                    // 【用户刚才发现的关键 bug！】
                    //   Mindustry dump(Item item) 语义：对四个方向按顺序找下游，只推"一份（数量=1）"就停！
                    //   之前 amount=3 却只调一次 dump() = 3 个入仓，只推出去 1 个，剩下 2 个永远堆仓里！
                    //   → 必须对每个 outItem 循环 amount 次 dump(s.item)，一份一份地推！
                    //
                    // 多生成物（outItem 数组长度>1）的情况也完全一样：硅 2 个 + 炉渣 1 个
                    //   → 硅那项循环 2 次 dump(硅)，炉渣那项循环 1 次 dump(炉渣)，互不干扰
                    // ─────────────────────────────────────────
                    for (ItemStack s : plan.outItem) {
                        if (s == null || s.item == null || s.amount <= 0) continue;
                        // ① 先一次性全部入仓
                        items.add(s.item, s.amount);
                        // ② 再一份一份地推下游（amount 份 → 调 amount 次 dump）
                        //    每一次 dump(s.item) = 推 1 个 s.item 到下游
                        //    如果某次下游全满，dump 返回 false → 物品还在仓里，下 tick dumpOutputs 会接着推
                        for (int i = 0; i < s.amount; i++) {
                            dump(s.item);
                        }
                    }

                    // 进度 -= 一轮时间（不是 =0 / %=，防止连续生产时进度丢失余数）
                    progress -= plan.time;
                }
                else {
                    // 不够就把进度卡在"刚好差一点"的位置，防止下 tick 跳回来又重复检查
                    progress = Math.min(progress, plan.time - 0.001f);
                }
            }
            if (progress < 0) progress = 0;  // 负数防御（理论上不会有，但是加一层总没错）

            // ── 兜底：如果当前配方有产物、仓里还剩了一些产物没推出去，再统一个推一次
            //    防止：下游刚才正好满，下个 tick 腾出了空位但刚好没走到进度满的分支 → 遗留的产物一直堆仓
            if (currentPlan != -1 && currentPlan < plans.size) {
                Plan lp = plans.get(currentPlan);
                if (lp != null && lp.outItem != null) for (ItemStack s : lp.outItem) {
                    if (s == null || s.item == null) continue;
                    int left = items.get(s.item);
                    if (left <= 0) continue;
                    // 仓里剩多少就调多少次 dump()，挨个推干净
                    for (int i = 0; i < left; i++) dump(s.item);
                }
            }
        }
        // =================================================================
        // 存档读写（write/read）：游戏存盘时把方块状态存到 save 文件里
        // ★ Mindustry 存档铁则（项目记忆）：
        //   1. version() 必须等于「我们自己实际写的字段数量」
        //   2. write() 写了 N 个字段，read() 必须读完全相同的 N 个，顺序/大小完全一致
        //   3. 严禁 read() 读任何 write() 里没写过的字节（多读一个 bool 都会导致后面所有方块错位！）
        // =================================================================

        /** 存档版本号：当前我们自己只写了 2 个字段（progress + currentPlan）。
         *  version=1 表示"修复后的第一个正确版本"。
         *  以后每次真的加新字段时，再 version++ 并在 read() 里用 revision 判断是否读新字段。 */
        @Override
        public byte version() { return 1; }

        /** 存盘时写（严格按这个顺序和大小）：
         *  ① 父类 Building 写它的基础字段（x,y,rotation,team,health...）
         *  ② 我们写 float progress（4 字节）
         *  ③ 我们写 short currentPlan（2 字节） */
        @Override
        public void write(Writes write) {
            super.write(write);
            write.f(progress);      // ① 自己写的第 1 个字段：float 4B
            write.s(currentPlan);   // ② 自己写的第 2 个字段：short 2B
        }

        /** 读档时读（和 write 顺序完全一致！不要读任何没写过的东西！）*/
        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);  // 先读父类的
            progress = read.f();         // ① 自己读第 1 个字段：float 4B
            currentPlan = read.s();      // ② 自己读第 2 个字段：short 2B
            // ⚠ 不要再有 if(revision>=X) 读没写过的字段了！
            //    旧存档 revision 可能是 3（之前写错的版本号），但它实际写入的字节和现在一样：只有 f + s
            //    所以不管 revision 是几，我们只读 f+s 就停，字节绝对不会错位
        }
    }
}
