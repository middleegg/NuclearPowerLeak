package Npl.Rouge;

import arc.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.game.EventType.*;

import java.io.*;

import static mindustry.Vars.*;

/**
 * RougeNet —— Rouge 联机大厅的网络层
 * ==============================================================
 * 设计原则（对齐原版）：
 *   ① 房主权威：树结构、进度、选中节点、撤离窗口、换图全部由房主决定，客户端只镜像。
 *   ② 不断线：换图走原版 {@link mindustry.net.WorldReloader}（begin → 换世界 → end），
 *      客户端在**同一条连接**上收到 worldDataBegin + WorldStream，socket / Player / admin 全程保留。
 *   ③ 树不靠 seed 重建：RougeTree.generateTree 依赖本机文件系统遍历顺序，
 *      因此直接同步树拓扑（见 RougeTree.writeTo / readFrom）。
 *
 * 原版给 mod 留的自定义包通道（NetClient/NetServer 里的 @Remote 方法）：
 *   - 房主 → 客户端：Call.serverBinaryPacketReliable(type, data)
 *     客户端用 netServer.addBinaryPacketHandler(type, (player, data) -> ...) 收
 *   - 客户端 → 房主：Call.clientBinaryPacketReliable(type, data)
 *     房主用 netClient.addBinaryPacketHandler(type, data -> ...) 收
 * （注意这两张 handler 表挂在“对侧”对象上，是原版既有的约定，不是笔误。）
 */
public class RougeNet {

    /** 房主 → 客户端：完整大厅状态 */
    public static final String CH_STATE = "npl-rouge-state";
    /** 房主 → 客户端：小事件（选中 / 进出大厅 / 撤离窗口） */
    public static final String CH_EVENT = "npl-rouge-event";
    /** 客户端 → 房主：请求补发状态 */
    public static final String CH_HELLO = "npl-rouge-hello";
    /** 房主 → 客户端：商店打开（非战斗节点） */
    public static final String CH_SHOP_OPEN = "npl-rouge-shop-open";
    /** 客户端 → 房主：推荐藏品 */
    public static final String CH_RECOMMEND = "npl-rouge-recommend";
    /** 房主 → 客户端：推荐更新 */
    public static final String CH_RECOMMEND_UPDATE = "npl-rouge-recommend-update";

    /** 事件类型 */
    public static final byte EV_SELECT = 1;
    public static final byte EV_LOBBY = 2;
    public static final byte EV_EVAC = 3;
    public static final byte EV_SHOP_OPEN = 4;
    public static final byte EV_NODE_ENTER = 5;

    private static final int VERSION = 1;
    private static boolean initialized = false;
    private static boolean selfTested = false;

    /** 是否处于大厅（房主权威，客户端镜像） */
    public static boolean inLobby = false;
    /** 房主当前选中的节点 id，-1 = 没选 */
    public static int selectedId = -1;
    /** 撤离窗口是否打开（客户端用于显示状态） */
    public static boolean evacAvailable = false;
    /** 撤离窗口打开的波次 */
    public static int evacWave = 0;
    /** 本进程是否参与过联机会话（用于退出时恢复本地存档） */
    public static boolean wasInSession = false;
    /** 客户端是否已经收到过房主的完整状态（用于首连失败时重试请求） */
    public static boolean stateReceived = false;

    public static boolean isMultiplayer(){
        return net.active();
    }

    public static boolean isClient(){
        return net.client();
    }

    public static boolean isHost(){
        return net.active() && !net.client();
    }

    public static void init(){
        if(initialized) return;
        initialized = true;

        // 进度跨会话恢复（此前 RougeSave.load() 在整个工程里没有任何调用点）
        if(!net.client()){
            RougeSave.load();
        }

        // 客户端：接收房主广播
        netServer.addBinaryPacketHandler(CH_STATE, (player, data) -> handleState(data));
        netServer.addBinaryPacketHandler(CH_EVENT, (player, data) -> handleEvent(data));
        netServer.addBinaryPacketHandler(CH_SHOP_OPEN, (player, data) -> handleShopOpen(data));
        netServer.addBinaryPacketHandler(CH_RECOMMEND_UPDATE, (player, data) -> handleRecommendUpdate(data));
        // 房主：接收客户端请求
        netClient.addBinaryPacketHandler(CH_HELLO, data -> handleHello());
        netClient.addBinaryPacketHandler(CH_RECOMMEND, data -> handleRecommend(data));

        // 有人进来（此时对方已加载完世界流）就补发一次完整状态
        Events.on(PlayerJoin.class, e -> {
            if(isHost()) sendStateToAll();
        });

        // 退出联机会话后把本地存档读回来。
        // 只靠这个事件不够可靠（客户端断开时 net.active()/net.client() 的复位顺序取决于 arc），
        // 所以真正的判断放在幂等的 checkSessionEnded() 里，本事件、每帧 tick、打开树界面前都会调用。
        Events.on(StateChangeEvent.class, e -> checkSessionEnded());

        Log.info("[RougeNet] initialized");
    }

    /**
     * 会话结束后恢复本地存档（幂等，可以每帧调用）。
     *
     * 客户端内存里存的是房主的进度（treeSeed / storedItems / completedNodeIds / inLobby…），
     * 如果不还原，之后单机打开树界面会用**房主的 treeSeed + 本机地图列表**重建一棵树，
     * 并且 save() 写进客户端自己的 rouge_save.bin。
     */
    public static void checkSessionEnded(){
        if(!wasInSession) return;
        // 会话还活着：主机仍在开服，或者客户端仍连着
        if(net.server() || (net.client() && net.active())) return;

        wasInSession = false;
        inLobby = false;
        selectedId = -1;
        stateReceived = false;
        RougeSave.isInRougeMode = false;
        RougeSave.load();
        Log.info("[RougeNet] session ended; local save restored");
    }

    // ==================== 房主 → 客户端 ====================

    /** 房主：把完整大厅状态广播给所有客户端 */
    public static void sendStateToAll(){
        if(!isHost()) return;
        byte[] data = encodeState();
        if(data == null) return;
        Call.serverBinaryPacketReliable(CH_STATE, data);
    }

    /** 房主：广播一个小事件 */
    public static void broadcastEvent(byte type, int a, int b){
        if(!isHost()) return;
        byte[] data;
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Writes w = new Writes(new DataOutputStream(baos));
            w.i(type);
            w.i(a);
            w.i(b);
            w.close();
            data = baos.toByteArray();
        }catch(Exception e){
            Log.err("[RougeNet] broadcastEvent failed", e);
            return;
        }
        Call.serverBinaryPacketReliable(CH_EVENT, data);
    }

    /** 房主：广播“我选中了哪个节点”（客户端会高亮同一个节点） */
    public static void broadcastSelect(int nodeId){
        selectedId = nodeId;
        broadcastEvent(EV_SELECT, nodeId, 0);
    }

    /** 房主：广播“现在在大厅 / 已经离开大厅” */
    public static void broadcastLobby(boolean lobby){
        inLobby = lobby;
        broadcastEvent(EV_LOBBY, lobby ? 1 : 0, 0);
    }

    /** 房主：广播撤离窗口状态 */
    public static void broadcastEvac(boolean available, int wave){
        evacAvailable = available;
        evacWave = wave;
        broadcastEvent(EV_EVAC, available ? 1 : 0, wave);
    }

    /** RougeSave.save() 的钩子：进度变化后同步（sendStateToAll 不写盘，无递归风险） */
    public static void onSave(){
        if(isHost() && isMultiplayer()) sendStateToAll();
    }

    // ==================== 编解码 ====================

    private static byte[] encodeState(){
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Writes w = new Writes(new DataOutputStream(baos));

            w.i(VERSION);
            w.i(inLobby ? 1 : 0);
            w.i(selectedId);
            w.i(evacAvailable ? 1 : 0);
            w.i(evacWave);
            w.i(RougeSave.currentEvacWave);
            w.str(RougeSave.currentSectorName == null ? "" : RougeSave.currentSectorName);

            RougeSave.writeTo(w);
            RougeTree.writeTo(w);

            w.close();
            return baos.toByteArray();
        }catch(Exception e){
            Log.err("[RougeNet] encodeState failed", e);
            return null;
        }
    }

    private static void handleState(byte[] data){
        if(!isClient()) return;
        applyState(data, true);
    }

    /**
     * 解析并应用一份完整状态。
     * @param applyUi 是否顺带驱动界面（协议自检时传 false）
     * @return 是否成功
     */
    private static boolean applyState(byte[] data, boolean applyUi){
        try{
            Reads r = new Reads(new DataInputStream(new ByteArrayInputStream(data)));

            int version = r.i();
            if(version != VERSION){
                Log.warn("[RougeNet] state version mismatch: @", version);
                return false;
            }

            inLobby = r.i() != 0;
            selectedId = r.i();
            evacAvailable = r.i() != 0;
            evacWave = r.i();

            RougeSave.currentEvacWave = r.i();
            String sector = r.str();
            RougeSave.currentSectorName = sector.isEmpty() ? null : sector;

            RougeSave.readFrom(r, null);
            RougeTree.readFrom(r);

            RougeSave.isInRougeMode = true;
            wasInSession = true;

            // 联调日志：双开测试时用它确认客户端确实收到了房主状态
            Log.info("[RougeNet] state applied: inLobby=@ sector=@ selected=@ evac=@ nodes=@",
                inLobby, RougeSave.currentSectorName, selectedId, evacAvailable,
                RougeSave.completedNodeIds.size);

            if(applyUi){
                applyRemoteToUi();
                // 只有世界已经就绪（界面确实被驱动过）才算“收到”；
                // 否则让 tick 里的重试继续请求，避免首连时状态包来得太早、树界面一直不出现
                stateReceived = state.isGame();
            }else{
                stateReceived = true;
            }
            return true;
        }catch(Throwable e){
            Log.err("[RougeNet] applyState failed", e);
            return false;
        }
    }

    /**
     * 协议自检：把当前状态编码 → 打乱内存态 → 解码 → 比对关键字段。
     * 由于编解码是同一套手写 Writes/Reads，字段顺序错位是最容易犯又最难发现的 bug；
     * 房主开局时跑一次，日志出现 "[RougeNet] selfTest PASSED" 就说明协议是对称的。
     * 注意：它会覆盖内存中的 RougeSave/RougeTree 状态（正常情况下解码结果与原来一致）。
     */
    public static void selfTest(){
        if(selfTested) return;
        selfTested = true;

        try{
            boolean lobby = inLobby;
            int sel = selectedId;
            boolean ev = evacAvailable;
            int ew = evacWave;
            String sector = RougeSave.currentSectorName;
            int cev = RougeSave.currentEvacWave;
            long seed = RougeSave.treeSeed;
            int nodes = RougeSave.completedNodeIds.size;
            int branches = RougeSave.branchChoices.size;
            int tech = RougeSave.techLevels.size;
            int items = RougeSave.storedItems.size;
            String rootMap = RougeTree.getRoot() == null ? null : RougeTree.getRoot().mapName;

            byte[] data = encodeState();
            if(data == null){
                Log.err("[RougeNet] selfTest: encode failed");
                return;
            }

            // 破坏内存态，确认解码确实重建了数据而不是"看起来没变"
            inLobby = !lobby;
            selectedId = -999;
            evacAvailable = !ev;
            evacWave = -1;
            RougeSave.clearProgress();
            RougeTree.root = null;

            if(!applyState(data, false)){
                Log.err("[RougeNet] selfTest: decode failed");
                return;
            }

            boolean ok = inLobby == lobby
                && selectedId == sel
                && evacAvailable == ev
                && evacWave == ew
                && java.util.Objects.equals(RougeSave.currentSectorName, sector)
                && RougeSave.currentEvacWave == cev
                && RougeSave.treeSeed == seed
                && RougeSave.completedNodeIds.size == nodes
                && RougeSave.branchChoices.size == branches
                && RougeSave.techLevels.size == tech
                && RougeSave.storedItems.size == items
                && java.util.Objects.equals(RougeTree.getRoot() == null ? null : RougeTree.getRoot().mapName, rootMap);

            Log.info("[RougeNet] selfTest @ (lobby=@ sector=@ nodes=@ branches=@ tech=@ items=@)",
                ok ? "PASSED" : "FAILED", lobby, sector, nodes, branches, tech, items);

            if(!ok){
                Log.err("[RougeNet] selfTest mismatch: lobby @->@ selected @->@ evac @->@ wave @->@ sector @->@ seed @->@ nodes @->@",
                    lobby, inLobby, sel, selectedId, ev, evacAvailable, ew, evacWave,
                    sector, RougeSave.currentSectorName, seed, RougeSave.treeSeed, nodes,
                    RougeSave.completedNodeIds.size);
            }
        }catch(Throwable t){
            Log.err("[RougeNet] selfTest failed", t);
        }
    }

    private static void handleEvent(byte[] data){
        if(!isClient()) return;
        try{
            Reads r = new Reads(new DataInputStream(new ByteArrayInputStream(data)));
            byte type = (byte)r.i();
            int a = r.i();
            int b = r.i();

            switch(type){
                case EV_SELECT -> {
                    selectedId = a;
                    if(RougeCampaignScreen.isShown()) RougeCampaignScreen.show();
                }
                case EV_LOBBY -> {
                    inLobby = a != 0;
                    if(!inLobby){
                        RougeCampaignScreen.hide();
                    }else if(state.isGame()){
                        // 树界面只在大厅（游戏状态）里显示，避免盖在任务地图上
                        RougeCampaignScreen.show();
                    }
                }
                case EV_EVAC -> {
                    evacAvailable = a != 0;
                    evacWave = b;
                    RougeGameControl.onRemoteEvacChanged(evacAvailable, evacWave);
                }
                default -> {
                }
            }

            dismissGameOverUi();
        }catch(Throwable e){
            Log.err("[RougeNet] handleEvent failed", e);
        }
    }

    private static void handleHello(){
        if(!isHost()) return;
        sendStateToAll();
    }

    // ==================== 客户端 UI 跟随 ====================

    private static void applyRemoteToUi(){
        dismissGameOverUi();

        // 撤离窗口状态不依赖世界是否就绪，先应用掉
        RougeGameControl.onRemoteEvacChanged(evacAvailable, evacWave);

        // 世界还没加载完（例如刚连上、世界流还在传）时先不动树界面，
        // 等 WorldLoadEvent / PlayerJoin 那次状态包再显示
        if(!state.isGame()) return;

        if(inLobby){
            RougeCampaignScreen.show();
        }else{
            RougeCampaignScreen.hide();
        }
    }

    /** 换图/回大厅时把原版的失败结算弹窗收掉，否则它会一直盖在大厅上面 */
    private static void dismissGameOverUi(){
        try{
            if(ui != null && ui.restart != null && ui.restart.isShown()){
                ui.restart.hide();
            }
        }catch(Throwable ignored){
        }
    }

    /** 客户端：世界加载完成后主动要一次状态（房主 PlayerJoin 时也会推，这是双保险） */
    public static void requestState(){
        if(!isClient()) return;
        Call.clientBinaryPacketReliable(CH_HELLO, new byte[0]);
    }

    // ==================== 商店与推荐 ====================

    /** 房主：广播商店打开 */
    public static void broadcastShopOpen(Seq<Artifact> options){
        if(!isHost()) return;
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Writes w = new Writes(new DataOutputStream(baos));
            w.i(EV_SHOP_OPEN);
            w.i(options.size);
            for(Artifact a : options){
                w.str(a != null ? a.id : "");
            }
            w.close();
            Call.serverBinaryPacketReliable(CH_SHOP_OPEN, baos.toByteArray());
        }catch(Exception e){
            Log.err("[RougeNet] broadcastShopOpen failed", e);
        }
    }

    /** 客户端：发送推荐 */
    public static void broadcastRecommendation(int index, String artifactId){
        if(!isClient()) return;
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Writes w = new Writes(new DataOutputStream(baos));
            w.i(index);
            w.str(artifactId);
            w.str(player.name);
            w.close();
            Call.clientBinaryPacketReliable(CH_RECOMMEND, baos.toByteArray());
        }catch(Exception e){
            Log.err("[RougeNet] broadcastRecommendation failed", e);
        }
    }

    /** 房主：处理客户端推荐 */
    private static void handleRecommend(byte[] data){
        if(!isHost()) return;
        try{
            Reads r = new Reads(new DataInputStream(new ByteArrayInputStream(data)));
            int index = r.i();
            String artifactId = r.str();
            String playerName = r.str();
            r.close();

            ShopRecommendationDialog.addRecommendation(index, playerName);
        }catch(Exception e){
            Log.err("[RougeNet] handleRecommend failed", e);
        }
    }

    /** 客户端：处理商店打开 */
    private static void handleShopOpen(byte[] data){
        if(!isClient()) return;
        try{
            Reads r = new Reads(new DataInputStream(new ByteArrayInputStream(data)));
            r.i(); // type
            int count = r.i();
            Seq<Artifact> options = new Seq<>();
            for(int i = 0; i < count; i++){
                String id = r.str();
                Artifact a = ArtifactDatabase.get(id);
                if(a != null) options.add(a);
            }
            r.close();

            // Show shop recommendation dialog for client
            ShopRecommendationDialog.show(options, () -> {
                // After recommendation, show the actual shop (read-only for client)
                ArtifactDialog.showShop(null);
            });
        }catch(Exception e){
            Log.err("[RougeNet] handleShopOpen failed", e);
        }
    }

    /** 客户端：处理推荐更新 */
    private static void handleRecommendUpdate(byte[] data){
        // Update recommendation display
    }

    /** 房主：广播节点进入（非战斗节点） */
    public static void broadcastNodeEnter(String nodeName, String nodeType){
        if(!isHost()) return;
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Writes w = new Writes(new DataOutputStream(baos));
            w.i(EV_NODE_ENTER);
            w.str(nodeName);
            w.str(nodeType);
            w.close();
            Call.serverBinaryPacketReliable(CH_EVENT, baos.toByteArray());
        }catch(Exception e){
            Log.err("[RougeNet] broadcastNodeEnter failed", e);
        }
    }
}
