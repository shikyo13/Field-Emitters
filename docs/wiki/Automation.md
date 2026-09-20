# Datapacks and KubeJS

Field presets work without KubeJS. They reuse the tuner's server-side network settings, including persistence and connection rules. Applying a preset changes the connected network; it does not create emitters, supply FE, load chunks, or grant management access.

## Supported platforms

| Minecraft | Loader | Datapacks and commands | KubeJS adapter |
| --- | --- | --- | --- |
| 1.21.1 | NeoForge | Yes | KubeJS 2101.7.2-build.377 |
| 1.21.1 | Forge | Yes | No compatible official KubeJS build |
| 1.21.1 | Fabric | Yes | No compatible official KubeJS build |
| 1.20.1 | Forge | Yes | KubeJS 2001.6.5-build.26 |
| 1.20.1 | Fabric | Yes | KubeJS 2001.6.5-build.26 |

KubeJS and its dependencies are optional and are not bundled. Install matching versions on the server and clients when using KubeJS. See the [KubeJS platform notes](https://kubejs.com/wiki/other/major-updates/7.0).

## Datapack presets

Put a definition at `data/<namespace>/fieldemitters/presets/<name>.json`. The preset ID is `<namespace>:<name>`. Custom directory names are the same on both Minecraft versions.

For example, `data/mypack/fieldemitters/presets/security_gate.json`:

```json
{
  "color": "#52E5FF",
  "enabled": true,
  "locked": false,
  "controls": {
    "Barrier": "mypack:hostiles",
    "SensorMode": 1,
    "Sensor": { "Groups": 4, "OwnerExempt": false },
    "PulseTicks": 4,
    "DamageEnabled": false
  }
}
```

Put the referenced filter at `data/mypack/fieldemitters/filters/hostiles.json`:

```json
{
  "Groups": 1,
  "Directions": 63,
  "MobMode": 1,
  "MobList": ["minecraft:creeper", "#mypack:dangerous_mobs"]
}
```

Each filter slot accepts either an inline object or a filter ID. This includes `Barrier`, `Sensor`, `Damage`, checkpoint `Players`/`Items`, and directional overrides. Filter references cannot reference another filter, preventing reference cycles.

Presets are partial updates: omitted controls retain their current values. Inline and referenced filters are also partial updates. The named preset is recorded on the network as the last applied preset, not a live link to the datapack. Editing an unlocked field does not change the datapack definition.

Two built-in examples are available:

- `fieldemitters:hostile_barrier`: block hostile mobs, cyan appearance.
- `fieldemitters:smart_tripwire`: allow passage and pulse for matching crossings.

### Control format

`controls` uses the serialized setting names from the tuner. Use `/fieldemitters export` to obtain a complete, valid JSON starting point instead of guessing names. Booleans are JSON `true`/`false`; colors use `#RRGGBB`. Unknown names, incorrect types and out-of-range values reject the definition rather than silently clamp it.

| Setting | Values |
| --- | --- |
| `Groups` in a filter | Bitmask: hostile 1, passive 2, players 4, dropped items 8, other entities 16. Add values to combine them; 31 selects all categories. |
| `Directions` in a filter | Bitmask: down 1, up 2, north 4, south 8, west 16, east 32; 63 selects all directions. |
| `SensorMode` | 0 off; 1 pulse on crossing; 2 output while touching. |
| `MobMode`, `ItemMode` | 0 use the single-type fields; 1 selected list; 2 everything outside the list. |
| `MobList`, `ItemList` | Registry IDs or `#namespace:tag` strings. |
| `PlayerList` | Objects with `Id` (UUID string) and optional `Name`. |
| `OwnerExempt` | Whether the filter exempts the owner. |
| `PulseTicks` | Pulse duration in game ticks. Crossings refresh an active pulse; pulses do not queue. |

Recipes and item/entity tags remain ordinary Minecraft datapack resources. Use the Minecraft-version-appropriate vanilla directories (`recipes`/`tags/items` on 1.20.1; `recipe`/`tags/item` on 1.21.1).

### Locks and reloads

`locked: true` prevents tuner settings edits, connection overrides and shift-click colour cycling. Applying a locked preset clears existing connection overrides so they cannot bypass the preset. The lock and preset identity follow the network and survive saves. It does not change ownership, badges or the list of network managers. Operators and trusted server scripts can still change or unlock the field.

`/reload` loads definitions atomically. If any field definition is invalid, the previous registry remains available and the server logs identify the bad file/setting. Correct the definition and reload again. Successful reloads affect future applications only; running fields retain their settings until explicitly reapplied. Removing a definition likewise does not erase settings or unlock an existing field.

## Commands

These commands require permission level 2. Use base-block coordinates in the command source's dimension. They work from operators, command blocks and datapack functions.

```mcfunction
fieldemitters presets
fieldemitters apply 0 64 0 mypack:security_gate
fieldemitters enable 0 64 0 false
fieldemitters unlock 0 64 0
fieldemitters status 0 64 0
fieldemitters export 0 64 0
```

`status` returns the detector's current redstone strength as its command result, allowing `execute store result`. For chained rails, signal and crossing queries read their shared detector. `export` prints preset JSON; it does not write files. Existing world/entity data and private inventories are not exported.

## KubeJS server scripts

Place scripts in `kubejs/server_scripts/`. The `FieldEmitters` binding exists only in server scripts. `FieldEmitterEvents` handlers also run on the server.

```js
ServerEvents.loaded(event => {
  console.info('Field presets: ' + FieldEmitters.presets())
})

FieldEmitterEvents.crossing(event => {
  // This is a completed crossing, not merely touching the field.
  console.info(event.entity.name.string + ' crossed ' + event.field.position)
})

FieldEmitterEvents.contraband(event => {
  console.info('Contraband detected: ' + event.entity.name.string)
})

FieldEmitterEvents.powerChanged(event => {
  console.info('Field is powered: ' + event.powered)
})
```

Obtain a handle with `FieldEmitters.get(serverLevel, x, y, z)`. Available properties/methods:

- `level`, `position`, `enabled`, `powered`, `crossings`, `signal`, `status`, `preset`, `locked`.
- `applyPreset('namespace:name')`, `setEnabled(true)`, `unlock()`, `exportPreset()`.

Handles require a loaded emitter and the server thread. They do not force-load chunks. A handle stops working if its emitter is replaced; request a new handle after relocating or replacing hardware. Scripts are trusted server administration code, so they may manage a field regardless of its player ownership or preset lock.

### Passage decisions

```js
FieldEmitterEvents.passage(event => {
  if (event.entity.type === 'minecraft:creeper') event.deny()
  // Explicit allow also overrides normal blocking and checkpoint holds.
  // Only use it for entities that should bypass those rules.
})
```

`event.blocked` is the normal or current decision. `allow()` and `deny()` override it; if neither is called, ordinary rules apply. Direction is exposed as `event.direction` (down/up/north/south/west/east). `event.count` describes the recorded count increment for crossing and contraband events.

Passage is a collision decision, not a crossing notification. It can run repeatedly, including proactive checks for nearby players in each direction. Keep handlers fast and free of inventory changes, rewards or other once-only effects. Use `crossing` or `contraband` for those. Player decisions are refreshed every five ticks and expire after fifteen ticks; this lets clients predict the same collision as the server. Nested field events are suppressed to prevent recursive scripts.
