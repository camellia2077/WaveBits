from __future__ import annotations

from pathlib import Path

from ..constants import ROOT_DIR
from ..errors import ToolError


_PHASE11_IMPORT_STD_RULES: dict[Path, tuple[str, ...]] = {
    ROOT_DIR / "libs" / "audio_core" / "CMakeLists.txt": (
        "CXX_MODULE_STD ON",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "flash" / "codec.cpp": (
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "codec.cpp": (
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "ultra" / "codec.cpp": (
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "transport" / "compat" / "frame_codec.cpp": (
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pipeline" / "pipeline.cpp": (
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "transport" / "transport.cpp": (
        "import std;",
    ),
}

_PHASE12_IMPORT_STD_RULES: dict[Path, tuple[str, ...]] = {
    ROOT_DIR / "libs" / "audio_core" / "src" / "flash" / "phy_clean.cpp": (
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "phy_clean.cpp": (
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "ultra" / "phy_clean.cpp": (
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "phy_compat.cpp": (
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "ultra" / "phy_compat.cpp": (
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "fsk" / "fsk_codec.cpp": (
        "import std;",
    ),
}

_PHASE13_IMPORT_STD_RULES: dict[Path, tuple[str, ...]] = {
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "flash" / "codec.cppm": (
        "export module bag.flash.codec;",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "flash" / "phy_clean.cppm": (
        "export module bag.flash.phy_clean;",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "fsk" / "codec.cppm": (
        "export module bag.fsk.codec;",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "pro" / "codec.cppm": (
        "export module bag.pro.codec;",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "pro" / "phy_compat.cppm": (
        "export module bag.pro.phy_compat;",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "pro" / "phy_clean.cppm": (
        "export module bag.pro.phy_clean;",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "ultra" / "codec.cppm": (
        "export module bag.ultra.codec;",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "ultra" / "phy_compat.cppm": (
        "export module bag.ultra.phy_compat;",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "ultra" / "phy_clean.cppm": (
        "export module bag.ultra.phy_clean;",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "transport" / "compat" / "frame_codec.cppm": (
        "export module bag.transport.compat.frame_codec;",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "transport" / "facade.cppm": (
        "export module bag.transport.facade;",
        "import std;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "pipeline" / "pipeline.cppm": (
        "export module bag.pipeline;",
        "import std;",
    ),
}

_PHASE14_IMPORT_STD_RULES: dict[Path, tuple[str, ...]] = {
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "common" / "config.cppm": (
        "export module bag.common.config;",
        "import std;",
        "enum class TransportMode : std::uint8_t",
        "struct CoreConfig {",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "common" / "types.cppm": (
        "export module bag.common.types;",
        "import std;",
        "export import bag.common.config;",
        "std::size_t sample_count",
        "std::vector<std::uint8_t> bits",
        "std::string text",
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

_PHASE21_IMPORT_STD_RULES: dict[Path, tuple[str, ...]] = {
    ROOT_DIR / "libs" / "audio_io" / "CMakeLists.txt": (
        "CXX_MODULE_STD ON",
    ),
    ROOT_DIR / "libs" / "audio_io" / "modules" / "audio_io" / "wav.cppm": (
        "export module audio_io.wav;",
        "import std;",
        "std::vector<std::int16_t>",
    ),
    ROOT_DIR / "libs" / "audio_io" / "modules" / "audio_io" / "wav_impl.cpp": (
        "import std;",
        '#include "../../src/wav_io_backend.h"',
    ),
}

_PHASE22_IMPORT_STD_RULES: dict[Path, tuple[str, ...]] = {
    ROOT_DIR / "libs" / "audio_api" / "CMakeLists.txt": (
        "CXX_MODULE_STD ON",
    ),
    ROOT_DIR / "libs" / "audio_api" / "src" / "bag_api.cpp": (
        "import std;",
        "import bag.common.version;",
        '#include "bag_api_impl.inc"',
    ),
}

_IMPORT_STD_RULE_SETS: tuple[dict[Path, tuple[str, ...]], ...] = (
    _PHASE11_IMPORT_STD_RULES,
    _PHASE12_IMPORT_STD_RULES,
    _PHASE13_IMPORT_STD_RULES,
    _PHASE14_IMPORT_STD_RULES,
    _PHASE21_IMPORT_STD_RULES,
    _PHASE22_IMPORT_STD_RULES,
)
_HOST_IMPORT_STD_SCAN_ROOTS: tuple[Path, ...] = (
    ROOT_DIR / "libs" / "audio_core",
    ROOT_DIR / "libs" / "audio_api",
    ROOT_DIR / "libs" / "audio_io",
)
_REQUIRED_BASELINE_IMPORT_STD_CMAKE_PATHS: tuple[Path, ...] = (
    ROOT_DIR / "libs" / "audio_core" / "CMakeLists.txt",
    ROOT_DIR / "libs" / "audio_api" / "CMakeLists.txt",
    ROOT_DIR / "libs" / "audio_io" / "CMakeLists.txt",
)


def _collect_required_baseline_import_std_source_paths() -> tuple[Path, ...]:
    paths: set[Path] = set()
    for rules in _IMPORT_STD_RULE_SETS:
        for path, required_tokens in rules.items():
            if path.suffix not in {".cpp", ".cppm"}:
                continue
            if "import std;" not in required_tokens:
                continue
            paths.add(path)
    return tuple(sorted(paths))


_REQUIRED_BASELINE_IMPORT_STD_SOURCE_PATHS: tuple[Path, ...] = (
    _collect_required_baseline_import_std_source_paths()
)
_PROMOTED_CORE_MODULE_INTERFACE_IMPORT_STD_ONLY_RULES: dict[Path, tuple[str, ...]] = {
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "common" / "config.cppm": (
        "module;",
        "export module bag.common.config;",
        "import std;",
        "enum class TransportMode : std::uint8_t",
        "struct CoreConfig {",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "common" / "types.cppm": (
        "module;",
        "export module bag.common.types;",
        "import std;",
        "export import bag.common.config;",
        "std::size_t sample_count",
        "std::vector<std::uint8_t> bits",
        "std::string text",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "flash" / "codec.cppm": (
        "module;",
        "export module bag.flash.codec;",
        "import std;",
        "export import bag.common.error_code;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "flash" / "phy_clean.cppm": (
        "module;",
        "export module bag.flash.phy_clean;",
        "import std;",
        "export import bag.common.config;",
        "export import bag.common.error_code;",
        "export import bag.transport.decoder;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "fsk" / "codec.cppm": (
        "module;",
        "export module bag.fsk.codec;",
        "import std;",
        "std::vector<std::int16_t>",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "pro" / "codec.cppm": (
        "module;",
        "export module bag.pro.codec;",
        "import std;",
        "export import bag.common.error_code;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "pro" / "phy_compat.cppm": (
        "module;",
        "export module bag.pro.phy_compat;",
        "import std;",
        "export import bag.common.config;",
        "export import bag.common.error_code;",
        "export import bag.transport.decoder;",
        "std::unique_ptr<ITransportDecoder>",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "pro" / "phy_clean.cppm": (
        "module;",
        "export module bag.pro.phy_clean;",
        "import std;",
        "export import bag.common.config;",
        "export import bag.common.error_code;",
        "export import bag.transport.decoder;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "ultra" / "codec.cppm": (
        "module;",
        "export module bag.ultra.codec;",
        "import std;",
        "export import bag.common.error_code;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "ultra" / "phy_compat.cppm": (
        "module;",
        "export module bag.ultra.phy_compat;",
        "import std;",
        "export import bag.common.config;",
        "export import bag.common.error_code;",
        "export import bag.transport.decoder;",
        "std::unique_ptr<ITransportDecoder>",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "ultra" / "phy_clean.cppm": (
        "module;",
        "export module bag.ultra.phy_clean;",
        "import std;",
        "export import bag.common.config;",
        "export import bag.common.error_code;",
        "export import bag.transport.decoder;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "transport" / "compat" / "frame_codec.cppm": (
        "module;",
        "export module bag.transport.compat.frame_codec;",
        "import std;",
        "export import bag.common.config;",
        "export import bag.common.error_code;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "transport" / "facade.cppm": (
        "module;",
        "export module bag.transport.facade;",
        "import std;",
        "export import bag.common.config;",
        "export import bag.common.error_code;",
        "export import bag.transport.decoder;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "modules" / "bag" / "pipeline" / "pipeline.cppm": (
        "module;",
        "export module bag.pipeline;",
        "import std;",
        "export import bag.common.config;",
        "export import bag.common.error_code;",
        "export import bag.common.types;",
    ),
}
_PROMOTED_CORE_MODULE_INTERFACE_FORBIDDEN_TOKENS: tuple[str, ...] = (
    "#if !defined(",
    "#if defined(",
    "#include <",
)
_IMPORT_STD_SINGLE_PATH_FORBIDDEN_TOKENS: tuple[str, ...] = (
    "#if defined(",
    "#else",
)
_IMPORT_STD_SINGLE_PATH_WITH_INCLUDE_FALLBACK_FORBIDDEN_TOKENS: tuple[str, ...] = (
    "#if defined(",
    "#else",
    "#include <",
)
_BOUNDARY_HOST_SINGLE_PATH_FORBIDDEN_TOKENS: tuple[str, ...] = (
    "#if !defined(WAVEBITS_MODULE_IMPL_WRAPPER)",
) + _IMPORT_STD_SINGLE_PATH_WITH_INCLUDE_FALLBACK_FORBIDDEN_TOKENS
_CORE_MODULE_INTERFACE_DUAL_PATHS: tuple[Path, ...] = tuple(
    sorted(
        path
        for path in _REQUIRED_BASELINE_IMPORT_STD_SOURCE_PATHS
        if path.suffix == ".cppm"
        and path.is_relative_to(ROOT_DIR / "libs" / "audio_core" / "modules")
        and path not in _PROMOTED_CORE_MODULE_INTERFACE_IMPORT_STD_ONLY_RULES
    )
)
_CORE_MODULE_IMPLEMENTATION_DUAL_PATHS: tuple[Path, ...] = tuple(
    sorted(
        path
        for path in _REQUIRED_BASELINE_IMPORT_STD_SOURCE_PATHS
        if path.suffix == ".cpp"
        and path.is_relative_to(ROOT_DIR / "libs" / "audio_core" / "src")
    )
)
_AUDIO_IO_MODULE_INTERFACE_DUAL_PATHS: tuple[Path, ...] = (
    ROOT_DIR / "libs" / "audio_io" / "modules" / "audio_io" / "wav.cppm",
)
_AUDIO_IO_BACKEND_BRIDGE_DUAL_PATHS: tuple[Path, ...] = (
    ROOT_DIR / "libs" / "audio_io" / "modules" / "audio_io" / "wav_impl.cpp",
)
_BOUNDARY_HOST_DUAL_PATH_RULES: dict[Path, tuple[str, ...]] = {
    ROOT_DIR / "libs" / "audio_api" / "src" / "bag_api.cpp": (
        '#include "bag_api.h"',
        "import std;",
        "import bag.common.config;",
        "import bag.common.version;",
        "import bag.transport.facade;",
    ),
}


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


def _run_ordered_token_rules(
    rules: dict[Path, tuple[str, ...]],
    *,
    error_prefix: str,
) -> None:
    failures: list[str] = []
    for path, ordered_tokens in sorted(rules.items()):
        content = path.read_text(encoding="utf-8")
        search_start = 0
        for token in ordered_tokens:
            position = content.find(token, search_start)
            if position == -1:
                failures.append(f"{path.relative_to(ROOT_DIR)} missing or reordered token: {token}")
                break
            search_start = position + len(token)

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError(f"{error_prefix}\n{joined}")


def _run_forbidden_token_rules(
    paths: tuple[Path, ...],
    forbidden_tokens: tuple[str, ...],
    *,
    error_prefix: str,
) -> None:
    failures: list[str] = []
    for path in paths:
        content = path.read_text(encoding="utf-8")
        present_tokens = [token for token in forbidden_tokens if token in content]
        if present_tokens:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} contains forbidden tokens: {', '.join(present_tokens)}"
            )

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError(f"{error_prefix}\n{joined}")


def _iter_actual_import_std_source_paths() -> tuple[Path, ...]:
    paths: list[Path] = []
    for root in _HOST_IMPORT_STD_SCAN_ROOTS:
        for pattern in ("*.cpp", "*.cppm"):
            for path in sorted(root.rglob(pattern)):
                content = path.read_text(encoding="utf-8")
                if "import std;" in content:
                    paths.append(path)
    return tuple(sorted(paths))


def _iter_actual_import_std_cmake_paths() -> tuple[Path, ...]:
    paths: list[Path] = []
    libs_dir = ROOT_DIR / "libs"
    for path in sorted(libs_dir.rglob("CMakeLists.txt")):
        content = path.read_text(encoding="utf-8")
        if "IMPORT_STD=1" in content or "CXX_MODULE_STD ON" in content:
            paths.append(path)
    return tuple(paths)


def run_phase11_import_std_policy_checks() -> None:
    _run_required_token_rules(
        _PHASE11_IMPORT_STD_RULES,
        error_prefix="Host import-std required-baseline regression detected in core implementation files:",
    )


def run_phase12_import_std_policy_checks() -> None:
    _run_required_token_rules(
        _PHASE12_IMPORT_STD_RULES,
        error_prefix="Host import-std required-baseline regression detected in expanded implementation files:",
    )


def run_phase13_import_std_policy_checks() -> None:
    _run_required_token_rules(
        _PHASE13_IMPORT_STD_RULES,
        error_prefix="Host import-std required-baseline regression detected in promoted core module interfaces:",
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
                f"{path.relative_to(ROOT_DIR)} should stay retired in the current foundation end-state"
            )

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError(
            "Foundation module standardization regression detected in required bag.common baseline:\n"
            f"{joined}"
        )


def run_phase21_import_std_policy_checks() -> None:
    _run_required_token_rules(
        _PHASE21_IMPORT_STD_RULES,
        error_prefix="audio_io host import-std regression detected in required front-end baseline:",
    )


def run_phase22_import_std_policy_checks() -> None:
    _run_required_token_rules(
        _PHASE22_IMPORT_STD_RULES,
        error_prefix="Boundary-adjacent host import-std regression detected in required bag_api baseline:",
    )


def run_required_import_std_baseline_coverage_checks() -> None:
    failures: list[str] = []

    actual_source_paths = set(_iter_actual_import_std_source_paths())
    required_source_paths = set(_REQUIRED_BASELINE_IMPORT_STD_SOURCE_PATHS)
    unexpected_source_paths = sorted(path.relative_to(ROOT_DIR) for path in actual_source_paths - required_source_paths)
    missing_source_paths = sorted(path.relative_to(ROOT_DIR) for path in required_source_paths - actual_source_paths)
    if unexpected_source_paths:
        failures.append(
            "unexpected host import-std source files outside required baseline: "
            + ", ".join(str(path) for path in unexpected_source_paths)
        )
    if missing_source_paths:
        failures.append(
            "required-baseline host import-std source files no longer contain import std;: "
            + ", ".join(str(path) for path in missing_source_paths)
        )

    actual_cmake_paths = set(_iter_actual_import_std_cmake_paths())
    required_cmake_paths = set(_REQUIRED_BASELINE_IMPORT_STD_CMAKE_PATHS)
    unexpected_cmake_paths = sorted(path.relative_to(ROOT_DIR) for path in actual_cmake_paths - required_cmake_paths)
    missing_cmake_paths = sorted(path.relative_to(ROOT_DIR) for path in required_cmake_paths - actual_cmake_paths)
    if unexpected_cmake_paths:
        failures.append(
            "unexpected host import-std CMake owners outside required baseline: "
            + ", ".join(str(path) for path in unexpected_cmake_paths)
        )
    if missing_cmake_paths:
        failures.append(
            "required-baseline host import-std CMake owners missing import-std wiring: "
            + ", ".join(str(path) for path in missing_cmake_paths)
        )

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError(
            "Host import-std required-baseline coverage regression detected:\n"
            f"{joined}"
        )


def run_required_import_std_baseline_shape_checks() -> None:
    failures: list[str] = []

    categorized_paths = set(
        tuple(_PROMOTED_CORE_MODULE_INTERFACE_IMPORT_STD_ONLY_RULES)
        + _CORE_MODULE_INTERFACE_DUAL_PATHS
        + _CORE_MODULE_IMPLEMENTATION_DUAL_PATHS
        + _AUDIO_IO_MODULE_INTERFACE_DUAL_PATHS
        + _AUDIO_IO_BACKEND_BRIDGE_DUAL_PATHS
        + tuple(_BOUNDARY_HOST_DUAL_PATH_RULES)
    )
    required_source_paths = set(_REQUIRED_BASELINE_IMPORT_STD_SOURCE_PATHS)
    uncategorized_paths = sorted(path.relative_to(ROOT_DIR) for path in required_source_paths - categorized_paths)
    if uncategorized_paths:
        failures.append(
            "required-baseline host import-std source files missing retained-shape category: "
            + ", ".join(str(path) for path in uncategorized_paths)
        )

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError(
            "Host import-std required-baseline shape regression detected:\n"
            f"{joined}"
        )

    _run_ordered_token_rules(
        _PROMOTED_CORE_MODULE_INTERFACE_IMPORT_STD_ONLY_RULES,
        error_prefix="Promoted core module interface required-baseline regression detected:",
    )
    _run_forbidden_token_rules(
        tuple(_PROMOTED_CORE_MODULE_INTERFACE_IMPORT_STD_ONLY_RULES),
        _PROMOTED_CORE_MODULE_INTERFACE_FORBIDDEN_TOKENS,
        error_prefix="Promoted core module interface fallback reintroduction detected:",
    )

    core_module_interface_rules = {
        path: (
            "module;",
            "export module",
            "import std;",
        )
        for path in _CORE_MODULE_INTERFACE_DUAL_PATHS
    }
    _run_ordered_token_rules(
        core_module_interface_rules,
        error_prefix="Retained core module interface single-path regression detected:",
    )
    _run_forbidden_token_rules(
        _CORE_MODULE_INTERFACE_DUAL_PATHS,
        _PROMOTED_CORE_MODULE_INTERFACE_FORBIDDEN_TOKENS,
        error_prefix="Retained core module interface fallback reintroduction detected:",
    )

    core_module_implementation_rules = {
        path: (
            "module;",
            "import std;",
            "module bag.",
        )
        for path in _CORE_MODULE_IMPLEMENTATION_DUAL_PATHS
    }
    _run_ordered_token_rules(
        core_module_implementation_rules,
        error_prefix="Retained core module implementation single-path regression detected:",
    )
    _run_forbidden_token_rules(
        _CORE_MODULE_IMPLEMENTATION_DUAL_PATHS,
        ("#if !defined(WAVEBITS_MODULE_IMPL_WRAPPER)",) + _IMPORT_STD_SINGLE_PATH_WITH_INCLUDE_FALLBACK_FORBIDDEN_TOKENS,
        error_prefix="Core module implementation fallback reintroduction detected:",
    )

    audio_io_module_interface_rules = {
        path: (
            "module;",
            "export module audio_io.wav;",
            "import std;",
        )
        for path in _AUDIO_IO_MODULE_INTERFACE_DUAL_PATHS
    }
    _run_ordered_token_rules(
        audio_io_module_interface_rules,
        error_prefix="audio_io module interface single-path regression detected:",
    )
    _run_forbidden_token_rules(
        _AUDIO_IO_MODULE_INTERFACE_DUAL_PATHS,
        _PROMOTED_CORE_MODULE_INTERFACE_FORBIDDEN_TOKENS,
        error_prefix="audio_io module interface fallback reintroduction detected:",
    )

    audio_io_backend_bridge_rules = {
        path: (
            "module;",
            "#include <cstdint>",
            "#include <filesystem>",
            "#include <vector>",
            '#include "../../src/wav_io_backend.h"',
            "module audio_io.wav;",
            "import std;",
        )
        for path in _AUDIO_IO_BACKEND_BRIDGE_DUAL_PATHS
    }
    _run_ordered_token_rules(
        audio_io_backend_bridge_rules,
        error_prefix="Required audio_io backend-bridge single-path regression detected:",
    )
    _run_forbidden_token_rules(
        _AUDIO_IO_BACKEND_BRIDGE_DUAL_PATHS,
        _IMPORT_STD_SINGLE_PATH_FORBIDDEN_TOKENS,
        error_prefix="audio_io backend-bridge guard reintroduction detected:",
    )

    _run_ordered_token_rules(
        _BOUNDARY_HOST_DUAL_PATH_RULES,
        error_prefix="Boundary-host single-path regression detected:",
    )
    _run_forbidden_token_rules(
        tuple(_BOUNDARY_HOST_DUAL_PATH_RULES),
        _BOUNDARY_HOST_SINGLE_PATH_FORBIDDEN_TOKENS,
        error_prefix="Boundary-host guard reintroduction detected:",
    )


def run_host_import_std_policy_checks() -> None:
    run_phase11_import_std_policy_checks()
    run_phase12_import_std_policy_checks()
    run_phase13_import_std_policy_checks()
    run_phase14_import_std_policy_checks()
    run_phase21_import_std_policy_checks()
    run_phase22_import_std_policy_checks()
    run_required_import_std_baseline_coverage_checks()
    run_required_import_std_baseline_shape_checks()
