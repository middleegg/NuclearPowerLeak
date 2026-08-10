# CI/CD 自动化构建

> **工作流文件**：[.github/workflows/commitTest.yml](file:///d:/NuclearPowerLeak-master/.github/workflows/commitTest.yml)  
> **触发条件**：push（推送） 或 pull_request（合并请求）  
> **运行环境**：GitHub 托管 ubuntu-latest Runner

---

## 1. 工作流总览

本项目通过 **GitHub Actions** 实现推送代码后自动构建跨平台 Mod JAR 的 CI/CD 流水线。无需本地配置 Android SDK，即可产出同时兼容 PC + Android 的发布版 JAR。

### 1.1 工作流元信息

```yaml
name: Build Mod           # 工作流名称（Actions 页面显示）

on: [push, pull_request]  # 触发事件：推送代码 & PR
```

### 1.2 完整流程可视化

```
开发者 push 到 GitHub 仓库
    │
    ▼
GitHub Actions 触发 commitTest.yml
    │
    ├─► 运行环境：ubuntu-latest
    │
    ├─► Step 1: actions/checkout@v4  拉取源码
    │
    ├─► Step 2: 设置 PATH 追加 Android Build Tools 34.0.0
    │
    ├─► Step 3: actions/setup-java@v5  安装 Temurin JDK 17
    │
    ├─► Step 4: Build mod jar
    │      │
    │      ├─ chmod +x ./gradlew
    │      └─ ./gradlew deploy  ← 触发跨平台构建
    │            │
    │            ├─ jar Task          → 桌面版 JAR（含 .class）
    │            └─ jarAndroid Task   → DEX 化 JAR（含 classes.dex）
    │            └─ deploy Task       → 合并二为一 + 清理中间产物
    │
    └─► Step 5: actions/upload-artifact@v7 上传构建产物
           │
           ├─ 路径：build/libs/NuclearPowerLeak-master.jar
           ├─ 名称：NuclearPowerLeak-master（同仓库名）
           └─ archive: false  ← ★ 单文件直接上传，不套 ZIP
```

---

## 2. 各步骤详解

完整 YAML：[commitTest.yml](file:///d:/NuclearPowerLeak-master/.github/workflows/commitTest.yml)

### 2.1 Step 1：Checkout 代码

```yaml
- uses: actions/checkout@v4
```

使用官方 `checkout` Action v4 将当前仓库代码克隆到 Runner 工作目录。  
**效果**：获取当前 push 或 PR 的 HEAD 提交代码。

### 2.2 Step 2：设置 PATH（添加 d8 工具）

```yaml
- name: Set up PATH
  run: |
    echo "${ANDROID_HOME}/build-tools/34.0.0" >> $GITHUB_PATH
```

GitHub 提供的 ubuntu-latest 镜像已预装 Android SDK，其中 `build-tools/34.0.0` 目录包含 `d8` 工具（Gradle `jarAndroid` Task 内部会调用）。通过追加到 `$GITHUB_PATH`，确保后续 Step 中 `d8` 命令可用。

### 2.3 Step 3：安装 JDK 17

```yaml
- name: Set up JDK 17
  uses: actions/setup-java@v5
  with:
    distribution: 'temurin'   # Eclipse Temurin 发行版（Adoptium）
    java-version: 17          # 严格匹配 build.gradle 中 VERSION_17
```

关键点：
- `distribution: 'temurin'`：Mindustry 官方推荐的 JDK 发行版（兼容性最佳）
- `java-version: 17`：严格 17，避免高版本（21+）生成 Android 不支持的字节码

### 2.4 Step 4：运行 Gradle 构建

```yaml
- name: Build mod jar
  run: |
    chmod +x ./gradlew          # Linux 需加执行权限（Windows 自动忽略）
    ./gradlew deploy            # 触发跨平台构建
```

#### deploy Task 内部执行顺序

参考 [build.gradle#L90-L103](file:///d:/NuclearPowerLeak-master/build.gradle#L90-L103)：

```
deploy
  ├── 依赖 jar → 输出 NuclearPowerLeak-masterDesktop.jar（桌面版）
  │
  ├── 依赖 jarAndroid → 运行 d8 命令：
  │   d8 --classpath <Mindustry+Arc+android.jar 等> 
  │      --min-api 14 
  │      --output NuclearPowerLeak-masterAndroid.jar
  │      NuclearPowerLeak-masterDesktop.jar
  │   输出含 classes.dex 的 Android 兼容版
  │
  ├── 合并两个 JAR（以 ZipTree 同时解压后重新打包）
  │     → NuclearPowerLeak-master.jar
  │
  └── doLast 删除临时 Android.jar
```

#### 最终 JAR 内部结构

```
NuclearPowerLeak-master.jar
  ├── Npl/                        桌面版 .class
  │   ├── nu.class
  │   ├── NuUI.class
  │   ├── content/
  │   └── newSth/
  ├── classes.dex                 Android 版字节码（由 d8 产出）
  ├── mod.hjson                   Mod 元数据
  └── assets/                     资源文件
      ├── sprites/
      └── bundles/
```

### 2.5 Step 5：上传构建产物

```yaml
- name: Upload built jar file
  uses: actions/upload-artifact@v7
  with:
    archive: false                        # ★ v7 新增：不套 ZIP 壳，直接上传 JAR
    name: ${{ github.event.repository.name }}   # 产物名 = 仓库名
    path: build/libs/${{ github.event.repository.name }}.jar
```

关键点：
- `archive: false`：upload-artifact@v7 的新参数，单文件时直接保留原格式，避免用户下载后还需解压一次
- `name` 和 `path` 使用 `${{ github.event.repository.name }}` 动态插值 → 当仓库名改变时无需改 YAML

---

## 3. 如何获取 CI 构建产物

### 3.1 Web 界面操作步骤

1. 打开仓库主页 → 点击 **Actions** 标签页
2. 在左侧工作流列表选择 **"Build Mod"**
3. 点击一次成功（绿色 ✓）的运行记录
4. 页面滚动到底部 **Artifacts** 区域
5. 找到名为 `NuclearPowerLeak-master` 的条目点击下载
6. 直接得到 JAR 文件（不需要解压，因为 `archive: false`）

### 3.2 构建产物保留时间

GitHub 默认保留 Artifacts **90 天**（公开仓库）。若需永久保存，应配合 Draft Release 或 Tags 上传。

---

## 4. 本地 vs CI 构建对比

| 维度 | 本地 gradlew deploy | GitHub Actions CI |
|------|-------------------|------------------|
| 环境准备 | 需自行安装 JDK17 + Android SDK + d8 | 完全托管，零配置 |
| 构建耗时 | 约 10s~30s（冷启动更长） | 约 2~4 分钟（含环境初始化） |
| 平台兼容验证 | 只验证本机系统 | 干净的 Ubuntu + 官方 Android SDK |
| 产物获取 | 本地 build/libs/ | Actions Artifacts 下载 |
| 成本 | 免费（占用本机） | 免费账户每月 2000 分钟 Action 额度 |
| 失败排查 | 直接看本地终端 | 查看 Job 日志（带时间戳 + 彩色） |

---

## 5. 常见 CI 失败原因

| 错误现象 | 可能原因 | 修复方法 |
|---------|---------|---------|
| `./gradlew: Permission denied` | 缺少 Step 中 chmod | 确认 chmod +x ./gradlew 步骤存在 |
| `No valid Android SDK found` | ANDROID_HOME 变量空或路径错 | Runner 默认自带，若缺失需用 setup-android Action |
| `d8: command not found` | Build Tools 未加入 PATH | 确认 PATH 设置步骤正确，版本号 34.0.0 存在 |
| `CompileWithJdk17` 类版本错 | JDK 版本不是 17 | 确认 setup-java 的 `java-version: 17` |
| 依赖下载失败 | 网络访问 GitHub Releases 受限 | 换个时间段重试；或改用国内镜像仓库（企业版 Runner） |
| 中文文件名 Sprite 丢失 | 文件系统编码 | 确保提交时 PNG 文件路径为 UTF-8；必要时改为英文命名 |

---

## 6. 工作流增强建议（可选）

### 6.1 添加构建缓存，缩短构建时间

```yaml
# 在 checkout 后、setup-java 前添加：
- name: Setup Gradle
  uses: gradle/actions/setup-gradle@v3
  with:
    cache-read-only: false
```

### 6.2 发布 Tag 时自动上传到 GitHub Releases

```yaml
# 新 Job：
release:
  needs: buildJar
  if: startsWith(github.ref, 'refs/tags/v')
  steps:
    - uses: actions/download-artifact@v4
      with: { name: NuclearPowerLeak-master }
    - uses: softprops/action-gh-release@v1
      with:
        files: NuclearPowerLeak-master.jar
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

### 6.3 自动运行测试（如后续添加 JUnit）

```yaml
- name: Run tests
  run: ./gradlew test
```

### 6.4 Java 版本矩阵测试（验证兼容性）

```yaml
strategy:
  matrix:
    java: [ '17', '21' ]
```

但注意：Mindustry Mod 当前官方推荐 17，21 仅验证构建通过性，最终发布版使用 17。

---

## 7. 安全提示

- `${{ secrets.GITHUB_TOKEN }}` 为 GitHub 自动注入的短令牌，无需手动创建 Secret
- 工作流中不要 `echo` 输出敏感环境变量值（即使是系统自带的）
- 若构建脚本改为 `pull_request_target` 触发，**不要**执行来自 PR 的不可信代码（存在 Secrets 泄露风险）
- 当前 `on: [push, pull_request]` 为安全组合，pull_request 沙箱不暴露写权限

---

**🔗 相关文档**：
- [构建与运行](./构建与运行.md)（本地构建三模式详解）
- [系统架构](./系统架构.md)
