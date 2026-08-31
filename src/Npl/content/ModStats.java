package Npl.content;

import arc.util.Strings;
import mindustry.content.StatusEffects;
import mindustry.entities.bullet.BulletType;
import mindustry.ui.Styles;
import mindustry.world.consumers.ConsumeItems;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatCat;
import mindustry.world.meta.Stats;
import mindustry.world.meta.StatUnit;
import mindustry.world.meta.StatValues;
import Npl.newSth.*;
import Npl.newSth.walls.NuHealerProjector;

public class ModStats {
    public static final StatCat multiMode = new StatCat("multiMode");
    public static final StatCat tianfa = new StatCat("tianfa");

    // 定义你的自定义 Stat
    public static final Stat Reversible = new Stat("reversible", StatCat.general);
    public static final Stat Magentic = new Stat("magentic", StatCat.general);
    public static final Stat Stability = new Stat("stability", StatCat.general);
    public static final Stat Recipe = new Stat("recipe", StatCat.general);
    public static final Stat modeCount = new Stat("modeCount", StatCat.general);
    public static final Stat bloody = new Stat("bloody",StatCat.general);
    public static final Stat maxReload = new Stat("maxReload",StatCat.general);
    public static final Stat weaky = new Stat("weaky",StatCat.general);
    public static final Stat ticksW = new Stat("ticksW",StatCat.general);
    public static final Stat modeInfo = new Stat("modeInfo", multiMode);
    // 天罚（TianFa）详情面板用的自定义 Stat
    public static final Stat tianfaInfo = new Stat("tianfaInfo", tianfa);
    public static final Stat tianfaAmmo = new Stat("tianfaAmmo", tianfa);
    // 治疗塔（NuHealerProjector）详情面板用的自定义分类与 Stat
    public static final StatCat healer = new StatCat("healer");
    public static final Stat healerInfo = new Stat("healerInfo", healer);
    // 如果你以后想添加更多自定义属性，在这里继续加：
    // public static final Stat MAGIC_POWER = new Stat("magic-power", StatCat.general);

    /** 天罚详情面板：机制说明 + 各弹药物品的打击参数。在 setStats() 的 super 调用之后调用。 */
    public static void buildTianFaStats(TianFa block, Stats stats){
        // 移除原版炮塔数值，改用自定义机制说明（做法对齐 MultiPowerTurret）
        stats.remove(Stat.damage);
        stats.remove(Stat.reload);
        stats.remove(Stat.range);
        stats.remove(Stat.ammo);

        // —— 天罚机制 ——
        stats.add(tianfaInfo, t -> {
            t.background(Styles.black6);
            t.margin(6f);
            t.add("[accent]弹药存放制").left().row();
            t.add("[lightgray]存量上限: " + block.maxStock + " 发").left().row();
            t.add("[lightgray]存放间隔: " + Strings.autoFixed(block.stockInterval / 60f, 2) + "s / 发").left().row();
            t.add("[lightgray]齐射间隔: " + Strings.autoFixed(block.waveDelay / 60f, 2) + "s").left().row();

            t.add("[accent]天降打击").left().padTop(4f).row();
            t.add("[lightgray]预警: " + Strings.autoFixed(block.warnTime, 2) + "s   坠落: " + Strings.autoFixed(block.fallTime, 2) + "s").left().row();
            t.add("[lightgray]打击半径: " + (int)block.strikeRadius + " " + StatUnit.blocks.localized()
                + "   中心伤害: " + (int)block.strikeDamage).left().row();

            StringBuilder fx = new StringBuilder();
            if(block.applyBurn) fx.append("点燃(").append(Strings.autoFixed(block.burnTime, 1)).append("s) ");
            if(block.applyKnockback) fx.append("击退 ");
            if(block.applySlow) fx.append("减速(").append(Strings.autoFixed(block.slowTime, 1)).append("s) ");
            if(block.leaveLinger) fx.append("残留区(").append(Strings.autoFixed(block.lingerTime, 0)).append("s, ").append((int)block.lingerDps).append("/s) ");
            t.add("[accent]附效").left().padTop(4f).row();
            t.add("[lightgray]" + (fx.length() == 0 ? "无" : fx.toString().trim())).left();
        });

        // —— 各弹药物品的打击参数 ——
        for(var e : block.ammoTypes){
            BulletType bt = e.value;
            if(bt == null) continue;

            float dmg = bt.damage > 0f ? bt.damage : bt.splashDamage;
            if(dmg <= 0f) dmg = block.strikeDamage;
            float radius = bt.splashDamageRadius > 0f ? bt.splashDamageRadius : block.strikeRadius;

            String itemName = e.key.localizedName;
            String params = "伤害: " + (int)dmg + "   半径: " + (int)radius + " " + StatUnit.blocks.localized();
            String statusName = bt.status != null && bt.status != StatusEffects.none ? bt.status.localizedName : null;
            String fxDesc = TianFa.ammoLandFx(bt);

            stats.add(tianfaAmmo, t -> {
                t.background(Styles.black6);
                t.margin(6f);
                t.add("[accent]" + itemName).left().row();
                t.add("[lightgray]" + params).left();
                if(statusName != null){
                    t.row().add("[lightgray]状态: " + statusName).left();
                }
                if(fxDesc.length() > 0){
                    t.row().add("[lightgray]落地: " + fxDesc).left();
                }
            });
        }
    }

    /** 治疗塔详情面板：治疗数值 / 晶体爆炸对敌伤害 / 晶体对友方治疗。在 setStats() 的 super 调用之后调用。 */
    public static void buildHealerStats(NuHealerProjector block, Stats stats){
        stats.add(Stat.repairTime, (int)(block.chargeTime / 60f), StatUnit.seconds);
        stats.add(Stat.range, block.range, StatUnit.blocks);
        if (block.findConsumer(c -> c instanceof ConsumeItems) instanceof ConsumeItems cons) {
            stats.remove(Stat.booster);
            stats.add(Stat.booster, StatValues.itemBoosters(
                    "{0}" + StatUnit.timesSpeed.localized(),
                    stats.timePeriod, block.optionalMultiplier, 0f,
                    cons.items)
            );
        }

        // —— 治疗参数：单次治疗数值 / 晶体爆炸对敌伤害 / 晶体对友方治疗 ——
        float baseHealPct = block.healPercent * block.chargeTime;                 // 基准：每次治疗的建筑最大生命百分比
        float boostedHealPct = baseHealPct * block.optionalMultiplier;            // 满速加成后
        float explodeTiles = block.explosionRange / mindustry.Vars.tilesize;    // 爆炸半径（世界单位 → 格）

        stats.add(healerInfo, t -> {
            t.background(Styles.black6);
            t.margin(6f);

            t.add("[accent]单次治疗（建筑）").left().row();
            t.add("[lightgray]基准: " + Strings.autoFixed(baseHealPct, 1) + "% 建筑最大生命 / 次").left().row();
            t.add("[lightgray]满速加成: ×" + Strings.autoFixed(block.optionalMultiplier, 1)
                    + " → " + Strings.autoFixed(boostedHealPct, 1) + "%").left().row();
            t.add("[lightgray]治疗波增幅: ×1.0 → ×2.0（波间叠加，逐次递减）").left().row();
            t.add("[lightgray]分层: " + block.maxLayers + " 层，逐层半径 +" + (int)(block.layerRadiusStep * 100f)
                    + "%，层间隔 " + Strings.autoFixed(block.layerDelay / 60f, 2) + "s").left().row();

            t.add("[accent]晶体爆炸（对敌方）").left().padTop(4f).row();
            t.add("[lightgray]伤害: " + (int)block.explosionDamage + " / 单位").left().row();
            t.add("[lightgray]半径: " + Strings.autoFixed(explodeTiles, 1) + " " + StatUnit.blocks.localized()).left().row();

            t.add("[accent]晶体治疗（对友方单位）").left().padTop(4f).row();
            t.add("[lightgray]单次回复: " + (int)block.unitHealAmount + " × 治疗波增幅").left();
        });
    }
}
