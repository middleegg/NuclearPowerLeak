package Npl.content;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.Rand;
import arc.math.geom.Rect;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Table;
import arc.struct.IntMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.ai.UnitCommand;
import mindustry.ai.types.BuilderAI;
import mindustry.ai.types.DefenderAI;
import mindustry.ai.types.FlyingAI;
import mindustry.ai.types.MinerAI;
import mindustry.audio.SoundLoop;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.content.StatusEffects;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.entities.effect.*;
import mindustry.entities.effect.MultiEffect;
import mindustry.entities.part.DrawPart;
import mindustry.entities.part.HaloPart;
import mindustry.entities.part.RegionPart;
import mindustry.entities.part.ShapePart;
import mindustry.entities.pattern.*;
import mindustry.entities.units.WeaponMount;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.MultiPacker;
import mindustry.graphics.Pal;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.type.unit.*;
import mindustry.type.weapons.PointDefenseWeapon;
import mindustry.type.weapons.RepairBeamWeapon;
import mindustry.world.meta.BlockFlag;
import mindustry.world.meta.Env;
import Npl.content.*;
import Npl.newSth.Type.*;
import Npl.newSth.AI.*;
import Npl.newSth.*;
import static arc.graphics.g2d.Draw.*;
import static arc.graphics.g2d.Lines.*;
import static arc.math.Angles.*;
import static mindustry.Vars.*;

public class CalamityUnitType {
    public static UnitType
    dragon,dragonTail;
    public static void load(){
        FedUnitEntity.register(SegmentWormEntity.class, SegmentWormEntity::new);
        FedUnitEntity.register(SegmentUnitEntity.class, SegmentUnitEntity::new);
        dragon = new UnitType("dragon"){{
            constructor = SegmentUnitEntity::create;
            EntityMapping.nameMap.put(name, constructor);
            health = 200000;
            speed = 2f;
            researchCostMultiplier = 2.3f;
            armor = 90;
            hitSize = 100f;
            alwaysUnlocked = false;
            targetAir = true;
        }};
        dragonTail = new  UnitType("dragonTail"){{
            constructor = SegmentWormEntity::create;
            EntityMapping.nameMap.put(name, constructor);
            health = 100000;
            speed = 2f;
            researchCostMultiplier = 2.3f;
            armor = 90;
            hitSize = 100f;
            alwaysUnlocked = false;
            targetAir = true;
        }};
    }
}