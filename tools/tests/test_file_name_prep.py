from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path


TOOLS_DIR = Path(__file__).resolve().parents[1]
if str(TOOLS_DIR) not in sys.path:
    sys.path.insert(0, str(TOOLS_DIR))

from repo_tooling.file_name.analyze import build_candidate_review, classify_name_style
from repo_tooling.file_name.collect import parse_git_status_candidates
from repo_tooling.file_name.model import FileNamePrepResult
from repo_tooling.file_name.render import render_result


class FileNamePrepTests(unittest.TestCase):
    def test_parse_git_status_candidates_keeps_added_and_renamed_targets(self) -> None:
        status = "\n".join(
            [
                "?? tools/repo_tooling/file_name/prep.py",
                "A  docs/notes/file_name_workflow.md",
                "R  old_name.py -> new_name.py",
                "M  README.md",
                "D  obsolete.txt",
            ]
        )
        self.assertEqual(
            parse_git_status_candidates(status),
            [
                "tools/repo_tooling/file_name/prep.py",
                "docs/notes/file_name_workflow.md",
                "new_name.py",
            ],
        )

    def test_classify_name_style_distinguishes_common_shapes(self) -> None:
        self.assertEqual(classify_name_style("audio_runtime"), "snake_case")
        self.assertEqual(classify_name_style("audio-runtime"), "kebab-case")
        self.assertEqual(classify_name_style("AudioRuntime"), "other")

    def test_build_candidate_review_flags_generic_token(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            repo_root = Path(temp_dir)
            target_dir = repo_root / "libs" / "audio_io"
            target_dir.mkdir(parents=True)
            (target_dir / "audio_io_api.h").write_text("", encoding="utf-8")
            (target_dir / "audio_io_types.h").write_text("", encoding="utf-8")
            current = Path.cwd()
            try:
                # The analysis uses cwd-independent repository constants in production,
                # so this test only validates pure basename-level checks.
                review = build_candidate_review(
                    "libs/audio_io/audio_common.hpp",
                    suggested_reading=["`.agent/guides/file-name-styles.md`"],
                )
            finally:
                _ = current
            messages = [check.message for check in review.checks if check.kind == "banned_tokens"]
            self.assertEqual(len(messages), 1)
            self.assertIn("common", messages[0])

    def test_render_result_lists_required_sections(self) -> None:
        result = FileNamePrepResult(
            candidates=[],
            candidate_paths=[],
            message_path="temp/file-name/message.txt",
            source_description="git status --short (added + renamed files)",
        )
        rendered = render_result(result)
        self.assertIn("## Mechanizable Checks", rendered)
        self.assertIn("## Agent Judgment Needed", rendered)
        self.assertIn("## Candidate Files", rendered)
        self.assertIn("## Suggested Reading", rendered)


if __name__ == "__main__":
    unittest.main()
