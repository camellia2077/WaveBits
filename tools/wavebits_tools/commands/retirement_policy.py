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

_UNIT_TEST_REQUIRED_TOKENS: tuple[str, ...] = (
    '#include "test_std_support.h"',
    "bag::flash::EncodeBytesToPcm16",
    "bag::pro::EncodeTextToPayload",
    "bag::transport::compat::EncodeFrame",
    "bag::transport::compat::DecodeFrame",
)

_AUDIO_CORE_CMAKE_PATH = ROOT_DIR / "libs" / "audio_core" / "CMakeLists.txt"
_AUDIO_CORE_CMAKE_FORBIDDEN_TOKENS: tuple[str, ...] = (
    "src/pro/text_codec.cpp",
    "src/pro/frame_codec.cpp",
)
_AUDIO_CORE_FSK_MODULES_ONLY_PATTERN = re.compile(
    r"if\(WAVEBITS_HOST_MODULES\)\s+"
    r"list\(APPEND bag_core_sources\s+"
    r"src/fsk/fsk_codec\.cpp\s+"
    r"\)\s+"
    r"endif\(\)",
    re.MULTILINE,
)

_LEGACY_HEADER_PATHS: tuple[Path, ...] = (
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

_BAG_API_PATH = ROOT_DIR / "libs" / "audio_api" / "src" / "bag_api.cpp"
_BAG_API_REQUIRED_TOKENS: tuple[str, ...] = (
    '#include "bag/legacy/common/config.h"',
    '#include "bag/legacy/common/version.h"',
    '#include "bag/legacy/transport/transport.h"',
)
_BAG_API_FORBIDDEN_TOKENS: tuple[str, ...] = (
    '#include "bag/common/config.h"',
    '#include "bag/common/version.h"',
    '#include "bag/transport/transport.h"',
)

_UNIT_TESTS_PATH = ROOT_DIR / "Test" / "unit" / "unit_tests.cpp"
_UNIT_TEST_REQUIRED_TOKENS: tuple[str, ...] = (
    '#include "bag/legacy/flash/phy_clean.h"',
    '#include "bag/legacy/pro/codec.h"',
    '#include "bag/legacy/transport/compat/frame_codec.h"',
)
_UNIT_TEST_FORBIDDEN_TOKENS: tuple[str, ...] = (
    '#include "bag/flash/phy_clean.h"',
    '#include "bag/pro/codec.h"',
    '#include "bag/transport/compat/frame_codec.h"',
)

_AUDIO_CORE_SOURCE_REQUIRED_TOKENS: dict[Path, tuple[str, ...]] = {
    ROOT_DIR / "libs" / "audio_core" / "src" / "flash" / "codec.cpp": (
        '#include "bag/legacy/flash/codec.h"',
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "flash" / "phy_clean.cpp": (
        '#include "bag/legacy/flash/phy_clean.h"',
        '#include "bag/legacy/flash/codec.h"',
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pipeline" / "pipeline.cpp": (
        '#include "bag/legacy/pipeline/pipeline.h"',
        '#include "bag/legacy/transport/transport.h"',
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "codec.cpp": (
        '#include "bag/legacy/pro/codec.h"',
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "phy_clean.cpp": (
        '#include "bag/legacy/pro/phy_clean.h"',
        '#include "bag/legacy/pro/codec.h"',
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "phy_compat.cpp": (
        '#include "bag/legacy/pro/phy_compat.h"',
        '#include "bag/legacy/flash/phy_clean.h"',
        '#include "bag/legacy/pro/codec.h"',
        '#include "bag/legacy/transport/compat/frame_codec.h"',
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "transport" / "compat" / "frame_codec.cpp": (
        '#include "bag/legacy/transport/compat/frame_codec.h"',
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "transport" / "transport.cpp": (
        '#include "bag/legacy/transport/transport.h"',
        '#include "bag/legacy/flash/phy_clean.h"',
        '#include "bag/legacy/pro/phy_clean.h"',
        '#include "bag/legacy/ultra/phy_clean.h"',
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "ultra" / "codec.cpp": (
        '#include "bag/legacy/ultra/codec.h"',
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "ultra" / "phy_clean.cpp": (
        '#include "bag/legacy/ultra/phy_clean.h"',
        '#include "bag/legacy/ultra/codec.h"',
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "ultra" / "phy_compat.cpp": (
        '#include "bag/legacy/ultra/phy_compat.h"',
        '#include "bag/legacy/flash/phy_clean.h"',
        '#include "bag/legacy/transport/compat/frame_codec.h"',
        '#include "bag/legacy/ultra/codec.h"',
    ),
}

_LEGACY_INCLUDE_PREFIX = '#include "bag/legacy/'
_LEGACY_HEADERS_ROOT = ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "legacy"
_ALLOWED_LEGACY_INCLUDE_ROOTS: tuple[Path, ...] = (
    ROOT_DIR / "libs" / "audio_core" / "src",
)
_ALLOWED_LEGACY_INCLUDE_OWNERS: set[Path] = {
    _BAG_API_PATH,
    _UNIT_TESTS_PATH,
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "link" / "link_layer.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "phy" / "fun" / "fun_phy.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "phy" / "pro" / "pro_phy.h",
}


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
            "Phase 17 regression detected in audited boundary-adjacent host files:\n"
            f"{joined}"
        )


def run_phase18_compatibility_eviction_policy_checks() -> None:
    failures: list[str] = []

    for path in _RETIRED_WRAPPER_PATHS:
        if path.exists():
            failures.append(f"retired wrapper still exists: {path.relative_to(ROOT_DIR)}")

    unit_tests_content = _UNIT_TESTS_PATH.read_text(encoding="utf-8")
    missing_required = [
        token for token in _UNIT_TEST_REQUIRED_TOKENS if token not in unit_tests_content
    ]
    if missing_required:
        failures.append(
            "Test/unit/unit_tests.cpp missing required Phase 18 coverage tokens: "
            + ", ".join(missing_required)
        )

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
    if not _AUDIO_CORE_FSK_MODULES_ONLY_PATTERN.search(cmake_content):
        failures.append(
            "libs/audio_core/CMakeLists.txt no longer documents src/fsk/fsk_codec.cpp "
            "as a WAVEBITS_HOST_MODULES-only source"
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
            "Phase 18 compatibility-eviction regression detected:\n"
            f"{joined}"
        )


def run_phase19_bridge_retirement_policy_checks() -> None:
    failures: list[str] = []

    for path in _LEGACY_HEADER_PATHS:
        if not path.exists():
            failures.append(f"Phase 19 legacy carve-out header missing: {path.relative_to(ROOT_DIR)}")

    for path in _RETIRED_BRIDGE_HEADER_PATHS:
        if path.exists():
            failures.append(f"retired Phase 19 bridge header still exists: {path.relative_to(ROOT_DIR)}")

    for path, required_tokens, forbidden_tokens in (
        (_BAG_API_PATH, _BAG_API_REQUIRED_TOKENS, _BAG_API_FORBIDDEN_TOKENS),
        (_UNIT_TESTS_PATH, _UNIT_TEST_REQUIRED_TOKENS, _UNIT_TEST_FORBIDDEN_TOKENS),
    ):
        content = path.read_text(encoding="utf-8")
        missing_required = [token for token in required_tokens if token not in content]
        if missing_required:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} missing required Phase 19 carve-out tokens: "
                + ", ".join(missing_required)
            )

        present_forbidden = [token for token in forbidden_tokens if token in content]
        if present_forbidden:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} still depends on shared bridge headers: "
                + ", ".join(present_forbidden)
            )

    for path, required_tokens in sorted(_AUDIO_CORE_SOURCE_REQUIRED_TOKENS.items()):
        content = path.read_text(encoding="utf-8")
        missing_required = [token for token in required_tokens if token not in content]
        if missing_required:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} missing required Phase 19 legacy includes: "
                + ", ".join(missing_required)
            )

    for path in _iter_code_files():
        content = path.read_text(encoding="utf-8")

        present_retired_tokens = [
            token for token in _RETIRED_BRIDGE_INCLUDE_TOKENS if token in content
        ]
        if present_retired_tokens:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} references retired Phase 19 bridge headers: "
                + ", ".join(present_retired_tokens)
            )

        if _LEGACY_INCLUDE_PREFIX not in content:
            continue
        if path.is_relative_to(_LEGACY_HEADERS_ROOT):
            continue
        if _is_allowed_legacy_include_owner(path):
            continue

        failures.append(
            f"{path.relative_to(ROOT_DIR)} should not include Phase 19 legacy carve-out headers"
        )

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError(
            "Phase 19 bridge-retirement regression detected:\n"
            f"{joined}"
        )


def run_retirement_policy_checks() -> None:
    run_phase17_boundary_host_standardization_policy_checks()
    run_phase18_compatibility_eviction_policy_checks()
    run_phase19_bridge_retirement_policy_checks()
