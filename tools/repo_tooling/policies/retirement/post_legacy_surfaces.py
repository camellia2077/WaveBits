from __future__ import annotations

from pathlib import Path

from ...constants import ROOT_DIR
from ...errors import ToolError
from .common import is_allowed_legacy_include_owner, iter_code_files, legacy_include_prefix, read_modules_leaf_smoke_content


_ROOT_CMAKE_PATH = ROOT_DIR / "CMakeLists.txt"
_ROOT_SINGLE_LANE_REQUIRED_TOKENS: tuple[str, ...] = (
    'set(CMAKE_EXPERIMENTAL_CXX_IMPORT_STD "d0edc3af-4c50-42ea-a356-e2862fe7a444")',
    "Root host currently supports the single clang++ + Ninja lane only.",
    "Current root host target requires C++23 import std support from the active Clang toolchain.",
    "set(CMAKE_CXX_STANDARD 20)",
)
_ROOT_SINGLE_LANE_FORBIDDEN_TOKENS: tuple[str, ...] = ("WAVEBITS_HOST_MODULES",)
_CLI_PATH = ROOT_DIR / "tools" / "repo_tooling" / "cli" / "app.py"
_CLI_HOST_OFF_RETIREMENT_FORBIDDEN_TOKENS: tuple[str, ...] = (
    '"--no-modules"',
    "WAVEBITS_HOST_MODULES=OFF",
    "--experimental-modules",
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
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "legacy" / "transport" / "compat" / "frame_codec.h",
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
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "transport" / "compat" / "frame_codec.h",
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
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "internal" / "transport" / "facade.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "internal" / "transport" / "compat" / "frame_codec.h",
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
_UNIT_TESTS_PATH = ROOT_DIR / "libs" / "audio_io" / "tests" / "unit_tests.cpp"
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
_MODULES_LEAF_REQUIRED_TOKENS: tuple[str, ...] = (
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
    "bag::pro::kSymbolsPerPayloadByte",
    "import bag.transport.compat.frame_codec;",
    "bag::transport::compat::EncodeFrame",
    "bag::transport::compat::DecodeFrame",
    "bag::transport::compat::kMaxFramePayloadBytes",
)
_AUDIO_CORE_SOURCE_REQUIRED_TOKENS: dict[Path, tuple[str, ...]] = {}


def run_post_legacy_surfaces_policy_checks() -> None:
    failures: list[str] = []

    root_cmake_content = _ROOT_CMAKE_PATH.read_text(encoding="utf-8")
    missing_root_required = [token for token in _ROOT_SINGLE_LANE_REQUIRED_TOKENS if token not in root_cmake_content]
    if missing_root_required:
        failures.append(
            f"{_ROOT_CMAKE_PATH.relative_to(ROOT_DIR)} missing direct single-lane host tokens: " + ", ".join(missing_root_required)
        )
    present_root_forbidden = [token for token in _ROOT_SINGLE_LANE_FORBIDDEN_TOKENS if token in root_cmake_content]
    if present_root_forbidden:
        failures.append(
            f"{_ROOT_CMAKE_PATH.relative_to(ROOT_DIR)} still exposes retired root host switch tokens: " + ", ".join(present_root_forbidden)
        )

    cli_content = _CLI_PATH.read_text(encoding="utf-8")
    present_cli_forbidden = [token for token in _CLI_HOST_OFF_RETIREMENT_FORBIDDEN_TOKENS if token in cli_content]
    if present_cli_forbidden:
        failures.append(
            f"{_CLI_PATH.relative_to(ROOT_DIR)} still exposes retired host-off tokens: " + ", ".join(present_cli_forbidden)
        )

    for path in _REMOVED_LEGACY_HEADER_PATHS:
        if path.exists():
            failures.append(f"deleted legacy header reintroduced: {path.relative_to(ROOT_DIR)}")
    for path in _RETIRED_BRIDGE_HEADER_PATHS:
        if path.exists():
            failures.append(f"retired bridge header still exists: {path.relative_to(ROOT_DIR)}")
    for path in _RETIRED_INTERNAL_HEADER_PATHS:
        if path.exists():
            failures.append(f"modules-only compat internal header still exists: {path.relative_to(ROOT_DIR)}")

    bag_api_content = _BAG_API_PATH.read_text(encoding="utf-8")
    missing_bag_api_required = [token for token in _BAG_API_REQUIRED_TOKENS if token not in bag_api_content]
    if missing_bag_api_required:
        failures.append(
            f"{_BAG_API_PATH.relative_to(ROOT_DIR)} missing required internal declaration includes: " + ", ".join(missing_bag_api_required)
        )
    present_bag_api_forbidden = [token for token in _BAG_API_FORBIDDEN_TOKENS if token in bag_api_content]
    if present_bag_api_forbidden:
        failures.append(
            f"{_BAG_API_PATH.relative_to(ROOT_DIR)} still depends on retired or legacy declaration headers: " + ", ".join(present_bag_api_forbidden)
        )

    unit_tests_content = _UNIT_TESTS_PATH.read_text(encoding="utf-8")
    missing_unit_test_required = [token for token in _UNIT_TEST_INTERNAL_REQUIRED_TOKENS if token not in unit_tests_content]
    if missing_unit_test_required:
        failures.append(
            f"{_UNIT_TESTS_PATH.relative_to(ROOT_DIR)} missing required internal declaration includes: " + ", ".join(missing_unit_test_required)
        )
    present_unit_test_forbidden = [
        token
        for token in _UNIT_TEST_SHARED_BRIDGE_FORBIDDEN_TOKENS + _UNIT_TEST_LEGACY_FORBIDDEN_TOKENS
        if token in unit_tests_content
    ]
    if present_unit_test_forbidden:
        failures.append(
            f"{_UNIT_TESTS_PATH.relative_to(ROOT_DIR)} still depends on retired or legacy declaration headers: " + ", ".join(present_unit_test_forbidden)
        )
    present_unit_test_owner_tokens = [token for token in _UNIT_TEST_OWNER_CLEANUP_FORBIDDEN_TOKENS if token in unit_tests_content]
    if present_unit_test_owner_tokens:
        failures.append(
            f"{_UNIT_TESTS_PATH.relative_to(ROOT_DIR)} still owns flash/pro low-level coverage tokens: " + ", ".join(present_unit_test_owner_tokens)
        )

    modules_leaf_content = read_modules_leaf_smoke_content()
    missing_leaf_required = [token for token in _MODULES_LEAF_REQUIRED_TOKENS if token not in modules_leaf_content]
    if missing_leaf_required:
        failures.append(
            "Test/modules/leaf_module_smoke*.cpp missing required "
            "bag.transport.compat.frame_codec module coverage tokens: " + ", ".join(missing_leaf_required)
        )

    for path, required_tokens in sorted(_AUDIO_CORE_SOURCE_REQUIRED_TOKENS.items()):
        content = path.read_text(encoding="utf-8")
        missing_required = [token for token in required_tokens if token not in content]
        if missing_required:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} missing required internal declaration includes: " + ", ".join(missing_required)
            )

    prefix = legacy_include_prefix()
    for path in iter_code_files():
        content = path.read_text(encoding="utf-8")
        present_retired_tokens = [token for token in _RETIRED_BRIDGE_INCLUDE_TOKENS if token in content]
        if present_retired_tokens:
            failures.append(f"{path.relative_to(ROOT_DIR)} references retired bridge headers: " + ", ".join(present_retired_tokens))
        present_retired_internal_tokens = [token for token in _RETIRED_INTERNAL_INCLUDE_TOKENS if token in content]
        if present_retired_internal_tokens:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} references retired internal compat headers: " + ", ".join(present_retired_internal_tokens)
            )
        if prefix in content and not is_allowed_legacy_include_owner(path):
            failures.append(f"{path.relative_to(ROOT_DIR)} should not include deleted legacy headers")

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError("Post-legacy / bridge-retirement policy regression detected:\n" f"{joined}")
