from __future__ import annotations

import subprocess

from ...constants import DEFAULT_CXX_COMPILER, ROOT_DIR
from ...errors import ToolError
from .common import android_native_private_include_root, iter_android_private_headers


def run_android_private_header_self_contained_policy_checks() -> None:
    failures: list[str] = []
    include_root = android_native_private_include_root()

    for header_path in iter_android_private_headers():
        relative_include = header_path.relative_to(include_root).as_posix()
        probe_source = f'#include "{relative_include}"\nint main() {{ return 0; }}\n'

        try:
            result = subprocess.run(
                (
                    DEFAULT_CXX_COMPILER,
                    "-std=c++23",
                    "-x",
                    "c++",
                    "-fsyntax-only",
                    "-I",
                    str(include_root),
                    "-",
                ),
                input=probe_source,
                text=True,
                capture_output=True,
                check=False,
                cwd=ROOT_DIR,
            )
        except FileNotFoundError as exc:
            raise ToolError("Android private header self-contained check requires `clang++` on PATH.") from exc

        if result.returncode == 0:
            continue

        stderr_lines = [line.strip() for line in result.stderr.splitlines() if line.strip()]
        first_error = stderr_lines[0] if stderr_lines else "syntax check failed with no stderr output"
        failures.append(f"{header_path.relative_to(ROOT_DIR)} is not self-contained under C++23: {first_error}")

    if failures:
        joined = "\n".join(f"- {failure}" for failure in failures)
        raise ToolError("Android private header self-contained regression detected:\n" f"{joined}")
