## CLI 命令

### Rust CLI debug 构建

```powershell
python tools/run.py cli build
```

### Rust CLI release 构建

```powershell
python tools/run.py cli build --release
```

### Windows 交付版 release 构建

```powershell
python tools/run.py windows build
```

行为：
- 通过 Python 调度 CMake 与 Cargo。
- CMake 会以 `Release` 配置写入当前 build tree。
- 默认传 `-DFLIPBITS_BUILD_TESTS=OFF`，不会把 `Test/` 里的测试目标纳入这次 Windows 交付构建。
- 如果当前 `build/.../bin/` 里残留旧的 `test_*.exe`，命令会顺手清掉。
- 构建完成后会把 `FlipBits.exe` 和所需运行时 DLL 放到 `build/.../bin/`。

### Windows 交付版 debug 构建

```powershell
python tools/run.py windows build --debug
```

行为：
- 通过 Python 调度 CMake 与 Cargo。
- CMake 会以 `Debug` 配置写入当前 build tree。
- 默认仍然传 `-DFLIPBITS_BUILD_TESTS=OFF`，适合只看 CLI 产物、不想混入测试 exe 的场景。

### Windows 构建时显式带上 Test/

```powershell
python tools/run.py windows build --build-tests
```

说明：
- 这会向 CMake 传 `-DFLIPBITS_BUILD_TESTS=ON`。
- 根 `CMakeLists.txt` 会重新把 `Test/` 加回当前 build tree。
- `windows build --build-tests` 不只是“允许测试目标存在”，还会额外触发一次测试目标构建。
- 此时 `build/.../bin/` 里可能出现 `test_*.exe`，这是预期行为。

### 直接配置 CMake build type / tests 开关

```powershell
python tools/run.py configure --build-dir build/dev --build-type Release --no-build-tests
python tools/run.py configure --build-dir build/dev --build-type Debug --build-tests
```

说明：
- `--build-type` 会传给 `CMAKE_BUILD_TYPE`。
- `--build-tests / --no-build-tests` 会传给 `FLIPBITS_BUILD_TESTS`。
- `windows build` 只是把这两个开关封装成更适合 Windows CLI 交付的默认行为。
