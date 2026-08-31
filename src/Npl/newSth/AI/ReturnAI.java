package Npl.newSth.AI;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.ai.types.*;
import arc.struct.*;
import mindustry.async.*;
import mindustry.entities.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.entities.abilities.*;
import Npl.content.*;
import Npl.newSth.*;
import Npl.newSth.Type.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.meta.*;
import mindustry.entities.units.*;

import static mindustry.Vars.*;

/**
 * ReturnAi — 子单位自动寻找母体并钻入
 *
 * 联动 DevourAbility/EatAbility.returnChildTypes：
 * 母体的 Ability 中填入子单位类型名，ReturnAi 自动识别并钻入
 */
public class ReturnAI extends AIController {
    public Unit motherUnit;

    /** 寻找母体的宽限期（秒），期间找不到不自杀 */
    public float searchGrace = 5f;
    private float searchTimer = 0f;

    public static <T extends Ability> T getAbility(Unit unit, Class<T> cls) {
        for (Ability a : unit.abilities) {
            if (cls.isInstance(a)) {
                return (T) a;
            }
        }
        // 兜底：unit.abilities 可能为空，检查 type.abilities
        if (unit.type != null && unit.type.abilities != null) {
            for (Ability a : unit.type.abilities) {
                if (cls.isInstance(a)) {
                    return (T) a;
                }
            }
        }
        return null;
    }

    /** 匹配子单位类型名：兼容 mod 前缀（"npl-flame" 也匹配 "flame"） */
    private static boolean matchesChild(arc.struct.Seq<String> list, String typeName) {
        if (list == null) return false;
        if (list.contains(typeName)) return true;
        for (String s : list) {
            if (typeName.endsWith("-" + s)) return true;
        }
        return false;
    }

    @Override
    public void updateUnit() {
        if (unit == null) return;

        // 母体不存在或已死亡，寻找新的
        if (motherUnit == null || motherUnit.dead || !motherUnit.isAdded()) {
            findMother();
            if (motherUnit == null) {
                // 宽限期内不自杀，给母体出现留时间
                searchTimer += Time.delta;
                if (searchTimer > searchGrace * 60f) {
                    unit.kill();
                }
                return;
            }
            searchTimer = 0f;
        }

        // 碰到母体 → 吸收
        if (unit.within(motherUnit, unit.hitSize + motherUnit.hitSize)) {
            EatAbility eat = getAbility(motherUnit, EatAbility.class);
            if (eat != null) {
                eat.onAbsorb(motherUnit);
            } else {
                DevourAbility devour = getAbility(motherUnit, DevourAbility.class);
                if (devour != null) {
                    devour.onAbsorb(motherUnit);
                }
            }
            unit.remove();
            Fx.absorb.at(unit.x, unit.y);
            return;
        }

        // 直接朝母体移动（不依赖寻路，避免 moveTo 失效）
        Vec2 v = vec.set(motherUnit.x - unit.x, motherUnit.y - unit.y);
        if (v.len() > 1f) {
            v.setLength(unit.speed());
            unit.approach(v);
        }
        unit.lookAt(motherUnit.x, motherUnit.y);
    }

    /**
     * 自动寻找母体：
     * 遍历同队单位，检查其身上的 EatAbility/DevourAbility 的 returnChildTypes 是否包含本单位类型名
     */
    public void findMother() {
        String myTypeName = unit.type.name;

        Unit closest = null;
        float closestDist = Float.MAX_VALUE;

        for (Unit u : Groups.unit) {
            if (u == null || u == unit || u.dead || !u.isAdded()) continue;
            if (u.team != unit.team) continue;

            // 检查 EatAbility
            EatAbility eat = getAbility(u, EatAbility.class);
            if (eat != null && matchesChild(eat.returnChildTypes, myTypeName)) {
                float dist = u.dst(unit);
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = u;
                }
                continue;
            }

            // 检查 DevourAbility
            DevourAbility devour = getAbility(u, DevourAbility.class);
            if (devour != null && matchesChild(devour.returnChildTypes, myTypeName)) {
                float dist = u.dst(unit);
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = u;
                }
            }
        }

        if (closest != null) {
            motherUnit = closest;
        }
    }
}
