from __future__ import annotations

import argparse

from commands.apply_translation_replacements import (
    DEFAULT_JSON_PATH,
    apply_translation_replacements,
)
from commands.add_translation_key import add_translation_key
from commands.fix_android_resource_escapes import run_fix_android_resource_escapes
from commands.check_mixed_language import run_mixed_language_check
from commands.check_translation_key_alignment import (
    DEFAULT_OUTPUT_DIRECTORY as DEFAULT_KEY_ALIGNMENT_OUTPUT_DIRECTORY,
    run_translation_key_alignment_check,
)
from commands.compare_translation_quality import generate_comparisons_for_res
from commands.build_replacements_from_suggestions import build_replacements_from_suggestions
from core.translation_cli_payloads import (
    compare_error_payload,
    compare_payload,
    emit_json_payload,
    fix_resource_escapes_payload,
    key_alignment_payload,
    mixed_language_payload,
    replace_payload,
    write_json_payload,
)
from core.translation_job_manifest import (
    build_compare_manifest_patch,
    build_replace_manifest_patch,
    update_job_manifest,
    normalize_path_string,
)
from core.translation_paths import DEFAULT_RES_DIRECTORY


def run_all_translation_reports() -> None:
    print("[translate] Generating translation quality reviews (de, es, ja, pt-BR, ru, uk, zh, zh-rTW)")
    generate_comparisons_for_res()
    print("[translate] Generating mixed-language reports")
    run_mixed_language_check()
    print("[translate] Generating translation key alignment reports")
    run_translation_key_alignment_check()


def run_all_translation_reports_quiet() -> None:
    generate_comparisons_for_res(quiet=True)
    run_mixed_language_check(quiet=True)
    run_translation_key_alignment_check(quiet=True)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Android translation tooling entrypoint.",
    )
    subparsers = parser.add_subparsers(dest="command")

    all_parser = subparsers.add_parser(
        "all",
        help="Generate all translation review reports.",
    )
    all_parser.add_argument(
        "--quiet",
        action="store_true",
        help="Suppress routine progress output.",
    )
    all_parser.add_argument(
        "--json-output",
        action="store_true",
        help="Emit machine-readable JSON instead of human-readable text.",
    )
    compare_parser = subparsers.add_parser(
        "compare",
        help="Generate translation quality review reports.",
    )
    compare_parser.add_argument(
        "--res-dir",
        default=str(DEFAULT_RES_DIRECTORY),
        help="Android res root. Defaults to apps/audio_android/app/src/main/res.",
    )
    compare_parser.add_argument(
        "--output-dir",
        default="",
        help="Override output directory for generated review markdown.",
    )
    compare_parser.add_argument(
        "--lang",
        default="",
        help="Limit localized review generation to one language folder such as de, ja, or zh-rTW.",
    )
    compare_parser.add_argument(
        "--text-type",
        default="",
        help="Limit review generation to one text type: app_text or sample_text.",
    )
    compare_parser.add_argument(
        "--group",
        default="",
        help="Limit review generation to one review group such as ancient_dynasty or other.",
    )
    compare_parser.add_argument(
        "--prompt-mode",
        default="agent_json",
        help="Prompt style for generated review files: agent_json or manual_notes.",
    )
    compare_parser.add_argument(
        "--no-clean",
        action="store_true",
        help="Do not delete the output directory before writing review files.",
    )
    compare_parser.add_argument(
        "--job-dir",
        default="",
        help="Optional agent job directory. When set, compare updates <job-dir>/job_manifest.json.",
    )
    compare_parser.add_argument(
        "--quiet",
        action="store_true",
        help="Suppress routine progress output.",
    )
    compare_parser.add_argument(
        "--json-output",
        action="store_true",
        help="Emit machine-readable JSON instead of human-readable text.",
    )
    mixed_parser = subparsers.add_parser(
        "mixed-language",
        help="Generate mixed-language reports.",
    )
    mixed_parser.add_argument(
        "--quiet",
        action="store_true",
        help="Suppress routine progress output.",
    )
    mixed_parser.add_argument(
        "--json-output",
        action="store_true",
        help="Emit machine-readable JSON instead of human-readable text.",
    )
    key_alignment_parser = subparsers.add_parser(
        "key-alignment",
        help="Check that localized translation keys are a strict subset of the English base keys.",
    )
    key_alignment_parser.add_argument(
        "--res-dir",
        default=str(DEFAULT_RES_DIRECTORY),
        help="Android res root. Defaults to apps/audio_android/app/src/main/res.",
    )
    key_alignment_parser.add_argument(
        "--output-dir",
        default="",
        help="Override output directory for generated key-alignment task reports.",
    )
    key_alignment_parser.add_argument(
        "--quiet",
        action="store_true",
        help="Suppress routine progress output.",
    )
    key_alignment_parser.add_argument(
        "--json-output",
        action="store_true",
        help="Emit machine-readable JSON instead of human-readable text.",
    )
    replace_parser = subparsers.add_parser(
        "replace",
        help="Apply replacements from replacements.json into Android sample string XML files.",
    )
    replace_parser.add_argument(
        "--res-dir",
        default=str(DEFAULT_RES_DIRECTORY),
        help="Android res root. Defaults to apps/audio_android/app/src/main/res.",
    )
    replace_parser.add_argument(
        "--json",
        default=str(DEFAULT_JSON_PATH),
        help="Path to the replacement JSON file.",
    )
    replace_parser.add_argument(
        "--skip-smoke-check",
        action="store_true",
        help="Skip the post-replacement Android resource smoke check (:app:mergeDebugResources).",
    )
    replace_parser.add_argument(
        "--auto-fix-json",
        action="store_true",
        help="Apply high-confidence JSON syntax repairs before running replace.",
    )
    replace_parser.add_argument(
        "--quiet",
        action="store_true",
        help="Suppress routine progress output.",
    )
    replace_parser.add_argument(
        "--json-output",
        action="store_true",
        help="Emit machine-readable JSON instead of human-readable text.",
    )
    replace_parser.add_argument(
        "--summary-out",
        default="",
        help="Optional path to write the structured replace result JSON artifact.",
    )
    replace_parser.add_argument(
        "--job-dir",
        default="",
        help="Optional agent job directory. When set, replace updates <job-dir>/job_manifest.json.",
    )
    build_replacements_parser = subparsers.add_parser(
        "build-replacements",
        help="Convert full-line suggestion JSON into minimal find/replace replacements JSON.",
    )
    build_replacements_parser.add_argument(
        "--res-dir",
        default=str(DEFAULT_RES_DIRECTORY),
        help="Android res root. Defaults to apps/audio_android/app/src/main/res.",
    )
    build_replacements_parser.add_argument(
        "--input",
        required=True,
        help="Path to suggestion JSON containing dir/items/name/xml/replace_full.",
    )
    build_replacements_parser.add_argument(
        "--output",
        required=True,
        help="Path to write the generated replacements JSON.",
    )
    build_replacements_parser.add_argument(
        "--json-output",
        action="store_true",
        help="Emit machine-readable JSON instead of human-readable text.",
    )
    fix_escapes_parser = subparsers.add_parser(
        "fix-resource-escapes",
        help="Normalize Android string resource escaping inside values*/ XML files.",
    )
    fix_escapes_parser.add_argument(
        "--res-dir",
        default=str(DEFAULT_RES_DIRECTORY),
        help="Android res root. Defaults to apps/audio_android/app/src/main/res.",
    )
    fix_escapes_parser.add_argument(
        "files",
        nargs="*",
        help="Optional explicit XML files to repair. Defaults to all values*/ XML files under --res-dir.",
    )
    fix_escapes_parser.add_argument(
        "--quiet",
        action="store_true",
        help="Suppress routine progress output.",
    )
    fix_escapes_parser.add_argument(
        "--json-output",
        action="store_true",
        help="Emit machine-readable JSON instead of human-readable text.",
    )
    add_key_parser = subparsers.add_parser(
        "add-key",
        help="Add a string key to the English baseline file.",
    )
    add_key_parser.add_argument(
        "--res-dir",
        default=str(DEFAULT_RES_DIRECTORY),
        help="Android res root. Defaults to apps/audio_android/app/src/main/res.",
    )
    add_key_parser.add_argument(
        "--file",
        required=True,
        help="Resource XML filename, such as strings_audio.xml.",
    )
    add_key_parser.add_argument(
        "--key",
        required=True,
        help="String resource key to add.",
    )
    add_key_parser.add_argument(
        "--en",
        required=True,
        help="English baseline value.",
    )
    add_key_parser.add_argument(
        "--localized",
        default=None,
        help="Optional explicit fallback value for existing values-* files. Use only for intentionally shared text.",
    )
    add_key_parser.add_argument(
        "--context",
        default=None,
        help="Optional CONTEXT comment inserted above the English baseline key.",
    )
    add_key_parser.add_argument(
        "--json-output",
        action="store_true",
        help="Emit machine-readable JSON instead of human-readable text.",
    )

    return parser.parse_args()

def run() -> int:
    args = parse_args()
    command = args.command or "all"
    quiet = getattr(args, "quiet", False)
    json_output = getattr(args, "json_output", False)

    if command == "all":
        compare_result = generate_comparisons_for_res(quiet=quiet or json_output, emit_text=not json_output)
        mixed_result = run_mixed_language_check(quiet=quiet or json_output, emit_text=not json_output)
        key_result = run_translation_key_alignment_check(quiet=quiet or json_output, emit_text=not json_output)
        if not json_output:
            return 0
        emit_json_payload(
            {
                "ok": True,
                "command": "all",
                "exit_code": 0,
                "summary": {
                    "steps": 3,
                },
                "artifacts": {},
                "errors": [],
                "steps": {
                    "compare": compare_payload(compare_result),
                    "mixed-language": mixed_language_payload(mixed_result),
                    "key-alignment": key_alignment_payload(key_result),
                },
            }
        )
        return 0
    if command == "compare":
        if not quiet and not json_output:
            print("[translate] Generating translation quality reviews (de, es, ja, pt-BR, ru, uk, zh, zh-rTW)")
        try:
            result = generate_comparisons_for_res(
                res_dir=args.res_dir,
                output_dir=args.output_dir or None,
                lang=args.lang or None,
                text_type=args.text_type or None,
                group=args.group or None,
                prompt_mode=args.prompt_mode or "agent_json",
                no_clean=args.no_clean,
                quiet=quiet or json_output,
                emit_text=not json_output,
            )
        except ValueError as exc:
            if json_output:
                emit_json_payload(compare_error_payload(output_dir=args.output_dir or "", error=str(exc)))
            else:
                print(str(exc))
            return 2
        if args.job_dir:
            manifest_path = update_job_manifest(args.job_dir, build_compare_manifest_patch(result))
        else:
            manifest_path = None
        payload = compare_payload(result)
        if manifest_path is not None:
            payload["artifacts"]["job_manifest"] = manifest_path
        if json_output:
            emit_json_payload(payload)
        return result.exit_code
    if command == "mixed-language":
        if not quiet and not json_output:
            print("[translate] Generating mixed-language reports")
        result = run_mixed_language_check(quiet=quiet or json_output, emit_text=not json_output)
        if json_output:
            emit_json_payload(mixed_language_payload(result))
        return result.exit_code
    if command == "key-alignment":
        if not quiet and not json_output:
            print("[translate] Generating translation key alignment reports")
        result = run_translation_key_alignment_check(
            res_dir=args.res_dir,
            output_dir=args.output_dir or DEFAULT_KEY_ALIGNMENT_OUTPUT_DIRECTORY,
            quiet=quiet or json_output,
            emit_text=not json_output,
        )
        if json_output:
            emit_json_payload(key_alignment_payload(result))
        return result.exit_code
    if command == "replace":
        result = apply_translation_replacements(
            res_dir=args.res_dir,
            json_path=args.json,
            run_smoke_check=not args.skip_smoke_check,
            auto_fix_json=args.auto_fix_json,
            quiet=quiet or json_output,
            emit_text=not json_output,
        )
        payload = replace_payload(
            result,
            json_path=args.json,
            summary_out=args.summary_out or None,
            normalize_path=normalize_path_string,
        )
        if args.summary_out:
            write_json_payload(args.summary_out, payload)
        if args.job_dir:
            manifest_path = update_job_manifest(
                args.job_dir,
                build_replace_manifest_patch(
                    json_path=args.json,
                    summary_out=args.summary_out or None,
                    payload=payload,
                ),
            )
            payload["artifacts"]["job_manifest"] = manifest_path
        if json_output:
            emit_json_payload(payload)
        return result.exit_code
    if command == "build-replacements":
        result = build_replacements_from_suggestions(
            input_path=args.input,
            output_path=args.output,
            res_dir=args.res_dir,
        )
        if json_output:
            emit_json_payload(
                {
                    "ok": result.exit_code == 0,
                    "command": "build-replacements",
                    "exit_code": result.exit_code,
                    "summary": {
                        "built_items": result.built_items,
                        "skipped_items": result.skipped_items,
                        "dir": result.dir_name,
                    },
                    "artifacts": {
                        "input_json": normalize_path_string(args.input),
                        "output_json": normalize_path_string(args.output) if result.output_path else None,
                    },
                    "errors": list(result.errors),
                }
            )
        elif result.exit_code == 0:
            print(f"Built replacements: {result.built_items} (skipped unchanged: {result.skipped_items})")
            print(f"Wrote: {result.output_path}")
        else:
            for error in result.errors:
                print(error)
        return result.exit_code
    if command == "fix-resource-escapes":
        result = run_fix_android_resource_escapes(
            res_dir=args.res_dir,
            files=args.files,
            quiet=quiet or json_output,
            emit_text=not json_output,
        )
        if json_output:
            emit_json_payload(fix_resource_escapes_payload(result))
        return result.exit_code
    if command == "add-key":
        result = add_translation_key(
            filename=args.file,
            key=args.key,
            english_value=args.en,
            localized_value=args.localized,
            context=args.context,
            res_dir=args.res_dir,
        )
        payload = {
            "ok": result.exit_code == 0,
            "command": "add-key",
            "exit_code": result.exit_code,
            "summary": {
                "touched_files": len(result.touched_files),
                "skipped_files": len(result.skipped_files),
                "localized_fallback_used": result.localized_fallback_used,
            },
            "artifacts": {
                "touched_files": [normalize_path_string(path) for path in result.touched_files],
                "skipped_files": [normalize_path_string(path) for path in result.skipped_files],
            },
            "errors": result.errors,
        }
        if json_output:
            emit_json_payload(payload)
        else:
            print(
                f"Added key `{args.key}` to {len(result.touched_files)} files "
                f"(skipped existing: {len(result.skipped_files)})."
            )
            if not result.localized_fallback_used:
                print("Localized files were not updated; run key-alignment to generate translation tasks.")
            else:
                print("Localized fallback was applied explicitly; verify that the shared text is intentional.")
            if result.touched_files:
                print("Touched:")
                for path in result.touched_files:
                    print(f"- {normalize_path_string(path)}")
            for error in result.errors:
                print(error)
        return result.exit_code

    raise ValueError(f"Unsupported command: {command}")


if __name__ == "__main__":
    raise SystemExit(run())
