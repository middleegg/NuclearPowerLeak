package  Npl.newSth.Type;

import  Npl.newSth.Type.FedUnitType;
import  Npl.newSth.effects.WormDecal;
import arc.graphics.g2d.Draw;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Tmp;
import arc.util.Time;
import arc.util.Log;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;
import mindustry.entities.Effect;
import mindustry.content.Fx;

/**
 * ★★★ 分段虫子单位的"头部" Entity（多节单位系统的核心控制器）★★★
 *<p>
 * 【这个类是干什么的】
 *   SegmentWormEntity 是一条"虫子"的头部，负责整条虫子的所有逻辑：
 *   - 持有段身数组 segments[]，每节段身是一个 SegmentUnitEntity
 *   - 每帧通过 PU132 蠕虫连接算法计算每节段身应该在哪、朝哪个方向
 *   - 管理血量分布（头部和所有段身共享一条血条）
 *   - 处理段身死亡时的分裂/链式合并
 *   - 控制大招期间的特殊行为（如 oppression 开大招时减速）
 *<p>
 * 【在多节单位系统中的角色】
 *   一条完整的虫子 = 1 个 SegmentWormEntity（头部）+ N 个 SegmentUnitEntity（段身）
 *   - 头部是"大脑"：负责 AI、移动、武器、绘制段身
 *   - 段身是"肢体"：位置完全由头部控制，自己不会思考也不会移动
 *   - 玩家选中任意一节（头或段身）都能控制整条虫子
 * <p>
 * 【核心算法原理 —— PU132 蠕虫连接算法】
 *   段身要"跟着头部走"且看起来自然，不能瞬移也不能脱节。PU132 mod 的算法分两步：
 * <p>
 *   第 1 步：速度传播（updateSegmentVLocal）
 *     - 每节段身继承前一段的速度，方向指向"前一段"
 *     - 速度大小 = max(前一段速度, 自己速度, 头部3帧平均速度)
 *     - 这样头部移动时，速度会像波一样传到尾部，产生"拖尾感"
 * <p>
 *   第 2 步：约束修正（updateSegmentsLocal）
 *     - 先按速度移动段身
 *     - 再计算"理想位置"（前一段正后方 segmentSpacing/2 处）
 *     - 用拉回力把段身拉向理想位置（jointStrength 控制硬度）
 *     - 拉回力会向后传播 segmentCast 段，让整条虫子连贯不打结
 * <p>
 *   第 3 步：clampRange 防脱节（工具方法）
 *     - 限制段身真实朝向相对父段不超过 ±segmentRotationRange 度
 *     - 从根源避免段身转超过 90° 导致"脱节"（头和身子分家）
 * <p>
 * 【设计来源】
 *   借鉴 PU132 mod 的 WormDefaultUnit，适配 Mindustry v154.3 原生 API。
 *   不使用 @EntityDef 注解处理器，通过手动注册 classId 绕过 checkEntityMapping 检查。
 * <p>
 * 【v154.3 适配要点】
 *   - 每个自定义 Entity 必须注册唯一 classId 到 EntityMapping.idMap（见 ZEntityRegister）
 *   - mod 贴图在 atlas 中带 modname- 前缀，查找时要双名字兼容
 *   - 玩家队伍默认用 CommandAI 而非 aiController，待机判断需特殊处理
 * <p>
 * 【注意】
 *   本文件已移除反作弊系统（原 PU132 EndWormUnit 的 invTime/immunity/rogueDamageResist/lastHealth）。
 *   如需防秒杀功能，请自行在 damage() 中实现。
 *   触手系统（TentacleAbility/TentaclesBase/VoidPortalBulletType）已移除，本文件无相关引用。
 */
public class SegmentWormEntity extends UnitEntity {

    /** 工厂方法（UnitType.constructor 用这个创建实例） */
    public static SegmentWormEntity create() {
        return new SegmentWormEntity();
    }

    /**
     * 返回注册的 classId（绕过 v154.3 的 checkEntityMapping 检查）
     * ★ 为什么需要这个：v154.3 要求每个自定义 Entity class 有唯一 classId 注册到 EntityMapping.idMap，
     *   不注册会导致 UnitType.init() 抛 ClassCastException。
     *   我们用 ZEntityRegister 统一管理，避免注解处理器。
     */
    @Override
    public int classId() {
        return FedUnitEntity.classId(SegmentWormEntity.class);
    }

    /**
     * ★ 头部碰撞过滤
     * 头部与非相邻段身（index >= 2）碰撞，与相邻段身（index 0,1）不碰撞。
     *
     * 【为什么要这样做】
     *   - 与第 0 段不碰撞：避免头部和最前段身推挤抖动（它们离得近）
     *   - 与第 1 段不碰撞：同样离头部近，容易触发碰撞抖动
     *   - 与第 2+ 段碰撞：防止头部穿过身体（用户反馈"头可以穿过身体"问题）
     */
    @Override
    public boolean collides(mindustry.gen.Hitboxc other) {
        if (other instanceof SegmentUnitEntity seg && seg.head == this) {
            // 头部与非相邻段身碰撞（index >= 2），相邻段身（index 0,1）不碰撞
            return seg.segmentIndex >= 2;
        }
        return super.collides(other);
    }

    /**
     * ★ 判断单位是否处于待机状态（无目标且无命令，应该静止）
     *
     * 【为什么要特殊判断】
     *   v154.3 玩家队伍默认用 CommandAI 而不是 WormAI，所以不能依赖 WormAI.isIdle。
     *   如果不判断待机就清零速度，会导致：
     *   - 玩家给单位下令移动时（target==null 但 targetPos!=null）被误判为待机，速度被清零
     *
     * 【三种 controller 情况】
     *   1. Player（玩家进入单位）：isPlayer()=true，不算待机
     *   2. CommandAI（玩家选中但不进入，玩家队伍默认）：用 hasCommand() 判断，有命令=玩家在指挥移动
     *   3. AIController（敌方/刷怪）：用 target 字段判断，target==null=待机
     *
     * 【手机端兼容】
     *   AIController.target 是 protected 字段，反射在混淆环境下可能失败。
     *   优先用 hasTarget() 方法（v154.3 有），失败则返回 false（保守策略，不静止）。
     */
    public boolean isIdle() {
        if (isPlayer()) return false;
        mindustry.entities.units.UnitController c = controller();
        if (c instanceof mindustry.ai.types.CommandAI cmd) {
            return !cmd.hasCommand();
        }
        // 手机端兼容：AIController.target 反射在混淆环境下可能失败
        // 使用 hasTarget() 方法替代反射，如果没有该方法则返回 false（保守策略，不静止）
        if (c instanceof mindustry.entities.units.AIController ai) {
            try {
                // 优先尝试 hasTarget() 方法（v154.3 AIController 有此方法）
                java.lang.reflect.Method m = ai.getClass().getMethod("hasTarget");
                m.setAccessible(true);
                return !((Boolean) m.invoke(ai));
            } catch (Throwable e1) {
                // fallback：尝试反射 target 字段
                try {
                    java.lang.reflect.Field f = mindustry.entities.units.AIController.class.getDeclaredField("target");
                    f.setAccessible(true);
                    Object t = f.get(ai);
                    return t == null;
                } catch (Throwable e2) {
                    // 手机端混淆环境：无法获取 target，返回 false（保守策略，不静止）
                    // 这样不会因为判断错误而导致单位异常静止
                    return false;
                }
            }
        }
        return false;
    }

    // ==================== 终极技能（Ultimate Skill）====================
    // 仅 SegmentConfig.freezeOnUlt=true 的单位（如 oppression 压迫者）生效
    // devourer/arcnelidia 等不受影响

    /**
     * ★ 大招是否激活（充能期间 + 射击期间）
     *
     * 【什么情况算大招激活】
     *   检查 mounts 中是否有 continuous 武器正在充能（mount.charging）
     *   或正在射击（mount.bullet != null）。
     *
     * 【为什么需要这个】
     *   oppression 压迫者开大招（主激光持续射击）时需要减速移动，
     *   通过这个方法判断"现在是不是在大招期间"。
     */
    public boolean isUltActive() {
        if (mounts == null) return false;
        SegmentConfig cfg = configs.get(type != null ? type.name : "");
        if (cfg == null || !cfg.freezeOnUlt) return false;
        for (mindustry.entities.units.WeaponMount mount : mounts) {
            if (mount.weapon != null && mount.weapon.continuous
                    && (mount.bullet != null || mount.charging)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ★ 大招期间的减速倍率（PU132 OppressionComp.updateLaserSpeed）
     *
     * 【为什么减速而不是完全锁定】
     *   PU132 原版：speedMultiplier *= 0.075f（减速到 7.5%）
     *   完全锁定会让单位看起来很僵硬，减速到 7.5% 让单位还能缓慢移动，更自然。
     *
     * 【生效条件】
     *   - 头部 mounts[0].bullet != null（主激光射击中）或 mount.charging（充能中）
     *   - SegmentConfig.freezeOnUlt=true
     */
    public float ultSpeedMultiplier() {
        if (!isUltActive()) return 1f;
        SegmentConfig cfg = type != null ? configs.get(type.name) : null;
        return cfg != null ? cfg.ultSpeedMultiplier : 0.075f;
    }

    // ==================== 段身数据结构 ====================

    /** 段身列表（顺序：头部后方第一段到最后一段/尾部） */
    public SegmentUnitEntity[] segments = new SegmentUnitEntity[0];
    /** 段身位置缓存（PU132 segments[]，用于物理模拟，与 segments[i].x/y 同步） */
    protected Vec2[] segPositions;
    /** 段身速度（PU132 segmentVelocities[]，让段身有惯性，产生拖尾感） */
    protected Vec2[] segVelocities;
    /** 段身朝向（PU132 segmentUnits[i].rotation，每段独立朝向，用于约束算法） */
    protected float[] segRotations;
    /**
     * 头部移动轨迹点（PU132 老式延迟跟随模式用）
     * 长度 = 段数 * 8，环形数组，pathIndex 是当前写入位置。
     * 当前算法改用速度传播，此数组保留但未使用，供未来扩展。
     */
    protected Vec2[] pathPoints;
    protected int pathIndex = 0;

    // ==================== PU132 连接算法参数 ====================
    // 这些参数控制虫子的"手感"，调参时请参考以下说明

    /**
     * 段身朝向相对父段的最大角度差（度，PU132 angleLimit）
     * - 调小 = 段身更硬（转向幅度小），调大 = 段身更软（转向幅度大）
     * - PU132 原版默认 30f；80f 会导致头折回碰身子
     */
    public float angleLimit = 30f;
    /** 段身间距（PU132 segmentOffset，两节段身中心之间的距离） */
    public float segmentSpacing = 23f;
    /**
     * 段身阻力（PU132 drag，速度传播模式下用）
     * - PU132 原版 0.007f 太小，尾部会抖
     * - 当前项目用 3.0f（arcnelidia 调好的稳定值）
     */
    public float segmentDrag = 0.007f;
    /**
     * 关节强度（PU132 jointStrength，0~1）
     * - 越大 = 段身越快被拉回理想位置 = 越硬
     * - PU132 原版默认 1f（WormComp），WormDefaultUnit 用 0.08f
     * - 我们用 WormComp 式约束，所以默认 1f
     */
    public float jointStrength = 1f;
    /**
     * 拉回力传播段数（PU132 segmentCast）
     * - 每段的拉回力会传播到后面 segmentCast 段，越靠后力越小
     * - 越大 = 段身之间越连贯，不容易打结
     * - PU132 原版默认 4
     */
    public int segmentCast = 4;
    /**
     * 角度物理平滑（PU132 anglePhysicsSmooth，0~1）
     * - 0 = 完全硬限制（clampedAngle），1 = 完全平滑
     * - 越大 = 转向越平滑，但越容易超角度
     * - PU132 原版默认 0f
     */
    public float anglePhysicsSmooth = 0f;
    /**
     * 防止角度漂移（PU132 preventDrifting）
     * - 静止时使用段身自身朝向而非 angleTo，避免 atan2 精度问题导致缓慢漂移
     * - PU132 原版默认 false
     */
    public boolean preventDrifting = false;
    /**
     * 头部偏移（PU132 headOffset，第0段身相对头部的额外偏移）
     * - 正数 = 第0段身离头部更远
     * - PU132 原版默认 0f
     */
    public float headOffset = 0f;

    // ==================== 血量分布参数 ====================

    /** 血量分布速率（PU132 healthDistribution，越大血量分布越快） */
    public float healthDistributionRate = 0.1f;
    /** 血量分布效率（受伤降低，慢慢恢复，PU132 healthDistributionEfficiency） */
    protected float healthDistributionEfficiency = 1f;

    // ==================== 分裂/链式合并参数 ====================

    /**
     * 段身伤害缩放（PU132 segmentDamageScl，splittable 模式下有效）
     * - 段身受伤害时，血量减少 amount × segmentDamageScl
     * - 越大 = 段身越脆，越容易死亡分裂
     * - 默认 6f（PU132 UnityUnitType 默认值），toxobyte 8f，catenapede 12f
     */
    public float segmentDamageScl = 6f;
    /**
     * 弹幕同步范围（PU132 barrageRange）
     * - 当头部被玩家控制且正在射击时，段身在 barrageRange 范围内会复制头部 aimX/aimY 齐射
     * - PU132 默认 150f，devourer 240f
     */
    public float barrageRange = 150f;
    /**
     * 再生间隔（PU132 regenTime，单位 tick，0=不再生）
     * - 每 regenTime tick 长出一节新尾部段身，期间会扣血（health/段数/2）
     * - PU132 toxobyte 原值：15*60f = 900 tick（15秒长一节）
     * - 默认 0：不启用再生
     */
    public float regenTime = 0f;
    /**
     * 段身数上限（PU132 maxSegments，达到上限后停止再生）
     * - toxobyte 原值 25，arcnelidia 不启用再生
     */
    public int maxSegments = 0;
    /** 当前再生计时器（累加 Time.delta，达到 regenTime 后重置） */
    protected float repairTime = 0f;
    /**
     * 是否启用轻微晃动（arcnelidia=true 轻微晃动，toxobyte=false 完全静止）
     * - 借鉴 v154.3 UnitComp.wobble()，但振幅更小（0.02f vs 原版 0.05f）
     */
    public boolean wobbleEnabled = false;
    /**
     * 是否启用分裂（PU132 splittable）
     * - true：段身有独立血量，死亡时虫子分裂
     * - false：段身伤害转移给头部（默认）
     */
    public boolean splittable = false;
    /**
     * 是否启用链式合并（PU132 chainable）
     * - true：两条同类型虫子靠近时合并成更长虫子
     * - false：不合并（默认）
     */
    public boolean chainable = false;
    /** 链式合并扫描计时器（每 5 秒扫描一次附近尾部虫子） */
    protected float chainScanTimer = 0f;
    /** 分裂音效（PU132 默认 Sounds.door） */
    public static arc.audio.Sound splitSound = null;
    /** 链式合并音效（PU132 默认 Sounds.door） */
    public static arc.audio.Sound chainSound = null;

    /** 贴图前缀缓存（type.name + "-"，用于快速查找段身贴图） */
    protected String texturePrefix = null;

    /** 液压装饰（WormDecal），按头部 type.name 查找（仅 oppression 有，其他单位为 null） */
    public static final arc.struct.ObjectMap<String, WormDecal> wormDecals = new arc.struct.ObjectMap<>();

    // ==================== SegmentConfig 段身配置类 ====================

    /**
     * 段身配置类（在 Z_Units.load 中注册，支持多个分段单位）
     *
     * 【为什么要用配置类】
     *   一条虫子需要很多参数（段数、间距、是否分裂、武器分组等），
     *   用配置类把这些参数打包，按 UnitType.name 注册到 configs Map，
     *   头部 add() 时自动根据 type.name 查 Config 创建段身。
     *
     * 【多单位支持】
     *   configs Map 可以注册多个虫子单位：
     *   - "arcnelidia" → 9段，间距22.7，不分裂
     *   - "toxobyte" → 25段，间距16.25，可分裂可合并
     *   - "oppression" → 大招期间锁定移动
     */
    public static class SegmentConfig {
        public final mindustry.type.UnitType segmentType;
        public final int count;
        public final float spacing;
        /** 再生间隔（0=不再生，toxobyte=15*60f） */
        public final float regenTime;
        /** 段身数上限（0=不限制，与 regenTime 配合使用） */
        public final int maxSegments;
        /** 是否启用轻微晃动（arcnelidia=true，toxobyte=false） */
        public final boolean wobble;
        /**
         * 是否启用分裂（PU132 splittable）
         * - true：段身独立承受伤害，中间段身死亡时后半段变成新虫子
         * - false：段身伤害转移给头部（默认）
         */
        public final boolean splittable;
        /**
         * 是否启用链式合并（PU132 chainable）
         * - true：头部每 5 秒扫描附近尾部虫子，合并成更长虫子
         * - false：不合并（默认）
         */
        public final boolean chainable;
        /**
         * 段身朝向相对父段的最大角度差（度，PU132 angleLimit）
         * - 调小 = 段身更硬（转向幅度小），调大 = 段身更软（转向幅度大）
         * - PU132 toxobyte 30f，arcnelidia 默认 25f
         */
        public final float angleLimit;
        /**
         * 段身伤害缩放（PU132 segmentDamageScl，splittable 模式下有效）
         * - 越大 = 段身越脆，越容易死亡分裂
         * - toxobyte 原版 8f，catenapede 原版 12f
         */
        public final float segmentDamageScl;
        /** 血量分布速率（PU132 healthDistribution），默认 0.1f，catenapede 原版 0.15f */
        public final float healthDistribution;
        /** 关节强度（PU132 jointStrength，0~1），越大 = 段身越快被拉回理想位置 = 越硬，默认 1f */
        public final float jointStrength;
        /** 拉回力传播段数（PU132 segmentCast），越大 = 段身越连贯，默认 4 */
        public final int segmentCast;
        /** 角度物理平滑（PU132 anglePhysicsSmooth，0~1），默认 0f（硬限制） */
        public final float anglePhysicsSmooth;
        /** 防止角度漂移（PU132 preventDrifting），默认 false */
        public final boolean preventDrifting;
        /** 头部偏移（PU132 headOffset），默认 0f */
        public final float headOffset;
        /** 弹幕同步范围（PU132 barrageRange），默认 150f，devourer 240f */
        public final float barrageRange;
        /**
         * 段身武器分组大小（PU132 segmentWeapons 的每组武器数）
         * - 0 或负数 = 不分组（所有段身绘制所有武器）
         * - oppression: 2（6个武器分3组，每组2个，尾部空组）
         * - devourer: 0（不分组，所有段身绘制所有武器）
         * - arcnelidia/toxobyte/catenapede: 0（单武器）
         */
        public final int segmentWeaponGroupSize;
        /**
         * 大招期间是否锁定移动和旋转（只有 oppression=true）
         * - true：当 continuous 武器正在充能或射击时，清零 vel 并锁定 rotation
         * - false：devourer/arcnelidia 等不受影响，开大招时仍可移动
         */
        public final boolean freezeOnUlt;
        /**
         * 每秒回血（0=不回血）
         * 压迫者: 250，吞噬者: 120，电弧虫: 10，吸血虫: 10，toxobyte: 5
         */
        public float healPerSecond;
        /**
         * 受到伤害倍率（1.0=正常，0.9=减伤10%，0.8=减伤20%）
         * ★ 这是游戏平衡配置，不是反作弊
         * 吞噬者: 0.9，压迫者: 0.8
         */
        public float damageMultiplier = 1f;
        /** 大招期间速度倍率（1.0=正常，0.075=只剩7.5%），压迫者: 0.075 */
        public float ultSpeedMultiplier = 0.075f;

        // ===== 构造函数（重载链，最终都调到最全的那个）=====

        public SegmentConfig(mindustry.type.UnitType t, int c, float s) {
            this(t, c, s, 0f, 0, false, false, false);
        }
        public SegmentConfig(mindustry.type.UnitType t, int c, float s, float regenTime, int maxSegments) {
            this(t, c, s, regenTime, maxSegments, false, false, false);
        }
        public SegmentConfig(mindustry.type.UnitType t, int c, float s, float regenTime, int maxSegments, boolean wobble) {
            this(t, c, s, regenTime, maxSegments, wobble, false, false);
        }
        public SegmentConfig(mindustry.type.UnitType t, int c, float s, float regenTime, int maxSegments, boolean wobble, boolean splittable, boolean chainable) {
            this(t, c, s, regenTime, maxSegments, wobble, splittable, chainable, 30f);
        }
        public SegmentConfig(mindustry.type.UnitType t, int c, float s, float regenTime, int maxSegments, boolean wobble, boolean splittable, boolean chainable, float angleLimit) {
            this(t, c, s, regenTime, maxSegments, wobble, splittable, chainable, angleLimit, 6f);
        }
        public SegmentConfig(mindustry.type.UnitType t, int c, float s, float regenTime, int maxSegments, boolean wobble, boolean splittable, boolean chainable, float angleLimit, float segmentDamageScl) {
            this(t, c, s, regenTime, maxSegments, wobble, splittable, chainable, angleLimit, segmentDamageScl, 0.1f);
        }
        public SegmentConfig(mindustry.type.UnitType t, int c, float s, float regenTime, int maxSegments, boolean wobble, boolean splittable, boolean chainable, float angleLimit, float segmentDamageScl, float healthDistribution) {
            this(t, c, s, regenTime, maxSegments, wobble, splittable, chainable, angleLimit, segmentDamageScl, healthDistribution, 1f);
        }
        public SegmentConfig(mindustry.type.UnitType t, int c, float s, float regenTime, int maxSegments, boolean wobble, boolean splittable, boolean chainable, float angleLimit, float segmentDamageScl, float healthDistribution, float jointStrength) {
            this(t, c, s, regenTime, maxSegments, wobble, splittable, chainable, angleLimit, segmentDamageScl, healthDistribution, jointStrength, 4);
        }
        public SegmentConfig(mindustry.type.UnitType t, int c, float s, float regenTime, int maxSegments, boolean wobble, boolean splittable, boolean chainable, float angleLimit, float segmentDamageScl, float healthDistribution, float jointStrength, int segmentCast) {
            this(t, c, s, regenTime, maxSegments, wobble, splittable, chainable, angleLimit, segmentDamageScl, healthDistribution, jointStrength, segmentCast, 0f);
        }
        public SegmentConfig(mindustry.type.UnitType t, int c, float s, float regenTime, int maxSegments, boolean wobble, boolean splittable, boolean chainable, float angleLimit, float segmentDamageScl, float healthDistribution, float jointStrength, int segmentCast, float anglePhysicsSmooth) {
            this(t, c, s, regenTime, maxSegments, wobble, splittable, chainable, angleLimit, segmentDamageScl, healthDistribution, jointStrength, segmentCast, anglePhysicsSmooth, false);
        }
        public SegmentConfig(mindustry.type.UnitType t, int c, float s, float regenTime, int maxSegments, boolean wobble, boolean splittable, boolean chainable, float angleLimit, float segmentDamageScl, float healthDistribution, float jointStrength, int segmentCast, float anglePhysicsSmooth, boolean preventDrifting) {
            this(t, c, s, regenTime, maxSegments, wobble, splittable, chainable, angleLimit, segmentDamageScl, healthDistribution, jointStrength, segmentCast, anglePhysicsSmooth, preventDrifting, 0f);
        }
        public SegmentConfig(mindustry.type.UnitType t, int c, float s, float regenTime, int maxSegments, boolean wobble, boolean splittable, boolean chainable, float angleLimit, float segmentDamageScl, float healthDistribution, float jointStrength, int segmentCast, float anglePhysicsSmooth, boolean preventDrifting, float headOffset) {
            this(t, c, s, regenTime, maxSegments, wobble, splittable, chainable, angleLimit, segmentDamageScl, healthDistribution, jointStrength, segmentCast, anglePhysicsSmooth, preventDrifting, headOffset, 150f);
        }
        public SegmentConfig(mindustry.type.UnitType t, int c, float s, float regenTime, int maxSegments, boolean wobble, boolean splittable, boolean chainable, float angleLimit, float segmentDamageScl, float healthDistribution, float jointStrength, int segmentCast, float anglePhysicsSmooth, boolean preventDrifting, float headOffset, float barrageRange) {
            this(t, c, s, regenTime, maxSegments, wobble, splittable, chainable, angleLimit, segmentDamageScl, healthDistribution, jointStrength, segmentCast, anglePhysicsSmooth, preventDrifting, headOffset, barrageRange, 0);
        }
        public SegmentConfig(mindustry.type.UnitType t, int c, float s, float regenTime, int maxSegments, boolean wobble, boolean splittable, boolean chainable, float angleLimit, float segmentDamageScl, float healthDistribution, float jointStrength, int segmentCast, float anglePhysicsSmooth, boolean preventDrifting, float headOffset, float barrageRange, int segmentWeaponGroupSize) {
            this(t, c, s, regenTime, maxSegments, wobble, splittable, chainable, angleLimit, segmentDamageScl, healthDistribution, jointStrength, segmentCast, anglePhysicsSmooth, preventDrifting, headOffset, barrageRange, segmentWeaponGroupSize, false);
        }
        public SegmentConfig(mindustry.type.UnitType t, int c, float s, float regenTime, int maxSegments, boolean wobble, boolean splittable, boolean chainable, float angleLimit, float segmentDamageScl, float healthDistribution, float jointStrength, int segmentCast, float anglePhysicsSmooth, boolean preventDrifting, float headOffset, float barrageRange, int segmentWeaponGroupSize, boolean freezeOnUlt) {
            segmentType = t; count = c; spacing = s;
            this.regenTime = regenTime; this.maxSegments = maxSegments;
            this.wobble = wobble; this.splittable = splittable; this.chainable = chainable;
            this.angleLimit = angleLimit;
            this.segmentDamageScl = segmentDamageScl;
            this.healthDistribution = healthDistribution;
            this.jointStrength = jointStrength;
            this.segmentCast = segmentCast;
            this.anglePhysicsSmooth = anglePhysicsSmooth;
            this.preventDrifting = preventDrifting;
            this.headOffset = headOffset;
            this.barrageRange = barrageRange;
            this.segmentWeaponGroupSize = segmentWeaponGroupSize;
            this.freezeOnUlt = freezeOnUlt;
            this.healPerSecond = 0f;
        }
    }

    /**
     * 按 UnitType.name 注册的段身配置（key = 头部名字，如 "arcnelidia" / "toxobyte"）
     * ★ 注意：v154.3 mod 单位 type.name 可能带 mod 前缀（如 "create-toxobyte"），
     *   configs.put 时用 toxobyte.name 作为 key 可避免此问题。
     */
    public static final java.util.Map<String, SegmentConfig> configs = new java.util.HashMap<>();

    /** 旧静态字段（向后兼容，优先用 configs Map） */
    public static mindustry.type.UnitType defaultSegmentType = null;
    public static int defaultSegmentCount = 5;
    public static float defaultSegmentSpacing = 23f;

    /** 段身是否已创建（兜底：add() 没触发就在 update() 里创建） */
    private boolean segmentsCreated = false;

    /**
     * ★ 存档读入的段身数量（-1 = 未读档，0+ = 按此数量重建段身）
     * PU132 WormDefaultUnit.read/write：保存 segmentUnits.length 防止读档时重建完整虫子
     * v158 调用顺序：read() → add()，所以 add() 时可读取此值
     */
    private int savedSegmentCount = -1;

    /** 上一帧速度（PU132 lastVelocityC，用于段身速度平滑的 3 帧平均） */
    protected final Vec2 lastVelocityC = new Vec2();
    /** 上上帧速度（PU132 lastVelocityD，用于 3 帧平均） */
    protected final Vec2 lastVelocityD = new Vec2();

    // ==================== 主更新逻辑 ====================

    /**
     * ★ 每帧更新（虫子的核心逻辑都在这里）
     *
     * 【执行顺序】
     *   1. 大招减速（在 super.update() 前预减速 vel）
     *   2. 保存速度历史（用于段身 3 帧平均）
     *   3. super.update()（AI、移动、武器、状态效果等）
     *   4. 待机静止检查（super.update() 后清零 vel）
     *   5. 大招减速 2（super.update() 后再次减速 vel + 旋转）
     *   6. 兜底创建段身（如果 add() 没创建）
     *   7. 初始化段身数组
     *   8. ★ PU132 蠕虫连接算法（速度传播 + 约束修正）
     *   9. 血量分布
     *   10. 再生（长出新段）
     *   11. 轻微晃动
     *   12. 链式合并扫描
     */
    @Override
    public void update() {
        // ★ 大招期间减速移动（PU132 OppressionComp: speedMultiplier *= 0.075f）
        // 在 super.update() 前减速 vel，防止物理系统加速
        float ultScl = ultSpeedMultiplier();
        if (ultScl < 1f) {
            vel.scl(ultScl);
        }

        // PU132 WormDefaultUnit.update L71-72：保存速度历史（用于 updateSegmentVLocal 3 帧平均）
        lastVelocityD.set(lastVelocityC);
        lastVelocityC.set(vel);

        super.update();

        // ★ 待机静止：super.update() 后检查
        // 【为什么在 super.update() 后清零而不是前】
        //   super.update() 前不清零 vel，否则会抵消 AI 上一帧设置的速度，导致单位永远无法有效加速移动
        //   只在 super.update() 后确认真的待机时才清零 vel
        if (isIdle()) {
            vel.setZero();
        }

        // ★ 大招期间减速 2：super.update() 后再次减速 vel + 减速旋转
        // 【为什么这里再减速一次】
        //   speedMultiplier 在 super.update() 中由 StatusComp 重置为 1f，所以这里设置才有效
        //   PU132 原版：speedMultiplier *= 0.075f，影响 rotateMove 中的旋转速度
        if (ultScl < 1f) {
            vel.scl(ultScl);
            speedMultiplier *= ultScl;
        }

        // ★ 兜底：如果 add() 没创建段身，在第一次 update() 时创建
        if (!segmentsCreated) {
            SegmentConfig cfg = type != null ? configs.get(type.name) : null;
            if (cfg != null) {
                try {
                    // 缓存贴图前缀（兜底路径）
                    if (type != null && texturePrefix == null) texturePrefix = type.name + "-";
                    applyConfig(cfg);
                    // ★ 读档时用 savedSegmentCount，新建时用 cfg.count（PU132 addSegments=false 模式）
                    int count = savedSegmentCount >= 0 ? savedSegmentCount : cfg.count;
                    createSegments(count, cfg.segmentType);
                    segmentsCreated = true;
                } catch (Throwable t) {
                    Log.err("[头部] 段身创建失败", t);
                    segmentsCreated = true;
                }
            } else if (defaultSegmentType != null) {
                // 旧路径（向后兼容）
                try {
                    segmentSpacing = defaultSegmentSpacing;
                    int count = savedSegmentCount >= 0 ? savedSegmentCount : defaultSegmentCount;
                    createSegments(count, defaultSegmentType);
                    segmentsCreated = true;
                } catch (Throwable t) {
                    Log.err("[头部] 旧路径创建失败", t);
                    segmentsCreated = true;
                }
            }
        }

        // 初始化位置/速度/朝向数组
        if (segPositions == null || segPositions.length != segments.length) {
            segPositions = new Vec2[segments.length];
            segVelocities = new Vec2[segments.length];
            segRotations = new float[segments.length];
            for (int i = 0; i < segments.length; i++) {
                segPositions[i] = new Vec2(x, y);
                segVelocities[i] = new Vec2();
                segRotations[i] = rotation;
            }
        }

        // ★★★ PU132 蠕虫连接算法 ★★★
        // 核心流程（两步走）：
        //   1. updateSegmentVLocal()：速度传播 —— 每段继承前一段速度，产生拖尾感
        //   2. updateSegmentsLocal()：约束修正 —— 先按速度移动，再拉回理想位置
        // 这样段身会沿着头部走过的路径移动，有自然的延迟和惯性效果

        // 第 1 步：速度传播 —— 让段身继承前一段速度
        updateSegmentVLocal(lastVelocityC);

        // 第 2 步：约束修正 —— 按速度移动后，将段身拉回理想位置
        updateSegmentsLocal();

        // 血量分布效率恢复（PU132 WormDefaultUnit.update L74）
        healthDistributionEfficiency = Mathf.clamp(healthDistributionEfficiency + (Time.delta / 160f));

        // 血量分布：每段单独计算（3 邻居局部平均，PU132 WormDefaultUnit.distributeHealth L179-202）
        // ★ splittable=true 时段身有独立血量，不进行血量分布（PU132 WormComp.update L249-258）
        if (healthDistributionRate > 0 && !splittable) {
            for (int i = 0; i < segments.length; i++) {
                distributeHealth(i);
            }
        }

        // ★ 再生（PU132 WormDefaultUnit.update L81-94，regenTime > 0 时启用）
        if (regenAvailable()) {
            repairTime += Time.delta;
            if (repairTime >= regenTime) {
                // 扣血 + 长出新段
                float damage = (health / segments.length) / 2f;
                damage(damage);
                addSegment();
                repairTime = 0f;
            }
        }

        // ★ 轻微晃动（arcnelidia 启用，toxobyte 不启用）
        // 借鉴 v154.3 UnitComp.wobble()：振幅 0.05f，这里用 0.02f 更轻微
        if (wobbleEnabled) {
            x += Mathf.sin(Time.time + (id % 10) * 12f, 25f, 0.02f) * Time.delta * elevation;
            y += Mathf.cos(Time.time + (id % 10) * 12f, 25f, 0.02f) * Time.delta * elevation;
        }

        // ★ 链式合并扫描（PU132 WormComp.updatePost L341-350，每 5 秒扫描一次）
        if (chainable && segments.length > 0) {
            chainScanTimer += Time.delta;
            if (chainScanTimer >= 300f) {  // 5 秒
                chainScanTimer = 0f;
                tryChainMerge();
            }
        }
    }

    /**
     * 从 SegmentConfig 应用参数到头部字段
     * 抽取公共逻辑，避免 add() 和 update() 重复代码
     */
    private void applyConfig(SegmentConfig cfg) {
        segmentSpacing = cfg.spacing;
        regenTime = cfg.regenTime;
        maxSegments = cfg.maxSegments;
        wobbleEnabled = cfg.wobble;
        splittable = cfg.splittable;
        chainable = cfg.chainable;
        angleLimit = cfg.angleLimit;
        segmentDamageScl = cfg.segmentDamageScl;
        healthDistributionRate = cfg.healthDistribution;
        jointStrength = cfg.jointStrength;
        segmentCast = cfg.segmentCast;
        anglePhysicsSmooth = cfg.anglePhysicsSmooth;
        preventDrifting = cfg.preventDrifting;
        headOffset = cfg.headOffset;
        barrageRange = cfg.barrageRange;
    }

    /**
     * 是否可再生（PU132 WormDefaultUnit.regenAvailable L97-99）
     * 需 regenTime > 0 且段数未达上限
     */
    public boolean regenAvailable() {
        return regenTime > 0f && segments.length < maxSegments;
    }

    /**
     * ★ 添加新段身（PU132 WormDefaultUnit.addSegment L336-368，简化版）
     * 在尾部追加一个新段身，扩展所有数组。
     *
     * 【执行步骤】
     *   1. 保存旧数组引用
     *   2. 创建新数组（长度+1）
     *   3. 复制旧数据到新数组
     *   4. 在尾部创建新 SegmentUnitEntity
     *   5. 旧尾部 isTail=false，新段身 isTail=true
     */
    public void addSegment() {
        if (segments.length <= 0) return;
        int oldLen = segments.length;
        int newLen = oldLen + 1;

        SegmentUnitEntity[] oldSegs = segments;
        Vec2[] oldPos = segPositions;
        Vec2[] oldVel = segVelocities;
        float[] oldRot = segRotations;

        segments = new SegmentUnitEntity[newLen];
        segPositions = new Vec2[newLen];
        segVelocities = new Vec2[newLen];
        segRotations = new float[newLen];

        for (int i = 0; i < oldLen; i++) {
            segments[i] = oldSegs[i];
            segPositions[i] = oldPos[i];
            segVelocities[i] = oldVel[i];
            segRotations[i] = oldRot[i];
        }

        // 旧尾部不再是尾部
        if (segments[oldLen - 1] != null) {
            segments[oldLen - 1].isTail = false;
        }

        // 创建新段身（与旧尾部同类型）
        mindustry.type.UnitType segType = segments[oldLen - 1] != null ? segments[oldLen - 1].type : defaultSegmentType;
        SegmentUnitEntity newSeg = (SegmentUnitEntity) segType.create(team);

        // 新段身位置 = 旧尾部正后方 segmentSpacing 处
        Vec2 oldTailPos = oldPos[oldLen - 1];
        float oldTailRot = oldRot[oldLen - 1];
        Vec2 newPos = new Vec2();
        newPos.trns(oldTailRot + 180f, segmentSpacing).add(oldTailPos);

        newSeg.set(newPos.x, newPos.y);
        newSeg.rotation = oldTailRot;
        newSeg.head = this;
        newSeg.segmentIndex = oldLen;
        newSeg.isTail = true;
        newSeg.texturePrefix = type.name + "-";
        newSeg.elevation = elevation;
        newSeg.health = health;
        newSeg.maxHealth = maxHealth;
        newSeg.dead = false;
        newSeg.add();

        segPositions[oldLen] = newPos;
        segVelocities[oldLen] = new Vec2(segVelocities[oldLen - 1]);
        segRotations[oldLen] = oldTailRot;
        segments[oldLen] = newSeg;
    }

    /**
     * ★ 受到伤害
     *
     * 【本方法做的事】
     *   1. 应用 SegmentConfig.damageMultiplier 减伤（游戏平衡配置，非反作弊）
     *   2. 调用 super.damage() 扣血
     *   3. 降低血量分布效率（受伤后短时间内血量分布变慢）
     *
     * 【已移除】
     *   原反作弊系统（invTime 无敌帧、immunity 抗性、rogueDamageResist 流氓伤害抗性、
     *   单次最大伤害限制、抗性递增）已全部移除。
     *   如需防秒杀功能，请自行在此方法中实现。
     */
    @Override
    public void damage(float amount) {
        // ★ 伤害减免（SegmentConfig.damageMultiplier，非反作弊，是游戏平衡配置）
        SegmentConfig cfg = type != null ? configs.get(type.name) : null;
        if (cfg != null && cfg.damageMultiplier != 1f) {
            amount *= cfg.damageMultiplier;
        }

        // 扣血
        super.damage(amount);

        // 受伤降低血量分布效率（短时间内段身血量分布变慢）
        healthDistributionEfficiency = Mathf.clamp(healthDistributionEfficiency - (amount / 15f));
    }

    /**
     * ★ 死亡处理
     * 【已移除】原反作弊的"拒绝死亡"逻辑（lastHealth > 100f 时不死）已移除。
     * 现在直接调用 super.kill() 正常死亡。
     */
    @Override
    public void kill() {
        super.kill();
    }

    /**
     * ★ 销毁处理（死亡时调用）
     * 销毁所有段身，每节段身位置触发爆炸效果 + 震屏 + 死亡音效。
     * （借鉴 PU132 WormDefaultUnit.destroy L234-267，简化版）
     *
     * 【已移除】原反作弊的"拒绝销毁"逻辑（lastHealth > 100f 时不销毁）已移除。
     */
    @Override
    public void destroy() {
        super.destroy();
        // 销毁所有段身
        for (SegmentUnitEntity seg : segments) {
            if (seg == null || !seg.isAdded()) continue;
            seg.head = null;
            float shake = seg.hitSize / 3f;
            Fx.explosion.at(seg);
            Effect.shake(shake, shake, seg);
            type.deathSound.at(seg);
            seg.remove();
        }
    }

    /**
     * ★ 存档写入（PU132 WormDefaultUnit.write L462-478 简化版）
     *
     * 【为什么只保存段身数量】
     *   PU132 原版完整保存每段位置/朝向/类型/血量，v158 简化为只保存段身数量：
     *   - 读档时按保存数量 createSegments() 重建段身（位置由算法计算）
     *   - 这样修复"读档后段身重置成完整"的问题：
     *     原本3个头无段身 → 保存 segments.length=0 → 读档创建0段身 → 不重建
     *
     * 【v158 调用顺序】read() → add() → update()
     */
    @Override
    public void write(Writes write) {
        super.write(write);
        // 写出当前段身数量
        write.i(segments.length);
        // 写出 splittable 标志（PU132 原版，用于未来扩展保存段身独立血量）
        write.bool(splittable);
        // 写出再生计时器（PU132 原版）
        write.f(repairTime);
    }

    /**
     * ★ 存档读取（PU132 WormDefaultUnit.read L418-458 简化版）
     *
     * 【v158 注意事项】
     *   v158 UnitEntity.read(Reads) 不带 revision 参数（与 Building.read 不同）
     *   v158 调用顺序：read() → add() → update()
     *   - read() 中读取 savedSegmentCount，但 type 此时可能未设置（TypeIO.readUnit 在 super.read 中调用）
     *   - add() 中根据 savedSegmentCount 创建对应数量段身
     */
    @Override
    public void read(Reads read) {
        super.read(read);
        savedSegmentCount = read.i();
        boolean savedSplittable = read.bool();
        splittable = savedSplittable;
        repairTime = read.f();
    }

    /**
     * 获取段身（index=-1 表示头部自己，PU132 WormDefaultUnit.getSegment L204-208）
     * - index < 0：返回头部自己
     * - index >= segments.length：返回 null
     * - 其他：返回 segments[index]
     */
    protected Unit getSegment(int index) {
        if (index < 0) return this;
        if (index >= segments.length) return null;
        return segments[index];
    }

    /**
     * ★ 3 邻居局部血量平均（PU132 WormDefaultUnit.distributeHealth L179-202）
     *
     * 【为什么要血量分布】
     *   头部和所有段身"共用一条血条"。当某节受到伤害时，血量会慢慢平均到邻居，
     *   这样鼠标悬停任意段身看到的血量都差不多，整条虫子像一个整体。
     *
     * 【算法】
     *   对 index 段身，取（index-1, index, index+1）三段做平均：
     *   1. 累加三段的 health 和 maxHealth
     *   2. 计算平均值 mHealth / mMaxHealth
     *   3. 用 Mathf.lerpDelta 把每段血量向平均值靠拢
     *   4. healthDistributionRate 控制靠拢速度，healthDistributionEfficiency 受伤后会降低
     *
     * 【index=-1 的含义】
     *   index=-1 时表示头部自己（与第 0 段一起平均）
     */
    protected void distributeHealth(int index) {
        if (dead || health <= 0f) return;  // 头部已死亡时不进行血量分布
        int idx = 0;
        float mHealth = 0f;
        float mMaxHealth = 0f;
        for (int i = -1; i <= 1; i++) {
            Unit seg = getSegment(index + i);
            if (seg == null) break;
            mHealth += seg.health;
            mMaxHealth += seg.maxHealth;
            idx++;
        }
        if (idx == 0) return;
        mMaxHealth /= idx;
        mHealth /= idx;
        for (int i = -1; i <= 1; i++) {
            Unit seg = getSegment(index + i);
            if (seg == null) break;
            if (!Mathf.equal(seg.health, mHealth, 0.001f)) {
                seg.health = Mathf.lerpDelta(seg.health, mHealth, healthDistributionRate * healthDistributionEfficiency);
            }
            if (!Mathf.equal(seg.maxHealth, mMaxHealth, 0.001f)) {
                seg.maxHealth = Mathf.lerpDelta(seg.maxHealth, mMaxHealth, healthDistributionRate * healthDistributionEfficiency);
            }
        }
    }

    // ==================== PU132 角度工具方法 ====================

    /**
     * ★ 限制角度到 relative ± range 范围内（v154.3 Angles.clampRange 等价实现）
     *
     * 【为什么需要这个】
     *   直接限制段身真实朝向相对父段，从根源避免超过 90° 脱节。
     *   PU132 的 clampedAngle 限制的是"向量角度"（seg.angleTo(ideal)），
     *   不是真实朝向，所以段身真实朝向仍可能超过 90° 导致脱节。
     *   clampRange 直接限制真实朝向，更可靠。
     *
     * 【参数说明】
     *   - angle：要限制的角度（段身真实朝向）
     *   - relative：参考角度（父段朝向）
     *   - range：允许的最大角度差（度）
     */
    protected static float clampRange(float angle, float relative, float range) {
        if (range >= 180f) return angle;
        float diff = angleDistSigned(angle, relative);
        if (Math.abs(diff) > range) {
            float target = diff > 0 ? relative + range : relative - range;
            return target % 360f;
        }
        return angle;
    }

    /**
     * ★★★ PU132 蠕虫连接算法 —— 第 1 步：速度传播 ★★★
     * （PU132 WormDefaultUnit.updateSegmentVLocal L101-124）
     *
     * 【为什么要速度传播】
     *   如果段身只是"被拉回理想位置"，移动会很僵硬（像弹簧）。
     *   速度传播让每段继承前一段的速度，头部移动时速度像波一样传到尾部，
     *   产生自然的"拖尾感"——头部走了，尾巴跟着甩过来。
     *
     * 【算法步骤（每段）】
     *   1. 计算方向：段身指向前一段（i=0 时指向头部）
     *   2. 计算速度大小：取 max(前一段速度, 自己速度, 头部3帧平均速度)
     *      - 头部3帧平均：避免头部瞬时速度波动导致段身抖动
     *   3. 把速度加到段身的 segVelocities[i]
     *   4. 速度衰减：segV.scl(1 - segmentDrag * delta)
     *      - 防止 segVelocities 无限增长导致抖动
     *
     * 【为什么不同步到段身实体 vel】
     *   VelComp.update() 会根据 vel 移动段身位置，然后 updateSegmentsLocal 又重置位置，
     *   造成每帧抖动。所以 segVelocities 是内部物理模拟用的，段身实体 vel 保持为零。
     */
    protected void updateSegmentVLocal(Vec2 vec) {
        int len = segments.length;
        for (int i = 0; i < len; i++) {
            Vec2 seg = segPositions[i];
            Vec2 segV = segVelocities[i];
            segV.limit(type.speed);

            // 方向：段身指向前一段（i=0 时指向头部）
            float angleB = i != 0
                    ? Angles.angle(seg.x, seg.y, segPositions[i - 1].x, segPositions[i - 1].y)
                    : Angles.angle(seg.x, seg.y, x, y);
            // 速度大小：取前一段速度（i=0 时取头部上一帧速度）
            float velocity = i != 0 ? segVelocities[i - 1].len() : vec.len();

            // 头部 3 帧速度平均（避免瞬时速度波动导致段身抖动）
            Tmp.v1.set(vel).add(vec).add(lastVelocityD).scl(1f / 3f);

            // 真实速度 = 三者最大值
            float trueVel = Math.max(Math.max(velocity, segV.len()), Tmp.v1.len());
            Tmp.v1.trns(angleB, trueVel);
            segV.add(Tmp.v1);
            segV.setLength(trueVel);

            // ★ 不同步到段身实体 vel
            // 原因：VelComp.update() 会根据 vel 移动段身位置，
            //   然后 updateSegmentsLocal 又重置位置，造成每帧抖动
            // segVelocities 是内部物理模拟用的，段身实体 vel 保持为零
            // segments[i].vel.set(segV);

            // ★ 速度衰减（PU132 WormDefaultUnit.updateSegmentsLocal L142/L160）
            //   防止 segVelocities 无限增长导致抖动
            segV.scl(Mathf.clamp(1f - (segmentDrag * Time.delta)));
        }
    }

    /**
     * ★★★ PU132 蠕虫连接算法 —— 第 2 步：约束修正 ★★★
     * （完全照搬 PU132 WormComp.updatePost 算法 L299-351）
     *
     * 【为什么要约束修正】
     *   速度传播只让段身"有惯性"，但段身可能偏离理想位置越来越远。
     *   约束修正把段身拉回"理想位置"（前一段正后方 segmentSpacing/2 处），
     *   让虫子保持连贯的链条形状。
     *
     * 【PU132 原版核心流程（每段）】
     *   Step 1：计算理想位置 = 上一段后方（segmentOffset/2 + offset）处
     *   Step 2：计算 angTo（段身指向理想位置的角度）
     *           - preventDrifting 且静止时用自身朝向，避免 atan2 精度问题
     *   Step 3：角度平滑（rotation = angTo - 差值 * (1 - anglePhysicsSmooth)）
     *   Step 4：段身沿自身朝向移动 = 上一段的 deltaLen
     *   Step 5：计算拉回向量 = (段身位置 + 段身朝向*segmentOffset/2) - 理想位置
     *           - 拉回力 = 向量 * jointStrength * Time.delta
     *   Step 6：拉回力传播到后面 segmentCast 段（scl = cast / segmentCast，越靠前力越大）
     *   Step 7：同步到 Vec2 位置（用于 draw 等）
     *   Step 8：更新 last 为当前段，继续下一段
     *
     * 【关键参数】
     *   - jointStrength：拉回力强度，越大段身越硬
     *   - segmentCast：拉回力传播段数，越大虫子越连贯
     *   - angleLimit：段身朝向相对父段的最大角度差
     */
    protected void updateSegmentsLocal() {
        float segmentOffset = segmentSpacing / 2f;
        int len = segments.length;
        if (len == 0) return;

        // PU132：last = 头部，从头部开始遍历
        Unit last = this;

        for (int i = 0; i < len; i++) {
            Vec2 seg = segPositions[i];
            SegmentUnitEntity segU = segments[i];

            // ★ Step 1：计算理想位置（上一段后方 segmentOffset + offset 处）
            // 头部的第0段身有额外 headOffset 偏移，其他段无偏移
            float offset = (last == this) ? headOffset : 0f;
            Tmp.v1.trns(last.rotation + 180f, segmentOffset + offset).add(last);

            // ★ Step 2：计算 angTo（段身指向理想位置的角度）
            // PU132：preventDrifting 且静止时用自身朝向，避免 atan2 精度问题导致漂移
            float rdx = segU.deltaX - last.deltaX;
            float rdy = segU.deltaY - last.deltaY;
            float angTo;
            if (!preventDrifting || (last.deltaLen() > 0.001f && (rdx * rdx) + (rdy * rdy) > 0.00001f)) {
                angTo = Angles.angle(segU.x, segU.y, Tmp.v1.x, Tmp.v1.y);
            } else {
                angTo = segU.rotation;
            }

            // ★ Step 3：角度平滑（PU132 原版公式）
            // rotation = angTo - (差值 * (1 - anglePhysicsSmooth))
            // anglePhysicsSmooth=0 时是硬限制，=1 时完全平滑
            float angleDiff = angleDistSigned(angTo, last.rotation, angleLimit);
            segU.rotation = angTo - (angleDiff * (1f - anglePhysicsSmooth));

            // ★ Step 4：段身沿自身朝向移动 = 上一段的 deltaLen（PU132 原版）
            // 这样段身会跟着前一段的移动量走，保持间距
            Tmp.v3.trns(segU.rotation, last.deltaLen());
            segU.trns(Tmp.v3.x, Tmp.v3.y);
            seg.set(segU.x, segU.y);

            // ★ Step 5：计算拉回向量（PU132 原版）
            // 拉回向量 = (段身前方 segmentOffset 处) - 理想位置
            // 用 jointStrength 控制拉回力强度
            Tmp.v2.trns(segU.rotation, segmentOffset).add(seg).sub(Tmp.v1);
            Tmp.v2.scl(Mathf.clamp(jointStrength * Time.delta));

            // ★ Step 6：拉回力传播到后面 segmentCast 段（PU132 原版逻辑，适配数组）
            // scl = cast / segmentCast → 越靠前力越大，越靠后力越小
            // 这样整条虫子会连贯地被拉回，不会只有一段被拉
            int cast = segmentCast;
            int idx = i;
            while (cast > 0 && idx < len) {
                float scl = cast / (float) segmentCast;
                segments[idx].set(segments[idx].x - (Tmp.v2.x * scl), segments[idx].y - (Tmp.v2.y * scl));
                segments[idx].updateLastPosition();
                segPositions[idx].set(segments[idx].x, segments[idx].y);
                idx++;
                cast--;
            }

            // ★ Step 7：同步到 Vec2 位置（用于 draw 等）
            seg.set(segU.x, segU.y);

            // 血量分布（在约束修正后做，保证位置已更新）
            if (healthDistributionRate > 0) distributeHealth(i);

            // ★ Step 8：更新 last 为当前段，继续下一段
            last = segU;
        }
    }

    // ==================== PU132 角度工具方法（Utils.java）====================

    /**
     * 带符号角度差（PU132 Utils.angleDistSigned 第177行）
     * 返回 a 相对 b 的角度差，范围 -180~180
     *
     * 【为什么需要带符号】
     *   普通角度差只返回 0~180，无法判断 a 在 b 左边还是右边。
     *   带符号角度差可以知道旋转方向：正值=顺时针，负值=逆时针。
     */
    private static float angleDistSigned(float a, float b) {
        a += 360f;
        a %= 360f;
        b += 360f;
        b %= 360f;
        float d = Math.abs(a - b) % 360f;
        int sign = (a - b >= 0f && a - b <= 180f) || (a - b <= -180f && a - b >= -360f) ? 1 : -1;
        return (d > 180f ? 360f - d : d) * sign;
    }

    /**
     * 带符号角度差，超过 start 才返回差值（PU132 Utils.angleDistSigned 第187行）
     * 用于头部/前一段朝向调整：超过 angleLimit 才转
     *
     * 【为什么超过 start 才返回】
     *   角度差在 angleLimit 范围内时返回 0（不需要调整），
     *   超过 angleLimit 时返回超出部分（需要调整的量）。
     *   这样段身在小范围内可以自由摆动，超过限制才被约束。
     */
    private static float angleDistSigned(float a, float b, float start) {
        float dst = angleDistSigned(a, b);
        if (Math.abs(dst) > start) {
            return dst > 0 ? dst - start : dst + start;
        }
        return 0f;
    }

    // 注：clampedAngle（PU132 Utils.clampedAngle 第200行）已注释，
    //     当前用 angleDistSigned(a, b, angleLimit) 替代，效果类似。
    // private static float clampedAngle(float angle, float relative, float limit) { ... }

    /**
     * ★ 重写 remove()：确保所有段身也被移除（借鉴 PU132 WormDefaultUnit.remove L270-276）
     *
     * 【已移除】原反作弊的"拒绝移除"逻辑（lastHealth > 100f 时不移除）已移除。
     */
    @Override
    public void remove() {
        super.remove();
        for (SegmentUnitEntity seg : segments) {
            if (seg != null && seg.isAdded()) {
                seg.head = null;
                seg.remove();
            }
        }
    }

    /**
     * ★ 重写 clipSize()：让镜头边缘能看到整条虫子（借鉴 PU132 WormDefaultUnit.clipSize L216-218）
     *
     * 【为什么要重写】
     *   Mindustry 只渲染镜头范围内的单位，clipSize 决定"多大范围算可见"。
     *   默认 clipSize 只覆盖头部，段身可能在镜头边缘闪烁。
     *   重写后返回 segments.length * segmentSpacing * 2f，覆盖整条虫子。
     */
    @Override
    public float clipSize() {
        if (segments.length == 0) return super.clipSize();
        return segments.length * segmentSpacing * 2f;
    }

    // 注：v154.3 中影子由 UnitType.draw() 内部自动调用 drawShadow(unit)
    //    段身 flying=true 会自动画影子，不需要重写 drawShadow()（UnitEntity 也没有此方法）
    //    段身 type.region 在 SegmentUnitEntity.draw() 中切换为 segment/tail 贴图，
    //    影子会用切换后的贴图，与头部影子一起由 UnitType 自动绘制

    /**
     * ★ 段身死亡通知（由 SegmentUnitEntity.kill 调用）
     *
     * 【分裂逻辑（PU132 WormComp.remove L397-424）】
     *   - splittable=true 且中间段身死亡：后半段创建为新虫子
     *   - splittable=true 且尾部段身死亡：直接移除尾部
     *   - splittable=false：伤害已转移给头部，段身死亡只移除自己
     *
     * 【分裂时发生什么】
     *   1. 找到死亡段身的索引 deadIdx
     *   2. 如果是中间段身死亡（deadIdx < length-1）：
     *      - 后半段段身 [deadIdx+1, end) 创建为新虫子（新 SegmentWormEntity）
     *      - 转移后半段段身的 head 指针、segmentIndex、isTail
     *      - 播放 splitSound
     *   3. 压缩当前头部的段身数组（移除死段及后半段）
     */
    public void onSegmentDied(SegmentUnitEntity seg) {
        int deadIdx = -1;
        for (int i = 0; i < segments.length; i++) {
            if (segments[i] == seg) { deadIdx = i; break; }
        }
        if (deadIdx < 0) return;

        // ★ 分裂：中间段身死亡，后半段创建为新虫子（PU132 splittable）
        if (splittable && deadIdx < segments.length - 1) {
            // 后半段段身 [deadIdx+1, end)
            int tailLen = segments.length - deadIdx - 1;
            SegmentUnitEntity[] tailSegs = new SegmentUnitEntity[tailLen];
            Vec2[] tailPos = new Vec2[tailLen];
            Vec2[] tailVel = new Vec2[tailLen];
            float[] tailRot = new float[tailLen];
            for (int i = 0; i < tailLen; i++) {
                tailSegs[i] = segments[deadIdx + 1 + i];
                tailPos[i] = segPositions[deadIdx + 1 + i];
                tailVel[i] = segVelocities[deadIdx + 1 + i];
                tailRot[i] = segRotations[deadIdx + 1 + i];
            }
            // 创建新头部（与原头部同类型）
            try {
                SegmentWormEntity newHead = (SegmentWormEntity) type.create(team);
                newHead.set(tailSegs[0].x, tailSegs[0].y);
                newHead.rotation = tailRot[0];
                newHead.segmentsCreated = true;  // 跳过自动创建段身
                newHead.splittable = true;
                newHead.chainable = chainable;
                newHead.segmentSpacing = segmentSpacing;
                newHead.regenTime = regenTime;
                newHead.maxSegments = maxSegments;
                newHead.wobbleEnabled = wobbleEnabled;
                newHead.add();
                // 转移后半段段身给新头部
                newHead.segments = tailSegs;
                newHead.segPositions = tailPos;
                newHead.segVelocities = tailVel;
                newHead.segRotations = tailRot;
                for (int i = 0; i < tailLen; i++) {
                    tailSegs[i].head = newHead;
                    tailSegs[i].segmentIndex = i;
                    tailSegs[i].isTail = (i == tailLen - 1);
                }
                // 播放分裂音效
                if (splitSound != null) splitSound.at(this);
            } catch (Throwable t) {
                Log.err("[分裂] 创建新头部失败", t);
            }
        } else if (splittable && splitSound != null) {
            // 尾部段身死亡，播放分裂音效
            splitSound.at(this);
        }

        // 从当前头部列表中移除死段及后半段（分裂时后半段已转移给新头部）
        int newLen = splittable ? deadIdx : 0;
        if (!splittable) {
            // 非分裂模式：保留所有存活段身
            for (SegmentUnitEntity s : segments) {
                if (s != null && s != seg && s.isAdded()) newLen++;
            }
        }
        SegmentUnitEntity[] newSegs = new SegmentUnitEntity[newLen];
        Vec2[] newPos = new Vec2[newLen];
        Vec2[] newVel = new Vec2[newLen];
        float[] newRot = new float[newLen];
        int idx = 0;
        if (splittable) {
            // 分裂模式：只保留死段之前的段身 [0, deadIdx)
            for (int i = 0; i < deadIdx; i++) {
                if (segments[i] != null && segments[i].isAdded()) {
                    newSegs[idx] = segments[i];
                    newPos[idx] = segPositions[i];
                    newVel[idx] = segVelocities[i];
                    newRot[idx] = segRotations[i];
                    newSegs[idx].segmentIndex = idx;
                    newSegs[idx].isTail = (idx == newLen - 1);
                    idx++;
                }
            }
        } else {
            // 非分裂模式：保留所有存活段身
            for (int i = 0; i < segments.length; i++) {
                if (segments[i] != null && segments[i] != seg && segments[i].isAdded()) {
                    newSegs[idx] = segments[i];
                    newPos[idx] = segPositions[i];
                    newVel[idx] = segVelocities[i];
                    newRot[idx] = segRotations[i];
                    newSegs[idx].segmentIndex = idx;
                    newSegs[idx].isTail = (idx == newLen - 1);
                    idx++;
                }
            }
        }
        segments = newSegs;
        segPositions = newPos;
        segVelocities = newVel;
        segRotations = newRot;
    }

    /**
     * ★ 链式合并扫描（PU132 WormComp.updatePost L341-350）
     * 每 5 秒扫描附近，找到同类型尾部虫子合并。
     *
     * 【为什么要链式合并】
     *   两条同类型虫子靠近时合并成一条更长的虫子，
     *   模拟虫子"接龙"生长的行为。
     *
     * 【扫描条件】
     *   - 同队伍、同类型（type 相同）
     *   - 对方有段身
     *   - 对方尾部段身在自己 segmentSpacing*1.2f 范围内
     *   - 总段数 < maxSegments
     */
    protected void tryChainMerge() {
        if (maxSegments > 0 && segments.length >= maxSegments) return;  // 已达上限
        if (segments.length == 0) return;
        // 扫描头部附近 segmentSpacing*2 范围内的同类型尾部虫子
        mindustry.entities.Units.nearby(team, x, y, segmentSpacing * 2f, u -> {
            if (!(u instanceof SegmentWormEntity)) return;
            SegmentWormEntity other = (SegmentWormEntity) u;
            if (other == this) return;
            if (other.type != type) return;  // 同类型
            if (other.segments.length == 0) return;  // 对方有段身
            // 对方尾部段身
            SegmentUnitEntity otherTail = other.segments[other.segments.length - 1];
            // 距离检查
            if (!within(otherTail, segmentSpacing * 1.2f)) return;
            // 总段数检查
            int totalLen = segments.length + other.segments.length;
            if (maxSegments > 0 && totalLen >= maxSegments) return;
            // 合并：把对方的段身追加到自己后面
            mergeFrom(other);
        });
    }

    /**
     * ★ 把 other 的所有段身合并到自己后面（PU132 WormComp.connect L62-81）
     *
     * 【合并操作】
     *   1. 扩展自己的 segments/segPositions/segVelocities/segRotations 数组
     *   2. 把对方段身的 head 指针改为自己
     *   3. 重新计算 segmentIndex 和 isTail
     *   4. 移除对方头部（段身已转移）
     *   5. 播放 chainSound
     *
     * 【注意事项】
     *   - 合并是单向的：this 吸收 other 的段身，other 头部被移除
     *   - 合并后总段数不能超过 maxSegments
     *   - 只扫描同类型（type 相同）的虫子
     */
    public void mergeFrom(SegmentWormEntity other) {
        int myLen = segments.length;
        int otherLen = other.segments.length;
        int newLen = myLen + otherLen;

        SegmentUnitEntity[] newSegs = new SegmentUnitEntity[newLen];
        Vec2[] newPos = new Vec2[newLen];
        Vec2[] newVel = new Vec2[newLen];
        float[] newRot = new float[newLen];

        // 复制自己的段身
        for (int i = 0; i < myLen; i++) {
            newSegs[i] = segments[i];
            newPos[i] = segPositions[i];
            newVel[i] = segVelocities[i];
            newRot[i] = segRotations[i];
        }
        // 追加 other 的段身
        for (int i = 0; i < otherLen; i++) {
            SegmentUnitEntity s = other.segments[i];
            newSegs[myLen + i] = s;
            newPos[myLen + i] = other.segPositions[i];
            newVel[myLen + i] = other.segVelocities[i];
            newRot[myLen + i] = other.segRotations[i];
            s.head = this;  // 段身归属改为 this
            s.segmentIndex = myLen + i;
            s.isTail = (myLen + i == newLen - 1);  // 最后一节是尾部
        }
        // 旧尾部的 isTail 改为 false（不再是尾部）
        if (myLen > 0) {
            newSegs[myLen - 1].isTail = false;
        }
        segments = newSegs;
        segPositions = newPos;
        segVelocities = newVel;
        segRotations = newRot;

        // 移除 other 头部（段身已转移，other 不再持有段身）
        other.segments = new SegmentUnitEntity[0];
        other.segPositions = null;
        other.segVelocities = null;
        other.segRotations = null;
        other.remove();

        // 播放合并音效
        if (chainSound != null) chainSound.at(this);
    }

    /**
     * ★ 创建段身（在 add() 时调用一次）
     *
     * 【PU132 原版做法】
     *   段身初始展开成扇形（WormComp.add L454-468）：
     *   - 每段角度 = rotation + angleLimit + i * angleLimit
     *   - 位置 = 前一段后方 segmentOffset 处
     *   - 这样初始就是自然的展开状态，不需要靠碰撞弹开
     */
    public void createSegments(int count, mindustry.type.UnitType segmentType) {
        segments = new SegmentUnitEntity[count];
        segPositions = new Vec2[count];
        segVelocities = new Vec2[count];
        segRotations = new float[count];

        // 初始化路径点数组：长度 = 段数 * 8（足够所有段身延迟跟随）
        pathPoints = new Vec2[count * 8];
        for (int i = 0; i < pathPoints.length; i++) {
            pathPoints[i] = new Vec2(x, y);
        }

        // ★ PU132 原版做法：段身初始展开成扇形
        //   每段角度 = rotation + angleLimit + i * angleLimit
        //   位置 = 前一段后方 segmentOffset 处
        //   这样初始就是自然的展开状态，不需要靠碰撞弹开
        float[] rot = {rotation + angleLimit};
        Tmp.v1.trns(rot[0] + 180f, segmentSpacing + headOffset).add(this);

        for (int i = 0; i < count; i++) {
            // PU132 原版：段身初始位置和朝向
            float segX = Tmp.v1.x;
            float segY = Tmp.v1.y;
            float angle = rot[0];

            // 用 segmentType 创建段身（SegmentUnitEntity 实例）
            SegmentUnitEntity seg = (SegmentUnitEntity) segmentType.create(team);
            seg.set(segX, segY);
            seg.rotation = angle;
            seg.head = this;
            seg.segmentIndex = i;
            seg.isTail = (i == count - 1);
            seg.texturePrefix = type.name + "-";
            seg.add();

            segments[i] = seg;
            segPositions[i] = new Vec2(seg.x, seg.y);
            segVelocities[i] = new Vec2();
            segRotations[i] = angle;

            // 计算下一段的位置（PU132 原版逻辑）
            rot[0] += angleLimit;
            Tmp.v2.trns(rot[0] + 180f, segmentSpacing);
            Tmp.v1.add(Tmp.v2);
        }
    }

    /**
     * ★ 头部被添加到世界时调用
     * - 缓存贴图前缀
     * - 创建段身（优先用 configs Map，否则用旧静态字段）
     */
    @Override
    public void add() {
        super.add();
        // 缓存贴图前缀
        if (type != null) texturePrefix = type.name + "-";
        // 在头部被添加到世界时创建段身（优先用 configs Map，否则用旧静态字段）
        if (!segmentsCreated && segments.length == 0) {
            SegmentConfig cfg = type != null ? configs.get(type.name) : null;
            if (cfg != null) {
                applyConfig(cfg);
                try {
                    // ★ 读档时用 savedSegmentCount，新建时用 cfg.count（PU132 addSegments=false 模式）
                    int count = savedSegmentCount >= 0 ? savedSegmentCount : cfg.count;
                    createSegments(count, cfg.segmentType);
                    segmentsCreated = true;
                } catch (Throwable t) {
                    Log.err("[头部] 段身创建失败", t);
                    segmentsCreated = true;
                }
            } else if (defaultSegmentType != null) {
                segmentSpacing = defaultSegmentSpacing;
                try {
                    int count = savedSegmentCount >= 0 ? savedSegmentCount : defaultSegmentCount;
                    createSegments(count, defaultSegmentType);
                    segmentsCreated = true;
                } catch (Throwable t) {
                    Log.err("[头部] 旧路径创建失败", t);
                    segmentsCreated = true;
                }
            }
        }
    }

    /**
     * ★★★ 头部绘制：先画头部，再统一绘制所有段身 ★★★
     * （还原 PU132 UnityUnitType.drawBody L640-681）
     *
     * 【PU132 原版架构】
     *   - WormSegmentUnit.add() 不加入 Groups.draw（L61 注释掉）
     *   - WormSegmentUnit.draw() 为空方法（L397-399）
     *   - 段身渲染由头部 UnityUnitType.drawBody() 驱动：
     *     遍历 segmentUnits[]，设 Draw.z(z - (i+1)/10000)，调 segment.drawBody()
     *
     * 【z 层级（头部在最上方）】
     *   - 头部 z = flyingLayer（由 super.draw() → UnitType.draw 设置）
     *   - 段身 0 z = flyingLayer - 1/10000（紧贴头部下方，最高段身）
     *   - 段身 1 z = flyingLayer - 2/10000
     *   - ...
     *   - 段身 n-1 z = flyingLayer - n/10000（最低，尾部）
     *   - 覆盖：头部 > 段身0 > 段身1 > ... > 尾部
     *
     * 【为什么段身必须由头部统一绘制】
     *   Mindustry 的 Draw 批处理在不同 entity 的 draw() 之间会 flush，
     *   z-sorting 只在同一 flush 内生效，跨 entity 的 z 值不会被正确排序。
     *   所以段身不能在 Groups.draw 中自己绘制，必须由头部统一绘制。
     */
    @Override
    public void draw() {
        // ★ 先绘制头部（UnitType.draw 设置 z = flyingLayer，绘制头部 body/cell/weapons）
        super.draw();

        // ★ 绘制段身影子（PU132 WormDefaultUnit.drawShadow L220-227）
        // 段身不在 Groups.draw 中，不会自动画影子，需由头部统一绘制
        // ★ 关键：drawShadow 前必须设 Draw.z(Layer.darkness)，否则影子画在 flyingLayer
        //   会盖在段身贴图之上（v158 UnitType.draw L1495-1497 会自动设 z，这里手动设）
        if (segments.length > 0) {
            Draw.z(mindustry.graphics.Layer.darkness);
            for (int i = 0; i < segments.length; i++) {
                SegmentUnitEntity seg = segments[i];
                if (seg != null && seg.isAdded() && !seg.dead && seg.type != null) {
                    seg.type.drawShadow(seg);
                }
            }
        }

        // ★ 头部绘制完后，统一绘制所有段身（借鉴 PU132 UnityUnitType.drawBody L656-681）
        if (segments.length > 0) {
            float baseZ = type.flyingLayer;
            for (int i = 0; i < segments.length; i++) {
                SegmentUnitEntity seg = segments[i];
                if (seg != null && seg.isAdded() && !seg.dead) {
                    // ★ z 递减：段身 0 最高（紧贴头部），段身 n-1 最低（尾部）
                    Draw.z(baseZ - (i + 1f) / 10000f);
                    seg.drawBody();
                }
            }
        }

        // ★ 再生建造动画（参考 PU132 UnityUnitType.drawBody 第670-675行 + UnitSpawnAbility.draw）
        if (regenAvailable() && segments.length > 0) {
            SegmentUnitEntity tail = segments[segments.length - 1];
            if (tail != null && tail.isAdded()) {
                arc.util.Tmp.v1.trns(tail.rotation + 180f, segmentSpacing).add(tail);
                float sx = arc.util.Tmp.v1.x, sy = arc.util.Tmp.v1.y;

                String p = texturePrefix != null ? texturePrefix : "arcnelidia-";
                String modP = "nu-" + p;
                arc.graphics.g2d.TextureRegion tailRegion = findRegion(p + "tail", modP + "tail");

                float progress = repairTime / regenTime;
                float drawZ = type.flyingLayer - (segments.length + 2f) / 10000f;

                Draw.draw(drawZ, () -> {
                    float pulse = 0.6f + 0.4f * Mathf.sin(repairTime / 10f);
                    float radius = 6f + pulse * 8f;
                    Draw.color(mindustry.graphics.Pal.accent, pulse * 0.5f);
                    arc.graphics.g2d.Lines.stroke(2f * pulse);
                    arc.graphics.g2d.Lines.circle(sx, sy, radius);

                    float scanY = Mathf.lerp(-radius, radius, progress);
                    Draw.alpha(pulse * 0.7f);
                    arc.graphics.g2d.Lines.stroke(1.5f);
                    arc.graphics.g2d.Lines.line(sx - radius, sy + scanY, sx + radius, sy + scanY);

                    Draw.reset();

                    if (tailRegion.found()) {
                        mindustry.graphics.Drawf.construct(
                                sx, sy,
                                tailRegion,
                                tail.rotation - 90f,
                                progress,
                                1f,
                                repairTime
                        );
                    }
                });
            }
        }
    }

    /**
     * 查找贴图：先试 name，找不到再试 prefixedName（与 SegmentUnitEntity 相同逻辑）
     * ★ 为什么需要双名字：Mindustry 给 mod 贴图加 modname- 前缀，
     *   但 UnitType.load 用不带前缀的名字查找，所以要两种都试。
     */
    private static arc.graphics.g2d.TextureRegion findRegion(String name, String prefixedName) {
        arc.graphics.g2d.TextureRegion r = arc.Core.atlas.find(name);
        if (r.found()) return r;
        return arc.Core.atlas.find(prefixedName);
    }
}
