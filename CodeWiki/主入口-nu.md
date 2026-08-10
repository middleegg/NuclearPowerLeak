# 主入口类 nu

> **文件路径**：[nu.java](file:///d:/NuclearPowerLeak-master/src/Npl/nu.java)  
> **包名**：`Npl`  
> **父类**：`mindustry.mod.Mod`  
> **mod.hjson 对应配置**：`main: "Npl.nu"`

---

## 1. 角色定位

`Npl.nu` 是整个 NuclearPowerLeak Mod 的**主入口类**。Mindustry 启动加载 Mod 时：

1. 读取 `mod.hjson` 的 `main` 字段得到 `"Npl.nu"`
2. 通过反射 `Class.forName("Npl.nu").newInstance()` 实例化
3. 调用构造方法 `nu()`
4. 在合适的游戏阶段回调 `loadContent()` 等生命周期方法

> 🔍 **为什么叫 `nu`？**  
> `mod.hjson` 中 `name: "nu"` 是 Mod 的短 ID（内部名），主类同名便于记忆。在资源加载、Sprite 查找、Bundle 键名中，所有 Mod 专属内容均以 `nu-` 前缀命名。

---

## 2. 类结构

```java
public class nu extends Mod {
    public nu()         { /* 构造 + 事件监听注册 */ }
    public void loadContent() { /* 内容加载入口 */ }
}
```

---

## 3. 生命周期时序

```
Mindustry 启动流程
    │
    ▼
① 反射实例化 nu 类 → 执行 nu() 构造方法
    │
    ├── 输出日志："Loaded ExampleJavaMod constructor."
    │
    └── 注册事件监听器：Events.on(ClientLoadEvent, e -> { ... })
          │  (此时不执行，仅注册)
          │
          ▼  （等到游戏主菜单加载完成后）
          └── Time.runTask(10f, () -> { 显示 frog 对话框 })
    │
    ▼
② 引擎调用 loadContent()
    │
    ├── NuItems.load()     ← 16 种物品注册
    ├── NuLiquid.load()    ← 4 种液体注册
    ├── Azer.load()        ← 星球 + 规则注册
    └── NuBlocks.load()    ← 2 种方块注册
    │
    ▼
③ 内容完成后，ClientLoadEvent 触发（仅客户端）
    │
    └── 延迟 10 tick（约 166 ms） → 弹出 "frog" 欢迎对话
```

---

## 4. 构造方法详解

### 4.1 完整代码

[nu.java#L15-L29](file:///d:/NuclearPowerLeak-master/src/Npl/nu.java#L15-L29)

```java
public nu(){
    Log.info("Loaded ExampleJavaMod constructor.");
    
    // 监听：游戏客户端加载完成事件
    Events.on(ClientLoadEvent.class, e -> {
        Time.runTask(10f, () -> {
            BaseDialog dialog = new BaseDialog("frog");
            dialog.cont.add("behold").row();
            // 资源名格式：modId-spriteName → "nu-frog"
            dialog.cont.image(Core.atlas.find("nu-frog")).pad(20f).row();
            dialog.cont.button("OK", dialog::hide).size(100f, 50f);
            dialog.show();
        });
    });
}
```

### 4.2 要点解析

| 代码 | 说明 |
|------|------|
| `Log.info(...)` | Arc 框架日志。Mindustry 控制台中输出 INFO 级日志 |
| `Events.on(ClientLoadEvent.class, e -> ...)` | 注册客户端加载完成事件。仅 PC/Android 客户端触发，无头服务器不触发 |
| `Time.runTask(10f, () -> ...)` | 延迟 10 tick（60 tick/s ≈ 0.17s）执行，避免与引擎的 UI 初始化竞争 |
| `new BaseDialog("frog")` | Arc Scene2D 对话框，标题为 "frog" |
| `dialog.cont.add("behold")` | 添加文本行（含义："看哪！"） |
| `Core.atlas.find("nu-frog")` | 在合并图集中查找 Mod 专属 Sprite。格式：`{modId}-{spriteName}` |
| `.pad(20f)` | 四周留白 20 px |
| `.row()` | UI 换行 |
| `button("OK", dialog::hide)` | 点击按钮时调用 `dialog.hide()` 关闭 |
| `.size(100f, 50f)` | 按钮尺寸：100×50 px |

### 4.3 资源依赖

对话框使用的青蛙图标对应磁盘文件：
- `assets/sprites/frog.png`（或打包后的图集 `nu-frog`）

> 💡 **为什么延迟 10 tick？**  
> ClientLoadEvent 触发时 UI 根表可能还在布局阶段，立即创建对话框有时会被下一帧重建覆盖。延迟 10 tick 确保主界面稳定。

---

## 5. loadContent() 方法详解

### 5.1 代码

[nu.java#L31-L37](file:///d:/NuclearPowerLeak-master/src/Npl/nu.java#L31-L37)

```java
@Override
public void loadContent() {
    NuItems.load();     // 1. 先注册物品（方块/星球会用到）
    NuLiquid.load();    // 2. 注册液体
    Azer.load();        // 3. 注册星球（会用到 NuItems.bigIron 做初始物资）
    NuBlocks.load();    // 4. 注册方块（会用到物品、液体、配方等）
}
```

### 5.2 加载顺序的必要性

```
NuItems.load() 必须最先：
    │
    ├── Azer.load() 中使用 NuItems.bigIron 设置初始物资
    └── NuBlocks.load() 中使用 NuItems.bigIron/magent 作为建材和配方
            │
            └── ConfigurableBlock 初始化也需要 Item 类型已注册
                （initCapacities() 需要 Vars.content.items().size）

NuLiquid.load() 必须在 NuBlocks.load() 之前：
    若方块配方中添加了液体消耗/产出，需先保证 Liquid 实例可用
```

> ✅ **推荐的 Mindustry Mod 标准加载顺序**：  
> Items → Liquids → Units → Blocks → Planets → TechTree  
> 本项目按 Items → Liquids → Planets → Blocks 顺序，Planet 在 Blocks 之前是因为 Azer.ruleSetter 不依赖方块。

---

## 6. 导入清单

[nu.java#L1-L13](file:///d:/NuclearPowerLeak-master/src/Npl/nu.java#L1-L13)

| Import | 用途 |
|--------|------|
| `arc.*` / `arc.util.*` | Core、Log、Time 等 Arc 框架核心 |
| `mindustry.game.EventType.*` | ClientLoadEvent 等事件 |
| `mindustry.mod.*` | Mod 基类 |
| `mindustry.ui.dialogs.*` | BaseDialog 对话框 |
| `Npl.content.*` | 所有内容注册器（NuItems、NuLiquid 等） |
| `Npl.newSth.NewItemsType` | 扩展物品类型（未来可在主类中直接使用） |
| `arc.graphics.Color` | 颜色支持 |
| `arc.struct.Seq` | Arc 集合（当前未直接使用，为扩展预留） |
| `mindustry.type.Item` | Item 基础类型 |

---

## 7. mod.hjson 与主类的关系

```hjson
displayName: "NuclearPowerLeak"   // 显示名
name: "nu"                        // 内部 ID（资源前缀 modId）
author: "Zero(middle_egg), ..."
main: "Npl.nu"                    // ★ 主类全限定名
description: "..."
version: 0.1
minGameVersion: 158               // 最低 Mindustry 版本（v158）
java: true                        // 声明这是 Java Mod（启用 ClassLoader）
```

> ⚠️ **错误排查**：若 `main` 字段写错（如大小写、包名），Mindustry 会在加载时抛出 `ClassNotFoundException` 并跳过该 Mod。

---

## 8. 扩展建议（主类增强）

当前主类较为精简，后续可添加的钩子：

### 8.1 注册服务器端内容

```java
// 在构造方法中添加：
Events.on(ServerLoadEvent.class, e -> {
    Log.info("NuclearPowerLeak loaded on server.");
});
```

### 8.2 世界生成事件

```java
Events.on(WorldLoadEvent.class, e -> {
    if (state.rules.sector != null && state.rules.sector.planet == Azer.Azer) {
        // 进入 Azer 扇区时执行特殊逻辑
        // 例如：初始核心旁生成一个辐射源
    }
});
```

### 8.3 注册科技树

```java
// loadContent() 最后：
TechTree.load();  // 自定义 TechTree 类，串联物品→方块的解锁链路
```

### 8.4 启动时调用 NuUI.init()

当前 `NuUI.init()` 未被调用，可接入主类：
```java
Events.on(ClientLoadEvent.class, e -> {
    Time.runTask(10f, () -> {
        // ...原 frog 对话框...
        NuUI.init();  // 初始化 HUD 面板
    });
});
```

---

## 9. 调试技巧

| 目标 | 方法 |
|------|------|
| 验证加载顺序 | 在每个 `Xxx.load()` 开头加 `Log.info("Loading Xxx...")`，查看日志输出顺序 |
| 验证 ClientLoadEvent 是否触发 | 将 frog 对话框中的文字改成带 Mod 名的个性化提示 |
| 调试缺失 Sprite | 临时将 `Core.atlas.find("nu-frog")` 改为 `Core.atlas.find("error")`，确保占位图机制正常 |
| 热更测试 | 使用 Gradle `gradlew jar` 后在 Mindustry 中重新导入 Mod（无需重启游戏，利用 `afterPatch()` 钩子） |

---

**🔗 相关文档**：
- [项目总览](./项目总览.md)
- [系统架构](./系统架构.md)
- [UI 模块 NuUI](./UI-NuUI.md)
- [构建与运行](./构建与运行.md)
