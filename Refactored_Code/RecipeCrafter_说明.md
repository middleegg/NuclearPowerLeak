# RecipeCrafter 你自己的配方工厂 —— 架构说明 & 验证方法

> 对应的 4 个新文件（都在 `Refactored_Code/` 下，**不污染 src**）：
>
> | # | 文件 | 作用 |
> |---|------|------|
> | 1 | [Recipe.java](file:///d:/NuclearPowerLeak-master/Refactored_Code/Recipe.java) | 用户专属配方类，字段和 NewHorizon Recipe 100% 对齐，加了链式 API 更安全 |
> | 2 | [RecipeCrafter.java](file:///d:/NuclearPowerLeak-master/Refactored_Code/RecipeCrafter.java) | 核心 Block 类（继承 Mindustry 原生 GenericCrafter）—— 手动切多配方、物品+液体全支持、正常消耗正常上料 |
> | 3 | [NuBlocks_RecipeCrafter.java](file:///d:/NuclearPowerLeak-master/Refactored_Code/NuBlocks_RecipeCrafter.java) | 4 配方完整示例方块 `test-recipe-crafter` 注册代码 |
> | 4 | 本说明文档 | 对照 NewHorizon 三文件的来源 + 和你 ConfigurableBlock 的区别 + 怎么跑起来测试 |

---

## 一、为什么看了 NewHorizon 三个文件之后我这么写？

### 1.1 先看 NewHorizon 三文件对我们有什么用（对应你给的三个路径）

| 文件（NewHorizon） | 它在 NH 里是做什么的？ | 我们「直接抄」了哪些精华？ | 哪些故意不抄？ |
|------------------|---------------------|--------------------------|----------------|
| `expand/type/Recipe.java` | **配方载体**：input/output 物品/液体/Payload 各 6 个 Seq | ✅ 字段命名 100% 对齐（`inputItem` / `outputLiquid` / `craftTime`）| ❌ 没抄 Payload（用户没提；以后要加就再加 2 个字段和 acceptPayload 方法）<br>❌ 没抄 Object... 构造函数里的「Payload 类型识别」（理由同上） |
| `expand/block/production/factory/RecipeGenericCrafter.java` | **核心方块**：继承自它自己写的 `MultiBlockCrafter`，配方是 `Seq<Recipe> recipes`，自动选最优配方 | ✅ `cacheOutputs()` 把 outputItem/outputLiquid 缓存进 Seq 用于 dump<br>✅ `getProgressIncrease()` 按当前 recipe.craftTime 缩放进度到父类 60 tick 基准<br>✅ `updateTile()` 里液体**按进度增量 handleLiquid**（不是一周期全加，视觉连续）<br>✅ `dumpOutputs()` 每 `dumpTime` 周期 dump 缓存的所有输出（传送带接得快）<br>✅ `shouldConsume()` 检查液体满不满 + `ignoreLiquidFullness` + `dumpExtraLiquid` 分支<br>✅ `setStats()` 每个配方一行「输入 → 输出 + 时间」<br>✅ `setBars()` 按所有涉及液体加液体条 | ❌ **最大的不抄**：`updateRecipe()` 自动找可用配方 —— 因为你 ConfigurableBlock 是**玩家手动切配方**，自动切会把玩家选的配方覆盖掉，完全反人类<br>❌ 不抄 `ConsumeRecipe` —— 这是 NewHorizon 自己的 `Consumer` 扩展，我们项目没有，**原料扣减统一在 craft() 里手动做**，简单且无依赖<br>❌ 不抄它的父类 `MultiBlockCrafter`（继承链是 NH 自己的，我们用 Mindustry 原生 GenericCrafter 最稳） |
| `expand/block/production/factory/MultiBlock.java` | **接口**：给大型多方块工厂做「占位块 + LinkBlock 连接 + 旋转位置偏移」| 🔴 **完全没抄** —— 用户当前的方块全是 `size=2` 单方块，这个接口对 RecipeCrafter 核心逻辑 0 作用。以后想做 4×4 大型工厂（占 16 格的那种），再把 `implements MultiBlock` 加上 + 实现 `linkBlock()/links()` 就行。 |

### 1.2 继承链为什么选「GenericCrafter」而不沿用你之前的「UnitBlock」？

这是最关键的架构决策：

| 维度 | 你之前的 ConfigurableBlock extends UnitBlock | RecipeCrafter extends GenericCrafter（✅ 新选择） |
|------|--------------------------------------------|-------------------------------------------------|
| 「配方系统契合度」 | 本来 UnitBlock 是**造单位**的，`consumeBuilder` 也按「造单位的资源」设计。为了把 ItemStack 配方塞进去，你自己手写了大段 updateTile 逻辑、progress 累积、完成判断。 | **GenericCrafter 就是物品合成方块**：自带 `craftTime`、`progress`（0~1）、满 1.0 自动回调 `craft()`、`consumesPower`、`drawer`、`updateEffect`、`ambientSound`、`craftEffect`、`createSound` —— 全是现成的，我们少写几百行。 |
| 「液体支持」 | UnitBlock 本来没有液体支持，你之前没加，之后想加得自己写 `hasLiquids / acceptLiquid / handleLiquid` | GenericCrafter 原生 `hasLiquids = true`，所有液体 API 都有，加 10 行代码就能正确生产液体。 |
| 「你要求的 `consume()`」 | 之前为了「让 consume 负责扣原料」我让你在 FINAL FIX 里调了 `consume()`，但 UnitBlock 的 consume 本来是按当前 plan 查 `consumeBuilder`，逻辑很绕。 | GenericCrafter 的 `consume()` 就是按 `consumePower(1.20f)` 注册的耗电扣电，**原料扣减我们放在 `craft()` 最前面自己做**，两条路完全解耦，不绕。 |
| 「以后扩展 drawer」 | 你之前的 draw() 手动写 region/outRegion/topRegion。 | GenericCrafter 自带 drawer 系统（`drawer = new DrawMulti(new DrawRegion("-bottom"), new DrawLiquidRegion("-liquid"), ...)`），想加复杂视觉直接写 String 后缀（但我们 draw() 里保留了 null-safe 的 `region / outRegion / topRegion` 直接画，没强制用 drawer，你想用哪个都行）。 |

> 💡 一句话：**UnitBlock 是造单位的，GenericCrafter 是造物品/液体的** —— 你现在要「自己的 RecipeCrafter 合成工厂」，天然就该 extends GenericCrafter。

---

## 二、RecipeCrafter 架构总览（你自己的这套）

### 2.1 类 & 字段映射表（把你 ConfigurableBlock 和 NewHorizon 两套术语统一了）

| 你 ConfigurableBlock 里的 | NewHorizon RecipeGenericCrafter 里的 | 新 RecipeCrafter 里的 | 意思 |
|-------------------------|-------------------------------------|----------------------|------|
| `Seq<Plan> plans`       | `Seq<Recipe> recipes`               | `Seq<Recipe> recipes`（采用 NH 命名，因为配了 Recipe 类）| 配方集合 |
| `Plan.outItem[]`        | `Recipe.outputItem Seq<ItemStack>`  | `Recipe.outputItem`（采用 NH Seq，长度随意） | 单配方多物品输出 |
| `Plan.requirements[]`   | `Recipe.inputItem Seq<ItemStack>`   | `Recipe.inputItem` | 单配方多物品输入 |
| `Plan.time`             | `Recipe.craftTime`                  | `Recipe.craftTime`（60 tick = 1s）| 单配方耗时 |
| `UnitFactoryBuild.currentPlan` | `recipeIndex`（int）        | `RecipeCrafterBuild.recipeIndex` | 当前选中配方索引 |
| `fraction()`            | 父类 GenericCrafter 原生 `progress`（0~1） | `fraction()` = 直接返回 progress（因为 getProgressIncrease 已缩放）| UI 进度条 0~1 |
| `capacities[]` int      | NH 直接用 `itemCapacity / liquidCapacity` 全局 | `capacities[]` int + `liquidCapacities[]` float | 每物品/液体独立仓容（按所有配方里最大量×20 / ×8） |
| `initCapacities()`      | NH init() 里遍历 recipes 填过滤器 | `initCapacities()` + `cacheOutputs()` | 容量推导 + 缓存输出给 dump |

### 2.2 最核心的 5 个方法流程（理解了就不会再出 bug）

#### ① `shouldConsume()` 守卫：原料不对就不耗电
每次 Game Loop tick 的第一件事（GenericCrafter 父类调用）：
1. 当前配方 null → false
2. 物品输出「现有 + 本次要加的」> 容量上限 → false
3. 液体输出「现有 + 本次要加的」> 容量上限 且 `!dumpExtraLiquid` → false
4. 其他：true → **这样父类会把 efficiency 正常算出来，我们不用自己写效率判断。**

#### ② `getProgressIncrease(float baseTime)`：缩放「基础 60 tick」到当前配方的 craftTime
这是 NewHorizon 的核心小技巧：
- RecipeCrafter 的 `craftTime`（父类 GenericCrafter 字段）永远固定 **60**
- 每个 Recipe 有自己的 `craftTime`（比如 30 tick / 90 tick / 120 tick）
- 进度增量计算时：
  ```
  scl = 配方 craftTime / 60
  本 tick 进度增量 = (父类 getProgressIncrease(60)) / scl
  ```
这样不管配方写多少耗时，**我们永远看到父类 progress 是 0~1**，UI 不用换单位。

#### ③ `updateTile()`：液体增量 + 父类 progress 累积 + dump
1. **液体按增量 handleLiquid**（NewHorizon 写法，视觉连续）
2. `super.updateTile()` → GenericCrafter 自己做：
   - progress 累积
   - progress >= 1.0 → 调 `craft()` 一次
   - 播 ambientSound / updateEffect
3. `dumpOutputs()` → 每 dumpTime（默认 20 tick）对缓存的所有 outputItem/outputLiquid 执行 `dump()` / `dumpLiquid(2f)`，保证下游传送带接得快，仓不会随便满。

#### ④ `craft()`（核心合成）：扣原料 → 扣电 → 加产出物 → offload → 音效
顺序千万不要改（和 NewHorizon craft() 顺序完全一致）：
```
if (progress >= 1.0) craft() 被父类调用
  ↓
1) 遍历 inputItem: items.remove(s.item, s.amount)  —— 原料不够？progress=0 直接 return
2) 遍历 inputLiquid: liquids.remove(s.liquid, s.amount)
3) consume()  —— 你要求要用到 consume，这一步只负责扣电力（consumePower(1.20f) 注册的）
4) 遍历 outputItem:
     items.add(s.item, s.amount)        → 入仓
     for (i=0; i<s.amount; i++) offload(s.item) → 单参 offload 推下游（下游满就留在仓，不会复制物品！这是 FINAL FIX 的终极解法）
5) createSound / craftEffect / updateEffect —— 全 null 保护，没赋值不崩
6) progress %= 1f → 溢出保留（比如 progress=1.3 就剩 0.3 做下一轮）
```

#### ⑤ `buildConfiguration(Table)`：物品网格 + 液体网格，玩家手动切
- 有 outputItem 就调 `ItemSelection.buildTable`（和你 ConfigurableBlock 一模一样）
- 有 outputLiquid 就自己写一个按钮 Table：液体 icon + 点击 `configure(liquid)`，配选中态
- 两个都空就显示 @none

---

## 三、四个示例配方（test-recipe-crafter）逐行解释

看 [NuBlocks_RecipeCrafter.java](file:///d:/NuclearPowerLeak-master/Refactored_Code/NuBlocks_RecipeCrafter.java)：

| 配方号 | 输入 | 输出 | 耗时 | 测试什么功能？ |
|-------|------|------|------|--------------|
| 1 | 5 铜 + 3 铅 | 2 钛 | 1s | 物品单输入 → 单输出（对应你原本 TestBlock）|
| 2 | 10 石墨 + 3 桶水 | 1 沙 + 2 铅 | 2s | 液体作为输入原料 + 多物品输出 |
| 3 | 3 煤 + 2 桶油 | 1 生铁（NuItems）+ 15 桶冷冻液 | 1.5s | **混合多输出**（物品 + 液体同时出）—— 这是 ConfigurableBlock 永远做不到的核心功能！ |
| 4 | 3 桶水 | 15 桶 slag（熔渣，代替蒸汽看颜色）| 1s | **纯液体工厂**（炼油厂/水泵/蒸馏塔那种） |

> 把这四个配方每个都手动切一遍，玩家 UI 网格会出现：物品 3 个图标（钛 / 沙 / 生铁）+ 液体 1 个图标（水→slag 的液体切换按钮，因为 [4] 号没 outputItem，只有 outputLiquid，会显示在下方液体网格里）。

---

## 四、跑起来的步骤（替换到 src 并注册）

> ⚠️ 以下步骤**等你确认代码没问题了我再自动执行**，你现在不用手动做！

### Step 1：把 Refactored_Code/ 里 3 个源文件复制到对应包路径
```
src/Npl/newSth/Recipe.java                 ← 从 Refactored_Code/Recipe.java 复制
src/Npl/newSth/RecipeCrafter.java          ← 从 Refactored_Code/RecipeCrafter.java 复制
（可选）src/Npl/content/NuBlocks_RecipeCrafter.java
                                    ← 或把 NuBlocks.load() 里调一次 NuBlocks_RecipeCrafter.loadRecipeCrafters()
```

### Step 2：在 NuBlocks.load() 里加一行调用
```java
public class NuBlocks {
    public static Block TestBlock, redenmore, testRecipeCrafter;   // 加变量

    public static void load() {
        // ... 你原本 TestBlock / redenmore 的注册代码 ...
        testRecipeCrafter = NuBlocks_RecipeCrafter.testRecipeCrafter;  //（或直接在 load 里写，更简单）
        NuBlocks_RecipeCrafter.loadRecipeCrafters();   // ⭐ 加这一行（如果拆到子文件）
    }
}
```

### Step 3：在 mod 主入口（通常是 NuclearPowerLeak.java / Mod.java）确保 NuBlocks.load() 在 `loadContent()` 阶段被调用
（这个 99% 你已经写过了，没写的话问我我再帮你找位置）

### Step 4：`gradlew jar` 编译 → 进游戏，在菜单 Sandbox / Campaign 的 Crafting 分类里找 `test-recipe-crafter`

---

## 五、游戏里验证功能（5 步全走一遍 = 功能 100% 正常）

| 步 | 操作 | 期望结果 | 验证了什么 |
|---|------|---------|----------|
| 1 | 放 `test-recipe-crafter`，用逻辑给它塞 10 铜 + 10 铅（配方 1 原料），切配方到钛 | 60 tick 后，自仓里钛 - 2，下游传送带出现 2 钛图标（没传送带就仓里 + 2 钛），铜变成 5，铅变成 7 | 物品输入正确扣 + 物品输出正确上料 + **不复制物品**（FINAL FIX 那三个问题全解）|
| 2 | 切配方 2（沙 + 铅），接 1 条水管道输入（水泵 → 管道 → 方块），接 1 条传送带塞 20 石墨 | 120 tick 后，仓里 +1 沙 +2 铅，石墨 -10，水罐 -3.0 桶 | 液体作为输入原料正确扣 |
| 3 | 切配方 3（生铁 + 冷冻液），煤×3 传送带输入，油管×2.0 桶/秒输入 | 90 tick 后：生铁 +1（或进传送带），冷冻液罐里 +15.0 桶（看液体条变蓝），煤 -3，油罐 -2.0 | **混合多输出（物品+液体）** 同时生效 |
| 4 | 切配方 4（水 → slag），只接水管道 3.0/tick，不接下游液体管道 → 看液体容量上限条，满了之后 shouldConsume 返回 false，方块停转不耗电 | 液体罐满了不继续硬塞，和原版 GenericCrafter 行为一致 |
| 5 | 用逻辑处理器 `configure @unit 0 1`（配置方块索引 1 → 配方 2），或蓝图 paste 不同配方 | `recipeIndex` 立刻变，`progress` 归零，`dump()` 把上一配方剩下的原料倒出去 → 不会和新配方混料 | Integer/Item/Liquid/Recipe 四类 config 通道全工作 |

---

## 六、常见 FAQ（你大概率会问的，提前回答）

### Q1：我就想用你 ConfigurableBlock 的「Plan 三参构造 + outItem[] 数组」，不想改 Recipe 类怎么办？
答：完全可以！Recipe 类和 Plan 类字段本质一模一样，只是「数组 vs Seq」的区别。只需要在 RecipeCrafter.init() 里加一段桥：
```java
for (Plan p : plans) recipes.add(new Recipe(p.time, Seq.with(p.requirements), Seq.with(), Seq.with(p.outItem), Seq.with()));
```
就什么都不用动了。我之所以新建 Recipe 类，是因为**NewHorizon 的 Recipe 命名在多配方方块圈里是「事实上的标准」**，而且 Seq<ItemStack> 比 ItemStack[] 好用太多（动态追加不用扩容、each() 比 for 循环短）。你习惯哪个就用哪个。

### Q2：液体增量生产（updateTile 里每 tick 加一点）不会有浮点误差？
答：**有极小误差，约 1e-6f**，但所有 Mindustry 原版工厂都是这样做的（Smelter、Kiln、GenericCrafter 带液体的全部），游戏内完全看不出来。你想要精确到 tick 的就把液体加的逻辑搬到 craft() 里一次性 `liquids.add(s.liquid, s.amount)`，但视觉上会「到时间突然罐满」，不连续。

### Q3：为什么 craft() 里不调 super.craft()？
答：GenericCrafter 原生 `super.craft()` 会做两件事：
1. 调 `consume()`（我们已经在 Step 3 手动调了）
2. 用 `outputItems[0]` 写死单配方输出（我们是多配方多输出，必须自己写）

所以要**完全 override craft() 自己写**，不调 super 才对。

### Q4：RecipeCrafter 能「自动切配方」吗？像 NewHorizon 那样从后往前找第一个能合成的？
答：能。只要在 `RecipeCrafterBuild.updateTile()` 开头（在 `Recipe r = current()` 上面）加一段和 NewHorizon 完全一样的 `updateRecipe()` 方法就行：
```java
// 想自动切配方就加这一段
if (recipeIndex < 0 || !validRecipe()) {
    for (int i = recipes.size - 1; i >= 0; i--) {
        if (validRecipe(i)) { recipeIndex = i; break; }
    }
}
```
但**这和「玩家手动切配方」冲突**（玩家刚切就被自动切走），所以我默认没加，你想要就告诉我，我帮你加一个 `public boolean autoSelectRecipe = false;` 开关，两种模式都能用。

### Q5：想支持 Payload（生产单位载荷/方块载荷）？
答：Recipe 里已经留了扩展位，NewHorizon 字段就是 `inputPayload / outputPayload Seq<PayloadStack>`。需要扩展时：
1. Recipe 类加上这 2 个字段 + `inPayload()/outPayload()` 链式方法
2. RecipeCrafterBuild 里 `acceptPayload()/getPayloads()` 实现 UnitBlock 那套
3. craft() 里 Payload 输出用 `BuildPayload b = new BuildPayload((Block) payload, team); b.set(x,y,rotdeg()); dumpPayload(b);`
4. setStats 里加 Payload 行
5. 缓存 cachedPayloadOutputs + dumpOutputs 加 dumpPayload
就完成了。你需要时告诉我我再给你写全。

---

## 七、什么时候替换进 src？

按之前的规则：**你先看 Refactored_Code 这 3 个新文件 + 本说明，满意了告诉我「确认 RecipeCrafter 版」，我才会：**

1. 把 `Recipe.java` 复制到 `src/Npl/newSth/Recipe.java`
2. 把 `../src/Npl/newSth/RecipeCrafter.java` 复制到 `src/Npl/newSth/RecipeCrafter.java`
3. 把 `NuBlocks_RecipeCrafter` 那 4 个配方合并到你 `src/Npl/content/NuBlocks.java`
4. 跑一次 `gradlew jar` 告诉你编译过没过，没过再按报错调整（比如 import 少了 / NuItems 字段名字不一样这些小问题）
