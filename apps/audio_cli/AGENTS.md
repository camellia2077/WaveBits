# apps/audio_cli Agent Rules

- `apps/audio_cli` 是当前 CLI 表现层主目录；正式主线在 `apps/audio_cli/rust/`。
- 先读本文件的“快速定位 / 当前代码边界 / 编译与测试”，再决定是否继续下钻。
- 若改动涉及共享业务逻辑或跨边界接线，优先读这些文档，不要先全量 `rg`：
  - `docs/architecture/repo-map.md`
  - `docs/architecture/compatibility-layer-inventory.md`
  - `docs/design/transports.md`
  - `docs/testing.md`
  - `docs/presentation/README.md`
  - `docs/presentation/cli/v0.2/0.2.0.md`

## 快速定位

- 优先按职责找入口，不要默认从最大文件或全仓搜索开始。
- 常见入口速查：
  - CLI 参数与子命令定义：`rust/src/cli.rs`
  - 命令分发与 encode/decode 主流程：`rust/src/commands.rs`
  - `bag_api` FFI 封装：`rust/src/bag_api.rs`
  - `audio_io` WAV / metadata FFI 封装：`rust/src/audio_io_api.rs`
  - 文本 / 文件读写：`rust/src/fs_io.rs`
  - 进度条等终端表现逻辑：`rust/src/progress.rs`
  - 错误类型：`rust/src/error.rs`
  - 程序入口：`rust/src/main.rs`
  - crate 组织与常量：`rust/src/lib.rs`
  - Rust 单测：`rust/src/tests.rs`
  - CLI 集成测试：`rust/tests/cli.rs`
  - Cargo 与依赖：`rust/Cargo.toml`
  - Rust CLI 到根仓构建的桥接：`rust/build.rs`

## 当前代码边界

- CLI 当前是 Rust `clap` 表现层，不要把共享业务逻辑重新写进 `apps/audio_cli`。
- 文本 `<->` PCM 编解码优先通过 `libs/audio_api/include/bag_api.h` 对应的 Rust FFI 封装接入。
- mono PCM16 WAV 与 `WBAG` metadata 优先通过 `libs/audio_io/include/audio_io_api.h` 对应的 Rust FFI 封装接入。
- transport mode / 字符集规则以 `docs/design/transports.md` 为准：
  - `flash` 不限字符集
  - `pro` 仅允许 ASCII
  - `ultra` 面向 UTF-8
- 如果只是改 CLI 表现层，通常不需要先深入 `libs/audio_core/`。
- 如需改共享边界或验证“允许依赖什么”，先看 `docs/architecture/compatibility-layer-inventory.md`，避免误判主线边界。

## 常见任务应该看哪里

- 改命令参数、帮助文案、子命令结构：
  - `rust/src/cli.rs`
  - `rust/src/tests.rs`
  - `rust/tests/cli.rs`
- 改 encode/decode 行为、输出文案、stdout/stderr 契约：
  - `rust/src/commands.rs`
  - `rust/src/error.rs`
  - `rust/tests/cli.rs`
- 改与 `bag_api` 的接线：
  - `rust/src/bag_api.rs`
  - `libs/audio_api/include/bag_api.h`
  - `libs/audio_api/src/bag_api.cpp`
- 改 WAV、`WBAG` metadata 或 `audio_io` 接线：
  - `rust/src/audio_io_api.rs`
  - `libs/audio_io/include/audio_io_api.h`
  - `libs/audio_io/include/wav_io.h`
  - `libs/audio_io/src/audio_io_api.cpp`
  - `libs/audio_io/src/wav_io_bytes_impl.inc`
- 改终端进度条或交互式输出行为：
  - `rust/src/progress.rs`
  - `rust/src/commands.rs`
- 改文件输入输出与 UTF-8 文本读取：
  - `rust/src/fs_io.rs`
- 改构建、链接、静态库搜索路径：
  - `rust/build.rs`
  - `rust/cmake/CMakeLists.txt`
  - 根仓 `CMakeLists.txt`

## 修改时的注意事项

- CLI 的 `stdout` 主要承载用户可消费结果；进度、诊断或临时 UI 优先考虑 `stderr`，避免破坏脚本调用。
- 改帮助文案、命令参数或错误提示时，同步检查：
  - `rust/src/tests.rs`
  - `rust/tests/cli.rs`
- 改 FFI 结构体、enum 或 ABI 常量时，必须同步对齐：
  - Rust 侧 `rust/src/bag_api.rs` / `rust/src/audio_io_api.rs`
  - C/C++ 侧头文件与实现
- 不要在 CLI 层重新发明 transport 校验、WAV metadata 解析或业务规则；优先复用共享库边界。

## 编译与测试

- 编译与测试优先从仓库根目录通过 `python tools/run.py cli ...` 执行。
- Rust CLI 的中间产物默认落在 `build/<dir>/rust-cli/target/`，最终可执行文件默认复制到 `build/<dir>/bin/`。
- 需要先整理 Rust 代码格式时，优先运行：
  - `cargo fmt`
- 修改 CLI presentation 版本时，优先运行：
  - `python tools/run.py cli bump-version <version>`
  - 只把 `Cargo.toml` 作为版本号来源；`Cargo.lock` 必须由 Cargo 自动刷新，不手工编辑。
- 修改 Rust CLI 后，最小编译与测试验证优先运行：
  - `python tools/run.py cli test`
- 需要做优化构建时，优先运行：
  - `python tools/run.py cli build --release`
- 若只想检查真实 CLI 子进程行为，可定向运行：
  - `cargo test --test cli`
- 需要检查主仓 host 工作流是否仍然连通时，运行更大范围验证：
  - `python tools/run.py verify --build-dir build/dev --skip-android`
