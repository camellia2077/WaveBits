from __future__ import annotations

from pathlib import Path

from ...constants import ROOT_DIR
from ...errors import ToolError


_PHASE17_BOUNDARY_RULES: dict[Path, tuple[str, ...]] = {
    ROOT_DIR / "libs" / "audio_api" / "CMakeLists.txt": (
        "CXX_MODULE_STD ON",
    ),
    ROOT_DIR / "libs" / "audio_api" / "src" / "bag_api.cpp": (
        "import std;",
        "import bag.transport.facade;",
        '#include "bag_api_impl.inc"',
    ),
}


def run_boundary_hosts_policy_checks() -> None:
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
