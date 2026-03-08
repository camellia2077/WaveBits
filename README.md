# Binary_Audio_Generator
用于把文字转化为二进制音频（FSK）。

## 依赖安装
```
pip install -r python/requirements.txt
```

## 使用示例
编码：
```
python python/main.py encode --text "Hello" --out data/output_audio/hello.wav
```

从文本文件编码：
```
python python/main.py encode --text-file data/input.txt --out data/output_audio/hello.wav
```

解码：
```
python python/main.py decode --in data/output_audio/output.wav
```

解码并写入文本文件：
```
python python/main.py decode --in data/output_audio/output.wav --out-text data/output_text/output.txt
```

## C++ 版本（libsndfile）
内核版本：`0.1.1`（详见 [docs/core.md](./docs/core.md)）
表现层版本：`0.1.1`（详见 [docs/presentation.md](./docs/presentation.md)）
测试说明：详见 [docs/testing.md](./docs/testing.md)

共享代码目录：
- `libs/audio_core`：协议、FSK 编解码、pipeline 等核心能力
- `libs/audio_api`：跨平台稳定 C API
- `libs/audio_io`：WAV 读写等 I/O 能力

### 依赖（MSYS2 UCRT64）
```
pacman -S --needed \
  mingw-w64-ucrt-x86_64-gcc \
  mingw-w64-ucrt-x86_64-cmake \
  mingw-w64-ucrt-x86_64-ninja \
  mingw-w64-ucrt-x86_64-pkgconf \
  mingw-w64-ucrt-x86_64-libsndfile
```

### 构建
```
cmake -S . -B build -G Ninja
cmake --build build
```

### 开发工具入口
```
python tools/run.py configure --build-dir build/dev
python tools/run.py build --build-dir build/dev
python tools/run.py test --build-dir build/dev
python tools/run.py test --build-dir build/dev --report-dir build/test-artifacts/reports/latest
python tools/run.py verify --build-dir build/dev --skip-android
python tools/run.py android assemble-debug
python tools/run.py roundtrip --build-dir build/dev --mode flash --text "Hello"
python tools/run.py smoke --build-dir build/dev
```

说明：`tools/` 只做编排，实际构建规则仍以 `CMake` / `Gradle` 为准。
说明：可见测试音频产物默认输出到 `build/test-artifacts/`。
说明：`python tools/run.py test` 默认额外产出 `summary.json` 与 `run.log`，用于机器汇总和人工排查。

### 使用示例
编码：
```
build/bin/binary_audio_cpp.exe encode --text "Hello" --out data/output_audio/hello.wav
```

从文本文件编码：
```
build/bin/binary_audio_cpp.exe encode --text-file data/input.txt --out data/output_audio/hello.wav
```

解码：
```
build/bin/binary_audio_cpp.exe decode --in data/output_audio/output.wav
```

解码并写入文本文件：
```
build/bin/binary_audio_cpp.exe decode --in data/output_audio/output.wav --out-text data/output_text/output.txt
```

查看 CLI 相关第三方许可证：
```
build/bin/binary_audio_cpp.exe licenses
```

查看 CLI 与内核版本：
```
build/bin/binary_audio_cpp.exe version
```


## 致谢
本项目使用了以下第三方库，感谢其贡献：
- libsndfile
- NumPy
- SciPy

## Licenses
- 本项目：MIT（见 [LICENSE](./LICENSE)）
- libsndfile：LGPL-2.1-or-later
- NumPy：BSD-3-Clause
- SciPy：BSD-3-Clause

说明：第三方库许可证请以上游项目仓库中的 LICENSE 文件为准。
