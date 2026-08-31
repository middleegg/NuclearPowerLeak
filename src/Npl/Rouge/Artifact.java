package Npl.Rouge;

import arc.*;
import arc.graphics.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.type.*;

/**
 * Artifact —— 藏品（集成战略道具）
 * ==============================================================
 * 单局内有效，重置后清空
 * 可叠加、可升级、有套装效果
 */
public class Artifact {

    /** 稀有度枚举 */
    public enum Rarity {
        N(1, "普通", Color.gray),
        R(2, "稀有", Color.blue),
        SR(3, "史诗", Color.purple),
        SSR(4, "传说", Color.gold);

        public final int tier;
        public final String displayName;
        public final Color color;

        Rarity(int tier, String displayName, Color color) {
            this.tier = tier;
            this.displayName = displayName;
            this.color = color;
        }
    }

    /** 效果类型枚举 */
    public enum EffectType {
        // 基础数值
        UNIT_DAMAGE("单位伤害", "所有单位攻击力"),
        UNIT_HEALTH("单位血量", "所有单位生命值"),
        UNIT_SPEED("单位速度", "所有单位移动速度"),
        BUILDING_HEALTH("建筑血量", "所有建筑生命值"),
        BUILDING_DAMAGE("建筑伤害", "炮塔攻击力"),
        CORE_STORAGE("核心存储", "核心资源容量"),

        // 资源相关
        DROP_RATE("掉落率", "击杀敌人掉落资源概率"),
        SELL_BONUS("出售收益", "出售资源获得更多"),
        STARTING_RESOURCES("起始资源", "开局额外资源"),

        // 特殊规则
        KILL_DROP("击杀掉落", "击杀敌人必定掉落资源"),
        BUILDING_REGEN("建筑回血", "建筑缓慢自动修复"),
        UNIT_REVIVE("单位复活", "死亡单位概率复活"),
        OVERDRIVE("超频", "建筑可超频运行"),
        SHIELD("护盾", "开局获得临时护盾"),
        VISION("视野", "扩大战争迷雾视野"),
        AMMO_SAVE("弹药节约", "炮塔射击不消耗弹药"),
        CHAIN("连锁", "攻击概率连锁多个敌人"),
        CRIT("暴击", "攻击概率造成暴击"),
        LIFE_STEAL("吸血", "造成伤害回复生命"),
        SPLASH("溅射", "攻击造成范围伤害"),
        PIERCE("穿透", "子弹穿透多个敌人");

        public final String displayName;
        public final String description;

        EffectType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
    }

    /** 单个效果 */
    public static class Effect {
        public EffectType type;
        public float value;
        public boolean isPercent;

        public Effect(EffectType type, float value, boolean isPercent) {
            this.type = type;
            this.value = value;
            this.isPercent = isPercent;
        }

        public String getDisplayString() {
            if (isPercent) {
                int percent = Math.round(value * 100);
                return (value >= 0 ? "+" : "") + percent + "%";
            } else {
                return (value >= 0 ? "+" : "") + Math.round(value);
            }
        }
    }

    /** 套装定义 */
    public static class SetBonus {
        public String setName;
        public int requiredCount;
        public Effect bonusEffect;

        public SetBonus(String setName, int requiredCount, Effect bonusEffect) {
            this.setName = setName;
            this.requiredCount = requiredCount;
            this.bonusEffect = bonusEffect;
        }
    }

    // ===== 实例字段 =====

    public String id;
    public String name;
    public String description;
    public Rarity rarity;
    public Seq<Effect> effects;
    public String setId;
    public int level;
    public int maxLevel;
    public int iconId;
    public boolean unique;
    public ObjectMap<String, Object> extraData;

    public Artifact() {
        effects = new Seq<>();
        level = 1;
        maxLevel = 1;
        extraData = new ObjectMap<>();
    }

    public Artifact(String id, String name, String description, Rarity rarity,
                    Seq<Effect> effects, String setId, int maxLevel, int iconId, boolean unique) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.rarity = rarity;
        this.effects = effects;
        this.setId = setId;
        this.level = 1;
        this.maxLevel = maxLevel;
        this.iconId = iconId;
        this.unique = unique;
        this.extraData = new ObjectMap<>();
    }

    /** 获取升级后的效果值 */
    public float getEffectValue(EffectType type) {
        for (Effect e : effects) {
            if (e.type == type) {
                return e.value * level;
            }
        }
        return 0;
    }

    /** 是否可以升级 */
    public boolean canUpgrade() {
        return level < maxLevel;
    }

    /** 升级 */
    public void upgrade() {
        if (canUpgrade()) {
            level++;
        }
    }

    /** 获取稀有度颜色 */
    public Color getColor() {
        return rarity != null ? rarity.color : Color.white;
    }

    /** 获取稀有度显示名 */
    public String getRarityName() {
        return rarity != null ? rarity.displayName : "未知";
    }

    /** 获取效果描述文本 */
    public String getEffectsText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < effects.size; i++) {
            Effect e = effects.get(i);
            if (i > 0) sb.append(", ");
            sb.append(e.type.displayName).append(" ").append(e.getDisplayString());
        }
        return sb.toString();
    }

    /** 克隆 */
    public Artifact copy() {
        Artifact copy = new Artifact();
        copy.id = this.id;
        copy.name = this.name;
        copy.description = this.description;
        copy.rarity = this.rarity;
        copy.effects = new Seq<>();
        for (Effect e : this.effects) {
            copy.effects.add(new Effect(e.type, e.value, e.isPercent));
        }
        copy.setId = this.setId;
        copy.level = this.level;
        copy.maxLevel = this.maxLevel;
        copy.iconId = this.iconId;
        copy.unique = this.unique;
        copy.extraData = new ObjectMap<>();
        copy.extraData.putAll(this.extraData);
        return copy;
    }
}
