package Npl.newSth;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.*;
import arc.audio.*;
import arc.util.*;
import mindustry.Vars;
import mindustry.gen.*;
import mindustry.entities.abilities.Ability;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;

import static mindustry.Vars.*;

/**
 * 【拦截场 Intercept Field】单位技能
 * 作者规则：
 *  (1) speed > 5 的子弹 → 免费拦截（不消耗次数盾）
 *  (2) speed ≤ 5 且 damage > maxDamage → 消耗 1 层次数盾拦截
 *  (3) speed ≤ 5 且 damage ≤ maxDamage → 不拦截，但受伤倍率固定为 injuredMultiplier
 *  (4) 次数盾被消耗至 0 的瞬间 → 单位护甲 armor 归零，直到重新获得 ≥1 层盾
 *  (5) 每隔 reload tick 自动补一层盾，最多 max 层
 *  (6) 可选碎盾惩罚：破盾时 breakCooldown 内禁止补层
 *  (7) 新：自定义颜色 + 多场重叠融合（ForceFieldAbility 风格，Layer.shields 分层 + 透明混合）
 *
 *  注：此 Mindustry 版本 Bullet/Unit 是 entity mixin，team/type/vel/x/y/armor/hitSize
 *      全部是字段（不是 getter 方法），这里和 NewHorizon TurretShield.java 保持一致。
 */
public class InterceptFieldAbility extends Ability {
    /* ======================================================
     *                   可调参数（对外）
     * ====================================================== */
    /** 拦截/识别范围（像素） */
    public float radius = 80f;
    /** 速度门槛：> 这个值 = 高速 = 免费拦截 */
    public float speedThreshold = 5f;
    /** 伤害门槛：≤ speedThreshold 的子弹，若 damage > 它 = 要消耗 1 层盾拦截 */
    public float maxDamage = 40f;
    /** speed ≤ 门槛 且 damage ≤ 门槛 的子弹放行，但该子弹击中时伤害 × 此倍率
     *  （>1 额外增伤，<1 减伤，=1 不变） */
    public float injuredMultiplier = 1.2f;

    /** 次数盾最大层数 */
    public int max = 4;
    /** 每多少 tick 自动补 1 层（60 = 1 秒） */
    public float reload = 60f * 3;        // 默认 3 秒补一层
    /** 破盾惩罚多少 tick 内不许补层（0 = 关闭惩罚） */
    public float breakCooldown = 60f * 5; // 默认破盾后 5 秒补不回来

    /* ============ 【新】颜色 + 融合 ============ */
    /**
     * 自定义拦截场颜色（如果 useShieldColor = true，则忽略此值，用单位自带的 shieldColor）。
     * 例：new Color(1f, 0.3f, 0.1f, 0.6f) = 半透明橙色拦截场。
     */
    public Color color = Pal.shield;
    /** true = 沿用 unit.type.shieldColor(unit)（默认），false = 用上面 color 字段覆盖 */
    public boolean useShieldColor = true;
    /** 绘制多边形边数（6 = 六边形，32 = 接近圆形），和 ForceFieldAbility 对齐 */
    public int sides = 32;
    /** 多边形旋转角度（度），正六边形 30° 就是扁的那种 */
    public float rotation = 0f;
    /** 出场/消失时半径缩放 lerp 速度，越大出现越快（ForceFieldAbility 默认 0.06） */
    public float scaleLerp = 0.06f;
    /** 多个拦截场叠在一起时，每一层之间的 z 轴偏移（ForceFieldAbility 用 0.001f）。
     *  分层但接近 = 透明叠加时看起来像"融合"。 */
    public float layerStep = 0.001f;

    /* ============ 特效/音效 ============ */
    public Effect highSpeedInterceptFx = Fx.hitBulletSmall; // 免费拦截（高速）
    public Effect consumeShieldFx      = Fx.hitBulletSmall; // 消耗盾拦截（重慢弹）
    public Effect chargeFx             = Fx.spawn;          // 补一层（蓝色圆形扩散）
    public Effect breakFx              = Fx.shieldBreak;    // 破盾
    public Sound  breakSnd             = Sounds.shieldBreak;
    public Sound  interceptSnd         = Sounds.none;
    public Sound  hitSnd               = Sounds.shieldHit;
    public float  hitSndVolume         = 0.12f;

    /* ======================================================
     *                   运行时状态（内部）
     * ====================================================== */
    private int count;                       // 当前次数盾层数
    private float chargeTimer;               // 补层计时
    private float breakPenaltyLeft;          // 破盾惩罚剩余 tick
    private boolean hadShieldLastFrame = true;
    private float originalArmor = -1f;       // 记录单位原本的 armor，破盾后要恢复
    private transient float realRad;
    private transient Unit paramUnit;

    /* 出场/消失动画：0~1 缩放，和 ForceFieldAbility 字段同名方便对照 */
    protected float radiusScale, alpha;
    /* 给"融合色"临时缓存（每只单位每次 draw 都用同一个 tmp 防止 GC） */
    private static final Color tmpColor = new Color();

    /* ======================================================
     *                   Ability 基础方法
     * ====================================================== */
    public InterceptFieldAbility() {
    }

    public InterceptFieldAbility(float radius, float speedThreshold, float maxDamage,
                                 float injuredMultiplier, int max, float reload, float breakCooldown) {
        this.radius = radius;
        this.speedThreshold = speedThreshold;
        this.maxDamage = maxDamage;
        this.injuredMultiplier = injuredMultiplier;
        this.max = max;
        this.reload = reload;
        this.breakCooldown = breakCooldown;
        this.color = color;
    }

    /** 【新】指定自定义颜色的便捷构造（useShieldColor 置为 false） */
    public InterceptFieldAbility(Color customColor, float radius, float speedThreshold, float maxDamage,
                                 float injuredMultiplier, int max, float reload, float breakCooldown) {
        this(radius, speedThreshold, maxDamage, injuredMultiplier, max, reload, breakCooldown);
        this.color = customColor;
        this.useShieldColor = false;
    }

    @Override
    public InterceptFieldAbility copy() {
        // —— 注意：这里不能用 super.copy()（它内部走 ClassMap.newInstance 反射实例化），
        //    否则用户在 FederalUnitType 里如果用了匿名子类写法 {{ color=NuColor.PaleColor; }}，
        //    ClassMap 识别不出匿名子类类型，会 new 回父类 InterceptFieldAbility 的全新对象，
        //    {{}} 里的赋值直接丢失 → 圈颜色永远退回默认的单位金色 shieldColor。
        //    所以我们改为显式 new 具体类 + 手工把每一个字段复制过去。
        InterceptFieldAbility out = new InterceptFieldAbility();
        out.radius = radius;
        out.speedThreshold = speedThreshold;
        out.maxDamage = maxDamage;
        out.injuredMultiplier = injuredMultiplier;
        out.max = max;
        out.reload = reload;
        out.breakCooldown = breakCooldown;
        // 颜色深拷贝（Color 是可变对象）
        out.color = new Color(color);
        out.useShieldColor = useShieldColor;
        out.sides = sides;
        out.rotation = rotation;
        out.scaleLerp = scaleLerp;
        out.layerStep = layerStep;
        out.highSpeedInterceptFx = highSpeedInterceptFx;
        out.consumeShieldFx = consumeShieldFx;
        out.chargeFx = chargeFx;
        out.breakFx = breakFx;
        out.breakSnd = breakSnd;
        out.interceptSnd = interceptSnd;
        out.hitSnd = hitSnd;
        out.hitSndVolume = hitSndVolume;

        // —— 颜色双保险：只要 color 不是默认 Pal.shield（蓝），就认为用户想自定义，
        //    自动关闭 useShieldColor，防止忘写那个开关。
        if (!out.color.equals(Pal.shield)) {
            out.useShieldColor = false;
        }

        // —— ★ 开局自带拦截场：每只单位拿到 Ability 副本时，直接把次数盾层数赋满 max
        //    （如果写在 init(UnitType) 里只会对模板对象生效，copy() 到工作副本后 count 还是默认 0，
        //     导致开局 max*reload 秒内一直没盾 —— 用户看到"开局的拦截场消失了"）
        out.count = out.max;
        out.chargeTimer = 0f;
        out.breakPenaltyLeft = 0f;
        out.hadShieldLastFrame = (out.count > 0);
        return out;
    }

    /* ======================================================
     *  【链式 setter 集合】——返回 this，一行链式调用搞定所有自定义参数
     *  用法：
     *  abilities.add(new InterceptFieldAbility(...)
     *      .color(NuColor.PaleColor)   // 自定义颜色（会自动关 useShieldColor）
     *      .sides(32)
     *      .rotation(0f));
     * ====================================================== */
    public InterceptFieldAbility color(Color c) {
        if (c != null) {
            this.color = new Color(c);
            this.useShieldColor = false;  // 显式调了 color() = 用户想自定义，自动关跟随单位色
        }
        return this;
    }
    public InterceptFieldAbility useShieldColor(boolean v) { this.useShieldColor = v; return this; }
    public InterceptFieldAbility sides(int v)             { this.sides = v; return this; }
    public InterceptFieldAbility rotation(float v)        { this.rotation = v; return this; }
    public InterceptFieldAbility radius(float v)          { this.radius = v; return this; }
    public InterceptFieldAbility speedThreshold(float v)  { this.speedThreshold = v; return this; }
    public InterceptFieldAbility maxDamage(float v)       { this.maxDamage = v; return this; }
    public InterceptFieldAbility injuredMultiplier(float v){ this.injuredMultiplier = v; return this; }
    public InterceptFieldAbility max(int v)               { this.max = v; return this; }
    public InterceptFieldAbility reload(float v)          { this.reload = v; return this; }
    public InterceptFieldAbility breakCooldown(float v)   { this.breakCooldown = v; return this; }
    public InterceptFieldAbility scaleLerp(float v)       { this.scaleLerp = v; return this; }
    public InterceptFieldAbility layerStep(float v)       { this.layerStep = v; return this; }

    @Override
    public void init(mindustry.type.UnitType type) {
        super.init(type);
        originalArmor = type.armor;
        count = max;                          // 出生时满层
        radiusScale = 0f;                     // 出场从小变大
        alpha = 0f;
    }

    @Override
    public String localized() {
        return "拦截场";
    }

    @Override
    public void addStats(arc.scene.ui.layout.Table t){
        super.addStats(t);
        t.add(Core.bundle.format("bullet.range", Strings.autoFixed(radius / Vars.tilesize, 2)));
        t.row();
        t.add(abilityStat("frequency", Strings.autoFixed(max, 0)));
        t.row();
        t.add(abilityStat("speed", Strings.autoFixed(speedThreshold, 2)));
        t.row();
        t.add(abilityStat("damage", Strings.autoFixed(maxDamage, 1)));
        t.row();
        t.add(abilityStat("repairspeed", Strings.autoFixed(reload / 60f, 2)));
        t.row();
        t.add(abilityStat("multiplier", Strings.autoFixed(injuredMultiplier, 2)));
        t.row();
        if (breakCooldown > 0f) {
            t.add(abilityStat("cooldown", Strings.autoFixed(breakCooldown / 60f, 2)));
        }
    }

    /* ======================================================
     *                   主逻辑 update
     * ====================================================== */
    @Override
    public void update(Unit unit) {
        if (unit == null) return;

        if (originalArmor < 0f) originalArmor = Math.max(unit.armor, 0f);

        // —— Step 1. 破盾惩罚倒计时
        if (breakPenaltyLeft > 0f) {
            breakPenaltyLeft -= Time.delta;
            if (breakPenaltyLeft < 0f) breakPenaltyLeft = 0f;
        }

        // —— Step 2. 补次数盾
        if (count < max && breakPenaltyLeft <= 0f) {
            chargeTimer += Time.delta;
            if (chargeTimer >= reload) {
                chargeTimer = 0f;
                count++;
                float r = Math.max(radius, unit.hitSize);
                if (chargeFx != null && unit.type != null) {
                    chargeFx.at(unit.x, unit.y, r, effectiveColor(unit, alpha));
                }
                if (count == 1) giveArmorBack(unit);
            }
        } else {
            chargeTimer = 0f;
        }

        // —— Step 3. 破盾瞬间（从有→无 只触发一次）
        boolean hasShieldNow = count > 0;
        if (!hasShieldNow && hadShieldLastFrame) {
            float r = Math.max(radius, unit.hitSize);
            if (breakFx != null) {
                breakFx.at(unit.x, unit.y, r, effectiveColor(unit, 1f), this);
            }
            breakSnd.at(unit);
            if (breakCooldown > 0f) breakPenaltyLeft = breakCooldown;
        }
        hadShieldLastFrame = hasShieldNow;

        // —— Step 4. 没盾 → 强制 armor 归零
        if (count <= 0) takeArmorAway(unit);

        // —— Step 5. 出场动画：有盾时 radiusScale 向 1 靠近；无盾时向 0 靠近
        if (count > 0) {
            radiusScale = Mathf.lerpDelta(radiusScale, 1f, scaleLerp);
            // 被 hit 时 alpha 拉到 1，然后这里慢慢衰减，效果就是被打就亮一下
            alpha = Math.max(alpha - Time.delta / 10f, 0f);
        } else {
            radiusScale = Mathf.lerpDelta(radiusScale, 0f, scaleLerp);
            alpha = 0f;
        }

        // —— Step 6. 扫子弹，应用拦截场规则
        realRad = Math.max(radius, unit.hitSize) * radiusScale;
        paramUnit = unit;
        if (radiusScale > 0.05f) {
            Groups.bullet.intersect(
                    unit.x - realRad, unit.y - realRad,
                    realRad * 2f, realRad * 2f,
                    (Cons<Bullet>)this::consumeBullet
            );
        }
    }

    /* ======================================================
     *  拦截场扫子弹：方法引用 this::consumeBullet
     * ====================================================== */
    private void consumeBullet(Bullet b) {
        if (b == null || paramUnit == null) return;
        if (b.type == null || paramUnit.type == null) return;
        if (b.team == paramUnit.team) return;
        if (!b.type.absorbable) return;
        // 多边形内判定（对齐 ForceFieldAbility 方式，sides=32 就是圆）
        if (!arc.math.geom.Intersector.isInRegularPolygon(
                sides, paramUnit.x, paramUnit.y, realRad, rotation, b.x, b.y)) return;

        float bulletSpeed = b.vel.len();
        float bulletDmg   = b.damage;

        // —— 命中一下就把 alpha（高光透明度）拉高，画出闪一下的效果
        alpha = 1f;

        // 规则 (1) 高速 → 免费拦截
        if (bulletSpeed > speedThreshold) {
            b.remove();
            if (highSpeedInterceptFx != null) {
                highSpeedInterceptFx.at(b.x, b.y, 0, effectiveColor(paramUnit, 1f));
            }
            if (interceptSnd != Sounds.none) interceptSnd.at(paramUnit, 0.6f);
            if (hitSnd != Sounds.none) hitSnd.at(b.x, b.y, 1f + Mathf.range(0.1f), hitSndVolume);
            return;
        }

        // 规则 (2) 低速 + 重伤害 → 消耗 1 层盾
        if (bulletDmg > maxDamage) {
            if (count > 0) {
                count--;
                b.remove();
                if (consumeShieldFx != null) {
                    consumeShieldFx.at(b.x, b.y, 0, effectiveColor(paramUnit, 1f));
                }
                if (interceptSnd != Sounds.none) interceptSnd.at(paramUnit, 0.8f);
                if (hitSnd != Sounds.none) hitSnd.at(b.x, b.y, 1f + Mathf.range(0.1f), hitSndVolume);
                if (count == 0) takeArmorAway(paramUnit);
            }
            return;
        }

        // 规则 (3) 低速 + 轻伤害 → 放行。受伤倍率在 UnitDamageEvent 里统一乘。
        // 但闪一下高光依然生效（表示"场感知到了但没挡"）
    }

    /* ======================================================
     *                   规则 (3)：受伤倍率
     * ====================================================== */
    public float modifyDamage(Unit unit, Bullet bullet, float originalDamage) {
        if (bullet == null || bullet.type == null) return originalDamage;
        float spd = bullet.vel.len();
        if (spd <= speedThreshold && bullet.damage <= maxDamage) {
            return originalDamage * injuredMultiplier;
        }
        return originalDamage;
    }

    /* ======================================================
     *  【新】ForceFieldAbility 式融合绘制
     *  关键：Draw.z(Layer.shields + layerStep * alpha) 叠加层时 z 值接近，
     *       Draw.color(base, white, alpha) 做白高光混合 + 半透明填充，
     *       两个/多个场叠在一起时就会呈现"融合"感。
     * ====================================================== */
    @Override
    public void draw(Unit unit) {
        if (unit == null) return;
        if (radiusScale <= 0.02f) return;

        float rr = radius * radiusScale;    // 画描边外层时用原 radius（和 FFA 一致描边大小）
        float rrPoly = rr;                  // 里层填充用缩放后半径
        Color c = effectiveColor(unit, 1f);

        if (Vars.renderer.animateShields) {
            // —— 动画开启时：单层填充 + 超高 z 精细分层 → 完全融合透明叠加，像 Merge Sphere
            Draw.z(Layer.shields + layerStep * Mathf.clamp(alpha));
            Draw.color(c, Color.white, Mathf.clamp(alpha));
            Fill.poly(unit.x, unit.y, sides, rrPoly * 1.01f, rotation);
        } else {
            // —— 动画关闭时：ForceFieldAbility 同款"半透明填充 + 描边"
            // 1) 外层淡淡的一整圈填充（0.09 alpha 原版）
            Draw.z(Layer.shields);
            Lines.stroke(1.5f, c);
            Draw.alpha(0.09f);
            Fill.poly(unit.x, unit.y, sides, radius, rotation);
            // 2) 描边
            Draw.alpha(1f);
            Lines.poly(unit.x, unit.y, sides, radius, rotation);

            // 3) 额外再加一个"有动画的内层高亮层"，和外层叠加，多场重叠时这部分会融合
            Draw.z(Layer.shields + layerStep * Mathf.clamp(alpha) + 0.0001f);
            Draw.color(c, Color.white, Mathf.clamp(alpha) * 0.75f);
            Fill.poly(unit.x, unit.y, sides, rrPoly, rotation);
        }

        Draw.reset();
    }

    /* ======================================================
     *                   辅助：护甲归零 / 还原
     * ====================================================== */
    private void takeArmorAway(Unit unit) {
        if (unit == null) return;
        if (originalArmor < 0f) originalArmor = Math.max(unit.armor, 0f);
        unit.armor = 0f;
    }

    private void giveArmorBack(Unit unit) {
        if (unit == null || originalArmor < 0f) return;
        unit.armor = originalArmor;
    }

    /* ======================================================
     *  【新】取最终颜色：useShieldColor ? unit.type.shieldColor : this.color
     *       返回值写到 tmpColor 中返回，避免每次调用 new Color 导致 GC。
     * ====================================================== */
    private Color effectiveColor(Unit unit, float wantAlpha) {
        if (useShieldColor && unit != null && unit.type != null) {
            tmpColor.set(unit.type.shieldColor(unit));
        } else {
            tmpColor.set(color);
        }
        tmpColor.a = wantAlpha * tmpColor.a;
        return tmpColor;
    }

    /* ======================================================
     *                   对外读接口
     * ====================================================== */
    public int count()                { return count; }
    public boolean hasAnyShield()     { return count > 0; }
    public boolean isPenalty()        { return breakPenaltyLeft > 0f; }
    public float penaltyLeft()        { return breakPenaltyLeft; }
    public void forceSetCount(int c)  { count = Mathf.clamp(c, 0, max); }
}
