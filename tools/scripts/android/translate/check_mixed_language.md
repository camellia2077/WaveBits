# check_mixed_language.py

## Purpose

`check_mixed_language.py` scans Android localized string resources and reports strings that appear to contain untranslated or mixed-language fragments.

It is intended as a fast lint-style helper for localization review. It writes separate `.md` reports for app UI text and sample text, grouped by language. Each report lists suspicious keys, the localized text, and the English source text.

## Default paths

- Input: `apps/audio_android/app/src/main/res`
- Output: `temp/mixed_language_reports/<lang>/app_text/mixed_language_report.md` and `temp/mixed_language_reports/<lang>/sample_text/mixed_language_report.md`

The script resolves these paths from the repository root, so it can be run from the repository root with:

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py"
```

The unified outer entrypoint is:

```powershell
pwsh -NoLogo -Command "python tools/scripts/android/translate/run.py"
```

`check_mixed_language.py` itself now acts as a library-style module and no longer serves as the direct entrypoint.


## Output split

Reports are split by language first, then by responsibility:

- `temp/mixed_language_reports/<lang>/app_text/mixed_language_report.md` for app UI strings, such as `strings.xml`.
- `temp/mixed_language_reports/<lang>/sample_text/mixed_language_report.md` for sample text XML files whose names start with `audio_samples_`.

This makes it easier to send only app UI text or only sample text to a reviewer or another agent.

For sample reports, entries also include `SAMPLE_LENGTH: SHORT` or `SAMPLE_LENGTH: LONG` so reviewers can keep the original short/long intent in mind while fixing suspicious mixed-language fragments.

## Detection strategy

The script compares `values` English resources with every localized `values-*` directory.

For non-Latin target languages such as Chinese, Japanese, and Russian, it looks for suspicious Latin/English chunks in the localized text. For example, a Chinese string that accidentally contains `stolen floor` would be reported unless the words are explicitly whitelisted.

For Latin-script target languages such as German, Spanish, and Portuguese, it checks whether multi-word English source phrases were copied directly into the localized string. This catches likely untranslated fragments while allowing normal localized Latin-script text.

Each language report keeps the `.md` extension, but now uses lighter Markdown with only headings and field labels so the report is cheaper to paste into prompts:

```text
# Mixed Language Report [APP_TEXT][DE]
 
## DE | Latin N-gram Check
TOTAL_ISSUES: 12

FILE: strings.xml
KEY: audio_input_editor_inline_hint
ISSUE: 可疑漏翻/混杂: TXT editor
TR: Längere Passagen bleiben hier eingeklappt. Öffne den TXT-Editor...
EN: Longer passages stay collapsed here. Open the TXT editor...
```

## Pro ASCII exception

Pro mode is a special case in this repository.

Pro sample text is not normal localized prose. It is fixed protocol/encoding input and must remain ASCII in every language. These sample keys contain `_ascii_`, for example:

- `audio_sample_ancient_dynasty_ascii_alloy_hand_no_warmth`
- `audio_sample_labyrinth_of_mutability_ascii_thread_pulls_the_hero`

For those keys, the script does not check language mixing. It only verifies that the localized resource contains ASCII-range characters.

Pro UI strings can also legitimately include protocol terms such as `ASCII`, `byte`, `Token`, `Nibble`, or `0x`. Keys like these are treated as Pro ASCII context and skipped for mixed-language detection:

- `audio_pro_visual_token_mapping`
- `audio_pro_visual_ascii_byte`
- `audio_pro_visual_byte_binary`
- `validation_pro_ascii_only`

This prevents false positives where expected protocol words are reported as mixed language.

## Whitelist

`WHITELIST` contains short technical terms that are allowed across languages, such as:

- `ascii`
- `ui`
- `id`
- `hex`
- `ok`

Add to the whitelist only when a term is intentionally shared across localized UI and should not be translated.

## Notes for agents

- This script is heuristic. A report item is suspicious, not automatically wrong.
- Do not flag Pro sample `_ascii_` strings as untranslated English. They must remain ASCII for all languages.
- If new Pro protocol UI keys are added and false-positive, update `is_pro_ascii_context_key()`.
- If new non-translated technical terms are expected across languages, update `WHITELIST`.
- Output reports are generated under `temp/mixed_language_reports/<lang>/app_text/` and `temp/mixed_language_reports/<lang>/sample_text/` and should usually not be committed.
- XML scanning, resource parsing, and Markdown writing are intentionally shared with `compare_translation_quality.py`; keep parsing and output formatting decoupled from report logic.
