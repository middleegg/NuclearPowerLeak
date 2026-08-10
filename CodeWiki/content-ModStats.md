# ModStats 自定义统计模块

> **所属包**：`Npl.content`  
> **源文件**：[ModStats.java](file:///d:/NuclearPowerLeak-master/src/Npl/content/ModStats.java)  
> **类型**：全局常量定义类  
> **依赖**：`mindustry.world.meta.Stat`、`mindustry.world.meta.StatCat`

---

## 1. 模块职责

`ModStats` 是**自定义 Stat（统计属性）注册中心**，解决以下需求：
- 原版 Mindustry 的物品/方块统计面板（如成本、可燃性等）是固定枚举
- 本 Mod 的扩展物品新增了 3 种独有属性（可逆性、磁性、稳定性）
- 通过向游戏注册新的 `Stat` 实例，让新属性自动显示在原版 UI 面板中

> 🔍 **Stat 是什么？** Mindustry 中每个 Item/Block 的属性面板通过 `Stat` 对象作为键值存储。自定义 Stat 可让 Mod 作者无缝扩展属性面板而无需自定义 UI。

---

## 2. 自定义 Stat 列表

### 2.1 所有已定义 Stat

| 字段名 | 内部标识 | 分类 | 中文译名 | 英文原名 | 用途 |
|--------|---------|------|----------|---------|------|
| `Reversible` | `reversible` | `StatCat.general` | 可逆性 | reversible | 表示物品参与可逆反应的能力百分比 |
| `Magentic` | `magentic` | `StatCat.general` | 磁性 | magentic | 表示物品磁力强度百分比 |
| `Stability` | `stability` | `StatCat.general` | 稳定性 | stability | 表示物品化学/物理稳定性，可为负 |
| `Recipe` | `recipe` | `StatCat.general` | 配方 | recipe | 预留用于展示配方 |
| `modeCount` | `modeCount` | `StatCat.general` | 模式数 | modeCount | 预留用于多配方方块展示模式数 |

位置：[ModStats.java#L8-L12](file:///d:/NuclearPowerLeak-master/src/Npl/content/ModStats.java#L8-L12)

### 2.2 代码实现

```java
public class ModStats {
    public static final Stat Reversible = new Stat("reversible", StatCat.general);
    public static final Stat Magentic   = new Stat("magentic",   StatCat.general);
    public static final Stat Stability  = new Stat("stability",  StatCat.general);
    public static final Stat Recipe     = new Stat("recipe",     StatCat.general);
    public static final Stat modeCount  = new Stat("modeCount",  StatCat.general);
}
```

### 2.3 Stat 参数详解

```java
new Stat(String name, StatCat category)
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `name` | String | Stat 的唯一标识，用于国际化 Bundle 查找键名 |
| `category` | StatCat | 分类（影响面板分组），可选值：`general` / `power` / `crafting` / `liquids` / `items` 等 |

---

## 3. Stat 使用流程

### 3.1 全链路流程

```
① ModStats 定义 Stat 常量
        │
        ▼
② NewItemsType.setStats() 中使用 stats.addPercent() 写入值
        │
        ▼
③ Bundle 中配置 stat.<name> 的中英文翻译
        │
        ▼
④ 游戏 UI 自动读取 Stat 列表并渲染到详情面板
```

### 3.2 具体使用位置

| 步骤 | 文件位置 | 说明 |
|------|---------|------|
| ① 定义 | [ModStats.java#L8-L10](file:///d:/NuclearPowerLeak-master/src/Npl/content/ModStats.java#L8-L10) | 实例化 3 个 Stat 对象 |
| ② 写入值 | [NewItemsType.java#L19-L31](file:///d:/NuclearPowerLeak-master/src/Npl/newSth/NewItemsType.java#L19-L31) | 每个物品 setStats() 时写入数值 |
| ③ 翻译-EN | [bundle.properties#L1-L3](file:///d:/NuclearPowerLeak-master/assets/bundles/bundle.properties#L1-L3) | `stat.reversible = reversible` |
| ③ 翻译-CN | [bundle_zh_CN.properties#L2-L4](file:///d:/NuclearPowerLeak-master/assets/bundles/bundle_zh_CN.properties#L2-L4) | `stat.reversible = 可逆性` |
| ④ 渲染 | （Mindustry 引擎内置） | 游戏自动在物品详情页显示 |

---

## 4. 实际使用示例

### 4.1 在 NewItemsType 中写入统计值

[NewItemsType.java#L19-L31](file:///d:/NuclearPowerLeak-master/src/Npl/newSth/NewItemsType.java#L19-L31)

```java
@Override
public void setStats() {
    super.setStats();  // 先添加原版属性（硬度、成本等）
    
    // 仅当值 > 0.01 时显示（避免 0 值污染面板）
    if (reversible > 0.01f) {
        stats.addPercent(Reversible, reversible);  // 按百分比格式显示
    }
    if (magentic > 0.01f) {
        stats.addPercent(Magentic, magentic);
    }
    if (stability > 0.01f) {
        stats.addPercent(Stability, stability);
    }
}
```

> ⚠️ **设计细节**：使用 `> 0.01f` 阈值避免将极小数值或 0 值显示在面板。注意：瓶装磁暴的 `stability = -0.2f`（负值），**不会**被此条件显示。若需要显示负值，需调整判断或改用 `Math.abs()`。

### 4.2 各物品实际生效情况

| 物品 | Reversible | Magentic | Stability | 面板显示效果 |
|------|-----------|----------|-----------|-------------|
| monoSiliCrystal (单晶体) | 40% | - | - | 显示：可逆性 40% |
| magent (磁铁) | 30% | 60% | - | 显示：可逆性 + 磁性 |
| alkSliver (碱银) | - | - | 100% | 显示：稳定性 100% |
| thallide (铊化物) | 200% | - | 20% | 显示：可逆性 200% + 稳定性 20% |
| bottledMagenticStorm | 200% | 300% | -20% | **仅显示可逆性 + 磁性**（负值稳定性被过滤） |

---

## 5. 国际化（Bundle 键名规则）

Mindustry 自动按以下模式查找 Stat 名称：

```
Bundle 键名 = "stat." + Stat.name
```

| Stat 字段 | name 值 | Bundle 键名 | 中文翻译 |
|-----------|---------|-------------|----------|
| Reversible | `reversible` | `stat.reversible` | 可逆性 |
| Magentic | `magentic` | `stat.magentic` | 磁性 |
| Stability | `stability` | `stat.stability` | 稳定性 |

当前翻译文件：
- 英文：[bundle.properties](file:///d:/NuclearPowerLeak-master/assets/bundles/bundle.properties)
- 中文：[bundle_zh_CN.properties](file:///d:/NuclearPowerLeak-master/assets/bundles/bundle_zh_CN.properties)

---

## 6. stats.add / addPercent 方法对比

| 方法 | 格式效果 | 适用场景 |
|------|---------|---------|
| `stats.add(Stat, float)` | 直接显示数值（如 "2.3"） | 成本、温度、硬度等绝对值 |
| `stats.addPercent(Stat, float)` | 数值 × 100 后加 %（如 "40%"） | 可逆性/磁性/稳定性等比率 |
| `stats.add(Stat, Seq<ItemStack>)` | 显示物品图标列表 | 配方输入输出等 |
| `stats.add(Stat, table -> {...})` | 自定义 UI 片段 | 复杂展示（如 ConfigurableBlock 的配方表） |

---

## 7. 预留 Stat 说明

### Recipe & modeCount

```java
public static final Stat Recipe    = new Stat("recipe", StatCat.general);
public static final Stat modeCount = new Stat("modeCount", StatCat.general);
```

这两个 Stat 已定义但当前代码**未实际使用**。推测用途：
- **`Recipe`**：在 ConfigurableBlock 或 GenericCrafter 方块的 setStats() 中，展示所有可用配方
- **`modeCount`**：在 ConfigurableBlock 中，展示该方块支持的模式总数

> 💡 **后续实现建议**：
> ```java
> // 在 ConfigurableBlock.setStats() 中
> stats.add(modeCount, plans.size);
> stats.add(Recipe, table -> { ... 渲染配方表格 ... });
> ```

---

## 8. 扩展新 Stat 的标准步骤

若后续需增加新属性（如 `toxicity` 毒性），按以下步骤：

1. **在 ModStats.java 中添加常量**
```java
public static final Stat Toxicity = new Stat("toxicity", StatCat.general);
```

2. **在 NewItemsType 中添加字段和 setStats 写入**
```java
public float toxicity = 0f;

// setStats() 中：
if (toxicity > 0.01f) stats.addPercent(Toxicity, toxicity);
```

3. **在两个 Bundle 中添加翻译**
```properties
# bundle.properties
stat.toxicity = toxicity
# bundle_zh_CN.properties
stat.toxicity = 毒性
```

4. **在 NuItems 中给对应物品设值**
```java
thallide = new NewItemsType("thallide", ...) {{
    toxicity = 0.95f;  // 铊化物剧毒
}};
```

---

**🔗 相关文档**：
- [NewItemsType 物品类型扩展](./newSth-NewItemsType.md)
- [NuItems 物品模块](./content-NuItems.md)
- [资源与国际化](./资源与国际化.md)
