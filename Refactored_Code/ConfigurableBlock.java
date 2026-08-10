/*
 * ⚠️ 重构改版 - 仅放入 Refactored_Code 文件夹供用户核实
 * ⚠️ 不影响原始 src 目录下任何文件
 * ========================================================
 * 改动：
 *   1) Plan 扩展：支持多物品输出 + 液体(输入/输出)
 *   2) Recipe -> Plan 自动桥接：addRecipe(Recipe) / setRecipes(Recipe...)
 *   3) acceptItem 修复：支持多输出物判断（避免误拒绝）
 *   4) updateTile 修复：扣料 + 放料同时处理多物品/液体
 *   5) setStats 更新：显示多输出物列表、液体、多原料
 *   6) buildConfiguration / config(Item) 更新：按"首个输出物"作为索引锚点
 *   7) hasLiquids=true，开放液体仓
 *   8) getMaximumAccepted 对输出物做最大限制
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
        update = true;
        solid = true;
        hasItems = true;
        hasLiquids = true;              // ✅ 改版：启用液体仓
        hasPower = true;
        consumesPower = true;
        size = 2;
        health = 100;
        rotate = true;
        configurable = true;
        itemCapacity = 30;
        liquidCapacity = 30f;           // ✅ 改版：液体容量默认 30 单位

        // 按索引切换配方
        config(Integer.class, (UnitFactoryBuild build, Integer i) -> {
            if (!configurable) return;
            if (build.currentPlan == i) return;
            build.currentPlan = i < 0 || i >= plans.size ? -1 : i;
            build.progress = 0;
            dump();   // ✅ 改版：切换配方时把不再需要的原料/液体甩出去，避免堵仓
        });

        // 按物品切换配方（以"第一个输出物品"作锚点，向后兼容）
        config(Item.class, (UnitFactoryBuild build, Item item) -> {
            if (!configurable) return;
            int next = plans.indexOf(p -> p.outputItems != null && p.outputItems.length > 0
                                         && p.outputItems[0].item == item);
            if (build.currentPlan == next) return;
            build.currentPlan = next;
            build.progress = 0;
            dump();
        });

        // 清除配置
        configClear((UnitFactoryBuild build) -> {
            build.currentPlan = -1;
            build.progress = 0;
        });
    }

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

    /** 自动根据所有配方的最大单次量推导每物品/液体容量 */
    public void initCapacities() {
        capacities = new int[Vars.content.items().size];

        int maxAmount = 0;
        for (Plan plan : plans) {
            if (plan == null) continue;
            // 输入物品
            if (plan.inputItems != null) for (ItemStack stack : plan.inputItems) {
                if (stack.amount > maxAmount) maxAmount = stack.amount;
            }
            // 输出物品（也需要能放得下）
            if (plan.outputItems != null) for (ItemStack stack : plan.outputItems) {
                if (stack.amount > maxAmount) maxAmount = stack.amount;
            }
        }

        int unifiedLimit = Math.max(1, maxAmount * 6);
        Arrays.fill(capacities, unifiedLimit);

        consumeBuilder.each(c -> c.multiplier = b -> state.rules.unitCost(b.team));
    }

    @Override
    public void checkContentArrayCapacity(int items, int liquids) {
        super.checkContentArrayCapacity(items, liquids);
        if (capacities.length != items) capacities = Arrays.copyOf(capacities, items);
    }

    // ============================================================
    // ✅ 改版：Recipe -> Plan 桥接 API（向前兼容）
    // ============================================================

    /** 将单个 Recipe 转成 Plan 并追加到 plans */
    public Plan addRecipe(Npl.content.Recipe r) {
        Plan p = RecipeBridge.fromRecipe(r);
        plans.add(p);
        return p;
    }

    /** 批量设置 plans（覆盖原 plans） */
    public void setRecipes(Npl.content.Recipe... recipes) {
        plans.clear();
        for (Npl.content.Recipe r : recipes) {
            if (r != null) plans.add(RecipeBridge.fromRecipe(r));
        }
    }

    // ============================================================

    @Override
    public void setBars() {
        super.setBars();
        addBar("progress", (UnitFactoryBuild e) -> new Bar("bar.progress", Pal.ammo, e::fraction));
    }

    @Override
    public boolean outputsItems() { return true; }
    public  boolean outputsLiquids(){ return true; }  // ✅ 改版：可输出液体

    @Override
    public void setStats() {
        super.setStats();
        stats.remove(Stat.itemCapacity);

        stats.add(Stat.output, table -> {
            table.row();
            for (Plan plan : plans) {
                if (plan == null) continue;
                table.table(Styles.grayPanel, t -> {

                    // —— 左侧：所有输出物（物品 + 液体）
                    t.table(out -> {
                        out.left();
                        boolean anyOutput = false;

                        // 多物品输出
                        if (plan.outputItems != null) for (ItemStack s : plan.outputItems) {
                            if (s == null || s.item == null) continue;
                            out.image(s.item.uiIcon).size(40).pad(4f).scaling(Scaling.fit)
                                    .with(i -> StatValues.withTooltip(i, s.item));
                            if (s.amount > 1) {
                                out.add("×" + s.amount).color(Color.lightGray).padLeft(2f);
                            }
                            out.row();
                            anyOutput = true;
                        }
                        // 多液体输出
                        if (plan.outputLiquids != null) for (LiquidStack s : plan.outputLiquids) {
                            if (s == null || s.liquid == null) continue;
                            out.image(s.liquid.uiIcon).size(40).pad(4f).scaling(Scaling.fit)
                                    .with(i -> StatValues.withTooltip(i, s.liquid));
                            out.add(Strings.autoFixed(s.amount, 1) + "u").color(Color.lightGray).padLeft(2f);
                            out.row();
                            anyOutput = true;
                        }
                        if (!anyOutput) {
                            out.add("@none").color(Color.lightGray);
                        }
                    }).left();

                    // —— 中间：时间（若多液体则还显示液量）
                    t.table(info -> {
                        info.add("@output").left().color(Color.accent); info.row();
                        info.add(
                            (plan.outputItems != null ? plan.outputItems.length : 0) + " 种物品 / "
                          + (plan.outputLiquids != null ? plan.outputLiquids.length : 0) + " 种液体"
                        ).left().color(Color.lightGray); info.row();
                        info.add(Strings.autoFixed(plan.time / 60f, 1) + " " + Core.bundle.get("unit.seconds"))
                                .color(Color.lightGray);
                    }).left().padLeft(10f);

                    // —— 右侧：消耗原料（物品 + 液体）
                    t.table(req -> {
                        req.right();
                        int i = 0;
                        if (plan.inputItems != null) for (ItemStack stack : plan.inputItems) {
                            if (i % 4 == 0) req.row(); i++;
                            req.add(StatValues.displayItem(stack.item, stack.amount, plan.time, true)).pad(5);
                        }
                        if (plan.inputLiquids != null) for (LiquidStack stack : plan.inputLiquids) {
                            if (i % 4 == 0) req.row(); i++;
                            req.add(StatValues.displayLiquid(stack.liquid, stack.amount, plan.time, true)).pad(5);
                        }
                        if (i == 0) req.add("@none").color(Color.lightGray);
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
            // 兼容：把所有输出物/液体都暴露给逻辑，第一个物品作为锚点
            if (plan.outputItems != null) for (ItemStack s : plan.outputItems) {
                if (s != null && s.item != null && !s.item.isBanned() && s.item.unlockedNow()) {
                    options.add(s.item);
                }
            }
            if (plan.outputLiquids != null) for (LiquidStack s : plan.outputLiquids) {
                if (s != null && s.liquid != null && !s.liquid.isBanned() && s.liquid.unlockedNow()) {
                    options.add(s.liquid);
                }
            }
        }
    }

    @Override
    public TextureRegion[] icons() {
        return new TextureRegion[]{region, outRegion, topRegion};
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {
        Draw.rect(region, plan.drawx(), plan.drawy());
        Draw.rect(outRegion, plan.drawx(), plan.drawy(), plan.rotation * 90);
        Draw.rect(topRegion, plan.drawx(), plan.drawy());
    }

    @Override
    public void buildConfiguration(Table table) {
        // 以"第一个输出物品"作为可点击 UI 条目
        Seq<Item> items = Seq.with(plans)
                .map(p -> (p != null && p.outputItems != null && p.outputItems.length > 0) ? p.outputItems[0].item : null)
                .retainAll(i -> i != null && i.unlockedNow() && !i.isBanned());

        if (items.any()) {
            ItemSelection.buildTable(
                    this,
                    table,
                    items,
                    () -> {
                        int cur = ((UnitFactoryBuild) Vars.control.input.config.blockBuild()).currentPlan;
                        if (cur < 0 || cur >= plans.size) return null;
                        Plan p = plans.get(cur);
                        return (p != null && p.outputItems != null && p.outputItems.length > 0)
                                ? p.outputItems[0].item : null;
                    },
                    item -> {
                        int idx = plans.indexOf(p -> p.outputItems != null && p.outputItems.length > 0
                                                    && p.outputItems[0].item == item);
                        if (idx != -1) configure(idx);
                    },
                    selectionRows,
                    selectionColumns
            );
        } else {
            table.table(Styles.black3, t -> t.add("@none").color(Color.lightGray));
        }
    }

    @Override
    public boolean acceptPayload(Building source, Payload payload) {
        return false;
    }

    // ============================================================
    // 内部类 Plan（改版：多物品 + 液体 I/O）
    // ============================================================
    public static class Plan {
        /** 输出物品（可多个）。向后兼容：原先 outItem = outputItems[0] */
        public ItemStack[] outputItems   = {};
        /** 输入物品（可多个）；向后兼容：requirements 字段等同 inputItems */
        public ItemStack[] inputItems    = {};
        /** 输出液体（可多个） */
        public LiquidStack[] outputLiquids = {};
        /** 输入液体（可多个） */
        public LiquidStack[] inputLiquids  = {};
        public float time = 60f;

        // 向后兼容：让旧代码访问 plan.outItem / plan.requirements 还能正常工作
        /** @deprecated 向后兼容字段，读取 outputItems[0] */
        @Deprecated
        public ItemStack outItem;
        /** @deprecated 向后兼容字段，读取 inputItems */
        @Deprecated
        public ItemStack[] requirements;

        public Plan(ItemStack[] outputItems, ItemStack[] inputItems,
                    LiquidStack[] outputLiquids, LiquidStack[] inputLiquids, float time) {
            this.outputItems   = outputItems   != null ? outputItems   : new ItemStack[0];
            this.inputItems    = inputItems    != null ? inputItems    : new ItemStack[0];
            this.outputLiquids = outputLiquids != null ? outputLiquids : new LiquidStack[0];
            this.inputLiquids  = inputLiquids  != null ? inputLiquids  : new LiquidStack[0];
            this.time = time;
            // 给旧字段别名赋值，保证 legacy 代码不 NPE
            this.outItem        = this.outputItems.length > 0 ? this.outputItems[0] : null;
            this.requirements   = this.inputItems;
        }

        Plan() {
            outputItems = new ItemStack[0];
            inputItems  = new ItemStack[0];
            outputLiquids = new LiquidStack[0];
            inputLiquids  = new LiquidStack[0];
            requirements = inputItems;
        }

        /** 传统三参构造（单输出+原料数组+时间），原老代码不报错 */
        public Plan(ItemStack outItem, float time, ItemStack[] requirements) {
            this.outputItems   = outItem != null ? new ItemStack[]{ outItem } : new ItemStack[0];
            this.inputItems    = requirements != null ? requirements : new ItemStack[0];
            this.outputLiquids = new LiquidStack[0];
            this.inputLiquids  = new LiquidStack[0];
            this.time = time;
            this.outItem = outItem;
            this.requirements = this.inputItems;
        }
    }

    // ============================================================
    // Recipe -> Plan 桥接器
    // ============================================================
    public static class RecipeBridge {
        public static Plan fromRecipe(Npl.content.Recipe r) {
            ItemStack[]   outI  = r.outputItems   != null ? r.outputItems.toArray(ItemStack.class)   : null;
            ItemStack[]   inI   = r.inputItems    != null ? r.inputItems.toArray(ItemStack.class)    : null;
            LiquidStack[] outL  = r.outputLiquid  != null ? r.outputLiquid.toArray(LiquidStack.class) : null;
            LiquidStack[] inL   = r.inputLiquid   != null ? r.inputLiquid.toArray(LiquidStack.class)  : null;
            return new Plan(outI, inI, outL, inL, r.craftTime);
        }
    }

    // ============================================================
    // 建筑实体类 UnitFactoryBuild（更新：多物品+液体）
    // ============================================================
    public class UnitFactoryBuild extends Building {

        public int currentPlan = -1;
        public float progress = 0f;

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
                    if (p != null && ((p.outputItems != null && p.outputItems.length > 0 && p.outputItems[0].item != null && p.outputItems[0].item.unlockedNow())
                                   || (p.outputLiquids != null && p.outputLiquids.length > 0 && p.outputLiquids[0].liquid != null && p.outputLiquids[0].liquid.unlockedNow()))) {
                        currentPlan = i;
                        break;
                    }
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
                    // 先物品后液体，挑第一个有效的画选择圈
                    if (p.outputItems != null && p.outputItems.length > 0 && p.outputItems[0].item != null) {
                        drawItemSelection(p.outputItems[0].item);
                    } else if (p.outputLiquids != null && p.outputLiquids.length > 0 && p.outputLiquids[0].liquid != null) {
                        drawLiquidSelection(p.outputLiquids[0].liquid);
                    }
                }
            }
        }

        @Override
        public Object senseObject(LAccess sensor) {
            if (sensor == LAccess.config) {
                if (currentPlan == -1 || currentPlan >= plans.size) return null;
                Plan p = plans.get(currentPlan);
                if (p == null) return null;
                // 向后兼容：优先返回第一个输出物品
                if (p.outputItems != null && p.outputItems.length > 0) return p.outputItems[0].item;
                if (p.outputLiquids != null && p.outputLiquids.length > 0) return p.outputLiquids[0].liquid;
                return null;
            }
            return super.senseObject(sensor);
        }

        @Override
        public double sense(LAccess sensor) {
            if (sensor == LAccess.progress) return Mathf.clamp(fraction());
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
                    Item icon = null;
                    if (currentPlan != -1 && currentPlan < plans.size) {
                        Plan p = plans.get(currentPlan);
                        if (p != null && p.outputItems != null && p.outputItems.length > 0 && p.outputItems[0].item != null) {
                            icon = p.outputItems[0].item;
                        }
                    }
                    if (icon == null) {
                        i.setDrawable(Icon.cancel);
                        i.setColor(Color.lightGray);
                    } else {
                        i.setDrawable(reg.set(icon.uiIcon));
                        i.setColor(Color.white);
                    }
                    i.setScaling(Scaling.fit);
                }).size(32).padBottom(-4).padRight(2);

                t.label(() -> {
                    if (currentPlan == -1 || currentPlan >= plans.size) return "@none";
                    Plan p = plans.get(currentPlan);
                    if (p == null) return "@none";
                    int nI = p.outputItems   != null ? p.outputItems.length   : 0;
                    int nL = p.outputLiquids != null ? p.outputLiquids.length : 0;
                    StringBuilder sb = new StringBuilder();
                    if (nI > 0 && p.outputItems[0].item != null) sb.append(p.outputItems[0].item.localizedName);
                    if (nI + nL > 1) sb.append(" +").append(nI + nL - 1);
                    return sb.length() == 0 ? "@none" : sb.toString();
                }).wrap().width(230f).color(Color.lightGray);
            }).left();
        }

        @Override
        public Object config() {
            return currentPlan;
        }

        @Override
        public void draw() {
            Draw.rect(region, x, y);
            Draw.rect(outRegion, x, y, rotdeg());

            if (currentPlan != -1 && currentPlan < plans.size && fraction() > 0) {
                Plan p = plans.get(currentPlan);
                if (p != null) {
                    // 显示"第一个有效输出物"的渐显动画
                    TextureRegion icon = null;
                    if (p.outputItems != null && p.outputItems.length > 0 && p.outputItems[0].item != null) {
                        icon = p.outputItems[0].item.uiIcon;
                    } else if (p.outputLiquids != null && p.outputLiquids.length > 0 && p.outputLiquids[0].liquid != null) {
                        icon = p.outputLiquids[0].liquid.uiIcon;
                    }
                    if (icon != null) {
                        Draw.z(Layer.blockOver);
                        Draw.color(Pal.accent);
                        Draw.alpha(0.5f);
                        Draw.rect(icon, x, y, 32 * fraction());
                        Draw.color();
                    }
                }
            }

            Draw.z(Layer.blockOver);
            drawPayload();
            Draw.z(Layer.blockOver + 0.1f);
            Draw.rect(topRegion, x, y);
        }

        /** ✅ 改版：同时检查所有物品输出 + 所有液体输出都有空间 */
        private boolean canAcceptAllOutputs(Plan plan) {
            if (plan == null) return false;
            // 物品
            if (plan.outputItems != null) for (ItemStack s : plan.outputItems) {
                if (s == null || s.item == null) continue;
                if (getMaximumAccepted(s.item) < s.amount) return false;
            }
            // 液体
            if (plan.outputLiquids != null) for (LiquidStack s : plan.outputLiquids) {
                if (s == null || s.liquid == null) continue;
                if (liquids.get(s.liquid) + s.amount > liquidCapacity) return false;
            }
            return true;
        }

        /** ✅ 改版：检查所有输入（物品+液体）是否充足 */
        private boolean hasAllInputs(Plan plan) {
            if (plan == null) return false;
            if (plan.inputItems != null) for (ItemStack s : plan.inputItems) {
                if (s == null || s.item == null) continue;
                if (items.get(s.item) < s.amount) return false;
            }
            if (plan.inputLiquids != null) for (LiquidStack s : plan.inputLiquids) {
                if (s == null || s.liquid == null) continue;
                if (liquids.get(s.liquid) < s.amount) return false;
            }
            return true;
        }

        @Override
        public void updateTile() {
            if (!configurable) currentPlan = 0;

            if (currentPlan < 0 || currentPlan >= plans.size) {
                currentPlan = -1;
                return;
            }

            Plan plan = plans.get(currentPlan);
            if (plan == null) { currentPlan = -1; return; }
            boolean anyOutItem   = plan.outputItems   != null && plan.outputItems.length > 0   && plan.outputItems[0].item != null;
            boolean anyOutLiquid = plan.outputLiquids != null && plan.outputLiquids.length > 0 && plan.outputLiquids[0].liquid != null;
            boolean outputBanned = (anyOutItem   && plan.outputItems[0].item.isBanned())
                                 ||(anyOutLiquid && plan.outputLiquids[0].liquid.isBanned());
            if (!anyOutItem && !anyOutLiquid) { currentPlan = -1; return; }
            if (outputBanned) { currentPlan = -1; return; }

            if (efficiency > 0) {
                progress += edelta() * Vars.state.rules.unitBuildSpeed(team);
            }

            if (progress >= plan.time) {
                if (canAcceptAllOutputs(plan) && hasAllInputs(plan)) {
                    // —— 扣输入 ——
                    if (plan.inputItems != null) for (ItemStack s : plan.inputItems) {
                        items.remove(s.item, s.amount);
                    }
                    if (plan.inputLiquids != null) for (LiquidStack s : plan.inputLiquids) {
                        liquids.remove(s.liquid, s.amount);
                    }
                    // —— 放输出 ——
                    if (plan.outputItems != null) for (ItemStack s : plan.outputItems) {
                        items.add(s.item, s.amount);
                    }
                    if (plan.outputLiquids != null) for (LiquidStack s : plan.outputLiquids) {
                        liquids.add(s.liquid, s.amount);
                    }
                    // 溢出给下游（原版 offload 仅对物品，液体用 handleLiquid 自动排出）
                    if (plan.outputItems != null) for (ItemStack s : plan.outputItems) {
                        offload(s.item);
                    }
                    progress -= plan.time;
                    consume();
                    if (createSound != null) {
                        createSound.at(this, 1f + Mathf.range(0.06f), createSoundVolume);
                    }
                } else {
                    progress = Math.min(progress, plan.time - 0.001f);
                }
            }

            if (progress < 0) progress = 0;
        }

        @Override
        public boolean shouldConsume() {
            if (currentPlan == -1 || currentPlan >= plans.size) return false;
            if (!enabled) return false;
            Plan plan = plans.get(currentPlan);
            if (plan == null) return false;
            return canAcceptAllOutputs(plan);
        }

        @Override
        public BlockStatus status() {
            if (!team.activateUnitFactories()) return BlockStatus.inactiveUnitFactory;
            return super.status();
        }

        @Override
        public int getMaximumAccepted(Item item) {
            if (item == null || item.id >= capacities.length) return 0;
            return Mathf.round(capacities[item.id] * state.rules.unitCost(team));
        }

        /** ✅ 改版：接受当前配方所需的原料物品 + 当前配方/所有输出物品（避免 offload 出不去） */
        @Override
        public boolean acceptItem(Building source, Item item) {
            if (currentPlan == -1 || currentPlan >= plans.size) return false;
            Plan plan = plans.get(currentPlan);
            if (plan == null) return false;

            // 输入需要？
            if (plan.inputItems != null) {
                for (ItemStack stack : plan.inputItems) {
                    if (stack.item == item && items.get(item) < getMaximumAccepted(item)) return true;
                }
            }
            // 本配方产出的物品也接受（切换配方后可能需要重新入仓）
            if (plan.outputItems != null) {
                for (ItemStack stack : plan.outputItems) {
                    if (stack.item == item && items.get(item) < getMaximumAccepted(item)) return true;
                }
            }
            return false;
        }

        /** ✅ 改版：接受当前配方所需的液体输入 + 输出液体 */
        @Override
        public boolean acceptLiquid(Building source, Liquid liquid, float amount) {
            if (!super.acceptLiquid(source, liquid, amount)) return false;
            if (currentPlan == -1 || currentPlan >= plans.size) return false;
            Plan plan = plans.get(currentPlan);
            if (plan == null) return false;
            boolean need = false;
            if (plan.inputLiquids != null) for (LiquidStack s : plan.inputLiquids) if (s.liquid == liquid) { need = true; break; }
            if (!need && plan.outputLiquids != null) for (LiquidStack s : plan.outputLiquids) if (s.liquid == liquid) { need = true; break; }
            return need && liquids.get(liquid) + amount <= liquidCapacity;
        }

        @Override
        public byte version() {
            return 4;   // ✅ 改版：version 从 3 → 4，加液体 + 多物品状态
        }

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
            if (revision >= 3) { /* 占位 */ }
            // v4 目前未加新字段，留作未来扩展口；若将来要写 currentOutputLiquidIdx 等，加在 revision>=4
            if (revision >= 4) { /* 预留 */ }
        }
    }
}
