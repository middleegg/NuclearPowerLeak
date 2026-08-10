package Npl.content;

import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.graphics.MultiPacker.*;
import mindustry.world.meta.*;
import mindustry.type.*;
import mindustry.game.EventType.*;
import Npl.content.*;
import Npl.newSth.*;

public class NuStatus{
    public static OriStatus chaos,paralysis,lack,radiation;
    public static void load(){
        chaos = new OriStatus("chaos"){{
            color = NuColor.ChaosColor;
            damageMultiplier = 1.35f;
            reloadMultiplier = 1.35f;
            healthMultiplier = 0.65f;
            speedMultiplier = 0.65f;
            init(() -> {
                affinity(StatusEffects.melting, (unit, result, time) -> result.set(StatusEffects.melting, result.time + time));
                affinity(StatusEffects.freezing, (unit, result, time) -> result.set(StatusEffects.freezing, result.time + time));
            });
        }};
        paralysis = new OriStatus("paralysis"){{
            color = NuColor.ParalysisColor;
            speedMultiplier = 0.1f;
            healthMultiplier = 0.85f;
            damageMultiplier = 0.85f;
            transitionDamage = 128;
            show = true;
            init(() -> {
                affinity(lack, (unit, result, time) -> {
                    unit.damage(transitionDamage);
                });
                opposite(chaos, StatusEffects.melting,StatusEffects.burning,StatusEffects.unmoving);
            });
        }};
        lack = new OriStatus("lack"){{
            color = NuColor.LackColor;
            injuredMultiplier = 1.5f;
            armorPercent = -1f;
            show = true;
        }};
        radiation = new OriStatus("radiation"){{
            color = NuColor.RadiationColor;
            damage = 1.5f;
            healthMultiplier = 0.75f;
            injuredMultiplier = 2f;
            show = true;
        }};
    }
}