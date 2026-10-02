package Npl.Rouge;

import arc.*;
import arc.Events;
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;

import mindustry.content.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.storage.*;

import Npl.Rouge.Artifact;
import Npl.Rouge.Artifact.*;

import static mindustry.Vars.*;

/**
 * ArtifactManager —— 藏品管理器
 * ==============================================================
 * 管理藏品库存、效果应用、特殊规则、套装效果
 */
public class ArtifactManager {

    /** 当前持有的藏品 */
    public static Seq<Artifact> inventory = new Seq<>();

    /** 已应用的累计效果（缓存，避免重复计算） */
    public static ObjectMap<EffectType, Float> cachedEffects = new ObjectMap<>();

    /** 效果是否过期需要重新计算 */
    private static boolean dirty = true;

    /** 是否启用 */
    public static boolean enabled = true;

    // ===== 初始化 =====

    public static void init() {
        ArtifactDatabase.load();
        registerEvents();
    }

    private static void registerEvents() {
        // 每帧更新效果
        Events.run(EventType.Trigger.update, () -> {
            if (!enabled || state.isMenu()) return;
            updateEffects();
        });

        // 击杀事件 - 处理击杀掉落
        Events.on(UnitDestroyEvent.class, event -> {
            if (!enabled) return;
            onUnitKill(event.unit);
        });

        // 建筑伤害事件 - 处理建筑回血
        Events.run(EventType.Trigger.update, () -> {
            if (!enabled || state.isMenu()) return;
            updateBuildingRegen();
        });

        // Tap事件 - 处理超频点击
        Events.on(TapEvent.class, event -> {
            if (!enabled || state.isMenu()) return;
            handleOverdrive(event);
        });
    }

    // ===== 藏品管理 =====

    /** 添加藏品 */
    public static void addArtifact(Artifact artifact) {
        if (artifact == null) return;
        // 唯一藏品不重复添加
        if (artifact.unique) {
            for (Artifact a : inventory) {
                if (a.id.equals(artifact.id)) return;
            }
        }
        inventory.add(artifact.copy());
        dirty = true;
    }

    /** 添加藏品（按ID） */
    public static void addArtifact(String id) {
        Artifact def = ArtifactDatabase.get(id);
        if (def != null) addArtifact(def);
    }

    /** 移除藏品 */
    public static void removeArtifact(Artifact artifact) {
        inventory.remove(artifact);
        dirty = true;
    }

    /** 升级藏品 */
    public static void upgradeArtifact(Artifact artifact) {
        if (artifact != null && artifact.canUpgrade()) {
            artifact.upgrade();
            dirty = true;
        }
    }

    /** 清空所有藏品 */
    public static void clear() {
        inventory.clear();
        cachedEffects.clear();
        dirty = true;
    }

    /** 获取藏品数量 */
    public static int count() {
        return inventory.size;
    }

    /** 获取指定套装的藏品数量 */
    public static int getSetCount(String setId) {
        int count = 0;
        for (Artifact a : inventory) {
            if (setId.equals(a.setId)) count++;
        }
        return count;
    }

    // ===== 效果计算 =====

    /** 重新计算所有效果 */
    public static void recalculate() {
        cachedEffects.clear();

        // 累加所有藏品效果
        for (Artifact a : inventory) {
            for (Artifact.Effect e : a.effects) {
                float val = e.value * a.level;
                cachedEffects.put(e.type, cachedEffects.get(e.type, 0f) + val);
            }
        }

        // 应用套装效果
        for (String setId : ArtifactDatabase.setBonuses.keys()) {
            SetBonus bonus = ArtifactDatabase.getSetBonus(setId);
            if (bonus != null && getSetCount(setId) >= bonus.requiredCount) {
                Artifact.Effect e = bonus.bonusEffect;
                cachedEffects.put(e.type, cachedEffects.get(e.type, 0f) + e.value);
            }
        }

        dirty = false;
    }

    /** 获取效果值 */
    public static float getEffect(EffectType type) {
        if (dirty) recalculate();
        return cachedEffects.get(type, 0f);
    }

    /** 更新效果（每帧调用） */
    private static void updateEffects() {
        if (dirty) recalculate();
    }

    // ===== 效果应用 =====

    /** 应用规则效果到 GameRules */
    public static void applyToRules(Object rules) {
        if (rules == null) return;
        if (dirty) recalculate();

        try {
            // 使用反射设置规则字段
            float dmgMult = 1f + getEffect(EffectType.UNIT_DAMAGE);
            float hpMult = 1f + getEffect(EffectType.UNIT_HEALTH);
            float bldHpMult = 1f + getEffect(EffectType.BUILDING_HEALTH);

            Class<?> clazz = rules.getClass();
            java.lang.reflect.Field unitDmgField = clazz.getField("unitDamageMultiplier");
            java.lang.reflect.Field unitHpField = clazz.getField("unitHealthMultiplier");
            java.lang.reflect.Field blockHpField = clazz.getField("blockHealthMultiplier");

            unitDmgField.setFloat(rules, unitDmgField.getFloat(rules) * dmgMult);
            unitHpField.setFloat(rules, unitHpField.getFloat(rules) * hpMult);
            blockHpField.setFloat(rules, blockHpField.getFloat(rules) * bldHpMult);
        } catch (Exception e) {
            Log.err("Failed to apply artifact effects to rules", e);
        }
    }

    // ===== 特殊规则 =====

    /** 单位击杀处理 */
    private static void onUnitKill(Unit unit) {
        if (unit == null) return;

        // 击杀掉落
        float killDropChance = getEffect(EffectType.KILL_DROP);
        if (killDropChance > 0 || Mathf.chance(0.1f + getEffect(EffectType.DROP_RATE))) {
            dropResources(unit);
        }

        // 单位复活
        float reviveChance = getEffect(EffectType.UNIT_REVIVE);
        if (reviveChance > 0 && Mathf.chance(reviveChance)) {
            reviveUnit(unit);
        }
    }

    /** 击杀掉落资源 - 直接添加到玩家核心 */
    private static void dropResources(Unit unit) {
        if (unit == null) return;

        Seq<Item> items = content.items().select(i -> i != null && !i.hidden);
        int count = 1 + Mathf.random(2);
        for (int i = 0; i < count; i++) {
            Item item = items.random();
            if (item != null) {
                // 直接添加到玩家核心
                Building core = Groups.build.find(b -> b.team == player.team() && b instanceof CoreBlock.CoreBuild);
                if (core != null) {
                    core.items.add(item, 1);
                }
            }
        }
    }

    /** 复活单位 */
    private static void reviveUnit(Unit unit) {
        if (unit == null) return;
        UnitType type = unit.type;
        type.spawn(unit.team, unit.x, unit.y);
    }

    /** 建筑回血 */
    private static void updateBuildingRegen() {
        float regen = getEffect(EffectType.BUILDING_REGEN);
        if (regen <= 0) return;

        // 每帧回血，这里用静态计时器控制频率
        if (Time.time % 60 < Time.delta) {
            for (Building build : Groups.build) {
                if (build.team == player.team() && build.health < build.maxHealth()) {
                    build.health = Math.min(build.health + regen, build.maxHealth());
                }
            }
        }
    }

    /** 处理超频点击 */
    private static void handleOverdrive(TapEvent event) {
        if (getEffect(EffectType.OVERDRIVE) <= 0) return;
        if (event.tile == null) return;
        Building build = event.tile.build;
        if (build == null || build.team != player.team()) return;

        // 点击建筑切换超频状态
        if (build.block instanceof Turret) {
            // 超频效果通过其他方式实现，这里仅做提示
            ui.showInfoToast("[accent]超频功能已激活[]", 1.5f);
        }
    }

    // ===== 存档 =====

    /** 保存藏品到 RougeSave */
    public static void save() {
        // 序列化为字符串 id:level,id:level,...
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < inventory.size; i++) {
            Artifact a = inventory.get(i);
            if (i > 0) sb.append(",");
            sb.append(a.id).append(":").append(a.level);
        }
        RougeSave.shortTermMemory.put("artifacts", sb.toString());
    }

    /** 加载藏品 */
    public static void load() {
        inventory.clear();
        Object obj = RougeSave.shortTermMemory.get("artifacts");
        if (obj instanceof String) {
            String data = (String) obj;
            if (data.isEmpty()) return;
            for (String entry : data.split(",")) {
                String[] parts = entry.split(":");
                if (parts.length >= 1) {
                    Artifact def = ArtifactDatabase.get(parts[0]);
                    if (def != null) {
                        Artifact copy = def.copy();
                        if (parts.length >= 2) {
                            try {
                                copy.level = Math.max(1, Integer.parseInt(parts[1]));
                            } catch (NumberFormatException ignored) {}
                        }
                        inventory.add(copy);
                    }
                }
            }
        }
        dirty = true;
    }

    /** 重置（新局开始时调用） */
    public static void reset() {
        inventory.clear();
        cachedEffects.clear();
        dirty = true;
    }

    // ===== UI辅助 =====

    /** 获取按稀有度分组的藏品 */
    public static ObjectMap<Rarity, Seq<Artifact>> getGroupedByRarity() {
        ObjectMap<Rarity, Seq<Artifact>> grouped = new ObjectMap<>();
        for (Artifact a : inventory) {
            if (!grouped.containsKey(a.rarity)) {
                grouped.put(a.rarity, new Seq<>());
            }
            grouped.get(a.rarity).add(a);
        }
        return grouped;
    }

    /** 获取所有已激活的套装效果 */
    public static Seq<SetBonus> getActiveSetBonuses() {
        Seq<SetBonus> active = new Seq<>();
        for (String setId : ArtifactDatabase.setBonuses.keys()) {
            SetBonus bonus = ArtifactDatabase.getSetBonus(setId);
            if (bonus != null && getSetCount(setId) >= bonus.requiredCount) {
                active.add(bonus);
            }
        }
        return active;
    }
}
