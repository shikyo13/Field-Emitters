# Blocking filters

The Blocking tab decides which entities a field stops. Categories combine with OR: a checked category matches on its own. Details combine with AND: every filled-in detail must also match. Blank details add no restriction.

## Categories

| Category | Matches |
| --- | --- |
| Hostile | Entities Minecraft classifies as monsters, such as zombies and creepers. |
| Passive | Living entities that are neither players nor monsters, such as cows, sheep and villagers. |
| Players | Player characters. |
| Drops | Loose item entities on the ground. |
| Nonliving | Everything else, such as arrows, boats, minecarts and experience orbs. |

## Details

- **Age:** any age, babies only or adults only. Dropped items and nonliving entities need *Any age*.
- **Entity type or #group:** one entity type such as `minecraft:creeper`, or an existing entity-type tag such as `#minecraft:skeletons`. Keep the matching category checked.
- **Dropped item or #group:** one item such as `minecraft:gunpowder` or an item tag such as `#minecraft:logs`. Applies to items on the ground with the Drops category checked, not to inventories.
- **Specific mob or player (UUID):** one individual. Sample it with the tuner and press *Match this sampled individual*, or paste a UUID.
- **Custom entity label:** a label given with Minecraft's `/tag` command, entered without `#`.

## Mode

- **Block selected:** matching entities are stopped and everything else passes.
- **Allow selected only:** only matching entities pass and everything else is stopped. Example: check *Passive* and *Babies only* to let baby animals through and stop everything else.

**Skip owner: Yes** always lets the player who placed the emitter pass, even in *Allow selected only* mode.

## Directions

The direction checkboxes choose the movement directions the field blocks. *To South* means moving from north to south; *Upward* means moving from below to above. These are world directions, not the way you are facing. Uncheck a direction to let entities pass that way, for example a one-way exit from a mob farm.

## Per-link rules

A perimeter can use different rules on different sides. Select a link on the Connections tab and change its Blocking and Detection filters; the defaults still apply to the other links. See [Controls and the tuner](Controls.md).

## Mob and item lists

Independent mob and item allow/block lists have their own screens, with inventory and JEI/EMI drag-and-drop boxes. See [Mob and item lists](Type-lists.md) for their interaction with age, direction and existing filters.
