package Npl.content;

import static mindustry.Vars.*;
import static mindustry.type.ItemStack.*;
import Npl.newSth.*;
import Npl.content.NuItems;
import Npl.content.*;
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import mindustry.*;
import mindustry.world.consumers.*;
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
import Npl.content.*;

import static mindustry.Vars.*;
import static mindustry.type.ItemStack.*;

public class envBlocks {
    public static Block
    //environmentBlocks
    desert,desertWall,Yellowstone,YellowstoneWall,Brownstone,BrownstoneWall,rubberTree,rubberFloor,
    hollyFloor,holyWall,glossy,darkness,altar,hollyTree,lossLiquid,huiye,harborWater,
    //ores
    bigIronOre,coalHill,sulfurFragOre,frailPolyesterOre,pumiceMegaOre,uranCrystalWall,
    //WallOres
    bigIronWores,coalHillOre,sulfurFragwores,frailPolyesterWores,pumiceMegaWores,uranCrystalWallOres;
    public static void load(){
        desert = new Floor("desert"){{
            itemDrop = Items.sand;
            playerUnmineable = true;
            speedMultiplier = 0.85f;
            useColor = true;
            variants = 3;
            attributes.set(Attribute.water, -0.3f);
        }};
        desertWall= new StaticWall("desertWall"){{
            attributes.set(Attribute.sand, 1.25f);
            variants = 2;
            useColor = true;
        }};
        Yellowstone = new Floor("yellowStone"){{
            playerUnmineable = true;
            speedMultiplier = 0.95f;
            useColor = true;
            variants = 3;
            attributes.set(Attribute.water, -0.25f);
        }};
        YellowstoneWall= new StaticWall("yellowStoneWall"){{
            attributes.set(Attribute.sand, 1f);
            variants = 2;
            useColor = true;
        }};
        Brownstone = new Floor("brownStone"){{
            playerUnmineable = true;
            speedMultiplier = 0.85f;
            useColor = true;
            variants = 3;
            attributes.set(Attribute.water, 0.1f);
        }};
        BrownstoneWall= new StaticWall("brownStoneWall"){{
            attributes.set(Attribute.sand, 1f);
            attributes.set(NuAttribute.rubber, 0.3f);
            variants = 2;
            useColor = true;
        }};
        rubberFloor = new Floor("rubberFloor"){{
            playerUnmineable = true;
            useColor = true;
            variants = 2;
            attributes.set(Attribute.water, 0.2f);
        }};
        rubberTree = new StaticWall("rubberTree"){{
            attributes.set(NuAttribute.rubber, 0.3f);
            variants = 2;
            useColor = true;
        }};
    }

}