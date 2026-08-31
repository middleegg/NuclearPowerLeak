package Npl.newSth;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * 风暴合成器 StormCrafterBlock
 * ===============================================================
 * 继承 GenericCrafter：保留「原料消耗 / 产物产出 / 配方 / 耗电」全部逻辑。
 *
 * 视觉节奏：蓄能 → 爆发 → 余韵，循环往复（雷暴感）。
 *   Phase 1 蓄能：外部能量粒子由外向内聚入核心；核心等离子球带高频电噪与电弧触须。
 *   Phase 2 充能：外圈"感应线圈"被一圈高亮扫弧逐渐点亮，扫满 360° 线圈成型。
 *   Phase 3 放电循环：
 *       持续以 lightningFireInterval 释放常规闪电（保持输出）；
 *       并以 burstCycle 为周期：
 *         蓄势(核心收紧、线圈增亮) → 爆发(白闪 + 冲击波 + 密集放电) → 余韵(回落)。
 *
 * 每栋 Building 独立状态机，phase 会写入存档；粒子不存档（读档后自然重新生成）。
 * ===============================================================
 */
public class StormCrafterBlock extends GenericCrafter {

    /* ==========================================================
     *                  风暴外观颜色
     * ========================================================== */

    public Color stormColor = new Color(0x6F9BFFff);
    public Color stormBrightColor = new Color(0xE3F2FDff);
    public Color stormGlowColor = new Color(0x3F5FFFaa);

    /* ==========================================================
     *                  Phase 1 聚能粒子
     * ========================================================== */

    /** Phase 1 总时长（tick）*/
    public float phase1Duration = 300f;
    /** 粒子生成波间隔（tick）*/
    public float particleWaveInterval = 60f;
    /** 每波粒子数 */
    public int particlePerWave = 5;
    /** 粒子向心速度（像素/tick）*/
    public float particleSpeed = 2.8f;
    /** 粒子尺寸（远处 → 近核心）*/
    public float particleSizeFrom = 1.5f;
    public float particleSizeTo   = 7f;
    /** 粒子生成半径（由该半径向内收拢）*/
    public float particleMaxDist = 240f;
    /** 粒子螺旋偏移强度 */
    public float particleSwirl = 0.8f;

    /* ==========================================================
     *                  外圈感应线圈 & 扫弧
     * ========================================================== */

    /** 线圈半径（px）*/
    public float outerRingRadius = 40f * 8f;
    /** 线圈线宽 */
    public float outerRingWidth = 3f;
    /** Phase 2 充能扫弧速度（度/tick）*/
    public float ringSweepSpeedPhase2 = 3f;
    /** Phase 3 循环高亮扫弧速度（度/tick）*/
    public float ringSweepSpeedPhase3 = 1.8f;
    /** 循环高亮扫弧覆盖角度（度）*/
    public float ringSweepSweepAngle = 45f;
    /** 循环高亮扫弧线宽 */
    public float ringSweepWidth = 5f;
    /** 线圈绕组数量（径向短刻线）*/
    public int ringCoilTicks = 40;
    /** 允许实际完成合成的最低阶段：1/2/3 */
    public int craftStartPhase = 3;

    /* ==========================================================
     *                  等离子核心
     * ========================================================== */

    /** 核心基础尺寸（px）*/
    public float coreSize = 40f;
    /** 内层细环半径 / 线宽 */
    public float innerRingRadius = 56f;
    public float innerRingWidth  = 2f;
    /** 核心心跳速度 / 幅度 */
    public float corePulseSpeed = 8f;
    public float corePulseAmp   = 0.1f;
    /** 外发光层数 / 层厚系数 */
    public int   coreGlowLayers = 20;
    public float coreGlowMul    = 4f;
    /** 核心电弧触须数量 */
    public int coreTendrils = 6;

    /** 光照半径（<=0 不加光照）*/
    public float lightRadius = 260f;

    /* ==========================================================
     *                  雷暴爆发节奏（Phase 3）
     * ========================================================== */

    /** 两次大爆发之间的周期（tick）*/
    public float burstCycle = 220f;
    /** 爆发前蓄势时长（tick）：核心收紧、线圈增亮 */
    public float burstChargeTime = 70f;
    /** 爆发白闪衰减时长（tick）*/
    public float burstFlashTime = 26f;
    /** 冲击波扩散时长（tick）*/
    public float shockwaveTime = 34f;
    /** 冲击波最大半径 / 线宽 */
    public float shockwaveMaxRadius = 170f;
    public float shockwaveWidth = 5f;

    /* ==========================================================
     *                  LightningBulletType 发射
     * ========================================================== */

    /** 使用的闪电子弹类型（为 null 则不发射）*/
    public BulletType lightningBullet = null;
    /** 常规放电间隔（tick）*/
    public float lightningFireInterval = 10f;
    /** 常规放电每次条数范围 */
    public int lightningFireCountMin = 2;
    public int lightningFireCountMax = 5;
    /** 大爆发时额外放电条数范围 */
    public int burstBoltMin = 5;
    public int burstBoltMax = 9;
    /** 单条闪电长度范围（格）*/
    public int lightningLengthMin = 8;
    public int lightningLengthMax = 22;

    public StormCrafterBlock(String name) {
        super(name);
    }

    /* ==========================================================
     *                  Building
     * ========================================================== */

    public class StormCrafterBuild extends GenericCrafterBuild {

        /* ---------- 阶段状态（写入存档）---------- */
        /** 0=未启动 1=蓄能 2=充能 3=放电循环 */
        public int phase = 0;
        public float phaseTick = 0f;
        public float firstSweepAngle = 0f;
        public float loopSweepAngle = 0f;
        public float fireCd = 0f;
        public float burstTimer = 0f;
        public float lastProgress = 0f;

        /* ---------- 临时视觉状态（不存档）---------- */
        public float burstFlash = 0f;
        public float shockwave = 1f;
        public float particleWaveCd = 0f;
        /** 每 4 float 一个粒子：dist, angle, life, swirl */
        public float[] particles = new float[0];

        @Override
        public void updateTile() {
            super.updateTile();

            if (phase < craftStartPhase && craftTime > 0f) {
                progress = Math.min(progress, craftTime - 0.001f);
                warmup = Mathf.approachDelta(warmup, 0f, 0.1f);
            }

            boolean producing = progress > lastProgress + 0.0001f;
            if (phase == 0 && producing) {
                phase = 1;
                phaseTick = 0f;
                particleWaveCd = 0f;
            }
            lastProgress = progress;

            boolean enabled = efficiency > 0f;
            float delta = Time.delta;

            phaseTick += delta;

            if (phase == 1) {
                particleWaveCd -= delta;
                if (particleWaveCd <= 0f) {
                    spawnParticleWave();
                    particleWaveCd += Math.max(1f, particleWaveInterval);
                }
                if (phaseTick >= phase1Duration) {
                    phase = 2;
                    firstSweepAngle = 0f;
                }
            }

            if (phase == 2) {
                firstSweepAngle += ringSweepSpeedPhase2 * delta;
                if (firstSweepAngle >= 360f) {
                    firstSweepAngle = 360f;
                    phase = 3;
                    loopSweepAngle = 0f;
                    fireCd = 0f;
                    burstTimer = burstCycle;
                    shockwave = 1f;
                }
            }

            if (phase == 3) {
                loopSweepAngle = (loopSweepAngle + ringSweepSpeedPhase3 * delta) % 360f;
                if (loopSweepAngle < 0f) loopSweepAngle += 360f;

                if (enabled) {
                    burstTimer += delta;
                    if (burstTimer >= burstCycle) {
                        burstTimer -= burstCycle;
                        triggerBurst();
                    }
                    if (lightningBullet != null) {
                        fireCd += delta;
                        if (fireCd >= lightningFireInterval) {
                            fireCd = 0f;
                            fireLightning(lightningFireCountMin, lightningFireCountMax);
                        }
                    }
                }

                burstFlash = Mathf.approachDelta(burstFlash, 0f, delta / Math.max(1f, burstFlashTime));
                shockwave = Mathf.clamp(shockwave + delta / Math.max(1f, shockwaveTime));
            }

            updateParticles();
        }

        protected void triggerBurst() {
            burstFlash = 1f;
            shockwave = 0f;
            if (efficiency > 0f && lightningBullet != null) {
                fireLightning(burstBoltMin, burstBoltMax);
            }
        }

        protected void spawnParticleWave() {
            int count = Math.max(0, particlePerWave);
            if (count == 0) return;

            float[] np = new float[particles.length + count * 4];
            System.arraycopy(particles, 0, np, 0, particles.length);
            int base = particles.length;
            long timeSeed = Time.millis() + tile.pos() * 7L;

            for (int i = 0; i < count; i++) {
                long s = timeSeed + i * 131L;
                np[base + i * 4    ] = Mathf.randomSeed(s, particleMaxDist * 0.55f, particleMaxDist);
                np[base + i * 4 + 1] = Mathf.randomSeed(s + 1L, 0f, 360f);
                np[base + i * 4 + 2] = 0f;
                np[base + i * 4 + 3] = Mathf.randomSeed(s + 2L, -1f, 1f);
            }
            particles = np;
        }

        protected void updateParticles() {
            int n = particles.length;
            if (n == 0) return;

            int alive = 0;
            for (int i = 0; i < n; i += 4) {
                float dist = particles[i] - particleSpeed * Time.delta;
                particles[i] = dist;
                particles[i + 1] += particles[i + 3] * particleSwirl * Time.delta;
                particles[i + 2] += Time.delta;
                if (dist > 1f) alive++;
            }

            if (alive * 4 < n * 3 / 4) {
                float[] np = new float[alive * 4];
                int w = 0;
                for (int i = 0; i < n; i += 4) {
                    if (particles[i] > 1f) {
                        np[w++] = particles[i];
                        np[w++] = particles[i + 1];
                        np[w++] = particles[i + 2];
                        np[w++] = particles[i + 3];
                    }
                }
                particles = np;
            }
        }

        /**
         * 从建筑中心向随机角度发射闪电。
         * count 条，每条独立随机角度与长度。
         * 注意：LightningBulletType 是共享实例，长度字段用完必须还原，
         * 否则会污染该 bullet 的所有其他使用者。
         */
        protected void fireLightning(int countMin, int countMax) {
            BulletType bt = lightningBullet;
            if (bt == null) return;

            long baseSeed = Time.millis() * 31L + tile.pos() * 7L;
            int count = (countMax > countMin)
                ? Mathf.randomSeed(baseSeed, countMin, countMax + 1)
                : countMin;

            LightningBulletType lbt = (bt instanceof LightningBulletType && lightningLengthMax > lightningLengthMin)
                ? (LightningBulletType) bt : null;
            int oldLen = 0, oldRand = 0;
            if (lbt != null) {
                oldLen = lbt.lightningLength;
                oldRand = lbt.lightningLengthRand;
            }

            for (int i = 0; i < count; i++) {
                long seed = baseSeed + i * 131L;
                float angle = Mathf.randomSeed(seed + 7L, 0f, 360f);
                if (lbt != null) {
                    lbt.lightningLength = Mathf.randomSeed(seed + 19L, lightningLengthMin, lightningLengthMax + 1);
                    lbt.lightningLengthRand = 0;
                }
                bt.create(this, team, x, y, angle, 1f);
            }

            if (lbt != null) {
                lbt.lightningLength = oldLen;
                lbt.lightningLengthRand = oldRand;
            }
        }

        /* ==========================================================
         *                  渲染
         * ========================================================== */

        @Override
        public void draw() {
            super.draw();
            if (phase <= 0) return;

            float cx = x, cy = y;
            float keepA = Mathf.clamp(phaseTick / 40f);
            float p2Enter = (phase == 1) ? 0f
                : (phase == 2) ? Mathf.clamp(firstSweepAngle / 90f)
                               : 1f;

            float charge = 0f;
            if (phase == 3 && burstCycle > burstChargeTime) {
                float start = burstCycle - burstChargeTime;
                if (burstTimer >= start) {
                    charge = Mathf.clamp((burstTimer - start) / burstChargeTime);
                }
            }
            float flash = burstFlash;

            float cs = coreSize * (0.35f + 0.65f * keepA);
            if (keepA > 0.01f) {
                int tb = (int) (Time.time * 0.5f);
                float noise = Mathf.randomSeed(tb * 7L + tile.pos(), -0.05f, 0.05f);
                float pulse = Mathf.sin(Time.time * corePulseSpeed) * corePulseAmp;
                cs *= 1f + pulse + noise - charge * 0.18f + flash * 0.5f;
            }

            drawCore(cx, cy, cs, keepA, flash);
            drawInnerRing(cx, cy, keepA);
            drawParticles(cx, cy, keepA);
            drawCoil(cx, cy, keepA, p2Enter, charge, flash);

            if (shockwave < 1f && keepA > 0.01f) {
                Draw.color(stormBrightColor, keepA * (1f - shockwave) * 0.85f);
                Lines.stroke(shockwaveWidth * (1f - shockwave));
                Lines.circle(cx, cy, shockwaveMaxRadius * shockwave);
            }

            if (lightRadius > 0.1f) {
                float boost = 1f + charge * 0.6f + flash * 1.5f;
                Drawf.light(cx, cy, lightRadius * (1f + flash * 0.6f), stormColor,
                            Math.min(1f, 0.9f * keepA * boost));
            }

            Draw.reset();
        }

        private void drawCore(float cx, float cy, float cs, float keepA, float flash) {
            if (keepA <= 0.01f || cs <= 0.01f) return;

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

            if (flash > 0.01f) {
                Draw.color(Color.white, keepA * flash);
                Fill.circle(cx, cy, cs * (1.1f + flash * 0.8f));
            }

            if (coreTendrils <= 0) return;
            int tb = (int) (Time.time * 0.5f);
            Draw.color(stormBrightColor, keepA * 0.9f);
            Lines.stroke(1.6f);
            for (int i = 0; i < coreTendrils; i++) {
                long s = tb * 131L + i * 977L + tile.pos();
                float ang = Mathf.randomSeed(s, 0f, 360f);
                float len = cs * (1.25f + Mathf.randomSeed(s + 1L, 0.2f, 0.75f));
                float x0 = cx + Angles.trnsx(ang, cs * 0.95f);
                float y0 = cy + Angles.trnsy(ang, cs * 0.95f);
                float midAng = ang + Mathf.randomSeed(s + 2L, -22f, 22f);
                float mx = cx + Angles.trnsx(midAng, (cs + len) * 0.5f);
                float my = cy + Angles.trnsy(midAng, (cs + len) * 0.5f);
                float x1 = cx + Angles.trnsx(ang, len);
                float y1 = cy + Angles.trnsy(ang, len);
                Lines.line(x0, y0, mx, my, false);
                Lines.line(mx, my, x1, y1, false);
            }
        }

        private void drawInnerRing(float cx, float cy, float keepA) {
            if (keepA <= 0.01f) return;
            Draw.color(stormColor, keepA * 0.7f);
            Lines.stroke(innerRingWidth);
            Lines.circle(cx, cy, innerRingRadius);
            Draw.color(stormColor, keepA * 0.25f);
            Lines.stroke(innerRingWidth * 3f);
            Lines.circle(cx, cy, innerRingRadius);
        }

        private void drawParticles(float cx, float cy, float keepA) {
            if (particles.length == 0 || keepA <= 0.01f) return;

            for (int i = 0; i < particles.length; i += 4) {
                float dist = particles[i];
                if (dist <= 1f) continue;
                float ang = particles[i + 1];
                float t = Mathf.clamp(1f - dist / particleMaxDist);
                float sz = Mathf.lerp(particleSizeFrom, particleSizeTo, t);
                float al = keepA * (0.2f + 0.8f * t);
                if (al < 0.01f) continue;

                float px = cx + Angles.trnsx(ang, dist);
                float py = cy + Angles.trnsy(ang, dist);
                Draw.color(stormColor, al * 0.4f);
                Fill.circle(px, py, sz * 2.2f);
                Draw.color(stormBrightColor, al);
                Fill.circle(px, py, sz);
            }
        }

        private void drawCoil(float cx, float cy, float keepA, float p2Enter, float charge, float flash) {
            if (phase < 2 || p2Enter <= 0.01f) return;

            float r = outerRingRadius;
            float a = keepA * p2Enter;

            if (phase == 2) {
                float sw = Mathf.clamp(firstSweepAngle, 0f, 360f);
                Draw.color(stormColor, a * 0.9f);
                Lines.stroke(outerRingWidth);
                Lines.arc(cx, cy, r, sw / 360f, -90f);
                if (sw > 0.5f) {
                    Draw.color(stormGlowColor, a * 0.35f);
                    Lines.stroke(outerRingWidth * 3.2f);
                    Lines.arc(cx, cy, r, sw / 360f, -90f);
                }
                return;
            }

            float baseA = keepA * (0.85f + charge * 0.15f + flash * 0.5f);
            Draw.color(stormColor, Math.min(1f, baseA));
            Lines.stroke(outerRingWidth);
            Lines.circle(cx, cy, r);
            Draw.color(stormGlowColor, keepA * (0.3f + charge * 0.3f + flash * 0.4f));
            Lines.stroke(outerRingWidth * 3f);
            Lines.circle(cx, cy, r);

            if (ringCoilTicks > 0) {
                Draw.color(stormColor, keepA * 0.5f);
                Lines.stroke(2f);
                for (int i = 0; i < ringCoilTicks; i++) {
                    float ca = i * 360f / ringCoilTicks;
                    float ix = cx + Angles.trnsx(ca, r - outerRingWidth * 2f);
                    float iy = cy + Angles.trnsy(ca, r - outerRingWidth * 2f);
                    float ox = cx + Angles.trnsx(ca, r + outerRingWidth * 2f);
                    float oy = cy + Angles.trnsy(ca, r + outerRingWidth * 2f);
                    Lines.line(ix, iy, ox, oy, false);
                }
            }

            float to = loopSweepAngle;
            float from = to - ringSweepSweepAngle;
            float hx = cx + Angles.trnsx(to, r);
            float hy = cy + Angles.trnsy(to, r);
            Draw.color(stormBrightColor, keepA);
            Lines.stroke(ringSweepWidth + charge * 2f);
            Lines.arc(cx, cy, r, ringSweepSweepAngle / 360f, from);
            Draw.color(Color.white, keepA);
            Fill.circle(hx, hy, ringSweepWidth * 1.3f);

            if (flash > 0.01f) {
                Draw.color(Color.white, keepA * flash * 0.8f);
                Lines.stroke(6f * flash);
                Lines.circle(cx, cy, r * (1f + (1f - flash) * 0.15f));
            }
        }

        /* ==========================================================
         *                  存档
         * ========================================================== */

        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(phase);
            write.f(phaseTick);
            write.f(firstSweepAngle);
            write.f(loopSweepAngle);
            write.f(fireCd);
            write.f(burstTimer);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            phase           = read.i();
            phaseTick       = read.f();
            firstSweepAngle = read.f();
            loopSweepAngle  = read.f();
            fireCd          = read.f();
            burstTimer      = read.f();
            lastProgress    = 0f;
            burstFlash      = 0f;
            shockwave       = 1f;
        }
    }
}
