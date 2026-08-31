package Npl.newSth.Type;

import mindustry.gen.UnitEntity;

/**
 * 白色单位 Entity - 每受击一次扣除指定百分比最大生命值
 *
 * 百分比通过 FedUnitType.damagePercent 配置
 */
public class WhiteColorEntity extends UnitEntity {

    /** 工厂方法（UnitType.constructor 用这个创建实例） */
    public static WhiteColorEntity create() {
        return new WhiteColorEntity();
    }

    @Override
    public int classId() {
        return FedUnitEntity.classId(WhiteColorEntity.class);
    }
    /**
     * 重写伤害方法：每受击一次扣除配置百分比的最大生命值
     */
    @Override
    public void damage(float amount) {
        float percent = 0.2f;
        if (type instanceof FedUnitType) {
            percent = ((FedUnitType) type).damagePercent;
        }
        health = health - maxHealth * percent;
        if (health <= 0f) {
            health = 0;
            dead = true;
            remove();
        }
    }
}
