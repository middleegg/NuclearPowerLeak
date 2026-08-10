package Npl.newSth.consumes;

import mindustry.type.*;
import mindustry.world.consumers.*;
import Npl.newSth.*;
import Npl.content.*;

public class ConsumeItemStability extends ConsumeItemEfficiency{
    public float minStability;

    public ConsumeItemStability(float minStability){
        this.minStability = minStability;
        filter = item -> NewItemsType.stability >= this.minStability;
    }

    public ConsumeItemStability(){
        this(0.4f);
    }

    @Override
    public float itemEfficiencyMultiplier(Item item){
        return NewItemsType.stability;
    }

}
