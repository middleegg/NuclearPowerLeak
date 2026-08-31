package Npl.newSth;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.ShockMine;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

import static mindustry.Vars.*;

/**
 * AttractMine extends ShockMine
 * - 持续吸引范围内敌方单位（地面+空中），越靠近中心越快
 * - 每次只吸引最近的 maxAttractCount 个单位
 * - touchRadius 与 attractStrength 呈反比
 * - 单位碰到中心时触发：
 *   1) 该单位损失20%最大血量
 *   2) 地雷损失20%血量上限
 *   3) 对全范围内所有敌方单位造成伤害
 *   4) 生成三层色毒泡泡（光色、暗色、渐变）
 *   5) 10秒冷却期
 */
public class AttractMine extends ShockMine{

    public float attractRange = 120f;
    public float attractStrength = 0.8f;
    public int maxAttractCount = 5;
    public float airAttractMultiplier = 0.4f;
    public float touchRadius;
    public float touchDamageFraction = 0.2f;
    public float mineDamageFraction = 0.2f;
    /** 碰撞后对全范围造成的伤害 */
    public float rangeDamage = 25f;
    /** 冲击波对接触单位造成的伤害 */
    public float shockwaveDamage = 25f;
    /** 冲击波扩散速度（像素/秒） */
    public float shockwaveSpeed = 180f;
    /** 冲击波环宽 */
    public float shockwaveRingWidth = 16f;
    /** 触发冷却（10秒） */
    public float touchCooldownTime = 600f;
    public int maxBubbles = 8;
    public float bubbleRadius = 14f;
    public float bubbleDuration = 30f * 60f;
    public float bubbleDotDps = 8f;

    // 泡泡三层色
    public Color bubbleLightColor = Color.valueOf("c8ff8a");  // 光色
    public Color bubbleDarkColor = Color.valueOf("3a8a1a");   // 暗色
    public Color bubbleGradColor = Color.valueOf("7acc40");   // 渐变中间色

    public AttractMine(String name){
        super(name);
        this.update = true;
        this.buildCostMultiplier = 0f;
        this.solid = false;
        this.health = 200;
        this.category = Category.defense;
    }

    @Override
    public void init(){
        super.init();
        touchRadius = 40f / attractStrength;
        updateClipRadius(attractRange);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.range, attractRange / tilesize, StatUnit.blocks);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        float wx = x * tilesize + size * tilesize / 2f;
        float wy = y * tilesize + size * tilesize / 2f;

        Draw.z(Layer.plans);
        Draw.color(valid ? Pal.accent : Pal.redderDust, 0.2f);
        Fill.circle(wx, wy, attractRange);

        Draw.color(valid ? Pal.accent : Pal.redderDust, 0.8f);
        Lines.stroke(1.5f);
        Lines.circle(wx, wy, attractRange);

        Draw.color(Pal.redderDust, 0.25f);
        Fill.circle(wx, wy, touchRadius);
        Draw.color(Pal.redderDust, 0.7f);
        Lines.circle(wx, wy, touchRadius);

        for(int i = 0; i < 8; i++){
            float angle = i * 45f * Mathf.degRad;
            float r1 = attractRange * 0.5f;
            float r2 = attractRange;
            float x1 = wx + Mathf.cos(angle) * r1;
            float y1 = wy + Mathf.sin(angle) * r1;
            float x2 = wx + Mathf.cos(angle) * r2;
            float y2 = wy + Mathf.sin(angle) * r2;
            Draw.color(valid ? Pal.accent : Pal.redderDust, 0.4f);
            Lines.line(x1, y1, x2, y2);
        }

        Draw.reset();
    }

    public class AttractMineBuild extends ShockMineBuild{

        private Seq<Unit> nearbyBuffer = new Seq<>();
        private Seq<Bubble> bubbles = new Seq<>();
        private Seq<Shockwave> shockwaves = new Seq<>();
        private float touchCooldown = 0f;

        @Override
        public void updateTile(){
            super.updateTile();

            if(touchCooldown > 0f) touchCooldown -= delta();

            // 遍历收集范围内所有敌方单位
            nearbyBuffer.clear();
            Units.nearbyEnemies(team, x, y, attractRange, u -> {
                if(u != null && u.isValid()) nearbyBuffer.add(u);
            });

            // 按距离排序，只吸引最近的 maxAttractCount 个
            nearbyBuffer.sort(u -> u.dst2(x, y));
            int count = Math.min(maxAttractCount, nearbyBuffer.size);

            for(int i = 0; i < count; i++){
                Unit u = nearbyBuffer.get(i);
                if(u == null || !u.isValid()) continue;

                boolean isAir = u.type != null && u.type.flying;
                float strength = isAir ? attractStrength * airAttractMultiplier : attractStrength;

                float dx = x - u.x, dy = y - u.y;
                float d2 = dx * dx + dy * dy;

                if(d2 > 0.0001f){
                    float d = (float)Math.sqrt(d2);
                    // 越靠近中心越快：距离越近，falloff越大
                    float closeness = 1f - Math.min(d / attractRange, 1f);
                    float vx = dx / d * strength * (0.3f + closeness * 0.7f);
                    float vy = dy / d * strength * (0.3f + closeness * 0.7f);
                    u.vel.add(vx, vy);
                    if(!isAir){
                        u.apply(StatusEffects.slow, 10f);
                    }
                }

                // 检查是否碰到中心
                if(d2 < touchRadius * touchRadius && touchCooldown <= 0f){
                    triggerTouch(u);
                }
            }

            // 更新冲击波
            for(int i = shockwaves.size - 1; i >= 0; i--){
                Shockwave sw = shockwaves.get(i);
                float prevR = sw.radius;
                sw.radius += shockwaveSpeed * delta() / 60f;
                sw.life -= delta();

                float innerR = prevR - shockwaveRingWidth;
                float outerR = sw.radius + shockwaveRingWidth;

                Units.nearbyEnemies(team, x, y, outerR, enemy -> {
                    if(sw.hitSet.contains(enemy)) return;
                    float ed = enemy.dst(x, y);
                    if(ed >= innerR && ed <= outerR){
                        enemy.damage(shockwaveDamage);
                        sw.hitSet.add(enemy);
                    }
                });

                if(sw.radius >= attractRange || sw.life <= 0f){
                    shockwaves.remove(i);
                }
            }

            // 更新泡泡（只在碰撞后存在）
            for(int i = bubbles.size - 1; i >= 0; i--){
                Bubble b = bubbles.get(i);
                b.life -= delta();
                if(b.life <= 0f){
                    bubbles.remove(i);
                    continue;
                }
                float dps = bubbleDotDps;
                Units.nearbyEnemies(team, b.x, b.y, bubbleRadius, enemy -> {
                    enemy.damage(dps * delta() / 60f);
                    enemy.apply(StatusEffects.slow, 20f);
                });
            }
        }

        private void triggerTouch(Unit touched){
            // 10秒冷却
            touchCooldown = touchCooldownTime;

            // 1. 碰到的单位损失20%最大血量
            touched.damage(touched.maxHealth() * touchDamageFraction);

            // 2. 地雷自身损失20%血量上限
            damage(maxHealth * mineDamageFraction);

            // 3. 对全范围内所有敌方单位造成伤害
            Units.nearbyEnemies(team, x, y, attractRange, enemy -> {
                enemy.damage(rangeDamage);
            });

            // 4. 生成冲击波
            shockwaves.add(new Shockwave());

            // 5. 生成随机数量三层色毒泡泡（仅在碰撞后出现）
            int bubbleCount = Mathf.random(3, 6);
            for(int b = 0; b < bubbleCount; b++){
                spawnRandomBubble();
            }

            // 6. 视觉效果
            Fx.steam.at(x, y);
        }

        private void spawnRandomBubble(){
            float angle = Mathf.random(360f) * Mathf.degRad;
            float dist = Mathf.random(bubbleRadius, attractRange * 0.85f);
            float bx = x + Mathf.cos(angle) * dist;
            float by = y + Mathf.sin(angle) * dist;
            float life = bubbleDuration * Mathf.random(0.7f, 1f);
            bubbles.add(new Bubble(bx, by, life));
        }

        @Override
        public void draw(){
            super.draw();

            Draw.z(Layer.effect);

            // 吸引范围圈
            Draw.color(Pal.accent, 0.08f + Mathf.absin(Time.time, 2f, 0.04f));
            Fill.circle(x, y, attractRange);

            Draw.color(Pal.accent, 0.3f + Mathf.absin(Time.time, 3f, 0.1f));
            Lines.stroke(1f);
            Lines.circle(x, y, attractRange);

            // 旋转射线
            for(int i = 0; i < 6; i++){
                float angle = i * 60f * Mathf.degRad + Time.time * 0.02f;
                float r1 = attractRange * 0.3f;
                float r2 = attractRange * 0.9f;
                float x1 = x + Mathf.cos(angle) * r1;
                float y1 = y + Mathf.sin(angle) * r1;
                float x2 = x + Mathf.cos(angle) * r2;
                float y2 = y + Mathf.sin(angle) * r2;
                Draw.color(Pal.accent, 0.25f);
                Lines.line(x1, y1, x2, y2);
            }

            // 触发区域
            Draw.color(Pal.redderDust, 0.15f + Mathf.absin(Time.time, 4f, 0.05f));
            Fill.circle(x, y, touchRadius);
            Draw.color(Pal.redderDust, 0.5f);
            Lines.stroke(1.5f);
            Lines.circle(x, y, touchRadius);

            // 冷却指示
            if(touchCooldown > 0f){
                float cdRatio = touchCooldown / touchCooldownTime;
                Draw.color(Pal.gray, 0.3f);
                Fill.circle(x, y, touchRadius * 1.2f);
                Draw.color(Pal.accent, 0.6f);
                Lines.stroke(2f);
                Lines.arc(x, y, touchRadius * 1.2f, 1f - cdRatio);
            }

            // 冲击波渲染
            for(int i = 0; i < shockwaves.size; i++){
                Shockwave sw = shockwaves.get(i);
                float r = sw.radius;
                float progress = r / attractRange;
                float alpha = (1f - progress) * 0.8f;

                Draw.color(Pal.redderDust, alpha);
                Lines.stroke(3f * (1f - progress * 0.5f));
                Lines.circle(x, y, r);

                Draw.color(Color.valueOf("ffaa44"), alpha * 0.5f);
                Fill.circle(x, y, r);
                Draw.color(Color.valueOf("ffaa44"), alpha * 0.3f);
                Lines.stroke(1f);
                Lines.circle(x, y, Math.max(r - shockwaveRingWidth, 0f));

                Draw.color(Color.white, alpha * 0.4f);
                Lines.stroke(1f);
                Lines.circle(x, y, r + 4f);
            }

            // 三层色泡泡
            for(int i = 0; i < bubbles.size; i++){
                Bubble b = bubbles.get(i);
                float lifeRatio = Math.min(b.life / (bubbleDuration * 0.5f), 1f);
                float pulse = Mathf.absin(Time.time + i * 17f, 3f, 0.15f);
                float alpha = lifeRatio * (0.4f + pulse);

                // 第一层：暗色（外圈底色）
                Draw.color(bubbleDarkColor, alpha * 0.6f);
                Fill.circle(b.x, b.y, bubbleRadius);

                // 第二层：渐变色（中间过渡）
                Draw.color(bubbleGradColor, alpha * 0.5f);
                Fill.circle(b.x, b.y, bubbleRadius * 0.7f);

                // 第三层：光色（内核高光）
                Draw.color(bubbleLightColor, alpha * 0.8f);
                Fill.circle(b.x, b.y, bubbleRadius * 0.4f);

                // 光色描边
                Draw.color(bubbleLightColor, alpha);
                Lines.stroke(1.5f);
                Lines.circle(b.x, b.y, bubbleRadius);

                // 渐变描边
                Draw.color(bubbleGradColor, alpha * 0.6f);
                Lines.stroke(1f);
                Lines.circle(b.x, b.y, bubbleRadius * 0.7f);

                // 白色亮点中心
                Draw.color(Color.white, alpha * 0.4f);
                Fill.circle(b.x, b.y, bubbleRadius * 0.15f);
            }

            Draw.reset();
        }
    }

    static class Shockwave{
        float radius;
        float life;
        Seq<Unit> hitSet = new Seq<>();

        Shockwave(){
            this.radius = 0f;
            this.life = 120f;
        }
    }

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
