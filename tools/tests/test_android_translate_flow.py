from __future__ import annotations

import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


TOOLS_DIR = Path(__file__).resolve().parents[1]
RUN_PY = TOOLS_DIR / "scripts" / "android" / "translate" / "run.py"


class AndroidTranslateFlowTests(unittest.TestCase):
    def test_compare_quiet_generates_review_markdown_with_xml_and_name(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            temp_root = Path(tmp_dir)
            res_dir = temp_root / "res"
            output_dir = temp_root / "out"
            self._write_fixture_resources(res_dir)

            completed = self._run_cli(
                "compare",
                "--res-dir",
                str(res_dir),
                "--output-dir",
                str(output_dir),
                "--quiet",
            )

            self.assertEqual(completed.returncode, 0, completed.stderr)
            self.assertEqual(completed.stdout.strip(), "")
            review_path = output_dir / "it" / "sample_text" / "sacred_machine.md"
            self.assertTrue(review_path.exists())
            review_text = review_path.read_text(encoding="utf-8")
            self.assertIn("DIR: values-it", review_text)
            self.assertIn("XML: values-it/audio_samples_sacred_machine_rite_of_maintenance.xml", review_text)
            self.assertIn("NAME: audio_sample_sacred_machine_themed_caliper_oil_rite", review_text)

    def test_replace_quiet_updates_target_string(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            temp_root = Path(tmp_dir)
            res_dir = temp_root / "res"
            replacements_json = temp_root / "replacements.json"
            self._write_fixture_resources(res_dir)
            replacements_json.write_text(
                json.dumps(
                    {
                        "dir": "values-it",
                        "items": [
                            {
                                "name": "audio_sample_sacred_machine_themed_caliper_oil_rite",
                                "find": "toccare",
                                "replace": "sfiorare",
                            }
                        ],
                    },
                    ensure_ascii=False,
                    indent=2,
                ),
                encoding="utf-8",
            )

            completed = self._run_cli(
                "replace",
                "--res-dir",
                str(res_dir),
                "--json",
                str(replacements_json),
                "--skip-smoke-check",
                "--quiet",
            )

            self.assertEqual(completed.returncode, 0, completed.stderr)
            self.assertEqual(completed.stdout.strip(), "")
            updated_xml = (
                res_dir / "values-it" / "audio_samples_sacred_machine_rite_of_maintenance.xml"
            ).read_text(encoding="utf-8")
            self.assertIn("sfiorare il primo ingranaggio", updated_xml)
            self.assertNotIn("toccare il primo ingranaggio", updated_xml)

    def test_replace_auto_fix_json_quiet_repairs_json_and_applies_change(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            temp_root = Path(tmp_dir)
            res_dir = temp_root / "res"
            replacements_json = temp_root / "replacements.json"
            self._write_fixture_resources(res_dir)
            replacements_json.write_text(
                (
                    "{\n"
                    '  "dir": "values-it",\n'
                    '  "items": [\n'
                    "    {\n"
                    '      "name": "config_theme_mode_subtitle",\n'
                    '      "find": "modalità di sistema",\n'
                    '      "replace": "modalità di \\"sistema\\"",\n'
                    '      "note": "broken "quote" sample"\n'
                    "    }\n"
                    "  ]\n"
                    "}\n"
                ),
                encoding="utf-8",
            )

            completed = self._run_cli(
                "replace",
                "--res-dir",
                str(res_dir),
                "--json",
                str(replacements_json),
                "--skip-smoke-check",
                "--auto-fix-json",
                "--quiet",
            )

            self.assertEqual(completed.returncode, 0, completed.stdout + completed.stderr)
            self.assertEqual(completed.stdout.strip(), "")
            repaired_json = replacements_json.read_text(encoding="utf-8")
            self.assertIn('\\"quote\\"', repaired_json)
            updated_xml = (res_dir / "values-it" / "strings_settings.xml").read_text(encoding="utf-8")
            self.assertIn('modalità di \\"sistema\\"', updated_xml)

    def test_compare_json_output_is_machine_readable(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            temp_root = Path(tmp_dir)
            res_dir = temp_root / "res"
            output_dir = temp_root / "out"
            self._write_fixture_resources(res_dir)

            completed = self._run_cli(
                "compare",
                "--res-dir",
                str(res_dir),
                "--output-dir",
                str(output_dir),
                "--json-output",
            )

            self.assertEqual(completed.returncode, 0, completed.stderr)
            payload = json.loads(completed.stdout)
            self.assertTrue(payload["ok"])
            self.assertEqual(payload["command"], "compare")
            self.assertEqual(payload["summary"]["english_review_files"], 2)
            self.assertEqual(payload["summary"]["localized_languages"], 1)
            self.assertEqual(payload["artifacts"]["output_dir"], str(output_dir))

    def test_replace_json_output_is_machine_readable(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            temp_root = Path(tmp_dir)
            res_dir = temp_root / "res"
            replacements_json = temp_root / "replacements.json"
            self._write_fixture_resources(res_dir)
            replacements_json.write_text(
                json.dumps(
                    {
                        "dir": "values-it",
                        "items": [
                            {
                                "name": "audio_sample_sacred_machine_themed_caliper_oil_rite",
                                "find": "toccare",
                                "replace": "sfiorare",
                            }
                        ],
                    },
                    ensure_ascii=False,
                    indent=2,
                ),
                encoding="utf-8",
            )

            completed = self._run_cli(
                "replace",
                "--res-dir",
                str(res_dir),
                "--json",
                str(replacements_json),
                "--skip-smoke-check",
                "--json-output",
            )

            self.assertEqual(completed.returncode, 0, completed.stderr)
            payload = json.loads(completed.stdout)
            self.assertTrue(payload["ok"])
            self.assertEqual(payload["command"], "replace")
            self.assertEqual(payload["summary"]["applied_replacements"], 1)
            self.assertEqual(payload["summary"]["validation_error_count"], 0)
            self.assertFalse(payload["summary"]["smoke_check_ran"])
            self.assertEqual(payload["artifacts"]["dir"], "values-it")
            self.assertEqual(payload["errors"], [])

    def test_replace_auto_normalizes_android_escapes_in_touched_xml(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            temp_root = Path(tmp_dir)
            res_dir = temp_root / "res"
            replacements_json = temp_root / "replacements.json"
            self._write_fixture_resources(res_dir)

            strings_it_path = res_dir / "values-it" / "strings_settings.xml"
            strings_it_path.write_text(
                (
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    "<resources>\n"
                    "    <string name=\"config_theme_mode_subtitle\">Scegli la modalità di sistema, chiara o scura.</string>\n"
                    "    <string name=\"config_custom_brand_theme_accent_label\">Colore d'accento</string>\n"
                    "</resources>\n"
                ),
                encoding="utf-8",
            )
            replacements_json.write_text(
                json.dumps(
                    {
                        "dir": "values-it",
                        "items": [
                            {
                                "name": "config_theme_mode_subtitle",
                                "find": "modalità di sistema",
                                "replace": "modalità selezionata",
                            }
                        ],
                    },
                    ensure_ascii=False,
                    indent=2,
                ),
                encoding="utf-8",
            )

            completed = self._run_cli(
                "replace",
                "--res-dir",
                str(res_dir),
                "--json",
                str(replacements_json),
                "--skip-smoke-check",
                "--quiet",
            )

            self.assertEqual(completed.returncode, 0, completed.stderr)
            updated_xml = strings_it_path.read_text(encoding="utf-8")
            self.assertIn("modalità selezionata", updated_xml)
            self.assertIn("Colore d\\'accento", updated_xml)

    def test_add_key_json_output_adds_english_baseline_only(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            temp_root = Path(tmp_dir)
            res_dir = temp_root / "res"
            values_dir = res_dir / "values"
            values_it_dir = res_dir / "values-it"
            values_dir.mkdir(parents=True, exist_ok=True)
            values_it_dir.mkdir(parents=True, exist_ok=True)
            xml_text = (
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                "<resources>\n"
                "</resources>\n"
            )
            (values_dir / "strings_audio.xml").write_text(xml_text, encoding="utf-8")
            (values_it_dir / "strings_audio.xml").write_text(xml_text, encoding="utf-8")

            completed = self._run_cli(
                "add-key",
                "--res-dir",
                str(res_dir),
                "--file",
                "strings_audio.xml",
                "--key",
                "audio_new_label",
                "--en",
                "New label",
                "--json-output",
            )

            self.assertEqual(completed.returncode, 0, completed.stderr)
            payload = json.loads(completed.stdout)
            self.assertTrue(payload["ok"])
            self.assertEqual(payload["command"], "add-key")
            self.assertEqual(payload["summary"]["touched_files"], 1)
            self.assertFalse(payload["summary"]["localized_fallback_used"])
            base_text = (values_dir / "strings_audio.xml").read_text(encoding="utf-8")
            it_text = (values_it_dir / "strings_audio.xml").read_text(encoding="utf-8")
            self.assertIn('<string name="audio_new_label">New label</string>', base_text)
            self.assertNotIn("audio_new_label", it_text)

    def test_key_alignment_json_output_accepts_res_and_output_dir(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            temp_root = Path(tmp_dir)
            res_dir = temp_root / "res"
            output_dir = temp_root / "reports"
            values_dir = res_dir / "values"
            values_it_dir = res_dir / "values-it"
            values_dir.mkdir(parents=True, exist_ok=True)
            values_it_dir.mkdir(parents=True, exist_ok=True)
            (values_dir / "strings_audio.xml").write_text(
                (
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    "<resources>\n"
                    "    <string name=\"audio_new_label\">New label</string>\n"
                    "</resources>\n"
                ),
                encoding="utf-8",
            )
            (values_it_dir / "strings_audio.xml").write_text(
                (
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    "<resources>\n"
                    "</resources>\n"
                ),
                encoding="utf-8",
            )

            completed = self._run_cli(
                "key-alignment",
                "--res-dir",
                str(res_dir),
                "--output-dir",
                str(output_dir),
                "--json-output",
            )

            self.assertEqual(completed.returncode, 2, completed.stderr)
            payload = json.loads(completed.stdout)
            self.assertFalse(payload["ok"])
            self.assertEqual(payload["command"], "key-alignment")
            self.assertEqual(payload["summary"]["alignment_issue_count"], 1)
            self.assertEqual(payload["artifacts"]["output_dir"], str(output_dir))
            self.assertTrue((output_dir / "it" / "it_translation_tasks.md").exists())

    def _run_cli(self, *args: str) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [sys.executable, str(RUN_PY), *args],
            cwd=TOOLS_DIR.parent,
            text=True,
            capture_output=True,
            check=False,
        )

    def _write_fixture_resources(self, res_dir: Path) -> None:
        values_dir = res_dir / "values"
        values_it_dir = res_dir / "values-it"
        values_dir.mkdir(parents=True, exist_ok=True)
        values_it_dir.mkdir(parents=True, exist_ok=True)

        (values_dir / "audio_samples_sacred_machine_rite_of_maintenance.xml").write_text(
            (
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                "<resources>\n"
                "    <string name=\"audio_sample_sacred_machine_themed_caliper_oil_rite\">The brass caliper touches the first gear after sacred oil is applied.</string>\n"
                "    <string name=\"audio_sample_sacred_machine_themed_bolt_sequence_litany\">Every bolt is counted before the chamber answers the rite.</string>\n"
                "    <string name=\"audio_sample_pro_ascii_caliper_oil_rite\">BRASS CALIPER TOUCHES FIRST GEAR</string>\n"
                "</resources>\n"
            ),
            encoding="utf-8",
        )
        (values_it_dir / "audio_samples_sacred_machine_rite_of_maintenance.xml").write_text(
            (
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                "<resources>\n"
                "    <string name=\"audio_sample_sacred_machine_themed_caliper_oil_rite\">Il calibro d'ottone può toccare il primo ingranaggio solo dopo l'olio sacro.</string>\n"
                "    <string name=\"audio_sample_sacred_machine_themed_bolt_sequence_litany\">Ogni bullone viene contato prima che la camera risponda al rito.</string>\n"
                "    <string name=\"audio_sample_pro_ascii_caliper_oil_rite\">BRASS CALIPER TOUCHES FIRST GEAR</string>\n"
                "</resources>\n"
            ),
            encoding="utf-8",
        )
        (values_dir / "strings_settings.xml").write_text(
            (
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                "<resources>\n"
                "    <string name=\"config_theme_mode_subtitle\">Choose system, light, or dark mode.</string>\n"
                "</resources>\n"
            ),
            encoding="utf-8",
        )
        (values_it_dir / "strings_settings.xml").write_text(
            (
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                "<resources>\n"
                "    <string name=\"config_theme_mode_subtitle\">Scegli la modalità di sistema, chiara o scura.</string>\n"
                "</resources>\n"
            ),
            encoding="utf-8",
        )


if __name__ == "__main__":
    unittest.main()
