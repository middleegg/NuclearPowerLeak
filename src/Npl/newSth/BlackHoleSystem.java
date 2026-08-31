package Npl.newSth;

import arc.Core;
import arc.Events;
import arc.graphics.Blending;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.gl.FrameBuffer;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import mindustry.entities.Units;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;

/**
 * 黑洞系统：视觉扭曲（shader 全屏后处理）+ 游戏逻辑（引力拉扯 + 撕裂伤害）。
 * <p>
 * 触发方式：{@code NuFx.blackHole.at(x, y)}。
 * Effect 第一帧会调 {@link #spawnGameplay} 生成一个持续 90 帧的 gameplay 实体，
 * 每帧对范围内单位施加速度力（引力拉扯），对事件视界内的单位/建筑造成持续伤害（撕裂）。
 * <p>
 * 视觉部分：每帧 {@link #register} 把扭曲参数写入 {@link BlackHoleShader}，
 * 系统在 preDraw 捕获整帧、postDraw 用 shader 贴回屏幕。
 */
public class BlackHoleSystem {

    private static boolean inited = false;
    private static FrameBuffer buffer;

    /** 上一帧是否有任何黑洞注册（preDraw 时检查，决定本帧是否 begin buffer）。 */
    private static boolean lastFrameActive = false;

    /** 活动中的黑洞 gameplay 实体列表。 */
    private static final Seq<BlackHole> activeHoles = new Seq<>();

    // ====================== 可调参数 ======================
    /** 引力拉扯强度（越大拉得越猛）。默认 3.5。 */
    public static float pullStrength = 3.5f;
    /** 事件视界内每秒撕裂伤害。默认 120。 */
    public static float tearDamage = 120f;
    /** 引力作用范围倍数（相对 radius）。默认 3.5。 */
    public static float pullRangeMul = 3.5f;
    /** 事件视界半径倍数（相对 radius），进入此范围开始撕裂。默认 0.5。 */
    public static float eventHorizonMul = 0.5f;

    /** 初始化入口，需在 ClientLoadEvent 调用一次。 */
    public static void init(){
        if(inited) return;
        inited = true;

        buffer = new FrameBuffer();

        // ── preDraw：上一帧有黑洞时 begin buffer ──
        Events.run(Trigger.preDraw, () -> {
            if(lastFrameActive){
                try {
                    buffer.resize(Core.graphics.getWidth(), Core.graphics.getHeight());
                    buffer.begin(Color.clear);
                } catch(Throwable t){
                    Log.err("[BlackHoleSystem] buffer begin failed", t);
                    lastFrameActive = false;
                }
            }
        });

        // ── postDraw：若 begin 过 buffer 就 end + blit ──
        Events.run(Trigger.postDraw, () -> {
            BlackHoleShader sh = BlackHoleShader.instance;

            if(sh == null){
                if(lastFrameActive){
                    try { buffer.end(); } catch(Throwable ignored){}
                    lastFrameActive = false;
                }
                return;
            }

            boolean thisFrameActive = sh.hasAny();

            if(lastFrameActive){
                try {
                    buffer.end();
                    Draw.blend(Blending.disabled);
                    buffer.blit(sh);
                    Draw.blend();
                } catch(Throwable t){
                    Log.err("[BlackHoleSystem] buffer blit failed", t);
                }
            }

            lastFrameActive = thisFrameActive;
            sh.clear();
        });

        // ── update：更新所有活动黑洞（引力拉扯 + 撕裂伤害）──
        Events.run(Trigger.update, () -> {
            activeHoles.removeAll(bh -> !bh.update());
        });
    }

    /** Effect draw 时调用：注册扭曲参数到 shader。
     *  shader 懒加载：首次调用时编译。 */
    public static void register(float x, float y, float radius, float strength){
        BlackHoleShader sh = BlackHoleShader.instance;
        if(sh == null){
            BlackHoleShader.load();
            sh = BlackHoleShader.instance;
            if(sh == null) return;
        }
        sh.add(x, y, radius, strength);
    }

    /** 生成一个黑洞 gameplay 实体（引力 + 撕裂），由 Effect 第一帧调用。 */
    public static void spawnGameplay(float x, float y, float radius){
        activeHoles.add(new BlackHole(x, y, radius, 90f));
    }

    /**
     * 黑洞 gameplay 实体：每帧对范围内单位施加引力、对事件视界内单位/建筑造成撕裂伤害。
     * <p>
     * 引力：所有单位（不分敌我）都被拉向中心，越近力越大（平方衰减）。
     * 撕裂：进入事件视界（radius × {@link #eventHorizonMul}）的单位和建筑每秒受到 {@link #tearDamage} 伤害。
     */
    public static class BlackHole {
        public float x, y;
        public float radius;
        public float life;
        public final float maxLife;

        public BlackHole(float x, float y, float radius, float life){
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.maxLife = life;
            this.life = life;
        }

        /** @return true 表示仍存活，false 表示生命周期结束。 */
        public boolean update(){
            life -= Time.delta;
            if(life <= 0) return false;

            float pullRange = radius * pullRangeMul;
            float horizon = radius * eventHorizonMul;

            // —— ① 引力拉扯 + 撕裂单位（所有单位，不分敌我）——
            //    Units.nearby 无 (x,y,range,cons) 重载，用矩形版本：x,y,w,h,cons
            try {
                float r = pullRange;
                Units.nearby(x - r, y - r, r * 2f, r * 2f, u -> {
                    if(u == null || u.dead || !u.isValid()) return;
                    float dx = x - u.x;
                    float dy = y - u.y;
                    float d = Mathf.len(dx, dy);
                    if(d < 0.01f || d > pullRange) return;

                    // 引力：越近越强，平方衰减
                    float t = Mathf.clamp(1f - d / pullRange);
                    float force = t * t * pullStrength;
                    u.vel.x += dx / d * force;
                    u.vel.y += dy / d * force;

                    // 事件视界内：撕裂伤害
                    if(d < horizon){
                        u.damage(tearDamage * Time.delta);
                    }
                });
            } catch(Throwable ignored){
                // 世界未加载完时 Groups 可能异常，静默跳过
            }

            // —— ② 撕裂建筑（建筑无速度无法拉扯，只造成伤害）——
            try {
                if(Groups.build != null && Groups.build.tree() != null){
                    float r = pullRange;
                    Groups.build.intersect(x - r, y - r, r * 2f, r * 2f, b -> {
                        if(b == null || b.dead || !b.isValid()) return;
                        float d = Mathf.dst(x, y, b.x, b.y);
                        if(d < horizon){
                            b.damage(tearDamage * Time.delta);
                        }
                    });
                }
            } catch(Throwable ignored){}

            return true;
        }
    }
}
