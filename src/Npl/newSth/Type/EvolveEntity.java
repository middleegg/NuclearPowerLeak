package Npl.newSth.Type;

import arc.*;
import arc.math.*;
import arc.util.*;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.gen.Hitboxc;
import mindustry.gen.UnitEntity;
import mindustry.entities.Units;
import mindustry.type.Weapon;
import mindustry.entities.units.WeaponMount;
import mindustry.content.StatusEffects;

/**
 * EvolveEntity — 可进化单位实体
 * 
 * 额外功能：阶段增益系统
 * 每当血量累计降低 maxHealth 的 20% 时，获得长达60秒的增益
 * 增益叠加，最多5层
 */
public class EvolveEntity extends UnitEntity {
    public float damageMultiplier = 1f;
    public float armorMultiplier = 1f;
    public float regenMultiplier = 1f;
    public float healthMultiplier = 1f;
    public boolean unlockWeapon = false;

    /** 阶段增益系统 */
    private float lastStageHealth = 0f;
    private int currentStage = 0;
    private static final int MAX_STAGES = 5;
    /** 每个阶段持续时间（秒） */
    private static final float STAGE_DURATION = 60f;
    /** 每个阶段的增益倍率 */
    private static final float STAGE_DAMAGE_BONUS = 0.15f;
    private static final float STAGE_ARMOR_BONUS = 0.1f;
    private static final float STAGE_REGEN_BONUS = 0.05f;
    /** 每个阶段的剩余时间数组 */
    private float[] stageTimers = new float[MAX_STAGES];

    @Override
    public void add() {
        super.add();
        lastStageHealth = health;
    }

    @Override
    public void damage(float amount) {
        // 护甲减免
        float damageReduction = armorMultiplier * 0.5f;
        float finalDamage = amount * (1f - Math.min(damageReduction, 0.8f));
        super.damage(finalDamage);

        // 检查阶段增益
        checkStageBuff();
    }

    /** 检查是否触发阶段增益 */
    private void checkStageBuff() {
        if (maxHealth <= 0f) return;
        float dropThreshold = maxHealth * 0.2f;
        float totalDrop = lastStageHealth - health;

        if (totalDrop >= dropThreshold) {
            // 触发一个阶段增益
            if (currentStage < MAX_STAGES) {
                int slot = findFreeStageSlot();
                if (slot >= 0) {
                    stageTimers[slot] = STAGE_DURATION;
                    currentStage++;
                    // 触发特效
                    mindustry.content.Fx.heal.at(x, y);
                }
            }
            // 重置阶段基准线（只能触发一次，除非回血后又下降）
            lastStageHealth = health;
        }
    }

    private int findFreeStageSlot() {
        for (int i = 0; i < MAX_STAGES; i++) {
            if (stageTimers[i] <= 0f) return i;
        }
        return -1;
    }

    /** 获取当前活跃阶段数 */
    public int getActiveStages() {
        int count = 0;
        for (int i = 0; i < MAX_STAGES; i++) {
            if (stageTimers[i] > 0f) count++;
        }
        return count;
    }

    /** 获取阶段总伤害倍率 */
    public float getStageDamageBonus() {
        return getActiveStages() * STAGE_DAMAGE_BONUS;
    }

    /** 获取阶段总护甲倍率 */
    public float getStageArmorBonus() {
        return getActiveStages() * STAGE_ARMOR_BONUS;
    }

    /** 获取阶段总回复倍率 */
    public float getStageRegenBonus() {
        return getActiveStages() * STAGE_REGEN_BONUS;
    }

    @Override
    public void update() {
        super.update();

        // 更新阶段计时器
        for (int i = 0; i < MAX_STAGES; i++) {
            if (stageTimers[i] > 0f) {
                stageTimers[i] -= Time.delta / 60f;
                if (stageTimers[i] <= 0f) {
                    stageTimers[i] = 0f;
                    currentStage = Math.max(0, currentStage - 1);
                }
            }
        }

        // 阶段回复
        int stages = getActiveStages();
        if (stages > 0 && health < maxHealth) {
            health += maxHealth * getStageRegenBonus() * Time.delta / 60f;
            health = Math.min(health, maxHealth);
        }
    }

    @Override
    public int classId() {
        return FedUnitEntity.classId(EvolveEntity.class);
    }
}
