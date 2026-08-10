
package Npl.newSth;

import arc.graphics.Color;
import mindustry.type.Item;
import static Npl.content.ModStats.Reversible;   // 👈 静态导入
import static Npl.content.ModStats.Magentic;
import static Npl.content.ModStats.Stability;

    public class NewItemsType extends Item {
        public static float reversible = 0f;
        public static float magentic = 0f;
        public static float stability = 0f;
        public NewItemsType(String name, Color color) {
            super(name, color);
        }

        @Override
        public void setStats() {
            super.setStats();
            if (reversible > 0.01f) {
                // ✅ 使用自定义 Stat，显示为百分比（如 80%）
                stats.addPercent(Reversible, reversible);
            }
            if (magentic > 0.01f) {
                stats.addPercent(Magentic,magentic);
            }
            if (stability > 0.01f) {
                stats.addPercent(Stability,stability);
            }
        }
    }