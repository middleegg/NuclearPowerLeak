package Npl.newSth;

import arc.func.Cons;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.graphics.*;

/**
 * 模块化爆炸特效（参考 WaveEffect 的模块化写法）
 * =================================================
 *   作用：
 *   把 NuFx.explosion1 那种「在 new Effect(..., lambda) 里写死」的参数，
 *   全部提成类的公共可写字段，用户能像 WaveEffect 一样：
 *     ① 用默认参数 new expEffect() 直接用
 *     ② 用双大括号匿名子类改参数（和 Weapon/BulletType 一个味道）
 *     ③ 链式/字段式改单个值（比如只改 strokeTo 不改 particles）
 * =================================================
 *   两部分组成（和 explosion1 视觉一致）：
 *     1) 主冲击波圈（描边空心环，sizeFrom→sizeTo）
 *     2) particles 颗向外飞散的方形粒子
 * =================================================
 *   用法：
 *     expEffect boom = new expEffect(){{
 *         sizeTo = 80f;
 *         particles = 12;
 *         particleSizeTo = 1.6f;
 *         strokeFrom = 3f;
 *         colorFrom = Color.valueOf("FF6A00");   // 橙红
 *         colorTo   = Color.valueOf("FFD54F");   // 金黄
 *     }};
 *     boom.at(unit.x, unit.y);
 */
public class expEffect extends Effect {

    /* ==========================================================
     *                  可调参数（用户可写）
     * ========================================================== */

    // ---------- 冲击波圈 ----------
    /** 起始颜色（进度 0）*/
    public Color colorFrom = Color.white.cpy();
    /** 结束颜色（进度 1，越淡越接近透明黄/白）*/
    public Color colorTo   = Color.white.cpy();

    /** 起始半径（进度 0） */
    public float sizeFrom = 0f;
    /** 结束半径（进度 1，默认 80 像素，对应 explosion1 最大圈） */
    public float sizeTo   = 80f;

    /** 起始线宽（进度 0） */
    public float strokeFrom = 3f;
    /** 结束线宽（进度 1，一般 0~1 就行） */
    public float strokeTo   = 1f;

    /** 多边形边数：-1 = 自动算圆顶点数；其它 = 正 N 边形（比如 6=六边形冲击波） */
    public int sides = -1;

    /** 整体旋转角（给粒子 + 圈一起转的全局旋转） */
    public float rotation = 0f;

    /** 尺寸/半径曲线（默认 pow2Out → 开始快 后段慢） */
    public Interp interp = Interp.pow2Out;

    // ---------- 飞散粒子 ----------
    /** 多少颗粒子向外飞（默认 12） */
    public int particles = 12;
    /** 粒子起始尺寸（半边长，Fill.square 是半边长 *2） */
    public float particleSizeFrom = 1.5f;
    /** 粒子结束尺寸（一般更小，粒子慢慢变小看不见） */
    public float particleSizeTo   = 0.25f;
    /** 粒子起始飞行距离 */
    public float flyDistanceFrom = 0f;
    /** 粒子结束飞行距离（粒子最外圈能飞到多少像素） */
    public float flyDistanceTo   = 55f;
    /** 尺寸曲线（粒子飞行/半径用哪个 interp，默认和圈一样的 pow2Out） */
    public Interp particleInterp = Interp.pow2Out;
    /** 粒子透明度的曲线：1 - f^2（爆炸前一段粒子很明显，后段加速消失） */
    public Interp particleAlphaInterp = Interp.pow2In;

    // ---------- 光照光晕 ----------
    /** 光照颜色，null = 用当前绘制的颜色（推荐直接填颜色） */
    public @Nullable Color lightColor;
    /** 光照半径 = 当前圈半径 × 这个倍数（3 就够大了，夜里会亮） */
    public float lightScl = 3f;
    /** 光照不透明度 0~1 */
    public float lightOpacity = 0.8f;
    /** 光照透明度用的曲线（默认 reverse：刚出来暗？不对 → 用线性也行，爆炸一般刚开始亮 → Interp.linear 也行，这里保留 WaveEffect 默认） */
    public Interp lightInterp = Interp.reverse;

    // ---------- 偏移 ----------
    public float offsetX, offsetY;

    /* ==========================================================
     *                  构造器（3 种，和 Mindustry Effect 兼容）
     * ========================================================== */

    public expEffect() {
        this.lifetime = 30f;
        // clip 会在 init() 根据 sizeTo 自动补足，这里给个保底不会在屏幕外被裁
        this.clip = 200f;
    }

    public expEffect(float lifetime, float clip) {
        this.lifetime = lifetime;
        this.clip = clip;
    }

    public expEffect(float lifetime, float clip, Cons<expEffect> cons) {
        this(lifetime, clip);
        cons.get(this);
    }

    /* ==========================================================
     *                  初始化：算 clip 不会被裁
     *   参考 WaveEffect.init()：把 sizeTo/stroke 最大 丢进 clip
     *   我们额外再 + 粒子 flyDistanceTo，避免粒子飞太远时整段不显示
     * ========================================================== */
    @Override
    public void init() {
        float maxStroke = Math.max(strokeFrom, strokeTo);
        float maxSize   = Math.max(sizeFrom, sizeTo);
        float maxFly    = Math.max(flyDistanceFrom, flyDistanceTo);
        // 圈能到 maxSize；粒子最远能到 maxFly + 2*粒子边长（保守点）
        float extent = Math.max(maxSize, maxFly + particleSizeFrom * 2f);
        this.clip = Math.max(this.clip, extent + maxStroke);
    }

    /* ==========================================================
     *                  复制（给 ClassMap/模板机制 用）
     *   注：Mindustry 159.6 的 Effect 基类并没有 copy() 方法，
     *      所以这个方法不加 @Override，当作我们自己的公开工具方法，
     *      用来在 NuFx/主类里手动克隆一份变体（避免字段 Color 引用共享导致颜色串）。
     *   Color 必须 cpy()，不然一堆对象共享同一个 Color 引用，
     *   用户改一个 explosion 其它一起变色
     * ========================================================== */
    public expEffect copy() {
        expEffect out = new expEffect();

        // —— Effect 基类的真实公开字段（见 Mindustry entities/Effect.java L30-L44）——
        //   （注意：Effect 里没有 billow/cull/depthShadow 这些字段，都是我之前误加的；
        //     layer 是原始 float，不是 Float 对象，不能跟 null 比较）
        out.renderer       = this.renderer;        // 渲染 Cons 也拷走（子类 render 重写了就不生效，但兼容起见带上）
        out.lifetime       = this.lifetime;
        out.clip           = this.clip;
        out.startDelay     = this.startDelay;
        out.baseRotation   = this.baseRotation;
        out.followParent   = this.followParent;
        out.rotWithParent  = this.rotWithParent;
        out.layer          = this.layer;
        out.layerDuration  = this.layerDuration;

        // —— 自己加的参数 ——
        out.colorFrom = colorFrom.cpy();
        out.colorTo   = colorTo.cpy();
        if (lightColor != null) out.lightColor = lightColor.cpy();
        out.sizeFrom = sizeFrom;
        out.sizeTo = sizeTo;
        out.strokeFrom = strokeFrom;
        out.strokeTo = strokeTo;
        out.sides = sides;
        out.rotation = rotation;
        out.interp = interp;
        out.particles = particles;
        out.particleSizeFrom = particleSizeFrom;
        out.particleSizeTo = particleSizeTo;
        out.flyDistanceFrom = flyDistanceFrom;
        out.flyDistanceTo = flyDistanceTo;
        out.particleInterp = particleInterp;
        out.particleAlphaInterp = particleAlphaInterp;
        out.lightScl = lightScl;
        out.lightOpacity = lightOpacity;
        out.lightInterp = lightInterp;
        out.offsetX = offsetX;
        out.offsetY = offsetY;

        return out;
    }

    /* ==========================================================
     *                  渲染（参考 explosion1 的两部分）
     * ========================================================== */
    @Override
    public void render(EffectContainer e) {
        float fin  = e.fin();            // 0→1 线性进度
        float ifin = interp.apply(fin);  // 半径/尺寸经过 interp 后的曲线进度
        float ox = e.x + Angles.trnsx(e.rotation, offsetX, offsetY);
        float oy = e.y + Angles.trnsy(e.rotation, offsetX, offsetY);
        float rot = rotation + e.rotation;

        // —————————— ① 冲击波圈（对应 explosion1 第 1 段）——————————
        float ringR   = interp.apply(sizeFrom, sizeTo, fin);
        float ringA   = 1f - fin;  // 越后越淡（1→0）
        if (ringA > 0.02f) {
            Draw.color(colorFrom, colorTo, ifin);   // 颜色 A→B 按进度插值
            Lines.stroke(interp.apply(strokeFrom, strokeTo, fin));
            if (sides <= 0) {
                Lines.circle(ox, oy, ringR);
            } else {
                Lines.poly(ox, oy, sides, ringR, rot);
            }
        }

        // —————————— ② N 颗粒子向外飞散（对应 explosion1 第 2 段）——————————
        int p = Math.max(0, particles);
        if (p > 0) {
            float fly    = particleInterp.apply(flyDistanceFrom, flyDistanceTo, fin);
            // 透明度：1 - fin²，爆炸后段消失得更快（和 explosion1 一样）
            float pAlpha = 1f - Math.max(0f, Math.min(1f, particleAlphaInterp.apply(fin)));
            if (pAlpha > 0.02f) {
                // 粒子颜色 A→B，和圈用同样的过渡
                Draw.color(Tmp.c1.set(colorFrom).lerp(colorTo, ifin).a(pAlpha));
                for (int i = 0; i < p; i++) {
                    // 每颗粒子等分 360°
                    float ang = (i * 360f / p) + rot;
                    float px = ox + Angles.trnsx(ang, fly);
                    float py = oy + Angles.trnsy(ang, fly);
                    float half = particleInterp.apply(particleSizeFrom, particleSizeTo, fin);
                    // Fill.square 第 4 个参数是旋转角度（粒子顺着飞出去的方向转）
                    Fill.square(px, py, half, ang);
                }
                Draw.color(); // 恢复 Draw 默认色（白）
            }
        }

        // —————————— ③ 光照光晕（夜里能看到一团光，WaveEffect 同款）——————————
        if (lightOpacity > 0f) {
            float lightFin = e.fin(lightInterp);
            if (lightFin > 0.01f) {
                Color lc = lightColor == null
                        ? Tmp.c1.set(colorFrom).lerp(colorTo, ifin)
                        : lightColor;
                Drawf.light(ox, oy, ringR * lightScl, lc, lightOpacity * lightFin);
            }
        }

        Draw.reset(); // 重置颜色/线宽/透明度，防止污染下一次渲染
    }
}
