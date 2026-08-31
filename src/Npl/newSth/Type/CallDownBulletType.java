package Npl.newSth.Type;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.entities.Damage;
import mindustry.entities.Units;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Bullet;
import mindustry.gen.Teamc;
import mindustry.graphics.Layer;

/**
 * CallDownBulletType —— 天降打击弹药（导弹 + 标记 + Lotus 坠落 + 范围伤害）
 *
 * 两阶段状态机（状态存于 b.data，不生成新实体）：
 *   阶段 1（data == null）：导弹飞行
 *     - homingPower 自动追踪范围内最近的敌方单位
 *     - 命中目标（hit）或贴近目标（update 兜底检测）→ 进入阶段 2
 *   阶段 2（data == MarkState）：标记点悬停
 *     - 预警 0 ~ warnTime：白色圆环从 0 扩充到 callDownRadius，后半段圆内填充红色脉冲圆
 *     - warnTime ~ warnTime+fallTime：Lotus 贴图从高空（dropHeight）旋转坠向标记点
 *     - 落地：对 callDownRadius 内敌方单位/建筑造成 callDownDamage 伤害，随后子弹消失
 *
 * 用法：可作为任意武器（炮台 Weapon 或单位 Weapon）的 bullet，
 * 也可放在 ItemTurret 的 ammo(...) 里作为额外弹药。
 */
public class CallDownBulletType extends BulletType {

    /** 落地伤害半径（像素） */
    public float callDownRadius = 48f;
    /** 落地范围伤害 */
    public float callDownDamage = 340f;
    /** 预警时间（秒）：圆环扩充 + 圆填充 */
    public float warnTime = 1.15f;
    /** Lotus 坠落时间（秒） */
    public float fallTime = 0.55f;
    /** Lotus 起始下落高度（像素，相对标记点） */
    public float dropHeight = 340f;
    /** 预警环颜色 */
    public Color warnColor = Color.valueOf("ffd27a");
    /** 预警填充颜色 */
    public Color warnFill = Color.valueOf("ff3b2f");
    /** Lotus 贴图大小（像素） */
    public float lotusSize = 30f;

    /** 标记阶段状态（存于 b.data） */
    private static class MarkState{
        float t = 0f;
        float rot = 0f;
    }

    private static TextureRegion lotusRegion;

    public CallDownBulletType(float speed){
        this.speed = speed;
        this.damage = 0f;
        this.hitSize = 8f;
        // 飞行 + 预警 + 坠落的总时长兜底（帧），落地后子弹会自行 remove
        this.lifetime = 600f;
        this.homingPower = 1.5f;
        this.homingRange = 250f;
        this.pierce = false;
        this.hittable = false;
        this.absorbable = false;
        this.collidesTiles = false;
        this.hitEffect = Fx.none;
        this.despawnEffect = Fx.none;
        this.shootEffect = Fx.none;
        this.smokeEffect = Fx.none;
        this.trailEffect = null;
    }

    @Override
    public void load(){
        super.load();
        lotusRegion = Core.atlas.find("nu-Lotus");
    }

    @Override
    public void init(Bullet b){
        super.init(b);
        b.data = null;
    }

    @Override
    public void hit(Bullet b){
        // 阶段 2 中，忽略碰撞回调
        if(b.data instanceof MarkState) return;
        startMark(b);
    }

    @Override
    public void update(Bullet b){
        if(b.data instanceof MarkState){
            updateMark(b, (MarkState)b.data);
            return;
        }

        // 阶段 1：贴近目标即触发（兜底，防 homing 尾声擦过不打 hit()）
        Teamc target = Units.closestTarget(b.team, b.x, b.y,
                120f,
                u -> !u.dead() && u.isAdded(),
                t -> true);
        if(target != null && Mathf.dst(b.x, b.y, target.getX(), target.getY()) < 14f){
            startMark(b);
            return;
        }
    }

    /** 命中触发：清空速度，切换到标记阶段 */
    private void startMark(Bullet b){
        b.vel.set(0f, 0f);
        b.rotation(0f);
        b.data = new MarkState();
    }

    private void updateMark(Bullet b, MarkState s){
        s.t += Time.delta;
        s.rot += 90f * Time.delta;

        float total = warnFrames() + fallFrames();
        if(s.t >= total){
            // ========== 落地结算 ==========
            Damage.damage(b.team, b.x, b.y, callDownRadius, callDownDamage, true, true);
            Fx.explosion.at(b.x, b.y);
            Fx.shockwave.at(b.x, b.y);
            b.remove();
        }
    }

    @Override
    public void draw(Bullet b){
        if(b.data instanceof MarkState){
            drawMark(b, (MarkState)b.data);
            return;
        }

        // 阶段 1：发光弹体 + 拖尾
        Draw.z(Layer.bullet);
        Draw.color(Color.white, 0.25f);
        Draw.rect(lotusRegion != null && lotusRegion.found() ? lotusRegion : Core.atlas.find("nuclearpowerleak-coin"),
                b.x, b.y, lotusSize * 0.7f, lotusSize * 0.7f, b.rotation());
        Draw.color(warnColor, 0.85f);
        Fill.circle(b.x, b.y, 3.5f);
        Draw.reset();
    }

    private void drawMark(Bullet b, MarkState s){
        float warnF = warnFrames();
        Draw.z(Layer.effect);

        if(s.t < warnF){
            float p = s.t / warnF;
            float ringR = callDownRadius * p;
            // 后半段开始填充圆
            float fillP = Mathf.clamp((p - 0.55f) / 0.45f, 0f, 1f);

            // 填充脉冲圆
            float pulse = 1f + 0.08f * (float)Math.sin(s.t * 0.5f);
            Draw.color(warnFill, (0.12f + 0.35f * p) * pulse);
            Fill.circle(b.x, b.y, callDownRadius * fillP * pulse);

            // 外圈预警环
            Draw.color(warnColor, 0.95f);
            Lines.stroke(2.5f);
            Lines.circle(b.x, b.y, ringR);

            // 环头亮点
            Draw.color(Color.white, 0.9f);
            Fill.circle(b.x + ringR, b.y, 3f);
        }else{
            float fallP = Mathf.clamp((s.t - warnF) / fallFrames(), 0f, 1f);
            // 落地靶心：预警环收缩到中心
            Draw.color(warnColor, 0.6f);
            Lines.stroke(2f);
            Lines.circle(b.x, b.y, callDownRadius * (0.2f + 0.8f * (1f - fallP)));

            // Lotus 从天而降
            float y = b.y + (1f - fallP) * dropHeight;
            float scale = 0.6f + 0.4f * fallP;
            Draw.z(Layer.effect + 1);
            if(lotusRegion != null && lotusRegion.found()){
                Draw.color(Color.white, 1f);
                Draw.alpha(1f);
                Draw.rect(lotusRegion, b.x, y, lotusSize * scale, lotusSize * scale, s.rot);
            }
        }
        Draw.reset();
    }

    private float warnFrames(){ return warnTime * 60f; }
    private float fallFrames(){ return fallTime * 60f; }
}
