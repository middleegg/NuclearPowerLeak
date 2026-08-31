package Npl.newSth;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.game.Team;
import mindustry.entities.Damage;
import mindustry.content.*;
import mindustry.entities.abilities.Ability;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

/**
 * ShockwaveAbility —— 给单位装备冲击波+毒泡泡能力
 * 每隔 cooldown 秒自动释放一次：
 *   1. 从单位位置向外扩散的圆环，波前经过的敌方单位/建筑受伤害（代码杀）
 *   2. 在范围内生成随机数量治疗泡泡，持续数十秒，治疗友方单位
 *   3. fullMap 模式：扩散至覆盖地图四角的最大距离
 */
public class ShockwaveAbility extends Ability{

    /** 冲击波最大半径（普通模式） */
    public float maxRadius = 320f;
    /** 扩散速度（像素/秒，普通模式） */
    public float expandSpeed = 400f;
    /** 环宽（碰撞判定宽度） */
    public float ringWidth = 16f;

    public boolean fullMap = false;
    /** 冲击波伤害 */
    public float shockwaveDamage = 145f;
    /** 自动释放冷却（秒） */
    public float cooldown = 90f;
    /** 毒泡泡最大数量 */
    public int maxBubbles = 6;
    /** 泡泡半径 */
    public float bubbleRadius = 16f;
    /** 泡泡持续时间（秒） */
    public float bubbleDuration = 25f;
    /** 泡泡每秒治疗量 */
    public float bubbleHealPerSec = 12f;
    /** 主环颜色 */
    public Color waveColor = Color.valueOf("ff4444");
    /** 泡泡颜色（治疗色） */
    public Color bubbleColor = Color.valueOf("4dffaa");

    public ShockwaveAbility(float cooldown, float maxRadius, float shockwaveDamage, int maxBubbles, Color waveColor){
        this.cooldown = cooldown;
        this.maxRadius = maxRadius;
        this.shockwaveDamage = shockwaveDamage;
        this.maxBubbles = 0; // 保留你原来的写法（错误2），自行处理
        this.waveColor = waveColor;
    }

    public ShockwaveAbility(boolean fullMap, float cooldown){
        this.fullMap = fullMap;
        this.cooldown = cooldown;
        if (fullMap) {
            this.maxRadius = 100000f; // 占位值，全图模式下实际视觉半径在触发时计算
        } else {
            this.maxRadius = 320f;
        }
    }

    private float timer = 0f;
    private Seq<Shockwave> shockwaves = new Seq<>();
    private Seq<Bubble> bubbles = new Seq<>();

    @Override
    public void update(Unit unit){
        super.update(unit);

        timer += Time.delta;

        // 每次释放间隔 cooldown 秒
        if(timer >= cooldown * 60f){
            timer = 0f;
            triggerShockwave(unit);
        }

        // 更新冲击波
        for(int i = shockwaves.size - 1; i >= 0; i--){
            Shockwave sw = shockwaves.get(i);
            sw.radius += sw.expandSpeed * Time.delta / 60f; // expandSpeed 单位：像素/秒
            sw.life -= Time.delta;

            // 处理所有距离 <= 当前半径的目标（已按距离排序，用指针推进）
            while(sw.targetIndex < sw.targets.size){
                Target t = sw.targets.get(sw.targetIndex);
                if(t.distance > sw.radius){
                    break; // 后面的目标更远，因为已排序
                }
                if(!t.isDead() && t.getTeam() != unit.team){
                    t.damage(unit.team);
                }
                sw.targetIndex++;
            }

            if(sw.radius >= sw.maxRadius || sw.life <= 0f){
                shockwaves.remove(i);
            }
        }

        // 更新泡泡
        for(int i = bubbles.size - 1; i >= 0; i--){
            Bubble b = bubbles.get(i);
            b.life -= Time.delta;
            if(b.life <= 0f){
                bubbles.remove(i);
                continue;
            }
            Groups.unit.each(u -> {
                if(u.team != unit.team || u.dead) return;
                float dist = Mathf.dst(b.x, b.y, u.x, u.y);
                if(dist <= bubbleRadius){
                    u.heal(bubbleHealPerSec * Time.delta / 60f);
                    if(!u.hasEffect(StatusEffects.overclock)){
                        u.apply(StatusEffects.overclock, 60f);
                    }
                }
            });
        }
    }

    private void triggerShockwave(Unit unit){
        float actualMaxRadius;
        float actualSpeed;

        if(fullMap){
            // 全图覆盖：取单位到地图四个角的最大距离，确保任何位置都能覆盖全图
            float w = world.unitWidth() * tilesize;
            float h = world.unitHeight() * tilesize;
            float d1 = Mathf.dst(unit.x, unit.y, 0f, 0f);
            float d2 = Mathf.dst(unit.x, unit.y, w, 0f);
            float d3 = Mathf.dst(unit.x, unit.y, 0f, h);
            float d4 = Mathf.dst(unit.x, unit.y, w, h);
            actualMaxRadius = Math.max(Math.max(d1, d2), Math.max(d3, d4));

            // 3 秒内扩散完，速度为 像素/秒（update 中会除以 60）
            actualSpeed = actualMaxRadius / 3f;
        } else {
            actualMaxRadius = maxRadius;
            actualSpeed = expandSpeed;
        }

        // 快照所有敌方目标并按距离排序
        Seq<Target> targets = new Seq<>();
        float collectRadius = actualMaxRadius + ringWidth;

        Groups.unit.each(u -> {
            if(u.team != unit.team && !u.dead){
                float dist = Mathf.dst(unit.x, unit.y, u.x, u.y);
                if(dist <= collectRadius){
                    targets.add(new Target(u, dist));
                }
            }
        });

        Groups.build.each(b -> {
            if(b.team != unit.team && !b.dead && b.isValid()){
                float dist = Mathf.dst(unit.x, unit.y, b.x, b.y);
                if(dist <= collectRadius){
                    targets.add(new Target(b, dist));
                }
            }
        });

        targets.sort((a, b) -> Float.compare(a.distance, b.distance));

        Shockwave sw = new Shockwave(unit.x, unit.y, actualMaxRadius, actualSpeed, targets);
        shockwaves.add(sw);

        // 泡泡生成
        int bubbleCount = Mathf.random(3, 5);
        for(int b = 0; b < bubbleCount && bubbles.size < maxBubbles; b++){
            spawnBubble(unit);
        }

        Fx.steam.at(unit.x, unit.y);
    }

    private void spawnBubble(Unit unit){
        float angle = Mathf.random(360f) * Mathf.degRad;
        float spawnRange = fullMap ? 500f : maxRadius * 0.85f;
        float dist = Mathf.random(bubbleRadius, spawnRange);
        float bx = unit.x + Mathf.cos(angle) * dist;
        float by = unit.y + Mathf.sin(angle) * dist;
        float life = bubbleDuration * 60f * Mathf.random(0.7f, 1f);
        bubbles.add(new Bubble(bx, by, life));
    }

    @Override
    public void draw(Unit unit){
        super.draw(unit);

        Draw.z(Layer.effect);

        // 冲击波渲染
        for(int i = 0; i < shockwaves.size; i++){
            Shockwave sw = shockwaves.get(i);
            float r = sw.radius;
            float progress = Mathf.clamp(r / sw.maxRadius);
            float alpha = (1f - progress) * 0.8f;

            // 主环
            Draw.color(waveColor, alpha);
            Lines.stroke(3f * (1f - progress * 0.5f));
            Lines.circle(sw.x, sw.y, r);

            // 内侧光环
            Draw.color(Color.valueOf("ffaa44"), alpha * 0.4f);
            Fill.circle(sw.x, sw.y, r);
            Draw.color(Color.valueOf("ffaa44"), alpha * 0.3f);
            Lines.stroke(1f);
            Lines.circle(sw.x, sw.y, Math.max(r - ringWidth, 0f));

            // 外侧光晕
            Draw.color(Color.white, alpha * 0.4f);
            Lines.stroke(1f);
            Lines.circle(sw.x, sw.y, r + 4f);
        }

        // 泡泡渲染
        for(int i = 0; i < bubbles.size; i++){
            Bubble b = bubbles.get(i);
            float lifeRatio = Math.min(b.life / (bubbleDuration * 60f * 0.5f), 1f);
            float pulse = Mathf.absin(Time.time + i * 17f, 3f, 0.15f);
            float alpha = lifeRatio * (0.3f + pulse);

            Draw.color(bubbleColor, alpha);
            Fill.circle(b.x, b.y, bubbleRadius);

            Draw.color(bubbleColor, alpha * 1.5f);
            Lines.stroke(1f);
            Lines.circle(b.x, b.y, bubbleRadius);

            Draw.color(Color.white, alpha * 0.3f);
            Fill.circle(b.x, b.y, bubbleRadius * 0.3f);
        }

        Draw.reset();
    }

    /** 目标（单位或建筑） */
    static class Target{
        Object obj;
        float distance;
        boolean isUnit;

        Target(Unit u, float distance){
            this.obj = u;
            this.distance = distance;
            this.isUnit = true;
        }

        Target(Building b, float distance){
            this.obj = b;
            this.distance = distance;
            this.isUnit = false;
        }

        boolean isDead(){
            if(isUnit) return ((Unit)obj).dead;
            return ((Building)obj).dead || !((Building)obj).isValid();
        }

        Team getTeam(){
            if(isUnit) return ((Unit)obj).team;
            return ((Building)obj).team;
        }

        /** 代码杀：伤害 = maxHealth * 10，通过 Damage.damage 归属到来源队伍 */
        void damage(Team sourceTeam){
            if(isUnit){
                Unit u = (Unit)obj;
                if(!u.dead){
                    // 使用单位碰撞体积作为判定半径，确保 Damage.damage 可靠命中
                    Damage.damage(sourceTeam, u.x, u.y, Math.max(u.hitSize / 2f, 0.01f), u.maxHealth * 10f);
                }
            } else {
                Building b = (Building)obj;
                if(!b.dead && b.isValid()){
                    Damage.damage(sourceTeam, b.x, b.y, b.block.size * tilesize / 2f, b.maxHealth * 10f);
                }
            }
        }
    }

    /** 冲击波数据 */
    static class Shockwave{
        float x, y;
        float radius;
        float life;
        float maxRadius;
        float expandSpeed;
        Seq<Target> targets;
        int targetIndex = 0;

        Shockwave(float x, float y, float maxRadius, float expandSpeed, Seq<Target> targets){
            this.x = x;
            this.y = y;
            this.radius = 0f;
            // 动态计算生命：扩散所需时间（帧）+ 30 帧余量，保证扩散完整播完
            this.life = maxRadius / expandSpeed * 60f + 30f;
            this.maxRadius = maxRadius;
            this.expandSpeed = expandSpeed;
            this.targets = targets;
        }
    }

    /** 毒泡泡数据 */
    static class Bubble{
        float x, y;
        float life;

        Bubble(float x, float y, float life){
            this.x = x;
            this.y = y;
            this.life = life;
        }
    }
}