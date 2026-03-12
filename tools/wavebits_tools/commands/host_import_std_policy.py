from __future__ import annotations

from pathlib import Path

from ..constants import ROOT_DIR
from ..errors import ToolError


_PHASE11_IMPORT_STD_RULES: dict[Path, tuple[str, ...]] = {
    ROOT_DIR / "libs" / "audio_core" / "CMakeLists.txt": (
        "WAVEBITS_CORE_IMPORT_STD=1",
        "CXX_MODULE_STD ON",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "flash" / "codec.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "flash" / "codec.clang.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "codec.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "codec.clang.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "ultra" / "codec.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "ultra" / "codec.clang.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "transport" / "compat" / "frame_codec.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "transport" / "compat" / "frame_codec.clang.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pipeline" / "pipeline.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pipeline" / "pipeline.clang.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "transport" / "transport.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "transport" / "transport.clang.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
}

_PHASE12_IMPORT_STD_RULES: dict[Path, tuple[str, ...]] = {
    ROOT_DIR / "libs" / "audio_core" / "src" / "flash" / "phy_clean.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "flash" / "phy_clean.clang.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "phy_clean.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "phy_clean.clang.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "ultra" / "phy_clean.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "ultra" / "phy_clean.clang.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "phy_compat.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "phy_compat.clang.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "ultra" / "phy_compat.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "ultra" / "phy_compat.clang.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "fsk" / "fsk_codec.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "fsk" / "fsk_codec.clang.cpp": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "flash" / "phy_clean.cppm": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "pro" / "phy_clean.cppm": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "ultra" / "phy_clean.cppm": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "pro" / "phy_compat.cppm": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "ultra" / "phy_compat.cppm": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "fsk" / "codec.cppm": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
}

_PHASE13_IMPORT_STD_RULES: dict[Path, tuple[str, ...]] = {
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "flash" / "codec.cppm": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "pro" / "codec.cppm": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "ultra" / "codec.cppm": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "transport" / "compat" / "frame_codec.cppm": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "transport" / "facade.cppm": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "pipeline" / "pipeline.cppm": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
    ),
}

_PHASE14_IMPORT_STD_RULES: dict[Path, tuple[str, ...]] = {
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "common" / "config.cppm": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
        "std::uint8_t",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "common" / "types.cppm": (
        "WAVEBITS_CORE_IMPORT_STD",
        "import std;",
        "std::int16_t",
        "std::size_t",
        "std::int64_t",
        "std::uint8_t",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "common" / "error_code.cppm": (
        "export module bag.common.error_code;",
        "enum class ErrorCode",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "common" / "version.cppm": (
        "export module bag.common.version;",
        "CoreVersion()",
    ),
}

_PHASE14_RETIRED_COMMON_HEADERS: tuple[Path, ...] = (
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "common" / "config.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "common" / "error_code.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "common" / "types.h",
    ROOT_DIR / "libs" / "audio_core" / "include" / "bag" / "common" / "version.h",
)


def _run_required_token_rules(
    rules: dict[Path, tuple[str, ...]],
    *,
    error_prefix: str,
) -> None:
    failures: list[str] = []
    for path, required_tokens in sorted(rules.items()):
        content = path.read_text(encoding="utf-8")
        missing_tokens = [token for token in required_tokens if token not in content]
        if missing_tokens:
            missing = ", ".join(missing_tokens)
            failures.append(f"{path.relative_to(ROOT_DIR)} missing: {missing}")

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError(f"{error_prefix}\n{joined}")


def run_phase11_import_std_policy_checks() -> None:
    _run_required_token_rules(
        _PHASE11_IMPORT_STD_RULES,
        error_prefix="Phase 11 import-std regression detected in audited host pilot files:",
    )


def run_phase12_import_std_policy_checks() -> None:
    _run_required_token_rules(
        _PHASE12_IMPORT_STD_RULES,
        error_prefix="Phase 12 import-std regression detected in audited host expansion files:",
    )


def run_phase13_import_std_policy_checks() -> None:
    _run_required_token_rules(
        _PHASE13_IMPORT_STD_RULES,
        error_prefix="Phase 13 import-std regression detected in audited core interface files:",
    )


def run_phase14_import_std_policy_checks() -> None:
    failures: list[str] = []
    for path, required_tokens in sorted(_PHASE14_IMPORT_STD_RULES.items()):
        content = path.read_text(encoding="utf-8")
        missing_tokens = [token for token in required_tokens if token not in content]
        if missing_tokens:
            missing = ", ".join(missing_tokens)
            failures.append(f"{path.relative_to(ROOT_DIR)} missing: {missing}")

    for path in _PHASE14_RETIRED_COMMON_HEADERS:
        if path.exists():
            failures.append(
                f"{path.relative_to(ROOT_DIR)} should stay retired after Phase 19"
            )

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError(
            "Phase 14 regression detected in audited bag.common foundation files:\n"
            f"{joined}"
        )


def run_host_import_std_policy_checks() -> None:
    run_phase11_import_std_policy_checks()
    run_phase12_import_std_policy_checks()
    run_phase13_import_std_policy_checks()
    run_phase14_import_std_policy_checks()
