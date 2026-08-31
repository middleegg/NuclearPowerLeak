# Sprites 文件夹结构说明

## 📁 rouge/ - Rouge 模式贴图

### rouge/maps/ - 地图预览图
用于 Rouge 战役中各关卡的预览图片。

| 文件名 | 对应地图 | 尺寸建议 |
|--------|----------|----------|
| frozenForest.png | 冻土森林 | 256x256 |
| crateredBattleground.png | 陨石坑战场 | 256x256 |
| extractionOutpost.png | 前哨提取站 | 256x256 |
| frontier.png | 边境 | 256x256 |
| groundZero.png | 原点 | 256x256 |
| impact0078.png | 撞击点0078 | 256x256 |
| nuclearComplex.png | 核子复合体 | 256x256 |
| onset.png | 起始 | 256x256 |
| overgrowth.png | 蔓生 | 256x256 |
| saltFlats.png | 盐滩 | 256x256 |
| siege.png | 围攻 | 256x256 |
| stronghold.png | 要塞 | 256x256 |
| tarFields.png | 焦油田 | 256x256 |
| weatheredChannels.png | 风蚀河道 | 256x256 |
| windsweptIslands.png | 迎风群岛 | 256x256 |
| aegis.png | 神盾 | 256x256 |
| atlas.png | 阿特拉斯 | 256x256 |
| atolls.png | 环礁 | 256x256 |
| basin.png | 盆地 | 256x256 |
| biomassFacility.png | 生物质设施 | 256x256 |
| caldera.png | 火山口 | 256x256 |
| coastline.png | 海岸线 | 256x256 |
| crevice.png | 裂缝 | 256x256 |
| crossroads.png | 十字路口 | 256x256 |
| desolateRift.png | 荒芜裂谷 | 256x256 |
| facility32m.png | 32号设施 | 256x256 |
| fallenVessel.png | 坠落飞船 | 256x256 |
| fungalPass.png | 真菌通道 | 256x256 |
| infestedCanyons.png | 侵染峡谷 | 256x256 |
| intersect.png | 交汇点 | 256x256 |
| karst.png | 喀斯特 | 256x256 |
| lake.png | 湖泊 | 256x256 |
| marsh.png | 沼泽 | 256x256 |
| peaks.png | 山峰 | 256x256 |
| ravine.png | 峡谷 | 256x256 |
| ruinousShores.png | 毁灭海岸 | 256x256 |
| split.png | 分裂 | 256x256 |
| stainedMountains.png | 染血山脉 | 256x256 |
| sunkenPier.png | 沉没码头 | 256x256 |
| taintedWoods.png | 污染树林 | 256x256 |
| littoralShipyard.png | 滨海船厂 | 256x256 |
| navalFortress.png | 海军要塞 | 256x256 |
| mycelialBastion.png | 菌丝堡垒 | 256x256 |
| perilousHarbor.png | 危险港湾 | 256x256 |
| cruxscape.png | 十字地带 | 256x256 |

### rouge/nodes/ - 节点图标
用于 Rouge 战役地图上的节点显示（圆形节点）。

| 文件名 | 用途 | 尺寸建议 |
|--------|------|----------|
| node-normal.png | 普通节点（未解锁） | 64x64 |
| node-locked.png | 锁定节点 | 64x64 |
| node-completed.png | 已完成节点 | 64x64 |
| node-boss.png | Boss节点 | 64x64 |
| node-start.png | 起始节点 | 64x64 |
| node-end.png | 终点节点 | 64x64 |

### rouge/ui/ - Rouge 模式UI贴图
Rouge 模式特有的UI元素。

| 文件名 | 用途 | 尺寸建议 |
|--------|------|----------|
| branch-choice-bg.png | 分支选择背景 | 400x300 |
| evac-button.png | 撤离按钮 | 128x48 |
| header-bg.png | 顶部标题背景 | 800x64 |
| progress-bar.png | 进度条 | 256x16 |
| progress-bar-bg.png | 进度条背景 | 256x16 |

---

## 📁 techtree/ - 外场演绎（科技树）贴图

### techtree/nodes/ - 科技树节点图标
科技树中各个科技节点的图标。

| 文件名 | 用途 | 尺寸建议 |
|--------|------|----------|
| tech-weapon.png | 武器科技 | 48x48 |
| tech-defense.png | 防御科技 | 48x48 |
| tech-production.png | 生产科技 | 48x48 |
| tech-power.png | 能源科技 | 48x48 |
| tech-unit.png | 单位科技 | 48x48 |
| tech-upgrade.png | 升级科技 | 48x48 |
| tech-locked.png | 锁定状态 | 48x48 |
| tech-researched.png | 已研究状态 | 48x48 |

### techtree/backgrounds/ - 科技树背景
科技树界面的背景图片。

| 文件名 | 用途 | 尺寸建议 |
|--------|------|----------|
| tech-bg-main.png | 主背景 | 1920x1080 |
| tech-bg-panel.png | 面板背景 | 400x600 |

### techtree/icons/ - 科技树分类图标
科技树分类标签的图标。

| 文件名 | 用途 | 尺寸建议 |
|--------|------|----------|
| cat-military.png | 军事分类 | 32x32 |
| cat-industry.png | 工业分类 | 32x32 |
| cat-science.png | 科学分类 | 32x32 |
| cat-special.png | 特殊分类 | 32x32 |

---

## 📁 ui/sectors/ - 地图小图标（已存在）
用于地图上显示的小尺寸图标，命名与地图文件名一致。

---

## 📝 贴图制作规范

### 格式要求
- 格式：PNG（带透明通道）
- 色彩模式：RGBA
- 命名规范：小写英文，使用连字符分隔（如 `frozenForest.png`）

### 风格建议
- 地图预览图：使用游戏内截图或渲染图，保持一致的视觉风格
- 节点图标：使用圆形设计，与圆形节点匹配
- UI元素：扁平化设计，与 Mindustry 原生UI风格一致

### 分辨率参考
- 小图标：32x32 或 64x64
- 中等图标：128x128 或 256x256
- 背景图：1920x1080 或根据实际需求调整
