package Npl.newSth;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.meta.*;
import Npl.content.*;

import static mindustry.Vars.*;

/**
 * 辐射净化器：输入液体 + 电力，将作用范围内的辐射源方块按映射表替换成对应目标。
 *
 * 每种辐射源方块有各自指定的替换目标：
 *   uranCrystalWall → desertWall
 *   crystalCune     → desert
 *   huiye           → desert
 *   thalliumOre     → desert
 *   ...
 *
 * 未在映射表中的辐射源方块不会被改造。
 */
public class RadiationPurifierBlock extends Block {

    /** 辐射源方块 → 替换目标方块 映射表。 */
    public ObjectMap<Block, Block> blockMap = new ObjectMap<>();

    /** 每周期扫描并替换的时间间隔（帧）。 */
    public float purifyInterval = 30f;

    /** 作用半径（格）。 */
    public float purifyRange = 8f;

    /** 每周期最多替换多少格（防止一次性大改造成卡顿）。 */
    public int maxReplacePerCycle = 3;

    public RadiationPurifierBlock(String name){
        super(name);
        update = true;
        solid = true;
        configurable = false;
        size = 2;
        hasLiquids = true;
        hasPower = true;
        liquidCapacity = 30f;

        consumeLiquid(NuLiquid.dirtySolution, 0.1f);
        consumePower(2f);
    }

    /** 在所有内容加载完成后填充映射表。
     *  注意：构造函数阶段 envBlocks.xxx 还没初始化（为 null），
     *  在那里 put 会触发 ObjectMap "key cannot be null" 崩溃，必须挪到 init()。 */
    @Override
    public void init(){
        super.init();
        blockMap.clear();
        blockMap.put(envBlocks.uranCrystalWall, envBlocks.desertWall);
        blockMap.put(envBlocks.crystalCune, envBlocks.desert);
        blockMap.put(envBlocks.huiye, envBlocks.desert);
        blockMap.put(envBlocks.thalliumOre, envBlocks.desert);
        blockMap.put(envBlocks.glossy, envBlocks.desert);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, purifyRange * tilesize, Pal.plastanium);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.range, purifyRange / 8f, StatUnit.blocks);
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("progress", (RadiationPurifierBuild e) -> new Bar(
            () -> Core.bundle.get("bar.progress"),
            () -> Pal.plastanium,
            () -> e.purifyProgress
        ));
    }

    public class RadiationPurifierBuild extends Building {

        public float purifyTimer = 0f;
        public float purifyProgress = 0f;

        @Override
        public void updateTile(){
            boolean hasLiquid = liquids.get(NuLiquid.dirtySolution) > 0.001f;
            boolean hasPower = power.status > 0.001f;
            if(!hasLiquid || !hasPower){
                purifyProgress = 0f;
                return;
            }

            purifyTimer += Time.delta;
            purifyProgress = Mathf.clamp(purifyTimer / purifyInterval);

            if(purifyTimer >= purifyInterval){
                purifyTimer = 0f;
                purify();
            }
        }

        void purify(){
            float rangeWorld = purifyRange * tilesize;
            int replaced = 0;

            int minX = Mathf.round((x - rangeWorld) / tilesize);
            int maxX = Mathf.round((x + rangeWorld) / tilesize);
            int minY = Mathf.round((y - rangeWorld) / tilesize);
            int maxY = Mathf.round((y + rangeWorld) / tilesize);

            for(int wx = minX; wx <= maxX && replaced < maxReplacePerCycle; wx++){
                for(int wy = minY; wy <= maxY && replaced < maxReplacePerCycle; wy++){
                    Tile t = world.tile(wx, wy);
                    if(t == null) continue;
                    if(Mathf.dst(x, y, wx * tilesize, wy * tilesize) > rangeWorld) continue;

                    Block target = getReplacement(t);
                    if(target == null) continue;

                    if(target instanceof Wall){
                        t.setBlock(target);
                    }else if(target instanceof Floor){
                        t.setFloorNet((Floor)target);
                    }else{
                        continue;
                    }
                    replaced++;
                }
            }
        }

        @Nullable
        Block getReplacement(Tile t){
            Block floor = t.floor();
            Block replacement = blockMap.get(floor);
            if(replacement != null) return replacement;

            Block overlay = t.overlay();
            if(overlay != null){
                replacement = blockMap.get(overlay);
                if(replacement != null) return replacement;
            }

            Block wall = t.block();
            if(wall != null){
                replacement = blockMap.get(wall);
                if(replacement != null) return replacement;
            }

            return null;
        }

        @Override
        public void drawSelect(){
            float rangeWorld = purifyRange * tilesize;
            Drawf.dashCircle(x, y, rangeWorld, Pal.plastanium);
            Draw.color(Pal.plastanium, 0.2f);
            Lines.stroke(1f);
            Lines.circle(x, y, rangeWorld);
            Draw.reset();
        }
    }
}
