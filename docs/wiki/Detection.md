# Detection and redstone

The Detection tab reports entities over redstone. Its filter is independent of the Blocking filter and uses the same categories, details, directions and owner option. *Detect selected* reports matching entities; *Detect unselected* reports everything outside the selection.

## Signal modes

- **Off:** no output.
- **Pulse on crossing:** one redstone pulse each time a matching entity passes completely through the field. Pulses last the configured duration and are separated by two ticks. Up to 100,000 pulses queue while the output is busy.
- **On while touching:** the output stays on while a matching entity overlaps the field.

A crossing counts only after the whole entity has cleared the far side. Touching the field, bouncing off it or walking along it does not count. The Power tab shows the lifetime crossing counter and the most recent detection, and can reset both.

## Items

With the Drops category selected, **Count each stack** sends one pulse per item stack and **Count each item** sends one pulse per item in the stack, delivered one at a time. Mobs and players always count once.

## Output and input faces

The Connections tab chooses the detection output face. The Power tab chooses the redstone input face and mode:

- **Always:** the field runs whenever it has energy.
- **On while high:** the field runs only while the input face receives a signal.
- **On while low:** the field runs only while the input face receives no signal.

Input and output use different faces. Top and bottom refer to the emitter block itself; north, south, east and west are world directions. Redstone never supplies energy; a field with no stored energy stays off in every mode.

Rails in one connected chain report through the chain's source rail, shown on the Connections tab, so a wide doorway made of several rails gives one counter and one output.
