# NuLiquid 液体模块

> **所属包**：`Npl.content`  
> **源文件**：[NuLiquid.java](file:///d:/NuclearPowerLeak-master/src/Npl/content/NuLiquid.java)  
> **类型**：内容注册器（静态工具类）  
> **依赖**：`mindustry.type.Liquid`、`arc.graphics.Color`

---

## 1. 模块职责

`NuLiquid` 是**自定义液体注册中心**，负责：
1. 声明所有自定义液体的静态字段
2. 在 `load()` 方法中实例化并配置液体的物理/化学属性

与原版 Mindustry 液体体系完全兼容，可直接用于：
- 液体管道运输
- 方块冷却/加热
- 化学反应输入输出
- 与原版液体混合（通过 `canStayOn` 控制）

---

## 2. 液体字段总览

| 字段名 | 中文名 | 颜色 | 温度 | 类型定位 |
|--------|--------|------|------|----------|
| `nuclearFluid` | 核流体 | `#00FF00` 荧光绿 | 5.0（高温） | 核反应冷却/工作介质 |
| `dirtySolution` | 脏溶液 | `#56118B` 深紫 | 4.5（高温） | 核废料溶液 |
| `liquidOxygen` | 液氧 | `#66AAFF` 淡蓝 | -10.0（极低温） | 工业冷却剂 |
| `strangeLiquid` | 奇液 | `#6FA5FF` 蓝紫 | -6.0（低温） | 特殊量子液体 |
| `water` | 水 | （原版引用） | 常温 | 引用原版液体 |

字段声明位置：[NuLiquid.java#L11](file:///d:/NuclearPowerLeak-master/src/Npl/content/NuLiquid.java#L11)

---

## 3. 公共方法

### 3.1 `load()` - 液体注册入口

```java
public static void load()
```

**调用链**：
```
nu.loadContent()
    └──► NuLiquid.load()
          ├──► nuclearFluid = new Liquid("nuclearFluid", ...)
          ├──► dirtySolution  = new Liquid("dirtySolution", ...)
          ├──► liquidOxygen   = new Liquid("liquidOxygen", ...)
          └──► strangeLiquid  = new Liquid("strangeLiquid", ...)
```

调用位置：[nu.java#L33](file:///d:/NuclearPowerLeak-master/src/Npl/nu.java#L33)

---

## 4. 液体属性详解

### 4.1 核流体 (nuclearFluid)

```java
nuclearFluid = new Liquid("nuclearFluid", Color.valueOf("00FF00")) {{
    temperature = 5f;                           // 高温
    lightColor  = Color.valueOf("00FF0071");    // 带 71% 不透明的发光色
}};
```
位置：[NuLiquid.java#L13-L16](file:///d:/NuclearPowerLeak-master/src/Npl/content/NuLiquid.java#L13-L16)

**属性说明**：
| 属性 | 值 | 含义 |
|------|-----|------|
| `temperature` | 5.0 | 工作温度很高（参考：水=0.5，岩浆=1.0+） |
| `lightColor` | `#00FF0071` | 绿色荧光效果（Alpha 通道 ~44%） |

**用途推测**：核反应堆的主冷却剂或工作流体。

---

### 4.2 脏溶液 (dirtySolution)

```java
dirtySolution = new Liquid("dirtySolution", Color.valueOf("56118B")) {{
    temperature   = 4.5f;                        // 高温
    viscosity     = 0.85f;                       // 高粘度（水=1.0，更粘稠）
    flammability  = 3f;                          // ★ 高度可燃 300%
    capPuddles    = false;                       // 不形成水洼（易扩散）
    incinerable   = true;                        // 可焚烧处理
    blockReactive = true;                        // 与方块发生反应
    lightColor    = Color.valueOf("56118BFF");   // 深紫色完全不透明发光
    canStayOn.addAll(water, strangeLiquid, liquidOxygen);  // 可浮于这 3 种液体之上
}};
```
位置：[NuLiquid.java#L17-L26](file:///d:/NuclearPowerLeak-master/src/Npl/content/NuLiquid.java#L17-L26)

**属性说明**：
| 属性 | 值 | 含义 |
|------|-----|------|
| `viscosity` | 0.85 | 比水略粘稠，流速较慢 |
| `flammability` | 3.0 | 可燃性是普通可燃物的 3 倍 |
| `capPuddles` | false | 不自动限制在小水洼，会大范围流动 |
| `incinerable` | true | 可在焚烧设施中处理消除 |
| `blockReactive` | true | 接触方块会发生化学反应（如腐蚀、放热等） |
| `canStayOn` | [water, strangeLiquid, liquidOxygen] | 密度关系：脏溶液会**浮在**这些液体表面，不混合 |

**用途推测**：核工业产生的有毒废料溶液，需特殊处理。由于可燃且具反应性，泄漏风险极大——契合"核泄漏"主题。

---

### 4.3 液氧 (liquidOxygen)

```java
liquidOxygen = new Liquid("liquidOxygen", Color.valueOf("66AAFF")) {{
    temperature  = -10f;                         // ★ 极低温 -10
    lightColor   = Color.valueOf("99CCFFFF");    // 淡蓝半透明荧光
    viscosity    = 0.1f;                         // ★ 极低粘度（超流动）
    heatCapacity = 2.2f;                         // ★ 高比热容 2.2（水=1.0）
    boilPoint    = 1f;                           // 沸点温度 1
    coolant      = true;                         // ★ 标记为冷却剂
}};
```
位置：[NuLiquid.java#L27-L34](file:///d:/NuclearPowerLeak-master/src/Npl/content/NuLiquid.java#L27-L34)

**属性说明**：
| 属性 | 值 | 含义 |
|------|-----|------|
| `temperature` | -10.0 | 极度低温（水=0.5，冷冻液≈-2.5） |
| `viscosity` | 0.1 | 仅水的 1/10，流动极快 |
| `heatCapacity` | 2.2 | 吸收热量能力是水的 2.2 倍，**优秀冷却剂** |
| `boilPoint` | 1.0 | 温度超过 1 即汽化膨胀 |
| `coolant` | true | 标记为冷却剂，可用于冷却型方块 |

**用途推测**：
- 高强度冷却（如核聚变反应堆、数据中心）
- 配合高比热容 + 低粘度 = 快速导热循环
- 低温特性可用于冻结工艺

---

### 4.4 奇液 (strangeLiquid)

```java
strangeLiquid = new Liquid("strangeLiquid", Color.valueOf("6FA5FF")) {{
    temperature  = -6f;                          // 低温
    lightColor   = Color.valueOf("6FA5FFFF");    // 蓝紫完全不透明荧光
    viscosity    = 0.1f;                         // 极低粘度
    heatCapacity = 1f;                           // 常规比热容（同水）
}};
```
位置：[NuLiquid.java#L35-L40](file:///d:/NuclearPowerLeak-master/src/Npl/content/NuLiquid.java#L35-L40)

**属性说明**：
| 属性 | 值 | 含义 |
|------|-----|------|
| `temperature` | -6.0 | 低温但不如液氧极端 |
| `viscosity` | 0.1 | 超快流动 |
| `heatCapacity` | 1.0 | 与水相同的吸热能力 |

**用途推测**：配合 `dirtySolution.canStayOn` 配置，可作为脏溶液的**底层封存液**——把脏溶液浮在奇液之上，形成密封式核废料储存池。

---

## 5. 液体物理属性速查表

| 属性名 | 类型 | 默认参考（水） | 作用 |
|--------|------|---------------|------|
| `temperature` | float | 0.5 | 液体基础温度。正数加热方块，负数冷却方块 |
| `viscosity` | float | 1.0 | 流动阻力。越小流得越快（<0.2 基本无阻力） |
| `heatCapacity` | float | 1.0 | 单位体积吸热能力。越大冷却/保温效果越好 |
| `flammability` | float | 0.0 | 可燃性。>0 会被引燃，高值燃烧更剧烈 |
| `explosiveness` | float | 0.0 | 液体爆炸性 |
| `radioactivity` | float | 0.0 | 液体放射性 |
| `boilPoint` | float | ∞ | 汽化温度阈值。超过则蒸发产生气体 |
| `coolant` | boolean | false | 是否可作为冷却剂被设施选用 |
| `capPuddles` | boolean | true | 是否限制为小水洼（false 会扩散很远） |
| `incinerable` | boolean | false | 是否可通过焚烧清除 |
| `blockReactive` | boolean | false | 是否会接触方块引发反应 |
| `lightColor` | Color | null | 液体在黑暗中的发光颜色 |
| `canStayOn` | Seq<Liquid> | 空 | 密度分层：该液体可浮在列表中液体上方 |

---

## 6. 液体密度与分层关系

由 `dirtySolution.canStayOn.addAll(water, strangeLiquid, liquidOxygen)` 可推出：

```
密度从小 → 大（上方 → 下方）：
───────────────────────────────
  dirtySolution  (浮在最上层)
───────────────────────────────
  water / strangeLiquid / liquidOxygen  (任一种都可做底层)
───────────────────────────────
```

> 🔬 **实际用途**：储存脏溶液的罐体底部先注入奇液或液氧形成"液封"，再注入脏溶液浮在上方，防止其直接接触罐体金属引发反应。

---

## 7. 资源文件对应

| 液体 | 贴图文件 | 路径 |
|------|----------|------|
| nuclearFluid | `nuclearFluid.png` | `assets/sprites/liquid/` |
| dirtySolution | `dirtySolution.png` | `assets/sprites/liquid/` |
| liquidOxygen | `liquidOxygen.png` | `assets/sprites/liquid/` |
| strangeLiquid | `strangeLiquid.png` | `assets/sprites/liquid/` |

---

**🔗 相关文档**：
- [NuItems 物品模块](./content-NuItems.md)
- [NuBlocks 方块模块](./content-NuBlocks.md)
- [资源与国际化](./资源与国际化.md)
