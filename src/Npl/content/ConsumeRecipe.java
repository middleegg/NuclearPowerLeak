package Npl.content;

import arc.func.Boolf;
import mindustry.gen.Building;
import mindustry.world.consumers.Consume;

public class ConsumeRecipe extends Consume {
    public final Boolf<Building> valid;
    public final Boolf<Building> display;

    public ConsumeRecipe(Boolf<Building> valid, Boolf<Building> display) {
        this.valid = valid;
        this.display = display;
    }

    public ConsumeRecipe(Boolf<Building> valid) {
        this(valid, valid);
    }

    // 不覆盖任何父类方法，直接用字段
    public boolean isValid(Building build) {
        return valid.get(build);
    }
}