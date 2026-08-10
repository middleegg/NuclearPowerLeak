/*
 * NuBlocks_RecipeCrafter 示例 —— 只放进 Refactored_Code 供用户核实
 * ================================================================
 * 演示：在你自己的 Npl 项目里，怎么用 RecipeCrafter + Recipe 两种链式写法（推荐链式 / 仿 NH）
 * 注册一个叫 "test-recipe-crafter" 的方块，它有 4 个配方：
 *
 *   [1] 物品单输出  → 5 铜 + 3 铅 × 1 秒 → 2 钛
 *   [2] 物品多输出  → 10 石墨 + 1 水(3.0桶) × 2 秒 → 1 沙 + 2 铅
 *   [3] 混合多输出  → 3 煤 + 10 油(2.0桶) × 1.5s → 1 生铁(NuItems.bigIron) + 15 液氧(Liquids.cryofluid)
 *   [4] 纯液体       → 20 水(3.0桶) × 1.0s → 15 蒸汽(Liquids.slag 仿 NewHorizon，没有 steam 就用 slag 看颜色)
 *
 * ⚠️ 4 号配方用 Liquids.slag 代替真正的蒸汽，你有了自定义液体把它换掉就行。
 */
package Npl.content;

import Npl.newSth.Recipe;
import Npl.newSth.RecipeCrafter;
import arc.util.Log;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.type.Category;
import mindustry.world.Block;

import static mindustry.type.ItemStack.with;

public class NuBlocks_RecipeCrafter {
    public static Block testRecipeCrafter;

    public static void loadRecipeCrafters() {
        Log.info("[NuclearPowerLeak] 加载 RecipeCrafter 示例方块 ...");

        testRecipeCrafter = new RecipeCrafter("test-recipe-crafter") {{

            // ======================================================================
            // 配方 1：物品单输出（链式写法 · 推荐，最清晰）
            // ======================================================================
            recipes.add(new Recipe()
                .inItem(Items.copper, 5)
                .inItem(Items.lead, 3)
                .outItem(Items.titanium, 2)
                .time(60f)                  // 60 tick = 1s
            );

            // ======================================================================
            // 配方 2：物品多输出 + 液体输入（仿 NewHorizon 的 Object... 可变参数写法）
            // ======================================================================
            recipes.add(new Recipe(
                Items.graphite, 10,        // 石墨 × 10
                Liquids.water, 3.0f        // 水 3.0 桶（注意液体要写 f，虽然我们在 Recipe 里自动转了 Integer）
            )
                .outItem(Items.sand, 1)
                .outItem(Items.lead, 2)
                .time(120f)                // 120 tick = 2s
            );

            // ======================================================================
            // 配方 3：物品 + 液体 → 物品 + 液体（混合多输出）—— 链式 + 自定义物品 NuItems
            // ======================================================================
            recipes.add(new Recipe()
                .inItem(Items.coal, 3)
                .inLiquid(Liquids.oil, 2.0f)
                .outItem(NuItems.bigIron, 1)           // 自定义「生铁」
                .outLiquid(Liquids.cryofluid, 15f)     // 液氧/冷冻液 15 桶（你也可以换自己的 Liquid）
                .time(90f)                  // 1.5s
            );

            // ======================================================================
            // 配方 4：纯液体（水 → 蒸汽，用 slag 代替看颜色）
            // ======================================================================
            recipes.add(new Recipe()
                .inLiquid(Liquids.water, 3.0f)
                .outLiquid(Liquids.slag, 15f)
                .time(60f)
            );

            // ======================================================================
            // 方块基础属性
            // ======================================================================
            requirements(Category.crafting, with(
                NuItems.bigIron, 30,
                Items.titanium, 20,
                Items.silicon, 30
            ));

            size = 2;
            health = 260;
            itemCapacity = 50;
            liquidCapacity = 40f;
            consumesPower = true;
            consumePower(1.20f);            // 1.2 power/tick 耗电

            // 想播放合成音就加这两行（不加也不会崩，FINAL FIX 里有空指针保护）
            // createSound = Sounds.grind;
            // createSoundVolume = 0.06f;

            // 想播放合成粒子就加这行（不加也不会崩）
            // craftEffect = Fx.creep;
        }};
    }
}
