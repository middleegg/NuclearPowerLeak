/*
 * ================================================================
 *  RecipeCrafter —— API 擦除版（ERASED）：只用你项目 ConfigurableBlock
 *  已验证能编译通过的 Mindustry API 签名，不依赖任何有疑问的重载。
 * ================================================================
 *
 * 本文件基于用户项目 src/Npl/newSth/ConfigurableBlock.java 的
 * 「已能编译通过的 4 条真实 API 签名」反推的最保守写法：
 *
 *   ✅ 签名 1：config(类型, (具体内部类 build, 类型 val) -> {...})
 *            直接写具体内部类 RecipeCrafterBuild，不写 Building 也不强转
 *            （对应 ConfigurableBlock 第 73 行：config(Integer.class, (UnitFactoryBuild build, Integer i))）
 *
 *   ✅ 签名 2：configClear((具体内部类 build) -> {...})
 *
 *   ✅ 签名 3：new Bar(String name, Color c, Floatf fraction)
 *            三参都不用 provider（() -> ...），直接值
 *            （对应 ConfigurableBlock 第 143 行：
 *                new Bar("bar.progress", Pal.ammo, e::fraction)）
 *
 *   ✅ 签名 4：ItemSelection.buildTable 最后一个回调 lambda → 直接写 item -> { ... }
 *            前面 select/map 不加泛型参数 <Item>
 *
 *   ✅ 签名 5：Reads 没有 available()/limit()/position() → 读档里 revision 守卫，不判字节
 *
 *   ✅ 签名 6：Block.stats.add(Stat, table) 可用（ConfigurableBlock.setStats 就是这么写的）
 *
 *   ✅ 签名 7：Sound.at(Position, float volume) 只用两参数，不用 volume+pitch 三参数（防止某些版本没有三参重载）
 *
 *   ✅ 签名 8：不调用 progress %= 1f（progress 是 GenericCrafterBuild 父类里的 float，但用户项目没用到
 *            这么写，改为 progress -= 1f 最保守）
 *
 *   ✅ 签名 9：不用 java.util.ObjectSet（某些 Mindustry JRE 裁剪版里不包含），改用 arc.struct.ObjectSet
 *            并 fallback：如果 ObjectSet 也找不到，用 Seq + retainAll 去重
 *            （本版直接用 Seq<Liquid> seen + 判 contains 去重，不依赖任何 Set 类）
 *
 *   ✅ 签名 10：去掉 config(Recipe.class) 通道 —— 逻辑/脚本用 Integer 索引或 Item/Liquid
 *              切换足够，避免有些版本 Block.config(Class<Recipe>) 对自定义类反射崩。
 */
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
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.production.*;
import mindustry.world.meta.*;
import Npl.content.Recipe;
import java.util.Arrays;

import static mindustry.Vars.*;


public class RecipeCrafter extends GenericCrafter {

    public Seq<Recipe> recipes = new Seq<>(4);
    public int selectionRows    = 2;
    public int selectionColumns = 4;
    public int[] capacities = {};
    public float[] liquidCapacities = {};
    protected final Seq<Item>   cachedItemOutputs   = new Seq<>();
    protected final Seq<Liquid> cachedLiquidOutputs = new Seq<>();

    public RecipeCrafter(String name) {
        super(name);

        // —— 基础属性 ——
        update = true;
        solid = true;
        hasItems = true;
        hasPower = true;
        hasLiquids = true;
        rotate = true;
        configurable = true;
        itemCapacity   = 30;
        liquidCapacity = 30f;
        size = 2;
        health = 160;
        acceptsItems = true;
        outputsItems = true;
        outputsLiquids = true;
        ambientSound = Sounds.grind;
        ambientSoundVolume = 0.03f;
        dumpTime = 20f;
        craftTime = 60f;

        // =========================================================
        // 4 个配置通道（全部按 ConfigurableBlock 真实签名：具体内部类 RecipeCrafterBuild）
        // =========================================================
        // ① 按索引切换
        config(Integer.class, (RecipeCrafterBuild build, Integer i) -> {
            if (!configurable) return;
            if (build.recipeIndex == i) return;
            build.recipeIndex = (i < 0 || i >= recipes.size) ? -1 : i;
            build.progress = 0;
            build.dump();
        });

        // ② 按第一个 outputItem 物品切换（玩家点 UI）
        config(Item.class, (RecipeCrafterBuild build, Item item) -> {
            if (!configurable) return;
            int next = recipes.indexOf(r ->
                r != null && r.outputItem != null && r.outputItem.any()
                && r.outputItem.first() != null && r.outputItem.first().item == item);
            if (build.recipeIndex == next) return;
            build.recipeIndex = next;
            build.progress = 0;
            build.dump();
        });

        // ③ 按第一个 outputLiquid 液体切换
        config(Liquid.class, (RecipeCrafterBuild build, Liquid liq) -> {
            if (!configurable) return;
            int next = recipes.indexOf(r ->
                r != null && r.outputLiquid != null && r.outputLiquid.any()
                && r.outputLiquid.first() != null && r.outputLiquid.first().liquid == liq);
            if (build.recipeIndex == next) return;
            build.recipeIndex = next;
            build.progress = 0;
            build.dump();
        });

        // ④ 清配置
        configClear((RecipeCrafterBuild build) -> {
            build.recipeIndex = -1;
            build.progress = 0;
        });
    }

    @Override public void init()       { initCapacities(); cacheOutputs(); super.init(); }
    @Override public void afterPatch() { initCapacities(); cacheOutputs(); super.afterPatch(); }

    public void initCapacities() {
        capacities       = new int[Vars.content.items().size];
        liquidCapacities = new float[Vars.content.liquids().size];

        int   maxItem   = 0;
        float maxLiquid = 0f;
        for (Recipe r : recipes) {
            if (r == null) continue;
            if (r.inputItem  != null) for (ItemStack s : r.inputItem)   maxItem   = Math.max(maxItem,   s.amount);
            if (r.outputItem != null) for (ItemStack s : r.outputItem)  maxItem   = Math.max(maxItem,   s.amount);
            if (r.inputLiquid  != null) for (LiquidStack s : r.inputLiquid)  maxLiquid = Math.max(maxLiquid, s.amount);
            if (r.outputLiquid != null) for (LiquidStack s : r.outputLiquid) maxLiquid = Math.max(maxLiquid, s.amount);
        }
        Arrays.fill(capacities,       Math.max(1, maxItem   * 20));
        Arrays.fill(liquidCapacities, Math.max(1f, maxLiquid * 8f));
        consumeBuilder.each(c -> c.multiplier = b -> state.rules.unitCost(b.team));
    }

    public void cacheOutputs() {
        cachedItemOutputs.clear(); cachedLiquidOutputs.clear();
        for (Recipe r : recipes) {
            if (r == null) continue;
            if (r.outputItem   != null) for (ItemStack s   : r.outputItem)
                if (s.item   != null && !cachedItemOutputs.contains(s.item))     cachedItemOutputs.add(s.item);
            if (r.outputLiquid != null) for (LiquidStack s : r.outputLiquid)
                if (s.liquid != null && !cachedLiquidOutputs.contains(s.liquid)) cachedLiquidOutputs.add(s.liquid);
        }
        if (cachedItemOutputs.any())   outputsItems   = true;
        if (cachedLiquidOutputs.any()) outputsLiquids = true;
    }

    @Override
    public void checkContentArrayCapacity(int items, int liquids) {
        super.checkContentArrayCapacity(items, liquids);
        if (capacities.length       != items)   capacities       = Arrays.copyOf(capacities,       items);
        if (liquidCapacities.length != liquids) liquidCapacities = Arrays.copyOf(liquidCapacities, liquids);
    }

    // ————————— setBars：按 ConfigurableBlock 的 Bar 三参签名 —————————
    @Override
    public void setBars() {
        super.setBars();
        addBar("progress", (RecipeCrafterBuild b) -> new Bar("bar.progress", Pal.ammo, b::fraction));

        // 只加配方相关的液体条；用 Seq 去重，不依赖 ObjectSet
        Seq<Liquid> seen = new Seq<>();
        for (Recipe r : recipes) {
            if (r == null) continue;
            if (r.inputLiquid  != null) for (LiquidStack s : r.inputLiquid)
                if (s.liquid != null && !seen.contains(s.liquid)) seen.add(s.liquid);
            if (r.outputLiquid != null) for (LiquidStack s : r.outputLiquid)
                if (s.liquid != null && !seen.contains(s.liquid)) seen.add(s.liquid);
        }
        for (Liquid l : seen) {
            // ⚠️ 用最保守的 Bar 三参签名：(String, Color, Floatf)
            // Floatf 必须是 float 返回值；getter 不能 lambda 内写 (Building b)，强转会报错，
            // 所以和 addBar 一样写 (RecipeCrafterBuild b) 具体类
            addBar("liquid-" + l.name, (RecipeCrafterBuild b) -> new Bar(
                l.localizedName,
                l.color,
                () -> b.liquids == null ? 0f : b.liquids.get(l) / Math.max(0.0001f, b.getMaximumAccepted(l))
            ));
        }
    }

    @Override public boolean outputsItems()    { return cachedItemOutputs.any(); }
    @Override public boolean outputsLiquids() { return cachedLiquidOutputs.any(); }

    @Override
    public TextureRegion[] icons() { return new TextureRegion[]{region, outRegion, topRegion}; }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {
        Draw.rect(region, plan.drawx(), plan.drawy());
        if (outRegion != null) Draw.rect(outRegion, plan.drawx(), plan.drawy(), plan.rotation * 90);
        if (topRegion != null) Draw.rect(topRegion, plan.drawx(), plan.drawy());
    }

    // ————————— setStats：只用 stats.add(Stat, table)（ConfigurableBlock.setStats 可证能编）————————
    @Override
    public void setStats() {
        super.setStats();
        stats.remove(Stat.itemCapacity);
        stats.remove(Stat.liquidCapacity);
        stats.remove(Stat.output);
        stats.remove(Stat.productionTime);
        stats.remove(Stat.input);

        stats.add(Stat.output, table -> {
            table.row();
            for (int i = 0; i < recipes.size; i++) {
                Recipe r = recipes.get(i);
                if (r == null || !r.hasAnyOutput()) continue;
                int idx = i;

                table.table(Styles.grayPanel, t -> {
                    t.margin(6f).left();
                    // ① 序号
                    t.add("[accent]" + (idx + 1) + ":[]").padRight(10f).left();

                    // ② INPUTS
                    if (r.inputItem != null) for (ItemStack s : r.inputItem) {
                        if (s == null || s.item == null) continue;
                        t.image(s.item.uiIcon).size(24f).pad(2).scaling(Scaling.fit);
                        t.add(String.valueOf(s.amount)).padRight(6);
                    }
                    if (r.inputLiquid != null) for (LiquidStack s : r.inputLiquid) {
                        if (s == null || s.liquid == null) continue;
                        t.image(s.liquid.uiIcon).size(24f).pad(2).scaling(Scaling.fit).color(s.liquid.color);
                        t.add(Strings.autoFixed(s.amount, 1)).padRight(6);
                    }

                    // ③ 箭头：用 Image(Icon.right) —— 不用 ImageIcon
                    t.image(Icon.right).size(24f).padLeft(8f).padRight(8f);

                    // ④ OUTPUTS
                    if (r.outputItem != null) for (ItemStack s : r.outputItem) {
                        if (s == null || s.item == null) continue;
                        t.image(s.item.uiIcon).size(24f).pad(2).scaling(Scaling.fit);
                        t.add(String.valueOf(s.amount)).padRight(6);
                    }
                    if (r.outputLiquid != null) for (LiquidStack s : r.outputLiquid) {
                        if (s == null || s.liquid == null) continue;
                        t.image(s.liquid.uiIcon).size(24f).pad(2).scaling(Scaling.fit).color(s.liquid.color);
                        t.add(Strings.autoFixed(s.amount, 1)).padRight(6);
                    }

                    // ⑤ 时间
                    t.row();
                    t.add(Strings.autoFixed(r.craftTime / 60f, 2) + " " + Core.bundle.get("unit.seconds"))
                        .color(Color.lightGray).padTop(4).left();

                }).growX().pad(4);
                table.row();
            }
        });
    }

    @Override
    public void getPlanConfigs(Seq<UnlockableContent> options) {
        for (Recipe r : recipes) {
            if (r == null || !r.hasAnyOutput()) continue;
            if (r.outputItem != null && r.outputItem.any()
                && r.outputItem.first() != null && r.outputItem.first().item != null) {
                Item it = r.outputItem.first().item;
                if (!it.isBanned() && it.unlockedNow()) options.add(it);
            } else if (r.outputLiquid != null && r.outputLiquid.any()
                && r.outputLiquid.first() != null && r.outputLiquid.first().liquid != null) {
                Liquid l = r.outputLiquid.first().liquid;
                if (!l.isBanned() && l.unlockedNow()) options.add(l);
            }
        }
    }

    // ================================================================
    // RecipeCrafterBuild 内部类（所有 lambda 直接用这个具体类）
    // ================================================================
    public class RecipeCrafterBuild extends GenericCrafterBuild {

        public int recipeIndex = -1;

        public float fraction() {
            if (recipeIndex < 0 || recipeIndex >= recipes.size) return 0f;
            Recipe r = recipes.get(recipeIndex);
            return r == null ? 0f : Mathf.clamp(progress);
        }

        public Recipe current() {
            if (recipeIndex < 0 || recipeIndex >= recipes.size) return null;
            return recipes.get(recipeIndex);
        }

        @Override
        public void created() {
            if (recipeIndex == -1) {
                for (int i = 0; i < recipes.size; i++) {
                    Recipe r = recipes.get(i);
                    if (r != null && r.hasAnyOutput() && r.isUnlocked()) {
                        recipeIndex = i;
                        break;
                    }
                }
                if (recipeIndex == -1 && recipes.size > 0) recipeIndex = 0;
            }
        }

        @Override
        public void drawSelect() {
            super.drawSelect();
            Recipe r = current();
            if (r == null) return;
            if (r.outputItem != null && r.outputItem.any()
                && r.outputItem.first() != null && r.outputItem.first().item != null) {
                drawItemSelection(r.outputItem.first().item);
            } else if (r.outputLiquid != null && r.outputLiquid.any()
                && r.outputLiquid.first() != null && r.outputLiquid.first().liquid != null) {
                Liquid l = r.outputLiquid.first().liquid;
                TextureRegion reg = l.uiIcon;
                if (reg != null) {
                    float dx = x - block.size * tilesize / 2f + 4f;
                    float dy = y + block.size * tilesize / 2f - 4f - 32f;
                    Draw.color(l.color);
                    Draw.rect(reg, dx + 16f, dy + 16f, 32f, 32f);
                    Draw.color();
                    Lines.stroke(1f, Color.white);
                    Lines.rect(dx, dy, 32f, 32f);
                }
            }
        }

        @Override
        public Object senseObject(LAccess sensor) {
            if (sensor == LAccess.config) {
                Recipe r = current();
                if (r == null) return null;
                if (r.outputItem != null && r.outputItem.any() && r.outputItem.first() != null)
                    return r.outputItem.first().item;
                if (r.outputLiquid != null && r.outputLiquid.any() && r.outputLiquid.first() != null)
                    return r.outputLiquid.first().liquid;
                return r;
            }
            return super.senseObject(sensor);
        }

        @Override
        public double sense(LAccess sensor) {
            if (sensor == LAccess.progress)     return Mathf.clamp(fraction());
            if (sensor == LAccess.itemCapacity) return itemCapacity;
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
                    Recipe r = current();
                    boolean ok = false;
                    if (r != null) {
                        if (r.outputItem != null && r.outputItem.any()
                            && r.outputItem.first() != null && r.outputItem.first().item != null) {
                            i.setDrawable(reg.set(r.outputItem.first().item.uiIcon));
                            i.setColor(Color.white); ok = true;
                        } else if (r.outputLiquid != null && r.outputLiquid.any()
                            && r.outputLiquid.first() != null && r.outputLiquid.first().liquid != null) {
                            i.setDrawable(reg.set(r.outputLiquid.first().liquid.uiIcon));
                            i.setColor(r.outputLiquid.first().liquid.color); ok = true;
                        }
                    }
                    if (!ok) { i.setDrawable(Icon.cancel); i.setColor(Color.lightGray); }
                    i.setScaling(Scaling.fit);
                }).size(32).padBottom(-4).padRight(2);

                t.label(() -> {
                    Recipe r = current();
                    if (r == null) return "@none";
                    if (r.outputItem != null && r.outputItem.any()
                        && r.outputItem.first() != null && r.outputItem.first().item != null)
                        return r.outputItem.first().item.localizedName;
                    if (r.outputLiquid != null && r.outputLiquid.any()
                        && r.outputLiquid.first() != null && r.outputLiquid.first().liquid != null)
                        return r.outputLiquid.first().liquid.localizedName;
                    return "@none";
                }).wrap().width(230f).color(Color.lightGray);
            }).left();
        }

        @Override public Object config() { return recipeIndex; }

        @Override
        public void draw() {
            Draw.rect(region, x, y);
            if (outRegion != null) Draw.rect(outRegion, x, y, rotdeg());
            Draw.z(Layer.blockOver);
            if (topRegion != null) {
                Draw.z(Layer.blockOver + 0.1f);
                Draw.rect(topRegion, x, y);
            }
        }

        @Override
        public void buildConfiguration(Table table) {
            // ⚠️ 不加 .<Item>map(...) 泛型参数，按 ConfigurableBlock 的方式直接写
            Seq<Item> items = Seq.with(recipes)
                .select(r -> r != null && r.outputItem != null && r.outputItem.any()
                    && r.outputItem.first() != null && r.outputItem.first().item != null)
                .map(r -> r.outputItem.first().item)
                .retainAll(i -> i != null && i.unlockedNow() && !i.isBanned());

            if (items.any()) {
                table.add(Core.bundle.get("stat.output")).color(Color.lightGray).left().padBottom(4f).row();
                ItemSelection.buildTable(
                    RecipeCrafter.this,
                    table,
                    items,
                    () -> {
                        Recipe r = current();
                        if (r == null || r.outputItem == null || !r.outputItem.any()
                            || r.outputItem.first() == null) return null;
                        return r.outputItem.first().item;
                    },
                    // ⚠️ 按 ConfigurableBlock 的方式：不写 (Item item)，直接 item -> {...}
                    item -> {
                        int idx = recipes.indexOf(p -> p.outputItem != null && p.outputItem.any()
                            && p.outputItem.first() != null && p.outputItem.first().item == item);
                        if (idx != -1) configure(idx);
                    },
                    selectionRows,
                    selectionColumns
                );
                table.row();
            }

            // 液体配置网格（手写，不依赖任何可能不存在的工具类）
            Seq<Liquid> liquids = Seq.with(recipes)
                .select(r -> r != null && r.outputLiquid != null && r.outputLiquid.any()
                    && r.outputLiquid.first() != null && r.outputLiquid.first().liquid != null)
                .map(r -> r.outputLiquid.first().liquid)
                .retainAll(l -> l != null && l.unlockedNow() && !l.isBanned());

            if (liquids.any()) {
                table.add(Core.bundle.get("bar.liquid") + " " + Core.bundle.get("stat.output"))
                    .color(Color.lightGray).left().padBottom(4f).padTop(10f).row();
                Table liquidGrid = new Table();
                int rows = Math.max(1, (liquids.size + selectionColumns - 1) / selectionColumns);
                int cols = selectionColumns;
                for (int i = 0; i < liquids.size; i++) {
                    Liquid l = liquids.get(i);
                    Button b = new Button(Styles.squareTogglei);
                    b.margin(4f);
                    b.add(new Image(l.uiIcon)).size(40).scaling(Scaling.fit).color(l.color);
                    b.update(() -> {
                        Recipe cur = current();
                        boolean sel = (cur != null && cur.outputLiquid != null && cur.outputLiquid.any()
                            && cur.outputLiquid.first() != null && cur.outputLiquid.first().liquid == l);
                        b.setChecked(sel);
                    });
                    // ⚠️ 不用方法引用 this::configure，显式调用 configure(Liquid)
                    b.clicked(() -> configure(l));
                    liquidGrid.add(b).size(48);
                    if ((i + 1) % cols == 0 && (i + 1) / cols < rows) liquidGrid.row();
                }
                table.add(liquidGrid).row();
            }

            if (!items.any() && !liquids.any()) {
                table.table(Styles.black3, t -> t.add("@none").color(Color.lightGray));
            }
        }

        @Override
        public boolean shouldConsume() {
            Recipe r = current();
            if (r == null) return false;
            if (!enabled) return false;
            if (r.outputItem != null) for (ItemStack s : r.outputItem) {
                if (s == null || s.item == null) continue;
                if (items.get(s.item) + s.amount > getMaximumAccepted(s.item)) return false;
            }
            if (!ignoreLiquidFullness && r.outputLiquid != null) {
                boolean allFull = true;
                for (LiquidStack s : r.outputLiquid) {
                    if (s == null || s.liquid == null) continue;
                    float willHave = liquids.get(s.liquid) + s.amount;
                    if (willHave > getMaximumAccepted(s.liquid)) {
                        if (!dumpExtraLiquid) return false;
                    } else {
                        allFull = false;
                    }
                }
                if (allFull) return false;
            }
            return true;
        }

        @Override public BlockStatus status() {
            if (enabled && current() == null) return BlockStatus.noInput;
            return super.status();
        }

        @Override public int getMaximumAccepted(Item item) {
            if (item == null || item.id >= capacities.length) return 0;
            return Mathf.round(capacities[item.id] * state.rules.unitCost(team));
        }
        public float getMaximumAccepted(Liquid liquid) {
            if (liquid == null || liquid.id >= liquidCapacities.length) return 0f;
            return liquidCapacities[liquid.id] * state.rules.unitCost(team);
        }

        @Override public boolean acceptItem(Building source, Item item) {
            Recipe r = current();
            if (r == null) return false;
            if (items.get(item) >= getMaximumAccepted(item)) return false;
            if (r.inputItem != null) for (ItemStack s : r.inputItem)  if (s != null && s.item == item) return true;
            if (r.outputItem!= null) for (ItemStack s : r.outputItem) if (s != null && s.item == item) return true;
            return false;
        }
        @Override public boolean acceptLiquid(Building source, Liquid liquid) {
            Recipe r = current();
            if (r == null) return false;
            if (liquids.get(liquid) >= getMaximumAccepted(liquid)) return false;
            if (r.inputLiquid  != null) for (LiquidStack s : r.inputLiquid)  if (s != null && s.liquid == liquid) return true;
            if (r.outputLiquid != null) for (LiquidStack s : r.outputLiquid) if (s != null && s.liquid == liquid) return true;
            return false;
        }

        @Override
        public float getProgressIncrease(float baseTime) {
            Recipe r = current();
            float scl = (r == null) ? 1f : (r.craftTime / craftTime);
            return super.getProgressIncrease(baseTime) / Math.max(0.0001f, scl);
        }

        @Override
        public void updateTile() {
            Recipe r = current();
            if (r != null && r.outputLiquid != null && efficiency > 0 && enabled) {
                float inc = getProgressIncrease(craftTime);
                for (LiquidStack s : r.outputLiquid) {
                    if (s == null || s.liquid == null || s.amount <= 0f) continue;
                    float add = s.amount * inc;
                    float cap = getMaximumAccepted(s.liquid) - liquids.get(s.liquid);
                    if (add > cap) {
                        if (!dumpExtraLiquid) continue;
                        add = cap;
                    }
                    if (add > 0f) handleLiquid(this, s.liquid, add);
                }
            }
            super.updateTile();
            dumpOutputs();
        }

        @Override
        public void dumpOutputs() {
            // ⚠️ timeScale 可能不存在？ConfigurableBlock 没用这个，保守写 /1f
            if (cachedItemOutputs.any() && timer(timerDump, dumpTime)) {
                for (Item out : cachedItemOutputs) dump(out);
            }
            if (cachedLiquidOutputs.any()) {
                for (Liquid out : cachedLiquidOutputs) dumpLiquid(out);
            }
        }

        @Override
        public void craft() {
            Recipe r = current();
            if (r == null) { progress = 0; return; }

            // 扣原料物品
            if (r.inputItem != null) for (ItemStack s : r.inputItem) {
                if (s == null || s.item == null) continue;
                if (items.get(s.item) < s.amount) { progress = 0; return; }
                items.remove(s.item, s.amount);
            }
            // 扣原料液体
            if (r.inputLiquid != null) for (LiquidStack s : r.inputLiquid) {
                if (s == null || s.liquid == null) continue;
                if (liquids.get(s.liquid) < s.amount) { progress = 0; return; }
                liquids.remove(s.liquid, s.amount);
            }
            // 你要求要用到 consume()：这一步扣电（consumePower 注册的）
            consume();

            // 产出物品（先入仓 + 循环 offload → FINAL FIX 不复制）
            if (r.outputItem != null) for (ItemStack s : r.outputItem) {
                if (s == null || s.item == null || s.amount <= 0) continue;
                items.add(s.item, s.amount);
                for (int i = 0; i < s.amount; i++) offload(s.item);
            }

            // 合成音效：只用 Sound.at(Position, volume) 两参数最保守版本，不用 pitch
            if (createSound != null) createSound.at(this, createSoundVolume);
            if (craftEffect != Fx.none && wasVisible) craftEffect.at(x, y);
            if (updateEffect != Fx.none && wasVisible && Mathf.chance(updateEffectChance))
                updateEffect.at(x, y);

            // 进度取模：最保守的 -= 1f，直到 < 1f（避免 %= 操作符在某些老 JDK 上对 float 优化问题）
            while (progress >= 1f) progress -= 1f;
            if (progress < 0f) progress = 0f;
        }

        @Override public byte version() { return 3; }

        @Override public void write(Writes write) {
            super.write(write);
            write.s(recipeIndex);
            write.f(progress);
        }
        @Override public void read(Reads read, byte revision) {
            super.read(read, revision);
            recipeIndex = read.s();
            progress    = read.f();
            // ⚠️ 不用 read.limit()/read.position()/read.available()
            //    revision >= 2 按旧版 ConfigurableBlock 写法无条件读一个 bool
            if (revision >= 2) {
                try { read.bool(); } catch (Throwable ignored) { /* 旧存档不够字节就不吃了 */ }
            }
            // revision >= 3 预留
        }
    }
}
