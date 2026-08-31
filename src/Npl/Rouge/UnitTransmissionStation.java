package Npl.Rouge;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.*;
import mindustry.world.meta.*;

import arc.input.*;

import Npl.content.NuColor;

import static mindustry.Vars.*;

public class UnitTransmissionStation extends Block {

    public float captureRange = 56f;
    public float animDuration = 30f;
    public float deployRange = 80f;
    private static final float ABSORB_INTERVAL = 300f;

    public UnitTransmissionStation(String name){
        super(name);
        this.size = 3;
        this.health = 2000;
        this.solid = true;
        this.update = true;
        this.alwaysUnlocked = true;
        this.configurable = false;
        this.category = Category.units;
        this.sync = true;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.itemCapacity, RougeTechTree.getUnitStorageLimit());
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        float wx = x * tilesize + size * tilesize / 2f;
        float wy = y * tilesize + size * tilesize / 2f;

        Draw.z(Layer.plans);
        Draw.color(valid ? Pal.accent : Pal.redderDust, 0.3f);
        Fill.circle(wx, wy, captureRange);

        Draw.color(valid ? Pal.accent : Pal.redderDust, 0.8f);
        Lines.stroke(1.5f);
        Lines.circle(wx, wy, captureRange);

        Draw.reset();
    }

    public class UnitTransmissionStationBuild extends Building {

        private String selectedReleaseType = null;
        private int releaseQuantity = 0;
        private float deployCooldown = 0f;
        private static final float COOLDOWN_TIME = 600f;

        private float inputAnimTimer = 0f;
        private float inputAnimFromX = 0f;
        private float inputAnimFromY = 0f;
        private UnitType inputAnimUnitType = null;

        private float outputAnimTimer = 0f;
        private float outputAnimToX = 0f;
        private float outputAnimToY = 0f;
        private UnitType outputAnimUnitType = null;
        private int outputAnimCount = 0;

        private boolean commandModeActive = false;
        private float commandModeTimer = 0f;
        private float cursorWorldX = 0f;
        private float cursorWorldY = 0f;
        private boolean countdownStarted = false;

        private float pendingDeployX = 0f;
        private float pendingDeployY = 0f;
        private float pendingDeployTimer = 0f;

        private float releaseEffectTimer = 0f;
        private float releaseEffectX = 0f;
        private float releaseEffectY = 0f;

        private float absorbTimer = 0f;

        private String currentMode(){
            if(RougeSave.isInRougeMode) return "ROUGE";
            if(state.isCampaign()) return "CAMPAIGN";
            return "CUSTOM";
        }

        private String storageKey(String unitTypeName){
            return currentMode() + "_unit_" + unitTypeName;
        }

        private int getStoredCount(String unitTypeName){
            Object val = RougeSave.shortTermMemory.get(storageKey(unitTypeName), 0);
            if(val instanceof String){
                try{
                    return Integer.parseInt((String)val);
                }catch(Exception e){
                    return 0;
                }
            }
            return (int)val;
        }

        private void setStoredCount(String unitTypeName, int count){
            if(count <= 0){
                RougeSave.shortTermMemory.remove(storageKey(unitTypeName));
            }else{
                RougeSave.shortTermMemory.put(storageKey(unitTypeName), String.valueOf(count));
            }
        }

        private int getTotalStoredCount(){
            int total = 0;
            String prefix = currentMode() + "_unit_";
            for(var entry : RougeSave.shortTermMemory.entries()){
                if(entry.key.startsWith(prefix)){
                    try{
                        total += Integer.parseInt(entry.value.toString());
                    }catch(NumberFormatException ignored){
                        // 值不是有效数字，跳过该条目
                    }
                }
            }
            return total;
        }

        private int getCapacity(){
            return RougeTechTree.getUnitStorageLimit();
        }

        private Seq<String> getStoredUnitTypes(){
            Seq<String> types = new Seq<>();
            String prefix = currentMode() + "_unit_";
            for(var entry : RougeSave.shortTermMemory.entries()){
                if(entry.key.startsWith(prefix)){
                    int val = 0;
                    if(entry.value instanceof String){
                        try{
                            val = Integer.parseInt((String)entry.value);
                        }catch(Exception e){
                            continue;
                        }
                    }else{
                        val = (int)entry.value;
                    }
                    if(val > 0){
                        types.add(entry.key.substring(prefix.length()));
                    }
                }
            }
            return types;
        }

        @Override
        public void updateTile(){
            super.updateTile();

            if(deployCooldown > 0){
                deployCooldown -= Time.delta;
                if(deployCooldown < 0) deployCooldown = 0;
            }

            absorbTimer += Time.delta;
            if(absorbTimer >= ABSORB_INTERVAL){
                absorbTimer = 0f;
                absorbOneUnitInRange();
            }

            if(inputAnimTimer > 0) inputAnimTimer -= Time.delta;
            if(outputAnimTimer > 0) outputAnimTimer -= Time.delta;
            if(releaseEffectTimer > 0) releaseEffectTimer -= Time.delta;

            if(commandModeActive){
                commandModeTimer += Time.delta;
                updateCommandMode();
            }

            if(pendingDeployTimer > 0){
                pendingDeployTimer -= Time.delta;
                if(pendingDeployTimer <= 0){
                    pendingDeployTimer = 0f;
                    tryDeployAt(pendingDeployX, pendingDeployY);
                }
            }
        }

        private void absorbOneUnitInRange(){
            if(getTotalStoredCount() >= getCapacity()) return;

            for(Unit u : Groups.unit){
                if(u == null || u.team != team || u.isPlayer()) continue;
                if(u.dst(x, y) > captureRange) continue;

                String typeName = u.type.name;
                int currentCount = getStoredCount(typeName);
                if(currentCount < getCapacity()){
                    setStoredCount(typeName, currentCount + 1);
                    triggerInputAnim(u.x, u.y, u.type);
                    u.remove();
                    return;
                }
            }
        }

        private void triggerInputAnim(float fromX, float fromY, UnitType type){
            inputAnimTimer = animDuration;
            inputAnimFromX = fromX;
            inputAnimFromY = fromY;
            inputAnimUnitType = type;
        }

        private void triggerOutputAnim(float toX, float toY, UnitType type, int count){
            outputAnimTimer = animDuration;
            outputAnimToX = toX;
            outputAnimToY = toY;
            outputAnimUnitType = type;
            outputAnimCount = count;
            releaseEffectTimer = 72f;
            releaseEffectX = toX;
            releaseEffectY = toY;
        }

        private void updateCommandMode(){
            cursorWorldX = Core.input.mouseWorldX();
            cursorWorldY = Core.input.mouseWorldY();

            if(Core.input.keyTap(KeyCode.mouseLeft)){
                pendingDeployX = cursorWorldX;
                pendingDeployY = cursorWorldY;
                pendingDeployTimer = COOLDOWN_TIME;
                countdownStarted = true;
                WarningLineManager.show("已选择释放地点，倒计时10秒...");
                exitCommandMode();
            }

            if(Core.input.keyTap(KeyCode.escape)){
                exitCommandMode();
            }
        }

        private void tryDeployAt(float atX, float atY){
            if(selectedReleaseType == null){
                WarningLineManager.show("请先选择要释放的单位类型");
                exitCommandMode();
                return;
            }

            int stored = getStoredCount(selectedReleaseType);
            int count = Math.min(releaseQuantity, stored);
            if(count <= 0){
                WarningLineManager.show("没有可释放的单位");
                exitCommandMode();
                return;
            }

            if(deployCooldown > 0){
                WarningLineManager.show("冷却中: " + String.format("%.1f", deployCooldown / 60f) + "s");
                exitCommandMode();
                return;
            }

            UnitType type = Vars.content.unit(selectedReleaseType);
            if(type == null){
                WarningLineManager.show("单位类型无效");
                exitCommandMode();
                return;
            }

            for(int i = 0; i < count; i++){
                float angle = Mathf.random(360f) * Mathf.degRad;
                float dist = Mathf.sqrt(Mathf.random()) * deployRange;
                float spawnX = atX + Mathf.cos(angle) * dist;
                float spawnY = atY + Mathf.sin(angle) * dist;
                Unit u = type.create(team);
                u.set(spawnX, spawnY);
                u.add();
            }

            setStoredCount(selectedReleaseType, stored - count);
            deployCooldown = COOLDOWN_TIME;
            triggerOutputAnim(atX, atY, type, count);
            WarningLineManager.show("释放 " + type.localizedName + " x" + count + "（剩余 x" + (stored - count) + "）");
            RougeSave.save();
            selectedReleaseType = null;
            releaseQuantity = 0;
            exitCommandMode();
        }

        private void enterCommandMode(){
            if(selectedReleaseType == null){
                WarningLineManager.show("请先选择要释放的单位类型");
                return;
            }
            if(releaseQuantity <= 0){
                WarningLineManager.show("请先设置释放数量");
                return;
            }
            if(getStoredCount(selectedReleaseType) <= 0){
                WarningLineManager.show("没有可释放的单位");
                return;
            }
            commandModeActive = true;
            commandModeTimer = 0f;
            countdownStarted = false;
            WarningLineManager.show("选择释放地点 - 左键点击地图选择 | ESC取消");
        }

        private void exitCommandMode(){
            if(commandModeActive){
                commandModeActive = false;
                countdownStarted = false;
            }
        }

        @Override
        public void tapped(){
            showStationDialog();
        }

        private void showStationDialog(){
            BaseDialog dialog = new BaseDialog("单位传输站");
            float contentWidth = Math.min(Core.graphics.getWidth() * 0.55f, 640f);

            dialog.keyDown(key -> {
                if(key == KeyCode.escape) dialog.hide();
            });

            Table main = new Table();
            Runnable[] refresh = new Runnable[1];

            // 就地刷新：只重建内容，不再关闭/重开对话框，消除每次点击"跳到新界面"的闪烁感
            refresh[0] = () -> {
                main.clearChildren();

                // ===== 标题栏 =====
                main.table(title -> {
                    title.setBackground(Styles.black5);
                    title.image().color(Pal.accent).width(4).height(36).padRight(10);
                    title.add("[accent]单位传输站[]").fontScale(1.25f).color(Pal.accent).pad(8).left();
                    title.add().growX();
                    title.add("[gray]" + currentMode() + "[]").fontScale(0.75f).color(Color.gray).padRight(10);
                    title.button(Icon.cancel, () -> dialog.hide()).size(40).pad(6);
                }).fillX().height(46).row();

                // ===== 储存状态（可滚动列表，点击选中） =====
                main.table(Styles.black5, header -> {
                    header.image().color(Pal.accent).width(3).height(28).padRight(8);
                    header.add("[accent]储存状态[]  [gray]点击选择一种单位[]").fontScale(0.9f).left();
                }).fillX().left().padLeft(10).row();

                Seq<String> storedTypes = getStoredUnitTypes();
                main.pane(Styles.noBarPane, scroll -> {
                    scroll.table(list -> {
                        list.defaults().pad(3);
                        if(storedTypes.isEmpty()){
                            list.add("[gray]暂无储存单位 — 单位进入吸收范围后自动捕获[]")
                                .color(Color.gray).pad(20);
                        }
                        for(String typeName : storedTypes){
                            UnitType type = Vars.content.unit(typeName);
                            if(type == null) continue;
                            int count = getStoredCount(typeName);
                            boolean selected = typeName.equals(selectedReleaseType);

                            list.table(Tex.whiteui, row -> {
                                row.setColor(selected ? Color.valueOf("2A3A2A") : Color.valueOf("1E1E1E"));
                                row.left().defaults().pad(6);
                                row.image().color(selected ? Pal.accent : Color.valueOf("444444"))
                                    .width(3).height(34).padRight(4);
                                row.image(type.uiIcon).size(36).padLeft(4);
                                row.table(info -> {
                                    info.add(type.localizedName).color(selected ? Pal.accent : Color.white).fontScale(0.9f).left().row();
                                    info.add("可用 x" + count).color(count > 0 ? Color.green : Color.gray).fontScale(0.75f).left();
                                }).growX().padLeft(8).padRight(8);
                                if(selected){
                                    row.image(Icon.right).color(Pal.accent).size(18).padRight(10);
                                }

                                row.clicked(() -> {
                                    selectedReleaseType = typeName;
                                    releaseQuantity = count;
                                    refresh[0].run();
                                });
                            }).fillX().height(54).padBottom(3).row();
                        }
                    }).growX().pad(6);
                }).growX().height(220).row();

                // ===== 总容量 =====
                main.table(Styles.black3, cap -> {
                    cap.left().defaults().pad(8);
                    int total = getTotalStoredCount();
                    float cr = getCapacity() > 0 ? Mathf.clamp((float)total / getCapacity()) : 0f;
                    cap.add("总容量").color(Color.lightGray).fontScale(0.8f).padLeft(6);
                    cap.add(new Bar(
                        () -> "",
                        () -> cr >= 1f ? Color.red : Pal.accent,
                        () -> cr
                    )).width(180).height(14).padLeft(8).padRight(8);
                    cap.add(total + " / " + getCapacity())
                        .color(cr >= 1f ? Color.red : Pal.accent).fontScale(0.8f);
                    cap.add().growX();
                }).fillX().pad(6).row();

                // ===== 数量选择器（仅选中单位后显示） =====
                if(selectedReleaseType != null){
                    UnitType type = Vars.content.unit(selectedReleaseType);
                    int maxCount = getStoredCount(selectedReleaseType);
                    if(type != null && maxCount > 0){
                        main.table(Styles.black5, header -> {
                            header.image().color(Pal.accent).width(3).height(28).padRight(8);
                            header.add("[accent]释放数量[]").fontScale(0.9f).color(Pal.accent).left();
                        }).fillX().left().padLeft(10).row();

                        main.table(Tex.whiteui, qty -> {
                            qty.setColor(Color.valueOf("1E1E1E"));
                            qty.left().defaults().pad(6);
                            qty.image(type.uiIcon).size(32).padLeft(8);
                            qty.table(info -> {
                                info.left();
                                info.add(type.localizedName).color(Pal.accent).fontScale(0.9f).left().row();
                                info.add("可用 x" + maxCount).color(Color.gray).fontScale(0.75f).left();
                            }).growX().padLeft(8);

                            qty.button(Icon.left, () -> {
                                releaseQuantity = Math.max(1, releaseQuantity - 1);
                                refresh[0].run();
                            }).size(36);

                            qty.add(String.valueOf(releaseQuantity)).color(Color.yellow).fontScale(1.15f)
                                .width(42).center().pad(4);

                            qty.button(Icon.right, () -> {
                                releaseQuantity = Math.min(maxCount, releaseQuantity + 1);
                                refresh[0].run();
                            }).size(36);

                            qty.button("[accent]最大[]", Styles.defaultt, () -> {
                                releaseQuantity = maxCount;
                                refresh[0].run();
                            }).size(64, 36).padLeft(8).padRight(8);
                        }).fillX().pad(6).row();
                    }
                }

                // ===== 释放按钮 =====
                boolean canDeploy = selectedReleaseType != null && releaseQuantity > 0
                    && getStoredCount(selectedReleaseType) >= releaseQuantity && deployCooldown <= 0;
                String btnText = deployCooldown > 0 ?
                    "[gray]冷却中 (" + String.format("%.0f", deployCooldown / 60f) + "s)[]" :
                    (selectedReleaseType == null ? "[gray]请先选择一种单位[]" : "[accent]释放 " + releaseQuantity + " 个[]");

                main.button(btnText, Styles.defaultt, () -> {
                    dialog.hide();
                    enterCommandMode();
                }).growX().height(46).pad(10).disabled(b -> !canDeploy).row();

            };

            refresh[0].run();

            dialog.cont.add(main).width(contentWidth);
            dialog.addCloseButton();
            dialog.show();
        }

        @Override
        public void draw(){
            super.draw();

            Draw.z(Layer.effect);
            Draw.color(Pal.accent);
            Lines.stroke(2f);
            Lines.square(x, y, size * tilesize / 2f + 5f);
            Draw.reset();

            drawCaptureRange();
            drawStorageStatus();
            drawAnimations();
            drawReleaseEffect();

            if(commandModeActive){
                drawCommandMode();
            }

            if(pendingDeployTimer > 0){
                drawPendingDeploy();
            }
        }

        private void drawPendingDeploy(){
            float timeLeft = pendingDeployTimer;
            float arcProgress = Mathf.clamp(timeLeft / COOLDOWN_TIME, 0f, 1f);
            float centerX = pendingDeployX;
            float centerY = pendingDeployY;
            float circleRadius = deployRange;

            Draw.z(65f);

            Draw.color(Pal.accent, 0.25f + Mathf.absin(Time.time, 3f, 0.1f));
            Lines.stroke(2f);
            Lines.circle(centerX, centerY, circleRadius);

            if(arcProgress > 0){
                // 圆环倒计时：整圈高亮圆环随剩余时间收缩
                Draw.color(Pal.accent, 0.85f + Mathf.absin(Time.time, 6f, 0.15f));
                Lines.stroke(5f);
                Lines.arc(centerX, centerY, circleRadius, arcProgress, -90f);
            }

            Draw.color(Pal.accent, 0.5f);
            Lines.stroke(1.5f);
            for(int i = 0; i < 4; i++){
                float angle = i * 90f * Mathf.degRad;
                float ix = centerX + Mathf.cos(angle) * circleRadius * 0.7f;
                float iy = centerY + Mathf.sin(angle) * circleRadius * 0.7f;
                float ox = centerX + Mathf.cos(angle) * circleRadius;
                float oy = centerY + Mathf.sin(angle) * circleRadius;
                Lines.line(ix, iy, ox, oy);
            }

            Draw.color(Pal.accent);
            Lines.stroke(2f);
            Lines.line(centerX - 40, centerY, centerX + 40, centerY);
            Lines.line(centerX, centerY - 40, centerX, centerY + 40);

            if(selectedReleaseType != null){
                int count = getStoredCount(selectedReleaseType);
                UnitType type = Vars.content.unit(selectedReleaseType);
                if(type != null && count > 0){
                    Draw.z(Layer.playerName);
                    Font font = Fonts.outline != null ? Fonts.outline : Fonts.def;
                    font.setColor(Pal.accent);
                    font.getData().setScale(1.1f);
                    font.draw(type.localizedName + " x" + count, centerX, centerY + 60f, Align.center);
                    font.getData().setScale(1f);

                    font.setColor(timeLeft < 120f ? Color.red : Color.yellow);
                    font.draw(String.format("%.1fs", timeLeft / 60f), centerX, centerY + 40f, Align.center);

                    font.setColor(Color.white);
                    font.getData().setScale(0.9f);
                    font.draw("释放中...", centerX, centerY - 50f, Align.center);
                    font.getData().setScale(1f);
                }
            }

            Draw.reset();
        }

        private void drawCaptureRange(){
            Draw.z(Layer.effect);
            float pulse = Mathf.absin(Time.time, 2f, 0.12f);

            Draw.color(NuColor.CoreColor, 0.28f + pulse);
            Fill.circle(x, y, captureRange);

            Draw.color(NuColor.CoreColor, 0.7f);
            Lines.stroke(1.5f);
            Lines.circle(x, y, captureRange);

            Draw.color(NuColor.CoreColor, 0.5f + pulse);
            Lines.stroke(1.2f);
            for(int i = 0; i < 8; i++){
                float angle = i * 45f * Mathf.degRad + Time.time * 0.3f;
                float ix = x + Mathf.cos(angle) * captureRange * 0.82f;
                float iy = y + Mathf.sin(angle) * captureRange * 0.82f;
                float ox = x + Mathf.cos(angle) * captureRange;
                float oy = y + Mathf.sin(angle) * captureRange;
                Lines.line(ix, iy, ox, oy);
            }
            Draw.reset();
        }

        private void drawStorageStatus(){
            Seq<String> storedTypes = getStoredUnitTypes();
            if(storedTypes.isEmpty()) return;

            float screenW = Core.graphics.getWidth();
            float posX = screenW * 0.8f;
            float posY = Core.graphics.getHeight() * 0.85f;

            Draw.z(65f);
            Draw.color(0, 0, 0, 0.6f);
            float boxW = 130f;
            float boxH = 55f + storedTypes.size * 25f;
            Fill.rect(posX, posY, boxW, boxH);

            Draw.color(Pal.accent);
            Lines.stroke(1.5f);
            Lines.rect(posX - boxW/2, posY - boxH/2, boxW, boxH);

            Draw.z(66f);
            Font font = Fonts.outline != null ? Fonts.outline : Fonts.def;
            font.setColor(Pal.accent);
            font.getData().setScale(0.9f);
            font.draw("[" + currentMode() + "] 储存单位", posX, posY + boxH/2 - 15, Align.center);
            font.getData().setScale(0.8f);

            int yOffset = 0;
            for(String typeName : storedTypes){
                UnitType type = Vars.content.unit(typeName);
                if(type == null) continue;
                int count = getStoredCount(typeName);
                float yPos = posY + boxH/2 - 40 - yOffset;

                font.setColor(typeName.equals(selectedReleaseType) ? Pal.accent : Color.white);
                font.draw(type.localizedName + " x" + count, posX, yPos, Align.center);
                yOffset += 22;
            }

            font.getData().setScale(1f);
            font.setColor(Color.white);
            Draw.reset();
        }

        private void drawAnimations(){
            if(inputAnimTimer > 0 && inputAnimUnitType != null){
                float progress = 1f - (inputAnimTimer / animDuration);
                float ease = Interp.smooth.apply(progress);

                float drawX = Mathf.lerp(inputAnimFromX, x, ease);
                float drawY = Mathf.lerp(inputAnimFromY, y, ease);

                float alpha = progress < 0.7f ? 1f : 1f - (progress - 0.7f) / 0.3f;

                Draw.z(Layer.effect);

                // 牵引能量束：3 条向传输站收拢的弧线
                float inDx = x - inputAnimFromX;
                float inDy = y - inputAnimFromY;
                float inLen = Mathf.len(inDx, inDy);
                if(inLen > 0.01f){
                    float nx = -inDy / inLen;
                    float ny = inDx / inLen;
                    Draw.color(NuColor.CoreColor, alpha * 0.35f);
                    Lines.stroke(1.4f);
                    for(int line = -1; line <= 1; line++){
                        float bow = line * 14f * (1f - ease);
                        float px = 0f, py = 0f;
                        for(int s = 0; s <= 8; s++){
                            float tp = s / 8f;
                            float te = Interp.smooth.apply(tp);
                            float sx = Mathf.lerp(inputAnimFromX, drawX, te) + nx * bow * Mathf.sin(tp * Mathf.PI);
                            float sy = Mathf.lerp(inputAnimFromY, drawY, te) + ny * bow * Mathf.sin(tp * Mathf.PI);
                            if(s > 0){
                                Lines.line(px, py, sx, sy);
                            }
                            px = sx;
                            py = sy;
                        }
                    }
                }

                // 彗星拖尾（青 → 强调色渐变）
                for(int i = 0; i < 16; i++){
                    float trailProgress = Mathf.clamp(progress - 0.04f * i, 0f, 1f);
                    float trailEase = Interp.smooth.apply(trailProgress);
                    float tx = Mathf.lerp(inputAnimFromX, x, trailEase);
                    float ty = Mathf.lerp(inputAnimFromY, y, trailEase);
                    float trailSize = Mathf.lerp(10f, 2f, trailEase) * (1f - i * 0.05f);

                    Draw.color(NuColor.CoreElseColor, NuColor.CoreColor, trailEase);
                    Draw.alpha(alpha * (1f - i * 0.06f) * 0.9f);
                    Fill.circle(tx, ty, trailSize);
                }

                for(int i = 0; i < 6; i++){
                    float orbitAngle = i * 60f * Mathf.degRad + progress * 360f * Mathf.degRad;
                    float orbitRadius = 15f * (1f - ease);
                    float ox = drawX + Mathf.cos(orbitAngle) * orbitRadius;
                    float oy = drawY + Mathf.sin(orbitAngle) * orbitRadius;
                    Draw.color(Color.white, alpha * 0.7f);
                    Fill.circle(ox, oy, 2f * (1f - ease));
                }

                float iconSize = Mathf.lerp(0.8f, 0.15f, ease);
                Draw.color(NuColor.CoreColor, alpha);
                Draw.rect(inputAnimUnitType.uiIcon, drawX, drawY,
                    inputAnimUnitType.region.width * iconSize / 4f,
                    inputAnimUnitType.region.height * iconSize / 4f);

                Draw.color(Color.white, alpha * (1f - ease));
                Fill.circle(drawX, drawY, 4f * (1f - ease));

                // 捕获收束：到达时白色收缩环 + 强调色外环
                if(progress > 0.78f){
                    float cap = (progress - 0.78f) / 0.22f;
                    float capAlpha = 1f - cap;
                    Draw.color(Color.white, capAlpha * 0.9f);
                    Lines.stroke(2.5f * capAlpha + 0.5f);
                    Lines.circle(x, y, Mathf.lerp(26f, 6f, cap));
                    Draw.color(NuColor.CoreColor, capAlpha * 0.7f);
                    Lines.stroke(1.5f * capAlpha);
                    Lines.circle(x, y, Mathf.lerp(36f, 12f, cap));
                }

                Draw.reset();
            }

            if(outputAnimTimer > 0 && outputAnimUnitType != null){
                float progress = 1f - (outputAnimTimer / animDuration);
                float ease = Interp.pow5Out.apply(progress);

                float drawX = Mathf.lerp(x, outputAnimToX, ease);
                float drawY = Mathf.lerp(y, outputAnimToY, ease);

                float alpha = progress > 0.2f ? 1f : progress / 0.2f;

                Draw.z(Layer.effect);

                float outDx = outputAnimToX - x;
                float outDy = outputAnimToY - y;
                float outLen = Mathf.len(outDx, outDy);
                float outAng = Mathf.angle(outDx, outDy);

                // 速度拖尾：沿运动方向的能量流线，越快越长
                if(outLen > 0.01f){
                    for(int i = 0; i < 8; i++){
                        float trailProgress = Mathf.clamp(progress - 0.05f * (i + 1), 0f, 1f);
                        float trailEase = Interp.pow5Out.apply(trailProgress);
                        float tx = Mathf.lerp(x, outputAnimToX, trailEase);
                        float ty = Mathf.lerp(y, outputAnimToY, trailEase);
                        float speed = 1f - trailProgress;
                        float streak = Mathf.lerp(26f, 6f, speed) * (1f - i * 0.1f);
                        float thick = Mathf.lerp(3f, 9f, trailEase) * (1f - i * 0.1f);
                        Draw.color(Color.orange, alpha * (1f - i * 0.1f) * 0.7f);
                        Lines.stroke(thick);
                        float halfS = streak * 0.5f;
                        float sdx = Mathf.cos(outAng * Mathf.degRad) * halfS;
                        float sdy = Mathf.sin(outAng * Mathf.degRad) * halfS;
                        Lines.line(tx - sdx, ty - sdy, tx + sdx, ty + sdy);
                    }
                }

                // 出膛闪光：起点亮环 + 放射粒子
                if(progress < 0.5f){
                    float mf = 1f - progress / 0.5f;
                    float mr = Mathf.lerp(4f, 30f, Interp.pow2Out.apply(progress * 2f));
                    Draw.color(Color.yellow, alpha * mf * 0.9f);
                    Lines.stroke(3f * mf);
                    Lines.circle(x, y, mr);
                    Draw.color(Color.orange, alpha * mf * 0.7f);
                    for(int i = 0; i < 5; i++){
                        float pa = i * 72f * Mathf.degRad;
                        Fill.circle(x + Mathf.cos(pa) * mr, y + Mathf.sin(pa) * mr, 2.5f * mf);
                    }
                }

                // 图标：带 overshoot 回弹的放大
                float bt = progress;
                float bs = 1.70158f;
                float backOut = 1f + (bs + 1f) * (bt - 1f) * (bt - 1f) * (bt - 1f) + bs * (bt - 1f) * (bt - 1f);
                float sizeInterp = Mathf.lerp(0.5f, 1.2f, backOut);
                Draw.color(Color.yellow, alpha);
                Draw.rect(outputAnimUnitType.uiIcon, drawX, drawY,
                    outputAnimUnitType.region.width * sizeInterp / 4f,
                    outputAnimUnitType.region.height * sizeInterp / 4f);

                // 落点标记：到达时白色收缩环
                if(progress > 0.82f){
                    float lp = (progress - 0.82f) / 0.18f;
                    float la = 1f - lp;
                    Draw.color(Color.white, la * 0.9f);
                    Lines.stroke(2.5f * la + 0.5f);
                    Lines.circle(outputAnimToX, outputAnimToY, Mathf.lerp(24f, 8f, lp));
                }

                Draw.reset();
            }
        }

        private void drawReleaseEffect(){
            if(releaseEffectTimer <= 0) return;

            float totalDuration = 72f;
            float progress = 1f - (releaseEffectTimer / totalDuration);
            float alpha = 1f - progress;

            Draw.z(Layer.effect);

            float ringRadius = Mathf.lerp(10f, deployRange + 30f, Interp.pow2Out.apply(progress));
            Draw.color(Pal.accent, alpha * 0.8f);
            Lines.stroke(3f * alpha);
            Lines.circle(releaseEffectX, releaseEffectY, ringRadius);

            Draw.color(Color.white, alpha * 0.5f);
            Lines.stroke(1.5f * alpha);
            Lines.circle(releaseEffectX, releaseEffectY, ringRadius * 0.8f);

            for(int i = 0; i < 8; i++){
                float angle = i * 45f * Mathf.degRad + progress * 60f * Mathf.degRad;
                float dist = ringRadius * 0.9f;
                float px = releaseEffectX + Mathf.cos(angle) * dist;
                float py = releaseEffectY + Mathf.sin(angle) * dist;
                Draw.color(Pal.accent, alpha);
                Fill.circle(px, py, 3f * alpha);
            }

            Draw.color(Pal.accent, alpha * 0.3f);
            Lines.stroke(1f);
            for(int i = 0; i < 12; i++){
                float angle = i * 30f * Mathf.degRad + progress * 20f * Mathf.degRad;
                float innerDist = ringRadius * 0.6f;
                float outerDist = ringRadius;
                float ix = releaseEffectX + Mathf.cos(angle) * innerDist;
                float iy = releaseEffectY + Mathf.sin(angle) * innerDist;
                float ox = releaseEffectX + Mathf.cos(angle) * outerDist;
                float oy = releaseEffectY + Mathf.sin(angle) * outerDist;
                Lines.line(ix, iy, ox, oy);
            }

            Draw.reset();
        }

        private void drawCommandMode(){
            Draw.z(64f);

            float camX = Core.camera.position.x;
            float camY = Core.camera.position.y;

            Draw.color(0, 0, 0, 0.3f);
            Fill.rect(camX, camY, Core.camera.width, Core.camera.height);

            float circleRadius = deployRange;
            float centerX = cursorWorldX;
            float centerY = cursorWorldY;

            Draw.z(65f);

            if(!countdownStarted){
                Draw.color(Pal.accent, 0.15f + Mathf.absin(Time.time, 3f, 0.05f));
                Lines.stroke(1.5f);
                Lines.circle(centerX, centerY, circleRadius);

                Draw.color(Pal.accent, 0.4f + Mathf.absin(Time.time, 4f, 0.2f));
                Lines.stroke(1.5f);
                for(int i = 0; i < 4; i++){
                    float angle = i * 90f * Mathf.degRad + Time.time * 0.5f;
                    float ix = centerX + Mathf.cos(angle) * circleRadius * 0.7f;
                    float iy = centerY + Mathf.sin(angle) * circleRadius * 0.7f;
                    float ox = centerX + Mathf.cos(angle) * circleRadius;
                    float oy = centerY + Mathf.sin(angle) * circleRadius;
                    Lines.line(ix, iy, ox, oy);
                }

                Draw.color(Pal.accent, 0.6f);
                Lines.stroke(2f);
                Lines.circle(centerX, centerY, 10f);
                Lines.line(centerX - 20, centerY, centerX + 20, centerY);
                Lines.line(centerX, centerY - 20, centerX, centerY + 20);

            }else{
                float timeLeft = COOLDOWN_TIME - commandModeTimer;
                float arcProgress = Mathf.clamp(timeLeft / COOLDOWN_TIME, 0f, 1f);

                Draw.color(Pal.accent, 0.25f + Mathf.absin(Time.time, 3f, 0.1f));
                Lines.stroke(2f);
                Lines.circle(centerX, centerY, circleRadius);

                if(arcProgress > 0){
                    // 圆环倒计时：整圈高亮圆环随剩余时间收缩
                    Draw.color(Pal.accent, 0.85f + Mathf.absin(Time.time, 6f, 0.15f));
                    Lines.stroke(5f);
                    Lines.arc(centerX, centerY, circleRadius, arcProgress, -90f);
                }

                Draw.color(Pal.accent, 0.5f);
                Lines.stroke(1.5f);
                for(int i = 0; i < 4; i++){
                    float angle = i * 90f * Mathf.degRad;
                    float ix = centerX + Mathf.cos(angle) * circleRadius * 0.7f;
                    float iy = centerY + Mathf.sin(angle) * circleRadius * 0.7f;
                    float ox = centerX + Mathf.cos(angle) * circleRadius;
                    float oy = centerY + Mathf.sin(angle) * circleRadius;
                    Lines.line(ix, iy, ox, oy);
                }

                Draw.color(Pal.accent);
                Lines.stroke(2f);
                Lines.line(centerX - 40, centerY, centerX + 40, centerY);
                Lines.line(centerX, centerY - 40, centerX, centerY + 40);
            }

            if(selectedReleaseType != null){
                int count = getStoredCount(selectedReleaseType);
                UnitType type = Vars.content.unit(selectedReleaseType);
                if(type != null && count > 0){
                    Draw.z(Layer.playerName);
                    Font font = Fonts.outline != null ? Fonts.outline : Fonts.def;
                    font.setColor(Pal.accent);
                    font.getData().setScale(1.1f);
                    font.draw(type.localizedName + " x" + count, centerX, centerY + 60f, Align.center);
                    font.getData().setScale(1f);

                    if(!countdownStarted){
                        font.setColor(Color.yellow);
                        font.draw("左键选择地点开始倒计时", centerX, centerY + 40f, Align.center);
                    }else{
                        float timeLeft = COOLDOWN_TIME - commandModeTimer;
                        font.setColor(timeLeft < 120f ? Color.red : Color.yellow);
                        font.draw(String.format("%.1fs", timeLeft / 60f), centerX, centerY + 40f, Align.center);
                    }

                    font.setColor(Color.white);
                    font.draw("ESC退出", centerX, centerY - 50f, Align.center);
                }
            }

            Draw.reset();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.bool(commandModeActive);
            write.str(selectedReleaseType != null ? selectedReleaseType : "");
            write.f(deployCooldown);
            write.i(releaseQuantity);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            commandModeActive = read.bool();
            String savedType = read.str();
            selectedReleaseType = savedType.isEmpty() ? null : savedType;
            deployCooldown = read.f();
            releaseQuantity = read.i();
        }
    }
}
