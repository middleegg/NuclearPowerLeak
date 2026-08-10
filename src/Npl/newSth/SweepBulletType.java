package Npl.newSth;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.entities.bullet.*;
import mindustry.content.Fx;
import Npl.newSth.*;
import Npl.content.*;
/**
 * 扇形扫描场子弹类型
 * <p>
 * 子弹不飞行（speed=0），对着瞄准方向持续生成一个扇形扫描场，
 * 场内敌方单位/建筑每 tick 受到 damage 伤害，友方单位每 tick 受到 heal 治疗。
 * <p>
 * 视觉：填充扇形 + 外弧描边 + 同心弧网格 + 脉冲呼吸效果。
 */
public class SweepBulletType extends BulletType {

    /* ===================== 可配置参数 ===================== */

    /** 扇形总角度（度），如 90 = 以瞄准方向为中心 ±45° 的扇形 */
    public float fieldAngle = 90f;

    /** 扫描半径（像素） */
    public float scanRadius = 80f;

    /** 扫描领域颜色（描边 + 亮线 + 中心点） */
    public Color scanColor = Color.valueOf("4FC3F7");

    /** 扫描领域填充颜色（扇形填充，null = 用 scanColor） */
    public Color fillColor = null;

    /** 友方治疗颜色 */
    public Color healColor = Color.valueOf("66BB6A");

    /** 命中特效（敌人，每 tick 触发） */
    public Effect hitEffect = NuFx.arcHit;

    /** 治疗特效（友方，每 tick 触发） */
    public Effect healEffect = Fx.heal;

    /** 友方每 tick 治疗量 */
    public float heal = 0f;

    /** 是否画同心弧网格（雷达圈线） */
    public boolean drawRadarRings = true;

    /** 同心弧圈数 */
    public int radarRingCount = 3;

    /** 伤害/治疗触发间隔（tick），每隔这么多帧触发一次伤害和治疗。默认 5 */
    public float damageInterval = 5f;

    /** 填充透明度（0~1，越大扇形越亮） */
    public float fillAlpha = 0.15f;

    /** 呼吸脉冲速度（0 = 关闭脉冲，越大闪得越快） */
    public float pulseSpeed = 0f;

    /** 呼吸脉冲幅度（0~1，透明度波动范围） */
    public float pulseMagnitude = 0.1f;

    /** 是否自动瞄准受伤友军（true = 扫描场自动转向血量最低的受伤友军） */
    public boolean autoHealTarget = false;

    /** 友军血量低于多少比例才被锁定（0~1，0.99 = 只要不满血就锁） */
    public float healThreshold = 0.99f;

    /* ===================== 构造 ===================== */

    public SweepBulletType(float damage) {
        super();
        this.damage = damage;
        this.speed = 0f;
        this.lifetime = 60f;          // 默认持续 60 tick
        this.collides = false;
        this.collidesTiles = false;
        this.collidesAir = true;
        this.collidesGround = true;
        this.hitSize = 0f;
        this.despawnHit = false;
        this.absorbable = false;
        this.hittable = false;
        this.keepVelocity = false;
        this.pierce = true;
    }

    /* ===================== 运行时 ===================== */

    @Override
    public void update(Bullet b) {
        // ———— 跟随发射者：子弹位置同步到 owner 单位 ————
        if (b.owner instanceof Unit u && !u.dead) {
            b.set(u.x, u.y);
        }

        // ———— 自动瞄准受伤友军：找到血量最低的受伤友军，扫描场自动转向它 ————
        if (autoHealTarget && heal > 0f) {
            final float[] worstHpRatio = {1f};
            final Unit[] bestTarget = {null};

            try {
                Groups.unit.intersect(b.x - scanRadius, b.y - scanRadius,
                        scanRadius * 2f, scanRadius * 2f, ally -> {
                    if (ally == null || ally.dead) return;
                    if (ally.team != b.team) return;
                    if (b.owner == ally) return;             // 不瞄准自己
                    if (ally.health >= ally.maxHealth * healThreshold) return;  // 满血跳过
                    float ratio = ally.health / ally.maxHealth;
                    if (ratio < worstHpRatio[0]) {
                        worstHpRatio[0] = ratio;
                        bestTarget[0] = ally;
                    }
                });
            } catch (NullPointerException ignored) {}

            if (bestTarget[0] != null) {
                // 扫描场转向受伤友军
                float targetAng = Angles.angle(b.x, b.y, bestTarget[0].x, bestTarget[0].y);
                b.rotation(targetAng);
            } else if (b.owner instanceof Unit u && !u.dead) {
                // 没有受伤友军时，跟随单位朝向
                b.rotation(u.rotation());
            }
        } else if (b.owner instanceof Unit u && !u.dead) {
            b.rotation(u.rotation());   // 非自动治疗模式：跟随单位朝向
        }

        float centerAng = b.rotation();
        float halfAngle = fieldAngle / 2f;

        // ———— 伤害/治疗按 damageInterval 间隔触发，不是每 tick 都触发 ————
        // timer ID 用 2（0 被 updateTrail 占用，1 被 ContinuousBulletType 占用）
        if (!b.timer(2, damageInterval)) return;

        // ———— 检测扫描范围内的所有单位（敌方+友方） ————
        // 用 Groups.unit.intersect 直接遍历所有单位，自己分阵营
        try {
            Groups.unit.intersect(b.x - scanRadius, b.y - scanRadius, scanRadius * 2f, scanRadius * 2f, u -> {
                if (u == null || u.dead) return;

                float dist = Mathf.dst(b.x, b.y, u.x, u.y);
                if (dist > scanRadius) return;

                float unitAng = Angles.angle(b.x, b.y, u.x, u.y);
                if (Angles.angleDist(unitAng, centerAng) > halfAngle) return;

                // 在扇形范围内
                if (u.team == b.team) {
                    // 友方：每 tick 治疗
                    if (heal > 0f) {
                        u.heal(heal);
                        healEffect.at(u.x, u.y, unitAng);
                    }
                } else {
                    // 敌方：每 tick 伤害
                    u.damage(damage);
                    hitEffect.at(u.x, u.y, unitAng);
                }
            });
        } catch (NullPointerException ignored) {}

        // ———— 检测扫描范围内的敌方建筑 ————
        try {
            Groups.build.intersect(b.x - scanRadius, b.y - scanRadius, scanRadius * 2f, scanRadius * 2f, build -> {
                if (build == null || build.team == b.team) return;

                float dist = Mathf.dst(b.x, b.y, build.x, build.y);
                if (dist > scanRadius) return;

                float buildAng = Angles.angle(b.x, b.y, build.x, build.y);
                if (Angles.angleDist(buildAng, centerAng) > halfAngle) return;

                build.damage(damage);
                hitEffect.at(build.x, build.y, buildAng);
            });
        } catch (NullPointerException ignored) {}
    }

    /* ===================== 渲染：固定扇形扫描场 ===================== */

    @Override
    public void draw(Bullet b) {
        float centerAng = b.rotation();
        float halfAngle = fieldAngle / 2f;
        float alpha = Mathf.clamp(1f - b.fin() * 0.3f);
        Color fc = fillColor != null ? fillColor : scanColor;

        // 呼吸脉冲
        float pulse = 0f;
        if (pulseSpeed > 0f) {
            pulse = Mathf.sin(Time.time * pulseSpeed) * pulseMagnitude;
        }
        float pa = Mathf.clamp(fillAlpha + pulse, 0.01f, 1f);

        float startAng = centerAng - halfAngle;
        float endAng = centerAng + halfAngle;

        // ———— 1) 同心弧网格 ————
        if (drawRadarRings) {
            for (int i = 1; i <= radarRingCount; i++) {
                float r = scanRadius * (i / (float) (radarRingCount + 1));
                if (r > 1f) {
                    // 只画扇形范围内的弧线段
                    int segs = 12;
                    Lines.stroke(1f, new Color(scanColor).a(alpha * 0.15f));
                    for (int j = 0; j < segs; j++) {
                        float a1 = startAng + (endAng - startAng) * (j / (float) segs);
                        float a2 = startAng + (endAng - startAng) * ((j + 1) / (float) segs);
                        float x1 = b.x + Angles.trnsx(a1, r);
                        float y1 = b.y + Angles.trnsy(a1, r);
                        float x2 = b.x + Angles.trnsx(a2, r);
                        float y2 = b.y + Angles.trnsy(a2, r);
                        Lines.line(x1, y1, x2, y2);
                    }
                }
            }
        }

        // ———— 2) 填充扇形（从中心向两侧渐淡） ————
        int fillSegs = 16;
        for (int i = 0; i < fillSegs; i++) {
            float t0 = i / (float) fillSegs;
            float t1 = (i + 1) / (float) fillSegs;
            float a1 = startAng + (endAng - startAng) * t0;
            float a2 = startAng + (endAng - startAng) * t1;
            // 越靠近中心越亮
            float centerT = (t0 + t1) * 0.5f;
            float distFromCenter = Math.abs(centerT - 0.5f) * 2f;  // 0=中心, 1=边缘
            float segAlpha = alpha * pa * (1f - distFromCenter * 0.5f);

            Draw.color(new Color(fc).a(segAlpha));
            Fill.quad(
                b.x, b.y,
                b.x + Angles.trnsx(a1, scanRadius), b.y + Angles.trnsy(a1, scanRadius),
                b.x + Angles.trnsx(a2, scanRadius), b.y + Angles.trnsy(a2, scanRadius),
                b.x, b.y
            );
        }

        // ———— 3) 外弧描边（扇形最外圈的弧线） ————
        int arcSegs = 16;
        Lines.stroke(2f, new Color(scanColor).a(alpha * 0.6f));
        for (int i = 0; i < arcSegs; i++) {
            float a1 = startAng + (endAng - startAng) * (i / (float) arcSegs);
            float a2 = startAng + (endAng - startAng) * ((i + 1) / (float) arcSegs);
            float x1 = b.x + Angles.trnsx(a1, scanRadius);
            float y1 = b.y + Angles.trnsy(a1, scanRadius);
            float x2 = b.x + Angles.trnsx(a2, scanRadius);
            float y2 = b.y + Angles.trnsy(a2, scanRadius);
            Lines.line(x1, y1, x2, y2);
        }

        // ———— 4) 两侧边线（从中心到外圈） ————
        Lines.stroke(1.5f, new Color(scanColor).a(alpha * 0.5f));
        Lines.line(b.x, b.y, b.x + Angles.trnsx(startAng, scanRadius), b.y + Angles.trnsy(startAng, scanRadius));
        Lines.line(b.x, b.y, b.x + Angles.trnsx(endAng, scanRadius), b.y + Angles.trnsy(endAng, scanRadius));

        // ———— 5) 中心瞄准线（最亮，指示扫描方向） ————
        Lines.stroke(2f, new Color(scanColor).a(alpha * 0.9f));
        Lines.line(b.x, b.y, b.x + Angles.trnsx(centerAng, scanRadius), b.y + Angles.trnsy(centerAng, scanRadius));

        // ———— 6) 扫描中心点 ————
        Draw.color(new Color(scanColor).a(alpha * 0.6f));
        Fill.circle(b.x, b.y, 4f);
        Draw.color(Color.white.cpy().a(alpha * 0.8f));
        Fill.circle(b.x, b.y, 2f);

        Draw.reset();
    }

    @Override
    public void hit(Bullet b) {}

    @Override
    public void hit(Bullet b, float x, float y) {}

    @Override
    public void despawned(Bullet b) {}
}
