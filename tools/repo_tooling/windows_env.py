from __future__ import annotations

import os
import subprocess
from dataclasses import dataclass
from pathlib import Path

from .errors import ToolError


@dataclass(frozen=True)
class MsvcEnvironment:
    installation_path: Path
    init_script: Path
    env: dict[str, str]


def load_msvc_environment() -> MsvcEnvironment:
    if os.name != "nt":
        raise ToolError("MSVC environment bootstrap is only supported on Windows hosts.")

    vswhere_path = _find_vswhere()
    installation_path = _query_visual_studio_installation(vswhere_path)
    init_script = _find_init_script(installation_path)
    env = _capture_initialized_environment(init_script)
    _validate_initialized_environment(env, init_script)
    return MsvcEnvironment(
        installation_path=installation_path,
        init_script=init_script,
        env=env,
    )


def _find_vswhere() -> Path:
    default_path = Path(r"C:\Program Files (x86)\Microsoft Visual Studio\Installer\vswhere.exe")
    if default_path.exists():
        return default_path
    raise ToolError(
        "Could not locate vswhere.exe. Install Visual Studio Build Tools / Visual Studio first."
    )


def _query_visual_studio_installation(vswhere_path: Path) -> Path:
    result = subprocess.run(
        [
            str(vswhere_path),
            "-latest",
            "-products",
            "*",
            "-requires",
            "Microsoft.VisualStudio.Component.VC.Tools.x86.x64",
            "-property",
            "installationPath",
        ],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )
    installation = result.stdout.strip()
    if result.returncode != 0 or not installation:
        raise ToolError(
            "Could not resolve a Visual Studio installation with VC tools via vswhere."
        )
    installation_path = Path(installation)
    if not installation_path.exists():
        raise ToolError(f"Resolved Visual Studio installation does not exist: {installation_path}")
    return installation_path


def _find_init_script(installation_path: Path) -> Path:
    candidates = [
        installation_path / "Common7" / "Tools" / "VsDevCmd.bat",
        installation_path / "VC" / "Auxiliary" / "Build" / "vcvars64.bat",
    ]
    for candidate in candidates:
        if candidate.exists():
            return candidate
    raise ToolError(
        "Could not locate VsDevCmd.bat or vcvars64.bat under the resolved Visual Studio installation."
    )


def _capture_initialized_environment(init_script: Path) -> dict[str, str]:
    command = _init_command(init_script)
    result = subprocess.run(
        command,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
        shell=True,
    )
    if result.returncode != 0:
        stderr = result.stderr.strip() or result.stdout.strip()
        raise ToolError(
            "Failed to initialize the Visual Studio build environment.\n"
            f"Script: {init_script}\n"
            f"output:\n{stderr}"
        )

    env: dict[str, str] = {}
    for line in result.stdout.splitlines():
        if "=" not in line or line.startswith("="):
            continue
        key, value = line.split("=", 1)
        env[key] = value
    return env


def _init_command(init_script: Path) -> str:
    script = str(init_script)
    if init_script.name.lower() == "vsdevcmd.bat":
        body = f'call "{script}" -no_logo -arch=x64 -host_arch=x64 >nul && set'
    else:
        body = f'call "{script}" >nul && set'
    return f'cmd.exe /d /c "{body}"'


def _validate_initialized_environment(env: dict[str, str], init_script: Path) -> None:
    required_keys = [
        "VCINSTALLDIR",
        "VCToolsInstallDir",
        "WindowsSdkDir",
    ]
    missing = [key for key in required_keys if not env.get(key)]
    if missing:
        joined = ", ".join(missing)
        raise ToolError(
            "Visual Studio environment initialization completed, but required variables are missing: "
            f"{joined}. Script: {init_script}"
        )
