package Npl.newSth.AI;

import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.types.*;

/**
 * 虫子（多节段）单位专用飞行 AI（复刻自 PU132 的 WormAI，继承自 FlyingAI）。
 * <p>
 * 在多节段单位系统中的角色：
 * <ul>
 *   <li>每个节段（段身）都是一个独立的飞行单位，各自挂载一份 WormAI 来控制自己的移动。</li>
 *   <li>头部节段负责索敌与进攻；后续节段通过「记仇机制」把受击位置回传给头部，
 *       由头部统一决策追击方向，从而让整条虫子表现得像一个整体。</li>
 *   <li>本类只重写 {@link #updateMovement()} 控制移动逻辑；
 *       索敌与开火交给父类 FlyingAI 的 updateTargeting/updateWeapons 自动处理，
 *       这样既能复用 v158 原版的稳定行为，又能针对虫子做最小改动。</li>
 * </ul>
 * <p>
 * ★ v158 适配说明：去掉了旧版 PU132 中 {@code command() == UnitCommand.attack} 的判断，
 * 因为 v158 已无该 API，而 FlyingAI 默认就是攻击模式，无需再额外判断。
 * <p>
 * @see FlyingAI
 */
public class WormAI extends FlyingAI{
    /** 记仇目标的世界坐标，由段身受击时通过 {@link #setTarget} 回传给头部。 */
    public Vec2 pos = new Vec2();
    /** 当前记仇目标的「优先级分数」，分数高的会覆盖分数低的，保证头部优先追击威胁最大的方向。 */
    public float score = 0f;
    /** 记仇剩余时间（秒），归零后清空目标，避免虫子一直追着旧位置跑。 */
    public float time = 0f;
    /** 预留的旋转计时器（当前未使用，保留以兼容 PU132 字段结构）。 */
    protected float rotateTime = 0f;

    /**
     * 每帧更新移动逻辑。这是本 AI 的核心，按优先级从高到低处理三种情况：
     * <ol>
     *   <li>有目标 → 冲向目标或盘旋攻击；</li>
     *   <li>无目标但有记仇位置 → 移动到记仇位置；</li>
     *   <li>无目标且无记仇 → 波次模式下回出生点；否则原地待机并锁死速度。</li>
     * </ol>
     */
    @Override
    public void updateMovement(){
        // —— 情况 2：无目标但有记仇位置 ——
        // PU132 的记仇机制：段身被打到后会把位置回传给头部，头部在没有显式目标时
        // 会先朝这个位置移动一段时间，让虫子能「还手」而不是傻站着。
        if(target == null && time > 0f){
            moveTo(pos, 0f);
        }

        // —— 情况 1：有目标且单位带武器 → 进入攻击移动 ——
        if(target != null && unit.hasWeapons()){
            if(!unit.type.circleTarget){
                // 非盘旋模式（如 arcnelidia）：直接冲到目标脸前近战。
                // ★ 这里停止距离固定用 30f，而不是 unit.range()*0.8f。
                //   原因：unit.range() 通常很大（例如 210），乘以 0.8 后停止距离仍有 168，
                //   单位在离目标很远的地方就停下，导致看起来「够不着」。
                //   改成 30f 让单位贴到目标跟前再停，攻击手感更扎实。
                moveTo(target, 30f);
                unit.lookAt(target);
            }else{
                // 盘旋模式（如 toxobyte / catenapede / devourer / oppression）：围绕目标转圈输出。
                // ★ 这里改用 v158 原生的 circleAttack，而不是自己写 attack() + moveAt。
                //   原因：circleAttack 内部用 movePref 处理移动，对 omniMovement=false
                //   的单位有正确的速度分解，比自己手写更可靠。
                // ★ v158 已无 circleTargetRadius 字段，因此盘旋半径固定为 120f（PU132 默认值）。
                circleAttack(120f);
            }
        }

        // —— 情况 3：既无目标也无记仇 ——
        if(target == null && time <= 0f && Vars.state.rules.waves && unit.team == Vars.state.rules.defaultTeam){
            // 波次模式下的防守方：朝最近的出生点移动，把虫子推回前线。
            moveTo(getClosestSpawner(), Vars.state.rules.dropZoneRadius + 120f);
        }else if(target == null && time <= 0f){
            // ★ 真正的「完全待机」分支：既没有目标、没有记仇、也不是波次推进。
            // 这里必须显式把速度归零，原因如下：
            //   1. Mindustry 的飞行单位在上一帧若残留速度，物理引擎不会自动把它衰减到 0，
            //      单位会继续「滑行」漂移，看起来像没刹住车。
            //   2. 对多节段单位来说漂移更致命：头部漂移会通过 parent 链把段身拽歪，
            //      导致整条虫子身体错位、液压装饰拉扯，严重影响观感。
            //   3. 因此待机时强制 unit.vel.setZero()，让虫子稳稳停住，段身保持整齐。
            unit.vel.setZero();
        }

        // 记仇计时器递减；归零时清掉分数，防止旧的高分目标永久霸占头部注意力。
        rotateTime = Math.max(0f, rotateTime - Time.delta);
        if(time <= 0f) score = 0f;
        time = Math.max(0f, time - Time.delta);
    }

    /**
     * 段身受击时调用，把受击位置和「威胁分数」通知给头部。
     *
     * <p>原理：分数高的会覆盖分数低的（{@code if(score < this.score) return;}），
     * 这样当多个段身同时被打时，头部会优先追击威胁最大的那个方向，而不是被低分目标带偏。
     * 记仇持续 3 秒（180 帧），过期后自动清空，避免虫子追着陈旧位置空跑。
     *
     * @param x     受击世界坐标 X
     * @param y     受击世界坐标 Y
     * @param score 本次受击的威胁分数（越高越优先）
     */
    public void setTarget(float x, float y, float score){
        if(score < this.score) return;
        pos.set(x, y);
        this.score = score;
        time = 3f * 60f;
    }
}
