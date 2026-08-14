
package Npl.newSth;

import arc.graphics.Color;
import mindustry.type.Item;
import static Npl.content.ModStats.Reversible;
import static Npl.content.ModStats.Magentic;
import static Npl.content.ModStats.Stability;

public class NewItemsType extends Item {
    /** 每个实例独立的值——去掉 static，这样每个物品互不影响 */
    public float reversible = 0f;
    public float magentic = 0f;
    public float stability = 0f;

    public NewItemsType(String name, Color color) {
        super(name, color);
    }

    @Override
    public void setStats() {
        super.setStats();
        // 三个属性无论大小都显示（包括 0%）
        stats.addPercent(Reversible, reversible);
        stats.addPercent(Magentic, magentic);
        stats.addPercent(Stability, stability);
    }
}