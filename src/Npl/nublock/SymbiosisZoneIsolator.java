package Npl.nublock;

import arc.graphics.Color;

/**
 * 共生区隔离器（纯标记方块，红色）。
 * 范围内单位与建筑免疫共生区的一切效果，由 {@link Npl.newSth.RadiationSystem} 每周期扫描生效。
 *
 * 需要贴图：assets/sprites/blocks/effect/symbiosisShield.png
 */
public class SymbiosisZoneIsolator extends ZoneIsolator {

    public SymbiosisZoneIsolator(String name){
        super(name);
        // 共生区基色：红
        rangeColor = Color.valueOf("FF406D");
    }
}
