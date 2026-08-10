package Npl;

import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import Npl.content.NuItems;
import arc.Core;   // 👈 添加这一行
import static mindustry.Vars.ui;

public class NuUI {
    public static void init() {
        Table panel = new Table();
        panel.setBackground(Tex.buttonEdge4);
        panel.defaults().pad(4);

        // 添加图标
        if (NuItems.bigIron != null) {
            panel.image(NuItems.bigIron.uiIcon).size(32);
        } else {
            // 备用：使用通用占位图标
            panel.image(Core.atlas.find("clear")).size(32);  // 或者用 Icon.settings.getRegion()
        }
        // 添加文本
        panel.label(() -> "生铁: " + (NuItems.bigIron != null ? "已加载" : "未加载"));

        // 挂载到 HUD（其余代码不变）
        Table overlay = ui.hudGroup.find("overlaymarker");
        Table bottom = overlay != null ? overlay.find("bottom") : null;
        if (bottom != null) {
            bottom.add(panel).right().bottom().pad(10);
        } else {
            Table mainTable = new Table();
            mainTable.setFillParent(true);
            mainTable.bottom().right();
            mainTable.add(panel).pad(10);
            ui.hudGroup.addChild(mainTable);
        }
    }
}