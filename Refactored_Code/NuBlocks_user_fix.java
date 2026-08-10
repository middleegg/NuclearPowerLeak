/*
 * ⚠️ 修复版 - 仅放入 Refactored_Code 文件夹供用户核实
 * ⚠️ 不影响原始 src 目录下任何文件
 * ==============================================================
 * 修复点：
 *   1) 修正原 NuBlocks 中使用的 "modeCount" 字段 —— ConfigurableBlock 没有该字段，
 *      导致编译期字段注入失败（原代码是把 recipes 写在了 Block 初始化块里，
 *      但 plans（Seq<Plan>）为空 → 实际运行不显示 UI、不做配方、不生产
 *
 *   2) 在每个配方创建完成后，手动构造一个 Plan 并 add() 到 plans：
 *         Plan p = new Plan(ItemStack[]{...}, craftTime, ItemStack[]{reqs...});
 *         plans.add(p);
 *      这里**严格使用用户新版 ConfigurableBlock 的 Plan 三参构造：**
 *         new Plan(ItemStack[] outItem, float time, ItemStack[] requirements)
 *
 *   3) 同时给出：单输出 / 多输出两种写法的演示方块，让用户验证修复是否成功。
 */
package Npl.content;

import static mindustry.Vars.*;
import static mindustry.type.ItemStack.*;

import Npl.newSth.ConfigurableBlock;
import Npl.newSth.ConfigurableBlock.Plan;   // ✅ 使用用户自己 Plan 的包路径
import Npl.content.NuItems;
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
    public static Block multiOutTest;   // ✅ 新增：多输出测试方块，便于你直接验证 outItem[] 多输出也能正常上限/扣料/上传送带

    public static void load() {
        Log.info("NuBlocks.load() called!");

        // ================================================================
        // ① TestBlock —— 原版 3 个配方，修复为正确填充 plans
        // ================================================================
        TestBlock = new ConfigurableBlock("test-block") {{

            /* ========================================================
             * ⚠️ 原代码这里写 modeCount=3，字段不存在所以是"伪赋值"。
             *    下面直接写 3 个 Plan 加到 plans，100% 等价。
             * ====================================================== */

            // 配方 0：5 铜 → 2 铅
            plans.add(new Plan(
                new ItemStack[]{ new ItemStack(Items.lead, 2) },    // outItem[] = [铅×2]
                60f,                                                // time
                with(Items.copper, 5)                               // requirements = [铜×5]
            ));

            // 配方 1：3 铅 → 1 钛
            plans.add(new Plan(
                new ItemStack[]{ new ItemStack(Items.titanium, 1) },
                60f,
                with(Items.lead, 3)
            ));

            // 配方 2：3 铜 + 2 铅 → 1 生铁
            plans.add(new Plan(
                new ItemStack[]{ new ItemStack(NuItems.bigIron, 1) },
                60f,
                with(Items.copper, 3, Items.lead, 2)
            ));

            // 方块成本 + 尺寸
            requirements(Category.crafting, with(NuItems.bigIron, 10));
            size = 2;
        }};

        // ================================================================
        // ② redenmore（传统 GenericCrafter 原样保留，未改动）
        // ================================================================
        redenmore = new GenericCrafter("redenmore") {{
            requirements(Category.crafting, with(NuItems.bigIron, 40));
            outputItem = new ItemStack(NuItems.magent, 1);
            craftTime = 60f;
            hasItems = hasPower = true;
            ambientSound = Sounds.loopGrind;
            ambientSoundVolume = 0.025f;
            consumeItems(ItemStack.with(NuItems.bigIron, 3));
            consumePower(0.50f);
            size = 2;
        }};

        // ================================================================
        // ③ multiOutTest —— ✅ 多输出 outItem[] 测试方块（验证三个 Bug 是否全修）
        //    配方：2 生铁 + 1 石墨
        //       → 1 磁铁 + 2 橡胶 + 3 气浮石  （三物品同时输出！）
        //    放三输出的原因：方便直接看到"是否都有容量限制"、"是否都进入传送带"
        // ================================================================
        multiOutTest = new ConfigurableBlock("multi-out-test") {{

            // 只做 1 个配方，直接多物品 outItem[]
            plans.add(new Plan(
                // outItem[] —— 三输出
                new ItemStack[]{
                    new ItemStack(NuItems.magent, 1),   // 磁铁 ×1
                    new ItemStack(NuItems.rubber, 2),   // 橡胶 ×2
                    new ItemStack(NuItems.pumice, 3)    // 气浮石 ×3
                },
                120f,     // 2 秒
                // requirements
                with(
                    NuItems.bigIron, 2,    // 生铁 ×2
                    Items.graphite, 1      // 石墨 ×1
                )
            ));

            requirements(Category.crafting, with(
                NuItems.bigIron, 30,
                NuItems.magent, 10
            ));
            size = 2;
            health = 200;
            itemCapacity = 50;    // 三输出 50 够用
        }};
    }
}
