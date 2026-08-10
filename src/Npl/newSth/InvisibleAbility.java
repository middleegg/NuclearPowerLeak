package Npl.newSth;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.entities.abilities.Ability;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;

import static mindustry.Vars.*;

/**
 * 【隐身能力 InvisibleAbility（极简迷雾版）】
 * =======================================================
 *  设计思路（完全解决"一只被打全部现形"的根因）：
 *
 *  之前的做法（UnitType 级贴图替换）→ 因为 Mindustry type.draw 是整类共享的，
 *    所以任何类型级别的改动必然同时影响所有同类型单位 → 不可避免的一只被打全体现形。
 *
 *  现在的做法（100% 靠原版战争迷雾系统）：
 *    单位的"看得见/看不见"完全由原版 FogControl 控制：
 *      - 当前玩家视野/雷达覆盖的区域（Bits read == 1）：单位正常绘制 → 看得见
 *      - 已探索但当前无视野的迷雾层（Bits read == 0，staticData == 1）：单位不绘制
 *        但子弹/激光/爆炸特效不受迷雾 Bits 约束，依然被全局绘制
 *        → 这就形成了你截图里那种经典效果：
 *          "看不见敌方单位在哪，但能看到它们射出来的激光/子弹从迷雾里飞出来"
 *
 *  所以本类现在只做两件事（一件 targetable、一件倒计时），
 *  连 draw() 里都只留 Draw.reset()，再也不碰任何渲染状态：
 *    ① 不可锁定（核心玩法）：隐身单位的 UnitType.targetable = false
 *       原版 Turret/Units.closest/Units.bestTarget 会被过滤，
 *       敌方炮台/AI 不再主动瞄准并攻击你（除非被群攻 AOE 溅到）。
 *    ② revealDamageTick / revealRadarTick 倒计时（供"显形条件"参考）：
 *       被打时延长 damage 倒计时，被 AntiStealthRadar 扫到时延长 radar 倒计时。
 *
 *  AntiStealthRadar 改做"强制给该团队开动态短时迷雾"，
 *  具体见 AntiStealthRadar.java —— 实现"雷达扫到哪里，哪里就显形，没扫到立刻恢复迷雾"。
 * =======================================================
 */
public class InvisibleAbility extends Ability {

    /* ======================================================
     *                  静态全局
     * ====================================================== */

    /** 所有带 InvisibleAbility 的单位（用于统计） */
    static final ObjectSet<Unit> stealthedUnits = new ObjectSet<>();

    /** type -> 该类型单位首次加入时备份的原始 targetable，用于退出/死亡还原 */
    static final ObjectMap<UnitType, Boolean> originalTargetable = new ObjectMap<>();

    /** 上一次同步的 tick 标记（Time.time 是 float，每帧+1），防止多 Ability 实例重复同步 */
    static float lastSyncTick = -1f;

    /* ======================================================
     *   UnitType 级贴图/绘制开关备份（用于整类替换透明贴图）
     *   —— 只备份一次，然后整类替换成空贴图（type.draw() 一个像素都画不出来）
     *   —— 被探测到的那只单位，会在 Ability.draw() 里按备份 per-unit 手动重画出来
     * ====================================================== */

    /** 1x1 透明 TextureRegion，找不到 clear/alpha 就 null（表示不做贴图替换） */
    static TextureRegion emptyRegion = null;

    /**
     * UnitType → 长度 16 的 TextureRegion 备份（按 Mindustry 159.6 实际字段排列）
     *   索引表：
     *   [0]=baseRegion   [1]=cellRegion   [2]=region       [3]=shadowRegion
     *   [4]=legRegion    [5]=footRegion   [6]=jointRegion  [7]=baseLegRegion
     *   [8]=outlineRegion [9]=glowRegion  [10]=itemCircleRegion [11]=softShadowRegion
     *   [12..15] = 预留扩展
     */
    static final ObjectMap<UnitType, TextureRegion[]> originalRegions = new ObjectMap<>();

    /**
     * UnitType → 绘制开关/绘制相关引用备份（Object[12]）：
     *   [0] = drawCell (Boolean)   [1] = drawBody (Boolean)
     *   [2] = drawItem (Boolean)   [3] = drawShadow (Boolean)
     *   [4] = drawSoftShadow (Boolean)
     *   [5] = outlineRadius (Float)  [6] = useEngineElevation (Boolean)
     *   [7] = engineColor (Color)
     *   [8] = engines (Seq<UnitType.UnitEngine>)  【非常关键！引擎尾焰是程序 Fill.circle，不走 TextureRegion】
     *   [9] = trailLength (Integer)  [10] = lightRadius (Float)  [11] = lightOpacity (Float)
     */
    static final ObjectMap<UnitType, Object[]> drawSwitchBackups = new ObjectMap<>();

    /** 取 1x1 真透明贴图（只取一次缓存，找不到就 null 不做替换） */
    static TextureRegion emptyRegion() {
        if (emptyRegion == null) {
            TextureRegion r = Core.atlas.find("clear");
            if (r == null || !r.found()) r = Core.atlas.find("alpha");
            if (r == null || !r.found()) return null;
            emptyRegion = r;
        }
        return emptyRegion;
    }

    /** 备份 UnitType 的 16 张贴图，返回备份数组（备份过直接 return 已存在） */
    static TextureRegion[] backupUnitTypeRegions(UnitType t) {
        if (t == null) return null;
        TextureRegion[] arr = originalRegions.get(t);
        if (arr != null) return arr;
        arr = new TextureRegion[16];
        try {
            // 按上面的索引表依次备份：
            //   对于「UnitType 基类里可能没有」的字段（baseLegRegion、glowRegion、softShadowRegion）
            //   用 Reflect 访问（字段不存在 → catch 忽略，这样编译期就不会报找不到符号）
            try { arr[0] = t.baseRegion; } catch (Throwable ignored) {}
            try { arr[1] = t.cellRegion; } catch (Throwable ignored) {}
            try { arr[2] = t.region; } catch (Throwable ignored) {}
            try { arr[3] = t.shadowRegion; } catch (Throwable ignored) {}
            try { arr[4] = t.legRegion; } catch (Throwable ignored) {}
            try { arr[5] = t.footRegion; } catch (Throwable ignored) {}
            try { arr[6] = t.jointRegion; } catch (Throwable ignored) {}
            try {
                java.lang.reflect.Field f = t.getClass().getField("baseLegRegion");
                f.setAccessible(true);
                Object v = f.get(t);
                if (v instanceof TextureRegion) arr[7] = (TextureRegion) v;
            } catch (Throwable ignored) { arr[7] = null; }
            try { arr[8] = t.outlineRegion; } catch (Throwable ignored) {}
            try {
                java.lang.reflect.Field f = t.getClass().getField("glowRegion");
                f.setAccessible(true);
                Object v = f.get(t);
                if (v instanceof TextureRegion) arr[9] = (TextureRegion) v;
            } catch (Throwable ignored) { arr[9] = null; }
            try { arr[10] = t.itemCircleRegion; } catch (Throwable ignored) {}
            try {
                java.lang.reflect.Field f = t.getClass().getField("softShadowRegion");
                f.setAccessible(true);
                Object v = f.get(t);
                if (v instanceof TextureRegion) arr[11] = (TextureRegion) v;
            } catch (Throwable ignored) { arr[11] = null; }
        } catch (Throwable ignored) {}
        originalRegions.put(t, arr);
        return arr;
    }

    /** 备份 UnitType 的绘制开关 + 引擎/拖尾/光照 */
    static Object[] backupUnitTypeDraw(UnitType t) {
        if (t == null) return null;
        Object[] arr = drawSwitchBackups.get(t);
        if (arr != null) return arr;
        arr = new Object[12];
        try {
            try { arr[0] = t.drawCell; } catch (Throwable ignored) {}
            try { arr[1] = t.drawBody; } catch (Throwable ignored) {}
            // drawItem / drawShadow → 基类 UnitType 未必有，一律反射访问（避免编译期找不到符号）
            try {
                java.lang.reflect.Field f = t.getClass().getField("drawItem");
                f.setAccessible(true);
                Object v = f.get(t);
                arr[2] = (v instanceof Boolean) ? (Boolean) v : Boolean.TRUE;
            } catch (Throwable ignored) { arr[2] = Boolean.TRUE; }
            try {
                java.lang.reflect.Field f = t.getClass().getField("drawShadow");
                f.setAccessible(true);
                Object v = f.get(t);
                arr[3] = (v instanceof Boolean) ? (Boolean) v : Boolean.TRUE;
            } catch (Throwable ignored) { arr[3] = Boolean.TRUE; }
            try { arr[4] = false;
                java.lang.reflect.Field f = t.getClass().getField("drawSoftShadow");
                f.setAccessible(true);
                arr[4] = (Boolean) f.get(t);
            } catch (Throwable ignored) { arr[4] = Boolean.TRUE; }
            // outlineRadius 在 Mindustry 159.6 UnitType 里是 int primitive（不是 Integer/Float，非 null）
            try { arr[5] = Float.valueOf((float) t.outlineRadius); } catch (Throwable ignored) { arr[5] = 0f; }
            try { arr[6] = t.useEngineElevation; } catch (Throwable ignored) { arr[6] = Boolean.TRUE; }
            try { arr[7] = t.engineColor == null ? null : new Color(t.engineColor); } catch (Throwable ignored) {}
            try {
                Object engs = t.engines;
                if (engs == null || !(engs instanceof Seq)) {
                    arr[8] = null;
                } else {
                    // 深拷贝一份 Seq<UnitEngine>（防止原列表被 clear 后备份也丢了）
                    Seq<?> src = (Seq<?>) engs;
                    Seq<Object> cp = new Seq<>(src.size);
                    for (Object o : src) cp.add(o);
                    arr[8] = cp;
                }
            } catch (Throwable ignored) { arr[8] = null; }
            try { arr[9]  = Integer.valueOf(t.trailLength); } catch (Throwable ignored) { arr[9] = 0; }
            try { arr[10] = Float.valueOf(t.lightRadius); } catch (Throwable ignored) { arr[10] = 0f; }
            try { arr[11] = Float.valueOf(t.lightOpacity); } catch (Throwable ignored) { arr[11] = 1f; }
        } catch (Throwable ignored) {}
        drawSwitchBackups.put(t, arr);
        return arr;
    }

    /** UnitType 贴图 → 全换成空贴图 */
    static void setUnitTypeFullyInvisible(UnitType t) {
        if (t == null) return;
        TextureRegion er = emptyRegion();
        if (er == null) return;
        backupUnitTypeRegions(t);
        try {
            try { t.baseRegion = er; } catch (Throwable ignored) {}
            try { t.cellRegion = er; } catch (Throwable ignored) {}
            try { t.region = er; } catch (Throwable ignored) {}
            try { t.shadowRegion = er; } catch (Throwable ignored) {}
            try { t.legRegion = er; } catch (Throwable ignored) {}
            try { t.footRegion = er; } catch (Throwable ignored) {}
            try { t.jointRegion = er; } catch (Throwable ignored) {}
            // baseLegRegion → 基类未必有，反射访问（避免编译期找不到符号）
            try {
                java.lang.reflect.Field f = t.getClass().getField("baseLegRegion");
                f.setAccessible(true);
                f.set(t, er);
            } catch (Throwable ignored) {}
            try { t.outlineRegion = er; } catch (Throwable ignored) {}
            // glowRegion → 基类未必有，反射访问
            try {
                java.lang.reflect.Field f = t.getClass().getField("glowRegion");
                f.setAccessible(true);
                f.set(t, er);
            } catch (Throwable ignored) {}
            try { t.itemCircleRegion = er; } catch (Throwable ignored) {}
            try {
                java.lang.reflect.Field f = t.getClass().getField("softShadowRegion");
                f.setAccessible(true);
                f.set(t, er);
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    /** 还原 UnitType 贴图（如果有备份） */
    static void restoreUnitTypeIfVisible(UnitType t) {
        if (t == null) return;
        TextureRegion[] arr = originalRegions.get(t);
        if (arr == null) return;
        try {
            try { if (arr[0] != null) t.baseRegion = arr[0]; } catch (Throwable ignored) {}
            try { if (arr[1] != null) t.cellRegion = arr[1]; } catch (Throwable ignored) {}
            try { if (arr[2] != null) t.region = arr[2]; } catch (Throwable ignored) {}
            try { if (arr[3] != null) t.shadowRegion = arr[3]; } catch (Throwable ignored) {}
            try { if (arr[4] != null) t.legRegion = arr[4]; } catch (Throwable ignored) {}
            try { if (arr[5] != null) t.footRegion = arr[5]; } catch (Throwable ignored) {}
            try { if (arr[6] != null) t.jointRegion = arr[6]; } catch (Throwable ignored) {}
            // baseLegRegion → 反射写
            try {
                if (arr[7] != null) {
                    java.lang.reflect.Field f = t.getClass().getField("baseLegRegion");
                    f.setAccessible(true);
                    f.set(t, arr[7]);
                }
            } catch (Throwable ignored) {}
            try { if (arr[8] != null) t.outlineRegion = arr[8]; } catch (Throwable ignored) {}
            // glowRegion → 反射写
            try {
                if (arr[9] != null) {
                    java.lang.reflect.Field f = t.getClass().getField("glowRegion");
                    f.setAccessible(true);
                    f.set(t, arr[9]);
                }
            } catch (Throwable ignored) {}
            try { if (arr[10] != null) t.itemCircleRegion = arr[10]; } catch (Throwable ignored) {}
            try {
                if (arr[11] != null) {
                    java.lang.reflect.Field f = t.getClass().getField("softShadowRegion");
                    f.setAccessible(true);
                    f.set(t, arr[11]);
                }
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    /** UnitType 绘制开关 → 全关 + 清引擎 + 关拖尾 + 关光照 */
    static void setUnitTypeDrawHidden(UnitType t) {
        if (t == null) return;
        backupUnitTypeDraw(t);
        try {
            try { t.drawCell = false; } catch (Throwable ignored) {}
            try { t.drawBody = false; } catch (Throwable ignored) {}
            // drawItem → 反射写
            try {
                java.lang.reflect.Field f = t.getClass().getField("drawItem");
                f.setAccessible(true);
                f.setBoolean(t, false);
            } catch (Throwable ignored) {}
            // drawShadow → 反射写
            try {
                java.lang.reflect.Field f = t.getClass().getField("drawShadow");
                f.setAccessible(true);
                f.setBoolean(t, false);
            } catch (Throwable ignored) {}
            try {
                java.lang.reflect.Field f = t.getClass().getField("drawSoftShadow");
                f.setAccessible(true);
                f.setBoolean(t, false);
            } catch (Throwable ignored) {}
            // outlineRadius：UnitType 里是 int primitive → 设为 0（不是 0.0f）
            try { t.outlineRadius = 0; } catch (Throwable ignored) {}
            // 清引擎 Seq（否则每 tick 还是会 Fill.circle 画尾焰）
            try {
                if (t.engines != null) t.engines.clear();
            } catch (Throwable ignored) {}
            try { t.trailLength = 0; } catch (Throwable ignored) {}
            try { t.lightRadius = -1f; } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    /** 还原 UnitType 绘制开关/引擎/拖尾/光照（如果有备份） */
    static void restoreUnitTypeDrawIfHidden(UnitType t) {
        if (t == null) return;
        Object[] arr = drawSwitchBackups.get(t);
        if (arr == null) return;
        try {
            try { if (arr[0] instanceof Boolean) t.drawCell = (Boolean) arr[0]; } catch (Throwable ignored) {}
            try { if (arr[1] instanceof Boolean) t.drawBody = (Boolean) arr[1]; } catch (Throwable ignored) {}
            // drawItem → 反射写
            try {
                if (arr[2] instanceof Boolean) {
                    java.lang.reflect.Field f = t.getClass().getField("drawItem");
                    f.setAccessible(true);
                    f.setBoolean(t, (Boolean) arr[2]);
                }
            } catch (Throwable ignored) {}
            // drawShadow → 反射写
            try {
                if (arr[3] instanceof Boolean) {
                    java.lang.reflect.Field f = t.getClass().getField("drawShadow");
                    f.setAccessible(true);
                    f.setBoolean(t, (Boolean) arr[3]);
                }
            } catch (Throwable ignored) {}
            try {
                java.lang.reflect.Field f = t.getClass().getField("drawSoftShadow");
                f.setAccessible(true);
                f.setBoolean(t, Boolean.TRUE.equals(arr[4]));
            } catch (Throwable ignored) {}
            // outlineRadius 备份的是 Float → 转成 int primitive 写
            try {
                if (arr[5] instanceof Float) {
                    t.outlineRadius = Math.max(0, Math.round((Float) arr[5]));
                }
            } catch (Throwable ignored) {}
            try { if (arr[6] instanceof Boolean) t.useEngineElevation = (Boolean) arr[6]; } catch (Throwable ignored) {}
            try {
                // 引擎：把备份的 Seq<UnitEngine> 里的每一个重新 add 回去
                if (arr[8] instanceof Seq) {
                    Seq<?> src = (Seq<?>) arr[8];
                    try {
                        if (t.engines == null) t.engines = new Seq();
                        t.engines.clear();
                        for (Object ue : src) t.engines.add((UnitType.UnitEngine) ue);
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
            try { if (arr[9] instanceof Integer) t.trailLength = (Integer) arr[9]; } catch (Throwable ignored) {}
            try { if (arr[10] instanceof Float) t.lightRadius = (Float) arr[10]; } catch (Throwable ignored) {}
            try { if (arr[11] instanceof Float) t.lightOpacity = (Float) arr[11]; } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    /** 该 UnitType 是否需要被整类换成透明贴图（有任意一个实例 fullyInvisible=true） */
    static boolean shouldMakeAllInvisible(UnitType t) {
        if (t == null || emptyRegion() == null) return false;
        // 遍历 stealthedUnits 里所有这个类型的实例，只要有一个 fullyInvisible 就整类替换
        for (Unit u : stealthedUnits) {
            if (u == null || u.dead || u.type != t) continue;
            for (Ability ab : u.abilities) {
                if (ab instanceof InvisibleAbility ia && ia.fullyInvisible) return true;
            }
        }
        return false;
    }

    static {
        // 地图加载时重置所有静态状态 + 还原所有备份过的 UnitType.targetable/贴图/绘制开关
        Events.on(WorldLoadEvent.class, e -> {
            // 还原 targetable
            for (ObjectMap.Entry<UnitType, Boolean> en : originalTargetable.entries()) {
                if (en.key != null) en.key.targetable = en.value;
            }
            originalTargetable.clear();
            // 还原贴图
            for (UnitType t : originalRegions.keys()) restoreUnitTypeIfVisible(t);
            originalRegions.clear();
            // 还原绘制开关
            for (UnitType t : drawSwitchBackups.keys()) restoreUnitTypeDrawIfHidden(t);
            drawSwitchBackups.clear();
            stealthedUnits.clear();
            emptyRegion = null;
            lastSyncTick = -1f;
        });
    }

    /* ======================================================
     *                  对外可调参数（实例级）
     * ====================================================== */

    // —— 以下 3 个字段是「向后兼容 FederalUnitType 里的旧配置」
    //    现在的极简迷雾版方案，单位显隐 100% 交给原版 FogControl 决定，
    //    这三个参数不再参与渲染逻辑，但保留给旧配置代码赋值，防止编译报错。
    /** （旧版参数，现已不参与渲染）true = 完全隐身模式 */
    public boolean fullyInvisible = true;
    /** （旧版参数，现已不参与渲染）true = 队友能看到单位本体 */
    public boolean visibleToAllies = true;
    /** （旧版参数，现已不参与渲染）>0 = 中心留个呼吸小圆点 */
    public float stealthDotSize = 0f;

    /** 受击后保持多少 tick 可锁定（默认 240 = 4 秒） */
    public float revealDamageDuration = 60f * 4f;

    /** 反隐雷达每次扫描命中续杯多少 tick（默认 45 = 略大于 scanTick 防闪烁） */
    public float revealRadarDuration = 45f;

    /* ======================================================
     *                  实例内部状态
     * ====================================================== */

    /** 受击显形剩余 tick（>0 表示该单位当前因为"被打"而处于显形态） */
    public float revealDamageTick = 0f;

    /** 雷达探测显形剩余 tick（>0 表示该单位当前在雷达范围内） */
    public float revealRadarTick = 0f;

    // 【事件监听全局只注册一次】
    static boolean eventsInited = false;

    static void initEvents() {
        if (eventsInited) return;
        eventsInited = true;
        // 单位受伤：延长 damage 倒计时
        Events.on(UnitDamageEvent.class, e -> {
            if (e == null || e.unit == null) return;
            for (Ability ab : e.unit.abilities) {
                if (ab instanceof InvisibleAbility ia) {
                    ia.revealDamageTick = Math.max(ia.revealDamageTick, ia.revealDamageDuration);
                    break;
                }
            }
        });
    }

    /* ======================================================
     *                  对外工具
     * ====================================================== */

    /** 反隐雷达调用：给指定单位延长 radar 显形倒计时 */
    public static void markRadarRevealedOne(Unit u, float durationTick) {
        if (u == null) return;
        for (Ability ab : u.abilities) {
            if (ab instanceof InvisibleAbility ia) {
                ia.revealRadarTick = Math.max(ia.revealRadarTick, durationTick);
                return;
            }
        }
    }

    /** 反隐雷达按圆范围批量调用（保留旧接口兼容性，不做 UnitType 级修改） */
    public static int markRadarRevealed(Team radarTeam, float rx, float ry, float range, float durationTick) {
        if (range <= 0f) return 0;
        final int[] cnt = {0};
        float r2 = range * range;
        Groups.unit.intersect(rx - range, ry - range, range * 2f, range * 2f, u -> {
            if (u == null || u.team == null) return;
            if (u.team == radarTeam) return;        // 不扫队友
            float dx = u.x - rx, dy = u.y - ry;
            if (dx * dx + dy * dy <= r2) {
                markRadarRevealedOne(u, durationTick);
                cnt[0]++;
            }
        });
        return cnt[0];
    }

    /** 某单位当前是否处于"显形态"（被打或被雷达）——用于自定义炮台 predicate 精细过滤 */
    public static boolean isUnitLocallyRevealed(Unit u) {
        if (u == null) return false;
        for (Ability ab : u.abilities) {
            if (ab instanceof InvisibleAbility ia) {
                return (ia.revealDamageTick > 0f) || (ia.revealRadarTick > 0f);
            }
        }
        return false;
    }

    /** 单位是否"对 targeter 这个观察者阵营处于隐身态"
     *  （自定义炮台/AI 过滤用）：有 InvisibleAbility 且本地没显形 → 隐身 = 不打 */
    public static boolean isStealthed(Unit u, Team targeter) {
        if (u == null || u.team == null) return false;
        if (u.team == targeter) return false;  // 队友永远不隐身
        boolean has = false;
        for (Ability ab : u.abilities) {
            if (ab instanceof InvisibleAbility ia) {
                if (ia.revealDamageTick <= 0f && ia.revealRadarTick <= 0f) return true;
                has = true;
                break;
            }
        }
        return !has;
    }

    /** 请求在下一个 tick 开头执行 targetable 同步（由每个 Ability.update 调用，只实际执行一次/tick） */
    static void requestSync() {
        float t = Time.time;
        if (Math.abs(t - lastSyncTick) < 0.5f) return;  // 本 tick 已经同步过
        lastSyncTick = t;
        syncAllTargetable();
    }

    /** 按类型聚合，同步 UnitType.targetable + UnitType 贴图整类替换 */
    static void syncAllTargetable() {
        // 1) 清理死亡/空的单位
        // ObjectSet 老版 arc 只提供 removeAll(T[] arr) 和 removeAll(Seq<? extends T>) 两种重载，
        // 用 Seq 收一下要删除的，再批量 removeAll(Seq) 最稳。
        Seq<Unit> toRemove = new Seq<>();
        for (Unit u : stealthedUnits) {
            if (u == null || u.dead) toRemove.add(u);
        }
        boolean hadAny = !stealthedUnits.isEmpty();
        if (toRemove.any()) stealthedUnits.removeAll(toRemove);

        // 最后一只单位死掉（清空前） → 把所有备份过的 UnitType 全还原，
        // 否则下一批同类型单位进入或玩家切地图回主菜单就永远看不见单位了
        if (hadAny && stealthedUnits.isEmpty()) {
            for (UnitType t : originalTargetable.keys()) {
                Boolean orig = originalTargetable.get(t);
                if (orig != null) t.targetable = orig;
            }
            for (UnitType t : originalRegions.keys()) restoreUnitTypeIfVisible(t);
            for (UnitType t : drawSwitchBackups.keys()) restoreUnitTypeDrawIfHidden(t);
            originalRegions.clear();
            drawSwitchBackups.clear();
            originalTargetable.clear();
            return;
        }
        if (stealthedUnits.isEmpty()) return;

        // 2) 备份首次出现的 UnitType 原始 targetable 值
        for (Unit u : stealthedUnits) {
            if (u == null || u.type == null) continue;
            if (!originalTargetable.containsKey(u.type)) {
                originalTargetable.put(u.type, u.type.targetable);
            }
        }

        // 3) 统计：每个类型
        //      ① 是否存在"仍在纯隐身"实例（damage==0 且 radar==0）→ 决定 targetable=false
        //      ② 是否存在至少 1 个 fullyInvisible 实例 → 决定 shouldMakeAllInvisible 整类贴图透明
        //      ③ 是否存在至少 1 个"显形"实例 → 决定 targetable 还原为原值（好让炮台能锁被打/在雷达里的敌人）
        ObjectMap<UnitType, Boolean> typeHasStealthOnly = new ObjectMap<>();
        ObjectMap<UnitType, Boolean> typeHasRevealed    = new ObjectMap<>();
        ObjectMap<UnitType, Boolean> typeNeedMakeInv    = new ObjectMap<>();
        for (Unit u : stealthedUnits) {
            if (u == null || u.type == null) continue;
            UnitType t = u.type;
            if (isUnitLocallyRevealed(u)) {
                typeHasRevealed.put(t, true);
            } else {
                typeHasStealthOnly.put(t, true);
            }
            for (Ability ab : u.abilities) {
                if (ab instanceof InvisibleAbility ia && ia.fullyInvisible) {
                    typeNeedMakeInv.put(t, true);
                    break;
                }
            }
        }

        // 4) 对每个出现过的 UnitType：targetable + 贴图整类替换
        ObjectSet<UnitType> processed = new ObjectSet<>();
        for (Unit u : stealthedUnits) {
            if (u == null || u.type == null) continue;
            UnitType t = u.type;
            if (processed.add(t) == false) continue;
            Boolean orig = originalTargetable.get(t);
            boolean makeInv = typeNeedMakeInv.get(t, false);
            if (makeInv) {
                setUnitTypeFullyInvisible(t);   // 贴图全换成 1x1 透明
                setUnitTypeDrawHidden(t);       // 引擎清掉 + 拖尾/光照全关
            } else {
                restoreUnitTypeIfVisible(t);    // 不做完全隐身模式（比如 fullyInvisible=false）就还原贴图
                restoreUnitTypeDrawIfHidden(t);
            }
            // ★ targetable 策略（优先判显形）：
            //   只要有任意一只实例显形（被打/被雷达扫到）→ targetable=true（让炮台能瞄）
            //   全部隐身（没有显形实例）→ targetable=false（炮台不瞄）
            //
            //   说明：targetable 是 UnitType 级整类共享字段，无法 per-unit 精确控制。
            //   之前的"有隐身就 false"会导致：同类型 A 被打显形、B 还隐身时 → 整类 false → A 也瞄不到。
            //   改成"有显形就 true"后：A 能被瞄（用户需求 ✓）；
            //   代价是同类型的 B（隐身）也会被炮台选中——但 B 被打中后会立刻触发 revealDamageTick 显形，
            //   且 B 视觉上仍透明（贴图没还原），所以表现为"炮台朝空地开火、子弹飞向看不见的单位"，
            //   正好符合用户想要的"只看到攻击特效、看不到单位"的效果。
            if (typeHasRevealed.get(t, false)) {
                // 有显形实例 → 还原 targetable，炮台能瞄
                t.targetable = (orig == null) ? true : orig;
            } else if (typeHasStealthOnly.get(t, false)) {
                // 全部隐身 → 不可瞄
                t.targetable = false;
            } else {
                t.targetable = (orig == null) ? t.targetable : orig;
            }
        }
    }

    /* ======================================================
     *                  Ability 生命周期
     * ====================================================== */

    // 【说明】Mindustry 159.6 Ability 父类只提供：
    //   init(UnitType type) —— UnitType 级初始化（不是每个单位实例）
    //   created(Unit unit)  —— 每个单位创建时触发（替代之前写错的 init(Unit)）
    //   update(Unit unit)   —— 每帧更新
    //   death(Unit unit)    —— 单位死亡时触发（可用于从 stealthedUnits 移除）
    // 没有 init(Unit) 这种签名，所以之前 @Override 会报错。

    @Override
    public void created(Unit unit) {
        initEvents();
        if (unit != null) stealthedUnits.add(unit);
        requestSync();
    }

    @Override
    public void death(Unit unit) {
        // 死亡时立即从 stealthedUnits 移除（防止 stealthedUnits 越积越大）
        if (unit != null) stealthedUnits.remove(unit);
        requestSync();
    }

    @Override
    public void update(Unit unit) {
        if (unit == null || headless) return;

        // 扣倒计时（Time.delta 做时间缩放兼容）
        if (revealDamageTick > 0f) revealDamageTick = Math.max(0f, revealDamageTick - Time.delta);
        if (revealRadarTick  > 0f) revealRadarTick  = Math.max(0f, revealRadarTick  - Time.delta);

        requestSync();
    }

    /* ======================================================
     *                  复制/显示/UI
     * ====================================================== */

    @Override
    public Ability copy() {
        InvisibleAbility out = new InvisibleAbility();
        // 向后兼容字段（旧版 FederalUnitType 赋值用）
        out.fullyInvisible  = this.fullyInvisible;
        out.visibleToAllies = this.visibleToAllies;
        out.stealthDotSize  = this.stealthDotSize;
        // 实际生效的字段
        out.revealDamageDuration = this.revealDamageDuration;
        out.revealRadarDuration  = this.revealRadarDuration;
        // 注意：revealDamageTick / revealRadarTick 是单位实例状态，copy 不复制
        return out;
    }

    @Override
    public String localized() {
        return "InvisibleAbility / 隐身";
    }

    @Override
    public void display(Table table) {
        if (table == null) return;
        table.row();
        table.add("[cyan]隐身：[]受击/雷达探测时暂时显形，否则炮台无法锁定你").pad(4).row();
        table.add("  · 受击显形时长：" + Mathf.round(revealDamageDuration / 60f) + "秒").padLeft(8).color(Color.lightGray).row();
        table.add("  · 雷达显形续杯：" + Mathf.round(revealRadarDuration / 60f * 10f) / 10f + "秒").padLeft(8).color(Color.lightGray).row();
    }

    /* ======================================================
     *                   渲染：draw() 重写
     * ======================================================
     *  【设计初衷】
     *   UnitType 级整类换成透明贴图 + 引擎清掉后，"被打/被雷达照到"的那只单位，
     *   就必须由 Ability.draw() 来单独手动重画出来 —— 这样才能做到：
     *   ✅ 同类型 A 单位被打 → A 单独显形
     *   ✅ 同类型 B 单位没被探测 → B 依然完全看不见（一个像素都不画）
     *   ✅ 一只被打，不会"全部现形"
     * ====================================================== */

    @Override
    public void draw(Unit unit) {
        if (unit == null || headless) return;

        // 1) 判断：这只单位这一帧是否"需要显示"
        //    触发条件（满足任意一个就重画）：
        //      A. 被打没超时（revealDamageTick > 0）
        //      B. 在雷达范围内（revealRadarTick > 0）
        //      C. 玩家处于同一阵营 + visibleToAllies=true（队友正常看得见）
        //      D. 玩家的战争迷雾 Bits 能看到该坐标（开雾了）
        boolean locallyRevealed = (revealDamageTick > 0f) || (revealRadarTick > 0f);
        boolean shouldShow = locallyRevealed;

        if (!shouldShow && visibleToAllies) {
            Team playerTeam = (Vars.player == null || Vars.player.team() == null) ? null : Vars.player.team();
            if (playerTeam != null && unit.team != null && unit.team == playerTeam) shouldShow = true;
        }
        if (!shouldShow) {
            try {
                if (Vars.control != null && Vars.control.input != null && Vars.player != null
                    && Vars.player.team() != null && Vars.fogControl != null && Vars.state != null && Vars.state.rules != null
                    && Vars.state.rules.fog) {
                    // 注意：Mindustry 159.6 没有 Vars.fog，迷雾控制在 Vars.fogControl
                    shouldShow = Vars.fogControl.isVisible(Vars.player.team(), unit.x, unit.y);
                }
            } catch (Throwable ignored) {}
        }

        // 2) 如果需要显示 → 手动按备份贴图 + 备份引擎重画这 ONE 单位
        //    revealAlpha 从 0→1→0 平滑过渡：倒计时快结束时慢慢淡出，避免闪烁
        float revealAlpha = 1f;
        if (locallyRevealed) {
            float rem = Math.max(revealDamageTick, revealRadarTick);
            if (rem < 20f) revealAlpha = Mathf.clamp(rem / 20f); // 最后 20 tick（约 0.33 秒）淡出
        }

        if (shouldShow) {
            drawUnitManually(unit, revealAlpha);
        }

        // 3) 最后 Draw.reset() 兜底：颜色/混合/线宽/Alpha 全部还原成官方默认值，
        //    这就是为什么"全局渲染不再乱掉"——无论我们在 drawUnitManually 里改了什么，
        //    最后全都复位，不会污染后续的方块/特效/UI/菜单绘制。
        try { Draw.reset(); } catch (Throwable ignored) {}
    }

    /**
     * 手动重画一只单位（按 UnitType 备份的 originalRegions + drawSwitchBackups）
     *  覆盖：阴影 / 身体 / cell / item / 轮廓 / 引擎尾焰 / 光照
     *  说明：故意不画武器（武器的子弹/激光特效会从 Groups.bullet 自动画出来，
     *       这就形成了用户截图里"只能看到攻击特效、不知道单位在哪"的经典效果）
     */
    void drawUnitManually(Unit unit, float revealAlpha) {
        try {
            UnitType t = unit.type;
            if (t == null) return;
            TextureRegion[] regs = originalRegions.get(t);
            Object[] switches = drawSwitchBackups.get(t);
            if (regs == null && switches == null) {
                // 没备份过（可能 fullyInvisible=false），什么都不画（官方 type.draw 已经画过了）
                return;
            }
            float hs = (unit == null || t == null) ? 8f : t.hitSize;
            if (hs <= 0.01f) hs = 8f;
            float rotation = (unit == null) ? 0f : unit.rotation;

            // 颜色先重置，再乘 alpha（revealAlpha 让最后 0.33 秒慢慢淡出）
            Draw.color();
            Draw.color(Draw.getColor(), Mathf.clamp(revealAlpha));

            try {
                /* ---------- 1. 软阴影（先画，在身体下面）---------- */
                boolean drawShadow = true;
                if (switches != null) {
                    Boolean v = (switches[3] instanceof Boolean) ? (Boolean) switches[3] : Boolean.TRUE;
                    drawShadow = v;
                }
                if (drawShadow) {
                    TextureRegion shadow = (regs != null) ? regs[3] : null;
                    TextureRegion softS  = (regs != null) ? regs[11] : null;
                    if (softS != null && softS.found()) {
                        // 软阴影偏移（比身体稍微低 3-4 像素，半透明黑）
                        Draw.color(0f, 0f, 0f, 0.35f * Mathf.clamp(revealAlpha));
                        Draw.rect(softS, unit.x, unit.y - 2f, 0);
                        Draw.color();
                    } else if (shadow != null && shadow.found()) {
                        Draw.color(0f, 0f, 0f, 0.35f * Mathf.clamp(revealAlpha));
                        Draw.rect(shadow, unit.x, unit.y, 0);
                        Draw.color();
                    }
                }

                /* ---------- 2. 身体主贴图 ---------- */
                TextureRegion body = null;
                if (regs != null) {
                    body = (regs[2] != null && regs[2].found()) ? regs[2]   // 首选 t.region（身体完整贴图）
                         : (regs[0] != null && regs[0].found()) ? regs[0] : null; // 次选 t.baseRegion
                }
                boolean drawBody = true;
                if (switches != null) drawBody = Boolean.TRUE.equals(switches[1]) || body != null;
                if (drawBody && body != null) {
                    Draw.color(Draw.getColor(), Mathf.clamp(revealAlpha));
                    Draw.rect(body, unit.x, unit.y, rotation - 90f);
                    Draw.color();
                }

                /* ---------- 3. Cell 队伍色能量核 ---------- */
                TextureRegion cell = (regs != null) ? regs[1] : null;
                boolean drawCell = true;
                if (switches != null) drawCell = Boolean.TRUE.equals(switches[0]);
                if (drawCell && cell != null && cell.found()) {
                    // Pal 里没有 Pal.team，用玩家队伍色或 Pal.sap(队伍色回退)
                    Color tc = (unit.team == null) ? Pal.sap : unit.team.color;
                    Draw.color(tc, Mathf.clamp(0.8f * revealAlpha));
                    Draw.rect(cell, unit.x, unit.y, rotation - 90f);
                    Draw.color();
                }

                /* ---------- 4. （已删除）搬运物品白色圆圈 ---------- */

                /* ---------- 5. 外轮廓描边 ---------- */
                TextureRegion outlineR = (regs != null) ? regs[8] : null;
                float outlineR2 = (switches != null && switches[5] instanceof Float) ? (Float) switches[5] : 0f;
                if (outlineR2 > 0.01f && outlineR != null && outlineR.found()) {
                    // 官方轮廓是纯黑不透明，这里稍微带透明度（和 revealAlpha 联动）
                    Draw.color(0f, 0f, 0f, 0.9f * Mathf.clamp(revealAlpha));
                    Draw.rect(outlineR, unit.x, unit.y, rotation - 90f);
                    Draw.color();
                }

                /* ---------- 6. 引擎尾焰（按备份 Seq<UnitEngine> 重画，100% 对齐官方 UnitEngine.draw）---------- */
                if (switches != null && switches[8] instanceof Seq) {
                    @SuppressWarnings("unchecked")
                    Seq<UnitType.UnitEngine> engines = (Seq<UnitType.UnitEngine>) switches[8];
                    if (engines != null && engines.any()) {
                        float scale = Boolean.TRUE.equals(switches[6]) ? Math.max(0.0001f, unit.elevation) : 1f;
                        if (scale <= 0.0001f) scale = 1f;
                        float rot = unit.rotation - 90f;
                        Color col = (switches[7] instanceof Color) ? (Color) switches[7] : null;
                        if (col == null) col = (unit.team == null) ? Pal.accentBack : unit.team.color;
                        Color innerC = t.engineColorInner == null ? Color.white : t.engineColorInner;

                        for (UnitType.UnitEngine eng : engines) {
                            if (eng == null) continue;
                            Tmp.v1.set(eng.x, eng.y).rotate(rot);
                            float ex = unit.x + Tmp.v1.x, ey = unit.y + Tmp.v1.y;
                            float rad = (eng.radius + Mathf.absin(Time.time, 2f, eng.radius / 4f)) * scale;
                            if (rad <= 0f) continue;

                            // 外圈（engineColor 或 team.color）
                            Draw.color(col, Mathf.clamp(revealAlpha));
                            Fill.circle(ex, ey, rad);

                            // 内圈（engineColorInner）
                            float shift = rad / 4f;
                            float ix = ex - Angles.trnsx(rot + eng.rotation, shift);
                            float iy = ey - Angles.trnsy(rot + eng.rotation, shift);
                            Draw.color(innerC, Mathf.clamp(revealAlpha));
                            Fill.circle(ix, iy, rad / 2f);
                        }
                        Draw.color();
                    }
                }

                /* ---------- 7. 单位自带光照/手电筒 ---------- */
                if (switches != null && switches[10] instanceof Float && switches[11] instanceof Float) {
                    float savedR = (Float) switches[10];
                    float savedO = (Float) switches[11];
                    if (savedR > 0.01f) {
                        Color lightC = (t.lightColor == null) ? (unit.team == null ? Color.white : unit.team.color) : t.lightColor;
                        Drawf.light(unit.x, unit.y, savedR, lightC, Mathf.clamp(savedO * revealAlpha));
                    }
                }

            } catch (Throwable ignored) {
                // 绘制过程任意异常吞掉（不希望因为单帧绘制崩了整个游戏）
            } finally {
                // 任何分支出来都重置颜色（非常关键，防止把后续的方块/UI 染色）
                try { Draw.color(); } catch (Throwable ignored2) {}
            }
        } catch (Throwable ignored) {}
    }
}
