# Translating Field Emitters

All shipped English text lives in [`en_us.json`](../src/main/resources/assets/fieldemitters/lang/en_us.json). This includes the Field Tuner, nested filter and access screens, tooltips, item instructions, animation names, configuration labels, and server feedback.

Copy this file alongside it using Minecraft's language code, such as `de_de.json` or `pt_br.json`. Translate the values, leaving the keys unchanged. You do not need to edit Java. Missing translations fall back to English.

## Formatting

- Keep placeholders such as `%1$s` and `%2$s`. You may move them to suit your language, but do not rename or remove them. Their numbers identify the original arguments.
- Translate complete sentences. A placeholder can be a number, player name, direction, item name, or another translated phrase. Check the English value and the call site when its meaning is unclear.
- Preserve registry IDs (`minecraft:creeper`), tag examples (`#minecraft:logs`), command names (`/tag`), UUIDs, and six-digit color codes. They are input syntax, not words to translate.
- Preserve intentional `\n` paragraph breaks. Save UTF-8 JSON; escape quotes inside values.
- Keep short button labels concise. Long button labels fit their bounds and expose their full text on hover; truncated field labels do the same. Tooltips can carry longer explanations.
- Translation changes can also be tested in a resource pack at `assets/fieldemitters/lang/<language>.json`. Reload resources with F3+T, then reopen the tuner.

Player account names, saved custom field names, access-group identifiers, IDs, tags, and UUIDs are never translated. Old saved detection descriptions remain readable as literal text; new detections retain their translation keys when saved. Existing network names remain intact. Clearing a name restores a localized location-based default.

## Code organization

`ControlTab` identifies tuner sections. `FilterPurpose` identifies the independent blocking, sensor, damage, and checkpoint editors. `Control` identifies individual settings and selects their help and editability. None of these depend on visible labels.

`UiText` resolves strings where a client-only widget API requires a string. Use `Component.translatable` for messages that cross the network or are persisted. Keep the component intact on the server; calling `getString()` there would choose the server's language instead of the recipient's. Resolve to a string only at a client presentation boundary.

Network response payloads carry components. The affected payload registration version is bumped so old string-based clients are not treated as compatible. Client and server need the matching mod version.

`ScreenMetrics` owns shared layout dimensions and interaction timings. Domain limits belong to the class that enforces them, such as `EntityFilter.MAX_TYPES`. Keep geometry-specific measurements local. Do not duplicate a gameplay limit in a new screen or use a displayed label as a state identifier.

When adding a setting:

1. Add a stable control identity and place it in the appropriate tab builder.
2. Add complete label and help entries to `en_us.json`. Prefer a descriptive key that stays stable when wording changes.
3. Supply dynamic values through numbered placeholders, not English sentence concatenation.
4. Reuse the existing filter editor for the relevant `FilterPurpose` rather than copying its behavior.
5. Verify the setting with replacement labels, long text, a resource reload, and server feedback. Also verify any persisted data remains readable.

Translation coverage should include all tuner tabs, player/mob/item lists, management and badge screens, inventory checkpoints, hover text, animation names, field names, and validation failures. A successful compile alone does not verify layout or localization.
