package Npl.newSth;

import arc.graphics.Color;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.ui.*;
import mindustry.content.*;
import mindustry.type.StatusEffect;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.meta.*;
import Npl.content.*;

import static mindustry.Vars.*;

public class GraduallyFasterTurret extends PowerTurret {

    // ⭐ 配置参数
    public float bloodyStart = 1f;              // 初始攻速倍率（100%）
    public float bloodyPerShot = 0.25f;         // 每次攻击+25%
    public float bloodyMax = 5f;                // 最大攻速倍率（500%）
    public float weaky = 0.3f;                  // 过热期间每秒装填减少量
    public float overheatDuration = 15 * 60f;   // 过热持续15秒
    public float cooldownDuration = 12 * 60f;   // 过热后冷却12秒
    public float overheatDamage = 0.5f;         // 过热期间每帧伤害
    public float idleDecayDelay = 60 * 2.5f;    // 不攻击多久后开始衰减
    public float idleDecaySpeed = 0.01f;        // 衰减速度
    public Effect overHeatedEffect = Fx.fire;

    // ⭐ 流血状态：作为本类的静态字段（不需要单独文件）
    public static StatusEffect bleeding = new StatusEffect("npl-bleeding") {{
        speedMultiplier = 0.85f;                // 基础减速（15%）
        damage = 3f;                            // 基础 DOT（3点/秒）
        effect = Fx.hitBulletSmall;             // 持续播放的视觉特效
        effectChance = 0.15f;                   // 每帧触发的概率
        color = Color.valueOf("cc2222");        // 状态图标颜色
        // 注意：StatusEffect 的 damage 会穿过大部分防御机制，
        // 是直接的持续伤害，符合“穿甲 DOT”的需求。
    }};

    public GraduallyFasterTurret(String name) {
        super(name);
        reload = 60f;

        // ⭐ 配置子弹：命中施加流血
        shootType = new BasicBulletType(4f, 20f) {{
            width = 8f;
            height = 12f;
            lifetime = 60f;
            speed = 4f;
            damage = 20f;

            // 命中时施加流血状态
            status = GraduallyFasterTurret.bleeding;
            statusDuration = 60f * 3f;          // 默认3秒（后续会动态改）
            hitEffect = Fx.hitBulletBig;
            shootEffect = Fx.shootBig;
        }};
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(ModStats.bloody, StatValues.number(bloodyPerShot * 100f, StatUnit.percent));
        stats.add(ModStats.weaky, StatValues.number(weaky * 100f, StatUnit.percent));
        stats.add(ModStats.ticksW, overheatDuration / 60f, StatUnit.seconds);
    }

    @Override
    public void setBars() {
        super.setBars();

        // 叠速条
        addBar("add-ReloadSpeed", (GraduallyFasterTurretBuild e) -> new Bar(
                () -> "叠速 " + (int)((e.bloody - bloodyStart) / (bloodyMax - bloodyStart) * 100f) + "%",
                () -> NuColor.PaleColor,
                () -> (e.bloody - bloodyStart) / (bloodyMax - bloodyStart)
        ));

        // 过热条
        addBar("overHeated", (GraduallyFasterTurretBuild e) -> new Bar(
                () -> "过热",
                () -> NuColor.BloodColor,
                () -> e.overHeated ? e.overHeatTimer / overheatDuration : 0f
        ));

        // 冷却条
        addBar("coolingDown", (GraduallyFasterTurretBuild e) -> new Bar(
                () -> "冷却中",
                () -> NuColor.SurvivalColor,
                () -> e.coolingDown ? e.cooldownTimer / cooldownDuration : 0f
        ));
    }

    public class GraduallyFasterTurretBuild extends PowerTurretBuild {

        // ⭐ 运行时状态
        public float bloody = bloodyStart;
        public boolean overHeated = false;
        public float overHeatTimer = 0f;
        public float maxReload = 12f;
        public float anyTimer = 0f;
        public boolean coolingDown = false;
        public float cooldownTimer = 0f;

        @Override
        public float baseReloadSpeed(){
            if (overHeated) {
                return 60f / maxReload;
            }
            return bloody;
        }

        @Override
        public void updateTile() {
            if (overHeated) {
                overHeatTimer -= delta();
                if (coolant == null || liquids.currentAmount() <= 0.001f) {
                    overHeatedEffect.at(x + Mathf.range(size * tilesize / 2f), y + Mathf.range(size * tilesize / 2f));
                    damageContinuous(overheatDamage * delta());
                }
                maxReload = Math.max(1f, maxReload - weaky * delta() / 60f);
                if (overHeatTimer <= 0f) {
                    overHeated = false;
                    bloody = bloodyStart;
                    maxReload = 12f;
                    coolingDown = true;
                    cooldownTimer = cooldownDuration;
                }
            } else if (coolingDown) {
                cooldownTimer -= delta();
                if (cooldownTimer <= 0f) {
                    coolingDown = false;
                    cooldownTimer = 0f;
                }
            } else {
                anyTimer += delta();
                if (anyTimer >= idleDecayDelay) {
                    bloody = Math.max(bloodyStart, Mathf.lerpDelta(bloody, bloodyStart, idleDecaySpeed));
                    anyTimer = 0f;
                }
            }
            super.updateTile();
        }

        @Override
        protected void shoot(BulletType type) {
            if (coolingDown) return;

            super.shoot(type);

            if (!overHeated) {
                bloody = Math.min(bloodyMax, bloody + bloodyPerShot);

                // ⭐ 根据 bloody 动态调整流血强度
                // intensity: 0（刚叠）~ 1（满叠）
                float intensity = (bloody - bloodyStart) / (bloodyMax - bloodyStart);

                // DOT 伤害：3 ~ 9 点/秒
                bleeding.damage = 3f + 6f * intensity;

                // 减速：0.85 ~ 0.65（越叠越慢）
                bleeding.speedMultiplier = 0.85f - 0.2f * intensity;

                // 持续时间：2 ~ 5 秒
                if (shootType instanceof BasicBulletType b) {
                    b.statusDuration = 60f * (2f + 3f * intensity);
                }

                // 达到 500% → 进入过热
                if (bloody >= bloodyMax) {
                    overHeated = true;
                    overHeatTimer = overheatDuration;
                    maxReload = 12f;

                    // 过热瞬间给一发强化流血（可选）
                    bleeding.damage = 15f;
                    bleeding.speedMultiplier = 0.5f;
                }
            }
            anyTimer = 0f;
        }

        @Override
        public boolean hasAmmo() {
            if (coolingDown) return false;
            return super.hasAmmo();
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.f(bloody);
            write.bool(overHeated);
            write.f(overHeatTimer);
            write.f(maxReload);
            write.bool(coolingDown);
            write.f(cooldownTimer);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            bloody = read.f();
            overHeated = read.bool();
            overHeatTimer = read.f();
            maxReload = read.f();
            if (revision >= 1) {
                coolingDown = read.bool();
                cooldownTimer = read.f();
            }
        }
    }
}