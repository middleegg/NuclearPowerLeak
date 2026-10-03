# Rouge 联机大厅（房主主导的房间）

> 实现文件：`src/Npl/Rouge/RougeNet.java`（网络层）、`src/Npl/Rouge/RougeLobby.java`（大厅与换图）。
> 本文记录**为什么这么设计**以及**怎么测**，避免以后重复推导。

## 1. 为什么大厅必须是一张"活着的地图"

原版 `NetServer.update()` 只要看到 `net.server() && state.isMenu()` 就会主动关服
（`ui.loadfrag.show("@server.closing")` → `net.closeServer()`）。
所以"大家一起待在主菜单里看战役树"是不成立的：
**大厅 = 一张大厅图（活着的 `State.playing`）+ 盖在上面的 `RougeCampaignScreen` 树界面。**

铁律：`WorldReloader.begin()` 期间 `logic.reset()` 让 `state.isMenu()` 为 true，
所以 `begin() → world.loadMap → logic.play() → end()` 必须在同一个调用栈内跑完，
任何时刻都不能让帧以 `isMenu` 收尾（`ui.loadAnd` 的 7 tick 延后把整段包在一起，和原版 `Control.playSector` 一致）。

## 2. 为什么换图不会掉线

房主换图复用原版 `mindustry.net.WorldReloader`（`Control.playSector` 内部就是这套）：

- `begin()`：收集所有客户端 `Player` → `Call.worldDataBegin()`
- 中间：`world.loadMap(...)`（大厅图或节点图）
- `end()`：对每个客户端 `p.reset()` + `netServer.sendWorldAndAssets(p)` → `WorldStream`

客户端在**同一条连接**上重载世界（`NetworkIO.loadWorld`：`Groups.clear()` → 把同一个 `Player` 重新绑定），
socket / NetConnection / Player / admin 全程保留。单机下 `begin()/end()` 退化为 `logic.reset()`，行为与原来一致。

## 3. 为什么同步"树拓扑"而不是 seed

`RougeTree.generateTree(seed)` 依赖 `RougeMaps.loadAvailableMaps()`，
而它是 `dir.list()` 的文件系统遍历顺序 + 开发机绝对路径 `G:\NuclearPowerLeak\assets\rougeMaps`。
同一个 seed 在两台机器上会生成**不同的树**，所以客户端根本不重建树：
房主通过 `RougeTree.writeTo/readFrom` 把拓扑 + 锁定/完成/折叠状态整体发过去，
客户端也完全不碰本机地图文件（地图靠世界流过来）。

## 4. 协议（原版给 mod 留的自定义包通道）

| 通道 | 方向 | 内容 |
|---|---|---|
| `npl-rouge-state` | 房主 → 客户端 | 完整状态：`inLobby`、选中节点、撤离窗口、`currentSectorName/currentEvacWave` + `RougeSave` 全字段 + `RougeTree` 拓扑 |
| `npl-rouge-event` | 房主 → 客户端 | 小事件：`EV_SELECT`(选中) / `EV_LOBBY`(进出大厅) / `EV_EVAC`(撤离窗口) |
| `npl-rouge-hello` | 客户端 → 房主 | 请求补发状态（客户端世界加载完时发一次；房主 `PlayerJoin` 时也会推，双保险） |

发送：`Call.serverBinaryPacketReliable(String, byte[])` / `Call.clientBinaryPacketReliable(String, byte[])`（都是广播重载；
注意 `Loc.client` 的"单目标"重载参数是 `NetConnection`，传 null 不是广播）。
接收：客户端 `netServer.addBinaryPacketHandler(...)`、服务端 `netClient.addBinaryPacketHandler(...)`
（这两张表挂在"对侧"对象上是原版既有约定，不是笔误）。

## 5. 权威划分（按需求确认）

- 房主：建树、选中、出发、撤离、失败判定、写 `rouge_save.bin`。
- 客户端：只镜像。**不写存档**（`RougeSave.save()` 在 `net.client()` 时直接返回）、
  **不重复给核心加物品**（世界流里已经带了核心物品）、不 `generateTree`、不执行 `completeNode`/分支选择。
- 客户端退出联机会话回主菜单时重新 `RougeSave.load()`，避免把房主进度写进自己的存档。

## 6. 大厅图

优先 `assets/rougeMaps/lobby.msav`，没有就用 `RougeLobby.HUB_FALLBACK`（默认 `testingGrounds`）。
载入后：清掉非默认队伍/非废墟的建筑、关掉波次与胜负判定、没核心就在地图中心附近补一个 `coreShard`。

⚠️ **不要在 hub rules 里改 `defaultTeam`**：`World.loadMap(map, checkRules)` 是用
`checkRules.defaultTeam` 校验"这张图有没有该阵营的核心"的（`World.java: state.teams.cores(checkRules.defaultTeam).size == 0`
→ `invalidMap = true` + `Core.app.post(() -> state.set(State.menu))`）。
在联机开服流程里，那个 post 会在 `net.host()` 之后落地 → menu + 开着的服务器 → 原版关服。
所以 `hostLobby/enterMap/returnToLobby` 都在 `world.loadMap` 之后立刻检查 `world.isInvalidMap()` 并提前失败。

## 7. 双开测试清单

0. **协议自检**：房主第一次开房时日志会打印
   `[RougeNet] selfTest PASSED (lobby=… sector=… nodes=… branches=… tech=… items=…)`。
   它把状态编码 → 打乱内存态 → 解码 → 比对关键字段，专门抓"手写 Writes/Reads 字段顺序错位"这种最难发现的 bug。
   若出现 `FAILED` / `mismatch`，先把那一行贴出来，不用急着测联机。
1. 房主：主菜单 → Rouge 模式 → **联机大厅（开房）**；日志应有 `[RougeLobby] hub map = ...`、`[RougeNet] initialized`。
2. 客户端：原版 **加入游戏** 连进来 → 应自动弹出同一个树界面，房主拖动/点击节点时高亮同步；
   客户端还应有 `[RougeNet] state applied: inLobby=true sector=… nodes=…`，HUD 右上角除「模式指挥」外还有「重新同步」按钮（手动补状态用）。
3. 房主点节点 → 出发：两端一起进入同一张地图，**不掉线**（客户端只是重载世界）。
4. 房主按撤离（客户端显示"等待房主撤离…"）：两端一起回到大厅图 + 树界面。
5. 故意失败（送掉核心）：同样回到大厅。
6. 单机回归：`进入模拟` → 选节点 → 出发 → 撤离 → 回主菜单，行为与改动前一致。

## 7.5 失败对照表（测试时对号入座）

| 现象 | 先看哪条日志 | 处理 |
|---|---|---|
| 点开房后被打回主菜单 / 服务器秒关 | `[RougeLobby] hub map '@' invalid: no core for team @` | 大厅图缺该阵营核心：放一个 `assets/rougeMaps/lobby.msav`，或改 `RougeLobby.HUB_FALLBACK` |
| 开房成功但服务器列表里别人进不来 | `[Server] Opened a server on port ...` 是否出现 / 端口占用 | `net.host` 失败会有 `开服失败` 弹窗；换端口（`settings` 里的 `port`） |
| 客户端连上但看不到树界面 | 客户端有没有 `[RougeNet] state applied: inLobby=true …` | 没有 → 点 HUD 的「重新同步」；有但没显示 → 看那行里的 `sector` 是否为空、`state.isGame()` 是否为假 |
| 客户端卡在加载画面 | 房主日志 `[RougeLobby] enterMap failed` | 节点图非法（同样会打印缺哪个阵营核心）；修图或换节点 |
| 出发后客户端掉线 | 房主日志有没有 `enterMap failed` | `WorldReloader.end()` 之后的流被中断；把日志贴出来 |
| 撤离后没回大厅 | 房主日志 `[RougeLobby] returnToLobby failed` | 大厅图在返回时被判非法；看异常里的图名与阵营 |
| 客户端回来后物资/进度不对 | 客户端有没有 `session ended; local save restored` | 客户端在别人房间里只镜像、不落盘；退出后会自动还原本地存档 |
| 单机行为变了 | `[RougeNet]` 相关日志在单机应当**完全不出现**（除 initialized） | 若出现，说明某处联机分支守卫漏了 |

## 8. 改动清单（方便 review / 回退）

| 文件 | 内容 |
|---|---|
| `src/Npl/Rouge/RougeNet.java`（新） | 自定义包通道、状态编解码、`selfTest()`、`checkSessionEnded()` |
| `src/Npl/Rouge/RougeLobby.java`（新） | 大厅图、开房、`WorldReloader` 换图、大厅 HUD |
| `src/Npl/Rouge/RougeTree.java` | `writeTo/readFrom`（树拓扑同步） |
| `src/Npl/Rouge/RougeSave.java` | `writeTo/readFrom/readCount/clearProgress`；客户端不落盘；`save()` 触发同步；`load()` 失败清空 |
| `src/Npl/Rouge/RougeCampaignScreen.java` | 客户端只读、选中广播、`isShown/hide`、出发走 `RougeLobby.enterMap`、藏品规则修复 |
| `src/Npl/Rouge/RougeGameControl.java` | 客户端守卫、房主专属撤离、撤离/失败回大厅 |
| `src/Npl/Rouge/RougeMaps.java` | `getMapFile(String)` |
| `src/Npl/Rouge/RougeMenu.java` / `src/Npl/nu.java` | 新入口 + 初始化 |

## 9. 已知限制（Phase 2 候选）

- 客户端不能发起撤离/选择（当前是"只有房主"策略）。
- 藏品 / 科技等级仍是房主本地，未按玩家拆分同步。
- 房主退出到主菜单时客户端由原版机制断开，没有房主迁移。
- headless 专用服务端跑不了（`Control`/UI 在 headless 不存在）。
- `RougeCampaignScreen` 里用反射读 `Map.tiles` 判断敌人核心的代码在 v160.5 永远抛 `NoSuchFieldException`
  （`Map` 已无 `tiles` 字段），因此 `rules.attackMode` 恒为 false —— 既有问题，未改动。
