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
 * 继承 GenericCrafter：保留所有「原料消耗 / 产物产出 / 配方 / 耗电」逻辑。
 * 额外新增：三阶段视觉 + 真正的 LightningBulletType 发射。
 *
 * 三阶段（每栋 Building 独立状态机）：
 *   Phase 1 [0 ~ 300 tick = 5 秒]
 *       中心光圈 + 中心上方淡入光球；每 60 tick（1 秒）一波 5 粒子，
 *       从中心向随机 360° 发散，发散途中尺寸不断变大。
 *       ⚠ Phase 1 只进行一次，不复用。
 *
 *   Phase 2 [300 tick 起，直到填满圆环]
 *       保留光球，Phase 1 粒子不再生成；
 *       光球外 40 格（= 320 px）处，从 0° 起开始画弧线，
 *       弧线扫到 90° → 显示为 1/4 圆环；
 *       弧线扫到 360° → 完整圆环出现（圆环不会消失）。
 *       填满圆环后立刻进入 Phase 3。
 *       填满后继续循环：一道高亮扫弧不断在完整圆环上绕圈旋转。
 *
 *   Phase 3 [圆环首次填满后，永久持续，不收回]
 *       继承 Phase 2 全部视觉（光球 + 完整圆环 + 循环高亮扫弧）。
 *       额外：每隔 lightningFireInterval tick，从 Building 位置向随机 360°
 *       发射 lightningBullet（LightningBulletType），一次发射
 *       lightningFireCount 条（各自独立随机角度）。
 *       只要方块有效且有电，这个发射就永远继续，不自动结束。
 * ===============================================================
 * 使用方法：
 *   在 NuBlocks 中：
 *     stormCrafter = new StormCrafterBlock("storm-crafter") {{
 *         requirements(Category.crafting, with(...));
 *         size = 2; health = 800;
 *         hasItems = hasPower = true;
 *         craftTime = 200f;
 *         outputItem = new ItemStack(NuItems.magent, 1);
 *         consumeItems(ItemStack.with(NuItems.bigIron, 2));
 *         consumePower(3.0f);
 *
 *         // ======= 风暴专属字段 =======
 *         stormColor = Color.valueOf("6F9BFF");
 *         lightningBullet = new LightningBulletType(){{
 *             damage = 12f;
 *             lightningLength = 22;
 *             lightningLengthRand = 5;
 *             lightningColor = Color.valueOf("E3F2FD");
 *             lightningType = new LightningBulletType(){{ // 二级分支链闪
 *                 damage = 6f;
 *                 lightningLength = 10;
 *                 lightningColor = Color.valueOf("B3E5FC");
 *             }};
 *         }};
 *         lightningFireInterval = 10;
 *         lightningFireCount = 2;
 *     }};
 *
 * 需要贴图：
 *   storm-crafter.png（方块本体，GenericCrafter 自动加载）
 */
public class StormCrafterBlock extends GenericCrafter {

    /* ==========================================================
     *                  风暴外观颜色
     * ========================================================== */

    /** 风暴主色（光球、光圈、圆环、闪电气氛光）*/
    public Color stormColor = new Color(0x6F9BFFff);

    /** 风暴亮色（闪电本体、圆环扫弧高亮）*/
    public Color stormBrightColor = new Color(0xE3F2FDff);

    /** 风暴暗色（外发光层）*/
    public Color stormGlowColor = new Color(0x3F5FFFaa);

    /* ==========================================================
     *                  Phase 1 粒子
     * ========================================================== */

    /** Phase 1 总时长（tick），默认 300 = 5 秒 */
    public float phase1Duration = 300f;
    /** 粒子间隔波（默认 60 tick = 每 1 秒一波）*/
    public float particleWaveInterval = 60f;
    /** 每波粒子数 */
    public int particlePerWave = 5;
    /** 粒子速度（像素/tick）*/
    public float particleSpeed = 2.8f;
    /** 粒子尺寸范围 */
    public float particleSizeFrom = 1.5f;
    public float particleSizeTo   = 7f;
    /** 粒子最远飞多远 */
    public float particleMaxDist = 240f;

    /* ==========================================================
     *                  圆环 & 扫弧
     * ========================================================== */

    /** 外圈圆环半径（= 40 格 * 8 = 320 px）*/
    public float outerRingRadius = 40f * 8f;
    /** 圆环宽度 */
    public float outerRingWidth = 3f;
    /** 第一圈扫弧速度（度/tick），默认 3°/tick → 扫完一圈 120 tick ≈ 2 秒 */
    public float ringSweepSpeedPhase2 = 3f;
    /** 填满后，循环高亮扫弧的速度（度/tick）*/
    public float ringSweepSpeedPhase3 = 1.8f;
    /** 循环高亮扫弧的覆盖角度（度）*/
    public float ringSweepSweepAngle = 45f;
    /** 循环高亮扫弧的宽度 */
    public float ringSweepWidth = 5f;
    /** 允许实际完成合成的最低阶段：
     *   1 = Phase 1（粒子阶段）就允许生产（原版行为，无延迟）
     *   2 = Phase 2（扫弧阶段）起才允许生产 ← 默认值
     *   3 = 只有 Phase 3（完整圆环 + 闪电）才允许生产 */
    public int craftStartPhase = 3;
    /* ==========================================================
     *                  光球 / 光圈
     * ========================================================== */

    /** 光球大小（px）*/
    public float coreSize = 40f;
    /** 光圈半径（套在光球外面的细环）*/
    public float innerRingRadius = 56f;
    public float innerRingWidth  = 2f;
    /** 光球迷你心跳速度 */
    public float corePulseSpeed = 8f;
    public float corePulseAmp   = 0.1f;
    public int   coreGlowLayers = 4;
    public float coreGlowMul    = 4f;

    /** 光照半径（= 0 不加光照）*/
    public float lightRadius = 260f;

    /* ==========================================================
     *                  LightningBulletType 发射
     * ========================================================== */

    /** Phase 3 使用的闪电子弹类型（必填，不填就不发射）*/
    public BulletType lightningBullet = null;

    /** 每隔多少 tick 发射一次闪电 */
    public float lightningFireInterval = 10f;

    /** 每次发射最少几条（各自随机角度）*/
    public int lightningFireCountMin = 2;
    /** 每次发射最多几条（每次实际数量在此范围内随机）*/
    public int lightningFireCountMax = 5;

    /** 闪电最短长度（格）。每次发射前随机一条 [min, max] 的长度写入 LightningBulletType */
    public int lightningLengthMin = 8;
    /** 闪电最短长度（格）。<= min 时退化为固定长度 */
    public int lightningLengthMax = 22;

    /* ==========================================================
     *                  构造
     * ========================================================== */

    public StormCrafterBlock(String name) {
        super(name);
    }

    /* ==========================================================
     *                  Building：状态机 + 渲染 + I/O
     * ========================================================== */

    public class StormCrafterBuild extends GenericCrafterBuild {

        /* ---------- 阶段状态（写入 save）---------- */
        /** 0=未启动 1=Phase1(粒子) 2=Phase2(扫弧中) 3=Phase3(永久) */
        public int phase = 0;
        /** 当前阶段累计 tick（启动 Phase1 起持续累加）*/
        public float phaseTick = 0f;
        /** 第一圈扫弧的累计角度（0→360 表示完成第一圈）*/
        public float firstSweepAngle = 0f;
        /** Phase3 循环高亮扫弧累计角度（0→∞，mod 360 用）*/
        public float loopSweepAngle = 0f;
        /** Phase3 闪电发射计时 */
        public float fireCd = 0f;
        /** 上一帧 progress（用于检测"正在生产"）*/
        public float lastProgress = 0f;

        /* ---------- 粒子（不写入 save，读档后没粒子也无所谓）---------- */
        /** 每 4 float 为一个粒子：dist, angle, life, seed */
        public float[] particles = new float[0];

        @Override
        public void updateTile() {
            // —— 基础合成逻辑（原 GenericCrafter） ——
            super.updateTile();
            if (phase < craftStartPhase && craftTime > 0f) {
                progress = Math.min(progress, craftTime - 0.001f);
                warmup = Mathf.approachDelta(warmup, 0f, 0.1f);
            }
            // —— 阶段机：放置后不立即启动，只有真正生产时才启动。
            //    启动条件：phase == 0 且 progress > lastProgress（配方进度在推进，
            //    说明原料+电力都满足，正在实际生产）。
            //    启动后 Phase3 永久持续，不因生产停止而收回（符合用户"不收回"需求）。
            boolean producing = (progress > lastProgress + 0.0001f);

            if (phase == 0 && producing) {
                // 检测到生产开始 → 启动三阶段视觉（仅一次，读档后若 phase>0 不再触发）
                phase = 1;
                phaseTick = 0f;
            }
            lastProgress = progress;

            // Phase3 闪电是否允许发射 = 有电
            boolean enabled = efficiency > 0f;

            phaseTick += 1f;

            // ---------- Phase 1：粒子 ----------
            if (phase == 1) {
                // 每 particleWaveInterval tick 一波粒子
                if ((int)phaseTick % (int)particleWaveInterval == 0 &&
                    Mathf.equal(phaseTick, (float)(int)phaseTick, 0.4f)) {
                    spawnParticleWave();
                }
                // 到点自动进 Phase 2
                if (phaseTick >= phase1Duration) {
                    phase = 2;
                    firstSweepAngle = 0f;
                }
            }

            // ---------- Phase 2：扫弧填圆环 ----------
            if (phase == 2) {
                firstSweepAngle += ringSweepSpeedPhase2;
                // 扫弧到 360° = 填满 → 进入 Phase3
                if (firstSweepAngle >= 360f) {
                    firstSweepAngle = 360f;
                    phase = 3;
                    loopSweepAngle = 0f;
                    fireCd = 0f;
                }
            }

            // ---------- Phase 3：永久保持 + 循环高亮扫弧 + 发射闪电 ----------
            if (phase == 3) {
                loopSweepAngle += ringSweepSpeedPhase3;
                loopSweepAngle = loopSweepAngle % 360f;
                if (loopSweepAngle < 0f) loopSweepAngle += 360f;

                if (enabled && lightningBullet != null) {
                    fireCd += 1f;
                    if (fireCd >= lightningFireInterval) {
                        fireCd = 0f;
                        fireLightning();
                    }
                }
            }

            // ---------- 所有阶段：更新已存在的粒子（向外移动 + 生命衰减）----------
            updateParticles();
        }

        /** 生成一波粒子（粒子数据写入 particles 数组）*/
        protected void spawnParticleWave() {
            float[] np = new float[particles.length + particlePerWave * 4];
            System.arraycopy(particles, 0, np, 0, particles.length);
            int base = particles.length;
            long timeSeed = Time.millis();
            for (int i = 0; i < particlePerWave; i++) {
                long s = timeSeed + i * 131L + tile.pos() * 7L;
                float ang = Mathf.randomSeed(s, 0f, 360f);
                long seed = (long)(ang * 100f) ^ s;
                np[base + i * 4    ] = 0f;                 // dist
                np[base + i * 4 + 1] = ang;                // angle
                np[base + i * 4 + 2] = 0f;                 // life（从 0 起，tick 数）
                np[base + i * 4 + 3] = (float)(seed & 0x7FFFFFFF);  // seed（float，够用）
            }
            particles = np;
        }

        /** 更新粒子（移动物理 + 裁剪生命结束的）*/
        protected void updateParticles() {
            if (particles.length == 0) return;
            int alive = 0;
            for (int i = 0; i < particles.length; i += 4) {
                float life = particles[i + 2] + 1f;
                float dist = particles[i] + particleSpeed;
                if (dist > particleMaxDist) dist = particleMaxDist;
                particles[i]     = dist;
                particles[i + 2] = life;
                // 存活判定：至少还有一小段寿命能画出来
                float maxLife = particleMaxDist / particleSpeed + 30f;
                if (life < maxLife && dist < particleMaxDist - 0.5f) alive++;
            }
            // 压缩死亡粒子
            if (alive * 4 < particles.length * 3 / 4) {
                float[] np = new float[alive * 4];
                int w = 0;
                float maxLife = particleMaxDist / particleSpeed + 30f;
                for (int i = 0; i < particles.length; i += 4) {
                    if (particles[i + 2] < maxLife && particles[i] < particleMaxDist - 0.5f) {
                        np[w++] = particles[i];
                        np[w++] = particles[i+1];
                        np[w++] = particles[i+2];
                        np[w++] = particles[i+3];
                    }
                }
                particles = np;
            }
        }

        /** 从 Building 中心随机角度发射 LightningBullet
         *  数量本身也是随机的（lightningFireCountMin ~ lightningFireCountMax）
         *  每条闪电长度也是随机的（lightningLengthMin ~ lightningLengthMax）*/
        protected void fireLightning() {
            BulletType bt = lightningBullet;
            if (bt == null) return;
            long baseSeed = Time.millis() * 31L + tile.pos() * 7L;
            int count = (lightningFireCountMax > lightningFireCountMin)
                ? (int) Mathf.randomSeed(baseSeed, lightningFireCountMin, lightningFireCountMax + 1)
                : lightningFireCountMin;

            // 只对 LightningBulletType 生效：动态设置 lightningLength / lightningLengthRand
            // 实际长度 = lightningLength + random(0, lightningLengthRand) = [min, max]
            mindustry.entities.bullet.LightningBulletType lbt = null;
            if (lightningLengthMax > lightningLengthMin && bt instanceof mindustry.entities.bullet.LightningBulletType) {
                lbt = (mindustry.entities.bullet.LightningBulletType) bt;
            }

            for (int i = 0; i < count; i++) {
                long seed = baseSeed + i * 131L;
                float angle = Mathf.randomSeed(seed + 7L, 0f, 360f);

                // 每条闪电随机长度：把 [min,max] 拆成 base + rand
                if (lbt != null) {
                    int len = (int) Mathf.randomSeed(seed + 19L, lightningLengthMin, lightningLengthMax + 1);
                    lbt.lightningLength = len;
                    lbt.lightningLengthRand = 0;   // 长度已随机，不再叠加 rand
                }

                bt.create(this, team, x, y, angle, 1f);
            }
        }

        /* ==========================================================
         *                  渲染
         * ========================================================== */

        @Override
        public void draw() {
            super.draw();   // 画 sprites/storm-crafter.png + 进度条等原版内容

            // 方块上的风暴视觉
            if (phase <= 0) return;
            float cx = x, cy = y;

            // 阶段可见性
            float p1FadeIn = Mathf.clamp(phaseTick / 40f);           // Phase1 前 40 tick 淡入
            float p2Enter  = (phase == 1) ? 0f :
                (phase == 2) ? Mathf.clamp(firstSweepAngle / 90f)   // 扫过 90° 已很明显
                             : 1f;
            float keepA = Mathf.clamp(p1FadeIn);

            // -------- 1) 核心光球（Phase1 淡入，之后一直存在）--------
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

            // -------- 2) 光圈（细环套光球外，Phase1 起就有）--------
            if (keepA > 0.01f) {
                float a = keepA * 0.7f;
                Draw.color(stormColor, a);
                Lines.stroke(innerRingWidth);
                Lines.circle(cx, cy, innerRingRadius);
                Draw.color(stormColor, keepA * 0.25f);
                Lines.stroke(innerRingWidth * 3f);
                Lines.circle(cx, cy, innerRingRadius);
            }

            // -------- 3) 粒子 --------
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

            // -------- 4) 外圈扫弧 / 完整圆环 --------
            if (phase >= 2 && p2Enter > 0.01f) {
                float r = outerRingRadius;
                float a = keepA * Mathf.clamp(p2Enter);

                if (phase == 2) {
                    // 第一阶段扫弧
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
                    // 完整圆环底色（不消失）
                    Draw.color(stormColor, keepA * 0.85f);
                    Lines.stroke(outerRingWidth);
                    Lines.circle(cx, cy, r);
                    Draw.color(stormGlowColor, keepA * 0.3f);
                    Lines.stroke(outerRingWidth * 3f);
                    Lines.circle(cx, cy, r);

                    // 循环高亮扫弧（再一次）
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

            // -------- 5) 光照 --------
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

        /* ==========================================================
         *                  统计 / 写入 save
         * ========================================================== */

        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(phase);
            write.f(phaseTick);
            write.f(firstSweepAngle);
            write.f(loopSweepAngle);
            write.f(fireCd);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            phase           = read.i();
            phaseTick       = read.f();
            firstSweepAngle = read.f();
            loopSweepAngle  = read.f();
            fireCd          = read.f();
            lastProgress    = 0f;  // 读档后重置，下一帧 super.updateTile() 会推进 progress
            // 读档后粒子数组是空的（不写入），不影响后续功能
        }
    }
}
