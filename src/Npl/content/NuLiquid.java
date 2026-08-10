package Npl.content;

import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.content.Liquids.*;
import mindustry.type.Liquid;
import mindustry.type.*;
import arc.graphics.*;

public class NuLiquid {
    public static Liquid water,nuclearFluid,dirtySolution,liquidOxygen,strangeLiquid;
    public static void load(){
        nuclearFluid=new Liquid("nuclearFluid",Color.valueOf("00FF00")){{
            temperature=5f;
            lightColor=Color.valueOf("00FF0071");
        }};
        dirtySolution=new Liquid("dirtySolution",Color.valueOf("56118B")){{
            temperature=4.5f;
            viscosity=0.85f;
            flammability=3f;
            capPuddles=false;
            incinerable=true;
            blockReactive=true;
            lightColor=Color.valueOf("56118BFF");
            canStayOn.addAll(water,strangeLiquid,liquidOxygen);
        }};
        liquidOxygen=new Liquid("liquidOxygen",Color.valueOf("66AAFF")){{
            temperature=-10f;
            lightColor=Color.valueOf("99CCFFFF");
            viscosity=0.1f;
            heatCapacity=2.2f;
            boilPoint=1f;
            coolant=true;
        }};
        strangeLiquid=new Liquid("strangeLiquid",Color.valueOf("6FA5FF")){{
            temperature=-6f;
            lightColor=Color.valueOf("6FA5FFFF");
            viscosity=0.1f;
            heatCapacity=1f;
        }};
    }
}
