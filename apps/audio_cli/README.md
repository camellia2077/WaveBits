# audio_cli

这个目录当前承载 Rust CLI 主线：

- `rust/`
  - 使用 `clap` 的 Rust CLI。
  - 已经通过 `bag_api` 复用真实 text <-> PCM 编解码能力。
  - 已经通过最小 `audio_io` C ABI 复用真实 mono PCM16 WAV 读写能力。

## 当前 Rust CLI

- 包名 / 可执行名：`binary_audio_cpp`
- 表现层版本：`0.1.1`
- 构建标签：`rust-wav`
- 当前状态：
  - 提供 `version` / `encode` / `decode`
  - `version` 会输出 Rust presentation 版本、`core` 版本和当前 build 标签
  - `encode` 输出真实 mono PCM16 WAV 文件
  - `decode` 读取带 WaveBits metadata 的真实 WAV 文件，不再需要显式传入 `--mode`
  - 当前已经调用 `bag_api` 和最小 `audio_io` C ABI

## 使用方式

在 `apps/audio_cli/rust` 下运行：

```bash
cargo run --target x86_64-pc-windows-gnu -- version
cargo run --target x86_64-pc-windows-gnu -- encode --text "Hello" --out temp/output.wav
cargo run --target x86_64-pc-windows-gnu -- decode --in temp/output.wav
```

如需写出解码文本：

```bash
cargo run --target x86_64-pc-windows-gnu -- decode --in temp/output.wav --out-text temp/decoded.txt
```

## 说明

- 当前 CLI 已经不再写自定义 `.stub` 容器，而是写真实 WAV。
- 根仓 `CMake` 主构建已经会通过 Cargo 构建这个 Rust CLI，`python tools/run.py verify --build-dir build/dev` 也会先跑 `cargo test` 再跑现有 `ctest` CLI smoke。
- 当前 `audio_io` 只暴露了最小 bytes-based C ABI；如果后续需要 WBAG metadata 或 path API，再单独扩展。
- 当前 CLI 会写入并读取最小 WaveBits metadata，用来自动恢复 `decode` 所需的 transport mode / frame samples。
