# Changelog

## Unreleased

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
