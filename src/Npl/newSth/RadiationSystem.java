package Npl.newSth;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.Blending;
import arc.graphics.gl.FrameBuffer;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.struct.IntMap;
import arc.struct.IntSeq;
import arc.struct.IntSet;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.content.Fx;
import mindustry.game.EventType.*;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.MendProjector;
import mindustry.world.blocks.defense.RegenProjector;
import mindustry.world.blocks.defense.Wall;
import Npl.content.NuBlocks;
import Npl.content.NuStatus;
import Npl.content.envBlocks;
import Npl.newSth.walls.NuHealerProjector;
import Npl.nublock.ZoneIsolator;
import Npl.nublock.InfectionZoneIsolator;
import Npl.nublock.SymbiosisZoneIsolator;

import static mindustry.Vars.*;

/**
 * 区域系统（明日方舟元素损伤式），同时管理三种区域：
 *
 * 区域：
 * ─ 辐射区（绿）：crystalCune / huiye / uranCrystalWall / thalliumOre / uranCrystalCrusher
 * ─ 感染区（紫）：infectionZone
 * ─ 共生区（红）：symbiosisZone
 *   每个区域的源、累积值、隔离器、进度环颜色相互独立，互不干扰。
 *
 * 机制总览（三大区域共用同一套机制，仅视觉与源不同）：
 * ─ 累积：区域内每 {@link #cycleInterval} 秒 +{@link #gainPerCycle}，上限 {@link #radiationMax}
 * ─ 爆表：攒满瞬间受到最大生命 × {@link #burstPercent} 的无视护甲伤害，且最大生命永久 ×(1-{@link #maxHealthLoss})（仅单位），
 *   随后进度条以 {@link #drainPerSecond}/秒 倒转清空（约 5 秒），倒转期间不再累积
 * ─ 中毒：爆表后单位获得 radPoison 状态，持续 {@link #poisonDuration} 秒，
 *   每秒流失 {@link #poisonDpsPercent}×100% 最大生命；建筑爆表后进入中毒状态持续掉血
 * ─ 治疗压制：处于任意区域内的治疗类建筑（NuHealerProjector / MendProjector / RegenProjector）失效
 * ─ 隔离：{@link ZoneIsolator} 子类（radiationShield / infectionShield / symbiosisShield）
 *   范围内的单位与建筑免疫对应区域的一切效果（不累积、暂停中毒伤害、治疗不受压制）
 * ─ 存留：离开区域后数值不衰减，一直留在体内，再进区继续累积
 * ─ 判定对象：所有队伍的单位 + 建筑（墙壁类除外），敌我一视同仁
 * ─ 视觉：环绕实体正中央的能量环（{@link #ringLayer} 层），弧长 = 数值/上限，绿→红渐变 + 加法发光；
 *   玩家处于某区域时整屏泛该区颜色，滞留越久（累积越高）颜色越深（{@link RadiationShader}）
 */
public class RadiationSystem {

    // ==================== 参数区 ====================

    /** 数值上限（进度条转满一圈） */
    public static final float radiationMax = 500f;
    /** 每个累积周期增加的数值 */
    public static final float gainPerCycle = 5f;
    /** 累积周期（秒） */
    public static final float cycleInterval = 2f;
    /** 爆表伤害占最大生命的百分比（无视护甲） */
    public static final float burstPercent = 0.75f;
    /** 每次爆表永久削减单位最大生命的比例（乘法叠加：0.1 = 每次爆表 ×0.9） */
    public static final float maxHealthLoss = 0.1f;
    /** 爆表后中毒持续时间（秒） */
    public static final float poisonDuration = 5f;
    /** 中毒期间单位每秒流失的最大生命百分比 */
    public static final float poisonDpsPercent = 0.03f;
    /** 建筑中毒期间每秒流失的最大生命百分比（爆表后持续掉血）。 */
    public static final float buildPoisonDpsPercent = 0.04f;
    /** 爆表后的倒转速度（数值/秒），500 / 100 = 5 秒清空 */
    public static final float drainPerSecond = 100f;
    /** 源判定半径（格） */
    public static final float detectRadius = 3f;

    /** 刚进入区域时的整屏泛色强度 */
    public static final float shaderMinIntensity = 0.12f;
    /** 滞留越久泛色越深：接近爆表时的整屏泛色强度 */
    public static final float shaderMaxIntensity = 0.55f;

    /** 进度环绘制层级：居中环绕实体，绘制在单位/建筑之上（原为 overlayUI，现调整到 shields 层）。 */
    public static final float ringLayer = Layer.shields;

    // ==================== 区域定义 ====================

    /** 一个区域：拥有独立的源集合、隔离器类型、累积数据与配色。 */
    public static class Zone {
        /** 区域名（用于日志） */
        public final String name;
        /** 进度环基色 */
        public final Color ringColor;
        /** 整屏泛色 */
        public final Color tint;
        /** 该区域的源方块（地板或任意方块），可通过 sources.add() 外部追加 */
        public final ObjectSet<Block> sources = new ObjectSet<>();
        /** 该区域的隔离器方块类型（{@link ZoneIsolator} 子类） */
        public Class<? extends ZoneIsolator> isolatorType;

        /** 单位数值（按实体 id） */
        final IntMap<Float> unitRads = new IntMap<>();
        /** 建筑数值（按实体 id） */
        final IntMap<Float> buildRads = new IntMap<>();
        /** 正在倒转的单位 id */
        final IntSet drainingUnits = new IntSet();
        /** 正在倒转的建筑 id */
        final IntSet drainingBuilds = new IntSet();
        /** 调试日志：每张地图每个实体只记一次 */
        final IntSet loggedUnits = new IntSet();
        final IntSet loggedBuilds = new IntSet();
        /** 该区域场上的隔离器（每周期刷新） */
        final Seq<Building> isolators = new Seq<>();

        public Zone(String name, String ringHex, String tintHex){
            this.name = name;
            this.ringColor = Color.valueOf(ringHex);
            this.tint = Color.valueOf(tintHex);
        }

        void clear(){
            unitRads.clear();
            buildRads.clear();
            drainingUnits.clear();
            drainingBuilds.clear();
            loggedUnits.clear();
            loggedBuilds.clear();
            isolators.clear();
        }

        float unitRad(int id){
            Float v = unitRads.get(id);
            return v == null ? 0f : v;
        }

        float buildRad(int id){
            Float v = buildRads.get(id);
            return v == null ? 0f : v;
        }

        /** 是否处于本区隔离器的免疫范围内。 */
        boolean shielded(float x, float y){
            for(int i = 0; i < isolators.size; i++){
                Building s = isolators.get(i);
                float r = ((ZoneIsolator)s.block).range;
                if(Mathf.dst2(x, y, s.x, s.y) <= r * r) return true;
            }
            return false;
        }
    }

    /** 辐射区（绿） */
    public static final Zone radiation = new Zone("radiation", "7CFF6D", "8FDE5D");
    /** 感染区（紫） */
    public static final Zone infection = new Zone("infection", "C662FF", "C662FF");
    /** 共生区（红） */
    public static final Zone symbiosis = new Zone("symbiosis", "FF406D", "FF406D");

    /** 全部区域 */
    public static final Seq<Zone> zones = Seq.with(radiation, infection, symbiosis);

    /** 兼容旧引用：辐射区圆环基色 */
    public static final Color ringColor = radiation.ringColor;

    /** 判断任意方块是否属于某个区的辐射源。 */
    public static boolean isRadiationTile(Tile t){
        if(t == null) return false;
        Block floor = t.floor();
        Block block = t.block();
        Block overlay = t.overlay();
        for(Zone z : zones){
            if(z.sources.contains(floor) || z.sources.contains(block) || z.sources.contains(overlay)){
                return true;
            }
        }
        return false;
    }

    // ==================== 数据存储 ====================

    /** 处于中毒状态的建筑 id（爆表后持续掉血，跨区共用）。 */
    private static final IntSet poisonedBuilds = new IntSet();
    /** 单位中毒开始时间（id → 经过时间秒），用于轮廓变色。 */
    private static final IntMap<Float> poisonStartTimes = new IntMap<>();

    /** 全屏后处理帧缓冲：整帧渲染进这里，再用 RadiationShader 贴回屏幕。 */
    private static FrameBuffer radiationBuffer;
    /** 本帧是否已开始全屏捕获（preDraw 决定，postDraw 复用，避免前后不一致）。 */
    private static boolean radiationActive = false;
    /** 本地玩家当前所处区域（preDraw 计算，postDraw/着色器复用）。 */
    private static Zone currentZone = null;

    private static float cycleTimer = 0f;
    private static boolean inited = false;

    // ==================== 初始化 ====================

    /** 在 ClientLoad 时调用一次。 */
    public static void init(){
        if(inited) return;
        inited = true;

        radiationBuffer = new FrameBuffer();

        radiation.sources.addAll(envBlocks.crystalCune, envBlocks.huiye, envBlocks.uranCrystalWall, envBlocks.thalliumOre, NuBlocks.uranCrystalCrusher);
        infection.sources.addAll(envBlocks.infectionZone);
        symbiosis.sources.addAll(envBlocks.symbiosisZone);

        radiation.isolatorType = RadiationShieldBlock.class;
        infection.isolatorType = InfectionZoneIsolator.class;
        symbiosis.isolatorType = SymbiosisZoneIsolator.class;

        Log.info("[RadiationSystem] 初始化完成：辐射区 @ 源 / 感染区 @ 源 / 共生区 @ 源",
                radiation.sources.size, infection.sources.size, symbiosis.sources.size);

        // 换图清空所有区域数据
        Events.on(WorldLoadEvent.class, e -> clear());

        // 逻辑 tick：倒转（每帧）+ 累积周期（每 2 秒）+ 残留清理
        Events.run(Trigger.update, RadiationSystem::update);

        // 渲染：环绕实体正中央绘制进度环
        Events.run(Trigger.draw, () -> Draw.draw(ringLayer, RadiationSystem::drawRings));

        // 全屏扭曲：玩家单位处于某区域时，画面整体泛该区颜色 + 扰动。
        // 正确做法是"整帧后处理"：preDraw 把整帧渲染进自建 FrameBuffer，
        // 帧末 postDraw 再用 RadiationShader 贴回屏幕。
        Events.run(Trigger.preDraw, () -> {
            currentZone = playerZone();
            radiationActive = currentZone != null;
            if(radiationActive){
                Unit pu = player.unit();
                float p = pu == null ? 0f : Mathf.clamp(currentZone.unitRad(pu.id) / radiationMax, 0f, 1f);
                RadiationShader sh = RadiationShader.instance;
                sh.tint.set(currentZone.tint);
                // 滞留越久（累积越高），泛色越深
                sh.intensity = Mathf.lerp(shaderMinIntensity, shaderMaxIntensity, p);
                radiationBuffer.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
                radiationBuffer.begin(Color.clear);
            }
        });

        Events.run(Trigger.postDraw, () -> {
            if(radiationActive){
                radiationBuffer.end();
                Draw.blend(Blending.disabled);
                radiationBuffer.blit(RadiationShader.instance);
                Draw.blend();
            }
        });
    }

    /** 清空全部区域数据（换图时）。 */
    public static void clear(){
        for(Zone z : zones) z.clear();
        poisonedBuilds.clear();
        poisonStartTimes.clear();
        cycleTimer = 0f;
    }

    // ==================== 逻辑 ====================

    private static void update(){
        // 爆表后的倒转（每帧，各区独立）
        float drain = drainPerSecond * Time.delta / 60f;
        for(Zone z : zones){
            drainAll(z.unitRads, z.drainingUnits, drain);
            drainAll(z.buildRads, z.drainingBuilds, drain);
        }

        // 中毒：每秒流失 % 最大生命（状态由爆表施加，提供图标与发光标记；任一区域被隔离即暂停）
        for(Unit u : Groups.unit){
            if(u.hasEffect(NuStatus.radPoison) && !shieldedAnywhere(u.x, u.y)){
                u.damage(u.maxHealth * poisonDpsPercent * Time.delta / 60f);
            }
        }

        // 建筑中毒：爆表后进入中毒状态，持续掉血（被隔离则暂停）
        updateBuildPoison();

        // 累积周期
        cycleTimer += Time.delta / 60f;
        if(cycleTimer >= cycleInterval){
            cycleTimer -= cycleInterval;
            accumulate();
        }
    }

    /** 倒转：draining 集合中的实体持续扣减，归零后退出倒转状态。 */
    private static void drainAll(IntMap<Float> rads, IntSet draining, float drain){
        if(draining.isEmpty()) return;

        var it = draining.iterator();
        while(it.hasNext){
            int id = it.next();
            Float boxed = rads.get(id);
            float v = (boxed == null ? radiationMax : boxed) - drain;
            if(v <= 0f){
                it.remove();
                rads.remove(id);
            }else{
                rads.put(id, v);
            }
        }
    }

    /** 一个累积周期：各区域内实体 +gain，攒满则爆表。 */
    private static void accumulate(){
        // 刷新各区域隔离器列表
        for(Zone z : zones) refreshIsolators(z);

        for(Unit u : Groups.unit){
            if(u.dead) continue;
            for(Zone z : zones){
                if(z.drainingUnits.contains(u.id) || z.shielded(u.x, u.y)) continue;
                if(!nearSource(z, u.x, u.y, 0f)) continue;

                float before = z.unitRad(u.id);
                if(before == 0f && z.loggedUnits.add(u.id)){
                    Log.info("[RadiationSystem] 单位 @ 进入 @ 区，开始累积 (@, @)", u.type.name, z.name, Mathf.round(u.x / 8f), Mathf.round(u.y / 8f));
                }
                float v = before + gainPerCycle;
                if(v >= radiationMax){
                    burstUnit(z, u);
                }else{
                    z.unitRads.put(u.id, v);
                }
            }
        }

        for(Building b : Groups.build){
            if(b.block instanceof Wall) continue;
            for(Zone z : zones){
                if(z.drainingBuilds.contains(b.id) || z.shielded(b.x, b.y)) continue;

                // 大建筑按 中心 + 半边长 判定，保证边缘贴着源也能命中
                if(!nearSource(z, b.x, b.y, b.block.size / 2f)) continue;

                float before = z.buildRad(b.id);
                if(before == 0f && z.loggedBuilds.add(b.id)){
                    Log.info("[RadiationSystem] 建筑 @ 进入 @ 区，开始累积 (@, @)", b.block.name, z.name, Mathf.round(b.x / 8f), Mathf.round(b.y / 8f));
                }
                float v = before + gainPerCycle;
                if(v >= radiationMax){
                    burstBuild(z, b);
                }else{
                    z.buildRads.put(b.id, v);
                }
            }
        }

        // 任意区域内的治疗类建筑失效（被该区隔离器保护则除外）
        for(Building b : Groups.build){
            if(!(b.block instanceof NuHealerProjector || b.block instanceof MendProjector || b.block instanceof RegenProjector)) continue;
            boolean suppressed = false;
            for(Zone z : zones){
                if(!z.shielded(b.x, b.y) && nearSource(z, b.x, b.y, b.block.size / 2f)){
                    suppressed = true;
                    break;
                }
            }
            b.enabled = !suppressed;
        }

        cleanup();
    }

    /** 刷新某区域的隔离器列表（每周期一次）。 */
    private static void refreshIsolators(Zone z){
        z.isolators.clear();
        if(z.isolatorType == null) return;
        for(Building b : Groups.build){
            if(z.isolatorType.isInstance(b.block)) z.isolators.add(b);
        }
    }

    /** 是否处于任意区域隔离器的免疫范围内。 */
    private static boolean shieldedAnywhere(float x, float y){
        for(Zone z : zones){
            if(z.shielded(x, y)) return true;
        }
        return false;
    }

    /** 本地玩家当前控制的单位所处的区域（多区重叠时取累积值最高者），无则返回 null。 */
    private static Zone playerZone(){
        Unit u = player.unit();
        if(u == null || u.dead) return null;

        Zone best = null;
        float bestV = 0f;
        for(Zone z : zones){
            if(!nearSource(z, u.x, u.y, 0f)) continue;
            float v = z.unitRad(u.id);
            if(best == null || v > bestV){
                best = z;
                bestV = v;
            }
        }
        return best;
    }

    /** 爆表：高额无视护甲伤害 + 永久削减最大生命 + 中毒状态 + 冲击波特效 + 进入倒转状态。 */
    private static void burstUnit(Zone z, Unit u){
        z.unitRads.put(u.id, radiationMax);
        z.drainingUnits.add(u.id);
        poisonStartTimes.put(u.id, Time.time);
        u.damagePierce(u.maxHealth * burstPercent);
        // 每次爆表永久削减 10% 最大生命（乘法叠加），当前生命随之钳制
        u.maxHealth *= 1f - maxHealthLoss;
        u.health = Math.min(u.health, u.maxHealth);
        u.apply(NuStatus.radPoison, poisonDuration * 60f);
        Fx.shockwave.at(u.x, u.y, z.ringColor);
    }

    private static void burstBuild(Zone z, Building b){
        z.buildRads.put(b.id, radiationMax);
        z.drainingBuilds.add(b.id);
        poisonedBuilds.add(b.id);
        b.damage(b.maxHealth * burstPercent);
        Fx.shockwave.at(b.x, b.y, z.ringColor);
    }

    /** 建筑中毒：爆表后进入中毒状态，按 buildPoisonDpsPercent 持续掉血。 */
    private static void updateBuildPoison(){
        if(poisonedBuilds.isEmpty()) return;
        float dps = buildPoisonDpsPercent * Time.delta / 60f;
        for(Building b : Groups.build){
            if(!poisonedBuilds.contains(b.id)) continue;
            if(b.block instanceof Wall || b.dead){
                poisonedBuilds.remove(b.id);
                continue;
            }
            if(shieldedAnywhere(b.x, b.y)) continue;
            b.damage(b.maxHealth * dps);
            if(b.health <= 0f){
                poisonedBuilds.remove(b.id);
            }
        }
    }

    /** 清理已死亡/被拆除实体的残留数据（每周期一次）。 */
    private static void cleanup(){
        IntSet live = new IntSet();
        IntSeq remove = new IntSeq();

        for(Unit u : Groups.unit) live.add(u.id);
        for(Zone z : zones){
            remove.clear();
            IntSeq keys = z.unitRads.keys().toSeq();
            for(int i = 0; i < keys.size; i++){
                if(!live.contains(keys.get(i))) remove.add(keys.get(i));
            }
            for(int i = 0; i < remove.size; i++){
                z.unitRads.remove(remove.get(i));
                z.drainingUnits.remove(remove.get(i));
            }
        }
        remove.clear();
        IntSeq pkeys = poisonStartTimes.keys().toSeq();
        for(int i = 0; i < pkeys.size; i++){
            if(!live.contains(pkeys.get(i))) remove.add(pkeys.get(i));
        }
        for(int i = 0; i < remove.size; i++){
            poisonStartTimes.remove(remove.get(i));
        }

        live.clear();
        for(Building b : Groups.build) live.add(b.id);
        for(Zone z : zones){
            remove.clear();
            IntSeq keys = z.buildRads.keys().toSeq();
            for(int i = 0; i < keys.size; i++){
                if(!live.contains(keys.get(i))) remove.add(keys.get(i));
            }
            for(int i = 0; i < remove.size; i++){
                z.buildRads.remove(remove.get(i));
                z.drainingBuilds.remove(remove.get(i));
            }
        }
    }

    /** 某区域中 (x, y) 周围 radius 格内（圆形，含自身格）是否存在源。 */
    private static boolean nearSource(Zone z, float x, float y, float radius){
        if(z.sources.isEmpty()) return false;

        Tile center = world.tileWorld(x, y);
        if(center == null) return false;

        float range = detectRadius + radius;
        int r = Mathf.ceil(range);
        float rangeSq = range * range;

        for(int dx = -r; dx <= r; dx++){
            for(int dy = -r; dy <= r; dy++){
                if(dx * dx + dy * dy > rangeSq) continue;
                Tile t = world.tile(center.x + dx, center.y + dy);
                if(t != null && (z.sources.contains(t.floor()) || z.sources.contains(t.block()) || z.sources.contains(t.overlay()))){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 中毒轮廓颜色：随时间在 团队色 → 绿 → 青蓝 → 紫 → 团队色 之间渐变。
     * @param elapsed 中毒已持续时间（秒）
     */
    static Color poisonOutlineColor(Unit unit, float elapsed){
        float dur = poisonDuration;
        float p = Mathf.clamp(elapsed / dur, 0f, 1f);
        Color team = unit.team.color;

        Color a, b;
        float local;
        if(p < 0.25f){
            a = team; b = Color.green;
            local = p / 0.25f;
        }else if(p < 0.5f){
            a = Color.green; b = Color.cyan;
            local = (p - 0.25f) / 0.25f;
        }else if(p < 0.75f){
            a = Color.cyan; b = Color.purple;
            local = (p - 0.5f) / 0.25f;
        }else{
            a = Color.purple; b = team;
            local = (p - 0.75f) / 0.25f;
        }

        return Tmp.c1.set(a).lerp(b, local);
    }

    // ==================== 渲染 ====================

    private static void drawRings(){
        // 中毒单位：变色外环（团队色 → 绿 → 青蓝 → 紫 → 团队色）
        for(Unit u : Groups.unit){
            Float start = poisonStartTimes.get(u.id);
            if(start == null) continue;
            float elapsed = Time.time - start;
            if(elapsed > poisonDuration) continue;

            Color c = poisonOutlineColor(u, elapsed);
            float r = u.hitSize / 2f + 6f;
            Draw.color(c);
            Lines.stroke(2.5f);
            Lines.circle(u.x, u.y, r);
            Draw.reset();
        }

        // 中毒建筑：整体泛辐射绿光（爆表进入中毒后才发光）
        for(Building b : Groups.build){
            if(poisonedBuilds.contains(b.id)){
                float alpha = 0.3f + Mathf.absin(Time.time, 6f, 0.15f);
                Draw.blend(Blending.additive);
                Draw.color(ringColor, alpha);
                Draw.rect(b.block.region, b.x, b.y);
                Draw.blend();
                Draw.reset();
            }
        }

        // 各区进度环：居中环绕实体
        for(Zone z : zones){
            for(Unit u : Groups.unit){
                float v = z.unitRad(u.id);
                if(v > 0f){
                    drawRing(u.x, u.y, u.hitSize / 2f + 5f, v, z.ringColor);
                }
            }

            for(Building b : Groups.build){
                if(b.block instanceof Wall) continue;
                float v = z.buildRad(b.id);
                if(v > 0f){
                    drawRing(b.x, b.y, b.block.size * 4f + 5f, v, z.ringColor);
                }
            }
        }
    }

    /** 环绕实体正中央的能量环：底环 + 发光进度弧（基色→红），高数值时脉冲警告。 */
    private static void drawRing(float x, float y, float radius, float value, Color color){
        float fract = Mathf.clamp(value / radiationMax);

        // 比例超过 60% 开始脉冲，越接近爆表闪得越快
        float pulse = fract > 0.6f ? 0.75f + Mathf.absin(Time.time, 5f + (1f - fract) * 12f, 0.25f) : 1f;

        // 底环
        Draw.color(color, 0.35f * pulse);
        Lines.stroke(2f);
        Lines.circle(x, y, radius);

        // 进度弧：加法混合发光
        Draw.blend(Blending.additive);
        Draw.color(Tmp.c1.set(color).lerp(Color.scarlet, fract), 0.95f * pulse);
        Lines.stroke(3.5f);
        Lines.arc(x, y, radius, fract);
        Draw.blend();
        Draw.reset();
    }
}
