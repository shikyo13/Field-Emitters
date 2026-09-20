# Detection and redstone

The Detection tab reports entities over redstone. Like blocking, its directions can be written in world terms or relative to the area the posts enclose; see [Blocking filters](Filters.md). Its filter is independent of the Blocking filter and uses the same categories, details, directions and owner option. *Detect selected* reports matching entities; *Detect unselected* reports everything outside the selection.

## Signal modes

- **Off:** no output.
- **Pulse on crossing:** a matching crossing starts a redstone pulse immediately. Another crossing while the output is on refreshes the pulse duration; simultaneous crossings share one pulse. Pulses never queue or replay afterward. The lifetime counter still records each crossing, or each item when item counting is enabled. A dropped stack triggers one detection regardless of stack size.
- **On while touching:** the output stays on while a matching entity overlaps the field.

A crossing counts only after the whole entity has cleared the far side. Touching the field, bouncing off it or walking along it does not count. The Power tab shows the lifetime crossing counter and the most recent detection, and can reset both.

## Items

With the Drops category selected, **Count each stack** sends one pulse per item stack and **Count each item** sends one pulse per item in the stack, delivered one at a time. Mobs and players always count once.

## Output and input faces

The Connections tab chooses the detection output face. The Power tab chooses the redstone input face and mode:

- **Always (while powered):** the field runs whenever it has energy.
- **Only while redstone is ON:** the group runs while any connected emitter or rail receives a signal.
- **Only while redstone is OFF:** the group runs only while none of its connected emitters or rails receives a signal.

The input reads every side except the output face by default. Choose a single side with **Read redstone from** when a neighboring circuit should be ignored. Top and bottom refer to the emitter block itself; north, south, east and west are world directions. One redstone input controls the entire connected group. Redstone never supplies energy: connect an FE source to any member. A server configured with `energyPerCellTick = 0` does not require FE.

Rails in one connected chain report through the chain's source rail, shown on the Connections tab, so a wide doorway made of several rails gives one counter and one output.
