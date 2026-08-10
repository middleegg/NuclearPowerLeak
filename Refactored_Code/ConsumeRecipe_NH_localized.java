
package Npl.newSth;

import Npl.content.Recipe;
import arc.func.Func;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.type.ItemStack;
import mindustry.type.LiquidStack;
import mindustry.ui.ReqImage;
import mindustry.world.Block;
import mindustry.world.consumers.Consume;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatValues;

/**
 * 完全照抄 NewHorizonMod-2.1.2 newhorizon.expand.block.consumer.ConsumeRecipe
 * 本地化改动：
 *   1) 包名改为 Npl.newSth
 *   2) Recipe import 改为 Npl.content.Recipe（原本 NH 自己的 type.Recipe）
 *   3) 去掉所有 PayloadStack / UnlockableContent / getPayloads() 分支（Mindustry159.6 无 Payload 体系）
 *   4) build() 里 Seq.toArray(ItemStack.class) 改成手动 new ItemStack[size]（Mindustry159.6 的 Seq 不一定有泛型 toArray，手动最稳）
 *   5) display(Stats) 方法：手动实现 stats.add 的显示——NH 没写 display，我们加上，防止方块属性面板空
 *   6) build() 里 StatValues.stack(item, amount) 用 ReqImage（NH 原写法保留）
 *   其余 6 个 @Override 方法签名与 NH 原版一字不动照抄（apply/update/trigger/efficiency/build）
 */
public class ConsumeRecipe extends Consume {

    public final @Nullable Func<Building, Recipe> recipe;
    public final Func<Building, Recipe> display;

    @SuppressWarnings("unchecked")
    public <T extends Building> ConsumeRecipe(Func<T, Recipe> recipe, Func<T, Recipe> display) {
        this.recipe  = (Func<Building, Recipe>) recipe;
        this.display = (Func<Building, Recipe>) display;
    }

    @SuppressWarnings("unchecked")
    public <T extends Building> ConsumeRecipe(Func<T, Recipe> recipe) {
        this.recipe  = (Func<Building, Recipe>) recipe;
        this.display = (Func<Building, Recipe>) recipe;
    }

    // ==========================================================
    // ↓↓↓ 完全照抄 NH ConsumeRecipe.apply()，去掉 acceptsPayload（我们没有 Payload）
    // ==========================================================
    @Override
    public void apply(Block block) {
        block.hasItems = true;
        block.hasLiquids = true;
        block.acceptsItems = true;
    }

    // ==========================================================
    // ↓↓↓ 完全照抄 NH ConsumeRecipe.update()（每 tick 扣输入液体流式）
    // ==========================================================
    @Override
    public void update(Building build) {
        if (recipe.get(build) == null) return;
        for (LiquidStack stack : recipe.get(build).inputLiquid) {
            // NH 原版：liquids.remove(liquid, amount * edelta * multiplier)
            build.liquids.remove(stack.liquid, stack.amount * build.edelta() * multiplier.get(build));
        }
    }

    // ==========================================================
    // ↓↓↓ 完全照抄 NH ConsumeRecipe.trigger()（生产完成一刻，扣物品/Payload，一次性）
    // 改动：删除 PayloadStack 分支
    // ==========================================================
    @Override
    public void trigger(Building build) {
        if (recipe.get(build) == null) return;
        for (ItemStack stack : recipe.get(build).inputItem) {
            build.items.remove(stack.item, Math.round(stack.amount * multiplier.get(build)));
        }
    }

    // ==========================================================
    // ↓↓↓ 完全照抄 NH ConsumeRecipe.efficiency()
    // 改动：删除 PayloadStack contains 的 min 判断
    // ==========================================================
    @Override
    public float efficiency(Building build) {
        float ed = build.edelta() * build.efficiencyScale();
        if (ed <= 0.00000001f) return 0f;
        if (recipe.get(build) == null) return 0f;
        float min = 1f;

        // —— 物品必须齐（效率 0 或 1）——
        for (ItemStack stack : recipe.get(build).inputItem) {
            if (!build.items.has(stack.item, Math.round(stack.amount * multiplier.get(build)))) {
                min = 0f;
                break;
            }
        }
        // —— 液体按比例裁剪（0~1 之间）——
        if (min > 0f) {
            for (LiquidStack stack : recipe.get(build).inputLiquid) {
                min = Math.min(
                    build.liquids.get(stack.liquid) / (stack.amount * ed * multiplier.get(build)),
                    min
                );
            }
        }
        return min;
    }

    // ==========================================================
    // ↓↓↓ 完全照抄 NH ConsumeRecipe.build(Building,Table)
    // 改动：删除 PayloadStack 分支；toArray 改成手动 copy；ReqImage 用 StatValues.stack
    // ==========================================================
    @Override
    public void build(Building build, Table table) {
        if (display.get(build) == null) return;
        table.update(() -> {
            table.clear();
            table.left();

            // 手动 copy Seq → T[]（Mindustry159.6 不依赖 Seq.toArray(T) 新方法，最稳）
            Recipe r = display.get(build);
            ItemStack[] currentItem = new ItemStack[r.inputItem.size];
            for (int k = 0; k < currentItem.length; k++) currentItem[k] = r.inputItem.get(k);
            LiquidStack[] currentLiquid = new LiquidStack[r.inputLiquid.size];
            for (int k = 0; k < currentLiquid.length; k++) currentLiquid[k] = r.inputLiquid.get(k);

            table.table(cont -> {
                int i = 0;
                if (currentItem != null) {
                    for (ItemStack stack : currentItem) {
                        cont.add(new ReqImage(
                                StatValues.stack(stack.item, Math.round(stack.amount * multiplier.get(build))),
                                () -> build.items != null
                                        && build.items.has(stack.item, Math.round(stack.amount * multiplier.get(build)))
                        )).padRight(8).left();
                        if (++i % 4 == 0) cont.row();
                    }
                }
                if (currentLiquid != null) {
                    for (LiquidStack stack : currentLiquid) {
                        cont.add(new ReqImage(stack.liquid.uiIcon,
                                () -> build.liquids != null && build.liquids.get(stack.liquid) > 0
                        )).size(Vars.iconMed).padRight(8);
                        if (++i % 4 == 0) cont.row();
                    }
                }
            });
        });
    }

    // ==========================================================
    // ↓↓↓ NH ConsumeRecipe 没写 display(Stats)，我们补上（防止方块详情面板空）
    // 逻辑和我们之前写的 ConsumeCon 一样：拿第一个非空配方当默认展示
    // ==========================================================
    @Override
    public void display(Stats stats) {
        // display(Stats)：这里拿不到 Building，所以只能展示"默认配方"=调用方传的 recipe/display 是 getter，
        //   我们这里用 null 安全回退：让 Stats 默认的 Stat.input/output 不崩
        // （真实的配方列表会在 RecipeGenericCrafter.setStats() 里单独画自己的表格，不依赖这里）
        Recipe def = null;
        try {
            if (recipe != null) def = recipe.get(null);
        } catch (Throwable ignore) {}
        if (def == null) return;

        // 输入物品：Seq → ItemStack[]
        if (def.inputItem != null && def.inputItem.any()) {
            ItemStack[] arr = new ItemStack[def.inputItem.size];
            for (int k = 0; k < arr.length; k++) arr[k] = def.inputItem.get(k);
            if (stats.timePeriod < 0) stats.add(Stat.input,  StatValues.items(arr));
            else                       stats.add(Stat.input,  StatValues.items(stats.timePeriod, arr));
        }
        // 输入液体：单独画 stat
        if (def.inputLiquid != null && def.inputLiquid.any()) {
            for (LiquidStack s : def.inputLiquid) {
                if (s == null || s.liquid == null) continue;
                stats.add(Stat.input, StatValues.liquid(s.liquid, s.amount * 60f, true));
            }
        }
    }
}
