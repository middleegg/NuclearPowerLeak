# ConfigurableBlock 可配置方块

> **所属包**：`Npl.newSth`  
> **源文件**：[ConfigurableBlock.java](file:///d:/NuclearPowerLeak-master/src/Npl/newSth/ConfigurableBlock.java)  
> **类型**：自定义方块类（继承自 `mindustry.world.Block`）  
> **内部类**：`Plan`（配方）、`UnitFactoryBuild`（建筑实体）  
> **代码量**：约 470 行，本 Mod 最复杂的单个类

---

## 1. 模块职责

`ConfigurableBlock` 是本 Mod 的**核心机制创新**：实现了一个**运行时可切换多配方的生产方块**。原版 Mindustry 的生产方块（GenericCrafter 等）通常只能绑定单一固定配方，而 ConfigurableBlock 支持：

- 一个方块内置**多个配方 Plan**
- 玩家**点击方块 → 选择输出物品**即可切换配方
- 支持**逻辑处理器**通过 configure 指令切换
- 自动保存/恢复当前选择的配方和进度

---

## 2. 类结构总览

```
mindustry.world.Block
        │
        ▼
ConfigurableBlock
 ├─ 字段：capacities, plans, selectionRows/Columns
 ├─ 内部类 Plan（配方结构）
 └─ 内部类 UnitFactoryBuild extends Building
      ├─ 字段：currentPlan, progress
      └─ 覆写方法：updateTile / draw / buildConfiguration / write&read ...
```

---

## 3. ConfigurableBlock 字段

### 3.1 公开字段（可在初始化块配置）

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `capacities` | `int[]` | `{}` | 每种物品的最大容量（按 Item.id 索引） |
| `plans` | `Seq<Plan>` | `new Seq<>(4)` | **配方列表**，索引 0~N 对应各模式 |
| `selectionRows` | `int` | 2 | 配置面板行数 |
| `selectionColumns` | `int` | 4 | 配置面板列数 |

> 💡 在方块定义的双花括号初始化块中，实际配方存放在 `plans` 中。`TestBlock` 目前通过独立的 `recipes[]` 数组初始化，建议后续改为直接填充 `plans`。

### 3.2 构造方法默认设定

```java
public ConfigurableBlock(String name) {
    super(name);
    update = true;          // 每 tick 执行 updateTile
    solid = true;           // 实体方块（不可穿越）
    hasItems = true;        // 有物品仓
    hasPower = true;        // 接电
    consumesPower = true;   // 耗电
    size = 2;               // 2×2
    health = 100;           // 血量
    rotate = true;          // 可旋转（有出物口 outRegion）
    configurable = true;    // ★ 可配置（点击弹出面板）
    itemCapacity = 30;      // 默认容量/物品
    // ... 配置处理器 ...
}
```
位置：[ConfigurableBlock.java#L45-L80](file:///d:/NuclearPowerLeak-master/src/Npl/newSth/ConfigurableBlock.java#L45-L80)

---

## 4. 配置处理机制（config）

Mindustry 方块通过 `config(Type, handler)` 注册配置入口，ConfigurableBlock 注册了三种：

### 4.1 按索引切换配方

```java
config(Integer.class, (UnitFactoryBuild build, Integer i) -> {
    if (!configurable) return;
    if (build.currentPlan == i) return;           // 相同则无操作
    build.currentPlan = i < 0 || i >= plans.size ? -1 : i;
    build.progress = 0;                            // 切换后进度清零
});
```

**用途**：逻辑处理器 Logic Processor 使用 `configure` 数字指令切换。

### 4.2 按输出物品切换配方

```java
config(Item.class, (UnitFactoryBuild build, Item item) -> {
    int next = plans.indexOf(p -> p.outItem.item == item);
    if (build.currentPlan == next) return;
    build.currentPlan = next;
    build.progress = 0;
});
```

**用途**：玩家点击配置面板选择物品后触发。

### 4.3 清除配置

```java
configClear((UnitFactoryBuild build) -> {
    build.currentPlan = -1;  // -1 表示未选择任何配方
    build.progress = 0;
});
```

---

## 5. 内部类 Plan - 配方结构

### 5.1 字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `outItem` | `ItemStack` | **单个**输出物品 + 数量 |
| `requirements` | `ItemStack[]` | 消耗原料数组 |
| `time` | `float` | 生产耗时（tick） |

### 5.2 构造方法

```java
public Plan(ItemStack outItem, float time, ItemStack[] requirements)
```

> ⚠️ **当前限制**：Plan 仅支持**单输出物品**。若需要多输出或液体/Payload，需扩展 Plan 结构（参考通用 Recipe 类）。

---

## 6. 内部类 UnitFactoryBuild - 建筑实体

每个被玩家放置在地图上的 ConfigurableBlock 方块，其运行时状态由 `UnitFactoryBuild` 实例承载。

### 6.1 核心字段

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `currentPlan` | `int` | -1 | 当前配方索引（-1 = 未选择） |
| `progress` | `float` | 0f | 当前配方的完成进度 0 ~ time |

### 6.2 `fraction()` - 完成比例

```java
public float fraction() {
    if (currentPlan == -1 || currentPlan >= plans.size) return 0;
    Plan p = plans.get(currentPlan);
    return p == null ? 0 : progress / p.time;  // 0~1
}
```
用于进度条和逻辑感知 `LAccess.progress`。

---

## 7. 生命周期方法详解

### 7.1 Block 级别（所有方块共享一次）

| 方法 | 作用 | 关键行为 |
|------|------|---------|
| `init()` | 内容加载后初始化一次 | 调用 `initCapacities()` 计算每物品容量上限 |
| `afterPatch()` | Mod 热更后调用 | 再次 `initCapacities()` 确保数组容量 |
| `checkContentArrayCapacity()` | 物品数变化时 | 扩容 `capacities[]` |
| `setBars()` | 设置血条/进度条 | 添加 `progress` 进度条 |
| `setStats()` | 详情页统计 | 移除默认 itemCapacity，添加配方表格 |
| `icons()` | 菜单图标 | 返回 `[region, outRegion, topRegion]` |
| `drawPlanRegion()` | 建造预览 | 绘制底图 + 出口 + 顶图 |
| `buildConfiguration()` | 配置面板 | 生成物品选择网格 |
| `outputsItems()` | 是否产出 | `true` |
| `getPlanConfigs()` | 逻辑配置列表 | 返回所有解锁的输出物品 |

### 7.2 Building 级别（每个放置实例）

| 方法 | 作用 | 关键行为 |
|------|------|---------|
| `created()` | 被放置时 | 自动选择第一个已解锁配方（避免空转） |
| `updateTile()` | 每 tick | ★ 核心：推进 progress、完成后扣原料+产出 |
| `shouldConsume()` | 是否应消耗 | 无配方/输出满时不耗电 |
| `status()` | 状态图标 | 未激活单位工厂时返回特殊状态 |
| `getMaximumAccepted()` | 单物品最大容量 | 读取 capacities[item.id] × teamCost |
| `acceptItem()` | 是否接受物品 | 仅接受当前配方所需的 |
| `draw()` | 绘制 | 底图→出口→（可选进度图标）→顶图 |
| `drawSelect()` | 选中时 | 绘制当前输出物品的悬浮选择环 |
| `display()` | 悬浮信息 | 显示所选配方图标和名称 |
| `senseObject(LAccess)` | 逻辑读取 | `config` 返回输出物品 |
| `sense(LAccess)` | 逻辑读取 | `progress` 返回比例, `itemCapacity` |
| `config()` | 获取配置值 | 返回 currentPlan（int） |
| `write()` `read()` | 存档序列化 | 保存 progress 和 currentPlan，含版本号兼容 |

---

## 8. 核心生产循环

`updateTile()` 是整个方块的"心脏"，每 tick 执行。

### 8.1 流程

```
每 tick 调用 updateTile():
  │
  ├─► 若无有效配方（currentPlan 越界/空/输出物被禁用）→ currentPlan = -1，return
  │
  ├─► if (efficiency > 0):
  │       progress += edelta() * unitBuildSpeedMultiplier(team)
  │
  ├─► if (progress >= plan.time):  // 一周期完成
  │     │
  │     ├─► 检查 maxAccept(产出物) >= 产出数量
  │     │     │
  │     │     ├── 是 → 给产出物 items.add()，progress -= time
  │     │     │       调用 consume()（扣原料）
  │     │     │       播放 createSound（可选）
  │     │     │
  │     │     └── 否 → progress 卡在 time - 0.001（微差，不溢出）
  │
  └─► progress < 0 时钳制为 0
```

代码位置：[ConfigurableBlock.java#L364-L401](file:///d:/NuclearPowerLeak-master/src/Npl/newSth/ConfigurableBlock.java#L364-L401)

### 8.2 `shouldConsume()` - 消费条件

只有同时满足以下条件才真正消耗电力和原料：
1. `currentPlan != -1` 且合法
2. `enabled == true`（逻辑没关掉）
3. 配方存在且输出物存在
4. 还有空间放产出物

---

## 9. 容量计算 initCapacities()

```java
public void initCapacities() {
    capacities = new int[Vars.content.items().size];
    
    // 找所有配方中单次消耗最大量
    int maxAmount = 0;
    for (Plan plan : plans) {
        for (ItemStack stack : plan.requirements) {
            if (stack.amount > maxAmount) maxAmount = stack.amount;
        }
    }
    
    // 统一容量 = max(1, maxAmount × 6)
    int unifiedLimit = Math.max(1, maxAmount * 6);
    Arrays.fill(capacities, unifiedLimit);
    
    // 消耗成本随规则缩放（RTS AI 成本倍率）
    consumeBuilder.each(c -> c.multiplier = b -> state.rules.unitCost(b.team));
}
```
容量公式：**单种物品最大仓容量 = 配方最大单次用量 × 6**，保证至少能存 6 轮的料。

---

## 10. UI：配方选择面板 buildConfiguration()

点击方块 → 触发 `buildConfiguration(Table)` → 生成物品选择网格：

```
使用 Mindustry 内置 ItemSelection.buildTable：
  ├─ 候选：plans 中所有已解锁、未禁用的输出物品
  ├─ 当前选择：currentPlan → 对应物品（无则 null）
  ├─ 选择回调：点击物品 → 按物品配置
  └─ 布局：selectionRows（2） × selectionColumns（4）网格
```

位置：[ConfigurableBlock.java#L194-L220](file:///d:/NuclearPowerLeak-master/src/Npl/newSth/ConfigurableBlock.java#L194-L220)

---

## 11. 统计面板 setStats()

移除原版默认的 ItemCapacity，改为**逐配方渲染的灰色面板表格**，每行包含：
- 左：产出物品大图标（可悬停查看）+ 数量
- 中：物品名 + 制作时间（秒，保留 1 位小数）
- 右：原料网格（每 4 个换行，显示名称、数量、每秒消耗）

位置：[ConfigurableBlock.java#L129-L170](file:///d:/NuclearPowerLeak-master/src/Npl/newSth/ConfigurableBlock.java#L129-L170)

---

## 12. 逻辑处理器接口

Logic Processor 可通过以下方式与方块交互：

| 操作 | 指令写法 | 效果 |
|------|---------|------|
| 读取进度 | `sensor @progress block1` | 返回 0.0 ~ 1.0 完成比例 |
| 读取配置 | `sensor @config block1` | 返回当前输出 Item（可对比） |
| 读取容量 | `sensor @itemCapacity block1` | 返回 itemCapacity = 30 |
| 切换配方（索引） | `configure block1 0` | 切换到 plans[0]（铜→铅） |
| 切换配方（物品） | `configure block1 @lead` | 按输出物品自动查找匹配索引 |
| 停止生产 | `configure block1 -1` | 取消所有配方 |

---

## 13. 存档序列化与版本兼容

```java
// 写档
write(Writes w) {
    super.write(w);
    w.f(progress);     // float
    w.s(currentPlan);  // short（plans 多的时候也够）
}

// 读档
read(Reads r, byte revision) {
    super.read(r, revision);
    progress = r.f();
    currentPlan = r.s();
    if (revision >= 2) r.bool();   // v2 遗留字段，跳过
    if (revision >= 3) { /* 预留 */ }
}

version() { return 3; }  // 存档版本号
```

支持从旧版存档的升级读取（`revision` 字段标识版本），避免存档因 Mod 更新损坏。

---

## 14. TestBlock 使用范例

[NuBlocks.java#L56-L76](file:///d:/NuclearPowerLeak-master/src/Npl/content/NuBlocks.java#L56-L76)

```java
TestBlock = new ConfigurableBlock("test-block") {{
    modeCount = 3;
    recipes[0] = new Recipe(Items.copper, 5).output(Items.lead, 2).craftTime(60f);
    recipes[1] = new Recipe(Items.lead,   3).output(Items.titanium, 1).craftTime(60f);
    recipes[2] = new Recipe(Items.copper, 3, Items.lead, 2)
                        .output(NuItems.bigIron, 1).craftTime(60f);
    requirements(Category.crafting, with(NuItems.bigIron, 10));
    size = 2;
}};
```

> ⚠️ **注**：以上代码使用了独立的 `recipes[]` + `modeCount` 字段（`ConfigurableBlock` 中未定义，因此实际上通过父类机制到 `plans` 需要额外转换。当前实现可能通过初始化块配合自定义配方数组填充 `plans`。若需完善，可在构造或 init 阶段将 Recipe[] 自动转换为 Plan[]。

---

**🔗 相关文档**：
- [Recipe 配方系统](./content-Recipe.md)
- [NuBlocks 方块模块](./content-NuBlocks.md)
