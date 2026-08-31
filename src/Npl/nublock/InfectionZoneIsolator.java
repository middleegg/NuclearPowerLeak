package Npl.nublock;

import arc.graphics.Color;

/**
 * 感染区隔离器（纯标记方块，紫色）。
 * 范围内单位与建筑免疫感染区的一切效果，由 {@link Npl.newSth.RadiationSystem} 每周期扫描生效。
 *
 * 需要贴图：assets/sprites/blocks/effect/infectionShield.png
 */
public class InfectionZoneIsolator extends ZoneIsolator {

    public InfectionZoneIsolator(String name){
        super(name);
        // 感染区基色：紫
        rangeColor = Color.valueOf("C662FF");
    }
}
