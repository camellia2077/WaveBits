from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


TOOLS_DIR = Path(__file__).resolve().parents[1]
if str(TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_DIR))

from repo_tooling.commands import cli as cli_command
from repo_tooling.errors import ToolError


class _Args:
    def __init__(self, *, action: str, version: str | None) -> None:
        self.action = action
        self.version = version


class CliVersionTests(unittest.TestCase):
    def test_bump_cli_version_updates_manifest_and_runs_cargo_update(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            cli_dir = Path(temp_dir)
            cargo_toml = cli_dir / "Cargo.toml"
            cargo_toml.write_text(
                '[package]\nname = "flipbits"\nversion = "0.2.4"\nedition = "2021"\n',
                encoding="utf-8",
            )

            with (
                patch.object(cli_command, "CLI_RUST_DIR", cli_dir),
                patch.object(cli_command, "run") as mock_run,
            ):
                cli_command.bump_cli_version("0.2.5")

            self.assertIn('version = "0.2.5"', cargo_toml.read_text(encoding="utf-8"))
            mock_run.assert_called_once_with(["cargo", "update", "-p", "flipbits"], cwd=cli_dir)

    def test_bump_cli_version_rejects_non_semver(self) -> None:
        with self.assertRaises(ToolError):
            cli_command.bump_cli_version("0.2")

    def test_cli_bump_version_requires_version(self) -> None:
        with self.assertRaises(ToolError):
            cli_command.cmd_cli(_Args(action="bump-version", version=None))

    def test_cli_build_rejects_version_argument(self) -> None:
        with self.assertRaises(ToolError):
            cli_command.cmd_cli(_Args(action="build", version="0.2.5"))


if __name__ == "__main__":
    unittest.main()
