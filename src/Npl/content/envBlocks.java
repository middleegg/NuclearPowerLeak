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
    desert,desertWall,yellowStone,yellowStoneWall,brownStone,brownStoneWall,rubberTree,rubberFloor,
    hollyFloor,hollyWall,glossy,darkness,altar,hollyTree,lossLiquid,huiye,harborWater,uranCrystalWall,crystalCune,
    //ores
    bigIronOre,coalHill,sulfurFragOre,frailPolyesterOre,pumiceMegaOre,
    //WallOres
    bigIronWores,coalHillOre,sulfurFragWores,frailPolyesterWores,pumiceMegaWores;
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
            desert.asFloor().wall= this;
            variants = 2;
            useColor = true;
        }};
        yellowStone = new Floor("yellowStone"){{
            playerUnmineable = true;
            speedMultiplier = 0.95f;
            useColor = true;
            variants = 3;
            attributes.set(Attribute.water, -0.25f);
        }};
        yellowStoneWall= new StaticWall("yellowStoneWall"){{
            attributes.set(Attribute.sand, 1f);
            yellowStone.asFloor().wall= this;
            variants = 2;
            useColor = true;
        }};
        brownStone = new Floor("brownStone"){{
            playerUnmineable = true;
            speedMultiplier = 0.85f;
            useColor = true;
            variants = 3;
            attributes.set(Attribute.water, 0.1f);
        }};
        brownStoneWall= new StaticWall("brownStoneWall"){{
            attributes.set(Attribute.sand, 1f);
            attributes.set(NuAttribute.oriRubber, 0.3f);
            variants = 2;
            brownStone.asFloor().wall= this;
            useColor = true;
        }};
        rubberFloor = new Floor("rubberFloor"){{
            playerUnmineable = true;
            useColor = true;
            variants = 2;
            attributes.set(Attribute.water, 0.2f);
        }};
        rubberTree = new StaticWall("rubberTree"){{
            attributes.set(NuAttribute.oriRubber, 0.3f);
            variants = 2;
            rubberFloor.asFloor().wall= this;
            useColor = true;
        }};
        uranCrystalWall = new StaticWall("uranCrystalWall"){{
            attributes.set(NuAttribute.uranCrystal, 1f);
            variants = 1;
            useColor = true;
        }};
        crystalCune = new Floor("crystalCune"){{
            playerUnmineable = true;
            useColor = true;
            variants = 2;
        }};
        hollyFloor = new Floor("hollyFloor"){{
           useColor = true;
           variants = 1;
        }};
        hollyWall = new StaticWall("hollyWall"){{
            useColor = true;
            variants = 1;
        }};
        glossy = new SteamVent("glossy"){{
            parent = blendGroup = hollyFloor;
            attributes.set(Attribute.steam, 1f);
        }};
        darkness = new Floor("darkness"){{
            useColor = true;
            variants = 1;
        }};
        altar = new Floor("altar"){{
            useColor = true;
            variants = 1;
        }};
        hollyTree = new TreeBlock("hollyTree");
        lossLiquid = new Floor("lossLiquid"){{
            speedMultiplier = 0.7f;
            useColor = true;
            variants = 0;
            status = StatusEffects.wet;
            statusDuration = 90f;
            liquidDrop = Liquids.water;
            isLiquid = true;
            cacheLayer = CacheLayer.water;
            albedo = 0.9f;
            supportsOverlay = true;
        }};
        huiye = new Floor("huiye"){{
            useColor = true;
            variants = 0;
            speedMultiplier = 0.65f;
            status = NuStatus.radiation;
            statusDuration = 90f;
            liquidDrop = NuLiquid.dirtySolution;
            isLiquid = true;
            cacheLayer = CacheLayer.water;
            albedo = 0.9f;
            supportsOverlay = true;
        }};
        harborWater = new  Floor("harborWater"){{
            speedMultiplier = 1.4f;
            useColor = true;
            variants = 0;
            status = StatusEffects.wet;
            statusDuration = 90f;
            liquidDrop = Liquids.water;
            isLiquid = true;
            cacheLayer = CacheLayer.water;
            albedo = 0.9f;
            supportsOverlay = true;
        }};

        //ores

        bigIronOre = new OreBlock("bigIronOre",NuItems.bigIron){{
            oreDefault = true;
            oreThreshold = 0.81f;
            oreScale = 23.47619f;
            variants = 2;
        }};
        coalHill = new OreBlock("coalHill",NuItems.Tcoal){{
            useColor = true;
            oreDefault = true;
            oreThreshold = 0.846f;
            oreScale = 24.428572f;
            variants = 2;
        }};
        sulfurFragOre = new OreBlock("sulfurFragOre",NuItems.sulFurFrag){{
            oreDefault = true;
            oreThreshold = 0.864f;
            oreScale = 24.904762f;
            variants = 3;
        }};
        frailPolyesterOre = new OreBlock("frailPolyesterOre",NuItems.frailPolyester){{
            oreDefault = true;
            oreThreshold = 0.828f;
            oreScale = 23.952381f;
            variants = 3;
        }};
        pumiceMegaOre = new OreBlock("pumiceMegaOre",NuItems.pumice){{
            oreDefault = true;
            oreThreshold = 0.882f;
            oreScale = 25.380953f;
            variants = 2;
        }};

        //wallores

        bigIronWores = new OreBlock("bigIronWores", NuItems.bigIron){{
            wallOre = true;
            variants = 2;
        }};
        coalHillOre = new OreBlock("coalHillOre", NuItems.Tcoal){{
            wallOre = true;
            variants = 2;
        }};
        sulfurFragWores = new OreBlock("sulfurFragWores", NuItems.sulFurFrag){{
            wallOre = true;
            variants = 2;
        }};
        frailPolyesterWores = new OreBlock("frailPolyesterWores", NuItems.frailPolyester){{
            wallOre = true;
            variants = 3;
        }};
        pumiceMegaWores = new OreBlock("pumiceMegaWores", NuItems.pumice){{
            wallOre = true;
        }};
    }

}