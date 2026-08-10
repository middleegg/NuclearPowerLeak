package Npl.newSth;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import mindustry.world.consumers.*;
import Npl.newSth.Type.coins;
import Npl.newSth.consumes.ConsumeCoins;

import static mindustry.Vars.*;

/**
 * Coin 消耗机（高级工厂/研究台等）。
 * <p>
 * 通过 ConsumeCoins 消费者实现：
 * - efficiency() 检查 coins 是否足够 → 不够时方块效率为 0，停工
 * - trigger() 在 consume() 时自动扣款（合成完成时）
 * <p>
 * 同时可搭配标准消费者（consumeItems / consumePower）使用。
 * 产出物品走正常 offload 流程。
 */
public class CoinConsumerBlock extends Block {

    /** 合成一次的用时（ticks） */
    public float craftTime = 120f;
    /** 每次合成消耗的 coins 数量 */
    public int coinPerCraft = 0;
    /** 单次合成产出的物品（若为 null，则不输出物品） */
    public ItemStack outputItem = null;

    public Effect craftEffect = Fx.formsmoke;
    public Effect updateEffect = Fx.none;
    public float updateEffectChance = 0.04f;

    public CoinConsumerBlock(String name){
        super(name);
        solid = true;
        update = true;
        hasItems = true;
        acceptsItems = true;
        envEnabled |= Env.space;
    }

    @Override
    public void init(){
        // 自动注册 ConsumeCoins 消费者（coinPerCraft > 0 时）
        // 这样 updateConsumption() 会自动检查 coins 是否够用
        // consume() 时会自动调用 trigger() 扣款
        if(coinPerCraft > 0){
            // 检查 consumeBuilder 是否已添加过 ConsumeCoins
            boolean has = false;
            for(Consume c : consumeBuilder){
                if(c instanceof ConsumeCoins){
                    has = true;
                    break;
                }
            }
            if(!has){
                consume(new ConsumeCoins(coinPerCraft));
            }
        }

        super.init();
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.productionTime, craftTime / 60f, StatUnit.seconds);
        if(outputItem != null){
            stats.add(Stat.output, outputItem);
        }
    }

    @Override
    public boolean outputsItems(){
        return outputItem != null;
    }

    public class CoinConsumerBuild extends Building {

        public float progress;
        public float totalProgress;
        public float warmup;

        @Override
        public void updateTile(){
            // efficiency 由 updateConsumption() 自动设置
            // 它会检查所有消费者：items、power、ConsumeCoins
            // 如果 coins 不够，ConsumeCoins.efficiency() 返回 0 → efficiency = 0 → 方块停工
            if(efficiency > 0){
                progress += getProgressIncrease(craftTime);
                warmup = Mathf.approachDelta(warmup, 1f, 0.1f);
                totalProgress += warmup * Time.delta;

                if(wasVisible && Mathf.chanceDelta(updateEffectChance)){
                    updateEffect.at(x + Mathf.range(size * 2f), y + Mathf.range(size * 2f));
                }
            }else{
                warmup = Mathf.approachDelta(warmup, 0f, 0.1f);
            }

            if(progress >= 1f){
                // consume() 会调用所有消费者的 trigger()
                // ConsumeCoins.trigger() 会自动扣除 coins
                // ConsumeItems.trigger() 会自动扣除 items
                consume();

                // 产出物品
                if(outputItem != null){
                    for(int i = 0; i < outputItem.amount; i++){
                        offload(outputItem.item);
                    }
                }

                craftEffect.at(x, y);
                progress %= 1f;
            }

            // 持续输出物品到传送带
            if(outputItem != null && items != null){
                dumpOutputs();
            }
        }

        private void dumpOutputs(){
            if(outputItem != null && items.get(outputItem.item) > 0){
                dump(outputItem.item);
            }
        }

        @Override
        public boolean shouldConsume(){
            return enabled;
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);
        }

        @Override
        public float warmup(){
            return warmup;
        }

        @Override
        public float totalProgress(){
            return totalProgress;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(progress);
            write.f(totalProgress);
            write.f(warmup);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            progress = read.f();
            totalProgress = read.f();
            warmup = read.f();
        }
    }
}
