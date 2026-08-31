package Npl.newSth;

import arc.util.Time;
import arc.util.Log;
import arc.util.io.Writes;
import arc.util.io.Reads;
import mindustry.content.Fx;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
import mindustry.*;
import Npl.newSth.Type.*;

/**
 * EvolveAbility — 进化能力，两种模式
 * 
 * twiceBreath = false（默认）：按时间进化，到 evolveTime 后进化
 * twiceBreath = true：血量归零后进化（Boss转阶段）
 * 
 * 进化后始终满血
 * 需要与 EatAbility 搭配使用，进化时从单位身上的 EatAbility 读取 absorbedCount
 */
public class EvolveAbility extends Ability {

    /** true = 死亡进化（Boss转阶段），false = 倒计时进化 */
    public boolean twiceBreath = false;
    /** 进化时间（tick），仅 twiceBreath=false 时有效 */
    public float evolveTime = 60f * 30f;
    /** 进化后的单位类型 */
    public UnitType evolveUnitType;

    private float evolveTimer = 0f;
    private boolean hasEvolved = false;

    @Override
    public void update(Unit unit) {
        if (hasEvolved) return;

        if (twiceBreath) {
            // 死亡触发模式：血量归零时进化
            if (unit.health <= 0f) {
                evolve(unit);
            }
        } else {
            // 倒计时模式
            evolveTimer += Time.delta;
            if (evolveTimer >= evolveTime) {
                evolveTimer = 0f;
                evolve(unit);
            }
        }
    }

    /** 进化为目标单位类型，继承 EatAbility 的加成，始终满血 */
    public void evolve(Unit unit) {
        if (evolveUnitType == null) {
            Log.err("EvolveAbility: evolveUnitType is null, cannot evolve!");
            return;
        }

        // 从单位身上找 EatAbility，读取吞噬数
        int absorbed = 0;
        float healthMul = 1f, damageMul = 1f, armorMul = 1f, shieldMul = 1f;
        for (Ability ab : unit.abilities) {
            if (ab instanceof EatAbility eat) {
                absorbed = eat.getAbsorbedCount();
                healthMul = 1f + absorbed * eat.healthBonusPerAbsorb;
                damageMul = 1f + absorbed * eat.damageBonusPerAbsorb;
                armorMul = 1f + absorbed * eat.armorBonusPerAbsorb;
                shieldMul = 1f + absorbed * eat.shieldBonusPerAbsorb;
                break;
            }
        }

        Log.info("EvolveAbility: evolving! twiceBreath=" + twiceBreath + " absorbed=" + absorbed);

        // 创建进化后的单位
        Unit newUnit = evolveUnitType.create(unit.team);
        newUnit.set(unit.x, unit.y);
        newUnit.rotation = unit.rotation;

        // 继承加成 + 满血
        float newMax = newUnit.type.health * healthMul;
        newUnit.maxHealth = newMax;
        newUnit.health = newMax; // 始终满血

        newUnit.shield = newUnit.type.health * shieldMul * 0.1f;
        newUnit.armor = newUnit.type.armor * armorMul;
        newUnit.reloadMultiplier = 1f + absorbed * 0.01f;

        if (newUnit instanceof EvolveEntity) {
            EvolveEntity custom = (EvolveEntity) newUnit;
            custom.damageMultiplier = damageMul;
            custom.armorMultiplier = armorMul;
        }

        newUnit.add();
        unit.remove();
        hasEvolved = true;

        // 进化特效
        Fx.unitAssemble.at(newUnit.x, newUnit.y, newUnit.rotation);
        Fx.reactorsmoke.at(newUnit.x, newUnit.y);
        Log.info("EvolveAbility: evolved to " + evolveUnitType.name);
    }

    public void write(Writes write) {
        write.bool(hasEvolved);
        write.f(evolveTimer);
    }

    public void read(Reads read) {
        hasEvolved = read.bool();
        evolveTimer = read.f();
    }
}
