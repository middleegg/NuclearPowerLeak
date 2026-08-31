package Npl.newSth;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.draw.DrawLiquidOutputs;

public class DrawModLiquidOutputs extends DrawLiquidOutputs{

    @Override
    public void load(Block block){
        GenericCrafter crafter = (GenericCrafter)block;
        if(crafter.outputLiquids == null) return;

        int hyphen = block.name.indexOf('-');
        String modPrefix = hyphen == -1 ? null : block.name.substring(0, hyphen + 1);

        liquidOutputRegions = new TextureRegion[2][crafter.outputLiquids.length];
        for(int i = 0; i < crafter.outputLiquids.length; i++){
            String liquidName = crafter.outputLiquids[i].liquid.name;
            if(modPrefix != null && liquidName.startsWith(modPrefix)){
                liquidName = liquidName.substring(modPrefix.length());
            }
            for(int j = 1; j <= 2; j++){
                liquidOutputRegions[j - 1][i] = Core.atlas.find(block.name + "-" + liquidName + "-output" + j);
            }
        }
    }
}
