from __future__ import annotations

from pathlib import Path

from ..constants import ROOT_DIR
from ..errors import ToolError


class _Rule:
    def __init__(
        self,
        *,
        required: tuple[str, ...] = (),
        forbidden: tuple[str, ...] = (),
    ) -> None:
        self.required = required
        self.forbidden = forbidden


_AUDIO_IO_RULES: dict[Path, _Rule] = {
    ROOT_DIR / "libs" / "audio_io" / "include" / "wav_io.h": _Rule(
        required=(
            "struct WavPcm16",
            "std::vector<std::int16_t>",
            "std::filesystem::path",
        ),
        forbidden=(
            "#include <sndfile.h>",
            "import std;",
        ),
    ),
    ROOT_DIR / "libs" / "audio_io" / "modules" / "audio_io" / "wav.cppm": _Rule(
        required=(
            "export module audio_io.wav;",
            "std::vector<std::int16_t>",
        ),
        forbidden=(
            "wav_io_shared.h",
            "wav_io_detail.h",
            "wav_io_backend.h",
            "#include <sndfile.h>",
        ),
    ),
    ROOT_DIR / "libs" / "audio_io" / "modules" / "audio_io" / "wav_impl.cpp": _Rule(
        required=(
            "module audio_io.wav;",
            '#include "../../src/wav_io_backend.h"',
            "WriteMonoPcm16WavBackend",
            "ReadMonoPcm16WavBackend",
        ),
        forbidden=(
            "wav_io_shared.h",
            "wav_io_detail.h",
            "#include <sndfile.h>",
        ),
    ),
    ROOT_DIR / "libs" / "audio_io" / "src" / "wav_io.cpp": _Rule(
        required=(
            '#include "wav_io.h"',
            '#include "wav_io_backend.h"',
            "WriteMonoPcm16WavBackend",
            "ReadMonoPcm16WavBackend",
        ),
        forbidden=(
            "wav_io_shared.h",
            "wav_io_detail.h",
            "#include <sndfile.h>",
        ),
    ),
    ROOT_DIR / "libs" / "audio_io" / "src" / "wav_io_backend.h": _Rule(
        required=(
            "struct WavIoReadResult",
            "std::vector<std::int16_t>",
            "std::filesystem::path",
        ),
        forbidden=(
            "#include <sndfile.h>",
            "wav_io_shared.h",
            "wav_io_detail.h",
        ),
    ),
    ROOT_DIR / "libs" / "audio_io" / "src" / "wav_io_backend.cpp": _Rule(
        required=(
            '#include "wav_io_backend.h"',
            "#include <sndfile.h>",
            "WriteMonoPcm16WavBackend",
            "ReadMonoPcm16WavBackend",
        ),
        forbidden=(
            '#include "wav_io.h"',
            "import std;",
            "wav_io_shared.h",
            "wav_io_detail.h",
        ),
    ),
}

_RETIRED_SHARED_IMPLEMENTATION_HEADERS: tuple[Path, ...] = (
    ROOT_DIR / "libs" / "audio_io" / "src" / "wav_io_shared.h",
    ROOT_DIR / "libs" / "audio_io" / "src" / "wav_io_detail.h",
)

_SNDFILE_INCLUDE_TOKEN = "#include <sndfile.h>"
_SNDFILE_ALLOWED_OWNER = ROOT_DIR / "libs" / "audio_io" / "src" / "wav_io_backend.cpp"


def _iter_audio_io_code_files() -> list[Path]:
    code_files: list[Path] = []
    audio_io_root = ROOT_DIR / "libs" / "audio_io"
    for pattern in ("*.h", "*.hpp", "*.cpp", "*.cppm"):
        code_files.extend(sorted(audio_io_root.rglob(pattern)))
    return code_files


def run_audio_io_boundary_policy_checks() -> None:
    failures: list[str] = []

    for path, rule in sorted(_AUDIO_IO_RULES.items()):
        content = path.read_text(encoding="utf-8")
        missing_tokens = [token for token in rule.required if token not in content]
        if missing_tokens:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} missing required tokens: "
                + ", ".join(missing_tokens)
            )

        present_forbidden = [token for token in rule.forbidden if token in content]
        if present_forbidden:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} contains forbidden tokens: "
                + ", ".join(present_forbidden)
            )

    for path in _RETIRED_SHARED_IMPLEMENTATION_HEADERS:
        if path.exists():
            failures.append(
                f"{path.relative_to(ROOT_DIR)} should stay retired in the current audio_io boundary model"
            )

    for path in _iter_audio_io_code_files():
        content = path.read_text(encoding="utf-8")
        if _SNDFILE_INCLUDE_TOKEN in content and path != _SNDFILE_ALLOWED_OWNER:
            failures.append(
                f"{path.relative_to(ROOT_DIR)} should not include sndfile directly"
            )

    wav_impl_path = ROOT_DIR / "libs" / "audio_io" / "modules" / "audio_io" / "wav_impl.cpp"
    wav_impl_content = wav_impl_path.read_text(encoding="utf-8")
    backend_include_index = wav_impl_content.find('#include "../../src/wav_io_backend.h"')
    module_decl_index = wav_impl_content.find("module audio_io.wav;")
    if backend_include_index == -1 or module_decl_index == -1:
        failures.append(
            "libs/audio_io/modules/audio_io/wav_impl.cpp must keep both the backend include "
            "and the module declaration so the front-end/backend split stays explicit"
        )
    elif backend_include_index > module_decl_index:
        failures.append(
            "libs/audio_io/modules/audio_io/wav_impl.cpp must include "
            "../../src/wav_io_backend.h before module audio_io.wav; to avoid attaching "
            "backend declarations to the named module"
        )

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError(
            "audio_io boundary regression detected:\n"
            f"{joined}"
        )
