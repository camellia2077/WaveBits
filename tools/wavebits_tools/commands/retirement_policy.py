from __future__ import annotations

import re
from pathlib import Path

from ..constants import ROOT_DIR
from ..errors import ToolError


_PHASE17_BOUNDARY_RULES: dict[Path, tuple[str, ...]] = {
    ROOT_DIR / "libs" / "audio_api" / "CMakeLists.txt": (
        "WAVEBITS_API_IMPORT_STD=1",
        "CXX_MODULE_STD ON",
    ),
    ROOT_DIR / "libs" / "audio_api" / "src" / "bag_api.cpp": (
        "WAVEBITS_API_IMPORT_STD",
        "import std;",
        "import bag.transport.facade;",
        "std::vector<std::int16_t>",
        "std::size_t",
        "std::unique_ptr",
    ),
}

_RETIRED_WRAPPER_PATHS: tuple[Path, ...] = (
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "pro" / "text_codec.h",
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "text_codec.cpp",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "pro" / "frame_codec.h",
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "frame_codec.cpp",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "fsk" / "fsk_codec.h",
)

_RETIRED_WRAPPER_INCLUDE_TOKENS: tuple[str, ...] = (
    '#include "bag/pro/text_codec.h"',
    '#include "bag/pro/frame_codec.h"',
    '#include "bag/fsk/fsk_codec.h"',
)

_MODULES_PHASE2_WRAPPER_EVICTION_REQUIRED_TOKENS: tuple[str, ...] = (
    '#include "test_std_support.h"',
    "import bag.flash.phy_clean;",
    "bag::flash::EncodeBytesToPcm16",
    "bag::flash::DecodePcm16ToBytes",
    "bag::pro::EncodeTextToPayload",
    "bag::pro::DecodePayloadToText",
    "bag::pro::DecodeSymbolsToPayload",
)

_AUDIO_CORE_CMAKE_PATH = ROOT_DIR / "libs" / "audio_core" / "CMakeLists.txt"
_AUDIO_CORE_CMAKE_FORBIDDEN_TOKENS: tuple[str, ...] = (
    "src/pro/text_codec.cpp",
    "src/pro/frame_codec.cpp",
)
_AUDIO_CORE_MODULES_ONLY_PATTERN = re.compile(
    r"if\(WAVEBITS_HOST_MODULES\)\s+"
    r"list\(APPEND bag_core_sources\s+"
    r"src/common/version\.cpp\s+"
    r"src/flash/codec\.cpp\s+"
    r"src/flash/phy_clean\.cpp\s+"
    r"src/fsk/fsk_codec\.cpp\s+"
    r"src/pro/codec\.cpp\s+"
    r"src/pro/phy_clean\.cpp\s+"
    r"src/pro/phy_compat\.cpp\s+"
    r"src/pipeline/pipeline\.cpp\s+"
    r"src/transport/compat/frame_codec\.cpp\s+"
    r"src/transport/transport\.cpp\s+"
    r"src/ultra/codec\.cpp\s+"
    r"src/ultra/phy_clean\.cpp\s+"
    r"src/ultra/phy_compat\.cpp\s+"
    r"\)\s+"
    r"endif\(\)",
    re.MULTILINE,
)

_ANDROID_NATIVE_PACKAGE_CMAKE_PATH = (
    ROOT_DIR / "apps" / "audio_android" / "native_package" / "CMakeLists.txt"
)
_ANDROID_NATIVE_WRAPPERS_CMAKE_PATH = (
    ROOT_DIR / "apps" / "audio_android" / "native_package" / "src" / "CMakeLists.txt"
)
_ANDROID_NATIVE_PACKAGE_FORBIDDEN_TOKENS: tuple[str, ...] = (
    "libs/audio_api/src/bag_api.cpp",
    "libs/audio_core/src/common/version.cpp",
    "libs/audio_core/src/flash/codec.cpp",
    "libs/audio_core/src/flash/phy_clean.cpp",
    "src/fsk/fsk_codec.cpp",
    "libs/audio_core/src/pro/codec.cpp",
    "libs/audio_core/src/pro/phy_clean.cpp",
    "src/pro/phy_compat.cpp",
    "src/pipeline/pipeline.cpp",
    "src/transport/compat/frame_codec.cpp",
    "libs/audio_core/src/transport/transport.cpp",
    "libs/audio_core/src/ultra/codec.cpp",
    "libs/audio_core/src/ultra/phy_clean.cpp",
    "src/ultra/phy_compat.cpp",
    "src/bag_api_package.cpp",
    "src/audio_core_common_version.cpp",
    "src/audio_core_flash_codec.cpp",
    "src/audio_core_flash_phy_clean.cpp",
    "src/audio_core_pro_codec.cpp",
    "src/audio_core_pro_phy_clean.cpp",
    "src/audio_core_transport_transport.cpp",
    "src/audio_core_ultra_codec.cpp",
    "src/audio_core_ultra_phy_clean.cpp",
    "private_include",
)
_ANDROID_NATIVE_PACKAGE_REQUIRED_TOKENS: tuple[str, ...] = (
    "bag_android_native_packaged",
    "bag_android_native_wrappers",
    "$<TARGET_OBJECTS:bag_android_native_wrappers>",
    '"${CMAKE_CURRENT_LIST_DIR}/src"',
    "add_library(bag_android_native ALIAS bag_android_native_packaged)",
)
_ANDROID_NATIVE_WRAPPERS_REQUIRED_TOKENS: tuple[str, ...] = (
    "bag_android_native_wrappers",
    "bag_api_package.cpp",
    "audio_core_common_version.cpp",
    "audio_core_flash_codec.cpp",
    "audio_core_flash_phy_clean.cpp",
    "audio_core_pro_codec.cpp",
    "audio_core_pro_phy_clean.cpp",
    "audio_core_transport_transport.cpp",
    "audio_core_ultra_codec.cpp",
    "audio_core_ultra_phy_clean.cpp",
    "../private_include",
    "libs/audio_api/include",
)

_ROOT_CMAKE_PATH = ROOT_DIR / "CMakeLists.txt"
_ROOT_HOST_OFF_RETIREMENT_REQUIRED_TOKENS: tuple[str, ...] = (
    "Root host WAVEBITS_HOST_MODULES=OFF has been retired.",
    "set(CMAKE_CXX_STANDARD 20)",
)

_CLI_PATH = ROOT_DIR / "tools" / "wavebits_tools" / "cli.py"
_CLI_HOST_OFF_RETIREMENT_FORBIDDEN_TOKENS: tuple[str, ...] = (
    '"--no-modules"',
    "WAVEBITS_HOST_MODULES=OFF",
)

_REMOVED_LEGACY_HEADER_PATHS: tuple[Path, ...] = (
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "legacy" / "common" / "config.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "legacy" / "common" / "error_code.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "legacy" / "common" / "types.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "legacy" / "common" / "version.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "legacy" / "flash" / "codec.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "legacy" / "flash" / "phy_clean.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "legacy" / "pipeline" / "pipeline.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "legacy" / "pro" / "codec.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "legacy" / "pro" / "phy_compat.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "legacy" / "pro" / "phy_clean.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "legacy" / "transport" / "decoder.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "legacy" / "transport" / "transport.h",
    ROOT_DIR
    / "libs"
    / "audio_core"
    / "include"
    / "bag"
    / "legacy"
    / "transport"
    / "compat"
    / "frame_codec.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "legacy" / "ultra" / "codec.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "legacy" / "ultra" / "phy_compat.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "legacy" / "ultra" / "phy_clean.h",
)

_RETIRED_BRIDGE_HEADER_PATHS: tuple[Path, ...] = (
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "common" / "config.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "common" / "error_code.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "common" / "types.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "common" / "version.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "flash" / "codec.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "flash" / "phy_clean.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "pipeline" / "pipeline.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "pro" / "codec.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "pro" / "phy_compat.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "pro" / "phy_clean.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "transport" / "decoder.h",
    ROOT_DIR
    / "libs"
    / "audio_core"
    / "include"
    / "bag"
    / "transport"
    / "compat"
    / "frame_codec.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "transport" / "transport.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "ultra" / "codec.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "ultra" / "phy_compat.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "ultra" / "phy_clean.h",
)

_RETIRED_BRIDGE_INCLUDE_TOKENS: tuple[str, ...] = (
    '#include "bag/common/config.h"',
    '#include "bag/common/error_code.h"',
    '#include "bag/common/types.h"',
    '#include "bag/common/version.h"',
    '#include "bag/flash/codec.h"',
    '#include "bag/flash/phy_clean.h"',
    '#include "bag/pipeline/pipeline.h"',
    '#include "bag/pro/codec.h"',
    '#include "bag/pro/phy_compat.h"',
    '#include "bag/pro/phy_clean.h"',
    '#include "bag/transport/decoder.h"',
    '#include "bag/transport/compat/frame_codec.h"',
    '#include "bag/transport/transport.h"',
    '#include "bag/ultra/codec.h"',
    '#include "bag/ultra/phy_compat.h"',
    '#include "bag/ultra/phy_clean.h"',
)

_RETIRED_INTERNAL_HEADER_PATHS: tuple[Path, ...] = (
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "internal" / "common" / "config.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "internal" / "common" / "error_code.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "internal" / "common" / "types.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "internal" / "common" / "version.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "internal" / "flash" / "codec.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "internal" / "flash" / "phy_clean.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "internal" / "pipeline" / "pipeline.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "internal" / "pro" / "codec.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "internal" / "pro" / "phy_compat.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "internal" / "pro" / "phy_clean.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "internal" / "transport" / "decoder.h",
    ROOT_DIR
    / "libs"
    / "audio_core"
    / "include"
    / "bag"
    / "internal"
    / "transport"
    / "facade.h",
    ROOT_DIR
    / "libs"
    / "audio_core"
    / "include"
    / "bag"
    / "internal"
    / "transport"
    / "compat"
    / "frame_codec.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "internal" / "ultra" / "codec.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "internal" / "ultra" / "phy_compat.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "internal" / "ultra" / "phy_clean.h",
)

_RETIRED_INTERNAL_INCLUDE_TOKENS: tuple[str, ...] = (
    '#include "bag/internal/common/config.h"',
    '#include "bag/internal/common/error_code.h"',
    '#include "bag/internal/common/types.h"',
    '#include "bag/internal/common/version.h"',
    '#include "bag/internal/flash/codec.h"',
    '#include "bag/internal/flash/phy_clean.h"',
    '#include "bag/internal/pipeline/pipeline.h"',
    '#include "bag/internal/pro/codec.h"',
    '#include "bag/internal/pro/phy_compat.h"',
    '#include "bag/internal/pro/phy_clean.h"',
    '#include "bag/internal/transport/decoder.h"',
    '#include "bag/internal/transport/facade.h"',
    '#include "bag/internal/transport/compat/frame_codec.h"',
    '#include "bag/internal/ultra/codec.h"',
    '#include "bag/internal/ultra/phy_compat.h"',
    '#include "bag/internal/ultra/phy_clean.h"',
)

_BAG_API_PATH = ROOT_DIR / "libs" / "audio_api" / "src" / "bag_api.cpp"
_BAG_API_REQUIRED_TOKENS: tuple[str, ...] = (
    '#include "bag_api.h"',
    "import bag.common.config;",
    "import bag.common.version;",
    "import bag.transport.facade;",
)
_BAG_API_FORBIDDEN_TOKENS: tuple[str, ...] = (
    '#include "bag/common/config.h"',
    '#include "bag/common/version.h"',
    '#include "bag/transport/transport.h"',
    '#include "bag/internal/common/version.h"',
    '#include "bag/internal/transport/facade.h"',
    '#include "bag/legacy/common/config.h"',
    '#include "bag/legacy/common/version.h"',
    '#include "bag/legacy/transport/transport.h"',
)

_UNIT_TESTS_PATH = ROOT_DIR / "Test" / "unit" / "unit_tests.cpp"
_UNIT_TEST_INTERNAL_REQUIRED_TOKENS: tuple[str, ...] = ()
_UNIT_TEST_OWNER_CLEANUP_FORBIDDEN_TOKENS: tuple[str, ...] = (
    '#include "bag/internal/flash/phy_clean.h"',
    '#include "bag/internal/pro/codec.h"',
    "import bag.flash.phy_clean;",
    "import bag.pro.codec;",
    "bag::flash::",
    "bag::pro::",
)
_UNIT_TEST_SHARED_BRIDGE_FORBIDDEN_TOKENS: tuple[str, ...] = (
    '#include "bag/flash/phy_clean.h"',
    '#include "bag/pro/codec.h"',
    '#include "bag/transport/compat/frame_codec.h"',
)
_UNIT_TEST_LEGACY_FORBIDDEN_TOKENS: tuple[str, ...] = (
    '#include "bag/legacy/flash/phy_clean.h"',
    '#include "bag/legacy/pro/codec.h"',
    '#include "bag/legacy/transport/compat/frame_codec.h"',
)

_MODULES_PHASE2_LEAF_SMOKE_PATH = ROOT_DIR / "Test" / "modules" / "phase2_leaf_smoke.cpp"
_MODULES_PHASE2_LEAF_REQUIRED_TOKENS: tuple[str, ...] = (
    "import bag.flash.phy_clean;",
    "bag::flash::EncodeBytesToPcm16",
    "bag::flash::DecodePcm16ToBytes",
    "bag::pro::EncodeTextToPayload",
    "bag::pro::DecodePayloadToText",
    "bag::pro::DecodeSymbolsToPayload",
    "bag::pro::kSymbolsPerPayloadByte",
    "import bag.transport.compat.frame_codec;",
    "bag::transport::compat::EncodeFrame",
    "bag::transport::compat::DecodeFrame",
    "bag::transport::compat::kMaxFramePayloadBytes",
)

_AUDIO_CORE_SOURCE_REQUIRED_TOKENS: dict[Path, tuple[str, ...]] = {}

_LEGACY_INCLUDE_PREFIX = '#include "bag/legacy/'
_ALLOWED_LEGACY_INCLUDE_ROOTS: tuple[Path, ...] = ()
_ALLOWED_LEGACY_INCLUDE_OWNERS: set[Path] = set()


def _iter_code_files() -> list[Path]:
    code_files: set[Path] = set()
    for root in (ROOT_DIR / "libs", ROOT_DIR / "Test", ROOT_DIR / "apps"):
        if not root.is_dir():
            continue
        for pattern in ("*.cpp", "*.h", "*.hpp", "*.cppm", "*.cc", "*.cxx"):
            code_files.update(path for path in root.rglob(pattern) if path.is_file())
    return sorted(code_files)


def _is_allowed_legacy_include_owner(path: Path) -> bool:
    if path in _ALLOWED_LEGACY_INCLUDE_OWNERS:
        return True
    return any(path.is_relative_to(root) for root in _ALLOWED_LEGACY_INCLUDE_ROOTS)


def run_phase17_boundary_host_standardization_policy_checks() -> None:
    failures: list[str] = []
    for path, required_tokens in sorted(_PHASE17_BOUNDARY_RULES.items()):
        content = path.read_text(encoding="utf-8")
        missing_tokens = [token for token in required_tokens if token not in content]
        if missing_tokens:
            missing = ", ".join(missing_tokens)
            failures.append(f"{path.relative_to(ROOT_DIR)} missing: {missing}")

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError(
            "Boundary-adjacent host implementation regression detected in audited files:\n"
            f"{joined}"
        )


def run_phase18_compatibility_eviction_policy_checks() -> None:
    failures: list[str] = []

    for path in _RETIRED_WRAPPER_PATHS:
        if path.exists():
            failures.append(f"retired wrapper still exists: {path.relative_to(ROOT_DIR)}")

    modules_phase2_content = _MODULES_PHASE2_LEAF_SMOKE_PATH.read_text(encoding="utf-8")
    missing_phase2_required = [
        token
        for token in _MODULES_PHASE2_WRAPPER_EVICTION_REQUIRED_TOKENS
        if token not in modules_phase2_content
    ]
    if missing_phase2_required:
        failures.append(
            "Test/modules/phase2_leaf_smoke.cpp missing required wrapper-eviction coverage tokens: "
            + ", ".join(missing_phase2_required)
        )

    unit_tests_content = _UNIT_TESTS_PATH.read_text(encoding="utf-8")
    present_forbidden = [
        token for token in _RETIRED_WRAPPER_INCLUDE_TOKENS if token in unit_tests_content
    ]
    if present_forbidden:
        failures.append(
            "Test/unit/unit_tests.cpp still depends on retired wrapper headers: "
            + ", ".join(present_forbidden)
        )

    cmake_content = _AUDIO_CORE_CMAKE_PATH.read_text(encoding="utf-8")
    present_cmake_forbidden = [
        token for token in _AUDIO_CORE_CMAKE_FORBIDDEN_TOKENS if token in cmake_content
    ]
    if present_cmake_forbidden:
        failures.append(
            "libs/audio_core/CMakeLists.txt still lists retired wrapper sources: "
            + ", ".join(present_cmake_forbidden)
        )
    if not _AUDIO_CORE_MODULES_ONLY_PATTERN.search(cmake_content):
        failures.append(
            "libs/audio_core/CMakeLists.txt no longer documents src/fsk/fsk_codec.cpp, "
            "src/pro/phy_compat.cpp, src/ultra/phy_compat.cpp, src/pipeline/pipeline.cpp, "
            "and src/transport/compat/frame_codec.cpp as WAVEBITS_HOST_MODULES-only sources"
        )

    android_native_package_content = _ANDROID_NATIVE_PACKAGE_CMAKE_PATH.read_text(encoding="utf-8")
    missing_android_required = [
        token
        for token in _ANDROID_NATIVE_PACKAGE_REQUIRED_TOKENS
        if token not in android_native_package_content
    ]
    if missing_android_required:
        failures.append(
            "apps/audio_android/native_package/CMakeLists.txt missing packaged-target tokens: "
            + ", ".join(missing_android_required)
        )
    present_android_forbidden = [
        token
        for token in _ANDROID_NATIVE_PACKAGE_FORBIDDEN_TOKENS
        if token in android_native_package_content
    ]
    if present_android_forbidden:
        failures.append(
            "apps/audio_android/native_package/CMakeLists.txt still lists bag_core modules-only "
            "sources: "
            + ", ".join(present_android_forbidden)
        )

    android_native_wrappers_content = _ANDROID_NATIVE_WRAPPERS_CMAKE_PATH.read_text(encoding="utf-8")
    missing_android_wrapper_required = [
        token
        for token in _ANDROID_NATIVE_WRAPPERS_REQUIRED_TOKENS
        if token not in android_native_wrappers_content
    ]
    if missing_android_wrapper_required:
        failures.append(
            "apps/audio_android/native_package/src/CMakeLists.txt missing wrapper-target tokens: "
            + ", ".join(missing_android_wrapper_required)
        )

    for path in _iter_code_files():
        content = path.read_text(encoding="utf-8")
        present_tokens = [
            token for token in _RETIRED_WRAPPER_INCLUDE_TOKENS if token in content
        ]
        if present_tokens:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} references retired wrapper headers: "
                + ", ".join(present_tokens)
            )

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError(
            "Retired wrapper regression detected:\n"
            f"{joined}"
        )


def run_phase19_bridge_retirement_policy_checks() -> None:
    failures: list[str] = []

    root_cmake_content = _ROOT_CMAKE_PATH.read_text(encoding="utf-8")
    missing_root_required = [
        token for token in _ROOT_HOST_OFF_RETIREMENT_REQUIRED_TOKENS if token not in root_cmake_content
    ]
    if missing_root_required:
        failures.append(
            f"{_ROOT_CMAKE_PATH.relative_to(ROOT_DIR)} missing retired-host-off guard tokens: "
            + ", ".join(missing_root_required)
        )

    cli_content = _CLI_PATH.read_text(encoding="utf-8")
    present_cli_forbidden = [
        token for token in _CLI_HOST_OFF_RETIREMENT_FORBIDDEN_TOKENS if token in cli_content
    ]
    if present_cli_forbidden:
        failures.append(
            f"{_CLI_PATH.relative_to(ROOT_DIR)} still exposes retired host-off tokens: "
            + ", ".join(present_cli_forbidden)
        )

    for path in _REMOVED_LEGACY_HEADER_PATHS:
        if path.exists():
            failures.append(
                f"deleted legacy header reintroduced: {path.relative_to(ROOT_DIR)}"
            )

    for path in _RETIRED_BRIDGE_HEADER_PATHS:
        if path.exists():
            failures.append(f"retired bridge header still exists: {path.relative_to(ROOT_DIR)}")

    for path in _RETIRED_INTERNAL_HEADER_PATHS:
        if path.exists():
            failures.append(
                f"modules-only compat internal header still exists: {path.relative_to(ROOT_DIR)}"
            )

    bag_api_content = _BAG_API_PATH.read_text(encoding="utf-8")
    missing_bag_api_required = [
        token for token in _BAG_API_REQUIRED_TOKENS if token not in bag_api_content
    ]
    if missing_bag_api_required:
        failures.append(
            f"{_BAG_API_PATH.relative_to(ROOT_DIR)} missing required internal declaration includes: "
            + ", ".join(missing_bag_api_required)
        )

    present_bag_api_forbidden = [
        token for token in _BAG_API_FORBIDDEN_TOKENS if token in bag_api_content
    ]
    if present_bag_api_forbidden:
        failures.append(
            f"{_BAG_API_PATH.relative_to(ROOT_DIR)} still depends on retired or legacy declaration headers: "
            + ", ".join(present_bag_api_forbidden)
        )

    unit_tests_content = _UNIT_TESTS_PATH.read_text(encoding="utf-8")
    missing_unit_test_required = [
        token for token in _UNIT_TEST_INTERNAL_REQUIRED_TOKENS if token not in unit_tests_content
    ]
    if missing_unit_test_required:
        failures.append(
            f"{_UNIT_TESTS_PATH.relative_to(ROOT_DIR)} missing required internal declaration includes: "
            + ", ".join(missing_unit_test_required)
        )

    present_unit_test_forbidden = [
        token
        for token in _UNIT_TEST_SHARED_BRIDGE_FORBIDDEN_TOKENS + _UNIT_TEST_LEGACY_FORBIDDEN_TOKENS
        if token in unit_tests_content
    ]
    if present_unit_test_forbidden:
        failures.append(
            f"{_UNIT_TESTS_PATH.relative_to(ROOT_DIR)} still depends on retired or legacy declaration headers: "
            + ", ".join(present_unit_test_forbidden)
        )

    present_unit_test_owner_tokens = [
        token for token in _UNIT_TEST_OWNER_CLEANUP_FORBIDDEN_TOKENS if token in unit_tests_content
    ]
    if present_unit_test_owner_tokens:
        failures.append(
            f"{_UNIT_TESTS_PATH.relative_to(ROOT_DIR)} still owns flash/pro low-level coverage tokens: "
            + ", ".join(present_unit_test_owner_tokens)
        )

    modules_phase2_content = _MODULES_PHASE2_LEAF_SMOKE_PATH.read_text(encoding="utf-8")
    missing_phase2_required = [
        token
        for token in _MODULES_PHASE2_LEAF_REQUIRED_TOKENS
        if token not in modules_phase2_content
    ]
    if missing_phase2_required:
        failures.append(
            f"{_MODULES_PHASE2_LEAF_SMOKE_PATH.relative_to(ROOT_DIR)} missing required "
            "bag.transport.compat.frame_codec module coverage tokens: "
            + ", ".join(missing_phase2_required)
        )

    for path, required_tokens in sorted(_AUDIO_CORE_SOURCE_REQUIRED_TOKENS.items()):
        content = path.read_text(encoding="utf-8")
        missing_required = [token for token in required_tokens if token not in content]
        if missing_required:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} missing required internal declaration includes: "
                + ", ".join(missing_required)
            )

    for path in _iter_code_files():
        content = path.read_text(encoding="utf-8")

        present_retired_tokens = [
            token for token in _RETIRED_BRIDGE_INCLUDE_TOKENS if token in content
        ]
        if present_retired_tokens:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} references retired bridge headers: "
                + ", ".join(present_retired_tokens)
            )

        present_retired_internal_tokens = [
            token for token in _RETIRED_INTERNAL_INCLUDE_TOKENS if token in content
        ]
        if present_retired_internal_tokens:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} references retired internal compat headers: "
                + ", ".join(present_retired_internal_tokens)
            )

        if _LEGACY_INCLUDE_PREFIX in content and not _is_allowed_legacy_include_owner(path):
            failures.append(
                f"{path.relative_to(ROOT_DIR)} should not include deleted legacy headers"
            )

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError(
            "Post-legacy / bridge-retirement policy regression detected:\n"
            f"{joined}"
        )


def run_retirement_policy_checks() -> None:
    run_phase17_boundary_host_standardization_policy_checks()
    run_phase18_compatibility_eviction_policy_checks()
    run_phase19_bridge_retirement_policy_checks()
