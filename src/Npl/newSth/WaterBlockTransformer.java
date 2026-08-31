package Npl.newSth;

import arc.*;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ctype.ContentType;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.meta.*;
import Npl.content.*;

import static mindustry.Vars.*;

/**
 * 水体改造器：只能建造在水中，自动洪泛检测整片相连水体，将其改造为目标液体地板。
 *
 * 机制：
 *  - 只能放在液体地板上（canPlaceOn 校验）
 *  - 放置后自动洪泛检测整个相连水体（无范围限制，直到断开）
 *  - 只能选择液体地板作为目标
 *  - 按格计时：总时间 = 格数 × 每格秒数；持续耗液+耗电；进度条显示
 *  - 自动匹配输入液体：huiye→dirtySolution, strangeLiquid→water
 */
public class WaterBlockTransformer extends Block {

    /** 替换后的默认目标液体地板。 */
    public Floor targetFloor = (Floor)envBlocks.huiye;

    /** 每格水体消耗的输入液体量。 */
    public float liquidPerTile = 3f;

    /** 改造每格所需时间（秒）。 */
    public float timePerTile = 2f;

    /** 水体格数上限，超过此数量拒绝改造。 */
    public int maxWaterTiles = 200;

    /** 源液体名称 → 所需输入液体名称的映射。 */
    public ObjectMap<String, String> liquidMap = new ObjectMap<>();

    public WaterBlockTransformer(String name){
        super(name);
        update = true;
        solid = true;
        configurable = true;
        hasLiquids = true;
        hasPower = true;
        size = 2;
        liquidCapacity = 500f;
        outputsLiquid = false;

        consumePower(3f);

        liquidMap.put("huiye", "dirtySolution");
        liquidMap.put("strangeLiquid", "water");
        liquidMap.put("nuclearFluid", "dirtySolution");
        liquidMap.put("water", "dirtySolution");
    }

    public Liquid getRequiredInput(Liquid source){
        if(source == null) return NuLiquid.dirtySolution;
        String inputName = liquidMap.get(source.name);
        if(inputName != null){
            Liquid found = content.liquid(inputName);
            if(found != null) return found;
        }
        return NuLiquid.dirtySolution;
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        super.canPlaceOn(tile, team, rotation);
        Floor f = tile.floor();
        return f != null && f.isLiquid && isSourceLiquid(f.liquidDrop);
    }

    boolean isSourceLiquid(Liquid l){
        return l != null && liquidMap.containsKey(l.name);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.range, maxWaterTiles, StatUnit.blocks);
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("progress", (WaterBlockTransformerBuild e) -> new Bar(
            () -> Core.bundle.get("bar.progress"),
            () -> Pal.heal,
            () -> e.progress
        ));
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Tile t = world.tile(x, y);
        if(t != null && t.floor().isLiquid){
            Drawf.dashCircle(x * tilesize, y * tilesize, 4f * tilesize, Pal.heal);
        }
    }

    public class WaterBlockTransformerBuild extends Building {

        public Floor target;
        public float timer = 0f;
        public float progress = 0f;
        public int lastTileCount = 0;
        public boolean overLimit = false;
        public float totalTime = 0f;
        public Liquid sourceLiquid;
        public Liquid inputLiquid;

        @Override
        public void buildConfiguration(Table table){
            table.table(Tex.button, t -> {
                t.defaults().size(200f, 50f);
                t.button("[green]目标液体: " + (target == null ? "无" : target.localizedName), this::showFloorPicker).get();
            }).pad(4f);
        }

        private void showFloorPicker(){
            BaseDialog dialog = new BaseDialog("选择目标液体地板");
            dialog.cont.pane(Styles.noBarPane, list -> {
                list.defaults().size(220f, 46f).pad(2f);
                int col = 0;
                for(Block b : content.blocks()){
                    if(!(b instanceof Floor)) continue;
                    Floor f = (Floor)b;
                    if(!f.isLiquid) continue;
                    list.button(f.localizedName, () -> {
                        configure(f);
                        dialog.hide();
                    });
                    if(++col % 3 == 0) list.row();
                }
            }).size(720f, 480f);
            dialog.addCloseButton();
            dialog.show();
        }

        @Override
        public void configure(Object value){
            target = (Floor) value;
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(target == null ? -1 : target.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1){
                short tid = read.s();
                target = tid < 0 ? null : (Floor) content.getByID(ContentType.block, tid);
            }
        }

        @Override
        public void updateTile(){
            sourceLiquid = tile.floor().liquidDrop;

            if(target == null || sourceLiquid == null){
                progress = 0f;
                return;
            }

            boolean hasPower = power.status > 0.001f;
            if(!hasPower){
                progress = 0f;
                return;
            }

            Seq<Tile> waterTiles = scanWater();
            lastTileCount = waterTiles.size;

            if(waterTiles.size == 0){
                overLimit = false;
                progress = 0f;
                timer = 0f;
                return;
            }

            if(waterTiles.size > maxWaterTiles){
                overLimit = true;
                progress = 0f;
                return;
            }

            overLimit = false;
            inputLiquid = getRequiredInput(sourceLiquid);
            totalTime = waterTiles.size * timePerTile;

            float available = liquids.get(inputLiquid);
            if(available < 0.001f){
                return;
            }

            timer += Time.delta;
            progress = totalTime > 0.001f ? Mathf.clamp(timer / totalTime) : 1f;

            float totalNeed = waterTiles.size * liquidPerTile;
            float consumeAmount = (totalNeed / totalTime) * Time.delta;
            if(consumeAmount > 0f){
                liquids.remove(inputLiquid, Math.min(consumeAmount, available));
            }

            if(timer >= totalTime){
                timer = 0f;
                progress = 0f;
                doTransform(waterTiles);
            }
        }

        void doTransform(Seq<Tile> waterTiles){
            if(inputLiquid == null) return;
            float totalNeed = waterTiles.size * liquidPerTile;
            if(liquids.get(inputLiquid) < totalNeed) return;

            liquids.remove(inputLiquid, totalNeed);
            for(Tile t : waterTiles){
                t.setFloorNet(target);
            }
        }

        Seq<Tile> scanWater(){
            Seq<Tile> result = new Seq<>();
            IntSet visited = new IntSet();
            Queue<Tile> queue = new Queue<>();

            Liquid src = sourceLiquid;

            Tile start = tile;
            if(start != null && start.floor().liquidDrop == src){
                queue.addLast(start);
                visited.add(start.pos());
            }

            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

            while(!queue.isEmpty()){
                if(result.size >= maxWaterTiles + 1) break;

                Tile t = queue.removeFirst();
                result.add(t);

                for(int[] d : dirs){
                    int nx = t.x + d[0];
                    int ny = t.y + d[1];
                    Tile n = world.tile(nx, ny);
                    if(n == null) continue;
                    if(visited.contains(n.pos())) continue;
                    if(n.floor().liquidDrop != src) continue;

                    visited.add(n.pos());
                    queue.addLast(n);
                }
            }

            return result;
        }

        @Override
        public void drawSelect(){
            Seq<Tile> tiles = scanWater();
            Color c = overLimit ? Color.red : Color.green;
            for(Tile t : tiles){
                Draw.color(c);
                Draw.alpha(0.4f);
                Fill.rect(t.worldx(), t.worldy(), 4f, 4f);
            }
            Draw.reset();
        }
    }
}
