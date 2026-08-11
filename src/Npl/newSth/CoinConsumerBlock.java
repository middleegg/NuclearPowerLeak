package Npl.newSth;

import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.meta.*;
import Npl.newSth.Type.coins;
import Npl.newSth.consumes.ConsumeCoins;

/**
 * ============================================================
 * 【CoinConsumerBlock = coins 单配方消耗器】
 *  继承 GenericCrafter，通过 ConsumeCoins 消费者集成 coins 消耗。
 *  GenericCrafterBuild 会自动：
 *    - 检查 ConsumeCoins.efficiency() → coins 不够则停工
 *    - 合成完成时调用 ConsumeCoins.trigger() → 自动扣 coins
 *  无需手动覆写 shouldConsume() 或 craft()。
 * ============================================================
 * 使用示例（NuBlocks.java）：
 *   coinConsumer = new CoinConsumerBlock("coin-consumer") {{
 *       requirements(Category.crafting, with(...));
 *       size = 2; health = 1000;
 *       hasItems = true; hasPower = true;
 *       craftTime   = 60f * 5;
 *       coinPerCraft = 10;                                    // ★ 核心接口
 *       outputItem  = new ItemStack(NuItems.bigIron, 100);
 *       consumeItems(ItemStack.with(NuItems.Tcoal, 20));
 *       consumePower(1.5f);
 *   }};
 */
public class CoinConsumerBlock extends GenericCrafter {

    /** ★ 核心接口：每轮合成消耗的 coins 数量 */
    public int coinPerCraft = 0;

    public CoinConsumerBlock(String name) {
        super(name);
        update = true;
        solid = true;
        hasItems = true;
        hasPower = true;
        consumesPower = true;
        size = 2;
        health = 100;
        itemCapacity = 30;
        liquidCapacity = 30f;
    }

    /** init 阶段注册 ConsumeCoins 消费者（此时 coinPerCraft 已被 {{}} 赋值）*/
    @Override
    public void init() {
        if (coinPerCraft > 0) {
            consume(new ConsumeCoins(coinPerCraft));
        }
        super.init();
    }

    @Override
    public void setStats() {
        super.setStats();
        if (coinPerCraft > 0) {
            stats.add(Stat.input, coinPerCraft + " coins/craft");
        }
    }
}
