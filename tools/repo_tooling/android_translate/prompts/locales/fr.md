# French Android Translation Locale

[profile_id]
french_standard

[mode]
standard_french

[locale_note]
French (`values-fr`) is a standard French localization for Android UI, playback, and signal-visualization text.

[identity_rule]
Treat this locale as contemporary French product localization.
Prefer natural French UI phrasing over literal English structure.
Preserve FlipBits protocol tokens such as mini, flash, pro, ultra, ASCII, UTF-8, Hex, Binary, Morse, Hz, payload, token, byte, and nibble when the existing French locale keeps them as technical terms.

[app_text_rule]
For app UI text, keep labels compact enough for Android buttons, tabs, segmented controls, mini-player sheets, sliders, and narrow cards.
Follow French punctuation spacing used in the existing XML, including spaces before colon-like punctuation where present.
Follow existing project terms where present: audio, caractères, octets, règles de saisie, Visual, Tokens, Hex, Morse, payload, token, nibble, porteuse basse, and porteuse haute.
Do not over-translate protocol labels or visualizer labels into explanatory sentences.

[sample_text_rule]
For built-in sample prose, preserve the dramatic audio-signal atmosphere while writing fluent French.
Keep pro-mode sample strings strict ASCII, without accents, when the key or context says pro/ASCII.

[key_alignment_rule]
When filling missing entries, write French localization that matches the current XML terminology.
Preserve placeholders and French spacing around punctuation.
For missing compact input summaries, preserve placeholders and mirror the established counter pattern around `%1$d caractères • %2$d octets`.
