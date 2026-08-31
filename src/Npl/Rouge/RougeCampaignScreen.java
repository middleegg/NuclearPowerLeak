package Npl.Rouge;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import arc.scene.event.Touchable;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.mod.Mods;
import mindustry.mod.Mods.*;
import mindustry.type.*;
import mindustry.type.unit.*;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.modules.*;

import Npl.content.NuColor;

import static mindustry.Vars.*;

/**
 * RougeCampaignScreen —— Rouge 模式指挥界面
 * ==============================================================
 * 布局：
 *   - 全屏路线总览（从左到右展开，支持分页）
 *   - 点击节点弹出详情面板
 * 功能：
 *   - 分页切换（关卡太多时自动分页）
 *   - 小地图导航
 *   - 折叠/展开已完成分支
 *   - 选择分支后封锁其他分支
 */
public class RougeCampaignScreen {

    private static BaseDialog dialog;
    private static RougeTree.Node selectedNode = null;
    private static TreePanel treePanel;
    private static ScrollPane treeScroll;
    private static Minimap minimap;
    private static int currentPage = 0;
    private static int totalPages = 1;
    private static Label pageLabel;
    private static Table selectedInfoPanel;

    public static void show(){
        RougeSave.isInRougeMode = true;

        if(RougeNet.isClient()){
            // 联机客户端：树与进度都是房主同步过来的，绝不能本地重建
            // （generateTree 依赖本机文件系统遍历顺序，两端会生成完全不同的树）
            if(RougeTree.getRoot() == null) return;
            selectedNode = RougeNet.selectedId >= 0 ? RougeTree.findById(RougeNet.selectedId) : null;
            if(dialog != null){
                dialog.hide();
                dialog = null;
            }
            build();
            dialog.show();
            return;
        }

        // 刚从别人的联机房间退出来时，先把本地存档读回来再建树
        // （否则会用房主的 treeSeed + 本机地图列表建树，并把它写进自己的存档）
        RougeNet.checkSessionEnded();

        if(RougeSave.treeSeed == 0){
            RougeSave.treeSeed = System.nanoTime();
            RougeTree.generateTree(RougeSave.treeSeed, 10);
            RougeSave.save();
        }else{
            RougeTree.generateTree(RougeSave.treeSeed, 10);
            applySaveState();
        }
        if(dialog != null){
            dialog.hide();
            dialog = null;
        }
        selectedNode = null;
        build();
        dialog.show();
    }

    /** 树界面是否正在显示（联机状态机用） */
    public static boolean isShown(){
        return dialog != null && dialog.isShown();
    }

    /** 隐藏树界面（联机状态机用） */
    public static void hide(){
        if(dialog != null) dialog.hide();
    }

    private static void applySaveState(){
        for(int i = 0; i < RougeSave.completedNodeIds.size; i++){
            int id = RougeSave.completedNodeIds.get(i);
            RougeTree.Node node = RougeTree.findById(id);
            if(node != null){
                node.completed = true;
            }
        }
        for(var entry : RougeSave.branchChoices.entries()){
            RougeTree.Node node = RougeTree.findById(entry.key);
            if(node != null){
                int chosenBranch = entry.value;
                if(node.left != null && node.middle != null && node.right != null){
                    if(chosenBranch != 0) RougeTree.lockBranch(node.left);
                    if(chosenBranch != 1) RougeTree.lockBranch(node.middle);
                    if(chosenBranch != 2) RougeTree.lockBranch(node.right);
                }
            }
        }
    }

    private static void build(){
        dialog = new BaseDialog("模式指挥");

        TextureRegionDrawable blackBg = new TextureRegionDrawable(Core.atlas.find("white")){{
            tint.set(new Color(0.05f, 0.05f, 0.1f, 1f));
        }};
        dialog.setBackground(blackBg);

        dialog.cont.table(top -> {
            top.add("模式指挥").color(Pal.accent).fontScale(1.4f).left().padRight(30);
            top.add("选择关卡节点开始挑战 | 滚轮缩放 | 右键拖动").color(Color.lightGray).left();
            if(RougeNet.isClient()){
                String waiting = RougeNet.selectedId >= 0
                    ? "[orange]房主已选择节点，等待出发…[]"
                    : "[orange]联机大厅：等待房主选择…[]";
                top.add(waiting).color(Color.orange).left().padLeft(20);
            }
        }).growX().pad(15).row();

        dialog.cont.table(Styles.black3, res -> {
            res.add("携带资源: ").color(Color.lightGray);
            int total = RougeSave.storedTotal();
            if(total == 0){
                res.add("[gray](空)[]").color(Color.gray);
            }else{
                Table items = new Table();
                int i = 0;
                for(var e : RougeSave.storedItems.entries()){
                    int amount = e.value;
                    if(amount > 0){
                        items.image(e.key.uiIcon).size(20).padRight(2);
                        items.add(amount + "").color(Color.lightGray).padRight(8);
                        if(++i % 6 == 0) items.row();
                    }
                }
                res.add(items);
            }
        }).growX().pad(10).row();

        dialog.cont.table(main -> {
            // 左侧：路线总览（放大）
            main.table(treeContainer -> {
                treeContainer.background(Styles.black6);
                treeContainer.add("路线总览").color(Pal.accent).pad(10).row();
                treeContainer.row();

                treePanel = new TreePanel();
                treeScroll = new ScrollPane(treePanel, Styles.defaultPane);
                treeScroll.setScrollingDisabled(false, false);
                treeScroll.setFadeScrollBars(false);
                treeScroll.setOverscroll(false, false);
                treeContainer.add(treeScroll).size(Core.graphics.getWidth() * 0.6f, Core.graphics.getHeight() * 0.65f).pad(10);

                treeContainer.row();
                treeContainer.table(controls -> {
                    controls.add("[gray]点击节点查看详情[]").color(Color.gray).padRight(15);

                    controls.button("折叠全部", Styles.defaultt, () -> {
                        RougeTree.Node root = RougeTree.getRoot();
                        if(root != null) collapseAllCompleted(root);
                        treePanel.rebuild();
                        updatePagination();
                    }).size(90, 30).padRight(5);

                    controls.button("展开全部", Styles.defaultt, () -> {
                        RougeTree.Node root = RougeTree.getRoot();
                        if(root != null) expandAll(root);
                        treePanel.rebuild();
                        updatePagination();
                    }).size(90, 30).padRight(15);

                    controls.button("< 上一页", Styles.defaultt, () -> {
                        if(currentPage > 0){
                            currentPage--;
                            treePanel.rebuild();
                            updatePagination();
                        }
                    }).size(80, 30).padRight(5);

                    pageLabel = new Label("", Styles.defaultLabel);
                    controls.add(pageLabel).padRight(5);

                    controls.button("下一页 >", Styles.defaultt, () -> {
                        if(currentPage < totalPages - 1){
                            currentPage++;
                            treePanel.rebuild();
                            updatePagination();
                        }
                    }).size(80, 30);
                }).pad(10);
            }).grow().pad(10);

            // 右侧：当前增益 + 选中节点信息
            main.table(rightPanel -> {
                rightPanel.background(Styles.black6);

                // 当前增益面板
                rightPanel.table(Styles.black3, bonus -> {
                    bonus.add("[accent]当前增益[]").color(Pal.accent).pad(8).row();
                    bonus.table(content -> {
                        content.left();
                        float dmgMult = RougeTechTree.getUnitDamageMultiplier();
                        if(dmgMult > 1.0f){
                            int dmgPercent = Math.round((dmgMult - 1.0f) * 100);
                            content.add("[green]■[] 单位伤害: [yellow]+" + dmgPercent + "%[]").left().padLeft(10).padTop(4).row();
                        }else{
                            content.add("[gray]■[] 单位伤害: 无加成").left().padLeft(10).padTop(4).row();
                        }

                        float hpMult = RougeTechTree.getBuildingHealthMultiplier();
                        if(hpMult > 1.0f){
                            int hpPercent = Math.round((hpMult - 1.0f) * 100);
                            content.add("[green]■[] 建筑血量: [yellow]+" + hpPercent + "%[]").left().padLeft(10).padTop(4).row();
                        }else{
                            content.add("[gray]■[] 建筑血量: 无加成").left().padLeft(10).padTop(4).row();
                        }

                        int startRes = RougeTechTree.getStartingResources();
                        if(startRes > 0){
                            content.add("[green]■[] 起始资源: [yellow]+" + startRes + " 铜+铅[]").left().padLeft(10).padTop(4).row();
                        }else{
                            content.add("[gray]■[] 起始资源: 无加成").left().padLeft(10).padTop(4).row();
                        }

                        float evacRatio = RougeTechTree.getEvacuationRatio();
                        int evacPercent = Math.round(evacRatio * 100);
                        if(evacRatio > 0.5f){
                            content.add("[green]■[] 撤离效率: [yellow]" + evacPercent + "%[]").left().padLeft(10).padTop(4).row();
                        }else{
                            content.add("[gray]■[] 撤离效率: 50%（基础）").left().padLeft(10).padTop(4).row();
                        }

                        int coreStorage = RougeTechTree.getCoreStorageLimit();
                        if(coreStorage > 1000){
                            content.add("[green]■[] 核心存储: [yellow]" + coreStorage + "[]").left().padLeft(10).padTop(4).padBottom(4).row();
                        }else{
                            content.add("[gray]■[] 核心存储: 1000（基础）").left().padLeft(10).padTop(4).padBottom(4).row();
                        }
                    }).growX();
                }).growX().pad(10).row();

                // 选中节点信息面板
                rightPanel.table(info -> {
                    info.background(Styles.black6);
                    selectedInfoPanel = info;
                    updateSelectedInfoPanel();
                }).width(280).growY().pad(10).padTop(0);
            }).growY().pad(10).padLeft(0);
        }).grow().row();

        updatePagination();

        minimap = new Minimap();
        dialog.cont.add(minimap).bottom().right().pad(15);

        dialog.cont.table(bottom -> {
            bottom.button("重置进度", Styles.defaultt, () -> {
                if(RougeNet.isClient()){
                    ui.showInfoToast("[scarlet]联机时只有房主能重置进度[]", 3f);
                    return;
                }
                new BaseDialog("确认重置"){{
                    cont.add("确定要重置所有 Rouge 模式进度吗？").pad(20).row();
                    buttons.button("确定", () -> {
                        RougeSave.reset();
                        RougeSave.treeSeed = System.nanoTime();
                        RougeTree.generateTree(RougeSave.treeSeed, 10);
                        ArtifactManager.reset();
                        ArtifactManager.save();
                        RougeSave.save();
                        hide();
                        if(dialog != null){
                            dialog.hide();
                            dialog = null;
                        }
                        RougeCampaignScreen.show();
                    }).size(100, 40);
                    buttons.button("取消", this::hide).size(100, 40);
                }}.show();
            }).size(140, 40).padRight(20);

            bottom.button("藏品图鉴", Styles.defaultt, () -> {
                ArtifactDialog.showCollection();
            }).size(120, 40).padRight(20);

            bottom.button("返回", Styles.defaultt, () -> {
                RougeSave.isInRougeMode = false;
                dialog.hide();
            }).size(120, 40);
        }).pad(15);

        dialog.addCloseButton();
    }

    private static void updatePagination(){
        RougeTree.Node root = RougeTree.getRoot();
        if(root == null || pageLabel == null) return;

        int maxDepth = getMaxDepth(root, 0);
        // 使用 TreePanel 的动态 levelsPerPage
        int levelsPerPage = treePanel != null ? treePanel.levelsPerPage : 5;
        totalPages = Math.max(1, (maxDepth - 1) / levelsPerPage + 1);

        if(currentPage >= totalPages) currentPage = totalPages - 1;
        if(currentPage < 0) currentPage = 0;

        pageLabel.setText("第 " + (currentPage + 1) + " / " + totalPages + " 页");
    }

    /** 更新右侧选中节点信息面板 */
    private static void updateSelectedInfoPanel(){
        if(selectedInfoPanel == null) return;
        selectedInfoPanel.clear();

        if(selectedNode == null){
            selectedInfoPanel.add("[gray]点击左侧节点查看详情[]").color(Color.gray).pad(20);
            return;
        }

        // 绿色边框标题
        selectedInfoPanel.table(title -> {
            title.background(Styles.black3);
            title.add("[green]节点信息[]").color(Color.green).fontScale(1.2f).pad(10);
        }).growX().pad(10).row();

        // 地图图标和名称
        selectedInfoPanel.table(header -> {
            TextureRegion icon = RougeTree.getNodeIcon(selectedNode.mapName);
            header.image(icon).size(48).pad(10);
            header.table(name -> {
                name.add("[accent]" + selectedNode.mapName + "[]").color(Pal.accent).fontScale(1.1f).left().row();
                if(selectedNode.completed){
                    name.add("[green]已完成[]").color(Color.green).left().row();
                }else if(selectedNode.locked){
                    name.add("[red]已封锁[]").color(Color.red).left().row();
                }else{
                    name.add("[yellow]可挑战[]").color(Color.yellow).left().row();
                }
            }).growX();
        }).growX().pad(10).row();

        // 详细信息
        selectedInfoPanel.table(info -> {
            info.background(Styles.black3);
            info.left().top();

            float difficulty = getNodeDifficulty(selectedNode);
            String diffStars = getDifficultyStars(difficulty);
            info.add("难度: " + diffStars).color(getDifficultyColor(difficulty)).left().padLeft(10).padTop(5).row();
            info.add("波次: " + getWaveCount(selectedNode)).color(Color.lightGray).left().padLeft(10).row();
            info.add("地图大小: " + getMapSize(selectedNode)).color(Color.lightGray).left().padLeft(10).padBottom(5).row();
        }).growX().pad(10).row();

        // 描述
        selectedInfoPanel.table(desc -> {
            desc.background(Styles.black3);
            desc.add("描述").color(Pal.accent).left().pad(10).row();
            desc.add(getNodeDescription(selectedNode)).color(Color.gray).growX().padLeft(10).padRight(10).padBottom(10).row();
        }).growX().pad(10).row();

        // 点击打开详情按钮
        selectedInfoPanel.button("[green]查看详情[]", Styles.defaultt, () -> {
            showNodeDetailPopup(selectedNode);
        }).size(250, 45).pad(10).row();

        // 进入战斗按钮（联机客户端只读：出发由房主决定）
        boolean canEnter = !selectedNode.locked && isUnlocked(selectedNode) && !selectedNode.completed;
        if(RougeNet.isClient()){
            selectedInfoPanel.add(canEnter ? "[gray]等待房主出发…[]" : "[gray]该节点暂不可进入[]")
                .color(Color.gray).size(250, 45).pad(10);
            return;
        }

        TextButton enterBtn = new TextButton("进入战斗", Styles.defaultt);
        enterBtn.setDisabled(!canEnter);
        enterBtn.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y){
                if(!enterBtn.isDisabled()){
                    launchNode(selectedNode);
                }
            }
        });
        selectedInfoPanel.add(enterBtn).size(250, 45).pad(10);
    }

    private static int getMaxDepth(RougeTree.Node node, int depth){
        if(node == null) return depth;
        int maxChildDepth = depth;
        if(node.left != null) maxChildDepth = Math.max(maxChildDepth, getMaxDepth(node.left, depth + 1));
        if(node.middle != null) maxChildDepth = Math.max(maxChildDepth, getMaxDepth(node.middle, depth + 1));
        if(node.right != null) maxChildDepth = Math.max(maxChildDepth, getMaxDepth(node.right, depth + 1));
        return maxChildDepth;
    }

    private static void collapseAllCompleted(RougeTree.Node node){
        if(node == null) return;
        if(node.completed && !getAllChildren(node).isEmpty()){
            node.collapsed = true;
        }else{
            node.collapsed = false;
            for(RougeTree.Node child : getAllChildren(node)){
                collapseAllCompleted(child);
            }
        }
    }

    private static void expandAll(RougeTree.Node node){
        if(node == null) return;
        node.collapsed = false;
        for(RougeTree.Node child : getAllChildren(node)){
            expandAll(child);
        }
    }

    private static Seq<RougeTree.Node> getUnlockedChildren(RougeTree.Node node){
        Seq<RougeTree.Node> children = new Seq<>();
        if(node.left != null && !node.left.locked) children.add(node.left);
        if(node.middle != null && !node.middle.locked) children.add(node.middle);
        if(node.right != null && !node.right.locked) children.add(node.right);
        return children;
    }

    private static Seq<RougeTree.Node> getAllChildren(RougeTree.Node node){
        Seq<RougeTree.Node> children = new Seq<>();
        if(node.left != null) children.add(node.left);
        if(node.middle != null) children.add(node.middle);
        if(node.right != null) children.add(node.right);
        return children;
    }

    private static void showNodeDetailPopup(RougeTree.Node node){
        BaseDialog detailDialog = new BaseDialog("关卡详情");

        TextureRegionDrawable blackBg = new TextureRegionDrawable(Core.atlas.find("white")){{
            tint.set(new Color(0.08f, 0.08f, 0.12f, 1f));
        }};
        detailDialog.setBackground(blackBg);

        detailDialog.cont.add(node.mapName).color(Pal.accent).fontScale(1.3f).center().pad(10).row();
        detailDialog.cont.row();

        detailDialog.cont.table(preview -> {
            preview.background(Styles.black3);
            TextureRegion icon = RougeTree.getNodeIcon(node.mapName);
            preview.image(icon).size(80).pad(15);
        }).growX().height(110).pad(10).row();

        detailDialog.cont.row();

        float difficulty = getNodeDifficulty(node);
        String diffStars = getDifficultyStars(difficulty);
        detailDialog.cont.add("难度: " + diffStars).color(getDifficultyColor(difficulty)).left().padLeft(15).row();
        detailDialog.cont.row();

        detailDialog.cont.add("波次: " + getWaveCount(node)).color(Color.lightGray).left().padLeft(15).row();
        detailDialog.cont.row();

        detailDialog.cont.add("地图大小: " + getMapSize(node)).color(Color.lightGray).left().padLeft(15).row();
        detailDialog.cont.row();

        detailDialog.cont.add("描述").color(Pal.accent).left().pad(10);
        detailDialog.cont.row();
        detailDialog.cont.add(getNodeDescription(node)).color(Color.gray).growX().padLeft(15).padRight(15).row();
        detailDialog.cont.row();

        detailDialog.cont.row();
        if(node.completed){
            detailDialog.cont.add("[green]已完成[]").color(Color.green).center().pad(10).row();
        }else if(node.locked){
            detailDialog.cont.add("[red]已封锁[]").color(Color.red).center().pad(10).row();
        }else if(!isUnlocked(node)){
            detailDialog.cont.add("[gray]需要完成前置节点[]").color(Color.gray).center().pad(10).row();
        }

        detailDialog.cont.row();
        boolean canEnter = !node.locked && isUnlocked(node) && !node.completed;
        if(RougeNet.isClient()){
            detailDialog.cont.add("[gray]等待房主出发…[]").color(Color.gray).size(200, 50).pad(20);
        }else{
            TextButton enterBtn = new TextButton("进入战斗", Styles.defaultt);
            enterBtn.setDisabled(!canEnter);
            enterBtn.addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y){
                    if(!enterBtn.isDisabled()){
                        detailDialog.hide();
                        launchNode(node);
                    }
                }
            });
            detailDialog.cont.add(enterBtn).size(200, 50).pad(20);
        }

        Seq<RougeTree.Node> children = getUnlockedChildren(node);
        if(!children.isEmpty()){
            detailDialog.cont.row();
            if(node.collapsed){
                detailDialog.cont.button("展开分支", Styles.defaultt, () -> {
                    node.collapsed = false;
                    treePanel.rebuild();
                    detailDialog.hide();
                    showNodeDetailPopup(node);
                }).size(200, 40).pad(10);
            }else{
                detailDialog.cont.button("折叠分支", Styles.defaultt, () -> {
                    node.collapsed = true;
                    treePanel.rebuild();
                    detailDialog.hide();
                    showNodeDetailPopup(node);
                }).size(200, 40).pad(10);
            }
        }

        detailDialog.addCloseButton();
        detailDialog.show();
    }

    private static float getNodeDifficulty(RougeTree.Node node){
        int depth = 0;
        RougeTree.Node n = node;
        while(n.parent != null){
            depth++;
            n = n.parent;
        }
        return 1f + depth * 0.5f;
    }

    private static String getDifficultyStars(float difficulty){
        int stars = (int)difficulty;
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < 5; i++){
            sb.append(i < stars ? "★" : "☆");
        }
        String diffName;
        if(difficulty <= 1.5f) diffName = "简单";
        else if(difficulty <= 2.5f) diffName = "普通";
        else if(difficulty <= 3.5f) diffName = "困难";
        else diffName = "极难";
        return sb + " " + diffName;
    }

    private static Color getDifficultyColor(float difficulty){
        if(difficulty <= 1.5f) return Color.green;
        if(difficulty <= 2.5f) return Color.yellow;
        if(difficulty <= 3.5f) return Color.orange;
        return Color.red;
    }

    private static int getWaveCount(RougeTree.Node node){
        float diff = getNodeDifficulty(node);
        if(diff <= 1.5f) return 5;
        if(diff <= 2.5f) return 8;
        if(diff <= 3.5f) return 12;
        return 15;
    }

    private static String getMapSize(RougeTree.Node node){
        if(node.mapName.contains("frozenForest")) return "480×480";
        if(node.mapName.contains("planetaryTerminal")) return "300×300";
        if(node.mapName.contains("geothermalStronghold")) return "280×280";
        if(node.mapName.contains("testingGrounds")) return "320×320";
        return "256×256";
    }

    private static String getNodeDescription(RougeTree.Node node){
        if(node.mapName.contains("frozenForest")) return "初始关卡，适合新手熟悉操作";
        if(node.mapName.contains("planetaryTerminal")) return "行星终端，通往深空的中转站";
        if(node.mapName.contains("geothermalStronghold")) return "地热要塞，利用地热能源的地下堡垒";
        if(node.mapName.contains("testingGrounds")) return "试验场，武器测试与战术演练区域";
        return "未知区域，需要探索";
    }

    private static boolean isUnlocked(RougeTree.Node node){
        if(node.parent == null) return true;
        return node.parent.completed;
    }

    private static void launchNode(RougeTree.Node node){
        if(RougeNet.isClient()){
            ui.showInfoToast("[scarlet]联机时由房主选择并出发[]", 2f);
            return;
        }
        if(node.locked){
            ui.showInfoToast("该路线已被封锁", 2f);
            return;
        }
        Fi mapFile = RougeMaps.getMapFile(node.mapName);
        if(mapFile == null || !mapFile.exists()){
            ui.showInfoToast("地图文件不存在: " + node.mapName, 3f);
            return;
        }

        Map map;
        try{
            map = MapIO.createMap(mapFile, true);
            try{
                java.lang.reflect.Field fileField = Map.class.getDeclaredField("file");
                fileField.setAccessible(true);
                fileField.set(map, mapFile);
            }catch(Exception re){
                Log.err("Failed to set map file field", re);
            }
        }catch(Exception e){
            Log.err("Failed to load map: " + node.mapName, e);
            ui.showInfoToast("加载地图失败: " + node.mapName, 3f);
            return;
        }

        RougeSave.currentSectorName = node.mapName;
        RougeSave.currentEvacWave = getWaveCount(node);
        // 先广播“离开大厅”：否则 save() 触发的状态包里 inLobby 还是 true，
        // 客户端会先重建树界面、再收到 EV_LOBBY(false)，白白闪一下
        RougeNet.broadcastLobby(false);
        RougeSave.save();

        dialog.hide();
        launchMap(map);
    }

    private static void launchMap(Map map){
        if(RougeNet.isClient()) return;
        try{
            boolean hasEnemyCore = false;
            try{
                java.lang.reflect.Field tilesField = Map.class.getDeclaredField("tiles");
                tilesField.setAccessible(true);
                mindustry.world.Tile[][] tiles = (mindustry.world.Tile[][]) tilesField.get(map);
                if(tiles != null){
                    for(int x = 0; x < map.width; x++){
                        for(int y = 0; y < map.height; y++){
                            if(tiles[x] != null && tiles[x][y] != null){
                                mindustry.world.Tile tile = tiles[x][y];
                                if(tile.block() instanceof CoreBlock && tile.team() == Team.crux){
                                    hasEnemyCore = true;
                                    break;
                                }
                            }
                        }
                        if(hasEnemyCore) break;
                    }
                }
            }catch(Exception e){
                Log.err("Failed to detect enemy cores", e);
            }
            Log.info("Rouge: launching map " + map.plainName() + ", hasEnemyCore=" + hasEnemyCore);

            Rules rules = map.rules().copy();
            rules.attackMode = hasEnemyCore;

            setupEnemySpawns(rules);
            applyTechBonuses(rules);
            // 藏品对规则的影响必须作用在这个 rules 上：
            // WorldLoadEvent 里那句 ArtifactManager.applyToRules(state.rules) 是在 world.loadMap 内部触发的，
            // 那时 state.rules 还是地图自带的规则对象，紧接着就被这里的 rules 覆盖掉了。
            ArtifactManager.applyToRules(rules);

            // 统一走 RougeLobby.enterMap：内部使用原版 WorldReloader，
            // 联机时房主换图、客户端在同一条连接上收到新世界（不掉线），单机下行为不变
            RougeLobby.enterMap(map, rules, RougeSave.currentSectorName, RougeSave.currentEvacWave);

            Log.info("Rouge: launched map " + map.plainName());
        }catch(Exception e){
            Log.err("Failed to launch map", e);
            ui.showInfoToast("启动地图失败: " + e.getMessage(), 3f);
        }
    }

    private static void setupEnemySpawns(Rules rules){
        SpawnGroup dagger = new SpawnGroup(UnitTypes.dagger);
        dagger.unitAmount = 3;
        dagger.begin = 1;
        dagger.spacing = 2;
        dagger.max = 12;
        dagger.unitScaling = 1.5f;

        SpawnGroup crawler = new SpawnGroup(UnitTypes.crawler);
        crawler.unitAmount = 2;
        crawler.begin = 3;
        crawler.spacing = 2;
        crawler.max = 8;
        crawler.unitScaling = 1.2f;

        SpawnGroup vela = new SpawnGroup(UnitTypes.vela);
        vela.unitAmount = 1;
        vela.begin = 8;
        vela.spacing = 3;
        vela.max = 5;
        vela.unitScaling = 1.0f;

        rules.spawns = new Seq<>(new SpawnGroup[]{
            dagger, crawler, vela
        });
    }

    private static void applyTechBonuses(Rules rules){
        // 单位伤害加成
        float dmgMult = RougeTechTree.getUnitDamageMultiplier();
        if(dmgMult > 1.0f){
            rules.unitDamageMultiplier = dmgMult;
        }

        // 建筑血量加成
        float hpMult = RougeTechTree.getBuildingHealthMultiplier();
        if(hpMult > 1.0f){
            rules.blockHealthMultiplier = hpMult;
        }
    }

    private static Fi getModAssets(){
        Mods.LoadedMod mod = mods.getMod("nu");
        if(mod != null && mod.root != null){
            Fi assets = mod.root.child("assets");
            if(assets.exists()) return assets;
        }
        return Core.files.local("");
    }

    static void onNodeCompleted(RougeTree.Node node){
        if(RougeNet.isClient()) return;

        node.completed = true;
        RougeSave.completedNodeIds.add(node.id);

        if(RougeTree.isEndMap(node.mapName)){
            onVictory(node);
            return;
        }

        if(node.left != null && node.middle != null && node.right != null){
            showBranchChoiceDialog(node);
        }else{
            unlockNext(node);
        }

        RougeSave.save();
        if(dialog != null){
            dialog.hide();
            dialog = null;
        }
        show();
    }

    private static void onVictory(RougeTree.Node endNode){
        int bonus = 500;
        int current = RougeSave.storedItems.get(Items.copper, 0);
        RougeSave.storedItems.put(Items.copper, current + bonus);
        current = RougeSave.storedItems.get(Items.lead, 0);
        RougeSave.storedItems.put(Items.lead, current + bonus);

        RougeSave.save();

        String endMapName = getEndMapDisplayName(endNode.mapName);

        new BaseDialog("胜利！"){{
            cont.add("[accent]恭喜通关 Rouge 模式！[]").color(Pal.accent).fontScale(1.3f).pad(20).row();
            cont.row();
            cont.add("你成功完成了路线终点：").color(Color.lightGray).padBottom(5).row();
            cont.add("[yellow]" + endMapName + "[]").fontScale(1.2f).padBottom(20).row();
            cont.row();
            cont.add("获得奖励：").color(Color.lightGray).padBottom(10).row();
            cont.table(reward -> {
                reward.image(Items.copper.uiIcon).size(32).padRight(8);
                reward.add("[yellow]+" + bonus + " 铜矿[]").color(Color.yellow).left().row();
                reward.image(Items.lead.uiIcon).size(32).padRight(8);
                reward.add("[yellow]+" + bonus + " 铅[]").color(Color.yellow).left().row();
            }).padBottom(20).row();
            cont.row();
            cont.add("[gray]提示：你可以重置进度开始新的挑战[]").color(Color.gray).padBottom(15).row();

            buttons.button("继续游戏", Styles.defaultt, () -> {
                hide();
                if(dialog != null){
                    dialog.hide();
                    dialog = null;
                }
                RougeCampaignScreen.show();
            }).size(140, 45);
            buttons.button("重置并重新开始", Styles.defaultt, () -> {
                hide();
                RougeSave.reset();
                RougeSave.treeSeed = System.nanoTime();
                RougeTree.generateTree(RougeSave.treeSeed, 5);
                RougeSave.save();
                if(dialog != null){
                    dialog.hide();
                    dialog = null;
                }
                RougeCampaignScreen.show();
            }).size(160, 45);
        }}.show();
    }

    private static String getEndMapDisplayName(String mapName){
        switch(mapName){
            case "planetaryTerminal": return "行星终端";
            case "geothermalStronghold": return "地热要塞";
            case "testingGrounds": return "试验场";
            default: return mapName;
        }
    }

    private static void showBranchChoiceDialog(RougeTree.Node node){
        new BaseDialog("选择分支"){{
            cont.add("[accent]挑战完成！[] 选择要解锁的路线：").pad(15).row();
            cont.add("[gray]未选择的路线将永久封锁[]").color(Color.gray).padBottom(15).row();

            cont.table(left -> {
                left.background(Styles.black6);
                left.image(RougeTree.getNodeIcon(node.left.mapName)).size(36).pad(8);
                left.table(info -> {
                    info.add("[accent]左线: " + node.left.mapName).color(Color.white).left().row();
                    info.add("[gray]终点: planetaryTerminal[]").color(Color.lightGray).left().row();
                    info.add("[orange]封锁中线和右线[]").left();
                }).growX().padLeft(10);
            }).growX().pad(5).row();
            cont.button("← 选择左线", Styles.defaultt, () -> {
                RougeTree.completeNode(node.id, 0);
                RougeSave.branchChoices.put(node.id, 0);
                RougeSave.save();
                hide();
                if(dialog != null){
                    dialog.hide();
                    dialog = null;
                }
                RougeCampaignScreen.show();
            }).size(240, 50).pad(5).row();

            cont.table(mid -> {
                mid.background(Styles.black6);
                mid.image(RougeTree.getNodeIcon(node.middle.mapName)).size(36).pad(8);
                mid.table(info -> {
                    info.add("[accent]中线: " + node.middle.mapName).color(Color.white).left().row();
                    info.add("[gray]终点: geothermalStronghold[]").color(Color.lightGray).left().row();
                    info.add("[orange]封锁左线和右线[]").left();
                }).growX().padLeft(10);
            }).growX().pad(5).row();
            cont.button("↓ 选择中线", Styles.defaultt, () -> {
                RougeTree.completeNode(node.id, 1);
                RougeSave.branchChoices.put(node.id, 1);
                RougeSave.save();
                hide();
                if(dialog != null){
                    dialog.hide();
                    dialog = null;
                }
                RougeCampaignScreen.show();
            }).size(240, 50).pad(5).row();

            cont.table(right -> {
                right.background(Styles.black6);
                right.image(RougeTree.getNodeIcon(node.right.mapName)).size(36).pad(8);
                right.table(info -> {
                    info.add("[accent]右线: " + node.right.mapName).color(Color.white).left().row();
                    info.add("[gray]终点: testingGrounds[]").color(Color.lightGray).left().row();
                    info.add("[orange]封锁左线和中线[]").left();
                }).growX().padLeft(10);
            }).growX().pad(5).row();
            cont.button("选择右线 →", Styles.defaultt, () -> {
                RougeTree.completeNode(node.id, 2);
                RougeSave.branchChoices.put(node.id, 2);
                RougeSave.save();
                hide();
                if(dialog != null){
                    dialog.hide();
                    dialog = null;
                }
                RougeCampaignScreen.show();
            }).size(240, 50).pad(5).row();

            buttons.button("取消", this::hide).size(100, 40);
        }}.show();
    }

    private static void unlockNext(RougeTree.Node node){
        if(node.left != null && !node.left.locked){
            node.left.locked = false;
        }else if(node.middle != null && !node.middle.locked){
            node.middle.locked = false;
        }else if(node.right != null && !node.right.locked){
            node.right.locked = false;
        }
    }

    static class Minimap extends Group {
        private static final float MINIMAP_W = 180f;
        private static final float MINIMAP_H = 120f;

        Minimap(){
            setSize(MINIMAP_W, MINIMAP_H);
        }

        @Override
        public void draw(){
            Draw.color(0.1f, 0.1f, 0.15f, 0.9f);
            Fill.rect(x + MINIMAP_W / 2, y + MINIMAP_H / 2, MINIMAP_W, MINIMAP_H);
            Draw.color(Color.white);
            Lines.stroke(1f);
            Lines.rect(x + 0.5f, y + 0.5f, MINIMAP_W - 1, MINIMAP_H - 1);

            if(treePanel == null || treePanel.nodeVisuals.isEmpty()){
                Draw.reset();
                return;
            }

            float treeW = treePanel.contentWidth;
            float treeH = treePanel.contentHeight;
            float scaleX = (MINIMAP_W - 10) / treeW;
            float scaleY = (MINIMAP_H - 10) / treeH;
            float scale = Math.min(scaleX, scaleY);
            float offsetX = (MINIMAP_W - treeW * scale) / 2;
            float offsetY = (MINIMAP_H - treeH * scale) / 2;

            for(NodeVisual nv : treePanel.nodeVisuals){
                RougeTree.Node node = nv.node;
                if(node.parent != null && !node.parent.collapsed){
                    NodeVisual parentVisual = null;
                    for(NodeVisual pv : treePanel.nodeVisuals){
                        if(pv.node == node.parent){ parentVisual = pv; break; }
                    }
                    if(parentVisual != null) drawMiniLine(parentVisual, nv, offsetX, offsetY, scale);
                }
            }

            for(NodeVisual nv : treePanel.nodeVisuals){
                drawMiniNode(nv, offsetX, offsetY, scale);
            }

            Draw.reset();
            super.draw();
        }

        private void drawMiniLine(NodeVisual parent, NodeVisual child, float ox, float oy, float s){
            float x1 = ox + (parent.x + TreePanel.NODE_W) * s;
            float y1 = oy + (parent.y + TreePanel.NODE_H / 2) * s;
            float x2 = ox + child.x * s;
            float y2 = oy + (child.y + TreePanel.NODE_H / 2) * s;

            Color c;
            if(child.node.locked) c = Color.red;
            else if(child.node.completed) c = Color.green;
            else if(isUnlocked(child.node)) c = Color.yellow;
            else c = Color.gray;

            Draw.color(c);
            Lines.stroke(1f);
            Lines.line(x1, y1, x2, y2);
        }

        private void drawMiniNode(NodeVisual nv, float ox, float oy, float s){
            float nx = ox + nv.x * s;
            float ny = oy + nv.y * s;
            float nr = TreePanel.NODE_R * s;

            RougeTree.Node node = nv.node;
            Color c;
            if(node.completed) c = Color.green;
            else if(node.locked) c = Color.red;
            else if(isUnlocked(node)) c = Color.yellow;
            else c = Color.gray;

            Draw.color(c);
            Fill.circle(nx + nr, ny + nr, nr);
        }

        private boolean isUnlocked(RougeTree.Node node){
            if(node.parent == null) return true;
            return node.parent.completed;
        }
    }

    static class TreePanel extends Group {
        static final float NODE_R = 32f;
        static final float NODE_W = NODE_R * 2;
        static final float NODE_H = NODE_R * 2;
        static final float H_SPACING = 80f;
        static final float V_SPACING = 50f;
        static final int MIN_LEVELS_PER_PAGE = 2;

        Seq<NodeVisual> nodeVisuals = new Seq<>();
        private float contentWidth = 400f;
        private float contentHeight = 300f;
        private int levelsPerPage = MIN_LEVELS_PER_PAGE;

        TreePanel(){
            buildTree();
            setupInput();
        }

        private void setupInput(){
            addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y){
                    handleClick(x, y);
                }
            });
            this.touchable = Touchable.enabled;
        }

        void rebuild(){
            buildTree();
        }

        /** 根据屏幕宽度计算每页能显示的层数 */
        private int calculateLevelsPerPage(){
            float screenWidth = Core.graphics.getWidth();
            // 减去右侧信息面板(280)和边距
            float availableWidth = screenWidth - 320f;
            float levelWidth = NODE_W + H_SPACING;
            int calculated = Math.max(MIN_LEVELS_PER_PAGE, (int)(availableWidth / levelWidth));
            // 限制最大值，避免一页显示太多
            return Math.min(calculated, 4);
        }

        void buildTree(){
            RougeTree.Node root = RougeTree.getRoot();
            if(root == null) return;

            // 动态计算每页显示层数
            levelsPerPage = calculateLevelsPerPage();

            int minDepth = currentPage * levelsPerPage;
            int maxDepth = minDepth + levelsPerPage;

            layoutNode(root, 30f, 0f, 0);

            nodeVisuals.clear();
            collectNodes(root, 0, minDepth, maxDepth);

            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
            float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for(NodeVisual nv : nodeVisuals){
                minX = Math.min(minX, nv.x);
                maxX = Math.max(maxX, nv.x + NODE_W);
                minY = Math.min(minY, nv.y);
                maxY = Math.max(maxY, nv.y + NODE_H);
            }

            float pad = 80f;
            contentWidth = (maxX - minX) + pad * 2;
            contentHeight = (maxY - minY) + pad * 2;

            float offsetX = -minX + pad;
            float offsetY = -minY + pad;
            for(NodeVisual nv : nodeVisuals){
                nv.x += offsetX;
                nv.y += offsetY;
            }

            setSize(contentWidth, contentHeight);
        }

        private void layoutNode(RougeTree.Node node, float x, float y, int depth){
            if(node == null) return;

            node.x = x;
            node.y = y;
            node.depth = depth;

            // 只布局当前页范围内的节点
            int minDepth = currentPage * levelsPerPage;
            int maxDepth = minDepth + levelsPerPage;

            if(node.collapsed || depth >= maxDepth) return;

            float childX = x + NODE_W + H_SPACING;

            Seq<RougeTree.Node> children = getAllChildren(node);

            if(!children.isEmpty()){
                int count = children.size;
                float totalHeight = count * NODE_H + (count - 1) * V_SPACING;
                float startY = y + totalHeight / 2 - NODE_H / 2;

                for(int i = 0; i < children.size; i++){
                    layoutNode(children.get(i), childX, startY - i * (NODE_H + V_SPACING), depth + 1);
                }
            }
        }

        private void collectNodes(RougeTree.Node node, int depth, int minDepth, int maxDepth){
            if(node == null) return;
            if(depth >= minDepth && depth < maxDepth){
                nodeVisuals.add(new NodeVisual(node, node.x, node.y));
            }
            if(node.collapsed) return;
            if(depth + 1 < maxDepth){
                if(node.left != null) collectNodes(node.left, depth + 1, minDepth, maxDepth);
                if(node.middle != null) collectNodes(node.middle, depth + 1, minDepth, maxDepth);
                if(node.right != null) collectNodes(node.right, depth + 1, minDepth, maxDepth);
            }
        }

        private void handleClick(float x, float y){
            for(NodeVisual nv : nodeVisuals){
                if(nv.contains(x, y)){
                    selectedNode = nv.node;
                    // 房主把“我选了哪个节点”广播出去，客户端会高亮同一个节点
                    if(RougeNet.isHost()) RougeNet.broadcastSelect(nv.node.id);
                    treePanel.rebuild();
                    updateSelectedInfoPanel();
                    break;
                }
            }
        }

        @Override
        public void draw(){
            for(NodeVisual nv : nodeVisuals){
                drawConnections(nv);
            }

            for(NodeVisual nv : nodeVisuals){
                drawNode(nv);
            }

            super.draw();
        }

        private void drawConnections(NodeVisual nv){
            if(nv == null || nv.node == null) return;
            RougeTree.Node node = nv.node;
            if(node.left != null && !node.collapsed) drawLine(nv, findVisual(node.left));
            if(node.middle != null && !node.collapsed) drawLine(nv, findVisual(node.middle));
            if(node.right != null && !node.collapsed) drawLine(nv, findVisual(node.right));
        }

        private NodeVisual findVisual(RougeTree.Node node){
            for(NodeVisual nv : nodeVisuals){
                if(nv.node == node) return nv;
            }
            return null;
        }

        private void drawLine(NodeVisual parent, NodeVisual child){
            if(parent == null || child == null) return;
            float x1 = parent.x + NODE_W;
            float y1 = parent.y + NODE_H / 2;
            float x2 = child.x;
            float y2 = child.y + NODE_H / 2;

            Color lineColor;
            if(child.node.locked){
                lineColor = Color.red;
            }else if(child.node.completed){
                lineColor = Color.green;
            }else if(isUnlocked(child.node)){
                lineColor = Color.yellow;
            }else{
                lineColor = Color.gray;
            }

            Draw.color(lineColor);
            Lines.stroke(2f);
            Lines.line(x1, y1, x2, y2);
            Draw.reset();
        }

        private void drawNode(NodeVisual nv){
            float x = nv.x;
            float y = nv.y;
            RougeTree.Node node = nv.node;
            float cx = x + NODE_R;
            float cy = y + NODE_R;

            boolean isSelected = (node == selectedNode);

            // 内部填充始终为暗色
            Color bgColor = new Color(0.12f, 0.12f, 0.15f, 0.95f);
            // 外框颜色根据状态/类型变化
            Color borderColor;

            // 节点颜色逻辑：已占领→绿，已封锁→红，未占领→根据类型着色（仅外框）
            if(node.completed){
                // 已占领 - 绿色外框
                borderColor = Color.green;
            }else if(node.locked){
                // 已封锁 - 红色外框
                borderColor = Color.red;
            }else{
                // 未占领 - 根据节点类型着色（仅外框）
                borderColor = getNodeColor(node.type);
            }

            // 选中时高亮边框
            if(isSelected){
                borderColor = Color.cyan;
            }

            // 绘制内部填充（始终暗色）
            Draw.color(bgColor);
            Fill.circle(cx, cy, NODE_R);

            // 绘制外框（根据类型着色）
            Draw.color(borderColor);
            Lines.stroke(isSelected ? 3f : 2f);
            Lines.circle(cx, cy, NODE_R);
            Draw.reset();

            if(node.collapsed){
                Seq<RougeTree.Node> children = new Seq<>();
                if(node.left != null && !node.left.locked) children.add(node.left);
                if(node.middle != null && !node.middle.locked) children.add(node.middle);
                if(node.right != null && !node.right.locked) children.add(node.right);
                if(!children.isEmpty()){
                    Font font = Fonts.def;
                    font.setColor(Color.green);
                    font.draw("+" + countVisibleChildren(node), cx, cy + font.getLineHeight() / 4, 0, Align.center, false);
                    Draw.reset();
                }
            }

            // 绘制节点类型图标（不显示地图名和图标）
            drawNodeTypeIcon(node, cx, cy);

            if(node.locked){
                Draw.color(Color.red);
                Lines.stroke(3f);
                float s = NODE_R * 0.4f;
                Lines.line(cx - s, cy - s, cx + s, cy + s);
                Lines.line(cx + s, cy - s, cx - s, cy + s);
                Draw.reset();
            }else if(node.completed){
                Font font = Fonts.def;
                font.setColor(Color.green);
                font.draw("✓", cx + NODE_R * 0.5f, cy + 4);
                Draw.reset();
            }
        }

        /** 获取节点类型对应的颜色 */
        private Color getNodeColor(RougeTree.NodeType type){
            if(type == null) return Color.gray;
            switch(type){
                case ATTACK: return NuColor.SailColor;
                case DEFENSE: return Color.yellow;
                case REST: return NuColor.SurvivalColor;
                case GIFT: return NuColor.PaleColor;
                case SHOP: return NuColor.HonorColor;
                case SPECIAL: return Color.pink;
                default: return Color.gray;
            }
        }

        /** 绘制节点类型图标 */
        private void drawNodeTypeIcon(RougeTree.Node node, float cx, float cy){
            // 根据节点类型绘制一个简单的标识
            Color typeColor = getNodeColor(node.type);
            Draw.color(typeColor);
            Lines.stroke(2f);

            float r = NODE_R * 0.35f;
            switch(node.type){
                case ATTACK:
                    // 进攻 - 菱形
                    Lines.line(cx - r, cy, cx, cy - r);
                    Lines.line(cx, cy - r, cx + r, cy);
                    Lines.line(cx + r, cy, cx, cy + r);
                    Lines.line(cx, cy + r, cx - r, cy);
                    break;
                case DEFENSE:
                    // 防守 - 盾牌形（倒U）
                    Lines.line(cx - r, cy + r * 0.5f, cx - r, cy - r * 0.3f);
                    Lines.line(cx - r, cy - r * 0.3f, cx, cy - r);
                    Lines.line(cx, cy - r, cx + r, cy - r * 0.3f);
                    Lines.line(cx + r, cy - r * 0.3f, cx + r, cy + r * 0.5f);
                    Lines.line(cx + r, cy + r * 0.5f, cx, cy + r);
                    Lines.line(cx, cy + r, cx - r, cy + r * 0.5f);
                    break;
                case REST:
                    // 休息 - 圆形
                    Lines.circle(cx, cy, r * 0.7f);
                    break;
                case GIFT:
                    // 馈赠 - 十字/加号
                    Lines.line(cx - r, cy, cx + r, cy);
                    Lines.line(cx, cy - r, cx, cy + r);
                    break;
                case SHOP:
                    // 商店 - 方形
                    Lines.line(cx - r, cy - r, cx + r, cy - r);
                    Lines.line(cx + r, cy - r, cx + r, cy + r);
                    Lines.line(cx + r, cy + r, cx - r, cy + r);
                    Lines.line(cx - r, cy + r, cx - r, cy - r);
                    break;
                case SPECIAL:
                    // 特殊 - 星形（简化为三角形）
                    Lines.line(cx, cy - r, cx + r, cy + r);
                    Lines.line(cx + r, cy + r, cx - r, cy + r);
                    Lines.line(cx - r, cy + r, cx, cy - r);
                    break;
            }
            Draw.reset();
        }

        private int countVisibleChildren(RougeTree.Node node){
            if(!node.collapsed) return 0;
            int count = 0;
            if(node.left != null && !node.left.locked) count += 1 + countVisibleChildren(node.left);
            if(node.middle != null && !node.middle.locked) count += 1 + countVisibleChildren(node.middle);
            if(node.right != null && !node.right.locked) count += 1 + countVisibleChildren(node.right);
            return count;
        }

        private boolean isUnlocked(RougeTree.Node node){
            if(node.parent == null) return true;
            return node.parent.completed;
        }
    }

    static class NodeVisual {
        RougeTree.Node node;
        float x, y;

        NodeVisual(RougeTree.Node node, float x, float y){
            this.node = node;
            this.x = x;
            this.y = y;
        }

        boolean contains(float px, float py){
            float cx = x + TreePanel.NODE_R;
            float cy = y + TreePanel.NODE_R;
            float dx = px - cx;
            float dy = py - cy;
            return dx * dx + dy * dy <= TreePanel.NODE_R * TreePanel.NODE_R;
        }
    }
}
