
package Npl.content;

import Npl.newSth.ConsumeRecipe;
import Npl.newSth.RecipeGenericCrafter;
import arc.graphics.Color;
import mindustry.content.Items;
import mindustry.content.Liquids;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.world.Block;

import static mindustry.content.Items.copper;
import static mindustry.type.ItemStack.with;

/**
 * 本地化 NuBlocks 注册示例（RecipeGenericCrafter 版）
 * 对应原来的 NuBlocks.java 注册逻辑：
 *   1. 只保留 4 个基础配方（纯物品 / 物品+液体 / 大量物品 / 纯液体）
 *   2. 保持 category = Category.crafting（用户已改的分类，不动）
 *   3. 不引用 PayloadStack（已在 Recipe 里移除）
 *   4. requirements 仍然 copper+lead 双铜合金，和你 ConfigurableBlock 同款
 *
 * 【怎么合入真实 NuBlocks.java】
 *   把这个文件里 testRecipeCrafter 整段初始化块 copy 到你 NuBlocks.java 里，
 *   顶部加 2 个 import：
 *       import Npl.newSth.ConsumeRecipe;
 *       import Npl.newSth.RecipeGenericCrafter;
 */
public class NuBlocks_NH_localized_demo {

    public static Block testRecipeCrafter;

    public static void load() {
        testRecipeCrafter = new RecipeGenericCrafter("test-recipe-crafter") {{
            // 本地化：category=crafting，用户已明确要求（原本 production）
            requirements(Category.crafting, with(Items.copper, 120, Items.lead, 80, Items.silicon, 40));

            health = 420;
            size = 2;
            craftTime = 60f;
            itemCapacity = 40;
            liquidCapacity = 30f;
            hasPower = true;
            hasLiquids = true;
            ambientSound = Sounds.loopGrind;  // 和 ConfigurableBlock 同款 grind 音效
            ambientSoundVolume = 0.08f;
            solid = true;
            update = true;
            rotate = false;

            // 注册 4 个测试配方（覆盖全场景：纯物品 / 物品+液体 / 大量物品 / 纯液体）
            recipes.add(new Recipe() {{
                // 配方 1：铜5 + 铅3 → 钛2（纯物品多输入多输出）
                inItem(with(Items.copper, 5, Items.lead, 3));
                outItem(with(Items.titanium, 2));
                craftTime = 90f;
            }});
            recipes.add(new Recipe() {{
                // 配方 2：煤2 + 水1.5 → 硅3 + 渣1（物品+液体多输出）
                inItem(with(Items.coal, 2));
                inLiquid(with(Liquids.water, 1.5f));  // 注意：如果你希望 inLiquid 也接受 with()，需要给 Recipe 补 LiquidStack with
                outItem(with(Items.silicon, 3, Items.slag, 1));
                craftTime = 70f;
            }});
            recipes.add(new Recipe() {{
                // 配方 3：钍4 → 相位织物3（大量物品单输出）
                inItem(with(Items.thorium, 4));
                outItem(with(Items.phaseFabric, 3));
                craftTime = 180f;
            }});
            recipes.add(new Recipe() {{
                // 配方 4：水3 + 冷0.5 → 渣液1.5（纯液体）
                inLiquid(Liquids.water, 3f);
                inLiquid(Liquids.cryofluid, 0.5f);
                outLiquid(Liquids.slag, 1.5f);
                craftTime = 50f;
            }});

            // 默认配方索引：0
            // （recipeIndex 是 Building 级字段，这里不用写）

            consumePower(1.8f);   // 轻度耗电（和你 ConfigurableBlock 差不多）
            // 如果你要选默认配方：别在这里改，Building 初始化时自动会找第一个可做的

            // 注意：上面构造函数已经注册了 ConsumeRecipe，所以这里不要重复 consume(new ConsumeRecipe(...))
        }};
    }
}
