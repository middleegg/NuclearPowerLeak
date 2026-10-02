package Npl.Rouge;

import arc.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.modules.*;

import static mindustry.Vars.*;

/**
 * RougeGameControl —— Rouge 模式的游戏内逻辑（树杈状）
 * ==============================================================
 * 职责：
 *   ① WorldLoadEvent：如果在 Rouge 模式，开局把携带资源加到核心里
 *   ② 撤离规则：
 *      - 防守图（无敌人核心）：每隔5波出现撤离按钮（5/10/15/20/25）
 *        第5波出现，若未撤离则第6波消失，直到第10波再出现
 *      - 进攻图（有敌人核心）：摧毁所有敌人核心后撤离按钮才出现
 *   ③ 撤离：把核心资源存进 RougeSave，标记节点完成，处理分支选择，返回主菜单
 *   ④ GameOverEvent：失败时不存资源（资源丢失）
 */
public class RougeGameControl {

    private static boolean initialized = false;
    /** 撤离按钮 */
    private static Table evacButtonTable;
    private static boolean evacAvailable = false;
    /** 当前节点（用于撤离后处理） */
    private static RougeTree.Node currentNode;

    /** 是否为防守图（无敌人核心） */
    private static boolean isDefenseMap = true;
    /** 敌人核心数量（用于进攻图） */
    private static int enemyCoreCount = 0;

    public static void init(){
        if(initialized) return;
        initialized = true;

        // ① 开局加载：把携带资源加到核心，检测地图类型
        Events.on(WorldLoadEvent.class, e -> {
            // 联机客户端：世界（含核心里的物品）是房主流式发过来的，
            // 这里再 applyStoredToCore / 加起始资源会让物品翻倍；rules 也已经是房主应用过的版本
            if(net.client()){
                evacAvailable = false;
                removeEvacButton();
                return;
            }

            if(RougeSave.currentSectorName == null) return;

            // 查找当前节点
            currentNode = findNodeByName(RougeSave.currentSectorName);

            // 检测地图类型和敌人核心数量
            detectMapType();

            // 应用藏品效果到游戏规则
            ArtifactManager.applyToRules(state.rules);

            // 等核心生成完再加资源
            Time.run(30f, () -> {
                Building core = findCore();
                if(core == null) return;

                int before = core.items.total();
                RougeSave.applyStoredToCore(core.items);

                // 应用科技树起始资源加成
                int startRes = RougeTechTree.getStartingResources();
                if(startRes > 0){
                    core.items.add(Items.copper, startRes);
                    core.items.add(Items.lead, startRes);
                }

                int added = core.items.total() - before;
                if(added > 0){
                    Log.info("Rouge: added " + added + " stored items to core");
                }
            });

            // 重置撤离状态
            evacAvailable = false;
            removeEvacButton();
        });

        // ② 波次事件：防守图每隔5波显示撤离按钮，进攻图检测核心
        Events.on(WaveEvent.class, e -> {
            if(RougeSave.currentSectorName == null) return;
            // 联机客户端：撤离窗口由房主决定（房主通过 RougeNet 广播过来）
            if(net.client()) return;

            int wave = state.wave;

            if(isDefenseMap){
                // 防守图：每隔5波出现撤离按钮（5, 10, 15, 20, 25...）
                if(wave % 5 == 0 && wave > 0){
                    // 正好是5的倍数波，显示撤离按钮
                    if(!evacAvailable){
                        evacAvailable = true;
                        showEvacButton();
                        ui.showInfoToast("第 " + wave + " 波：可以撤离！", 5f);
                        RougeNet.broadcastEvac(true, wave);
                    }
                }else if(evacAvailable){
                    // 不是5的倍数波，隐藏撤离按钮
                    evacAvailable = false;
                    removeEvacButton();
                    ui.showInfoToast("撤离窗口已关闭，等待下一撤离点", 3f);
                    RougeNet.broadcastEvac(false, wave);
                }
            }else{
                // 进攻图：每波检查敌人核心是否被摧毁
                checkEnemyCores();
            }
        });

        // ④ 游戏结束：失败时不存资源（联机时由房主把全队带回大厅）
        Events.on(GameOverEvent.class, e -> {
            if(RougeSave.currentSectorName == null) return;
            if(net.client()) return;

            Log.info("Rouge: sector failed, resources lost");
            evacAvailable = false;
            removeEvacButton();
            RougeSave.currentSectorName = null;
            currentNode = null;

            if(RougeNet.isMultiplayer()){
                RougeNet.broadcastEvac(false, state.wave);
                RougeNet.sendStateToAll();
                Time.run(60f, RougeLobby::returnToLobby);
            }
        });
    }

    /** 检测地图类型：是否有敌人核心 */
    private static void detectMapType(){
        enemyCoreCount = 0;
        for(Building b : Groups.build){
            if(b.block instanceof CoreBlock && b.team != player.team() && b.team != Team.derelict){
                enemyCoreCount++;
            }
        }
        isDefenseMap = (enemyCoreCount == 0);
        Log.info("Rouge: map type detected - " + (isDefenseMap ? "defense" : "attack") + ", enemy cores: " + enemyCoreCount);
    }

    /** 检查进攻图敌人核心是否全被摧毁 */
    private static void checkEnemyCores(){
        int remainingCores = 0;
        for(Building b : Groups.build){
            if(b.block instanceof CoreBlock && b.team != player.team() && b.team != Team.derelict){
                remainingCores++;
            }
        }

        if(remainingCores == 0 && enemyCoreCount > 0){
            // 所有敌人核心被摧毁，显示撤离按钮
            if(!evacAvailable){
                evacAvailable = true;
                showEvacButton();
                ui.showInfoToast("所有敌人核心已被摧毁！可以撤离！", 5f);
                RougeNet.broadcastEvac(true, state.wave);
            }
        }
    }

    /** 按名称查找树节点 */
    private static RougeTree.Node findNodeByName(String name){
        return findNodeRecursive(RougeTree.getRoot(), name);
    }

    private static RougeTree.Node findNodeRecursive(RougeTree.Node node, String name){
        if(node == null) return null;
        if(node.mapName.equals(name)) return node;
        RougeTree.Node found = findNodeRecursive(node.left, name);
        if(found != null) return found;
        found = findNodeRecursive(node.middle, name);
        if(found != null) return found;
        return findNodeRecursive(node.right, name);
    }

    /** 显示撤离按钮到 HUD 右上角（联机客户端只显示状态，撤离权在房主） */
    private static void showEvacButton(){
        removeEvacButton();
        evacButtonTable = new Table(Styles.black5);
        evacButtonTable.top().right();

        if(RougeNet.isClient()){
            evacButtonTable.add("[gray]等待房主撤离…[]").pad(12);
        }else{
            evacButtonTable.button("[accent]撤离 (第 " + state.wave + " 波)[]", Styles.defaultt, () -> {
                doEvacuate();
            }).size(160, 50).pad(4);
        }

        evacButtonTable.pack();
        evacButtonTable.setPosition(Core.graphics.getWidth() - evacButtonTable.getWidth() - 10,
            Core.graphics.getHeight() - evacButtonTable.getHeight() - 60);
        ui.hudGroup.addChild(evacButtonTable);
    }

    /** 客户端：房主广播来的撤离窗口状态（客户端自己不判断波次） */
    static void onRemoteEvacChanged(boolean available, int wave){
        if(!RougeNet.isClient()) return;

        if(available){
            if(!evacAvailable){
                evacAvailable = true;
                showEvacButton();
                ui.showInfoToast("房主可以撤离了（第 " + wave + " 波）", 4f);
            }
        }else if(evacAvailable){
            evacAvailable = false;
            removeEvacButton();
        }
    }

    private static void removeEvacButton(){
        if(evacButtonTable != null){
            evacButtonTable.remove();
            evacButtonTable = null;
        }
    }

    /** 执行撤离：存资源 + 标记完成 + 处理分支 + 返回模式界面（联机时只有房主可触发） */
    private static void doEvacuate(){
        if(RougeNet.isClient()){
            ui.showInfoToast("[scarlet]联机时由房主决定撤离[]", 3f);
            return;
        }

        // 1. 拿到核心资源
        Building core = findCore();

        if(core != null){
            float ratio = RougeTechTree.getEvacuationRatio();
            // 应用藏品出售收益加成
            ratio *= (1f + ArtifactManager.getEffect(Artifact.EffectType.SELL_BONUS));
            RougeSave.storeFromCore(core.items, ratio);
            Log.info("Rouge: stored " + RougeSave.storedTotal() + " items (ratio: " + ratio + ")");
        }

        // 2. 根据节点类型给予藏品奖励
        if(currentNode != null){
            giveArtifactRewards(currentNode);
        }

        // 3. 标记节点完成
        final RougeTree.Node branchNode;
        if(currentNode != null){
            RougeSave.completedNodeIds.add(currentNode.id);
            ArtifactManager.save();
            RougeSave.save();

            // 处理分支选择
            if(currentNode.left != null && currentNode.middle != null && currentNode.right != null){
                // 三分叉点：弹出选择对话框（在返回主菜单后）
                branchNode = currentNode;
            }else{
                branchNode = null;
                if(currentNode.left != null || currentNode.middle != null || currentNode.right != null){
                    // 线性节点：自动解锁下一个
                    if(currentNode.left != null && !currentNode.left.locked){
                        currentNode.left.locked = false;
                    }else if(currentNode.middle != null && !currentNode.middle.locked){
                        currentNode.middle.locked = false;
                    }else if(currentNode.right != null && !currentNode.right.locked){
                        currentNode.right.locked = false;
                    }
                }
            }
        }else{
            branchNode = null;
        }

        RougeSave.save();

        // ===== 联机：房主把全队带回大厅（客户端跟着世界流一起回去，不会掉线）=====
        if(RougeNet.isMultiplayer()){
            ui.showInfoToast("撤离成功！资源已保存", 3f);
            evacAvailable = false;
            removeEvacButton();
            // 先把字段清干净再广播，避免状态包里还带着刚刚打完的那个 sector
            RougeSave.currentSectorName = null;
            currentNode = null;
            RougeNet.broadcastEvac(false, state.wave);
            RougeNet.sendStateToAll();

            RougeLobby.returnToLobby();

            Time.run(30f, () -> {
                if(branchNode != null){
                    showBranchChoiceAfterEvac(branchNode);
                }else{
                    RougeCampaignScreen.show();
                }
            });
            return;
        }

        // 4. 提示 + 返回主菜单
        ui.showInfoToast("撤离成功！资源已保存", 3f);
        evacAvailable = false;
        removeEvacButton();
        RougeSave.currentSectorName = null;
        currentNode = null;

        // 显示返回主菜单对话框
        Time.run(30f, () -> {
            new BaseDialog("撤离成功"){{
                cont.add("资源已保存到 Rouge 数据库").pad(20).row();
                // 显示获得的藏品
                if(!ArtifactManager.inventory.isEmpty()){
                    int newCount = ArtifactManager.count();
                    cont.add("[accent]当前藏品: " + newCount + " 件[]").padBottom(10).row();
                }
                buttons.button("返回模式", () -> {
                    hide();
                    try{
                        // 返回主菜单
                        control.getClass().getMethod("dispose").invoke(control);
                    }catch(Exception e){
                        ui.showInfoToast("请按 Esc 返回主菜单", 3f);
                    }
                    // 延迟后显示模式界面
                    Time.run(60f, () -> {
                        if(branchNode != null){
                            showBranchChoiceAfterEvac(branchNode);
                        }else{
                            RougeCampaignScreen.show();
                        }
                    });
                }).size(150, 50);
            }}.show();
        });
    }

    /** 根据节点类型给予藏品奖励 */
    private static void giveArtifactRewards(RougeTree.Node node){
        if(node == null) return;

        switch(node.type){
            case GIFT:
                // 馈赠节点：免费获得2个藏品
                ArtifactDialog.showGift(2, null);
                break;
            case SHOP:
                // 商店节点：打开商店对话框
                ArtifactDialog.showShop(null);
                break;
            case SPECIAL:
                // 特殊节点：必定获得1个SR以上藏品
                Artifact artifact = ArtifactDatabase.getRandom(new Rand(), Artifact.Rarity.SR);
                if(artifact != null){
                    ArtifactDialog.showAcquired(artifact, null);
                }
                break;
            default:
                // 进攻/防守节点：25%概率获得1个藏品
                if(Mathf.chance(0.25)){
                    Artifact reward = ArtifactDatabase.getRandom(new Rand());
                    if(reward != null){
                        ArtifactDialog.showAcquired(reward, null);
                    }
                }
                break;
        }
    }

    /** 撤离后显示分支选择 */
    private static void showBranchChoiceAfterEvac(RougeTree.Node node){
        new BaseDialog("选择分支"){{
            cont.add("通关！选择要解锁的路线，另外两条将永久封锁").pad(20).row();

            // 左线
            cont.button("← 左线: " + node.left.mapName, Styles.defaultt, () -> {
                RougeTree.completeNode(node.id, 0);
                RougeSave.branchChoices.put(node.id, 0);
                RougeSave.save();
                hide();
                Time.run(30f, () -> RougeCampaignScreen.show());
            }).size(250, 50).pad(5).row();

            // 中线
            if(node.middle != null){
                cont.button("↓ 中线: " + node.middle.mapName, Styles.defaultt, () -> {
                    RougeTree.completeNode(node.id, 1);
                    RougeSave.branchChoices.put(node.id, 1);
                    RougeSave.save();
                    hide();
                    Time.run(30f, () -> RougeCampaignScreen.show());
                }).size(250, 50).pad(5).row();
            }

            // 右线
            cont.button("右线: " + node.right.mapName + " →", Styles.defaultt, () -> {
                RougeTree.completeNode(node.id, 2);
                RougeSave.branchChoices.put(node.id, 2);
                RougeSave.save();
                hide();
                Time.run(30f, () -> RougeCampaignScreen.show());
            }).size(250, 50).pad(5).row();

            buttons.button("稍后决定", () -> {
                hide();
                Time.run(30f, () -> RougeCampaignScreen.show());
            }).size(120, 40);
        }}.show();
    }

    /** 找玩家队伍的核心建筑 */
    private static Building findCore(){
        Building core = player.core();
        if(core != null) return core;
        for(Building b : Groups.build){
            if(b.team == player.team() && b.block instanceof CoreBlock){
                return b;
            }
        }
        return null;
    }
}
