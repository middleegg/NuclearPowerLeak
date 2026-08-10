package Npl.newSth;

import arc.scene.ui.layout.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.consumers.Consume;
import mindustry.world.meta.*;
import Npl.content.Recipe;

import static mindustry.Vars.*;


/**
 * 类似 NewHorizon 的 ConsumeRecipe：
 *   把一个 Npl.content.Recipe 封装成 Mindustry 标准的 Consume。
 * 支持：
 *   - 物品输入 (Recipe.inputItem  Seq<ItemStack>)
 *   - 液体输入 (Recipe.inputLiquid Seq<LiquidStack>)
 *   - 电力: 由 ConsumePower 单独处理（本类不包办）
 *
 * 严格按原版 ConsumeItems / ConsumeLiquid / ConsumeLiquidBase 同签名范式实现:
 *   apply / build / trigger / update / efficiency / display
 */
public class ConsumeCon extends Consume {

    public Recipe recipe;

    public ConsumeCon(Recipe recipe) {
        this.recipe = recipe == null ? new Recipe() : recipe;
    }

    protected ConsumeCon() {
        this(null);
    }

    // —————————————————————————————————— apply：给方块打属性标记
    @Override
    public void apply(Block block) {
        if (recipe == null) return;

        if (recipe.inputItem != null && recipe.inputItem.any()) {
            block.hasItems = true;
            block.acceptsItems = true;
            for (ItemStack s : recipe.inputItem) {
                if (s == null || s.item == null) continue;
                if (s.item.id < block.itemFilter.length) block.itemFilter[s.item.id] = true;
            }
        }
        if (recipe.outputItem != null && recipe.outputItem.any()) {
            block.hasItems = true;
            for (ItemStack s : recipe.outputItem) {
                if (s == null || s.item == null) continue;
                if (s.item.id < block.itemFilter.length) block.itemFilter[s.item.id] = true;
            }
        }

        if ((recipe.inputLiquid  != null && recipe.inputLiquid .any())
         || (recipe.outputLiquid != null && recipe.outputLiquid.any())) {
            block.hasLiquids = true;
        }
        if (recipe.inputLiquid != null) {
            for (LiquidStack s : recipe.inputLiquid) {
                if (s == null || s.liquid == null) continue;
                if (s.liquid.id < block.liquidFilter.length) block.liquidFilter[s.liquid.id] = true;
            }
        }
        if (recipe.outputLiquid != null) {
            for (LiquidStack s : recipe.outputLiquid) {
                if (s == null || s.liquid == null) continue;
                if (s.liquid.id < block.liquidFilter.length) block.liquidFilter[s.liquid.id] = true;
            }
        }
    }

    // —————————————————————————————————— build：方块详情面板里"需要的资源"UI
    @Override
    public void build(Building build, Table table) {
        if (recipe == null) return;
        table.top().left();
        table.table(c -> {
            c.top().left();
            int i = 0;
            if (recipe.inputItem != null) for (ItemStack s : recipe.inputItem) {
                if (s == null || s.item == null) continue;
                int amt = Math.round(s.amount * multiplier.get(build));
                c.add(new ReqImage(
                        StatValues.stack(s.item, amt),
                        () -> build.items == null ? false : build.items.has(s.item, amt)
                )).padRight(8).top().left();
                if (++i % 4 == 0) c.row();
            }
            if (recipe.inputLiquid != null) for (LiquidStack s : recipe.inputLiquid) {
                if (s == null || s.liquid == null) continue;
                c.add(new ReqImage(
                        s.liquid.uiIcon,
                        () -> build.liquids == null ? false : build.liquids.get(s.liquid) > 0
                )).size(iconMed).padRight(8).top().left();
                if (++i % 4 == 0) c.row();
            }
        }).top().left();
    }

    // —————————————————————————————————— trigger：生产完成那一刻调用（= 扣原料）
    @Override
    public void trigger(Building build) {
        if (recipe == null) return;
        float mult = multiplier.get(build);

        if (recipe.inputItem != null) for (ItemStack s : recipe.inputItem) {
            if (s == null || s.item == null) continue;
            build.items.remove(s.item, Math.round(s.amount * mult));
        }
        // 液体 trigger 是一次扣完（不乘 edelta，和物品语义一致）
        if (recipe.inputLiquid != null) for (LiquidStack s : recipe.inputLiquid) {
            if (s == null || s.liquid == null) continue;
            build.liquids.remove(s.liquid, s.amount * mult);
        }
    }

    // —————————————————————————————————— update：液体每 tick 流式扣（物品/载荷不在这里扣）
    @Override
    public void update(Building build) {
        if (recipe == null) return;
        if (recipe.inputLiquid == null || !recipe.inputLiquid.any()) return;
        float mult = multiplier.get(build);
        for (LiquidStack s : recipe.inputLiquid) {
            if (s == null || s.liquid == null) continue;
            // 完全照抄 ConsumeLiquid.update 行 38
            build.liquids.remove(s.liquid, s.amount * build.edelta() * mult);
        }
    }

    // —————————————————————————————————— efficiency：原料够不够？1 够 / 0 不够 / 液体 0~1
    @Override
    public float efficiency(Building build) {
        if (recipe == null) return 0f;
        if (build.consumeTriggerValid()) return 1f;
        float mult = multiplier.get(build);

        if (recipe.inputItem != null) {
            for (ItemStack s : recipe.inputItem) {
                if (s == null || s.item == null) continue;
                int need = Math.round(s.amount * mult);
                if (!build.items.has(s.item, need)) return 0f;
            }
        }

        float liqEff = 1f;
        float ed = Math.max(build.edelta() * build.efficiencyScale(), 0.00000001f);
        if (recipe.inputLiquid != null) {
            for (LiquidStack s : recipe.inputLiquid) {
                if (s == null || s.liquid == null) continue;
                float need = s.amount * ed * mult;
                if (need <= 0f) continue;
                liqEff = Math.min(liqEff, build.liquids.get(s.liquid) / need);
                if (liqEff <= 0f) return 0f;
            }
        }
        liqEff = Math.min(liqEff, 1f);
        if (liqEff < 1f) return liqEff;

        return 1f;
    }

    // —————————————————————————————————— display：方块属性面板（Stat 菜单）里的显示
    @Override
    public void display(Stats stats) {
        if (recipe == null) return;
        Stat input = booster ? Stat.booster : Stat.input;

        if (recipe.inputItem != null && recipe.inputItem.any()) {
            if (stats.timePeriod < 0) {
                stats.add(input, StatValues.items(recipe.inputItem));
            } else {
                stats.add(input, StatValues.items(stats.timePeriod, recipe.inputItem));
            }
        }
        if (recipe.inputLiquid != null) for (LiquidStack s : recipe.inputLiquid) {
            if (s == null || s.liquid == null) continue;
            // 照抄 ConsumeLiquid.display 行 51：amount * 60f → /秒
            stats.add(input, s.liquid, s.amount * 60f, true);
        }
    }
}
