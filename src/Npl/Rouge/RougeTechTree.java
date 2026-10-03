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
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.modules.*;

import static mindustry.Vars.*;

/**
 * RougeTechTree —— 外场演绎（科技树）
 * ==============================================================
 * 布局：
 *   - 左侧：科技树节点（从左到右展开，占满剩余空间）
 *   - 右侧：详情面板（固定宽度 320px，贴右边框）
 * 功能：
 *   - 小地图导航
 *   - 折叠/展开已满级分支
 */
public class RougeTechTree {

    private static BaseDialog dialog;
    private static TechNode selectedNode = null;
    private static Table detailPanel;
    private static TreePanel treePanel;
    private static ScrollPane treeScroll;
    private static Minimap minimap;
    private static boolean readOnly = false;

    public static final Seq<TechNode> techNodes = new Seq<>();

    public static class TechNode {
        public String id;
        public String name;
        public String description;
        public TextureRegion icon;
        public int maxLevel;
        public int currentLevel;
        public IntSeq costPerLevel;
        public TechNode parent;
        public Seq<TechNode> children = new Seq<>();
        public float x, y;
        public boolean collapsed = false;

        public TechNode(String id, String name, String description, TextureRegion icon, int maxLevel, IntSeq costs){
            this.id = id;
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.maxLevel = maxLevel;
            this.costPerLevel = costs;
            this.currentLevel = 0;
        }

        public int getCurrentCost(){
            if(currentLevel >= costPerLevel.size) return costPerLevel.get(costPerLevel.size - 1);
            return costPerLevel.get(currentLevel);
        }

        public boolean isMaxed(){
            return currentLevel >= maxLevel;
        }

        public boolean canAfford(){
            return RougeSave.getStored(Items.copper) >= getCurrentCost() && !isMaxed();
        }

        public boolean isUnlocked(){
            return parent == null || parent.parent == null || parent.currentLevel > 0;
        }

        public boolean allChildrenMaxed(){
            if(children.isEmpty()) return isMaxed();
            for(TechNode child : children){
                if(!child.allChildrenMaxed()) return false;
            }
            return true;
        }
    }

    private static void initTechTree(){
        if(!techNodes.isEmpty()) return;

        TechNode coreRoot = new TechNode("core_root", "核心强化", "提升核心存储上限（每级+500）",
            Core.atlas.find("nu-core_root"), 5, new IntSeq(new int[]{100, 200, 400, 800, 1600}));
        techNodes.add(coreRoot);

        TechNode startRes = new TechNode("start_resources", "起始资源", "每关开局额外获得资源（每级+100铜+铅）",
            Core.atlas.find("nu-start_resources"), 5, new IntSeq(new int[]{150, 300, 600, 1200, 2400}));
        startRes.parent = coreRoot;
        coreRoot.children.add(startRes);
        techNodes.add(startRes);

        TechNode unitDmg = new TechNode("unit_damage", "单位伤害", "提升我方单位伤害（每级+5%）",
            Core.atlas.find("nu-unit_damage"), 10, new IntSeq(new int[]{200, 400, 800, 1600, 3200, 4000, 5000, 6000, 7000, 8000}));
        unitDmg.parent = coreRoot;
        coreRoot.children.add(unitDmg);
        techNodes.add(unitDmg);

        TechNode buildHp = new TechNode("building_hp", "建筑血量", "提升建筑最大血量（每级+10%）",
            Core.atlas.find("nu-building_hp"), 5, new IntSeq(new int[]{250, 500, 1000, 2000, 4000}));
        buildHp.parent = coreRoot;
        coreRoot.children.add(buildHp);
        techNodes.add(buildHp);

        TechNode evacEff = new TechNode("evac_efficiency", "撤离效率", "提升撤离时资源携带比例（每级+5%，初始50%）",
            Core.atlas.find("nu-evac_efficiency"), 5, new IntSeq(new int[]{300, 600, 1200, 2400, 4800}));
        evacEff.parent = coreRoot;
        coreRoot.children.add(evacEff);
        techNodes.add(evacEff);

        TechNode advancedDmg = new TechNode("advanced_damage", "高级伤害", "大幅提升单位伤害（每级+10%）",
            Core.atlas.find("nu-advanced_damage"), 5, new IntSeq(new int[]{2000, 4000, 8000, 16000, 32000}));
        advancedDmg.parent = unitDmg;
        unitDmg.children.add(advancedDmg);
        techNodes.add(advancedDmg);

        TechNode advancedHp = new TechNode("advanced_hp", "高级血量", "大幅提升建筑血量（每级+15%）",
            Core.atlas.find("nu-advanced_hp"), 5, new IntSeq(new int[]{2500, 5000, 10000, 20000, 40000}));
        advancedHp.parent = buildHp;
        buildHp.children.add(advancedHp);
        techNodes.add(advancedHp);

        TechNode unitStorage = new TechNode("unit_storage", "仓储扩容", "提升单位传输站储存上限（每级+5，初始10）",
            Core.atlas.find("nu-unit_storage"), 5, new IntSeq(new int[]{400, 800, 1600, 3200, 6400}));
        unitStorage.parent = evacEff;
        evacEff.children.add(unitStorage);
        techNodes.add(unitStorage);

        loadLevels();
    }

    private static void loadLevels(){
        if(RougeSave.techLevels == null) return;
        for(TechNode node : techNodes){
            Integer level = RougeSave.techLevels.get(node.id.hashCode());
            if(level != null) node.currentLevel = level;
        }
    }

    private static void saveLevels(){
        if(RougeSave.techLevels == null){
            RougeSave.techLevels = new IntMap<>();
        }
        for(TechNode node : techNodes){
            RougeSave.techLevels.put(node.id.hashCode(), node.currentLevel);
        }
        RougeSave.save();
    }

    public static void show(){
        readOnly = false;
        initTechTree();
        selectedNode = null;
        build();
        dialog.show();
    }

    public static void showReadOnly(){
        readOnly = true;
        initTechTree();
        selectedNode = null;
        build();
        dialog.show();
    }

    private static void build(){
        dialog = new BaseDialog("外场演绎 — 科技树");

        TextureRegionDrawable blackBg = new TextureRegionDrawable(Core.atlas.find("white")){{
            tint.set(new Color(0.05f, 0.05f, 0.1f, 1f));
        }};
        dialog.setBackground(blackBg);

        dialog.cont.table(top -> {
            if(readOnly){
                top.add("[gray]外场演绎 (只读)[]").color(Color.gray).fontScale(1.4f).left().padRight(30);
                top.add("[gray]仅房主可操作[]").color(Color.gray).left();
            }else{
                top.add("外场演绎").color(Pal.accent).fontScale(1.4f).left().padRight(30);
                top.add("消耗铜矿获取永久增益").color(Color.lightGray).left();
            }
        }).growX().pad(15).row();

        dialog.cont.table(main -> {
            main.table(left -> {
                left.background(Styles.black6);
                left.add("科技树").color(Pal.accent).pad(10).row();
                left.row();
                treePanel = new TreePanel();
                treeScroll = new ScrollPane(treePanel, Styles.defaultPane);
                treeScroll.setScrollingDisabled(false, false);
                treeScroll.setFadeScrollBars(false);
                treeScroll.setOverscroll(false, false);
                left.add(treeScroll).grow().pad(10);

                left.row();
                left.table(controls -> {
                    controls.add("[gray]滚轮缩放 | 右键拖动平移[]").color(Color.gray).padRight(15);

                    controls.button("折叠全部", Styles.defaultt, () -> {
                        for(TechNode node : techNodes){
                            if(node.allChildrenMaxed() && !node.children.isEmpty()){
                                node.collapsed = true;
                            }
                        }
                        treePanel.rebuild();
                    }).size(90, 30).padRight(5);

                    controls.button("展开全部", Styles.defaultt, () -> {
                        for(TechNode node : techNodes){
                            node.collapsed = false;
                        }
                        treePanel.rebuild();
                    }).size(90, 30);
                }).pad(10);
            }).grow().left();

            main.table(right -> {
                right.background(Styles.black6);
                detailPanel = new Table();
                updateDetailPanel();
                right.add(detailPanel).size(320, 0).growY().pad(10);
            }).width(320).growY().right();
        }).grow().pad(10).row();

        minimap = new Minimap();

        dialog.cont.table(bottom -> {
            bottom.button("关闭", Styles.defaultt, dialog::hide).size(120, 40);
        }).pad(15);

        dialog.addCloseButton();
    }

    private static void updateDetailPanel(){
        detailPanel.clear();

        if(selectedNode == null){
            detailPanel.add("[gray]请在左侧选择一个科技节点[]").color(Color.gray).pad(20);
            return;
        }

        TechNode node = selectedNode;

        detailPanel.table(header -> {
            header.image(node.icon).size(48).pad(10);
            header.table(info -> {
                info.add(node.name).color(Pal.accent).fontScale(1.3f).left().row();
                info.add("等级: " + node.currentLevel + "/" + node.maxLevel).color(Color.lightGray).left();
            }).growX();
        }).growX().pad(10).row();

        detailPanel.row();

        detailPanel.add("描述").color(Pal.accent).left().pad(10);
        detailPanel.row();
        detailPanel.add(node.description).color(Color.lightGray).growX().padLeft(10).padRight(10).row();
        detailPanel.row();

        detailPanel.add("效果").color(Pal.accent).left().pad(10);
        detailPanel.row();
        detailPanel.add(getEffectDescription(node)).color(Color.yellow).growX().padLeft(10).padRight(10).row();
        detailPanel.row();

        if(node.parent != null){
            detailPanel.add("前置需求").color(Pal.accent).left().pad(10);
            detailPanel.row();
            String reqName = node.parent.name + " Lv.1";
            boolean met = node.parent.currentLevel > 0;
            detailPanel.add((met ? "[green]✓[] " : "[red]✗[] ") + reqName).color(met ? Color.green : Color.red).padLeft(10).row();
            detailPanel.row();
        }

        detailPanel.row();
        if(node.isMaxed()){
            detailPanel.add("[green]已满级[]").color(Color.green).center().pad(20).row();
        }else if(readOnly){
            detailPanel.add("[gray]当前等级: " + node.currentLevel + "/" + node.maxLevel + "[]").color(Color.gray).center().pad(10).row();
            detailPanel.row();
            detailPanel.add("[gray]仅房主可研究[]").color(Color.gray).center().pad(10).row();
        }else{
            detailPanel.add("费用: [yellow]" + node.getCurrentCost() + " 铜矿[]").color(Color.lightGray).center().pad(10).row();
            detailPanel.row();

            boolean canResearch = node.canAfford() && node.isUnlocked();
            TextButton researchBtn = new TextButton("研究", Styles.defaultt);
            researchBtn.setDisabled(!canResearch);
            researchBtn.addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y){
                    if(!researchBtn.isDisabled()){
                        doResearch(node);
                    }
                }
            });
            detailPanel.add(researchBtn).size(200, 50).pad(20);
        }

        if(!node.children.isEmpty()){
            detailPanel.row();
            if(node.collapsed){
                detailPanel.button("展开分支", Styles.defaultt, () -> {
                    node.collapsed = false;
                    treePanel.rebuild();
                }).size(200, 40).pad(10);
            }else{
                detailPanel.button("折叠分支", Styles.defaultt, () -> {
                    node.collapsed = true;
                    treePanel.rebuild();
                }).size(200, 40).pad(10);
            }
        }
    }

    private static String getEffectDescription(TechNode node){
        switch(node.id){
            case "core_root":
                return "核心存储上限: " + (1000 + node.currentLevel * 500) + " → " + (1000 + (node.currentLevel + 1) * 500);
            case "start_resources":
                return "起始资源: +" + (node.currentLevel * 100) + " → +" + ((node.currentLevel + 1) * 100);
            case "unit_damage":
                return "单位伤害: +" + (node.currentLevel * 5) + "% → +" + ((node.currentLevel + 1) * 5) + "%";
            case "building_hp":
                return "建筑血量: +" + (node.currentLevel * 10) + "% → +" + ((node.currentLevel + 1) * 10) + "%";
            case "evac_efficiency":
                return "撤离携带: " + ((int)(RougeTechTree.getEvacuationRatio() * 100)) + "% → " + ((int)((node.currentLevel + 1) * 5 + 50)) + "%";
            case "advanced_damage":
                return "额外单位伤害: +" + (node.currentLevel * 10) + "% → +" + ((node.currentLevel + 1) * 10) + "%";
            case "advanced_hp":
                return "额外建筑血量: +" + (node.currentLevel * 15) + "% → +" + ((node.currentLevel + 1) * 15) + "%";
            default:
                return "效果等级 " + node.currentLevel;
        }
    }

    private static void doResearch(TechNode node){
        int cost = node.getCurrentCost();
        if(RougeSave.getStored(Items.copper) < cost){
            ui.showInfoToast("铜矿不足", 2f);
            return;
        }

        int current = RougeSave.storedItems.get(Items.copper, 0);
        RougeSave.storedItems.put(Items.copper, current - cost);

        node.currentLevel++;
        saveLevels();

        ui.showInfoToast(node.name + " 升级到 Lv." + node.currentLevel, 2f);

        updateDetailPanel();
        treePanel.rebuild();
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

            for(TechNodeVisual nv : treePanel.nodeVisuals){
                TechNode node = nv.node;
                if(node.parent != null){
                    TechNodeVisual parentVisual = null;
                    for(TechNodeVisual pv : treePanel.nodeVisuals){
                        if(pv.node == node.parent){ parentVisual = pv; break; }
                    }
                    if(parentVisual != null) drawMiniLine(parentVisual, nv, offsetX, offsetY, scale);
                }
            }

            for(TechNodeVisual nv : treePanel.nodeVisuals){
                drawMiniNode(nv, offsetX, offsetY, scale);
            }

            Draw.reset();
            super.draw();
        }

        private void drawMiniLine(TechNodeVisual parent, TechNodeVisual child, float ox, float oy, float s){
            float x1 = ox + (parent.x + TreePanel.NODE_W / 2) * s;
            float y1 = oy + parent.y * s;
            float x2 = ox + (child.x + TreePanel.NODE_W / 2) * s;
            float y2 = oy + (child.y + TreePanel.NODE_H) * s;

            Color c;
            if(child.node.isMaxed()) c = Color.green;
            else if(child.node.canAfford() && child.node.isUnlocked()) c = Color.yellow;
            else if(child.node.isUnlocked()) c = Color.gray;
            else c = Color.red;

            Draw.color(c);
            Lines.stroke(1f);
            Lines.line(x1, y1, x2, y2);
        }

        private void drawMiniNode(TechNodeVisual nv, float ox, float oy, float s){
            float nx = ox + nv.x * s;
            float ny = oy + nv.y * s;
            float nw = TreePanel.NODE_W * s;
            float nh = TreePanel.NODE_H * s;

            TechNode node = nv.node;
            Color c;
            if(node.isMaxed()) c = Color.green;
            else if(node.canAfford() && node.isUnlocked()) c = Color.yellow;
            else if(node.isUnlocked()) c = Color.lightGray;
            else c = Color.red;

            Draw.color(c);
            Fill.rect(nx + nw / 2, ny + nh / 2, nw, nh);
        }
    }

    static class TreePanel extends Group {
        static final float NODE_W = 100f;
        static final float NODE_H = 60f;
        static final float H_SPACING = 80f;
        static final float V_SPACING = 40f;

        Seq<TechNodeVisual> nodeVisuals = new Seq<>();
        private float contentWidth = 320f;
        private float contentHeight = 350f;

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

        void buildTree(){
            nodeVisuals.clear();

            TechNode root = techNodes.first();
            layoutNode(root, 0f, 0f);

            collectNodes(root);

            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
            float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for(TechNodeVisual nv : nodeVisuals){
                minX = Math.min(minX, nv.x);
                maxX = Math.max(maxX, nv.x + NODE_W);
                minY = Math.min(minY, nv.y);
                maxY = Math.max(maxY, nv.y + NODE_H);
            }

            float pad = 60f;
            contentWidth = (maxX - minX) + pad * 2;
            contentHeight = (maxY - minY) + pad * 2;

            float offsetX = -minX + pad;
            float offsetY = -minY + pad;
            for(TechNodeVisual nv : nodeVisuals){
                nv.x += offsetX;
                nv.y += offsetY;
            }

            setSize(contentWidth, contentHeight);
        }

        private void layoutNode(TechNode node, float x, float y){
            if(node == null) return;
            node.x = x;
            node.y = y;

            if(node.collapsed) return;

            if(!node.children.isEmpty()){
                int count = node.children.size;
                float totalWidth = count * NODE_W + (count - 1) * H_SPACING;
                float startX = x - totalWidth / 2 + NODE_W / 2;
                float childY = y + NODE_H + V_SPACING;

                for(int i = 0; i < node.children.size; i++){
                    TechNode child = node.children.get(i);
                    layoutNode(child, startX + i * (NODE_W + H_SPACING), childY);
                }
            }
        }

        private void collectNodes(TechNode node){
            if(node == null) return;
            nodeVisuals.add(new TechNodeVisual(node, node.x, node.y));
            if(node.collapsed) return;
            for(TechNode child : node.children){
                collectNodes(child);
            }
        }

        private void handleClick(float x, float y){
            for(TechNodeVisual nv : nodeVisuals){
                if(nv.contains(x, y)){
                    selectedNode = nv.node;
                    updateDetailPanel();
                    break;
                }
            }
        }

        @Override
        public void draw(){
            for(TechNodeVisual nv : nodeVisuals){
                TechNode node = nv.node;
                if(node.parent != null && !node.parent.collapsed){
                    TechNodeVisual parentVisual = findVisual(node.parent);
                    if(parentVisual != null) drawLine(parentVisual, nv);
                }
            }

            for(TechNodeVisual nv : nodeVisuals){
                drawNode(nv);
            }

            super.draw();
        }

        private TechNodeVisual findVisual(TechNode node){
            for(TechNodeVisual nv : nodeVisuals){
                if(nv.node == node) return nv;
            }
            return null;
        }

        private void drawLine(TechNodeVisual parent, TechNodeVisual child){
            float x1 = parent.x + NODE_W / 2;
            float y1 = parent.y;
            float x2 = child.x + NODE_W / 2;
            float y2 = child.y + NODE_H;

            Color lineColor;
            if(child.node.isMaxed()){
                lineColor = Color.green;
            }else if(child.node.canAfford() && child.node.isUnlocked()){
                lineColor = Color.yellow;
            }else if(child.node.isUnlocked()){
                lineColor = Color.gray;
            }else{
                lineColor = Color.red;
            }

            Draw.color(lineColor);
            Lines.stroke(2f);
            Lines.line(x1, y1, x2, y2);
            Draw.reset();
        }

        private void drawNode(TechNodeVisual nv){
            float x = nv.x;
            float y = nv.y;
            TechNode node = nv.node;

            boolean isSelected = (node == selectedNode);

            Color borderColor;
            Color bgColor = new Color(0.12f, 0.12f, 0.15f, 0.95f);

            if(isSelected){
                borderColor = Color.cyan;
                bgColor.set(0.15f, 0.2f, 0.25f, 0.95f);
            }else if(node.isMaxed()){
                borderColor = Color.green;
                bgColor.set(0.1f, 0.3f, 0.1f, 0.95f);
            }else if(node.canAfford() && node.isUnlocked()){
                borderColor = Color.yellow;
            }else if(node.isUnlocked()){
                borderColor = Color.lightGray;
            }else{
                borderColor = Color.red;
                bgColor.set(0.25f, 0.1f, 0.1f, 0.95f);
            }

            Draw.color(bgColor);
            Fill.rect(x + NODE_W / 2, y + NODE_H / 2, NODE_W, NODE_H);

            Draw.color(borderColor);
            Lines.stroke(isSelected ? 3f : 2f);
            Lines.rect(x + 0.5f, y + 0.5f, NODE_W - 1, NODE_H - 1);
            Draw.reset();

            if(node.collapsed && !node.children.isEmpty()){
                Font font = Fonts.def;
                font.setColor(Color.green);
                font.draw("+" + countVisibleChildren(node), x + NODE_W / 2, y + NODE_H / 2 + font.getLineHeight() / 4, 0, Align.center, false);
                Draw.reset();
            }

            if(node.icon != null){
                Draw.color(Color.white);
                Draw.rect(node.icon, x + 20, y + NODE_H / 2, 24, 24);
            }

            Font font = Fonts.def;
            font.setColor(Color.white);
            font.draw(node.name, x + 42, y + NODE_H / 2 + 6, 0, Align.left, false);
            font.setColor(Color.gray);
            font.draw("Lv." + node.currentLevel + "/" + node.maxLevel, x + 42, y + NODE_H / 2 - 10, 0, Align.left, false);
        }

        private int countVisibleChildren(TechNode node){
            if(!node.collapsed) return 0;
            int count = 0;
            for(TechNode child : node.children){
                count += 1 + countVisibleChildren(child);
            }
            return count;
        }
    }

    static class TechNodeVisual {
        TechNode node;
        float x, y;

        TechNodeVisual(TechNode node, float x, float y){
            this.node = node;
            this.x = x;
            this.y = y;
        }

        boolean contains(float px, float py){
            return px >= x && px <= x + TreePanel.NODE_W
                && py >= y && py <= y + TreePanel.NODE_H;
        }
    }

    public static int getCoreStorageLimit(){
        TechNode node = findNode("core_root");
        return 1000 + (node != null ? node.currentLevel * 500 : 0);
    }

    public static int getUnitStorageLimit(){
        TechNode node = findNode("unit_storage");
        return 10 + (node != null ? node.currentLevel * 5 : 0);
    }

    public static int getStartingResources(){
        TechNode node = findNode("start_resources");
        return node != null ? node.currentLevel * 100 : 0;
    }

    public static float getUnitDamageMultiplier(){
        TechNode dmg = findNode("unit_damage");
        TechNode advDmg = findNode("advanced_damage");
        float mult = 1.0f;
        if(dmg != null) mult += dmg.currentLevel * 0.05f;
        if(advDmg != null) mult += advDmg.currentLevel * 0.10f;
        return mult;
    }

    public static float getBuildingHealthMultiplier(){
        TechNode hp = findNode("building_hp");
        TechNode advHp = findNode("advanced_hp");
        float mult = 1.0f;
        if(hp != null) mult += hp.currentLevel * 0.10f;
        if(advHp != null) mult += advHp.currentLevel * 0.15f;
        return mult;
    }

    public static float getEvacuationRatio(){
        TechNode node = findNode("evac_efficiency");
        return 0.5f + (node != null ? node.currentLevel * 0.05f : 0f);
    }

    private static TechNode findNode(String id){
        for(TechNode node : techNodes){
            if(node.id.equals(id)) return node;
        }
        return null;
    }
}
