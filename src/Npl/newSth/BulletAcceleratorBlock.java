package Npl.newSth;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.Bar;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * 子弹加速器方块。
 * <p>
 * 功能概述：
 *   1. 自身不会阻挡子弹（underBullets = true，子弹从方块上方/下方直线穿过）
 *   2. 子弹进入方块范围 → 施加一个永久速度加成（沿子弹当前方向叠加一个固定速度标量）
 *   3. "永久"定义：从被加速那一刻起，直到子弹消失，始终拥有这个速度加成。
 *      实现方式：
 *        a) 立刻给 vel 增加一个沿飞行方向的速度 boostSpeed
 *        b) 在 bullet.data 上挂一个 AcceleratedTag 记录"已经被本方块加速过了 + 原始speed/drag已被覆盖"，
 *           避免同一个子弹被同一个加速器重复加速（被多个不同加速器加速是允许的）。
 * <p>
 * 可配置字段（Block级别，类似 OverdriveProjector 的 speedBoost 等）：
 *   range             : 检测范围（以方块为中心的圆，像素），默认 = size*tilesize/2（方块本身大小）
 *   boostSpeed        : 永久叠加的速度标量（像素/tick），默认 2.0
 *   affectEnemy       : 是否给敌方子弹也加速（默认 false，只加速我方）
 *   boostColor        : 加速特效/光圈颜色
 *   consumePowerPerTick : 每 tick 消耗的功率（和 OverdriveProjector 类似的供电驱动逻辑）
 *
 * @author mod
 */
public class BulletAcceleratorBlock extends Block {

    /* ===================== 可配置参数 ===================== */

    /** 子弹进入被加速的检测半径（像素）。默认覆盖方块本身尺寸 */
    public float range = -1f;

    /** 单次叠加的永久速度标量（像素/tick）。沿子弹当前飞行方向叠加 */
    public float boostSpeed = 2.0f;

    /** 是否同时加速敌方阵营子弹（默认只加速我方/友方） */
    public boolean affectEnemy = false;

    /** 加速/显示光圈颜色 */
    public Color baseColor = new Color(0x64B5F6ff);
    public Color phaseColor = new Color(0xBBDEFBff);

    public boolean hasBoost = true;

    /** OverdriveProjector 风格：每 useTime tick 消耗一批物品做 phase 增益 */
    public float useTime = 400f;
    /** phase 物品存在时，速度加成的额外倍率增量（额外加在 boostSpeed 上的系数） */
    public float phaseBoostMul = 0.5f;
    public float phaseRangeBoost = 20f;

    /* ===================== 构造 ===================== */

    public BulletAcceleratorBlock(String name){
        super(name);
        solid = true;            // 玩家/单位会碰撞（走路会绕开）
        update = true;           // 每 tick 调 updateTile
        group = BlockGroup.projectors;
        hasItems = true;
        hasPower = true;          // 电力驱动（具体功耗在实例化时用 consumePower(...) 设置）
        canOverdrive = false;    // 不接受别的 OverdriveProjector 加速
        emitLight = true;
        lightRadius = 50f;
        envEnabled |= Env.space;
        // ★ 关键：子弹不会与此方块发生 tile 碰撞，直线穿过
        underBullets = true;
    }

    @Override
    public void load(){
        super.load();
    }

    @Override
    public void init(){
        super.init();
        if(range < 0) range = size * tilesize / 2f + 4f;
    }

    @Override
    public void setStats(){
        stats.timePeriod = useTime;
        super.setStats();

        stats.add(Stat.speedIncrease, "+" + Mathf.round(boostSpeed * 60f) + "/s");
        stats.add(Stat.range, range / tilesize, StatUnit.blocks);
        stats.add(Stat.productionTime, useTime / 60f, StatUnit.seconds);

        if(findConsumer(f -> f instanceof ConsumeItems) instanceof ConsumeItems items){
            stats.remove(Stat.booster);
            stats.add(Stat.booster, StatValues.itemBoosters(
                "+{0}% speed", stats.timePeriod,
                phaseBoostMul * 100f, phaseRangeBoost, items.items));
        }
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("boost", (BulletAcceleratorBuild e) -> new Bar(
            () -> Core.bundle.format("bar.boost",
                Mathf.round(Math.max(e.realBoostSpeed() * 60f, 0f)) + "/s"),
            () -> Pal.accent,
            () -> Mathf.clamp(e.realBoostSpeed() /
                (hasBoost ? boostSpeed + boostSpeed * phaseBoostMul : boostSpeed), 0f, 1f)
        ));
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, baseColor);
    }

    /* ====================================================================== */
    /*                           Building 内部类                               */
    /* ====================================================================== */

    public class BulletAcceleratorBuild extends Building {

        public float heat, charge = Mathf.random(60f), phaseHeat, smoothEfficiency, useProgress;
        /** 本加速器曾经加速过的子弹 id 集合（避免同一子弹被反复加速） */
        public transient IntSet boostedBullets = new IntSet();

        public float realBoostSpeed(){
            return (boostSpeed + phaseHeat * boostSpeed * phaseBoostMul) * efficiency;
        }

        public float realRange(){
            return range + phaseHeat * phaseRangeBoost;
        }

        @Override
        public void drawLight(){
            Drawf.light(x, y, lightRadius * smoothEfficiency, baseColor, 0.7f * smoothEfficiency);
        }

        @Override
        public void updateTile(){
            smoothEfficiency = Mathf.lerpDelta(smoothEfficiency, efficiency, 0.08f);
            heat = Mathf.lerpDelta(heat, efficiency > 0 ? 1f : 0f, 0.08f);

            if(hasBoost){
                phaseHeat = Mathf.lerpDelta(phaseHeat, optionalEfficiency, 0.1f);
            }

            if(efficiency > 0){
                useProgress += delta();
            }
            if(useProgress >= useTime){
                consume();
                useProgress %= useTime;
            }

            // —— 加速检测：只在有效工作（有电）时才加速
            if(efficiency > 0f && enabled){
                processBullets();
            }
        }

        /**
         * 检测范围内所有子弹，若尚未被本加速器加速过则加速。
         */
        private void processBullets(){
            float r = realRange();
            float r2 = r * r;
            float bs = realBoostSpeed();

            Groups.bullet.intersect(x - r, y - r, r * 2f, r * 2f, b -> {
                if(b == null) return;

                // 阵营过滤
                if(!affectEnemy && b.team != team) return;

                // 距离过滤
                if(Mathf.dst2(b.x, b.y, x, y) > r2) return;

                // 避免重复加速（同一个子弹被同一个加速器只加一次）
                if(boostedBullets.contains(b.id())) return;
                boostedBullets.add(b.id());

                // 沿子弹当前方向叠加一个速度
                Vec2 v = b.vel;
                float len = v.len();
                if(len > 0.001f){
                    float nx = v.x / len;
                    float ny = v.y / len;
                    v.x += nx * bs;
                    v.y += ny * bs;
                }else{
                    // 速度为 0 的子弹，根据其 rotation 方向加
                    float ang = b.rotation() * Mathf.degRad;
                    v.x += Mathf.cos(ang) * bs;
                    v.y += Mathf.sin(ang) * bs;
                }

                // 给子弹打 tag：记录累计被加速了多少（供 SpeedDamageBulletType 做抗衰减补偿）
                tagAccelerated(b, bs);

                // 触发一个小特效
                if(!headless){
                    mindustry.content.Fx.hitBulletSmall.at(b.x, b.y, 0f, baseColor);
                }
            });
        }

        /**
         * 在 bullet.data 上写一个标志位。
         * 如果 bullet.data == null 或不是 tag，则新建一个。
         * <p>
         * 注：不修改 bullet.type.drag（因为 BulletType 是同类型子弹共享的单例）。
         * "永久速度"的抗衰减逻辑由 SpeedDamageBulletType.update() 检测该 tag 后执行。
         * 对于非自定义的 BulletType，至少保证 boost 一次性叠加，之后遵循原 type 的 drag。
         */
        private void tagAccelerated(Bullet b, float addedSpeed){
            Object d = b.data;
            AcceleratedTag tag;
            if(d instanceof AcceleratedTag t){
                tag = t;
            }else{
                tag = new AcceleratedTag();
                tag.originalDrag = b.type.drag;
                tag.originalBaseSpeed = b.type.speed;
                tag.originalData = d;
                b.data = tag;
            }
            tag.times++;
            tag.boostSum += addedSpeed;
        }

        @Override
        public void drawSelect(){
            float r = realRange();
            Drawf.dashCircle(x, y, r, baseColor);
        }

        @Override
        public void draw(){
            super.draw();
            if(!Lod.l2) return;

            float f = 1f - (Time.time / 100f) % 1f;

            Draw.color(baseColor, phaseColor, phaseHeat);
            Draw.alpha(heat * Mathf.absin(Time.time, 50f / Mathf.PI2, 1f) * 0.5f * Lod.alpha2);
            Draw.rect(region, x, y);
            Draw.alpha(Lod.alpha2);
            Lines.stroke((2f * f + 0.1f) * heat);

            float r = Math.max(0f, Mathf.clamp(2f - f * 2f) * size * tilesize / 2f - f - 0.2f);
            float w = Mathf.clamp(0.5f - f) * size * tilesize;
            Lines.beginLine();
            for(int i = 0; i < 4; i++){
                Lines.linePoint(
                    x + Geometry.d4(i).x * r + Geometry.d4(i).y * w,
                    y + Geometry.d4(i).y * r - Geometry.d4(i).x * w);
                if(f < 0.5f) Lines.linePoint(
                    x + Geometry.d4(i).x * r - Geometry.d4(i).y * w,
                    y + Geometry.d4(i).y * r + Geometry.d4(i).x * w);
            }
            Lines.endLine(true);

            Draw.reset();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(heat);
            write.f(phaseHeat);
            write.f(useProgress);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            heat = read.f();
            phaseHeat = read.f();
            useProgress = read.f();
        }
    }

    /* ====================================================================== */
    /*                            子弹 data 标签                               */
    /* ====================================================================== */

    /**
     * 挂在 bullet.data 上的标签。
     * 如果子弹的 data 是这个类的实例，则说明它已经被至少一个 BulletAcceleratorBlock 加速过。
     */
    public static class AcceleratedTag {
        /** 原始 bullet.type.drag（供参考，目前未直接做还原） */
        public float originalDrag = 0f;
        /** 被加速时记录的原始子弹基础速度（type.speed 默认值） */
        public float originalBaseSpeed = 1f;
        /** 原来的 bullet.data，避免覆盖掉使用者原本放在 data 上的其他数据 */
        public Object originalData = null;
        /** 被加速次数 */
        public int times = 0;
        /** 累计被叠加的速度标量（像素/tick）。用于做"永久"抗衰减补偿 */
        public float boostSum = 0f;
    }
}
