# CLI Contract

## Purpose

This document defines the stable command-line contract for the Android translation tooling.

The goal is to make command behavior reliable for both:

- human operators
- future agent callers

It focuses on:

- exit codes
- output behavior
- `--quiet`
- the boundary between human-readable text output and machine-readable JSON output

## Principle

Text content and output format should be treated as separate concerns.

- Command logic should decide:
  - whether the step succeeded
  - how many items were processed
  - what errors occurred
- The CLI layer should decide:
  - whether to print human-readable text
  - whether to suppress routine logs with `--quiet`
  - whether a future machine-readable JSON format should be emitted

Current implementation supports both:

- `--quiet`
- `--json-output`

## Stable Exit Codes

### `compare`

- `0`: review markdown generation completed
- non-zero: unexpected runtime failure

### `mixed-language`

- `0`: command completed
- non-zero: unexpected runtime failure

Note:
- The current mixed-language command generates reports even when issues are found.
- It does not currently use issue-count-based non-zero exit codes.

### `key-alignment`

- `0`: no alignment issues found
- `2`: alignment issues found
- non-zero other than `2`: unexpected runtime failure

### `replace`

- `0`: all replacements applied successfully
- `1`: path/setup/preflight failure
- `2`: validation errors occurred
- `3`: replacements were applied but Android resource smoke check failed
- `4`: validation errors occurred and smoke check also failed

### `build-replacements`

- `0`: replacements JSON generated successfully
- `1`: input file missing or setup failure
- `2`: suggestion schema or conversion validation failure

### `add-key`

- `0`: English baseline key was added or already existed
- `1`: one or more target files could not be read or written
- `2`: English baseline resource file was not found

By default, `add-key` writes only `values/<file>`. It writes existing `values-*` files only when `--localized` is explicitly provided for deliberately shared text.

## Human-Readable Output

By default, commands may print human-oriented progress and summaries.

Examples:

- `compare`
  - output directory
  - generated file counts

- `replace`
  - applied replacement count
  - localized diff excerpts
  - smoke check progress
  - validation errors

- `key-alignment`
  - issue count
  - report directory

- `mixed-language`
  - issue count
  - report directory

These text lines are useful for manual runs, but they are not the long-term machine contract.

## `--quiet`

Supported commands:

- `all`
- `compare`
- `build-replacements`
- `mixed-language`
- `key-alignment`
- `replace`

`--quiet` means:

- suppress routine progress output
- suppress normal success summaries
- keep essential error output

For `replace`, `--quiet` suppresses:

- applied replacement count
- diff rendering
- success footer
- routine smoke check banner

For `compare`, `--quiet` suppresses:

- output directory banner
- generated file counts

For `mixed-language` and `key-alignment`, `--quiet` suppresses:

- issue-count summaries
- report directory summaries

## JSON Output Contract

Supported commands:

- `all`
- `compare`
- `mixed-language`
- `key-alignment`
- `replace`

`--json-output` does not change business logic.
It only changes presentation format.

For `replace`, the input schema is a single top-level object:

```json
{
  "dir": "values-ja",
  "items": [
    {
      "name": "string_name",
      "find": "current substring",
      "replace": "improved substring"
    }
  ]
}
```

Recommended shape:

```json
{
  "ok": true,
  "command": "replace",
  "exit_code": 0,
  "summary": {
    "applied_replacements": 2,
    "already_applied_count": 1,
    "skipped_unchanged_count": 0,
    "failed_not_found_count": 0,
    "failed_ambiguous_count": 0,
    "failed_validation_count": 0,
    "validation_error_count": 0,
    "smoke_check_ran": true,
    "smoke_check_ok": true
  },
  "artifacts": {
    "output_dir": null
  },
  "errors": []
}
```

### Command-specific JSON summaries

#### `compare`

```json
{
  "ok": true,
  "command": "compare",
  "exit_code": 0,
  "summary": {
    "english_review_files": 8,
    "localized_review_files": 91,
    "localized_languages": 10,
    "prompt_mode": "agent_json",
    "prompt_version": "v2"
  },
  "artifacts": {
    "output_dir": "C:/code/WaveBits/temp/ai_translation_reviews"
  },
  "errors": []
}
```

#### `replace`

```json
{
  "ok": true,
  "command": "replace",
  "exit_code": 0,
  "summary": {
    "applied_replacements": 2,
    "already_applied_count": 1,
    "skipped_unchanged_count": 0,
    "failed_not_found_count": 0,
    "failed_ambiguous_count": 0,
    "failed_validation_count": 0,
    "validation_error_count": 0,
    "smoke_check_ran": true,
    "smoke_check_ok": true
  },
  "artifacts": {},
  "errors": []
}
```

`run.py compare` also accepts `--prompt-mode`:

- `agent_json`: prompt asks the reviewer to return replacement JSON
- `manual_notes`: prompt asks the reviewer to return plain review notes

Generated review markdown now includes header metadata lines such as:

- `PROMPT_MODE`
- `PROMPT_VERSION`
- `GENERATED_AT`

These are part of the human-readable artifact, not the CLI JSON envelope, but agents may rely on them when deciding whether a review file is stale or suitable for direct JSON generation.

Generated `*.task.json` files may also include locale-specific persona metadata under `locale_profile`, including a stable profile id, mode, note, and rule fragments. This is part of the machine-readable agent contract for locale-specific writing modes such as `values-la`.

Current `*.task.json` shape:

```json
{
  "task_version": 2,
  "prompt_mode": "agent_json",
  "prompt_version": "v4",
  "generated_at": "2026-04-29T01:31:49Z",
  "language": "la",
  "language_tag": "LA",
  "locale_profile": {
    "id": "high_gothic_dog_latin",
    "mode": "stylized_dog_latin",
    "note": "High Gothic (`values-la`) is a stylized Dog Latin / pseudo-Latin locale.",
    "identity_rule": "This locale is High Gothic, not classical Latin and not a standard translation locale.",
    "app_text_rule": "For app UI text, keep labels usable and recognizable...",
    "sample_text_rule": "For sample prose, atmosphere outranks literal fidelity...",
    "key_alignment_rule": "When filling missing entries, write in the repository's High Gothic house style..."
  },
  "text_type": "sample_text",
  "prompt_text_type": "sample_text",
  "group": "ancient_dynasty",
  "prompt_ref": "la/_prompts/agent_json_sample_text.md",
  "entry_count": 10,
  "entries": [
    {
      "dir": "values-la",
      "xml": "values-la/audio_samples_ancient_dynasty_absolute_materialism.xml",
      "name": "audio_sample_ancient_dynasty_themed_molecular_law_unthreads_flesh",
      "sample_length": "SHORT",
      "context": "This file contains localized themed sample text for flash/ultra.",
      "en": "Molecular bonds severed; organic matter eradicated.",
      "localized": "Vincula molecularia soluta; carnis penitus eradicata."
    }
  ]
}
```

Stability notes:

- `task_version` is the top-level schema version for agent task files.
- `locale_profile` is the stable place for locale-specific writing persona data.
- Agents that skip Markdown should read `locale_profile` directly instead of inferring persona only from `prompt_ref`.

`run.py replace` accepts `--summary-out <path>`, which writes the same structured JSON result payload to disk for agent-job audit trails.
`run.py compare` and `run.py replace` also accept `--job-dir <path>`, which updates `<job-dir>/job_manifest.json` so a single job folder can point to:

- generated review markdown files
- the replacement input JSON
- the structured replace result JSON

`replace --json-output` also returns `status_counts`, which separates:

- `applied`
- `already_applied`
- `skipped_unchanged`
- `failed_not_found`
- `failed_ambiguous`
- `failed_validation`

#### `key-alignment`

```json
{
  "ok": false,
  "command": "key-alignment",
  "exit_code": 2,
  "summary": {
    "alignment_issue_count": 14
  },
  "artifacts": {
    "output_dir": "C:/code/WaveBits/temp/translation_key_alignment_reports"
  },
  "errors": []
}
```

#### `mixed-language`

```json
{
  "ok": true,
  "command": "mixed-language",
  "exit_code": 0,
  "summary": {
    "suspicious_issue_count": 3
  },
  "artifacts": {
    "output_dir": "C:/code/WaveBits/temp/mixed_language_reports"
  },
  "errors": []
}
```

## Guidance For Future Refactors

When extending this CLI, prefer:

1. core functions returning structured result objects
2. CLI rendering those results as either text or JSON
3. `--quiet` controlling only routine text noise
4. exit codes remaining stable even if output formatting evolves

Do not make agent-facing success detection depend on fragile free-form prose.

The durable contract should be:

- exit code first
- optional structured JSON second
- human-readable text only as a convenience layer
