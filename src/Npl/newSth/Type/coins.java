package Npl.newSth.Type;

import arc.*;
import arc.graphics.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.type.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.ui.*;
import arc.graphics.g2d.*;
import arc.scene.utils.*;

import static mindustry.Vars.*;

/**
 * 自定义跨战役货币 coins。
 * <p>
 * 继承 Item 以便注册为 ContentType.item。
 * 货币不存入核心 ItemStorage。
 * <p>
 * 货币分为两类：
 * 1. 战役货币（campaignCoins）：在所有战役地图之间共享，通过 state.rules.tags 持久化到存档
 * 2. 自定义地图货币（customCoins）：在自定义地图（非战役）之间共享，通过独立存储
 * <p>
 * 屏幕左侧中间显示当前地图类型对应的 coins 数量。
 */
public class coins extends Item {

    /* ===================== 货币存储 ===================== */

    /** 战役模式下的 coins（跨地图共享，存入 Core.settings） */
    private static int campaignCoins = 0;
    /** 自定义地图模式下的 coins（跨地图共享，存入 Core.settings） */
    private static int customCoins = 0;
    /** UI 是否已初始化 */
    private static boolean uiInitialized = false;
    /** UI 表引用，用于更新 */
    private static Table uiTable = null;

    /* ===================== 构造 ===================== */

    public coins(String name, Color color){
        super(name, color);

        alwaysUnlocked = true;
        cost = 1.0f;
        hardness = 1;

        // 客户端加载：从 Core.settings 读取全局 coins + 初始化 UI
        Events.on(ClientLoadEvent.class, e -> {
            loadFromSettings();
            if(!uiInitialized){
                initUI();
                uiInitialized = true;
            }
        });

        // 状态变化：进入地图时刷新 UI，离开地图时隐藏 UI
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

    /* ===================== 模式判定 ===================== */

    /** 当前是否是战役地图（sector 模式） */
    private static boolean isCampaign(){
        try{
            return state != null && state.isCampaign();
        }catch(Exception e){
            return false;
        }
    }

    /* ===================== 货币管理（对外 API） ===================== */

    /** 获取当前模式下的 coins 数量 */
    public static int getAmount(){
        return isCampaign() ? campaignCoins : customCoins;
    }

    /** 获取当前战役/地图 coins 数量（= 全局对应模式数量） */
    public static int getSectorAmount(){
        return getAmount();
    }

    /** 增加当前模式下的 coins */
    public static void add(int amount){
        if(isCampaign()){
            campaignCoins += amount;
            if(campaignCoins < 0) campaignCoins = 0;
            saveCampaign();
        }else{
            customCoins += amount;
            if(customCoins < 0) customCoins = 0;
            saveCustom();
        }
        updateUI();
    }

    /** 扣除当前模式下的 coins */
    public static boolean spend(int amount){
        if(isCampaign()){
            if(campaignCoins >= amount){
                campaignCoins -= amount;
                saveCampaign();
                updateUI();
                return true;
            }
        }else{
            if(customCoins >= amount){
                customCoins -= amount;
                saveCustom();
                updateUI();
                return true;
            }
        }
        return false;
    }

    /** 直接设置当前模式下的 coins */
    public static void setAmount(int amount){
        int v = Math.max(0, amount);
        if(isCampaign()){
            campaignCoins = v;
            saveCampaign();
        }else{
            customCoins = v;
            saveCustom();
        }
        updateUI();
    }

    /** 增加当前战役/地图 coins（同 add） */
    public static void addSector(int amount){
        add(amount);
    }

    /** 扣除当前战役/地图 coins（同 spend） */
    public static boolean spendSector(int amount){
        return spend(amount);
    }

    /* ===================== 持久化（基于 Core.settings，全局跨存档） ===================== */

    private static void saveCampaign(){
        Core.settings.put("npl-coins-campaign", campaignCoins);
    }

    private static void saveCustom(){
        Core.settings.put("npl-coins-custom", customCoins);
    }

    private static void loadFromSettings(){
        campaignCoins = Core.settings.getInt("npl-coins-campaign", 0);
        customCoins = Core.settings.getInt("npl-coins-custom", 0);
        Log.info("Loaded coins [campaign=" + campaignCoins + ", custom=" + customCoins + "]");
    }

    /* ===================== UI ===================== */

    private static void initUI(){
        try{
            Time.runTask(3f, () -> addCoinsDisplay());
        }catch(Exception e){
            Log.err("Failed to init coins UI: " + e.getMessage());
        }
    }

    private static void addCoinsDisplay(){
        try{
            // 匿名 Table：draw 先画 90% 不透明灰黑背景，再画内容
            Table coinsTable = new Table(){
                @Override
                public void draw(){
                    Draw.color(0.08f, 0.08f, 0.1f, 0.9f);
                    Fill.rect(x + width/2f, y + height/2f, width, height);
                    Draw.color();
                    super.draw();
                }
            };
            coinsTable.name = "npl-coins-display";

            coinsTable.setPosition(70f, Core.graphics.getHeight() / 2f - 20f);

            // 找到 coins Item（它的 uiIcon 已经由 Item.java 的帧动画逻辑自动切换了）
            Item coinItem = null;
            if(mindustry.Vars.content != null){
                coinItem = mindustry.Vars.content.items().find(i -> "coins".equals(i.name));
            }
            final Item finalCoin = coinItem;

            coinsTable.table(t -> {
                t.defaults().pad(3f);

                // 匿名 Element：Draw.rect 直接读 uiIcon 当前 UV，天然跟随帧切换
                if(finalCoin != null){
                    Element icon = new Element(){
                        @Override
                        public void draw(){
                            Draw.color(1f, 1f, 1f, 1f);
                            Draw.rect(finalCoin.uiIcon, x + width/2f, y + height/2f, width, height);
                            Draw.color();
                        }
                    };
                    icon.setSize(28f, 28f);
                    t.add(icon).padRight(4f).size(28f);
                }else{
                    t.add("★").padRight(4f);
                }

                t.add("coins").padLeft(2f);

                Label valueLabel = new Label("0");
                valueLabel.update(() -> {
                    valueLabel.setText(String.valueOf(coins.getSectorAmount()));
                });
                t.add(valueLabel).padLeft(6f).width(50f);
            }).pad(6f);

            coinsTable.visible = false;

            Core.scene.add(coinsTable);

            coinsTable.update(() -> {
                coinsTable.setPosition(70f, Core.graphics.getHeight() / 2f - 20f);
            });

            uiTable = coinsTable;
            Log.info("Coins display added to UI.");
        }catch(Exception e){
            Log.err("Failed to add coins display: " + e.getMessage());
        }
    }

    private static void updateUI(){
        try{
            if(uiTable != null){
                uiTable.invalidateHierarchy();
            }
        }catch(Exception ignored){}
    }

    /* ===================== 核心显示 ===================== */

    public static String formatDisplay(){
        return getAmount() + " coins";
    }

    @Override
    public String toString(){
        return "Coins";
    }
}
