package Npl.content;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.entities.abilities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.units.UnitAssembler.*;
import Npl.content.*;
import Npl.newSth.BulletTailEffect;
import Npl.newSth.BlackHoleSystem;
import Npl.newSth.effects.CuneEffect;
import Npl.newSth.expEffect;
import Npl.newSth.LightningStormEffect;
import Npl.newSth.TextPopupEffect;
import Npl.newSth.OutlineBurstEffect;

import static arc.graphics.g2d.Draw.rect;
import static arc.graphics.g2d.Draw.*;
import static arc.graphics.g2d.Lines.*;
import static arc.math.Angles.*;
import static mindustry.Vars.*;

/**
 * 自定义特效集合（类比 Mindustry 原版 Fx 类）
 * 用法：在任何地方调用 NuFx.explosion1.at(x, y, Color.scarlet); 即可
 */
public class NuFx {
    /** 原版 Fx.rand 的复制品，供需要 setSeed 的特效使用 */
    public static final Rand rand = new Rand();
    /** 原版 Fx.v 的复制品，供 trns() 临时向量运算使用 */
    public static final Vec2 v = new Vec2();

    // ========================================================
    // 爆炸 1：冲击波圈 + 12 方向飞散粒子
    // ========================================================
    public static Effect dotPoison = new Effect(45f,e->{
        color(NuColor.DivineWrathColor, NuItems.dirtyCoagulum.color, e.fin());
        randLenVectors(e.id, 3, 2f + e.fin() * 7f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.1f + e.fout() * 1.4f);
            Fill.circle(e.x + x, e.y + y, 0.1f + e.fout() * 4f);
        });
        Draw.reset();
    }).layer(Layer.bullet);
    public static Effect explosion1 = new Effect(30f, 200f, e -> {
        float f = e.fin();

        // ① 冲击波圈（Out 曲线：初快后慢，物理感）
        float ringR = 80f * Interp.pow2Out.apply(f);
        float ringA = 1f - f;
        if (ringA > 0.02f) {
            Lines.stroke(3f - 2f * f, new Color(e.color).a(ringA));
            Lines.circle(e.x, e.y, ringR);
        }

        // ② 12 颗粒子向 12 个方向飞
        int particles = 12;
        float flyDistance = 55f * Interp.pow2Out.apply(f);
        float particleAlpha = 1f - Mathf.pow(f, 2f);
        if (particleAlpha > 0.02f) {
            Draw.color(new Color(e.color).a(particleAlpha));
            for (int i = 0; i < particles; i++) {
                float angle = i * (360f / particles);
                float px = e.x + Angles.trnsx(angle, flyDistance);
                float py = e.y + Angles.trnsy(angle, flyDistance);
                float size = (3f - 2.5f * f) * 2f;
                Fill.square(px, py, size, angle);
            }
            Draw.color();  // 恢复默认颜色（白）
        }

        Draw.reset(); // 防止颜色/线宽污染后续渲染
    });

    // ========================================================
    // Pale 烟：PaleColor → PaleBackColor 的渐变色烟雾小圆
    // ========================================================
    public static Effect PaleSmoke = new Effect(100, 60f, e -> {
        // Draw.color 只能传 1 个颜色 + 1 个 alpha；这里手动做"色 A 线性插值到色 B"
        Color lerped = Tmp.c1.set(e.color).lerp(NuColor.PaleBackColor, e.fin());
        Draw.color(lerped, 1f);
        float r = (7f - e.fin() * 7f) / 2f;
        if (r > 0.1f) Fill.circle(e.x, e.y, r);
        Draw.reset();
    });
    public static Effect SailSmoke = new Effect(100, 60f, e -> {
        // Draw.color 只能传 1 个颜色 + 1 个 alpha；这里手动做"色 A 线性插值到色 B"
        Color lerped = Tmp.c1.set(e.color).lerp(NuColor.SailBackColor, e.fin());
        Draw.color(lerped, 1f);
        float r = (7f - e.fin() * 7f) / 2f;
        if (r > 0.1f) Fill.circle(e.x, e.y, r);
        Draw.reset();
    });
    // ========================================================
    // 爆炸 2：小爆炸（最大半径 10 像素，适合小单位死亡特效
    // ========================================================
    public static Effect explosion2 = new Effect(30f, 40f, e -> {
        float f = e.fin();
        // 最大半径 10（要求"大小不超过10"）
        float maxR = 10f;
        float ringR = maxR * Interp.pow2Out.apply(f);
        float ringA = 1f - f;
        if (ringA > 0.02f) {
            Lines.stroke(1.6f - 1.1f * f, new Color(e.color).a(ringA));
            Lines.circle(e.x, e.y, ringR);
        }
        int particles = 6;
        float fly = 7f * Interp.pow2Out.apply(f);
        float pa = 1f - Mathf.pow(f, 2f);
        if (pa > 0.02f) {
            Draw.color(new Color(e.color).a(pa));
            for (int i = 0; i < particles; i++) {
                float ang = i * (360f / particles);
                float px = e.x + trnsx(ang, fly);
                float py = e.y + trnsy(ang, fly);
                float sz = 1.3f * (1.4f - 1.1f * f);
                Fill.square(px, py, sz, ang);
            }
            Draw.color();
        }
        Draw.reset();
    });

    // ========================================================
    // 绿色激光蓄力特效 · 纯改版（颜色从 Pal.heal → 纯白色）
    //   视觉：描边光环（由小向外扩散、由细变粗再消失）
    //         + 中心实心蓄力点（由小变大）
    //         + 20 颗随机方向圆形粒子
    //         + 15 颗随机方向旋转 45° 方块粒子
    // ========================================================

    /** 原版 Fx.greenLaserCharge 的白色版（lifetime 80 / clip 100）。适合普通激光武器蓄力。 */
    public static Effect LaserChargeWhite = new Effect(80f, 100f, e -> {
        color(Color.white);
        stroke(e.fin() * 2f);
        Lines.circle(e.x, e.y, 4f + e.fout() * 100f);
        Fill.circle(e.x, e.y, e.fin() * 20f);
        randLenVectors(e.id, 20, 40f * e.fout(),
                (x, y) -> Fill.circle(e.x + x, e.y + y, e.fin() * 3f));
        color(Color.white);
        randLenVectors(e.id, 15, 14f * e.fout(),
                (x, y) -> Fill.square(e.x + x, e.y + y, e.fin() * 2.2f, 45f));
    });
    public static Effect ChargeDesp = new Effect(80f, 100f, e -> {
        color(NuColor.DespColor);
        stroke(e.fin() * 5f);
        Lines.circle(e.x, e.y, 4f + e.fout() * 100f);
        Fill.circle(e.x, e.y, e.fin() * 20f);
        color(NuColor.PaleColor);
        Fill.circle(e.x, e.y, e.fin() * 20f);
        color(Color.white);
        randLenVectors(e.id, 15, 14f * e.fout(),
                (x, y) -> Fill.square(e.x + x, e.y + y, e.fin() * 2.2f, 45f));
    });

    /** 原版 Fx.greenLaserChargeSmall 的白色版（lifetime 50 / clip 60，整体缩小 40%）。
     *  适合小口径/速射激光的蓄力视觉。 */
    public static Effect LaserChargeSmallWhite = new Effect(50f, 60f, e -> {
        float s = 0.5f;
        color(Color.white);
        stroke(e.fin() * 1.5f*s);
        Lines.circle(e.x, e.y, 2.4f + e.fout() * 60f*s);   // 4 * 0.6 = 2.4；100 * 0.6 = 60
        Fill.circle(e.x, e.y, e.fin() * 12f*s);              // 20 * 0.6 = 12
        randLenVectors(e.id, 12, 24f * e.fout(),           // 40 * 0.6 = 24；粒子数 20→12
                (x, y) -> Fill.circle(e.x + x, e.y + y, e.fin() * 1.8f*s));   // 3 * 0.6 = 1.8
        color(Color.white);
        randLenVectors(e.id, 9, 8.4f * e.fout(),           // 14 * 0.6 = 8.4；15→9
                (x, y) -> Fill.square(e.x + x, e.y + y, e.fin() * 1.32f, 45f)); // 2.2 * 0.6 = 1.32
    });
    public static Effect LaserChargeSmallCore = new Effect(50f, 60f, e -> {
        float s = 0.5f;
        color(NuColor.CoreConColor);
        stroke(e.fin() * 1.5f*s);
        Lines.circle(e.x, e.y, 2.4f + e.fout() * 60f*s);   // 4 * 0.6 = 2.4；100 * 0.6 = 60
        Fill.circle(e.x, e.y, e.fin() * 12f*s);              // 20 * 0.6 = 12
        randLenVectors(e.id, 12, s*24f * e.fout(),           // 40 * 0.6 = 24；粒子数 20→12
                (x, y) -> Fill.circle(e.x + x, e.y + y, e.fin() * 1.8f*s));   // 3 * 0.6 = 1.8
        color(Color.white);
        randLenVectors(e.id, 9, s*8.4f * e.fout(),           // 14 * 0.6 = 8.4；15→9
                (x, y) -> Fill.square(e.x + x, e.y + y, e.fin() * s *1.32f, 45f)); // 2.2 * 0.6 = 1.32
    });
    public static Effect LaserChargeHonor = new Effect(80f, 100f, e -> {
        float s = 2f;
        color(NuColor.HonorConColor);
        stroke(e.fin() * 1.5f*s);
        Lines.circle(e.x, e.y, 4f + e.fout() * 60f*s);   // 4 * 0.6 = 2.4；100 * 0.6 = 60
        Fill.circle(e.x, e.y, e.fin() * 12f*s);              // 20 * 0.6 = 12
        randLenVectors(e.id, 20, s*24f * e.fout(),           // 40 * 0.6 = 24；粒子数 20→12
                (x, y) -> {
            Fill.circle(e.x + x, e.y + y, e.fin() * 2.5f*s);
            Drawf.light(e.x + x, e.y + y, e.fin() * 17.5f, NuColor.HonorConColor, 0.7f);
                });   // 3 * 0.6 = 1.8
        color(Color.white);
        Fill.circle(e.x, e.y, e.fin() * 10);
        Drawf.light(e.x, e.y, e.fin() * 20f, NuColor.HonorConColor, 0.7f);
    }).followParent(true).rotWithParent(true);

    /** 蓄力光圈：从中心向外扩张到 maxR，持续一段时间，再缩回。
     *  调用：chargeRing.at(x, y); */
    public static Effect chargeRing = new Effect(90f, 200f, e -> {
        float maxR      = 40f;    // 最终半径
        float expandEnd = 0.5f;   // 0 ~ 50%：扩张阶段
        float holdEnd   = 0.75f;  // 50% ~ 75%：持续阶段；75% ~ 100%：收缩阶段

        float r;
        if (e.fin() < expandEnd) {
            // 扩张：0 → maxR（pow2Out 初快后慢，蓄力感）
            r = maxR * Interp.pow2Out.apply(e.fin() / expandEnd);
        } else if (e.fin() < holdEnd) {
            r = maxR;
        } else {
            // 收缩：maxR → 0（pow2In 初慢后快，收回干脆）
            r = maxR * (1f - Interp.pow2In.apply((e.fin() - holdEnd) / (1f - holdEnd)));
        }

        color(NuColor.BloodColor);
        stroke(2f * e.fout());            // 线宽随整体寿命淡出
        Lines.circle(e.x, e.y, r);
        Fill.circle(e.x, e.y, r * 0.15f); // 中心一个小光点（可选，不要可删）
    });

    // ========================================================
    // reactorExplosion · 白色版（原版 Fx.reactorExplosion 改色）
    //   原紫色 Pal.reactorPurple / Pal.reactorPurple2 / Pal.lighterOrange
    //   全部替换为 Color.white
    // ========================================================
    public static Effect ExplosionWhite = new Effect(30, 500f, b -> {
        float intensity = 6.8f;
        float baseLifetime = 25f + intensity * 11f;
        b.lifetime = 50f + intensity * 65f;
        color(Color.white);       // 原版：Pal.reactorPurple2
        alpha(0.7f);
        for(int i = 0; i < 4; i++){
            rand.setSeed(b.id*2 + i);
            float lenScl = rand.random(0.4f, 1f);
            int fi = i;
            b.scaled(b.lifetime * lenScl, e -> {
                randLenVectors(e.id + fi - 1, e.fin(Interp.pow10Out), (int)(2.9f * intensity), 22f * intensity, (x, y, in, out) -> {
                    float fout = e.fout(Interp.pow5Out) * rand.random(0.5f, 1f);
                    float rad = fout * ((2f + intensity) * 2.35f);

                    Fill.circle(e.x + x, e.y + y, rad);
                    Drawf.light(e.x + x, e.y + y, rad * 2.5f, Color.white, 0.5f);  // 原版：Pal.reactorPurple
                });
            });
        }
        b.scaled(baseLifetime, e -> {
            Draw.color();
            e.scaled(5 + intensity * 2f, i -> {
                stroke((3.1f + intensity/5f) * i.fout());
                Lines.circle(i.x, i.y, (3f + i.fin() * 14f) * intensity);
                Drawf.light(i.x, i.y, i.fin() * 14f * 2f * intensity, Color.white, 0.9f * i.fout());
            });
            color(Color.white);       // 原版：color(Pal.lighterOrange, Pal.reactorPurple, e.fin())
            stroke((2f * e.fout()));
            Draw.z(Layer.effect + 0.001f);
            randLenVectors(e.id + 1, e.finpow() + 0.001f, (int)(8 * intensity), 28f * intensity, (x, y, in, out) -> {
                lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + out * 4 * (4f + intensity));
                Drawf.light(e.x + x, e.y + y, (out * 4 * (3f + intensity)) * 3.5f, Draw.getColor(), 0.8f);
            });
        });
    });
    public static Effect ExplosionNuclear = new Effect(30, 500f, b -> {
        float intensity = 6.8f;
        float s = 1.5f;
        float baseLifetime = 25f + intensity * 11f;
        b.lifetime = 50f + intensity * 65f;
        color(NuColor.NuclearColor);       // 原版：Pal.reactorPurple2
        alpha(0.7f);
        for(int i = 0; i < 4; i++){
            rand.setSeed(b.id*2 + i);
            float lenScl = rand.random(0.4f, 1f);
            int fi = i;
            b.scaled(b.lifetime * lenScl, e -> {
                randLenVectors(e.id + fi - 1, e.fin(Interp.pow10Out), (int)(2.9f * s * intensity), 22f * s * intensity, (x, y, in, out) -> {
                    float fout = e.fout(Interp.pow5Out) * s * rand.random(0.5f, 1f);
                    float rad = fout * ((2f + intensity) * 2.35f*s);
                    Fill.circle(e.x + x, e.y + y, rad * s );
                    Drawf.light(e.x + x, e.y + y, rad * 2.5f * s, NuColor.NuclearBackColor, 0.5f);  // 原版：Pal.reactorPurple
                });
            });
        }
        b.scaled(baseLifetime, e -> {
            Draw.color();
            e.scaled(5 + intensity * 2f*s, i -> {
                stroke((3.1f + intensity/5f) * i.fout());
                Lines.circle(i.x, i.y, (3f + i.fin() * 14f) * intensity*s);
                Drawf.light(i.x, i.y, i.fin() * 14f * 2f * intensity*s, NuColor.NuclearColor, 0.9f * i.fout());
            });
            color(NuColor.NuclearColor);       // 原版：color(Pal.lighterOrange, Pal.reactorPurple, e.fin())
            stroke((2f*s*e.fout()));
            Draw.z(Layer.effect + 0.001f);
            randLenVectors(e.id + 1, e.finpow() + 0.001f, (int)(8 * intensity), s*28f * intensity, (x, y, in, out) -> {
                lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + out * 4 * s *(4f + intensity));
                Drawf.light(e.x + x, e.y + y, (out * 4 * (3f + intensity)) * s* 3.5f, Draw.getColor(), 0.8f);
            });
        });
    });
    public static Effect RightsExplosion = new Effect(30, 500f, b -> {
        float intensity = 6.8f;
        float s = 0.6f;
        float baseLifetime = 25f + intensity * 11f;
        b.lifetime = 50f + intensity * 65f;
        color(NuColor.HonorColor);       // 原版：Pal.reactorPurple2
        alpha(0.7f);
        for(int i = 0; i < 4; i++){
            rand.setSeed(b.id*2 + i);
            float lenScl = rand.random(0.4f, 1f);
            int fi = i;
            b.scaled(b.lifetime * lenScl, e -> {
                randLenVectors(e.id + fi - 1, e.fin(Interp.pow10Out), (int)(2.9f * s * intensity), 22f * s * intensity, (x, y, in, out) -> {
                    float fout = e.fout(Interp.pow5Out) * s * rand.random(0.5f, 1f);
                    float rad = fout * ((2f + intensity) * 2.35f*s);
                    Fill.circle(e.x + x, e.y + y, rad * s );
                    Drawf.light(e.x + x, e.y + y, rad * 2.5f * s, NuColor.NuclearBackColor, 0.5f);  // 原版：Pal.reactorPurple
                });
            });
        }
        b.scaled(baseLifetime, e -> {
            Draw.color();
            e.scaled(5 + intensity * 2f*s, i -> {
                stroke((3.1f + intensity/5f) * i.fout());
                Lines.circle(i.x, i.y, (3f + i.fin() * 14f) * intensity*s);
                Drawf.light(i.x, i.y, i.fin() * 14f * 2f * intensity*s, NuColor.NuclearColor, 0.9f * i.fout());
            });
            color(NuColor.HonorColor);       // 原版：color(Pal.lighterOrange, Pal.reactorPurple, e.fin())
            stroke((2f*s*e.fout()));
            Draw.z(Layer.effect + 0.001f);
            randLenVectors(e.id + 1, e.finpow() + 0.001f, (int)(8 * intensity), s*28f * intensity, (x, y, in, out) -> {
                lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + out * 4 * s *(4f + intensity));
                Drawf.light(e.x + x, e.y + y, (out * 4 * (3f + intensity)) * s* 3.5f, Draw.getColor(), 0.8f);
            });
        });
    });
    public static Effect SailExplosion = new Effect(30, 500f, b -> {
        float intensity = 6.8f;
        float s = 0.6f;
        float baseLifetime = 25f + intensity * 11f;
        b.lifetime = 50f + intensity * 65f;
        color(NuColor.SailColor);       // 原版：Pal.reactorPurple2
        alpha(0.7f);
        for(int i = 0; i < 4; i++){
            rand.setSeed(b.id*2 + i);
            float lenScl = rand.random(0.4f, 1f);
            int fi = i;
            b.scaled(b.lifetime * lenScl, e -> {
                randLenVectors(e.id + fi - 1, e.fin(Interp.pow10Out), (int)(2.9f * s * intensity), 22f * s * intensity, (x, y, in, out) -> {
                    float fout = e.fout(Interp.pow5Out) * s * rand.random(0.5f, 1f);
                    float rad = fout * ((2f + intensity) * 2.35f*s);
                    Fill.circle(e.x + x, e.y + y, rad * s );
                    Drawf.light(e.x + x, e.y + y, rad * 2.5f * s, NuColor.SailColor, 0.5f);  // 原版：Pal.reactorPurple
                });
            });
        }
        b.scaled(baseLifetime, e -> {
            Draw.color();
            e.scaled(5 + intensity * 2f*s, i -> {
                stroke((3.1f + intensity/5f) * i.fout());
                Lines.circle(i.x, i.y, (3f + i.fin() * 14f) * intensity*s);
                Drawf.light(i.x, i.y, i.fin() * 14f * 2f * intensity*s, NuColor.SailColor, 0.9f * i.fout());
            });
            color(NuColor.SailColor);       // 原版：color(Pal.lighterOrange, Pal.reactorPurple, e.fin())
            stroke((2f*s*e.fout()));
            Draw.z(Layer.effect + 0.001f);
            randLenVectors(e.id + 1, e.finpow() + 0.001f, (int)(8 * intensity), s*28f * intensity, (x, y, in, out) -> {
                lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + out * 4 * s *(4f + intensity));
                Drawf.light(e.x + x, e.y + y, (out * 4 * (3f + intensity)) * s* 3.5f, Draw.getColor(), 0.8f);
            });
        });
    });
    public static Effect EnergyExplosion = new Effect(30, 500f, b -> {
        float intensity = 6.8f;
        float s = 0.6f;
        float baseLifetime = 25f + intensity * 11f;
        b.lifetime = 50f + intensity * 65f;
        color(NuColor.EnergyColor);       // 原版：Pal.reactorPurple2
        alpha(0.7f);
        for(int i = 0; i < 4; i++){
            rand.setSeed(b.id*2 + i);
            float lenScl = rand.random(0.4f, 1f);
            int fi = i;
            b.scaled(b.lifetime * lenScl, e -> {
                randLenVectors(e.id + fi - 1, e.fin(Interp.pow10Out), (int)(2.9f * s * intensity), 22f * s * intensity, (x, y, in, out) -> {
                    float fout = e.fout(Interp.pow5Out) * s * rand.random(0.5f, 1f);
                    float rad = fout * ((2f + intensity) * 2.35f*s);
                    Fill.circle(e.x + x, e.y + y, rad * s );
                    Drawf.light(e.x + x, e.y + y, rad * 2.5f * s, NuColor.EnergyColor, 0.5f);  // 原版：Pal.reactorPurple
                });
            });
        }
        b.scaled(baseLifetime, e -> {
            Draw.color();
            e.scaled(5 + intensity * 2f*s, i -> {
                stroke((3.1f + intensity/5f) * i.fout());
                Lines.circle(i.x, i.y, (3f + i.fin() * 14f) * intensity*s);
                Drawf.light(i.x, i.y, i.fin() * 14f * 2f * intensity*s, NuColor.EnergyColor, 0.9f * i.fout());
            });
            color(NuColor.EnergyColor);       // 原版：color(Pal.lighterOrange, Pal.reactorPurple, e.fin())
            stroke((2f*s*e.fout()));
            Draw.z(Layer.effect + 0.001f);
            randLenVectors(e.id + 1, e.finpow() + 0.001f, (int)(8 * intensity), s*28f * intensity, (x, y, in, out) -> {
                lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + out * 4 * s *(4f + intensity));
                Drawf.light(e.x + x, e.y + y, (out * 4 * (3f + intensity)) * s* 3.5f, Draw.getColor(), 0.8f);
            });
        });
    });
    public static Effect DespExplosion = new Effect(30, 500f, b -> {
        float intensity = 6.8f;
        float s = 0.25f;
        float baseLifetime = 25f + intensity * 11f;
        b.lifetime = 50f + intensity * 65f;
        color(NuColor.NuclearColor);       // 原版：Pal.reactorPurple2
        alpha(0.7f);
        for(int i = 0; i < 4; i++){
            rand.setSeed(b.id*2 + i);
            float lenScl = rand.random(0.4f, 1f);
            int fi = i;
            b.scaled(b.lifetime * lenScl, e -> {
                randLenVectors(e.id + fi - 1, e.fin(Interp.pow10Out), (int)(2.9f * s * intensity), 22f * s * intensity, (x, y, in, out) -> {
                    float fout = e.fout(Interp.pow5Out) * s * rand.random(0.5f, 1f);
                    float rad = fout * ((2f + intensity) * 2.35f*s);
                    Fill.circle(e.x + x, e.y + y, rad * s );
                    Drawf.light(e.x + x, e.y + y, rad * 2.5f * s, NuColor.NuclearBackColor, 0.5f);  // 原版：Pal.reactorPurple
                });
            });
        }
        b.scaled(baseLifetime, e -> {
            Draw.color();
            e.scaled(5 + intensity * 2f*s, i -> {
                stroke((3.1f + intensity/5f) * i.fout());
                Lines.circle(i.x, i.y, (3f + i.fin() * 14f) * intensity*s);
                Drawf.light(i.x, i.y, i.fin() * 14f * 2f * intensity*s, NuColor.NuclearColor, 0.9f * i.fout());
            });
            color(NuColor.NuclearColor);       // 原版：color(Pal.lighterOrange, Pal.reactorPurple, e.fin())
            stroke((2f*s*e.fout()));
            Draw.z(Layer.effect + 0.001f);
            randLenVectors(e.id + 1, e.finpow() + 0.001f, (int)(8 * intensity), s*28f * intensity, (x, y, in, out) -> {
                lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), 1f + out * 4 * s *(4f + intensity));
                Drawf.light(e.x + x, e.y + y, (out * 4 * (3f + intensity)) * s* 3.5f, Draw.getColor(), 0.8f);
            });
        });
    });
    // ========================================================
    // neoplasiaSmoke · 白色版（原版 Fx.neoplasiaSmoke 改色）
    //   原 Pal.neoplasmMid → Color.white
    // ========================================================
    public static Effect ConsumeSmoke = new Effect(240f, e -> {
        color(Color.white);        // 原版：Pal.neoplasmMid
        alpha(0.6f);

        rand.setSeed(e.id);
        for(int i = 0; i < 3; i++){
            float len = rand.random(10f), rot = rand.range(40f) + e.rotation;

            e.scaled(e.lifetime * rand.random(0.3f, 1f), b -> {
                v.trns(rot, len * b.finpow());
                Fill.circle(e.x + v.x, e.y + v.y, 3.3f * b.fslope() + 0.2f);
            });
        }
    });
    public static Effect NuclearConsumeSmoke = new Effect(280f, e -> {
        color(NuColor.NuclearColor);        // 原版：Pal.neoplasmMid
        alpha(0.6f);

        rand.setSeed(e.id);
        for(int i = 0; i < 6; i++){
            float len = rand.random(10f), rot = rand.range(120f) + e.rotation;

            e.scaled(e.lifetime * rand.random(0.3f, 1f), b -> {
                v.trns(rot, len * b.finpow());
                Fill.circle(e.x + v.x, e.y + v.y, 3.3f * b.fslope() + 0.2f);
            });
        }
    });
    public static Effect ThallideConsumeSmoke = new Effect(280f, e -> {
        color(NuItems.thallide.color);        // 原版：Pal.neoplasmMid
        alpha(0.6f);

        rand.setSeed(e.id);
        for(int i = 0; i < 6; i++){
            float len = rand.random(10f), rot = rand.range(120f) + e.rotation;

            e.scaled(e.lifetime * rand.random(0.3f, 1f), b -> {
                v.trns(rot, len * b.finpow());
                Fill.circle(e.x + v.x, e.y + v.y, 3.3f * b.fslope() + 0.2f);
            });
        }
    });
    // ========================================================
    // CuneEffect 模块化示例 · 3 个常用预设
    //   更多参数见 newSth/effects/CuneEffect.java 类注释
    // ========================================================

    /** ① 360° 全方位爆炸（24 颗圆形粒子，默认 fallback 圆 + 白→黄→红） */
    public static CuneEffect cuneExplode = new CuneEffect(50f, 300f){{
        particles        = 24;
        velocityMin      = 1.8f;
        velocityMax      = 4f;
        lifetimeMin      = 18f;
        lifetimeMax      = 42f;
        sizeFrom         = 6f;
        sizeTo           = 0.6f;
        colorFrom        = Color.valueOf("FFE082");   // 浅黄
        colorTo          = Color.valueOf("FF5252");   // 红
        additive         = true;
        lightRadius      = 160f;
        lightColor       = Color.valueOf("FFB74D");
        lightOpacity     = 0.65f;
        lightScl         = 2.2f;
    }};

    /** ② 90° 锥形朝前喷射（比如炮口/喷射类武器的出焰） */
    public static CuneEffect cuneConeFront = new CuneEffect(36f, 220f){{
        particles        = 18;
        baseAngleOffset  = 0f;                    // 0=右, 90=上, 180=左, 270=下, 配合 at(x,y,rotation) 用
        spreadAngleRange = 90f;                   // ±45° 锥形
        velocityMin      = 2.5f;
        velocityMax      = 5f;
        lifetimeMin      = 14f;
        lifetimeMax      = 28f;
        sizeFrom         = 5f;
        sizeTo           = 0.5f;
        colorFrom        = Color.valueOf("FFFFFF");   // 白
        colorTo          = Color.valueOf("A470FF");   // 紫
        additive         = true;
    }};

    /** ③ 等你画完贴图后用的"贴图碎片爆发"模板：把 spriteName 改成你的文件名即可。
     *  例：spriteName = "cune_shard";  // 对应 assets/sprites/cune_shard.png */
    public static CuneEffect cuneCustomSprite = new CuneEffect(60f, 320f){{
        particles        = 20;
        velocityMin      = 1.2f;
        velocityMax      = 3.2f;
        lifetimeMin      = 24f;
        lifetimeMax      = 48f;
        sizeFrom         = 10f;
        sizeTo           = 1.5f;
        colorFrom        = Color.white;
        colorTo          = Color.white;
        additive         = true;
        spin             = 8f;     // 贴图自带自旋 8°/tick，破碎感更强
        // ========== 你改这一行就行 ==========
        // spriteName = "你的贴图文件名(不带.png)";
    }};

    // —— 以后再加别的特效：在后面继续写 public static Effect xxx = new Effect(...); ——

    // ========================================================
    // 子弹尾部特效（3 个预设，直接赋值给 BulletType.trailEffect 即可）
    // ========================================================

    /** ① 高能能量弹尾（蓝→紫→青，发光点 + 粒子 + 烟雾，适合激光/等离子/重型炮） */
    public static Effect energyTail = new BulletTailEffect(){{
        colorFrom    = Color.valueOf("A470FF");
        colorTo      = Color.valueOf("7AE7FF");
        particles    = 5;
        particleShape = 0;     // 圆形粒子
        particleSpread = 8f;
        coreFrom     = 5f; coreTo = 0.5f;
        coreGlowMul  = 1.4f;
        smokeLayers  = 1;
        smokeFrom    = 2f; smokeTo = 12f;
        alphaMul     = 1f;
    }};
    public static Effect sailEnergyTail = new BulletTailEffect(){{
        colorFrom    = NuColor.SailColor;
        colorTo      = NuColor.SailBackColor;
        particles    = 12;
        particleShape = 3;     // 圆形粒子
        particleSpread = 8f;
        coreFrom     = 5f; coreTo = 0.5f;
        coreGlowMul  = 1.4f;
        smokeLayers  = 1;
        smokeFrom    = 2f; smokeTo = 12f;
        alphaMul     = 1f;
    }};
    public static Effect bloodEnergyTail = new BulletTailEffect(){{
        colorFrom    = NuColor.BloodColor;
        colorTo      = NuColor.BloodBackColor;
        particles    = 12;
        particleShape = 3;     // 圆形粒子
        particleSpread = 8f;
        coreFrom     = 5f; coreTo = 0.5f;
        coreGlowMul  = 1.4f;
        smokeLayers  = 1;
        smokeFrom    = 2f; smokeTo = 12f;
        alphaMul     = 1f;
    }};
    public static Effect survivalEnergyTail = new BulletTailEffect(){{
        colorFrom    = NuColor.SurvivalColor;
        colorTo      = NuColor.SurvivalBackColor;
        particles    = 12;
        particleShape = 3;     // 圆形粒子
        particleSpread = 8f;
        coreFrom     = 5f; coreTo = 0.5f;
        coreGlowMul  = 1.4f;
        smokeLayers  = 1;
        smokeFrom    = 2f; smokeTo = 12f;
        alphaMul     = 1f;
    }};
    public static Effect HonorEnergyTail = new BulletTailEffect(){{
        colorFrom    = NuColor.HonorColor;
        colorTo      = NuColor.HonorBackColor;
        particles    = 12;
        particleShape = 3;     // 圆形粒子
        particleSpread = 8f;
        coreFrom     = 5f; coreTo = 0.5f;
        coreGlowMul  = 1.4f;
        smokeLayers  = 1;
        smokeFrom    = 2f; smokeTo = 12f;
        alphaMul     = 1f;
    }};
    public static Effect despEnergyTail = new BulletTailEffect(){{
        colorFrom    = NuColor.DespColor;
        colorTo      = NuColor.DespBackColor;
        particles    = 5;
        particleShape = 0;     // 圆形粒子
        particleSpread = 8f;
        coreFrom     = 5f; coreTo = 0.5f;
        coreGlowMul  = 1.4f;
        smokeLayers  = 1;
        smokeFrom    = 2f; smokeTo = 12f;
        alphaMul     = 1f;
    }};

    /** ② 导弹烟尾（白→灰白浓黑烟 + 尾浪环 + 少量火星，适合 Missile / Bomb / 榴弹） */
    public static Effect missileSmokeTail = new BulletTailEffect(){{
        colorFrom    = Color.valueOf("E8E4D8");
        colorTo      = Color.valueOf("56534B");
        additive     = false;   // 烟不用叠加发光（叠加会亮得像发光粉）
        particles    = 2;
        particleSpread = 3f;
        drawCore     = false;   // 尾焰核心关掉，只要烟
        smokeLayers  = 2;
        smokeFrom    = 2.5f; smokeTo = 9f;
        smokeAlphaMul = 0.45f;
        ringCount    = 1;
        ringStroke   = 1.4f;
        alphaMul     = 1.1f;
    }};
    public static Effect FireTail = new BulletTailEffect(){{
        colorFrom    = NuColor.HonorColor;
        colorTo      = NuColor.HonorBackColor;
        additive     = true;   // 烟不用叠加发光（叠加会亮得像发光粉）
        particles    = 4;
        particleSpread = 9f;
        drawCore     = true;   // 尾焰核心关掉，只要烟
        smokeLayers  = 2;
        smokeFrom    = 2.5f; smokeTo = 9f;
        smokeColor = NuColor.HonorSmokeColor;
        smokeAlphaMul = 0.45f;
        ringCount    = 1;
        ringStroke   = 1.4f;
        alphaMul     = 1.1f;
    }};
    public static Effect DespSmokeTail = new BulletTailEffect(){{
        colorFrom    = NuColor.DespColor;
        colorTo      = NuColor.DespBackColor;
        additive     = true;   // 烟不用叠加发光（叠加会亮得像发光粉）
        particles    = 3;
        particleSpread = 3f;
        drawCore     = true;   // 尾焰核心关掉，只要烟
        smokeLayers  = 2;
        smokeFrom    = 2.5f; smokeTo = 9f;
        smokeAlphaMul = 0.45f;
        ringCount    = 1;
        ringStroke   = 1.4f;
        alphaMul     = 1.1f;
    }};

    /** ③ 狙击强发光尾（金→橙红，只留超亮尾焰点 + 少量火星，适合 Flak / 狙击 / 长射程） */
    public static Effect sniperGlowTail = new BulletTailEffect(){{
        colorFrom    = Color.valueOf("FFD54F");
        colorTo      = Color.valueOf("FF7043");
        particles    = 3;
        particleSpread = 2f;
        coreFrom     = 7f; coreTo = 0.4f;
        coreGlowMul  = 1.6f;
        smokeLayers  = 0;       // 狙击只要一瞬间亮尾，不要烟
        alphaMul     = 1f;
    }};

    // ========================================================
    // 子弹爆炸特效（6 个预设，赋值给 BulletType.hitEffect / despawnEffect 即可）
    // ========================================================

    /** ① 通用小型爆炸（橙红→金，小冲击波 + 8 粒子，适合普通子弹命中） */
    public static expEffect bulletHitSmall = new expEffect(){{
        lifetime    = 18f;
        sizeTo      = 30f;
        strokeFrom  = 2.2f;
        strokeTo    = 0.3f;
        colorFrom   = Color.valueOf("FF6A00");
        colorTo     = Color.valueOf("FFD54F");
        particles   = 8;
        particleSizeFrom = 1.4f;
        particleSizeTo   = 0.2f;
        flyDistanceTo    = 22f;
        lightColor  = Color.valueOf("FF8C00");
        lightScl    = 2f;
        lightOpacity = 0.6f;
    }};

    public static expEffect sailHitSmall = new expEffect(){{
        lifetime    = 15f;
        sizeTo      = 24f;
        strokeFrom  = 2.2f;
        strokeTo    = 0.3f;
        colorFrom   = NuColor.SailColor;
        colorTo     = NuColor.SailBackColor;
        particles   = 4;
        particleSizeFrom = 1.4f;
        particleSizeTo   = 0.2f;
        flyDistanceTo    = 22f;
        lightColor  = NuColor.SailConColor;
        lightScl    = 2f;
        lightOpacity = 0.6f;
    }};
    /** ② 高能等离子爆炸（紫→青蓝，大圈 + 16 粒子，适合能量炮/激光命中） */
    public static expEffect plasmaHit = new expEffect(){{
        lifetime    = 28f;
        sizeTo      = 55f;
        strokeFrom  = 3f;
        strokeTo    = 0.5f;
        colorFrom   = Color.valueOf("A470FF");
        colorTo     = Color.valueOf("7AE7FF");
        particles   = 16;
        particleSizeFrom = 1.8f;
        particleSizeTo   = 0.3f;
        flyDistanceTo    = 40f;
        lightColor  = Color.valueOf("B388FF");
        lightScl    = 3f;
        lightOpacity = 0.8f;
    }};
    public static expEffect sailPlasmaHit = new expEffect(){{
        lifetime    = 20f;
        sizeTo      = 80f;
        strokeFrom  = 3f;
        strokeTo    = 0.5f;
        colorFrom   = NuColor.SailColor;
        colorTo     = NuColor.SailBackColor;
        particles   = 24;
        particleSizeFrom = 4f;
        particleSizeTo   = 1.3f;
        flyDistanceTo    = 40f;
        lightColor  = Color.valueOf("B388FF");
        lightScl    = 3f;
        lightOpacity = 0.8f;
    }};

    /** ③ 燃烧爆破（深红→黑烟，多粒子 + 暗色光照，适合燃烧弹/凝固汽油弹命中） */
    public static expEffect burnHit = new expEffect(){{
        lifetime    = 40f;
        sizeTo      = 38f;
        strokeFrom  = 2.5f;
        strokeTo    = 0.2f;
        colorFrom   = Color.valueOf("D84315");
        colorTo     = Color.valueOf("424242");
        particles   = 20;
        particleSizeFrom = 1.6f;
        particleSizeTo   = 0.15f;
        flyDistanceTo    = 30f;
        lightColor  = Color.valueOf("FF5722");
        lightScl    = 2.5f;
        lightOpacity = 0.5f;
    }};

    /** ④ 电弧爆炸（亮蓝→白，冲击波快速扩散 + 稀疏粒子，适合电击/EMP弹命中） */
    public static expEffect arcHit = new expEffect(){{
        lifetime    = 16f;
        sizeTo      = 42f;
        strokeFrom  = 2.8f;
        strokeTo    = 0.6f;
        colorFrom   = Color.valueOf("29B6F6");
        colorTo     = Color.valueOf("E1F5FE");
        particles   = 6;
        particleSizeFrom = 1.2f;
        particleSizeTo   = 0.1f;
        flyDistanceTo    = 25f;
        lightColor  = Color.valueOf("40C4FF");
        lightScl    = 3.5f;
        lightOpacity = 0.9f;
    }};
    public static expEffect sailArcHit = new expEffect(){{
        lifetime    = 16f;
        sizeTo      = 21f;
        strokeFrom  = 2.8f;
        strokeTo    = 0.6f;
        colorFrom   = NuColor.SailColor;
        colorTo     = NuColor.SailBackColor;
        particles   = 9;
        particleSizeFrom = 2.1f;
        particleSizeTo   = 0.3f;
        flyDistanceTo    = 25f;
        lightColor  = NuColor.SailColor;
        lightScl    = 3.5f;
        lightOpacity = 0.9f;
    }};

    /** ⑤ 重型爆炸（橙→深红，超大圈 + 24 粒子 + 强光照，适合大口径/榴弹/炸弹消亡） */
    public static expEffect heavyBoom = new expEffect(){{
        lifetime    = 45f;
        sizeTo      = 120f;
        strokeFrom  = 5f;
        strokeTo    = 0.8f;
        colorFrom   = Color.valueOf("FF6A00");
        colorTo     = Color.valueOf("B71C1C");
        particles   = 24;
        particleSizeFrom = 2.5f;
        particleSizeTo   = 0.3f;
        flyDistanceTo    = 80f;
        lightColor  = Color.valueOf("FF8C00");
        lightScl    = 4f;
        lightOpacity = 1f;
    }};
    public static expEffect DespBoom = new expEffect(){{
        lifetime    = 25f;
        sizeTo      = 64f;
        strokeFrom  = 5f;
        strokeTo    = 0.8f;
        colorFrom   = NuColor.DespColor;
        colorTo     = NuColor.DespBackColor;
        particles   = 24;
        particleSizeFrom = 2.5f;
        particleSizeTo   = 0.3f;
        flyDistanceTo    = 80f;
        lightColor  = NuColor.BombColor;
        lightScl    = 4f;
        lightOpacity = 1f;
    }};

    /** ⑥ 毒蚀腐蚀爆炸（酸绿→暗绿，小圈 + 密集粒子，适合腐蚀/酸液弹命中） */
    public static expEffect corrodeHit = new expEffect(){{
        lifetime    = 35f;
        sizeTo      = 28f;
        strokeFrom  = 2f;
        strokeTo    = 0.2f;
        colorFrom   = Color.valueOf("76FF03");
        colorTo     = Color.valueOf("33691E");
        particles   = 18;
        particleSizeFrom = 1.3f;
        particleSizeTo   = 0.2f;
        flyDistanceTo    = 20f;
        lightColor  = Color.valueOf("8BC34A");
        lightScl    = 2f;
        lightOpacity = 0.4f;
    }};

    // ========================================================
    // 四芒星命中特效（刀刃命中时从中心发散出四芒星 + 光晕）
    // ========================================================

    /** 四芒星命中特效（从中心向 4 方向发散，自带光晕，适合刀刃命中） */
    public static Effect bladeHitStar = new Effect(25f, 60f, e -> {
        float f = e.fin();
        float alpha = 1f - f;

        // —————— 1) 中心光晕（快速扩散淡出） ——————
        float glowR = 4f + 18f * Interp.pow2Out.apply(f);
        Draw.color(new Color(e.color).a(alpha * 0.4f));
        Fill.circle(e.x, e.y, glowR);

        // —————— 2) 四芒星主体（4 条从中心向外的尖刺） ——————
        // 每条尖刺是一个细长三角形：底在中心、尖向外
        float spikeLen = 10f + 28f * Interp.pow2Out.apply(f);
        float spikeWidth = 3f * (1f - f * 0.5f);

        Draw.color(new Color(e.color).a(alpha));
        // 4 个方向：0°(右)、90°(上)、180°(左)、270°(下)
        // 再叠加 e.rotation 让特效随刀刃方向旋转
        for (int i = 0; i < 4; i++) {
            float baseAng = e.rotation + i * 90f;
            float tipX = e.x + Angles.trnsx(baseAng, spikeLen);
            float tipY = e.y + Angles.trnsy(baseAng, spikeLen);
            // 两侧底点（垂直于尖刺方向偏移 spikeWidth/2）
            float leftX  = e.x + Angles.trnsx(baseAng + 90f, spikeWidth);
            float leftY  = e.y + Angles.trnsy(baseAng + 90f, spikeWidth);
            float rightX = e.x + Angles.trnsx(baseAng - 90f, spikeWidth);
            float rightY = e.y + Angles.trnsy(baseAng - 90f, spikeWidth);

            Fill.tri(
                e.x, e.y,              // 底中心
                tipX, tipY,            // 尖端
                leftX, leftY           // 左侧
            );
            Fill.tri(
                e.x, e.y,              // 底中心
                tipX, tipY,            // 尖端
                rightX, rightY         // 右侧
            );
        }

        // —————— 3) 中心实心亮点 ——————
        Draw.color(new Color(e.color).a(alpha * 0.8f));
        Fill.circle(e.x, e.y, 3f * (1f - f * 0.3f));

        Draw.reset();
    });

    /* ============================================================
     *  ⚡ 闪电系列 7+ 特效（基于 LightningStormEffect 模块化类）
     * ============================================================
     *   模块化开关：useCoreBall / useStorm / useLightning
     *   可以自由组合。以下是典型预设：
     *   ① 完整风暴（光球+风暴+闪电）
     *   ② 单独风暴（只台风，不闪电，单独提取）
     *   ③ 单独光球+闪电（不生成风暴，炮台普通雷击）
     *   ④ 单独闪光球（蓄力视觉）
     *   ⑤ 短程速射雷击（炮台连发）
     *   ⑥ 粗重型雷击（Boss 大招）
     *   ⑦ 紫金雷系（奥术/电磁风格）
     * ============================================================ */

    // ① 电磁爆发：Phase1 粒子+光圈 → Phase2 扫弧填圆环 → Phase3 保持
    //    ⚠ 此 Effect 仅提供纯渲染视觉（光球、光圈、粒子、扫弧圆环）。
    //      不发射任何 LightningBullet。如果需要原版闪电子弹，请用 StormCrafterBlock，
    //      其 Building.updateTile() 会调用 lightningBullet.create(...) 发射真正的 LightningBulletType。
    //    lifetime 300f（Phase1 占 0.25 = 75 tick ≈ 1.25s；Phase2 占 0.3 = 90 tick ≈ 1.5s；
    //             剩余 Phase3 ≈ 135 tick 保持形态，无收回）
    public static Effect lightningStormFull = new LightningStormEffect(300f, 380f, e -> {}){{
        stormColor       = new Color(0x6F9BFFff);
        glowColor        = new Color(0x3F5FFFaa);
        lightningColor   = new Color(0xE3F2FDff);
        sizeMul          = 1f;
        phase1End        = 0.25f;   // Phase1 = 0.25 * 300 = 75 tick
        phase2End        = 0.55f;   // Phase2 = 0.30 * 300 = 90 tick
        coreSize         = 40f;
        innerRingRadius  = 56f;
        innerRingWidth   = 2f;
        outerRadius      = 40f * 8f;  // 40 格 = 320 px
        outerWidth       = 3f;
        outerSweepWidth  = 5f;
        particleBurstCount    = 5;
        particleBurstInterval = 0.08f;
        particleSpeed    = 3.2f;
        particleSizeFrom = 1.5f;
        particleSizeTo   = 7f;
        particleMaxDist  = 240f;
        lightRadius      = 260f;
    }};

    // ② 单独爆发圆环（无粒子，只扫弧填圆环 → 保持）
    public static Effect typhoonCloudOnly = new LightningStormEffect(140f, 340f, e -> {}){{
        useCoreBall  = false;
        useParticles = false;
        useInnerRing = false;
        useOuterArc  = true;
        phase1End    = 0.001f;  // Phase1 立刻跳过
        phase2End    = 0.55f;   // 扫弧在 0~55% 进度内完成 360°
        stormColor   = new Color(0x8FA9D5cc);
        outerRadius  = 180f;
        outerWidth   = 4f;
        outerSweepWidth = 6f;
        lightRadius  = 180f;
        lightColor   = new Color(0x8FA9D588);
    }};

    // ③ 光球+扫弧（蓄能爆发，攻击命中视觉）
    public static Effect lightningStrike = new LightningStormEffect(48f, 260f, e -> {}){{
        useCoreBall  = true;
        useParticles = false;
        useInnerRing = true;
        useOuterArc  = true;
        phase1End    = 0.22f;   // 光球淡入
        phase2End    = 0.75f;   // 扫弧到 75% 处已够明显
        stormColor   = new Color(0xFFD54Fff);
        glowColor    = new Color(0xFF8F00aa);
        lightningColor = Color.white;
        coreSize     = 20f;
        coreGlowMul  = 4f;
        outerRadius  = 140f;
        outerWidth   = 3f;
        lightRadius  = 220f;
        lightColor   = new Color(0xFFE082ff);
    }};

    // ④ 单独蓄能光球（不扫弧不粒子，单纯一个脉动球）
    public static Effect lightningChargeSphere = new LightningStormEffect(36f, 200f, e -> {}){{
        useCoreBall  = true;
        useParticles = false;
        useInnerRing = true;
        useOuterArc  = false;
        phase1End    = 0.20f;
        phase2End    = 1.1f;    // 永不进入 Phase2（保持 Phase1 形态直到结束）
        stormColor   = new Color(0x4FC3F7ff);
        glowColor    = new Color(0x0288D1aa);
        coreGlowMul  = 5.2f;
        coreSize     = 24f;
        innerRingRadius = 34f;
        innerRingWidth  = 2f;
        lightRadius  = 180f;
        lightColor   = new Color(0x4FC3F7cc);
    }};

    // ⑤ 速射命中（短 lifetime，小球 + 细扫弧）
    public static Effect lightningRapidHit = new LightningStormEffect(22f, 180f, e -> {}){{
        useCoreBall  = true;
        useParticles = false;
        useInnerRing = false;
        useOuterArc  = true;
        phase1End    = 0.15f;
        phase2End    = 0.75f;
        stormColor   = new Color(0xB3E5FCff);
        lightningColor = new Color(0xE1F5FEff);
        coreSize     = 12f;
        coreGlowMul  = 3.2f;
        outerRadius  = 100f;
        outerWidth   = 2.2f;
        outerSweepWidth = 3.5f;
        lightRadius  = 120f;
    }};

    // ⑥ 重型雷击（Boss 大招——大放扫弧 + 大光球 + 粒子爆发）
    public static Effect lightningHeavyThunder = new LightningStormEffect(120f, 420f, e -> {}){{
        useCoreBall  = true;
        useParticles = true;
        useInnerRing = true;
        useOuterArc  = true;
        phase1End    = 0.30f;
        phase2End    = 0.70f;
        stormColor   = new Color(0xCE93D8ff);
        glowColor    = new Color(0x4A148Ccc);
        lightningColor = new Color(0xF3E5F5ff);
        coreSize     = 40f;
        coreGlowMul  = 5.5f;
        innerRingRadius = 56f;
        outerRadius  = 300f;
        outerWidth   = 4f;
        outerSweepWidth = 7f;
        particleBurstCount = 8;
        particleBurstInterval = 0.07f;
        particleMaxDist = 260f;
        particleSpeed = 4f;
        lightRadius  = 380f;
        lightColor   = new Color(0xE1BEE7dd);
        sizeMul      = 1.15f;
    }};

    // ⑦ 奥术雷（紫色/粉色，魔法少女系）
    public static Effect lightningArcane = new LightningStormEffect(60f, 260f, e -> {}){{
        useCoreBall  = true;
        useParticles = true;
        useInnerRing = true;
        useOuterArc  = true;
        stormColor   = new Color(0xEA80FCff);
        glowColor    = new Color(0x7B1FA2cc);
        lightningColor = new Color(0xF8BBD0ff);
        phase1End    = 0.20f;
        phase2End    = 0.75f;
        coreSize     = 22f;
        coreGlowMul  = 4.5f;
        outerRadius  = 150f;
        outerWidth   = 2.4f;
        outerSweepWidth = 4.5f;
        particleBurstCount = 5;
        particleBurstInterval = 0.06f;
        particleMaxDist = 130f;
        lightRadius  = 240f;
        lightColor   = new Color(0xEA80FCcc);
    }};

    /* ============================================================
     *  🌪 灰云扫弧（纯爆发云感，无闪电气味，灰+白浓淡扫弧）
     * ============================================================ */
    public static Effect pureTyphoonStorm = new LightningStormEffect(200f, 360f, e -> {}){{
        useCoreBall  = false;
        useParticles = false;
        useInnerRing = false;
        useOuterArc  = true;
        phase1End    = 0.001f;
        phase2End    = 0.60f;
        stormColor   = new Color(0xB0BEC5cc);
        glowColor    = new Color(0x37474Fbb);
        outerRadius  = 210f;
        outerWidth   = 5f;
        outerSweepWidth = 7f;
        sizeMul      = 1.2f;
        lightRadius  = 120f;
        lightColor   = new Color(0xECEFF166);
    }};

    /* ============================================================
     *  🅣 文字跳出弹出特效（基于 TextPopupEffect 模块化类）
     *      直接 at(单位x, 单位y) 即可，自带弹跳+描边+上升+淡出
     * ============================================================ */

    // 默认暴击红字（大弹跳 + 抖动 + 上升）
    public static Effect textCritRed = new TextPopupEffect(60f, 120f, e -> {}){{
        text             = "CRIT!";                  // 可在 at 后单独改
        textColor        = new Color(0xFF5252ff);
        textSize         = 1.6f;
        outlineColor     = Color.black;
        outlineOffset    = 2f;
        riseDistance     = 55f;
        popupPeak        = 1.5f;
        popupTo          = 1.0f;
        useShake         = true;
        shakeAmplitude   = 2f;
        shakeFrequency   = 12f;
        useShadow        = true;
        fadeStart        = 0.6f;
    }};

    // 单位复活橙粉字（抖动+旋转）
    public static Effect textResurrect = new TextPopupEffect(70f, 140f, e -> {}){{
        text             = "复活！";
        textColor        = new Color(0xFF7A59ff);
        textSize         = 1.7f;
        outlineColor     = Color.black;
        outlineOffset    = 2.2f;
        riseDistance     = 60f;
        popupPeak        = 1.4f;
        popupTo          = 1.05f;
        useShake         = true;
        shakeAmplitude   = 1.5f;
        useRotate        = true;
        rotateFrom       = -8f;
        rotateTo         = 2f;
        useShadow        = true;
        fadeStart        = 0.55f;
    }};

    // 治疗绿字（柔和上升，不带抖动）
    public static Effect textHealGreen = new TextPopupEffect(55f, 120f, e -> {}){{
        text             = "+HP";
        textColor        = new Color(0x81C784ff);
        textSize         = 1.3f;
        outlineColor     = Color.black;
        outlineOffset    = 1.6f;
        riseDistance     = 40f;
        popupFrom        = 0.6f;
        popupPeak        = 1.2f;
        popupTo          = 1f;
        useShake         = false;
        useShadow        = true;
        fadeStart        = 0.55f;
        useGravityDrop   = true;
        dropStart        = 0.5f;
        dropDistance     = 6f;
    }};

    // 技能名金色大字（慢速弹入 + 大超射 + 旋转）
    public static Effect textSkillGold = new TextPopupEffect(90f, 160f, e -> {}){{
        text             = "技能发动！";
        textColor        = new Color(0xFFD54Fff);
        textSize         = 2.0f;
        outlineColor     = new Color(0x3E2723cc);
        outlineOffset    = 2.6f;
        riseDistance     = 80f;
        popupEnd         = 0.35f;
        popupFrom        = 0f;
        popupPeak        = 1.6f;
        popupTo          = 1.1f;
        useShake         = true;
        shakeAmplitude   = 1.2f;
        useRotate        = true;
        rotateFrom       = -15f;
        rotateTo         = 0f;
        useShadow        = true;
        fadeStart        = 0.55f;
        shrinkEnd        = 0.1f;
    }};

    // 警告红方标字（超快弹入 + 强抖动）
    public static Effect textWarningRed = new TextPopupEffect(50f, 140f, e -> {}){{
        text             = "警告！";
        textColor        = new Color(0xFF1744ff);
        textSize         = 1.8f;
        outlineColor     = Color.black;
        outlineOffset    = 2.4f;
        riseDistance     = 30f;
        popupEnd         = 0.15f;
        popupFrom        = 0.5f;
        popupPeak        = 1.35f;
        popupTo          = 1f;
        useShake         = true;
        shakeAmplitude   = 3f;
        shakeFrequency   = 18f;
        useShadow        = true;
        fadeStart        = 0.55f;
    }};

    // ========================================================
    // 火焰系列移植（原版 Fx.shootSmallFlame / hitFlameSmall 改色为 PaleColor）
    // ========================================================

    /** 原版 Fx.shootSmallFlame 的 Pale 色（射弹出焰，32f lifetime / 80f clip） */
    public static Effect FlamePale = new Effect(32f, 80f, e -> {
        color(NuColor.PaleColor, NuColor.PaleBackColor, Color.gray, e.fin());

        randLenVectors(e.id, 12, e.finpow() * 60f, e.rotation, 10f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.65f + e.fout() * 1.5f);
        });
    }).followParent(false);
    public static Effect HonorFlame = new Effect(32f, 140f, e -> {
        float s = 160f / 60f; // 视觉大小=160，对应 Layer1: 60f*s=160f
        color(NuColor.HonorColor, NuColor.HonorBackColor, Color.gray, e.fin());
        randLenVectors(e.id, 35, e.finpow() * 60f*s, e.rotation, 10f*s, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 1.2f + e.fout() * 2.5f * s);
        });
        color(NuColor.PaleConColor, NuColor.PaleColor, e.fin());
        randLenVectors(e.id + 1, 14, e.finpow() * 90f, e.rotation, 15f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 2.5f + e.fout() * 5f);
        });
        color(NuColor.HonorConColor, NuColor.HonorColor, e.fin());
        randLenVectors(e.id + 2, 28, e.finpow() * 40f*s, e.rotation, 8f*s, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.8f + e.fout() * 2f * s);
        });
    }).followParent(false);
    /** 原版 Fx.hitFlameSmall 的 Pale 色（命中火焰，14f lifetime） */
    public static Effect hitFlamePale = new Effect(14, e -> {
        color(NuColor.PaleColor, NuColor.PaleBackColor, e.fin());
        stroke(0.5f + e.fout());

        randLenVectors(e.id, 2, 1f + e.fin() * 15f, e.rotation, 50f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            lineAngle(e.x + x, e.y + y, ang, e.fout() * 3 + 1f);
        });
    });
    public static Effect hitFlameHonor = new Effect(20, 60f, e -> {
        float s = 3f;
        color(NuColor.HonorColor, NuColor.HonorBackColor, e.fin());
        stroke(0.5f + e.fout()*s);

        // Layer 1: 主火苗（12条粗线，匹配旋转）
        randLenVectors(e.id, 12, 1f + e.fin() * 15f*s, e.rotation, 50f*s, (x, y) -> {
            float ang = Mathf.angle(x, y);
            lineAngle(e.x + x, e.y + y, ang, e.fout()* 6 + 2f);
        });
        // Layer 2: 亮色粗火星（匹配旋转）
        color(NuColor.PaleConColor, NuColor.PaleColor, e.fin());
        stroke(1.5f + e.fout() * 2f);
        randLenVectors(e.id + 1, 8, 1f + e.fin() * 25f, e.rotation, 70f, (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fout() * 7 + 3f);
        });
        // Layer 3: 近核细火星（密集短线）
        color(NuColor.HonorConColor, NuColor.HonorColor, e.fin());
        stroke(0.8f + e.fout());
        randLenVectors(e.id + 2, 15, 1f + e.fin() * 10f*s, e.rotation, 40f*s, (x, y) -> {
            lineAngle(e.x + x, e.y + y, Mathf.angle(x, y), e.fout() * 3 + 1f);
        });
    });

    // ========================================================
    // 激光/闪电系列移植（原版 Fx.hitLaserBlast / chainLightning 照搬）
    // ========================================================

    /** 原版 Fx.hitLaserBlast（激光命中爆破，12f lifetime） */
    public static Effect BlastHit = new Effect(12, e -> {
        color(e.color);
        stroke(e.fout() * 1.5f);

        randLenVectors(e.id, 8, e.finpow() * 17f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            lineAngle(e.x + x, e.y + y, ang, e.fout() * 4 + 1f);
        });
    });
    public static Effect HonorBlastHit = new Effect(12, e -> {
        color(e.color);
        stroke(e.fout() * 1.5f);

        randLenVectors(e.id, 8, e.finpow() * 17f, (x, y) -> {
            float ang = Mathf.angle(x, y);
            lineAngle(e.x + x, e.y + y, ang, e.fout() * 4 + 1f);
        });
    });
    /**
     *
     */
    public static Effect HollyFire = new Effect(75f, e -> {
        color(NuColor.DivineWrathColor, NuLiquid.divineTears.color, e.fin());
        randLenVectors(e.id, 3, 2f + e.fin() * 7f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.1f + e.fout() * 1.4f);
            Fill.circle(e.x + x, e.y + y, 0.1f + e.fout() * 4f);
        });
    });
    /** 原版 Fx.chainLightning（连锁闪电，20f lifetime / 300f clip，需 at(x,y,rotation,color,Position)）
     *  用法：NuFx.chainLightning.at(x1, y1, 0f, color, new Position(){...目标点...});
     *  或直接传 Building/Unit（它们都实现了 Position） */
    public static Effect HonorChain = new Effect(20f, 300f, e -> {
        if(!(e.data instanceof Position p)) return;
        float tx = p.getX(), ty = p.getY(), dst = Mathf.dst(e.x, e.y, tx, ty);
        Tmp.v1.set(p).sub(e.x, e.y).nor();
        float normx = Tmp.v1.x, normy = Tmp.v1.y;
        float range = 6f;
        int links = Mathf.ceil(dst / range);
        float spacing = dst / links;
        Lines.stroke(2.5f * e.fout());
        Draw.color(NuColor.HonorColor, e.color, e.fin());
        Lines.beginLine();
        Lines.linePoint(e.x, e.y);
        rand.setSeed(e.id);
        for(int i = 0; i < links; i++){
            float nx, ny;
            if(i == links - 1){
                nx = tx;
                ny = ty;
            }else{
                float len = (i + 1) * spacing;
                Tmp.v1.setToRandomDirection(rand).scl(range/2f);
                nx = e.x + normx * len + Tmp.v1.x;
                ny = e.y + normy * len + Tmp.v1.y;
            }
            Lines.linePoint(nx, ny);
        }
        Lines.endLine();
    }).followParent(false).rotWithParent(false);
    public static Effect chainLightning = new Effect(20f, 300f, e -> {
        if(!(e.data instanceof Position p)) return;
        float tx = p.getX(), ty = p.getY(), dst = Mathf.dst(e.x, e.y, tx, ty);
        Tmp.v1.set(p).sub(e.x, e.y).nor();
        float normx = Tmp.v1.x, normy = Tmp.v1.y;
        float range = 6f;
        int links = Mathf.ceil(dst / range);
        float spacing = dst / links;
        Lines.stroke(2.5f * e.fout());
        Draw.color(Color.white, e.color, e.fin());
        Lines.beginLine();
        Lines.linePoint(e.x, e.y);
        rand.setSeed(e.id);
        for(int i = 0; i < links; i++){
            float nx, ny;
            if(i == links - 1){
                nx = tx;
                ny = ty;
            }else{
                float len = (i + 1) * spacing;
                Tmp.v1.setToRandomDirection(rand).scl(range/2f);
                nx = e.x + normx * len + Tmp.v1.x;
                ny = e.y + normy * len + Tmp.v1.y;
            }

            Lines.linePoint(nx, ny);
        }

        Lines.endLine();
    }).followParent(false).rotWithParent(false);

    // ========================================================
    // 轨道炮系列移植（原版 Fx.instTrail / instShoot / instHit 照搬）
    // ========================================================

    /** 原版 Fx.instTrail（轨道炮子弹拖尾，30f lifetime） */
    public static Effect HonorInstTrail = new Effect(30, e -> {
        for(int i = 0; i < 2; i++){
            color(i == 0 ? NuColor.HonorBackColor : NuColor.HonorColor);
            float m = i == 0 ? 1f : 0.5f;
            float rot = e.rotation + 180f;
            float w = 15f * e.fout() * m;
            Drawf.tri(e.x, e.y, w, (30f + Mathf.randomSeedRange(e.id, 15f)) * m, rot);
            Drawf.tri(e.x, e.y, w, 10f * m, rot + 180f);
        }
        Drawf.light(e.x, e.y, 60f, Pal.bulletYellowBack, 0.6f * e.fout());
    });
    public static Effect SailInstTrail = new Effect(30, e -> {
        for(int i = 0; i < 2; i++){
            color(i == 0 ? NuColor.SailBackColor : NuColor.SailColor);
            float m = i == 0 ? 1f : 0.5f;
            float rot = e.rotation + 180f;
            float w = 15f * e.fout() * m;
            Drawf.tri(e.x, e.y, w, (30f + Mathf.randomSeedRange(e.id, 15f)) * m, rot);
            Drawf.tri(e.x, e.y, w, 10f * m, rot + 180f);
        }
        Drawf.light(e.x, e.y, 60f, Pal.bulletYellowBack, 0.6f * e.fout());
    });
    public static Effect EnergyInstTrail = new Effect(30, e -> {
        for(int i = 0; i < 2; i++){
            color(i == 0 ? NuColor.EnergyBackColor : NuColor.EnergyColor);
            float m = i == 0 ? 1f : 0.5f;
            float rot = e.rotation + 180f;
            float w = 15f * e.fout() * m;
            Drawf.tri(e.x, e.y, w, (30f + Mathf.randomSeedRange(e.id, 15f)) * m, rot);
            Drawf.tri(e.x, e.y, w, 10f * m, rot + 180f);
        }
        Drawf.light(e.x, e.y, 60f, Pal.bulletYellowBack, 0.6f * e.fout());
    });
    /** 原版 Fx.instShoot（轨道炮开火，24f lifetime） */
    public static Effect HonorInstShoot = new Effect(24f, e -> {
        e.scaled(10f, b -> {
            color(NuColor.PaleColor, NuColor.HonorBackColor, b.fin());
            stroke(b.fout() * 3f + 0.2f);
            Lines.circle(b.x, b.y, b.fin() * 50f);
        });

        color(NuColor.HonorColor);

        for(int i : Mathf.signs){
            Drawf.tri(e.x, e.y, 13f * e.fout(), 85f, e.rotation + 90f * i);
            Drawf.tri(e.x, e.y, 13f * e.fout(), 50f, e.rotation + 20f * i);
        }

        Drawf.light(e.x, e.y, 180f, Pal.bulletYellowBack, 0.9f * e.fout());
    });
    public static Effect SailInstShoot = new Effect(24f, e -> {
        e.scaled(10f, b -> {
            color(NuColor.SailColor, NuColor.SailBackColor, b.fin());
            stroke(b.fout() * 3f + 0.2f);
            Lines.circle(b.x, b.y, b.fin() * 50f);
        });
        color(NuColor.SailColor);
        for(int i : Mathf.signs){
            Drawf.tri(e.x, e.y, 13f * e.fout(), 85f, e.rotation + 90f * i);
            Drawf.tri(e.x, e.y, 13f * e.fout(), 50f, e.rotation + 20f * i);
        }
        Drawf.light(e.x, e.y, 180f, Pal.bulletYellowBack, 0.9f * e.fout());
    });
    public static Effect EnergyInstShoot = new Effect(24f, e -> {
        e.scaled(10f, b -> {
            color(NuColor.EnergyColor, NuColor.EnergyBackColor, b.fin());
            stroke(b.fout() * 3f + 0.2f);
            Lines.circle(b.x, b.y, b.fin() * 50f);
        });
        color(NuColor.EnergyColor);
        for(int i : Mathf.signs){
            Drawf.tri(e.x, e.y, 13f * e.fout(), 85f, e.rotation + 90f * i);
            Drawf.tri(e.x, e.y, 13f * e.fout(), 50f, e.rotation + 20f * i);
        }
        Drawf.light(e.x, e.y, 180f, Pal.bulletYellowBack, 0.9f * e.fout());
    });
    public static Effect HonorInstBomb = new Effect(15f, 100f, e -> {
        color(NuColor.HonorColor);
        stroke(e.fout() * 4f);
        Lines.circle(e.x, e.y, 4f + e.finpow() * 20f);
        for(int i = 0; i < 4; i++){
            Drawf.tri(e.x, e.y, 6f, 80f * e.fout(), i*90 + 45);
        }
        color();
        for(int i = 0; i < 4; i++){
            Drawf.tri(e.x, e.y, 3f, 30f * e.fout(), i*90 + 45);
        }
        Drawf.light(e.x, e.y, 150f, NuColor.HonorColor, 0.9f * e.fout());
    });
    public static Effect SailInstBomb = new Effect(15f, 100f, e -> {
        color(NuColor.SailColor);
        stroke(e.fout() * 4f);
        Lines.circle(e.x, e.y, 4f + e.finpow() * 20f);
        for(int i = 0; i < 4; i++){
            Drawf.tri(e.x, e.y, 6f, 80f * e.fout(), i*90 + 45);
        }
        color();
        for(int i = 0; i < 4; i++){
            Drawf.tri(e.x, e.y, 3f, 30f * e.fout(), i*90 + 45);
        }
        Drawf.light(e.x, e.y, 150f, NuColor.SailColor, 0.9f * e.fout());
    });
    public static Effect EnergyInstBomb = new Effect(15f, 100f, e -> {
        color(NuColor.EnergyColor);
        stroke(e.fout() * 4f);
        Lines.circle(e.x, e.y, 4f + e.finpow() * 20f);
        for(int i = 0; i < 4; i++){
            Drawf.tri(e.x, e.y, 6f, 80f * e.fout(), i*90 + 45);
        }
        color();
        for(int i = 0; i < 4; i++){
            Drawf.tri(e.x, e.y, 3f, 30f * e.fout(), i*90 + 45);
        }
        Drawf.light(e.x, e.y, 150f, NuColor.EnergyColor, 0.9f * e.fout());
    });

    /** 原版 Fx.instHit（轨道炮命中，20f lifetime / 200f clip） */
    public static Effect HonorInstHit = new Effect(20f, 200f, e -> {
        color(NuColor.HonorColor);
        for(int i = 0; i < 2; i++){
            color(i == 0 ? NuColor.HonorColor : NuColor.HonorBackColor);
            float m = i == 0 ? 1f : 0.5f;
            for(int j = 0; j < 5; j++){
                float rot = e.rotation + Mathf.randomSeedRange(e.id + j, 50f);
                float w = 23f * e.fout() * m;
                Drawf.tri(e.x, e.y, w, (80f + Mathf.randomSeedRange(e.id + j, 40f)) * m, rot);
                Drawf.tri(e.x, e.y, w, 20f * m, rot + 180f);
            }
        }
        e.scaled(10f, c -> {
            color(NuColor.HonorColor);
            stroke(c.fout() * 2f + 0.2f);
            Lines.circle(e.x, e.y, c.fin() * 30f);
        });
        e.scaled(12f, c -> {
            color(Pal.bulletYellowBack);
            randLenVectors(e.id, 25, 5f + e.fin() * 80f, e.rotation, 60f, (x, y) -> {
                Fill.square(e.x + x, e.y + y, c.fout() * 3f, 45f);
            });
        });
    });
    public static Effect SailInstHit = new Effect(20f, 200f, e -> {
        color(NuColor.SailColor);
        for(int i = 0; i < 2; i++){
            color(i == 0 ? NuColor.SailColor : NuColor.SailBackColor);
            float m = i == 0 ? 1f : 0.5f;
            for(int j = 0; j < 5; j++){
                float rot = e.rotation + Mathf.randomSeedRange(e.id + j, 50f);
                float w = 23f * e.fout() * m;
                Drawf.tri(e.x, e.y, w, (80f + Mathf.randomSeedRange(e.id + j, 40f)) * m, rot);
                Drawf.tri(e.x, e.y, w, 20f * m, rot + 180f);
            }
        }
        e.scaled(10f, c -> {
            color(NuColor.SailColor);
            stroke(c.fout() * 2f + 0.2f);
            Lines.circle(e.x, e.y, c.fin() * 30f);
        });
        e.scaled(12f, c -> {
            color(Pal.bulletYellowBack);
            randLenVectors(e.id, 25, 5f + e.fin() * 80f, e.rotation, 60f, (x, y) -> {
                Fill.square(e.x + x, e.y + y, c.fout() * 3f, 45f);
            });
        });
    });
    public static Effect EnergyInstHit = new Effect(20f, 200f, e -> {
        color(NuColor.EnergyColor);
        for(int i = 0; i < 2; i++){
            color(i == 0 ? NuColor.EnergyColor : NuColor.EnergyBackColor);
            float m = i == 0 ? 1f : 0.5f;
            for(int j = 0; j < 5; j++){
                float rot = e.rotation + Mathf.randomSeedRange(e.id + j, 50f);
                float w = 23f * e.fout() * m;
                Drawf.tri(e.x, e.y, w, (80f + Mathf.randomSeedRange(e.id + j, 40f)) * m, rot);
                Drawf.tri(e.x, e.y, w, 20f * m, rot + 180f);
            }
        }
        e.scaled(10f, c -> {
            color(NuColor.EnergyColor);
            stroke(c.fout() * 2f + 0.2f);
            Lines.circle(e.x, e.y, c.fin() * 30f);
        });
        e.scaled(12f, c -> {
            color(Pal.bulletYellowBack);
            randLenVectors(e.id, 25, 5f + e.fin() * 80f, e.rotation, 60f, (x, y) -> {
                Fill.square(e.x + x, e.y + y, c.fout() * 3f, 45f);
            });
        });
    });
    // ========================================================
    // 黑洞坍缩特效：膨胀→环形稳定→加速坍缩消失
    // 阶段 1 (0 ~ 60f)：实心圆不断膨胀至最大半径 maxR
    // 阶段 2 (60f ~ 80f)：出现外环轮廓，粒子以箭头方向飞向外环后消失，
    //              实心圆与外环同步等比收缩
    // 阶段 3 (80f ~ 120f)：收缩速度加快（坍缩加速），最终一起消失
    // ========================================================

    /** 黑洞坍缩特效（重写版），270f lifetime
     *  阶段 1 (0 ~ 60f)：实心圆膨胀至 maxR
     *  阶段 2 (60f ~ 180f)：外环轮廓出现，核心+外环缓慢收缩到 25%，
     *              随机角度粒子从核心向外逸散，越过轮廓继续飞
     *  阶段 3 (180f ~ 270f)：加速收缩到 1%，期间粒子持续逸散，
     *              到达 1% 时整体消失（无淡出） */
    public static Effect blackHoleCollapse = new Effect(270f, 480f, e -> {
        float t = e.time;
        float maxR = 60f;
        float particleSize = 4f;
        float ringWidth = 10f;
        float gap = maxR * 0.6f;
        float outlineMaxR = maxR + gap;
        // ====== 阶段 1：膨胀 0 ~ 60f ======
        float coreR, currentOutlineR;
        if(t <= 60f){
            float s = t / 60f;
            coreR = maxR * s;
            currentOutlineR = 0f;
        }
        // ====== 阶段 2：收缩到 25% (60f ~ 180f) ======
        else if(t <= 180f){
            float s = (t - 60f) / 120f;
            float scale = 1f - 0.75f * s; // 1 -> 0.25
            coreR = maxR * scale;
            currentOutlineR = outlineMaxR * scale;
        }
        // ====== 阶段 3：加速坍缩 180f ~ 270f ======
        else{
            float s = (t - 180f) / 90f;
            float scale = 0.25f * (1f - s * s) + 0.01f * s;
            coreR = maxR * scale;
            currentOutlineR = outlineMaxR * scale;
            if(scale <= 0.015f) return;
        }

        // ---- 核心实心圆 ----
        color(NuColor.DespColor);
        Fill.circle(e.x, e.y, coreR);

        // ---- 外环轮廓（阶段 2 起出现）----
        if(currentOutlineR > 0.1f){
            stroke(ringWidth);
            color(NuColor.DespBackColor);
            Lines.circle(e.x, e.y, currentOutlineR);

            Drawf.light(e.x, e.y, currentOutlineR * 1.3f, NuColor.DespBackColor,
                0.6f * Math.min(currentOutlineR / outlineMaxR, 1f));
        }

        // ====== 阶段 2 + 3：随机角度逸散粒子 ======
        if(t > 60f){
            int pcount = 24;
            float cycleDur = 40f;

            for(int i = 0; i < pcount; i++){
                float birthOffset = (i / (float)pcount) * cycleDur;
                float localT = ((t - 60f) + birthOffset) % cycleDur;
                if(localT < 0f) localT += cycleDur;

                float prog = localT / cycleDur;

                // 随机角度（seed 固定，方向不变）
                float seed = Mathf.randomSeed((long)e.id + i * 131L);
                float angle = seed * Mathf.PI * 2f;

                // 从核心边缘 → 越过轮廓 + gap*2
                float startR = coreR;
                float endR = currentOutlineR + gap * 2f;
                float r = startR + (endR - startR) * prog;

                float px = e.x + Mathf.cos(angle) * r;
                float py = e.y + Mathf.sin(angle) * r;

                // 最后 20% 淡出消失
                float visibleT = 0.8f;
                float alpha;
                if(prog < visibleT){
                    alpha = 1f;
                }else{
                    alpha = 1f - (prog - visibleT) / (1f - visibleT);
                }

                if(alpha > 0.01f){
                    color(NuColor.DespBackColor, alpha);
                    Fill.circle(px, py, particleSize * alpha);
                }
            }
        }
    });

    /* ============================================================
     *  🅤 单位描边爆发特效（OutlineBurstEffect）
     *      将 effect 附加到单位上，会把单位/武器的 outlineRegion 画得更厚、更紫
     *      特效结束时自动触发下一个 Effect（比如爆炸）
     * ============================================================ */

    // —— 静态示例：最常用默认组合（紫色描边 + 结束时放 ExplosionWhite）
    public static Effect outlinePurpleBurst = new OutlineBurstEffect(40f, e -> {}){{
        outlineColor   = new Color(0xAA44FFff);
        outlineFrom    = 1.0f;   // 初始厚度倍数
        outlineTo      = 5.0f;   // 结束厚度倍数
        alphaFrom      = 0.2f;
        alphaTo        = 0.95f;
        useAdditive    = true;
        drawWeapons    = true;
        nextEffect     = NuFx.ExplosionWhite;  // ← 结束后自动接这个
    }};

    // 更柔和的紫色描边（不接任何结束特效）
    public static Effect outlinePurpleSoft = new OutlineBurstEffect(60f, e -> {}){{
        outlineColor   = new Color(0xBB66FFee);
        outlineFrom    = 1.2f;
        outlineTo      = 3.2f;
        alphaFrom      = 0.2f;
        alphaTo        = 0.7f;
        useAdditive    = true;
        drawWeapons    = true;
        nextEffect     = null;
    }};

    // 纯结束触发器（不画任何 outline，只是把父单位延迟 lifetime 后触发 nextEffect）
    public static Effect delayedTrigger = new OutlineBurstEffect(30f, e -> {}){{
        drawOutlines   = false;
        nextEffect     = NuFx.ExplosionNuclear;
    }};
    // 原版 Fx.lava 的 Honor 色版（完整大小，360° 均匀扩散）
    public static Effect lavaHonor = new Effect(40, 100f, e -> {
        color(NuColor.HonorColor, NuColor.HonorBackColor, e.fin());
        randLenVectors(e.id, 12, e.finpow() * 60f, e.rotation, 25f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 1.2f + e.fout() * 2.5f);
        });
    });

    public static Effect PaleLava = new Effect(80, 120f, e -> {
        color(NuColor.PaleColor, NuColor.PaleBackColor, e.fin());
        randLenVectors(e.id, 12, e.finpow() * 60f, e.rotation, 25f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 1.2f + e.fout() * 2.5f);
        });
    });

    // 原版 Fx.lava 的 Honor 色版（缩小 50%，360° 均匀扩散，适合小型方块）
    public static Effect lavaHonorSmall = new Effect(40, 50f, e -> {
        color(NuColor.HonorColor, NuColor.HonorBackColor, e.fin());
        randLenVectors(e.id, 8, e.finpow() * 30f, 0f, 360f, (x, y) -> {
            Fill.circle(e.x + x, e.y + y, 0.8f + e.fout() * 1.5f);
        });
    });

    // 去掉整片 Fill.circle 大圆盘（大量塔同时释放时 overdraw 极高），只保留细线扩散环
    public static Effect healWave = new Effect(360f, e -> {
        float maxRadius = e.data instanceof Float f ? f : 100f;
        float r = e.fin() * maxRadius;
        Draw.color(Pal.heal);
        Draw.alpha(e.fout());
        Lines.stroke(3f);
        Lines.circle(e.x, e.y, r);
        Lines.stroke(2f);
        Lines.circle(e.x, e.y, r * 0.85f);
        Lines.stroke(1f);
        Lines.circle(e.x, e.y, r * 0.7f);
        Draw.alpha(1f);
    });

    public static class HealBurstData {
        public float maxRadius;
        public float yOffset;

        public HealBurstData(float maxRadius, float yOffset) {
            this.maxRadius = maxRadius;
            this.yOffset = yOffset;
        }
    }

    public static Effect healBurst = new Effect(60f, e -> {
        float maxRadius = 100f;
        float yOffset = 20f;
        if (e.data instanceof HealBurstData d) {
            maxRadius = d.maxRadius;
            yOffset = d.yOffset;
        }
        color(Pal.heal, Color.white, e.fin());
        float progress = e.fin();
        float radius = maxRadius;

        float alpha = 1f - progress;
        alpha *= 0.5f + 0.5f * Mathf.sin(progress * 25f);
        Draw.alpha(alpha);

        int sides = 12;
        for (int i = 0; i < sides; i++) {
            float angle1 = i / (float) sides * Mathf.PI2 + progress * 5f;
            float angle2 = (i + 1) / (float) sides * Mathf.PI2 + progress * 5f;
            float r1 = radius * (0.85f + 0.3f * Mathf.sin(i * 2.5f + progress * 20f));
            float r2 = radius * (0.85f + 0.3f * Mathf.sin((i + 1) * 2.5f + progress * 20f));
            Lines.line(
                e.x + Mathf.cos(angle1) * r1,
                e.y + yOffset + Mathf.sin(angle1) * r1,
                e.x + Mathf.cos(angle2) * r2,
                e.y + yOffset + Mathf.sin(angle2) * r2
            );
        }
    });

    /** heal 模式切换特效：绿色脉冲波纹 + 中心治疗十字。
     *  调用：healSwitch.at(x, y); */
    public static Effect healSwitch = new Effect(60f, e -> {
        float p = e.fin();
        float radius = 8f + p * 48f;

        // 外圈脉冲波纹
        Draw.color(Pal.heal);
        Draw.alpha(1f - p);
        Lines.stroke(3f * (1f - p) + 0.5f);
        Lines.circle(e.x, e.y, radius);

        // 内圈脉冲波纹（相位错开，形成脉冲感）
        Draw.alpha((1f - p) * 0.6f);
        Lines.stroke(2f * (1f - p) + 0.3f);
        Lines.circle(e.x, e.y, radius * 0.6f);

        // 中心治疗十字
        float cross = 14f * (1f - p) + 3f;
        float thick = cross * 0.3f;
        Draw.alpha(Mathf.clamp(1f - p * 1.4f));
        Fill.rect(e.x - cross, e.y - thick / 2f, cross * 2f, thick);
        Fill.rect(e.x - thick / 2f, e.y - cross, thick, cross * 2f);

        Drawf.light(e.x, e.y, radius * 2.2f, Pal.heal, 0.6f * (1f - p));
    });

    /** heal 模式开火炮口特效：一圈平滑扩散的绿色光环 + 十字闪光。
     *  调用：healMuzzle.at(x, y, rotation); */
    public static Effect healMuzzle = new Effect(30f, e -> {
        float p = e.fin();
        float radius = 4f + p * 16f;

        Draw.color(Pal.heal);
        Draw.alpha(1f - p);
        Lines.stroke(2.5f * (1f - p) + 0.4f);
        Lines.circle(e.x, e.y, radius);

        float cross = 8f * (1f - p) + 2f;
        float thick = cross * 0.32f;
        Draw.alpha(Mathf.clamp(1f - p * 1.3f));
        Fill.rect(e.x - cross, e.y - thick / 2f, cross * 2f, thick);
        Fill.rect(e.x - thick / 2f, e.y - cross, thick, cross * 2f);

        Drawf.light(e.x, e.y, radius * 3f, Pal.heal, 0.5f * (1f - p));
    });

    /** heal 扫描场上浮治疗光点：绿色小光点缓慢上浮并淡出。
     *  调用：healMote.at(x, y); */
    public static Effect healMote = new Effect(55f, e -> {
        float p = e.fin();
        float rise = p * 20f;
        float a = 1f - p;

        Draw.color(Pal.heal);
        Draw.alpha(a * 0.9f);
        Fill.circle(e.x, e.y + rise, 2.4f * (1f - p * 0.4f));

        Draw.color(Color.white);
        Draw.alpha(a * 0.5f);
        Fill.circle(e.x, e.y + rise, 1.1f * (1f - p * 0.4f));

        Drawf.light(e.x, e.y + rise, 14f, Pal.heal, 0.35f * a);
    });

    private static final Color HEAL_BLUE = Color.valueOf("4FC3F7");
    private static final Color HEAL_GREEN = Color.valueOf("4CAF50");

    public static Effect healRuneCompress = new Effect(90f, e -> {
        float maxRadius = e.data instanceof Float f ? f : 100f;
        Tmp.c1.set(HEAL_BLUE).lerp(HEAL_GREEN, e.fin());
        Draw.color(Tmp.c1);
        float progress = e.fin();
        float radius = maxRadius * (1f - progress);
        float yOffset = 20f;

        Draw.alpha(1f - progress);
        Lines.stroke(2f);

        for (int i = 0; i < 6; i++) {
            float angle1 = i / 6f * Mathf.PI2 + progress * 10f;
            float angle2 = (i + 1) / 6f * Mathf.PI2 + progress * 10f;
            Lines.line(
                e.x + Mathf.cos(angle1) * radius,
                e.y + yOffset + Mathf.sin(angle1) * radius,
                e.x + Mathf.cos(angle2) * radius,
                e.y + yOffset + Mathf.sin(angle2) * radius
            );
        }

        for (int i = 0; i < 6; i++) {
            float angle = i / 6f * Mathf.PI2 + progress * 10f;
            Lines.line(
                e.x + Mathf.cos(angle) * radius * 0.5f,
                e.y + yOffset + Mathf.sin(angle) * radius * 0.5f,
                e.x + Mathf.cos(angle) * radius,
                e.y + yOffset + Mathf.sin(angle) * radius
            );
        }

        Draw.alpha(1f);
        Draw.reset();
    });

    public static Effect healEnergyBeam = new Effect(120f, e -> {
        float maxHeight = e.data instanceof Float f ? f : 200f;
        Tmp.c1.set(HEAL_BLUE).lerp(HEAL_GREEN, e.fin());
        Draw.color(Tmp.c1);
        float progress = e.fin();

        for (int i = 0; i < 8; i++) {
            float angle = Mathf.randomSeed(e.id + i) * Mathf.PI2;
            float speed = Mathf.randomSeed(e.id + i + 100) * 0.5f + 0.5f;
            float height = progress * maxHeight * speed;
            float offset = Mathf.sin(progress * Mathf.PI) * 10f * Mathf.randomSeed(e.id + i + 200);

            Draw.alpha(1f - progress);
            Fill.circle(
                e.x + Mathf.cos(angle) * offset,
                e.y + height,
                2f * (1f - progress)
            );
        }

        Draw.alpha(1f);
        Draw.reset();
    });

    public static Effect healShockwave = new Effect(150f, e -> {
        float maxRadius = e.data instanceof Float f ? f : 100f;
        Tmp.c1.set(HEAL_BLUE).lerp(HEAL_GREEN, e.fin());
        Draw.color(Tmp.c1);
        float progress = e.fin();
        float radius = maxRadius * progress;
        float inv = 1f - progress;

        Draw.alpha(0.22f * inv);
        Fill.circle(e.x, e.y, radius * 0.7f);
        Draw.alpha(0.1f * inv);
        Fill.circle(e.x, e.y, radius * 0.4f);

        Draw.alpha(0.07f * inv);
        Fill.circle(e.x, e.y, radius * 1.15f);

        Draw.alpha(inv);
        Lines.stroke(2f * inv);
        Lines.circle(e.x, e.y, radius);

        Draw.alpha(0.35f * inv);
        Lines.stroke(1f * inv);
        Lines.circle(e.x, e.y, radius * 0.8f);

        for (int i = 0; i < 16; i++) {
            float angle = i / 16f * Mathf.PI2 + progress * 3f;
            float seed = Mathf.randomSeed(e.id + i * 17);
            float dist = radius * (0.15f + seed * 0.85f);
            float len = (5f + seed * 8f) * inv;

            float x1 = e.x + Mathf.cos(angle) * dist;
            float y1 = e.y + Mathf.sin(angle) * dist;
            float x2 = e.x + Mathf.cos(angle) * (dist + len);
            float y2 = e.y + Mathf.sin(angle) * (dist + len);

            Draw.alpha(0.45f * inv);
            Lines.stroke(1.5f * inv);
            Lines.line(x1, y1, x2, y2);
        }

        Draw.alpha(1f);
        Draw.reset();
    });

    public static Effect healEnergyBlade = new Effect(180f, e -> {
        float maxRadius = e.data instanceof Float f ? f : 100f;
        Tmp.c1.set(HEAL_BLUE).lerp(HEAL_GREEN, e.fin());
        Draw.color(Tmp.c1);
        float progress = e.fin();
        float radius = maxRadius * (0.3f + 0.7f * progress);

        for (int i = 0; i < 4; i++) {
            float angle = i / 4f * Mathf.PI2 + progress * 8f;
            float nextAngle = (i + 0.5f) / 4f * Mathf.PI2 + progress * 8f;

            Draw.alpha(1f - progress);
            Lines.stroke(2f);
            Lines.line(
                e.x + Mathf.cos(angle) * radius * 0.3f,
                e.y + Mathf.sin(angle) * radius * 0.3f,
                e.x + Mathf.cos(nextAngle) * radius,
                e.y + Mathf.sin(nextAngle) * radius
            );
        }

        Draw.alpha(1f);
        Draw.reset();
    });

    // ========================================================
    // 黑洞（shader 版）：背景径向扭曲 + 事件视界 + 紫光晕 + 螺旋粒子
    // 触发方式：NuFx.blackHole.at(x, y);
    //
    // 工作原理：
    //   ① 本 Effect 在 Layer.effect 渲染时调 BlackHoleSystem.register(x, y, maxR, strength)
    //      把扭曲参数注册到 BlackHoleShader
    //   ② BlackHoleSystem 在 Trigger.preDraw 把整帧渲染捕获到 FrameBuffer（仅当上一帧有黑洞时）
    //   ③ BlackHoleSystem 在 Trigger.postDraw 用 BlackHoleShader 把 buffer 贴回屏幕
    //   ④ shader 内对每个 fragment 根据距离所有黑洞中心的距离做径向位移 + 中心暗化 + 紫光
    //   ⑤ Effect 本身叠加非扭曲内容（螺旋粒子 + 切线吸积流）在扭曲背景之上
    //
    // 注：因为 preDraw 时本帧还没 Effect.draw，第一帧扭曲不会生效，
    //     从第二帧起扭曲生效（对 90 帧 lifetime 无感知）
    // ========================================================
    public static Effect blackHole = new Effect(90f, 320f, e -> {
        float f = e.fin();        // 0~1 整体进度
        float fout = e.fout();    // 1~0 渐出
        float maxR = 80f;
        float alpha = fout * fout;

        // —— ① 注册全屏扭曲参数到 BlackHoleShader ——
        // 阶段：形成（0~0.25）/ 持续（0.25~0.75）/ 消散（0.75~1）
        float phaseIn = Mathf.clamp(f / 0.25f);
        float phaseOut = f < 0.75f ? 1f : 1f - (f - 0.75f) / 0.25f;
        float strength = phaseIn * phaseOut;
        if(strength > 0.02f){
            BlackHoleSystem.register(e.x, e.y, maxR, strength);
        }

        // —— ①.5 生成 gameplay 实体（引力拉扯 + 撕裂伤害），只在第一帧创建一次 ——
        if(f < 0.01f){
            BlackHoleSystem.spawnGameplay(e.x, e.y, maxR);
        }

        // —— ② 叠加在扭曲背景之上的视觉细节 ——
        // 旋转吸积流（沿切线方向的短弧）
        float rotation = e.time * 3f;
        Draw.color(new Color(1f, 0.8f, 0.4f, alpha));
        Lines.stroke(2f);
        for (int i = 0; i < 8; i++) {
            float a1 = i * 45f + rotation;
            float a2 = a1 + 25f;
            float rr = maxR * 1.4f;
            Lines.line(
                e.x + trnsx(a1, rr), e.y + trnsy(a1, rr),
                e.x + trnsx(a2, rr), e.y + trnsy(a2, rr)
            );
        }

        // 螺旋吸入粒子（从外向中心螺旋接近）
        for (int i = 0; i < 16; i++) {
            float baseA = i * (360f / 16f);
            float spiralA = baseA + e.time * 5f;
            float dist = maxR * 2f * (1f - f) + maxR * 0.3f;
            float px = e.x + trnsx(spiralA, dist);
            float py = e.y + trnsy(spiralA, dist);
            Draw.color(new Color(0.8f, 0.6f, 1f, alpha));
            Fill.circle(px, py, 1.5f * fout);
        }

        Draw.reset();
    }).layer(Layer.effect);

    // ========================================================
    // 挥砍刀光：弧形刀光 + 起点闪光 + 命中粒子
    // 用法：NuFx.slash.at(x, y, rotation);  rotation 为挥砍方向角度（度）
    // ========================================================
    public static Effect slash = new Effect(18f, 80f, e -> {
        float f = e.fin();        // 0~1 进度
        float fout = e.fout();    // 1~0 渐出
        float angle = e.rotation; // 挥砍方向

        // 弧形刀光参数：80° 跨度的弧，长度随进度增长
        int segs = 14;
        float arcSpan = 80f;
        float startAngle = angle - arcSpan / 2f;
        float maxLen = 40f * Interp.pow2Out.apply(f);

        // ① 弧形刀光本体（沿切线方向画一段粗弧）
        Draw.color(new Color(1f, 1f, 1f, fout));
        Lines.stroke(2.5f * fout + 0.5f);
        float prevX = e.x + trnsx(startAngle, 0);
        float prevY = e.y + trnsy(startAngle, 0);
        for (int i = 1; i <= segs; i++) {
            float t = i / (float) segs;
            float a = startAngle + arcSpan * t;
            float r = maxLen * (0.25f + 0.75f * t);  // 内圈短、外圈长，呈扇形
            float nx = e.x + trnsx(a, r);
            float ny = e.y + trnsy(a, r);
            Lines.line(prevX, prevY, nx, ny);
            prevX = nx;
            prevY = ny;
        }

        // ② 起点闪光（刀柄处的小亮点）
        Draw.color(new Color(1f, 1f, 1f, fout * 0.9f));
        Fill.circle(e.x, e.y, 3f * fout);

        // ③ 刀光外圈柔光（白色低 alpha 加粗一遍，制造辉光感）
        Draw.color(new Color(0.8f, 0.9f, 1f, 0.4f * fout));
        Lines.stroke(5f * fout + 1f);
        prevX = e.x + trnsx(startAngle, 0);
        prevY = e.y + trnsy(startAngle, 0);
        for (int i = 1; i <= segs; i++) {
            float t = i / (float) segs;
            float a = startAngle + arcSpan * t;
            float r = maxLen * (0.25f + 0.75f * t);
            float nx = e.x + trnsx(a, r);
            float ny = e.y + trnsy(a, r);
            Lines.line(prevX, prevY, nx, ny);
            prevX = nx;
            prevY = ny;
        }

        // ④ 命中粒子（沿刀光路径飞溅 6 颗）
        Draw.color(new Color(1f, 1f, 1f, fout));
        for (int i = 0; i < 6; i++) {
            float t = (i + 0.5f) / 6f;
            float a = startAngle + arcSpan * t;
            float r = maxLen * (0.25f + 0.75f * t);
            float px = e.x + trnsx(a, r);
            float py = e.y + trnsy(a, r);
            Fill.circle(px, py, 1.5f * fout);
        }

        Draw.reset();
    }).layer(Layer.effect);
}
