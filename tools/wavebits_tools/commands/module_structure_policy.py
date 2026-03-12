from __future__ import annotations

from pathlib import Path

from ..constants import ROOT_DIR
from ..errors import ToolError


_PHASE7_IMPORT_RULES: dict[Path, tuple[str, ...]] = {
    ROOT_DIR / "libs" / "audio_core" / "src" / "common" / "version.cpp": (
        "module bag.common.version;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "flash" / "codec.cpp": (
        "module bag.flash.codec;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "fsk" / "fsk_codec.cpp": (
        "module bag.fsk.codec;",
        "import bag.flash.phy_clean;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pipeline" / "pipeline.cpp": (
        "module bag.pipeline;",
        "import bag.transport.facade;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "pro" / "codec.cpp": (
        "module bag.pro.codec;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "transport" / "compat" / "frame_codec.cpp": (
        "module bag.transport.compat.frame_codec;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "transport" / "transport.cpp": (
        "module bag.transport.facade;",
        "import bag.flash.phy_clean;",
        "import bag.pro.phy_clean;",
        "import bag.ultra.phy_clean;",
    ),
    ROOT_DIR / "libs" / "audio_core" / "src" / "ultra" / "codec.cpp": (
        "module bag.ultra.codec;",
    ),
}


def run_phase7_import_policy_checks() -> None:
    failures: list[str] = []
    for path, required_tokens in sorted(_PHASE7_IMPORT_RULES.items()):
        content = path.read_text(encoding="utf-8")
        missing_tokens = [token for token in required_tokens if token not in content]
        if missing_tokens:
            missing = ", ".join(missing_tokens)
            failures.append(f"{path.relative_to(ROOT_DIR)} missing: {missing}")

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError(
            "Phase 7 import policy regression detected in audited source files:\n"
            f"{joined}"
        )


def run_module_structure_policy_checks() -> None:
    run_phase7_import_policy_checks()
