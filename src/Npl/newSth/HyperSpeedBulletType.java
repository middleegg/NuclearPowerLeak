package Npl.newSth;

import arc.math.*;
import mindustry.entities.bullet.*;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.*;
import mindustry.type.Weapon;
import mindustry.world.blocks.defense.turrets.*;

/**
 * HyperSpeedBulletType —— 「攻速越快伤害越高」子弹类型。
 *
 * 现在基于 Mindustry 的 {@link PointBulletType}（项目 v136 的实现 = "带拖尾的普通飞行弹"），
 * 继承它的全部原版特性：
 *   pierce / pierceCap / pierceBuilding / collidesAir / collidesGround /
 *   weaveScale / weaveMag / homingPower / homingRange / splashDamage /
 *   status / buildingDamageMultiplier / healAmount / fragBullets ……
 *
 * PointBulletType 的"外观"由以下字段控制（不再需要 width/height/bulletSprite）：
 *   trailEffect   拖尾粒子特效（建议 Fx.trailPoint 或自定义）
 *   trailSpacing  每经过多少像素触发一次拖尾（像素，默认 10）
 *   trailInterval 每经过多少 tick 触发一次拖尾（tick，默认 2）
 *   注意：PointBulletType 不需要 sprite。如果你想画贴图，可以继续在子类里用 bulletSprite。
 *
 * 伤害加成原理保持不变：子弹刚创建（init 时）会去读取**发射者的当前攻速 v**，
 * 然后把这发子弹的最终单发伤害改成：
 *
 *     finalDamage = damage + damageScale × v ^ speedExp
 *
 * 默认值：
 *     damageScale = 1.5
 *     speedExp    = 1.1
 *     v           = 每秒攻击次数 = 60 / reload × 各种 reloadMult × fireSpeedMul × efficiency …
 */
public class HyperSpeedBulletType extends PointBulletType {

    /* ============== 可配置：伤害加成公式 ============== */

    /** 额外伤害前的倍率（默认 1.5） */
    public float damageScale = 1.5f;

    /** 攻速 v 的指数（默认 1.1） */
    public float speedExp = 1.1f;

    /** 当无法从发射者身上拿到攻速时（单位武器 / 其他未知来源），用这个值作为 fallback */
    public float fallbackV = 1.0f;
    /* ============== 可配置：调试/日志 ============== */
    /** 当无法解析攻速时是否回退（true 就回退 fallbackV；false 就只用 damage，不加额外伤害） */
    public boolean fallbackOnUnknown = true;
    /* ============== 构造 ============== */
    /** 空构造（PointBulletType 要求），后续在初始化块里设置 speed/damage/trail 等 */
    public HyperSpeedBulletType(){
        super();
    }
    /** 快速构造：只给 speed 和 damage。拖尾、穿刺等继续在 {{}} 块里设置。 */
    public HyperSpeedBulletType(float speed, float damage){
        super();
        this.speed  = speed;
        this.damage = damage;
    }

    /* ============== 运行时 ============== */

    @Override
    public void init(Bullet b){
        super.init(b);
        applyHyperDamage(b);
    }

    /**
     * 根据发射者的实时攻速 v 给 b.damage 加成。
     * 只在 init 时跑一次，因为 v（当前攻速倍率）在发射瞬间即决定；
     * 后续如果飞行中 owner 的攻速再变，不影响这颗已经打出去的子弹（符合"射出即快照"的直觉）。
     */
    protected void applyHyperDamage(Bullet b){
        float v = resolveV(b);
        if (v <= 0f) v = fallbackOnUnknown ? fallbackV : 0f;

        if (v > 0f) {
            float bonus = damageScale * (float) Math.pow(v, (double) speedExp);
            b.damage = damage + bonus;
        } else {
            b.damage = damage;
        }
    }

    /**
     * 解析当前子弹对应的"每秒发射次数 v"。
     *
     * v 的明确定义：<b>v = 60 / reload</b>（发/秒），因为 reload 的单位是 tick/发，
     * 而 Mindustry 每秒固定 60 tick。例如 reload=20 → 基础 v = 60/20 = 3 发/秒。
     * 在此基础上再乘：弹药/武器 reloadMultiplier、炮塔 efficiency、
     * 单位 unit.reloadMultiplier、叠层 curReloadMul 等。
     */
    protected float resolveV(Bullet b){
        Object o = b.owner;

        // —— 首选：HyperSpeedTurret（同目标叠层炮塔）——
        if (o instanceof HyperSpeedTurret.HyperSpeedTurretBuild hb){
            return hb.currentShotsPerSecond();
        }

        // —— 次选：HyperSpeedWeapon（单位武器，同目标叠层）——
        if (o instanceof Unit unit){
            WeaponMount hitMount = null;
            Weapon hitWeapon = null;
            if (unit.mounts != null){
                // 找"当前子弹对应的 weapon / mount"：
                // 优先从 b.data 里看发射者是否塞了 WeaponMount reference；
                // 如果没有，就找这个 unit 上第一个 HyperSpeedWeapon 当作近似。
                if (b.data instanceof WeaponMount wm && wm.weapon instanceof HyperSpeedWeapon){
                    hitMount = wm;
                    hitWeapon = wm.weapon;
                }
                if (hitWeapon == null){
                    for (WeaponMount m : unit.mounts){
                        if (m.weapon instanceof HyperSpeedWeapon hs){
                            hitMount = m;
                            hitWeapon = hs;
                            break;
                        }
                    }
                }
            }
            if (hitWeapon instanceof HyperSpeedWeapon hs){
                return hs.currentShotsPerSecond(unit, hitMount);
            }
            // 普通 Weapon 保底：取第一个武器的基础 v = 60 / weapon.reload
            if (unit.mounts != null && unit.mounts.length > 0 && unit.mounts[0].weapon != null){
                Weapon w = unit.mounts[0].weapon;
                if (w.reload > 0f){
                    float base = 60f / w.reload;
                    float brm = 1f, urm = 1f;
                    if (w.bullet != null) brm = w.bullet.reloadMultiplier;
                    try { urm = unit.reloadMultiplier; } catch (Throwable ignore){}
                    return base * brm * urm;
                }
            }
        }

        // —— 其他 Turret：取 60/reload 作为基础 v（不叠层）——
        if (o instanceof Turret.TurretBuild tb){
            if (tb.block instanceof Turret t && t.reload > 0f){
                float base = 60f / t.reload;               // 100% 效率下每秒攻击次数
                BulletType bt = tb.peekAmmo();
                float ammo = (bt != null) ? bt.reloadMultiplier : 1f;
                // efficiency 是 BuildingComp 上的公开字段（不是方法）
                float eff  = Math.max(0.0001f, tb.efficiency);
                return base * ammo * eff;
            }
        }

        // 未支持：fallback
        return fallbackOnUnknown ? fallbackV : -1f;
    }
}
