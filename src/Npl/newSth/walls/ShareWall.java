package Npl.newSth.walls;

import arc.Events;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.world.blocks.defense.Wall;

import static mindustry.Vars.state;
import static mindustry.Vars.net;

public class ShareWall extends Wall {

    public ShareWall(String name) {
        super(name);
        Events.on(EventType.WorldLoadEvent.class, e -> {
            graphMap.clear();
            bfsQueue.clear();
            found.clear();
            dirtyWalls.clear();
            Log.info("[ShareWall] 世界加载，清理血量共享图");
        });
    }

    private static final ObjectMap<ShareWallBuild, HealthGraph> graphMap = new ObjectMap<>();
    private static final Seq<ShareWallBuild> bfsQueue = new Seq<>();
    private static final ObjectSet<ShareWallBuild> found = new ObjectSet<>();
    private static final ObjectSet<ShareWallBuild> dirtyWalls = new ObjectSet<>();

    public static class HealthGraph {
        public final Seq<ShareWallBuild> members = new Seq<>();

        public void add(ShareWallBuild wall) {
            if (wall != null && !members.contains(wall)) {
                members.add(wall);
                graphMap.put(wall, this);
            }
        }

        public void clearMembers() {
            for (ShareWallBuild wall : members) {
                graphMap.remove(wall);
            }
            members.clear();
        }

        public boolean isEmpty() {
            return members.size == 0;
        }

        public void applyDamage(float rawDamage) {
            if (rawDamage <= 0 || members.isEmpty()) return;

            float damage = rawDamage;
            float dm = state.rules.blockHealth(members.first().team);
            if (Mathf.zero(dm)) {
                damage = Float.MAX_VALUE;
            } else {
                damage /= dm;
            }

            float totalHealth = 0f;
            float totalMax = 0f;
            for (ShareWallBuild wall : members) {
                totalHealth += wall.health;
                totalMax += wall.maxHealth();
            }

            totalHealth -= damage;

            if (totalHealth <= 0f) {
                for (int i = members.size - 1; i >= 0; i--) {
                    ShareWallBuild wall = members.get(i);
                    graphMap.remove(wall);
                    wall.health = 0;
                    wall.kill();
                }
                members.clear();
                return;
            }

            float ratio = totalHealth / totalMax;
            for (ShareWallBuild wall : members) {
                wall.health = wall.maxHealth() * ratio;
                wall.healthChanged();
                wall.recentlyHealed();
            }
        }
    }

    public static void markDirty(ShareWallBuild wall) {
        dirtyWalls.add(wall);
    }

    public static void rebuildDirty() {
        if (dirtyWalls.isEmpty()) return;
        ObjectSet<ShareWallBuild> processed = new ObjectSet<>();
        for (ShareWallBuild wall : dirtyWalls) {
            if (!processed.contains(wall) && wall.isValid() && !wall.dead) {
                rebuildGraph(wall);
                HealthGraph graph = graphMap.get(wall);
                if (graph != null) {
                    processed.addAll(graph.members);
                }
            }
        }
        dirtyWalls.clear();
    }

    private static void rebuildGraph(ShareWallBuild start) {
        if (start == null || start.dead || !start.isValid()) return;

        found.clear();
        bfsQueue.clear();
        bfsQueue.add(start);

        while (bfsQueue.size > 0) {
            ShareWallBuild current = bfsQueue.pop();
            if (found.contains(current)) continue;
            if (current.dead || !current.isValid()) continue;
            found.add(current);

            for (Building neighbor : current.proximity) {
                if (neighbor instanceof ShareWallBuild swb
                        && !found.contains(swb)
                        && swb.team == start.team) {
                    bfsQueue.add(swb);
                }
            }
        }

        for (ShareWallBuild wall : found) {
            graphMap.remove(wall);
        }

        HealthGraph newGraph = new HealthGraph();
        for (ShareWallBuild wall : found) {
            newGraph.add(wall);
        }

        if (found.size > 1) {
            Log.info("[ShareWall] 重建血量共享图，成员数: " + found.size);
        }
    }

    public class ShareWallBuild extends WallBuild {

        @Override
        public void update() {
            super.update();
            rebuildDirty();
        }

        @Override
        public void onProximityUpdate() {
            super.onProximityUpdate();
            markDirty(this);
        }

        @Override
        public void damage(float damage) {
            if (net.client()) {
                super.damage(damage);
                return;
            }

            if (damage <= 0 || dead()) return;

            rebuildDirty();

            HealthGraph graph = graphMap.get(this);
            if (graph == null || graph.members.size <= 1) {
                super.damage(damage);
                return;
            }

            Log.info("[ShareWall] 分摊伤害: " + damage + " 成员数: " + graph.members.size);
            graph.applyDamage(damage);
        }

        @Override
        public void onDestroyed() {
            HealthGraph graph = graphMap.get(this);
            if (graph != null) {
                graph.members.remove(this);
                graphMap.remove(this);
                if (graph.members.size > 0) {
                    ShareWallBuild first = graph.members.first();
                    graph.members.clear();
                    graphMap.remove(first);
                    markDirty(first);
                }
            }
            super.onDestroyed();
        }
    }
}
