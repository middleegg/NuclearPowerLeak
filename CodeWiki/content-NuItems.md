# NuItems 物品模块

> **所属包**：`Npl.content`  
> **源文件**：[NuItems.java](file:///d:/NuclearPowerLeak-master/src/Npl/content/NuItems.java)  
> **类型**：内容注册器（静态工具类）  
> **依赖**：`NewItemsType`（扩展物品类型）、`ModStats`（自定义统计）

---

## 1. 模块职责

`NuItems` 是**自定义物品注册中心**，负责：
1. 声明所有自定义物品的静态字段（供全局引用）
2. 在 `load()` 方法中实例化并配置每个物品的属性
3. 引用原版物品（sand、graphite、pyratite）提供统一访问入口

---

## 2. 物品字段总览

### 2.1 自定义物品（16 种，NewItemsType）

全部 16 种自定义物品均为 `NewItemsType` 类型，支持扩展属性（可逆性、磁性、稳定性）。

| # | 字段名 | 中文名 | 颜色（十六进制） | 解锁状态 | 分类标签 |
|---|--------|--------|-----------------|----------|----------|
| 1 | `bigIron` | 生铁 | `#7e7e7e` | 始终解锁 | 基础矿物 |
| 2 | `Tcoal` | T型煤 | `#9c9480` | 需解锁 | 燃料矿物 |
| 3 | `sulFurFrag` | 硫芯 | `#b44632` | 需解锁 | 反应矿物 |
| 4 | `monoSiliCrystal` | 单晶体 | `#616161` | 需解锁 | 工业材料 |
| 5 | `magent` | 磁铁 | `#b42828` | 需解锁 | 工业材料 |
| 6 | `frailPolyester` | 脆纶 | `#00b1ff` | 需解锁 | 有机材料 |
| 7 | `pumice` | 气浮石 | `#c8c8c8` | 需解锁 | 工业材料 |
| 8 | `oriRubber` | 原胶 | `#956f4e` | 需解锁 | 有机原料 |
| 9 | `rubberFrag` | 胶种 | `#956f4e` | 需解锁 | 有机原料 |
| 10 | `alkSliver` | 碱银 | `#e6e6e6` | 需解锁 | 金属材料 |
| 11 | `rubber` | 橡胶 | `#432f1f` | 需解锁 | 工业材料 |
| 12 | `uranCrystal` | 铀晶 | `#9adba1` | 需解锁 | 核矿物 |
| 13 | `oriUranium` | 原铀 | `#50826e` | 需解锁 | 核矿物 |
| 14 | `thallide` | 铊化物 | `#9a7da1` | 需解锁 | 剧毒矿物 |
| 15 | `uranium` | 铀 | - | 需解锁 | 第一灾厄 |
| 16 | `bottledMagenticStorm` | 瓶装磁暴 | `#c0ecff` | 需解锁 | 第三灾厄 |

字段声明位置：[NuItems.java#L14-L18](file:///d:/NuclearPowerLeak-master/src/Npl/content/NuItems.java#L14-L18)

### 2.2 原版物品引用（3 种，Item）

| 字段名 | 对应原版物品 | 用途 |
|--------|-------------|------|
| `sand` | `mindustry.content.Items.sand` | 沙 |
| `graphite` | `mindustry.content.Items.graphite` | 石墨 |
| `pyratite` | `mindustry.content.Items.pyratite` | 硫化物（雷管材料） |

位置：[NuItems.java#L115-L117](file:///d:/NuclearPowerLeak-master/src/Npl/content/NuItems.java#L115-L117)

---

## 3. 公共静态方法

### 3.1 `load()` - 物品注册入口

```java
public static void load()
```

**作用**：由 `nu.loadContent()` 调用，一次性实例化所有物品。

**调用链**：
```
nu.loadContent()
    └──► NuItems.load()
          ├──► new NewItemsType("bigIron", ...)
          ├──► new NewItemsType("Tcoal", ...)
          ├──► ... (共 16 个物品)
          └──► sand/graphite/pyratite 原版引用赋值
```

调用位置：[nu.java#L32](file:///d:/NuclearPowerLeak-master/src/Npl/nu.java#L32)

---

## 4. 物品属性详解（按分类）

### 4.1 基础矿物类

#### 生铁 (bigIron)
```java
new NewItemsType("bigIron", Color.valueOf("7e7e7e")){{
    hardness = 1;        // 挖掘硬度
    cost = 1.3f;         // 建造成本权重
    alwaysUnlocked = true;  // 开局即解锁
    healthScaling = 0.01f;  // 方块血量加成
}};
```
位置：[NuItems.java#L21-L26](file:///d:/NuclearPowerLeak-master/src/Npl/content/NuItems.java#L21-L26)

**设计意图**：作为 Azer 星球初始资源和最基础的工业材料。

### 4.2 燃料与反应矿物

#### T型煤 (Tcoal)
```java
hardness = 2;
flammability = 1.7f;  // 可燃性（原版煤为 1.0）
explosiveness = 0.2f; // 爆炸性
```
比原版煤炭热值更高，形状经过辐射影响改变。

#### 硫芯 (sulFurFrag)
```java
hardness = 2;
cost = 0.3f;
radioactivity = 0.6f;  // 放射性
flammability = 1.0f;
explosiveness = 1.5f;  // 高爆炸性
```
地表裸露的硫矿石，兼具放射性与爆炸性。

### 4.3 工业材料类

#### 单晶体 (monoSiliCrystal)
```java
cost = 0.7f;
reversible = 0.4f;     // ★ 扩展属性：可逆性 40%
```
基础工业材料，附带可逆反应属性。

#### 磁铁 (magent)
```java
cost = 0.6f;
reversible = 0.3f;     // 可逆性 30%
magentic = 0.6f;       // ★ 扩展属性：磁性 60%
charge = 0.7f;         // 电荷属性
```
纯净的磁性铁材料，三灾厄中瓶装磁暴的基础材料之一。

#### 脆纶 (frailPolyester)
```java
cost = 0.6f;
hardness = 1;
flammability = 0.4f;
charge = 0.3f;
```
辐射侵蚀后的有机纤维，强度下降但仍可用。

#### 气浮石 (pumice)
```java
cost = 1.3f;
hardness = 3;           // 较高硬度
charge = 0.6f;
```
含钙盐的浮石材料，含微量银成分。

#### 碱银 (alkSliver)
```java
cost = 0.8f;
stability = 1f;         // ★ 扩展属性：稳定性 100%
```
碱性银金属，化学性质极度稳定。

### 4.4 有机橡胶产业链

| 物品 | 定位 | 关键属性 |
|------|------|----------|
| **原胶 (oriRubber)** | 原料（从枯死植物采集） | `flammability = 2.35f`, `hardness = 3` |
| **胶种 (rubberFrag)** | 中间品（采种机培养） | `flammability = 1.45f` |
| **橡胶 (rubber)** | 最终产品（工业基石） | `flammability = 2.0f`, `cost = 0.9f` |

### 4.5 核灾厄系列（核心特色）

#### 铀晶 (uranCrystal)
```java
cost = 0.8f;
radioactivity = 0.9f;   // 高放射性
explosiveness = 0.4f;
```
闪耀的翠绿色宝石，灾厄的象征。

#### 原铀 (oriUranium)
```java
cost = 0.5f;
radioactivity = 1.2f;   // ★ 极高放射性
explosiveness = 0.6f;
```
灾厄始源——未提纯的原始铀矿。

#### 铊化物 (thallide)
```java
reversible = 2f;        // 高可逆性
stability = 0.2f;       // ★ 稳定性仅 20%（低）
radioactivity = 0.8f;
explosiveness = 0.45f;
```
**第二灾厄**：人为将未到来的灾厄封锁在剧毒晶体中，稳定性很低（易泄漏）。

#### 瓶装磁暴 (bottledMagenticStorm)
```java
reversible = 2f;
stability = -0.2f;      // ★ 负稳定性！极不稳定
radioactivity = 1.6f;   // 极高放射性
charge = 3f;            // 极高电荷
explosiveness = 5f;     // ★ 爆炸性 500%！
magentic = 3f;          // ★ 磁性 300%！
```
**第三灾厄**：本项目最危险的物品——负稳定性意味着它会不断失控。爆炸性 5 倍、磁性 3 倍、电荷 3 倍，象征人类联邦葬礼的礼炮。

位置：[NuItems.java#L106-L114](file:///d:/NuclearPowerLeak-master/src/Npl/content/NuItems.java#L106-L114)

---

## 5. 物品属性速查表（原版属性 + 扩展属性）

| 属性名 | 类型 | 含义 | 来源 |
|--------|------|------|------|
| `hardness` | float | 挖掘/采集难度 | Mindustry 原版 Item |
| `cost` | float | 建造成本权重 | Mindustry 原版 Item |
| `alwaysUnlocked` | boolean | 是否无需科技解锁 | Mindustry 原版 Item |
| `healthScaling` | float | 物品做建材时的血量加成 | Mindustry 原版 Item |
| `flammability` | float | 可燃性 (0-1+) | Mindustry 原版 Item |
| `explosiveness` | float | 爆炸性 (0-1+) | Mindustry 原版 Item |
| `radioactivity` | float | 放射性 (0-1+) | Mindustry 原版 Item |
| `charge` | float | 电荷/能量属性 | Mindustry 原版 Item |
| ★ `reversible` | float | 可逆性（百分比显示） | **NewItemsType 扩展** |
| ★ `magentic` | float | 磁性（百分比显示） | **NewItemsType 扩展** |
| ★ `stability` | float | 稳定性（百分比显示，可为负） | **NewItemsType 扩展** |

---

## 6. 使用范例

### 6.1 在方块配方中引用

```java
// NuBlocks.java 中 test-block 使用生铁作为建材
requirements(Category.crafting, with(NuItems.bigIron, 10));

// 配方输入输出
recipes[2] = new Recipe(Items.copper, 3, Items.lead, 2)
    .output(NuItems.bigIron, 1)
    .craftTime(60f);
```
位置：[NuBlocks.java#L70-L75](file:///d:/NuclearPowerLeak-master/src/Npl/content/NuBlocks.java#L70-L75)

### 6.2 在 Azer 星球初始物资中使用

```java
r.loadout = ItemStack.list(
    NuItems.bigIron, 100   // 开局给 100 生铁
);
```
位置：[Azer.java#L53-L55](file:///d:/NuclearPowerLeak-master/src/Npl/content/Azer.java#L53-L55)

---

**🔗 相关文档**：
- [NewItemsType 扩展类型](./newSth-NewItemsType.md)
- [ModStats 自定义统计](./content-ModStats.md)
- [NuBlocks 方块模块](./content-NuBlocks.md)
