package Npl.newSth;

import arc.math.Mathf;
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
 * EatAbility — 吞噬变强
 * 定期生成子单位 → 吸收 → 每次吸收获得血量/伤害/护甲/护盾加成
 * 不包含进化逻辑（进化由 EvolveAbility 负责）
 */
public class EatAbility extends Ability {
    /** 子单位生成间隔（秒） */
    public float spawnInterval = 60f * 2f;
    /** 子单位类型 */
    public UnitType childType;
    /** 每个吸收+1%血量 */
    public float healthBonusPerAbsorb = 0.01f;
    /** 每个吸收+1%伤害 */
    public float damageBonusPerAbsorb = 0.01f;
    /** 每个吸收+1%护甲 */
    public float armorBonusPerAbsorb = 0.01f;
    /** 每个吸收+1%护盾 */
    public float shieldBonusPerAbsorb = 0.01f;
    /** 可回归的子单位类型名称列表，ReturnAi 自动识别并钻入 */
    public arc.struct.Seq<String> returnChildTypes = new arc.struct.Seq<>();

    private float spawnTimer = 0f;
    private int absorbedCount = 0;

    @Override
    public void update(Unit unit) {
        // 生成子单位
        spawnTimer += Time.delta;
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f;
            spawnChild(unit);
        }

        if (Time.time % 60f < 0.1f) {
            Log.info("EatAbility: absorbedCount=" + absorbedCount);
        }
    }

    /** 在每个CoreBlock范围内生成子单位 */
    public void spawnChild(Unit parent) {
        float coreRange = 240f;
        for (var build : mindustry.gen.Groups.build) {
            if (build.block instanceof mindustry.world.blocks.storage.CoreBlock && build.team == parent.team) {
                float angle = Mathf.random(0f, Mathf.PI2);
                float dist = Mathf.random(0f, coreRange);
                float childX = build.x + Mathf.cos(angle) * dist;
                float childY = build.y + Mathf.sin(angle) * dist;
                Unit child = childType.create(parent.team);
                child.set(childX, childY);
                child.add();
                Fx.spawn.at(childX, childY);
            }
        }
    }

    /** 吸收一个单位时调用，增加计数并立即应用加成 */
    public void onAbsorb(Unit unit) {
        absorbedCount++;
        applyBonuses(unit);
        Fx.absorb.at(unit.x, unit.y);
    }

    /** 应用吞噬加成到单位 */
    public void applyBonuses(Unit unit) {
        float healthMultiplier = 1f + absorbedCount * healthBonusPerAbsorb;
        float damageMultiplier = 1f + absorbedCount * damageBonusPerAbsorb;
        float armorMultiplier = 1f + absorbedCount * armorBonusPerAbsorb;
        float shieldMultiplier = 1f + absorbedCount * shieldBonusPerAbsorb;

        float newMax = unit.type.health * healthMultiplier;
        float ratio = unit.maxHealth > 0 ? unit.health / unit.maxHealth : 1f;
        unit.maxHealth = newMax;
        unit.health = newMax * ratio;

        unit.shield = unit.type.health * shieldMultiplier * 0.1f;
        unit.armor = unit.type.armor * armorMultiplier;
        unit.reloadMultiplier = 1f + absorbedCount * 0.01f;

        if (unit instanceof EvolveEntity) {
            EvolveEntity custom = (EvolveEntity) unit;
            custom.damageMultiplier = damageMultiplier;
            custom.armorMultiplier = armorMultiplier;
        }
        Log.info("EatAbility: applied bonuses, healthMul=" + healthMultiplier + " count=" + absorbedCount);
    }

    public int getAbsorbedCount() {
        return absorbedCount;
    }

    public void write(Writes write) {
        write.i(absorbedCount);
    }

    public void read(Reads read) {
        absorbedCount = read.i();
    }
}
