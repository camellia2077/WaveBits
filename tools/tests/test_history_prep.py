from __future__ import annotations

import sys
import unittest
from pathlib import Path


TOOLS_DIR = Path(__file__).resolve().parents[1]
if str(TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_DIR))

from repo_tooling.constants import ROOT_DIR
from repo_tooling.errors import ToolError
from repo_tooling.history.collect import normalize_scope


class HistoryPrepTests(unittest.TestCase):
    def test_normalize_scope_keeps_repo_relative_scope(self) -> None:
        self.assertEqual(normalize_scope("libs/audio_io"), "libs/audio_io")
        self.assertEqual(normalize_scope(r"libs\audio_io"), "libs/audio_io")

    def test_normalize_scope_converts_repo_absolute_scope(self) -> None:
        self.assertEqual(normalize_scope(str(ROOT_DIR / "libs" / "audio_io")), "libs/audio_io")

    def test_normalize_scope_rejects_absolute_scope_outside_repo(self) -> None:
        outside = Path("C:/").resolve()
        with self.assertRaises(ToolError):
            normalize_scope(str(outside))


if __name__ == "__main__":
    unittest.main()
