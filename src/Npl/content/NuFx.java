package Npl.content;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.units.UnitAssembler.*;
import Npl.content.*;
import Npl.newSth.BulletTailEffect;
import Npl.newSth.expEffect;
import Npl.newSth.LightningStormEffect;
import Npl.newSth.TextPopupEffect;

import static arc.graphics.g2d.Draw.rect;
import static arc.graphics.g2d.Draw.*;
import static arc.graphics.g2d.Lines.*;
import static arc.math.Angles.*;
import static mindustry.Vars.*;

/**
 * 自定义特效集合（类比 Mindustry 原版 Fx 类）
 * 用法：在任何地方调用 NuFx.explosion1.at(x, y, Color.scarlet); 即可
 */
public class NuFx {

    // ========================================================
    // 爆炸 1：冲击波圈 + 12 方向飞散粒子
    // ========================================================
    public static Effect explosion1 = new Effect(30f, 200f, e -> {
        float f = e.fin();

        // ① 冲击波圈（Out 曲线：初快后慢，物理感）
        float ringR = 80f * Interp.pow2Out.apply(f);
        float ringA = 1f - f;
        if (ringA > 0.02f) {
            Lines.stroke(3f - 2f * f, new Color(e.color).a(ringA));
            Lines.circle(e.x, e.y, ringR);
        }

        // ② 12 颗粒子向 12 个方向飞
        int particles = 12;
        float flyDistance = 55f * Interp.pow2Out.apply(f);
        float particleAlpha = 1f - Mathf.pow(f, 2f);
        if (particleAlpha > 0.02f) {
            Draw.color(new Color(e.color).a(particleAlpha));
            for (int i = 0; i < particles; i++) {
                float angle = i * (360f / particles);
                float px = e.x + Angles.trnsx(angle, flyDistance);
                float py = e.y + Angles.trnsy(angle, flyDistance);
                float size = (3f - 2.5f * f) * 2f;
                Fill.square(px, py, size, angle);
            }
            Draw.color();  // 恢复默认颜色（白）
        }

        Draw.reset(); // 防止颜色/线宽污染后续渲染
    });

    // ========================================================
    // Pale 烟：PaleColor → PaleBackColor 的渐变色烟雾小圆
    // ========================================================
    public static Effect PaleSmoke = new Effect(100, 60f, e -> {
        // Draw.color 只能传 1 个颜色 + 1 个 alpha；这里手动做"色 A 线性插值到色 B"
        Color lerped = Tmp.c1.set(e.color).lerp(NuColor.PaleBackColor, e.fin());
        Draw.color(lerped, 1f);
        float r = (7f - e.fin() * 7f) / 2f;
        if (r > 0.1f) Fill.circle(e.x, e.y, r);
        Draw.reset();
    });
    public static Effect SailSmoke = new Effect(100, 60f, e -> {
        // Draw.color 只能传 1 个颜色 + 1 个 alpha；这里手动做"色 A 线性插值到色 B"
        Color lerped = Tmp.c1.set(e.color).lerp(NuColor.SailBackColor, e.fin());
        Draw.color(lerped, 1f);
        float r = (7f - e.fin() * 7f) / 2f;
        if (r > 0.1f) Fill.circle(e.x, e.y, r);
        Draw.reset();
    });
    // ========================================================
    // 爆炸 2：小爆炸（最大半径 10 像素，适合小单位死亡特效
    // ========================================================
    public static Effect explosion2 = new Effect(30f, 40f, e -> {
        float f = e.fin();
        // 最大半径 10（要求"大小不超过10"）
        float maxR = 10f;
        float ringR = maxR * Interp.pow2Out.apply(f);
        float ringA = 1f - f;
        if (ringA > 0.02f) {
            Lines.stroke(1.6f - 1.1f * f, new Color(e.color).a(ringA));
            Lines.circle(e.x, e.y, ringR);
        }
        int particles = 6;
        float fly = 7f * Interp.pow2Out.apply(f);
        float pa = 1f - Mathf.pow(f, 2f);
        if (pa > 0.02f) {
            Draw.color(new Color(e.color).a(pa));
            for (int i = 0; i < particles; i++) {
                float ang = i * (360f / particles);
                float px = e.x + trnsx(ang, fly);
                float py = e.y + trnsy(ang, fly);
                float sz = 1.3f * (1.4f - 1.1f * f);
                Fill.square(px, py, sz, ang);
            }
            Draw.color();
        }
        Draw.reset();
    });

    // —— 以后再加别的特效：在后面继续写 public static Effect xxx = new Effect(...); ——

    // ========================================================
    // 子弹尾部特效（3 个预设，直接赋值给 BulletType.trailEffect 即可）
    // ========================================================

    /** ① 高能能量弹尾（蓝→紫→青，发光点 + 粒子 + 烟雾，适合激光/等离子/重型炮） */
    public static Effect energyTail = new BulletTailEffect(){{
        colorFrom    = Color.valueOf("A470FF");
        colorTo      = Color.valueOf("7AE7FF");
        particles    = 5;
        particleShape = 0;     // 圆形粒子
        particleSpread = 8f;
        coreFrom     = 5f; coreTo = 0.5f;
        coreGlowMul  = 1.4f;
        smokeLayers  = 1;
        smokeFrom    = 2f; smokeTo = 12f;
        alphaMul     = 1f;
    }};
    public static Effect sailEnergyTail = new BulletTailEffect(){{
        colorFrom    = NuColor.SailColor;
        colorTo      = NuColor.SailBackColor;
        particles    = 12;
        particleShape = 3;     // 圆形粒子
        particleSpread = 8f;
        coreFrom     = 5f; coreTo = 0.5f;
        coreGlowMul  = 1.4f;
        smokeLayers  = 1;
        smokeFrom    = 2f; smokeTo = 12f;
        alphaMul     = 1f;
    }};
    public static Effect bloodEnergyTail = new BulletTailEffect(){{
        colorFrom    = NuColor.BloodColor;
        colorTo      = NuColor.BloodBackColor;
        particles    = 12;
        particleShape = 3;     // 圆形粒子
        particleSpread = 8f;
        coreFrom     = 5f; coreTo = 0.5f;
        coreGlowMul  = 1.4f;
        smokeLayers  = 1;
        smokeFrom    = 2f; smokeTo = 12f;
        alphaMul     = 1f;
    }};
    public static Effect survivalEnergyTail = new BulletTailEffect(){{
        colorFrom    = NuColor.SurvivalColor;
        colorTo      = NuColor.SurvivalBackColor;
        particles    = 12;
        particleShape = 3;     // 圆形粒子
        particleSpread = 8f;
        coreFrom     = 5f; coreTo = 0.5f;
        coreGlowMul  = 1.4f;
        smokeLayers  = 1;
        smokeFrom    = 2f; smokeTo = 12f;
        alphaMul     = 1f;
    }};
    public static Effect despEnergyTail = new BulletTailEffect(){{
        colorFrom    = NuColor.DespColor;
        colorTo      = NuColor.DespBackColor;
        particles    = 5;
        particleShape = 0;     // 圆形粒子
        particleSpread = 8f;
        coreFrom     = 5f; coreTo = 0.5f;
        coreGlowMul  = 1.4f;
        smokeLayers  = 1;
        smokeFrom    = 2f; smokeTo = 12f;
        alphaMul     = 1f;
    }};

    /** ② 导弹烟尾（白→灰白浓黑烟 + 尾浪环 + 少量火星，适合 Missile / Bomb / 榴弹） */
    public static Effect missileSmokeTail = new BulletTailEffect(){{
        colorFrom    = Color.valueOf("E8E4D8");
        colorTo      = Color.valueOf("56534B");
        additive     = false;   // 烟不用叠加发光（叠加会亮得像发光粉）
        particles    = 2;
        particleSpread = 3f;
        drawCore     = false;   // 尾焰核心关掉，只要烟
        smokeLayers  = 2;
        smokeFrom    = 2.5f; smokeTo = 9f;
        smokeAlphaMul = 0.45f;
        ringCount    = 1;
        ringStroke   = 1.4f;
        alphaMul     = 1.1f;
    }};

    /** ③ 狙击强发光尾（金→橙红，只留超亮尾焰点 + 少量火星，适合 Flak / 狙击 / 长射程） */
    public static Effect sniperGlowTail = new BulletTailEffect(){{
        colorFrom    = Color.valueOf("FFD54F");
        colorTo      = Color.valueOf("FF7043");
        particles    = 3;
        particleSpread = 2f;
        coreFrom     = 7f; coreTo = 0.4f;
        coreGlowMul  = 1.6f;
        smokeLayers  = 0;       // 狙击只要一瞬间亮尾，不要烟
        alphaMul     = 1f;
    }};

    // ========================================================
    // 子弹爆炸特效（6 个预设，赋值给 BulletType.hitEffect / despawnEffect 即可）
    // ========================================================

    /** ① 通用小型爆炸（橙红→金，小冲击波 + 8 粒子，适合普通子弹命中） */
    public static expEffect bulletHitSmall = new expEffect(){{
        lifetime    = 18f;
        sizeTo      = 30f;
        strokeFrom  = 2.2f;
        strokeTo    = 0.3f;
        colorFrom   = Color.valueOf("FF6A00");
        colorTo     = Color.valueOf("FFD54F");
        particles   = 8;
        particleSizeFrom = 1.4f;
        particleSizeTo   = 0.2f;
        flyDistanceTo    = 22f;
        lightColor  = Color.valueOf("FF8C00");
        lightScl    = 2f;
        lightOpacity = 0.6f;
    }};

    public static expEffect sailHitSmall = new expEffect(){{
        lifetime    = 15f;
        sizeTo      = 24f;
        strokeFrom  = 2.2f;
        strokeTo    = 0.3f;
        colorFrom   = NuColor.SailColor;
        colorTo     = NuColor.SailBackColor;
        particles   = 4;
        particleSizeFrom = 1.4f;
        particleSizeTo   = 0.2f;
        flyDistanceTo    = 22f;
        lightColor  = NuColor.SailConColor;
        lightScl    = 2f;
        lightOpacity = 0.6f;
    }};
    /** ② 高能等离子爆炸（紫→青蓝，大圈 + 16 粒子，适合能量炮/激光命中） */
    public static expEffect plasmaHit = new expEffect(){{
        lifetime    = 28f;
        sizeTo      = 55f;
        strokeFrom  = 3f;
        strokeTo    = 0.5f;
        colorFrom   = Color.valueOf("A470FF");
        colorTo     = Color.valueOf("7AE7FF");
        particles   = 16;
        particleSizeFrom = 1.8f;
        particleSizeTo   = 0.3f;
        flyDistanceTo    = 40f;
        lightColor  = Color.valueOf("B388FF");
        lightScl    = 3f;
        lightOpacity = 0.8f;
    }};
    public static expEffect sailPlasmaHit = new expEffect(){{
        lifetime    = 20f;
        sizeTo      = 80f;
        strokeFrom  = 3f;
        strokeTo    = 0.5f;
        colorFrom   = NuColor.SailColor;
        colorTo     = NuColor.SailBackColor;
        particles   = 24;
        particleSizeFrom = 4f;
        particleSizeTo   = 1.3f;
        flyDistanceTo    = 40f;
        lightColor  = Color.valueOf("B388FF");
        lightScl    = 3f;
        lightOpacity = 0.8f;
    }};

    /** ③ 燃烧爆破（深红→黑烟，多粒子 + 暗色光照，适合燃烧弹/凝固汽油弹命中） */
    public static expEffect burnHit = new expEffect(){{
        lifetime    = 40f;
        sizeTo      = 38f;
        strokeFrom  = 2.5f;
        strokeTo    = 0.2f;
        colorFrom   = Color.valueOf("D84315");
        colorTo     = Color.valueOf("424242");
        particles   = 20;
        particleSizeFrom = 1.6f;
        particleSizeTo   = 0.15f;
        flyDistanceTo    = 30f;
        lightColor  = Color.valueOf("FF5722");
        lightScl    = 2.5f;
        lightOpacity = 0.5f;
    }};

    /** ④ 电弧爆炸（亮蓝→白，冲击波快速扩散 + 稀疏粒子，适合电击/EMP弹命中） */
    public static expEffect arcHit = new expEffect(){{
        lifetime    = 16f;
        sizeTo      = 42f;
        strokeFrom  = 2.8f;
        strokeTo    = 0.6f;
        colorFrom   = Color.valueOf("29B6F6");
        colorTo     = Color.valueOf("E1F5FE");
        particles   = 6;
        particleSizeFrom = 1.2f;
        particleSizeTo   = 0.1f;
        flyDistanceTo    = 25f;
        lightColor  = Color.valueOf("40C4FF");
        lightScl    = 3.5f;
        lightOpacity = 0.9f;
    }};
    public static expEffect sailArcHit = new expEffect(){{
        lifetime    = 16f;
        sizeTo      = 21f;
        strokeFrom  = 2.8f;
        strokeTo    = 0.6f;
        colorFrom   = NuColor.SailColor;
        colorTo     = NuColor.SailBackColor;
        particles   = 9;
        particleSizeFrom = 2.1f;
        particleSizeTo   = 0.3f;
        flyDistanceTo    = 25f;
        lightColor  = NuColor.SailColor;
        lightScl    = 3.5f;
        lightOpacity = 0.9f;
    }};

    /** ⑤ 重型爆炸（橙→深红，超大圈 + 24 粒子 + 强光照，适合大口径/榴弹/炸弹消亡） */
    public static expEffect heavyBoom = new expEffect(){{
        lifetime    = 45f;
        sizeTo      = 120f;
        strokeFrom  = 5f;
        strokeTo    = 0.8f;
        colorFrom   = Color.valueOf("FF6A00");
        colorTo     = Color.valueOf("B71C1C");
        particles   = 24;
        particleSizeFrom = 2.5f;
        particleSizeTo   = 0.3f;
        flyDistanceTo    = 80f;
        lightColor  = Color.valueOf("FF8C00");
        lightScl    = 4f;
        lightOpacity = 1f;
    }};

    /** ⑥ 毒蚀腐蚀爆炸（酸绿→暗绿，小圈 + 密集粒子，适合腐蚀/酸液弹命中） */
    public static expEffect corrodeHit = new expEffect(){{
        lifetime    = 35f;
        sizeTo      = 28f;
        strokeFrom  = 2f;
        strokeTo    = 0.2f;
        colorFrom   = Color.valueOf("76FF03");
        colorTo     = Color.valueOf("33691E");
        particles   = 18;
        particleSizeFrom = 1.3f;
        particleSizeTo   = 0.2f;
        flyDistanceTo    = 20f;
        lightColor  = Color.valueOf("8BC34A");
        lightScl    = 2f;
        lightOpacity = 0.4f;
    }};

    // ========================================================
    // 四芒星命中特效（刀刃命中时从中心发散出四芒星 + 光晕）
    // ========================================================

    /** 四芒星命中特效（从中心向 4 方向发散，自带光晕，适合刀刃命中） */
    public static Effect bladeHitStar = new Effect(25f, 60f, e -> {
        float f = e.fin();
        float alpha = 1f - f;

        // —————— 1) 中心光晕（快速扩散淡出） ——————
        float glowR = 4f + 18f * Interp.pow2Out.apply(f);
        Draw.color(new Color(e.color).a(alpha * 0.4f));
        Fill.circle(e.x, e.y, glowR);

        // —————— 2) 四芒星主体（4 条从中心向外的尖刺） ——————
        // 每条尖刺是一个细长三角形：底在中心、尖向外
        float spikeLen = 10f + 28f * Interp.pow2Out.apply(f);
        float spikeWidth = 3f * (1f - f * 0.5f);

        Draw.color(new Color(e.color).a(alpha));
        // 4 个方向：0°(右)、90°(上)、180°(左)、270°(下)
        // 再叠加 e.rotation 让特效随刀刃方向旋转
        for (int i = 0; i < 4; i++) {
            float baseAng = e.rotation + i * 90f;
            float tipX = e.x + Angles.trnsx(baseAng, spikeLen);
            float tipY = e.y + Angles.trnsy(baseAng, spikeLen);
            // 两侧底点（垂直于尖刺方向偏移 spikeWidth/2）
            float leftX  = e.x + Angles.trnsx(baseAng + 90f, spikeWidth);
            float leftY  = e.y + Angles.trnsy(baseAng + 90f, spikeWidth);
            float rightX = e.x + Angles.trnsx(baseAng - 90f, spikeWidth);
            float rightY = e.y + Angles.trnsy(baseAng - 90f, spikeWidth);

            Fill.tri(
                e.x, e.y,              // 底中心
                tipX, tipY,            // 尖端
                leftX, leftY           // 左侧
            );
            Fill.tri(
                e.x, e.y,              // 底中心
                tipX, tipY,            // 尖端
                rightX, rightY         // 右侧
            );
        }

        // —————— 3) 中心实心亮点 ——————
        Draw.color(new Color(e.color).a(alpha * 0.8f));
        Fill.circle(e.x, e.y, 3f * (1f - f * 0.3f));

        Draw.reset();
    });

    /* ============================================================
     *  ⚡ 闪电系列 7+ 特效（基于 LightningStormEffect 模块化类）
     * ============================================================
     *   模块化开关：useCoreBall / useStorm / useLightning
     *   可以自由组合。以下是典型预设：
     *   ① 完整风暴（光球+风暴+闪电）
     *   ② 单独风暴（只台风，不闪电，单独提取）
     *   ③ 单独光球+闪电（不生成风暴，炮台普通雷击）
     *   ④ 单独闪光球（蓄力视觉）
     *   ⑤ 短程速射雷击（炮台连发）
     *   ⑥ 粗重型雷击（Boss 大招）
     *   ⑦ 紫金雷系（奥术/电磁风格）
     * ============================================================ */

    // ① 电磁爆发：Phase1 粒子+光圈 → Phase2 扫弧填圆环 → Phase3 保持
    //    ⚠ 此 Effect 仅提供纯渲染视觉（光球、光圈、粒子、扫弧圆环）。
    //      不发射任何 LightningBullet。如果需要原版闪电子弹，请用 StormCrafterBlock，
    //      其 Building.updateTile() 会调用 lightningBullet.create(...) 发射真正的 LightningBulletType。
    //    lifetime 300f（Phase1 占 0.25 = 75 tick ≈ 1.25s；Phase2 占 0.3 = 90 tick ≈ 1.5s；
    //             剩余 Phase3 ≈ 135 tick 保持形态，无收回）
    public static Effect lightningStormFull = new LightningStormEffect(300f, 380f, e -> {}){{
        stormColor       = new Color(0x6F9BFFff);
        glowColor        = new Color(0x3F5FFFaa);
        lightningColor   = new Color(0xE3F2FDff);
        sizeMul          = 1f;
        phase1End        = 0.25f;   // Phase1 = 0.25 * 300 = 75 tick
        phase2End        = 0.55f;   // Phase2 = 0.30 * 300 = 90 tick
        coreSize         = 40f;
        innerRingRadius  = 56f;
        innerRingWidth   = 2f;
        outerRadius      = 40f * 8f;  // 40 格 = 320 px
        outerWidth       = 3f;
        outerSweepWidth  = 5f;
        particleBurstCount    = 5;
        particleBurstInterval = 0.08f;
        particleSpeed    = 3.2f;
        particleSizeFrom = 1.5f;
        particleSizeTo   = 7f;
        particleMaxDist  = 240f;
        lightRadius      = 260f;
    }};

    // ② 单独爆发圆环（无粒子，只扫弧填圆环 → 保持）
    public static Effect typhoonCloudOnly = new LightningStormEffect(140f, 340f, e -> {}){{
        useCoreBall  = false;
        useParticles = false;
        useInnerRing = false;
        useOuterArc  = true;
        phase1End    = 0.001f;  // Phase1 立刻跳过
        phase2End    = 0.55f;   // 扫弧在 0~55% 进度内完成 360°
        stormColor   = new Color(0x8FA9D5cc);
        outerRadius  = 180f;
        outerWidth   = 4f;
        outerSweepWidth = 6f;
        lightRadius  = 180f;
        lightColor   = new Color(0x8FA9D588);
    }};

    // ③ 光球+扫弧（蓄能爆发，攻击命中视觉）
    public static Effect lightningStrike = new LightningStormEffect(48f, 260f, e -> {}){{
        useCoreBall  = true;
        useParticles = false;
        useInnerRing = true;
        useOuterArc  = true;
        phase1End    = 0.22f;   // 光球淡入
        phase2End    = 0.75f;   // 扫弧到 75% 处已够明显
        stormColor   = new Color(0xFFD54Fff);
        glowColor    = new Color(0xFF8F00aa);
        lightningColor = Color.white;
        coreSize     = 20f;
        coreGlowMul  = 4f;
        outerRadius  = 140f;
        outerWidth   = 3f;
        lightRadius  = 220f;
        lightColor   = new Color(0xFFE082ff);
    }};

    // ④ 单独蓄能光球（不扫弧不粒子，单纯一个脉动球）
    public static Effect lightningChargeSphere = new LightningStormEffect(36f, 200f, e -> {}){{
        useCoreBall  = true;
        useParticles = false;
        useInnerRing = true;
        useOuterArc  = false;
        phase1End    = 0.20f;
        phase2End    = 1.1f;    // 永不进入 Phase2（保持 Phase1 形态直到结束）
        stormColor   = new Color(0x4FC3F7ff);
        glowColor    = new Color(0x0288D1aa);
        coreGlowMul  = 5.2f;
        coreSize     = 24f;
        innerRingRadius = 34f;
        innerRingWidth  = 2f;
        lightRadius  = 180f;
        lightColor   = new Color(0x4FC3F7cc);
    }};

    // ⑤ 速射命中（短 lifetime，小球 + 细扫弧）
    public static Effect lightningRapidHit = new LightningStormEffect(22f, 180f, e -> {}){{
        useCoreBall  = true;
        useParticles = false;
        useInnerRing = false;
        useOuterArc  = true;
        phase1End    = 0.15f;
        phase2End    = 0.75f;
        stormColor   = new Color(0xB3E5FCff);
        lightningColor = new Color(0xE1F5FEff);
        coreSize     = 12f;
        coreGlowMul  = 3.2f;
        outerRadius  = 100f;
        outerWidth   = 2.2f;
        outerSweepWidth = 3.5f;
        lightRadius  = 120f;
    }};

    // ⑥ 重型雷击（Boss 大招——大放扫弧 + 大光球 + 粒子爆发）
    public static Effect lightningHeavyThunder = new LightningStormEffect(120f, 420f, e -> {}){{
        useCoreBall  = true;
        useParticles = true;
        useInnerRing = true;
        useOuterArc  = true;
        phase1End    = 0.30f;
        phase2End    = 0.70f;
        stormColor   = new Color(0xCE93D8ff);
        glowColor    = new Color(0x4A148Ccc);
        lightningColor = new Color(0xF3E5F5ff);
        coreSize     = 40f;
        coreGlowMul  = 5.5f;
        innerRingRadius = 56f;
        outerRadius  = 300f;
        outerWidth   = 4f;
        outerSweepWidth = 7f;
        particleBurstCount = 8;
        particleBurstInterval = 0.07f;
        particleMaxDist = 260f;
        particleSpeed = 4f;
        lightRadius  = 380f;
        lightColor   = new Color(0xE1BEE7dd);
        sizeMul      = 1.15f;
    }};

    // ⑦ 奥术雷（紫色/粉色，魔法少女系）
    public static Effect lightningArcane = new LightningStormEffect(60f, 260f, e -> {}){{
        useCoreBall  = true;
        useParticles = true;
        useInnerRing = true;
        useOuterArc  = true;
        stormColor   = new Color(0xEA80FCff);
        glowColor    = new Color(0x7B1FA2cc);
        lightningColor = new Color(0xF8BBD0ff);
        phase1End    = 0.20f;
        phase2End    = 0.75f;
        coreSize     = 22f;
        coreGlowMul  = 4.5f;
        outerRadius  = 150f;
        outerWidth   = 2.4f;
        outerSweepWidth = 4.5f;
        particleBurstCount = 5;
        particleBurstInterval = 0.06f;
        particleMaxDist = 130f;
        lightRadius  = 240f;
        lightColor   = new Color(0xEA80FCcc);
    }};

    /* ============================================================
     *  🌪 灰云扫弧（纯爆发云感，无闪电气味，灰+白浓淡扫弧）
     * ============================================================ */
    public static Effect pureTyphoonStorm = new LightningStormEffect(200f, 360f, e -> {}){{
        useCoreBall  = false;
        useParticles = false;
        useInnerRing = false;
        useOuterArc  = true;
        phase1End    = 0.001f;
        phase2End    = 0.60f;
        stormColor   = new Color(0xB0BEC5cc);
        glowColor    = new Color(0x37474Fbb);
        outerRadius  = 210f;
        outerWidth   = 5f;
        outerSweepWidth = 7f;
        sizeMul      = 1.2f;
        lightRadius  = 120f;
        lightColor   = new Color(0xECEFF166);
    }};

    /* ============================================================
     *  🅣 文字跳出弹出特效（基于 TextPopupEffect 模块化类）
     *      直接 at(单位x, 单位y) 即可，自带弹跳+描边+上升+淡出
     * ============================================================ */

    // 默认暴击红字（大弹跳 + 抖动 + 上升）
    public static Effect textCritRed = new TextPopupEffect(60f, 120f, e -> {}){{
        text             = "CRIT!";                  // 可在 at 后单独改
        textColor        = new Color(0xFF5252ff);
        textSize         = 1.6f;
        outlineColor     = Color.black;
        outlineOffset    = 2f;
        riseDistance     = 55f;
        popupPeak        = 1.5f;
        popupTo          = 1.0f;
        useShake         = true;
        shakeAmplitude   = 2f;
        shakeFrequency   = 12f;
        useShadow        = true;
        fadeStart        = 0.6f;
    }};

    // 单位复活橙粉字（抖动+旋转）
    public static Effect textResurrect = new TextPopupEffect(70f, 140f, e -> {}){{
        text             = "复活！";
        textColor        = new Color(0xFF7A59ff);
        textSize         = 1.7f;
        outlineColor     = Color.black;
        outlineOffset    = 2.2f;
        riseDistance     = 60f;
        popupPeak        = 1.4f;
        popupTo          = 1.05f;
        useShake         = true;
        shakeAmplitude   = 1.5f;
        useRotate        = true;
        rotateFrom       = -8f;
        rotateTo         = 2f;
        useShadow        = true;
        fadeStart        = 0.55f;
    }};

    // 治疗绿字（柔和上升，不带抖动）
    public static Effect textHealGreen = new TextPopupEffect(55f, 120f, e -> {}){{
        text             = "+HP";
        textColor        = new Color(0x81C784ff);
        textSize         = 1.3f;
        outlineColor     = Color.black;
        outlineOffset    = 1.6f;
        riseDistance     = 40f;
        popupFrom        = 0.6f;
        popupPeak        = 1.2f;
        popupTo          = 1f;
        useShake         = false;
        useShadow        = true;
        fadeStart        = 0.55f;
        useGravityDrop   = true;
        dropStart        = 0.5f;
        dropDistance     = 6f;
    }};

    // 技能名金色大字（慢速弹入 + 大超射 + 旋转）
    public static Effect textSkillGold = new TextPopupEffect(90f, 160f, e -> {}){{
        text             = "技能发动！";
        textColor        = new Color(0xFFD54Fff);
        textSize         = 2.0f;
        outlineColor     = new Color(0x3E2723cc);
        outlineOffset    = 2.6f;
        riseDistance     = 80f;
        popupEnd         = 0.35f;
        popupFrom        = 0f;
        popupPeak        = 1.6f;
        popupTo          = 1.1f;
        useShake         = true;
        shakeAmplitude   = 1.2f;
        useRotate        = true;
        rotateFrom       = -15f;
        rotateTo         = 0f;
        useShadow        = true;
        fadeStart        = 0.55f;
        shrinkEnd        = 0.1f;
    }};

    // 警告红方标字（超快弹入 + 强抖动）
    public static Effect textWarningRed = new TextPopupEffect(50f, 140f, e -> {}){{
        text             = "警告！";
        textColor        = new Color(0xFF1744ff);
        textSize         = 1.8f;
        outlineColor     = Color.black;
        outlineOffset    = 2.4f;
        riseDistance     = 30f;
        popupEnd         = 0.15f;
        popupFrom        = 0.5f;
        popupPeak        = 1.35f;
        popupTo          = 1f;
        useShake         = true;
        shakeAmplitude   = 3f;
        shakeFrequency   = 18f;
        useShadow        = true;
        fadeStart        = 0.55f;
    }};
}
