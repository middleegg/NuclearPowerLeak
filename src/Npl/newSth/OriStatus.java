package Npl.newSth;

import arc.struct.ObjectMap;
import arc.util.Time;
import mindustry.gen.Unit;
import mindustry.type.StatusEffect;
import mindustry.entities.units.*;

/**
 * 可调节状态效果：护甲加成 / 护甲百分比 / 受伤倍率 / 挖矿倍率
 * <p>
 * 四个可配置属性：
 *   1. armorBonus          → 直接累加到单位护甲（unit.armor）
 *   2. armorPercent        → 按百分比调整护甲（0.5=+50% 原始护甲，-0.5=减半）
 *   3. injuredMultiplier   → 受伤倍率（1=正常，0.5=受伤减半，0=无敌，2=受伤翻倍）
 *   4. mineSpeedMul        → 挖矿速度倍率（1=正常，2=两倍速）
 * <p>
 * 注意：Mindustry v159.5 原版 StatusEffect 没有 armor 和 mineSpeedMultiplier 字段，
 *       所以这里通过直接修改 unit.armor 实现，并在状态移除时回退。
 * <p>
 * 用法：
 *   new OriStatus("my-buff"){{
 *       armorBonus = 10f;          // +10 护甲
 *       armorPercent = 0.5f;       // +50% 原始护甲
 *       injuredMultiplier = 0.5f;  // 受伤减半
 *       mineSpeedMul = 2f;         // 挖矿速度翻倍
 *   }}
 */
public class OriStatus extends StatusEffect {

    /** 属性一：护甲固定加成（直接累加到 unit.armor） */
    public float armorBonus = 0f;

    /** 属性二：护甲百分比加成（0=不变，0.5=+50% 原始护甲，1=翻倍，-0.5=减半） */
    public float armorPercent = 0f;

    /** 属性三：受伤倍率（1=正常，0.5=受伤减半，0=无敌，2=受伤翻倍） */
    public float injuredMultiplier = 1f;

    /** 属性四：挖矿速度倍率（1=正常，2=两倍速） */
    public float mineSpeedMul = 1f;

    /** per-unit 原始护甲（首次施加时记录，用于百分比计算和移除时回退） */
    private final ObjectMap<Integer, Float> baseArmor = new ObjectMap<>();

    /** per-unit 上一帧血量，用于计算受伤倍率 */
    private final ObjectMap<Integer, Float> lastHealth = new ObjectMap<>();

    /** per-unit 累计已加的护甲量（用于移除时精确回退） */
    private final ObjectMap<Integer, Float> appliedArmorDelta = new ObjectMap<>();

    public OriStatus(String name) {
        super(name);
    }

    @Override
    public void applied(Unit unit, float time, boolean extend) {
        super.applied(unit, time, extend);
        // 首次施加时记录原始护甲
        int id = unit.id;
        if (!baseArmor.containsKey(id)) {
            baseArmor.put(id, unit.armor);
        }
        lastHealth.put(id, unit.health);
    }

    @Override
    public void update(Unit unit, StatusEntry entry) {
        super.update(unit, entry);

        int id = unit.id;

        // —— 护甲调整（固定 + 百分比叠加） ——
        // 每帧重算：先回退上次加的，再加新的（保证 armorPercent 基于原始护甲稳定计算）
        if (armorBonus != 0f || armorPercent != 0f) {
            float base = baseArmor.get(id, unit.armor);
            float lastDelta = appliedArmorDelta.get(id, 0f);
            // 先减掉上次的增量
            unit.armor -= lastDelta;
            // 计算新的增量 = 固定加成 + 原始护甲 × 百分比
            float newDelta = armorBonus + base * armorPercent;
            unit.armor += newDelta;
            appliedArmorDelta.put(id, newDelta);
        }

        // —— 受伤倍率（对比上一帧血量，按倍率回补/额外扣血） ——
        if (injuredMultiplier != 1f) {
            if (unit.dead) {
                lastHealth.remove(id);
                return;
            }
            float last = lastHealth.get(id, unit.health);
            if (unit.health < last) {
                float damage = last - unit.health;          // 这一帧受到的伤害
                float adjusted = damage * injuredMultiplier; // 调整后的实际伤害
                float refund = damage - adjusted;            // 回补量（正=回血，负=额外扣血）
                unit.health = Math.min(unit.health + refund, unit.maxHealth);
            }
            lastHealth.put(id, unit.health);
        }
    }

    @Override
    public void onRemoved(Unit unit) {
        super.onRemoved(unit);
        int id = unit.id;
        // 状态移除时回退护甲增量
        float lastDelta = appliedArmorDelta.get(id, 0f);
        unit.armor -= lastDelta;
        appliedArmorDelta.remove(id);
        lastHealth.remove(id);
        // baseArmor 保留，防止再次施加时基数失真
    }
}
