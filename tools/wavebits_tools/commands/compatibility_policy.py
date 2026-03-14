from __future__ import annotations

from pathlib import Path

from ..constants import ROOT_DIR
from ..errors import ToolError


_COMPATIBILITY_ONLY_HEADER_ALLOWLIST: dict[str, tuple[Path, ...]] = {
    '#include "bag/pro/phy_compat.h"': (
        ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "phy_compat.cpp",
    ),
    '#include "bag/ultra/phy_compat.h"': (
        ROOT_DIR / "libs" / "audio_core" / "src" / "ultra" / "phy_compat.cpp",
    ),
}

_PHASE15_CONSUMER_RULES: dict[Path, tuple[str, ...]] = {
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "transport" / "decoder.cppm": (
        "export import bag.common.types;",
        "PcmBlock",
        "TextResult",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "pipeline" / "pipeline.cppm": (
        "export module bag.pipeline;",
        "import std;",
        "std::unique_ptr<IPipeline>",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "transport" / "facade.cppm": (
        "export module bag.transport.facade;",
        "import std;",
        "std::vector<std::int16_t>* out_pcm",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "transport" / "compat" / "frame_codec.cppm": (
        "export module bag.transport.compat.frame_codec;",
        "import std;",
        "std::uint8_t",
        "std::uint16_t",
        "std::size_t",
    ),
}

_PHASE15_RETIRED_HEADERS: tuple[Path, ...] = (
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "flash" / "phy_clean.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "pipeline" / "pipeline.h",
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
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "ultra" / "phy_compat.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "ultra" / "phy_clean.h",
)

_PHASE16_RETIRED_OUTER_HEADERS: tuple[Path, ...] = (
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "flash" / "codec.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "pro" / "codec.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "ultra" / "codec.h",
)

_INTERFACE_COMMON_HEADERS_ROOT = (
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "interface" / "common"
)
_INTERFACE_COMMON_HEADER_PATHS: tuple[Path, ...] = (
    _INTERFACE_COMMON_HEADERS_ROOT / "config.h",
    _INTERFACE_COMMON_HEADERS_ROOT / "error_code.h",
    _INTERFACE_COMMON_HEADERS_ROOT / "types.h",
)
_RESERVED_INTERFACE_OWNER_PATHS: tuple[Path, ...] = (
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "link" / "link_layer.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "phy" / "fun" / "fun_phy.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "phy" / "pro" / "pro_phy.h",
)
_ALLOWED_INTERFACE_COMMON_INCLUDE_OWNERS: set[Path] = set(_RESERVED_INTERFACE_OWNER_PATHS)
_INTERFACE_COMMON_INCLUDE_PREFIX = '#include "bag/interface/common/'
_RESERVED_INTERFACE_HEADER_RULES: dict[Path, tuple[tuple[str, ...], tuple[str, ...]]] = {
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "link" / "link_layer.h": (
        (
            '#include "bag/interface/common/error_code.h"',
            '#include "bag/interface/common/types.h"',
        ),
        (
            '#include "bag/legacy/common/error_code.h"',
            '#include "bag/legacy/common/types.h"',
            "import std;",
        ),
    ),
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "phy" / "fun" / "fun_phy.h": (
        (
            '#include "bag/interface/common/error_code.h"',
            '#include "bag/interface/common/types.h"',
        ),
        (
            '#include "bag/legacy/common/error_code.h"',
            '#include "bag/legacy/common/types.h"',
            "import std;",
        ),
    ),
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "phy" / "pro" / "pro_phy.h": (
        (
            '#include "bag/interface/common/error_code.h"',
            '#include "bag/interface/common/types.h"',
        ),
        (
            '#include "bag/legacy/common/error_code.h"',
            '#include "bag/legacy/common/types.h"',
            "import std;",
        ),
    ),
}
_RESERVED_INTERFACE_NO_IMPORT_STD_PATHS: tuple[Path, ...] = (
    *_INTERFACE_COMMON_HEADER_PATHS,
    *_RESERVED_INTERFACE_OWNER_PATHS,
)
_RESERVED_INTERFACE_FORBIDDEN_MODULE_TOKENS: tuple[str, ...] = (
    "module bag.interface",
    "import bag.interface",
)


def _iter_library_sources() -> list[Path]:
    sources: list[Path] = []
    libs_dir = ROOT_DIR / "libs"
    for library_dir in sorted(path for path in libs_dir.iterdir() if path.is_dir()):
        src_dir = library_dir / "src"
        if not src_dir.is_dir():
            continue
        sources.extend(sorted(src_dir.rglob("*.cpp")))
    return sources


def _iter_repo_code_files() -> list[Path]:
    code_files: set[Path] = set()
    for root in (ROOT_DIR / "libs", ROOT_DIR / "Test", ROOT_DIR / "apps"):
        if not root.is_dir():
            continue
        for pattern in ("*.cpp", "*.h", "*.hpp", "*.cppm", "*.cc", "*.cxx", "*.inc"):
            code_files.update(path for path in root.rglob(pattern) if path.is_file())
    return sorted(code_files)


def run_phase9_compatibility_header_policy_checks() -> None:
    failures: list[str] = []
    for path in _iter_library_sources():
        content = path.read_text(encoding="utf-8")
        for header_token, allowed_paths in _COMPATIBILITY_ONLY_HEADER_ALLOWLIST.items():
            if header_token not in content or path in allowed_paths:
                continue
            failures.append(
                f"{path.relative_to(ROOT_DIR)} depends on compatibility-only header: {header_token}"
            )

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError(
            "Compatibility-only include regression detected in libs/*/src:\n"
            f"{joined}"
        )


def run_phase15_consumer_standardization_policy_checks() -> None:
    failures: list[str] = []
    for path, required_tokens in sorted(_PHASE15_CONSUMER_RULES.items()):
        content = path.read_text(encoding="utf-8")
        missing_tokens = [token for token in required_tokens if token not in content]
        if missing_tokens:
            missing = ", ".join(missing_tokens)
            failures.append(f"{path.relative_to(ROOT_DIR)} missing: {missing}")

    for path in _PHASE15_RETIRED_HEADERS:
        if path.exists():
            failures.append(
                f"{path.relative_to(ROOT_DIR)} should stay retired in the current direct-consumer end-state"
            )

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError(
            "Direct-consumer standardization regression detected in audited files:\n"
            f"{joined}"
        )


def run_phase16_outer_header_standardization_policy_checks() -> None:
    failures: list[str] = []
    for path in _PHASE16_RETIRED_OUTER_HEADERS:
        if path.exists():
            failures.append(
                f"{path.relative_to(ROOT_DIR)} should stay retired in the current compatibility end-state"
            )

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError(
            "Retired outer compatibility surface regression detected:\n"
            f"{joined}"
        )


def run_reserved_interface_boundary_policy_checks() -> None:
    failures: list[str] = []

    for path in _INTERFACE_COMMON_HEADER_PATHS:
        if not path.exists():
            failures.append(
                f"reserved interface declaration header missing: {path.relative_to(ROOT_DIR)}"
            )

    if _INTERFACE_COMMON_HEADERS_ROOT.is_dir():
        unexpected_entries = sorted(
            path.relative_to(ROOT_DIR)
            for path in _INTERFACE_COMMON_HEADERS_ROOT.iterdir()
            if path not in _INTERFACE_COMMON_HEADER_PATHS
        )
        if unexpected_entries:
            failures.append(
                "reserved interface declaration layer contains unexpected entries: "
                + ", ".join(str(path) for path in unexpected_entries)
            )

    for path in _RESERVED_INTERFACE_NO_IMPORT_STD_PATHS:
        if not path.exists():
            continue
        content = path.read_text(encoding="utf-8")
        if "import std;" in content:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} should remain include-based and must not adopt import std;"
            )

    for path, (required_tokens, forbidden_tokens) in sorted(_RESERVED_INTERFACE_HEADER_RULES.items()):
        content = path.read_text(encoding="utf-8")
        missing_required = [token for token in required_tokens if token not in content]
        if missing_required:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} missing required reserved-interface includes: "
                + ", ".join(missing_required)
            )

        present_forbidden = [token for token in forbidden_tokens if token in content]
        if present_forbidden:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} contains forbidden reserved-interface tokens: "
                + ", ".join(present_forbidden)
            )

    for path in _iter_repo_code_files():
        content = path.read_text(encoding="utf-8")

        present_interface_module_tokens = [
            token for token in _RESERVED_INTERFACE_FORBIDDEN_MODULE_TOKENS if token in content
        ]
        if present_interface_module_tokens:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} should not declare or import bag.interface modules: "
                + ", ".join(present_interface_module_tokens)
            )

        if _INTERFACE_COMMON_INCLUDE_PREFIX not in content:
            continue
        if path.is_relative_to(_INTERFACE_COMMON_HEADERS_ROOT):
            continue
        if path in _ALLOWED_INTERFACE_COMMON_INCLUDE_OWNERS:
            continue

        failures.append(
            f"{path.relative_to(ROOT_DIR)} should not include reserved interface declaration headers outside approved owners"
        )

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError(
            "Reserved-interface declaration boundary regression detected:\n"
            f"{joined}"
        )


def run_compatibility_policy_checks() -> None:
    run_phase9_compatibility_header_policy_checks()
    run_phase15_consumer_standardization_policy_checks()
    run_reserved_interface_boundary_policy_checks()
    run_phase16_outer_header_standardization_policy_checks()
