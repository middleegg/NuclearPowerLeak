package Npl.newSth;

// ==================== 导入区 ====================
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
import mindustry.content.*;
import mindustry.world.blocks.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.world.Block;
import java.util.*;

import Npl.newSth.Type.coins;
import Npl.newSth.consumes.ConsumeCoins;

import static mindustry.Vars.*;

/**
 * ============================================================
 * 【CoinConsumerBlock = coins 多配方工厂】
 *  基于 HunfuBlock 架构（多配方：物品入/出 + 液体入/出），
 *  在 Plan 中额外新增：
 *    1. coinCost（int）：每轮合成消耗的 coins 数量
 *  其他完全照搬 HunfuBlock 的：
 *    - Plan 数组 plans（支持多配方、UI 切换、整数/物品 config）
 *    - capacities[] / liquidCapacities[] 自动计算
 *    - shouldConsume 物品+液体双检查
 *    - updateTile() 扣原料+出产物+兜底 dump
 *
 *  coins 消耗：
 *    - 由 shouldConsume() 额外检查 coins.getAmount() >= plan.coinCost
 *    - 进度满、扣完其他原料后，额外扣 coins
 * ============================================================
 */
public class CoinConsumerBlock extends Block {

    // ========== 字段区（HunfuBlock 同款）==========
    public int[] capacities = {};
    public float[] liquidCapacities = {};

    /** 【核心】多配方列表，每个 Plan = 一种配方（物品+液体+coins 消耗） */
    public Seq<Plan> plans = new Seq<>(4);

    public int selectionRows = 2;
    public int selectionColumns = 4;

    // ========== 兼容老单配方字段（会被 init() 转成 plans.get(0)）==========
    /** @deprecated 请用 plans 多配方；仍设此值的话 init() 会自动转成 Plan(0) */
    @Deprecated
    public float craftTime = 120f;
    /** @deprecated */
    @Deprecated
    public int coinPerCraft = 0;
    /** @deprecated */
    @Deprecated
    public ItemStack outputItem = null;

    public Effect craftEffect = Fx.formsmoke;
    public Effect updateEffect = Fx.none;
    public float updateEffectChance = 0.04f;

    // ========== 构造器（照搬 HunfuBlock，含三种 config 通道）==========
    public CoinConsumerBlock(String name) {
        super(name);
        update = true;
        solid = true;
        hasItems = true;
        hasPower = true;
        hasLiquids = true;
        consumesPower = true;
        size = 2;
        health = 100;
        rotate = false;
        configurable = true;
        itemCapacity = 30;
        liquidCapacity = 30f;

        // ① 整数索引：逻辑处理器切配方
        config(Integer.class, (CoinConsumerBuild build, Integer i) -> {
            if (!configurable) return;
            if (build.currentPlan == i) return;
            build.currentPlan = i < 0 || i >= plans.size ? -1 : i;
            build.progress = 0;
            build.dump();
            for (Liquid l : Vars.content.liquids()) build.dumpLiquid(l);
        });
        // ② 物品对象：UI 配方点选
        config(Item.class, (CoinConsumerBuild build, Item item) -> {
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
        configClear((CoinConsumerBuild build) -> {
            build.currentPlan = -1;
            build.progress = 0;
        });
    }

    // =================================================================
    // init：把老单配方字段 (craftTime/coinPerCraft/outputItem) 自动转成 plans[0]
    // =================================================================
    @Override
    public void init() {
        // —— 兼容老字段：如果 plans 为空但 outputItem/craftTime 有值，自动合成一个 Plan(0) ——
        if (plans.isEmpty() && outputItem != null) {
            plans.add(new Plan(
                    new ItemStack[]{outputItem},
                    craftTime,
                    null, null, null,
                    coinPerCraft
            ));
        }
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
    // UI / stats（照搬 HunfuBlock，额外显示 coinCost）
    // =================================================================
    @Override
    public void setBars() {
        super.setBars();
        addBar("progress", (CoinConsumerBuild e) -> new Bar("bar.progress", Pal.ammo, e::fraction));
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
                if ((firstOutItem == null || firstOutItem.item == null) &&
                    (firstOutLiq == null || firstOutLiq.liquid == null)) continue;

                table.table(Styles.grayPanel, t -> {
                    // ── 左：配方代表图标 ──
                    t.table(icons -> {
                        icons.left();
                        if (firstOutItem != null && firstOutItem.item != null) {
                            icons.image(firstOutItem.item.uiIcon).size(40).pad(10f).scaling(Scaling.fit)
                                    .with(i -> StatValues.withTooltip(i, firstOutItem.item));
                        } else if (firstOutLiq != null && firstOutLiq.liquid != null) {
                            icons.image(firstOutLiq.liquid.uiIcon).size(40).pad(10f).scaling(Scaling.fit)
                                    .with(i -> StatValues.withTooltip(i, firstOutLiq.liquid));
                        }
                        int outCount = 0;
                        if (plan.outItem   != null) outCount += plan.outItem.length;
                        if (plan.outLiquid != null) outCount += plan.outLiquid.length;
                        if (outCount > 1) icons.add(" +" + (outCount - 1)).color(Color.lightGray).padLeft(2f);
                        // ★ coins 成本：以标签形式贴在图标右下角
                        if (plan.coinCost > 0) {
                            icons.row();
                            icons.add(plan.coinCost + " coins/craft").color(Color.valueOf("E2FF6D")).padTop(2f).left();
                        }
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
        }
    }

    // =================================================================
    // 【Plan = 配方】HunfuBlock 全部字段 + coinCost
    // =================================================================
    public static class Plan {
        public ItemStack[]   outItem;
        public ItemStack[]   requirements;
        public LiquidStack[] outLiquid;
        public LiquidStack[] inLiquid;
        public float         time;
        /** ★ 新增：每轮合成消耗的 coins 数量 */
        public int           coinCost;

        /** 全参：物品出/入 + 液体出/入 + 耗时 + coins */
        public Plan(ItemStack[] outItem, float time, ItemStack[] requirements,
                    LiquidStack[] outLiquid, LiquidStack[] inLiquid, int coinCost) {
            this.outItem      = (outItem      != null) ? outItem      : new ItemStack[0];
            this.requirements = (requirements != null) ? requirements : new ItemStack[0];
            this.outLiquid    = (outLiquid    != null) ? outLiquid    : new LiquidStack[0];
            this.inLiquid     = (inLiquid     != null) ? inLiquid     : new LiquidStack[0];
            this.time         = time;
            this.coinCost     = coinCost;
        }
        /** 兼容 HunfuBlock 签名：物品出/入+液体出/入+耗时（coinCost=0）*/
        public Plan(ItemStack[] outItem, float time, ItemStack[] requirements,
                    LiquidStack[] outLiquid, LiquidStack[] inLiquid) {
            this(outItem, time, requirements, outLiquid, inLiquid, 0);
        }
        /** 兼容老签名：只有物品 */
        public Plan(ItemStack[] outItem, float time, ItemStack[] requirements) {
            this(outItem, time, requirements, null, null, 0);
        }
        /** ★ 便捷构造：只有物品 + coinCost（coins 工厂最常用） */
        public Plan(ItemStack[] outItem, float time, ItemStack[] requirements, int coinCost) {
            this(outItem, time, requirements, null, null, coinCost);
        }
        Plan(){
            this.outItem      = new ItemStack[0];
            this.requirements = new ItemStack[0];
            this.outLiquid    = new LiquidStack[0];
            this.inLiquid     = new LiquidStack[0];
            this.time         = 0f;
            this.coinCost     = 0;
        }
    }

    // =================================================================
    // 【CoinConsumerBuild】HunfuBuild 同款 + coins 消耗
    // =================================================================
    public class CoinConsumerBuild extends Building {

        public int   currentPlan = -1;
        public float progress    = 0f;

        // ── 兼容父类 warmup / totalProgress 接口（被 Building.warmup() 等覆写）──
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

        // ========== 配置面板 UI（照搬 HunfuBuild）==========
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

        // ========== shouldConsume：物品+液体+coins 三检查 ==========
        @Override
        public boolean shouldConsume() {
            if (currentPlan == -1 || currentPlan >= plans.size) return false;
            if (!enabled) return false;
            Plan plan = plans.get(currentPlan);
            if (plan == null) return false;
            boolean hasAnyOut = (plan.outItem != null && plan.outItem.length > 0)
                             || (plan.outLiquid != null && plan.outLiquid.length > 0);
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
            // ★ coins 够不够
            if (plan.coinCost > 0 && coins.getAmount() < plan.coinCost) return false;
            return true;
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
        // ✅ 核心 updateTile：HunfuBuild 同款 + 额外扣 coins
        // =================================================================
        @Override
        public void updateTile() {
            if (!configurable) currentPlan = 0;
            if (currentPlan < 0 || currentPlan >= plans.size) { currentPlan = -1; return; }
            Plan plan = plans.get(currentPlan);
            boolean hasAnyOut = (plan != null) && (
                    (plan.outItem != null && plan.outItem.length > 0) ||
                    (plan.outLiquid != null && plan.outLiquid.length > 0));
            if (plan == null || !hasAnyOut) { currentPlan = -1; return; }

            // Step 1：进度累积（只有 shouldConsume 通过才累积进度）
            if (efficiency > 0 && shouldConsume()) {
                progress += edelta();
                warmup = Mathf.approachDelta(warmup, 1f, 0.1f);
                totalProgress += warmup * Time.delta;

                if (wasVisible && Mathf.chanceDelta(updateEffectChance)) {
                    updateEffect.at(x + Mathf.range(size * 2f), y + Mathf.range(size * 2f));
                }
            } else {
                warmup = Mathf.approachDelta(warmup, 0f, 0.1f);
            }

            // Step 2：进度满 → 扣原料 + 扣 coins + 出产物
            if (progress >= plan.time) {
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
                boolean allInOk = true;
                if (plan.requirements != null) for (ItemStack s : plan.requirements) {
                    if (s == null || s.item == null) continue;
                    if (items.get(s.item) < s.amount) { allInOk = false; break; }
                }
                if (allInOk && plan.inLiquid != null) for (LiquidStack s : plan.inLiquid) {
                    if (s == null || s.liquid == null) continue;
                    if (liquids.get(s.liquid) < s.amount) { allInOk = false; break; }
                }
                // ★ coins 最终检查（防 race）
                boolean coinsOk = (plan.coinCost <= 0) || (coins.getAmount() >= plan.coinCost);

                if (allOutOk && allInOk && coinsOk) {
                    // 扣物品 + 液体
                    if (plan.requirements != null) for (ItemStack s : plan.requirements) {
                        if (s == null || s.item == null || s.amount <= 0) continue;
                        items.remove(s.item, s.amount);
                    }
                    if (plan.inLiquid != null) for (LiquidStack s : plan.inLiquid) {
                        if (s == null || s.liquid == null || s.amount <= 0) continue;
                        liquids.remove(s.liquid, s.amount);
                    }
                    // ★ 扣 coins
                    if (plan.coinCost > 0) {
                        coins.spend(plan.coinCost);
                    }
                    // 出物品
                    if (plan.outItem != null) for (ItemStack s : plan.outItem) {
                        if (s == null || s.item == null || s.amount <= 0) continue;
                        items.add(s.item, s.amount);
                        for (int i = 0; i < s.amount; i++) dump(s.item);
                    }
                    // 出液体
                    if (plan.outLiquid != null) for (LiquidStack s : plan.outLiquid) {
                        if (s == null || s.liquid == null || s.amount <= 0f) continue;
                        liquids.add(s.liquid, s.amount);
                        int times = (int)Math.ceil(s.amount);
                        for (int i = 0; i < times; i++) dumpLiquid(s.liquid);
                    }

                    craftEffect.at(x, y);
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
        public byte version() { return 1; }

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
            warmup = read.f();
            totalProgress = read.f();
        }

        // HunfuBlock 父类 Building 里需要覆写的 warmup/totalProgress 接口
        @Override
        public float warmup()            { return warmup; }
        @Override
        public float totalProgress()     { return totalProgress; }
    }
}
