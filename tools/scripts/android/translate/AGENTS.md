# Android Translate Agent Flow

## Scope

This document is the agent-facing workflow for the Android translation tooling.

Human web-chat workflow lives separately in:

- [docs/sop.md](/C:/code/WaveBits/tools/scripts/android/translate/docs/sop.md)

## Goals

- keep agent jobs scoped to one language, one text type, and one group when possible
- reduce prompt duplication with shared prompt docs
- prefer machine-readable artifacts over markdown parsing when available
- keep replace operations safe, local, and auditable

## New Android XML Key Flow

When adding a new Android XML string key, start from the Android wrapper:

```powershell
python tools/run.py android strings-add --file strings_audio.xml --key sample_key --en "Sample text"
```

Default behavior:

- write only the English `values/` baseline
- generate `temp/translation_key_alignment_reports/`
- do not copy English into `values-*`

Then repair each locale from the generated key-alignment task report. Use the locale profile metadata in the report or generated task JSON, especially for:

- `values-la`: stylized Dog Latin / High Gothic, not classical Latin
- `values-uk`: Ukrainian, not Russian

Only use `--localized` for deliberately shared text such as brand names, protocol tokens, or untranslatable UI symbols.

## Core Flow

1. Generate scoped review artifacts.

```powershell
py "C:\code\WaveBits\tools\scripts\android\translate\run.py" compare --lang de --text-type sample_text --group ancient_dynasty --prompt-mode agent_json --output-dir temp/agent_jobs/job_001/reviews --job-dir temp/agent_jobs/job_001 --no-clean --json-output
```

2. Prefer `*.task.json` over parsing review markdown directly.

Generated artifacts now include:

- review markdown
- shared prompt docs such as `de/_prompts/agent_json.md`
- structured task files such as `de/sample_text/ancient_dynasty.task.json`

3. Produce suggestions in full-line form when easier for the model.

Suggested intermediate schema:

```json
{
  "dir": "values-de",
  "items": [
    {
      "name": "audio_sample_example_key",
      "xml": "values-de/audio_samples_example.xml",
      "replace_full": "Full improved localized line."
    }
  ]
}
```

4. Convert full-line suggestions into minimal `find/replace`.

```powershell
py "C:\code\WaveBits\tools\scripts\android\translate\run.py" build-replacements --input temp/agent_jobs/job_001/suggestions.json --output temp/agent_jobs/job_001/replacements.json --json-output
```

5. Apply replacements.

```powershell
py "C:\code\WaveBits\tools\scripts\android\translate\run.py" replace --json temp/agent_jobs/job_001/replacements.json --summary-out temp/agent_jobs/job_001/replace_result.json --job-dir temp/agent_jobs/job_001 --json-output
```

## Compare Artifacts

`compare --json-output` now returns:

- `english_review_paths`
- `localized_review_paths`
- `prompt_doc_paths`
- `task_json_paths`
- `prompt_mode`
- `prompt_version`

Each review markdown file includes:

- `PROMPT_MODE`
- `PROMPT_VERSION`
- `GENERATED_AT`
- `PROMPT_REF`

## Replace Status Model

`replace --json-output` now reports finer-grained status counts:

- `applied`
- `already_applied`
- `skipped_unchanged`
- `failed_not_found`
- `failed_ambiguous`
- `failed_validation`

Important behavior:

- if `find` is missing but `replace` is already present in the current string, the entry is treated as `already_applied`
- rerunning the same replacement batch should therefore produce fewer false failures than before

## Job Directory Guidance

Recommended layout:

- `temp/agent_jobs/job_001/reviews/...`
- `temp/agent_jobs/job_001/suggestions.json`
- `temp/agent_jobs/job_001/replacements.json`
- `temp/agent_jobs/job_001/replace_result.json`
- `temp/agent_jobs/job_001/job_manifest.json`

When `--job-dir` is set, the manifest links:

- review paths
- prompt docs
- task JSON paths
- replacement input JSON
- structured replace result JSON

## Translation Rules

- avoid terminology that creates external-IP legal or branding risk
- do not add external faction mappings to repository files
- if the current localized line is already natural, fluent, and safe, leave it unchanged
- prefer the smallest necessary edit
- preserve `SHORT` versus `LONG`
- keep lineup tone, but do not force literal word order from English

## Useful Commands

Scoped compare:

```powershell
py "C:\code\WaveBits\tools\scripts\android\translate\run.py" compare --lang fr --text-type sample_text --group sacred_machine --prompt-mode agent_json --output-dir temp/agent_jobs/job_001/reviews --job-dir temp/agent_jobs/job_001 --no-clean --json-output
```

Build replacements from full-line suggestions:

```powershell
py "C:\code\WaveBits\tools\scripts\android\translate\run.py" build-replacements --input temp/agent_jobs/job_001/suggestions.json --output temp/agent_jobs/job_001/replacements.json --json-output
```

Apply replacements:

```powershell
py "C:\code\WaveBits\tools\scripts\android\translate\run.py" replace --json temp/agent_jobs/job_001/replacements.json --summary-out temp/agent_jobs/job_001/replace_result.json --job-dir temp/agent_jobs/job_001 --json-output
```
