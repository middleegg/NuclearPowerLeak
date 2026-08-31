package Npl.newSth.walls;

import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.struct.IntIntMap;
import arc.struct.IntSet;
import arc.struct.Seq;
import arc.util.Time;
import arc.util.Strings;
import arc.util.Tmp;
import arc.struct.EnumSet;
import mindustry.content.Fx;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.world.consumers.ConsumeItems;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.draw.*;
import mindustry.world.meta.*;
import mindustry.ui.Styles;

import Npl.content.ModStats;
import Npl.content.NuFx;

import static mindustry.Vars.*;

public class NuHealerProjector extends Block {

    public float range = 14f;
    public float healPercent = 2f / 60f;
    public float optionalMultiplier = 2f;
    public float optionalUseTime = 60f * 8f;
    public Color baseColor = Pal.heal;
    public boolean healBuildings = true;

    public DrawBlock drawer = new DrawDefault();

    public float chargeTime = 600f;

    public float explosionDamage = 300f;
    public float explosionRange = 24f;
    public float explosionHealPortion = 0.75f;
    /** 晶体撞击友方单位时给单位回复的生命值（非百分比）。 */
    public float unitHealAmount = 300f;
    public float orbitSpeed = 1.5f;
    public float crystalSize = 4.5f;
    public float respawnTime = 600f;
    public float dashAccel = 0.7f;
    public float dashMaxSpeed = 5.5f;

    public int maxLayers = 4;
    public float layerRadiusStep = 0.35f;
    public float layerDelay = 20f;

    // 性能与逻辑调参
    public float scanInterval = 10f;
    public float chainCooldown = 90f;
    public float chainMinCharge = 0.25f;
    public int maxWaveVisuals = 12;

    private static final Color CRYSTAL_BLUE = Color.valueOf("4FC3F7");
    private static final Color CRYSTAL_GREEN = Color.valueOf("4CAF50");
    private static final Color CRYSTAL_MIX = new Color(CRYSTAL_BLUE).lerp(CRYSTAL_GREEN, 0.5f);

    private static final IntIntMap buildingCoverMap = new IntIntMap();
    private static long coverFrame = -1;
    private static long cleanupFrame = -1;
    private static int nextWaveId = 0;

    private static final Seq<WaveInstance> activeWaves = new Seq<>();

    static {
        Events.on(WorldLoadEvent.class, e -> activeWaves.clear());
    }

    public static class WaveInstance {
        public float x, y;
        public float startTime;
        public float duration;
        public float maxRadius;
        public int sourcePos;
        public int id;

        public WaveInstance(float x, float y, float duration, float maxRadius, int sourcePos) {
            this.x = x;
            this.y = y;
            this.startTime = Time.time;
            this.duration = duration;
            this.maxRadius = maxRadius;
            this.sourcePos = sourcePos;
            this.id = nextWaveId++;
        }

        public float currentRadius() {
            return (Time.time - startTime) / duration * maxRadius;
        }

        public boolean isExpired() {
            return Time.time - startTime > duration;
        }
    }

    public static class OrbitCrystal {
        public float angle;
        public boolean active = true;
        public float respawnTimer = 0f;
        public float x, y;
        public int state = 0;
        public float stateTimer = 0f;
        public float currentRadius;
        public float speed = 0f;
        public Unit target;
        public boolean healing = false;

        public OrbitCrystal(float angle) {
            this.angle = angle;
        }
    }

    public NuHealerProjector(String name) {
        super(name);
        solid = true;
        update = true;
        group = BlockGroup.projectors;
        hasPower = true;
        hasItems = true;
        emitLight = true;
        suppressable = true;
        envEnabled |= Env.space;
        rotateDraw = false;
        flags = EnumSet.of(BlockFlag.blockRepair);
    }

    @Override
    public TextureRegion[] icons() {
        return drawer.finalIcons(this);
    }

    @Override
    public void load() {
        super.load();
        drawer.load(this);
    }

    @Override
    public void setStats() {
        stats.timePeriod = optionalUseTime;
        super.setStats();
        // 详情面板的具体构建逻辑统一放在 ModStats 中
        ModStats.buildHealerStats(this, stats);
    }

    /** 放置预览：画出治疗范围圈（虚线），以及多层涟漪的最大扩散范围提示。 */
    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        float cx = x * tilesize + offset, cy = y * tilesize + offset;
        Drawf.dashCircle(cx, cy, range * tilesize, baseColor);
        float maxR = range * tilesize * (1f + layerRadiusStep * (maxLayers - 1));
        Drawf.dashCircle(cx, cy, maxR, Tmp.c1.set(baseColor).a(0.35f));
    }

    public class NuHealerBuild extends Building {
        public Seq<Building> targets = new Seq<>();
        public int lastChange = -2;
        public float scanTimer = 0f;
        public float chainTimer = 0f;
        public float charge = 0f;
        public float chargeWarmup = 0f;
        public float cachedBoost = 1f;
        public boolean hasDamaged = false;
        public Seq<OrbitCrystal> crystals = new Seq<>(4);
        public int pendingLayers = 0;
        public int layersRemaining = 0;
        public int layerIndex = 0;
        public float layerTimer = 0f;
        public float layerPortion = 0f;
        public IntSet touchedWaves = new IntSet();
        /** 本 tick 是否有电（efficiency>0 即"有工作 且 通电"） */
        public boolean powered = false;
        /** 是否存在工作（受损建筑 或 晶体正在攻击/治疗目标），用于向电网申请电力 */
        public boolean crystalWork = false;

        public float layerRadius(int index) {
            return range * tilesize * (1f + layerRadiusStep * index);
        }

        public void detectTouches() {
            if (activeWaves.isEmpty()) return;
            for (int i = 0; i < activeWaves.size; i++) {
                WaveInstance wave = activeWaves.get(i);
                if (wave.sourcePos == pos() || touchedWaves.contains(wave.id)) continue;
                if (Mathf.dst(x, y, wave.x, wave.y) <= wave.currentRadius()) {
                    touchedWaves.add(wave.id);
                    if (pendingLayers < maxLayers - 1) pendingLayers++;
                }
            }
        }

        public void updateBuildingTargets() {
            targets.clear();
            if (!healBuildings) return;
            float r = layerRadius(0);
            indexer.eachBlock(team, Tmp.r1.setCentered(x, y, 2f * r), b -> true, targets::add);
        }

        @Override
        public void updateTile() {
            if (lastChange != world.tileChanges) {
                lastChange = world.tileChanges;
                updateBuildingTargets();
            }

            hasDamaged = false;
            for (int i = 0; i < targets.size; i++) {
                Building b = targets.get(i);
                if (b.damaged() && !b.isHealSuppressed()) {
                    hasDamaged = true;
                    break;
                }
            }

            if (chainTimer > 0f) {
                chainTimer -= delta();
            }

            if (checkSuppression()) {
                charge = Mathf.approachDelta(charge, 0f, 1f / 30f);
                chargeWarmup = Mathf.approachDelta(chargeWarmup, 0f, 1f / 70f);
                return;
            }

            globalCleanupWaves();
            detectTouches();

            if ((scanTimer -= delta()) <= 0f) {
                scanTimer = scanInterval;
                cachedBoost = calculateBoost();
                checkWaveTrigger();
            }

            // 有电 且 有工作 → 才允许行动（efficiency>0 表示 shouldConsume 成立且电力满足）
            powered = efficiency > 0f;
            updateCrystals();

            if (efficiency > 0 && hasDamaged) {
                if ((charge += edelta() * (1f + optionalEfficiency * 0.5f) * cachedBoost) >= chargeTime) {
                    release(1f);
                    charge = 0f;
                }
                chargeWarmup = Mathf.approachDelta(chargeWarmup, 1f, 1f / 30f);
            } else {
                charge = Mathf.approachDelta(charge, 0f, 1f / 30f);
                chargeWarmup = Mathf.approachDelta(chargeWarmup, 0f, 1f / 70f);
            }

            if (powered && layersRemaining > 0 && (layerTimer -= delta()) <= 0f) {
                doHeal(layerPortion, layerRadius(layerIndex));
                layerIndex++;
                layersRemaining--;
                layerTimer = layerDelay;
            }
        }

        /**
         * 释放一次治疗波。基础一圈立即爆发；若蓄力期间记录了其他治疗塔的涟漪触碰，
         * 则额外延时补放若干圈（半径逐圈扩大），总层数上限 maxLayers。
         */
        public void release(float portion) {
            portion = Mathf.clamp(portion, 0f, 1f);
            if (portion <= 0.001f) return;

            ensureCover();

            int totalLayers = Math.min(1 + pendingLayers, maxLayers);
            pendingLayers = 0;
            touchedWaves.clear();

            layerPortion = portion;
            doHeal(portion, layerRadius(0));

            if (totalLayers > 1) {
                layerIndex = 1;
                layersRemaining = totalLayers - 1;
                layerTimer = layerDelay;
            }
        }

        /** 以给定半径释放一圈治疗涟漪：范围内的受损建筑按覆盖度衰减治疗一次。 */
        public void doHeal(float portion, float radius) {
            float boost = calculateBoost();
            float healAmount = Mathf.lerp(1f, optionalMultiplier, optionalEfficiency) * healPercent * chargeTime * boost * portion;
            float r2 = radius * radius;

            if (healBuildings) {
                indexer.eachBlock(team, Tmp.r1.setCentered(x, y, 2f * radius), b -> true, b -> {
                    if (!b.damaged() || b.isHealSuppressed()) return;
                    if (Mathf.dst2(b.x, b.y, x, y) > r2) return;
                    int count = Math.max(buildingCoverMap.get(b.pos()), 1);
                    float factor = 1f / Mathf.sqrt(count);
                    b.heal(healAmount * factor * b.block.health / 100f);
                    b.recentlyHealed();
                    Fx.regenParticle.at(b.x, b.y, 0f, Pal.heal);
                });
            }

            activeWaves.add(new WaveInstance(x, y, 360f / 60f, radius, pos()));
            if (activeWaves.size <= maxWaveVisuals) {
                NuFx.healWave.at(x, y, 0f, radius);
            }
        }

        public void updateCrystals() {
            if (crystals.isEmpty()) {
                for (int i = 0; i < 4; i++) {
                    crystals.add(new OrbitCrystal(i / 4f * Mathf.PI2));
                }
            }

            float baseRadius = range * tilesize * 0.65f;
            float yOffset = size * tilesize / 2f;

            boolean hasChaser = false;
            boolean needTarget = false;
            for (int i = 0; i < crystals.size; i++) {
                OrbitCrystal c = crystals.get(i);
                if (!c.active) {
                    continue;
                }
                if (c.state == 2) {
                    hasChaser = true;
                } else if (c.state == 0) {
                    needTarget = true;
                }
            }

            // 治疗优先：先找受损的友方单位，没有才找敌人
            Unit allyInRange = (!hasChaser && needTarget)
                    ? Units.closest(team, x, y, range * tilesize, u -> !u.dead() && u.health < u.maxHealth)
                    : null;

            Unit enemyInRange = (!hasChaser && needTarget && allyInRange == null)
                    ? Units.closestEnemy(team, x, y, range * tilesize, u -> !u.dead())
                    : null;

            // 是否有"工作"：存在追击中的晶体、待重生的晶体，或探测到可治疗/可攻击的目标。
            // 该标记反馈给 shouldConsume()，实现"有工作才向电网申请电力"。
            crystalWork = hasChaser || allyInRange != null || enemyInRange != null;

            // 无电仅停止"工作"（不锁定、不冲刺、不治疗、不自爆），
            // 但环绕运动与晶体重生始终保持，让晶体无论是否处于工作状态都在旋转。
            boolean poweredNow = powered;

            boolean acquired = false;
            for (int i = 0; i < crystals.size; i++) {
                OrbitCrystal crystal = crystals.get(i);
                if (!crystal.active) {
                    crystal.respawnTimer -= delta();
                    if (crystal.respawnTimer <= 0f) {
                        crystal.active = true;
                        crystal.state = 0;
                        crystal.currentRadius = baseRadius;
                        crystal.target = null;
                        crystal.healing = false;
                    }
                    continue;
                }

                switch (crystal.state) {
                    case 0:
                        // 环绕始终进行，不受供电/工作状态影响
                        crystal.angle += orbitSpeed * delta() / 60f;
                        crystal.currentRadius = Mathf.approachDelta(crystal.currentRadius, baseRadius, delta() * 2f);
                        crystal.x = x + Mathf.cos(crystal.angle) * crystal.currentRadius;
                        crystal.y = y + yOffset + Mathf.sin(crystal.angle) * crystal.currentRadius;

                        if (!poweredNow) break;

                        if (allyInRange != null && !acquired) {
                            acquired = true;
                            crystal.state = 2;
                            crystal.speed = 0f;
                            crystal.target = allyInRange;
                            crystal.healing = true;
                        } else if (enemyInRange != null && !acquired) {
                            acquired = true;
                            crystal.state = 2;
                            crystal.speed = 0f;
                            crystal.target = enemyInRange;
                            crystal.healing = false;
                        }
                        break;

                    case 2:
                        // 断电：放弃追击，回到环绕轨道（环绕不停）
                        if (!poweredNow) {
                            crystal.state = 0;
                            crystal.target = null;
                            crystal.healing = false;
                            break;
                        }

                        if (crystal.target == null || crystal.target.dead()
                                || (crystal.healing && crystal.target.health >= crystal.target.maxHealth)) {
                            crystal.state = 0;
                            crystal.target = null;
                            crystal.healing = false;
                            break;
                        }

                        float targetX = crystal.target.x;
                        float targetY = crystal.target.y;
                        float len = Mathf.dst(crystal.x, crystal.y, targetX, targetY);

                        crystal.speed = Mathf.approachDelta(crystal.speed, dashMaxSpeed, dashAccel);
                        if (len > 0.001f) {
                            float step = Math.min(crystal.speed * delta(), len);
                            crystal.x += (targetX - crystal.x) / len * step;
                            crystal.y += (targetY - crystal.y) / len * step;
                        }

                        if (Mathf.dst(crystal.x, crystal.y, targetX, targetY) < crystalSize + crystal.target.hitSize()) {
                            if (crystal.healing) {
                                healCrystal(crystal);
                            } else {
                                explode(crystal);
                            }
                        }
                        break;
                }
            }
        }

        public void explode(OrbitCrystal crystal) {
            Units.nearbyEnemies(team, crystal.x, crystal.y, explosionRange, unit -> {
                if (unit != null && !unit.dead()) {
                    unit.damage(explosionDamage);
                }
            });

            Fx.explosion.at(crystal.x, crystal.y);

            // 治疗优先：晶体自爆必定释放一次治疗波（充能越高越强），攻击只是附带
            float portion = Math.max(charge / chargeTime, explosionHealPortion);
            if (portion > 0.05f) {
                release(portion);
                charge = 0f;
            }

            crystal.active = false;
            crystal.respawnTimer = respawnTime;
            crystal.healing = false;
        }

        /** 晶体撞击友方单位：治疗范围内受损单位，并释放一次治疗波，不造成任何伤害。 */
        public void healCrystal(OrbitCrystal crystal) {
            float amount = unitHealAmount * calculateBoost();
            Units.nearby(team, crystal.x, crystal.y, explosionRange, u -> {
                if (u.dead() || u.health >= u.maxHealth) return;
                u.heal(amount);
                Fx.regenParticle.at(u.x, u.y, 0f, Pal.heal);
            });

            Fx.healWave.at(crystal.x, crystal.y, explosionRange);
            NuFx.healWave.at(crystal.x, crystal.y, 0f, explosionRange);

            float portion = Math.max(charge / chargeTime, explosionHealPortion);
            if (portion > 0.05f) {
                release(portion);
                charge = 0f;
            }

            crystal.active = false;
            crystal.respawnTimer = respawnTime;
            crystal.healing = false;
        }

        public float calculateBoost() {
            int boostCount = 0;
            for (int i = 0; i < activeWaves.size; i++) {
                WaveInstance wave = activeWaves.get(i);
                if (wave.x == x && wave.y == y) continue;
                if (Mathf.dst(x, y, wave.x, wave.y) <= wave.currentRadius()) {
                    boostCount++;
                }
            }
            float boost = 1f;
            float currentBoost = 0.5f;
            for (int i = 0; i < boostCount; i++) {
                boost += currentBoost;
                currentBoost *= 0.5f;
            }
            return boost;
        }

        public void checkWaveTrigger() {
            if (chainTimer > 0f) return;
            if (charge < chargeTime * chainMinCharge) return;

            for (int i = 0; i < activeWaves.size; i++) {
                WaveInstance wave = activeWaves.get(i);
                if (wave.sourcePos == pos()) continue;
                if (Mathf.dst(x, y, wave.x, wave.y) <= wave.currentRadius()) {
                    chainTimer = chainCooldown;
                    float portion = Mathf.clamp(charge / chargeTime, 0f, 1f);
                    release(portion);
                    charge = 0f;
                    break;
                }
            }
        }

        @Override
        public boolean shouldConsume() {
            // 有工作才耗电：范围内有受损建筑，或晶体正在追击/待重生/探测到目标
            return hasDamaged || crystalWork;
        }

        @Override
        public void drawSelect() {
            super.drawSelect();
            Draw.color(baseColor);
            Lines.dashCircle(x, y, range * tilesize);
            if (healBuildings) {
                for (int i = 0; i < targets.size; i++) {
                    Drawf.selected(targets.get(i), Tmp.c1.set(baseColor).a(Mathf.absin(4f, 1f)));
                }
            }
        }

        @Override
        public void draw() {
            drawer.draw(this);
            drawCharge();
            drawCrystals();
        }

        @Override
        public void drawLight() {
            drawer.drawLight(this);
        }

        private void drawCharge() {
            float progress = charge / chargeTime;
            if (progress > 0.01f) {
                Draw.z(Layer.effect);
                float yOffset = size * tilesize / 2f + 10f;
                float radius = 20f + 10f * progress;
                float rotation = progress * Mathf.PI2 * 2f;

                Color currentColor = progress < 0.5f
                    ? Tmp.c1.set(CRYSTAL_BLUE).lerp(CRYSTAL_GREEN, progress * 2f)
                    : Tmp.c1.set(CRYSTAL_GREEN).lerp(Color.white, (progress - 0.5f) * 2f);

                Draw.color(currentColor);
                Draw.alpha(0.7f + 0.3f * progress);
                Lines.stroke(2f);

                for (int i = 0; i < 6; i++) {
                    float angle1 = rotation + i / 6f * Mathf.PI2 + Mathf.PI / 6f;
                    float angle2 = rotation + (i + 1) / 6f * Mathf.PI2 + Mathf.PI / 6f;
                    Lines.line(
                        x + Mathf.cos(angle1) * radius,
                        y + yOffset + Mathf.sin(angle1) * radius,
                        x + Mathf.cos(angle2) * radius,
                        y + yOffset + Mathf.sin(angle2) * radius
                    );
                }

                Lines.stroke(1f);
                for (int i = 0; i < 6; i++) {
                    float angle = rotation + i / 6f * Mathf.PI2 + Mathf.PI / 6f;
                    Lines.line(
                        x + Mathf.cos(angle) * radius * 0.3f,
                        y + yOffset + Mathf.sin(angle) * radius * 0.3f,
                        x + Mathf.cos(angle) * radius * 0.7f,
                        y + yOffset + Mathf.sin(angle) * radius * 0.7f
                    );
                }

                for (int i = 0; i < 6; i++) {
                    float angle = rotation + i / 6f * Mathf.PI2 + Mathf.PI / 6f;
                    float dist = radius * (0.4f + 0.3f * Mathf.sin(progress * 10f + i));
                    Fill.circle(
                        x + Mathf.cos(angle) * dist,
                        y + yOffset + Mathf.sin(angle) * dist,
                        1.5f
                    );
                }

                Draw.alpha(1f);
                Draw.reset();
            }
        }

        private void drawCrystals() {
            if (crystals.isEmpty()) return;

            Draw.z(Layer.effect);
            for (int i = 0; i < crystals.size; i++) {
                OrbitCrystal crystal = crystals.get(i);
                if (!crystal.active) continue;

                Draw.color(CRYSTAL_MIX);
                Draw.alpha(0.8f);

                float size = crystalSize;
                float rotation = crystal.angle * 2f + Time.time / 20f;
                Fill.poly(crystal.x, crystal.y, 4, size, (float)Math.toDegrees(rotation));

                Draw.alpha(0.2f);
                Fill.circle(crystal.x, crystal.y, size * 1.5f);
            }
            Draw.alpha(1f);
            Draw.reset();
        }

        @Override
        public float warmup() {
            return chargeWarmup;
        }

        @Override
        public float totalProgress() {
            return charge;
        }
    }

    /**
     * 覆盖度表按需重建：只有当本帧真的会发生治疗释放时才统计，
     * 取代过去"每个方块每帧都遍历所有目标"的做法。
     */
    private static void ensureCover() {
        if (coverFrame == state.updateId) return;
        coverFrame = state.updateId;
        buildingCoverMap.clear();
        for (Building b : Groups.build) {
            if (!(b instanceof NuHealerBuild nb)) continue;
            for (int j = 0; j < nb.targets.size; j++) {
                int pos = nb.targets.get(j).pos();
                buildingCoverMap.put(pos, buildingCoverMap.get(pos) + 1);
            }
        }
    }

    /** 全局每帧只清理一次过期波浪，取代过去每个方块各清一次。 */
    private static void globalCleanupWaves() {
        if (cleanupFrame == state.updateId) return;
        cleanupFrame = state.updateId;
        for (int i = activeWaves.size - 1; i >= 0; i--) {
            if (activeWaves.get(i).isExpired()) {
                activeWaves.remove(i);
            }
        }
    }
}
