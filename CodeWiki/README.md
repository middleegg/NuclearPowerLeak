# NuclearPowerLeak - Code Wiki 文档索引

> **项目名称**: NuclearPowerLeak (简称 NPL / nu)  
> **项目类型**: Mindustry Java Mod  
> **当前版本**: 0.1  
> **最低游戏版本**: Mindustry v158  
> **JDK 版本**: 17  
> **构建工具**: Gradle  

---

## 📚 文档导航

### 基础篇
| 文档 | 说明 |
|------|------|
| [项目总览](./项目总览.md) | 项目背景、核心功能、特色介绍 |
| [系统架构](./系统架构.md) | 整体架构设计、模块划分、技术选型 |
| [构建与运行](./构建与运行.md) | 环境准备、构建步骤、安装与运行指南 |
| [依赖关系](./依赖关系.md) | 外部依赖、内部模块依赖关系图 |

### 模块详解篇

#### content 包（内容定义层）
| 文档 | 说明 |
|------|------|
| [NuItems 物品模块](./content-NuItems.md) | 16种自定义物品定义、属性、用途 |
| [NuLiquid 液体模块](./content-NuLiquid.md) | 4种自定义液体定义、特性 |
| [NuBlocks 方块模块](./content-NuBlocks.md) | 自定义生产方块、配方配置 |
| [ModStats 自定义统计](./content-ModStats.md) | 可逆性、磁性、稳定性等自定义Stat |
| [Azer 星球模块](./content-Azer.md) | 自定义星球Azer的规则与生成器 |
| [Recipe 配方系统](./content-Recipe.md) | 通用配方数据结构与链式API |
| [ConsumeRecipe 消费器](./content-ConsumeRecipe.md) | 配方条件验证消费器 |

#### newSth 包（扩展机制层）
| 文档 | 说明 |
|------|------|
| [NewItemsType 物品类型扩展](./newSth-NewItemsType.md) | 扩展物品属性（可逆性/磁性/稳定性） |
| [ConfigurableBlock 可配置方块](./newSth-ConfigurableBlock.md) | 多模式多配方可切换生产方块 |

### 核心入口篇
| 文档 | 说明 |
|------|------|
| [主入口类 nu](./主入口-nu.md) | Mod生命周期、内容加载流程、启动事件 |
| [UI 模块 NuUI](./UI-NuUI.md) | HUD面板、自定义UI组件挂载 |

### 资源与配置篇
| 文档 | 说明 |
|------|------|
| [资源与国际化](./资源与国际化.md) | Sprites资源、多语言Bundle配置 |
| [CI/CD 自动化构建](./CI-CD.md) | GitHub Actions工作流配置解析 |

---

## 🗂️ 项目目录结构速览

```
NuclearPowerLeak-master/
├── src/
│   └── Npl/
│       ├── nu.java                  # Mod 主入口类
│       ├── NuUI.java                # 自定义 UI 模块
│       ├── content/                 # 内容定义包
│       │   ├── NuItems.java         # 自定义物品注册
│       │   ├── NuLiquid.java        # 自定义液体注册
│       │   ├── NuBlocks.java        # 自定义方块注册
│       │   ├── ModStats.java        # 自定义统计属性
│       │   ├── Azer.java            # 自定义星球
│       │   ├── Recipe.java          # 配方数据结构
│       │   ├── ConsumeRecipe.java   # 配方消费器
│       │   └── NuFactory.sl         # 工厂示例脚本
│       └── newSth/                  # 扩展机制包
│           ├── NewItemsType.java    # 扩展物品类型
│           ├── ConfigurableBlock.java # 可配置多配方方块
│           └── RecipeCrafter.sl     # 配方工匠脚本
├── assets/
│   ├── sprites/                     # 图片资源
│   │   ├── items/                   # 物品贴图
│   │   └── liquid/                  # 液体贴图
│   └── bundles/                     # 国际化资源
│       ├── bundle.properties        # 英文
│       └── bundle_zh_CN.properties  # 中文
├── build.gradle                     # Gradle 构建脚本
├── mod.hjson                        # Mod 元数据配置
├── .github/workflows/commitTest.yml # CI/CD 配置
└── CodeWiki/                        # 本 Wiki 文档目录
```

---

## 🔗 快速跳转
- [回到项目总览](./项目总览.md)
- [查看架构设计](./系统架构.md)
- [开始构建运行](./构建与运行.md)
