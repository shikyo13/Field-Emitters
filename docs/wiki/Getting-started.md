# Getting started

## Recipes

| Item | Pattern | Ingredients |
| --- | --- | --- |
| Field Emitter | `C A C` / `G R G` / `I I I` | C copper ingot, A amethyst shard, G glass, R block of redstone, I iron ingot |
| Field Rail (4) | `I C I` / `A R A` / `I C I` | I iron ingot, C copper ingot, A amethyst shard, R redstone dust |
| Field Tuner | `_ A _` / `C G C` / `I R I` | A amethyst shard, C copper ingot, G glass pane, I iron ingot, R redstone dust |

All three items are in the Field Emitters creative tab. Emitters and rails need a pickaxe to be picked up again. Breaking any section of an emitter post removes the whole post and drops one Field Emitter.

## Build a perimeter

1. Place a Field Emitter on the ground. It needs five free blocks of height and stands as a five-block post.
2. Place more emitters up to 20 blocks away to the north, south, east or west of the first one. Each post links to the nearest post in each of the four directions, so four posts make a rectangle and a row of posts makes a wall.
3. The field between two posts follows the terrain. It steps up or down with the ground, up to four blocks per step, and is five blocks high above the ground at every point. A link fails when the ground between the posts rises or drops more than four blocks in one step, or when the far post stands more than four blocks above or below the path.
4. Feed Forge Energy into any post with a cable, generator or energy cell from another mod. Field Emitters ships no generator, and redstone switches fields but never powers them. Connected posts pool their stored energy, so one input can run the whole perimeter.

The field switches on as soon as enough energy is stored for one tick of operation and projects outward along each link from the same end every time, so detection output and storage stay put no matter which post you feed. Turn the whole perimeter off from any post with **Field: Off** on the Power tab.

## Seal an opening

Field Rails cover doorways, pits and shafts. See [Rails](Rails.md).

## Choose what passes

Right-click a post or rail with an empty hand or with the Field Tuner to open its controls. The Blocking tab picks the categories the field stops; the Detection tab picks what it reports over redstone. See [Blocking filters](Filters.md) and [Detection and redstone](Detection.md).

The player who placed an emitter owns it. Other players cannot change its settings unless they are server operators. With the default filters the owner passes through their own fields.
