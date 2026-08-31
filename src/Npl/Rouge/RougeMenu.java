package Npl.Rouge;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.BaseDialog;

import Npl.Rouge.ArtifactDialog;

import static mindustry.Vars.*;

/**
 * RougeMenu —— 在主菜单新增「Rouge 模式」按钮
 * ==============================================================
 * 点击后弹出选择界面：外场演绎 / 进入模拟
 */
public class RougeMenu {

    private static boolean initialized = false;
    private static Drawable rougeModeIcon;

    public static void init(){
        if(initialized) return;
        initialized = true;

        // 加载按钮图标（mod资源需要加 mod 名前缀 nu-）
        TextureRegion region = Core.atlas.find("nu-RougeMode");
        if(region != null && region.found()){
            rougeModeIcon = new TextureRegionDrawable(region);
        }

        // 延迟，确保菜单完全 build 完
        Time.run(15f, RougeMenu::addRougeButton);
    }

    private static void addRougeButton(){
        if(ui.menufrag == null){
            Log.err("RougeMenu: ui.menufrag is null");
            return;
        }

        // 图标使用 RougeMode 贴图，如果没有则用原版 planet-serpulo
        Drawable icon;
        if(rougeModeIcon != null){
            icon = rougeModeIcon;
        }else{
            TextureRegion region = Core.atlas.find("planet-serpulo");
            if(region == null || region.found()) region = Core.atlas.find("white");
            icon = new TextureRegionDrawable(region);
        }

        // 加到 customButtons（图标在文字前面）
        ui.menufrag.addButton("Rouge 模式", icon, () -> {
            showRougeMainMenu();
        });

        // 清空旧菜单 WidgetGroup，再 build（避免新旧重叠）
        ui.menuGroup.clearChildren();
        ui.menufrag.build(ui.menuGroup);

        Log.info("RougeMenu: button added via addButton + rebuild");
    }

    /** 显示 Rouge 主菜单选择界面 */
    private static void showRougeMainMenu(){
        new BaseDialog(""){{
            cont.table(main -> {
                main.setBackground(Tex.whiteui);
                main.setColor(RougeCore.DARK_BG);

                // ===== 标题栏 =====
                main.table(title -> {
                    title.setBackground(Styles.black5);
                    title.image().color(RougeCore.DANGER_RED).width(4).height(34).padRight(10);
                    title.add("[#FF4444]ROUGE[] [white]模式[]").size(22).left().pad(8);
                    title.add().growX();
                    if(rougeModeIcon != null){
                        title.image(rougeModeIcon).size(32).padRight(12);
                    }
                }).fillX().pad(6).row();

                main.add("[gray]红黑外场演绎 · 消耗资源换取永久增益[]").color(Color.lightGray)
                    .size(12).left().padLeft(14).padTop(8).padBottom(8).row();

                // ===== 入口卡片 =====
                main.table(cards -> {
                    cards.defaults().pad(6);
                    menuEntry(cards, "外场演绎", "消耗资源获得永久增益", RougeCore.DANGER_RED,
                        () -> { hide(); RougeTechTree.show(); }).row();
                    menuEntry(cards, "进入模拟", "挑战树状分支模式", RougeCore.ALERT_ORANGE,
                        () -> { hide(); RougeCampaignScreen.show(); }).row();
                    menuEntry(cards, "联机大厅（开房）", "开一个房间，其他人用「加入游戏」进入", Pal.accent,
                        () -> { hide(); RougeLobby.hostLobby(); }).row();
                    menuEntry(cards, "藏品图鉴", "查看已收集的战术藏品", Pal.accent,
                        () -> { hide(); ArtifactDialog.showCollection(); }).row();
                }).growX().pad(12).row();

                // ===== 底部资源栏 =====
                main.table(footer -> {
                    footer.setBackground(Styles.black3);
                    footer.left();
                    footer.image().color(RougeCore.ALERT_ORANGE).width(3).height(18).padLeft(12).padRight(8);
                    footer.add("[lightgray]携带资源[]").size(13);
                    footer.add(String.valueOf(RougeSave.storedTotal())).color(RougeCore.ALERT_ORANGE)
                        .size(16).padLeft(6).padRight(12);
                }).fillX().pad(8).row();
            }).width(440).pad(10);

            addCloseButton();
        }}.show();
    }

    /** 构建单个入口卡片 */
    private static Cell menuEntry(Table parent, String title, String desc, Color accent, Runnable action){
        return parent.table(Tex.whiteui, card -> {
            card.setColor(RougeCore.PANEL_BG);
            card.left().defaults().pad(6);
            card.table(info -> {
                info.left();
                info.add("[white]" + title + "[]").color(accent).size(18).left().row();
                info.add("[gray]" + desc + "[]").color(Color.gray).size(11).left();
            }).growX().padLeft(10);
            card.button("[accent]进入[]", Styles.defaultt, action).size(110, 40).padRight(8);
        }).growX().height(68);
    }
}
