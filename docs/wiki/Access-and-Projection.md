# Access, checkpoints and projection towers

## Who can manage a field?

New networks are private. Open **Access → Field management** to add a Minecraft account name or UUID, remove a manager, or enable public management. Account names resolve to UUIDs; server nicknames are not account names.

Managers can change field settings. Only the owner and server operators can change the management list or public/private setting. Public management lets any player configure the field. Passage cards never grant management rights. Emitters owned by different players do not automatically join the same network.

This protects configuration, not the surrounding land. Emitters remain mineable at their previous hardness. Posts, surface rails and projection towers resist explosions and mob destruction.

## Access cards

Open **Access → Access cards**, turn on **Allow card holders through**, and add a group to **Allowed card groups**. Only cards issued by the field owner count.

To issue one, hold a blank Access Card in either hand, click **Issue card**, enter its group, and choose who may use it. **Anyone carrying it** makes it transferable. Choosing a player binds it to that player. **Allow this group through** adds the group to this field when you issue the card.

Cards let players bypass blocking. Damage and inventory checks still apply. A player specifically named in a blocking blacklist is still stopped, including while riding a vehicle.

**Revoke cards** invalidates the cards you issued for that group on every field that accepts them. It does not delete the items. Newly issued cards for the group work again.

A Card Holder carries 16 cards. In your inventory, right-click the holder with a card on the cursor to insert it, or with an empty cursor to remove one. Carry it normally or use a supported Curios slot. Cosmetic slots do not grant access.

Player filters also offer **Access groups / cards**. Those groups match cards as part of the player filter; they are separate from the passage setting above. Sensor, Damage and Inventory checkpoint can each use their own player filter.

## Tuner keybind

The **Open Field Tuner** binding is available in Controls under Field Emitters and is unbound by default. With a tuner in your inventory or an actual Curios slot, the key opens the emitter you are looking at, or the remote field list when you are looking elsewhere. Server permission checks still apply. Curios is optional; normal inventory use works without it.

## Inventory checkpoints

Open **Access → Inventory checkpoint**. The checkpoint has its own player/card selection and directional overrides. Its contraband item list supports IDs and tags, with listed-items or all-except-listed-items modes.

The available actions can be combined:

- Emit a redstone alert on first contact with contraband.
- Hold the player while they carry contraband.
- Drop matching items on the entry side.
- Send matching items to an inventory adjacent to that emitter, on the selected storage side.

Without overflow drops, storage checkpoints hold the player until the remaining items can be transferred. Missing or full storage leaves those items in the player's inventory. With overflow drops enabled, leftovers drop on the entry side. Item components, including custom names, are preserved. The scanner examines the player's ordinary inventory, armor and offhand; it does not recursively inspect containers or confiscate Curios contents.

Alerts share the emitter's normal redstone output and pulse-duration setting. They work with the normal sensor disabled or in counting/occupancy modes. An occupancy signal already holding the output high will keep it high; it cannot provide a separate electrical pulse edge at the same time. Stacked rails share their root's contact record and output, avoiding repeated alerts from adjacent rail sections.

## Projection towers

A projection tower occupies seven blocks vertically. Place it where all seven spaces are clear. Its GUI offers dome or full-sphere geometry and a radius from 8 to 24 blocks. Domes extend up to four blocks below the base, meeting solid terrain within that reach. A full sphere extends below the base; the entire sphere must fit within world height and loaded chunks. The tower never force-loads chunks.

In **Appearance → Projection**, choose one of six four-second startup animations:

- **Laser curtain:** two opposing fans accelerate around the shell, leaving the surface behind.
- **Rising scan ring:** a bright ring climbs from the lower rim to the crown.
- **Hex assembly:** staggered cells build upward from the base.
- **Meridian sweep:** luminous arcs trace the shell while the surface fills between them.
- **Projected seed:** a focused beam seeds the crown, then the surface spreads downward.
- **Plasma dissolve:** irregular openings close with luminous edges.

Projection is independent of **Pattern**: every animation works with hex lattice, smooth glow, drifting pixels, or plasma. Effects follow the field color by default. In Appearance, choose **Effect color: Separate color** to give impacts, pixels, plasma veins, and fizzle effects a separate color. Formation beams keep the field color. Existing towers default to Laser curtain. Existing posts and rails retain their saved Sweep, Dissolve, or Fade setting. Turning pattern animation off uses a simple fade without moving construction guides.

Only the outer shell has collision; the interior and dome floor stay open. Projection beams are visual only. Collision starts when formation completes and stops immediately when power is lost; the visible shell fades out. All surface styles retain curved impact ripples.

The tower accepts FE through its block energy capability. At the default energy setting, cost is approximately two FE per square block of shell surface per tick: `2 × π × radius² + 2 × π × radius × 4` surface area for a dome, `4 × π × radius²` for a sphere. Stored FE can be consumed above the external cable transfer limit; the full cost is deducted. Redstone controls operation but does not supply energy.


## Post, wall, floor and ceiling formation

Posts and surface rails offer the same six effects in **Appearance → Formation**, after the original Sweep, Dissolve / reform, and Fade choices. Surface pattern and colors remain independent.

| Effect | Flat-field behavior |
| --- | --- |
| Laser curtain | A fan projects from the source edge and accelerates toward the other emitter. |
| Scan line | The ring becomes a straight line: upward on walls, across floor and ceiling fields. |
| Hex assembly | Cells spread from the emitting edge across the surface. |
| Tracing ribs | Meridians become parallel lines tracing the panel before it fills in. |
| Projected seed | A beam seeds the center; a luminous rectangle expands to the edges. |
| Plasma dissolve | Smooth openings close across the plane with bright edges. |

Post fields follow the terrain as they form. Coplanar adjacent rails with matching visuals use one shared animation frame, so a wide doorway or bridge is not treated as a stack of separate one-block animations. Textures and plasma contours share coordinates across the connected field. Horizontal rail fields meet the top of their block space, keeping bridges level with adjacent full blocks. Simultaneous contacts create separate ripples, with brighter intersections on walls and bridges.

These six effects take four seconds. Blocking, detection, contact damage and inventory checkpoints start when construction finishes; losing power disables them immediately. The three original choices retain their existing progressive activation. With animation disabled, a new formation uses a fade. Invisible fields still follow their selected activation delay.
