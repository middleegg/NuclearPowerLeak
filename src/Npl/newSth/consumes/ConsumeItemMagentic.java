package Npl.newSth.consumes;

import mindustry.type.*;
import mindustry.world.consumers.*;
import Npl.newSth.*;
import Npl.content.*;

public class ConsumeItemMagentic extends ConsumeItemEfficiency{
    public float minMagentic;

    public ConsumeItemMagentic(float minMagentic){
        this.minMagentic = minMagentic;
        filter = item -> item instanceof NewItemsType ni && ni.magentic >= this.minMagentic;
    }

    public ConsumeItemMagentic(){
        this(0.1f);
    }

    @Override
    public float itemEfficiencyMultiplier(Item item){
        if (item instanceof NewItemsType ni) return ni.magentic;
        return 0f;
    }

}
