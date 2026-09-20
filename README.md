# Field Emitters

Terrain-following forcefields for Minecraft. Place Field Emitter posts around an area and they link up into a five-block-high perimeter that follows the ground between them. Mount Field Rails on walls, floors and ceilings to seal doorways, pits and shafts. Every field decides what passes through it, reports what crossed it, and runs on energy supplied by other mods.

[Download](https://www.curseforge.com/minecraft/mc-mods/field-emitters) · [Wiki](https://github.com/shikyo13/Field-Emitters/tree/main/docs/wiki) · [Issues](https://github.com/shikyo13/Field-Emitters/issues) · [Discord](https://discord.gg/NrdXnbWzGC)

[![Field Emitters: terrain-following forcefields](docs/images/showcase-thumbnail.png)](https://youtu.be/_XEWAr7P7vQ)

**[Watch the feature showcase](https://youtu.be/_XEWAr7P7vQ)**

The showcase combines studio views of the shipped models with gameplay captures.

| Posts link, the field follows | Rails seal the openings |
|---|---|
| ![Four posts around a stepped hillside](docs/images/perimeter-day.png) | ![Rails sealing a doorway, a shaft and a walkway](docs/images/rails-doorway.png) |

## New in 1.1.0

Projection towers add hollow spheres and domes, and horizontal rails make walkable bridges. Choose the formation animation separately from the field texture, configure contact damage, issue passage badges, invite network managers, or set up an inventory checkpoint that drops or stores contraband. The tuner groups these controls into dedicated pages and applies changes immediately.

## Features

- Emitter posts link to other posts up to 20 blocks away in the four cardinal directions and project a field that climbs and descends with the terrain.
- Rails mount on any block face, span up to 20 blocks to an opposing rail, and join side by side into one wall, floor or ceiling.
- Blocking filters by category: hostile, passive, players, dropped items and other entities. Narrow a filter with age, entity type or tag, item, UUID or scoreboard tag, choose the movement directions it applies to, invert it, and exempt the owner.
- Detection output on a redstone face: one pulse per completed crossing, or a steady signal while a matching entity touches the field. Counts dropped items per stack or per item.
- Separate rules for each link, redstone enable modes, six color presets or any hex color, an animated field pattern, and optional block light.
- A handheld Field Tuner that opens controls, manages every loaded field from anywhere in the dimension, samples mobs and players for filters, and cycles colors.
- Energy input on every emitter and rail: Forge Energy on Forge/NeoForge and Team Reborn Energy on Fabric. Connected emitters pool their stored energy.

## Getting started

1. Craft a Field Emitter with copper, an amethyst shard, glass, a block of redstone and iron. Craft a Field Tuner with an amethyst shard, copper, a glass pane, iron and redstone.
2. Place emitters on level ground in a rectangle or a line, up to 20 blocks apart. A post needs five free blocks of height.
3. Feed energy into any post from a cable or generator. Connected posts share power, so one input runs the whole perimeter.
4. Right-click a post with an empty hand or the tuner to choose what the field blocks and what it reports.
5. Right-click the air with the tuner to open the Field Manager and reach any loaded field remotely.

Rails are crafted four at a time from iron, copper, amethyst and redstone. Place one on a wall, floor or ceiling and another one facing it up to 20 blocks away. Place more rails side by side to widen the field.

The [wiki](https://github.com/shikyo13/Field-Emitters/tree/main/docs/wiki) covers filters, detection output, rails, energy and troubleshooting.

## Energy

With default settings a running field consumes 2 FE per projected block each tick. A twenty-block perimeter side costs about 190 FE/t; a whole 20 by 16 perimeter costs about 680 FE/t. Each emitter and rail stores 100,000 FE and accepts up to 10,000 FE/t. Redstone controls operation and carries detection output but does not supply energy. The server config sets the cost, capacity and transfer rate. Zero cost allows free operation. Fabric uses the same numeric energy scale through Team Reborn Energy. In Fabric 1.20.1 the server settings are in `config/fieldemitters-server.toml`; the other builds use the world’s `serverconfig` directory.

## Compatibility

| Minecraft | Loader | Java |
| --- | --- | --- |
| 1.21.1 | NeoForge 21.1.206 or later in the 21.1 series | 21 |
| 1.21.1 | Forge 52.1.0 or later in the 52 series | 21 |
| 1.21.1 | Fabric Loader 0.16.14 or later | 21 |
| 1.20.1 | Forge 47.4.10 or later in the 47 series | 17 |
| 1.20.1 | Fabric Loader 0.16.14 or later | 17 |

Install Field Emitters on both the server and clients. **ZeroMods Core is bundled; no separate Core download is needed.** An energy source from another mod is needed; Field Emitters does not generate power or keep chunks loaded. Use the matching Minecraft and loader file. Fabric requires Fabric API and Forge Config API Port; Team Reborn Energy is bundled.

## Build

The current NeoForge branch requires JDK 21 and [ZeroMods Core](https://github.com/shikyo13/ZeroMods-Core) checked out beside this repository in a folder named `ZeroMods-Core`. Use the included Gradle wrapper:

```sh
./gradlew build
```

The source build expects a sibling `ZeroMods-Core` checkout. For Fabric, first build its matching adapter from that checkout with `./gradlew :fabric-1.21.1:build` or `./gradlew :fabric-1.20.1:build`, then build Field Emitters. The JAR is written to `build/libs/`. The NeoForge source is on `main`; ports use `mc/1.21.1-forge`, `mc/1.21.1-fabric`, `mc/1.20.1-forge` and `mc/1.20.1-fabric`. Recipes, loot tables and tags are committed under `src/generated/resources`. The 1.21.1 Forge/NeoForge branches regenerate them with `./gradlew runData`; other branches retain their version-specific converted data.

## Contributing and license

Bug reports, translations and contributions are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) and the [Field Emitters license](LICENSE).

You may play, research, contribute, redistribute unmodified official releases and include them in modpacks without asking. Retain the license and credit. Separately released modified builds, ports and feature variants require permission.

Support me through [Buy Me a Coffee](https://buymeacoffee.com/zerotheabsolute) or [Patreon](https://www.patreon.com/cw/ZeroTheAbsolute/membership).

## Translations

Want to translate Field Emitters? All English strings, including the Field Tuner menus and tooltips, are in [en_us.json](src/main/resources/assets/fieldemitters/lang/en_us.json). See the [translation guide](docs/LOCALIZATION.md) for language files, placeholders, and testing instructions.

See [Datapacks and KubeJS](docs/wiki/Automation.md) for field presets, reusable filters, server commands and scripting events.
