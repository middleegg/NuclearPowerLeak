package Npl.newSth;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.graphics.*;

/**
 * 模块化子弹尾部特效（参考 expEffect 的模块化写法）
 * ============================================================
 *   一句话说明：
 *   把 Mindustry 原版所有"写死在 Fx.missileTrail / Fx.shootSmokeTile / Fx.sparkTrail 里的尾迹"
 *   合进一个类里，参数全开放，你想做成"能量弹发光尾 / 导弹烟爆炸尾 / 烟雾弹淡烟雾尾 / 花瓣散尾"都行
 * ============================================================
 *   组成（全部独立开关，关掉=不画，性能省一半）：
 *     1) core  ：尾部 1 个实心发光圆点（高能弹头尾焰）
 *     2) particles：N 个随机方向的方形/圆形粒子（像机枪子弹喷出的火星）
 *     3) smoke   ：1~2 圈慢速扩散的烟雾圆（Missile / Bomb 那种拖烟）
 *     4) ring    ：1 个逐渐散开的描边空心环（尾迹边缘的"尾浪"）
 * ============================================================
 *   字段怎么读：
 *     colorFrom   —— 出生颜色（特效刚出现在子弹尾部时）
 *     colorTo     —— 消失颜色（特效快淡完时，取插值 lerp(colorFrom, colorTo, f)）
 *     sizeFrom    —— 出生尺寸
 *     sizeTo      —— 消失尺寸       （一般 sizeTo > sizeFrom 表示越变越大）
 *     particles / smokeLayers / ringCount —— 该层画几个
 * ============================================================
 *   典型用法（4 种最常见尾迹，直接抄 ②③④⑤ 的任何一个）：
 *
 *   ① 直接用默认（什么都不改：粒子+发光点，适用于机枪小子弹）
 *       trailEffect = new BulletTailEffect();
 *
 *   ② 高能能量弹尾（蓝紫色拖烟 + 强发光点，适合激光 / 等离子炮）
 *       trailEffect = new BulletTailEffect(){{
 *           colorFrom    = Color.valueOf("A470FF");
 *           colorTo      = Color.valueOf("7AE7FF");
 *           particles    = 5;
 *           particleShape = 0;  // 0=圆 1=方
 *           particleSpread = 8f;
 *           coreFrom     = 5f;  coreTo = 0.5f;
 *           smokeLayers  = 1;
 *           smokeFrom    = 2f;  smokeTo = 12f;
 *       }};
 *
 *   ③ 导弹烟尾（白色浓烟 + 外圆环，适合 MissileBulletType / Bomb）
 *       trailEffect = new BulletTailEffect(){{
 *           colorFrom    = Color.valueOf("E8E4D8");
 *           colorTo      = Color.valueOf("56534B");
 *           particles    = 2;
 *           particleSpread = 3f;
 *           smokeLayers  = 2;
 *           smokeFrom    = 2.5f; smokeTo = 9f;
 *           ringCount    = 1;
 *           ringStroke   = 1.4f;
 *       }};
 *
 *   ④ 狙击弹强发光尾（只留一个超亮尾焰点 + 少量火星，适合 Flak / 远射狙击）
 *       trailEffect = new BulletTailEffect(){{
 *           colorFrom    = Color.valueOf("FFD54F");
 *           colorTo      = Color.valueOf("FF7043");
 *           particles    = 3;
 *           particleSpread = 2f;
 *           coreFrom     = 7f; coreTo = 0.4f;
 *           coreGlowMul  = 1.6f;   // 发光倍数大一点
 *       }};
 *
 *   ⑤ 花瓣散尾（粉色尾 + 方粒子 + 环形扩散，好看就是了）
 *       trailEffect = new BulletTailEffect(){{
 *           lifetime     = 22f;
 *           colorFrom    = Color.valueOf("FF80AB");
 *           colorTo      = Color.valueOf("E1BEE7");
 *           particles    = 7;
 *           particleShape = 1;     // 方形
 *           particleSpread = 10f;
 *           particleSizeFrom = 1.8f; particleSizeTo = 0.2f;
 *           ringCount    = 1;
 *           ringStroke   = 1.2f;
 *       }};
 *
 *   ============================================================
 *   对应到 BulletType 的正确配置（**请同时配这 4 行**，不然尾迹要么不触发要么不画）：
 *       trailLength   = 14;          // >0 才会生成 "条状渐变拖尾"
 *       trailWidth    = 2.6f;        // 条状拖尾宽度
 *       trailColor    = NuColor.DespColor;  // 条状拖尾颜色
 *       trailInterval = 4f;          // 每 4 tick 生成 1 个 BulletTailEffect 粒子
 *       trailEffect   = <上面的 BulletTailEffect 实例>;
 */
public class BulletTailEffect extends Effect {

    /* ==========================================================
     *                  可调参数（用户可写）
     * ========================================================== */

    // ---------- 全局（颜色 + 生命周期） ----------
    /** 起始颜色（f=0 时）。不传 color 参数就用它，传了 color 就把它作为 colorFrom 的起点 */
    public Color colorFrom = Pal.missileYellowBack.cpy();
    /** 结束颜色（f=1 时）*/
    public Color colorTo   = Pal.missileYellow.cpy();
    /** 是否叠加发光（Draw.color() 后再乘上颜色自身亮度，让浅色看起来会发光）*/
    public boolean additive = true;

    // ---------- 层 1：核心发光圆点（弹尾的"尾焰"） ----------
    /** 是否画核心点 */
    public boolean drawCore = true;
    public float coreFrom = 3.5f;   // f=0 大小
    public float coreTo   = 0.4f;   // f=1 大小
    /** 核心发光倍数（>1 = 更大更亮的外层柔光）*/
    public float coreGlowMul = 1.2f;

    // ---------- 层 2：粒子 ----------
    /** 是否画粒子层 */
    public boolean drawParticles = true;
    /** 粒子数量（一般 2~8，太多费性能）*/
    public int particles = 4;
    /** 0=圆形粒子 1=方形粒子 */
    public int particleShape = 0;
    /** 粒子向子弹四周随机扩散的半径（像素）；0 = 粒子都堆在弹尾不扩散 */
    public float particleSpread = 4.5f;
    /** 粒子尺寸 */
    public float particleSizeFrom = 1.8f;
    public float particleSizeTo   = 0.2f;
    /** 粒子飞散距离（像素，f=0→f=1 的过程中沿着随机方向飘这么远）*/
    public float particleFlyTo = 4f;

    // ---------- 层 3：烟雾（子弹身后 1~N 圈慢速扩散的圆） ----------
    /** 画几圈烟雾（0=不画，一般 1~2 够了）*/
    public int smokeLayers = 0;
    /** 烟雾颜色（传 null = 走全局 color 插值）*/
    public Color smokeColor = null;
    public float smokeFrom = 2f;
    public float smokeTo   = 10f;
    /** 烟雾层之间的偏移（越大越"分层感"越明显）*/
    public float smokeOffset = 1.5f;
    /** 烟雾透明度（0.15~0.4 比较有"烟"的味道，别设 1，太实了）*/
    public float smokeAlphaMul = 0.35f;

    // ---------- 层 4：描边圆环（尾浪） ----------
    /** 画几个环形扩散（0=不画，一般 1 就够了）*/
    public int ringCount = 0;
    public float ringFrom = 1.5f;
    public float ringTo   = 14f;
    public float ringStroke = 1.1f;
    /** 环之间的相位差（0.15 = 第二个环比第一个环晚启动 15% 的生命周期）*/
    public float ringPhase = 0.2f;

    // ---------- 额外 ----------
    /** 整体尺寸倍率（所有 layer 一起放大/缩小，省得一个个改）*/
    public float sizeMul = 1f;
    /** 整体透明度倍率（0~1）*/
    public float alphaMul = 1f;

    /* ==========================================================
     *                  构造
     * ==========================================================
     *   Mindustry 159.6 Effect 只有 3 个构造器：
     *     Effect()                                  — 默认
     *     Effect(float life, Cons<EffectContainer>) — 只有 lifetime
     *     Effect(float life, float clipsize, Cons)  — 全参
     *   没有双 float 版本，所以必须传一个 Cons（哪怕空 lambda），
     *   真正的渲染逻辑走下面 override 的 render(EffectContainer) 方法。
     */

    /** 默认：生命周期 20 tick（约 0.33 秒），裁剪盒 50px */
    public BulletTailEffect() {
        this(20f, 50f);
    }

    public BulletTailEffect(float lifetime, float clip) {
        super(lifetime, clip, e -> {});   // 空 Cons，真正逻辑在 override render
    }

    /** 这是 Mindustry 159.6 Effect 真正调用的渲染回调（父类 Effect.render 里就是 renderer.get(e)，
     *  我们 override 掉，直接访问 this 字段，实现模块化）*/
    @Override
    public void render(EffectContainer e) {
        // 1) 进度：f = 0 刚出生 → f = 1 马上消失
        float f = e.fin();
        if (f >= 1f || f < 0f) return;

        // 2) 全局颜色：如果调用方传了 e.color（不是白），就把 colorFrom 设成它，colorTo 用原 colorTo
        Color start, end;
        if (e.color != null && !e.color.equals(Color.white)) {
            start = Tmp.c1.set(e.color);
            end   = Tmp.c2.set(colorTo);
        } else {
            start = Tmp.c1.set(colorFrom);
            end   = Tmp.c2.set(colorTo);
        }
        Color col = Tmp.c3.set(start).lerp(end, Interp.pow2Out.apply(f));
        // 整体透明度：默认是"中段最亮，两头淡"的钟形曲线，比线性好看
        float lifeAlpha = (1f - f) * Mathf.sin(f * Mathf.PI);
        float alpha = Mathf.clamp(lifeAlpha * alphaMul);
        if (alpha <= 0.01f) return;

        Draw.reset();
        if (additive) Draw.blend(Blending.additive);   // 加色混合（发光感）
        // 还原在 finally 里统一做

        try {
            // ————————— 层 1：核心发光点（最大最亮，先画） —————————
            if (drawCore) {
                float coreR = Mathf.lerp(coreFrom, coreTo, f) * sizeMul;
                if (coreR > 0.1f) {
                    // 外发光：2x 大 + 半透明
                    Draw.color(col, alpha * 0.35f);
                    Fill.circle(e.x, e.y, coreR * coreGlowMul);
                    // 内核：纯色
                    Draw.color(col, alpha);
                    Fill.circle(e.x, e.y, coreR);
                }
            }

            // ————————— 层 2：粒子（N 个随机方向 + 固定种子，不要每帧抖动） —————————
            if (drawParticles && particles > 0) {
                // e.id 是 int 字段（不是方法），转 long 给 Mathf.randomSeed 当种子
                long seed = (long)(e.id + 1);
                float pSize = Mathf.lerp(particleSizeFrom, particleSizeTo, f) * sizeMul;
                float fly = Interp.pow2Out.apply(f) * particleFlyTo;
                if (pSize > 0.05f) {
                    Draw.color(col, alpha);
                    for (int i = 0; i < particles; i++) {
                        // 伪随机：seed+i 当种子，生成一个角度 + 距离
                        float r1 = Mathf.randomSeed(seed * 31L + i, -1f, 1f);
                        float r2 = Mathf.randomSeed(seed * 53L + i,  0f, 1f);
                        float ang = r1 * 180f;
                        float spr = r2 * particleSpread;
                        float px = e.x + Angles.trnsx(ang, spr + fly);
                        float py = e.y + Angles.trnsy(ang, spr + fly);
                        if (particleShape == 1) Fill.square(px, py, pSize, ang);
                        else                    Fill.circle(px, py, pSize);
                    }
                }
            }

            // ————————— 层 3：烟雾（大、淡、慢，先画底层再画上层，会叠成很有层次的烟） —————————
            if (smokeLayers > 0) {
                // 烟雾颜色：传了 smokeColor 用它，没传就走全局 col（复用 Tmp.c3）
                for (int i = 0; i < smokeLayers; i++) {
                    float fi = Mathf.clamp(f - i * 0.08f);  // 每层启动时间错开 8%
                    if (fi >= 1f) continue;
                    float sR = Mathf.lerp(smokeFrom, smokeTo, Interp.slowFast.apply(fi)) * sizeMul
                             + i * smokeOffset;
                    if (sR > 0.2f) {
                        float sa = Mathf.clamp((1f - fi) * smokeAlphaMul * alpha);
                        // 每一层稍微偏点位置，避免完全同心圆看着太假
                        float offX = Mathf.sin(seed() * 13f + i * 17f) * i * 0.4f;
                        float offY = Mathf.cos(seed() * 19f + i * 23f) * i * 0.4f;
                        Color sm = (smokeColor != null) ? new Color(smokeColor).a(sa) : new Color(col).a(sa);
                        Draw.color(sm);
                        Fill.circle(e.x + offX, e.y + offY, sR);
                    }
                }
            }

            // ————————— 层 4：描边圆环（尾浪，视觉上尾迹"被推开的空气圈"） —————————
            if (ringCount > 0) {
                for (int i = 0; i < ringCount; i++) {
                    float fi = Mathf.clamp(f - i * ringPhase);
                    if (fi >= 1f) continue;
                    float rR = Mathf.lerp(ringFrom, ringTo, Interp.pow2Out.apply(fi)) * sizeMul;
                    float ra = (1f - fi) * alpha;
                    if (rR > 0.2f && ra > 0.02f) {
                        Lines.stroke(ringStroke * (1f - fi) + 0.2f, new Color(col).a(ra));
                        Lines.circle(e.x, e.y, rR);
                    }
                }
                Draw.reset();
            }
        } finally {
            Draw.blend();      // 还原混合模式（无参 = 回到 normal）
            Draw.reset();   // additive 一定还原，不然整屏都半透明
        }
    }

    /** 辅助：给烟雾抖动一个稳定随机种子（用 e.id 没有传进来时的兜底）*/
    private static float seed() { return 1.3f; }
}
