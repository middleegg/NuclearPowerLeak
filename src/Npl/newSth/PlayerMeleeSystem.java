package Npl.newSth;

import arc.*;
import arc.input.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import Npl.content.*;

import static mindustry.Vars.*;

/**
 * 玩家手动挥砍系统 · 按 V 键（可改）在玩家朝向方向挥一刀。
 * <p>
 * 实现：监听 Trigger.update 检测按键 → 冷却控制 → 以玩家位置为原点、
 * 朝鼠标方向生成 NuFx.slash 刀光 + 扇形范围内的敌人造成伤害。
 *
 * <p><b>启用方式：</b>在 nu.loadContent() 里调用 PlayerMeleeSystem.load();
 * <pre>{@code
 * public void loadContent(){
 *     // ... 其他 mod 内容加载 ...
 *     PlayerMeleeSystem.load();   // 玩家近战系统
 * }
 * }</pre>
 *
 * <p>本系统可调字段：
 * <ul>
 *   <li>{@link #cooldown} 挥砍冷却（秒），默认 0.5</li>
 *   <li>{@link #slashRange} 刀光长度（像素），默认 50</li>
 *   <li>{@link #slashArc} 扇形角度，默认 90°</li>
 *   <li>{@link #damage} 单次伤害，默认 40</li>
 *   <li>{@link #key} 触发按键，默认 V</li>
 * </ul>
 */
public class PlayerMeleeSystem{

    // ====================== 可调参数（修改后立即生效） ======================
    /** 挥砍冷却（秒）。 */
    public static float cooldown = 0.5f;
    /** 刀光长度（像素）。 */
    public static float slashRange = 50f;
    /** 扇形伤害角度（度）。 */
    public static float slashArc = 90f;
    /** 单次挥砍伤害。 */
    public static float damage = 40f;

    /** 触发按键，默认 V。可改成 KeyCode.mouseLeft（左键）、KeyCode.f 等。 */
    public static KeyCode key = KeyCode.v;

    // ====================== 内部状态 ======================
    private static boolean loaded = false;
    private static float lastSlashTime = -999f;  // 上次挥砍的时间戳（秒）

    public static void load(){
        if(loaded) return;
        loaded = true;

        Events.run(EventType.Trigger.update, () -> {
            // 没在玩 / 玩家不存在 / 玩家单位为空 → 跳过
            if(!state.isGame() || player == null) return;
            Unit u = player.unit();
            if(u == null || u.dead) return;

            // 检测按键按下（按下瞬间触发，不连续）
            if(!Core.input.keyTap(key)) return;

            // 冷却检查
            if(Time.time - lastSlashTime < cooldown * 60f) return;
            lastSlashTime = Time.time;

            doSlash(player.unit());
        });
    }

    /** 在指定单位位置朝鼠标方向挥一刀。 */
    public static void doSlash(Unit u){
        if(u == null || !u.isValid()) return;

        // 鼠标在屏幕坐标 → 转换为游戏世界坐标
        float mx = Core.input.mouseWorldX();
        float my = Core.input.mouseWorldY();

        // 玩家位置 → 鼠标方向
        float angle = Angles.angle(u.x, u.y, mx, my);

        // ① 触发刀光 effect
        NuFx.slash.at(u.x, u.y, angle);

        // ② 扇形伤害
        float x = u.x, y = u.y;
        float range = slashRange;
        float arc = slashArc;
        float dmg = damage;
        Team team = u.team;

        // ② 扇形伤害（try-catch 包裹：世界未完全加载时某些 team 的 unit/build group 的
        //    QuadTree tree 可能为 null，直接 crash；这里吞掉异常，等下一帧 tree 就绪再正常伤害）
        try {
            // 敌方单位
            Units.nearbyEnemies(team, x, y, range, e -> {
                if(e.dead || !e.isValid()) return;
                float angTo = Angles.angle(x, y, e.x, e.y);
                if(withinArc(angle, angTo, arc)){
                    e.damage(dmg);
                }
            });

            // 敌方建筑
            if(Groups.build != null && Groups.build.tree() != null){
                Groups.build.intersect(x - range, y - range, range * 2f, range * 2f, build -> {
                    if(build == null || build.dead || !build.isValid()) return;
                    if(build.team == team || build.team == Team.derelict) return;
                    float angTo = Angles.angle(x, y, build.x, build.y);
                    if(withinArc(angle, angTo, arc)){
                        build.damage(dmg);
                    }
                });
            }
        } catch(Throwable t){
            // 静默忽略 tree 未就绪等瞬时异常，下一帧重试
        }
    }

    /** 判断 targetAngle 是否在 facing 中心 ± arc/2 度范围内。 */
    private static boolean withinArc(float facing, float targetAngle, float arc){
        float diff = Math.abs(Angles.angleDist(facing, targetAngle));
        return diff <= arc / 2f;
    }
}
