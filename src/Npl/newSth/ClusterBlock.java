package Npl.newSth;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.graphics.*;
import mindustry.graphics.MultiPacker.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import Npl.content.*;

import static mindustry.Vars.*;
import static mindustry.world.Block.*;

public class ClusterBlock extends TallBlock{
    public boolean wallOre = true; // 👈 自己定义一个 wallOre 字段
    public ClusterBlock(String name,Item ore){
        super(name);
        this.localizedName = ore.localizedName;
        this.itemDrop = ore;
        this.variants = 3;
        this.mapColor.set(ore.color);
        this.useColor = true;

        // 必须的挖掘属性
        this.solid = true;
        this.breakable = true;
        this.alwaysUnlocked = true; // 不需要研究就能挖
    };
    public void setup(Item ore){
        this.localizedName = ore.localizedName + (wallOre ? " " + Core.bundle.get("wallore") : "");
        this.itemDrop = ore;
        this.mapColor.set(ore.color);
        wallOre = true;
    }
    @Override
    public void createIcons(MultiPacker packer){
        for(int i = 0; i < variants; i++){
            //use name (e.g. "ore-copper1"), fallback to "copper1" as per the old naming system
            PixmapRegion shadow = Core.atlas.has(name + (i + 1)) ?
                    packer.get(name + (i + 1)) :
                    packer.get(itemDrop.name + (i + 1));

            Pixmap image = shadow.crop();

            int offset = image.width / tilesize - 1;
            int shadowColor = Color.rgba8888(0, 0, 0, 0.3f);

            for(int x = 0; x < image.width; x++){
                for(int y = offset; y < image.height; y++){
                    if(shadow.getA(x, y) == 0 && shadow.getA(x, y - offset) != 0){
                        image.setRaw(x, y, shadowColor);
                    }
                }
            }
            packer.add(PageType.environment, name + (i + 1), image);
            if(i == 0){
                packer.add(PageType.main, "block-" + name + "-full", image);
            }
            image.dispose();
        }
    }
    @Override
    public void init(){
        super.init();
        if(itemDrop != null){
            setup(itemDrop);
        }else{
            throw new IllegalArgumentException(name + " must have an item drop!");
        }
    }
    @Override
    public String getDisplayName(Tile tile){
        return itemDrop.localizedName;
    }
}