# NewItemsType 扩展物品类型

> **所属包**：`Npl.newSth`  
> **源文件**：[NewItemsType.java](file:///d:/NuclearPowerLeak-master/src/Npl/newSth/NewItemsType.java)  
> **类型**：类型扩展类（继承自 Mindustry 原版 Item）  
> **依赖**：`ModStats`（自定义Stat）、`arc.graphics.Color`

---

## 1. 模块职责

`NewItemsType` 是对 Mindustry 原版 `mindustry.type.Item` 的**直接扩展**，在不修改原版代码的前提下，为 NuclearPowerLeak 的 16 种自定义物品新增三种专属属性：

| 新增属性 | 含义 | 显示方式 |
|----------|------|---------|
| `reversible` (可逆性) | 参与可逆反应的效率 | 百分比（×100 后加 %） |
| `magentic` (磁性) | 磁力作用强度 | 百分比 |
| `stability` (稳定性) | 化学/物理稳定性（允许负值） | 百分比 |

> 🔬 扩展实现思路：
> 1. 继承 `Item` 添加 3 个 float 字段
> 2. 覆写 `setStats()` 调用原版 `stats.addPercent()` 将新字段写入 UI
> 3. ModStats 中注册对应 Stat 常量，确保名称正确翻译
> 4. 一切无缝接入原版 UI——无需任何自定义面板代码

---

## 2. 类结构

### 2.1 继承体系

```
mindustry.type.Item
        │
        │  extends
        ▼
  Npl.newSth.NewItemsType
```

### 2.2 字段清单

| 字段 | 类型 | 默认值 | 含义 | 范围 |
|------|------|--------|------|------|
| `reversible` | `float` | 0f | 可逆性 | 建议 0 ~ 2.0f（0%~200%） |
| `magentic` | `float` | 0f | 磁性 | 建议 0 ~ 3.0f（瓶装磁暴用到 3.0f） |
| `stability` | `float` | 0f | 稳定性 | 允许负数（瓶装磁暴 = -0.2f 表示极不稳定） |

字段声明位置：[NewItemsType.java#L11-L13](file:///d:/NuclearPowerLeak-master/src/Npl/newSth/NewItemsType.java#L11-L13)

---

## 3. 构造方法

```java
public NewItemsType(String name, Color color) {
    super(name, color);
}
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `name` | String | 物品内部 ID（Mindustry 会自动拼接 `modname-` 前缀） |
| `color` | Color | 物品图标主色（影响 UI 色条、未加载占位色） |

完全透传到父类 `Item(String, Color)`，保持与原版一致的构造语义。

---

## 4. 核心方法：`setStats()`

### 4.1 方法签名

```java
@Override
public void setStats()
```

覆写自 `mindustry.type.Item.setStats()`，由游戏引擎在物品注册后、UI 渲染前调用。

位置：[NewItemsType.java#L19-L31](file:///d:/NuclearPowerLeak-master/src/Npl/newSth/NewItemsType.java#L19-L31)

### 4.2 实现逻辑

```java
@Override
public void setStats() {
    super.setStats();  // ① 先调用父类，添加原版属性（硬度、成本、可燃性等）
    
    // ② 逐个判断扩展属性是否超过阈值 0.01，超过则按百分比加入统计面板
    if (reversible > 0.01f) {
        stats.addPercent(ModStats.Reversible, reversible);
    }
    if (magentic > 0.01f) {
        stats.addPercent(ModStats.Magentic, magentic);
    }
    if (stability > 0.01f) {
        stats.addPercent(ModStats.Stability, stability);
    }
}
```

### 4.3 阈值设计说明

使用 `> 0.01f`（即 > 1%）作为显示门槛：
- **避免 0 值刷屏**：16 种物品大多没有全部属性，没设置的值（=0）不应占面板
- **排除浮点误差**：正常计算下不会出现 0.001 这种"几乎为0"的有意义值
- **⚠️ 不显示负值**：如 `bottledMagenticStorm.stability = -0.2f` 会被条件过滤

> 💡 **若需显示负值**（如"不稳定性"），可调整判断条件，例如：
> ```java
> if (Math.abs(stability) > 0.01f) {
>     stats.addPercent(ModStats.Stability, stability);
> }
> ```
> Mindustry 的 `addPercent` 会正确显示 `-20%`。

---

## 5. 与 ModStats 的配合关系

```
┌──────────────────────────────────┐    ┌─────────────────────────────┐
│      ModStats.java               │    │     NewItemsType.java       │
│                                  │    │                             │
│  Reversible = new Stat(...)  ────┼───►│  if (reversible > 0.01)    │
│  Magentic   = new Stat(...)  ────┼───►│  if (magentic   > 0.01)    │
│  Stability  = new Stat(...)  ────┼───►│  if (stability  > 0.01)    │
└──────────────────────────────────┘    └──────────────┬──────────────┘
                                                        │
                                                        ▼
                                         stats.addPercent(Stat, value)
                                                        │
                                                        ▼
                                         Mindustry 自动渲染到详情面板
```

---

## 6. 使用范例（物品定义）

所有 NuItems 中的自定义物品均以 `NewItemsType` 实例化。完整清单请参考 [NuItems 物品模块](./content-NuItems.md)，以下为典型例子。

### 6.1 磁铁 (magent) - 磁性展示

```java
magent = new NewItemsType("magent", Color.valueOf("b42828")) {{
    alwaysUnlocked = false;
    cost = 0.6f;
    reversible = 0.3f;  // 30% 可逆性
    magentic   = 0.6f;  // 60% 磁性 ✨
    charge     = 0.7f;
}};
```

### 6.2 瓶装磁暴 - 极端属性

```java
bottledMagenticStorm = new NewItemsType("bottledMagenticStorm", Color.valueOf("c0ecff")) {{
    alwaysUnlocked = false;
    reversible = 2f;     // 200% 可逆
    stability  = -0.2f;  // -20% 不稳定（负，但被阈值过滤不显示）
    radioactivity = 1.6f;
    charge     = 3f;     // 300% 电荷
    explosiveness = 5f;  // 500% 爆炸！
    magentic   = 3f;     // 300% 磁性 ✨
}};
```

### 6.3 碱银 (alkSliver) - 高稳定性

```java
alkSliver = new NewItemsType("alkSliver", Color.valueOf("e6e6e6")) {{
    alwaysUnlocked = false;
    cost = 0.8f;
    stability = 1f;  // 100% 稳定（最佳）
}};
```

---

## 7. 属性对游戏机制的影响（当前 vs 潜力）

| 属性 | 当前状态 | 未来可接入的游戏机制 |
|------|---------|-------------------|
| `reversible` | 仅显示 | 可逆化学反应中提高回收率；回收设备上作为效率乘数 |
| `magentic` | 仅显示 | 磁暴炮塔吸附效果；磁选机分离原料；磁力运输带提升速度 |
| `stability` | 仅显示（正） | 负值物品有概率自爆；正值抗爆；衰变时间与稳定性成反比 |

---

## 8. 新增扩展属性的标准流程

若后续需要新增属性（如 `toxicity` 毒性、`hardnessPlus` 额外硬度加成）：

1. **ModStats 加 Stat**：`public static final Stat Toxicity = new Stat("toxicity", StatCat.general);`
2. **NewItemsType 加字段**：`public float toxicity = 0f;`
3. **setStats 加显示逻辑**：
```java
if (Math.abs(toxicity) > 0.01f) {
    stats.addPercent(ModStats.Toxicity, toxicity);
}
```
4. **两个 Bundle 加翻译**：`stat.toxicity = 毒性` 等
5. **NuItems 中赋值**：给对应物品设定属性值
6. **（可选）机制接入**：在相关方块逻辑中读取 `((NewItemsType)item).toxicity` 参与运算

---

**🔗 相关文档**：
- [ModStats 自定义统计](./content-ModStats.md)
- [NuItems 物品模块](./content-NuItems.md)
