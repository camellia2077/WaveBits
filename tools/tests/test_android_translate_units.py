from __future__ import annotations

import tempfile
import unittest
from pathlib import Path
import sys


TRANSLATE_DIR = Path(__file__).resolve().parents[1] / "scripts" / "android" / "translate"
if str(TRANSLATE_DIR) not in sys.path:
    sys.path.insert(0, str(TRANSLATE_DIR))

from commands.apply_translation_replacements import apply_replacement_in_string
from commands.add_translation_key import add_translation_key
from core.android_string_text import (
    decode_android_string_resource_text,
    encode_android_string_resource_text,
    find_high_risk_android_string_resource_patterns,
    normalize_android_string_resource_text,
)
from core.mixed_language_detection import (
    check_cjk_language_for_latin_chunks,
    check_non_cjk_language_for_cjk_chunks,
    describe_detection_strategy,
    should_skip_mixed_language_detection,
)
from commands.fix_android_resource_escapes import run_fix_android_resource_escapes
from commands.check_translation_key_alignment import TranslationKeyAlignmentChecker
from commands.build_replacements_from_suggestions import build_replacements_from_suggestions
from core.translation_resources import AndroidStringResourceRepository
from core.replacement_entries import load_replacement_entries
from core.replacement_json_preflight import load_replacement_json_with_preflight
from core.translation_cli_payloads import compare_error_payload, compare_payload, replace_payload
from core.translation_job_manifest import build_compare_manifest_patch, build_replace_manifest_patch, update_job_manifest
from core.translation_paths import get_faction_from_filename, get_review_groups_for_text_type
from core.xml_string_replacement import load_localized_directory_index, resolve_string_name_path


class ReplacementJsonPreflightTests(unittest.TestCase):
    def test_preflight_reports_available_auto_fix_without_mutating_file(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            json_path = Path(tmp_dir) / "replacements.json"
            original_text = (
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
            )
            json_path.write_text(original_text, encoding="utf-8")

            result = load_replacement_json_with_preflight(json_path, auto_fix=False)

            self.assertFalse(result.ok)
            self.assertTrue(result.changed)
            self.assertIn("--auto-fix-json", result.error or "")
            self.assertEqual(json_path.read_text(encoding="utf-8"), original_text)

    def test_preflight_auto_fix_rewrites_file(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            json_path = Path(tmp_dir) / "replacements.json"
            json_path.write_text(
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

            result = load_replacement_json_with_preflight(json_path, auto_fix=True)

            self.assertTrue(result.ok)
            self.assertTrue(result.changed)
            self.assertIn('\\"quote\\"', json_path.read_text(encoding="utf-8"))


class AddTranslationKeyTests(unittest.TestCase):
    def test_add_translation_key_updates_only_english_baseline_by_default(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            res_dir = Path(tmp_dir) / "res"
            values_dir = res_dir / "values"
            values_fr_dir = res_dir / "values-fr"
            values_dir.mkdir(parents=True, exist_ok=True)
            values_fr_dir.mkdir(parents=True, exist_ok=True)
            base_xml = values_dir / "strings_audio.xml"
            fr_xml = values_fr_dir / "strings_audio.xml"
            xml_text = (
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                "<resources>\n"
                "</resources>\n"
            )
            base_xml.write_text(xml_text, encoding="utf-8")
            fr_xml.write_text(xml_text, encoding="utf-8")

            result = add_translation_key(
                filename="strings_audio.xml",
                key="sample_new_key",
                english_value='Audio "sample" & status',
                context="Shown in the audio toolbar.",
                res_dir=res_dir,
            )

            self.assertEqual(result.exit_code, 0)
            self.assertEqual(len(result.touched_files), 1)
            self.assertEqual(len(result.skipped_files), 0)
            self.assertFalse(result.localized_fallback_used)
            base_text = base_xml.read_text(encoding="utf-8")
            fr_text = fr_xml.read_text(encoding="utf-8")
            self.assertIn("CONTEXT: Shown in the audio toolbar.", base_text)
            self.assertIn(
                '<string name="sample_new_key">Audio \\"sample\\" &amp; status</string>',
                base_text,
            )
            self.assertNotIn("sample_new_key", fr_text)

            second_result = add_translation_key(
                filename="strings_audio.xml",
                key="sample_new_key",
                english_value="Ignored",
                res_dir=res_dir,
            )

            self.assertEqual(second_result.exit_code, 0)
            self.assertEqual(len(second_result.touched_files), 0)
            self.assertEqual(len(second_result.skipped_files), 1)

    def test_add_translation_key_uses_localized_fallback_only_when_explicit(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            res_dir = Path(tmp_dir) / "res"
            values_dir = res_dir / "values"
            values_fr_dir = res_dir / "values-fr"
            values_dir.mkdir(parents=True, exist_ok=True)
            values_fr_dir.mkdir(parents=True, exist_ok=True)
            base_xml = values_dir / "strings_audio.xml"
            fr_xml = values_fr_dir / "strings_audio.xml"
            xml_text = (
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                "<resources>\n"
                "</resources>\n"
            )
            base_xml.write_text(xml_text, encoding="utf-8")
            fr_xml.write_text(xml_text, encoding="utf-8")

            result = add_translation_key(
                filename="strings_audio.xml",
                key="shared_protocol_key",
                english_value="FlipBits",
                localized_value="FlipBits",
                res_dir=res_dir,
            )

            self.assertEqual(result.exit_code, 0)
            self.assertEqual(len(result.touched_files), 2)
            self.assertTrue(result.localized_fallback_used)
            self.assertIn(
                '<string name="shared_protocol_key">FlipBits</string>',
                base_xml.read_text(encoding="utf-8"),
            )
            self.assertIn(
                '<string name="shared_protocol_key">FlipBits</string>',
                fr_xml.read_text(encoding="utf-8"),
            )

    def test_add_translation_key_reports_missing_base_file(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            result = add_translation_key(
                filename="strings_missing.xml",
                key="sample_new_key",
                english_value="Sample",
                res_dir=Path(tmp_dir) / "res",
            )

            self.assertEqual(result.exit_code, 2)
            self.assertTrue(any("Base resource file not found" in error for error in result.errors))


class ReplacementEntrySchemaTests(unittest.TestCase):
    def test_replacement_entries_load_dir_and_items(self) -> None:
        batch = load_replacement_entries(
            '{'
            '"dir":"values-it",'
            '"items":[{"name":"sample_key","find":"old","replace":"new"}]'
            '}'
        )

        self.assertEqual(batch.dir_name, "values-it")
        self.assertEqual(len(batch.items), 1)
        self.assertEqual(batch.items[0].name, "sample_key")

    def test_replacement_entries_require_dir(self) -> None:
        with self.assertRaisesRegex(ValueError, "missing fields: dir"):
            load_replacement_entries(
                '{"items":[{"name":"sample_key","find":"old","replace":"new"}]}'
            )

    def test_replacement_entries_require_items(self) -> None:
        with self.assertRaisesRegex(ValueError, "missing fields: items"):
            load_replacement_entries('{"dir":"values-it"}')

    def test_replacement_entries_require_name_find_replace(self) -> None:
        with self.assertRaisesRegex(ValueError, "missing fields: replace"):
            load_replacement_entries(
                '{"dir":"values-it","items":[{"name":"sample_key","find":"old"}]}'
            )


class AndroidStringTextTests(unittest.TestCase):
    def test_android_string_round_trip_escapes_apostrophes_quotes_and_prefixes(self) -> None:
        literal_text = "@alert says \"it's ready\""

        encoded = encode_android_string_resource_text(literal_text)
        decoded = decode_android_string_resource_text(encoded)

        self.assertEqual(encoded, '\\@alert says \\"it\\\'s ready\\"')
        self.assertEqual(decoded, literal_text)

    def test_high_risk_patterns_detect_raw_apostrophe_and_invalid_unicode_escape(self) -> None:
        risks = find_high_risk_android_string_resource_patterns("d'aiuto \\u12G4")

        self.assertTrue(any("raw ASCII apostrophe" in risk for risk in risks))
        self.assertTrue(any("invalid \\u escape sequence" in risk for risk in risks))

    def test_normalize_android_string_resource_text_escapes_raw_apostrophes(self) -> None:
        normalized = normalize_android_string_resource_text("Необов'язково")

        self.assertEqual(normalized, "Необов\\'язково")


class MixedLanguageDetectionTests(unittest.TestCase):
    def test_cjk_strategy_flags_english_chunks(self) -> None:
        suspicious = check_cjk_language_for_latin_chunks("검은 강철과 stolen floor 의 의식")

        self.assertIn("stolen floor", suspicious)

    def test_non_cjk_strategy_flags_cjk_chunks(self) -> None:
        suspicious = check_non_cjk_language_for_cjk_chunks("La fiamma custodisce 終焉 del rito.")

        self.assertEqual(suspicious, ["終焉"])

    def test_non_cjk_strategy_no_longer_flags_english_phrase_overlap(self) -> None:
        suspicious = check_non_cjk_language_for_cjk_chunks("La fiamma custodisce the final rite.")

        self.assertEqual(suspicious, [])

    def test_describe_detection_strategy_uses_cjk_vs_non_cjk_modes(self) -> None:
        self.assertEqual(describe_detection_strategy("ja"), (True, "CJK Check (仅查拉丁脚本残留)"))
        self.assertEqual(describe_detection_strategy("fr"), (False, "Non-CJK Check (仅查中日韩残留)"))

    def test_values_la_skips_regular_mixed_language_detection(self) -> None:
        self.assertTrue(should_skip_mixed_language_detection("la"))
        self.assertEqual(
            describe_detection_strategy("la"),
            (False, "Style-English Skip (仅保留 Pro ASCII 检查)"),
        )


class ApplyReplacementInStringTests(unittest.TestCase):
    def test_apply_replacement_in_string_updates_unique_match(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            xml_path = Path(tmp_dir) / "values-it.xml"
            original_xml = (
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                "<resources>\n"
                "    <string name=\"sample_key\">Il calibro può toccare il primo ingranaggio.</string>\n"
                "</resources>\n"
            )
            xml_path.write_text(original_xml, encoding="utf-8")

            attempt = apply_replacement_in_string(
                xml_path,
                string_name="sample_key",
                find_text="toccare",
                replace_text="sfiorare",
                xml_text=original_xml,
            )

            self.assertEqual(attempt.status, "applied")
            self.assertEqual(attempt.original_text, "Il calibro può toccare il primo ingranaggio.")
            self.assertEqual(attempt.updated_text, "Il calibro può sfiorare il primo ingranaggio.")

    def test_apply_replacement_in_string_rejects_ambiguous_find(self) -> None:
        xml_path = Path("C:/tmp/dummy.xml")
        xml_text = (
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            "<resources>\n"
            "    <string name=\"sample_key\">toccare il rito, poi toccare il sigillo.</string>\n"
            "</resources>\n"
        )

        attempt = apply_replacement_in_string(
            xml_path,
            string_name="sample_key",
            find_text="toccare",
            replace_text="sfiorare",
            xml_text=xml_text,
        )

        self.assertEqual(attempt.status, "ambiguous")
        self.assertIn("matched multiple times", attempt.error or "")

    def test_apply_replacement_in_string_marks_already_applied(self) -> None:
        xml_path = Path("C:/tmp/dummy.xml")
        xml_text = (
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            "<resources>\n"
            "    <string name=\"sample_key\">Il calibro può sfiorare il primo ingranaggio.</string>\n"
            "</resources>\n"
        )

        attempt = apply_replacement_in_string(
            xml_path,
            string_name="sample_key",
            find_text="toccare",
            replace_text="sfiorare",
            xml_text=xml_text,
        )

        self.assertEqual(attempt.status, "already_applied")


class LocalizedDirectoryIndexTests(unittest.TestCase):
    def test_load_localized_directory_index_reports_missing_dir(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            localized_dir, name_index, xml_text_cache, errors = load_localized_directory_index(
                Path(tmp_dir),
                "values-it",
            )

            self.assertIsNone(localized_dir)
            self.assertEqual(name_index, {})
            self.assertEqual(xml_text_cache, {})
            self.assertTrue(any("localized values directory not found" in error for error in errors))

    def test_resolve_string_name_path_requires_unique_match(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            res_dir = Path(tmp_dir) / "res"
            values_it_dir = res_dir / "values-it"
            values_it_dir.mkdir(parents=True, exist_ok=True)
            xml_a = values_it_dir / "strings_a.xml"
            xml_b = values_it_dir / "strings_b.xml"
            xml_body = (
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                "<resources>\n"
                "    <string name=\"shared_key\">text</string>\n"
                "</resources>\n"
            )
            xml_a.write_text(xml_body, encoding="utf-8")
            xml_b.write_text(xml_body, encoding="utf-8")

            _, name_index, _, errors = load_localized_directory_index(res_dir, "values-it")

            self.assertEqual(errors, [])
            xml_path, resolve_errors = resolve_string_name_path(
                name_index,
                "shared_key",
                dir_name="values-it",
            )

            self.assertIsNone(xml_path)
            self.assertTrue(any("matched multiple xml files" in error for error in resolve_errors))

    def test_resolve_string_name_path_reports_missing_name(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            res_dir = Path(tmp_dir) / "res"
            values_it_dir = res_dir / "values-it"
            values_it_dir.mkdir(parents=True, exist_ok=True)
            (values_it_dir / "strings_a.xml").write_text(
                (
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    "<resources>\n"
                    "    <string name=\"existing_key\">text</string>\n"
                    "</resources>\n"
                ),
                encoding="utf-8",
            )

            _, name_index, _, errors = load_localized_directory_index(res_dir, "values-it")

            self.assertEqual(errors, [])
            xml_path, resolve_errors = resolve_string_name_path(
                name_index,
                "missing_key",
                dir_name="values-it",
            )

            self.assertIsNone(xml_path)
            self.assertTrue(any("string name not found" in error for error in resolve_errors))


class TranslationKeyAlignmentTests(unittest.TestCase):
    def test_shared_ascii_baseline_file_does_not_create_translation_tasks(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            res_dir = root / "res"
            output_dir = root / "reports"
            values_dir = res_dir / "values"
            values_ko_dir = res_dir / "values-ko"
            values_dir.mkdir(parents=True, exist_ok=True)
            values_ko_dir.mkdir(parents=True, exist_ok=True)

            (values_dir / "audio_samples_pro_ascii_shared.xml").write_text(
                (
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    "<resources>\n"
                    "    <string name=\"audio_sample_example_ascii_line\">ASCII ONLY SAMPLE</string>\n"
                    "</resources>\n"
                ),
                encoding="utf-8",
            )

            checker = TranslationKeyAlignmentChecker(res_dir=res_dir, output_dir=output_dir)
            result = checker.run(quiet=True, emit_text=False)

            self.assertEqual(result.exit_code, 0)
            self.assertEqual(result.alignment_issue_count, 0)
            ok_report = (output_dir / "translation_tasks_ok.md").read_text(encoding="utf-8")
            self.assertIn("OK: No missing or extra localized translation keys found.", ok_report)

    def test_missing_localized_file_reports_file_level_issue(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            res_dir = root / "res"
            output_dir = root / "reports"
            values_dir = res_dir / "values"
            values_pl_dir = res_dir / "values-pl"
            values_dir.mkdir(parents=True, exist_ok=True)
            values_pl_dir.mkdir(parents=True, exist_ok=True)

            (values_dir / "audio_samples_example.xml").write_text(
                (
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    "<resources>\n"
                    "    <string name=\"audio_sample_example_themed_one\">One</string>\n"
                    "    <string name=\"audio_sample_example_themed_two\">Two</string>\n"
                    "</resources>\n"
                ),
                encoding="utf-8",
            )

            checker = TranslationKeyAlignmentChecker(res_dir=res_dir, output_dir=output_dir)
            result = checker.run(quiet=True, emit_text=False)

            self.assertEqual(result.exit_code, 2)
            report_text = (output_dir / "pl" / "pl_translation_tasks.md").read_text(encoding="utf-8")
            self.assertIn("# PL Translation Tasks", report_text)
            self.assertIn("PROMPT: Use this report to repair missing or misaligned [PL] Android string resources.", report_text)
            self.assertIn("DIR: values-pl", report_text)
            self.assertIn("ISSUE: localized file is missing for English base counterpart", report_text)
            self.assertIn("MISSING_KEY_COUNT: 2", report_text)
            self.assertIn("EXAMPLE_KEYS: audio_sample_example_themed_one, audio_sample_example_themed_two", report_text)
            self.assertNotIn("KEY: audio_sample_example_themed_one\nISSUE: missing localized translation for English base key", report_text)

    def test_missing_localized_key_reports_context_when_available(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            res_dir = root / "res"
            output_dir = root / "reports"
            values_dir = res_dir / "values"
            values_ko_dir = res_dir / "values-ko"
            values_dir.mkdir(parents=True, exist_ok=True)
            values_ko_dir.mkdir(parents=True, exist_ok=True)

            (values_dir / "strings_settings.xml").write_text(
                (
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    "<resources>\n"
                    "    <!-- CONTEXT: Accessibility label for the custom dual-tone preview. -->\n"
                    "    <string name=\"brand_theme_custom_accessibility\">Custom dual-tone theme preview.</string>\n"
                    "</resources>\n"
                ),
                encoding="utf-8",
            )
            (values_ko_dir / "strings_settings.xml").write_text(
                (
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    "<resources>\n"
                    "</resources>\n"
                ),
                encoding="utf-8",
            )

            checker = TranslationKeyAlignmentChecker(res_dir=res_dir, output_dir=output_dir)
            result = checker.run(quiet=True, emit_text=False)

            self.assertEqual(result.exit_code, 2)
            report_text = (output_dir / "ko" / "ko_translation_tasks.md").read_text(encoding="utf-8")
            self.assertIn("# KO Translation Tasks", report_text)
            self.assertIn("DIR: values-ko", report_text)
            self.assertIn("CONTEXT: Accessibility label for the custom dual-tone preview.", report_text)
            self.assertIn("EN: Custom dual-tone theme preview.", report_text)


class TranslationResourceContextTests(unittest.TestCase):
    def test_extract_strings_from_xml_reads_context_comments(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            xml_path = Path(tmp_dir) / "strings_settings.xml"
            xml_path.write_text(
                (
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    "<resources>\n"
                    "    <!-- CONTEXT: Group heading for custom presets. -->\n"
                    "    <string name=\"config_dual_tone_group_custom\">Custom</string>\n"
                    "    <string name=\"config_title\">Settings</string>\n"
                    "</resources>\n"
                ),
                encoding="utf-8",
            )

            repository = AndroidStringResourceRepository(Path(tmp_dir))
            parsed = repository.extract_strings_from_xml(xml_path)

            self.assertEqual(parsed.strings["config_dual_tone_group_custom"], "Custom")
            self.assertEqual(parsed.contexts["config_dual_tone_group_custom"], "Group heading for custom presets.")
            self.assertEqual(parsed.contexts["config_title"], None)


class TranslationGroupingTests(unittest.TestCase):
    def test_app_text_filename_maps_to_own_review_group(self) -> None:
        self.assertEqual(get_faction_from_filename("strings_settings.xml"), "strings_settings")
        self.assertEqual(
            get_review_groups_for_text_type("app_text"),
            (
                "strings_about",
                "strings_audio",
                "strings_common",
                "strings_saved",
                "strings_settings",
                "strings_validation",
                "other",
            ),
        )


class FixAndroidResourceEscapesTests(unittest.TestCase):
    def test_fix_android_resource_escapes_normalizes_string_nodes_in_place(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            res_dir = Path(tmp_dir) / "res"
            values_it_dir = res_dir / "values-it"
            values_it_dir.mkdir(parents=True, exist_ok=True)
            xml_path = values_it_dir / "strings_settings.xml"
            xml_path.write_text(
                (
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    "<resources>\n"
                    "    <string name=\"config_custom_brand_theme_accent_label\">Colore d'accento</string>\n"
                    "    <string name=\"config_custom_brand_theme_name_label\">Nome del preset</string>\n"
                    "</resources>\n"
                ),
                encoding="utf-8",
            )

            result = run_fix_android_resource_escapes(
                res_dir=res_dir,
                files=[str(xml_path)],
                quiet=True,
                emit_text=False,
            )

            self.assertEqual(result.files_checked, 1)
            self.assertEqual(result.files_updated, 1)
            self.assertEqual(result.strings_updated, 1)
            updated_text = xml_path.read_text(encoding="utf-8")
            self.assertIn("Colore d\\'accento", updated_text)
            self.assertIn("Nome del preset", updated_text)


class TranslationCliPayloadTests(unittest.TestCase):
    def test_compare_error_payload_keeps_empty_review_lists(self) -> None:
        payload = compare_error_payload(output_dir="temp/reviews", error="bad filter")

        self.assertFalse(payload["ok"])
        self.assertEqual(payload["command"], "compare")
        self.assertEqual(payload["summary"]["english_review_paths"], [])
        self.assertEqual(payload["summary"]["localized_review_paths"], [])
        self.assertEqual(payload["summary"]["prompt_doc_paths"], [])
        self.assertEqual(payload["summary"]["task_json_paths"], [])

    def test_compare_payload_includes_prompt_doc_paths(self) -> None:
        result = type(
            "CompareResult",
            (),
            {
                "exit_code": 0,
                "english_review_files": 1,
                "localized_review_files": 1,
                "localized_languages": 1,
                "english_review_paths": ("temp/reviews/en.md",),
                "localized_review_paths": ("temp/reviews/de.md",),
                "prompt_doc_paths": (
                    "temp/reviews/en/_prompts/agent_json_app_text.md",
                    "temp/reviews/de/_prompts/agent_json_sample_text.md",
                ),
                "task_json_paths": ("temp/reviews/de/sample_text/ancient_dynasty.task.json",),
                "prompt_mode": "agent_json",
                "prompt_version": "v3",
                "output_dir": "temp/reviews",
                "error": None,
            },
        )()

        payload = compare_payload(result)

        self.assertEqual(
            payload["summary"]["prompt_doc_paths"],
            [
                "temp/reviews/en/_prompts/agent_json_app_text.md",
                "temp/reviews/de/_prompts/agent_json_sample_text.md",
            ],
        )
        self.assertEqual(
            payload["summary"]["task_json_paths"],
            ["temp/reviews/de/sample_text/ancient_dynasty.task.json"],
        )

    def test_replace_payload_normalizes_paths_when_callback_is_provided(self) -> None:
        result = type(
            "ReplaceResult",
            (),
            {
                "exit_code": 0,
                "applied_replacements": 2,
                "already_applied_count": 1,
                "skipped_unchanged_count": 0,
                "failed_not_found_count": 0,
                "failed_ambiguous_count": 0,
                "failed_validation_count": 0,
                "validation_error_count": 0,
                "smoke_check_ran": True,
                "smoke_check_ok": True,
                "auto_fix_applied": False,
                "auto_fix_summary": None,
                "dir_name": "values-de",
                "errors": (),
            },
        )()

        payload = replace_payload(
            result,
            json_path="temp/in.json",
            summary_out="temp/out.json",
            normalize_path=lambda value: f"ABS::{value}",
        )

        self.assertEqual(payload["artifacts"]["input_json"], "ABS::temp/in.json")
        self.assertEqual(payload["artifacts"]["summary_out"], "ABS::temp/out.json")
        self.assertEqual(payload["summary"]["status_counts"]["already_applied"], 1)


class TranslationJobManifestTests(unittest.TestCase):
    def test_update_job_manifest_merges_compare_and_replace_sections(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            job_dir = Path(tmp_dir) / "job"

            compare_result = type(
                "CompareResult",
                (),
                {
                    "output_dir": job_dir / "reviews",
                    "prompt_mode": "agent_json",
                    "prompt_version": "v2",
                    "english_review_paths": (str(job_dir / "reviews" / "en.md"),),
                    "localized_review_paths": (str(job_dir / "reviews" / "de.md"),),
                    "prompt_doc_paths": (
                        str(job_dir / "reviews" / "en" / "_prompts" / "agent_json_app_text.md"),
                        str(job_dir / "reviews" / "de" / "_prompts" / "agent_json_sample_text.md"),
                    ),
                    "task_json_paths": (
                        str(job_dir / "reviews" / "de" / "sample_text" / "ancient_dynasty.task.json"),
                    ),
                },
            )()
            manifest_path = update_job_manifest(job_dir, build_compare_manifest_patch(compare_result))

            replace_patch = build_replace_manifest_patch(
                json_path=str(job_dir / "replacements.json"),
                summary_out=str(job_dir / "replace_result.json"),
                payload={"ok": True, "command": "replace"},
            )
            update_job_manifest(job_dir, replace_patch)

            manifest_text = Path(manifest_path).read_text(encoding="utf-8")
            self.assertIn('"compare"', manifest_text)
            self.assertIn('"replace"', manifest_text)
            self.assertIn("replace_result.json", manifest_text)
            self.assertIn("de", manifest_text)
            self.assertIn("agent_json_sample_text.md", manifest_text)
            self.assertIn("ancient_dynasty.task.json", manifest_text)
            self.assertIn('"manifest_version": 1', manifest_text)


class BuildReplacementsFromSuggestionsTests(unittest.TestCase):
    def test_build_replacements_from_suggestions_writes_minimal_find_replace(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_dir:
            root = Path(tmp_dir)
            res_dir = root / "res"
            values_es_dir = res_dir / "values-es"
            values_es_dir.mkdir(parents=True, exist_ok=True)
            (values_es_dir / "sample.xml").write_text(
                (
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    "<resources>\n"
                    "    <string name=\"sample_key\">La niebla cubre la torre.</string>\n"
                    "</resources>\n"
                ),
                encoding="utf-8",
            )
            input_path = root / "suggestions.json"
            input_path.write_text(
                (
                    "{\n"
                    '  "dir": "values-es",\n'
                    '  "items": [\n'
                    '    {"name": "sample_key", "xml": "values-es/sample.xml", "replace_full": "La niebla envuelve la torre."}\n'
                    "  ]\n"
                    "}\n"
                ),
                encoding="utf-8",
            )
            output_path = root / "replacements.json"

            result = build_replacements_from_suggestions(
                input_path=input_path,
                output_path=output_path,
                res_dir=res_dir,
            )

            self.assertEqual(result.exit_code, 0)
            self.assertEqual(result.built_items, 1)
            output_text = output_path.read_text(encoding="utf-8")
            self.assertIn('"find": "cubre"', output_text)
            self.assertIn('"replace": "envuelve"', output_text)


if __name__ == "__main__":
    unittest.main()
