package Npl.newSth;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import java.lang.reflect.*;

import static mindustry.Vars.*;

/**
 * 【反隐雷达 AntiStealthRadar】
 * =======================================================
 * 功能：
 *   1. 原版 Radar 一样有雾半径（fogRadius），探索战争迷雾
 *   2. 额外：以 detectionRange 为半径扫描范围内所有敌方 InvisibleAbility 隐身单位
 *      → 命中的单位会被强制"显形"（InvisibleAbility.radarRevealedTick 被延长到 revealPerStep）
 *   3. 每隔 scanTick 做一次扫描（默认 30 tick = 0.5 秒扫一次，性能友好）
 *   4. 旋转天线动画 + 扫描扇形视觉效果 + 发现目标时的警告光圈
 *   5. 要求电力：无效率 = 0，无法显形单位，只能保留基础开雾功能
 *
 * 放置/使用方法：
 *   在 NuBlocks.java 中注册后，像放置普通 Radar 一样摆放即可：
 *     antiStealthRadar = new AntiStealthRadar("anti-stealth-radar"){{
 *         requirements(Category.effect, with(
 *             Items.silicon, 120,
 *             Items.plastanium, 40,
 *             Items.surgeAlloy, 25
 *         ));
 *         size = 2;
 *         health = 2400;
 *         fogRadius = 12;                // 开雾半径（格）
 *         detectionRange = 12 * tilesize; // 反隐探测范围（像素，默认 12 格 = 开雾半径一致）
 *     }};
 *
 * 需要的贴图（放在 sprites/ 下，名字匹配方块 name）：
 *   anti-stealth-radar.png       → 旋转天线（region，自动加载）
 *   anti-stealth-radar-base.png  → 固定底座（baseRegion，load 里手动找）
 *   anti-stealth-radar-glow.png  → 发光层（glowRegion，可选，找不到会自动回退成不画）
 * =======================================================
 */
public class AntiStealthRadar extends Block {

    /* ======================================================
     * 【核心】反射访问 FogControl 的 private 动态开雾通道
     * ======================================================
     * 原版迷雾系统的开雾分为两条：
     *   - staticEvents（静态/永久：写入 Bits staticData → 地图一旦被探索过就永久亮）
     *   - dynamicEventQueue（动态/短时：仅写入 volatile Bits read → 下一帧不写就会消失）
     *
     * 我们要的"雷达照到哪里哪里显形、关掉雷达雾又回去"正是第二条 dynamicEventQueue，
     * 但它在 FogControl 里是 private final LongSeq，没有公开 API。
     * 所以这里做一次静态 Reflect 缓存，拿到 3 个关键成员后直接调用，
     * 失败（比如反射权限被禁）会自动回退为"只开静态雾"，不会崩溃。
     * ====================================================== */
    static boolean reflectInited = false;
    static boolean reflectOk    = false;
    static Field   fc_dynamicEventQueue;   // FogControl.dynamicEventQueue (LongSeq)
    static Field   fc_fog;                 // FogControl.fog (FogData[])
    static Field   fd_dynamicUpdated;      // FogControl$FogData.dynamicUpdated (boolean)
    static Class<?> fogEventClass;         // mindustry.game.FogControl$FogEventStruct
    static Method  fogEvent_get;           // FogEventStruct.get(int x,int y,int radius,int team) → long

    static void initReflect() {
        if (reflectInited) return;
        reflectInited = true;
        try {
            Class<?> fcCls = Class.forName("mindustry.game.FogControl");
            fc_dynamicEventQueue = fcCls.getDeclaredField("dynamicEventQueue");
            fc_dynamicEventQueue.setAccessible(true);
            fc_fog = fcCls.getDeclaredField("fog");
            fc_fog.setAccessible(true);

            // FogData.dynamicUpdated（内部静态类）
            Class<?> fdCls = Class.forName("mindustry.game.FogControl$FogData");
            fd_dynamicUpdated = fdCls.getDeclaredField("dynamicUpdated");
            fd_dynamicUpdated.setAccessible(true);

            // FogEventStruct.get(x,y,radius,team) 返回 long
            fogEventClass = Class.forName("mindustry.game.FogControl$FogEventStruct");
            fogEvent_get  = fogEventClass.getDeclaredMethod("get", int.class, int.class, int.class, int.class);
            fogEvent_get.setAccessible(true);

            reflectOk = true;
        } catch (Throwable ignored) {
            reflectOk = false;
        }
    }

    /**
     * 给雷达所属团队强制开"动态短时视野"（最核心的反隐可视化方法）
     *  - 只会修改 Bits read（动态实时视野），不会改 Bits staticData（永久已探索图）
     *  - 下一 tick 不调用的话，等 FogControl 动态刷新周期（默认 40ms = 25FPS）过了就会失效
     *  - 所以 Radar updateTile() 必须每 tick 都调用一次，维持反隐视野
     *
     * @param team    哪个团队能看到这块区域（一般就是雷达自己的 team）
     * @param cx      中心 tile 坐标 x（World.toTile(x)）
     * @param cy      中心 tile 坐标 y
     * @param radiusT 半径（单位 tile，比如 detectionRange/tilesize）
     * @return 是否成功写入 dynamicEventQueue（失败可能是反射没权限）
     */
    static boolean forceRevealForTeam(Team team, int cx, int cy, int radiusT) {
        if (team == null || radiusT <= 0 || !state.rules.fog) return false;
        if (!reflectInited) initReflect();
        if (!reflectOk || Vars.fogControl == null) return false;
        try {
            // 1) 构造 FogEventStruct.get(cx, cy, radiusT, team.id) → long packed
            long packed = (Long) fogEvent_get.invoke(null, cx, cy, Math.max(1, radiusT), team.id);

            // 2) 取 FogControl.dynamicEventQueue (LongSeq)，塞进去
            LongSeq q = (LongSeq) fc_dynamicEventQueue.get(Vars.fogControl);
            if (q != null) q.add(packed);

            // 3) 把该 team 的 FogData.dynamicUpdated=true，强制下一个 25FPS 周期 flush 到 Bits read
            Object[] fogArr = (Object[]) fc_fog.get(Vars.fogControl);
            if (fogArr != null && team.id >= 0 && team.id < fogArr.length && fogArr[team.id] != null) {
                fd_dynamicUpdated.setBoolean(fogArr[team.id], true);
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /* ======================================================
     *                  对外可调参数（方块级）
     * ====================================================== */

    /** 开雾预热时长（多少 tick 达到最大雾半径） */
    public float discoveryTime = 60f * 8f;

    /** 天线旋转速度（度/秒基准），越大转得越快 */
    public float rotateSpeed = 3.2f;

    /** 开雾半径（单位：格 = tilesize 像素）。原版 Radar 默认 10 格。 */
    public float fogRadius = 10;

    /** 反隐探测半径（像素）。默认和开雾半径一致；可以写死更大或更小。 */
    public float detectionRange = 10f * 8f; /* 默认值会在 init 后被 fogRadius*tilesize 覆盖，这里只是占位 */

    /** 扫描间隔 tick（默认 12 = 0.2 秒扫一次，因为现在每 tick 强制开动态雾，这里只用于统计/倒计时续杯，不是用于"可视化显形"）*/
    public float scanTick = 12f;

    /** 每次扫描让隐身单位保持"被雷达照到"的 targetable 显形时长（tick）。
     *  建议略大于 scanTick，保证下一帧扫描能续上，不会闪烁 targetable=false/true。 */
    public float revealPerStep = 30f;

    /** 是否需要电力驱动扫描（true = 没电时只开雾不反隐）*/
    public boolean consumePower = true;

    /** 天线底座贴图（自动在 load() 中找 {name}-base） */
    public TextureRegion baseRegion;
    /** 天线发光贴图（自动在 load() 中找 {name}-glow，找不到就是空的，不画） */
    public TextureRegion glowRegion;

    /** 发光颜色（默认草绿色，代表"雷达探测中"）*/
    public Color glowColor = Pal.sapBullet;
    /** 发光呼吸强度参数：越大脉冲越明显 */
    public float glowScl = 5f, glowMag = 0.6f;

    /* ======================================================
     *                  构造
     * ====================================================== */

    public AntiStealthRadar(String name) {
        super(name);
        update = true;
        solid = true;
        outlineIcon = true;
        flags = EnumSet.of(BlockFlag.hasFogRadius);
        // 默认耗电（逻辑里再判断 consumePower）
        hasPower = true;
    }

    @Override
    public void load() {
        super.load();
        // —— 手动加载额外的贴图（@Load 注解是 Annotations 编译器用的，mod 直接 Core.atlas.find 即可）
        // 底座：{name}-base
        baseRegion = Core.atlas.find(name + "-base");
        if (!baseRegion.found()) {
            // 没提供 -base 的话就直接用 region 兜底，避免编译期 NPE
            baseRegion = region;
        }
        // 发光层：{name}-glow（没做就不会画）
        glowRegion = Core.atlas.find(name + "-glow");
        // 没找到的话 glowRegion.found() 为 false，draw 里会跳过
    }

    @Override
    public void init() {
        super.init();
        // 如果用户没指定 detectionRange，默认和开雾一样大（像素）
        if (detectionRange <= 1) detectionRange = fogRadius * tilesize;
    }

    @Override
    public TextureRegion[] icons() {
        // 方块图标：底座+天线（如果两个 region 都是同一个图，这里也没事，渲染会自动重叠成一张）
        return new TextureRegion[]{baseRegion, region};
    }

    /* ======================================================
     *                  放置预览 / 选择时绘制
     * ====================================================== */

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        super.drawPlace(x, y, rotation, valid);
        float cx = x * tilesize + offset;
        float cy = y * tilesize + offset;
        // 1) 开雾范围（黄色虚线）
        Drawf.dashCircle(cx, cy, fogRadius * tilesize, Pal.accent);
        // 2) 反隐探测范围（绿色虚线，和上面有区分）
        Drawf.dashCircle(cx, cy, detectionRange, Pal.sapBullet);
    }

    /* ======================================================
     *                  Building：每个雷达方块实例
     * ====================================================== */

    public class AntiStealthRadarBuild extends Building {

        /* ---- 开雾相关（和 RadarBuild 保持一致） ---- */
        public float progress;              // 开雾预热进度 0~1
        public float lastRadius = 0f;       // 上次雾半径变化记录
        public float smoothEfficiency = 1f; // 平滑效率（掉电不会突然没雾）
        public float totalProgress;         // 天线旋转累计

        /* ---- 反隐扫描相关 ---- */
        public float scanTimer = 0f;        // 扫描计时器（到 scanTick 就扫一次）
        public boolean detectedThisFrame;   // 这一帧是否扫到了目标（用于视觉警示）
        public int lastDetectedCount;       // 上一轮扫到的敌人数（用于统计面板）

        /* ===============================================
         *              基础 Building 生命周期
         * =============================================== */

        @Override
        public float fogRadius() {
            // 开雾大小 = 方块雾半径 × 预热进度 × 平滑效率
            return fogRadius * progress * smoothEfficiency;
        }

        @Override
        public void updateTile() {
            // —— 开雾逻辑（照搬 Radar） ——
            smoothEfficiency = Mathf.lerpDelta(smoothEfficiency, efficiency, 0.05f);
            if (Math.abs(fogRadius() - lastRadius) >= 0.5f) {
                Vars.fogControl.forceUpdate(team, this);
                lastRadius = fogRadius();
            }
            progress += edelta() / discoveryTime;
            progress = Mathf.clamp(progress);
            totalProgress += efficiency * edelta();

            // —— 【★ 2026-08-05 每 tick 续杯 revealRadarTick，直接驱动 InvisibleAbility.draw 重画】
            //    为什么不再等 scanTick 0.5 秒扫一次：因为 revealRadarTick < 20 tick 时，drawUnitManually 的 revealAlpha 会开始淡出，
            //    0.5 秒扫一次会出现"半秒钟真隐身→突然闪烁显形"的难受效果。
            //    所以现在每 tick 都 markRadarRevealed(team, x, y, range, revealPerStep)，
            //    续杯 revealPerStep（默认 45 tick ≈ 0.75 秒），雷达扫到就稳定显形，
            //    雷达关掉/出范围，revealRadarTick 自然衰减，最后 20 tick（0.33 秒）平滑淡出。
            //    这样效果完美：雷达扫到哪里，哪里的单位就由 Ability.draw 单独手动重画显形；
            //    没扫到的同类型单位 → UnitType 整类透明贴图 → type.draw() 一个像素都不画 → 真·完全隐身，
            //    武器的子弹/激光/爆炸特效走 Groups.bullet EffectRenderer 照常画出来 →
            //    就是用户截图里那种"只能看到攻击特效从雾里飞出来，不知道单位在哪"的经典效果。
            float eff = consumePower ? Mathf.clamp(smoothEfficiency) : 1f;
            if (eff > 0.02f) {
                float range = detectionRange * eff;
                // 每 tick 续杯（revealRadarTick = max(当前, revealPerStep)），稳定
                InvisibleAbility.markRadarRevealed(team, x, y, range, revealPerStep);
                // 顺便统计"当前范围内多少个"（纯 UI 显示用，每 tick 统计开销很小）
                int cnt = 0;
                for (Unit u : InvisibleAbility.stealthedUnits) {
                    if (u != null && !u.dead && u.team != team && u.within(x, y, range)) cnt++;
                }
                lastDetectedCount = cnt;
                detectedThisFrame = cnt > 0;
            } else {
                // 没通电：统计清零
                lastDetectedCount = 0;
                detectedThisFrame = false;
            }

            // —— scanTick 周期保留（不需要了，但保留变量防止有人读档读不到字段崩）
            scanTimer += Time.delta;
            if (scanTimer > scanTick * 10f) scanTimer = 0f;
        }

        @Override
        public boolean canPickup() {
            return false;
        }

        @Override
        public float progress() {
            return progress;
        }

        /* ===============================================
         *              选中方块时：绘制探测范围圈
         * =============================================== */

        @Override
        public void drawSelect() {
            super.drawSelect();
            // 开雾范围（accent 黄）
            Drawf.dashCircle(x, y, fogRadius() * tilesize, Pal.accent);
            // 反隐范围（sap 绿）
            float eff = consumePower ? Mathf.clamp(smoothEfficiency) : 1f;
            if (eff > 0.02f) {
                Drawf.dashCircle(x, y, detectionRange * eff, Pal.sapBullet);
            }
        }

        /* ===============================================
         *              主绘制：底座 + 旋转天线 + 扫描扇形
         * =============================================== */

        @Override
        public void draw() {
            // 1) 固定底座（不旋转）
            Draw.rect(baseRegion, x, y);

            // 2) 旋转天线（角度 = rotateSpeed * 累计进度）
            float angle = rotateSpeed * totalProgress;
            Draw.rect(region, x, y, angle);

            // 3) 发光层（脉冲动画，和原版 Radar 一样）—— 只有 .found() 才画
            if (glowRegion != null && glowRegion.found()) {
                float pulse = 1f - glowMag + Mathf.absin(glowScl, glowMag);
                // 扫到目标时，发光变红色警告
                Color use = detectedThisFrame ? Tmp.c1.set(Pal.remove).lerp(glowColor, 0.3f) : glowColor;
                Drawf.additive(glowRegion, Tmp.c2.set(use).a(glowColor.a * pulse),
                    x, y, angle, Layer.blockAdditive);
            }

            // 4) 扫描扇形（绿色半透明扫光，只在"有电/可以反隐"时画）
            float eff = consumePower ? Mathf.clamp(smoothEfficiency) : 1f;
            if (eff > 0.02f) {
                float range = detectionRange * eff;
                Draw.color(Tmp.c1.set(Pal.sapBullet).a(0.07f * eff));
                // 扫扇形：用多个三角近似，避免过多 Draw 调用
                float sweep = 22f; // 扇形开角
                int sides = 24;
                float startA = angle - sweep;
                float endA = angle + sweep;
                for (int i = 0; i < sides; i++) {
                    float a1 = startA + (endA - startA) * (i / (float) sides);
                    float a2 = startA + (endA - startA) * ((i + 1) / (float) sides);
                    float x1 = x + Angles.trnsx(a1, range);
                    float y1 = y + Angles.trnsy(a1, range);
                    float x2 = x + Angles.trnsx(a2, range);
                    float y2 = y + Angles.trnsy(a2, range);
                    Fill.tri(x, y, x1, y1, x2, y2);
                }
                // 扇形边缘线（更清晰）
                Draw.color(Tmp.c1.set(Pal.sapBullet).a(0.35f * eff));
                Lines.stroke(1.2f);
                Lines.arc(x, y, range, sweep / 360f, angle);
                Lines.stroke(1f);
                Draw.color();
            }
        }

        /* ===============================================
         *              方块信息面板（配置页）
         * =============================================== */

        @Override
        public void buildConfiguration(Table table) {
            table.table(t -> {
                t.left();
                t.add("[lightgray]当前探测目标数：[]").left();
                t.add(String.valueOf(lastDetectedCount)).padLeft(10).color(lastDetectedCount > 0 ? Pal.remove : Pal.sapBullet).left();
                t.row();
                t.add("[lightgray]扫描间隔：[]").left();
                t.add(Strings.autoFixed(scanTick / 60f, 2) + " s").padLeft(10).left();
            }).row();
        }

        /* ===============================================
         *              读写存档（防止读档坏档）
         * =============================================== */

        @Override
        public void write(Writes write) {
            super.write(write);
            write.f(progress);
            write.f(scanTimer);
            write.i(lastDetectedCount);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            // 新方块首次保存版本：一定带 progress + scanTimer + lastDetectedCount 三个字段
            progress = read.f();
            scanTimer = read.f();
            lastDetectedCount = read.i();
        }
    }
}
