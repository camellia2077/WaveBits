from __future__ import annotations

from pathlib import Path

from ..constants import ROOT_DIR
from ..errors import ToolError


class _BoundaryRule:
    def __init__(
        self,
        *,
        required: tuple[str, ...] = (),
        forbidden: tuple[str, ...] = (),
    ) -> None:
        self.required = required
        self.forbidden = forbidden


_CONSUMER_SOURCE_RULES: dict[Path, _BoundaryRule] = {
    ROOT_DIR / "apps" / "audio_android" / "app" / "src" / "main" / "cpp" / "jni_bridge.cpp": _BoundaryRule(
        required=(
            '#include "bag_api.h"',
        ),
        forbidden=(
            '#include "bag/',
            '#include "wav_io.h"',
            "import bag.",
            "import audio_io.wav;",
        ),
    ),
    ROOT_DIR / "apps" / "audio_cli" / "rust" / "cmake" / "CMakeLists.txt": _BoundaryRule(
        required=(
            "cargo",
            "Cargo.toml",
            "FLIPBITS_CMAKE_BUILD_DIR",
        ),
        forbidden=(
            "bag_api",
            "audio_io",
            "bag_core",
        ),
    ),
    ROOT_DIR
    / "apps"
    / "audio_android"
    / "app"
    / "src"
    / "main"
    / "cpp"
    / "CMakeLists.txt": _BoundaryRule(
        required=(
            "${FLIPBITS_ROOT}/libs/audio_api/include",
            "${FLIPBITS_ROOT}/libs/audio_io/include",
            "bag_api",
        ),
        forbidden=(
            "${FLIPBITS_ROOT}/libs/audio_core/include",
            "target_link_libraries(audio_android_jni\n    PRIVATE\n        bag_core",
        ),
    ),
}

_TEST_SOURCE_RULES: dict[Path, _BoundaryRule] = {
    ROOT_DIR / "libs" / "audio_api" / "tests" / "api_tests.cpp": _BoundaryRule(
        required=(
            '#include "bag_api.h"',
        ),
        forbidden=(
            '#include "bag/',
            "import bag.",
            '#include "wav_io.h"',
        ),
    ),
    ROOT_DIR / "Test" / "artifact" / "artifact_tests.cpp": _BoundaryRule(
        required=(
            '#include "bag_api.h"',
            '#include "wav_io.h"',
        ),
        forbidden=(
            '#include "bag/',
            "import bag.",
        ),
    ),
}

_MODULE_TEST_FORBIDDEN_TOKENS: tuple[str, ...] = (
    '#include "bag/',
    "#include <",
)


def _run_boundary_rules(
    rules: dict[Path, _BoundaryRule],
    *,
    error_prefix: str,
) -> None:
    failures: list[str] = []
    for path, rule in sorted(rules.items()):
        content = path.read_text(encoding="utf-8")

        missing_required = [token for token in rule.required if token not in content]
        if missing_required:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} missing required tokens: "
                + ", ".join(missing_required)
            )

        present_forbidden = [token for token in rule.forbidden if token in content]
        if present_forbidden:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} contains forbidden tokens: "
                + ", ".join(present_forbidden)
            )

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError(f"{error_prefix}\n{joined}")


def run_consumer_boundary_policy_checks() -> None:
    _run_boundary_rules(
        _CONSUMER_SOURCE_RULES,
        error_prefix="Consumer boundary regression detected:",
    )


def run_boundary_first_tests_policy_checks() -> None:
    failures: list[str] = []

    for path, rule in sorted(_TEST_SOURCE_RULES.items()):
        content = path.read_text(encoding="utf-8")

        missing_required = [token for token in rule.required if token not in content]
        if missing_required:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} missing required tokens: "
                + ", ".join(missing_required)
            )

        present_forbidden = [token for token in rule.forbidden if token in content]
        if present_forbidden:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} contains forbidden tokens: "
                + ", ".join(present_forbidden)
            )

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError(
            "Boundary-first test regression detected:\n"
            f"{joined}"
        )


def run_module_first_tests_policy_checks() -> None:
    failures: list[str] = []

    modules_dir = ROOT_DIR / "Test" / "modules"
    for path in sorted(modules_dir.glob("*.cpp")):
        content = path.read_text(encoding="utf-8")
        present_forbidden = [
            token for token in _MODULE_TEST_FORBIDDEN_TOKENS if token in content
        ]
        if present_forbidden:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} regressed from module-first imports: "
                + ", ".join(present_forbidden)
            )

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError(
            "Module-first test regression detected:\n"
            f"{joined}"
        )


def run_boundary_policy_checks() -> None:
    run_consumer_boundary_policy_checks()
    run_boundary_first_tests_policy_checks()
    run_module_first_tests_policy_checks()
