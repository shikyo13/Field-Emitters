# Changelog

## Unreleased

- Add datapack field presets, reusable filters, operator commands and optional server-side KubeJS controls/events. Preset locks persist with networks and prevent tuner overrides.

- Prevent an open tuner from overwriting another manager's unrelated changes. Conflicting edits refresh to the server value with an inline notice; instant apply remains enabled.

- Persist shared network settings across joins, reconnects and world loads, including switched-off fields.
- Let one redstone input control a connected group of posts or rails. Show server-reported group energy and the actual reason a field is stopped.
- Show relative direction controls at every post in an enclosed perimeter.

- Blocking, detection and damage rules can name directions relative to the area the posts enclose, as Outside → Inside and Inside → Outside, instead of as world directions. One setting then reads correctly on every side of a perimeter and survives moving or rotating it, where world directions need a separate setting per side. World directions remain the default and are unchanged. The relative choice appears only where the posts enclose an area.
- Facing rails now link only when they choose each other, the same rule posts follow. Three rails in a line could previously give one rail a span reaching past its neighbour, which made the group look different depending on which rail was read and could switch its power on and off every tick.
- A field given rules of its own can be returned to the group rules from the Connections tab. Previously there was no way to undo a per-field rule, so later group-wide changes silently skipped that field forever. Rules kept for a partner that has since been removed are now dropped as well.
- Water and lava now stop at a powered field. Previously a fluid destroyed the field cell and took its place, leaving a permanent gap because the rebuild only refills air.
- Detection pulses trigger immediately and never queue. Simultaneous crossings share one pulse; further crossings refresh its duration. Item counting still updates the lifetime counter independently. Removed the pending-signal display and discard old saved pulse queues.
- The detection output face, checkpoint storage and formation origin of a group no longer move when a buffer empties or a redstone signal changes. They stay with the same emitter until the field is changed or broken. On an existing world the first rebuild may move a group's formation origin and its "connect redstone at" hint once, to the lowest post in the group, after which they stay put.
- A mount and its riders are now checked together. A field that stops the rider or the vehicle stops the whole group, so a blocked player can no longer cross on a horse, boat or minecart. Inventory checkpoints check riders the same way. A rider with passage rights, the exempt owner or an access badge holder, takes the group through.
- Fixed a crash when a post was placed directly above another post. Links between posts are now mutual, and stacked posts link to the post closest to their own height.
- The redstone input reads from any side except the output face by default. A single side can still be chosen.
- The Power tab now says what a stopped field is missing: no energy, not enough energy for the group, or the redstone condition. Mode names and help make clear that redstone switches fields and FE powers them.
- The tuner header shows the mod version.
- Connected emitters and rails now share one set of settings. Changes made from any block of a group apply to the whole group, and newly placed posts and rails adopt the group's settings when they link up. Previously a post applied changes to itself only, so blocking and detection rules could differ between the sides of one perimeter depending on which post was opened. The "Change settings for" toggle is gone; link-specific rules still work.
- Added translation support for tuner menus, filter editors, help, animation names, item instructions, configuration labels, and server messages.
- Separated control identities and filter purposes from display text. Organized tuner controls into tab builders and shared UI constants.
- Added fitted labels with full hover text for longer translations.
- Preserved translatable detection messages across saves. Existing custom names and legacy detection text remain readable.
- Updated menu-response networking; clients and servers must use the matching mod build.

## 1.1.0

- Projection towers create hollow spherical fields or domes. Domes reach four blocks below their base to meet nearby terrain. Surface rails form bridges level with adjacent full blocks.
- Multiple contacts produce simultaneous impact ripples. Intersections brighten on walls and bridges.
- Six field formation animations, including plasma dissolve, work independently of the selected field texture. Added visual styles, color presets and sound controls.
- Adjustable contact damage with directional filtering and fizzle effects.
- Separate directional rules and allow/block lists for players, mobs and items. Compatible recipe browsers can drag items and spawn eggs into filters.
- Private networks by default, invited managers and optional public management. Passage badges and access groups remain separate from management permissions.
- Access badges, a badge holder and a tuner keybind, with optional wearable-slot integration where supported.
- Inventory checkpoints can detect contraband, deny passage, drop matching items on the entry side, or transfer them into adjacent storage. Overflow dropping is optional.
- Reorganized tuner controls with immediate updates, clearer filter explanations and scaling for smaller windows. Revoked managers lose access to open controls immediately.
- ZeroMods Core is bundled. No separate Core download is needed.

## 1.0.0

First release for Minecraft 1.21.1 on NeoForge.

- Field Emitters: five-block posts that link to other posts up to 20 blocks away in the four cardinal directions and project a five-block-high field that follows the terrain between them.
- Field Rails: surface-mounted strips for any block face. Opposing rails up to 20 blocks apart span a one-block-wide field, and adjacent rails join into a single wall, floor or ceiling.
- Field Tuner: opens emitter controls, manages every loaded field remotely, samples mobs and players for filters, and cycles field colors.
- Blocking filters by category (hostile, passive, players, drops, nonliving) with optional age, entity type or tag, item, UUID and scoreboard tag details, movement direction rules, inversion and owner exemption.
- Detection with crossing pulses or presence output on a chosen redstone face, per-item or per-stack counting and a lifetime crossing counter.
- Per-link rule overrides, redstone enable modes, colors, ambient animation and optional block light.
- Forge Energy input on every emitter and rail. Defaults: 2 FE per field block each tick, 100,000 FE storage and 10,000 FE/t transfer, adjustable in the server config.
- Survival recipes for the emitter, rail and tuner, a Field Emitters creative tab, and drops for every emitter section.
