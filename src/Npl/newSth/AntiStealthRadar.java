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

import static mindustry.Vars.*;

/**
 * 【反隐雷达 AntiStealthRadar】
 * =======================================================
 * 功能：
 *   1. 和原版 Radar 一样有开雾半径（Block.fogRadius，单位：格），只有预热 + 有电才满半径
 *   2. 额外：以 detectionRange 为半径，给范围内所有敌方隐身单位"续杯显形"
 *      → InvisibleAbility.revealRadarTick = max(当前, 续杯时长)
 *      → InvisibleAbility.draw() 会在最后 20 tick 内淡出，所以续杯时长必须能覆盖到下一次扫描
 *   3. 扫描按 scanTick 周期执行（默认 12 tick = 0.2 秒一次），不是每 tick 执行
 *   4. 旋转天线动画 + 扫描扇形 + 发现目标时的红色警示发光
 *   5. 可配置是否需要电力：没电时只保留开雾，不反隐
 *
 * 【本次优化要点】
 *   - 扫描从"每 tick"改为"每 scanTick"（默认 12 tick → 开销降到 1/12，配置 30 tick 时降到 1/30）
 *   - 续杯时长自动取 max(revealPerStep, scanTick + 30)，保证两次扫描之间不会落进 20 tick 淡出窗口 → 不闪烁
 *     （原来靠每 tick 续杯来避免闪烁，现在用"时长覆盖周期"这个更便宜的办法）
 *   - 一次扫描同时完成"显形 + 统计"，删掉了原先每 tick 遍历 InvisibleAbility.stealthedUnits 的 O(N) 统计循环
 *   - 删除了从未被调用的 FogControl 私有字段反射方案（dynamicEventQueue / FogData.dynamicUpdated），
 *     静态状态与反射权限风险一并消失；如需恢复"强制开动态迷雾"版本，改回修改前的 AntiStealthRadar.java 即可
 *   - 修正字段隐藏：不再声明 float fogRadius 覆盖 Block.fogRadius(int)，改回和原版 Radar 一样用父类字段
 *   - 修正 detectionRange 单位混乱造成的"探测半径只有 1 格"问题（详见字段注释）
 *
 * 放置/使用方法（在 NuBlocks.java 中注册）：
 *   antiStealthRadar = new AntiStealthRadar("anti-stealth-radar"){{
 *       requirements(Category.effect, with(Items.silicon, 120, Items.plastanium, 40, Items.surgeAlloy, 25));
 *       size = 2;
 *       health = 2400;
 *       fogRadius = 12;                          // 开雾半径（格，int）——父类字段
 *       detectionRange = 12 * tilesize;          // 反隐半径（像素！想按格写就乘 tilesize）
 *       consumePower(10f);                       // 耗电由 consumePower(x) 声明（会自动置 hasPower）
 *   }};
 *
 * 需要的贴图（放在 sprites/ 下，名字匹配方块 name）：
 *   anti-stealth-radar.png       → 旋转天线（region，自动加载）
 *   anti-stealth-radar-base.png  → 固定底座（baseRegion，load 里手动找，找不到用 region 兜底）
 *   anti-stealth-radar-glow.png  → 发光层（glowRegion，可选，找不到就不画）
 * =======================================================
 */
public class AntiStealthRadar extends Block {

    /* ======================================================
     *                  对外可调参数（方块级）
     * ====================================================== */

    /** 开雾预热时长（多少 tick 达到最大雾半径）。注意：开雾半径本身用父类 Block.fogRadius（int，单位：格）。 */
    public float discoveryTime = 60f * 8f;

    /** 天线旋转速度（度/秒基准），越大转得越快 */
    public float rotateSpeed = 3.2f;

    /**
     * 反隐探测半径，单位是<b>像素</b>（Mindustry 世界单位）。
     *   <= 1 时自动取 fogRadius * tilesize（= 和开雾一样大）。
     *   ⚠ 别再写 detectionRange = fogRadius * 0.8f —— fogRadius 是"格"，乘出来的 11 像素比方块自己还小，
     *     反隐会等于没开。正确写法：fogRadius * 0.8f * tilesize 或 格数 * tilesize。
     */
    public float detectionRange = -1f;

    /** 扫描周期（tick）。默认 12 = 0.2 秒一次；不是每 tick 扫描，周期越短越灵敏、越费 CPU。 */
    public float scanTick = 12f;

    /**
     * 每次扫描给命中单位续杯的显形时长（tick）。
     * 实际下发的时长会自动取 max(revealPerStep, scanTick + 30)：
     * InvisibleAbility 的淡出窗口是最后 20 tick，续杯必须 > 扫描周期 + 20 才不会中途开始淡出（闪烁）。
     */
    public float revealPerStep = 30f;

    /** 是否必须通电才反隐（true = 没电只开雾）。耗电量请用方块定义里的 consumePower(x) 声明。 */
    public boolean requiresPower = true;

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

        // 开雾半径用父类 Block.fogRadius（int，单位：格），和原版 Radar 一致。
        // 千万不要在子类里再声明一个 float fogRadius：那会隐藏父类字段，
        // 游戏内部代码读 block.fogRadius 只会读到 0（开雾标记、面板数值都会出错）。
        fogRadius = 10;

        // 这里不再硬写 hasPower = true：耗电由方块定义里的 consumePower(x) 声明，
        // ConsumePower.apply() 会自动把 hasPower 置为 true；
        // 如果不需要电（requiresPower = false），也就不会白白接进电网。
    }

    @Override
    public void load() {
        super.load();
        // —— 手动加载额外贴图（@Load 注解是 Annotations 编译器用的，mod 直接 Core.atlas.find 即可）
        baseRegion = Core.atlas.find(name + "-base");
        if (!baseRegion.found()) {
            // 没提供 -base 就用本体贴图兜底，避免绘制期 NPE
            baseRegion = region;
        }
        glowRegion = Core.atlas.find(name + "-glow");
        // 没找到时 glowRegion.found() 为 false，draw() 会自动跳过
    }

    @Override
    public void init() {
        super.init();
        if (detectionRange <= 1f) {
            // 默认和开雾半径一致（fogRadius 是格，乘 tilesize 换成像素）
            detectionRange = fogRadius * tilesize;
        } else if (detectionRange < tilesize * 2f) {
            // 单位写错的兜底提醒：detectionRange 是像素，不是格
            Log.warn("[AntiStealthRadar] @ 的 detectionRange=@ 像素，比 2 格还小；" +
                "该字段单位是像素，想按格配置请乘 tilesize", name, detectionRange);
        }
    }

    @Override
    public TextureRegion[] icons() {
        // 方块图标：底座 + 天线
        return new TextureRegion[]{baseRegion, region};
    }

    /* ======================================================
     *                  放置预览
     * ====================================================== */

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        super.drawPlace(x, y, rotation, valid);
        float cx = x * tilesize + offset;
        float cy = y * tilesize + offset;
        // 1) 开雾范围（黄色虚线）
        Drawf.dashCircle(cx, cy, fogRadius * tilesize, Pal.accent);
        // 2) 反隐探测范围（绿色虚线）
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
        /** 距离下一次扫描的计时（tick）。参与存档，保证读档后不会立刻连扫。 */
        public float scanTimer = 0f;
        /** 上一次扫描命中的隐身单位数（面板显示用，参与存档）。 */
        public int lastDetectedCount = 0;
        /** 上一次扫描是否发现目标（决定发光是否转红；语义是"上一次扫描"，不是"这一帧"）。 */
        public boolean detectedLastScan = false;
        /** 上电后立即扫一次，避免白白等一个 scanTick（不存档；读档后为 false → 立刻扫一次，正好）。 */
        boolean scannedOnce = false;

        /* ===============================================
         *              基础 Building 生命周期
         * =============================================== */

        @Override
        public float fogRadius() {
            // 开雾大小 = 开雾半径(格) × 预热进度 × 平滑效率
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
            progress += edelta() / Math.max(1f, discoveryTime);
            progress = Mathf.clamp(progress);
            totalProgress += efficiency * edelta();

            // —— 反隐扫描：按 scanTick 周期执行 ——
            // 为什么不再每 tick 续杯：每 tick 调用 markRadarRevealed 会每 tick 做一次空间查询，
            // 30 tick 的周期就是 30 倍开销。改成"扫描时下发一个足够长的显形时长"，
            // 只要 duration >= scanTick + 20(淡出窗口)，两次扫描之间的显形就不会开始淡出 → 一样稳定不闪。
            float eff = requiresPower ? Mathf.clamp(smoothEfficiency) : 1f;
            if (eff <= 0.02f) {
                // 没电/无效率：清干净状态，不扫描
                scanTimer = 0f;
                scannedOnce = false;
                lastDetectedCount = 0;
                detectedLastScan = false;
                return;
            }

            float interval = Math.max(1f, scanTick);
            scanTimer += Time.delta;
            if (!scannedOnce || scanTimer >= interval) {
                // 保留余数（低帧率下不丢扫描次数）；首次上电时立即扫一次
                scanTimer = scannedOnce ? scanTimer % interval : 0f;
                scannedOnce = true;
                scan(eff);
            }
            // 目标数只在上面的 scan() 里更新：detectedLastScan 会一直保持到下一次扫描，
            // 所以"发现目标变红"的视觉警示会持续一整个扫描周期，而不是闪一下就没了。
        }

        /** 一次反隐扫描：给 detectionRange*eff 内的敌方隐身单位续杯显形，并统计命中数。 */
        protected void scan(float eff) {
            // 续杯时长必须覆盖"下一次扫描 + 20 tick 淡出窗口"，否则显形会在两次扫描之间开始淡出（闪烁）
            float duration = Math.max(revealPerStep, Math.max(1f, scanTick) + 30f);
            lastDetectedCount = InvisibleAbility.markRadarRevealed(team, x, y, detectionRange * eff, duration);
            detectedLastScan = lastDetectedCount > 0;
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
            float eff = requiresPower ? Mathf.clamp(smoothEfficiency) : 1f;
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

            // 3) 发光层（脉冲动画）—— 只有 .found() 才画
            if (glowRegion != null && glowRegion.found()) {
                float pulse = 1f - glowMag + Mathf.absin(glowScl, glowMag);
                // 上一次扫描发现目标 → 发光偏红警示
                Color use = detectedLastScan ? Tmp.c1.set(Pal.remove).lerp(glowColor, 0.3f) : glowColor;
                Drawf.additive(glowRegion, Tmp.c2.set(use).a(glowColor.a * pulse),
                    x, y, angle, Layer.blockAdditive);
            }

            // 4) 扫描扇形（绿色半透明扫光，只在有电/可反隐时画）
            float eff = requiresPower ? Mathf.clamp(smoothEfficiency) : 1f;
            if (eff > 0.02f) {
                float range = detectionRange * eff;
                float sweep = 22f;   // 半角：扇形总开角 = 2 * sweep = 44°
                int sides = 24;

                // 扇形填充（多个三角形近似）
                Draw.color(Tmp.c1.set(Pal.sapBullet).a(0.07f * eff));
                float startA = angle - sweep;
                float endA = angle + sweep;
                for (int i = 0; i < sides; i++) {
                    float a1 = startA + (endA - startA) * (i / (float) sides);
                    float a2 = startA + (endA - startA) * ((i + 1) / (float) sides);
                    Fill.tri(x, y,
                        x + Angles.trnsx(a1, range), y + Angles.trnsy(a1, range),
                        x + Angles.trnsx(a2, range), y + Angles.trnsy(a2, range));
                }

                // 扇形边缘线：占比和起始角必须和上面的扇形一致
                // （原来是 sweep/360 且从 angle 起画，只能画出半个扇形）
                Draw.color(Tmp.c1.set(Pal.sapBullet).a(0.35f * eff));
                Lines.stroke(1.2f);
                Lines.arc(x, y, range, (sweep * 2f) / 360f, startA);
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
                t.add(String.valueOf(lastDetectedCount)).padLeft(10)
                    .color(lastDetectedCount > 0 ? Pal.remove : Pal.sapBullet).left();
                t.row();
                t.add("[lightgray]扫描间隔：[]").left();
                t.add(Strings.autoFixed(Math.max(1f, scanTick) / 60f, 2) + " s").padLeft(10).left();
                t.row();
                t.add("[lightgray]探测半径：[]").left();
                t.add(Strings.autoFixed(detectionRange / tilesize, 1) + " 格").padLeft(10).left();
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
            // 字段顺序保持不变，老存档可继续读取
            progress = read.f();
            scanTimer = read.f();
            lastDetectedCount = read.i();
        }
    }
}
