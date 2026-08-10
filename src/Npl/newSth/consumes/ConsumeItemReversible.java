package Npl.newSth.consumes;

import mindustry.type.*;
import mindustry.world.consumers.*;
import Npl.newSth.*;
import Npl.content.*;

public class ConsumeItemReversible extends ConsumeItemEfficiency{
    public float minReversible;

    public ConsumeItemReversible(float minReversible){
        this.minReversible = minReversible;
        filter = item -> NewItemsType.reversible >= this.minReversible;
    }

    public ConsumeItemReversible(){
        this(0.4f);
    }

    @Override
    public float itemEfficiencyMultiplier(Item item){
        return NewItemsType.reversible;
    }
}
