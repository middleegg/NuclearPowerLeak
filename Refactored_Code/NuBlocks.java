/*
 * ⚠️ 重构改版 - 仅放入 Refactored_Code 文件夹供用户核实
 * ⚠️ 不影响原始 src 目录下任何文件
 * ========================================================
 * 修复：
 *   1) 原 test-block 声明了 recipes[] 和 modeCount，
 *      但未把 recipes 填充到 ConfigurableBlock.plans 中 → 导致运行时 plans 为空，UI 不显示、不生产
 *      修复：初始化末尾调用 setRecipes(recipes) 走 RecipeBridge 自动转换
 *
 *   2) 新增演示：advanced-block，演示新版 Plan 的"多物品输出 + 液体输入输出"
 *
 *   3) redenmore 保持不变，保持兼容
 */
package Npl.content;

import static mindustry.Vars.*;
import static mindustry.type.ItemStack.*;

import Npl.newSth.ConfigurableBlock;
import Npl.content.NuItems;
import Npl.content.NuLiquid;  // ✅ 新增：液体
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.effect.*;
import mindustry.entities.part.DrawPart.*;
import mindustry.entities.part.*;
import mindustry.entities.pattern.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.type.unit.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.campaign.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.blocks.legacy.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.blocks.logic.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.sandbox.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.blocks.units.*;
import mindustry.world.consumers.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;
import mindustry.ui.dialogs.*;
import arc.util.Log;

import static mindustry.Vars.*;
import static mindustry.type.ItemStack.*;


public class NuBlocks {
    public static Block TestBlock, redenmore;
    public static Block advancedBlock;   // ✅ 改版：新增演示方块

    public static void load() {
        Log.info("NuBlocks.load() called!");

        // ================================================================
        // TestBlock - 修复：recipes[] 填充到 plans 里
        // ================================================================
        TestBlock = new ConfigurableBlock("test-block") {{
            // 这里 modeCount = 3;   // ← 原代码写了但父类没这个字段（原问题点
            // 把所有 Recipe 先建一个临时数组，再统一注入：
            Recipe[] recipes = new Recipe[3];

            recipes[0] = new Recipe(Items.copper, 5)
                    .output(Items.lead, 2)
                    .craftTime(60f);

            recipes[1] = new Recipe(Items.lead, 3)
                    .output(Items.titanium, 1)
                    .craftTime(60f);

            recipes[2] = new Recipe(Items.copper, 3, Items.lead, 2)
                    .output(NuItems.bigIron, 1)
                    .craftTime(60f);

            // ✅ 修复：将 Recipe[] 批量转成 plans
            setRecipes(recipes);

            requirements(Category.crafting, with(NuItems.bigIron, 10));
            size = 2;
        }};

        // ================================================================
        // redenmore - 保留原版单配方（保持兼容，未改动
        // ================================================================
        redenmore = new GenericCrafter("redenmore") {{
            requirements(Category.crafting, with(NuItems.bigIron,40));
            outputItem = new ItemStack(NuItems.magent,1);
            craftTime =60f;
            hasItems=hasPower=true;
            ambientSound=Sounds.loopGrind;
            ambientSoundVolume=0.025f;
            consumeItems(ItemStack.with(NuItems.bigIron,3));
            consumePower(0.50f);
            size=2;
        }};

        // ================================================================
        // advancedBlock - ✅ 新版演示：多物品输出 + 液体输入输出
        //
        // 模式 0：  生铁×2 + 液氧×2.0u  →  磁铁×1 + 奇液×1.0u
        // 模式 1：  原铀×1 + 水×3.0u    →  铀晶×2 + 脏溶液×0.5u + 瓶装磁暴×1  （三输出！）
        // ================================================================
        advancedBlock = new ConfigurableBlock("advanced-block") {{

            // 直接使用 addRecipe(Recipe) 链式添加：

            // —— 配方 0：液氧冷却下冶炼铁
            addRecipe(new Recipe(
                    NuItems.bigIron, 2, NuLiquid.liquidOxygen, 2.0f  // 注意：液体输入用 Float 数量
            )
                .output(NuItems.magent, 1)
                .output(NuLiquid.strangeLiquid, 1.0f)
                .craftTime(120f)   // 2 秒
                .priority(0));

            // —— 配方 1：核反应（三输出！）
            addRecipe(new Recipe(
                    NuItems.oriUranium, 1,
                    Liquids.water,        3.0f
            )
                .output(NuItems.uranCrystal, 2)
                .output(NuLiquid.dirtySolution, 0.5f)
                .output(NuItems.bottledMagenticStorm, 1)   // ✅ 多物品 + 液体混合
                .craftTime(240f)   // 4 秒
                .priority(10)       // 高优先级
            );

            // 成本
            requirements(Category.crafting, with(
                NuItems.bigIron, 80,
                NuItems.magent,    20
            ));
            size = 3;      // 3×3，更高级
            health = 320;
            consumesPower = true;
            consumePower(1.2f);  // 耗电翻倍
        }};

    }
}
