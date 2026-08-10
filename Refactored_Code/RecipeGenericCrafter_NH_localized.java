
package Npl.newSth;

import Npl.content.Recipe;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Layer;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Scaling;
import arc.util.Strings;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.type.*;
import mindustry.ui.Bar;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.meta.BlockStatus;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import mindustry.world.meta.StatValue;

import static mindustry.Vars.state;

/**
 * 完全照抄 NewHorizonMod-2.1.2 newhorizon.expand.block.production.factory.RecipeGenericCrafter
 *
 * 本地化改动清单（重要！这些是从 NH 原版改出来的，别改回去）：
 *
 * 【继承链】
 *   NH 原版：extends MultiBlockCrafter（NH 自己的类，我们没有）
 *   本地化后：extends GenericCrafter（Mindustry 159.6 原生父类，和我们之前 RecipeCrafter 同父类）
 *
 * 【字段删减】
 *   NH 原版：3 个 Seq：itemOutput / liquidOutput / payloadOutput
 *   本地化后：只保留 itemOutput / liquidOutput；删除 payloadOutput（没有 Payload 体系）
 *
 * 【基础属性】
 *   NH 原版：没有 rotate 显式赋值（默认 true）
 *   本地化后：rotate = false（用户明确说不要方向判定）
 *
 * 【init() / outputsItems() / setStats() / setBars() / display() 静态 Table】
 *   NH 原版：这 5 个方法逻辑保留，只做以下 3 点改写：
 *     1) 删除所有 payloadOutput / recipe.outputPayload 相关
 *     2) 删除 UI.formatAmount（Mindustry159.6 UI 类没有这个方法）
 *     3) Table.color(Color) → 改成 .add(...).color(...)（Table 本身没有 color() 方法）
 *
 * 【内部类 RecipeGenericCrafterBuild（核心）】
 *   NH 原版：extends AdaptCrafterBuild（NH 自己的类，我们没有）
 *   本地化后：extends GenericCrafterBuild（GenericCrafter 自带的内部 Building 类）
 *
 *   字段：
 *     NH: recipeIndex = -1；方法：getRecipe() / getDisplayRecipe() / updateRecipe() / validRecipe()  → 全部保留，删 Payload 分支
 *     NH: updateTile() → 保留液体生产+物品容量裁剪，删 Payload
 *     NH: dumpOutputs() → 保留 itemOutput::dump + liquidOutput::dumpLiquid（液体用单参 dumpLiquid），删 Payload
 *     NH: shouldConsume() → 删除 ignoreLiquidFullness / dumpExtraLiquid（Building 基类没有这俩字段），简化成容量满就停
 *     NH: getProgressIncrease() → 保留 craftTime 按配方缩放逻辑，不改
 *     NH: craft() → 大改！
 *         - 保留 consume()（Mindustry 原生，走 ConsumeRecipe.trigger() 扣原料）
 *         - 物品输出：原来的 for (int i=0..amount) offload(item) 【改成】items.add(s.item,s.amount) 然后交给 dumpOutputs 用 dump() 推（1:1 不囤积）
 *         - 液体输出：NH 写 updateTile 流式加罐了，这里不重复
 *         - progress %= 1f 【改成】progress -= 1f（ConfigurableBlock 同款）
 *         - 删 wasVisible 判断（某些版本没有 wasVisible 字段）直接 craftEffect.at
 *
 *   新增本地化必要方法：
 *     - config()/configure(Integer/Item/Liquid)：和 RecipeCrafter/ConBlock 同款，lambda 首参写具体内部类
 *     - buildConfiguration(Table)：手写 Item/Liquid 网格（Mindustry159.6 没 ItemSelection），suppress 防 UI 递归
 *     - acceptItem()/acceptLiquid()/getMaximumAccepted()：按当前配方过滤
 *     - version()/write()/read()：v1 版本对齐（ConfigurableBlock 同款）
 *     - dumpEverything()：切配方清仓用，【防呆版本】不 while(true) 死循环
 *     - draw()/icons()/drawPlanRegion()：全做 null 检查 + rotate=false 不转
 *
 *   其他：
 *     - NH 原版 stack = new Stack() 冲突 → 写成全称 arc.scene.ui.layout.Stack
 *     - NH 原版 status() → 保留
 */
public class RecipeGenericCrafter extends GenericCrafter {

    public Seq<Recipe> recipes = new Seq<>();

    public Seq<Item>   itemOutput   = new Seq<>();
    public Seq<Liquid> liquidOutput = new Seq<>();

    public int selectionRows = 4;
    public int selectionColumns = 8;

    // ==========================================================
    // 贴图（和 ConfigurableBlock 同款：若没有 -out/-top 图，这俩就是 null → 绘制时判空）
    // ==========================================================
    public TextureRegion outRegion;
    public TextureRegion topRegion;

    public RecipeGenericCrafter(String name) {
        super(name);

        // —— 基础属性
        update = true;
        solid = true;
        hasItems = true;
        hasPower = true;
        hasLiquids = true;
        rotate = false;          // ← 本地化：用户明确说不要方向判定
        configurable = true;
        itemCapacity   = 30;
        liquidCapacity = 30f;
        size = 2;
        health = 160;
        acceptsItems = true;

        // ==========================================================
        // ↓↓↓ 完全照抄 NH RecipeGenericCrafter 第 37 行：注册 ConsumeRecipe
        // 用方法引用（和 NH RecipeGenericCrafterBuild::getRecipe 同款写法）
        // ==========================================================
        consume(new ConsumeRecipe(
            RecipeGenericCrafterBuild::getRecipe,
            RecipeGenericCrafterBuild::getDisplayRecipe
        ).update(true));

        // ————————— 配置通道：和 ConfigurableBlock 同款 lambda 首参写具体内部类 —————————
        // ① 按索引切换（逻辑处理器 configure 数字）
        config(Integer.class, (RecipeGenericCrafterBuild build, Integer i) -> {
            int next = i % recipes.size;
            if (next < 0) next += recipes.size;
            if (build.recipeIndex != next) {
                build.recipeIndex = next;
                build.dumpEverything();
            }
        });
        // ② 按物品切换（玩家点配置面板物品图标）
        config(Item.class, (RecipeGenericCrafterBuild build, Item item) -> {
            for (int i = 0; i < recipes.size; i++) {
                Recipe r = recipes.get(i);
                if (r != null && r.outputItem != null && r.outputItem.any()
                        && r.outputItem.first() != null && r.outputItem.first().item == item) {
                    if (build.recipeIndex != i) {
                        build.recipeIndex = i;
                        build.dumpEverything();
                    }
                    return;
                }
            }
        });
        // ③ 按液体切换（玩家点配置面板液体图标）
        config(Liquid.class, (RecipeGenericCrafterBuild build, Liquid liquid) -> {
            for (int i = 0; i < recipes.size; i++) {
                Recipe r = recipes.get(i);
                if (r != null && r.outputLiquid != null && r.outputLiquid.any()
                        && r.outputLiquid.first() != null && r.outputLiquid.first().liquid == liquid) {
                    if (build.recipeIndex != i) {
                        build.recipeIndex = i;
                        build.dumpEverything();
                    }
                    return;
                }
            }
        });
        // ④ 清空
        configClear((RecipeGenericCrafterBuild build) -> {
            if (build.recipeIndex != 0 && recipes.size > 0) {
                build.recipeIndex = 0;
                build.dumpEverything();
            }
        });
    }

    // ==========================================================
    // ↓↓↓ 完全照抄 NH RecipeGenericCrafter.display(UnlockableContent,float,float) 静态方法
    // 本地化改动：
    //   - UI.formatAmount 不存在 → 改成 autoFixed 或 "Xk" 简化
    //   - Table.color(Color) → .add(...).color(...)
    //   - Stack 写全称避免和 java.util.Stack 冲突
    // ==========================================================
    public static Table display(UnlockableContent content, float amount, float timePeriod) {
        Table table = new Table();
        arc.scene.ui.layout.Stack stack = new arc.scene.ui.layout.Stack();

        stack.add(new Table(o -> {
            o.left();
            o.add(new Image(content.uiIcon)).size(32f).scaling(Scaling.fit);
        }));

        if (amount != 0) {
            stack.add(new Table(t -> {
                t.left().bottom();
                String text;
                if (amount >= 1000f) {
                    text = Strings.autoFixed(amount / 1000f, 1) + "k";
                } else if ((int)amount == amount) {
                    text = Integer.toString((int) amount);
                } else {
                    text = Strings.autoFixed(amount, 2);
                }
                t.add(text).style(Styles.outlineLabel);
                t.pack();
            }));
        }

        table.add(stack);
        // 本地化：右边信息文字。原来的 StatValues.withTooltip(stack, content) 不保证有，这里改成最朴素的 add 文字
        String rate = Strings.autoFixed(amount / Math.max(0.0001f, timePeriod / 60f), 2);
        table.add((content.localizedName + "\n") + "[lightgray]" + rate + StatUnit.perSecond.localized())
                .padLeft(2).padRight(5).style(Styles.outlineLabel).color(Color.lightGray);
        return table;
    }

    // ==========================================================
    // ↓↓↓ 完全照抄 NH RecipeGenericCrafter.init()
    // 本地化：删除 Payload 相关 / outputsLiquids 字段 → outputsLiquid() 方法
    // ==========================================================
    @Override
    public void init() {
        super.init();

        recipes.each(recipe -> {
            recipe.inputItem  .each(stack -> itemFilter[stack.item.id] = true);
            recipe.inputLiquid.each(stack -> liquidFilter[stack.liquid.id] = true);

            recipe.outputItem  .each(stack -> itemOutput.add(stack.item));
            recipe.outputLiquid.each(stack -> liquidOutput.add(stack.liquid));
        });

        // NH 原版：清掉父类 GenericCrafter 默认单配方
        outputItem = null;
        outputLiquid = null;

        if (recipes.isEmpty()) {
            outputItems = new ItemStack[]{new ItemStack(Items.copper, 0)};
            outputLiquids = new LiquidStack[]{new LiquidStack(Liquids.water, 0f)};
        } else {
            Recipe firstRecipe = recipes.first();

            outputItems = new ItemStack[Math.max(firstRecipe.outputItem.size, 1)];
            for (int i = 0; i < outputItems.length; i++) {
                outputItems[i] = i < firstRecipe.outputItem.size
                        ? firstRecipe.outputItem.get(i)
                        : new ItemStack(Items.copper, 0);
            }

            outputLiquids = new LiquidStack[Math.max(firstRecipe.outputLiquid.size, 1)];
            for (int i = 0; i < outputLiquids.length; i++) {
                outputLiquids[i] = i < firstRecipe.outputLiquid.size
                        ? new LiquidStack(firstRecipe.outputLiquid.get(i).liquid, 0f)
                        : new LiquidStack(Liquids.water, 0f);
            }
        }

        // NH 原版：craftTime 设成 60，然后每个配方自己的 craftTime 在 getProgressIncrease 里缩放
        craftTime = 60f;

        // NH 原版：outputsLiquid 字段
        if (liquidOutput.any()) outputsLiquid = true;

        // ————————— 本地化：容量按"所有配方最大输入输出"初始化 —————————
        for (Recipe r : recipes) {
            if (r == null) continue;
            if (r.inputItem  != null) for (ItemStack s   : r.inputItem)   itemCapacity   = Math.max(itemCapacity,   s.amount * 2 + 20);
            if (r.outputItem != null) for (ItemStack s   : r.outputItem)  itemCapacity   = Math.max(itemCapacity,   s.amount * 2 + 20);
            if (r.inputLiquid  != null) for (LiquidStack s : r.inputLiquid)  liquidCapacity = Math.max(liquidCapacity, s.amount * 2f + 10f);
            if (r.outputLiquid != null) for (LiquidStack s : r.outputLiquid) liquidCapacity = Math.max(liquidCapacity, s.amount * 2f + 10f);
        }
    }

    // ==========================================================
    // ↓↓↓ 完全照抄 NH outputsItems()
    // ==========================================================
    @Override
    public boolean outputsItems() {
        return itemOutput.any();
    }

    // ==========================================================
    // ↓↓↓ 完全照抄 NH setStats() / display() StatValue（删 Payload 分支）
    // ==========================================================
    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.input, display());
        stats.remove(Stat.output);
        stats.remove(Stat.productionTime);
    }

    public StatValue display() {
        return table -> {
            table.row();
            table.table(cont -> {
                for (int i = 0; i < recipes.size; i++) {
                    Recipe recipe = recipes.get(i);
                    int finalI = i;
                    cont.table(t -> {
                        t.left().marginLeft(12f).add("[accent][" + (finalI + 1) + "]:[]").width(48f);
                        t.table(inner -> {
                            inner.table(row -> {
                                row.left();
                                recipe.inputItem  .each(stack -> row.add(display(stack.item, stack.amount, recipe.craftTime)));
                                recipe.inputLiquid.each(stack -> row.add(display(stack.liquid, stack.amount * Time.toSeconds, 60f)));
                            }).growX();
                            inner.table(row -> {
                                row.left();
                                row.image(Icon.right).size(32f).padLeft(8f).padRight(12f);
                                recipe.outputItem  .each(stack -> row.add(display(stack.item, stack.amount, recipe.craftTime)));
                                recipe.outputLiquid.each(stack -> row.add(display(stack.liquid, stack.amount * Time.toSeconds, 60f)));
                            }).growX();
                        });
                    }).fillX();
                    cont.row();
                }
            });
        };
    }

    // ==========================================================
    // ↓↓↓ 完全照抄 NH setBars()：删默认"liquid"条，给配方里所有液体单独加
    // 本地化：Bar 构造用(String, Color, Floatf) 三参版（ConfigurableBlock 同款，不用 provider）
    // ==========================================================
    @Override
    public void setBars() {
        super.setBars();
        removeBar("liquid");
        removeBar("progress");   // ← 本地化：用户之前说不需要渲染进度 bar（可选，要的话可以加回来）

        Seq<Liquid> seen = new Seq<>();
        recipes.each(recipe -> {
            recipe.inputLiquid .each(stack -> addLiquidBar(stack.liquid, seen));
            recipe.outputLiquid.each(stack -> addLiquidBar(stack.liquid, seen));
        });
    }
    private void addLiquidBar(Liquid l, Seq<Liquid> seen) {
        if (l == null || seen.contains(l)) return;
        seen.add(l);
        addBar("liquid-" + l.name, (RecipeGenericCrafterBuild b) -> new Bar(
                l.localizedName,
                l.color,
                () -> b.liquids == null ? 0f : b.liquids.get(l) / liquidCapacity
        ));
    }

    // ==========================================================
    // 贴图加载
    // ==========================================================
    @Override
    public void load() {
        super.load();
        outRegion = loadRegion(name + "-out");
        topRegion = loadRegion(name + "-top");
    }

    @Override
    public TextureRegion[] icons() {
        // 本地化：绝对不能塞 null 进数组 → 会在 MultiPacker 读 name 时 NPE 崩溃
        int count = 1;
        if (outRegion != null) count++;
        if (topRegion != null) count++;
        TextureRegion[] arr = new TextureRegion[count];
        int i = 0; arr[i++] = region;
        if (outRegion != null) arr[i++] = outRegion;
        if (topRegion != null) arr[i++] = topRegion;
        return arr;
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list) {
        Draw.rect(region, plan.drawx(), plan.drawy());
        if (outRegion != null) Draw.rect(outRegion, plan.drawx(), plan.drawy()); // rotate=false 不转
        if (topRegion != null) Draw.rect(topRegion, plan.drawx(), plan.drawy());
    }

    // ==========================================================
    // ↓↓↓ 内部 Building 类（完全照抄 NH RecipeGenericCrafterBuild 结构 + 本地化补方法）
    // ==========================================================
    public class RecipeGenericCrafterBuild extends GenericCrafterBuild {

        public int recipeIndex = -1;

        // ==========================================================
        // ↓↓↓ 完全照抄 NH getRecipe() / getDisplayRecipe() 一字不动
        // ==========================================================
        public Recipe getRecipe() {
            if (recipeIndex < 0 || recipeIndex >= recipes.size) return null;
            return recipes.get(recipeIndex);
        }
        public Recipe getDisplayRecipe() {
            if (recipeIndex < 0 && recipes.size > 0) return recipes.first();
            return getRecipe();
        }

        // ==========================================================
        // ↓↓↓ 完全照抄 NH updateRecipe()：找第一个能做的配方（删 Payload 分支）
        // ==========================================================
        public void updateRecipe() {
            for (int i = recipes.size - 1; i >= 0; i--) {
                boolean valid = true;
                Recipe r = recipes.get(i);
                for (ItemStack input : r.inputItem) {
                    if (items.get(input.item) < input.amount) { valid = false; break; }
                }
                if (valid) {
                    for (LiquidStack input : r.inputLiquid) {
                        if (liquids.get(input.liquid) < input.amount * Time.delta) { valid = false; break; }
                    }
                }
                if (valid) { recipeIndex = i; return; }
            }
            recipeIndex = -1;
        }

        // ==========================================================
        // ↓↓↓ 完全照抄 NH validRecipe()（删 Payload 分支）
        // ==========================================================
        public boolean validRecipe() {
            if (recipeIndex < 0) return false;
            Recipe r = recipes.get(recipeIndex);
            for (ItemStack input : r.inputItem) {
                if (items.get(input.item) < input.amount) return false;
            }
            for (LiquidStack input : r.inputLiquid) {
                if (liquids.get(input.liquid) < input.amount * Time.delta) return false;
            }
            return true;
        }

        // ==========================================================
        // ↓↓↓ 完全照抄 NH updateTile()（液体生产流式加罐；删 Payload；保留父类调用）
        // ==========================================================
        @Override
        public void updateTile() {
            if (!validRecipe()) updateRecipe();

            super.updateTile();

            Recipe current = getRecipe();
            if (current == null) return;

            // 液体流式产出（按 progress 增量，和 NH 原版 handleLiquid 思路一致）
            if (efficiency > 0 && current.outputLiquid != null && current.outputLiquid.any()) {
                float inc = getProgressIncrease(craftTime / current.craftTime);
                for (LiquidStack stack : current.outputLiquid) {
                    if (stack == null || stack.liquid == null) continue;
                    float add = Math.min(stack.amount * inc, liquidCapacity - liquids.get(stack.liquid));
                    if (add > 0f) handleLiquid(this, stack.liquid, add);
                }
            }

            // 物品容量裁剪（防止无限）
            if (current.outputItem != null) {
                for (ItemStack stack : current.outputItem) {
                    if (stack == null || stack.item == null) continue;
                    if (items.get(stack.item) >= itemCapacity) items.set(stack.item, itemCapacity);
                }
            }
        }

        // ==========================================================
        // ↓↓↓ 完全照抄 NH dumpOutputs()（删 Payload；液体改成单参 dumpLiquid）
        // ==========================================================
        @Override
        public void dumpOutputs() {
            boolean timer = timer(timerDump, dumpTime / timeScale);
            if (timer) itemOutput.each(this::dump);
            liquidOutput.each(this::dumpLiquid);
        }

        // ==========================================================
        // ↓↓↓ 本地化改写 shouldConsume()：删掉 NH 的 ignoreLiquidFullness/dumpExtraLiquid
        // （Building 基类没有这俩字段）→ 简化成"容量满就停"
        // ==========================================================
        @Override
        public boolean shouldConsume() {
            Recipe r = getRecipe();
            if (r == null) return false;
            if (!enabled) return false;

            // 输出物品：任何一个再一份就超容量 → 停
            if (r.outputItem != null) for (ItemStack s : r.outputItem) {
                if (s == null || s.item == null) continue;
                if (items.get(s.item) + s.amount > getMaximumAccepted(s.item)) return false;
            }
            // 输出液体：任何一个加满 → 停
            if (r.outputLiquid != null) for (LiquidStack s : r.outputLiquid) {
                if (s == null || s.liquid == null) continue;
                if (liquids.get(s.liquid) + s.amount * 0.001f > liquidCapacity - 0.001f) return false;
            }
            return true;
        }

        // ==========================================================
        // ↓↓↓ 完全照抄 NH getProgressIncrease()（按配方 craftTime 缩放）
        // ==========================================================
        @Override
        public float getProgressIncrease(float baseTime) {
            float scl = 1f;
            if (getRecipe() != null) scl = getRecipe().craftTime / craftTime;
            return super.getProgressIncrease(baseTime) / scl;
        }

        // ==========================================================
        // ↓↓↓ 本地化改写 craft()：
        //   - 保留 consume()（走 ConsumeRecipe.trigger 一次性扣原料）
        //   - 物品输出：items.add → 不直接 offload（交给 dumpOutputs() 用 dump()，保证 1:1 匹配）
        //   - 液体输出：updateTile 已流式加罐，这里不重复
        //   - progress：ConfigurableBlock 同款 progress -= 1f（不写 %=）
        //   - craftEffect：直接 at(x,y)
        // ==========================================================
        @Override
        public void craft() {
            if (getRecipe() == null) return;
            // 原料不足绝对不能 craft（efficiency 字段，159.6 是字段不是方法）
            if (efficiency < 0.9999f) return;

            // 扣原料（走 Consume 系统 → ConsumeRecipe.trigger()）
            consume();

            Recipe r = getRecipe();
            // 物品产物：加仓，不手动 offload
            if (r.outputItem != null) for (ItemStack s : r.outputItem) {
                if (s == null || s.item == null) continue;
                int add = Math.min(s.amount, itemCapacity - items.get(s.item));
                if (add > 0) items.add(s.item, add);
            }

            // 进度减掉 1，ConfigurableBlock 同款
            progress -= 1f;
            if (progress < 0f) progress = 0f;

            if (craftEffect != null) craftEffect.at(x, y);

            // 完成一次后再找下一个配方（玩家没手动切时能自动跳）
            updateRecipe();
        }

        // ==========================================================
        // ↓↓↓ 完全照抄 NH status()
        // ==========================================================
        @Override
        public BlockStatus status() {
            if (enabled && getRecipe() == null) return BlockStatus.noInput;
            return super.status();
        }

        // ==========================================================
        // 本地化新增：config 返回当前索引
        // ==========================================================
        @Override
        public Object config() { return recipeIndex; }

        // ==========================================================
        // 本地化新增：按当前配方接受/拒绝 物品/液体
        // ==========================================================
        @Override
        public boolean acceptItem(Building source, Item item) {
            Recipe r = getDisplayRecipe();
            if (r == null || !super.acceptItem(source, item)) return false;
            if (r.inputItem != null) for (ItemStack s : r.inputItem) {
                if (s != null && s.item == item) return items.get(item) < getMaximumAccepted(item);
            }
            return false;
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            Recipe r = getDisplayRecipe();
            if (r == null || !super.acceptLiquid(source, liquid)) return false;
            if (r.inputLiquid != null) for (LiquidStack s : r.inputLiquid) {
                if (s != null && s.liquid == liquid) return true;
            }
            return false;
        }

        @Override
        public int getMaximumAccepted(Item item) {
            int max = itemCapacity;
            Recipe r = getDisplayRecipe();
            if (r != null) {
                if (r.inputItem != null) for (ItemStack s : r.inputItem) {
                    if (s != null && s.item == item) max = Math.max(max, s.amount * 2 + 10);
                }
                if (r.outputItem != null) for (ItemStack s : r.outputItem) {
                    if (s != null && s.item == item) max = Math.max(max, s.amount * 2 + 10);
                }
            }
            return max;
        }

        // ==========================================================
        // 本地化新增：draw() / buildConfiguration(Table) / 存档 / dumpEverything
        // ==========================================================
        @Override
        public void draw() {
            Draw.rect(region, x, y);
            if (outRegion != null) Draw.rect(outRegion, x, y);   // rotate=false 不转
            Draw.z(Layer.blockOver);
            if (topRegion != null) {
                Draw.z(Layer.blockOver + 0.1f);
                Draw.rect(topRegion, x, y);
            }
        }

        @Override
        public void buildConfiguration(Table table) {
            Seq<Item> items = new Seq<>();
            for (Recipe r : recipes) {
                if (r == null || r.outputItem == null || !r.outputItem.any() || r.outputItem.first() == null) continue;
                Item it = r.outputItem.first().item;
                if (it != null && it.unlockedNow() && !it.isBanned() && !items.contains(it)) items.add(it);
            }

            if (items.any()) {
                table.add(Core.bundle.get("stat.output")).color(Color.lightGray).left().padBottom(4f).row();
                Table grid = new Table();
                int rows = Math.max(1, (items.size + selectionColumns - 1) / selectionColumns);
                int cols = selectionColumns;
                for (int i = 0; i < items.size; i++) {
                    Item it = items.get(i);
                    Button b = new Button(Styles.squareTogglei);
                    b.margin(4f);
                    b.add(new Image(it.uiIcon)).size(40).scaling(Scaling.fit);
                    final boolean[] suppress = {false};
                    b.update(() -> {
                        Recipe cur = getRecipe();
                        boolean on = (cur != null && cur.outputItem != null && cur.outputItem.any()
                                && cur.outputItem.first() != null && cur.outputItem.first().item == it);
                        suppress[0] = true;
                        try { b.setChecked(on); } finally { suppress[0] = false; }
                    });
                    b.clicked(() -> {
                        if (suppress[0]) return;
                        configure(it);
                    });
                    grid.add(b).size(48);
                    if ((i + 1) % cols == 0 && i / cols < rows - 1) grid.row();
                }
                table.add(grid).row();
                table.row();
            }

            Seq<Liquid> liquids = new Seq<>();
            for (Recipe r : recipes) {
                if (r == null || r.outputLiquid == null || !r.outputLiquid.any() || r.outputLiquid.first() == null) continue;
                Liquid l = r.outputLiquid.first().liquid;
                if (l != null && l.unlockedNow() && !l.isBanned() && !liquids.contains(l)) liquids.add(l);
            }
            if (liquids.any()) {
                table.add(Core.bundle.get("bar.liquid") + " " + Core.bundle.get("stat.output"))
                        .color(Color.lightGray).left().padBottom(4f).padTop(10f).row();
                Table grid = new Table();
                int rows = Math.max(1, (liquids.size + selectionColumns - 1) / selectionColumns);
                int cols = selectionColumns;
                for (int i = 0; i < liquids.size; i++) {
                    Liquid l = liquids.get(i);
                    Button b = new Button(Styles.squareTogglei);
                    b.margin(4f);
                    b.add(new Image(l.uiIcon)).size(40).scaling(Scaling.fit).color(l.color);
                    final boolean[] suppress = {false};
                    b.update(() -> {
                        Recipe cur = getRecipe();
                        boolean on = (cur != null && cur.outputLiquid != null && cur.outputLiquid.any()
                                && cur.outputLiquid.first() != null && cur.outputLiquid.first().liquid == l);
                        suppress[0] = true;
                        try { b.setChecked(on); } finally { suppress[0] = false; }
                    });
                    b.clicked(() -> {
                        if (suppress[0]) return;
                        configure(l);
                    });
                    grid.add(b).size(48);
                    if ((i + 1) % cols == 0 && i / cols < rows - 1) grid.row();
                }
                table.add(grid).row();
            }

            if (!items.any() && !liquids.any()) {
                table.table(Styles.black3, t -> t.add("@none").color(Color.lightGray));
            }
        }

        // ==========================================================
        // 切配方清仓（防呆版：绝对不会 while 死循环 → 死机）
        // ==========================================================
        public void dumpEverything() {
            try {
                int totalItems = Vars.content.items().size;
                for (int i = 0; i < totalItems; i++) {
                    Item it = Vars.content.item(i);
                    if (items == null || it == null) continue;
                    int maxAttempts = Math.max(1, Math.min(500, itemCapacity * 2));
                    for (int k = 0; k < maxAttempts && items.get(it) > 0; k++) {
                        int before = items.get(it);
                        boolean ok = dump(it);
                        int after  = items.get(it);
                        if (!ok || after >= before) break;
                    }
                }
            } catch (Throwable ignore) {}
            try {
                int totalLiquids = Vars.content.liquids().size;
                for (int i = 0; i < totalLiquids; i++) {
                    Liquid l = Vars.content.liquid(i);
                    if (liquids == null || l == null) continue;
                    int maxAttempts = Math.max(1, Math.min(500, itemCapacity * 2));
                    for (int k = 0; k < maxAttempts && liquids.get(l) > 0.001f; k++) {
                        float before = liquids.get(l);
                        dumpLiquid(l);
                        float after  = liquids.get(l);
                        if (after >= before - 0.0001f) break;
                    }
                }
            } catch (Throwable ignore) {}
        }

        // ==========================================================
        // 存档（ConfigurableBlock 同款：version=1，父类 GenericCrafterBuild 自己写 progress，
        // 我们自己只写 recipeIndex）
        // ==========================================================
        @Override
        public byte version() { return 1; }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.s(recipeIndex);
        }
        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            recipeIndex = read.s();
        }
    }
}
