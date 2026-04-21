from __future__ import annotations

from pathlib import Path

from ...constants import ROOT_DIR
from ...errors import ToolError
from .common import iter_code_files, read_modules_leaf_smoke_content


_RETIRED_WRAPPER_PATHS: tuple[Path, ...] = (
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "pro" / "text_codec.h",
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "text_codec.cpp",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "pro" / "frame_codec.h",
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "frame_codec.cpp",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "fsk" / "fsk_codec.h",
    ROOT_DIR / "libs" / "audio_core" / "src" / "common" / "version.clang.cpp",
    ROOT_DIR / "libs" / "audio_core" / "src" / "flash" / "codec.clang.cpp",
    ROOT_DIR / "libs" / "audio_core" / "src" / "flash" / "phy_clean.clang.cpp",
    ROOT_DIR / "libs" / "audio_core" / "src" / "fsk" / "fsk_codec.clang.cpp",
    ROOT_DIR / "libs" / "audio_core" / "src" / "pipeline" / "pipeline.clang.cpp",
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "codec.clang.cpp",
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "phy_clean.clang.cpp",
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "phy_compat.clang.cpp",
    ROOT_DIR / "libs" / "audio_core" / "src" / "transport" / "transport.clang.cpp",
    ROOT_DIR / "libs" / "audio_core" / "src" / "transport" / "compat" / "frame_codec.clang.cpp",
    ROOT_DIR / "libs" / "audio_core" / "src" / "ultra" / "codec.clang.cpp",
    ROOT_DIR / "libs" / "audio_core" / "src" / "ultra" / "phy_clean.clang.cpp",
    ROOT_DIR / "libs" / "audio_core" / "src" / "ultra" / "phy_compat.clang.cpp",
)
_RETIRED_WRAPPER_INCLUDE_TOKENS: tuple[str, ...] = (
    '#include "bag/pro/text_codec.h"',
    '#include "bag/pro/frame_codec.h"',
    '#include "bag/fsk/fsk_codec.h"',
)
_AUDIO_CORE_WRAPPER_MACRO_SCAN_ROOT = ROOT_DIR / "libs" / "audio_core" / "src"
_MODULES_PHASE2_WRAPPER_EVICTION_REQUIRED_TOKENS: tuple[str, ...] = (
    '#include "test_std_support.h"',
    "import bag.flash.signal;",
    "import bag.flash.voicing;",
    "import bag.flash.phy_clean;",
    "bag::flash::BuildPayloadLayout",
    "bag::flash::ApplyVoicingToPayload",
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
_AUDIO_CORE_SINGLE_LANE_REQUIRED_TOKENS: tuple[str, ...] = (
    "set(bag_core_sources",
    "src/common/version.cpp",
    "src/flash/codec.cpp",
    "src/flash/phy_clean.cpp",
    "src/flash/signal.cpp",
    "src/flash/voicing.cpp",
    "src/fsk/fsk_codec.cpp",
    "src/pro/codec.cpp",
    "src/pro/phy_clean.cpp",
    "src/pro/phy_compat.cpp",
    "src/pipeline/pipeline.cpp",
    "src/transport/compat/frame_codec.cpp",
    "src/transport/transport.cpp",
    "src/ultra/codec.cpp",
    "src/ultra/phy_clean.cpp",
    "src/ultra/phy_compat.cpp",
    "FILE_SET cxx_modules TYPE CXX_MODULES",
)
_ANDROID_NATIVE_PACKAGE_CMAKE_PATH = ROOT_DIR / "apps" / "audio_android" / "native_package" / "CMakeLists.txt"
_ANDROID_NATIVE_PACKAGE_OBJECTS_CMAKE_PATH = (
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
    "bag_android_native_package_objects",
    "$<TARGET_OBJECTS:bag_android_native_package_objects>",
    '"${CMAKE_CURRENT_LIST_DIR}/src"',
    "add_library(bag_android_native ALIAS bag_android_native_packaged)",
)
_ANDROID_NATIVE_PACKAGE_OBJECTS_REQUIRED_TOKENS: tuple[str, ...] = (
    'include("${FLIPBITS_ROOT}/cmake/flipbits_core_version.cmake")',
    "FLIPBITS_ANDROID_NATIVE_PACKAGE_GENERATED_INCLUDE_DIR",
    "version_generated.h",
    "bag_android_native_package_objects",
    "bag_api_package.cpp",
    "audio_core_common_version.cpp",
    "audio_core_flash_codec.cpp",
    "audio_core_flash_signal.cpp",
    "audio_core_flash_phy_clean.cpp",
    "audio_core_pro_codec.cpp",
    "audio_core_pro_phy_clean.cpp",
    "audio_core_transport_transport.cpp",
    "audio_core_ultra_codec.cpp",
    "audio_core_ultra_phy_clean.cpp",
    "../private_include",
    "libs/audio_api/include",
)
_ANDROID_NATIVE_PACKAGE_MACRO_SCAN_ROOT = ROOT_DIR / "apps" / "audio_android" / "native_package" / "src"
_ANDROID_BAG_API_PACKAGE_SOURCE_PATH = (
    ROOT_DIR / "apps" / "audio_android" / "native_package" / "src" / "bag_api_package.cpp"
)
_ANDROID_BAG_API_PACKAGE_REQUIRED_TOKENS: tuple[str, ...] = (
    '#include "bag_api.h"',
    '#include "android_bag/common/version.h"',
    '#include "android_bag/transport/facade.h"',
    '#include "../../../../libs/audio_api/src/bag_api_impl.inc"',
)
_ANDROID_BAG_API_PACKAGE_FORBIDDEN_TOKENS: tuple[str, ...] = (
    'libs/audio_api/src/bag_api.cpp',
)
_ANDROID_AUDIO_CORE_PACKAGE_SOURCE_RULES: dict[Path, tuple[str, ...]] = {
    ROOT_DIR / "apps" / "audio_android" / "native_package" / "src" / "audio_core_common_version.cpp": (
        '#include "android_bag/common/version.h"',
        '#include "../../../../libs/audio_core/src/common/version_impl.inc"',
    ),
    ROOT_DIR / "apps" / "audio_android" / "native_package" / "src" / "audio_core_flash_codec.cpp": (
        '#include "android_bag/flash/codec.h"',
        '#include "../../../../libs/audio_core/src/flash/codec_impl.inc"',
    ),
    ROOT_DIR / "apps" / "audio_android" / "native_package" / "src" / "audio_core_flash_signal.cpp": (
        "#include <algorithm>",
        "#include <cmath>",
        "#include <stdexcept>",
        '#include "android_bag/flash/signal.h"',
        '#include "../../../../libs/audio_core/src/flash/signal_impl.inc"',
    ),
    ROOT_DIR / "apps" / "audio_android" / "native_package" / "src" / "audio_core_flash_phy_clean.cpp": (
        '#include "android_bag/flash/codec.h"',
        '#include "android_bag/flash/phy_clean.h"',
        '#include "../../../../libs/audio_core/src/flash/phy_clean_impl.inc"',
    ),
    ROOT_DIR / "apps" / "audio_android" / "native_package" / "src" / "audio_core_pro_codec.cpp": (
        '#include "android_bag/pro/codec.h"',
        '#include "../../../../libs/audio_core/src/pro/codec_impl.inc"',
    ),
    ROOT_DIR / "apps" / "audio_android" / "native_package" / "src" / "audio_core_pro_phy_clean.cpp": (
        "#include <algorithm>",
        "#include <cmath>",
        '#include "android_bag/pro/codec.h"',
        '#include "android_bag/pro/phy_clean.h"',
        '#include "../../../../libs/audio_core/src/pro/phy_clean_impl.inc"',
    ),
    ROOT_DIR / "apps" / "audio_android" / "native_package" / "src" / "audio_core_transport_transport.cpp": (
        '#include "android_bag/flash/phy_clean.h"',
        '#include "android_bag/pro/phy_clean.h"',
        '#include "android_bag/transport/facade.h"',
        '#include "android_bag/ultra/phy_clean.h"',
        '#include "../../../../libs/audio_core/src/transport/transport_impl.inc"',
    ),
    ROOT_DIR / "apps" / "audio_android" / "native_package" / "src" / "audio_core_ultra_codec.cpp": (
        '#include "android_bag/ultra/codec.h"',
        '#include "../../../../libs/audio_core/src/ultra/codec_impl.inc"',
    ),
    ROOT_DIR / "apps" / "audio_android" / "native_package" / "src" / "audio_core_ultra_phy_clean.cpp": (
        "#include <algorithm>",
        "#include <cmath>",
        '#include "android_bag/ultra/codec.h"',
        '#include "android_bag/ultra/phy_clean.h"',
        '#include "../../../../libs/audio_core/src/ultra/phy_clean_impl.inc"',
    ),
}
_ANDROID_AUDIO_CORE_PACKAGE_SOURCE_FORBIDDEN_TOKENS: dict[Path, tuple[str, ...]] = {
    ROOT_DIR / "apps" / "audio_android" / "native_package" / "src" / "audio_core_common_version.cpp": (
        '#include "../../../../libs/audio_core/src/common/version.cpp"',
    ),
    ROOT_DIR / "apps" / "audio_android" / "native_package" / "src" / "audio_core_flash_codec.cpp": (
        '#include "../../../../libs/audio_core/src/flash/codec.cpp"',
    ),
    ROOT_DIR / "apps" / "audio_android" / "native_package" / "src" / "audio_core_flash_signal.cpp": (
        '#include "../../../../libs/audio_core/src/flash/signal.cpp"',
    ),
    ROOT_DIR / "apps" / "audio_android" / "native_package" / "src" / "audio_core_flash_phy_clean.cpp": (
        '#include "../../../../libs/audio_core/src/flash/phy_clean.cpp"',
    ),
    ROOT_DIR / "apps" / "audio_android" / "native_package" / "src" / "audio_core_pro_codec.cpp": (
        '#include "../../../../libs/audio_core/src/pro/codec.cpp"',
    ),
    ROOT_DIR / "apps" / "audio_android" / "native_package" / "src" / "audio_core_pro_phy_clean.cpp": (
        '#include "../../../../libs/audio_core/src/pro/phy_clean.cpp"',
    ),
    ROOT_DIR / "apps" / "audio_android" / "native_package" / "src" / "audio_core_transport_transport.cpp": (
        '#include "../../../../libs/audio_core/src/transport/transport.cpp"',
    ),
    ROOT_DIR / "apps" / "audio_android" / "native_package" / "src" / "audio_core_ultra_codec.cpp": (
        '#include "../../../../libs/audio_core/src/ultra/codec.cpp"',
    ),
    ROOT_DIR / "apps" / "audio_android" / "native_package" / "src" / "audio_core_ultra_phy_clean.cpp": (
        '#include "../../../../libs/audio_core/src/ultra/phy_clean.cpp"',
    ),
}
_UNIT_TESTS_PATH = ROOT_DIR / "libs" / "audio_io" / "tests" / "unit_tests.cpp"


def run_retired_wrappers_policy_checks() -> None:
    failures: list[str] = []

    for path in _RETIRED_WRAPPER_PATHS:
        if path.exists():
            failures.append(f"retired wrapper still exists: {path.relative_to(ROOT_DIR)}")

    modules_leaf_content = read_modules_leaf_smoke_content()
    missing_leaf_required = [
        token
        for token in _MODULES_PHASE2_WRAPPER_EVICTION_REQUIRED_TOKENS
        if token not in modules_leaf_content
    ]
    if missing_leaf_required:
        failures.append(
            "Test/modules/leaf_module_smoke*.cpp missing required wrapper-eviction coverage tokens: "
            + ", ".join(missing_leaf_required)
        )

    unit_tests_content = _UNIT_TESTS_PATH.read_text(encoding="utf-8")
    present_forbidden = [token for token in _RETIRED_WRAPPER_INCLUDE_TOKENS if token in unit_tests_content]
    if present_forbidden:
        failures.append(
            "libs/audio_io/tests/unit_tests.cpp still depends on retired wrapper headers: "
            + ", ".join(present_forbidden)
        )

    cmake_content = _AUDIO_CORE_CMAKE_PATH.read_text(encoding="utf-8")
    present_cmake_forbidden = [token for token in _AUDIO_CORE_CMAKE_FORBIDDEN_TOKENS if token in cmake_content]
    if present_cmake_forbidden:
        failures.append(
            "libs/audio_core/CMakeLists.txt still lists retired wrapper sources: "
            + ", ".join(present_cmake_forbidden)
        )
    missing_audio_core_required = [token for token in _AUDIO_CORE_SINGLE_LANE_REQUIRED_TOKENS if token not in cmake_content]
    if missing_audio_core_required:
        failures.append(
            "libs/audio_core/CMakeLists.txt no longer documents the direct single-lane "
            "module implementation and module file-set wiring: "
            + ", ".join(missing_audio_core_required)
        )

    android_native_package_content = _ANDROID_NATIVE_PACKAGE_CMAKE_PATH.read_text(encoding="utf-8")
    missing_android_required = [token for token in _ANDROID_NATIVE_PACKAGE_REQUIRED_TOKENS if token not in android_native_package_content]
    if missing_android_required:
        failures.append(
            "apps/audio_android/native_package/CMakeLists.txt missing packaged-target tokens: "
            + ", ".join(missing_android_required)
        )
    present_android_forbidden = [token for token in _ANDROID_NATIVE_PACKAGE_FORBIDDEN_TOKENS if token in android_native_package_content]
    if present_android_forbidden:
        failures.append(
            "apps/audio_android/native_package/CMakeLists.txt still lists bag_core modules-only "
            "sources: "
            + ", ".join(present_android_forbidden)
        )

    android_native_package_objects_content = _ANDROID_NATIVE_PACKAGE_OBJECTS_CMAKE_PATH.read_text(encoding="utf-8")
    missing_android_package_objects_required = [
        token for token in _ANDROID_NATIVE_PACKAGE_OBJECTS_REQUIRED_TOKENS if token not in android_native_package_objects_content
    ]
    if missing_android_package_objects_required:
        failures.append(
            "apps/audio_android/native_package/src/CMakeLists.txt missing package-object target tokens: "
            + ", ".join(missing_android_package_objects_required)
        )

    android_bag_api_package_content = _ANDROID_BAG_API_PACKAGE_SOURCE_PATH.read_text(encoding="utf-8")
    missing_android_bag_api_required = [token for token in _ANDROID_BAG_API_PACKAGE_REQUIRED_TOKENS if token not in android_bag_api_package_content]
    if missing_android_bag_api_required:
        failures.append(
            "apps/audio_android/native_package/src/bag_api_package.cpp missing package-owned "
            "boundary implementation tokens: "
            + ", ".join(missing_android_bag_api_required)
        )
    present_android_bag_api_forbidden = [token for token in _ANDROID_BAG_API_PACKAGE_FORBIDDEN_TOKENS if token in android_bag_api_package_content]
    if present_android_bag_api_forbidden:
        failures.append(
            "apps/audio_android/native_package/src/bag_api_package.cpp still uses the retired "
            "source-wrapper entry path: "
            + ", ".join(present_android_bag_api_forbidden)
        )

    for path, required_tokens in sorted(_ANDROID_AUDIO_CORE_PACKAGE_SOURCE_RULES.items()):
        content = path.read_text(encoding="utf-8")
        missing_required = [token for token in required_tokens if token not in content]
        if missing_required:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} missing package-owned implementation tokens: " + ", ".join(missing_required)
            )
        present_forbidden = [token for token in _ANDROID_AUDIO_CORE_PACKAGE_SOURCE_FORBIDDEN_TOKENS[path] if token in content]
        if present_forbidden:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} still includes retired module source entry paths: " + ", ".join(present_forbidden)
            )

    for path in iter_code_files():
        content = path.read_text(encoding="utf-8")
        present_tokens = [token for token in _RETIRED_WRAPPER_INCLUDE_TOKENS if token in content]
        if present_tokens:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} references retired wrapper headers: " + ", ".join(present_tokens)
            )

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError("Retired wrapper regression detected:\n" f"{joined}")
