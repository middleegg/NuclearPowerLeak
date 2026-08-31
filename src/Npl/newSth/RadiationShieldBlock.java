package Npl.newSth;

import Npl.nublock.ZoneIsolator;

/**
 * 辐射屏蔽器（纯标记方块）：继承自 {@link ZoneIsolator}，自身没有任何逻辑，
 * RadiationSystem 每周期扫描场上所有该方块，
 * 处于 {@link #range} 范围内的单位与建筑免疫辐射区的一切效果：
 * 不累积辐射、暂停中毒持续伤害、治疗建筑不受压制。
 * 屏蔽器自身恒在自身范围内，因此天然免疫。
 *
 * 需要贴图：assets/sprites/blocks/effect/radiationShield.png
 */
public class RadiationShieldBlock extends ZoneIsolator {

    public RadiationShieldBlock(String name){
        super(name);
        // 隔离范围提示色 = 辐射区圆环基色
        rangeColor = RadiationSystem.ringColor;
    }
}
