# Blocking filters

The Blocking tab decides which entities a field stops. Check the categories it applies to, then use the two lists below them for exceptions: **Always block** and **Always allow**. Categories combine with OR: a checked category matches on its own. The Sensor and Damage tabs use the same editor with their own lists.

## Directions

A rule can name directions in two ways. **Cardinal** names the way an entity travels, so North → South means entering from the north and leaving to the south. Each side of a perimeter then needs its own setting, because the same travel direction enters on one side and leaves on the opposite one.

**Relative (inside/outside)** names the crossing instead: Outside → Inside, or Inside → Outside. Each span works out which of its own faces is the inside, so one setting reads correctly on every side at once and stays correct if you move or rotate the perimeter. This is usually what you want for a base perimeter.

Relative is offered only where the posts enclose an area, which means at least three posts each linked to two others. A straight line of posts, a single span and rails have no inside, so they use world directions. Up and down are unambiguous and keep their own boxes in both modes. A relative rule uses the shared filter rather than the per-direction rules, since it already describes every direction.

## Categories

| Category | Matches |
| --- | --- |
| Hostile | Entities Minecraft classifies as monsters, such as zombies and creepers. |
| Passive | Living entities that are neither players nor monsters, such as cows, sheep and villagers. |
| Players | Player characters. |
| Drops | Loose item entities on the ground. |
| Nonliving | Everything else, such as boats, minecarts, primed TNT and experience orbs. |
| Projectiles | Arrows, tridents, fireballs and thrown items such as snowballs, eggs and ender pearls. |

Rules saved before Projectiles existed counted projectiles as Nonliving. They load with Projectiles selected wherever Nonliving was, so they keep matching the same entities.

## Exception lists

Each list names entities that are handled differently from their category. The green list spares them and the red list acts on them:

| Tab | Green list | Red list |
| --- | --- | --- |
| Blocking | Always allow | Always block |
| Sensor | Always detect | Never detect |
| Damage | Never damage | Always damage |

An entry can name a mob type or `#tag`, a dropped item or `#tag`, a player, an access card group, or one sampled mob or player. When both lists name the same entity, for example a player blocked by name who also carries an allowed card, **Always allow**, **Never detect** and **Never damage** win. Each list holds up to 64 entries; scroll over a list to page through it.

To add entries:

- Press **+** on a list. Choose Mob, Item, Player or Card group and type an ID, `#tag`, account name or group, or pick an online player.
- Sneak-right-click a mob or player with the Field Tuner, or sneak-right-click the air to sample yourself, then press **+** and **Add sampled type** or **Add sampled individual**.
- Drag a spawn egg or item from your inventory, JEI or EMI onto a list. A spawn egg adds its mob type.

Press **×** beside an entry to remove it. Adding an entry to one list removes it from the other.

## Details

- **Age:** any age, babies only or adults only. Dropped items, nonliving entities and projectiles need *Any age*.
- **Details:** limit the checked categories to one entity's UUID, or to entities with a label given by Minecraft's `/tag` command (entered without `#`).

Age and Details narrow the checked categories only. List entries keep their own rules.

**Skip owner: Yes** always lets the player who placed the emitter pass, even if a list names them.

## Rules from earlier versions

Rules saved by Field Emitters 1.3.0 and earlier used *Block selected* / *Allow selected only* modes and separate mob, item and player lists. They keep working unchanged. When you open one, the editor shows it as categories and lists, and it is saved in the new form only once you change it. A rule that uses a single entity type, a single item or other details the lists cannot express keeps the earlier editor.

## Direction checkboxes

The direction checkboxes choose the movement directions the field blocks. *To South* means moving from north to south; *Upward* means moving from below to above. These are world directions, not the way you are facing. Uncheck a direction to let entities pass that way, for example a one-way exit from a mob farm.

## Per-link rules

A perimeter can use different rules on different sides. Select a link on the Connections tab and change its Blocking and Sensor filters; the defaults still apply to the other links. See [Controls and the tuner](Controls.md).

## List screens

The inventory checkpoint and rules kept in the earlier editor use separate item, mob and player list screens. See [Mob and item lists](Type-lists.md).
