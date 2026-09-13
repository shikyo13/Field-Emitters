# Access, checkpoints and projection towers

These features are in the NeoForge 1.21.1 development build. They are not in the published 1.0.2 release or the other loader builds yet. The development checkout requires the sibling ZeroMods-Core project.

## Who can manage a field?

New networks are private. Open **Power → Network management access** to add a Minecraft account name or UUID, remove a manager, or enable public management. Account names resolve to UUIDs; server nicknames are not account names.

Managers can change field settings. Only the owner and server operators can change the management list or public/private setting. Public management lets any player configure the field. Passage badges never grant management rights. Emitters owned by different players do not automatically join the same network.

This protects configuration, not the surrounding land. Emitters remain mineable at their previous hardness. Posts, surface rails and projection towers resist explosions and mob destruction.

## Passage badges

Open **Power → Access badges**, hold a blank access badge in either hand, choose a group and issue it. A blank player UUID makes the badge transferable; supplying a UUID binds it to that player. Group names belong to the issuing player: another player issuing a badge called `staff` cannot grant access to your `staff` fields.

In a player filter, open **Access groups / badges** and enter the groups that rule should match. Enable the corresponding player-list mode. A listed player or a matching badge group counts as a match; movement restrictions and Skip owner still apply. Opposite directions can use different rules.

A badge holder carries 16 badges. In the inventory, right-click the holder with a badge on the cursor to insert it, or with an empty cursor to remove a badge. Carry the holder normally or equip it in a Curios slot. Cosmetic Curios slots do not grant access. Revoking a group invalidates existing badges in that group, including badges inside holders; it does not delete the physical items.

## Tuner keybind

The **Open Field Tuner** binding is available in Controls under Field Emitters and is unbound by default. With a tuner in your inventory or an actual Curios slot, the key opens the emitter you are looking at, or the remote field list when you are looking elsewhere. Server permission checks still apply. Curios is optional; normal inventory use works without it.

## Inventory checkpoints

Open **Power → Inventory checkpoint**. The checkpoint has its own player/badge selection and directional overrides. Its contraband item list supports IDs and tags, with listed-items or all-except-listed-items modes.

The available actions can be combined:

- Emit a redstone alert on first contact with contraband.
- Hold the player while they carry contraband.
- Drop matching items on the entry side.
- Send matching items to an inventory adjacent to that emitter, on the selected storage side.

Without overflow drops, storage checkpoints hold the player until the remaining items can be transferred. Missing or full storage leaves those items in the player's inventory. With overflow drops enabled, leftovers drop on the entry side. Item components, including custom names, are preserved. The scanner examines the player's ordinary inventory, armor and offhand; it does not recursively inspect containers or confiscate Curios contents.

Alerts share the emitter's normal redstone output and pulse-duration setting. They work with the normal sensor disabled or in counting/occupancy modes. An occupancy signal already holding the output high will keep it high; it cannot provide a separate electrical pulse edge at the same time. Stacked rails share their root's contact record and output, avoiding repeated alerts from adjacent rail sections.

## Projection towers

A projection tower occupies seven blocks vertically. Place it where all seven spaces are clear. Its GUI offers dome or full-sphere geometry and a radius from 8 to 24 blocks. A full sphere extends below the base; the entire sphere must fit within world height and loaded chunks. The tower never force-loads chunks.

In **Visuals → Projection**, choose one of six four-second startup animations:

- **Laser curtain:** two opposing fans accelerate around the shell, leaving the surface behind.
- **Rising scan ring:** a bright ring climbs from the lower rim to the crown.
- **Hex assembly:** staggered cells build upward from the base.
- **Meridian sweep:** luminous arcs trace the shell while the surface fills between them.
- **Projected seed:** a focused beam seeds the crown, then the surface spreads downward.
- **Plasma dissolve:** irregular openings close with luminous edges.

Projection is independent of **Pattern**: every animation works with hex lattice, smooth glow, drifting pixels, or plasma. Plasma now uses a translucent surface with moving luminous contours, rather than filled grid patches. The particle color controls those contours. Existing towers default to Laser curtain. Existing posts and rails retain their saved Sweep, Dissolve, or Fade setting. Turning pattern animation off uses a simple fade without moving construction guides.

Only the outer shell has collision; the interior and dome floor stay open. Projection beams are visual only. Collision starts when formation completes and stops immediately when power is lost; the visible shell fades out. All surface styles retain curved impact ripples. Mesh resolution and pattern coordinates are fixed to bound rendering work as the radius grows. Tower hardware currently reuses the existing emitter materials.

The tower accepts FE through its block energy capability. At the default energy setting, cost is approximately two FE per square block of shell surface per tick: `2 × π × radius²` surface area for a dome, `4 × π × radius²` for a sphere. Stored FE can be consumed above the external cable transfer limit; the full cost is deducted. Redstone-only power remains an explicit testing configuration, disabled by default.


## Post, wall, floor and ceiling formation

Posts and surface rails offer the same six effects in **Visuals → Formation**, after the original Sweep, Dissolve / reform, and Fade choices. Surface pattern and colors remain independent.

| Effect | Flat-field behavior |
| --- | --- |
| Laser curtain | A fan projects from the source edge and accelerates toward the other emitter. |
| Scan line | The ring becomes a straight line: upward on walls, across floor and ceiling fields. |
| Hex assembly | Cells spread from the emitting edge across the surface. |
| Tracing ribs | Meridians become parallel lines tracing the panel before it fills in. |
| Projected seed | A beam seeds the center; a luminous rectangle expands to the edges. |
| Plasma dissolve | Smooth openings close across the plane with bright edges. |

Post fields follow the terrain as they form. Coplanar adjacent rails with matching visuals use one shared animation frame, so a wide doorway or bridge is not treated as a stack of separate one-block animations. Textures and plasma contours remain anchored to the world plane.

The six new effects take four seconds. Blocking, detection, contact damage and inventory checkpoints start when construction finishes; losing power disables them immediately. The three original choices retain their existing progressive activation. With animation disabled, a new formation uses a quiet fade. Invisible fields still follow their selected activation delay.
