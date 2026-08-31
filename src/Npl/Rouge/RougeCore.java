package Npl.Rouge;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.Element;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.core.UI;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.meta.*;

import Npl.Rouge.Artifact.Rarity;
import Npl.content.NuItems;

import static mindustry.Vars.*;

/**
 * RougeCore - Rouge模式专用核心
 * ==============================================================
 * 红黑危险主题
 * 功能：
 *   1. 基础资源上限 + 外场演绎科技树叠加
 *   2. 点击弹出功能窗口（存储状态、物资选择、局内商店、藏品展示）
 */
public class RougeCore extends CoreBlock {

    /** 基础资源上限（不含科技树加成） */
    public int baseCapacity = 2000;

    /** 红黑主题颜色 - 更亮的红色 */
    public static final Color DANGER_RED = Color.valueOf("FF4444");
    public static final Color DARK_RED = Color.valueOf("CC0000");
    public static final Color ALERT_ORANGE = Color.valueOf("FF8800");
    public static final Color DARK_BG = Color.valueOf("1A0A0A");
    public static final Color PANEL_BG = Color.valueOf("3A1818");
    public static final Color BRIGHT_RED = Color.valueOf("FF6666");

    public RougeCore(String name){
        super(name);
        this.itemCapacity = baseCapacity;
        this.size = 3;
        this.health = 5000;
        this.alwaysUnlocked = true;
        this.solid = true;
        this.update = true;
        this.configurable = true;
    }

    @Override
    public void init(){
        this.itemCapacity = calculateCapacity();
        super.init();
    }

    /** 计算实际资源上限 = 基础 + 科技树加成 */
    public static int calculateCapacity(){
        return 1000 + RougeTechTree.getCoreStorageLimit();
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        drawPlaceText(Core.bundle.format("rouge.capacity", calculateCapacity()), x, y, valid);
    }

    @Override
    public void setStats(){
        super.setStats();
        // super.setStats() 已自动添加 Stat.itemCapacity（基于 this.itemCapacity），
        // 这里不再重复添加，避免 stats 面板出现两个容量条目。
    }

    @Override
    public void setBars(){
        super.setBars();
        // 复用原版 capacity bar 的逻辑（基于 storageCapacity 团队总容量），
        // 只改颜色为红黑主题，保持与其他核心一致。
        addBar("capacity", (RougeCoreBuild e) -> new Bar(
            () -> Core.bundle.format("rouge.bar.capacity",
                UI.formatAmount(e.items.total()), UI.formatAmount(e.storageCapacity)),
            () -> DANGER_RED,
            () -> e.storageCapacity > 0 ? (float)e.items.total() / e.storageCapacity : 0f
        ));
    }

    public class RougeCoreBuild extends CoreBuild {

        @Override
        public void updateTile(){
            // 科技树升级后 itemCapacity 可能变化：若 block.itemCapacity 与 calculateCapacity()
            // 不一致，更新之并触发 onProximityUpdate 重算 storageCapacity（团队所有核心同步）。
            // 注意：不能只改 itemCapacity 而不触发 proximity 更新，否则容量条会显示旧值。
            int newCap = calculateCapacity();
            if(block.itemCapacity != newCap){
                block.itemCapacity = newCap;
                onProximityUpdate();
            }
            super.updateTile();
        }

        public int getCapacity(){
            return calculateCapacity();
        }

        @Override
        public void tapped(){
            showCoreDialog();
        }

        @Override
        public void buildConfiguration(Table table){
            table.button(Icon.settings, () -> {
                showCoreDialog();
            }).size(40).tooltip("打开功能菜单");
        }

        /** 当前激活的标签页 */
        private int activeTab = 0;

        private static final String[] TAB_NAMES = {"存储状态", "局内商店", "藏品图鉴"};

        /** 显示核心功能对话框 - 红黑危险主题 */
        private void showCoreDialog(){
            BaseDialog dialog = new BaseDialog("");
            float dialogWidth = Math.min(Core.graphics.getWidth() * 0.7f, 1200f);
            float dialogHeight = Core.graphics.getHeight() * 0.75f;
            dialog.setSize(dialogWidth, dialogHeight);
            dialog.setPosition(
                (Core.graphics.getWidth() - dialogWidth) / 2f,
                (Core.graphics.getHeight() - dialogHeight) / 2f
            );

            dialog.cont.table(main -> {
                main.setBackground(Styles.black6);

                // ===== 顶部标题栏 - 红黑渐变风格 =====
                main.table(title -> {
                    title.setBackground(Styles.black5);
                    title.image().color(DANGER_RED).width(4).height(36).padRight(8);
                    title.add("[#FF4444]ROUGE[] CORE").size(20).color(DANGER_RED).pad(8).left();
                    title.add().growX();
                    title.add("[gray]外场演绎指挥中枢[]").size(11).color(Color.gray).padRight(10);
                    title.button(Icon.cancel, () -> dialog.hide()).size(40).pad(6);
                }).fillX().height(45).row();

                // ===== 状态条（容量 / 储晶，始终可见） =====
                main.table(status -> {
                    status.setBackground(Styles.black3);
                    status.left().defaults().pad(8);
                    float ratio = getCapacity() > 0 ? (float)items.total() / getCapacity() : 0f;
                    status.add("[lightgray]容量[]").size(11);
                    status.add(items.total() + " / " + getCapacity())
                        .color(ratio > 0.9f ? DANGER_RED : ratio > 0.7f ? ALERT_ORANGE : Color.white).size(12).padLeft(4);
                    status.image().color(DANGER_RED).width(2).height(16).padLeft(12).padRight(12);
                    status.image(NuItems.temporalStorageCrystal.uiIcon).size(18).padRight(5);
                    status.add(String.valueOf(TemporalStorageCrystal.getAmount())).color(ALERT_ORANGE).size(12);
                    status.add().growX();
                    status.add("[gray]外场演绎指挥中枢[]").size(10).padRight(8);
                }).fillX().row();

                // ===== 主体内容 - 标签页 =====
                main.table(content -> {
                    // 左侧标签按钮
                    content.table(tabBar -> {
                        tabBar.name = "tabBar";
                    }).width(130).fillY().top();

                    // 右侧内容区域
                    content.table(right -> {
                        right.name = "tabContent";
                        right.setBackground(Styles.black6);
                    }).grow().padLeft(5);
                }).grow().padTop(5);
            }).grow();

            dialog.addCloseButton();
            dialog.show();

            // 显示当前标签页
            switchTab(dialog, activeTab);
        }

        /** 构建左侧标签栏，高亮当前页 */
        private void buildTabBar(Table tabBar, BaseDialog dialog){
            tabBar.clear();
            tabBar.top();
            tabBar.defaults().size(120, 42).pad(3);
            for(int i = 0; i < TAB_NAMES.length; i++){
                final int idx = i;
                boolean active = activeTab == idx;
                TextButton b = tabBar.button(TAB_NAMES[i], active ? Styles.defaultt : Styles.flatt,
                    () -> switchTab(dialog, idx)).get();
                b.getLabel().setColor(active ? DANGER_RED : Color.lightGray);
                tabBar.row();
            }
        }

        /** 切换标签页 */
        private void switchTab(BaseDialog dialog, int tab){
            activeTab = tab;

            Table tabBar = (Table) dialog.cont.find("tabBar");
            if(tabBar != null) buildTabBar(tabBar, dialog);

            Table content = (Table) dialog.cont.find("tabContent");
            if(content == null) return;
            content.clear();

            switch(tab){
                case 0: buildStorageTab(content); break;
                case 1: buildShopTab(content, dialog); break;
                case 2: buildCollectionTab(content); break;
            }
        }

        /** 存储状态标签页 - HunfuBlock式角标显示 */
        private void buildStorageTab(Table content){
            content.defaults().pad(8);

            // 标题
            content.table(Styles.black5, header -> {
                header.image().color(DANGER_RED).width(3).height(28).padRight(8);
                header.add("[#FF4444]存储状态[]").size(18).color(DANGER_RED).left();
                header.add().growX();
                header.add("[gray]基础 1000 + 科技 " + (getCapacity() - 1000) + "[]")
                    .color(Color.gray).size(10).padRight(12);
            }).fillX().row();

            // 容量信息面板（带进度条）
            content.table(Tex.whiteui, panel -> {
                panel.setColor(PANEL_BG);
                panel.defaults().pad(6).left();

                float ratio = getCapacity() > 0 ? Mathf.clamp((float)items.total() / getCapacity()) : 0f;
                Color capColor = ratio > 0.9f ? DANGER_RED : ratio > 0.7f ? ALERT_ORANGE : Pal.accent;

                panel.add("当前容量").color(Color.lightGray).size(12).left().row();
                panel.add(new Bar(
                    () -> "",
                    () -> ratio > 0.9f ? DANGER_RED : ratio > 0.7f ? ALERT_ORANGE : Pal.accent,
                    () -> ratio
                )).width(340).height(18).padTop(2).row();
                panel.add(items.total() + " / " + getCapacity()).color(capColor).size(12).left();
            }).fillX().pad(10).row();

            // 资源网格 - 角标显示数量
            content.pane(Styles.noBarPane, itemList -> {
                itemList.table(grid -> {
                    int col = 0;
                    int maxCols = 5;
                    for(Item item : Vars.content.items()){
                        final int count = items.get(item);
                        if(count <= 0) continue;

                        String badge = String.valueOf(count);
                        Item finalItem = item;
                        grid.stack(
                            new Image(finalItem.uiIcon).setScaling(Scaling.fit),
                            new Table(badgeTable -> {
                                badgeTable.right().bottom();
                                badgeTable.add(badge).style(Styles.outlineLabel)
                                    .fontScale(0.7f)
                                    .color(Color.white)
                                    .padRight(2f).padBottom(2f);
                            })
                        ).size(52).pad(8).tooltip(finalItem.localizedName + " x" + count);

                        col++;
                        if(col >= maxCols){
                            col = 0;
                            grid.row();
                        }
                    }
                    if(items.total() == 0){
                        grid.add("[gray]核心内暂无资源[]").color(Color.gray).pad(20).row();
                    }
                }).growX().pad(10);
            }).grow();
        }

        /** 局内商店标签页 - 水平网格布局，3列，卡片加大50% */
        private void buildShopTab(Table content, BaseDialog parentDialog){
            content.defaults().pad(12);

            content.table(Styles.black5, header -> {
                header.image().color(DANGER_RED).width(4).height(32).padRight(10);
                header.add("[#FF4444]局内商店[]").size(20).color(DANGER_RED).left();
                header.add().growX();
                header.add("[gray]消耗时序储晶兑换[]").color(Color.gray).size(10).padRight(12);
            }).fillX().row();

            content.table(Styles.black3, currency -> {
                currency.left().defaults().pad(6);
                currency.image(NuItems.temporalStorageCrystal.uiIcon).size(22).padLeft(6);
                currency.add("时序储晶").color(Color.lightGray).size(13).padLeft(6);
                currency.add(String.valueOf(TemporalStorageCrystal.getAmount())).color(ALERT_ORANGE).size(15).padLeft(4);
                currency.add().growX();
            }).fillX().pad(10).row();

            content.pane(Styles.noBarPane, scroll -> {
                scroll.table(shop -> {
                    shop.defaults().pad(8);

                    // 第一行：基础单位包 | 高级单位包 | 飞行单位包
                    // 商品1: 基础单位包
                    shop.table(Tex.whiteui, item -> {
                        item.setColor(PANEL_BG);
                        item.top();
                        item.defaults().pad(5).left();
                        item.add("[white]基础单位包[]").color(Pal.accent).size(16).padTop(4).row();
                        item.image().color(DANGER_RED).height(2).growX().padTop(4).padBottom(6).row();
                        item.add("[gray]随机地面基础单位[]").color(Color.gray).size(12).row();
                        item.add("[darkgray]dagger / crawler / nova[]").color(Color.darkGray).size(11).padTop(2).row();
                        item.add().growY().row();
                        item.table(p -> {
                            p.image(NuItems.temporalStorageCrystal.uiIcon).size(16).padRight(5);
                            p.add("50").color(Color.yellow).size(14);
                        }).left().padBottom(6).row();
                        item.table(buttons -> {
                            buttons.defaults().pad(3);
                            buttons.button("[green]兑换[]", Styles.defaultt, () -> {
                                if(TemporalStorageCrystal.spend(50)){
                                    Seq<UnitType> types = Vars.content.units().select(u -> !u.flying && u.playerControllable);
                                    if(!types.isEmpty()){
                                        UnitType t = types.random();
                                        Unit u = t.create(team);
                                        u.set(x + Mathf.random(-20, 20), y + Mathf.random(-20, 20));
                                        u.add();
                                        ui.showInfoToast("[green]获得: " + t.localizedName + "[]", 2f);
                                    }
                                    parentDialog.hide();
                                }else{
                                    ui.showInfoToast("[red]时序储晶不足！[]", 2f);
                                }
                            }).size(80, 34);
                            buttons.button("[green]x5[]", Styles.defaultt, () -> {
                                if(TemporalStorageCrystal.spend(250)){
                                    Seq<UnitType> types = Vars.content.units().select(u -> !u.flying && u.playerControllable);
                                    if(!types.isEmpty()){
                                        for(int i = 0; i < 5; i++){
                                            UnitType t = types.random();
                                            Unit u = t.create(team);
                                            u.set(x + Mathf.random(-30, 30), y + Mathf.random(-30, 30));
                                            u.add();
                                        }
                                        ui.showInfoToast("[green]获得5个基础单位！[]", 2f);
                                    }
                                    parentDialog.hide();
                                }else{
                                    ui.showInfoToast("[red]时序储晶不足！需要250[]", 2f);
                                }
                            }).size(50, 34);
                        }).fillX().padBottom(4);
                    }).width(280).height(280).pad(8);

                    // 商品2: 高级单位包
                    shop.table(Tex.whiteui, item -> {
                        item.setColor(PANEL_BG);
                        item.top();
                        item.defaults().pad(5).left();
                        item.add("[white]高级单位包[]").color(Pal.accent).size(16).padTop(4).row();
                        item.image().color(DANGER_RED).height(2).growX().padTop(4).padBottom(6).row();
                        item.add("[gray]随机高级地面单位[]").color(Color.gray).size(12).row();
                        item.add("[darkgray]reign / mercy / corvus[]").color(Color.darkGray).size(11).padTop(2).row();
                        item.add().growY().row();
                        item.table(p -> {
                            p.image(NuItems.temporalStorageCrystal.uiIcon).size(16).padRight(5);
                            p.add("120").color(Color.yellow).size(14);
                        }).left().padBottom(6).row();
                        item.table(buttons -> {
                            buttons.defaults().pad(3);
                            buttons.button("[green]兑换[]", Styles.defaultt, () -> {
                                if(TemporalStorageCrystal.spend(120)){
                                    Seq<UnitType> types = Vars.content.units().select(u -> !u.flying && u.playerControllable && u.health > 500);
                                    if(!types.isEmpty()){
                                        UnitType t = types.random();
                                        Unit u = t.create(team);
                                        u.set(x + Mathf.random(-20, 20), y + Mathf.random(-20, 20));
                                        u.add();
                                        ui.showInfoToast("[green]获得高级单位: " + t.localizedName + "[]", 2f);
                                    }
                                    parentDialog.hide();
                                }else{
                                    ui.showInfoToast("[red]时序储晶不足！[]", 2f);
                                }
                            }).size(80, 34);
                            buttons.button("[green]x5[]", Styles.defaultt, () -> {
                                if(TemporalStorageCrystal.spend(600)){
                                    Seq<UnitType> types = Vars.content.units().select(u -> !u.flying && u.playerControllable && u.health > 500);
                                    if(!types.isEmpty()){
                                        for(int i = 0; i < 5; i++){
                                            UnitType t = types.random();
                                            Unit u = t.create(team);
                                            u.set(x + Mathf.random(-30, 30), y + Mathf.random(-30, 30));
                                            u.add();
                                        }
                                        ui.showInfoToast("[green]获得5个高级单位！[]", 2f);
                                    }
                                    parentDialog.hide();
                                }else{
                                    ui.showInfoToast("[red]时序储晶不足！需要600[]", 2f);
                                }
                            }).size(50, 34);
                        }).fillX().padBottom(4);
                    }).width(280).height(280).pad(8);

                    // 商品3: 飞行单位包
                    shop.table(Tex.whiteui, item -> {
                        item.setColor(PANEL_BG);
                        item.top();
                        item.defaults().pad(5).left();
                        item.add("[white]飞行单位包[]").color(Pal.accent).size(16).padTop(4).row();
                        item.image().color(DANGER_RED).height(2).growX().padTop(4).padBottom(6).row();
                        item.add("[gray]随机飞行单位[]").color(Color.gray).size(12).row();
                        item.add("[darkgray]flare / zenith / eclipse[]").color(Color.darkGray).size(11).padTop(2).row();
                        item.add().growY().row();
                        item.table(p -> {
                            p.image(NuItems.temporalStorageCrystal.uiIcon).size(16).padRight(5);
                            p.add("80").color(Color.yellow).size(14);
                        }).left().padBottom(6).row();
                        item.table(buttons -> {
                            buttons.defaults().pad(3);
                            buttons.button("[green]兑换[]", Styles.defaultt, () -> {
                                if(TemporalStorageCrystal.spend(80)){
                                    Seq<UnitType> types = Vars.content.units().select(u -> u.flying && u.playerControllable);
                                    if(!types.isEmpty()){
                                        UnitType t = types.random();
                                        Unit u = t.create(team);
                                        u.set(x + Mathf.random(-20, 20), y + Mathf.random(-20, 20));
                                        u.add();
                                        ui.showInfoToast("[green]获得飞行单位: " + t.localizedName + "[]", 2f);
                                    }
                                    parentDialog.hide();
                                }else{
                                    ui.showInfoToast("[red]时序储晶不足！[]", 2f);
                                }
                            }).size(80, 34);
                            buttons.button("[green]x5[]", Styles.defaultt, () -> {
                                if(TemporalStorageCrystal.spend(400)){
                                    Seq<UnitType> types = Vars.content.units().select(u -> u.flying && u.playerControllable);
                                    if(!types.isEmpty()){
                                        for(int i = 0; i < 5; i++){
                                            UnitType t = types.random();
                                            Unit u = t.create(team);
                                            u.set(x + Mathf.random(-30, 30), y + Mathf.random(-30, 30));
                                            u.add();
                                        }
                                        ui.showInfoToast("[green]获得5个飞行单位！[]", 2f);
                                    }
                                    parentDialog.hide();
                                }else{
                                    ui.showInfoToast("[red]时序储晶不足！需要400[]", 2f);
                                }
                            }).size(50, 34);
                        }).fillX().padBottom(4);
                    }).width(280).height(280).pad(8);

                    shop.row();

                    // 第二行：基础物资包 | 高级物资包 | 临时藏品
                    // 商品4: 基础物资包
                    shop.table(Tex.whiteui, item -> {
                        item.setColor(PANEL_BG);
                        item.top();
                        item.defaults().pad(5).left();
                        item.add("[white]基础物资包[]").color(Pal.accent).size(16).padTop(4).row();
                        item.image().color(DANGER_RED).height(2).growX().padTop(4).padBottom(6).row();
                        item.add("[gray]铜 / 铅 / 煤 / 硅[]").color(Color.gray).size(12).row();
                        item.add("[darkgray]大量基础物资[]").color(Color.darkGray).size(11).padTop(2).row();
                        item.add().growY().row();
                        item.table(p -> {
                            p.image(NuItems.temporalStorageCrystal.uiIcon).size(16).padRight(5);
                            p.add("30").color(Color.yellow).size(14);
                        }).left().padBottom(6).row();
                        item.table(buttons -> {
                            buttons.defaults().pad(3);
                            buttons.button("[green]兑换[]", Styles.defaultt, () -> {
                                if(TemporalStorageCrystal.spend(30)){
                                    player.team().items().add(Items.copper, Mathf.random(20, 50));
                                    player.team().items().add(Items.lead, Mathf.random(15, 40));
                                    player.team().items().add(Items.coal, Mathf.random(10, 30));
                                    player.team().items().add(Items.silicon, Mathf.random(10, 25));
                                    ui.showInfoToast("[green]兑换成功！获得基础物资[]", 2f);
                                    parentDialog.hide();
                                }else{
                                    ui.showInfoToast("[red]时序储晶不足！[]", 2f);
                                }
                            }).size(80, 34);
                            buttons.button("[green]x5[]", Styles.defaultt, () -> {
                                if(TemporalStorageCrystal.spend(150)){
                                    player.team().items().add(Items.copper, Mathf.random(100, 250));
                                    player.team().items().add(Items.lead, Mathf.random(75, 200));
                                    player.team().items().add(Items.coal, Mathf.random(50, 150));
                                    player.team().items().add(Items.silicon, Mathf.random(50, 125));
                                    ui.showInfoToast("[green]批量兑换成功！[]", 2f);
                                    parentDialog.hide();
                                }else{
                                    ui.showInfoToast("[red]时序储晶不足！需要150[]", 2f);
                                }
                            }).size(50, 34);
                        }).fillX().padBottom(4);
                    }).width(280).height(280).pad(8);

                    // 商品5: 高级物资包
                    shop.table(Tex.whiteui, item -> {
                        item.setColor(PANEL_BG);
                        item.top();
                        item.defaults().pad(5).left();
                        item.add("[white]高级物资包[]").color(Pal.accent).size(16).padTop(4).row();
                        item.image().color(DANGER_RED).height(2).growX().padTop(4).padBottom(6).row();
                        item.add("[gray]钛 / 钍 / 塑钢[]").color(Color.gray).size(12).row();
                        item.add("[darkgray]稀有高级物资[]").color(Color.darkGray).size(11).padTop(2).row();
                        item.add().growY().row();
                        item.table(p -> {
                            p.image(NuItems.temporalStorageCrystal.uiIcon).size(16).padRight(5);
                            p.add("100").color(Color.yellow).size(14);
                        }).left().padBottom(6).row();
                        item.table(buttons -> {
                            buttons.defaults().pad(3);
                            buttons.button("[green]兑换[]", Styles.defaultt, () -> {
                                if(TemporalStorageCrystal.spend(100)){
                                    player.team().items().add(Items.titanium, Mathf.random(10, 25));
                                    player.team().items().add(Items.thorium, Mathf.random(5, 15));
                                    player.team().items().add(Items.plastanium, Mathf.random(5, 10));
                                    ui.showInfoToast("[green]兑换成功！获得高级物资[]", 2f);
                                    parentDialog.hide();
                                }else{
                                    ui.showInfoToast("[red]时序储晶不足！[]", 2f);
                                }
                            }).size(80, 34);
                            buttons.button("[green]x5[]", Styles.defaultt, () -> {
                                if(TemporalStorageCrystal.spend(500)){
                                    player.team().items().add(Items.titanium, Mathf.random(50, 125));
                                    player.team().items().add(Items.thorium, Mathf.random(25, 75));
                                    player.team().items().add(Items.plastanium, Mathf.random(25, 50));
                                    ui.showInfoToast("[green]批量兑换成功！[]", 2f);
                                    parentDialog.hide();
                                }else{
                                    ui.showInfoToast("[red]时序储晶不足！需要500[]", 2f);
                                }
                            }).size(50, 34);
                        }).fillX().padBottom(4);
                    }).width(280).height(280).pad(8);

                    // 商品6: 临时藏品
                    shop.table(Tex.whiteui, item -> {
                        item.setColor(PANEL_BG);
                        item.top();
                        item.defaults().pad(5).left();
                        item.add("[white]临时藏品[]").color(Pal.accent).size(16).padTop(4).row();
                        item.image().color(DANGER_RED).height(2).growX().padTop(4).padBottom(6).row();
                        item.add("[gray]本局有效的战术藏品[]").color(Color.gray).size(12).row();
                        item.add("[darkgray]提供临时增益效果[]").color(Color.darkGray).size(11).padTop(2).row();
                        item.add().growY().row();
                        item.table(p -> {
                            p.image(NuItems.temporalStorageCrystal.uiIcon).size(16).padRight(5);
                            p.add("100").color(Color.yellow).size(14);
                        }).left().padBottom(6).row();
                        item.table(buttons -> {
                            buttons.defaults().pad(3);
                            buttons.button("[green]兑换[]", Styles.defaultt, () -> {
                                if(TemporalStorageCrystal.spend(100)){
                                    Artifact randArtifact = ArtifactDatabase.getRandom(new Rand());
                                    if(randArtifact != null){
                                        ArtifactDialog.showAcquired(randArtifact, () -> {
                                            ui.showInfoToast("[green]获得临时藏品: " + randArtifact.name + "[]", 2f);
                                        });
                                    }
                                    parentDialog.hide();
                                }else{
                                    ui.showInfoToast("[red]时序储晶不足！[]", 2f);
                                }
                            }).size(80, 34);
                        }).fillX().padBottom(4);
                    }).width(280).height(280).pad(8);
                }).growX().pad(12);
            }).grow();
        }

        /** 藏品图鉴标签页 */
        private void buildCollectionTab(Table content){
            content.defaults().pad(8);

            content.table(Styles.black5, header -> {
                header.image().color(DANGER_RED).width(4).height(28).padRight(8);
                header.add("[#FF4444]藏品图鉴[]").size(18).color(DANGER_RED).left();
                header.add().growX();
                header.add("[gray]已收集 " + ArtifactManager.inventory.size + " 件[]")
                    .color(Color.gray).size(10).padRight(12);
            }).fillX().row();

            content.pane(Styles.noBarPane, scroll -> {
                scroll.table(list -> {
                    if(ArtifactManager.inventory.isEmpty()){
                        list.add("[gray]暂无藏品，进入模拟挑战来获取吧[]").color(Color.gray).pad(30).row();
                    }else{
                        ObjectMap<Rarity, Seq<Artifact>> grouped = ArtifactManager.getGroupedByRarity();
                        Rarity[] order = {Rarity.SSR, Rarity.SR, Rarity.R, Rarity.N};
                        for(Rarity rarity : order){
                            Seq<Artifact> group = grouped.get(rarity);
                            if(group == null || group.isEmpty()) continue;

                            list.table(Styles.black3, sec -> {
                                sec.left().defaults().pad(5);
                                sec.image().color(rarity.color).width(3).height(18).padRight(6);
                                sec.add("[#" + rarity.color.toString() + "]" + rarity.displayName + "[]")
                                    .color(rarity.color).size(13);
                                sec.add("[gray](" + group.size + ")[]").color(Color.gray).size(10).padLeft(4);
                                sec.add().growX();
                            }).fillX().padTop(8).row();

                            for(Artifact a : group){
                                list.table(Tex.whiteui, item -> {
                                    item.setColor(PANEL_BG);
                                    item.left().defaults().pad(6);
                                    // 稀有度色条
                                    item.image().color(a.getColor()).width(3).height(40).padRight(8);
                                    // 图标
                                    TextureRegion icon = ArtifactDialog.getArtifactIconTexture(a);
                                    item.image(icon != null ? icon : Items.copper.uiIcon).size(32).padRight(8);
                                    // 信息
                                    item.table(info -> {
                                        info.left();
                                        info.add("[#" + a.getColor().toString() + "]" + a.name + "[]")
                                            .color(a.getColor()).size(12).left().row();
                                        String meta = a.level > 1 ? "[yellow]Lv." + a.level + "[]  " : "";
                                        info.add(meta + "[accent]" + a.getEffectsText() + "[]")
                                            .color(Color.lightGray).size(11).left().row();
                                        info.add(a.description).color(Color.gray)
                                            .width(300).size(10).left().row();
                                    }).growX();
                                }).growX().pad(3).row();
                            }
                        }
                    }
                }).growX().pad(10);
            }).grow();
        }

        @Override
        public void draw(){
            super.draw();
        }

        @Override
        public void write(Writes write){
            super.write(write);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
        }
    }
}
