# 仓库结构与文件地图

更新时间：2026-03-08

## 目标
这份文档用于回答两个问题：
- 某类功能代码大概在哪些文件里？
- 修改某个模块时，应该先看哪些文件，而不是全量扫描整个目录？

## 仓库大区块
- `libs/`
  - 共享库代码主区，包含 `audio_core`、`audio_api`、`audio_io`
- `apps/`
  - 表现层与平台集成，目前主要是 `audio_cli`、`audio_android`
- `Test/`
  - 单元、API、产物、CLI smoke 测试
- `docs/`
  - 架构、设计、发布说明、测试说明
- `tools/`
  - Python 编排脚本，不替代 `CMake` / `Gradle`

## `libs/` 总览

### `libs/audio_core`
- 作用：核心模式逻辑、编码/解码主链路、transport 分发。
- 构建入口：
  - `libs/audio_core/CMakeLists.txt`
- 最常先看的文件：
  - `libs/audio_core/include/bag/transport/transport.h`
  - `libs/audio_core/src/transport/transport.cpp`
  - `libs/audio_core/include/bag/common/config.h`
  - `libs/audio_core/include/bag/common/types.h`

### `libs/audio_api`
- 作用：稳定 C API，供 CLI、JNI 和未来其他平台层调用。
- 最常先看的文件：
  - `libs/audio_api/include/bag_api.h`
  - `libs/audio_api/src/bag_api.cpp`

### `libs/audio_io`
- 作用：WAV 读写与文件 I/O 边界。
- 最常先看的文件：
  - `libs/audio_io/include/wav_io.h`
  - `libs/audio_io/src/wav_io.cpp`

## `audio_core` 模块级地图

### 公共入口与分发
- 配置与类型
  - `libs/audio_core/include/bag/common/config.h`
  - `libs/audio_core/include/bag/common/types.h`
  - `libs/audio_core/include/bag/common/error_code.h`
- 版本
  - `libs/audio_core/include/bag/common/version.h`
  - `libs/audio_core/src/common/version.cpp`
- transport 门面
  - `libs/audio_core/include/bag/transport/transport.h`
  - `libs/audio_core/src/transport/transport.cpp`
- pipeline 兼容适配层
  - `libs/audio_core/include/bag/pipeline/pipeline.h`
  - `libs/audio_core/src/pipeline/pipeline.cpp`

### `flash`
- 先看：
  - `libs/audio_core/include/bag/flash/codec.h`
  - `libs/audio_core/src/flash/codec.cpp`
  - `libs/audio_core/include/bag/flash/phy_clean.h`
  - `libs/audio_core/src/flash/phy_clean.cpp`
- 当前语义：
  - 原始字节直通
  - clean `BFSK`
  - 无 frame / CRC / 长度字段

### `pro`
- 先看：
  - `libs/audio_core/include/bag/pro/codec.h`
  - `libs/audio_core/src/pro/codec.cpp`
  - `libs/audio_core/include/bag/pro/phy_clean.h`
  - `libs/audio_core/src/pro/phy_clean.cpp`
- 当前语义：
  - ASCII-only
  - `ASCII byte -> nibble`
  - `DTMF-like` 双音 clean PHY

### `ultra`
- 先看：
  - `libs/audio_core/include/bag/ultra/codec.h`
  - `libs/audio_core/src/ultra/codec.cpp`
  - `libs/audio_core/include/bag/ultra/phy_clean.h`
  - `libs/audio_core/src/ultra/phy_clean.cpp`
- 当前语义：
  - UTF-8 byte
  - `UTF-8 byte -> nibble`
  - clean `16-FSK`

## 兼容/遗留文件：通常不要先看
以下文件当前不属于 clean 主链路的第一阅读入口；只有在处理兼容行为、迁移遗留逻辑或专门修改旧接口时再看：
- `libs/audio_core/include/bag/pro/phy_compat.h`
- `libs/audio_core/src/pro/phy_compat.cpp`
- `libs/audio_core/include/bag/ultra/phy_compat.h`
- `libs/audio_core/src/ultra/phy_compat.cpp`
- `libs/audio_core/include/bag/pro/frame_codec.h`
- `libs/audio_core/src/pro/frame_codec.cpp`
- `libs/audio_core/include/bag/pro/text_codec.h`
- `libs/audio_core/src/pro/text_codec.cpp`
- `libs/audio_core/include/bag/fsk/fsk_codec.h`
- `libs/audio_core/src/fsk/fsk_codec.cpp`
- `libs/audio_core/include/bag/phy/*`
- `libs/audio_core/include/bag/link/*`
- `libs/audio_core/src/phy/*`

## 按任务快速跳转

### 改 mode 参数校验 / 分发
- 先看：
  - `libs/audio_core/src/transport/transport.cpp`
  - `libs/audio_api/src/bag_api.cpp`

### 改 `flash` 编解码
- 先看：
  - `libs/audio_core/src/flash/codec.cpp`
  - `libs/audio_core/src/flash/phy_clean.cpp`
- 再看：
  - `Test/unit/unit_tests.cpp`
  - `Test/artifact/artifact_tests.cpp`

### 改 `pro` 编解码
- 先看：
  - `libs/audio_core/src/pro/codec.cpp`
  - `libs/audio_core/src/pro/phy_clean.cpp`
  - `libs/audio_core/src/transport/transport.cpp`
- 再看：
  - `Test/unit/unit_tests.cpp`
  - `Test/api/api_tests.cpp`
  - `Test/cli/cli_smoke_tests.cpp`

### 改 `ultra` 编解码
- 先看：
  - `libs/audio_core/src/ultra/codec.cpp`
  - `libs/audio_core/src/ultra/phy_clean.cpp`
  - `libs/audio_core/src/transport/transport.cpp`
- 再看：
  - `Test/unit/unit_tests.cpp`
  - `Test/api/api_tests.cpp`
  - `Test/artifact/artifact_tests.cpp`

### 改 C API / JNI / CLI 接入
- 先看：
  - `libs/audio_api/include/bag_api.h`
  - `libs/audio_api/src/bag_api.cpp`
- 然后按平台看：
  - `apps/audio_cli/windows/src/main.cpp`
  - `apps/audio_android/app/src/main/cpp/jni_bridge.cpp`

### 改 WAV / I/O
- 先看：
  - `libs/audio_io/include/wav_io.h`
  - `libs/audio_io/src/wav_io.cpp`
- 再看：
  - `Test/unit/unit_tests.cpp`
  - `Test/artifact/artifact_tests.cpp`

## 测试地图
- `Test/unit/`
  - 核心 codec / PHY / transport / pipeline 最小行为
- `Test/api/`
  - C API 契约与错误语义
- `Test/artifact/`
  - `text -> PCM/WAV -> text` 产品主链路
- `Test/cli/`
  - 真实 CLI 子进程 smoke
- 详细测试口径见：
  - `docs/testing.md`
