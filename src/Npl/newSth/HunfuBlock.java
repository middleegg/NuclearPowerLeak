package Npl.newSth;

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
    public int[] capacities = {};
    public float[] liquidCapacities = {};
    public Seq<Plan> plans = new Seq<>(4);
    public int selectionRows = 2;
    public Effect craftEffect = Fx.none;
    public int selectionColumns = 4;

    // ========== 构造器：方块注册阶段调用 ==========
    public HunfuBlock(String name) {
        super(name);
        update = true;
        solid = true;
        hasItems = true;
        hasPower = true;
        hasLiquids = true;
        consumesPower = true;
        craftEffect = Fx.lava;
        size = 2;
        health = 100;
        rotate = false;
        configurable = true;
        itemCapacity = 30;
        liquidCapacity = 30f;

        // ① 整数索引：逻辑处理器切配方
        config(Integer.class, (HunfuBuild build, Integer i) -> {
            if (!configurable) return;
            if (build.currentPlan == i) return;
            build.currentPlan = i < 0 || i >= plans.size ? -1 : i;
            build.progress = 0;
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
        // ③ 清空
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

    public void initCapacities() {
        capacities = new int[Vars.content.items().size];
        int maxItem = 0;
        liquidCapacities = new float[Vars.content.liquids().size];
        float maxLiquid = 0f;

        for (Plan plan : plans) {
            if (plan == null) continue;
            if (plan.requirements != null) for (ItemStack stack : plan.requirements)
                if (stack != null && stack.amount > maxItem) maxItem = stack.amount;
            if (plan.outItem != null) for (ItemStack stack : plan.outItem)
                if (stack != null && stack.amount > maxItem) maxItem = stack.amount;
            if (plan.inLiquid != null) for (LiquidStack stack : plan.inLiquid)
                if (stack != null && stack.amount > maxLiquid) maxLiquid = stack.amount;
            if (plan.outLiquid != null) for (LiquidStack stack : plan.outLiquid)
                if (stack != null && stack.amount > maxLiquid) maxLiquid = stack.amount;
        }

        int unifiedItem = Math.max(1, maxItem * 10);
        Arrays.fill(capacities, unifiedItem);
        float unifiedLiquid = Math.max(1f, maxLiquid * 10f);
        Arrays.fill(liquidCapacities, unifiedLiquid);
    }

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

    @Override
    public boolean outputsItems()   { return true; }
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
        stats.remove(Stat.liquidCapacity);

        stats.add(Stat.output, table -> {
            table.row();
            for (Plan plan : plans) {
                if (plan == null) continue;
                ItemStack firstOutItem = (plan.outItem   != null && plan.outItem.length   > 0) ? plan.outItem[0]   : null;
                LiquidStack firstOutLiq = (plan.outLiquid != null && plan.outLiquid.length > 0) ? plan.outLiquid[0] : null;
                // ★ 允许只有 coinOutput 没有物品/液体输出（消耗原料产 coins 配方）
                boolean hasAnyOut = (firstOutItem != null && firstOutItem.item != null)
                                 || (firstOutLiq  != null && firstOutLiq.liquid != null)
                                 || (plan.coinOutput > 0);
                if (!hasAnyOut) continue;

                table.table(Styles.grayPanel, t -> {
                    // ── 左：配方代表图标 + coins 标签 ──
                    t.table(icons -> {
                        icons.left();
                        if (firstOutItem != null && firstOutItem.item != null) {
                            icons.image(firstOutItem.item.uiIcon).size(40).pad(10f).scaling(Scaling.fit)
                                    .with(i -> StatValues.withTooltip(i, firstOutItem.item));
                        } else if (firstOutLiq != null && firstOutLiq.liquid != null) {
                            icons.image(firstOutLiq.liquid.uiIcon).size(40).pad(10f).scaling(Scaling.fit)
                                    .with(i -> StatValues.withTooltip(i, firstOutLiq.liquid));
                        } else if (plan.coinOutput > 0) {
                            // 纯产 coins 配方：用文字占位（无对应图标）
                            icons.add("COINS").color(Color.valueOf("E2FF6D")).pad(10f);
                        }
                        int outCount = 0;
                        if (plan.outItem   != null) outCount += plan.outItem.length;
                        if (plan.outLiquid != null) outCount += plan.outLiquid.length;
                        if (outCount > 1) icons.add(" +" + (outCount - 1)).color(Color.lightGray).padLeft(2f);
                        icons.row();
                        // ★ coins 消耗
                        if (plan.coinCost > 0) {
                            icons.add(plan.coinCost + " coins/craft").color(Color.valueOf("FF6F6F")).padTop(2f).left();
                        }
                        // ★ coins 产出
                        if (plan.coinOutput > 0) {
                            icons.add(plan.coinOutput + " coins+").color(Color.valueOf("E2FF6D")).padTop(2f).left();
                        }
                    }).left();

                    // ── 中：名字 + 耗时 ──
                    t.table(info -> {
                        if (firstOutItem != null && firstOutItem.item != null)
                            info.add(firstOutItem.item.localizedName).left();
                        else if (firstOutLiq != null && firstOutLiq.liquid != null)
                            info.add(firstOutLiq.liquid.localizedName).left();
                        else if (plan.coinOutput > 0)
                            info.add("硬币 (coins)").left();
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

                    // ── 右：所有原料（物品+液体）──
                    t.table(req -> {
                        req.right();
                        int idx = 0;
                        if (plan.requirements != null) for (ItemStack stack : plan.requirements) {
                            if (idx++ % 4 == 0) req.row();
                            req.add(StatValues.displayItem(stack.item, stack.amount, plan.time, true)).pad(5);
                        }
                        if (plan.inLiquid != null) for (LiquidStack stack : plan.inLiquid) {
                            if (idx++ % 4 == 0) req.row();
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
            if (plan.outItem != null && plan.outItem.length > 0 && plan.outItem[0] != null && plan.outItem[0].item != null) {
                Item it = plan.outItem[0].item;
                if (it.unlockedNow()) options.add(it);
            } else if (plan.outLiquid != null && plan.outLiquid.length > 0 && plan.outLiquid[0] != null && plan.outLiquid[0].liquid != null) {
                Liquid lq = plan.outLiquid[0].liquid;
                if (lq.unlockedNow()) options.add(lq);
            }
            // ★ 纯 coinOutput 的配方（没有物品/液体输出）：暂时不在 getPlanConfigs 暴露（因为 UnlockableContent 里没有 coins）
            //   用户可以通过「整数索引」config(数字) 或「物品匹配」方式切换到其他配方。
        }
    }

    // =================================================================
    // 【Plan 内部类】= 一份配方（物品 + 液体 + coins）
    // =================================================================
    public static class Plan {
        public ItemStack[]   outItem;
        public ItemStack[]   requirements;
        public LiquidStack[] outLiquid;
        public LiquidStack[] inLiquid;
        public float         time;
        /** ★ 新增：每轮合成消耗 coins 数量（0 = 不消耗）*/
        public int           coinCost;
        /** ★ 新增：每轮合成生产 coins 数量（0 = 不生产）*/
        public int           coinOutput;

        /* ============== 所有构造器（含新增 coins 相关签名）============== */

        /** ★ 全参：物品出/入 + 液体出/入 + 耗时 + coinCost + coinOutput */
        public Plan(ItemStack[] outItem, float time, ItemStack[] requirements,
                    LiquidStack[] outLiquid, LiquidStack[] inLiquid,
                    int coinCost, int coinOutput) {
            this.outItem      = (outItem      != null) ? outItem      : new ItemStack[0];
            this.requirements = (requirements != null) ? requirements : new ItemStack[0];
            this.outLiquid    = (outLiquid    != null) ? outLiquid    : new LiquidStack[0];
            this.inLiquid     = (inLiquid     != null) ? inLiquid     : new LiquidStack[0];
            this.time         = time;
            this.coinCost     = coinCost;
            this.coinOutput   = coinOutput;
        }
        /** ★ 全参（coinCost 单参版，含旧签名兼容）：物品+液体+耗时+coinCost */
        public Plan(ItemStack[] outItem, float time, ItemStack[] requirements,
                    LiquidStack[] outLiquid, LiquidStack[] inLiquid, int coinCost) {
            this(outItem, time, requirements, outLiquid, inLiquid, coinCost, 0);
        }
        /** ★★ 兼容旧签名（物品出/入 + 液体出/入 + 耗时）→ 不消耗也不产 coins（原有配方 100% 兼容）*/
        public Plan(ItemStack[] outItem, float time, ItemStack[] requirements,
                    LiquidStack[] outLiquid, LiquidStack[] inLiquid) {
            this(outItem, time, requirements, outLiquid, inLiquid, 0, 0);
        }
        /** 兼容老签名：只有物品出/入 + 耗时 */
        public Plan(ItemStack[] outItem, float time, ItemStack[] requirements) {
            this(outItem, time, requirements, null, null, 0, 0);
        }
        /** ★ 便捷：物品出/入 + 耗时 + coinCost（最常用，消耗 coins 出物品）*/
        public Plan(ItemStack[] outItem, float time, ItemStack[] requirements, int coinCost) {
            this(outItem, time, requirements, null, null, coinCost, 0);
        }
        /** ★ 便捷：物品出/入 + 耗时 + coinCost + coinOutput（消耗 coins + 物品，出物品 + 少量 coins）*/
        public Plan(ItemStack[] outItem, float time, ItemStack[] requirements, int coinCost, int coinOutput) {
            this(outItem, time, requirements, null, null, coinCost, coinOutput);
        }
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
    // =================================================================
    public class HunfuBuild extends Building {

        public int   currentPlan = -1;
        public float progress    = 0f;
        public float warmup;
        public float totalProgress;

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
                                  || (p.outLiquid != null && p.outLiquid.length > 0 && p.outLiquid[0] != null && p.outLiquid[0].liquid != null && p.outLiquid[0].liquid.unlockedNow())
                                  || (p.coinOutput > 0);  // ★ 纯产 coins 配方也可以自动当选
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
                    if (p.outItem != null && p.outItem.length > 0 && p.outItem[0] != null && p.outItem[0].item != null) {
                        drawItemSelection(p.outItem[0].item);
                    }
                }
            }
        }

        @Override
        public boolean acceptPayload(Building source, mindustry.world.blocks.payloads.Payload payload) { return false; }

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
            return false;
        }

        @Override
        public Object senseObject(LAccess sensor) {
            if (sensor == LAccess.config) {
                if (currentPlan == -1 || currentPlan >= plans.size) return null;
                Plan p = plans.get(currentPlan);
                if (p == null) return null;
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
                        } else if (p.coinOutput > 0) {
                            // ★ 纯产 coins：用 cancel 图标 + 黄绿色占位
                            i.setDrawable(Icon.cancel);
                            i.setColor(Color.valueOf("E2FF6D"));
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
                    if (p.coinOutput > 0) return "硬币 (coins)";
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
                // ★ 纯产 coins 的配方（没有物品/液体输出）：暂时不加入配方面板候选（没有 UnlockableContent 可对应）
                //   可以通过「整数索引」config(Integer) 切换，或用逻辑处理器控制。
            }
            if (candidates.any()) {
                int idx = 0;
                for (UnlockableContent uc : candidates) {
                    if (idx % selectionColumns == 0) table.row();
                    Item item = (uc instanceof Item it) ? it : null;
                    Liquid liq = (uc instanceof Liquid lq) ? lq : null;
                    ImageButton b = table.button(Tex.whiteui, Styles.squareTogglei, () -> {
                        if (item != null) configure(item);
                        else if (liq != null) {
                            int pi = plans.indexOf(p ->
                                    p.outLiquid != null && p.outLiquid.length > 0
                                            && p.outLiquid[0] != null && p.outLiquid[0].liquid == liq);
                            if (pi >= 0) configure(pi);
                        }
                    }).size(50f).get();
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

        // ========== shouldConsume：物品 + 液体 + coins 三检查 ==========
        @Override
        public boolean shouldConsume() {
            if (currentPlan == -1 || currentPlan >= plans.size) return false;
            if (!enabled) return false;
            Plan plan = plans.get(currentPlan);
            if (plan == null) return false;
            // ★ hasAnyOut：允许「只有 coinOutput（纯产 coins）」的配方
            boolean hasAnyOut = (plan.outItem != null && plan.outItem.length > 0)
                             || (plan.outLiquid != null && plan.outLiquid.length > 0)
                             || (plan.coinOutput > 0);
            if (!hasAnyOut) return false;

            // 物品产物容量
            if (plan.outItem != null) for (ItemStack s : plan.outItem) {
                if (s == null || s.item == null) continue;
                int willHave = items.get(s.item) + s.amount;
                if (willHave > getMaximumAccepted(s.item)) return false;
            }
            // 液体产物容量
            if (plan.outLiquid != null) for (LiquidStack s : plan.outLiquid) {
                if (s == null || s.liquid == null) continue;
                float willHave = liquids.get(s.liquid) + s.amount;
                if (willHave > getMaximumAccepted(s.liquid)) return false;
            }
            // 物品原料
            if (plan.requirements != null) for (ItemStack s : plan.requirements) {
                if (s == null || s.item == null) continue;
                if (items.get(s.item) < s.amount) return false;
            }
            // 液体原料
            if (plan.inLiquid != null) for (LiquidStack s : plan.inLiquid) {
                if (s == null || s.liquid == null) continue;
                if (liquids.get(s.liquid) < s.amount) return false;
            }
            // ★ coins 够不够（消耗配方）
            if (plan.coinCost > 0 && coins.getAmount() < plan.coinCost) return false;
            return true;
        }

        @Override
        public BlockStatus status() {
            // —— Mindustry 159.6 BlockStatus 只有 6 个枚举值：
            //    active(绿) / noOutput(橙) / noInput(红) / logicDisable(紫) /
            //    inactiveUnitFactory(灰) / inactive(灰)
            if (currentPlan == -1 || currentPlan >= plans.size) return BlockStatus.noInput;
            Plan plan = plans.get(currentPlan);
            if (plan == null) return BlockStatus.noInput;
            boolean hasAnyOut = (plan.outItem != null && plan.outItem.length > 0)
                             || (plan.outLiquid != null && plan.outLiquid.length > 0)
                             || (plan.coinOutput > 0);
            if (!hasAnyOut) return BlockStatus.noInput;
            if (!enabled) return BlockStatus.logicDisable;

            // —— 产物满仓：noOutput（橙）——
            if (plan.outItem != null) for (ItemStack st : plan.outItem) {
                if (st == null || st.item == null) continue;
                if (items.get(st.item) + st.amount > getMaximumAccepted(st.item)) return BlockStatus.noOutput;
            }
            if (plan.outLiquid != null) for (LiquidStack st : plan.outLiquid) {
                if (st == null || st.liquid == null) continue;
                if (liquids.get(st.liquid) + st.amount > getMaximumAccepted(st.liquid)) return BlockStatus.noOutput;
            }

            // —— 缺 coins / 缺物品 / 缺液体：noInput（红）——
            if (plan.coinCost > 0 && coins.getAmount() < plan.coinCost) return BlockStatus.noInput;
            if (plan.requirements != null) for (ItemStack st : plan.requirements) {
                if (st == null || st.item == null) continue;
                if (items.get(st.item) < st.amount) return BlockStatus.noInput;
            }
            if (plan.inLiquid != null) for (LiquidStack st : plan.inLiquid) {
                if (st == null || st.liquid == null) continue;
                if (liquids.get(st.liquid) < st.amount) return BlockStatus.noInput;
            }

            // —— 所有条件都满足且有电真的在推进：active（绿）——
            if (efficiency > 0f && progress > 0f) return BlockStatus.active;
            // 有电但等原料中或进度刚开始：灰 inactive
            return BlockStatus.inactive;
        }

        @Override
        public int getMaximumAccepted(Item item) {
            if (item == null || item.id >= capacities.length) return 0;
            return capacities[item.id];
        }
        public float getMaximumAccepted(Liquid liquid) {
            if (liquid == null || liquid.id >= liquidCapacities.length) return 0f;
            return liquidCapacities[liquid.id];
        }

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
        // ✅ 核心 updateTile：HunfuBuild 同款 + 额外扣 coins / 产 coins
        // =================================================================
        @Override
        public void updateTile() {
            if (!configurable) currentPlan = 0;
            if (currentPlan < 0 || currentPlan >= plans.size) { currentPlan = -1; return; }
            Plan plan = plans.get(currentPlan);
            // ★ 允许只有 coinOutput 没有物品/液体输出的配方开工
            boolean hasAnyOut = (plan != null) && (
                    (plan.outItem != null && plan.outItem.length > 0) ||
                    (plan.outLiquid != null && plan.outLiquid.length > 0) ||
                    (plan.coinOutput > 0));
            if (plan == null || !hasAnyOut) { currentPlan = -1; return; }

            // Step 1：进度累积（只有 shouldConsume 通过才累积进度）
            if (efficiency > 0 && shouldConsume()) {
                progress += edelta();
                warmup = Mathf.approachDelta(warmup, 1f, 0.1f);
                totalProgress += warmup * Time.delta;
            } else {
                warmup = Mathf.approachDelta(warmup, 0f, 0.1f);
            }

            // Step 2：进度满 → 扣原料 + 扣 coins + 出产物 + 出 coins
            if (progress >= plan.time) {
                // ① 检查：物品/液体产物容量
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
                // ② 检查：物品/液体原料
                boolean allInOk = true;
                if (plan.requirements != null) for (ItemStack s : plan.requirements) {
                    if (s == null || s.item == null) continue;
                    if (items.get(s.item) < s.amount) { allInOk = false; break; }
                }
                if (allInOk && plan.inLiquid != null) for (LiquidStack s : plan.inLiquid) {
                    if (s == null || s.liquid == null) continue;
                    if (liquids.get(s.liquid) < s.amount) { allInOk = false; break; }
                }
                // ③ ★ 检查 coins（消耗配方）
                boolean coinsOk = (plan.coinCost <= 0) || (coins.getAmount() >= plan.coinCost);

                if (allOutOk && allInOk && coinsOk) {
                    // —— 扣物品 + 液体原料 ——
                    if (plan.requirements != null) for (ItemStack s : plan.requirements) {
                        if (s == null || s.item == null || s.amount <= 0) continue;
                        items.remove(s.item, s.amount);
                    }
                    if (plan.inLiquid != null) for (LiquidStack s : plan.inLiquid) {
                        if (s == null || s.liquid == null || s.amount <= 0) continue;
                        liquids.remove(s.liquid, s.amount);
                    }
                    // —— ★ 扣 coins（消耗配方）——
                    if (plan.coinCost > 0) {
                        coins.spend(plan.coinCost);
                    }
                    // —— 出物品产物 ——
                    if (plan.outItem != null) for (ItemStack s : plan.outItem) {
                        if (s == null || s.item == null || s.amount <= 0) continue;
                        items.add(s.item, s.amount);
                        for (int i = 0; i < s.amount; i++) dump(s.item);
                    }
                    // —— 出液体产物 ——
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

                    progress -= plan.time;
                } else {
                    progress = Math.min(progress, plan.time - 0.001f);
                }
            }
            if (progress < 0) progress = 0;

            // 兜底 dump
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
        @Override
        public byte version() { return 2; } // ★ version 1→2（加 warmup/totalProgress 字段），读档时 1 版会安全走 else 默认值

        @Override
        public void write(Writes write) {
            super.write(write);
            write.f(progress);
            write.s(currentPlan);
            write.f(warmup);
            write.f(totalProgress);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            progress = read.f();
            currentPlan = read.s();
            if (revision >= 2) { // 新存档有 warmup / totalProgress
                warmup = read.f();
                totalProgress = read.f();
            } else {            // 旧存档（revision=1）默认 0，安全
                warmup = 0f;
                totalProgress = 0f;
            }
        }

        // warmup / totalProgress 接口覆写
        @Override
        public float warmup()            { return warmup; }
        @Override
        public float totalProgress()     { return totalProgress; }
    }
}
