package Npl.content;

import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.content.Liquids.*;
import mindustry.type.Liquid;
import mindustry.type.*;
import arc.graphics.*;

public class NuLiquid {
    public static Liquid water,nuclearFluid,dirtySolution,liquidOxygen,strangeLiquid,
    prismEnergyLiquid,divineTears;
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
            heatCapacity=1.2f;
            boilPoint=1f;
            coolant=true;
        }};
        strangeLiquid=new Liquid("strangeLiquid",Color.valueOf("6FA5FF")){{
            temperature=-6f;
            lightColor=Color.valueOf("6FA5FFFF");
            viscosity=0.1f;
            heatCapacity=0.5f;
        }};
        prismEnergyLiquid = new Liquid("prismEnergyLiquid",Color.valueOf("F0FFFD")){{
            temperature=-3f;
            lightColor=Color.valueOf("F0FFFDB0");
            heatCapacity=1.6f;
            boilPoint=3f;
            viscosity=0.5f;
        }};
        divineTears = new Liquid("divineTears",Color.valueOf("F8FFB0")){{
            temperature=2f;
            lightColor=Color.valueOf("F8FFB0B0");
            heatCapacity=2.3f;
            boilPoint=12f;
            viscosity=1f;
        }};
    }
}
