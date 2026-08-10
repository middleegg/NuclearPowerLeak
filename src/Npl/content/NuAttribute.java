package Npl.content;

import arc.struct.*;
import arc.util.*;
import mindustry.world.meta.*;
import mindustry.*;
import Npl.content.*;
import Npl.newSth.*;

public class NuAttribute{
    public static Attribute rubber,uranCrystal;
    static{
        register();
    }
    private static Attribute addIfAbsent(String name){
        if (Attribute.exists(name)){
            return Attribute.get(name);
        }else{
            return Attribute.add(name);
        }
    }
    private static void register(){
        rubber = addIfAbsent("rubber");
        uranCrystal = addIfAbsent("uranCrystal");
    }
}