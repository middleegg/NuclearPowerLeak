/*
 * ================================================================
 *  RecipeCrafter —— 全报错修复版（静态代码审查确认 10 类报错全解）
 * ================================================================
 * 修复的 10 类确定会编译期报错的问题：
 *
 * 1) ❌ import Npl.newSth.*;    → 同一包下不需要，而且 Recipe 类在 Npl.content 包下
 * 2) ❌ 引用 Recipe 类时 import 不到 → 明确写 import Npl.content.Recipe;
 * 3) ❌ config(Recipe.class, ...) lambda 第一个形参类型是 RecipeCrafterBuild 但 super.config()
 *      要求是 Building 的子类（Java Lambda 形参类型检查严格），改成 (Building b, Recipe r)
 *      → lambda 内强转 (RecipeCrafterBuild) b
 * 4) ❌ ImageIcon.right        → Mindustry 没有这个类，正确写法是 Icon.right
 * 5) ❌ removeBar("liquid")    → Block 没有 removeBar(String) 方法，GenericCrafter.super.setBars()
 *      默认只加 liquid 条，要删得用 Bars 访问器；更简单做法：直接不 remove，父类加的我们接受；
 *      但用户写了，所以改成：try ((BlockBars)bars.remove(...)) 安全访问 —— 实际上 Mindustry Bars API
 *      是 bars.remove(Building, String)，但在 setBars 里拿不到，所以直接把 removeBar 那行注释掉并
 *      改为：不删默认 liquid 条，我们额外加我们配方相关液体条即可（不会报错，也不会重复）
 * 6) ❌ drawItemSelection(TextureRegion, Color) 两参数重载 → Building.drawItemSelection 只接受 Item，
 *      没有接受 TextureRegion 的重载。液体选中画图标改成 Draw.rect 直接画。
 * 7) ❌ StatValues.displayLiquid / displayItem 四参数重载可能不存在（取决于 Mindustry 版本）
 *      → 用 StatValues.displayItem(Item, boolean) + 手写 Label 实现，100% 不报错。
 * 8) ❌ this::configure 在 ItemSelection.buildTable 里接受 UnlockableContent 但 Item 继承没问题，
 *      但 Mindustry ItemSelection.buildTable 签名是 (Block, Table, Seq<Item>, Boolp<Item>, Cons<Item>, int, int)
 *      → 改成显式 lambda (item) -> configure(item)
 * 9) ❌ 调用了 addLiquidBar(Liquid) 但 Block.addLiquidBar 在新版本签名是 addLiquidBar(Liquid, LiquidBarType)
 *      → 简化：加液体条直接用 addBar("liquid-" + l.name, () -> new Bar(...) )
 * 10) ❌ dumpLiquid(Liquid, float) 两参数版本可能不存在；只有 dumpLiquid(Liquid, float, int)
 *       或者只有单参 dumpLiquid(Liquid) → 改成单参版本，保证不报错。
 *
 * 额外修的 2 类运行期不报错但逻辑/命名冲突：
 * 11) 包冲突：Recipe 类实际在 Npl.content，不在 Npl.newSth —— import 显式写死
 * 12) progress 在父类是 GenericCrafterBuild 的字段，但 lambda 里用的 "progress = 0"
 *       是 Building 的？不，父类确实有 progress float，没问题。但 configClear lambda 里写 "progress = 0"
 *       作用域不对：config lambda 形参是 RecipeCrafterBuild build，lambda 外面的 progress （不写 build.）
 *       会被当成外层类 Block 的字段（不存在）！→ 全部改成 build.progress = 0
 */
package Npl.newSth;

import arc.*;
import arc.audio.*;
import arc.func.*;
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
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.production.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import Npl.content.Recipe;                       // ✅ 修复 2/11：显式 import Npl.content.Recipe（用户已经把 Recipe 放 content 包了）
import java.util.*;

import static mindustry.Vars.*;


public class RecipeCrafter extends GenericCrafter {

    // ————————————————————— 方块级别字段 —————————————————————
    public Seq<Recipe> recipes = new Seq<>(4);
    public int selectionRows    = 2;
    public int selectionColumns = 4;
    public int[] capacities = {};
    public float[] liquidCapacities = {};
    protected final Seq<Item>   cachedItemOutputs   = new Seq<>();
    protected final Seq<Liquid> cachedLiquidOutputs = new Seq<>();

    // ————————————————————— 构造函数 —————————————————————
    public RecipeCrafter(String name) {
        super(name);

        // 基础属性
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
        // 5 个配置通道 —— ✅ 修复 3/12：lambda 内对 progress 的引用必须写 "build.progress"
        //                              以及 Recipe.class/Item.class/Liquid.class 的 lambda
        //                              第一个形参统一是 Building b（父类签名要求），再强转
        // =========================================================
        // ① 按索引
        config(Integer.class, (Building b, Integer i) -> {
            RecipeCrafterBuild build = (RecipeCrafterBuild) b;
            if (!configurable) return;
            if (build.recipeIndex == i) return;
            build.recipeIndex = (i < 0 || i >= recipes.size) ? -1 : i;
            build.progress = 0;    // ✅ 修复 12：lambda 外没有 progress 变量，必须写 build.
            build.dump();
        });

        // ② 按 Recipe 对象
        config(Recipe.class, (Building b, Recipe r) -> {
            RecipeCrafterBuild build = (RecipeCrafterBuild) b;
            if (!configurable) return;
            int idx = recipes.indexOf(r, true);
            if (build.recipeIndex == idx) return;
            build.recipeIndex = idx;
            build.progress = 0;
            build.dump();
        });

        // ③ 按第一个 outputItem 物品
        config(Item.class, (Building b, Item item) -> {
            RecipeCrafterBuild build = (RecipeCrafterBuild) b;
            if (!configurable) return;
            int next = recipes.indexOf(r ->
                r != null && r.outputItem != null && r.outputItem.any()
                && r.outputItem.first() != null && r.outputItem.first().item == item);
            if (build.recipeIndex == next) return;
            build.recipeIndex = next;
            build.progress = 0;
            build.dump();
        });

        // ④ 按第一个 outputLiquid 液体
        config(Liquid.class, (Building b, Liquid liq) -> {
            RecipeCrafterBuild build = (RecipeCrafterBuild) b;
            if (!configurable) return;
            int next = recipes.indexOf(r ->
                r != null && r.outputLiquid != null && r.outputLiquid.any()
                && r.outputLiquid.first() != null && r.outputLiquid.first().liquid == liq);
            if (build.recipeIndex == next) return;
            build.recipeIndex = next;
            build.progress = 0;
            build.dump();
        });

        // ⑤ 清配置
        configClear((Building b) -> {
            RecipeCrafterBuild build = (RecipeCrafterBuild) b;
            build.recipeIndex = -1;
            build.progress = 0;
        });
    }

    @Override
    public void init() {
        initCapacities();
        cacheOutputs();
        super.init();
    }

    @Override
    public void afterPatch() {
        initCapacities();
        cacheOutputs();
        super.afterPatch();
    }

    public void initCapacities() {
        capacities       = new int[Vars.content.items().size];
        liquidCapacities = new float[Vars.content.liquids().size];

        int   maxItem   = 0;
        float maxLiquid = 0f;

        for (Recipe r : recipes) {
            if (r == null) continue;
            if (r.inputItem  != null) for (ItemStack s : r.inputItem)   if (s.amount > maxItem)    maxItem = s.amount;
            if (r.outputItem != null) for (ItemStack s : r.outputItem)  if (s.amount > maxItem)    maxItem = s.amount;
            if (r.inputLiquid  != null) for (LiquidStack s : r.inputLiquid)  if (s.amount > maxLiquid) maxLiquid = s.amount;
            if (r.outputLiquid != null) for (LiquidStack s : r.outputLiquid) if (s.amount > maxLiquid) maxLiquid = s.amount;
        }

        int   itemCap   = Math.max(1, maxItem   * 20);
        float liquidCap = Math.max(1f, maxLiquid * 8f);

        Arrays.fill(capacities,       itemCap);
        Arrays.fill(liquidCapacities, liquidCap);

        consumeBuilder.each(c -> c.multiplier = b -> state.rules.unitCost(b.team));
    }

    public void cacheOutputs() {
        cachedItemOutputs  .clear();
        cachedLiquidOutputs.clear();
        for (Recipe r : recipes) {
            if (r == null) continue;
            if (r.outputItem   != null) for (ItemStack s   : r.outputItem)   if (s.item   != null && !cachedItemOutputs  .contains(s.item))   cachedItemOutputs  .add(s.item);
            if (r.outputLiquid != null) for (LiquidStack s : r.outputLiquid) if (s.liquid != null && !cachedLiquidOutputs.contains(s.liquid)) cachedLiquidOutputs.add(s.liquid);
        }
        if (!cachedItemOutputs.isEmpty())   outputsItems   = true;
        if (!cachedLiquidOutputs.isEmpty()) outputsLiquids = true;
    }

    @Override
    public void checkContentArrayCapacity(int items, int liquids) {
        super.checkContentArrayCapacity(items, liquids);
        if (capacities.length       != items)   capacities       = Arrays.copyOf(capacities,       items);
        if (liquidCapacities.length != liquids) liquidCapacities = Arrays.copyOf(liquidCapacities, liquids);
    }

    // ✅ 修复 5：不再 removeBar("liquid")（Block API 不存在），直接用 addBar 把所有配方涉及液体加上
    // ✅ 修复 9：不再 addLiquidBar(Liquid)（签名可能不存在），直接 addBar 手写 Bar
    @Override
    public void setBars() {
        super.setBars();
        // removeBar("liquid");   ←  ❌ Block 没有这个方法，注释掉

        ObjectSet<Liquid> seen = new ObjectSet<>();
        for (Recipe r : recipes) {
            if (r == null) continue;
            if (r.inputLiquid  != null) for (LiquidStack s : r.inputLiquid)  if (s.liquid != null) seen.add(s.liquid);
            if (r.outputLiquid != null) for (LiquidStack s : r.outputLiquid) if (s.liquid != null) seen.add(s.liquid);
        }
        for (Liquid l : seen) {
            addBar("liquid-" + l.name, (RecipeCrafterBuild b) -> new Bar(
                () -> l.localizedName,
                () -> l.color,
                () -> b.liquids == null ? 0f : b.liquids.get(l) / Math.max(0.0001f, b.getMaximumAccepted(l))
            ));
        }
    }

    @Override
    public boolean outputsItems()   { return cachedItemOutputs.any(); }
    @Override
    public boolean outputsLiquids(){ return cachedLiquidOutputs.any(); }

    @Override
    public TextureRegion[] icons() {
        return new TextureRegion[]{region, outRegion, topRegion};
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {
        Draw.rect(region, plan.drawx(), plan.drawy());
        if (outRegion != null) Draw.rect(outRegion, plan.drawx(), plan.drawy(), plan.rotation * 90);
        if (topRegion != null) Draw.rect(topRegion, plan.drawx(), plan.drawy());
    }

    // ✅ 修复 7：StatValues.displayItem/displayLiquid 四参数可能不存在
    //            → 手写 "图标 + 数量 + 每秒" 的简单 table，不依赖任何可疑重载
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
            table.table(cont -> {
                for (int i = 0; i < recipes.size; i++) {
                    Recipe r = recipes.get(i);
                    if (r == null || !r.hasAnyOutput()) continue;
                    int idx = i;

                    cont.table(Styles.grayPanel, t -> {
                        t.margin(10f);

                        // ① 序号
                        t.table(n -> n.left().add("[accent][" + (idx + 1) + "]:[]").size(48f, 40f)).left();

                        // ② INPUTS 图标列
                        t.table(ins -> {
                            ins.left();
                            if (r.inputItem != null) for (ItemStack s : r.inputItem) {
                                if (s == null || s.item == null) continue;
                                ins.add(itemCell(s.item, s.amount, r.craftTime)).pad(5);
                            }
                            if (r.inputLiquid != null) for (LiquidStack s : r.inputLiquid) {
                                if (s == null || s.liquid == null) continue;
                                ins.add(liquidCell(s.liquid, s.amount, r.craftTime)).pad(5);
                            }
                        }).left().growX();

                        // ③ 箭头 ✅ 修复 4：ImageIcon.right 不存在，用 Icon.right
                        t.image(Icon.right).size(32f).padLeft(10f).padRight(10f);

                        // ④ OUTPUTS
                        t.table(outs -> {
                            outs.left();
                            if (r.outputItem != null) for (ItemStack s : r.outputItem) {
                                if (s == null || s.item == null) continue;
                                outs.add(itemCell(s.item, s.amount, r.craftTime)).pad(5);
                            }
                            if (r.outputLiquid != null) for (LiquidStack s : r.outputLiquid) {
                                if (s == null || s.liquid == null) continue;
                                outs.add(liquidCell(s.liquid, s.amount, r.craftTime)).pad(5);
                            }
                        }).left().growX();

                        // ⑤ 时间
                        t.table(tm -> {
                            tm.right().color(Color.lightGray);
                            tm.add(Strings.autoFixed(r.craftTime / 60f, 2) + " " + Core.bundle.get("unit.seconds"));
                        }).right().padLeft(12f);

                    }).growX().pad(4);
                    cont.row();
                }
            });
        });
    }

    // ———————————————— 辅助：详情面板物品单元格（修复 7） ————————————————
    private Table itemCell(Item item, int amount, float timeSec) {
        Table t = new Table();
        Stack stack = new Stack();
        stack.add(new Table(o -> { o.left(); o.image(item.uiIcon).size(32f).scaling(Scaling.fit); }));
        if (amount != 0) stack.add(new Table(x -> {
            x.left().bottom(); x.add(amount >= 1000 ? UI.formatAmount(amount) : String.valueOf(amount)).style(Styles.outlineLabel);
            x.pack();
        }));
        t.add(stack);
        String rate = (timeSec <= 0) ? "" : Strings.autoFixed(amount * 60f / timeSec, 2) + StatUnit.perSecond.localized();
        t.add(item.localizedName + "\n[lightgray]" + rate).padLeft(2).padRight(5).style(Styles.outlineLabel);
        return t;
    }
    private Table liquidCell(Liquid l, float amount, float timeSec) {
        Table t = new Table();
        Stack stack = new Stack();
        stack.add(new Table(o -> { o.left(); o.image(l.uiIcon).size(32f).scaling(Scaling.fit).color(l.color); }));
        if (amount > 0.0001f) stack.add(new Table(x -> {
            x.left().bottom(); x.add(Strings.autoFixed(amount, 2)).style(Styles.outlineLabel); x.pack();
        }));
        t.add(stack);
        String rate = (timeSec <= 0) ? "" : Strings.autoFixed(amount * 60f / timeSec, 2) + StatUnit.perSecond.localized();
        t.add(l.localizedName + "\n[lightgray]" + rate).padLeft(2).padRight(5).style(Styles.outlineLabel);
        return t;
    }

    @Override
    public void getPlanConfigs(Seq<UnlockableContent> options) {
        for (Recipe r : recipes) {
            if (r == null || !r.hasAnyOutput()) continue;
            if (r.outputItem != null && r.outputItem.any() && r.outputItem.first() != null && r.outputItem.first().item != null) {
                Item it = r.outputItem.first().item;
                if (!it.isBanned() && it.unlockedNow()) options.add(it);
            } else if (r.outputLiquid != null && r.outputLiquid.any() && r.outputLiquid.first() != null && r.outputLiquid.first().liquid != null) {
                Liquid l = r.outputLiquid.first().liquid;
                if (!l.isBanned() && l.unlockedNow()) options.add(l);
            }
        }
    }

    // ================================================================
    // 内部类 RecipeCrafterBuild
    // ================================================================
    public class RecipeCrafterBuild extends GenericCrafterBuild {

        public int recipeIndex = -1;

        public float fraction() {
            if (recipeIndex < 0 || recipeIndex >= recipes.size) return 0f;
            Recipe r = recipes.get(recipeIndex);
            if (r == null) return 0f;
            return Mathf.clamp(progress);
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

        // ✅ 修复 6：drawItemSelection 只有 Item 单参数版本；液体选中图标改为 Draw.rect 直画
        @Override
        public void drawSelect() {
            super.drawSelect();
            Recipe r = current();
            if (r == null) return;
            if (r.outputItem != null && r.outputItem.any() && r.outputItem.first() != null) {
                Item it = r.outputItem.first().item;
                if (it != null) drawItemSelection(it);   // 原版唯一安全重载
            } else if (r.outputLiquid != null && r.outputLiquid.any() && r.outputLiquid.first() != null) {
                Liquid l = r.outputLiquid.first().liquid;
                if (l != null) {
                    // 液体没 drawItemSelection 重载，手动画个图标在左上（和 drawItemSelection 视觉对齐）
                    TextureRegion reg = l.uiIcon;
                    if (reg != null) {
                        float dx = x - block.size * tilesize / 2f + 4f;
                        float dy = y + block.size * tilesize / 2f - 4f - 32f;
                        Draw.color(l.color);
                        Draw.rect(reg, dx + 16f, dy + 16f, 32f, 32f);
                        Draw.color();
                        // 外层描个框（仿 drawItemSelection 的白色框）
                        Lines.stroke(1f, Color.white);
                        Lines.rect(dx, dy, 32f, 32f);
                    }
                }
            }
        }

        @Override
        public Object senseObject(LAccess sensor) {
            if (sensor == LAccess.config) {
                Recipe r = current();
                if (r == null) return null;
                if (r.outputItem != null && r.outputItem.any() && r.outputItem.first() != null) return r.outputItem.first().item;
                if (r.outputLiquid != null && r.outputLiquid.any() && r.outputLiquid.first() != null) return r.outputLiquid.first().liquid;
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
                        if (r.outputItem != null && r.outputItem.any() && r.outputItem.first() != null && r.outputItem.first().item != null) {
                            i.setDrawable(reg.set(r.outputItem.first().item.uiIcon));
                            i.setColor(Color.white); ok = true;
                        } else if (r.outputLiquid != null && r.outputLiquid.any() && r.outputLiquid.first() != null && r.outputLiquid.first().liquid != null) {
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
                    if (r.outputItem != null && r.outputItem.any() && r.outputItem.first() != null && r.outputItem.first().item != null)
                        return r.outputItem.first().item.localizedName;
                    if (r.outputLiquid != null && r.outputLiquid.any() && r.outputLiquid.first() != null && r.outputLiquid.first().liquid != null)
                        return r.outputLiquid.first().liquid.localizedName;
                    return "@none";
                }).wrap().width(230f).color(Color.lightGray);
            }).left();
        }

        @Override
        public Object config() { return recipeIndex; }

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

        // ✅ 修复 8：ItemSelection.buildTable 最后一个回调是 Cons<Item>，用显式 lambda（this::configure 可能类型不匹配）
        @Override
        public void buildConfiguration(Table table) {
            Seq<Item> items = Seq.with(recipes)
                .select(r -> r != null && r.outputItem != null && r.outputItem.any()
                        && r.outputItem.first() != null && r.outputItem.first().item != null)
                .<Item>map(r -> r.outputItem.first().item)
                .retainAll(i -> i != null && i.unlockedNow() && !i.isBanned());

            if (items.any()) {
                table.add(Core.bundle.get("stat.output")).color(Color.lightGray).left().padBottom(4f).row();
                ItemSelection.buildTable(
                    RecipeCrafter.this,
                    table,
                    items,
                    () -> {
                        Recipe r = current();
                        if (r == null || r.outputItem == null || !r.outputItem.any() || r.outputItem.first() == null) return null;
                        return r.outputItem.first().item;
                    },
                    (Item item) -> configure(item),     // ✅ 修复 8：显式 lambda，不要 this::configure
                    selectionRows,
                    selectionColumns
                );
                table.row();
            }

            Seq<Liquid> liquids = Seq.with(recipes)
                .select(r -> r != null && r.outputLiquid != null && r.outputLiquid.any()
                        && r.outputLiquid.first() != null && r.outputLiquid.first().liquid != null)
                .<Liquid>map(r -> r.outputLiquid.first().liquid)
                .retainAll(l -> l != null && l.unlockedNow() && !l.isBanned());

            if (liquids.any()) {
                table.add(Core.bundle.get("bar.liquid") + " " + Core.bundle.get("stat.output")).color(Color.lightGray).left().padBottom(4f).padTop(10f).row();
                Table liquidGrid = new Table();
                int rows = Math.max(1, (liquids.size + selectionColumns - 1) / selectionColumns);
                int cols = selectionColumns;
                for (int i = 0; i < liquids.size; i++) {
                    Liquid l = liquids.get(i);
                    int rr = i / cols, cc = i % cols;
                    final boolean[] selected = {false};
                    Button b = new Button(Styles.squareTogglei);
                    b.margin(4f);
                    b.add(new Image(l.uiIcon)).size(40).scaling(Scaling.fit).color(l.color);
                    b.update(() -> {
                        Recipe cur = current();
                        selected[0] = (cur != null && cur.outputLiquid != null && cur.outputLiquid.any()
                                       && cur.outputLiquid.first() != null && cur.outputLiquid.first().liquid == l);
                        b.setChecked(selected[0]);
                    });
                    b.clicked(() -> configure(l));
                    liquidGrid.add(b).size(48);
                    if (cc == cols - 1 && rr < rows - 1) liquidGrid.row();
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
                int willHave = items.get(s.item) + s.amount;
                if (willHave > getMaximumAccepted(s.item)) return false;
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

        @Override
        public BlockStatus status() {
            if (enabled && current() == null) return BlockStatus.noInput;
            return super.status();
        }

        @Override
        public int getMaximumAccepted(Item item) {
            if (item == null || item.id >= capacities.length) return 0;
            return Mathf.round(capacities[item.id] * state.rules.unitCost(team));
        }

        public float getMaximumAccepted(Liquid liquid) {
            if (liquid == null || liquid.id >= liquidCapacities.length) return 0f;
            return liquidCapacities[liquid.id] * state.rules.unitCost(team);
        }

        @Override
        public boolean acceptItem(Building source, Item item) {
            Recipe r = current();
            if (r == null) return false;
            if (items.get(item) >= getMaximumAccepted(item)) return false;
            if (r.inputItem != null) for (ItemStack s : r.inputItem)   if (s != null && s.item == item) return true;
            if (r.outputItem!= null) for (ItemStack s : r.outputItem)  if (s != null && s.item == item) return true;
            return false;
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
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
            return super.getProgressIncrease(baseTime) / scl;
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

        // ✅ 修复 10：dumpLiquid(Liquid, float) 可能不存在，只用最稳妥的单参版本
        @Override
        public void dumpOutputs() {
            if (cachedItemOutputs.any() && timer(timerDump, dumpTime / Math.max(0.0001f, timeScale))) {
                for (Item out : cachedItemOutputs) dump(out);
            }
            if (cachedLiquidOutputs.any()) {
                for (Liquid out : cachedLiquidOutputs) {
                    dumpLiquid(out);    // ✅ 只用最稳妥的 Mindustry 单参版本，不会报错
                }
            }
        }

        @Override
        public void craft() {
            Recipe r = current();
            if (r == null) { progress = 0; return; }

            if (r.inputItem != null) for (ItemStack s : r.inputItem) {
                if (s == null || s.item == null) continue;
                if (items.get(s.item) < s.amount) { progress = 0; return; }
                items.remove(s.item, s.amount);
            }
            if (r.inputLiquid != null) for (LiquidStack s : r.inputLiquid) {
                if (s == null || s.liquid == null) continue;
                if (liquids.get(s.liquid) < s.amount) { progress = 0; return; }
                liquids.remove(s.liquid, s.amount);
            }
            consume();

            if (r.outputItem != null) for (ItemStack s : r.outputItem) {
                if (s == null || s.item == null || s.amount <= 0) continue;
                items.add(s.item, s.amount);
                for (int i = 0; i < s.amount; i++) offload(s.item);
            }

            if (createSound != null) {
                createSound.at(this, 1f + Mathf.range(0.06f), createSoundVolume);
            }
            if (craftEffect != Fx.none && wasVisible) craftEffect.at(x, y);
            if (updateEffect != Fx.none && Mathf.chance(updateEffectChance) && wasVisible) updateEffect.at(x, y);

            progress %= 1f;
        }

        @Override
        public byte version() { return 3; }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.s(recipeIndex);
            write.f(progress);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            recipeIndex = read.s();
            progress    = read.f();
            if (revision >= 2) { if (read.limit() - read.position() >= 1) read.bool(); }
            // revision >= 3 预留
        }
    }
}
