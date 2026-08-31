package Npl.nublock;

import arc.graphics.Color;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.world.Block;

/**
 * 区域隔离器基类（纯标记方块）。
 *
 * 自身不含任何逻辑，由 {@link Npl.newSth.RadiationSystem} 每周期扫描场上所有该方块，
 * 处于 {@link #range} 范围内的单位与建筑免疫对应区域的一切效果：
 * 不累积、暂停中毒持续伤害、治疗建筑不受压制。隔离器自身恒在自身范围内，因此天然免疫。
 *
 * 所有区域通用（辐射区 / 感染区 / 共生区），子类只需决定 {@link #range} 与 {@link #rangeColor}。
 */
public class ZoneIsolator extends Block {

    /** 隔离半径（世界单位，1 格 = 8） */
    public float range = 10 * 8;

    /** 绘制隔离范围提示时使用的颜色 */
    public Color rangeColor = Color.valueOf("7CFF6D");

    public ZoneIsolator(String name){
        super(name);
        size = 2;
        solid = true;
        // 纯标记方块：无 consume、无 update 逻辑，一切由 RadiationSystem 处理
        update = false;
    }

    /** 放置预览时显示隔离范围 */
    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * 8, y * 8, range, rangeColor);
    }

    public class ZoneIsolatorBuild extends Building {
        /** 选中时显示隔离范围 */
        @Override
        public void drawConfigure(){
            super.drawConfigure();
            Drawf.dashCircle(x, y, range, rangeColor);
        }
    }
}
