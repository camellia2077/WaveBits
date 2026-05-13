# Android Translate Tool Guide

## Scope

This package contains the Android XML translation tooling. It is part of the formal `tools/run.py` command surface because it supports normal Android string-resource maintenance, prompt generation, lint/autofix, key alignment, and bulk replacements.

## Command Discovery First

- Agent first step must be command discovery, not document browsing:
  - run `python tools/run.py android-translate --help`
  - then run `python tools/run.py android-translate <subcommand> --help`
- Treat docs as secondary references only after command-level discovery.

## Primary Command Surface

- Entry point: `python tools/run.py android-translate`
- Discoverability commands:
  - `list-text-types`
  - `list-groups --text-type <app_text|sample_text>`
  - `list-langs`
- Translation review generation:
  - `compare`
  - `all`
- Term/glossary inspection:
  - `term-suggestions`
- XML inspection and repair:
  - `dump-xml-md` (supports `--with-en`)
  - `fix-resource-escapes`
  - `lint`
  - `autofix`
- Translation safety and alignment:
  - `mixed-language`
  - `key-alignment`
    - optional scope: `--lang <locale>`
    - optional strict stale gate: `--fail-on-stale`
- Bulk replacement workflow:
  - `build-replacements`
  - `replace`

## Term Suggestion Usage

- To inspect one English term across localized XML and surface current per-language term candidates:
  - `python tools/run.py android-translate term-suggestions --term Standard --text-type app_text --group strings_settings --whole-word`
- This command is term/glossary inspection, not automatic translation.
- It matches English baseline strings in `values/`, then shows the aligned current localized values from `values-*`.
- Candidate suggestions are only auto-ranked when the English source value exactly equals the requested term; longer sentence-level matches are still reported, but may not yield a single candidate term automatically.
- Use this command before destructive terminology renames so you can review how one English label currently fans out across languages.

## Lint + Autofix Quick Usage

- Run deterministic lint checks:
  - `python tools/run.py android-translate lint --lang fr --json-output`
- Apply deterministic low-risk autofixes:
  - `python tools/run.py android-translate autofix --lang fr --json-output`
- Note: `autofix` includes a final Android escape-normalization pass to prevent AAPT2 string escape failures.
- Typical loop:
  - `autofix` -> `lint` -> `compare`

## Where To Look

- Human web-chat workflow lives in `docs/sop.md`.
- CLI behavior and JSON contracts live in `docs/cli_contract.md`.
- Generated prompt/profile text lives under `prompts/`.
- Locale-specific rules live under `prompts/locales/`.

## Tool Boundaries

- Do not duplicate translation workflow steps in this file.
- Do not store locale prompt policy in this file.
- Keep generated prompt content in `prompts/` so the tool output and agent instructions stay aligned.
- Treat `values/` as the English baseline source of truth.
- Treat `values-*` as localized targets; do not assume they are baseline files.
- Prefer relative repository paths in generated docs/workflows/prompts; avoid absolute paths.

## Prompt Profiles

The translate tool loads locale profiles from:

- shared locale constraints: `prompts/locales/_shared.md`
- locale-specific constraints: `prompts/locales/{locale}.md`

Current shared locked English terms (must remain untranslated when present in EN source):

- `flash`, `pro`, `mini`, `ultra`, `ASCII`, `UTF-8`, `Hex`, `Binary`, `Morse`, `Emoji`, `Tokens`, `Mix`

For `sample_text`, some locales also load faction-specific style profiles from:

- `prompts/sample_text_profiles/values-{locale}/_global.md`
- `prompts/sample_text_profiles/values-{locale}/{group}.md`

Current faction-style locales include `ko`, `zh-rTW`, and `uk`.

All generated translation-tool artifacts must live under the repository root `temp/translations/`.

- Default review output: `temp/translations/reviews`
- Default key-alignment output: `temp/translations/key_alignment`
- Default XML dump output: `temp/translations/xml_dump`
- Translation lint baseline file: `temp/translations/lint-baseline.json`

When reading previously generated artifacts, read them from `temp/translations/...`, not from tool-local temp folders.

After changing a locale prompt profile, generate a scoped job and inspect the produced `_prompts/*.md` and `*.task.json` files:

```powershell
python tools/run.py android-translate compare --lang uk --text-type app_text --group strings_audio --prompt-mode agent_json --output-dir temp/translations/reviews/prompt_probe/reviews --job-dir temp/translations/reviews/prompt_probe --no-clean --json-output
```

For `sample_text` prompt/style debugging, run:

```powershell
python tools/run.py android-translate compare --lang uk --text-type sample_text --output-dir temp/translations/reviews/style_probe/reviews --job-dir temp/translations/reviews/style_probe --no-clean --json-output
```

For XML text inspection with EN baseline side-by-side:

```powershell
python tools/run.py android-translate dump-xml-md --lang uk --text-type sample_text --with-en --output-dir temp/translations/xml_dump/xml_dump_uk --no-clean
```
