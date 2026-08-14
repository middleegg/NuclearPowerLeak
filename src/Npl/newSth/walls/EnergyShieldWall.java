package Npl.newSth.walls;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.meta.*;

import Npl.content.NuFx;

import static mindustry.Vars.*;

/**
 * 能量护盾墙 · 继承原版 Wall（保留原版的闪反、反弹子弹、自动贴图等全部功能）
 * 同时具备电池特性：consumePowerBuffered 储电，outputsPower+consumesPower 让它像电池一样
 * 接线即共享电力（无需电力节点逐个串联）。
 *
 * 三项新增特性：
 *   ① 储电 —— consumePowerBuffered(powerCapacity)，电力由 PowerGraph 自动管理，
 *              邻接的电池/发电机/电力节点会自动组网共享
 *   ② 满电自恢复 —— 储电达到上限后，按 fullPowerRegenPerSec 每秒回复血量
 *   ③ 护盾接口 —— 认 shieldEnabled=false（关），Block 侧改 shieldEnabled=true 即可开启，
 *      半径/边数/颜色/吸收/排斥逻辑完整参考原版 BaseShield
 *
 * =============================================================
 * 【可调参数接口】在 NuBlocks.load() 里 new EnergyShieldWall("xxx"){{ ... }} 时设置
 * =============================================================
 *  // —— 储电 ——
 *  powerCapacity        = 2400f;     // 储电上限（= consumePowerBuffered 的容量）
 *  // —— 满电自恢复血量 ——
 *  fullPowerRegenPerSec = 1f;         // 储电满时每秒回复血量（1=每秒1HP，1:1）
 *  // —— 护盾接口（默认全关，按需打开） ——
 *  shieldEnabled        = false;      // ★ 默认不启动护盾，改成 true 整类启用
 *  shieldRadius         = 110f;       // 护盾半径（像素）
 *  shieldSides          = 24;         // 多边形边数
 *  shieldColor          = null;       // null=用队伍颜色
 */
public class EnergyShieldWall extends Wall {

    /* ==========================================================
     *                  ① 储电参数
     * ========================================================== */
    public float powerCapacity = 2400f;

    /* ==========================================================
     *          ② 满电自恢复血量
     * ========================================================== */
    /** 储电满时每秒回复血量。1=每秒回1HP（1:1 比例，不走 perSecond ×60 转换）*/
    public float fullPowerRegenPerSec = 1f;

    /* ==========================================================
     *          ③ 护盾接口（默认关闭）
     * ========================================================== */
    public boolean shieldEnabled = false;
    public float shieldRadius = 110f;
    public int shieldSides = 24;
    public @Nullable Color shieldColor = null;

    public EnergyShieldWall(String name){
        super(name);
        // —— 电池型储电 + 双向电力节点 ——
        //  hasPower+outputsPower+consumesPower=true 让它成为 PowerGraph 双向参与者
        //  → 邻接的电池/发电机/电力节点自动组网共享，无需电力节点串联
        //  consumePowerBuffered(cap) 自动管理 power.status (0~1) 和储能
        hasPower       = true;
        outputsPower   = true;     // ★ 像电池一样可输出电力
        consumesPower  = true;     // ★ 像电池一样可接受电力
        consumePowerBuffered(powerCapacity);   // 用 powerCapacity 容量初始化 buffered consume
        update         = true;
        drawDisabled   = false;
    }

    @Override
    public void init(){
        super.init();
        // consumePowerBuffered 可能在 init 阶段被 super.init() 处理，再确认一下容量
        if(consPower != null && consPower.capacity != powerCapacity){
            consPower.capacity = powerCapacity;
        }
        if(shieldEnabled) updateClipRadius(shieldRadius);
    }

    @Override
    public void setStats(){
        super.setStats();

        // ① 储电上限（consumePowerBuffered 容量）
        stats.add(Stat.powerCapacity, (int)powerCapacity, StatUnit.powerSecond);

        // ② 满电自恢复血量（1:1 显示，不走 perSecond ×60 转换）
        if(fullPowerRegenPerSec > 0f){
            stats.add(Stat.affinities, Core.bundle.format("nu.stat.regenrate", fullPowerRegenPerSec));
            stats.add(Stat.repairTime, Math.round(health / fullPowerRegenPerSec), StatUnit.seconds); // 满电回满时间
        }

        // ③ 护盾接口（默认关，开启才显示）
        if(shieldEnabled){
            stats.add(Stat.range, shieldRadius / tilesize, StatUnit.blocks);   // 护盾半径
        }
    }

    /* ==========================================================
     *         BaseShield 同款的 bullet/unit 拦截回调
     * ========================================================== */
    protected static EnergyShieldWallBuild paramShieldBuild;
    protected static final Cons<Bullet> shieldBulletConsumer = bullet -> {
        EnergyShieldWallBuild b = paramShieldBuild;
        if(b == null) return;
        if(bullet.team != b.team && bullet.type.absorbable && bullet.within(b, b.shieldRadius())){
            bullet.absorb();
        }
    };
    protected static final Cons<Unit> shieldUnitConsumer = unit -> {
        EnergyShieldWallBuild b = paramShieldBuild;
        if(b == null) return;
        float overlapDst = (unit.hitSize/2f + b.shieldRadius()) - unit.dst(b);
        if(overlapDst > 0){
            if(overlapDst > unit.hitSize * 1.5f){
                unit.kill();
            }else{
                unit.vel.setZero();
                unit.move(Tmp.v1.set(unit).sub(b).setLength(overlapDst + 0.01f));
                if(Mathf.chanceDelta(0.12f * Time.delta)){
                    Fx.circleColorSpark.at(unit.x, unit.y, b.team.color);
                }
            }
        }
    };

    /* ==========================================================
     *                    Building 实现
     * ========================================================== */
    public class EnergyShieldWallBuild extends WallBuild {

        public boolean shieldActive = shieldEnabled;
        public float smoothShieldRadius = 0f;
        public float shieldHit = 0f;

        /* —— 储电量读取（自动从 power.status 取）——
         *   实际储电量 = consPower.capacity × power.status（0~1）
         *   consumePowerBuffered 已自动维护 power.status
         */
        public float storedEnergy(){
            if(consPower == null) return 0f;
            return consPower.capacity * power.status;
        }

        @Override
        public void updateTile(){
            super.updateTile();

            // —— ① 充电由 PowerGraph 自动处理（consumePowerBuffered 内置）——
            // 这里只处理依赖储电的业务逻辑

            // —— ② 满电（≥99.9%）自动回血 ——
            //   fullPowerRegenPerSec=1 → 每秒回1HP（1:1 比例）
            if(fullPowerRegenPerSec > 0f && storedEnergy() >= powerCapacity * 0.999f && health < maxHealth()){
                heal(fullPowerRegenPerSec * Time.delta);
            }

            // —— ③ 护盾拦截 ——
            if(shieldActive && shieldEnabled){
                // 护盾半径随储电比例平滑变化（满电全开，没电缩没）
                smoothShieldRadius = Mathf.lerpDelta(smoothShieldRadius, shieldRadius * power.status, 0.05f);
                float rad = shieldRadius();
                if(rad > 1f){
                    paramShieldBuild = this;
                    Groups.bullet.intersect(x - rad, y - rad, rad*2f, rad*2f, shieldBulletConsumer);
                    Units.nearbyEnemies(team, x, y, rad + 10f, shieldUnitConsumer);
                }
                if(shieldHit > 0f) shieldHit = Mathf.clamp(shieldHit - Time.delta / 10f);
            }else{
                smoothShieldRadius = Mathf.lerpDelta(smoothShieldRadius, 0f, 0.05f);
            }
        }

        public float shieldRadius(){
            return smoothShieldRadius;
        }

        /* ----------------------------------------------------------
         *              渲染：墙本体 + 可选护盾圈
         * ---------------------------------------------------------- */
        @Override
        public void draw(){
            super.draw();
            if(shieldEnabled && shieldActive) drawShield();
        }

        @Override
        public void drawSelect(){
            super.drawSelect();
            if(shieldEnabled) Drawf.dashCircle(x, y, shieldRadius, team.color);
        }

        public void drawShield(){
            float radius = shieldRadius();
            if(radius < 1f) return;

            Draw.z(Layer.shields);
            Draw.color(shieldColor == null ? team.color : shieldColor, Color.white, Mathf.clamp(shieldHit));

            if(renderer.animateShields){
                Fill.poly(x, y, shieldSides, radius);
            }else{
                Lines.stroke(1.5f);
                Draw.alpha(0.09f + Mathf.clamp(0.08f * shieldHit));
                Fill.poly(x, y, shieldSides, radius);
                Draw.alpha(1f);
                Lines.poly(x, y, shieldSides, radius);
                Draw.reset();
            }
            Draw.reset();
        }

        /* ----------------------------------------------------------
         *   面板状态指示器（满电=绿，空电=红，介于之间=黄）
         * ---------------------------------------------------------- */
        @Override
        public BlockStatus status(){
            if(power == null) return BlockStatus.noInput;
            if(power.status >= 0.999f) return BlockStatus.active;      // 满电
            if(power.status <= 0.001f) return BlockStatus.noOutput;    // 空电
            return BlockStatus.noInput;                                  // 充电中
        }

        /** 电池式 warmup（用于贴图发光等效果，0~1）*/
        @Override
        public float warmup(){
            return power == null ? 0f : power.status;
        }

        /* ----------------------------------------------------------
         *              存档读写（护盾状态）
         *   注：power.status 由 Building 自带的 PowerModule 自动序列化，
         *       不需要在这里重复写
         * ---------------------------------------------------------- */
        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.bool(shieldActive);
            write.f(smoothShieldRadius);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1){
                shieldActive = read.bool();
                smoothShieldRadius = read.f();
            }
        }
    }
}
