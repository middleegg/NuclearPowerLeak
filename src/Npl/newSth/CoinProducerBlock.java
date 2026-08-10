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
import Npl.newSth.Type.coins;

import static mindustry.Vars.*;

/**
 * Coin 生产机（铸币厂）。
 * <p>
 * 参考 GenericCrafter 工作方式：
 * - 消耗 item + power（通过标准 consumer 系统）
 * - 每完成一次 craft，直接 coins.add(coinPerCraft) → 货币入池
 * - 不产出任何 Item 到核心/传送带
 */
public class CoinProducerBlock extends Block {

    /** 合成一次的用时（ticks） */
    public float craftTime = 60f;
    /** 每次合成产出的 coins 数量 */
    public int coinPerCraft = 1;

    public Effect craftEffect = Fx.smeltsmoke;
    public Effect updateEffect = Fx.none;
    public float updateEffectChance = 0.04f;

    public CoinProducerBlock(String name){
        super(name);
        solid = true;
        update = true;
        hasItems = true;
        acceptsItems = true;
        envEnabled |= Env.space;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.productionTime, craftTime / 60f, StatUnit.seconds);
        stats.add(Stat.output, coinPerCraft + " coins / craft");
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    public class CoinProducerBuild extends Building {

        public float progress;
        public float totalProgress;
        public float warmup;

        @Override
        public void updateTile(){
            // efficiency 由 updateConsumption() 自动设置（综合 power + items 等消费者）
            // getProgressIncrease 内部用 edelta() = efficiency * delta，已包含效率
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
                // 扣除原料（items 等）
                consume();

                // 生产 coins（直接入货币池，不产生 item）
                coins.add(coinPerCraft);

                craftEffect.at(x, y);
                progress %= 1f;
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
