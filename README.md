# Field Emitters — 0.2.0 demo

Minecraft **1.21.1 / NeoForge 21.1.206 / Java 21**.

Run `Launch Automation Demo.command` for the perimeter, a three-strip doorway, a floor/ceiling span, and a horizontal field. The doorway recognizes the demo player and pulses its lamp. Right-click an emitter or rail with an empty hand or Field Tuner to open its controls.

## Controls

Settings apply automatically. Buttons update immediately; valid text applies after a 350 ms typing pause or when leaving the page. Invalid IDs/UUIDs are retained in the editor for correction and are not sent. Close does not undo applied changes. The panel fits the available GUI size with matching mouse coordinates.

## Remote field manager

Right-click air with a Field Tuner, or choose **Emitters** in the controls, while holding the tuner. Each connected, loaded chain appears as one managed field. Select it to rename it or open any of its emitters remotely. In Connections, **Change settings for** chooses one emitter or your connected emitters; **Editing** selects an individual span's filters.

Names persist on the emitters. New placements record their placement time; the oldest loaded member anchors the field's displayed location. Existing demo emitters use a deterministic coordinate fallback. Switched-off emitters remain grouped. Unloaded chunks are never forced to load: a split or partially unloaded chain can appear as separate groups, retaining names; reconnecting groups uses the oldest member's name. Only emitters you may edit are listed, in the current dimension, up to 256 groups. Remote requests require a held tuner and server-side ownership permission.

## Settings tabs

- **Power:** enable, FE status, passage counter, recent detection, redstone input face and high/low/always mode.
- **Blocking:** choose hostile, passive, player, item, and other entities independently. Optional age, entity ID/tag, item ID/tag, UUID and scoreboard-tag constraints combine with AND. Categories combine with OR. “Allow selected only” allows matches and blocks everything else; “Detect unselected” inverts the detection predicate: selecting babies and choosing Allow selected only allows only babies through. Owner exemption is explicit.
- **Detection:** independent filters, crossing pulses or presence output, and entity-stack versus individual-item counting. A crossing completes after the whole entity clears the field. Contact and impact animation do not count. Pulses are separated by two ticks; duration is adjustable. The queue holds up to 100,000 pulses; excess pulses are dropped while the lifetime counter still advances. Disabling/powering off clears pending output.
- **Appearance:** hex color or presets, visual visibility, ambient field animation, and world lighting. Turning lighting off preserves emissive visuals and collision without adding block light.
- **Connections:** apply to this emitter or connected emitters you own; select an outgoing link for independent barrier/detection rules; configure sensor output face, pulse duration, and rail plane normal. Link overrides apply to both endpoints and persist independently of defaults. Input and output use different faces. Field orientation uses wall/floor descriptions and pulse duration uses seconds. Hold the tuner to see movement arrows: amber marks an enabled blocking direction, green a disabled direction. Selecting a link briefly highlights it. Appearance → Direction guides → Hidden disables the guides for your client. All filter inputs have hover help with examples; empty inputs impose no extra condition.

Direction buttons indicate movement **toward** that world side (South means North → South). Presence mode uses the approach direction; an entity initially spawned on the plane has no directional approach history.

Sneak-use the tuner on a living mob to sample it, or in the air to sample yourself. The GUI can apply the sample as an individual UUID or an entity type. Sneak-use an emitter cycles its color. IDs accept `namespace:name`; registry tags use `#namespace:tag`. Entity scoreboard tags have a separate field. Invalid IDs/UUIDs are rejected. Lists, tag autocomplete and a dedicated dropped-item sampling interaction are not included in this demo.

## Power and rails

Normal operation consumes **2 FE per projected cell per tick**, with a 100,000 FE buffer and 10,000 FE/t transfer per emitter. Connected emitters share available power. Redstone controls operation and carries sensor output; it supplies no energy by default.

The included demo world has `serverconfig/fieldemitters-server.toml` with `demoRedstonePower = true`. Its GUI identifies this test mode. Other worlds retain the default `false` setting.

Perimeter posts link in four cardinal directions up to 20 blocks apart and follow terrain with a five-block field height. Rails mount on all six faces and link to an opposing rail up to 20 blocks away. Each rail contributes a full one-block-wide strip; adjacent rails share power and join without internal bright borders. Floor/ceiling pairs span vertically. Choose Field shape: Horizontal floor / ceiling for a horizontal plane when the span is horizontal. The menu offers only orientations compatible with the span.

A chained rail network consolidates its sensor counter and output at the source shown in Connections. Configure the chain together (the default rail GUI scope) for uniform filtering. Do not connect a perimeter post directly to a rail; they use separate projection geometry.

## Build and verification

```sh
./gradlew build --console=plain --max-workers=2
```

`/fielddemo verify` near a powered perimeter exercises actual server entity movement, all original category combinations, owner exemption, power-off release, projection timing, terrain coverage, age/inversion/identity/item predicates, completed crossings, queued pulses and presence. `/fielddemo verifyrails` near powered rails exercises baby-mob collision and one-way release across each nearby span. These development commands require operator permission.

Runtime evidence is in `docs/evidence/automation-v1/`. The optional Minecraft-Control addon is used only by development runs; it is not bundled in the mod JAR. Visuals include the existing detailed emitter/tuner models, animated channels, opening iris, hex field, and perimeter impact waves. Rails currently use a simpler projection animation and do not yet have the perimeter's impact-wave effect.

This remains a creative prototype: no survival recipes/balance, multi-client acceptance, or large-farm performance qualification yet. GUI layout is verified at the demo's default 427×240 GUI resolution; unusually narrow windows may require a smaller GUI scale.

## Connected field visuals

Posts and surface rails share a world-aligned hex pattern and expanding impact rings. Adjacent rail strips share impact position and timing across their plane, so the pattern and wave continue across strip boundaries. Impacts respect blocking filters and travel directions, including dropped/nonliving entities selected by the filter. Ambient-animation Off leaves impact reactions active.
