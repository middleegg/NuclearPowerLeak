# 改版 ChangeLog（CodeWiki_Refactored 文件夹）

> **所有改动均仅在 `Refactored_Code/` 目录，未修改 `src/` 原文件**
> 用户核实后，再决定是否复制覆盖 `src/Npl/newSth/ConfigurableBlock.java` 和 `src/Npl/content/NuBlocks.java`

---

## 一、总体概览

| 类别 | 变化 |
|------|------|
| ✨ Plan 结构扩展 | 从单物品输出 → 多物品+液体 输入输出 |
| 🔗 Recipe → Plan 桥接 | 新增 `addRecipe()` `setRecipes()` 方法，解决「Recipe 定义了但 plans 为空」的问题 |
| 🐛 TestBlock 修复 | 原 `recipes[]` → `plans` 正确填充（原根本不工作） |
| 🧪 高级演示方块 advanced-block | 新增：演示多输出 + 液体配方（核反应三输出） |
| 🔒 向后兼容 | 旧代码访问 `plan.outItem` `plan.requirements` 不报错（@Deprecated 别名） |
| 💾 存档版本 | version() 从 3 → 4，预留扩展位 |

---

## 二、ConfigurableBlock.java 详细改动

### 2.1 Plan 内部类结构扩展

| 字段 | 原版 | 改版 |
|------|------|------|
| 输出物品 | `ItemStack outItem`（单） | `ItemStack[] outputItems`（多，新增） |
| 输入物品 | `ItemStack[] requirements` | `ItemStack[] inputItems`（语义更名） |
| 输出液体 | ❌ 无 | `LiquidStack[] outputLiquids`（新） |
| 输入液体 | ❌ 无 | `LiquidStack[] inputLiquids`（新） |
| 制作时间 | `float time` | `float time`（保留） |

**向后兼容**：保留了 `@Deprecated public ItemStack outItem` 和 `requirements`（指向 inputItems），旧代码不用改也不会 NPE。

三参构造 `Plan(outItem, time, requirements)` 也保留，原 test-block 的老写法直接可用。

### 2.2 新增桥接 API（Recipe → Plan）

```java
/** 追加单个 Recipe → Plan */
public Plan addRecipe(Npl.content.Recipe r)

/** 批量设置（覆盖原 plans） */
public void setRecipes(Npl.content.Recipe... recipes)
```

内部由静态工具类 `RecipeBridge.fromRecipe(r)` 完成所有字段映射：
- `Recipe.outputItems → Plan.outputItems`
- `Recipe.inputItems → Plan.inputItems`
- `Recipe.outputLiquid → Plan.outputLiquids`
- `Recipe.inputLiquid → Plan.inputLiquids`
- `Recipe.craftTime → Plan.time`

### 2.3 hasLiquids 开启 + 液体容量

原：`hasLiquids` 继承自 UnitBlock = false  
改：构造器显式设 `hasLiquids = true; liquidCapacity = 30f;`

### 2.4 initCapacities 容量计算扩展

原：只统计输入物品  
改：同时扫描 `plan.inputItems` 和 `plan.outputItems` 的单次最大量，保证输出物品仓也放得下。

### 2.5 切换配方时 dump()

```java
config(Integer.class, ...) {
    ...
    build.progress = 0;
    build.dump();   // ✅ 新增：切配方把非本配方的东西甩出去，防堵仓
}
```

### 2.6 acceptItem / acceptLiquid（输入+输出物都接受）

- 原 `acceptItem` 只收配方输入物品 → 改版同时收**本配方的输出物品**（切换回来不会卡住）
- 新增 `acceptLiquid` 覆写：接受本配方 inputLiquids / outputLiquids 中的液体

### 2.7 updateTile 核心循环（多物品+液体扣/放）

新增两个私有方法：

```java
private boolean canAcceptAllOutputs(Plan plan)  // 所有输出物（物品+液体）能否全装下
private boolean hasAllInputs(Plan plan)         // 所有输入（物品+液体）是否齐全
```

进入 `progress >= plan.time` 分支后：
```
同时校验 hasAllInputs + canAcceptAllOutputs  → 通过后：
  items.remove() 扣 inputItems
  liquids.remove() 扣 inputLiquids
  items.add()    加 outputItems
  liquids.add()  加 outputLiquids
  offload()      每个输出物品尝试推送到下游
  progress -= time; consume(); sound...
否则 progress 卡在 time-0.001
```

### 2.8 setStats 详情面板多输出/液体显示

原：单输出图标  
改：`outputItems` 每个都画 40px 图标 + ×N；`outputLiquids` 每个图标 + 数量单位 u；右侧消耗品逐行（每4个换行），物品用 `displayItem` / 液体用 `displayLiquid`。

### 2.9 buildConfiguration 兼容索引锚点

原：`plans.indexOf(p.outItem.item == item)`  
改：`p.outputItems[0].item == item`（按"首个输出物品"作为 UI 锚点）  
同时处理 `outputItems` 为空但 `outputLiquids` 有值的纯液体配方情况 → 在 created/drawSelect/display 各分支判断。

### 2.10 senseObject 返回策略

先尝试 outputItems[0].item → 退而求其次 outputLiquids[0].liquid → 都没有返回 null（原仅返回单物品）。

### 2.11 存档 version 4

原 `version() = 3` → `version() = 4`：
```
revision >= 4 时预留空占位，未来加 savedOutputLiquidIdx 等也不会破坏老存档。
```

---

## 三、NuBlocks.java 详细改动

### 3.1 🐛 修复：TestBlock 的 recipes → plans 桥接（原问题核心）

**原问题**：原 NuBlocks 里写了 `modeCount = 3; recipes[0..2] = new Recipe(...)`，但 ConfigurableBlock 这个类本身既没有 `modeCount` 字段，也没有 `recipes[]` 字段，更没把 Recipe[] 填充进 plans。运行时 **plans 为空 Seq，UI 显示 @none，切换不了，也不生产**。

**修复方式（两种等价写法均可）：**

```java
// 方式 A（新版推荐，更短）：
setRecipes(recipes);   // 直接调用新桥接 API

// 方式 B（手动，等价于桥接器内部逻辑）：
plans.clear();
for (int i = 0; i < recipes.length; i++) plans.add(RecipeBridge.fromRecipe(recipes[i]));
```

test-block 现在可正确显示 3 种输出：铅 / 钛 / 生铁，点击切换即工作。

### 3.2 🧪 新增 advanced-block 演示方块

3×3、320 血、1.2/tick 耗电。演示了 **2 模式多输出 + 液体**：

| 模式 | 输入 | 输出 | 时间 |
|------|------|------|------|
| 0 | 生铁×2 + 液氧×2.0u | 磁铁×1 + 奇液×1.0u | 120 tick |
| 1 | 原铀×1 + 水×3.0u | 铀晶×2 + 脏溶液×0.5u + **瓶装磁暴×1**（三输出！） | 240 tick |

建材：生铁×80 + 磁铁×20。

可用于直观验证新版 Plan 的液体 I/O 和多输出是否工作正常。

### 3.3 🧊 原版 redenmore 未动

redenmore 继续保持 GenericCrafter 原写法，不影响任何既有用法。

---

## 四、文件清单（Refactored_Code/）

| 路径 | 对应原文件 | 改动级别 |
|------|-----------|---------|
| `Refactored_Code/ConfigurableBlock.java` | `src/Npl/newSth/ConfigurableBlock.java` | **重度重构**（Plan扩展+桥接+液体+兼容层） |
| `Refactored_Code/NuBlocks.java` | `src/Npl/content/NuBlocks.java` | **修复 + 新增演示方块** |

---

## 五、核实步骤（用户侧建议）

1. 打开 CodeWiki 根目录下 **Refactored_Code/** 两个 java 文件浏览
2. 对照本文档各条目逐条验证是否符合预期：
   - ✅ Plan 现在支持 outputItems/inputItems/outputLiquids/inputLiquids 四个数组
   - ✅ 存在 `addRecipe(Recipe)` 和 `setRecipes(Recipe...)` 两个方法
   - ✅ TestBlock 初始化末尾调用了 `setRecipes(recipes)`
   - ✅ advanced-block 使用了 2 个 addRecipe 演示液体和多输出
   - ✅ `@Deprecated Plan.outItem / requirements` 仍存在以兼容旧代码
3. 核实通过后，手动复制两文件到 src 对应目录覆盖；或告诉我"确认"，我直接替换

---

## 六、未触及的（刻意保留不变）

- 不改动 Recipe.java —— 它是通用数据结构，保留原样，仅由桥接器读取
- 不改动 NewItemsType.java / ModStats.java / Azer.java / NuItems.java / NuLiquid.java —— 这些与 ConfigurableBlock 解耦
- 不改动 build.gradle、mod.hjson、assets —— 无需升级
- 不做代码格式化或重命名（最小化变动面），便于 diff
