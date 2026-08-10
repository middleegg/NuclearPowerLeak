package Npl.newSth;

import arc.func.Cons;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.graphics.*;

/**
 * 电磁爆发 · 视觉特效（纯渲染版本，不含任何物理子弹）
 * ============================================================
 * 三阶段视觉（按 e.fin() 进度 0→1 依次）：
 *   Phase 1 [0 ~ phase1End]        光圈 + 5 个从中心向外扩散并逐渐变大的粒子
 *   Phase 2 [phase1End ~ phase2End] 保留光球，外圈 40 格(=320px)处弧线扫一圈成圆环
 *   Phase 3 [phase2End ~ 1.0]      继承第二阶段视觉，不收回
 *
 * 注：此 Effect 不发射任何闪电子弹 —— 发射 LightningBulletType 的逻辑
 *     请在 Building.update() / Cons 中按自己的 tick 周期独立调用 bt.create()。
 * ============================================================
 */
public class LightningStormEffect extends Effect {

    /* ==========================================================
     *                  通用可调参数
     * ========================================================== */

    public Color stormColor     = new Color(0x6F9BFFff);
    public Color lightningColor = new Color(0xE3F2FDff);
    public Color glowColor      = new Color(0x3F5FFFaa);

    public float sizeMul   = 1f;
    public float alphaMul  = 1f;

    /** 光照半径（0 = 不加光）*/
    public float lightRadius = 250f;
    public Color lightColor  = new Color(0x91C4FFff);

    /* ==========================================================
     *                  阶段时间节点（0 ~ 1）
     * ========================================================== */

    /** Phase1 结束 / 进入 Phase2 */
    public float phase1End = 0.25f;
    /** Phase2 结束 / 进入 Phase3（圆环首次填满的时刻）*/
    public float phase2End = 0.55f;

    /* ==========================================================
     *                  核心光球
     * ========================================================== */

    public boolean useCoreBall = true;
    public float   coreSize    = 40f;
    public int     coreGlowLayers = 4;
    public float   coreGlowMul    = 4f;

    /* ==========================================================
     *                  中心光圈（Phase1 就出现的细光圈）
     * ========================================================== */

    public boolean useInnerRing = true;
    public float   innerRingRadius = 56f;   // ≈ 光球外面再套一圈
    public float   innerRingWidth  = 2f;

    /* ==========================================================
     *                  外圈扫弧圆环（40 格 = 320 px）
     * ========================================================== */

    public boolean useOuterArc  = true;
    public float   outerRadius  = 320f;      // 40 * 8 tilesize
    public float   outerWidth   = 3f;
    /** 填满后重新扫过时的高亮弧线宽度（叠加在完整圆环上）*/
    public float   outerSweepWidth = 5f;

    /* ==========================================================
     *                  Phase1 扩散粒子
     * ========================================================== */

    public boolean useParticles = true;
    /** 每波几个粒子 */
    public int     particleBurstCount = 5;
    /** 一波的间隔（tick 数，相对 lifetime）。lifetime*burstInterval 秒一波 */
    public float   particleBurstInterval = 0.08f;
    /** 粒子速度（像素/tick，相对 lifetime 缩放）*/
    public float   particleSpeed = 3.2f;
    public float   particleSizeFrom = 1.5f;
    public float   particleSizeTo   = 7f;
    /** 粒子最远飞多远（超过就不画了）*/
    public float   particleMaxDist = 240f;

    /* ==========================================================
     *                  构造函数
     * ========================================================== */

    public LightningStormEffect() {
        this(300f, 360f, e -> {});
    }

    public LightningStormEffect(float lifetime, float clip, Cons<EffectContainer> cons) {
        super(lifetime, clip, e -> {});
    }

    /* ==========================================================
     *                  渲染
     * ========================================================== */

    @Override
    public void render(final EffectContainer e) {
        final float x = e.x, y = e.y;
        final float fin = Mathf.clamp(e.fin());
        final Color col = (e.color == null) ? stormColor : e.color;
        final float aMul = Mathf.clamp(alphaMul);
        final float sMul = Math.max(0.001f, sizeMul);
        final float life = lifetime;

        // ========== 各阶段进入强度 ==========
        float p1 = Mathf.clamp(fin / Math.max(0.001f, phase1End));                // Phase1 进度
        float p2enter = (fin <= phase1End) ? 0f :                                // Phase2 进入程度
            Mathf.clamp((fin - phase1End) / Math.max(0.001f, phase2End - phase1End));
        float p3 = (fin > phase2End) ? Mathf.clamp((fin - phase2End) / Math.max(0.001f, 1f - phase2End)) : 0f;

        float coreAlpha = (useCoreBall ? 1f : 0f);
        // Phase1 时光球淡入；Phase2 后保持全亮（永不淡出）
        coreAlpha *= Mathf.clamp(p1);
        coreAlpha = Mathf.clamp(coreAlpha) * aMul;

        // ========== 1) 核心光球 ==========
        if (useCoreBall && coreAlpha > 0.01f) {
            float cs = coreSize * sMul * (0.35f + 0.65f * Mathf.slope(p1));
            float pulse = Mathf.sin(Time.time * 8f) * 0.1f;
            cs *= (1f + pulse);
            for (int g = coreGlowLayers; g >= 1; g--) {
                float gs = cs * (0.6f + g * coreGlowMul / coreGlowLayers);
                float ga = coreAlpha * (0.08f + 0.12f * (coreGlowLayers - g) / coreGlowLayers);
                Draw.color(col, ga);
                Fill.circle(x, y, gs);
            }
            Draw.color(col, coreAlpha);
            Fill.circle(x, y, cs);
            Draw.color(lightningColor, coreAlpha * 0.8f);
            Fill.circle(x, y, cs * 0.6f);
            Draw.color(Color.white, coreAlpha);
            Fill.circle(x, y, cs * 0.35f);
        }

        // ========== 2) 中心光圈（细环，Phase1 就有）==========
        if (useInnerRing && p1 > 0.01f) {
            float a = Mathf.clamp(p1) * aMul;
            Draw.color(col, a * 0.7f);
            Lines.stroke(innerRingWidth * sMul);
            Lines.circle(x, y, innerRingRadius * sMul);
            // 外发光
            Draw.color(col, a * 0.25f);
            Lines.stroke(innerRingWidth * sMul * 3f);
            Lines.circle(x, y, innerRingRadius * sMul);
        }

        // ========== 3) Phase1 扩散粒子（5 个一波，向外扩散 + 变大）==========
        if (useParticles && p1 > 0.001f) {
            long base = (long) e.id * 101L;
            // 计算一共多少波
            int waves = (int) (phase1End / Math.max(0.0001f, particleBurstInterval)) + 1;
            float pa = Mathf.clamp(p1) * aMul;

            for (int w = 0; w < waves; w++) {
                float wStart = (float) w * particleBurstInterval;
                if (wStart > phase1End) break;
                float wT = (fin - wStart);
                if (wT < 0f) continue;
                // 粒子持续：剩余 lifetime
                float lifeRatio = Mathf.clamp(wT / (1f - wStart + 0.001f));

                for (int i = 0; i < particleBurstCount; i++) {
                    long seed = base + w * 17L + i * 7L;
                    float ang = Mathf.randomSeed(seed, 0f, 360f);
                    float spd = Mathf.randomSeed(seed + 3L, particleSpeed * 0.7f, particleSpeed * 1.3f);
                    float dist = Math.min(particleMaxDist, life * wT * spd);
                    float sz = Mathf.lerp(particleSizeFrom, particleSizeTo, Mathf.clamp(wT * 2.5f));
                    float al = pa * (1f - Mathf.clamp(wT * 1.3f));
                    if (al < 0.01f || dist < 0.5f) continue;

                    float px = x + Angles.trnsx(ang, dist);
                    float py = y + Angles.trnsy(ang, dist);

                    Draw.color(col, al * 0.4f);
                    Fill.circle(px, py, sz * sMul * 2.2f);
                    Draw.color(lightningColor, al);
                    Fill.circle(px, py, sz * sMul);
                }
            }
        }

        // ========== 4) 外圈扫弧圆环（从 0° → 360° 填满；填满后重复高亮扫弧）==========
        if (useOuterArc && p2enter > 0.001f) {
            float r  = outerRadius * sMul;
            float a  = Mathf.clamp(p2enter) * aMul;

            // 弧进度：p2enter [0,1] 对应第一圈 0→360°
            // p3 [0,1] 对应后续持续扫弧（完整圆环已存在，叠加高亮弧）
            float firstSweepAngle = p2enter * 360f;

            // —— 4a) 第一阶段扫弧 / 完整圆环底色（用已扫过的部分画弧）——
            Draw.color(col, a * 0.9f);
            Lines.stroke(outerWidth * sMul);
            drawArc(x, y, r, 0f, firstSweepAngle);

            // 外发光
            if (firstSweepAngle > 0.5f) {
                Draw.color(glowColor, a * 0.35f);
                Lines.stroke(outerWidth * sMul * 3.2f);
                drawArc(x, y, r, 0f, firstSweepAngle);
            }

            // —— 4b) 第一圈填满后：始终画完整圆环（不消失），再加一道高亮扫弧 ——
            if (p2enter >= 1f) {
                float fullA = a;
                // 完整圆环（略暗一点）
                Draw.color(col, fullA * 0.85f);
                Lines.stroke(outerWidth * sMul);
                Lines.circle(x, y, r);

                Draw.color(glowColor, fullA * 0.3f);
                Lines.stroke(outerWidth * sMul * 3f);
                Lines.circle(x, y, r);

                // 高亮扫弧（长 40° 的亮弧段绕圈转，重新"再一次"填满的感觉）
                float sweepLoop = (Time.time * 1.8f + (long)(e.id * 11L)) % 360f;
                float sweepFrom = sweepLoop - 45f;
                float sweepTo   = sweepLoop;
                Draw.color(lightningColor, fullA);
                Lines.stroke(outerSweepWidth * sMul);
                drawArc(x, y, r, sweepFrom, sweepTo);

                // 扫弧端点亮点
                float hx = x + Angles.trnsx(sweepTo, r);
                float hy = y + Angles.trnsy(sweepTo, r);
                Draw.color(Color.white, fullA);
                Fill.circle(hx, hy, outerSweepWidth * 1.3f * sMul);
            }
        }

        // ========== 5) 光照 ==========
        if (lightRadius > 0.1f) {
            float la = Mathf.clamp(p1) * aMul;
            Drawf.light(x, y, lightRadius * sMul, lightColor, 0.9f * la);
        }
    }

    /** 画一段角度 arc（从 angFrom 到 angTo，以度为单位，自动处理跨 0°）*/
    private void drawArc(float x, float y, float r, float angFrom, float angTo) {
        // 把范围归算到非负连续区间
        float span = angTo - angFrom;
        if (span <= 0f) {
            // 例如 315°→360° 再被包到 315°→ 45°(=405°)，这里转成正向
            angTo += 360f;
            span = angTo - angFrom;
        }
        if (span <= 0.1f) return;
        // 至少 2 段，最多 48 段（1 段 ≈ 7.5°，够细）
        int segs = Math.max(2, (int) Math.ceil(span / 7.5f));
        for (int i = 0; i < segs; i++) {
            float t0 = i / (float) segs;
            float t1 = (i + 1) / (float) segs;
            float a0 = angFrom + span * t0;
            float a1 = angFrom + span * t1;
            float x0 = x + Angles.trnsx(a0, r);
            float y0 = y + Angles.trnsy(a0, r);
            float x1 = x + Angles.trnsx(a1, r);
            float y1 = y + Angles.trnsy(a1, r);
            Lines.line(x0, y0, x1, y1, false);
        }
    }
}
