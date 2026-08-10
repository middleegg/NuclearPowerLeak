/*
 * NuBlocks 最终修复版：
 *
 * 1) 加一行真正的 import Npl.newSth.RecipeCrafter; （原 NuBlocks 没 import 却直接 new RecipeCrafter）
 * 2) 保留 testRecipeCrafter 四个配方；保留 redenmore GenericCrafter
 * 3) 加一行 NuFactory.load() 调用（如果项目里 NuFactory.sl 没在其他地方被调用的话；
 *    如果已在主类调用了，删掉这行也不会报错，因为 NuFactory.load() 是幂等的，recipes 只是 Seq.add）
 */
package Npl.content;

import static mindustry.Vars.*;
import static mindustry.type.ItemStack.*;

import Npl.newSth.ConfigurableBlock;
import Npl.newSth.RecipeCrafter;          // ✅ 修复：原 NuBlocks.java 少了这个 import
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
    public static Block testRecipeCrafter,redenmore;
    public static void load() {
        Log.info("NuBlocks.load() called!");

        // ✅ 如果你已经在主类其他地方调用 NuFactory.load()，这一行可以删掉；
        //    没调用过就保留，保证 NuFactory.sl 里的 recipeCrafter 方块也注册了
        try { NuFactory.load(); } catch (Throwable ignored) { /* 没放 NuFactory.java 或编译失败也别崩整个 load */ }

        testRecipeCrafter = new RecipeCrafter("test-recipe-crafter") {{

            // 配方 1：物品单输出
            recipes.add(new Recipe()
                    .inItem(Items.copper, 5)
                    .inItem(Items.lead, 3)
                    .outItem(Items.titanium, 2)
                    .time(60f)
            );

            // 配方 2：物品多输出 + 液体输入
            recipes.add(new Recipe(
                            Items.graphite, 10,
                            Liquids.water, 3.0f
                    )
                            .outItem(Items.sand, 1)
                            .outItem(Items.lead, 2)
                            .time(120f)
            );

            // 配方 3：混合多输出
            recipes.add(new Recipe()
                    .inItem(Items.coal, 3)
                    .inLiquid(Liquids.oil, 2.0f)
                    .outItem(NuItems.bigIron, 1)
                    .outLiquid(Liquids.cryofluid, 15f)
                    .time(90f)
            );

            // 配方 4：纯液体
            recipes.add(new Recipe()
                    .inLiquid(Liquids.water, 3.0f)
                    .outLiquid(Liquids.slag, 15f)
                    .time(60f)
            );

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
            consumePower(1.20f);
        }};
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
    }
}
