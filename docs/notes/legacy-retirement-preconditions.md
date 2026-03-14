# Legacy Retirement Preconditions

更新时间：2026-03-14

## 用途
- 这份文档保留 `bag/legacy/**` 退休前置条件的最终结论。
- 当前该主题已完成，不再作为 active blocker checklist。

## 当前政策摘要
- `bag/legacy/**` 已于 `2026-03-13` 从 `libs/audio_core/include/` 删除。
- 当前 direct owner 集合保持为空，不允许回流。
- `retirement_policy.py` 当前同时阻止 deleted legacy header path 回流与 direct `#include "bag/legacy/..."` token 回流。

## 当前状态摘要
- Android 已从 root host `--no-modules` baseline 语义中拆出，当前只保留 `apps/audio_android/native_package/ -> bag_android_native` 的独立 packaging lane。
- Android package-private native exception 继续作为 Android 独立平台偏差跟踪，但当前已固定到 Android `C++23` baseline，不再与已删除的 `bag/legacy/**` surface 绑定。
- `--no-modules` formal baseline cutover 已完成；旧 `verify --build-dir build/legacy-host --skip-android --no-modules` 不再是正式 CI / release baseline。
- host `OFF` / `bag_api.cpp` / `audio_core` 主仓 fallback 链已经完成 closeout，不再是当前 `legacy` 退休主题的现役 blocker。
- 主仓 `bag/internal/**` owner 已清零；预留接口头当前通过 `bag/interface/common/*` 保留独立声明层。
- 若需要回顾 `Phase 3 / 4` 的历史执行底稿，应查看 `temp/archive/legacy-preconditions-burn-down*.md` 与同目录下的 closeout 快照，而不是重新把这些主题当成现役执行面。

## 退休前置条件追踪

| 条目 | 最终状态 | 说明 |
| --- | --- | --- |
| Android 已从 `bag/legacy/**` 删除 blocker 中拆出 | 满足 | Android 当前仍保留 package-private native exception，但它已不再直接或间接消费 `bag/legacy/**`，并已固定到 Android `C++23` baseline；该平台偏差继续由 Android 独立 lane 跟踪，不再阻塞 legacy 删除后的维持。 |
| `--no-modules` baseline / host `OFF` off-path 已正式退出 | 满足 | root host `WAVEBITS_HOST_MODULES=OFF` 已退休；正式 CI / release 现在只保留 `verify --build-dir build/dev --skip-android` 与 Android focused gate。 |
| `bag/legacy/**` 当前批准 owner 已全部清空 | 满足 | `bag_api.cpp`、`audio_core/src/*.cpp`、`unit_tests.cpp` 和预留接口头都已退出直接 legacy owner 集合，当前 direct owner 为 `0`。 |
| 预留接口头已有不依赖 `legacy` 的替代性声明入口 | 满足 | `link_layer.h`、`fun_phy.h`、`pro_phy.h` 的非-modules 分支已切到 `bag/interface/common/*`，主仓 `bag/internal/**` owner 已为 `0`。 |
| docs / verify / CI 在不依赖 `legacy` 的前提下仍具备等价保护 | 满足 | `retirement_policy.py` 已改成 post-delete guard，current-state docs / release gates 也已切到 post-legacy narrative；legacy 删除后继续具备等价保护。 |

## 当前结论
- `bag/legacy/**` 的删除前置条件与最终删除执行均已完成。
- 删除前 dry-run 与删除后 gate 验证都已完成。
- 当前已不再需要把以下问题继续登记为 blocker：
  - Android package-private native exception 是否继续存在
  - host `WAVEBITS_HOST_MODULES=OFF` 是否继续作为正式 baseline
  - `bag_api -> transport.facade -> flash/pro/ultra` 主仓 fallback 链是否还要继续保留
  - 预留接口头是否仍直接依赖主仓 `bag/internal/**`
- 若后续再出现 `bag/legacy/**` 路径或 include token，应视为 regression，而不是重新回到“是否满足前置条件”的讨论。

## 使用方式
- 只有在需要更新 post-legacy 审计结论或记录回流回归时，再更新这份文档。
- 当前上表已全部满足且删除已完成；后续不应重新把它当作删除前 checklist。
