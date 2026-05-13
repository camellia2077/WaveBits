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
from commands.dump_xml_text_md import generate_xml_text_dump
from commands.build_replacements_from_suggestions import build_replacements_from_suggestions
from commands.suggest_translation_terms import (
    build_term_suggestion_payload,
    print_term_suggestion_report,
    suggest_translation_terms,
)
from commands.translation_lint_and_autofix import (
    filter_new_lint_issues,
    load_lint_baseline,
    run_translation_autofix,
    run_translation_lint,
    save_lint_baseline,
)
from prompts.translation_review_prompts import get_supported_prompt_modes
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
from core.translation_paths import (
    DEFAULT_RES_DIRECTORY,
    DEFAULT_TRANSLATION_KEY_ALIGNMENT_DIRECTORY,
    DEFAULT_TRANSLATION_LINT_BASELINE_FILE,
    DEFAULT_TRANSLATION_REVIEWS_DIRECTORY,
    DEFAULT_TRANSLATION_XML_DUMPS_DIRECTORY,
    TEXT_TYPES,
    get_review_groups_for_text_type,
)
from core.translation_resources import AndroidStringResourceRepository


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
        default=str(DEFAULT_TRANSLATION_REVIEWS_DIRECTORY),
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
        choices=("", *TEXT_TYPES),
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
        choices=get_supported_prompt_modes(),
        help="Prompt style for generated review files: agent_json or manual_notes.",
    )
    compare_parser.add_argument(
        "--no-clean",
        action="store_true",
        help="Do not delete the output directory before writing review files.",
    )
    compare_parser.add_argument(
        "--job-dir",
        default=str(DEFAULT_TRANSLATION_REVIEWS_DIRECTORY),
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
    dump_parser = subparsers.add_parser(
        "dump-xml-md",
        help="Dump XML string texts into minimal .md files for text inspection.",
    )
    dump_parser.add_argument(
        "--res-dir",
        default=str(DEFAULT_RES_DIRECTORY),
        help="Android res root. Defaults to apps/audio_android/app/src/main/res.",
    )
    dump_parser.add_argument(
        "--output-dir",
        default=str(DEFAULT_TRANSLATION_XML_DUMPS_DIRECTORY),
        help="Override output directory for generated dump markdown.",
    )
    dump_parser.add_argument(
        "--lang",
        default="",
        help="Limit dump generation to one language folder such as ko, ja, or zh-rTW.",
    )
    dump_parser.add_argument(
        "--text-type",
        default="",
        choices=("", *TEXT_TYPES),
        help="Limit dump generation to one text type: app_text or sample_text.",
    )
    dump_parser.add_argument(
        "--group",
        default="",
        help="Limit dump generation to one review group such as exquisite_fall or strings_audio.",
    )
    dump_parser.add_argument(
        "--with-en",
        action="store_true",
        help="Include EN baseline text line for each key in output files.",
    )
    dump_parser.add_argument(
        "--no-clean",
        action="store_true",
        help="Do not delete the output directory before writing dump files.",
    )
    dump_parser.add_argument(
        "--quiet",
        action="store_true",
        help="Suppress routine progress output.",
    )
    list_text_types_parser = subparsers.add_parser(
        "list-text-types",
        help="List supported text types.",
    )
    list_text_types_parser.add_argument(
        "--quiet",
        action="store_true",
        help="Print values only.",
    )
    list_groups_parser = subparsers.add_parser(
        "list-groups",
        help="List supported groups for a text type.",
    )
    list_groups_parser.add_argument(
        "--text-type",
        default="sample_text",
        choices=TEXT_TYPES,
        help="Target text type: app_text or sample_text.",
    )
    list_groups_parser.add_argument(
        "--quiet",
        action="store_true",
        help="Print values only.",
    )
    list_langs_parser = subparsers.add_parser(
        "list-langs",
        help="List discovered localized language folders under values-*.",
    )
    list_langs_parser.add_argument(
        "--res-dir",
        default=str(DEFAULT_RES_DIRECTORY),
        help="Android res root. Defaults to apps/audio_android/app/src/main/res.",
    )
    list_langs_parser.add_argument(
        "--quiet",
        action="store_true",
        help="Print values only.",
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
        default=str(DEFAULT_TRANSLATION_KEY_ALIGNMENT_DIRECTORY),
        help="Override output directory for generated key-alignment task reports.",
    )
    key_alignment_parser.add_argument(
        "--lang",
        default="",
        help="Optional language folder suffix such as it, ja, or zh-rTW.",
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
    key_alignment_parser.add_argument(
        "--fail-on-stale",
        action="store_true",
        help="Return non-zero when localized-only stale keys/files are detected.",
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
    term_suggestions_parser = subparsers.add_parser(
        "term-suggestions",
        help="Inspect one English term across localized XML and suggest current per-language term candidates.",
    )
    term_suggestions_parser.add_argument(
        "--res-dir",
        default=str(DEFAULT_RES_DIRECTORY),
        help="Android res root. Defaults to apps/audio_android/app/src/main/res.",
    )
    term_suggestions_parser.add_argument(
        "--term",
        required=True,
        help="English source term to inspect, such as Standard or payload.",
    )
    term_suggestions_parser.add_argument(
        "--lang",
        default="",
        help="Optional language folder suffix such as de, ja, or zh-rTW. Defaults to all languages.",
    )
    term_suggestions_parser.add_argument(
        "--text-type",
        default="",
        choices=("", *TEXT_TYPES),
        help="Optional text type scope: app_text or sample_text.",
    )
    term_suggestions_parser.add_argument(
        "--group",
        default="",
        help="Optional review group scope such as strings_settings or exquisite_fall.",
    )
    term_suggestions_parser.add_argument(
        "--whole-word",
        action="store_true",
        help="Match the term as a whole word rather than a raw substring.",
    )
    term_suggestions_parser.add_argument(
        "--case-sensitive",
        action="store_true",
        help="Use case-sensitive matching.",
    )
    term_suggestions_parser.add_argument(
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
    lint_parser = subparsers.add_parser(
        "lint",
        help="Run deterministic translation lint checks on values-* XML files.",
    )
    lint_parser.add_argument(
        "--res-dir",
        default=str(DEFAULT_RES_DIRECTORY),
        help="Android res root. Defaults to apps/audio_android/app/src/main/res.",
    )
    lint_parser.add_argument(
        "--lang",
        default="",
        help="Optional language folder suffix such as fr, ko, or zh-rTW.",
    )
    lint_parser.add_argument(
        "--json-output",
        action="store_true",
        help="Emit machine-readable JSON instead of human-readable text.",
    )
    lint_parser.add_argument(
        "--baseline-file",
        default=str(DEFAULT_TRANSLATION_LINT_BASELINE_FILE),
        help="Optional lint baseline JSON path used by --fail-on-new and --write-baseline.",
    )
    lint_parser.add_argument(
        "--write-baseline",
        action="store_true",
        help="Write/update baseline file from current lint result, then exit 0.",
    )
    lint_parser.add_argument(
        "--fail-on-new",
        action="store_true",
        help="Fail only when issues are not present in baseline file.",
    )
    autofix_parser = subparsers.add_parser(
        "autofix",
        help="Apply deterministic low-risk translation autofixes on values-* XML files.",
    )
    autofix_parser.add_argument(
        "--res-dir",
        default=str(DEFAULT_RES_DIRECTORY),
        help="Android res root. Defaults to apps/audio_android/app/src/main/res.",
    )
    autofix_parser.add_argument(
        "--lang",
        default="",
        help="Optional language folder suffix such as fr, ko, or zh-rTW.",
    )
    autofix_parser.add_argument(
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
                output_dir=args.output_dir,
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
    if command == "dump-xml-md":
        result = generate_xml_text_dump(
            res_dir=args.res_dir,
            output_dir=args.output_dir,
            lang=args.lang or None,
            text_type=args.text_type or None,
            group=args.group or None,
            with_en=args.with_en,
            no_clean=args.no_clean,
            quiet=quiet,
            emit_text=not quiet,
        )
        if result.error:
            print(str(result.error))
        return result.exit_code
    if command == "list-text-types":
        if quiet:
            for value in TEXT_TYPES:
                print(value)
        else:
            print("Supported text types:")
            for value in TEXT_TYPES:
                print(f"- {value}")
        return 0
    if command == "list-groups":
        groups = get_review_groups_for_text_type(args.text_type)
        if quiet:
            for value in groups:
                print(value)
        else:
            print(f"Supported groups for {args.text_type}:")
            for value in groups:
                print(f"- {value}")
        return 0
    if command == "list-langs":
        repository = AndroidStringResourceRepository(args.res_dir)
        langs = [lang_code for lang_code, _path in repository.iter_localized_directories()]
        if quiet:
            for value in langs:
                print(value)
        else:
            print("Discovered localized language folders:")
            for value in langs:
                print(f"- {value}")
        return 0
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
            lang=args.lang or None,
            fail_on_stale=args.fail_on_stale,
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
    if command == "term-suggestions":
        result = suggest_translation_terms(
            term=args.term,
            res_dir=args.res_dir,
            lang=args.lang or None,
            text_type=args.text_type or None,
            group=args.group or None,
            whole_word=args.whole_word,
            case_sensitive=args.case_sensitive,
        )
        if json_output:
            emit_json_payload(build_term_suggestion_payload(result))
        else:
            print_term_suggestion_report(result)
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
    if command == "lint":
        result = run_translation_lint(res_dir=args.res_dir, lang=args.lang or None)
        effective_issues = list(result.issues)
        baseline_path = args.baseline_file or ""
        new_issues_count = None
        if args.write_baseline:
            if not baseline_path:
                print("--write-baseline requires --baseline-file")
                return 2
            save_lint_baseline(baseline_file=baseline_path, issues=result.issues, res_dir=args.res_dir)
            payload = {
                "ok": True,
                "command": "lint",
                "exit_code": 0,
                "summary": {
                    "checked_files": result.checked_files,
                    "issues": len(result.issues),
                    "errors": sum(1 for issue in result.issues if issue.level == "error"),
                    "warnings": sum(1 for issue in result.issues if issue.level == "warn"),
                    "baseline_written": True,
                    "baseline_file": normalize_path_string(baseline_path),
                },
                "artifacts": {
                    "baseline_file": normalize_path_string(baseline_path),
                },
                "errors": [],
                "issues": [],
            }
            if json_output:
                emit_json_payload(payload)
            else:
                print(f"Lint checked files: {result.checked_files}")
                print(f"Baseline written: {normalize_path_string(baseline_path)}")
            return 0

        if args.fail_on_new:
            if not baseline_path:
                print("--fail-on-new requires --baseline-file")
                return 2
            baseline_fingerprints = load_lint_baseline(baseline_file=baseline_path)
            effective_issues = filter_new_lint_issues(
                issues=result.issues,
                baseline_fingerprints=baseline_fingerprints,
                res_dir=args.res_dir,
            )
            new_issues_count = len(effective_issues)

        effective_exit_code = 2 if any(issue.level == "error" for issue in effective_issues) else 0
        payload = {
            "ok": effective_exit_code == 0,
            "command": "lint",
            "exit_code": effective_exit_code,
            "summary": {
                "checked_files": result.checked_files,
                "issues": len(effective_issues),
                "errors": sum(1 for issue in effective_issues if issue.level == "error"),
                "warnings": sum(1 for issue in effective_issues if issue.level == "warn"),
                "fail_on_new": bool(args.fail_on_new),
                "baseline_file": normalize_path_string(baseline_path) if baseline_path else None,
                "new_issues": new_issues_count,
            },
            "artifacts": {},
            "errors": [],
            "issues": [
                {
                    "file": normalize_path_string(issue.file),
                    "key": issue.key,
                    "level": issue.level,
                    "rule": issue.rule,
                    "message": issue.message,
                }
                for issue in effective_issues
            ],
        }
        if json_output:
            emit_json_payload(payload)
        else:
            print(f"Lint checked files: {result.checked_files}")
            print(
                f"Issues: {payload['summary']['issues']} "
                f"(errors={payload['summary']['errors']}, warnings={payload['summary']['warnings']})"
            )
            if args.fail_on_new and baseline_path:
                print(f"Baseline: {normalize_path_string(baseline_path)}")
            for issue in effective_issues:
                print(
                    f"- [{issue.level}] {normalize_path_string(issue.file)}::{issue.key} "
                    f"{issue.rule} - {issue.message}"
                )
        return effective_exit_code
    if command == "autofix":
        result = run_translation_autofix(res_dir=args.res_dir, lang=args.lang or None)
        payload = {
            "ok": True,
            "command": "autofix",
            "exit_code": 0,
            "summary": {
                "changed_files": len(result.changed_files),
                "replacement_passes": result.total_replacements,
            },
            "artifacts": {
                "changed_files": [normalize_path_string(path) for path in result.changed_files],
            },
            "errors": [],
        }
        if json_output:
            emit_json_payload(payload)
        else:
            print(f"Autofix changed files: {len(result.changed_files)}")
            for path in result.changed_files:
                print(f"- {normalize_path_string(path)}")
        return 0

    raise ValueError(f"Unsupported command: {command}")


if __name__ == "__main__":
    raise SystemExit(run())
