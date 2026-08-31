package Npl.newSth;

import arc.Events;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.util.Log;
import arc.util.Time;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.world.blocks.storage.CoreBlock;
import Npl.content.Azer;
import Npl.Rouge.RougeSave;

import static mindustry.Vars.*;

/**
 * 昼夜系统 —— 基于波次推进的「只黑不亮」绝望氛围
 * ==============================================================
 * 需求：区块刚开始（早上）画面毫无变化；随波次推进越来越黑，最终「极黑」——
 *      除会自发光的建筑 / 单位（{@code lightRadius} 光源）外，其余全部看不见，营造绝望感。
 *
 * 实现思路：直接复用 Mindustry 原生光照，不新增任何光源。
 *   ① 进图（{@link WorldLoadEvent}）时把 {@link mindustry.game.Rules#lighting} 置 true；
 *   ② 每帧把 {@link mindustry.game.Rules#ambientLight} 的 alpha 按「已推进波次」
 *      从 {@link #dayAmbient}（0，光照直接关闭 = 画面无变化）单调插值到
 *      {@link #nightAmbient}（1，除光源外极黑）；
 *   ③ 只黑不亮：区块内 alpha 只增不减，绝不会自己变回白天；
 *   ④ 占领区块后（战役 {@link SectorCaptureEvent}、Rouge 进攻图敌人核心全灭）
 *      迅速把 alpha 拉回 0，约 {@link #restoreDuration} 秒恢复白天。
 *
 * 生效范围：Azer 星球战役 + Rouge 模式（{@code RougeSave.currentSectorName != null}）。
 *
 * 注：原生光照会读取场上每个实体 / 特效的 lightRadius 作为光源，因此
 *     「自发光建筑与单位可见」这一条无需额外处理，自动生效。
 */
public class DayNightSystem {

    // ==================== 参数区 ====================

    /** 从区块起始波次算起，再推进多少波到达「极黑」（进度 1.0）。 */
    public static float fullDarkWaves = 18f;
    /** 白天的环境光 alpha（0 = 原生光照关闭，画面与原版完全一致）。 */
    public static final float dayAmbient = 0f;
    /** 极黑的环境光 alpha（1 = 除自发光的建筑 / 单位外全黑）。 */
    public static final float nightAmbient = 1f;
    /** 夜晚环境光底色（近黑，略带冷蓝，避免纯黑显得死板）。 */
    public static final Color nightColor = new Color(0.015f, 0.02f, 0.045f);
    /** 变黑过渡速度（每秒 alpha 变化量），波次跳变时平滑跟随。 */
    public static final float darkenRate = 0.12f;
    /** 占领区块后恢复白天的时长（秒）——「迅速恢复」。 */
    public static final float restoreDuration = 4f;
    /** Rouge 进攻图敌人核心检测间隔（秒）。 */
    public static final float coreCheckInterval = 0.5f;

    // ==================== 运行状态 ====================

    private static boolean inited = false;
    /** 当前地图是否处于本系统生效范围。 */
    private static boolean active = false;
    /** 是否已进入「占领后恢复白天」阶段。 */
    private static boolean restoring = false;
    /** 当前实际写入规则的环境光 alpha。 */
    private static float ambientAlpha = dayAmbient;
    /** 本区块的起始波次（进图时记录，作为进度基准，保证每个区块都从白天开始）。 */
    private static int baseWave = 1;
    /** 已到达的最大黑暗进度（只增不减，保证「只黑不亮」）。 */
    private static float maxProgress = 0f;
    /** Rouge 进攻图：进图时是否存在敌人核心。 */
    private static boolean rougeAttackMap = false;
    /** 敌人核心检测计时器。 */
    private static float coreCheckTimer = 0f;

    // ==================== 初始化 ====================

    /** 在 ClientLoad 时调用一次。 */
    public static void init(){
        if(inited) return;
        inited = true;

        // 换图 / 开局：判定是否启用，并重置为白天
        Events.on(WorldLoadEvent.class, e -> onWorldLoad());

        // 每帧：按波次推进插值环境光
        Events.run(Trigger.update, DayNightSystem::update);

        // 战役占领区块：迅速恢复白天
        Events.on(SectorCaptureEvent.class, e -> {
            if(active) startRestore();
        });

        Log.info("[DayNight] 昼夜系统初始化完成");
    }

    private static void onWorldLoad(){
        active = shouldApply();
        restoring = false;
        ambientAlpha = dayAmbient;
        baseWave = state.wave;
        maxProgress = 0f;
        coreCheckTimer = 0f;
        rougeAttackMap = false;

        if(!active) return;

        state.rules.lighting = true;
        applyAmbient();

        // Rouge 进攻图判定：一开始就有敌人核心 = 需要占领
        if(RougeSave.currentSectorName != null){
            for(Building b : Groups.build){
                if(b.block instanceof CoreBlock && b.team != player.team() && b.team != Team.derelict){
                    rougeAttackMap = true;
                    break;
                }
            }
        }

        Log.info("[DayNight] 启用昼夜：区块起始波次 @，再推进 @ 波到极黑（Rouge 进攻图=@）",
                baseWave, Mathf.round(fullDarkWaves), rougeAttackMap);
    }

    /** 是否需要启用：Azer 星球战役 或 Rouge 模式。 */
    private static boolean shouldApply(){
        if(RougeSave.currentSectorName != null) return true;
        return state.isCampaign() && state.getPlanet() == Azer.Azer;
    }

    // ==================== 逻辑 ====================

    private static void update(){
        if(!active) return;
        // 回到主菜单时不做任何事（active 会在下次 WorldLoadEvent 重算）
        if(!state.isGame()) return;

        // Rouge 进攻图：敌人核心全灭 = 已占领区块
        if(!restoring && rougeAttackMap){
            coreCheckTimer += Time.delta / 60f;
            if(coreCheckTimer >= coreCheckInterval){
                coreCheckTimer = 0f;
                if(enemyCoresCleared()) startRestore();
            }
        }

        float target, rate;
        if(restoring){
            // 占领后：迅速恢复白天
            target = dayAmbient;
            rate = (nightAmbient - dayAmbient) / Math.max(0.01f, restoreDuration);
        }else{
            // 只黑不亮：黑暗进度只增不减，目标随波次单调上升
            maxProgress = Math.max(maxProgress, waveProgress());
            target = Mathf.lerp(dayAmbient, nightAmbient, maxProgress);
            rate = darkenRate;
        }

        ambientAlpha = Mathf.approachDelta(ambientAlpha, target, rate / 60f);
        applyAmbient();
    }

    /** 波次进度：0 = 刚进区块（白天），1 = 已推进 fullDarkWaves 波（极黑）。 */
    private static float waveProgress(){
        float done = state.wave - baseWave;
        return Mathf.clamp(done / Math.max(1f, fullDarkWaves), 0f, 1f);
    }

    /** Rouge 进攻图敌人核心是否已全灭。 */
    private static boolean enemyCoresCleared(){
        for(Building b : Groups.build){
            if(b.block instanceof CoreBlock && b.team != player.team() && b.team != Team.derelict){
                return false;
            }
        }
        return true;
    }

    /** 进入「占领后恢复白天」阶段。 */
    private static void startRestore(){
        if(restoring) return;
        restoring = true;
        Log.info("[DayNight] 区块已占领，开始迅速恢复白天");
    }

    /** 把当前环境光写入本局规则（LightRenderer 每帧读取，实时生效）。 */
    private static void applyAmbient(){
        state.rules.lighting = true;
        Color amb = state.rules.ambientLight;
        amb.r = nightColor.r;
        amb.g = nightColor.g;
        amb.b = nightColor.b;
        amb.a = ambientAlpha;
    }
}
