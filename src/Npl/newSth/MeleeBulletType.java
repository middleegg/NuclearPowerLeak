package Npl.newSth;

import arc.math.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.game.*;
import mindustry.gen.*;
import Npl.content.*;

/**
 * 挥砍型子弹 · 用于近战武器/炮塔。
 * <p>
 * 特性：speed = 0（不会飞出去）、lifetime 短（一闪而过）、
 * 生成时立刻对扇形范围内的敌人造成伤害并触发 slash 刀光 effect，
 * 然后立即结束生命周期消失。
 *
 * <p><b>用法范例（挂在 Weapon 上让单位/玩家近战）：</b>
 * <pre>{@code
 * new Weapon("npl-melee"){{
 *     x = 0f; y = 0f;
 *     reload = 30f;          // 挥砍冷却（tick）
 *     recoil = 0f;
 *     shootCone = 100f;       // 扇形瞄准角度
 *     rotate = false;
 *     minShootVelocity = 0f;  // 不需要移动也能挥
 *     shootSound = Sounds.pierceShoot;
 *
 *     bullet = new MeleeBulletType(){{
 *         damage = 30f;
 *         slashRange = 40f;       // 刀光长度
 *         slashArc = 80f;          // 扇形角度
 *         slashEffect = NuFx.slash;
 *         hitEffect = Fx.hitBulletOut;
 *     }};
 * }};
 * }</pre>
 *
 * <p><b>用在炮塔上（PowerTurret）：</b>直接把 shootType 设为 MeleeBulletType 实例即可，
 * 因为 PowerTurret 默认会朝目标方向"开火"，子弹生成时的 rotation 就是炮塔朝向。
 */
public class MeleeBulletType extends BulletType{

    /** 挥砍作用范围（像素，相当于刀光长度）。*/
    public float slashRange = 40f;
    /** 扇形伤害角度（度）。只在扇形内的目标受伤害。*/
    public float slashArc = 80f;
    /** 挥砍刀光 effect。默认 NuFx.slash。*/
    public Effect slashEffect = NuFx.slash;
    /** 命中单目标时的 effect（每个被打中的目标触发一次）。*/
    public Effect hitEffect = Fx.none;

    public MeleeBulletType(){
        super();
        speed = 0f;            // 不飞
        lifetime = 6f;         // 短命（一闪）
        collides = false;      // 不走原版碰撞检测（我们手动扇形伤害）
        collidesAir = false;
        collidesGround = false;
        collidesTiles = false;
        hitSize = 0f;
        despawnHit = false;
        absorbable = false;
        hittable = false;
        keepVelocity = false;
        pierce = true;
    }

    /** 子弹生成时立刻挥砍：触发刀光 effect + 扇形伤害 + 立即销毁。 */
    @Override
    public void init(Bullet b){
        super.init(b);
        float rot = b.rotation();

        // 触发刀光 effect（朝子弹方向）
        if(slashEffect != null){
            slashEffect.at(b.x, b.y, rot);
        }

        // 扇形范围伤害
        Team team = b.team;
        float x = b.x, y = b.y;
        float range = slashRange;
        float arc = slashArc;
        float dmg = b.damage;

        // ① 敌方单位
        Units.nearbyEnemies(team, x, y, range, u -> {
            if(u.dead || !u.isValid()) return;
            float angTo = Angles.angle(x, y, u.x, u.y);
            if(!withinArc(rot, angTo, arc)) return;
            u.damage(dmg);
            if(hitEffect != null) hitEffect.at(u.x, u.y, angTo);
        });

        // ② 敌方建筑
        Groups.build.intersect(x - range, y - range, range * 2f, range * 2f, build -> {
            if(build == null || build.dead || !build.isValid()) return;
            if(build.team == team || build.team == Team.derelict) return;
            float angTo = Angles.angle(x, y, build.x, build.y);
            if(!withinArc(rot, angTo, arc)) return;
            build.damage(dmg);
            if(hitEffect != null) hitEffect.at(build.x, build.y, angTo);
        });

        // 立即结束生命（刀光一闪即逝）
        b.time = b.lifetime;
    }

    /** 不画子弹本身（刀光由 slashEffect 完成）。 */
    @Override
    public void draw(Bullet b){
        // 故意空实现：覆盖默认的子弹贴图绘制
    }

    /** 判断 targetAngle 是否在 facing 中心 ± arc/2 度范围内。 */
    private static boolean withinArc(float facing, float targetAngle, float arc){
        float diff = Math.abs(Angles.angleDist(facing, targetAngle));
        return diff <= arc / 2f;
    }
}
