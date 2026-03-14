# Legacy Release Gates

更新时间：2026-03-13

## 用途
- 这份文档定义当前与 `legacy` 支持政策相关的持续门禁与发布前门禁。
- 目标不是重新定义架构，而是把已经定稿的支持策略固定进 CI 与 release 流程。

## 持续门禁

### CI workflow
- 文件：
  - `.github/workflows/verify-host-and-legacy.yml`
- 当前职责：
  - 在 `pull_request` 和 `push -> main` 上运行
  - 验证当前静态检查分组可列出
  - 验证 host 默认 modules 主路径

### CI 当前执行命令
- `python tools/run.py verify --list-checks`
- `python tools/run.py verify --build-dir build/dev --skip-android`

### 为什么 CI 默认跳过 Android
- 当前 CI 的主要职责是持续锁定：
  - host 默认 modules 主路径
- Android 当前仍是受支持例外路径，但它已经通过 `apps/audio_android/native_package/ -> bag_android_native` 独立成平台 gate。
- 因此 Android 继续保留在发布前显式门禁里，而不是借 host `verify` 的默认步骤隐式表达。

## 发布前门禁

### 必跑命令
- `python tools/run.py verify --list-checks`
- `python tools/run.py verify --build-dir build/dev --skip-android`
- `python tools/run.py android native-debug`
- `python tools/run.py android assemble-debug`

### 发布前必须确认的文档项
- 当前 release notes 已存在：
  - `docs/core/vX.Y.Z.md`
- 当前版本总览已同步：
  - `docs/core.md`
- 当前 `legacy` 支持政策文档口径未漂移：
  - `docs/architecture/compatibility-layer-inventory.md`
  - `docs/architecture/module-topology.md`
  - `docs/testing.md`
  - `tools/README.md`

## 各门禁负责什么

### `verify --list-checks`
- 负责确认静态检查分组仍然能准确表达当前长期规则。
- 尤其要确认：
  - `retirement` 组仍在表达 post-legacy 的 `bag/legacy/**` deleted / no-reintroduction 口径

### `verify --build-dir build/dev --skip-android`
- 负责确认：
  - host 默认 modules 主路径可编、可测
  - 不把 Android gate 混进 host 主路径语义

### `android native-debug`
- 负责确认：
  - Android `native_package -> bag_android_native` 装配链仍可独立 configure/build
  - app `CMake` 不会回退到直接 `add_subdirectory(libs/audio_core)` 或 `add_subdirectory(libs/audio_api)`
  - Android native package 继续只编译 `audio_core` package-owned implementation sources、`bag_api` package-owned boundary implementation 与 `android_bag/**` 私有声明层

### `android assemble-debug`
- 负责确认：
  - Android `Gradle + externalNativeBuild` 装配仍可通过
  - APK 侧集成没有被 host modules / legacy 收口改动破坏

## 当前 gate 语义
- host 默认 modules 路径是正式主路径。
- Android gate 是显式独立的例外路径，不再挂在 host `--no-modules` baseline 语义下。
- root host `WAVEBITS_HOST_MODULES=OFF` 已退休。
- CI 当前只跑 host 主路径；Android 保留在发布前显式 gate。
- `bag/legacy/**` 已删除，不再属于发布支持面；任何 legacy 路径或 include token 回流都应被静态门禁阻止。
