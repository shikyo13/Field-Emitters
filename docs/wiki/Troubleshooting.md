# Troubleshooting

## Two posts do not link

- Posts link only to the north, south, east and west, up to 20 blocks apart, and only to the nearest post in each direction.
- The ground between them must not rise or drop more than four blocks in a single step, and the far post must stand within four blocks of the path's height.
- A post needs a clear five-block column. Blocks inside the field path do not stop a link; the field runs over the terrain.
- Posts link only to posts and rails only to rails.

## The field is not running

1. Open the Power tab and read the stored energy and demand. A field needs enough energy for one tick of operation before it starts.
2. Confirm that the energy mod's cable or generator outputs on the side touching the emitter or rail.
3. Read the redstone mode. *On while high* waits for a signal and *On while low* stops while a signal is present.
4. Check that **Field** is *On* for every emitter in the chain. Any emitter switched off drops its links.

## Mobs walk through the field

- Check the Blocking tab: the category must be checked and any details must match. Details combine with AND.
- Check the direction boxes. An unchecked direction lets entities pass that way.
- A mount and its riders are checked together, and the field stops the group when it stops either one. A rule that blocks passive mobs therefore also stops a player riding a horse, unless that player has passage rights. The exempt owner, and a listed player or access badge holder while the filter lets listed players pass, take their mount through with them.

## A gap appears where the field crosses a slab or carpet

The field fills whole blocks. Where a slab, carpet, snow layer or fence stands in the field path, that block is already occupied and the field cannot fill it, leaving a partial gap at that spot. Clear the path or raise the posts so the field crosses full blocks or open air.
- In *Allow selected only* mode, everything outside the selection is blocked and the selection passes.
- The field projects outward from the powered post over a few ticks after it switches on.

## I cannot edit an emitter

The player who placed an emitter owns it. Other players need operator permission. The Field Manager lists only the fields you may edit.

## Detection does not pulse

A crossing counts only when the whole entity passes completely through the field. Contact and impacts do not count. Confirm the output face on the Connections tab is next to the redstone you expect, and that the input face is a different side.

## The field pattern is missing or dark

Ambient animation, field visibility and block light are separate options on the Appearance tab. Hidden graphics still block and detect.
