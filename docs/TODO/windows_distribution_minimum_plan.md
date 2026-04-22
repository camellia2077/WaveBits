# Windows 可分发目标的最小落地清单

更新时间：2026-04-21

## 目标

目标不是“为了迁移而迁移”，而是让 `FlipBits.exe` 在 Windows 上更容易分发给普通用户使用：

- 用户电脑不需要安装 MSYS2
- 用户电脑不需要安装 MinGW 运行时
- 用户尽量不需要额外补 DLL
- 交付物尽量接近“拿到就能运行”

同时保持长期架构方向不跑偏：

- `clang++ / Clang-family` 继续作为开发主线与跨平台主线
- Android 继续沿用 Clang/NDK 心智
- 未来 Linux 继续优先复用 `clang++ / Clang-family` 路径
- `clang-cl + x86_64-pc-windows-msvc + /MT` 只作为 Windows 发布 lane
- Windows 发布 lane 是新增能力，不是替换现有主线

当前已知阻碍：

- 现有 CLI 产物依赖 `libstdc++-6.dll`、`libwinpthread-1.dll`、`libgcc_s_seh-1.dll`
- 当前 native 构建链还带有 GNU / MSYS2 / `pkg-config` / `sndfile` 路径假设
- 仓库里 Rust CLI 当前固定走 `x86_64-pc-windows-gnu`

## 按风险从高到低的步骤

### 1. 先确认 native 第三方依赖能不能跟着一起切到 Windows/MSVC 口径

这是风险最高的一步，也是决定这次方案能不能真正落地的一步。

要确认的核心不是 Rust，而是 native 依赖链：

- `libs/audio_io` 当前通过 `pkg-config` 发现 `sndfile`
- 现有环境里解析到的是 MSYS2 提供的 `sndfile`
- 这条链天然带有 GNU / MSYS2 假设

要达成“普通 Windows 用户拿到就能跑”的目标，这一步必须先有明确答案：

- `libsndfile` 是否继续保留
- 如果保留，是否能提供 MSVC ABI 可用版本
- 如果不能，是否要改成项目内受控构建
- 如果仍然不稳定，是否需要替换或临时移除该依赖路径

完成标准：

- 明确 `libsndfile` 在新 Windows 发布链中的来源和 ABI 口径
- 不再把能否分发建立在用户机器已有 MSYS2 的前提上

### 2. 建立一条专门面向 Windows 分发的 host lane

目标不是把整个仓库永久锁死为 Windows-only，而是新增一条“Windows 发布 lane”。

这条 lane 的建议形态：

- `clang-cl`
- `x86_64-pc-windows-msvc`
- `Ninja`

这样做的原因：

- 这条 lane 更符合 Windows 最终交付需求
- 可以摆脱当前 MinGW 运行时 DLL 依赖方向
- 也更适合作为“正式对外分发”的宿主构建链

这里要加一条硬约束：

- 不替换现有 `clang++` 开发主线
- 不影响 Android 当前构建心智
- 不预先把未来 Linux 路线改造成 MSVC 风格
- Windows 发布 lane 必须是单独命名、单独 build 目录、单独命令入口

这一步需要统一以下约束：

- 根 CMake 不再只接受当前 GNU 风格 `clang++` lane
- CLI 的 Rust target 不再固定为 `x86_64-pc-windows-gnu`
- `tools/run.py` 与文档里不再把 GNU target 当唯一真理
- 但默认开发命令仍然应保留 `clang++ / Clang-family` 主线心智

完成标准：

- 新建独立 build 目录时，能以 `clang-cl + x86_64-pc-windows-msvc` 完成 configure
- 至少能单独编译 native 静态库，不先要求最终 CLI 一步到位

### 3. 把 C/C++ 运行时切成静态 CRT

目标是减少 Windows 最终交付物对额外运行时 DLL 的依赖。

这一步的重点不是“改一个编译开关”，而是：

- 对 native targets 统一使用 `/MT`
- Debug 目标使用对应的静态 Debug CRT
- 避免同一发布链里混用 `/MT` 和 `/MD`

这样做能解决的是：

- 减少 CRT 缺失风险
- 降低“在开发机能跑、用户机打不开”的概率

这一步不能解决的是：

- 并非所有第三方依赖都会因此消失
- 仍然要看第三方库本身是静态还是动态

完成标准：

- 编译配置里能稳定看到 `/MT`
- native 产物不再依赖可避免的 MSVC CRT 动态库

### 4. 把 Rust CLI 接到新的 Windows 发布 lane

在 native lane 稳定后，再切 Rust CLI。

这里要改的是发布链，不是功能逻辑：

- Rust target 从 `x86_64-pc-windows-gnu` 切到 `x86_64-pc-windows-msvc`
- 移除当前 `build.rs` 中 GNU-only 假设
- 去掉当前 MinGW 风格的 `stdc++` 链接心智
- 让 Rust 最终消费的 native 静态库与 Rust target 处在同一 ABI 口径

这一步的真正目标是：

- 让 `FlipBits.exe` 不再依赖 MinGW 运行时 DLL
- 让 Windows 最终可执行产物符合新的发布 lane

完成标准：

- CLI 在新 target 下成功编译
- `FlipBits.exe` 不再要求 `libstdc++-6.dll`、`libwinpthread-1.dll`、`libgcc_s_seh-1.dll`

### 5. 用“干净用户机”思路验证交付物

这一步是实际目标验收，不是附带检查。

要验证的不是“开发机上能不能运行”，而是：

- 把 `FlipBits.exe` 和计划交付的必要文件复制到一个临时目录
- 不依赖开发机的 PATH
- 不依赖 MSYS2 安装
- 直接执行 `version`、`licenses`、`encode`、`decode`

建议重点看：

- `dumpbin` / `objdump` / `ldd` 的导入表
- 实际隔离目录运行结果
- 是否仍残留意外 DLL 依赖

完成标准：

- 当前 CLI 主路径在干净目录下通过
- Windows 最终交付清单可明确列出

### 6. 最后收口 tooling、文档和 legal 口径

只有当前面的发布链真正稳定后，才值得统一更新外围材料。

需要收口的内容包括：

- `tools/run.py` 的默认开发命令与单独的 Windows 发布命令
- CLI 构建文档与 AGENTS 文档
- legal 文档里关于 MinGW 运行时 DLL 的描述
- 最终发布目录结构与 notices 放置方式

这里的默认值策略必须明确：

- 默认开发/跨平台构建口径继续以 `clang++ / Clang-family` 为主
- Windows 发布使用显式命令入口，不和默认开发命令混在一起
- 文档中要明确区分“开发主线”和“Windows 发布 lane”

完成标准：

- 新的 Windows 发布命令对团队成员是清晰可用的
- 文档和实际产物一致
- legal 文档与最终分发形态一致

## 最小里程碑建议

### 里程碑 A：先打通“新的 Windows 发布链”

只关注：

- `libsndfile` 等 native 依赖口径
- `clang-cl + x86_64-pc-windows-msvc`
- `/MT`

暂时不要求最终交付体验完美。

验收标准：

- native 静态库能稳定编译
- Rust CLI 能接上新 lane

### 里程碑 B：再把交付体验做成“普通用户可用”

再完成：

- 无 MinGW 运行时 DLL 依赖
- 干净目录运行验证
- tooling / docs / legal 收口

验收标准：

- 普通 Windows 用户在没有 MSYS2 的机器上也能直接运行交付物

## 参考项目对照

可参考项目：`C:\code\time_tracer`

该项目值得借鉴的点：

- Windows CLI 明确采用 `x86_64-pc-windows-msvc`
- 明确要求 Visual Studio Build Tools、正确的 `cl.exe` / `link.exe` 与 Windows SDK
- 明确提醒不要被错误的 `link.exe` 或 PATH 劫持
- 使用统一的 `python tools/run.py ...` 命令入口
- 文档中把“Windows 构建前置条件”和“Windows CLI 专属说明”单独写清楚

该项目不应被机械照搬的点：

- 它的 Windows CLI 运行时核心是 `tracer_core.dll`，而 WaveBits 当前不是这套架构
- 它的第三方依赖问题不等同于 WaveBits 当前的 `libsndfile` 问题
- 它“没有 MinGW 那批 DLL”不等于“完全没有 Windows DLL”
- WaveBits 不应为了模仿它而把整个仓库主线改成 Windows-only

对 WaveBits 的实际启示是：

- 应借鉴它对 `windows-msvc`、VS/SDK 前置条件、命令入口和文档分层的做法
- 不应直接照搬它的 `core.dll` 架构和依赖处理策略
- WaveBits 仍应保持 `clang++ / Clang-family` 作为开发主线，只把 Windows 发布 lane 独立出来

## 一句话判断

如果目标是“让没有安装 MSYS2 的普通 Windows 用户也能直接使用”，那么这套方向是匹配目标的。

真正的高风险点不在 Rust CLI 本身，而在 native 依赖链是否能整体切到 Windows/MSVC 发布口径。
