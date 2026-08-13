package Npl.content;

import arc.graphics.Color;
import arc.util.Log;
import mindustry.entities.bullet.BombBulletType;
import mindustry.entities.bullet.LaserBulletType;
import mindustry.graphics.Pal;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import Npl.newSth.AI.WormAI;
import Npl.newSth.Type.FedUnitEntity;
import Npl.newSth.Type.SegmentUnitEntity;
import Npl.newSth.Type.SegmentWormEntity;

/**
 * 多节（虫子）单位加载类。
 * <p>
 * 移植自 PU132 的多节段单位系统（参考 zzw 的 Z_Units）。
 * <p>
 * <h3>多节单位的核心设计</h3>
 * <p>每个虫子由「头部 UnitType」和「段身 UnitType」两部分组成：
 * <ul>
 *   <li><b>头部</b>：用 {@link SegmentWormEntity}，有武器、AI、血量分布逻辑</li>
 *   <li><b>段身</b>：用 {@link SegmentUnitEntity}，hidden=true 不出现在数据库，
 *       位置完全由头部通过 {@code syncToHead(x, y, rot)} 控制</li>
 * </ul>
 * <p>
 * <h3>关键概念：SegmentConfig 注册</h3>
 * <p>每个虫子头部创建后，必须通过
 * {@code SegmentWormEntity.configs.put(headType.name, new SegmentWormEntity.SegmentConfig(...))}
 * 注册段身配置。SegmentWormEntity 在 add()/update() 时根据 {@code type.name} 查 Config
 * 来创建/管理段身。<b>key 必须用 {@code headType.name}</b>，因为 mod 单位的 name
 * 会自动加 mod 前缀（如 "nu-arcnelidia"），用字面字符串会查不到。
 * <p>
 * <h3>关键概念：constructor 与 aiController</h3>
 * <pre>{@code
 * constructor = SegmentWormEntity::create;   // 头部用虫子专用 Entity
 * aiController = () -> new WormAI();         // 待机静止的 AI
 * }</pre>
 * <p>WormAI 继承 FlyingAI，重写 updateMovement() 让单位待机时静止，
 * 不像默认 FlyingAI 会自动朝最近 spawn 移动。
 */
public class CalamityUnitType {

    /** 多节单位 UnitType 字段：每个虫子有「头部」和「段身」两个 UnitType */
    public static UnitType
        arcnelidia,            // 电弧虫头部
        arcnelidiaSegment;     // 电弧虫段身

    /**
     * 加载所有多节单位。在 mod 的 loadContent 阶段被调用一次。
     *
     * <p>加载顺序：
     * <ol>
     *   <li>注册自定义 Entity 的 classId（必需，否则 UnitType.init 抛异常）</li>
     *   <li>按顺序创建虫子（段身 UnitType 先于头部，因为头部 config 要引用段身）</li>
     *   <li>每个虫子内：段身 UnitType → 头部 UnitType → SegmentConfig 注册 → 反射设置音效</li>
     * </ol>
     */
    public static void load() {
        // ★ 关键：注册自定义 Entity 到 EntityMapping.idMap，否则 UnitType.init() 会失败 ★
        // 要求每个自定义 Entity class 有唯一 classId，必须在 idMap 占一个空 slot
        FedUnitEntity.register(SegmentWormEntity.class, SegmentWormEntity::new);
        FedUnitEntity.register(SegmentUnitEntity.class, SegmentUnitEntity::new);

        // ═══════════════════════════════════════════════════════════
        //  Arcnelidia (电弧虫) —— PU132 同名单位移植
        //  - 9 段（segmentLength=9）
        //  - segmentOffset=22.7f (PU132 23f - 0.3f)
        //  - hitSize=19.75f (段间距 22.7 > 半径之和 19.75, 不重叠)
        //  - angleLimit=30f, wobble=true (轻微晃动)
        //  - 头部武器：双激光 (mirror=true, LaserBulletType, surge 黄色)
        //  - 段身武器：投弹 (BombBulletType, splashDamage=250)
        //  - 不可分裂/合并 (splittable=false, chainable=false)
        // ═══════════════════════════════════════════════════════════

        // —— 段身 UnitType（先创建，头部 config 要引用它）——
        arcnelidiaSegment = new UnitType("arcnelidia-segment") {{
            health = 1600;  // ★ 提高段身血量（头部800×2），避免段身太脆
            speed = 0f;     // 段身不需要自己移动（由头部驱动）
            // ★ hitSize=19.75f（19.25 + 0.5）
            // 碰撞计算：段间距 22.7 > 半径 9.875+9.875=19.75，不重叠（间隙 2.95）
            hitSize = 19.75f;
            armor = 5f;
            flying = true;
            rotateSpeed = 1f;
            faceTarget = false;
            // ★ 关闭 wobble（PU132 原版静止时不晃动）
            wobble = false;

            // 用 SegmentUnitEntity（禁用 AI 和自身移动）
            constructor = SegmentUnitEntity::create;

            // ★ 隐藏段身（不出现在数据库/Spawner，玩家无法单独召唤）
            hidden = true;

            // ★ 段身不计入单位上限（PU132 WormSegmentUnit.isCounted 返回 false）
            useUnitCap = false;

            // ★ 段身关闭物理碰撞（physics=false），避免撞墙时被弹开导致尾部乱甩
            physics = false;
            hittable = true;

            // ===== 段身武器：BombBullet（PU132 原版，匿名武器无贴图） =====
            // 电弧虫段身投弹：splashDamage=250，爆炸色同电弧
            weapons.add(new Weapon() {{
                x = 0f;
                rotate = true;
                mirror = false;
                reload = 72f;  // 60 * 1.2，攻击频率减少一点点
                rotateSpeed = 50f;
                shootCone = 180f;
                bullet = new BombBulletType(27f, 250f) {{  // 25 + 250
                    width = 10f;
                    height = 14f;
                    hitEffect = mindustry.content.Fx.flakExplosion;
                    shootEffect = mindustry.content.Fx.none;
                    smokeEffect = mindustry.content.Fx.none;
                    collidesAir = false;
                    collidesGround = true;
                    splashDamage = 250f;  // 25 + 225
                    splashDamageRadius = 25f;
                    status = mindustry.content.StatusEffects.blasted;
                    statusDuration = 60f;
                }};
            }});
        }};

        // —— 头部 Arcnelidia 飞行分段虫子 ——
        arcnelidia = new UnitType("arcnelidia") {{
            // ===== 基础属性（PU132 原值） =====
            health = 800;  // PU132 原版
            speed = 4f;
            accel = 0.035f;
            rotateSpeed = 3.2f;
            hitSize = 19.75f;
            armor = 5f;
            flying = true;
            // PU132：engineSize=-1f（不显示引擎喷射效果）
            engineSize = -1f;
            range = 210f;
            // ★ PU132 原版 faceTarget=false：单位不盯着目标，而是朝飞行方向
            //   配合 circleTarget=false + moveTo(target, 30f)，单位直线冲过目标再折返
            //   这样整段身体都能发挥作用（段身投弹/激光）
            //   激光武器有 minShootVelocity=2.1f，必须移动才会发射
            faceTarget = false;
            // ★ arcnelidia 关闭原版 wobble（振幅 0.05f 太大），用自定义 wobbleEnabled（振幅 0.02f）
            wobble = false;
            // ★ drag 用飞行单位合理值（默认 0.3f 对飞行单位太大，速度衰减太快显得僵硬）
            drag = 0.018f;

            // 用自定义 Entity（SegmentWormEntity）
            constructor = SegmentWormEntity::create;
            // ★ 使用 WormAI（待机静止，不自动朝 spawn 移动）
            aiController = () -> new WormAI();

            // ===== 头部武器：双激光（PU132 原配置） =====
            // PU132 UnityUnitTypes.java：匿名武器，无炮台贴图
            weapons.add(new Weapon() {{
                x = 0f;
                reload = 10f;
                rotateSpeed = 50f;
                // shootSound 在后面用反射设置
                mirror = true;
                rotate = false;  // ★ 锁定朝向，不独立旋转
                minShootVelocity = 2.1f;
                bullet = new LaserBulletType(450f) {{  // 200 + 250
                    // PU132 原配置：surge 颜色（电弧激光，黄色）
                    colors = new Color[]{
                        Pal.surge.cpy().mul(1f, 1f, 1f, 0.4f),
                        Pal.surge,
                        Color.white
                    };
                    drawSize = 400f;
                    collidesAir = false;
                    length = 190f;
                    // ★ 加大激光宽度：20f（默认 15f，太细看起来像白线）
                    width = 20f;
                    // ★ 加长激光持续时间：24f（默认 16f，太短看起来断断续续）
                    lifetime = 24f;
                }};
            }});
        }};

        // ★ 注册 arcnelidia 段身配置到 configs Map ★
        // PU132 原版 segmentLength=9, segmentOffset=23f
        // 段间距 22.7f（PU132 23f - 0.3f，用户要求稍小一点）
        // wobble=true（arcnelidia 轻微晃动）
        // angleLimit=30f（龙的感觉：更大的弯曲角度）
        // anglePhysicsSmooth=0.5f（更平滑的转向，段身自然跟随头部）
        // segmentCast=6, jointStrength=0.6f（增大传播范围，减小关节强度防止脱节）
        SegmentWormEntity.configs.put(arcnelidia.name,
            new SegmentWormEntity.SegmentConfig(arcnelidiaSegment, 9, 22.7f, 0f, 0, true, false, false,
                30f, 6f, 0.1f, 0.6f, 6, 0.5f, false, 0f));
        // 电弧虫：每秒回10血
        SegmentWormEntity.configs.get(arcnelidia.name).healPerSecond = 10f;

        // 用反射设置 shootSound 和 visualElevation，避开编译期字段差异（v150 vs v154）
        try {
            Class<?> soundsClass = Class.forName("mindustry.gen.Sounds");
            java.lang.reflect.Field f = soundsClass.getField("shootLaser");
            Object snd = f.get(null);
            arc.audio.Sound sound = (arc.audio.Sound) snd;
            arcnelidia.weapons.first().shootSound = sound;
        } catch (Throwable t) {
            try { Log.err("set shootSound failed", t); } catch (Throwable ignored) {}
        }
        // PU132：visualElevation=0.8f（可能已移除该字段，静默忽略）
        try {
            java.lang.reflect.Field ve = arcnelidia.getClass().getSuperclass().getField("visualElevation");
            ve.setFloat(arcnelidia, 0.8f);
        } catch (Throwable ignored) {}
    }
}
