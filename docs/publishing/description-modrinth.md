<iframe width="560" height="315" src="https://www.youtube-nocookie.com/embed/HvTZJLOWlAM" title="Field Emitters showcase" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>

Field Emitters protects an area with glowing, terrain-following forcefields. Place emitter posts around your base, or mount Field Rails on walls, floors and ceilings to seal doorways, pits and shafts. Choose what passes through and use crossings to trigger redstone automation.

Feed energy into an emitter, then use the handheld Field Tuner to configure it. Connected, loaded emitters form one managed field and pool their stored energy, so one energy input can run the connected field.

## Getting started

1. Craft a Field Emitter and a Field Tuner. Each emitter post needs five free blocks of height.
2. Place posts up to 20 blocks apart. They link to the nearest post to the north, south, east or west and follow the terrain between them.
3. Supply energy from a cable or generator. The mod needs a power source from another mod.
4. Right-click an emitter with an empty hand or the tuner to choose what it blocks and detects. Changes apply immediately.
5. Right-click the air with the tuner to open the Field Manager and manage your loaded fields remotely.

Field Rails are crafted four at a time. Place one on a wall, floor or ceiling and another facing it up to 20 blocks away. Add rails side by side to widen the field.

## See what's happening

Impacts send a wave across the forcefield, continuing seamlessly across connected rails.

![Impact ripples spreading seamlessly across connected forcefields](https://raw.githubusercontent.com/shikyo13/Field-Emitters/v1.0.1/docs/media/impact-captures/impact-showcase.gif)

Use the Field Manager to find and rename your loaded fields, then open an emitter's controls remotely. The controls separate power, blocking, detection, appearance and connections.

## Set up each field

- Block hostile mobs, passive mobs, players, dropped items or other entities in any combination. Narrow the filter by age, entity type or tag, item, UUID or scoreboard tag.
- Choose which movement directions a rule applies to, invert the filter or exempt the owner. Each connection can have its own rules.
- Set a separate detection filter. Send one redstone pulse per completed crossing or a steady signal while something touches the field. Count dropped items per stack or per item, with a lifetime crossing counter.
- Pick one of six colors or enter a hex color. Turn block lighting off for dark builds and mob farms.
- Use redstone to enable, pause or gate the field. Detection output and redstone input use separate faces.

With default settings, a running field costs 2 FE per projected block each tick. Each emitter or rail stores 100,000 FE and accepts 10,000 FE/t. Fabric uses the same values in E and E/t through Team Reborn's Energy API. Server settings can change these values.

## Compatibility

Available for NeoForge 1.21.1, Forge 1.21.1 and 1.20.1, and Fabric 1.21.1 and 1.20.1. Install the matching build on both the server and clients. The Fabric build also needs [Fabric API](https://modrinth.com/mod/fabric-api) and [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port); Team Reborn Energy is bundled.

Fields work in loaded chunks. The mod does not keep chunks loaded.

## Modpacks

You may include Field Emitters in modpacks and redistribute unmodified official releases without asking. Keep the license and credit intact and link to an official project page.
