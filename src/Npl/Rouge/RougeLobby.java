package Npl.Rouge;

import arc.*;
import arc.files.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.net.*;
import mindustry.ui.*;
import mindustry.world.*;

import java.io.*;

import static mindustry.Vars.*;

/**
 * RougeLobby —— Rouge 联机大厅
 * ==============================================================
 * 为什么需要一个“大厅地图”：
 *   原版 NetServer.update() 一旦看到 net.server() && state.isMenu() 就会主动关服
 *   （ui.loadfrag.show("@server.closing") → net.closeServer()）。所以“大家一起待在主菜单里看树”
 *   是不成立的，大厅必须是一个**活着的游戏状态**：一张大厅图 + 盖在上面的战役树界面。
 *
 * 换图全部走原版 WorldReloader（Control.playSector 内部就是这套）：
 *   reloader.begin() → world.loadMap(...) → logic.play() → reloader.end()
 *   房主侧 begin() 收集所有客户端 Player 并发 worldDataBegin；
 *   end() 给每个客户端重发世界流（同一个 Player、同一条连接，不会掉线）。
 *   单机下 begin()/end() 退化为原来的 logic.reset()，所以单机流程行为不变。
 *
 * 铁律：begin() 期间 state.isMenu() 为 true，因此整段必须在**同一个调用栈/同一帧内**跑完，
 *       绝不能让帧在 isMenu 上收尾（否则下一帧就进关服分支）。
 */
public class RougeLobby {

    /** 首选大厅图（放在 assets/rougeMaps/lobby.msav 即可换成专用大厅） */
    public static final String HUB_MAP = "lobby";
    /** 没有专用大厅图时的兜底地图 */
    public static final String HUB_FALLBACK = "testingGrounds";

    private static boolean initialized = false;
    private static Map hubCache;
    private static Table hudTable;
    private static float stateRetryTimer = 0f;

    public static void init(){
        if(initialized) return;
        initialized = true;

        Events.on(WorldLoadEvent.class, e -> {
            // 客户端：世界已经加载完，主动要一次大厅状态（房主 PlayerJoin 时也会推，这是双保险）
            if(RougeNet.isClient()){
                RougeNet.requestState();
            }

            Time.run(5f, () -> {
                if(!RougeNet.isMultiplayer()) return;

                if(RougeNet.inLobby){
                    if(!RougeCampaignScreen.isShown()) RougeCampaignScreen.show();
                }else{
                    RougeCampaignScreen.hide();
                }
                setHud(RougeNet.inLobby);
            });
        });

        Events.run(Trigger.update, RougeLobby::tick);
    }

    // ==================== 大厅图 ====================

    /** 取得（并缓存）大厅地图对象；找不到返回 null */
    public static Map getHubMap(){
        if(hubCache != null) return hubCache;

        String[] candidates = {HUB_MAP, HUB_FALLBACK};
        for(String name : candidates){
            Fi file = RougeMaps.getMapFile(name);
            if(file == null || !file.exists()) continue;

            try{
                // MapIO.createMap 会把 file 一起塞进 Map（world.loadMap 内部读 map.file），这里不需要再反射设置
                hubCache = MapIO.createMap(file, true);
                Log.info("[RougeLobby] hub map = @", name);
                return hubCache;
            }catch(Exception e){
                Log.err("[RougeLobby] failed to load hub map " + name, e);
            }
        }

        Log.err("[RougeLobby] no hub map found (looked for @.msav / @.msav)", HUB_MAP, HUB_FALLBACK);
        return null;
    }

    /** 大厅规则：关掉波次/进攻/胜负判定，保证大厅是个安全的房间 */
    public static Rules buildHubRules(Map hub){
        Rules rules = hub.rules().copy();
        rules.waves = false;
        rules.attackMode = false;
        rules.spawns = new Seq<>();
        rules.pvp = false;
        rules.sector = null;
        rules.editor = false;
        rules.canGameOver = false;
        rules.waveTimer = false;
        rules.pauseDisabled = false;
        rules.infiniteResources = false;
        // 注意：绝不能在这里改 defaultTeam！
        // World.loadMap(map, checkRules) 会用 checkRules.defaultTeam 校验"这张图有没有该阵营的核心"，
        // 不匹配就会把地图标成 invalid 并 post 一个 state.set(State.menu)
        //（联机时那就是"刚开服就被原版关服"）。用地图自己的 defaultTeam，它才有核心。
        // 服务器列表里显示的模式名（NetworkIO.writeServerData 会带上它）
        rules.modeName = "Rouge 联机大厅";
        return rules;
    }

    /** 大厅里不该有敌军炮塔/核心（默认队伍与废墟保留） */
    private static void sanitizeHub(){
        try{
            Seq<Building> remove = new Seq<>();
            for(Building build : Groups.build){
                if(build.team != state.rules.defaultTeam && build.team != Team.derelict){
                    remove.add(build);
                }
            }
            for(Building build : remove){
                if(build.tile != null) build.tile.remove();
            }
        }catch(Throwable t){
            Log.err("[RougeLobby] sanitizeHub failed", t);
        }
    }

    /** 大厅图若没有核心，放一个，保证玩家有落脚点（失败也不影响大厅本身） */
    private static void ensureHubSpawn(){
        try{
            if(state.rules.defaultTeam.data().cores.size > 0) return;

            int cx = world.width() / 2, cy = world.height() / 2;
            for(int r = 0; r < 60; r++){
                for(int x = Math.max(0, cx - r); x <= Math.min(world.width() - 1, cx + r); x++){
                    for(int y = Math.max(0, cy - r); y <= Math.min(world.height() - 1, cy + r); y++){
                        Tile tile = world.tile(x, y);
                        if(tile != null && !tile.block().solid && !tile.floor().isLiquid){
                            tile.setBlock(Blocks.coreShard, state.rules.defaultTeam);
                            Log.info("[RougeLobby] hub had no core; placed one at @,@", x, y);
                            return;
                        }
                    }
                }
            }
        }catch(Throwable t){
            Log.err("[RougeLobby] ensureHubSpawn failed", t);
        }
    }

    // ==================== 开房 ====================

    /** 主菜单入口：开一个 Rouge 联机大厅（房主） */
    public static void hostLobby(){
        if(net.client()){
            ui.showInfoToast("[scarlet]你正作为客户端联机，不能开房[]", 3f);
            return;
        }
        if(net.active()){
            ui.showInfoToast("[scarlet]已经在一个联机会话里了[]", 3f);
            return;
        }

        Map hub = getHubMap();
        if(hub == null){
            ui.showInfoToast("[scarlet]找不到大厅地图：assets/rougeMaps/" + HUB_MAP + ".msav 或 " + HUB_FALLBACK + ".msav[]", 4f);
            return;
        }

        ui.loadAnd("正在开启 Rouge 联机大厅...", () -> {
            try{
                doHostLobby(hub);
            }catch(Throwable t){
                Log.err("[RougeLobby] hostLobby failed", t);
                ui.showException("[scarlet]开服失败[]", t);
                try{
                    if(net.active()) net.closeServer();
                }catch(Throwable ignored){
                }
                state.set(State.menu);
            }
        });
    }

    /** 实际开房流程（异常由 hostLobby 兜底） */
    private static void doHostLobby(Map hub){
        Rules rules = buildHubRules(hub);

        // 诊断日志：大厅被原版判非法时，这一行能看出是哪张图、缺哪个阵营的核心
        Log.info("[RougeLobby] hosting: hub='@' @x@ defaultTeam=@", hub.name(), hub.width, hub.height, rules.defaultTeam.name);

        // 先建世界、再开服：这样帧永远不会以 state.isMenu() 收尾
        logic.reset();
        world.loadMap(hub, rules);

        // loadMap 的第二个参数只用于校验，地图不合法时它会自己把 state 打回 menu 并 app.post。
        // 如果不在这里提前返回，那个 post 会在 net.host() 之后落地 → menu + 开着的服务器 → 原版关服。
        if(world.isInvalidMap()){
            Log.err("[RougeLobby] hub map '@' invalid: no core for team @. 放一个 assets/rougeMaps/lobby.msav，或改 RougeLobby.HUB_FALLBACK",
                hub.name(), rules.defaultTeam.name);
            ui.showInfoToast("[scarlet]大厅地图无效（缺 " + rules.defaultTeam.name + " 阵营核心），无法开房[]", 6f);
            state.set(State.menu);
            return;
        }

        state.rules = rules;
        state.map = hub;
        state.wave = 0;
        state.gameOver = false;
        logic.play();
        sanitizeHub();
        ensureHubSpawn();

        try{
            net.host(Core.settings.getInt("port", port));
        }catch(IOException e){
            ui.showException("[scarlet]开服失败[]", e);
            state.set(State.menu);
            return;
        }

        player.admin = true;
        Events.fire(new HostEvent());

        RougeSave.isInRougeMode = true;
        RougeSave.currentSectorName = null;
        RougeSave.currentEvacWave = 0;
        if(RougeSave.treeSeed == 0) RougeSave.treeSeed = System.nanoTime();
        RougeTree.generateTree(RougeSave.treeSeed, 10);
        RougeSave.save();

        // 开局跑一次协议自检：日志出现 "[RougeNet] selfTest PASSED" 才说明编解码字段顺序是对的
        RougeNet.selfTest();

        RougeNet.inLobby = true;
        RougeNet.selectedId = -1;
        RougeNet.evacAvailable = false;
        RougeNet.evacWave = 0;
        RougeNet.wasInSession = true;

        setHud(true);

        RougeNet.sendStateToAll();
        RougeCampaignScreen.show();

        ui.showInfoToast("[accent]大厅已开启[]：其他人用「加入游戏」连接即可", 6f);
    }

    // ==================== 换图 ====================

    /**
     * 进入一张地图（Rouge 节点）。房主执行；单机下与原来的行为等价。
     * 客户端不调用这个方法，它们靠 WorldReloader 发来的世界流换图。
     */
    public static void enterMap(Map map, Rules rules, String sectorName, int evacWave){
        if(RougeNet.isClient()) return;

        // 和原版 Control.playSector 一样用 ui.loadAnd 延后几 tick 再换世界，
        // 避免在场景输入回调里直接清 Groups / 替换 GameState
        ui.loadAnd(() -> {
            try{
                doEnterMap(map, rules, sectorName, evacWave);
            }catch(Throwable t){
                Log.err("[RougeLobby] enterMap failed", t);
                ui.showInfoToast("[scarlet]进入地图失败，详情见日志[]", 4f);
                if(net.active()){
                    // 别让帧以 state.isMenu() 收尾（会关服）；然后把全队带回大厅
                    state.set(State.playing);
                    returnToLobby();
                }else{
                    state.set(State.menu);
                    RougeCampaignScreen.show();
                }
            }
        });
    }

    /** 实际换图流程（异常由 enterMap 兜底） */
    private static void doEnterMap(Map map, Rules rules, String sectorName, int evacWave){
        Log.info("[RougeLobby] enterMap: @ (evacWave=@)", sectorName, evacWave);

        // 先告诉客户端“离开大厅”，让它们的树界面先收掉
        RougeNet.broadcastLobby(false);

        RougeSave.currentSectorName = sectorName;
        RougeSave.currentEvacWave = evacWave;
        RougeNet.evacAvailable = false;
        RougeNet.evacWave = 0;

        WorldReloader reloader = new WorldReloader();
        reloader.begin();

        world.loadMap(map, rules);
        if(world.isInvalidMap()){
            throw new IllegalStateException("Rouge: invalid map " + (map == null ? "?" : map.plainName())
                + " (no core for team " + rules.defaultTeam.name + ")");
        }

        state.rules = rules;
        state.map = map;
        state.wave = 1;
        state.wavetime = rules.waveSpacing;
        state.gameOver = false;
        logic.play();

        reloader.end();
        state.set(State.playing);

        if(RougeNet.isHost()) RougeNet.sendStateToAll();
    }

    /**
     * 撤离成功 / 失败后把全队带回大厅。
     * 只有联机房主会执行；单机保持原来的“回主菜单 + 显示树界面”行为。
     */
    public static void returnToLobby(){
        if(!RougeNet.isMultiplayer() || RougeNet.isClient()) return;

        Map hub = getHubMap();
        if(hub == null){
            RougeSave.currentSectorName = null;
            RougeCampaignScreen.show();
            return;
        }

        ui.loadAnd(() -> {
            try{
                doReturnToLobby(hub);
            }catch(Throwable t){
                Log.err("[RougeLobby] returnToLobby failed", t);
                ui.showInfoToast("[scarlet]返回大厅失败，详情见日志[]", 4f);
                // 关键：绝不能让帧以 state.isMenu() 收尾
                state.set(State.playing);
            }
        });
    }

    /** 实际回大厅流程（异常由 returnToLobby 兜底） */
    private static void doReturnToLobby(Map hub){
        Log.info("[RougeLobby] returnToLobby");

        Rules rules = buildHubRules(hub);

        RougeSave.currentSectorName = null;
        RougeSave.currentEvacWave = 0;
        RougeNet.inLobby = true;
        RougeNet.selectedId = -1;
        RougeNet.evacAvailable = false;
        RougeNet.evacWave = 0;

        WorldReloader reloader = new WorldReloader();
        reloader.begin();

        world.loadMap(hub, rules);
        if(world.isInvalidMap()){
            throw new IllegalStateException("Rouge: invalid hub map '" + hub.name()
                + "' (no core for team " + rules.defaultTeam.name + ")");
        }

        state.rules = rules;
        state.map = hub;
        state.wave = 0;
        state.gameOver = false;
        logic.play();
        sanitizeHub();
        ensureHubSpawn();

        reloader.end();
        state.set(State.playing);

        setHud(true);

        if(RougeNet.isHost()) RougeNet.sendStateToAll();
        RougeCampaignScreen.show();
    }

    // ==================== 大厅 HUD 按钮 ====================

    private static void tick(){
        // 会话结束后恢复本地存档（放在最前面：单机时也要能跑）
        RougeNet.checkSessionEnded();

        if(!RougeNet.isMultiplayer()) return;

        // 用 hudTable 是否存在作为“实际状态”，这样 hudGroup 还没准备好时会自动重试
        setHud(RougeNet.inLobby && state.isGame() && !state.isMenu());

        // 客户端首连没收到状态时（例如 hello 发得太早）每秒重试一次请求
        if(RougeNet.isClient() && !RougeNet.stateReceived){
            stateRetryTimer += Time.delta;
            if(stateRetryTimer >= 60f){
                stateRetryTimer = 0f;
                RougeNet.requestState();
            }
        }else{
            stateRetryTimer = 0f;
        }
    }

    /** 大厅 HUD 按钮：显示/隐藏（内部按实际状态判断，可每帧安全调用） */
    private static void setHud(boolean show){
        if(show == (hudTable != null)) return;

        try{
            if(show){
                if(ui == null || ui.hudGroup == null) return;   // 等下一帧再试

                Table table = new Table(Styles.black5);
                table.top().right();
                table.button("[accent]模式指挥[]", Styles.defaultt, RougeCampaignScreen::show).size(150, 46).pad(4);
                if(RougeNet.isClient()){
                    // 联调/救急：手动再向房主要一次大厅状态
                    table.button("[lightgray]重新同步[]", Styles.defaultt, RougeNet::requestState).size(120, 46).pad(4);
                }
                table.pack();
                table.setPosition(Core.graphics.getWidth() - table.getWidth() - 10,
                    Core.graphics.getHeight() - table.getHeight() - 60);
                ui.hudGroup.addChild(table);
                hudTable = table;
            }else{
                hudTable.remove();
                hudTable = null;
            }
        }catch(Throwable t){
            Log.err("[RougeLobby] setHud failed", t);
        }
    }
}
