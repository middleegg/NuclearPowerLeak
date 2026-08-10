# NuBlocks 方块模块

> **所属包**：`Npl.content`  
> **源文件**：[NuBlocks.java](file:///d:/NuclearPowerLeak-master/src/Npl/content/NuBlocks.java)  
> **类型**：内容注册器（静态工具类）  
> **依赖**：`ConfigurableBlock`、`NuItems`、`Recipe`、`GenericCrafter`

---

## 1. 模块职责

`NuBlocks` 是**自定义方块注册中心**，负责：
1. 声明所有自定义方块的静态字段
2. 在 `load()` 方法中实例化方块、配置配方、设置建造成本
3. 将自定义物品（生铁等）作为建材和配方输入输出使用

---

## 2. 方块字段总览

| 字段名 | 方块内部 ID | 类型 | 分类 | 尺寸 | 说明 |
|--------|------------|------|------|------|------|
| `TestBlock` | `test-block` | ConfigurableBlock | Category.crafting (合成) | 2×2 | **多配方可切换测试方块**，支持 3 种配方模式 |
| `redenmore` | `redenmore` | GenericCrafter | Category.crafting (合成) | 2×2 | **传统单配方工匠方块**，消耗生铁生产磁铁 |

字段声明位置：[NuBlocks.java#L53](file:///d:/NuclearPowerLeak-master/src/Npl/content/NuBlocks.java#L53)

---

## 3. 公共方法

### 3.1 `load()` - 方块注册入口

```java
public static void load()
```

**调用链**：
```
nu.loadContent()
    └──► NuBlocks.load()
          ├──► TestBlock = new ConfigurableBlock("test-block") {{ ... }}
          └──► redenmore = new GenericCrafter("redenmore") {{ ... }}
```

调用位置：[nu.java#L35](file:///d:/NuclearPowerLeak-master/src/Npl/nu.java#L35)

---

## 4. 方块详解

### 4.1 TestBlock - 多配方可切换方块

**代码位置**：[NuBlocks.java#L56-L76](file:///d:/NuclearPowerLeak-master/src/Npl/content/NuBlocks.java#L56-L76)

#### 核心配置

```java
TestBlock = new ConfigurableBlock("test-block") {{
    modeCount = 3;   // 3 种配方模式
    
    // 配方模式 0
    recipes[0] = new Recipe(Items.copper, 5)
            .output(Items.lead, 2)
            .craftTime(60f);
    
    // 配方模式 1
    recipes[1] = new Recipe(Items.lead, 3)
            .output(Items.titanium, 1)
            .craftTime(60f);
    
    // 配方模式 2
    recipes[2] = new Recipe(Items.copper, 3, Items.lead, 2)
            .output(NuItems.bigIron, 1)
            .craftTime(60f);
    
    // 建造成本：10 生铁
    requirements(Category.crafting, with(NuItems.bigIron, 10));
    size = 2;
}};
```

#### 三种配方模式详解

| 模式 | 输入 | 输出 | 制作时间 | 说明 |
|------|------|------|----------|------|
| **模式 0** | 铜 × 5 | 铅 × 2 | 60 tick (1秒) | 基础铜转铅 |
| **模式 1** | 铅 × 3 | 钛 × 1 | 60 tick | 铅转钛 |
| **模式 2** | 铜 × 3 + 铅 × 2 | 生铁 × 1 | 60 tick | 生产 Mod 专属生铁 |

#### 游戏中切换方式
1. 点击已放置的 test-block 方块
2. 在弹出的配置面板中，从 2×4 网格选择目标输出物品
3. 方块立即切换到对应模式，进度清零重算
4. 也可通过逻辑处理器（Logic Processor）使用 `configure` 指令按索引切换

#### 建造成本
| 材料 | 数量 |
|------|------|
| NuItems.bigIron (生铁) | 10 |

---

### 4.2 redenmore - 磁铁生产机

**代码位置**：[NuBlocks.java#L77-L87](file:///d:/NuclearPowerLeak-master/src/Npl/content/NuBlocks.java#L77-L87)

#### 核心配置

```java
redenmore = new GenericCrafter("redenmore") {{
    requirements(Category.crafting, with(NuItems.bigIron, 40)); // 建造成本
    outputItem = new ItemStack(NuItems.magent, 1);              // 每次产出 1 磁铁
    craftTime = 60f;                                            // 1 秒一次
    hasItems = hasPower = true;                                 // 需要物品和电力
    ambientSound = Sounds.loopGrind;                            // 环境音效：研磨机
    ambientSoundVolume = 0.025f;                                // 音量 2.5%
    consumeItems(ItemStack.with(NuItems.bigIron, 3));           // 每次消耗 3 生铁
    consumePower(0.50f);                                        // 持续消耗 0.5 电力/tick
    size = 2;                                                   // 2×2 尺寸
}};
```

#### 生产链公式

```
输入：生铁 × 3 + 电力 0.5/tick
    │  60 tick（1 秒）
    ▼
输出：磁铁 × 1
```

#### 生产成本与效率

| 项目 | 数值 |
|------|------|
| 建造成本 | 生铁 × 40 |
| 单周期消耗 | 生铁 × 3 |
| 单周期产出 | 磁铁 × 1 |
| 单周期时长 | 60 tick = 1 秒 |
| 电力消耗 | 0.50 × 60 = **30 电力/周期** |
| 理论产速 | 60 磁铁/分钟 |

#### 音效
- 使用原版 `Sounds.loopGrind`（循环研磨声）
- 音量 0.025（2.5%，轻微环境音）

---

## 5. 方块属性速查

### 5.1 通用方块属性

| 属性 | TestBlock | redenmore | 说明 |
|------|-----------|-----------|------|
| 基类 | ConfigurableBlock | GenericCrafter | 继承关系 |
| 内部 ID | `test-block` | `redenmore` | Mod 中唯一标识 |
| 分类 Category | crafting | crafting | 在菜单中的分类 |
| 尺寸 size | 2 | 2 | 占用 2×2 地块 |
| 是否需要电力 hasPower | true | true | - |
| 是否需要物品 hasItems | true | true | - |
| 是否可配置 configurable | true | false | - |
| 配方数 | 3（可切换） | 1（固定） | - |

### 5.2 ConfigurableBlock 额外属性

TestBlock 继承自 ConfigurableBlock，因此自动获得：
| 能力 | 说明 |
|------|------|
| 模式切换 | 支持 3 种运行时切换的配方模式 |
| 配方选择 UI | 点击方块弹出 2×4 物品选择网格 |
| 进度条显示 | Bar "progress" 显示当前配方完成比例 |
| 逻辑感知 | `sense config` 返回当前输出物品；`sense progress` 返回 0~1 |
| 持久化 | `progress` 和 `currentPlan` 自动保存到存档 |

---

## 6. 方块依赖关系

```
NuBlocks.load()
    │
    ├──► 依赖 ConfigurableBlock（newSth 包）───┐
    │                                         │
    ├──► 依赖 Recipe（content 包）─────────────┼──► 机制层
    │                                         │
    ├──► 使用 NuItems.bigIron 做建材 ──┐      │
    │                                 │      │
    └──► 配方中引用 Items.copper       ├──────┴──► 内容层
           lead, titanium (原版)      │
           NuItems.bigIron, magent    │
           ───────────────────────────┘
```

---

## 7. 使用范例（游戏内）

### 7.1 利用 TestBlock 生产生铁

1. 建造 1 个 TestBlock（成本 10 生铁，可用初始物资建造）
2. 点击方块 → 选择模式 2（输出生铁图标）
3. 接入铜和铅的供应
4. 接入电力
5. 观察：每 1 秒消耗 3 铜 + 2 铅，生产 1 生铁

### 7.2 利用 redenmore 生产磁铁

1. 建造 1 个 redenmore（成本 40 生铁）
2. 接入生铁供应（每 3 个/秒）
3. 接入电力（0.5/tick 持续）
4. 观察：研磨声响起，每 1 秒自动产出 1 磁铁

---

## 8. 扩展建议

当前 NuBlocks 仅定义了 2 个示例方块。基于核泄漏主题，后续可扩展的方块类型：

| 方块类型 | 建议名称 | 功能 |
|----------|---------|------|
| 核反应堆 | `nuclear-reactor` | 消耗铀晶/原铀 + 液氧冷却，产出大量电力 |
| 冷却塔 | `cooling-tower` | 使用液氧/奇液快速冷却高温方块 |
| 离心机 | `centrifuge` | 将脏溶液分离为可回收材料 + 核废料 |
| 磁暴产生器 | `magent-storm-gen` | 消耗瓶装磁暴生成范围磁场效果 |
| 废料焚化炉 | `waste-incinerator` | 消耗电力焚烧脏溶液/核废料（incinerable） |

---

**🔗 相关文档**：
- [ConfigurableBlock 可配置方块](./newSth-ConfigurableBlock.md)
- [Recipe 配方系统](./content-Recipe.md)
- [NuItems 物品模块](./content-NuItems.md)
