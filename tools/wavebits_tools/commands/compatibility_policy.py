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
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
        "std::unique_ptr<IPipeline>",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "transport" / "facade.cppm": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
        "std::vector<std::int16_t>* out_pcm",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "transport" / "compat" / "frame_codec.cppm": (
        "WAVEBITS_CORE_IMPORT_STD",
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


def _iter_library_sources() -> list[Path]:
    sources: list[Path] = []
    libs_dir = ROOT_DIR / "libs"
    for library_dir in sorted(path for path in libs_dir.iterdir() if path.is_dir()):
        src_dir = library_dir / "src"
        if not src_dir.is_dir():
            continue
        sources.extend(sorted(src_dir.rglob("*.cpp")))
    return sources


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
            "Phase 9 compatibility-header regression detected in libs/*/src:\n"
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
                f"{path.relative_to(ROOT_DIR)} should stay retired after Phase 19"
            )

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError(
            "Phase 15 regression detected in audited direct consumer files:\n"
            f"{joined}"
        )


def run_phase16_outer_header_standardization_policy_checks() -> None:
    failures: list[str] = []
    for path in _PHASE16_RETIRED_OUTER_HEADERS:
        if path.exists():
            failures.append(
                f"{path.relative_to(ROOT_DIR)} should stay retired after Phase 19"
            )

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError(
            "Phase 16 regression detected in audited outer compatibility files:\n"
            f"{joined}"
        )


def run_compatibility_policy_checks() -> None:
    run_phase9_compatibility_header_policy_checks()
    run_phase15_consumer_standardization_policy_checks()
    run_phase16_outer_header_standardization_policy_checks()
