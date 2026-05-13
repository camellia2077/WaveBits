# Shared Android Translation Locale Constraints

[profile_id]
shared_constraints

[mode]
shared_rules

[locale_note]
Shared constraints that apply to every locale profile unless explicitly overridden.

[identity_rule]
Never translate these product/protocol terms; preserve exact English spelling and casing: flash, pro, mini, ultra, ASCII, UTF-8, Hex, Binary, Morse, Emoji, Tokens, Mix.

[app_text_rule]
Keep the non-translatable term list unchanged in UI labels, hints, validation text, and settings descriptions: flash, pro, mini, ultra, ASCII, UTF-8, Hex, Binary, Morse, Emoji, Tokens, Mix.

[sample_text_rule]
When sample text contains locked protocol/product terms, keep them exactly as English tokens: flash, pro, mini, ultra, ASCII, UTF-8, Hex, Binary, Morse, Emoji, Tokens, Mix.

[key_alignment_rule]
If an English source string contains any locked term, localized output must retain the same English token form: flash, pro, mini, ultra, ASCII, UTF-8, Hex, Binary, Morse, Emoji, Tokens, Mix.
