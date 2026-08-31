package Npl.newSth;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import Npl.content.NuColor;

public class DamageRedirectAbility extends Ability {
    public float range = 500f;
    public float redirectFactor = 0.75f;

    private final ObjectMap<Unit, Float> unitPrevHealth = new ObjectMap<>();
    private final ObjectMap<Building, Float> buildPrevHealth = new ObjectMap<>();
    private float rangeSq;

    @Override
    public void update(Unit unit) {
        if (unit == null || unit.dead || Groups.unit == null || Groups.build == null) return;

        rangeSq = range * range;

        Groups.unit.intersect(unit.x - range, unit.y - range, range * 2, range * 2, u -> {
            if (u == null || u == unit || u.team != unit.team || u.dead) return;
            float currentHealth = u.health;
            Float prevHealth = unitPrevHealth.get(u);
            if (prevHealth != null && prevHealth > currentHealth + 0.01f) {
                float damage = prevHealth - currentHealth;
                u.health = prevHealth;
                unit.damage(damage * redirectFactor);
                unitPrevHealth.put(u, prevHealth);
            } else {
                unitPrevHealth.put(u, currentHealth);
            }
        });

        Groups.build.each(b -> {
            if (b == null || b.team != unit.team || b.dead || !b.isValid()) return;
            if (Mathf.dst2(unit.x, unit.y, b.x, b.y) > rangeSq) return;
            float currentHealth = b.health;
            Float prevHealth = buildPrevHealth.get(b);
            if (prevHealth != null && prevHealth > currentHealth + 0.01f) {
                float damage = prevHealth - currentHealth;
                float newHealth = Math.min(prevHealth, b.maxHealth());
                b.health = newHealth;
                b.healthChanged();
                b.recentlyHealed();
                unit.damage(damage * redirectFactor);
                buildPrevHealth.put(b, newHealth);
            } else {
                buildPrevHealth.put(b, currentHealth);
            }
        });

        Seq<Unit> staleUnits = new Seq<>();
        for (ObjectMap.Entry<Unit, Float> e : unitPrevHealth.entries()) {
            if (e.key == null || e.key.dead) staleUnits.add(e.key);
        }
        for (Unit u : staleUnits) unitPrevHealth.remove(u);

        Seq<Building> staleBuilds = new Seq<>();
        for (ObjectMap.Entry<Building, Float> e : buildPrevHealth.entries()) {
            if (e.key == null || e.key.dead || !e.key.isValid()) staleBuilds.add(e.key);
        }
        for (Building b : staleBuilds) buildPrevHealth.remove(b);
    }

    @Override
    public void draw(Unit unit) {
        if (unit == null) return;
        Draw.z(Layer.shields);
        Draw.color(NuColor.HonorColor);
        Lines.stroke(1f);
        Lines.circle(unit.x, unit.y, range);
    }

    @Override
    public String localized() { return "伤害转移"; }

    @Override
    public Ability copy() {
        DamageRedirectAbility copy = new DamageRedirectAbility();
        copy.range = this.range;
        copy.redirectFactor = this.redirectFactor;
        return copy;
    }
}
