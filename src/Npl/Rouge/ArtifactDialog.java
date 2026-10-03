package Npl.Rouge;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Scaling;

import mindustry.content.*;
import mindustry.core.GameState;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.type.*;

import Npl.Rouge.Artifact.*;

import static mindustry.Vars.*;

/**
 * ArtifactDialog —— 藏品相关对话框
 * ==============================================================
 * 包含：藏品获取、商店、藏品图鉴
 */
public class ArtifactDialog {

    // ===== 藏品获取对话框 =====

    /** 展示获取的藏品 */
    public static void showAcquired(Artifact artifact, Runnable onClose) {
        if (artifact == null) return;
        Dialog dialog = new Dialog("");

        dialog.cont.table(Styles.black6, t -> {
            t.add("[accent]获得藏品！[]").color(Pal.accent).pad(10).row();
            t.image().color(artifact.getColor()).height(2).pad(5).growX().row();
            t.table(info -> {
                // 图标
                info.add(getArtifactIcon(artifact)).size(48).pad(10);
                info.table(text -> {
                    text.add("[#" + artifact.getColor().toString() + "]" + artifact.name + "[]").growX().padBottom(5).row();
                    text.add("[gray]" + artifact.getRarityName() + "[]").growX().padBottom(5).row();
                    text.add(artifact.description).color(Color.lightGray).growX().padBottom(5).row();
                    text.add("[accent]" + artifact.getEffectsText() + "[]").growX().row();
                }).growX();
            }).growX().pad(10).row();
            t.image().color(Color.darkGray).height(1).pad(5).growX().row();
        }).growX().pad(10);

        dialog.buttons.button("[accent]收下[]", () -> {
            ArtifactManager.addArtifact(artifact);
            dialog.hide();
            if (onClose != null) onClose.run();
        }).size(120, 45);

        dialog.show();
    }

    // ===== 商店对话框 =====

    /** 展示商店，用资源购买藏品 */
    public static void showShop(Runnable onClose) {
        Dialog dialog = new Dialog("藏品商店");

        // 生成3个随机商品
        Rand rand = new Rand();
        Seq<Artifact> offers = new Seq<>();
        Artifact a1 = ArtifactDatabase.getRandom(rand, Rarity.N);
        Artifact a2 = ArtifactDatabase.getRandom(rand, Rarity.R);
        Artifact a3 = ArtifactDatabase.getRandom(rand, Rarity.SR);
        if (a1 != null) offers.add(a1);
        if (a2 != null) offers.add(a2);
        if (a3 != null) offers.add(a3);

        if (offers.isEmpty()) {
            ui.showInfoToast("[red]暂无商品可出售[]", 2f);
            return;
        }

        // Pause the game during shop
        state.set(mindustry.core.GameState.State.paused);

        // Broadcast shop open to all players (host only)
        if (RougeNet.isHost()) {
            RougeNet.broadcastShopOpen(offers);
        }

        // Show recommendation dialog first (5 second pause for recommendations)
        ShopRecommendationDialog.show(offers, () -> {
            // After recommendations, show the actual shop
            showShopContent(dialog, offers, onClose);
            // Unpause after shop opens
            state.set(mindustry.core.GameState.State.playing);
        });
    }

    private static void showShopContent(Dialog dialog, Seq<Artifact> offers, Runnable onClose){

        ObjectMap<Artifact, Integer> prices = new ObjectMap<>();
        prices.put(offers.get(0), 50);
        if (offers.size >= 2) prices.put(offers.get(1), 100);
        if (offers.size >= 3) prices.put(offers.get(2), 200);

        dialog.cont.table(Styles.black6, t -> {
            t.add("[accent]藏品商店[]").color(Pal.accent).pad(10).row();
            t.add("[gray]使用资源购买藏品，增强你的战力[]").padBottom(10).row();
            t.image().color(Color.darkGray).height(1).pad(5).growX().row();
        }).growX().pad(10).row();

        for (Artifact offer : offers) {
            if (offer == null) continue;
            int price = prices.get(offer, 50);

            dialog.cont.table(Styles.black3, item -> {
                item.add(getArtifactIcon(offer)).size(40).pad(8);
                item.table(info -> {
                    info.add("[#" + offer.getColor().toString() + "]" + offer.name + "[]").growX().padBottom(3).row();
                    info.add("[gray]" + offer.getRarityName() + "[]").growX().padBottom(3).row();
                    info.add(offer.description).color(Color.lightGray).growX().width(200).padBottom(3).row();
                    info.add("[accent]" + offer.getEffectsText() + "[]").growX().row();
                }).growX();
                item.table(actions -> {
                    actions.add("价格: [yellow]" + price + " 铜[]").padBottom(5).row();
                    actions.button("[green]购买[]", () -> {
                        mindustry.type.Item copper = mindustry.content.Items.copper;
                        int have = player.team().items().get(copper);
                        if (have >= price) {
                            player.team().items().remove(copper, price);
                            ArtifactManager.addArtifact(offer);
                            ui.showInfoToast("[green]购买成功！[]", 2f);
                            dialog.hide();
                            if (onClose != null) onClose.run();
                        } else {
                            ui.showInfoToast("[red]资源不足！[]", 2f);
                        }
                    }).size(80, 35);
                }).pad(8);
            }).growX().pad(5).row();
        }

        dialog.buttons.button("关闭", dialog::hide).size(100, 45);
        dialog.show();
    }

    // ===== 藏品图鉴对话框（带标签页） =====

    /** 展示藏品图鉴界面（包含收藏品和事件两个标签页） */
    public static void showCollection() {
        Dialog dialog = new Dialog("战术档案");
        dialog.setFillParent(true);

        dialog.cont.table(Styles.black6, t -> {
            t.add("[accent]战术档案 — 藏品与事件记录[]").color(Pal.accent).pad(10).row();
            t.image().color(Pal.accent).height(1).pad(2).growX().row();
        }).growX().pad(10).row();

        // 标签按钮
        dialog.cont.table(tabBar -> {
            tabBar.button("收藏品", Styles.defaultt, () -> showArtifactsTab(dialog)).size(120, 35).padRight(5);
            tabBar.button("事件", Styles.defaultt, () -> showEventsTab(dialog)).size(120, 35);
        }).growX().padLeft(10).row();

        // 内容区域
        dialog.cont.table(Styles.black3, content -> {
            content.name = "tabContent";
            showArtifactsTab(dialog);
        }).grow().pad(10);

        dialog.buttons.button("关闭", dialog::hide).size(100, 45);
        dialog.show();
    }

    /** 显示收藏品标签页内容 */
    private static void showArtifactsTab(Dialog dialog) {
        Table content = (Table) dialog.cont.find("tabContent");
        if (content == null) return;
        content.clear();

        content.pane(Styles.noBarPane, scroll -> {
            scroll.table(Styles.black6, t -> {
                if (ArtifactManager.inventory.isEmpty()) {
                    t.add("[gray]暂无藏品，进入模拟挑战来获取吧[]").pad(20);
                } else {
                    // 按稀有度分组显示
                    ObjectMap<Rarity, Seq<Artifact>> grouped = ArtifactManager.getGroupedByRarity();
                    Rarity[] order = {Rarity.SSR, Rarity.SR, Rarity.R, Rarity.N};
                    for (Rarity rarity : order) {
                        Seq<Artifact> group = grouped.get(rarity);
                        if (group == null || group.isEmpty()) continue;

                        t.add("[#" + rarity.color.toString() + "]" + rarity.displayName + " (" + group.size + ")[]").growX().pad(10).row();
                        t.image().color(rarity.color).height(1).pad(2).growX().row();

                        for (Artifact a : group) {
                            t.table(Styles.black3, item -> {
                                item.add(getArtifactIcon(a)).size(36).pad(6);
                                item.table(info -> {
                                    info.add("[#" + a.getColor().toString() + "]" + a.name + "[]").growX().padBottom(2).row();
                                    if (a.level > 1) {
                                        info.add("[yellow]Lv." + a.level + "[] ").growX().padBottom(2).row();
                                    }
                                    info.add(a.description).color(Color.gray).growX().width(250).padBottom(2).row();
                                    info.add("[accent]" + a.getEffectsText() + "[]").growX().row();
                                }).growX();
                                if (a.canUpgrade()) {
                                    item.button("[yellow]升级[]", () -> {
                                        ArtifactManager.upgradeArtifact(a);
                                        showArtifactsTab(dialog);
                                    }).size(60, 30).pad(6);
                                }
                            }).growX().pad(3).row();
                        }
                        t.row();
                    }

                    // 套装效果
                    Seq<SetBonus> activeSets = ArtifactManager.getActiveSetBonuses();
                    if (!activeSets.isEmpty()) {
                        t.add("[accent]已激活套装[]").growX().pad(10).row();
                        t.image().color(Pal.accent).height(1).pad(2).growX().row();
                        for (SetBonus bonus : activeSets) {
                            t.table(Styles.black3, st -> {
                                st.add("[gold]" + bonus.setName + " (" + bonus.requiredCount + "件)[]").growX().padBottom(2).row();
                                st.add("[yellow]" + bonus.bonusEffect.type.displayName + " " + bonus.bonusEffect.getDisplayString() + "[]").growX().row();
                            }).growX().pad(3).row();
                        }
                    }
                }
            }).growX().pad(10);
        }).grow().pad(10);
    }

    /** 显示事件标签页内容（占位） */
    private static void showEventsTab(Dialog dialog) {
        Table content = (Table) dialog.cont.find("tabContent");
        if (content == null) return;
        content.clear();

        content.table(Styles.black6, t -> {
            t.add("[gray]事件图鉴[]").color(Color.gray).pad(10).row();
            t.image().color(Color.darkGray).height(1).pad(2).growX().row();
            t.add("[gray]在模拟挑战中遇到的各种特殊事件将被记录于此[]").pad(10).row();
            t.add("[gray]此功能正在开发中，敬请期待…[]").color(Color.darkGray).pad(5).row();
        }).growX().pad(10);
    }

    // ===== 馈赠节点选择对话框 =====

    /** 馈赠节点：选择1-2个藏品 */
    public static void showGift(int count, Runnable onComplete) {
        Dialog dialog = new Dialog("神秘馈赠");

        Seq<Artifact> options = new Seq<>();
        Rand rand = new Rand();
        for (int i = 0; i < count; i++) {
            options.add(ArtifactDatabase.getRandom(rand));
        }

        dialog.cont.add("[accent]选择一份馈赠[]").color(Pal.accent).pad(10).row();
        dialog.cont.add("[gray]每种馈赠都是独特的力量[]").padBottom(10).row();
        dialog.cont.image().color(Color.darkGray).height(1).pad(5).growX().row();

        for (Artifact option : options) {
            if (option == null) continue;
            dialog.cont.button(b -> {
                b.table(Styles.black6, t -> {
                    t.add(getArtifactIcon(option)).size(40).pad(8);
                    t.table(info -> {
                        info.add("[#" + option.getColor().toString() + "]" + option.name + "[]").growX().padBottom(3).row();
                        info.add("[gray]" + option.getRarityName() + "[]").growX().padBottom(3).row();
                        info.add(option.description).color(Color.lightGray).growX().width(200).padBottom(3).row();
                        info.add("[accent]" + option.getEffectsText() + "[]").growX().row();
                    }).growX();
                }).growX().pad(5);
            }, () -> {
                dialog.hide();
                showAcquired(option, onComplete);
            }).growX().pad(5).row();
        }

        dialog.show();
    }

    // ===== 辅助方法 =====

    /** 获取藏品图标 */
    public static Element getArtifactIcon(Artifact artifact) {
        TextureRegion region = getArtifactIconTexture(artifact);
        if (region == null || region.texture == null) {
            region = Core.atlas.find("icon-gear");
        }
        if (region == null || region.texture == null) {
            region = Core.atlas.find("icon-question");
        }
        if (region == null || region.texture == null) {
            region = Core.atlas.find("white");
        }
        return new Image(region).setScaling(Scaling.fit);
    }

    /** 获取藏品图标 TextureRegion */
    public static TextureRegion getArtifactIconTexture(Artifact artifact) {
        int iconId = artifact.iconId;
        TextureRegion region = null;

        // 根据 iconId 获取对应图标
        if (iconId < 16) {
            // 使用物品图标作为藏品图标
            Seq<Item> items = content.items().select(i -> i != null && !i.hidden);
            if (iconId < items.size) {
                region = items.get(iconId).uiIcon;
            }
        }

        if (region == null) {
            region = mindustry.content.Items.copper.uiIcon;
        }

        return region;
    }
}
