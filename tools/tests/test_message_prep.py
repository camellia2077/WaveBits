from __future__ import annotations

import sys
import unittest
from pathlib import Path


TOOLS_DIR = Path(__file__).resolve().parents[1]
if str(TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_DIR))

from repo_tooling.message.infer import build_history_message_result, infer_message_type
from repo_tooling.message.parse import component_name_for_history, parse_history_file
from repo_tooling.message.render import render_result


class MessagePrepTests(unittest.TestCase):
    def test_component_name_infers_known_history_roots(self) -> None:
        self.assertEqual(component_name_for_history("docs/presentation/cli/v0.2/0.2.0.md"), "cli-presentation")
        self.assertEqual(component_name_for_history("docs/presentation/android/v0.3/0.3.0.md"), "android-presentation")
        self.assertEqual(component_name_for_history("docs/libs/v0.4/0.4.1.md"), "libs")

    def test_parse_history_file_reads_heading_and_sections(self) -> None:
        entry = parse_history_file("docs/presentation/cli/v0.2/0.2.0.md")
        self.assertEqual(entry.release_version, "v0.2.0")
        self.assertEqual(entry.component_name, "cli-presentation")
        self.assertGreaterEqual(len(entry.added), 1)
        self.assertGreaterEqual(len(entry.changed), 1)
        self.assertGreaterEqual(len(entry.fixed), 1)

    def test_build_history_message_result_prefers_history_content(self) -> None:
        result = build_history_message_result(["docs/presentation/cli/v0.2/0.2.0.md"])
        self.assertEqual(result.source_mode, "history")
        self.assertEqual(result.inferred_type, "feat")
        self.assertEqual(result.release_version, "v0.2.0")
        self.assertIn(("cli-presentation", "v0.2.0"), result.component_versions)

    def test_render_result_keeps_required_commit_sections(self) -> None:
        result = build_history_message_result(["docs/presentation/android/v0.3/0.3.0.md"])
        rendered = render_result(result)
        self.assertIn("[Summary]", rendered)
        self.assertIn("[Component Versions]", rendered)
        self.assertIn("[Added]", rendered)
        self.assertIn("[Changed & Refactored]", rendered)
        self.assertIn("[Fixed]", rendered)
        self.assertIn("[Verification]", rendered)
        self.assertIn("Release-Version: v0.3.0", rendered)


if __name__ == "__main__":
    unittest.main()
