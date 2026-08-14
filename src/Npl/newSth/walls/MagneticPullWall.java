package Npl.newSth.walls;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * 磁力墙 · 继承 Wall（保留原版的闪反、反弹子弹、自动贴图等全部功能）
 * 特性：被攻击（子弹命中）时，按概率触发「磁力吸引」——
 *        接下来若干 tick 把半径内的敌方单位持续拉向墙中心。
 *
 * =============================================================
 * 【可调参数接口】
 * =============================================================
 *  pullChance   = 0.25f;    // 每次受击触发概率（0~1，0.25=25%）
 *  pullRadius   = 160f;     // 吸引半径（像素）
 *  pullStrength = 3.5f;     // 吸引强度（越大拉得越快，像素/tick）
 *  pullDuration = 20f;      // 触发一次持续多少 tick（≈持续磁力时间）
 *  pullEffect   = Fx.steam; // 触发时的视觉特效（null=不放特效）
 */
public class MagneticPullWall extends Wall {

    /* ==========================================================
     *                  磁力吸引参数
     * ========================================================== */
    /** 每次子弹命中时触发磁力的概率（0~1） */
    public float pullChance = 0.25f;
    /** 吸引半径（像素） */
    public float pullRadius = 160f;
    /** 吸引强度（像素/tick，越大拉得越快越狠） */
    public float pullStrength = 3.5f;
    /** 触发一次持续多少 tick（> pullRadius/pullStrength 才够把敌人拉到身边） */
    public float pullDuration = 20f;
    /** 触发瞬间放一次的视觉特效（null=关闭），可改成 NuFx.xxx */
    public Effect pullEffect = Fx.steam;

    public MagneticPullWall(String name){
        super(name);
        // solid / destructible / walls 等默认已由 Wall 父类设置
        update = true; // 需要 updateTile() 跑持续拉力
    }

    @Override
    public void init(){
        super.init();
        updateClipRadius(pullRadius);
    }

    @Override
    public void setStats(){
        super.setStats();
        if(pullChance > 0f){
            stats.add(Stat.affinities, "Magnet " + (int)(pullChance*100) + "% / R:" + (int)(pullRadius/tilesize));
        }
    }

    /* ==========================================================
     *                    Building 实现
     * ========================================================== */
    public class MagneticPullWallBuild extends WallBuild {

        /** 当前还有多少 tick 保持磁力吸引（>0 表示磁力生效中） */
        public float pullTicksLeft = 0f;

        /* ----------------------------------------------------------
         * 子弹命中时 → 按概率触发磁力
         * ---------------------------------------------------------- */
        @Override
        public boolean collision(Bullet bullet){
            // 先走原版 Wall 的碰撞逻辑：闪白、闪电反击、反弹子弹
            boolean absorbed = super.collision(bullet);

            // 只对敌方子弹触发（防止自己子弹打到自己触发磁力）
            if(bullet.team != team && pullChance > 0f && Mathf.chance(pullChance)){
                triggerPull();
            }

            return absorbed;
        }

        /* 也可以直接被外部主动调用：((MagneticPullWallBuild)build).triggerPull(); */
        public void triggerPull(){
            // 若已经在吸引中，就叠加时间（不重置，避免反复触发越打越断）
            pullTicksLeft = Math.max(pullTicksLeft, pullDuration);
            if(pullEffect != null){
                pullEffect.at(x, y, team.color);
            }
        }

        /* ----------------------------------------------------------
         * Tick 级：对 pullRadius 范围内敌人施加向墙心的拉力
         * ---------------------------------------------------------- */
        @Override
        public void updateTile(){
            super.updateTile();

            if(pullTicksLeft > 0f){
                pullTicksLeft -= Time.delta;
                if(pullTicksLeft <= 0f) pullTicksLeft = 0f;

                float rad = pullRadius;
                // 拉力强度按"剩余持续时间"线性衰减，避免突然消失（可选，注释掉就是恒定强度）
                float scl = Mathf.clamp(pullTicksLeft / Math.max(pullDuration, 0.0001f));
                final float strength = pullStrength * scl;

                Units.nearbyEnemies(team, x, y, rad, u -> {
                    if(u == null || !u.isValid()) return;
                    // 不拉动飞行/免疫移动的单位（粗略判断：flying 单位通常不被磁力墙吸附）
                    if(u.type == null || u.type.flying) return;

                    float dx = x - u.x, dy = y - u.y;
                    float d2 = dx*dx + dy*dy;
                    if(d2 > 0.0001f){
                        float d = (float)Math.sqrt(d2);
                        // 越靠外拉力越大（越近越慢，避免冲到墙上抖动）
                        float falloff = Math.min(d / rad, 1f);
                        float vx = dx / d * strength * falloff;
                        float vy = dy / d * strength * falloff;
                        u.vel.add(vx, vy);
                        // 附带一点减速效果
                        u.apply(StatusEffects.slow, 10f);
                    }
                });
            }
        }

        /* ----------------------------------------------------------
         *              渲染：磁力激活时画一圈指示线
         * ---------------------------------------------------------- */
        @Override
        public void draw(){
            super.draw();

            if(pullTicksLeft > 0f){
                float a = Mathf.clamp(pullTicksLeft / Math.max(pullDuration, 0.0001f));
                Draw.z(Layer.buildBeam - 0.1f);
                Draw.color(team.color, a * 0.5f);
                Lines.stroke(1.5f * a);
                Lines.dashCircle(x, y, pullRadius * (0.9f + 0.1f * a));
                Draw.reset();
            }
        }

        /* ----------------------------------------------------------
         *              存档读写（磁力剩余时间）
         * ---------------------------------------------------------- */
        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(pullTicksLeft);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1) pullTicksLeft = read.f();
        }
    }
}
