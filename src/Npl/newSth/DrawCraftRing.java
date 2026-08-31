package Npl.newSth;

import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.draw.*;

import static mindustry.Vars.*;

/**
 * 【DrawCraftRing · 工厂环形进度条（画在方块外沿 · 科技感 v3：收缩 + 完成爆发）】
 * =======================================================
 * v3 新增两个动效（用户要求）：
 *   ★ shrink —— 随着进度推进，整圈（轨道/HUD 外框/刻度/扫描/进度弧）一起从 rBase 收缩到 rBase*shrinkTo
 *   ★ burst  —— 进度转满一圈（progress %= 1f 那一刻）检测到"回落"，整圈迅速向外扩张并淡出，
 *               外圈还会放一道更快的白色冲击环 + 中心闪光 → "充能完毕、能量释放"的手感
 *
 * 完成检测原理：GenericCrafterBuild.craft() 里 progress %= 1f，
 *   所以绘制时看到 p 比上一帧明显回落（p + 0.05 < lastP）就说明刚转完一圈。
 *   状态按建筑 id 存在本 drawer 内（IntFloatMap），超过 4096 个建筑会整体清一次防止长局内存增长。
 *   → 如果你的 progressSource 不是 0→1 循环量，把 burst 关掉即可。
 *
 * 【层次】① 底圈 ② HUD 虚线外框 ③ 待机辉光 ④ 细/主刻度 ⑤ 旋转扫描弧 ⑥ 发光层（加法）⑦ 主进度弧 ⑧ 世界光照 ⑨ 爆发冲击环
 * 【断电隐藏】hideIfNoPower = true：build.power.status ≈ 0 时整个环不画
 *
 * 【用法】放进 DrawMulti（前面要有负责画本体的 drawer）：
 *   drawer = new DrawMulti(
 *       new DrawDefault(),
 *       new DrawCraftRing(){{ gap = 2.5f; }}   // 收缩 + 爆发默认开
 *   );
 *
 * 【调手感】
 *   shrinkTo = 0.55f      收缩到原半径的多少（1f = 不缩，0.3f = 缩得很狠）
 *   shrinkCurve           收缩曲线（线性 / pow2In = 前段几乎不缩、最后一口气缩进去）
 *   burstDuration = 12f   爆发时长（tick，60 = 1 秒；12 ≈ 0.2 秒"迅速"）
 *   burstScale = 1.9f     爆发时扩到原半径的多少倍
 *   burstCurve            pow2Out = 冲出去快、收尾慢
 *   burstFlash = false    关掉白色冲击环/中心闪光，只留本体扩张
 * =======================================================
 */
public class DrawCraftRing extends DrawBlock {

    /* ======================================================
     *                  位置 / 缩放
     * ====================================================== */

    /** 环在方块外沿之外再往外多少像素（默认 2.5，越小越"贴着方块"） */
    public float gap = 2.5f;

    /** 手动指定半径（像素，>0 时优先生效）；<=0 = 自动 size * tilesize / 2f + gap */
    public float radiusOverride = -1f;

    /** 主弧线宽；<=0 = 按半径自动（r * strokeScale，限幅 1.6 ~ 8） */
    public float strokeWidth = -1f;
    /** 自动线宽系数 */
    public float strokeScale = 0.26f;

    /* ======================================================
     *                  ★ 收缩（随时间推进环变小）
     * ====================================================== */

    /** 是否让整圈随进度收缩 */
    public boolean shrink = true;
    /** 收缩到原半径的多少（0.05~1；1 = 不缩） */
    public float shrinkTo = 0.55f;
    /** 收缩曲线 */
    public Interp shrinkCurve = Interp.linear;

    /* ======================================================
     *                  ★ 完成爆发（转满一圈后迅速扩大）
     * ====================================================== */

    public boolean burst = true;
    /** 爆发时长（tick）。12 ≈ 0.2 秒，"迅速" */
    public float burstDuration = 12f;
    /** 爆发时扩到原半径的多少倍 */
    public float burstScale = 1.9f;
    /** 爆发曲线：pow2Out = 先冲出去、后减速 */
    public Interp burstCurve = Interp.pow2Out;
    /** 冲击环/闪光强度 */
    public float burstAlpha = 0.7f;
    /** 是否额外画白色冲击环 + 中心闪光 */
    public boolean burstFlash = true;

    /** 爆发期间世界光照额外增强的倍数（0 = 不增强） */
    public float burstLightBoost = 1.2f;

    /* ======================================================
     *                  断电隐藏
     * ====================================================== */

    /** 断电就不显示整个环：建筑有电力模块且 power.status <= 0.001 时直接跳过。
     *  没有电力模块的方块（build.power == null）不受影响。 */
    public boolean hideIfNoPower = true;

    /* ======================================================
     *                  ① 底圈（空槽）
     * ====================================================== */

    public boolean drawTrack = true;
    /** 底圈线宽；<=0 = 自动（主弧宽 * 1.7） */
    public float trackStroke = -1f;
    /** 底圈颜色：要够黑，加法发光才跳得出来（沙地上尤其明显） */
    public Color trackColor = new Color(0f, 0f, 0f, 0.55f);

    /** 进度为 0 时是否还画底圈/刻度/外框（false = 不工作时整个环都不画） */
    public boolean showIdle = true;

    /* ======================================================
     *                  ② HUD 虚线外框
     * ====================================================== */

    public boolean hudFrame = true;
    /** 外框半径相对主半径的倍率 */
    public float hudFrameScale = 1.22f;
    public float hudFrameStroke = 1f;
    public Color hudFrameColor = new Color(1f, 1f, 1f, 0.18f);

    /* ======================================================
     *                  ③ 待机辉光（空闲时也有"通电"感）
     * ====================================================== */

    public boolean standbyGlow = true;
    /** 待机辉光 = glowAlpha * 这个系数 */
    public float standbyAlpha = 0.30f;

    /* ======================================================
     *                  ④ 刻度圈
     * ====================================================== */

    /** 一圈细刻度数量；0 = 不画 */
    public int ticks = 12;
    /** 主刻度数量（默认 4 = 每 90° 一根长的）；0 = 不画 */
    public int majorTicks = 4;
    /** 细刻度线宽；<=0 = 自动（主弧宽 * 0.35） */
    public float tickStroke = -1f;
    /** 主刻度线宽；<=0 = 自动（主弧宽 * 0.55） */
    public float majorTickStroke = -1f;
    /** 细刻度沿半径的起止倍率 */
    public float tickInnerScale = 0.86f, tickOuterScale = 1.14f;
    /** 主刻度沿半径的起止倍率（更长） */
    public float majorInnerScale = 0.70f, majorOuterScale = 1.30f;
    /** 还没被进度走到的刻度颜色 */
    public Color tickColorOff = new Color(1f, 1f, 1f, 0.20f);
    /** 已经被进度点亮的刻度颜色 */
    public Color tickColorOn = new Color(1f, 1f, 1f, 0.70f);
    public Color majorTickColor = new Color(1f, 1f, 1f, 0.45f);

    /* ======================================================
     *                  ⑤ 旋转扫描弧
     * ====================================================== */

    public boolean scanSweep = true;
    /** 旋转速度（度/秒），会乘效率 */
    public float scanSpeed = 40f;
    /** 扫描弧长（度） */
    public float scanArc = 70f;
    public float scanWidth = 2f;
    public float scanAlpha = 0.16f;

    /* ======================================================
     *                  ⑥⑦ 进度弧
     * ====================================================== */

    /** 进度弧颜色（按进度从 colorFrom 插值到 colorTo） */
    public Color colorFrom = Pal.accent.cpy(), colorTo = Pal.accent.cpy();

    /** 起始角（度）。默认 -90 = 正上方；顺时针时它是这一圈的"终点" */
    public float startAngle = -90f;
    /** true = 顺时针增长 */
    public boolean clockwise = true;

    /** >1 = 把进度弧画成分段"电量格"（小半径下更清晰，推荐 12）；0/1 = 整段 */
    public int segments = 12;
    /** 分段时间隙占每段的百分比 0~0.9 */
    public float segmentGap = 0.42f;

    /** 进度弧底下叠一层更粗的半透明同色弧 → 能量发光 */
    public boolean glow = true;
    public float glowScale = 3.4f;
    public float glowAlpha = 0.32f;

    /** 发光/待机/扫描/爆发用加法混合；结束自动还原原来的混合模式 */
    public boolean additive = true;

    /** 呼吸脉动 */
    public boolean pulse = true;
    public float pulseScl = 3f, pulseMag = 0.18f;

    /** 世界光照：夜里真的照亮周围（0 = 不发光照） */
    public float lightScale = 2.6f;
    public float lightOpacity = 0.35f;
    /** 光照颜色，null = 用当前进度弧颜色 */
    public @Nullable Color lightColor;

    /* ======================================================
     *                  头亮点（默认关）
     * ====================================================== */

    public boolean drawHead = false;
    public float headSize = 3f;
    public Color headColor = Color.white.cpy();

    /* ======================================================
     *                  层级 / 进度来源
     * ====================================================== */

    /** 临时抬高绘制层级（让环压在相邻方块之上）；0 = 保持当前层级。draw() 结束会自动还原 */
    public float zOffset = 0f;

    /**
     * 进度来源；null（默认）= 用 build.progress()，也就是 GenericCrafter 的合成进度 0~1。
     * 想换成别的量就自己给（每帧都会重新取一次）：
     *   progressSource = b -> b.efficiency;                                      // 效率 0~1
     *   progressSource = b -> b.warmup();                                        // 运转热度 0~1
     *   progressSource = b -> 1f - b.progress();                                 // 倒计时（反向）
     *   progressSource = b -> ((ConfigurableBlock.UnitFactoryBuild)b).fraction();  // 自定义工厂
     * 注意：burst 的"转满一圈"检测依赖 0→1 的循环回落，非循环的量请把 burst 关掉。
     */
    public @Nullable Floatf<Building> progressSource;

    /* ======================================================
     *                  每建筑状态（检测"转满一圈"）
     * ====================================================== */

    /** 建筑 id → 上一帧进度 */
    private final IntFloatMap lastProgress = new IntFloatMap();
    /** 建筑 id → 本次爆发的起始时间（Time.time） */
    private final IntFloatMap burstTime = new IntFloatMap();

    /* ======================================================
     *                  绘制
     * ====================================================== */

    @Override
    public void draw(Building build){
        // ---------- 断电：整个环都不画 ----------
        if(hideIfNoPower && build.power != null && build.power.status <= 0.001f) return;

        // ---------- 进度 ----------
        float p = Mathf.clamp(progressSource != null ? progressSource.get(build) : build.progress());
        if(Float.isNaN(p)) p = 0f;
        if(p <= 0.0001f && !showIdle) return;

        int id = build.id;

        // ---------- 完成检测：进度明显回落 = 刚转满一圈（craft() 里 progress %= 1f）----------
        float lastP = lastProgress.get(id, -1f);
        if(burst && lastP >= 0f && p + 0.05f < lastP){
            burstTime.put(id, Time.time);
        }
        lastProgress.put(id, p);
        if(lastProgress.size > 4096){   // 长局防内存增长（很少触发）
            lastProgress.clear();
            burstTime.clear();
        }

        float bt = Time.time - burstTime.get(id, -1e9f);
        boolean bursting = burst && bt >= 0f && bt < burstDuration;
        float bf = bursting ? Mathf.clamp(bt / Math.max(1f, burstDuration)) : 1f;
        float burstFade = bursting ? (1f - bf) : 0f;

        // ---------- 半径：收缩 + 爆发扩张 ----------
        float rBase = radiusOverride > 0f ? radiusOverride : build.block.size * tilesize / 2f + gap;
        float rMin = rBase * Mathf.clamp(shrinkTo, 0.05f, 1f);
        float r = shrink ? Mathf.lerp(rBase, rMin, Mathf.clamp(shrinkCurve.apply(p))) : rBase;
        if(bursting){
            // 从收缩后的最小半径迅速冲到 rBase * burstScale
            r = Mathf.lerp(rMin, rBase * burstScale, Mathf.clamp(burstCurve.apply(bf)));
        }

        float x = build.x, y = build.y;
        float covered = 360f * p;
        // 爆发瞬间进度刚归零，为了"转完这一圈"的观感，爆发期间进度弧按满圈画并随爆发淡出
        float arcP = bursting ? 1f : p;
        float arcMul = bursting ? burstFade : 1f;

        // 线宽自动缩放
        float sw = strokeWidth > 0f ? strokeWidth : Mathf.clamp(r * strokeScale, 1.6f, 8f);
        float trackW = trackStroke > 0f ? trackStroke : sw * 1.7f;
        float tickW = tickStroke > 0f ? tickStroke : Math.max(1f, sw * 0.35f);
        float majorW = majorTickStroke > 0f ? majorTickStroke : Math.max(1.2f, sw * 0.55f);

        float step = ticks > 0 ? 360f / ticks : 0f;
        float pulseA = pulse ? (1f - pulseMag + Mathf.absin(pulseScl, pulseMag)) : 1f;
        // 扫描速度/亮度跟随效率：缺料/缺电时"没劲"
        float eff = Mathf.clamp(build.efficiency);

        float oldZ = Draw.z();
        if(zOffset != 0f) Draw.z(oldZ + zOffset);

        // ---------- ① 底圈 ----------
        if(drawTrack && trackW > 0f){
            Draw.color(trackColor.r, trackColor.g, trackColor.b, trackColor.a * Math.max(arcMul, 0.35f));
            Lines.stroke(trackW);
            Lines.circle(x, y, r);
        }

        // ---------- ② HUD 虚线外框 ----------
        if(hudFrame && hudFrameStroke > 0f){
            Lines.stroke(hudFrameStroke, hudFrameColor);
            Lines.dashCircle(x, y, r * hudFrameScale);
        }

        // ---------- ④ 刻度 ----------
        if(ticks > 0){
            for(int i = 0; i < ticks; i++){
                float ang = startAngle + (clockwise ? -i * step : i * step);
                boolean on = covered > 0.0001f && i * step <= covered;
                Lines.stroke(tickW, Tmp.c1.set(on ? tickColorOn : tickColorOff));
                line(x, y, ang, r * tickInnerScale, r * tickOuterScale);
            }
        }
        if(majorTicks > 0){
            float mstep = 360f / majorTicks;
            for(int i = 0; i < majorTicks; i++){
                float ang = startAngle + (clockwise ? -i * mstep : i * mstep);
                Lines.stroke(majorW, majorTickColor);
                line(x, y, ang, r * majorInnerScale, r * majorOuterScale);
            }
        }

        // ---------- ③⑤⑥⑨ 加法混合区：待机辉光 + 扫描 + 发光层 ----------
        Blending prevBlend = Draw.getBlend();
        if(additive) Draw.blend(Blending.additive);

        if(standbyGlow && sw > 0f){
            Lines.stroke(sw * 1.9f, Tmp.c1.set(colorTo).a(glowAlpha * standbyAlpha * pulseA));
            Lines.arc(x, y, r, 1f, startAngle);
        }

        if(scanSweep && scanArc > 0f){
            float sa = (Time.time * scanSpeed * Math.max(0.15f, eff)) % 360f;
            Lines.stroke(scanWidth, Tmp.c1.set(colorTo).a(scanAlpha * pulseA * Math.max(0.35f, eff)));
            Lines.arc(x, y, r, Mathf.clamp(scanArc / 360f), sa);
        }

        Color arc = null;
        float start = 0f;
        if(arcP > 0.0001f){
            start = clockwise ? startAngle - 360f * arcP : startAngle;
            arc = Tmp.c2.set(colorFrom).lerp(colorTo, arcP);

            if(glow && sw > 0f){
                Lines.stroke(sw * glowScale, Tmp.c1.set(arc).a(glowAlpha * pulseA * arcMul));
                Lines.arc(x, y, r, arcP, start);
            }
        }

        Draw.blend(prevBlend);

        // ---------- ⑦ 进度主弧（正常混合，颜色准）----------
        if(arcP > 0.0001f){
            Lines.stroke(sw, Tmp.c1.set(arc).a(arcMul));
            progressArc(x, y, r, arcP, start);

            if(drawHead && headSize > 0f && !bursting){
                float ha = startAngle + (clockwise ? -360f * arcP : 360f * arcP);
                Draw.color(headColor.r, headColor.g, headColor.b, headColor.a);
                Fill.circle(x + Angles.trnsx(ha, r), y + Angles.trnsy(ha, r), headSize);
            }
        }

        // ---------- ⑨ 爆发：白色冲击环 + 中心闪光（比本体扩得更快更远）----------
        if(bursting && burstFlash){
            Blending prevBlend2 = Draw.getBlend();
            if(additive) Draw.blend(Blending.additive);

            float wr = Mathf.lerp(rMin, rBase * burstScale * 1.35f, Mathf.clamp(burstCurve.apply(bf)));
            Lines.stroke(Math.max(1f, sw * (1.6f - 1.1f * bf)),
                Tmp.c1.set(Color.white).a(burstAlpha * burstFade));
            Lines.arc(x, y, wr, 1f, startAngle);

            Draw.color(Color.white.r, Color.white.g, Color.white.b, burstAlpha * 0.5f * burstFade);
            Fill.circle(x, y, rBase * 0.35f * burstFade);
            Draw.color();

            Draw.blend(prevBlend2);
        }

        // ---------- ⑧ 世界光照（爆发时额外变亮）----------
        if(lightOpacity > 0f && arcP > 0.0001f){
            float boost = 1f + (bursting ? burstLightBoost * burstFade : 0f);
            Drawf.light(x, y, r * lightScale, lightColor != null ? lightColor : arc,
                Mathf.clamp(lightOpacity * (0.35f + 0.65f * arcP) * pulseA * boost));
        }

        if(zOffset != 0f) Draw.z(oldZ);
        Draw.reset();
    }

    /** 沿 ang 方向画一段径向短线（刻度用） */
    private void line(float x, float y, float ang, float inner, float outer){
        Lines.line(
            x + Angles.trnsx(ang, inner), y + Angles.trnsy(ang, inner),
            x + Angles.trnsx(ang, outer), y + Angles.trnsy(ang, outer));
    }

    /** 进度弧：segments <= 1 画整段，否则画成分段"电量格" */
    private void progressArc(float x, float y, float r, float p, float start){
        if(segments <= 1){
            Lines.arc(x, y, r, p, start);
            return;
        }
        float seg = 1f / segments;
        float gap = seg * Mathf.clamp(segmentGap, 0f, 0.9f);
        for(int i = 0; i < segments; i++){
            float a = i * seg;
            if(a >= p) break;
            float b = Math.min(a + seg - gap, p);
            if(b > a) Lines.arc(x, y, r, b - a, start + 360f * a);
        }
    }
}
