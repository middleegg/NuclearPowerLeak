package Npl.Rouge;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

/**
 * TemporalStorageCrystal (时序储晶)
 * ==============================================================
 * 模式独立货币系统
 * - Rouge 模式、战役模式、自定义模式各自独立
 * - 外场演绎的研究材料
 * - 局外商店的兑换货币
 */
public class TemporalStorageCrystal extends Item {

    private static int rougeCrystals = 0;
    private static int campaignCrystals = 0;
    private static int customCrystals = 0;
    public static TextureRegion uiIcon;
    private static boolean uiInitialized = false;
    private static Table uiTable = null;

    public TemporalStorageCrystal(String name, Color color){
        super(name, color);

        alwaysUnlocked = true;
        cost = 1.0f;
        hardness = 1;

        Events.on(ClientLoadEvent.class, e -> {
            loadFromSettings();
            if(!uiInitialized){
                initUI();
                uiInitialized = true;
            }
        });

        Events.on(StateChangeEvent.class, e -> {
            if(e.to == mindustry.core.GameState.State.playing){
                if(uiTable != null){
                    uiTable.visible = true;
                    updateUI();
                }
            }else{
                if(uiTable != null){
                    uiTable.visible = false;
                }
            }
        });
    }

    private static String getCurrentMode(){
        if(RougeSave.isInRougeMode) return "ROUGE";
        if(state.isCampaign()) return "CAMPAIGN";
        return "CUSTOM";
    }

    public static int getAmount(){
        return getAmount(getCurrentMode());
    }

    public static int getAmount(String mode){
        switch(mode){
            case "ROUGE": return rougeCrystals;
            case "CAMPAIGN": return campaignCrystals;
            default: return customCrystals;
        }
    }

    public static void add(int amount){
        add(amount, getCurrentMode());
    }

    public static void add(int amount, String mode){
        switch(mode){
            case "ROUGE":
                rougeCrystals += amount;
                if(rougeCrystals < 0) rougeCrystals = 0;
                break;
            case "CAMPAIGN":
                campaignCrystals += amount;
                if(campaignCrystals < 0) campaignCrystals = 0;
                break;
            default:
                customCrystals += amount;
                if(customCrystals < 0) customCrystals = 0;
                break;
        }
        save();
        updateUI();
    }

    public static boolean spend(int amount){
        return spend(amount, getCurrentMode());
    }

    public static boolean spend(int amount, String mode){
        if(getAmount(mode) >= amount){
            switch(mode){
                case "ROUGE": rougeCrystals -= amount; break;
                case "CAMPAIGN": campaignCrystals -= amount; break;
                default: customCrystals -= amount; break;
            }
            save();
            updateUI();
            return true;
        }
        return false;
    }

    public static void setAmount(int amount){
        setAmount(amount, getCurrentMode());
    }

    public static void setAmount(int amount, String mode){
        amount = Math.max(0, amount);
        switch(mode){
            case "ROUGE": rougeCrystals = amount; break;
            case "CAMPAIGN": campaignCrystals = amount; break;
            default: customCrystals = amount; break;
        }
        save();
        updateUI();
    }

    private static void save(){
        Core.settings.put("npl-rouge-crystals-rouge", rougeCrystals);
        Core.settings.put("npl-rouge-crystals-campaign", campaignCrystals);
        Core.settings.put("npl-rouge-crystals-custom", customCrystals);
    }

    private static void loadFromSettings(){
        rougeCrystals = Core.settings.getInt("npl-rouge-crystals-rouge", 0);
        campaignCrystals = Core.settings.getInt("npl-rouge-crystals-campaign", 0);
        customCrystals = Core.settings.getInt("npl-rouge-crystals-custom", 0);
        Log.info("Loaded TemporalStorageCrystal - ROUGE:" + rougeCrystals + " CAMPAIGN:" + campaignCrystals + " CUSTOM:" + customCrystals);
    }

    private static void initUI(){
        try{
            Time.runTask(3f, () -> addCrystalDisplay());
        }catch(Exception e){
            Log.err("Failed to init TemporalStorageCrystal UI: " + e.getMessage());
        }
    }

    private static void addCrystalDisplay(){
        try{
            Table crystalTable = new Table(){
                @Override
                public void draw(){
                    Draw.color(0.08f, 0.08f, 0.12f, 0.9f);
                    Fill.rect(x + width/2f, y + height/2f, width, height);
                    Draw.color();
                    super.draw();
                }
            };
            crystalTable.name = "npl-rouge-crystal-display";

            crystalTable.setPosition(70f, Core.graphics.getHeight() / 2f + 30f);

            crystalTable.table(t -> {
                t.defaults().pad(3f);

                Element icon = new Element(){
                    @Override
                    public void draw(){
                        if(uiIcon == null || uiIcon.texture == null) return;
                        Draw.color(1f, 1f, 1f, 1f);
                        Draw.rect(uiIcon, x + width/2f, y + height/2f, width, height);
                        Draw.color();
                    }
                };
                icon.setSize(28f, 28f);
                t.add(icon).padRight(4f).size(28f);

                t.add("时序储晶").padLeft(2f);

                Label valueLabel = new Label("0");
                valueLabel.update(() -> {
                    String mode = getCurrentMode();
                    valueLabel.setText("[" + mode + "] " + getAmount(mode));
                });
                t.add(valueLabel).padLeft(6f).width(80f);
            }).pad(6f);

            crystalTable.visible = false;

            Core.scene.add(crystalTable);

            crystalTable.update(() -> {
                crystalTable.setPosition(70f, Core.graphics.getHeight() / 2f + 30f);
            });

            uiTable = crystalTable;
            Log.info("TemporalStorageCrystal display added to UI.");
        }catch(Exception e){
            Log.err("Failed to add TemporalStorageCrystal display: " + e.getMessage());
        }
    }

    private static void updateUI(){
        try{
            if(uiTable != null){
                uiTable.invalidateHierarchy();
            }
        }catch(RuntimeException ignored){
            // UI 可能已销毁，忽略
        }
    }

    public static String formatDisplay(){
        return getAmount() + " 时序储晶";
    }

    @Override
    public String toString(){
        return "TemporalStorageCrystal";
    }
}
