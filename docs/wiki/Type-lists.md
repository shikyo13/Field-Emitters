# Mob and item lists

Open **Block**, **Detect**, or **Damage**, choose the travel direction to edit, then open **Mob list** or **Item list**. A custom directional rule has its own lists. Shared rules continue to apply in directions without an override.

Each list holds up to 64 IDs or tags. Entries match with OR: any listed type or tag is enough.

| Mode | Block | Detect / Damage |
| --- | --- | --- |
| General filter | Uses the existing categories, details and inversion | Uses the existing categories, details and inversion |
| Whitelist | Lets the listed selection through; blocks the rest of that kind | Acts on the listed selection only |
| Blacklist | Blocks the listed selection only | Acts on everything of that kind except the listed selection |

Mob lists affect living entities other than players. Item lists affect dropped item entities, not inventory contents. Neither changes player-list behavior or the other list. Directions and **Skip owner** still apply. List modes replace the general categories, single-type restriction and inversion for their own kind of entity.

For mobs, **Age**, **Specific mob UUID** and **Custom entity label** narrow the listed selection before whitelist or blacklist is applied. For example, a blocking whitelist containing pigs with **Babies only** allows baby pigs and blocks adult pigs and all other mobs. Item lists ignore those mob details. An empty blocking whitelist blocks all of its kind; an empty blocking blacklist blocks none.

EMI integration requires EMI 1.1.24 or newer for custom-screen support. Neither recipe browser is required to use the filters.

## Adding entries

- Drag an item from the inventory shown at the bottom into any filter box. This creates a reference and never consumes or moves the stack.
- For a mob list, drag a spawn egg; it adds the mob type, not the egg item.
- With JEI or EMI installed, drag an item or spawn egg from its ingredient list into the boxes.
- Type an ID such as `minecraft:creeper` or `minecraft:gunpowder`, then press **Add** or Enter.
- Prefix a registry tag with `#`, such as `#minecraft:skeletons` for mobs or `#minecraft:logs` for items. Tags must be defined by Minecraft, a mod or a datapack; entering a name does not create one. A missing or empty tag matches nothing.
- Right-click an entry to remove it. Duplicate entries are ignored. Use Previous / Next for longer lists.

Item entries match item type only, ignoring stack size, damage, enchantments and other components. Players belong in the separate **Player list**.

Changes apply immediately. Newly added entries remain inactive while **General filter** mode is selected; choose whitelist or blacklist to use them. Existing worlds keep their previous general-filter behavior until a list mode is enabled. The old single-type field remains editable inside the list screen in General filter mode.
