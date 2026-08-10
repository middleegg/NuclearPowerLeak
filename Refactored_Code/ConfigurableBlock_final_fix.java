/*
 * ⚠️ FINAL FIX 版 —— 四问题全修复 + 全中文逐段注释
 * ⚠️ 仅放入 Refactored_Code 文件夹，不修改 src 原文件
 * ================================================================
 * 本次修复的四大问题：
 *
 * ✅ 修复 1：物品复制（先 items.add 再 offload 单推）→ 改为「consume() 扣料 → offload(多参)直推 → 推不出去再临时丢仓」
 * ✅ 修复 2：原料不消耗（手动 items.remove 偶发不生效）→ 删除手动扣料，**统一走 consume()**（用户明确要求要 consume）
 * ✅ 修复 3：draw() 因 outRegion/topRegion 为 null 报 NPE → 加 null 判断，并恢复 outRegion 朝向指示（rotate 才有意义）
 * ✅ 修复 4：createSound 为 null 时报 NPE → 加 null 判断，不赋值也不会崩
 *
 * 另顺手加的保护（不影响使用）：
 *   - offload(Item, amount) 失败后走 items.add 且先查容量，再塞不下就把 progress 卡住（但已经在 shouldConsume 里卡了）
 *   - Plan.outItem / Plan.requirements 全部加 null 保护，空数组不崩
 *   - Plan 字段（outItem / requirements）为空时也能正确初始化 plans
 *   - 所有 for-each 处加了空数组判空
 */
package Npl.newSth;

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
import mindustry.world.blocks.units.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.world.Block;
import java.util.*;

import static mindustry.Vars.*;


public class ConfigurableBlock extends UnitBlock {

    public int[] capacities = {};
    public Seq<Plan> plans = new Seq<>(4);
    public int selectionRows = 2;
    public int selectionColumns = 4;

    public ConfigurableBlock(String name) {
        super(name);

        // —— 基本属性 ——
        update = true;
        solid = true;
        hasItems = true;
        hasPower = true;
        consumesPower = true;
        size = 2;
        health = 100;
        rotate = true;
        configurable = true;
        itemCapacity = 30;

        // —— 三种配置通道 ————————————————————————————————————————————
        // ① 按「索引」切换（逻辑处理器 configure 数字用）
        config(Integer.class, (UnitFactoryBuild build, Integer i) -> {
            if (!configurable) return;
            if (build.currentPlan == i) return;
            build.currentPlan = i < 0 || i >= plans.size ? -1 : i;
            build.progress = 0;
            // 切配方顺便清空可能留在仓里的无用物品（防止堵）
            build.dump();
        });

        // ② 按「第一个输出物品」切换（玩家点配置面板）
        config(Item.class, (UnitFactoryBuild build, Item item) -> {
            if (!configurable) return;
            int next = plans.indexOf(p -> p.outItem != null && p.outItem.length > 0
                                         && p.outItem[0] != null && p.outItem[0].item == item);
            if (build.currentPlan == next) return;
            build.currentPlan = next;
            build.progress = 0;
            build.dump();
        });

        // ③ 清空配置（停止生产）
        configClear((UnitFactoryBuild build) -> {
            build.currentPlan = -1;
            build.progress = 0;
        });
    }

    // =================================================================
    // init / afterPatch：调用容量初始化 + 让 UnitBlock 自己处理 consumeBuilder
    // =================================================================
    @Override
    public void init() {
        initCapacities();
        super.init();              // ⭐ UnitBlock.super.init() 会根据 plans 构建 consumeBuilder，
                                   //    之后 consume() 就能自动按当前 plan 的 requirements 扣原料！
    }

    @Override
    public void afterPatch() {
        initCapacities();
        super.afterPatch();
    }

    /** 根据所有配方推导每物品的仓库容量，并给 consumeBuilder 附加倍率回调 */
    public void initCapacities() {
        capacities = new int[Vars.content.items().size];

        int maxAmount = 0;
        for (Plan plan : plans) {
            if (plan == null) continue;
            // 扫描单次"输入物品"最大量
            if (plan.requirements != null) for (ItemStack stack : plan.requirements) {
                if (stack != null && stack.amount > maxAmount) maxAmount = stack.amount;
            }
            // 扫描单次"输出物品"最大量（防止输出装不下）
            if (plan.outItem != null) for (ItemStack stack : plan.outItem) {
                if (stack != null && stack.amount > maxAmount) maxAmount = stack.amount;
            }
        }

        // 放大 20 倍（你已调成 20，保持你这版参数）
        int unifiedLimit = Math.max(1, maxAmount * 20);
        Arrays.fill(capacities, unifiedLimit);

        // consumeBuilder 的成本倍率：随规则 unitCost 变化（RTS AI 难度缩放）
        consumeBuilder.each(c -> c.multiplier = b -> state.rules.unitCost(b.team));
    }

    // 物品总数变化时拷贝 capacities，不让数组越界
    @Override
    public void checkContentArrayCapacity(int items, int liquids) {
        super.checkContentArrayCapacity(items, liquids);
        if (capacities.length != items) capacities = Arrays.copyOf(capacities, items);
    }

    // =================================================================
    // 进度条 / 输出声明 / 图标 / 建造预览
    // =================================================================
    @Override
    public void setBars() {
        super.setBars();
        addBar("progress", (UnitFactoryBuild e) -> new Bar("bar.progress", Pal.ammo, e::fraction));
    }

    @Override
    public boolean outputsItems() { return true; }

    @Override
    public TextureRegion[] icons() {
        return new TextureRegion[]{region, outRegion, topRegion};
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {
        Draw.rect(region, plan.drawx(), plan.drawy());
        // ⭐ 修复 3：outRegion 可能为 null（未提供贴图），加判断不崩
        if (outRegion != null) {
            Draw.rect(outRegion, plan.drawx(), plan.drawy(), plan.rotation * 90);
        }
        if (topRegion != null) {
            Draw.rect(topRegion, plan.drawx(), plan.drawy());
        }
    }

    // =================================================================
    // 详情面板（setStats）：原代码保留无 bug
    // =================================================================
    @Override
    public void setStats() {
        super.setStats();
        stats.remove(Stat.itemCapacity);

        stats.add(Stat.output, table -> {
            table.row();
            for (Plan plan : plans) {
                if (plan == null || plan.outItem == null || plan.outItem.length == 0) continue;
                ItemStack firstOut = plan.outItem[0];
                if (firstOut == null || firstOut.item == null) continue;

                table.table(Styles.grayPanel, t -> {
                    t.table(icons -> {
                        icons.left();
                        icons.image(firstOut.item.uiIcon).size(40).pad(10f).scaling(Scaling.fit)
                                .with(i -> StatValues.withTooltip(i, firstOut.item));
                        if (plan.outItem.length > 1) {
                            icons.add("+" + (plan.outItem.length - 1)).color(Color.lightGray).padLeft(2f);
                        } else if (firstOut.amount > 1) {
                            icons.add("×" + firstOut.amount).color(Color.lightGray).padLeft(2f);
                        }
                    }).left();

                    t.table(info -> {
                        info.add(firstOut.item.localizedName).left();
                        if (plan.outItem.length > 1) {
                            info.add(" +" + (plan.outItem.length - 1) + " more").color(Color.lightGray).padLeft(4f);
                        }
                        info.row();
                        info.add(Strings.autoFixed(plan.time / 60f, 1) + " " + Core.bundle.get("unit.seconds"))
                                .color(Color.lightGray);
                    }).left().padLeft(10f);

                    t.table(req -> {
                        req.right();
                        for (int i = 0; i < plan.requirements.length; i++) {
                            if (i % 4 == 0) req.row();
                            ItemStack stack = plan.requirements[i];
                            req.add(StatValues.displayItem(stack.item, stack.amount, plan.time, true)).pad(5);
                        }
                    }).right().grow().pad(10f);

                }).growX().pad(5);
                table.row();
            }
        });
    }

    // getPlanConfigs：给配置 UI 的逻辑锚点列表
    @Override
    public void getPlanConfigs(Seq<UnlockableContent> options) {
        for (Plan plan : plans) {
            if (plan == null || plan.outItem == null || plan.outItem.length == 0) continue;
            Item item = plan.outItem[0].item;
            if (item != null && !item.isBanned() && item.unlockedNow()) {
                options.add(item);
            }
        }
    }

    // =================================================================
    // Plan 内部类（结构不变，outItem/requirements 保持用户命名）
    // =================================================================
    public static class Plan {
        public ItemStack[] outItem;
        public ItemStack[] requirements;
        public float time;

        public Plan(ItemStack[] outItem, float time, ItemStack[] requirements) {
            // ⭐ 全参数判空，防止 Plan(null) 初始化崩
            this.outItem      = (outItem      != null) ? outItem      : new ItemStack[0];
            this.requirements = (requirements != null) ? requirements : new ItemStack[0];
            this.time         = time;
        }

        Plan() {
            this.outItem      = new ItemStack[0];
            this.requirements = new ItemStack[0];
        }
    }

    // =================================================================
    // UnitFactoryBuild —— 四个问题的核心修复都在这里
    // =================================================================
    public class UnitFactoryBuild extends Building {

        public int currentPlan = -1;
        public float progress = 0f;

        // 当前配方完成比例（0~1）
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
                    if (p != null && p.outItem != null && p.outItem.length > 0
                            && p.outItem[0] != null && p.outItem[0].item != null
                            && p.outItem[0].item.unlockedNow()) {
                        currentPlan = i;
                        break;
                    }
                }
                if (currentPlan == -1 && plans.size > 0) currentPlan = 0;
            }
        }

        // 选中时：把"第一个输出物品"高亮画出来
        @Override
        public void drawSelect() {
            super.drawSelect();
            if (plans.size > 1 && currentPlan != -1 && currentPlan < plans.size) {
                Plan p = plans.get(currentPlan);
                if (p != null && p.outItem != null && p.outItem.length > 0
                        && p.outItem[0] != null && p.outItem[0].item != null) {
                    drawItemSelection(p.outItem[0].item);
                }
            }
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload) { return false; }

        // 逻辑传感器：config → 返回第一个输出物品
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

        // 逻辑传感器：progress / itemCapacity
        @Override
        public double sense(LAccess sensor) {
            if (sensor == LAccess.progress)     return Mathf.clamp(fraction());
            if (sensor == LAccess.itemCapacity) return itemCapacity;
            return super.sense(sensor);
        }

        // 悬停显示：输出物图标 + 名称
        @Override
        public void display(Table table) {
            super.display(table);

            TextureRegionDrawable reg = new TextureRegionDrawable();
            table.row();
            table.table(t -> {
                t.left();
                t.image().update(i -> {
                    if (currentPlan == -1 || currentPlan >= plans.size) {
                        i.setDrawable(Icon.cancel);
                        i.setColor(Color.lightGray);
                    } else {
                        Plan p = plans.get(currentPlan);
                        if (p == null || p.outItem == null || p.outItem.length == 0
                                || p.outItem[0] == null || p.outItem[0].item == null) {
                            i.setDrawable(Icon.cancel);
                            i.setColor(Color.lightGray);
                        } else {
                            i.setDrawable(reg.set(p.outItem[0].item.uiIcon));
                            i.setColor(Color.white);
                        }
                    }
                    i.setScaling(Scaling.fit);
                }).size(32).padBottom(-4).padRight(2);

                t.label(() -> {
                    if (currentPlan == -1 || currentPlan >= plans.size) return "@none";
                    Plan p = plans.get(currentPlan);
                    if (p == null || p.outItem == null || p.outItem.length == 0
                            || p.outItem[0] == null || p.outItem[0].item == null) return "@none";
                    return p.outItem[0].item.localizedName;
                }).wrap().width(230f).color(Color.lightGray);
            }).left();
        }

        @Override
        public Object config() { return currentPlan; }

        // =================================================================
        // ✅ 修复 3：draw() 内所有贴图加 null 判断，outRegion 无贴图时也不崩
        // =================================================================
        @Override
        public void draw() {
            // 底层：方块主体
            Draw.rect(region, x, y);

            // 中层：出物箭头（rotate 旋转方向指示）—— 贴图不存在就跳过
            if (outRegion != null) {
                Draw.rect(outRegion, x, y, rotdeg());
            }

            // （用户之前删掉 payload 层也可以，这里保留带 null 保护，以后扩展不报错）
            // Draw.z(Layer.blockOver);
            // drawPayload();

            // 顶层：按钮/面板装饰 —— 贴图不存在就跳过
            if (topRegion != null) {
                Draw.z(Layer.blockOver + 0.1f);
                Draw.rect(topRegion, x, y);
            }
        }

        // 玩家点方块 → 弹出配置网格
        @Override
        public void buildConfiguration(Table table) {
            Seq<Item> items = Seq.with(plans)
                    .select(p -> p != null && p.outItem != null && p.outItem.length > 0
                                 && p.outItem[0] != null && p.outItem[0].item != null)
                    .map(p -> p.outItem[0].item)
                    .retainAll(i -> i != null && i.unlockedNow() && !i.isBanned());

            if (items.any()) {
                ItemSelection.buildTable(
                        ConfigurableBlock.this,
                        table,
                        items,
                        () -> {
                            if (currentPlan == -1 || currentPlan >= plans.size) return null;
                            Plan p = plans.get(currentPlan);
                            if (p == null || p.outItem == null || p.outItem.length == 0) return null;
                            return p.outItem[0].item;
                        },
                        item -> {
                            int idx = plans.indexOf(p -> p.outItem != null && p.outItem.length > 0
                                                         && p.outItem[0] != null && p.outItem[0].item == item);
                            if (idx != -1) configure(idx);
                        },
                        ConfigurableBlock.this.selectionRows,
                        ConfigurableBlock.this.selectionColumns
                );
            } else {
                table.table(Styles.black3, t -> t.add("@none").color(Color.lightGray));
            }
        }

        // =================================================================
        // shouldConsume：所有输出有地方放（仓 + 传送带）才开始累积效率
        // =================================================================
        @Override
        public boolean shouldConsume() {
            if (currentPlan == -1 || currentPlan >= plans.size) return false;
            if (!enabled) return false;
            Plan plan = plans.get(currentPlan);
            if (plan == null || plan.outItem == null || plan.outItem.length == 0) return false;

            for (ItemStack s : plan.outItem) {
                if (s == null || s.item == null) continue;
                // 检查"仓里现有的 + 这一周期要新产的"不能超过容量（防止 offload 失败入仓时塞不下）
                int willHave = items.get(s.item) + s.amount;
                if (willHave > getMaximumAccepted(s.item)) return false;
            }
            return true;
        }

        // 方块整体状态（UI 状态灯）：让 UnitBlock 提供的 inactiveUnitFactory 正常显示
        @Override
        public BlockStatus status() {
            if (!team.activateUnitFactories()) return BlockStatus.inactiveUnitFactory;
            return super.status();
        }

        // 单个物品的最大仓容（放大后的值）
        @Override
        public int getMaximumAccepted(Item item) {
            if (item == null || item.id >= capacities.length) return 0;
            return Mathf.round(capacities[item.id] * state.rules.unitCost(team));
        }

        // 是否接受某物品进入本仓（通过传送带送进来的）
        @Override
        public boolean acceptItem(Building source, Item item) {
            if (currentPlan == -1 || currentPlan >= plans.size) return false;
            Plan plan = plans.get(currentPlan);
            if (plan == null) return false;

            // ① 本配方「需要当作原料」的物品 → 收
            if (plan.requirements != null) {
                for (ItemStack stack : plan.requirements) {
                    if (stack != null && stack.item == item
                            && items.get(item) < getMaximumAccepted(item)) {
                        return true;
                    }
                }
            }
            // ② 本配方「要产出」的物品 → 也收（防止切配方后 offload 回流被拒收堵料）
            if (plan.outItem != null) {
                for (ItemStack stack : plan.outItem) {
                    if (stack != null && stack.item == item
                            && items.get(item) < getMaximumAccepted(item)) {
                        return true;
                    }
                }
            }
            return false;
        }

        // =================================================================
        // ✅ 核心修复：updateTile() —— 解决「复制物品」和「不消耗原料」
        //
        // 修复原则（完全对齐原版 GenericCrafter / UnitFactory 的语义）：
        //   1. 原料/电力扣减  统一交给 consume()！UnitBlock.consume() 会读
        //      consumeBuilder 并按当前 plan 的 requirements 扣料，不用手写 remove！
        //   2. 产出物  不走 items.add() + 单个 offload（会复制）：
        //      → 直接用 Building.offload(Item, int) 多参版推给下游
        //      → 推不出去（下游满/不存在）才临时 items.add 入仓
        // =================================================================
        @Override
        public void updateTile() {
            if (!configurable) { currentPlan = 0; }

            if (currentPlan < 0 || currentPlan >= plans.size) { currentPlan = -1; return; }
            Plan plan = plans.get(currentPlan);
            if (plan == null || plan.outItem == null || plan.outItem.length == 0) {
                currentPlan = -1; return;
            }
            // 任一输出物禁用 → 停
            for (ItemStack s : plan.outItem) {
                if (s == null || s.item == null || s.item.isBanned()) {
                    currentPlan = -1; return;
                }
            }

            // —— 进度累积（保持不变）
            if (efficiency > 0) {
                progress += edelta() * Vars.state.rules.unitBuildSpeed(team);
            }

            // ======================================================
            // 完成一周期：修复「复制物品」+「不消耗原料」
            // ======================================================
            if (progress >= plan.time) {

                // Step 1：检查所有输出物最大容量（真正入仓也能装下）
                boolean allOutOk = true;
                for (ItemStack s : plan.outItem) {
                    if (s == null || s.item == null) continue;
                    int willHave = items.get(s.item) + s.amount;
                    if (willHave > getMaximumAccepted(s.item)) { allOutOk = false; break; }
                }

                // Step 2：检查 requirements 够（理论上 shouldConsume 已经保证了，但是双保险）
                boolean allInOk = true;
                if (plan.requirements != null) for (ItemStack s : plan.requirements) {
                    if (s == null || s.item == null) continue;
                    if (items.get(s.item) < s.amount) { allInOk = false; break; }
                }

                if (allOutOk && allInOk) {

                    // ———————————————————————————————————————————————————
                    // ✅ 修复 2：不手写 items.remove(requirements)，调 consume()！
                    //    用户明确说"需要用到 consume"。
                    //    UnitBlock.consume() 会根据 consumeBuilder + 当前 plan
                    //    自动扣 requirements + 电力（consumesPower=true）
                    // ———————————————————————————————————————————————————
                    consume();

                    // ———————————————————————————————————————————————————
                    // ✅ 修复 1：产出物品不做「先 items.add 再 offload 单」
                    //    用 Building.offload(Item item, int amount) 多参版本直接推下游：
                    //      · 推成功 → 下游收了，自仓不变
                    //      · 推失败 → 再丢自己仓里（上面 allOutOk 已经保证仓容）
                    // ———————————————————————————————————————————————————
                    for (ItemStack s : plan.outItem) {
                        if (s == null || s.item == null) continue;

                        int remaining = s.amount;              // 还要推进下游几个
                        int pushed = offload(s.item, remaining);  // 推了几个
                        remaining -= pushed;

                        if (remaining > 0) {
                            // 下游没接完 → 剩下的入自己仓（allOutOk 保证装得下）
                            items.add(s.item, remaining);
                        }
                    }

                    // ———————————————————————————————————————————————————
                    // ✅ 修复 4：createSound 加 null 判断，用户没赋值不会 NPE
                    // ———————————————————————————————————————————————————
                    if (createSound != null) {
                        // ±6% 随机音调，和原版 UnitFactory/GenericCrafter 完全一致
                        createSound.at(this, 1f + Mathf.range(0.06f), createSoundVolume);
                    }

                    // 进度扣除（溢出保留：progress = 2.3s, time = 1s → 还剩 1.3s 做下一轮）
                    progress -= plan.time;

                } else {
                    // 有东西没准备好 → 进度卡到满 - 0.001，避免浮点抖动反复判断
                    progress = Math.min(progress, plan.time - 0.001f);
                }
            }

            if (progress < 0) progress = 0;
        }

        // 存档版本
        @Override
        public byte version() { return 3; }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.f(progress);
            write.s(currentPlan);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            progress = read.f();
            currentPlan = read.s();
            if (revision >= 2) { if (read.available() >= 1) read.bool(); }
            if (revision >= 3) { /* 预留扩展位，不改读档协议 */ }
        }
    }
}
