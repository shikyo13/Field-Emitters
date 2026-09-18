# Controls and the tuner

Right-click an emitter or rail with an empty hand or with the Field Tuner to open its controls. Settings apply as you change them. Buttons apply immediately; text fields apply after a short pause in typing or when you leave the field. Invalid IDs and UUIDs stay in the editor for correction and are not sent. Closing the screen does not undo applied changes.

Hover over any control for a description with examples.

## Tabs

- **Power:** switch the field on or off, read stored energy and demand, see the crossing counter and the most recent detection, reset the counter, and choose the redstone input face and mode (always on, on while the input is high, or on while it is low).
- **Blocking:** choose which entities the field stops. See [Blocking filters](Filters.md).
- **Detection:** choose which entities the field reports and how. See [Detection and redstone](Detection.md).
- **Appearance:** pick a color preset or enter a six-digit hex color, show or hide the field graphics, toggle the animated pattern, add or remove block light, and show or hide the direction guides on your client.
- **Connections:** select one outgoing link to give it its own blocking and detection rules, and choose the detection output face, the pulse length in seconds and the field shape for rail spans.

Connected emitters and rails form one group with one set of settings. Every change applies to all connected emitters you are allowed to edit, whichever block you opened, and a newly placed post or rail takes the group's settings when it links up. Only a link selected on the Connections tab keeps rules of its own.

Turning off world lighting on the Appearance tab keeps the field visible and solid without adding light, which suits dark mob farms. Hiding the field graphics keeps blocking and detection running.

## Per-link rules

On the Connections tab, **Editing** chooses the emitter's default rules or one outgoing link. A link's own blocking and detection filters take priority over the defaults and apply from both ends of that link. Energy, color and redstone settings always belong to the emitter.

## The Field Tuner

- **Right-click a post or rail** to open its controls.
- **Right-click the air** to open the Field Manager. Every connected, loaded chain you may edit in the current dimension appears as one field. Select a field to rename it or to open any of its emitters remotely. Names are stored on the emitters, and the oldest loaded member anchors the field's displayed location.
- **Sneak-right-click a mob or player** to sample it, or **sneak-right-click the air** to sample yourself. The Blocking and Detection tabs can then match that individual by UUID or its entity type.
- **Sneak-right-click a post or rail** to cycle the field color through the six presets.

While you hold the tuner, arrows on the field show the movement directions: amber where blocking is enabled, green where entities may pass. The Appearance tab or the client config can hide the guides.

Unloaded chunks are never loaded by the manager. A chain that is partly unloaded can appear as separate fields until its chunks load again.

Effect color defaults to **Matches field**, so changing the field color also changes its effects. Choose **Custom accent** to reveal separate presets and a hex color box. The **Purple field / magenta effects** preset enables a contrasting accent intentionally.
