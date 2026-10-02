package Npl.Rouge;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;

import Npl.Rouge.Artifact.*;

/**
 * ArtifactDatabase —— 藏品数据库
 * ==============================================================
 * 定义所有可用藏品，20+ 条目，含套装效果
 */
public class ArtifactDatabase {

    /** 所有藏品定义 */
    public static ObjectMap<String, Artifact> artifacts = new ObjectMap<>();

    /** 套装定义 */
    public static ObjectMap<String, SetBonus> setBonuses = new ObjectMap<>();

    /** 套装对应的藏品ID列表 */
    public static ObjectMap<String, Seq<String>> setArtifacts = new ObjectMap<>();

    public static void load() {
        if (!artifacts.isEmpty()) return;

        initSetBonuses();
        initArtifacts();
    }

    private static void initSetBonuses() {
        // 钢铁套装（3件）：单位伤害+30%
        setBonuses.put("steel", new SetBonus("钢铁意志", 3,
                new Effect(EffectType.UNIT_DAMAGE, 0.30f, true)));
        // 疾风套装（2件）：单位速度+25%
        setBonuses.put("wind", new SetBonus("疾风之影", 2,
                new Effect(EffectType.UNIT_SPEED, 0.25f, true)));
        // 坚守套装（3件）：建筑血量+40%
        setBonuses.put("fortress", new SetBonus("钢铁堡垒", 3,
                new Effect(EffectType.BUILDING_HEALTH, 0.40f, true)));
        // 贪婪套装（2件）：掉落率+50%
        setBonuses.put("greed", new SetBonus("贪婪之触", 2,
                new Effect(EffectType.DROP_RATE, 0.50f, true)));
    }

    private static void initArtifacts() {
        // ===== 普通藏品 (N) =====

        add("broken_blade", "破损的刀片", "一把生锈的刀片，仍能增加些许攻击力",
                Rarity.N, Seq.with(new Effect(EffectType.UNIT_DAMAGE, 0.05f, true)),
                null, 3, 0, false);

        add("worn_armor", "磨损的护甲", "破旧的护甲，提供少量防护",
                Rarity.N, Seq.with(new Effect(EffectType.UNIT_HEALTH, 0.05f, true)),
                null, 3, 1, false);

        add("old_coin", "古旧硬币", "一枚旧币，据说能带来财运",
                Rarity.N, Seq.with(new Effect(EffectType.SELL_BONUS, 0.10f, true)),
                null, 2, 2, false);

        add("rusty_gear", "生锈齿轮", "机械零件，可小幅提升建筑耐久",
                Rarity.N, Seq.with(new Effect(EffectType.BUILDING_HEALTH, 0.08f, true)),
                "fortress", 3, 3, false);

        add("swift_feather", "迅捷之羽", "轻盈的羽毛，让单位移动更快",
                Rarity.N, Seq.with(new Effect(EffectType.UNIT_SPEED, 0.06f, true)),
                "wind", 3, 4, false);

        add("steel_nail", "钢铁钉子", "坚固的钢铁，增强单位攻击",
                Rarity.N, Seq.with(new Effect(EffectType.UNIT_DAMAGE, 0.06f, true)),
                "steel", 3, 5, false);

        add("small_pouch", "小布袋", "能装更多资源",
                Rarity.N, Seq.with(new Effect(EffectType.CORE_STORAGE, 200f, false)),
                null, 2, 6, false);

        add("lucky_clover", "幸运四叶草", "概率带来好运",
                Rarity.N, Seq.with(new Effect(EffectType.DROP_RATE, 0.10f, true)),
                "greed", 2, 7, false);

        // ===== 稀有藏品 (R) =====

        add("sharp_sword", "锋利长剑", "锋利的武器，显著提升攻击力",
                Rarity.R, Seq.with(new Effect(EffectType.UNIT_DAMAGE, 0.12f, true)),
                "steel", 3, 8, false);

        add("iron_shard", "钢铁碎片", "强化单位的攻击和血量",
                Rarity.R, Seq.with(
                        new Effect(EffectType.UNIT_DAMAGE, 0.08f, true),
                        new Effect(EffectType.UNIT_HEALTH, 0.08f, true)),
                "steel", 3, 9, false);

        add("wind_boots", "疾风之靴", "让单位移动如风",
                Rarity.R, Seq.with(
                        new Effect(EffectType.UNIT_SPEED, 0.15f, true),
                        new Effect(EffectType.UNIT_DAMAGE, 0.05f, true)),
                "wind", 3, 10, false);

        add("fortress_block", "堡垒模块", "建筑血量大幅提升",
                Rarity.R, Seq.with(new Effect(EffectType.BUILDING_HEALTH, 0.20f, true)),
                "fortress", 3, 11, false);

        add("greedy_ring", "贪婪之戒", "出售资源获得更多收益",
                Rarity.R, Seq.with(
                        new Effect(EffectType.SELL_BONUS, 0.25f, true),
                        new Effect(EffectType.DROP_RATE, 0.15f, true)),
                "greed", 3, 12, false);

        add("ammo_box", "弹药箱", "炮塔射击节约弹药",
                Rarity.R, Seq.with(new Effect(EffectType.AMMO_SAVE, 0.20f, true)),
                null, 2, 13, false);

        add("medkit", "医疗包", "单位缓慢回复生命",
                Rarity.R, Seq.with(new Effect(EffectType.UNIT_HEALTH, 0.10f, true)),
                null, 2, 14, false);

        add("vision_lens", "视野透镜", "扩大视野范围",
                Rarity.R, Seq.with(new Effect(EffectType.VISION, 1.5f, true)),
                null, 2, 15, false);

        // ===== 史诗藏品 (SR) =====

        add("crystal_blade", "水晶之刃", "蕴含魔力的剑，攻击可连锁敌人",
                Rarity.SR, Seq.with(
                        new Effect(EffectType.UNIT_DAMAGE, 0.15f, true),
                        new Effect(EffectType.CHAIN, 0.15f, true)),
                "steel", 2, 16, false);

        add("thunder_hammer", "雷霆之锤", "攻击造成范围伤害",
                Rarity.SR, Seq.with(
                        new Effect(EffectType.UNIT_DAMAGE, 0.12f, true),
                        new Effect(EffectType.SPLASH, 0.20f, true)),
                "steel", 2, 17, false);

        add("phantom_cloak", "幻影披风", "单位攻击有概率暴击",
                Rarity.SR, Seq.with(
                        new Effect(EffectType.UNIT_DAMAGE, 0.10f, true),
                        new Effect(EffectType.CRIT, 0.15f, true)),
                "wind", 2, 18, false);

        add("regen_core", "再生核心", "建筑和单位都自动回复",
                Rarity.SR, Seq.with(
                        new Effect(EffectType.BUILDING_REGEN, 5f, false),
                        new Effect(EffectType.UNIT_HEALTH, 0.10f, true)),
                "fortress", 2, 19, false);

        add("treasure_map", "藏宝图", "击杀敌人必定掉落资源",
                Rarity.SR, Seq.with(
                        new Effect(EffectType.KILL_DROP, 1.0f, true),
                        new Effect(EffectType.DROP_RATE, 0.30f, true)),
                "greed", 2, 20, false);

        add("overdrive_chip", "超频芯片", "建筑可超频运行",
                Rarity.SR, Seq.with(
                        new Effect(EffectType.OVERDRIVE, 1.0f, true),
                        new Effect(EffectType.BUILDING_DAMAGE, 0.15f, true)),
                "fortress", 2, 21, false);

        add("piercing_round", "穿甲弹", "子弹穿透多个敌人",
                Rarity.SR, Seq.with(
                        new Effect(EffectType.PIERCE, 1.0f, true),
                        new Effect(EffectType.UNIT_DAMAGE, 0.08f, true)),
                null, 2, 22, false);

        // ===== 传说藏品 (SSR) =====

        add("dragon_heart", "龙之心", "传说级藏品，全面强化",
                Rarity.SSR, Seq.with(
                        new Effect(EffectType.UNIT_DAMAGE, 0.25f, true),
                        new Effect(EffectType.UNIT_HEALTH, 0.25f, true),
                        new Effect(EffectType.UNIT_SPEED, 0.15f, true)),
                "steel", 1, 23, true);

        add("phoenix_feather", "凤凰之羽", "单位死亡后概率复活",
                Rarity.SSR, Seq.with(
                        new Effect(EffectType.UNIT_REVIVE, 0.30f, true),
                        new Effect(EffectType.UNIT_HEALTH, 0.15f, true)),
                "fortress", 1, 24, true);

        add("void_orb", "虚空之球", "攻击吸血，持续作战",
                Rarity.SSR, Seq.with(
                        new Effect(EffectType.LIFE_STEAL, 0.15f, true),
                        new Effect(EffectType.UNIT_DAMAGE, 0.20f, true)),
                "steel", 1, 25, true);

        add("time_sandglass", "时空沙漏", "超频所有建筑，攻速翻倍",
                Rarity.SSR, Seq.with(
                        new Effect(EffectType.OVERDRIVE, 1.0f, true),
                        new Effect(EffectType.BUILDING_DAMAGE, 0.50f, true)),
                "fortress", 1, 26, true);

        add("greed_idol", "贪婪神像", "资源收益最大化",
                Rarity.SSR, Seq.with(
                        new Effect(EffectType.DROP_RATE, 1.0f, true),
                        new Effect(EffectType.SELL_BONUS, 0.50f, true),
                        new Effect(EffectType.KILL_DROP, 1.0f, true)),
                "greed", 1, 27, true);
    }

    private static void add(String id, String name, String description, Rarity rarity,
                            Seq<Effect> effects, String setId, int maxLevel, int iconId, boolean unique) {
        Artifact a = new Artifact(id, name, description, rarity, effects, setId, maxLevel, iconId, unique);
        artifacts.put(id, a);

        if (setId != null) {
            if (!setArtifacts.containsKey(setId)) {
                setArtifacts.put(setId, new Seq<>());
            }
            setArtifacts.get(setId).add(id);
        }
    }

    /** 获取藏品定义 */
    public static Artifact get(String id) {
        return artifacts.get(id);
    }

    /** 获取随机藏品（按稀有度权重） */
    public static Artifact getRandom(Rand rand) {
        if (artifacts.isEmpty()) load();
        int roll = rand.nextInt(100);
        Rarity targetRarity;
        if (roll < 50) targetRarity = Rarity.N;
        else if (roll < 80) targetRarity = Rarity.R;
        else if (roll < 95) targetRarity = Rarity.SR;
        else targetRarity = Rarity.SSR;

        Seq<Artifact> pool = new Seq<>();
        for (Artifact a : artifacts.values()) {
            if (a.rarity == targetRarity) pool.add(a);
        }
        if (pool.isEmpty()) {
            pool = artifacts.values().toSeq();
        }
        if (pool.isEmpty()) return null;
        return pool.random(rand).copy();
    }

    /** 获取指定稀有度的随机藏品 */
    public static Artifact getRandom(Rand rand, Rarity rarity) {
        if (artifacts.isEmpty()) load();
        Seq<Artifact> pool = new Seq<>();
        for (Artifact a : artifacts.values()) {
            if (a.rarity == rarity) pool.add(a);
        }
        if (pool.isEmpty()) return null;
        return pool.random(rand).copy();
    }

    /** 获取套装效果 */
    public static SetBonus getSetBonus(String setId) {
        return setBonuses.get(setId);
    }

    /** 获取所有藏品数量 */
    public static int count() {
        return artifacts.size;
    }
}
