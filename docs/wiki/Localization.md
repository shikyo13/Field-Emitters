# Localization

Choose your language in Minecraft's language settings. Field Emitters uses that language for item and block names, tuner screens, tooltips, status messages, key bindings and command feedback. Each player can use a different language on the same server.

## Included languages

English (`en_us`), Spanish (`es_es`), French (`fr_fr`), German (`de_de`), Japanese (`ja_jp`), Simplified Chinese (`zh_cn`), Traditional Chinese (`zh_tw`), Brazilian Portuguese (`pt_br`), Russian (`ru_ru`), Korean (`ko_kr`), Polish (`pl_pl`) and Italian (`it_it`).

The catalogs cover every current Field Emitters translation key. Native-speaker corrections and terminology suggestions are welcome. Coverage and formatting validation do not substitute for linguistic review.

## Contributing a translation

Translations live in `src/main/resources/assets/fieldemitters/lang/`. Start with `en_us.json` from the same loader/version branch. Copy its keys into the Minecraft locale file, and translate only the values. Use UTF-8 JSON without duplicate keys or trailing commas.

- Keep keys stable, including keys whose names describe older wording.
- Preserve formatting arguments such as `%1$s`, `%2$s` and `%s`. Numbered arguments may move to suit the sentence; do not remove or duplicate them.
- Keep deliberate `\n` line breaks. Do not insert raw line breaks inside a JSON string.
- Keep command examples, registry IDs, tags, UUIDs, hex colors and scripting identifiers unchanged. For example, `/tag @s add resident` and `#minecraft:logs` must remain usable as written.
- Keep the mod name **Field Emitters** unchanged. In-game equipment names can be translated.
- Do not translate player-supplied names, group IDs or preset IDs.
- Match the visible control labels when referring to options in a tooltip. Distinguish blocking, detection, damage and inventory inspection; they have separate filters.

Test both short labels and long tooltips in game. Some controls truncate long labels and reveal the full text on hover. Help paragraphs are bounded to avoid covering other controls, with full text available on hover when needed. Check CJK glyphs, smaller windows and larger GUI scales.

## Loader differences

The keys are shared across all five builds. Forge 1.21.1 deliberately uses different wording for badge holders and recipe-browser dragging because it does not advertise the same Curios/EMI integrations. Preserve those differences when porting translations.

New player-facing messages must use translation components. Resolve them on the client, rather than flattening server messages into English strings. Machine-readable status names, exported preset JSON, datapack keys and script API identifiers remain stable and untranslated. Developer log diagnostics may remain in English.
