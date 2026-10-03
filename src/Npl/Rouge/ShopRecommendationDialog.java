package Npl.Rouge;

import arc.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

/**
 * ShopRecommendationDialog —— 商店推荐对话框
 * ======================================================================
 * 非房主玩家可以点击藏品进行推荐
 * 房主可以看到所有玩家的推荐
 */
public class ShopRecommendationDialog {

    private static BaseDialog dialog;
    private static Seq<Artifact> options;
    private static IntMap<Seq<String>> recommendationDetails = new IntMap<>();
    private static Label timerLabel;
    private static int timeLeft = 5;
    private static Timer.Task countdownTask;
    private static Runnable onComplete;

    public static void addRecommendation(int index, String playerName){
        Seq<String> players = recommendationDetails.get(index, new Seq<>());
        if(!players.contains(playerName)){
            players.add(playerName);
            recommendationDetails.put(index, players);
        }
    }

    public static void show(Seq<Artifact> shopOptions, Runnable onComplete){
        recommendationDetails.clear();
        options = shopOptions;
        ShopRecommendationDialog.onComplete = onComplete;
        timeLeft = 5;
        build();
        dialog.show();

        startCountdown();
    }

    private static void build(){
        dialog = new BaseDialog("商店推荐");

        dialog.cont.table(top -> {
            top.add("[#FF4444]商店推荐[]").color(Pal.accent).fontScale(1.3f).left().padRight(20);
            timerLabel = top.add("[yellow]5s[]").color(Color.yellow).fontScale(1.2f).right().get();
        }).growX().pad(10).row();

        dialog.cont.add(RougeNet.isHost() ? "[gray]等待其他玩家推荐...[]" : "[gray]点击藏品进行推荐（仅一次）[]")
            .color(Color.gray).padBottom(10).row();

        dialog.cont.table(items -> {
            items.defaults().pad(5);

            for (int i = 0; i < options.size; i++) {
                Artifact artifact = options.get(i);
                if (artifact == null) continue;

                int index = i;
                items.table(item -> {
                    item.left().defaults().pad(4);

                    item.add(ArtifactDialog.getArtifactIcon(artifact)).size(36).pad(6);
                    item.table(text -> {
                        text.add("[#" + artifact.getColor().toString() + "]" + artifact.name + "[]")
                            .growX().padBottom(2).row();
                        text.add("[gray]" + artifact.getRarityName() + "[]").growX().padBottom(2).row();
                        text.add(artifact.description).color(Color.lightGray).growX().width(180).padBottom(2).row();
                    }).growX();

                    if (RougeNet.isHost()) {
                        Label recLabel = new Label("推荐: 0");
                        recLabel.update(() -> {
                            recLabel.setText("[yellow]推荐: " + getRecommendationCount(index) + "[]");
                        });
                        item.add(recLabel).padRight(8).width(80);
                    } else {
                        TextButton recBtn = new TextButton("推荐");
                        recBtn.setDisabled(false);
                        recBtn.clicked(() -> {
                            recommend(index, artifact);
                            recBtn.setDisabled(true);
                            recBtn.setText("[green]已推荐[]");
                        });
                        item.add(recBtn).size(70, 35).padRight(8);
                    }
                }).growX().height(55);
                items.row();
            }
        }).growX().pad(10).row();

        if (RougeNet.isHost()) {
            dialog.cont.button("完成", () -> {
                finishRecommendations();
            }).size(120, 40).pad(10);
        }
    }

    private static void startCountdown(){
        if (countdownTask != null) countdownTask.cancel();
        countdownTask = Timer.schedule(() -> {
            Time.run(0f, () -> {
                timeLeft--;
                if (timerLabel != null) {
                    if (timeLeft > 0) {
                        timerLabel.setText("[yellow]" + timeLeft + "s[]");
                    } else {
                        timerLabel.setText("[red]时间到[]");
                    }
                }
                if (timeLeft <= 0) {
                    finishRecommendations();
                }
            });
        }, 0f, 1f, 5);
    }

    private static void finishRecommendations(){
        if (countdownTask != null) countdownTask.cancel();
        dialog.hide();
        if (onComplete != null) onComplete.run();
    }

    private static void recommend(int index, Artifact artifact){
        addRecommendation(index, player.name);
        RougeNet.broadcastRecommendation(index, artifact.id);
    }

    private static int getRecommendationCount(int index){
        Seq<String> players = recommendationDetails.get(index);
        return players == null ? 0 : players.size;
    }

    public static void clear(){
        recommendationDetails.clear();
        if (countdownTask != null) countdownTask.cancel();
        if (dialog != null) dialog.hide();
    }
}
