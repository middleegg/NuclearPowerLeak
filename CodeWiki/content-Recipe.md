# Recipe 配方系统

> **所属包**：`Npl.content`  
> **源文件**：[Recipe.java](file:///d:/NuclearPowerLeak-master/src/Npl/content/Recipe.java)  
> **类型**：通用数据结构类  
> **依赖**：`mindustry.type.ItemStack`、`LiquidStack`、`PayloadStack`、`UnlockableContent`

---

## 1. 模块职责

`Recipe` 是本 Mod 的**通用配方数据结构**，抽象描述任意"输入→输出"型生产过程。核心特点：

- **多类型支持**：同时支持 Item、Liquid、Payload 三种内容的输入输出
- **链式 API**：通过 `output()`、`craftTime()`、`priority()` 方法实现链式配置
- **可变参数构造**：构造方法直接接受 `Item, count, Item, count...` 扁平参数数组

被 `NuBlocks.test-block` 方块和 `RecipeCrafter.sl` 示例脚本使用。

---

## 2. 字段清单

| 字段名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `inputItems` | `Seq<ItemStack>` | 空 Seq | 物品输入（原料） |
| `outputItems` | `Seq<ItemStack>` | 空 Seq | 物品输出（产物） |
| `inputLiquid` | `Seq<LiquidStack>` | 空 Seq | 液体输入 |
| `outputLiquid` | `Seq<LiquidStack>` | 空 Seq | 液体输出 |
| `inputPayload` | `Seq<PayloadStack>` | 空 Seq | Payload（单位/方块载体）输入 |
| `outputPayload` | `Seq<PayloadStack>` | 空 Seq | Payload 输出 |
| `craftTime` | `float` | 60f | 制作时间（tick），默认 1 秒 |
| `priority` | `int` | 0 | 优先级（多配方自动选择时排序用） |

声明位置：[Recipe.java#L10-L17](file:///d:/NuclearPowerLeak-master/src/Npl/content/Recipe.java#L10-L17)

### 2.1 单例空配方

```java
public static Recipe empty = new Recipe();
```

表示无输入、无输出、时间 60 tick 的空配方。可用于默认占位比较。

---

## 3. 构造方法

### 3.1 无参构造（空配方）

```java
public Recipe()
```

创建所有集合为空的空白配方。`Recipe.empty` 即使用此构造。

### 3.2 可变参数构造（直接定义输入）

```java
public Recipe(Object... objects)
```

**核心能力**：接受扁平交替的 `(内容, 数量)` 对，自动区分类型并加入对应输入集合。

**参数规则**（每 2 个为一组）：

| 第 i×2 参数类型 | 第 i×2+1 参数类型 | 加入集合 |
|---------------|-----------------|---------|
| `Item` 实例 | `Integer` | `inputItems` 中创建 ItemStack |
| `Liquid` 实例 | `Float` | `inputLiquid` 中创建 LiquidStack |
| `UnlockableContent` 实例 | `Integer` | `inputPayload` 中创建 PayloadStack |

**示例**：
```java
// 创建配方：输入 3 铜 + 2 铅
Recipe r = new Recipe(Items.copper, 3, Items.lead, 2);

// 创建配方：输入 1.0 水
Recipe r2 = new Recipe(Liquids.water, 1.0f);
```

内部实现：[Recipe.java#L22-L32](file:///d:/NuclearPowerLeak-master/src/Npl/content/Recipe.java#L22-L32)

```java
for (int i = 0; i < objects.length / 2; i++) {
    if (objects[i * 2] instanceof Item item 
     && objects[i * 2 + 1] instanceof Integer count) {
        inputItems.add(new ItemStack(item, count));
    } else if (objects[i * 2] instanceof Liquid liquid 
            && objects[i * 2 + 1] instanceof Float count) {
        inputLiquid.add(new LiquidStack(liquid, count));
    } else if (...) { ... }
}
```

---

## 4. 链式方法

### 4.1 `output()` - 定义输出

```java
public Recipe output(Object... objects)
```

与构造方法完全相同的参数规则，但写入**输出**集合（`outputItems` / `outputLiquid` / `outputPayload`）。

**返回**：`this`，支持链式调用。

实现位置：[Recipe.java#L35-L46](file:///d:/NuclearPowerLeak-master/src/Npl/content/Recipe.java#L35-L46)

### 4.2 `craftTime()` - 设置制作时间

```java
public Recipe craftTime(float time)
```

**返回**：`this`。

实现位置：[Recipe.java#L50-L53](file:///d:/NuclearPowerLeak-master/src/Npl/content/Recipe.java#L50-L53)

### 4.3 `priority()` - 设置优先级

```java
public Recipe priority(int priority)
```

**返回**：`this`。数值越大优先级越高（排序时越靠前）。

实现位置：[Recipe.java#L58-L61](file:///d:/NuclearPowerLeak-master/src/Npl/content/Recipe.java#L58-L61)

---

## 5. 完整使用示例

### 5.1 TestBlock 中的三种配方

[NuBlocks.java#L62-L72](file:///d:/NuclearPowerLeak-master/src/Npl/content/NuBlocks.java#L62-L72)

```java
// 配方 0：5 铜 → 2 铅，60 tick
recipes[0] = new Recipe(Items.copper, 5)
        .output(Items.lead, 2)
        .craftTime(60f);

// 配方 1：3 铅 → 1 钛
recipes[1] = new Recipe(Items.lead, 3)
        .output(Items.titanium, 1)
        .craftTime(60f);

// 配方 2：3 铜 + 2 铅 → 1 生铁
recipes[2] = new Recipe(Items.copper, 3, Items.lead, 2)
        .output(NuItems.bigIron, 1)
        .craftTime(60f);
```

### 5.2 多类型混合配方（未来扩展示例）

```java
Recipe nuclearRecipe = new Recipe(
    // 输入物品
    NuItems.oriUranium, 2,
    // 输入液体（Float！）
    NuLiquid.liquidOxygen, 5.0f
)
    .output(NuLiquid.nuclearFluid, 3.0f)   // 输出液体
    .output(NuItems.bottledMagenticStorm, 1) // 输出物品
    .craftTime(300f)   // 5 秒
    .priority(10);     // 高优先级
```

---

## 6. Recipe 在 ConfigurableBlock 中的转换

`ConfigurableBlock` 内部不直接使用 `Recipe` 类，而是使用其内部的 `Plan` 类。在加载时需要进行转换（NuBlocks 中 TestBlock 实际直接使用内部 recipes 数组，需注意）。

两种配方结构的对应关系：

| Recipe 字段 | ConfigurableBlock.Plan 对应 |
|-------------|----------------------------|
| `inputItems` | `requirements` (ItemStack[]) |
| `outputItems`（仅第一个） | `outItem` (ItemStack) |
| `craftTime` | `time` (float) |

> 💡 **注意**：Recipe 支持多物品输出，但 ConfigurableBlock.Plan 目前仅存储单一 `outItem`。若要使用 Recipe 的完整能力，需扩展 Plan 结构。

---

## 7. 与 RecipeCrafter.sl 的协同

`src/Npl/newSth/RecipeCrafter.sl` 中的 `RecipeCrafter` 类定义了：
```java
public Seq<Recipe> recipes = new Seq<>();
```

并通过继承 `GenericCrafter` 实现按配方优先级自动选择生产。可见 Recipe 类设计为通用结构，可在不同方块类型中复用。

---

## 8. 设计亮点与可扩展点

### 8.1 设计亮点

| 特点 | 说明 |
|------|------|
| **扁平参数 API** | `new Recipe(item, count, item, count...)` 写法简洁，无需嵌套 `ItemStack.with()` |
| **三类型统一** | Item/Liquid/Payload 一套 API 全覆盖 |
| **完全链式** | 构造→output→craftTime→priority 一气呵成，可读性极高 |
| **无副作用类** | 纯数据结构，不含逻辑，易于测试和传输 |

### 8.2 可扩展点（后续开发建议）

| 扩展方向 | 建议实现 |
|----------|---------|
| **输入条件验证** | 增加 `boolean canCraft(Building b)` 方法检查是否资源充足 |
| **应用消耗/产出** | 增加 `void apply(Building b)` 方法一次完成扣原料加产物 |
| **多输出支持** | ConfigurableBlock.Plan 改为 Seq<ItemStack> outItems，避免仅单输出 |
| **冷却/加热** | 增加 heatRequired / coolRequired 字段支持热力学配方 |
| **电力倍率** | 增加 powerMultiplier 字段（某些配方用电更多） |

---

**🔗 相关文档**：
- [ConsumeRecipe 配方消费器](./content-ConsumeRecipe.md)
- [ConfigurableBlock 可配置方块](./newSth-ConfigurableBlock.md)
- [NuBlocks 方块模块](./content-NuBlocks.md)
