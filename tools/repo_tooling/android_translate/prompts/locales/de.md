# German Android Translation Locale

[profile_id]
german_standard

[mode]
standard_german

[locale_note]
German (`values-de`) is a standard German localization for Android UI, playback, and signal-visualization text.

[identity_rule]
Treat this locale as contemporary German UI localization.
Use German capitalization rules for nouns and UI labels.
Prefer natural German word order over English word order, especially in playback, input-rule, byte-count, and visualizer strings.
Preserve FlipBits protocol tokens such as mini, flash, pro, ultra, ASCII, UTF-8, Hex, Binary, Morse, Hz, payload, token, byte, and nibble when the existing German locale keeps them as technical terms.

[app_text_rule]
For app UI text, keep labels compact enough for Android buttons, tabs, segmented controls, mini-player sheets, sliders, and narrow cards.
Use German compounds where they are natural, but avoid overlong compounds in narrow UI controls.
Follow existing project terms where present: Audio, Zeichen, Bytes, Eingaberegeln, Visual, Tokens, Hex, Morse, Payload, Token, Nibble, niedriger Träger, and hoher Träger.
Do not turn short controls, visualizer mode labels, or transport labels into explanatory sentences.

[sample_text_rule]
For built-in sample prose, preserve the dramatic audio-signal atmosphere while writing fluent German.
Keep pro-mode sample strings strict ASCII when the key or context says pro/ASCII.

[key_alignment_rule]
When filling missing entries, write German localization that matches the current XML terminology.
Preserve placeholders and do not alter German plural or case around numeric placeholders unless the full string requires it.
For missing compact input summaries, preserve placeholders and mirror the established counter pattern around `%1$d Zeichen • %2$d Bytes`.
