package Npl.newSth.consumes;

import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import Npl.newSth.Type.coins;

/**
 * 货币消费者（per-craft 模式，参考 ConsumeItems）。
 * <p>
 * - efficiency() 检查当前 coins 是否够一次合成，不够返回 0 → 方块停工
 * - trigger() 在 consume() 时实际扣款（合成完成时调用）
 * <p>
 * 用法：
 *   consume(new ConsumeCoins(10));  // 每次合成消耗 10 coins
 */
public class ConsumeCoins extends Consume {

    /** 每次合成消耗的 coins 数量 */
    public int amount;

    public ConsumeCoins(int amount){
        this.amount = amount;
    }

    public ConsumeCoins(){
        this(1);
    }

    @Override
    public void apply(Block block){
        // coins 是独立系统，不需要 hasItems / hasPower 等标记
    }

    /** 检查 coins 是否足够 → 不够则效率为 0，方块停工 */
    @Override
    public float efficiency(Building build){
        if(amount <= 0) return 1f;
        return coins.getAmount() >= amount ? 1f : 0f;
    }

    /** 合成完成时实际扣款（由 Building.consume() 调用） */
    @Override
    public void trigger(Building build){
        if(amount > 0){
            coins.spend(amount);
        }
    }

    @Override
    public void display(Stats stats){
        if(amount > 0){
            stats.add(booster ? Stat.booster : Stat.input, amount + " coins/craft");
        }
    }
}
