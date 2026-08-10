# Azer 自定义星球模块

> **所属包**：`Npl.content`  
> **源文件**：[Azer.java](file:///d:/NuclearPowerLeak-master/src/Npl/content/Azer.java)  
> **类型**：内容注册器（静态工具类）  
> **依赖**：`NuItems`（初始物资）、`mindustry.type.Planet`（基类）、`SerpuloPlanetGenerator`（地形生成器）

---

## 1. 模块职责

`Azer` 是**自定义星球注册模块**，负责：
1. 声明并实例化名为 `Azer` 的全新星球
2. 配置星球的星图可见性、轨道参数、视觉样式
3. 设置星球专属游戏规则（波次、初始物资、AI、建造速度等）
4. 配置大气、云层、地形生成器等视觉与游戏性参数

---

## 2. Azer 星球基本信息卡

| 属性 | 值 | 说明 |
|------|-----|------|
| **星球名称** | Azer（阿泽尔） | 对应星图显示和图标文件名 |
| **围绕公转** | `Planets.sun`（太阳） | 第一行星轨道 |
| **相对大小** | 1.2f | 比 Serpulo（原版主星球，值=1）大 20% |
| **生成器种子** | 1 | 控制随机地形细节 |
| **是否可见** | true | 星图中显示 |
| **是否可访问** | true | 可点击进入游玩 |
| **解锁状态** | alwaysUnlocked = true | 无需科技树，开局即可进入 |
| **星图颜色** | `#E2FF6D`（淡黄绿） | 星图上行星小点颜色 |
| **地形生成器** | SerpuloPlanetGenerator | 复用原版 Serpulo 六边形地形 |

---

## 3. 星球实例化详解

### 3.1 Planet 构造签名

```java
Planet(String name, Planet parent, float radius, int sectorSeed)
```

Azer 的构造：[Azer.java#L23-L28](file:///d:/NuclearPowerLeak-master/src/Npl/content/Azer.java#L23-L28)

```java
Azer = new Planet(
    "Azer",              // 名称
    Planets.sun,         // 公转中心：太阳
    1.2f,                // 半径（1.2 倍标准大小）
    1                    // 地图生成种子
) {{
    // 双花括号初始化块内配置属性
}};
```

### 3.2 初始化块内配置

```java
visible = true;               // 星图可见
accessible = true;            // 允许进入
alwaysUnlocked = true;        // 无需解锁
iconColor = Color.valueOf("E2FF6D");  // 星图标记色
```

---

## 4. 视觉与环境配置

### 4.1 网格加载器

```java
meshLoader = () -> new HexMesh(this, 4);
```
- 加载六边形 (`HexMesh`) 星球表面
- 参数 `4` 为网格细分级别，值越高越精细（性能开销越大）

### 4.2 大气层与云层

| 属性 | 值 | 效果 |
|------|-----|------|
| `atmosphereColor` | `#BED462` | 大气外层光晕颜色（淡黄绿） |
| `landCloudColor` | `#EFFFB1` | 陆地云层覆盖色（浅黄绿） |
| `atmosphereRadIn` | 0.12 | 大气内半径（紧贴地表） |
| `atmosphereRadOut` | 0.45 | 大气外半径（散射层厚度） |

### 4.3 双层云层配置（cloudMeshLoader）

```java
cloudMeshLoader = () -> new MultiMesh(
    // 第一层云：淡黄绿半透明
    new HexSkyMesh(this, 2, 0.15f, 0.14f, 5, 
        Color.valueOf("BED462").a(0.75f), 2, 0.42f, 1f, 0.43f),
    // 第二层云：更亮更淡的黄绿
    new HexSkyMesh(this, 3, 0.6f, 0.15f, 5, 
        Color.valueOf("DAEA9A").a(0.75f), 2, 0.42f, 1.2f, 0.45f)
);
```

`HexSkyMesh` 参数说明（按位置）：

| 位置 | 参数名（推测） | 第一层值 | 第二层值 | 含义 |
|------|--------------|---------|---------|------|
| 1 | planet | this | this | 关联星球 |
| 2 | seed | 2 | 3 | 各自随机种子 |
| 3 | noise | 0.15 | 0.6 | 云层噪声强度 |
| 4 | radius | 0.14 | 0.15 | 云层高度半径 |
| 5 | divisions | 5 | 5 | 网格细分数 |
| 6 | color | `#BED462` × 0.75 | `#DAEA9A` × 0.75 | 云层颜色 + 透明度 |
| 7-9 | 几何参数 | 2 / 0.42 / 1.0 | 2 / 0.42 / 1.2 | 云层形状控制 |

视觉效果：**两层不同噪声和高度的云层叠加**，呈现厚重的毒雾感大气，契合"核污染星球"主题。

---

## 5. 游戏规则配置 (ruleSetter)

### 5.1 ruleSetter 概述

`Planet.ruleSetter` 是一个 lambda，在每次加载该星球的地图时执行，用于给 `Rules` 对象设置专属规则。

位置：[Azer.java#L43-L62](file:///d:/NuclearPowerLeak-master/src/Npl/content/Azer.java#L43-L62)

```java
ruleSetter = r -> {
    // 在这里设置 Rules r 的各种属性
};
```

### 5.2 波次与敌对配置

| 属性 | 值 | 说明 |
|------|-----|------|
| `r.waves` | true | 启用波次入侵 |
| `r.waveTeam` | `Team.green` | 敌人使用绿色阵营 |
| `r.hideSpawns` | true | 隐藏敌人出生点（增强未知感） |
| `r.waveSpacing` | 76 × Time.toSeconds | 波次间隔 76 秒 |
| `r.initialWaveSpacing` | 5f × Time.toMinutes | 第一波前有 **5 分钟**准备期 |

> 💡 **5 分钟准备期解读**：对玩家友好——核泄漏主题的资源链较长，开局给足时间搭建生产线，防止被早期波次打爆。

### 5.3 建造与资源规则

| 属性 | 值 | 说明 |
|------|-----|------|
| `r.placeRangeCheck` | false | **取消建造距离限制** |
| `r.hideBannedBlocks` | true | 隐藏未解锁/禁用的方块 |

取消建造范围限制（`placeRangeCheck = false`）意味着玩家可在整个地图任意地点建造，无需依赖核心（Core）的建造范围——适合大型战役地图。

### 5.4 初始物资 (loadout)

```java
r.loadout = ItemStack.list(
    NuItems.bigIron, 100   // 开局立即获得 100 个生铁
);
```

**设计考量**：
- Azer 星球使用专属矿物体系，原版铜/铅等可能难以获取
- 直接给 100 生铁，确保玩家能立即建造 `TestBlock`（成本 10）、`redenmore`（成本 40）等基础 Mod 方块
- 避免开局卡壳，保证 Mod 内容能立即体验

### 5.5 队伍规则与 AI

```java
Rules.TeamRule teamRule = r.teams.get(r.defaultTeam);
teamRule.rtsAi = true;                   // 启用 RTS 风格 AI
teamRule.unitBuildSpeedMultiplier = 1f;   // 单位建造速度 1×
teamRule.buildSpeedMultiplier = 1f;       // 建筑建造速度 1×
```

| 属性 | 值 | 说明 |
|------|-----|------|
| `rtsAi` | true | 启用实时战略 AI（原版更高级的 AI 行为） |
| `unitBuildSpeedMultiplier` | 1f | 玩家单位建造速度（默认不变） |
| `buildSpeedMultiplier` | 1f | 玩家建筑建造速度（默认不变） |

---

## 6. 地形生成器

```java
generator = new SerpuloPlanetGenerator();
```

Azer 目前**复用原版 Serpulo 的地形生成器**。这意味着：
- 地形风格、资源分布、水域比例与 Serpulo 相同
- 矿物类型可能仍是原版的铜、铅、钛等，但 Azer 的初始物资已提供生铁确保起步

### 6.1 后续扩展方向

若要让 Azer 更具"核污染星球"特色，可自定义 PlanetGenerator：

```java
// 自定义生成器示例（未来）
public class AzerPlanetGenerator extends PlanetGenerator {
    // 重写生成方法，加入：
    // - 大面积辐射区（降低单位生命）
    // - 高比例脏溶液/核流体湖泊
    // - 原铀/铀晶矿脉取代部分原版矿物
    // - 随机污染衰变带
}
```

---

## 7. 完整规则速查卡

| 类别 | 配置项 | Azer 值 | 原版默认对比 |
|------|--------|---------|-------------|
| **波次** | 波次开启 | ✅ | 视地图而定 |
| | 敌方阵营 | 绿色 | 通常为 猩红 (cruzild) |
| | 波次间隔 | 76 秒 | ~60 秒 |
| | 首波准备 | 5 分钟 | ~1 分钟 |
| | 出生点隐藏 | 隐藏 | 显示 |
| **建造** | 范围限制 | ❌ 无限制 | ✅ 核心范围 |
| | 禁用方块显示 | 隐藏 | 通常显示 |
| **起步** | 初始物资 | 生铁×100 | 铜/铅等 |
| **AI** | RTS AI | ✅ 启用 | 视规则 |
| **视觉** | 星图色 | 淡黄绿 | - |
| | 大气 | 黄绿双层云 | - |

---

## 8. 使用方法

### 8.1 在游戏中进入 Azer

1. 启动 Mindustry 并确保 Mod 已启用
2. 主菜单 → **"星球地图 (Planet Map)"**
3. 星图中找到淡黄绿色的 **Azer** 星球
4. 点击进入 → 选择扇区 → 开始游戏

### 8.2 进入后的确认点

- 左下角/初始核心物资栏应显示 **生铁 × 100**
- 建造菜单中 `TestBlock`、`redenmore` 可用（成本为生铁）
- 波次计时器显示约 5 分钟倒计时
- 核心建造范围应无限制（地图边缘也能建造）

---

**🔗 相关文档**：
- [NuItems 物品模块](./content-NuItems.md)（生铁物资来源）
- [项目总览](./项目总览.md)
