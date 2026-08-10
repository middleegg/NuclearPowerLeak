# ConsumeRecipe 配方消费器

> **所属包**：`Npl.content`  
> **源文件**：[ConsumeRecipe.java](file:///d:/NuclearPowerLeak-master/src/Npl/content/ConsumeRecipe.java)  
> **类型**：自定义消费条件类  
> **基类**：`mindustry.world.consumers.Consume`  
> **依赖**：`arc.func.Boolf`、`mindustry.gen.Building`

---

## 1. 模块职责

`ConsumeRecipe` 是对 Mindustry 原版 `Consume` 体系的**轻量扩展**，用布尔判断函数 (`Boolf<Building>`) 描述任意自定义消费条件。

典型用途：
- 描述"是否满足某个 Recipe 配方的输入资源"这种动态条件
- 区分"内部判断用 valid" 和 "面板显示用 display" 两套条件
- 可嵌入方块 `consume()` 链，参与原版消费系统统计与 UI 渲染

---

## 2. 类定义与字段

### 2.1 继承关系

```
mindustry.world.consumers.Consume
          ▲
          │ extends
          │
  Npl.content.ConsumeRecipe
```

### 2.2 字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `valid` | `Boolf<Building>` | **有效性判断**：实际生产前必须为 true |
| `display` | `Boolf<Building>` | **显示判断**：仅用于 UI 上显示/隐藏该消费项 |

字段声明位置：[ConsumeRecipe.java#L8-L9](file:///d:/NuclearPowerLeak-master/src/Npl/content/ConsumeRecipe.java#L8-L9)

---

## 3. 构造方法

### 3.1 双参数构造

```java
public ConsumeRecipe(Boolf<Building> valid, Boolf<Building> display)
```

分别指定判断函数和显示函数。

**使用场景**：条件严格但显示宽松，避免 UI 闪烁。例如：
```java
// valid：需要至少 3 生铁才能开始
// display：只要 > 0 就在 UI 上高亮"已有原料"
new ConsumeRecipe(
    b -> b.items.get(NuItems.bigIron) >= 3,
    b -> b.items.get(NuItems.bigIron) > 0
);
```

### 3.2 单参数构造（valid = display）

```java
public ConsumeRecipe(Boolf<Building> valid)
```

display 和 valid 使用同一函数，适合简单场景。

---

## 4. 方法

### 4.1 `isValid(Building build)`

```java
public boolean isValid(Building build) {
    return valid.get(build);
}
```

位置：[ConsumeRecipe.java#L21-L23](file:///d:/NuclearPowerLeak-master/src/Npl/content/ConsumeRecipe.java#L21-L23)

**注意**：该方法**没有**使用 `@Override`，因为原版 `Consume` 类的 `isValid` 签名不同。这是一个**自定义新增方法**，需要使用方（自定义方块）手动调用。

---

## 5. 使用场景与范例

### 5.1 场景 1：为配方添加动态消费条件

```java
// 在自定义方块初始化块中
redenmore = new GenericCrafter("redenmore") {{
    requirements(Category.crafting, with(NuItems.bigIron, 40));
    
    // ... 原有 consumeItems、consumePower
    
    // 附加：仅当原料仓存在 > 10 生铁时才允许开工
    ConsumeRecipe condition = new ConsumeRecipe(
        b -> b.items.get(NuItems.bigIron) >= 10  // 有效条件：>= 10
    );
    
    // （需要修改方块代码调用 condition.isValid()）
}};
```

### 5.2 场景 2：复合多配方动态条件

```java
// 判断当前配置的配方是否都有足够原料
ConsumeRecipe multiRecipe = new ConsumeRecipe(build -> {
    if (!(build instanceof MyCrafterBuild b)) return false;
    Recipe r = b.getCurrentRecipe();
    if (r == null) return false;
    for (ItemStack stack : r.inputItems) {
        if (build.items.get(stack.item) < stack.amount) return false;
    }
    return true;
});

// 每 tick 检查
if (multiRecipe.isValid(this)) {
    progress += edelta();
}
```

---

## 6. 与 Mindustry 原版 Consume 的关系

Mindustry 的原版消费系统由以下类组成：
| 原版类 | 用途 |
|--------|------|
| `ConsumeItem` / `ConsumeItems` | 固定物品消耗 |
| `ConsumeLiquid` / `ConsumeLiquids` | 固定液体消耗 |
| `ConsumePower` | 电力消耗（持续/触发） |
| `ConsumeCoolant` | 冷却剂消耗 |
| `Consume` | 抽象基类，可扩展 |

`ConsumeRecipe` 继承 `Consume` 但**未覆写其抽象方法**（未被 @Override 标记），因此：

| 能力 | 是否支持 |
|------|---------|
| 加入方块 `consume()` 链（被原版统计） | ⚠️ 部分支持（需测试） |
| 手动调用 `isValid()` 判断 | ✅ 完全支持 |
| 自动触发扣减资源 | ❌ 不支持（仅条件判断） |
| 自动在详情面板显示消费项 | ⚠️ 需配合 UI 代码 |

> 💡 **定位建议**：ConsumeRecipe 是一个**条件判断辅助器**而非完整消费器。真正的资源扣减仍需调用者自行实现（参考 `GenericCrafterBuild.craft()`）。

---

## 7. 后续增强建议

### 7.1 完全接入原版系统

```java
// 建议覆写原版方法
@Override
public void apply(Block block) {
    block.hasItems = true;  // 确保方块有物品仓
}

@Override
public boolean isOptional(Building build) {
    return false;  // 非可选条件
}

@Override
public void trigger(Building build) {
    // 触发消费：可在此扣减输入资源
}

@Override
public void display(Stats stats) {
    // 在方块面板显示消费详情
}
```

### 7.2 与 Recipe 类深度整合

新增构造器直接接受 Recipe 实例，自动生成判断函数：
```java
public ConsumeRecipe(Recipe recipe) {
    this(build -> {
        for (ItemStack s : recipe.inputItems) {
            if (build.items.get(s.item) < s.amount) return false;
        }
        for (LiquidStack s : recipe.inputLiquid) {
            if (build.liquids.get(s.liquid) < s.amount) return false;
        }
        return true;
    });
}
```

---

**🔗 相关文档**：
- [Recipe 配方系统](./content-Recipe.md)
- [ConfigurableBlock 可配置方块](./newSth-ConfigurableBlock.md)
