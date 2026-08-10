package Npl.newSth;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

/**
 * 风暴合成器 · 快速测试版
 * ===============================================================
 * 继承 StormCrafterBlock，所有阶段时间大幅压缩，放置后立刻能看到完整三阶段：
 *   Phase 1: 60 tick (1 秒)  ← 原 300 tick
 *   Phase 2: 扫弧速度 9°/tick，约 40 tick (0.67 秒)扫满  ← 原 3°/tick
 *   Phase 3: 闪电每 5 tick 发射一次  ← 原 8 tick
 *
 * 用途：在 sandbox 模式下放置此方块，立刻观察：
 *   - 光球大小是否符合预期（coreSize=18）
 *   - 圆环生成速度（扫弧一圈耗时）
 *   - 闪电发射频率和数量（2~5 条随机）
 *
 * 不需要原料/电力（sandbox 下直接运行），生产配方只是占位。
 * ===============================================================
 */
public class StormCrafterTestBlock extends StormCrafterBlock {

    public StormCrafterTestBlock(String name) {
        super(name);

        // —— 测试专用：所有时间压缩 ——
        phase1Duration       = 60f;
        // 粒子波间隔：0.5 秒一波（原 1 秒）
        particleWaveInterval = 30f;

        // Phase 2：扫弧 9°/tick，40 tick (0.67 秒) 扫满一圈
        ringSweepSpeedPhase2 = 9f;
        // Phase 3 循环扫弧也快一点
        ringSweepSpeedPhase3 = 4f;

        // 闪电发射：每 5 tick 一次（比原 8 tick 更频繁，方便观察）
        lightningFireInterval = 5f;
        // 每次发射 3~6 条（比原 2~5 更多，方便看效果）
        lightningFireCountMin = 3;
        lightningFireCountMax = 6;
        // 闪电长度：6~28 格（比正式版范围更大，方便观察长度差异）
        lightningLengthMin = 6;
        lightningLengthMax = 28;

        // —— 默认视觉参数（和 stormCrafter 一致，方便对比）——
        coreSize         = 18f;
        innerRingRadius  = 28f;
        innerRingWidth   = 1.5f;
        coreGlowLayers   = 3;
        coreGlowMul      = 3f;
        corePulseSpeed   = 1.5f;
        corePulseAmp     = 0.03f;

        outerRingRadius       = 20f * 8f;   // 160 px
        outerRingWidth        = 2.5f;
        ringSweepSweepAngle   = 45f;
        ringSweepWidth        = 4f;

        lightRadius = 200f;

        // —— 默认闪电子弹（和 stormCrafter 一致）——
        lightningBullet = new LightningBulletType() {{
            damage              = 14f;
            lightningLength      = 22;
            lightningLengthRand  = 5;
            lightningColor       = new Color(0xE3F2FDff);
            lightningType        = new LightningBulletType() {{
                damage           = 7f;
                lightningLength  = 10;
                lightningColor   = new Color(0xB3E5FCff);
            }};
            hittable          = true;
            absorbable         = false;
            collidesGround     = true;
            collidesAir        = true;
            collidesTiles      = true;
        }};
    }

    @Override
    public void init(){
        super.init();
        // ★ super.init() 后强制使用 StormCrafterTestBuild（跳过贴图绘制）
        buildType = StormCrafterTestBuild::new;
    }

    /* ==========================================================
     *                  测试专用 Building
     * ========================================================== */

    public class StormCrafterTestBuild extends StormCrafterBuild {

        /** 测试方块不画原版贴图（避免缺贴图显示紫黑块），
         *  改为画一个简单的纯色方块底座，再画风暴视觉。*/
        @Override
        public void draw() {
            // —— 简易底座（替代贴图）——
            float s = size * 8f;  // tilesize = 8
            Draw.color(stormColor, 0.3f);
            Fill.rect(x, y, s * 2f, s * 2f);
            Draw.color(stormColor, 0.6f);
            Lines.stroke(2f);
            Lines.rect(x - s, y - s, s * 2f, s * 2f);
            Draw.reset();

            // —— 风暴视觉（复用父类 draw 里 phase > 0 的部分）——
            if (phase <= 0) return;
            float cx = x, cy = y;

            float p1FadeIn = Mathf.clamp(phaseTick / 40f);
            float p2Enter  = (phase == 1) ? 0f :
                (phase == 2) ? Mathf.clamp(firstSweepAngle / 90f)
                             : 1f;
            float keepA = Mathf.clamp(p1FadeIn);

            // 1) 核心光球
            if (keepA > 0.01f) {
                float cs = coreSize * (0.35f + 0.65f * keepA);
                float pulse = Mathf.sin(Time.time * corePulseSpeed) * corePulseAmp;
                cs *= (1f + pulse);
                for (int g = coreGlowLayers; g >= 1; g--) {
                    float gs = cs * (0.6f + g * coreGlowMul / coreGlowLayers);
                    float ga = keepA * (0.08f + 0.12f * (coreGlowLayers - g) / coreGlowLayers);
                    Draw.color(stormColor, ga);
                    Fill.circle(cx, cy, gs);
                }
                Draw.color(stormColor, keepA);
                Fill.circle(cx, cy, cs);
                Draw.color(stormBrightColor, keepA * 0.8f);
                Fill.circle(cx, cy, cs * 0.6f);
                Draw.color(Color.white, keepA);
                Fill.circle(cx, cy, cs * 0.35f);
            }

            // 2) 光圈
            if (keepA > 0.01f) {
                float a = keepA * 0.7f;
                Draw.color(stormColor, a);
                Lines.stroke(innerRingWidth);
                Lines.circle(cx, cy, innerRingRadius);
                Draw.color(stormColor, keepA * 0.25f);
                Lines.stroke(innerRingWidth * 3f);
                Lines.circle(cx, cy, innerRingRadius);
            }

            // 3) 粒子
            if (particles.length > 0) {
                float maxLife = particleMaxDist / particleSpeed + 30f;
                for (int i = 0; i < particles.length; i += 4) {
                    float dist = particles[i];
                    float ang  = particles[i + 1];
                    float life = particles[i + 2];
                    if (life <= 0f) continue;
                    float sz = Mathf.lerp(particleSizeFrom, particleSizeTo,
                                           Mathf.clamp(life * 0.05f));
                    float al = keepA * (1f - Mathf.clamp(life / maxLife));
                    if (al < 0.01f || dist < 0.5f) continue;
                    float px = cx + Angles.trnsx(ang, dist);
                    float py = cy + Angles.trnsy(ang, dist);
                    Draw.color(stormColor, al * 0.4f);
                    Fill.circle(px, py, sz * 2.2f);
                    Draw.color(stormBrightColor, al);
                    Fill.circle(px, py, sz);
                }
            }

            // 4) 外圈扫弧 / 完整圆环
            if (phase >= 2 && p2Enter > 0.01f) {
                float r = outerRingRadius;
                float a = keepA * Mathf.clamp(p2Enter);

                if (phase == 2) {
                    float sw = Mathf.clamp(firstSweepAngle, 0f, 360f);
                    Draw.color(stormColor, a * 0.9f);
                    Lines.stroke(outerRingWidth);
                    drawArc(cx, cy, r, 0f, sw);
                    if (sw > 0.5f) {
                        Draw.color(stormGlowColor, a * 0.35f);
                        Lines.stroke(outerRingWidth * 3.2f);
                        drawArc(cx, cy, r, 0f, sw);
                    }
                }

                if (phase == 3) {
                    Draw.color(stormColor, keepA * 0.85f);
                    Lines.stroke(outerRingWidth);
                    Lines.circle(cx, cy, r);
                    Draw.color(stormGlowColor, keepA * 0.3f);
                    Lines.stroke(outerRingWidth * 3f);
                    Lines.circle(cx, cy, r);

                    float to = loopSweepAngle;
                    float from = to - ringSweepSweepAngle;
                    float hx = cx + Angles.trnsx(to, r);
                    float hy = cy + Angles.trnsy(to, r);
                    Draw.color(stormBrightColor, keepA);
                    Lines.stroke(ringSweepWidth);
                    drawArc(cx, cy, r, from, to);
                    Draw.color(Color.white, keepA);
                    Fill.circle(hx, hy, ringSweepWidth * 1.3f);
                }
            }

            // 5) 光照
            if (lightRadius > 0.1f) {
                Drawf.light(cx, cy, lightRadius, stormColor, 0.9f * keepA);
            }

            Draw.reset();
        }

        private void drawArc(float x, float y, float r, float angFrom, float angTo) {
            float span = angTo - angFrom;
            if (span <= 0f) { angTo += 360f; span = angTo - angFrom; }
            if (span <= 0.1f) return;
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
}
