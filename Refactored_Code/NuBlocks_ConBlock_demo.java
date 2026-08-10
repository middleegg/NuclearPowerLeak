package Npl.content;

import Npl.newSth.ConBlock;
import Npl.newSth.ConsumeCon;
import arc.struct.*;
import mindustry.content.*;
import mindustry.type.*;

import static mindustry.type.ItemStack.*;


/**
 * 示范：注册一个 ConBlock 方块（con-test），配 4 个覆盖各种输入输出场景的配方。
 * 放 Refactored_Code 里，你核实后把内容合入真实的 NuBlocks.java。
 */
public class NuBlocks_ConBlock_demo {

    public static ConBlock conTest;

    public static void load() {

        conTest = new ConBlock("con-test") {{
            requirements(Category.production, with(
                    Items.copper, 200,
                    Items.lead,   150,
                    Items.silicon, 60,
                    Items.graphite, 50
            ));

            // —— 方块属性（可选，按需调）——
            size = 3;
            health = 320;
            craftTime = 60f;
            itemCapacity = 60;
            liquidCapacity = 60f;
            selectionRows = 2;
            selectionColumns = 4;

            // ============================================================
            // 4 个配方（完全使用你自己的 Recipe 类，inItem/outItem/inLiquid/outLiquid/inPayload/outPayload/craftTime）
            // ============================================================
            recipes.add(new Recipe() {{
                // 配方 1：纯物品（铜 + 铅 → 钛）
                inItem(with(Items.copper, 5, Items.lead, 3));
                outItem(with(Items.titanium, 2));
                craftTime = 90f;
            }});

            recipes.add(new Recipe() {{
                // 配方 2：物品 + 液体 → 多输出（煤 + 水 → 硅 + 炉渣）
                inItem(with(Items.coal, 2));
                inLiquid(LiquidStack.with(Liquids.water, 1.5f));
                outItem(with(Items.silicon, 3, Items.slag, 1));
                craftTime = 70f;
            }});

            recipes.add(new Recipe() {{
                // 配方 3：物品 + 载荷输入（1 个铜墙方块载荷）→ 物品输出（大量相位织物）
                inItem(with(Items.thorium, 4));
                // 载荷输入：铜墙（Blocks.copperWall，注意如果是方块需要给其加单位 payload）
                inPayload(Seq.with(new PayloadStack(Blocks.copperWall, 1)));
                outItem(with(Items.phaseFabric, 3));
                craftTime = 180f;
            }});

            recipes.add(new Recipe() {{
                // 配方 4：纯液体（水 + 冷 → 渣，液体同时有输入和输出）
                inLiquid(LiquidStack.with(Liquids.water, 3f, Liquids.cryofluid, 0.5f));
                outLiquid(LiquidStack.with(Liquids.slag, 1.5f));
                craftTime = 50f;
            }});

            // ============================================================
            // 可选：把"默认配方"包一层 ConsumeCon，交给 Mindustry 自带的 Consume 系统处理
            //   （优点：方块详情面板会显示标准"输入"条；电力/催化剂/冷却剂等能和 ConsumePower/ConsumeItemCoolant 叠加）
            //   （缺点：ConBlock.craft() 里已经手动扣过一遍输入，所以需要注意不要重复扣）
            //   👉 稳妥用法：ConsumeCon 仅作为"显示 + UI build"用，实际扣原料走 craft() 内手扣，最安全
            // ============================================================
            if (recipes.size > 0 && recipes.first() != null) {
                consume(new ConsumeCon(recipes.first()).update(false));   // update=false：Consume 系统不自动扣，只做展示/efficiency 参考
            }
            // 电力（原版 ConsumePower，独立于 ConsumeCon）
            consumePower(2f);
        }};
    }
}
