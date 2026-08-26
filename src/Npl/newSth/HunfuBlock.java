// ╔══════════════════════════════════════════════════════════════════════════╗
// ║                      📖 @Override 终极速查表（必读）                     ║
// ╠══════════════════════════════════════════════════════════════════════════╣
// ║                                                                          ║
// ║ ① 什么是 @Override ?                                                     ║
// ║   ├─ 它就是一个"标签"：告诉编译器「我下面这个方法，是想覆写父类/接口的」  ║
// ║   ├─ 如果父类根本没有这个方法（或签名写错了）→ 编译直接报错！              ║
// ║   └─ 好处：① 防止你拼错方法名  ② 父类改了签名时编译器会提醒你改          ║
// ║                                                                          ║
// ║ ② HunfuBlock 有「两层」！每层 @Override 的对象完全不同！！！               ║
// ║   ┌──────────────────────────┬─────────────────────────────────────┐    ║
// ║   │       你写在哪一层？      │        @Override 覆写谁的方法？      │    ║
// ║   ├──────────────────────────┼─────────────────────────────────────┤    ║
// ║   │ class HunfuBlock         │ 覆写 mindustry.world.Block 的方法    │    ║
// ║   │   extends Block          │ （方块"种类"的方法，所有实例共用）    │    ║
// ║   ├──────────────────────────┼─────────────────────────────────────┤    ║
// ║   │ class HunfuBuild         │ 覆写 mindustry.gen.Building 的方法   │    ║
// ║   │   extends Building       │ （地图上"单个方块实例"的行为方法）    │    ║
// ║   └──────────────────────────┴─────────────────────────────────────┘    ║
// ║                                                                          ║
// ║ ③ 本文件实际用到的方法分层对照表（✅=写这层才对，❌=放这层必报错）         ║
// ║   ┌────────────────────┬──────────────────┬──────────────────┐          ║
// ║   │      方法名         │ HunfuBlock 层    │ HunfuBuild 层    │          ║
// ║   │                    │ (extends Block)  │ (extends Building)│          ║
// ║   ├────────────────────┼──────────────────┼──────────────────┤          ║
// ║   │ init()             │        ✅        │        ❌        │          ║
// ║   │ afterPatch()       │        ✅        │        ❌        │          ║
// ║   │ checkContentArray… │        ✅        │        ❌        │          ║
// ║   │ load()             │        ✅ 贴图    │        ❌        │          ║
// ║   │ setBars()          │        ✅ 进度条  │        ❌        │          ║
// ║   │ outputsItems()     │        ✅        │        ❌        │          ║
// ║   │ icons()            │        ✅ 图标    │        ❌        │          ║
// ║   │ drawPlanRegion()   │        ✅ 蓝图    │        ❌        │          ║
// ║   │ setStats()         │        ✅ 详情面板│        ❌        │          ║
// ║   │ getPlanConfigs()   │        ✅ 配方候选│        ❌        │          ║
// ║   ├────────────────────┼──────────────────┼──────────────────┤          ║
// ║   │ created()          │        ❌        │        ✅ 初始化  │          ║
// ║   │ drawSelect()       │        ❌        │        ✅ 选中框  │          ║
// ║   │ draw()             │        ❌        │        ✅ 绘制    │          ║
// ║   │ drawLight()        │        ❌        │        ✅ 发光层  │          ║
// ║   │ acceptLiquid()     │        ❌        │        ✅ 接液体  │          ║
// ║   │ acceptItem()       │        ❌        │        ✅ 接物品  │          ║
// ║   │ acceptPayload()    │        ❌        │        ✅ 接载荷  │          ║
// ║   │ getMaximumAccepted │        ❌        │        ✅ 容量    │          ║
// ║   │ shouldConsume()    │        ❌        │        ✅ 开工?   │          ║
// ║   │ updateTile()       │        ❌        │        ✅ 每tick  │          ║
// ║   │ status()           │        ❌        │        ✅ 状态灯  │          ║
// ║   │ config()           │        ❌        │        ✅ 读配置  │          ║
// ║   │ buildConfiguration │        ❌        │        ✅ 配置UI  │          ║
// ║   │ display()          │        ❌        │        ✅ 详情UI  │          ║
// ║   │ sense()            │        ❌        │        ✅ 逻辑数  │          ║
// ║   │ senseObject()      │        ❌        │        ✅ 逻辑物  │          ║
// ║   │ version()          │        ❌        │        ✅ 存档版  │          ║
// ║   │ write(Writes)      │        ❌        │        ✅ 写存档  │          ║
// ║   │ read(Reads,byte)   │        ❌        │        ✅ 读存档  │          ║
// ║   │ warmup()           │        ❌        │        ✅ 升温    │          ║
// ║   │ totalProgress()    │        ❌        │        ✅ 总进度  │          ║
// ║   └────────────────────┴──────────────────┴──────────────────┘          ║
// ║                                                                          ║
// ║ ④ 必报编译错「不会覆盖或实现超类型的方法」的 3 种原因：                   ║
// ║    ❌ 原因 A：方法放错层了！（最常见！）                                   ║
// ║         → 比如把 load() 写进 HunfuBuild → Building 没 load() → 报错！    ║
// ║    ❌ 原因 B：方法签名写错了！                                             ║
// ║         → 父类是 drawPlanRegion(BuildPlan, Eachable<BuildPlan>)          ║
// ║         → 你写成 drawPlanRegion(BuildPlan plan) → 少参数 → 报错！        ║
// ║    ❌ 原因 C：返回值类型不匹配！                                           ║
// ║         → 父类 icons() 返回 TextureRegion[]                               ║
// ║         → 你写成 return drawer.finalIcons(); 但它返回的是 TextureRegion   ║
// ║         → 不是数组 → 报错！                                               ║
// ║                                                                          ║
// ║ ⑤ 3 秒口诀（以后拿不准就默念这 3 句）：                                    ║
// ║   口诀 1：「贴图/图标/蓝图/统计 → Block 层」(load/icons/drawPlanRegion)  ║
// ║   口诀 2：「画/每 tick/接东西/UI  → Building 层」(draw/updateTile/…)     ║
// ║   口诀 3：「放错层必报错，签名差一个字也必报错」                           ║
// ║                                                                          ║
// ║ ⑥ 记忆小贴士：                                                           ║
// ║   ├─ Block 层方法 = 描述"这个方块种类长什么样/能做什么"                  ║
// ║   │    （只在加载时调用一次，和地图上放了多少个实例无关）                  ║
// ║   └─ Building 层方法 = 描述"这台具体的机器此刻在干什么"                  ║
// ║        （每 tick 调用，每台机器都有自己的进度、物品、效率）                ║
// ║                                                                          ║
// ╚══════════════════════════════════════════════════════════════════════════╝

// ———【包声明】————————————————————————————————————————————
// 这个文件属于 NuclearPowerLeak 模组的 Npl.newSth 子包（存放你新加的自定义方块类）
package Npl.newSth;

// ———【Java / Arc 引擎 / Mindustry 依赖导入】————————————————
// 每个 import 的含义：
//  1) arc.*            = Arc 引擎顶层包（Core/Core.files 这些静态入口）
//  2) arc.audio.*      = 声音（Sound 类）
//  3) arc.graphics.*   = 颜色(Color)、纹理(Texture/Pixmap)、Pixmap.Format
//  4) arc.graphics.g2d.*= 2D 绘制（Draw / TextureRegion / Fill / Lines 等）
//  5) arc.math.*       = Mathf 数学工具（approachDelta / clamp / 随机）
//  6) arc.scene.style.*= UI 皮肤（TextureRegionDrawable / Icon 图标）
//  7) arc.scene.ui.*   = UI 组件（Label / ImageButton / Image）
//  8) arc.scene.ui.layout.*= Table 布局（你看到的所有面板、进度条都是它拼的）
//  9) arc.struct.*     = Mindustry 自己的集合（Seq<Plan> 有序列表）
// 10) arc.util.*       = 工具（Strings 格式化、Time.delta 时间步、Tmp 颜色复用）
// 11) arc.util.io.*    = Writes / Reads（存档二进制读写）
// 12) mindustry.world.draw.* = DrawBlock 体系（DrawDefault / DrawMulti / DrawRegion / DrawFlame 等）
// 13) mindustry.*      = 顶层入口（Vars.content / Vars.logic）
// 14) mindustry.ai.*   = 路径规划（暂时没用，import 预留）
// 15) mindustry.ctype.*= 内容类型基类（UnlockableContent：Item/Liquid 都继承它，配方面板选图标要它）
// 16) mindustry.entities.*= 世界实体（Effect 特效、Mech 等，暂时留 import 占位）
// 17) mindustry.entities.units.*= BuildPlan（编辑器 / 蓝图里"还没放下去的方块计划"，drawPlanRegion 要用）
// 18) mindustry.game.EventType.*= 事件监听（暂时未用，留着以后接 TapEvent/BlockBuildEvent）
// 19) mindustry.gen.*  = 代码生成的接口（Building / Unit / Player 都是它的子接口）
// 20) mindustry.graphics.*= Palette 调色板（Pal.ammo 进度条橙色）/ Drawf 绘制辅助
// 21) mindustry.io.*   = SaveFileReader 等存档工具（暂时未直接用）
// 22) mindustry.logic.*= LAccess：逻辑处理器 "sense 感知" 枚举（progress/itemCapacity 等）
// 23) mindustry.type.* = Item / Liquid / ItemStack / LiquidStack / Bar 这些类型
// 24) mindustry.ui.*   = Styles / StatValues / Tex：UI 样式 & 统计面板辅助
// 25) mindustry.content.* = Fx 特效库（Fx.lava 合成特效、Fx.formlessHit 等）
// 26) mindustry.world.blocks.* = Block 体系（Building 父接口都在这里）
// 27) mindustry.world.consumers.* = ConsumeItem/ConsumeLiquid 消费者系统（暂时走自定义 shouldConsume 没用上）
// 28) mindustry.world.meta.* = Stat 统计面板分类（output / itemCapacity）/ BlockStatus 方块状态灯
// 29) mindustry.world.Block = ⭐ 你真正要继承的父类（所有自定义方块的基类）
// 30) java.util.*       = JDK 集合（Arrays.copyOf / Arrays.fill / HashMap...）
// 31) Npl.newSth.Type.coins = 你自己写的 coins 全局单例（读/加/扣 coins）
// 32) static mindustry.Vars.*= 静态导入：后面直接用 Vars.content / Vars.state 省得再写前缀
import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.world.draw.*;
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
import mindustry.content.*;
import mindustry.world.blocks.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.world.Block;
import java.util.*;

import Npl.newSth.Type.coins;
import Npl.content.NuItems;   // ⭐ 配方面板里要引用 NuItems.coinsItem（纯产 coins 配方的按钮图标用它）

import static mindustry.Vars.*;

/**
 * ============================================================
 * 【HunfuBlock = 混成（物品 + 液体 + coins）多配方工厂】
 *  在原本「物品+液体双料多配方」基础上，新增两个 coins 接口：
 *
 *    Plan.coinCost   (int) ：每轮合成「消耗」的 coins 数量
 *    Plan.coinOutput (int) ：每轮合成「生产」的 coins 数量
 *
 *  两者可以单独用、也可以同时用（比如消耗 10 coins + 物品，产出另一种物品 + 3 coins）。
 *  其他完全保留原版 HunfuBlock 的功能：
 *    - Plan 数组 plans（多配方、UI 切换、整数/物品 config）
 *    - capacities[] / liquidCapacities[] 自动计算
 *    - shouldConsume 物品+液体+coins 三检查
 *    - updateTile() 扣原料+扣 coins+出产物+出 coins+兜底 dump
 * ============================================================
 * 使用示例（NuBlocks.java）：
 *
 *   // ① 消耗 coins 出物品（和之前 CoinConsumerBlock 多配方等价）
 *   hunfu = new HunfuBlock("hunfu") {{
 *       requirements(Category.crafting, with(...));
 *       size = 2; health = 800;
 *       plans = Seq.with(
 *           // 构造器：(outItem, time, requirements, outLiquid, inLiquid, coinCost, coinOutput)
 *           // 这里只用 coinCost：消耗 15 coins，出 100 个 bigIron，耗时 10 分钟
 *           new Plan(with(NuItems.bigIron, 100), 60f*60*10, null, null, null, 15, 0)
 *       );
 *   }};
 *
 *   // ② 消耗物品出 coins（反过来，比如把 100 个 magent 换成 120 coins）
 *   hunfu.plans.add(new Plan(null, 60f*30, with(NuItems.magent, 100), null, null, 0, 120));
 *
 *   // ③ 方便的四参数构造：(outItem, time, requirements, coinCost)
 *   hunfu.plans.add(new Plan(with(Items.graphite, 100), 60f*20, null, 80));
 *
 *   // ④ 方便的五参数构造：(outItem, time, requirements, coinCost, coinOutput)
 *   hunfu.plans.add(new Plan(with(NuItems.sulFurFrag, 100), 60f*16, with(Items.coal, 20), 40, 5));
 */
public class HunfuBlock extends Block {

    // ========== 字段区：本方块"整体级别的配置" ==========
// 这些是"HunfuBlock 这个类（= 所有同种方块共用的那份描述）"的字段，
// 不是地图上单个方块实例（HunfuBuild）的字段。类比："螺丝这个种类的生产上限"
// capacities[item.id] = 每个 HunfuBuild 实例能装多少个"item.id 对应的物品"
public int[] capacities = {};
// liquidCapacities[liquid.id] = 每个 HunfuBuild 实例能装多少"liquid.id 对应的液体"
public float[] liquidCapacities = {};
// plans = 所有配方（Plan 类就是配方）；每个方块可以有多个配方，用户在 UI 点选切换
public Seq<Plan> plans = new Seq<>(4);
// selectionRows = 配方面板 UI 网格列数（留着给 buildConfiguration 用，后面有遍历）
public int selectionRows = 2;
// craftEffect = 每合成满一轮，出产物时在方块上放的特效
public Effect craftEffect = Fx.none;
// selectionColumns = 配方面板 UI 网格列数（每满一行就 table.row() 换行）
public int selectionColumns = 4;
// ⭐ drawer = 这个方块"怎么画自己"的绘制器（Block 类的 Block.drawer 字段同名，但我们在类里显式再声明一次）
//    Block.drawer / Building.draw / Block.drawPlanRegion / Block.icons 四个位置都通过它做统一调度
public DrawBlock drawer = new DrawDefault();

// ========== 构造器：方块注册阶段调用 ==========
// 作用：NuBlocks.java 里写 new HunfuBlock("exchange") {{ ... }} 时，先走构造器，再走双括号初始化块。
//       这里设置的是"这个方块种类的默认属性"（大小、血量、是否需要电、有没有物品槽等）。
public HunfuBlock(String name) {
    // ⭐ 调用 Block(String name) 父构造器：把方块注册进内容系统；region/regions 自动以 name 为键找贴图。
    //   父类就是：mindustry.world.Block
    super(name);
    // update = true：告诉 Mindustry "这个方块每 tick 都要调 updateTile()（合成逻辑写在里面）"
    update = true;
    // solid = true：这个方块是实心的（单位不能踩过去、传送带不能穿）
    solid = true;
    // hasItems = true：有物品槽（items.get / items.remove 这些接口才会存在）
    hasItems = true;
    // hasPower = true：接电线才工作（后面会 consumePower）
    hasPower = true;
    // hasLiquids = true：有液体槽（liquids 接口才存在）
    hasLiquids = true;
    // consumesPower = true：声明为消耗电力（Mindustry 会给它在消费者系统里注册）
    consumesPower = true;
    // craftEffect：合成完成时播放的特效（默认 Fx.lava，熔岩喷发 → 工厂冒岩浆那种感觉）
    craftEffect = Fx.lava;
    // size = 2：占地 2×2 tiles（默认 1×1；冲击钻头是 3×3 那种）
    size = 2;
    // health = 100：基础血量（100 很脆，具体实例一般会在 NuBlocks 初始化里改大）
    health = 100;
    // rotate = false：不支持旋转（不像传送带/炮塔可以改变朝向）
    rotate = false;
    // configurable = true：可以被玩家点击弹出配置（= 配方面板）、也能被逻辑处理器 config(Integer) 切换
    configurable = true;
    // itemCapacity = 30：默认每个物品槽 30（后面 initCapacities 会按配方重新算，这是兜底）
    itemCapacity = 30;
    // liquidCapacity = 30：默认每种液体 30（同样兜底，实际容量在 initCapacities 里重算）
    liquidCapacity = 30f;

    // ① 整数索引：逻辑处理器切配方
    //    config(Integer.class, handler) 是 Block 自带的"配置反序列化 / 处理器发包 → 实例执行"回调登记。
    //    参数：build = 地图上具体这台方块实例（HunfuBuild），i = 用户发来的整数（plans 的下标）
    config(Integer.class, (HunfuBuild build, Integer i) -> {
        // configurable = false 时（可被逻辑/脚本关掉）就不让切
        if (!configurable) return;
        // 选的就是当前配方，无事发生直接返回（省得清进度、重 dump）
        if (build.currentPlan == i) return;
        // 把 i 规范化到 [-1, plans.size)：-1 代表"没选任何配方"
        build.currentPlan = i < 0 || i >= plans.size ? -1 : i;
        // 切换配方必须重置进度（防止"半桶石墨配方的进度"被拿来出硅）
        build.progress = 0;
        // dump() = 把 build.items 里所有能吐的物品都从接物口吐出去（清旧原料）
        build.dump();
        // 同样清空所有液体：遍历全部液体种类 dumpLiquid 各一次
        for (Liquid l : Vars.content.liquids()) build.dumpLiquid(l);
    });
    // ② 物品对象：UI 配方图标点选
    //    用户在配置面板点了一个"物品图标" → 执行下面的 handler，把它匹配成 plans 下标再复用整数配置
    config(Item.class, (HunfuBuild build, Item item) -> {
        if (!configurable) return;
        // 先按原版规则：找「第一个输出物品就是 item」的配方
        int next = plans.indexOf(p -> p.outItem != null && p.outItem.length > 0
                && p.outItem[0] != null && p.outItem[0].item == item);
        // ★ 没匹配到 且 item 正好是 NuItems.coinsItem → 按「第一个纯产 coins 配方」匹配
        //   （对应 coinProducer 这种 3 个都是「出 coins + 消耗原料」的方块）
        if (next < 0 && NuItems.coinsItem != null && item == NuItems.coinsItem) {
            next = plans.indexOf(p -> p != null && p.coinOutput > 0);
        }
        if (build.currentPlan == next) return;
        build.currentPlan = (next < 0 || next >= plans.size) ? -1 : next;
        // 同样重置进度、清原料
        build.progress = 0;
        build.dump();
        for (Liquid l : Vars.content.liquids()) build.dumpLiquid(l);
    });
    // ③ 清空
    //    configClear(handler) 是当"配置面板点重置"或"处理器发 0 空包"时回调：
    configClear((HunfuBuild build) -> {
        build.currentPlan = -1;       // 选一个不存在的配方 → 停摆
        build.progress = 0;           // 进度清 0
    });
}

// =================================================================
// init / afterPatch / initCapacities
// =================================================================
// ⭐⭐⭐ 讲解 @Override 第一件事：看到 @Override 先去想——"我在哪个类？这个类的父类是谁？"
//   1) HunfuBlock extends Block → 这里的 @Override 覆写 mindustry.world.Block.init()
//   2) Block.init() 是"所有方块注册完、进入世界加载前"被 Mindustry 内容系统统一调用的
//   3) 为什么要覆写？因为我们有 capacities[] / liquidCapacities[] 两个数组，需要先知道"有多少物品/液体"
//      才能正确分配长度 → 只有在 init 时 content.items() / content.liquids() 才全部加载完。
// ⭐ 规则：@Override 必须和父类方法签名完全一致（方法名、参数列表、返回值、泛型都要一样），
//   否则编译报错"不会覆盖或实现超类型的方法"。
@Override
public void init() {
    // 先算容量（根据所有 plans 里的物品/液体最大值分配）
    initCapacities();
    // 调父类的 init()：Mindustry 会在这里做 loads 贴图 / 统计面板初始化 / 建筑图标缓存
    super.init();
}
// ⭐ @Override Block.afterPatch()
//   afterPatch() = "MOD 加载全部结束 + 所有内容补丁（比如把某个方块血量改大的 mod）都打完之后"再跑一次
//   为什么还要再算一次？因为可能有别的 MOD 在 afterPatch 里给 plans 追加了新配方、或加了更多物品/液体，
//   这时数组长度要重新 copyOf 对齐、容量也要重新算。
@Override
public void afterPatch() {
    initCapacities();
    super.afterPatch();
}

// initCapacities()：自己写的工具方法（没有 @Override，因为 Block 里没有这个方法，是 HunfuBlock 专属的）
//   作用：遍历所有配方的 in/out 物品/液体最大值，×10 之后作为"该方块里每一种物品/液体容量上限"
public void initCapacities() {
    // 新建物品容量数组，长度 = 当前注册的物品总数（item.id 就是下标）
    capacities = new int[Vars.content.items().size];
    int maxItem = 0;
    // 新建液体容量数组，长度 = 当前注册的液体总数
    liquidCapacities = new float[Vars.content.liquids().size];
    float maxLiquid = 0f;

    // 遍历每一份配方，找"单轮合成涉及到的最多物品数/液体数"
    for (Plan plan : plans) {
        if (plan == null) continue;
        // 原料物品：找 stack.amount 最大的那个
        if (plan.requirements != null) for (ItemStack stack : plan.requirements)
            if (stack != null && stack.amount > maxItem) maxItem = stack.amount;
        // 产物物品：也用最大的 stack.amount 当指标（否则出产物会爆仓）
        if (plan.outItem != null) for (ItemStack stack : plan.outItem)
            if (stack != null && stack.amount > maxItem) maxItem = stack.amount;
        // 原料液体
        if (plan.inLiquid != null) for (LiquidStack stack : plan.inLiquid)
            if (stack != null && stack.amount > maxLiquid) maxLiquid = stack.amount;
        // 产物液体
        if (plan.outLiquid != null) for (LiquidStack stack : plan.outLiquid)
            if (stack != null && stack.amount > maxLiquid) maxLiquid = stack.amount;
    }

    // 统一物品容量：至少能装 1 个（否则容量 0 没法开工），默认给单轮最大值 × 10 余量
    int unifiedItem = Math.max(1, maxItem * 10);
    // Arrays.fill：把 capacities 这整个数组每一格都填成 unifiedItem（所有物品容量一致）
    Arrays.fill(capacities, unifiedItem);
    // 统一液体容量：同理，至少 1 单位余量
    float unifiedLiquid = Math.max(1f, maxLiquid * 10f);
    Arrays.fill(liquidCapacities, unifiedLiquid);
}

// ⭐ @Override Block.checkContentArrayCapacity(int items, int liquids)
//   Mindustry 在"加载 MOD 时先后扩展了物品/液体数量"时会调用：告诉每个方块"现在物品数变 items / 液体数变 liquids，
//   你自己的内部数组如果是按 id 下标的，赶紧扩容，不然会 ArrayIndexOutOfBounds。"
//   因为我们有 capacities[] / liquidCapacities[] 这种按 item.id 当数组下标的，必须响应。
@Override
public void checkContentArrayCapacity(int items, int liquids) {
    // 父类自己的数组（比如 Block.itemCapacity 等）先处理
    super.checkContentArrayCapacity(items, liquids);
    // 如果现在 capacities 长度和 Mindustry 通知的新长度不一样 → Arrays.copyOf 扩容/缩容
    if (capacities.length != items) capacities = Arrays.copyOf(capacities, items);
    if (liquidCapacities.length != liquids) liquidCapacities = Arrays.copyOf(liquidCapacities, liquids);
}

// =================================================================
// DrawBlock 渲染层：load / drawPlanRegion / icons 属于 Block（不是 Building）
//   —— 这里 this 是 HunfuBlock(Block)，匹配 DrawBlock.load(Block)/drawPlan(Block,...)/finalIcons(Block)。
//   draw / drawLight 在 HunfuBuild 里（Building 有这俩，接受 Building 参数）。
//
// ⭐⭐⭐ @Override 规则总结 1："方法归属（是 Block 的方法还是 Building 的方法）"
//   在"类 HunfuBlock extends Block"这一层，能 @Override 的是 Block 类里声明的方法：
//     init / afterPatch / checkContentArrayCapacity / load / setBars / outputsItems
//     icons / drawPlanRegion / setStats / getPlanConfigs / setBars
//   在"内部类 HunfuBuild extends Building"那一层，能 @Override 的是 Building 接口方法：
//     created / drawSelect / acceptPayload / draw / drawLight / acceptLiquid
//     senseObject / sense / display / config / buildConfiguration / shouldConsume / status
//     getMaximumAccepted / acceptItem / updateTile / version / write / read / warmup / totalProgress
//   放错层！！父类里没有这个方法 → 编译直接报"不会覆盖或实现超类型的方法"（这就是你之前遇到的 7 个错误根因）。
// =================================================================
// ⭐ @Override Block.load()
//   Block.load() 是"贴图加载阶段"调用的：把 blockName.png、blockName-team.png 这类贴图读进 TextureRegion。
//   为什么覆写？因为我们加了自定义 drawer（比如 DrawMulti / DrawRegion），DrawBlock 自己可能还要加载
//   "-top / -bottom / -glow" 这些额外后缀的贴图，必须显式调 drawer.load(this) 让它加载。
@Override
public void load(){
    // 先父类：把默认 region、teamRegion 这些基础贴图读进来
    super.load();
    // 再让 DrawBlock 子类自己加载额外后缀：DrawRegion("-top") 会在这里找 "blockName-top.png"
    drawer.load(this);
}

// =================================================================
// UI 相关：进度条 / 图标 / 详情面板 / 候选列表
// =================================================================
// ⭐ @Override Block.setBars()
//   setBars = "方块被放下去后，方块详情面板顶部的 2~3 条进度条"（最常见的就是 health 血条、power 电力条）
//   为什么覆写？因为我们想要"合成进度条"（当前配方进度 0..1）展示给玩家看。
@Override
public void setBars() {
    // 父类先跑：添加 health / power / warmup 这些默认条
    super.setBars();
    // addBar(名字, 把 Building e 转成 Bar 的函数)
    //   e 强转成 HunfuBuild（因为我们只会把 HunfuBlock 这种方块放下去）
    //   Bar(翻译 key="bar.progress"="进度", 颜色=Pal.ammo 弹药橙, 进度函数=e::fraction 方法引用)
    addBar("progress",
            (HunfuBuild e) -> new Bar(
                    "bar.progress",
                    Pal.ammo,
                    e::fraction
            ));
}

// ⭐ @Override Block.outputsItems()
//   outputsItems：Mindustry 传送带 / 路由器判断"这个方块会不会出物品、能不能接接料端"
//   旧的父类 Block.outputsItems() 会看"有没有 Consumer 系的产物品设置"，但我们走自定义 plans，
//   所以强制返回 true：告诉外部系统这个方块会出物品。
@Override
public boolean outputsItems()   { return true; }
// outputsLiquids：不是 Block 基类必须覆盖的方法（没有 @Override，父类也没有同名方法），
//   只是给你自己用的标识方法——可以删掉也可以保留，不影响编译。
public boolean outputsLiquids() { return true; }

// ⭐ @Override Block.icons()
//   icons()：返回的是内容面板 / 方块选择面板 / 地图缩略图里显示的图标 TextureRegion[]（一般是 1 个）
//   父类 Block.icons() 默认把 "blockName.png" 那一块贴图像素返回。
//   为什么覆写？因为我们的 drawer 可能是 DrawMulti（主图+上叠层），想让图标也是同样的叠层效果，
//   就必须走 drawer.finalIcons(this)（它会把 DrawMulti 里每一层的贴图都按顺序画出来）。
@Override
public TextureRegion[] icons() {
    return drawer.finalIcons(this);
}

// ⭐ @Override Block.drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list)
//   drawPlanRegion：用户在编辑器/蓝图模式下，"鼠标悬着还没点下去 / 正在显示计划"时，方块怎么画。
//   plan = 当前要画的那个放置计划（含 drawx/drawy / 旋转角度）
//   list = 同一条放置序列里其他的计划（用来和相邻的墙做"连墙"绘制，这里用不上但必须传进去）
//   父类默认 Draw.rect(region, plan.drawx(), plan.drawy())：只画主图一张
//   为什么覆写？和 icons 一样 —— DrawMulti / DrawRegion 的叠层要在蓝图预览里也同步显示。
@Override
public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {
    drawer.drawPlan(this, plan, list);
}

// ⭐ @Override Block.setStats()
//   setStats = "方块详情（点一下内容库那个图标弹出的面板）里左边那一列统计项"
//   为什么覆写？原版 Block 默认给你加 itemCapacity / liquidCapacity 这两行，但我们的容量是按配方自动算的、
//   每一种物品/液体容量值都一样，父类那两行没有意义，所以先 remove 默认项，再加一个"所有配方的详细表"。
@Override
public void setStats() {
    // 父类先跑：把功率、耗时、血量等基础统计项加上
    super.setStats();
    // 去掉父类自动加的"物品容量（每格多少）"单行（已经被 initCapacities 统一了）
    stats.remove(Stat.itemCapacity);
    // 去掉父类自动加的"液体容量"单行
    stats.remove(Stat.liquidCapacity);

    // stats.add(Stat.output, table -> { ... }) = 往"输出"这个大类里塞一张自定义 UI 表
    stats.add(Stat.output, table -> {
        // 每个配方占一行（先换行，避免和父类已有的输出项贴一起）
        table.row();
        // 遍历所有计划，给每个配方画一张灰底小卡片
        for (Plan plan : plans) {
            if (plan == null) continue;
            // 取"第一个输出物品 / 第一个输出液体"（给小卡片左侧当图标 + 标题用）
            ItemStack firstOutItem = (plan.outItem   != null && plan.outItem.length   > 0) ? plan.outItem[0]   : null;
            LiquidStack firstOutLiq = (plan.outLiquid != null && plan.outLiquid.length > 0) ? plan.outLiquid[0] : null;
            // ★ hasAnyOut：允许只有 coinOutput 没有物品/液体输出的"消耗原料产 coins 配方"也出现在详情里
            boolean hasAnyOut = (firstOutItem != null && firstOutItem.item != null)
                             || (firstOutLiq  != null && firstOutLiq.liquid != null)
                             || (plan.coinOutput > 0);
            if (!hasAnyOut) continue;

            // table(Styles.grayPanel, t -> { ... })：在大表内再嵌一个灰色底板小卡片
            table.table(Styles.grayPanel, t -> {
                // ── 左：配方代表图标 + coins 标签 ──
                t.table(icons -> {
                    icons.left();
                    if (firstOutItem != null && firstOutItem.item != null) {
                        // 有输出物品：画物品的 40×40 UI 图标，悬浮提示就是这个物品的 tooltip
                        icons.image(firstOutItem.item.uiIcon).size(40).pad(10f).scaling(Scaling.fit)
                                .with(i -> StatValues.withTooltip(i, firstOutItem.item));
                    } else if (firstOutLiq != null && firstOutLiq.liquid != null) {
                        // 有输出液体：画液体的 UI 图标（瓶装液体那张）
                        icons.image(firstOutLiq.liquid.uiIcon).size(40).pad(10f).scaling(Scaling.fit)
                                .with(i -> StatValues.withTooltip(i, firstOutLiq.liquid));
                    } else if (plan.coinOutput > 0) {
                        // 纯产 coins 配方：没有现成图标，用 COINS 黄绿色占位文字
                        icons.add("COINS").color(Color.valueOf("E2FF6D")).pad(10f);
                    }
                    // 统计"还有多少个其他输出"，在图标后面加个 "+N"
                    int outCount = 0;
                    if (plan.outItem   != null) outCount += plan.outItem.length;
                    if (plan.outLiquid != null) outCount += plan.outLiquid.length;
                    if (outCount > 1) icons.add(" +" + (outCount - 1)).color(Color.lightGray).padLeft(2f);
                    icons.row();
                    // ★ coins 消耗：在图标下方显示"X coins/craft"红色小字
                    if (plan.coinCost > 0) {
                        icons.add(plan.coinCost + " coins/craft").color(Color.valueOf("FF6F6F")).padTop(2f).left();
                    }
                    // ★ coins 产出：在图标下方显示"X coins+"黄绿色小字
                    if (plan.coinOutput > 0) {
                        icons.add(plan.coinOutput + " coins+").color(Color.valueOf("E2FF6D")).padTop(2f).left();
                    }
                }).left();
                // ── 中：名字 + 耗时 ──
                t.table(info -> {
                    // 标题栏：输出物品名 / 输出液体名 / "硬币 (coins)"
                    if (firstOutItem != null && firstOutItem.item != null)
                        info.add(firstOutItem.item.localizedName).left();
                    else if (firstOutLiq != null && firstOutLiq.liquid != null)
                        info.add(firstOutLiq.liquid.localizedName).left();
                    else if (plan.coinOutput > 0)
                        info.add("硬币 (coins)").left();
                    // 统计 inCount / outC 数量，给"(物N/液N)"小字提示
                    int inCount = 0, outC = 0;
                    if (plan.requirements != null) inCount += plan.requirements.length;
                    if (plan.inLiquid     != null) inCount += plan.inLiquid.length;
                    if (plan.outItem      != null) outC    += plan.outItem.length;
                    if (plan.outLiquid    != null) outC    += plan.outLiquid.length;
                    if (inCount > 1 || outC > 1)
                        info.add(" (物" + inCount + "/液" + outC + ")").color(Color.lightGray).padLeft(4f);
                    info.row();
                    // plan.time 单位 = tick（60 tick = 1 秒）→ 除以 60 变成秒，保留 1 位小数
                    info.add(Strings.autoFixed(plan.time / 60f, 1) + " " + Core.bundle.get("unit.seconds"))
                            .color(Color.lightGray);
                }).left().padLeft(10f);

                // ── 右：所有原料（物品+液体）网格 4 列 ──
                t.table(req -> {
                    req.right();
                    int idx = 0;
                    // 所有原料物品：每 4 个换一行
                    if (plan.requirements != null) for (ItemStack stack : plan.requirements) {
                        if (idx++ % 4 == 0) req.row();
                        // StatValues.displayItem(item, amount, plan.time, true) = 官方给的"物品+数量+速率"样式
                        req.add(StatValues.displayItem(stack.item, stack.amount, plan.time, true)).pad(5);
                    }
                    // 所有原料液体：同样 4 列排版
                    if (plan.inLiquid != null) for (LiquidStack stack : plan.inLiquid) {
                        if (idx++ % 4 == 0) req.row();
                        req.add(StatValues.displayLiquid(stack.liquid, stack.amount, true)).pad(5);
                    }
                }).right().grow().pad(10f);
            }).growX().pad(5);
            // 每个小卡片之间空一行
            table.row();
        }
    });
}

// ⭐ @Override Block.getPlanConfigs(Seq<UnlockableContent> options)
//   getPlanConfigs："放方块 -> 配置 -> 弹出配方图标网格"那个面板的候选收集入口（旧版 Mindustry 版本会调用它）
//   options：输出集合，往里加 Item / Liquid（都是 UnlockableContent 子类）
//   为什么覆写？因为我们有自己的 plans，要把"plans 里有解锁了的输出物品/输出液体"塞给 UI 画按钮。
@Override
public void getPlanConfigs(Seq<UnlockableContent> options) {
    for (Plan plan : plans) {
        if (plan == null) continue;
        if (plan.outItem != null && plan.outItem.length > 0 && plan.outItem[0] != null && plan.outItem[0].item != null) {
            Item it = plan.outItem[0].item;
            // 只有在"这个物品已经在当前研究进度下解锁"才加入候选
            if (it.unlockedNow()) options.add(it);
        } else if (plan.outLiquid != null && plan.outLiquid.length > 0 && plan.outLiquid[0] != null && plan.outLiquid[0].liquid != null) {
            Liquid lq = plan.outLiquid[0].liquid;
            if (lq.unlockedNow()) options.add(lq);
        }
        // ★ 纯 coinOutput 的配方（没有物品/液体输出，只有产 coins）：
        //   coins 本身就是一个 Item（继承 Item），所以直接加 NuItems.coinsItem（coinsItem.alwaysUnlocked=true，肯定 unlockedNow）
        else if (plan.coinOutput > 0 && NuItems.coinsItem != null) {
            if (NuItems.coinsItem.unlockedNow()) options.add(NuItems.coinsItem);
        }
    }
}

// =================================================================
    // 【Plan 内部类】= 一份配方（物品 + 液体 + coins）
    //      Plan 是 "static" 内部类：它不依赖任何具体的 HunfuBlock 实例，
    //      所以可以在 NuBlocks.java 里直接写 new Plan(...) 作为方块描述的一部分。
    // =================================================================
    public static class Plan {
        // outItem = 产出物品列表（数组，每项是 ItemStack = 物品+数量）；长度 0 = 不产物品
        public ItemStack[]   outItem;
        // requirements = 消耗物品列表（原料）；长度 0 = 不需要物品原料
        public ItemStack[]   requirements;
        // outLiquid = 产出液体列表；长度 0 = 不产液体
        public LiquidStack[] outLiquid;
        // inLiquid = 消耗液体列表；长度 0 = 不需要液体原料
        public LiquidStack[] inLiquid;
        // time = 单轮合成耗时（单位 = tick，60 tick = 1 秒）
        public float         time;
        /** ★ 新增：每轮合成消耗 coins 数量（0 = 不消耗）*/
        public int           coinCost;
        /** ★ 新增：每轮合成生产 coins 数量（0 = 不生产）*/
        public int           coinOutput;

        /* ============== 所有构造器（含新增 coins 相关签名）==============
         *   所有便利构造器都用 this(...) 委托到"7 参全参版"统一做 null→空数组的防御式处理：
         *   外部即使传 null 进来，内部也会转成 new XxxStack[0]，后面 for-in 就不会 NPE。
         */

        /** ★ 全参：物品出/入 + 液体出/入 + 耗时 + coinCost + coinOutput（所有其他构造器最终都走它）*/
        public Plan(ItemStack[] outItem, float time, ItemStack[] requirements,
                    LiquidStack[] outLiquid, LiquidStack[] inLiquid,
                    int coinCost, int coinOutput) {
            // 7 行赋值，每一行都是"非空就用外部的，空就 new 一个 length=0 的数组"
            this.outItem      = (outItem      != null) ? outItem      : new ItemStack[0];
            this.requirements = (requirements != null) ? requirements : new ItemStack[0];
            this.outLiquid    = (outLiquid    != null) ? outLiquid    : new LiquidStack[0];
            this.inLiquid     = (inLiquid     != null) ? inLiquid     : new LiquidStack[0];
            this.time         = time;
            this.coinCost     = coinCost;
            this.coinOutput   = coinOutput;
        }
        /** ★ 全参（coinCost 单参版，旧签名兼容）：物品+液体+耗时+coinCost → coinOutput=0 不产 coins */
        public Plan(ItemStack[] outItem, float time, ItemStack[] requirements,
                    LiquidStack[] outLiquid, LiquidStack[] inLiquid, int coinCost) {
            this(outItem, time, requirements, outLiquid, inLiquid, coinCost, 0);
        }
        /** ★★ 兼容旧签名（物品出/入 + 液体出/入 + 耗时）→ coinCost=0, coinOutput=0（原有 0-coins 配方 100% 兼容）*/
        public Plan(ItemStack[] outItem, float time, ItemStack[] requirements,
                    LiquidStack[] outLiquid, LiquidStack[] inLiquid) {
            this(outItem, time, requirements, outLiquid, inLiquid, 0, 0);
        }
        /** 兼容老签名：只有物品出/入 + 耗时（液体都传 null）*/
        public Plan(ItemStack[] outItem, float time, ItemStack[] requirements) {
            this(outItem, time, requirements, null, null, 0, 0);
        }
        /** ★ 便捷：物品出/入 + 耗时 + coinCost（消耗 coins 出物品，用得最多）*/
        public Plan(ItemStack[] outItem, float time, ItemStack[] requirements, int coinCost) {
            this(outItem, time, requirements, null, null, coinCost, 0);
        }
        /** ★ 便捷：物品出/入 + 耗时 + coinCost + coinOutput（消耗 coins + 物品 → 出物品 + 少量 coins）*/
        public Plan(ItemStack[] outItem, float time, ItemStack[] requirements, int coinCost, int coinOutput) {
            this(outItem, time, requirements, null, null, coinCost, coinOutput);
        }
        // 包访问权限的空参构造：给反射/某些工具调用用，手动把所有字段赋值为空
        Plan(){
            this.outItem      = new ItemStack[0];
            this.requirements = new ItemStack[0];
            this.outLiquid    = new LiquidStack[0];
            this.inLiquid     = new LiquidStack[0];
            this.time         = 0f;
            this.coinCost     = 0;
            this.coinOutput   = 0;
        }
    }
    // =================================================================
    // 【HunfuBuild 内部类】= 地图上放下去的那个方块实例（混成 + coins）
    //
    // ⭐⭐⭐ @Override 规则总结 2：—— 我现在在 HunfuBuild extends Building 这一层！
    //   所以这里所有 @Override 都覆写的是 mindustry.gen.Building（接口）里的方法：
    //     created()              = 方块刚被放下/被导入到地图时调用一次（用来做"默认选第一个配方"这种一次性初始化）
    //     drawSelect()           = 玩家鼠标选中这个方块时，世界里额外画的"选择装饰"
    //     acceptPayload(...)     = 这个方块能不能接收 payload（单位/运载方块）—— 混成工厂直接 return false
    //     draw()                 = ⭐⭐⭐ 最核心的"每一帧怎么画这个方块"（Building.draw = 画 region）
    //     drawLight()            = 同一帧紧接着画"发光叠加层"（比如 DrawGlowRegion 会在这里调）
    //     acceptLiquid(...)      = 外部管道往这个方块里灌液体时，"给不给接"
    //     senseObject(LAccess)   = 逻辑处理器 sensor 读取一个对象型配置（这里 sensor @config 应该返回当前出物品/液体）
    //     sense(LAccess)         = 逻辑处理器 sensor 读取数值型（progress/itemCapacity/liquidCapacity）
    //     display(Table)         = 方块详情面板下半部分（我们在这里加"当前配方图标 + 名字"一行）
    //     config()               = 方块的"当前配置"是什么 → 返回 currentPlan（Integer），给逻辑处理器读用
    //     buildConfiguration(Table) = 点方块"配置"按钮弹出的 UI（我们画配方图标网格）
    //     shouldConsume()        = 这一 tick 能开工吗？（原料/容量/coins 全满足才开工推进度）
    //     status()               = 方块右下角那个状态灯颜色（绿/橙/红/紫/灰）
    //     getMaximumAccepted(Item)= 最多接受多少个某物品（我们按 capacities[item.id] 给）
    //     acceptItem(...)        = 外部给物品时收不收
    //     updateTile()           = ⭐⭐⭐ 最核心的"每 tick 方块行为"：推进进度 / 扣原料 / 出产物 / 出 coins
    //     version()              = 存档版本号（我们写 2，读档时看 revision 决定要不要读新字段）
    //     write(Writes)          = 存档写二进制
    //     read(Reads,byte)       = 读档读二进制 + 旧存档安全兼容
    //     warmup()               = 工厂"升温系数 0..1"（给某些 drawer 做旋转速度/强度插值用）
    //     totalProgress()        = 工厂"总共累积进度（可到几亿）"，给 DrawParticles 这类 drawer 用
    //
    // ⚠️ 重要：如果你想给 Block 层已经有的方法（比如 load / icons / drawPlanRegion）再写一次 @Override，
    //         一定要回到 HunfuBlock extends Block 那一层去写；在这里写就会直接报
    //         「不会覆盖或实现超类型的方法」/「类型不兼容：HunfuBuild 不能转 Block」。
    // =================================================================
    public class HunfuBuild extends Building {

        // currentPlan = 当前选择的配方下标（plans 的 index）；-1 = 没选任何配方（= 停工）
        public int   currentPlan = -1;
        // progress = 这一轮合成累计推进量（单位 tick）；达到 plan.time 就完成一轮
        public float progress    = 0f;
        // warmup = 工厂"加速升温系数"：有电且满足原料时靠近 1，没电/没原料时降到 0；warmup() 接口返回它
        public float warmup;
        // totalProgress = 工厂总累计进度（只增不减）；totalProgress() 接口返回它
        public float totalProgress;

        // fraction()：方便方法，进度 / 总耗时，返回 0..1（给进度条、逻辑处理器 sense progress 直接用）
        public float fraction() {
            // 没选配方/下标越界 → 进度 0
            if (currentPlan == -1 || currentPlan >= plans.size) return 0;
            Plan p = plans.get(currentPlan);
            // 没拿到 Plan（理论不会，防御式）→ 0
            return p == null ? 0 : progress / p.time;
        }

        // ⭐ @Override Building.created()
        //   created() = 这个方块实例刚被放到地图上（或地图加载，实例刚反序列化完）时调用一次。
        //   为什么覆写？：玩家放下去的混成工厂，如果没有选配方，默认应该给它自动选"第一个有合法输出的配方"，
        //   不然放下去就是停工状态，玩家必须点配置才能开。
        @Override
        public void created() {
            // 只有"没选过配方"（currentPlan == -1）才做自动选；玩家已手动选过的不覆盖
            if (currentPlan == -1) {
                for (int i = 0; i < plans.size; i++) {
                    Plan p = plans.get(i);
                    // 找"有输出物品（解锁）/ 输出液体（解锁）/ coinOutput>0"的第一个配方
                    boolean hasOut = (p.outItem   != null && p.outItem.length   > 0 && p.outItem[0]   != null && p.outItem[0].item   != null && p.outItem[0].item.unlockedNow())
                                  || (p.outLiquid != null && p.outLiquid.length > 0 && p.outLiquid[0] != null && p.outLiquid[0].liquid != null && p.outLiquid[0].liquid.unlockedNow())
                                  || (p.coinOutput > 0);  // ★ 纯产 coins 配方也可以自动当选
                    if (hasOut) { currentPlan = i; break; }
                }
                // 如果循环也没找到（全都是"空配方"），那就把 currentPlan 设为 0（至少别是 -1，让 UI 正常工作）
                if (currentPlan == -1 && plans.size > 0) currentPlan = 0;
            }
        }

        // ⭐ @Override Building.drawSelect()
        //   drawSelect() = 玩家鼠标点到这个方块、选中期间，额外绘制的"选中装饰"。
        //   父类默认画"虚线框"。我们加一个：在方块右侧画一个悬浮物品图标（drawItemSelection = 官方现成辅助）
        @Override
        public void drawSelect() {
            // 先父类：画那个选中框
            super.drawSelect();
            // 只有多配方场景、且选中的配方存在、且配方有"第一个输出物品"才画
            if (plans.size > 1 && currentPlan != -1 && currentPlan < plans.size) {
                Plan p = plans.get(currentPlan);
                if (p != null) {
                    if
                    (p.outItem != null && p.outItem.length > 0 &&
                            p.outItem[0] != null && p.outItem[0].item != null&& p.coinOutput>0){
                        drawItemSelection(p.outItem[0].item);
                    }
                }
            }
        }

        // ⭐ @Override Building.acceptPayload(source, payload)
        //   这个方块能不能装下 Payload（Unit 被运载进去 / 方块被推走）→ 混成工厂永远不接受，return false。
        @Override
        public boolean acceptPayload(Building source, mindustry.world.blocks.payloads.Payload payload) { return false; }

        // ⭐ @Override Building.draw()
        //   draw() = 每帧真正绘制"这个方块长什么样"的入口。
        //   父类 Building.draw() 默认只画 Draw.rect(region, x, y) = 一张主贴图。
        //   为什么覆写？我们有自定义 drawer（DrawMulti/DrawRegion），要走 drawer.draw(this)
        //   —— 注意这里 this 是 HunfuBuild（Building），DrawBlock.draw(Building) 正好接受 Building 参数 ✅
        @Override
        public void draw(){
            drawer.draw(this);
        }

        // ⭐ @Override Building.drawLight()
        //   drawLight() = 在 draw() 之后紧接着"发光层"单独画一次（所有发光元素叠合），
        //   父类默认 super.drawLight() 会画"方块本身的 team light"。
        //   为什么覆写？DrawGlowRegion / DrawFlame 这类 DrawBlock 子类是在 drawLight() 里画的，
        //   所以必须补 drawer.drawLight(this) 让它们一起参与。
        @Override
        public void drawLight(){
            super.drawLight();
            drawer.drawLight(this);
        }

        // ⭐ @Override Building.acceptLiquid(Building source, Liquid liquid)
        //   acceptLiquid() = 管道想把液体 liquid 灌进来时调用：能不能接？
        //   只有"这个液体是当前配方的原料之一"且"现有容量没满"才允许灌。
        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            // 没选配方 → 不接
            if (currentPlan == -1 || currentPlan >= plans.size) return false;
            Plan plan = plans.get(currentPlan);
            // 没配方 / 配方不用液体原料 → 不接
            if (plan == null || plan.inLiquid == null) return false;
            for (LiquidStack stack : plan.inLiquid) {
                if (stack != null && stack.liquid == liquid
                        && liquids.get(liquid) < getMaximumAccepted(liquid)) {
                    return true;
                }
            }
            return false;
        }

        // ⭐ @Override Building.senseObject(LAccess sensor)
        //   senseObject() = 逻辑处理器执行 `sensor1 = @config 方块` 时调用：返回一个对象型结果。
        //   这里只特殊处理 LAccess.config（逻辑上的"配置感知"），返回当前配方的"输出物品/输出液体"对象；
        //   其他 LAccess 都交给父类默认处理。
        @Override
        public Object senseObject(LAccess sensor) {
            if (sensor == LAccess.config) {
                if (currentPlan == -1 || currentPlan >= plans.size) return null;
                Plan p = plans.get(currentPlan);
                if (p == null) return null;
                // 优先：有输出物品 → 返回那个物品（逻辑处理器能拿到物品对象）
                if (p.outItem != null && p.outItem.length > 0 && p.outItem[0] != null) return p.outItem[0].item;
                // 其次：有输出液体 → 返回那个液体
                if (p.outLiquid != null && p.outLiquid.length > 0 && p.outLiquid[0] != null) return p.outLiquid[0].liquid;
                // 都没有（纯产 coins 也没有对应的对象型）→ null
                return null;
            }
            return super.senseObject(sensor);
        }

        // ⭐ @Override Building.sense(LAccess sensor)
        //   sense() = 逻辑处理器 sensor 要数值型结果时的入口（progress、capacity 这些纯数字）。
        @Override
        public double sense(LAccess sensor) {
            // LAccess.progress → 返回 fraction()，Mathf.clamp 把它限制到 0..1 之间
            if (sensor == LAccess.progress)     return Mathf.clamp(fraction());
            // LAccess.itemCapacity → 返回这个方块声明级的 itemCapacity（总容量）
            if (sensor == LAccess.itemCapacity) return itemCapacity;
            // LAccess.liquidCapacity → 返回这个方块声明级的 liquidCapacity
            if (sensor == LAccess.liquidCapacity) return liquidCapacity;
            return super.sense(sensor);
        }

        // ⭐ @Override Building.display(Table table)
        //   display() = 方块详情面板"下半部分"：父类 display 已经给你放了血量条、进度条、配置这些。
        //   我们在下面再加一行：显示"当前配方的图标 + 名字"，一眼看出来现在做什么。
        @Override
        public void display(Table table) {
            // 先父类：把默认的那些显示行（进度、power、liquid 等）摆出来
            super.display(table);
            // reg = 一个"可反复 set region 的 drawable"（省得每帧 new 一个 TextureRegionDrawable）
            TextureRegionDrawable reg = new TextureRegionDrawable();
            table.row();
            table.table(t -> {
                t.left();
                // .update(i -> {...})：每一帧更新图标的图像（currentPlan 会变，配方图标也跟着变）
                t.image().update(i -> {
                    if (currentPlan == -1 || currentPlan >= plans.size) {
                        // 没配方 → 画一个 X 占位 + 灰颜色
                        i.setDrawable(Icon.cancel); i.setColor(Color.lightGray);
                    } else {
                        Plan p = plans.get(currentPlan);
                        if (p == null) { i.setDrawable(Icon.cancel); i.setColor(Color.lightGray); return; }
                        if (p.outItem != null && p.outItem.length > 0 && p.outItem[0] != null && p.outItem[0].item != null) {
                            // 有输出物品 → 画物品图标
                            i.setDrawable(reg.set(p.outItem[0].item.uiIcon));
                            i.setColor(Color.white);
                        } else if (p.outLiquid != null && p.outLiquid.length > 0 && p.outLiquid[0] != null && p.outLiquid[0].liquid != null) {
                            // 有输出液体 → 画液体图标
                            i.setDrawable(reg.set(p.outLiquid[0].liquid.uiIcon));
                            i.setColor(Color.white);
                        } else if (p.coinOutput > 0) {
                            // ★ 纯产 coins：用 cancel 图标 + 黄绿色占位（因为没有"coins 专属图标"）
                            i.setDrawable(coins.uiIcon);
                            i.setColor(Color.valueOf("C4C4C4"));
                        } else {
                            i.setDrawable(Icon.cancel); i.setColor(Color.lightGray);
                        }
                    }
                    i.setScaling(Scaling.fit);
                }).size(32).padBottom(-4).padRight(2);
                // 标签：旁边写"这个配方的名字"字符串，也是每帧读一次（因为 currentPlan 会变）
                t.label(() -> {
                    if (currentPlan == -1 || currentPlan >= plans.size) return "@none";
                    Plan p = plans.get(currentPlan);
                    if (p == null) return "@none";
                    if (p.outItem != null && p.outItem.length > 0 && p.outItem[0] != null && p.outItem[0].item != null)
                        return p.outItem[0].item.localizedName;
                    if (p.outLiquid != null && p.outLiquid.length > 0 && p.outLiquid[0] != null && p.outLiquid[0].liquid != null)
                        return p.outLiquid[0].liquid.localizedName;
                    if (p.coinOutput > 0) return "硬币 (coins)";
                    return "@none";
                }).wrap().width(230f).color(Color.lightGray);
            }).left();
        }

        // ⭐ @Override Building.config()
        //   config() = 当外部问"这个方块当前配置是什么"时调用（比如读档后给你同步显示、或逻辑处理器读配置缓存）
        //   我们直接返回 currentPlan（Integer），因为我们的 config(Integer.class, ...) 处理的就是整数下标。
        @Override
        public Object config() { return currentPlan; }

        // ========== 配置面板 UI ==========
        // ⭐ @Override Building.buildConfiguration(Table table)
        //   buildConfiguration() = 玩家点方块右上的"配置按钮"弹出的面板。
        //   我们做一个"配方图标网格"：每一个配方（出物品/出液体/★出 coins）→ 一个按钮；点按钮切配方。
        //   ★ 用「planIndex 精确绑定」避免歧义（例如 3 个都出 coins 的配方，各自指向独立下标，不会都跳到第一个）
        @Override
        public void buildConfiguration(Table table) {
            // candidates = 候选图标（Item/Liquid/NuItems.coinsItem）
            Seq<UnlockableContent> candidates = new Seq<>();
            // planIndexes = candidates 每一项对应的 plans[] 下标（★ 和 candidates 一一对应，消除所有歧义）
            Seq<Integer> planIndexes = new Seq<>();
            for (int i = 0; i < plans.size; i++) {
                Plan p = plans.get(i);
                if (p == null) continue;
                if (p.outItem != null && p.outItem.length > 0 && p.outItem[0] != null
                        && p.outItem[0].item != null && p.outItem[0].item.unlockedNow()) {
                    // 有输出物品且解锁 → 图标=该物品，index=i
                    candidates.add(p.outItem[0].item);
                    planIndexes.add(i);
                } else if (p.outLiquid != null && p.outLiquid.length > 0 && p.outLiquid[0] != null
                        && p.outLiquid[0].liquid != null && p.outLiquid[0].liquid.unlockedNow()) {
                    // 有输出液体且解锁 → 图标=该液体，index=i
                    candidates.add(p.outLiquid[0].liquid);
                    planIndexes.add(i);
                } else if (p.coinOutput > 0 && NuItems.coinsItem != null && NuItems.coinsItem.unlockedNow()) {
                    // ★ 纯产 coins（没有出物品/出液体，只有 coinOutput）→ 图标用 NuItems.coinsItem，index=i
                    //   解决 coinProducer 这种方块「3 个配方都只产 coins」时 candidates 为空 → 显示 @none 的问题
                    candidates.add(NuItems.coinsItem);
                    planIndexes.add(i);
                }
            }
            if (candidates.any()) {
                int idx = 0;
                for (UnlockableContent uc : candidates) {
                    final int planIndex = planIndexes.get(idx);   // 这个按钮对应 plans 的第几个配方
                    // 满 selectionColumns 列 → 换一行（让按钮按 4 列网格排）
                    if (idx % selectionColumns == 0) table.row();
                    // 把 uc 分别转成 Item / Liquid 引用，拿图标用（点击/选中都走 planIndex，不再依赖 匹配内容）
                    Item item = (uc instanceof Item it) ? it : null;
                    Liquid liq = (uc instanceof Liquid lq) ? lq : null;

                    // ⭐ 计算右下角角标要显示的数字（用户需求）：
                    //   ① 如果是「纯产 coins」配方（NuItems.coinsItem 图标） → 显示 "×" + plan.coinOutput
                    //   ② 如果是「有 outItem」配方（不是 coinsItem）    → 显示 "×" + 该配方第一个输出物品的数量
                    //   ③ 如果是「液体配方」                              → 不显示（用户要求液体不显示角标）
                    String badgeText = null;
                    Plan curPlan = (planIndex >= 0 && planIndex < plans.size) ? plans.get(planIndex) : null;
                    if (curPlan != null) {
                        // 情况 ①：纯产 coins（收集候选时 coinOutput 分支才会塞进 NuItems.coinsItem）
                        if (NuItems.coinsItem != null && item == NuItems.coinsItem) {
                            if (curPlan.coinOutput > 0) badgeText = "×" + curPlan.coinOutput;
                        }
                        // 情况 ②：出物品配方（不是 coinsItem，且 outItem 有内容）→ 显示 outItem[0].amount
                        else if (item != null && curPlan.outItem != null && curPlan.outItem.length > 0
                                && curPlan.outItem[0] != null && curPlan.outItem[0].item == item
                                && curPlan.outItem[0].amount > 1) {
                            // 数量 > 1 才显示，×1 没必要
                            badgeText = "×" + curPlan.outItem[0].amount;
                        }
                        // 情况 ③：液体 → badgeText 保持 null（不显示，符合用户要求）
                    }

                    // 每一帧读取 currentPlan/coinOutput/outItem 来决定角标内容
                    //   （用户要求：纯产 coins → ×coinOutput；物品配方 → ×amount；液体 → 空）
                    // lambda 要求捕获的局部变量是 effectively final → 把 badgeText 钉死在 finalBadge 上
                    final String finalBadge = badgeText;

                    // button(底板纹理, 样式, 点下回调) → size 50 方形按钮
                    ImageButton b = table.button(Tex.whiteui, Styles.squareTogglei, () -> {
                        // ★ 直接按绑定的 planIndex 切换——最精确，不会因为多个配方出同种物品/coins 跳到第一个
                        configure(planIndex);
                    }).size(50f).get();
                    // 清空 button 默认的"小勾"图标；我们自己塞"图标 + 右下角数量角标"进去
                    b.clearChildren();

                    // ⭐ Stack 布局：底层是 34×34 物品/液体图标，上层叠一个右下角对齐的小 Label（角标）
                    //   ImageButton 本质是 Table，所以可以用 stack(子1, 子2).grow() 把两层叠起来
                    b.stack(
                        // ——— 第 1 层：34×34 的物品/液体图标（和原来一致）———
                        new Image(
                            (item != null && item.uiIcon != null && item.uiIcon.texture != null) ? item.uiIcon :
                            (liq  != null && liq.uiIcon  != null && liq.uiIcon.texture  != null) ? liq.uiIcon  :
                            Core.atlas.find("white")
                        ).setScaling(Scaling.fit),

                        // ——— 第 2 层：右下角对齐的数量角标 Label ———
                        new Table(content -> {
                            content.right().bottom();  // 把内容钉在右下角
                            if (finalBadge != null) {
                                // Styles.outlineLabel = 带黑色描边的白字，叠在图标上不会看不清楚
                                content.add(finalBadge).style(Styles.outlineLabel)
                                        .fontScale(0.65f)          // 缩小字体（相对于默认 1.0 缩 65%）
                                        .color(Color.white)
                                        .padRight(-2f).padBottom(-4f); // 微微再贴右下一点，刚好露出"×20"
                            }
                        })
                    ).grow().pad(8f);  // grow = 占满按钮整个 50×50 空间，pad 8 让图标缩到约 34

                    // 每一帧更新按钮的"选中状态"：直接比较 currentPlan == planIndex（★ 最准确，类型无关）
                    b.update(() -> {
                        b.setChecked(currentPlan == planIndex);
                    });
                    table.add().pad(2);
                    idx++;
                }
            } else {
                // 没有任何候选 → 画一个"@none"灰底提示（和原版 Smelter 没配方时一致）
                table.table(Styles.black3, t -> t.add("@none").color(Color.lightGray));
            }
        }

        // ========== shouldConsume：物品 + 液体 + coins 三检查 ==========
        // ⭐ @Override Building.shouldConsume()
        //   shouldConsume() = Mindustry 的"消费者系统"会在推进效率前问一次：现在真的可以开工吗？
        //   —— 我们的 updateTile() 里也会用它做门控。只有"所有条件都 true"才让进度累计。
        @Override
        public boolean shouldConsume() {
            if (currentPlan == -1 || currentPlan >= plans.size) return false;
            if (!enabled) return false;
            Plan plan = plans.get(currentPlan);
            if (plan == null) return false;
            // ★ hasAnyOut：允许「只有 coinOutput（纯产 coins）」的配方（没有物品/液体输出也算"有输出"）
            boolean hasAnyOut = (plan.outItem != null && plan.outItem.length > 0)
                             || (plan.outLiquid != null && plan.outLiquid.length > 0)
                             || (plan.coinOutput > 0);
            if (!hasAnyOut) return false;

            // 检查 A：物品产物会不会爆仓（会的话 → 不消耗，先等传送带拉走）
            if (plan.outItem != null) for (ItemStack s : plan.outItem) {
                if (s == null || s.item == null) continue;
                int willHave = items.get(s.item) + s.amount;
                if (willHave > getMaximumAccepted(s.item)) return false;
            }
            // 检查 B：液体产物会不会爆仓
            if (plan.outLiquid != null) for (LiquidStack s : plan.outLiquid) {
                if (s == null || s.liquid == null) continue;
                float willHave = liquids.get(s.liquid) + s.amount;
                if (willHave > getMaximumAccepted(s.liquid)) return false;
            }
            // 检查 C：物品原料够不够
            if (plan.requirements != null) for (ItemStack s : plan.requirements) {
                if (s == null || s.item == null) continue;
                if (items.get(s.item) < s.amount) return false;
            }
            // 检查 D：液体原料够不够
            if (plan.inLiquid != null) for (LiquidStack s : plan.inLiquid) {
                if (s == null || s.liquid == null) continue;
                if (liquids.get(s.liquid) < s.amount) return false;
            }
            // ★ 检查 E：coins 够不够（消耗配方）
            if (plan.coinCost > 0 && coins.getAmount() < plan.coinCost) return false;
            return true;
        }

        // ⭐ @Override Building.status()
        //   status() = 方块右下角那个彩色小灯（看一眼就知道"现在为什么不工作"）。
        //   —— Mindustry 159.6 BlockStatus 只有 6 个枚举值：
        //      active(绿) 工作中
        //      noOutput(橙) 出不去产物（满仓）
        //      noInput(红) 没原料 / 没选配方
        //      logicDisable(紫) 逻辑处理器用 enable=false 关掉了
        //      inactiveUnitFactory(灰) 单位工厂空闲
        //      inactive(灰) 空闲/等原料
        @Override
        public BlockStatus status() {
            // 无合法配方 → 红 noInput
            if (currentPlan == -1 || currentPlan >= plans.size) return BlockStatus.noInput;
            Plan plan = plans.get(currentPlan);
            if (plan == null) return BlockStatus.noInput;
            boolean hasAnyOut = (plan.outItem != null && plan.outItem.length > 0)
                             || (plan.outLiquid != null && plan.outLiquid.length > 0)
                             || (plan.coinOutput > 0);
            if (!hasAnyOut) return BlockStatus.noInput;
            if (!enabled) return BlockStatus.logicDisable;

            // —— 先查"产物满仓"：橙 noOutput ——
            if (plan.outItem != null) for (ItemStack st : plan.outItem) {
                if (st == null || st.item == null) continue;
                if (items.get(st.item) + st.amount > getMaximumAccepted(st.item)) return BlockStatus.noOutput;
            }
            if (plan.outLiquid != null) for (LiquidStack st : plan.outLiquid) {
                if (st == null || st.liquid == null) continue;
                if (liquids.get(st.liquid) + st.amount > getMaximumAccepted(st.liquid)) return BlockStatus.noOutput;
            }

            // —— 再查"原料不够 / 不够 coins"：红 noInput ——
            if (plan.coinCost > 0 && coins.getAmount() < plan.coinCost) return BlockStatus.noInput;
            if (plan.requirements != null) for (ItemStack st : plan.requirements) {
                if (st == null || st.item == null) continue;
                if (items.get(st.item) < st.amount) return BlockStatus.noInput;
            }
            if (plan.inLiquid != null) for (LiquidStack st : plan.inLiquid) {
                if (st == null || st.liquid == null) continue;
                if (liquids.get(st.liquid) < st.amount) return BlockStatus.noInput;
            }

            // —— 有电、效率>0、并且进度已经开始推 → 绿 active（真的在合成）——
            if (efficiency > 0f && progress > 0f) return BlockStatus.active;
            // 有电但"等原料第一 tick / 效率刚升上来" → 灰 inactive
            return BlockStatus.inactive;
        }

        // ⭐ @Override Building.getMaximumAccepted(Item item)
        //   某物品最多接多少个 → capacities[item.id]（initCapacities 里按配方最大值统一出来的）
        @Override
        public int getMaximumAccepted(Item item) {
            if (item == null || item.id >= capacities.length) return 0;
            return capacities[item.id];
        }
        // 液体版：和物品版同理；Building 里没有"必须覆写"的同名方法，所以没 @Override（不影响）
        public float getMaximumAccepted(Liquid liquid) {
            if (liquid == null || liquid.id >= liquidCapacities.length) return 0f;
            return liquidCapacities[liquid.id];
        }

        // ⭐ @Override Building.acceptItem(source, item)
        //   acceptItem() = 外部（传送带/分流器/桥）想往这个方块里塞物品，给不给收？
        //   规则：只有 item 是当前配方的原料之一，并且容量没满，才收 → 避免被不相关物品占满槽位
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
        // ✅ 核心 updateTile：扣原料 / 扣 coins / 出产物 / 出 coins
        //    updateTile() 只有当 Block.update=true（我们在构造器打开了）时才会被 Mindustry 每 tick 调用一次。
        // =================================================================
        @Override
        public void updateTile() {
            // 如果配置被禁用 configurable=false（有些逻辑处理器会设置）→ 强制用第一份配方
            if (!configurable) currentPlan = 0;
            // 下标越界 → 直接把 currentPlan = -1 并跳出（防止 NPE）
            if (currentPlan < 0 || currentPlan >= plans.size) { currentPlan = -1; return; }
            Plan plan = plans.get(currentPlan);
            // ★ 允许只有 coinOutput 没有物品/液体输出的配方开工（纯产 coins）
            boolean hasAnyOut = (plan != null) && (
                    (plan.outItem != null && plan.outItem.length > 0) ||
                    (plan.outLiquid != null && plan.outLiquid.length > 0) ||
                    (plan.coinOutput > 0));
            if (plan == null || !hasAnyOut) { currentPlan = -1; return; }

            // Step 1：进度累积（只有 efficiency>0 且 shouldConsume() 全通过才推进）
            if (efficiency > 0 && shouldConsume()) {
                // progress += edelta()：edelta() = Time.delta × efficiency × 各种效率倍率（Mindustry 官方时间步）
                progress += edelta();
                // warmup 线性靠近 1（每 tick 最多走 0.1 × delta）
                warmup = Mathf.approachDelta(warmup, 1f, 0.1f);
                // totalProgress：带 warmup 权重的累计进度（给旋转件/粒子/火焰做"工作时长"指标）
                totalProgress += warmup * Time.delta;
            } else {
                // 不满足条件：warmup 线性靠近 0（工厂慢慢停了）
                warmup = Mathf.approachDelta(warmup, 0f, 0.1f);
            }

            // Step 2：进度满 → 扣原料 + 扣 coins + 出产物 + 出 coins
            if (progress >= plan.time) {
                // ① 检查：物品/液体产物容量（这一 tick 会不会爆仓？爆就等下一次）
                boolean allOutOk = true;
                if (plan.outItem != null) for (ItemStack s : plan.outItem) {
                    if (s == null || s.item == null) continue;
                    int willHave = items.get(s.item) + s.amount;
                    if (willHave > getMaximumAccepted(s.item)) { allOutOk = false; break; }
                }
                if (allOutOk && plan.outLiquid != null) for (LiquidStack s : plan.outLiquid) {
                    if (s == null || s.liquid == null) continue;
                    float willHave = liquids.get(s.liquid) + s.amount;
                    if (willHave > getMaximumAccepted(s.liquid)) { allOutOk = false; break; }
                }
                // ② 检查：物品/液体原料（这一轮原料够不够？）
                boolean allInOk = true;
                if (plan.requirements != null) for (ItemStack s : plan.requirements) {
                    if (s == null || s.item == null) continue;
                    if (items.get(s.item) < s.amount) { allInOk = false; break; }
                }
                if (allInOk && plan.inLiquid != null) for (LiquidStack s : plan.inLiquid) {
                    if (s == null || s.liquid == null) continue;
                    if (liquids.get(s.liquid) < s.amount) { allInOk = false; break; }
                }
                // ③ ★ 检查 coins（消耗配方才会不够）
                boolean coinsOk = (plan.coinCost <= 0) || (coins.getAmount() >= plan.coinCost);

                if (allOutOk && allInOk && coinsOk) {
                    // —— 扣物品原料 ——
                    if (plan.requirements != null) for (ItemStack s : plan.requirements) {
                        if (s == null || s.item == null || s.amount <= 0) continue;
                        items.remove(s.item, s.amount);
                    }
                    // —— 扣液体原料 ——
                    if (plan.inLiquid != null) for (LiquidStack s : plan.inLiquid) {
                        if (s == null || s.liquid == null || s.amount <= 0) continue;
                        liquids.remove(s.liquid, s.amount);
                    }
                    // —— ★ 扣 coins（消耗配方）——
                    if (plan.coinCost > 0) {
                        coins.spend(plan.coinCost);
                    }
                    // —— 出物品产物：先 add 到物品槽；再 dump(每个物品) amount 次，往外推给传送带/桥 ——
                    if (plan.outItem != null) for (ItemStack s : plan.outItem) {
                        if (s == null || s.item == null || s.amount <= 0) continue;
                        items.add(s.item, s.amount);
                        for (int i = 0; i < s.amount; i++) dump(s.item);
                    }
                    // —— 出液体产物：同样 add 进液体槽；再 dumpLiquid ceil(amount) 次推给管道 ——
                    if (plan.outLiquid != null) for (LiquidStack s : plan.outLiquid) {
                        if (s == null || s.liquid == null || s.amount <= 0f) continue;
                        liquids.add(s.liquid, s.amount);
                        int times = (int)Math.ceil(s.amount);
                        for (int i = 0; i < times; i++) dumpLiquid(s.liquid);
                    }
                    // —— ★ 产 coins（生产配方）——
                    if (plan.coinOutput > 0) {
                        coins.add(plan.coinOutput);
                    }

                    // 进度 -= 总时长（留着"溢出 tick"，避免刚好差 0.2 tick 造成每轮丢进度 → 长期偏慢）
                    progress -= plan.time;
                } else {
                    // 检查没通过：把进度卡在"刚好没完成"（下一个 tick 条件满足就立刻合成）
                    progress = Math.min(progress, plan.time - 0.001f);
                }
            }
            // 防御式：进度不能为负
            if (progress < 0) progress = 0;

            // 兜底 dump：如果出产物/出液体"还没被外部拉走"，尽量主动往接物口推（给 10 秒爆仓时慢慢排）
            if (currentPlan != -1 && currentPlan < plans.size) {
                Plan lp = plans.get(currentPlan);
                if (lp != null) {
                    if (lp.outItem != null) for (ItemStack s : lp.outItem) {
                        if (s == null || s.item == null) continue;
                        int left = items.get(s.item);
                        if (left <= 0) continue;
                        for (int i = 0; i < left; i++) dump(s.item);
                    }
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

        // ========== 读写：progress + currentPlan + warmup + totalProgress ==========
        // ⭐ @Override Building.version()
        //   version() = 每次写存档返回一个"存档版本字节"，读档时作为 revision 参数传进 read()。
        //   这里 = 2，因为我们比早期版本（1）多加了 warmup / totalProgress 两个字段。
        @Override
        public byte version() { return 2; }

        // ⭐ @Override Building.write(Writes write)
        //   写存档：先写父类（位置 / 物品 / 液体 / 功率效率）→ 再写我们自己的 4 个字段：
        @Override
        public void write(Writes write) {
            super.write(write);
            write.f(progress);      // 4 字节 float
            write.s(currentPlan);   // 2 字节 short
            write.f(warmup);        // 4 字节 float
            write.f(totalProgress); // 4 字节 float
        }

        // ⭐ @Override Building.read(Reads read, byte revision)
        //   读存档：revision == version() 上次写时返回的值 → 老版本只有 progress + currentPlan
        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            progress = read.f();
            currentPlan = read.s();
            if (revision >= 2) { // 新存档：继续读 warmup / totalProgress
                warmup = read.f();
                totalProgress = read.f();
            } else {            // 旧存档（revision=1 或更小）：两个字段设 0（安全降级，不会读错位）
                warmup = 0f;
                totalProgress = 0f;
            }
        }

        // warmup / totalProgress 接口覆写：
        //   Building 里有 warmup() / totalProgress() 两个默认方法（都返回 0），
        //   很多 DrawBlock 子类（DrawSpins / DrawParticles / DrawFlame / DrawTurret ...）会用它们控制动画速度。
        //   所以这里必须覆写、返回本地字段，否则 drawer 里的 warmup 永远是 0（旋转件不转 / 火焰不冒）。
        @Override
        public float warmup()            { return warmup; }
        @Override
        public float totalProgress()     { return totalProgress; }
    }
}
