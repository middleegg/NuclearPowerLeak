# UI 模块 NuUI

> **文件路径**：[NuUI.java](file:///d:/NuclearPowerLeak-master/src/Npl/NuUI.java)  
> **包名**：`Npl`  
> **类型**：UI 初始化工具类  
> **状态**：已实现但主类中**尚未接入调用**

---

## 1. 模块职责

`NuUI` 是自定义 HUD（抬头显示）面板的**挂载器**。目标：
- 在游戏主界面右下角添加一个小面板，显示 Mod 的运行状态指示器
- 当前显示内容：`bigIron`（生铁）图标 + 文本 "生铁: 已加载/未加载"
- **降级挂载策略**：优先挂到原版 HUD 指定位置，找不到则 fallback 到全屏 Table

---

## 2. 完整代码

[NuUI.java](file:///d:/NuclearPowerLeak-master/src/Npl/NuUI.java)

```java
package Npl;

import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import Npl.content.NuItems;
import arc.Core;
import static mindustry.Vars.ui;

public class NuUI {
    public static void init() {
        Table panel = new Table();
        panel.setBackground(Tex.buttonEdge4);   // 使用原版按钮边框背景
        panel.defaults().pad(4);               // 默认组件留白 4px

        // 左：生铁图标（若未加载则使用占位图）
        if (NuItems.bigIron != null) {
            panel.image(NuItems.bigIron.uiIcon).size(32);
        } else {
            panel.image(Core.atlas.find("clear")).size(32);
        }
        // 右：文本标签（lambda 动态更新）
        panel.label(() -> "生铁: " + (NuItems.bigIron != null ? "已加载" : "未加载"));

        // ============= 挂载逻辑（含降级） =============
        Table overlay = ui.hudGroup.find("overlaymarker");
        Table bottom  = overlay != null ? overlay.find("bottom") : null;
        if (bottom != null) {
            // 主路径：挂到底部 HUD 的容器里，靠右靠下
            bottom.add(panel).right().bottom().pad(10);
        } else {
            // 降级路径：创建全屏 Table，设置 fillParent
            Table mainTable = new Table();
            mainTable.setFillParent(true);
            mainTable.bottom().right();                 // 对齐方式
            mainTable.add(panel).pad(10);               // 添加小面板
            ui.hudGroup.addChild(mainTable);             // 挂载到 HUD 根节点
        }
    }
}
```

---

## 3. 核心元素解析

### 3.1 面板 `panel`

| 属性 | 值 | 说明 |
|------|-----|------|
| 背景 | `Tex.buttonEdge4` | 原版 Mindustry 的带边框按钮九切片纹理，视觉风格统一 |
| 组件留白 | `pad(4)` | 每个组件默认 4px 间距 |
| 布局方向 | 横向（默认） | 左图标 + 右文本 一行排列 |

### 3.2 图标显示（含降级策略）

```java
if (NuItems.bigIron != null) {
    panel.image(NuItems.bigIron.uiIcon).size(32);  // 32×32 图标
} else {
    // 占位：Core.atlas.find("clear") 是透明像素，避免空图标崩溃
    panel.image(Core.atlas.find("clear")).size(32);
}
```

> 💡 **设计说明**：`NuItems.bigIron` 理论上只要 `NuItems.load()` 被正确调用就不会为 null。此检查是防御式编程，应对：
> - 主类中 NuUI 早于 loadContent 初始化
> - 热更加载失败导致内容未注册

### 3.3 动态文本标签

```java
panel.label(() -> "生铁: " + (NuItems.bigIron != null ? "已加载" : "未加载"));
```

**关键点**：传入 `Supplier<CharSequence>`（lambda）而非普通字符串。  
效果：每帧 UI 重新布局时都会执行该 lambda，实现**状态自动刷新**。若未来改成动态显示"当前生铁库存"等实时数据，可直接在 lambda 中读取变量无需额外刷新。

---

## 4. HUD 挂载策略详解

Mindustry 的 HUD 结构（简化）：

```
ui.hudGroup (Group - HUD 根)
 ├─ "overlaymarker" (Table)    ← 覆盖标记层容器
 │   ├─ "top"                  ← 顶部区域（波次、资源等）
 │   └─ "bottom"               ← 底部区域（快捷栏、核心血条等）✅ 目标位置
 └─ (其他 HUD 层)
```

### 4.1 主路径：插入原版 HUD 容器

```java
Table overlay = ui.hudGroup.find("overlaymarker");
Table bottom  = overlay != null ? overlay.find("bottom") : null;
if (bottom != null) {
    bottom.add(panel).right().bottom().pad(10);
}
```

- 对齐：`right().bottom()` → 右下对齐
- 留白：`pad(10)` → 距屏幕边缘 10 px，避免贴边
- 优点：随 HUD 整体缩放、隐藏（过场动画时自动隐藏）

### 4.2 降级路径：自建 fillParent Table

```java
Table mainTable = new Table();
mainTable.setFillParent(true);       // 占满整个屏幕
mainTable.bottom().right();          // 内部子元素右下对齐
mainTable.add(panel).pad(10);
ui.hudGroup.addChild(mainTable);     // 直接挂到 HUD 根节点
```

适用场景：
- 其他 Mod 移除/改名了 `overlaymarker` 或 `bottom`
- Mindustry 新版本重构了 HUD 结构
- 玩家使用了自定义 HUD Mod

**注意**：此路径下若屏幕尺寸变化（如窗口缩放），`setFillParent(true)` 的 Table 会自动重新布局，自适应。

---

## 5. 接入主类

当前 `NuUI.init()` **未被调用**。需要在主类中接入：

```java
// 建议位置：nu.java 构造方法内的 ClientLoadEvent 回调中
Events.on(ClientLoadEvent.class, e -> {
    Time.runTask(10f, () -> {
        // ... 原有 frog 对话框 ...
        NuUI.init();  // ✅ 新增：初始化 HUD 面板
    });
});
```

> ⚠️ **为什么不在 loadContent() 中调用？**  
> `loadContent()` 执行时 UI 系统（`ui.hudGroup`）可能尚未创建，空指针风险。ClientLoadEvent 是 UI 就绪的明确信号。

---

## 6. 后续扩展方向

### 6.1 显示 Azer 星球实时数据

```java
// 改为显示：当前扇区污染度（模拟值）、灾厄进度
panel.label(() -> {
    if (Vars.state.rules.sector == null || Vars.state.rules.sector.planet != Azer.Azer) return "";
    int uranCount = Vars.state.teams.playerCores().first().items.get(NuItems.oriUranium);
    return "[green]Azer 扇区[] 原铀库存: " + uranCount;
});
```

### 6.2 多物品状态面板

在 panel 中每 4 个换行，展示：

| 物品 | 作用 |
|------|------|
| 瓶装磁暴 | 爆炸物倒计时 |
| 脏溶液 | 泄漏警告 |
| 磁铁 | 磁力效果激活状态 |

### 6.3 可点击按钮打开独立 Mod 面板

```java
panel.button("核工业手册", () -> {
    new ModGuideDialog().show();  // 自定义手册对话框
}).size(80, 40);
```

### 6.4 进度条式的三灾厄指示器

使用 `arc.scene.ui.ProgressBar` 可视化"三灾厄"的累积进度：
- 第一灾厄：铀的全球库存量
- 第二灾厄：铊化物在线方块数
- 第三灾厄：瓶装磁暴 + 放射性液体总量

---

## 7. UI 资源与样式参考

| 资源/样式 | 本项目使用 | 来源 |
|----------|-----------|------|
| `Tex.buttonEdge4` | 面板背景 | Mindustry 内置纹理（Tex 类） |
| `NuItems.bigIron.uiIcon` | 物品图标 | 注册物品时由 Mindustry 自动生成 |
| `Icon.cancel` / `Icon.settings` | （未来可用）通用图标 | mindustry.gen.Icon |
| `Core.atlas.find("nu-xxx")` | （可选）自定义 Sprite | assets/sprites 打包的图集 |

---

**🔗 相关文档**：
- [主入口类 nu](./主入口-nu.md)（如何接入 init()）
- [NuItems 物品模块](./content-NuItems.md)（bigIron 来源）
