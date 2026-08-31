package Npl.newSth.Type;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Angles;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.content.Fx;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;

public class CircleWaveBulletType extends BulletType {

    /** 最大扩散半径 */
    public float maxRadius = 320f;
    /** 扩散速度（像素/秒） */
    public float expandSpeed = 200f;
    /** 环的线宽 */
    public float ringWidth = 8f;
    /** 起始半径 */
    public float startRadius = 10f;

    public CircleWaveBulletType(float damage){
        this.damage = damage;
        this.speed = 0f;
        this.lifetime = 100f;
        this.pierce = true;
        this.pierceBuilding = true;
        this.collidesTiles = true;
        this.collidesAir = true;
        this.collidesGround = true;
        this.collides = false;
        this.hitEffect = Fx.none;
        this.despawnEffect = Fx.none;
        this.shootEffect = Fx.none;
        this.smokeEffect = Fx.none;
        this.hitSize = 0f;
        this.absorbable = false;
        this.hittable = false;
    }

    @Override
    public void init(Bullet b){
        b.data = startRadius;
    }

    @Override
    public void update(Bullet b){
        float currentRadius = (float)b.data;
        currentRadius += expandSpeed * Time.delta;
        if(currentRadius > maxRadius){
            currentRadius = maxRadius;
        }
        b.data = currentRadius;

        // 环的碰撞判定：距离在 [currentRadius - ringWidth, currentRadius + ringWidth] 范围内的敌人
        float innerBound = currentRadius - ringWidth;
        float outerBound = currentRadius + ringWidth;

        // 检测单位碰撞
        Groups.unit.each(u -> {
            if(u.team == b.team || u.dead) return;
            float dist = Mathf.dst(b.x, b.y, u.x, u.y);
            if(dist >= innerBound && dist <= outerBound){
                u.damagePierce(damage);
                if(knockback != 0f){
                    float angle = Angles.angle(b.x, b.y, u.x, u.y);
                    u.vel.add(Angles.trnsx(angle, knockback), Angles.trnsy(angle, knockback));
                }
            }
        });

        // 检测建筑碰撞
        for(Building build : Groups.build){
            if(build.team == b.team || build.dead || !build.isValid()) continue;
            float dist = Mathf.dst(b.x, b.y, build.x, build.y);
            if(dist >= innerBound && dist <= outerBound){
                build.damagePierce(damage);
            }
        }

        // 到达最大半径后计算持续时间
        if(currentRadius >= maxRadius){
            b.time(b.lifetime);
        }
    }

    @Override
    public void draw(Bullet b){
        float currentRadius = (float)b.data;
        float alpha = Mathf.clamp(1f - b.fin() * 0.5f);

        // 绘制外层光晕
        Draw.z(Layer.bullet - 1);
        Draw.color(Pal.lancerLaser, alpha * 0.3f);
        Fill.circle(b.x, b.y, currentRadius + ringWidth);

        // 绘制空心环 - 外层
        Draw.z(Layer.bullet);
        Draw.color(Pal.lancerLaser, alpha);
        Lines.stroke(ringWidth);
        Lines.circle(b.x, b.y, currentRadius);

        // 绘制空心环 - 内层高亮
        Draw.color(Color.white, alpha * 0.8f);
        Lines.stroke(ringWidth * 0.4f);
        Lines.circle(b.x, b.y, currentRadius);

        // 绘制中心点
        Draw.z(Layer.bullet + 1);
        Draw.color(Pal.lancerLaser, alpha);
        Fill.circle(b.x, b.y, 4f);
        Draw.color(Color.white, alpha);
        Fill.circle(b.x, b.y, 2f);

        Draw.reset();
    }

    @Override
    public void hit(Bullet b){}

    @Override
    public void hit(Bullet b, float x, float y){}

    @Override
    public void despawned(Bullet b){
        super.despawned(b);
    }
}
