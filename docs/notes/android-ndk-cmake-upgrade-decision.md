# Android NDK/CMake Upgrade Decision Gate

更新时间：2026-03-14

## 推荐阅读顺序
- `1/2`：先读 `docs/notes/android-import-std-toolchain-status.md`，确认当前 Android lane 的正式状态。
- `2/2`：再读本文件，判断未来何时才值得评估 `NDK/CMake` 升级。
- 交接给后续 agent 时，建议把这份文档作为第二份材料发送。

## 文档定位
- 这份文档是给后续 agent 的正式交接材料。
- 它回答的问题不是“现在要不要立刻升级”，而是：
  - 何时值得评估 Android `NDK/CMake` 升级
  - 升级前必须先确认哪些事实
  - 升级后哪些结果才算真实有效
  - 哪些情况应立即停止并回滚
- 它的目标是让后续 agent 在看不到 `temp/` 临时材料的情况下，仍能独立做出正确判断。
- 当前 Android `import std;` 的正式状态仍以：
  - `docs/notes/android-import-std-toolchain-status.md`
  为准。

## 一句话结论
- 截至 2026-03-14，当前 Android lane 已经证明：
  - `C++23` baseline 成立
  - 最小 named-modules owner shift 成立
- 但还没有证明：
  - Android toolchain 已具备 host-style `import std;` 所需的标准库模块 provider
- 因此，未来是否升级 `NDK/CMake`，不应按“版本越新越好”判断，而应按“是否出现新的官方信号与可验证收益”判断。

## 当前已确认的固定基线
- 当前正式 Android lane：
  - `Windows + externalNativeBuild + AGP 9.0.1 + Gradle 9.2.1 + CMake 4.1.2 + NDK 28.2.13676358`
- 当前仓库内实际 pin 位于：
  - `apps/audio_android/app/build.gradle.kts`
- 当前 Android native 入口继续使用：
  - `externalNativeBuild`
  - `apps/audio_android/app/src/main/cpp/CMakeLists.txt`
- 当前 Android packaging 路线仍是：
  - app 侧通过 `bag_api.h` 消费 native 能力
  - `native_package` 使用 package-owned implementation sources
  - host 与 Android 当前通过 `_impl.inc` / `bag_api_impl.inc` 共享实现体

## 当前已验证事实
1. Android native 已稳定运行在 `C++23` baseline。
2. Android `modules-smoke` 目前只证明最小 direct-owner shift：
   - `bag.common.version`
3. clean PATH 且 fresh `.cxx` 目录下，以下命令都已通过：
   - `python tools/run.py android modules-smoke --clean`
   - `python tools/run.py android modules-smoke`
   - `python tools/run.py android native-debug`
4. 当前 Android lane 不能被描述为：
   - “已经支持 host-style `import std;`”
   - “已经具备 Android-local `libc++` standard-library module provider”
5. 当前 Android `modules-smoke` 的意义是：
   - Android packaging graph 已可承接最小 named-module owner shift
   - 不等于 Android 标准库模块 provider 已经成立

## 当前关键证据

### 1. `msys64` 污染曾经真实存在
- 在未清理 PATH 的宿主环境中，bare probe 曾误命中：
  - `C:/msys64/ucrt64/lib/libc++.modules.json`
- 这说明宿主环境可能把 Android 实际状态“伪装成 provider 可用”。

### 2. clean PATH + fresh `.cxx` 后，真实问题收敛为 provider 缺失
- 在 clean PATH 且 fresh configure 后，最新 report 收敛为：
  - `cxx_import_std=unavailable`
  - `libcxx_modules_json_query=libc++.modules.json`
  - `libcxx_modules_json_exists=0`
  - `libcxx_modules_json_origin=missing`
  - `import_std_status=blocked-missing-libcxx-modules-json`
- 这个结果比“被 `msys64` 污染”更重要，因为它说明：
  - 清掉宿主污染之后
  - 当前 Android lane 仍没有找到 toolchain 自己提供的标准库模块 metadata/provider

### 3. 之前暴露过一个与 provider 无关的 wiring 问题，但已修复
- fresh configure 曾暴露：
  - `bag/common/version_generated.h` wiring 缺口
- 该问题已经修复，并已在 clean PATH + fresh `.cxx` 下复核通过。
- 因此当前 remaining blocker 不再是 generated-header wiring，而是 provider 缺失本身。

## 这份文档要防止的错误判断
1. 不要把“Android 已能跑 `C++23`”误解成“Android 已能跑 `import std;`”。
2. 不要把“`modules-smoke` 通过”误解成“Android 标准库模块 provider 已就绪”。
3. 不要把“clean PATH 后 build 通过”误解成“当前工具链已经正式支持 host-style `import std;`”。
4. 不要把“本机升级了某个工具”误解成“仓库中的 Android lane 已完成升级”。
5. 不要把“碰运气换个更新版本”当成正式的技术判断。

## 什么时候值得评估升级
1. 当前 clean/fresh gate 仍稳定，且 remaining blocker 继续收敛为：
   - `blocked-missing-libcxx-modules-json`
2. 我们准备推进下一阶段 Android owner 迁移，而目标范围已经依赖：
   - Android direct-owner 扩面
   - `_impl.inc` / `bag_api_impl.inc` 的退休前提
   - 更靠近 host-style `import std;` 的 Android lane
3. 候选版本的官方资料中，至少出现以下一种强信号：
   - Android `NDK` 官方 release notes 明确提到 `libc++` modules、标准库模块 metadata、`import std;` 或等价 provider 能力
   - `CMake` 官方 release notes / docs 明确提到 Android 上的 `import std;`、`CMAKE_CXX_COMPILER_IMPORT_STD`、标准库模块 provider 发现逻辑或修复
   - Android `externalNativeBuild` 对 C++ modules / standard-library modules 的兼容性有正式说明
4. 候选升级能够保持当前 Android app 基线整体兼容，至少要明确：
   - `AGP`
   - `Gradle`
   - `JDK`
   - app-side `CMake`
   - `NDK`
5. 当前工作窗口允许重跑完整 Android gate，并接受失败后立即回滚。

## 什么时候不值得评估升级
1. 当前失败模式还没收敛，仍混着：
   - PATH 污染
   - 旧 `.cxx` 缓存
   - generated-header wiring 问题
   - 与 provider 无关的普通 Android 构建故障
2. 没有任何官方资料暗示候选版本改进了 Android `import std;` provider。
3. 升级需要顺带牵动大范围：
   - `AGP`
   - `Gradle`
   - `JDK`
   - Android Studio
   但当前并没有独立的平台升级需求。
4. 升级动机只是“希望结构更统一”或“想试试看”，而不是解决明确的下一阶段技术阻塞。
5. 当前不能接受保留旧 pin、旧结论与回滚路径。

## 决策前必须先完成的本地复核
1. 在新的 clean PATH 会话中执行，不允许 PATH 残留 `C:\\msys64\\*`。
2. 删除 Android `.cxx` 目录，确保是 fresh configure，而不是沿用旧 cache。
3. 重新执行：
   - `python tools/run.py android modules-smoke --clean`
   - `python tools/run.py android modules-smoke`
   - `python tools/run.py android native-debug`
4. 必须检查 fresh report，至少覆盖：
   - `arm64-v8a`
   - `x86_64`
5. 必须记录以下 report 字段：
   - `cmake_version`
   - `android_ndk_version`
   - `cxx_import_std`
   - `libcxx_modules_json_query`
   - `libcxx_modules_json_exists`
   - `libcxx_modules_json_origin`
   - `import_std_status`
6. 必须保留当前 bare probe 与 target-aware probe 的结果。
7. 必须复核仓库内 pin，而不是只看本机安装版本：
   - `apps/audio_android/app/build.gradle.kts`

## 后续 agent 必须在线核实的官方信息
- 这些信息具有时效性，未来 agent 不能仅凭这份文档回忆，必须重新在线查官方来源并写清具体日期。
1. Android `NDK` 官方 release notes。
2. `CMake` 官方 release notes 与 C++ modules / `import std;` 官方文档。
3. Android Gradle Plugin 官方 release notes 或兼容矩阵。
4. 如有联动升级需要，再查：
   - `Gradle` 官方兼容矩阵
   - Android Studio / SDK 官方安装与兼容说明
5. 优先使用官方原始来源，不要用二手博客代替。

## 推荐的决策顺序
1. 先确认当前基线仍是：
   - `C++23` 成立
   - provider 仍缺失
2. 再确认当前 remaining blocker 仍是：
   - `blocked-missing-libcxx-modules-json`
3. 之后才去查候选 `NDK/CMake` 是否有新的官方信号。
4. 若官方资料没有明确提到 Android `libc++` modules / `import std;` / provider 集成变化：
   - 直接判定为“不值得升级”
5. 若只有 `NDK` 有强信号：
   - 优先评估最小 `NDK` 升级面
6. 若 `CMake` 也有明确相关信号：
   - 再评估 `NDK + app-side CMake` 联动升级
7. 不要默认同时升级：
   - `AGP + Gradle + NDK + CMake`
   除非官方兼容性要求如此
8. 如候选 `CMake` 不能被 Android SDK / `externalNativeBuild` 正式安装和消费：
   - 直接停止，不做“名义升级”

## 升级实施时的范围约束
1. 版本 pin 变更必须落在仓库显式控制点，不接受“本机环境改了但仓库没变”。
2. 优先保持最小升级面：
   - 能只升 `NDK` 就不要顺带升 `AGP/Gradle`
   - 能只升 app-side `CMake` 就不要误动 root host lane
3. 任何联动升级都要写清为什么是被兼容性迫使，而不是顺手带上。
4. 升级试验期间不要同时引入 Android ownership 结构大改，否则无法判断收益到底来自版本升级还是源码重构。

## 升级后必须重跑的验证
1. `python tools/run.py android modules-smoke --clean`
2. `python tools/run.py android modules-smoke`
3. `python tools/run.py android native-debug`
4. `python tools/run.py android assemble-debug`
5. 检查 fresh `.cxx` report，至少覆盖：
   - `arm64-v8a`
   - `x86_64`
6. 如要复核 provider 发现逻辑，再补跑：
   - bare probe
   - Android target-aware probe

## 什么结果才算升级真的有效
1. `modules-smoke` report 不再停留在：
   - `blocked-missing-libcxx-modules-json`
2. 如果发现 provider，它必须来自 Android toolchain 本地来源，而不是宿主环境外部路径。
3. 不接受以下“假成功”：
   - 只在旧 `.cxx` 缓存下成功
   - 只在单一 ABI 下成功
   - `libcxx_modules_json_origin=foreign`
   - `native-debug` 或 `assemble-debug` 回归失败
4. 如果 report 显示 provider 可用，但 Android gate 仍不稳定：
   - 只能说明“工具链可能有改善”
   - 不能改写正式口径为“Android 已支持 `import std;`”
5. 只有在 clean PATH + fresh `.cxx` + 多 ABI + Android gate 可重复通过时，才值得新开下一阶段 Android `import std;` 扩面计划。

## 结果判定矩阵
| 结果 | 解释 | 后续动作 |
| --- | --- | --- |
| provider 仍缺失，但 Android gate 通过 | Android 仍只是 `C++23` baseline | 维持现状，不改正式状态 |
| provider 命中外部宿主路径 | 仍有环境或缓存污染 | 先修隔离，再重新判断 |
| provider 成立，但只在部分 ABI 或旧缓存下成立 | 结果不稳定 | 不改正式状态，继续排查 |
| provider 成立，且 fresh clean PATH 下多 ABI gate 通过 | 当前版本升级具备明确收益 | 更新正式 docs，并新开 Android `import std;` 计划 |
| 升级需要额外牵动大量 Android 平台组件 | 问题已超出本专题 | 单开 Android 平台升级议题 |

## 回滚要求
1. 版本 pin 变更必须独立、清晰、可读。
2. 如果升级失败，应恢复：
   - app-side `NDK` pin
   - app-side `CMake` pin
   - 与之配套的 Android 构建接线
3. 在 gate 未稳定之前，不要改写正式状态文档。
4. 无论升级成功还是失败，都应留下新的正式记录，至少包含：
   - 日期
   - 实际版本组合
   - clean/fresh gate 结果
   - 是否发现 Android-local provider
   - 是否需要联动 `AGP/Gradle/JDK`

## 与当前 Android 策略的关系
- 这份文档不改变当前正式策略：
  - Android 仍是独立 `C++23` packaging lane
  - 当前不把 Android 写成已正式支持 host-style `import std;`
- 这份文档只定义一个升级判断 gate：
  - 未来何时值得尝试
  - 尝试后怎样才算成功

## 交接结论
- 当前最合理的默认动作不是“升级”，而是“保持现状并等待新的官方信号”。
- 只有当以下三件事同时成立时，才值得发起升级评估：
  1. 当前 blocker 仍稳定收敛为 provider 缺失
  2. 官方 release notes / docs 出现新的明确信号
  3. 能以最小升级面重跑并验证完整 Android gate
- 如果做不到这三点，后续 agent 应维持当前口径：
  - Android `C++23` baseline 已成立
  - Android `import std;` provider 仍未成立
