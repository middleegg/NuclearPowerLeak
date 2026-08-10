package Npl.newSth;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;

/**
 * 基于速度的伤害判定子弹类型。
 * <p>
 * 配置参数：
 *   damage (d)           : 基础伤害（父类字段）
 *   damageIncrease (i)   : 伤害增加量级
 * 判定规则：
 *   若子弹当前速度 > speedThreshold (默认 3.2)
 *       最终伤害 = d + i * (速度 ^ 1.2)
 *   否则
 *       最终伤害 = d + i
 * <p>
 * 附加功能：
 *   若此子弹被 {@link BulletAcceleratorBlock} 加速过（通过检测
 *   {@link BulletAcceleratorBlock.AcceleratedTag}），则在每帧 update 中做
 *   「抗衰减补偿」——保证经过加速器叠加的永久速度不会被 drag 慢慢抵消。
 */
public class SpeedDamageBulletType extends mindustry.entities.bullet.BasicBulletType {
    /* ===================== 可配置参数 ===================== */
    /** 伤害增加量级 i */
    public float damageIncrease = 0f;
    /** 速度阈值（默认 3.2），超过此阈值触发高次幂伤害加成 */
    public float speedThreshold = 4f;
    /** 速度指数（默认 1.2），用户若有需要也可以改 */
    public float speedPower = 1.2f;
    /** 是否启用抗衰减补偿（true = 被加速器加速后永久保持那个速度） */
    public boolean antiDecay = true;
    /* ===================== 构造 ===================== */

    public SpeedDamageBulletType(float speed, float damage, String bulletSprite){
        super(speed, damage, bulletSprite);
    }

    public SpeedDamageBulletType(float speed, float damage){
        super(speed, damage);
    }

    public SpeedDamageBulletType(){
        super();
    }

    /* ===================== 运行时 ===================== */

    @Override
    public void init(Bullet b){
        super.init(b);
        recalcDamage(b);
    }

    @Override
    public void update(Bullet b){
        // 注意：调用顺序上，BulletComp.update() 已经在调用我们之前
        // 执行过 vel.scl(1 - drag*delta) 和 vel.setLength(vel.len() + accel*delta) 了。
        // 所以 super.update(b) 之后立刻做抗衰减是最合适的时机。
        super.update(b);

        if(antiDecay){
            applyAntiDecay(b);
        }

        // 每帧根据实时速度重算伤害（抗衰减补偿之后算，拿到的就是补偿后的速度）
        recalcDamage(b);
    }

    /**
     * 抗衰减：如果 bullet.data 上挂着 AcceleratedTag（说明被某个
     * BulletAcceleratorBlock 永久加速过），则把速度的"低水位"维持在
     * originalBaseSpeed + boostSum，避免 drag 慢慢把加速效果吃光。
     */
    protected void applyAntiDecay(Bullet b){
        Object d = b.data;
        if(!(d instanceof BulletAcceleratorBlock.AcceleratedTag tag)) return;
        if(tag.boostSum <= 0f) return;

        Vec2 v = b.vel;
        float cur = v.len();
        float targetMin = tag.originalBaseSpeed + tag.boostSum;
        // 已经达到或超过目标速度（比如 accel 又加了一段）→ 不动
        if(cur >= targetMin - 0.001f) return;

        // 需要补足的速度量（沿当前方向）
        float need = targetMin - cur;
        if(cur > 0.001f){
            float s = need / cur;
            v.x += v.x * s;
            v.y += v.y * s;
        }else{
            // 当前几乎停住了，按子弹 rotation 方向补
            float ang = b.rotation() * Mathf.degRad;
            v.x += Mathf.cos(ang) * need;
            v.y += Mathf.sin(ang) * need;
        }
    }

    /** 根据 b.vel 的当前模长重新写入 b.damage */
    protected void recalcDamage(Bullet b){
        float v = b.vel.len();
        float d = this.damage;           // 基础伤害 d
        float i = this.damageIncrease;   // 增加量级 i
        float finalDamage;

        if(v > speedThreshold){
            finalDamage = d + i * Mathf.pow(v, speedPower);
        }else{
            finalDamage = d + i;
        }

        // Bullet 的 damage 字段直接控制碰撞造成的伤害
        b.damage = finalDamage;
    }

    /* ===================== 渲染（直接复用 BasicBulletType 即可） ===================== */
    // BasicBulletType.draw() 不变，不做额外覆盖
}

