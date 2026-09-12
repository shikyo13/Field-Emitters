# Field Emitters showcase

Editable 30-second video and seven presentation images for the README and the project pages. The source imports the mod's actual Minecraft model geometry and textures, alongside ten unedited captures of the Field Emitters 1.0.0 release build.

The **ProjectIcon** composition is the square version of the showcase hero. It keeps the title, emitter post, tuner, colors and type from the poster. The master renders at 2048 × 2048 with double-resolution model canvases, and the exporter produces a 400 × 400 PNG for CurseForge and the largest lossless WebP that fits Modrinth's 256 KiB limit.

## Preview and export

From this directory, with Node.js 22 or newer and Python 3 with Pillow:

```sh
npm ci
npm run dev
```

Open the Studio URL printed by the command. Choose **FieldEmitters** for the complete video, or a composition in **Scenes** for an individual shot. The seven scene files keep timing, text, framing and model angles easy to edit.

```sh
npm run render
```

This writes `../../docs/media/field-emitters-showcase.mp4`: 1920 × 1080, 30 fps, 30 seconds, H.264. The mod ships no sound assets, so the video is silent. Rendering uses two workers. Remotion installs its own headless Chromium for rendering; it does not launch Minecraft or change any listing.

Export the presentation images and the icons with:

```sh
npm run stills
npm run icon
```

The icon step renders `docs/publishing/field-emitters-logo-master.png`, then `scripts/export-icons.py` writes `curseforge-icon.png`, `modrinth-icon.webp` and `logo-manifest.json` next to it.

Regenerate the output manifest after exports, with `ffprobe` available on `PATH`:

```sh
npm run manifest
```

## Timeline

| Time | Scene |
|---|---|
| 0:00–0:05 | Title with the turning emitter post and tuner |
| 0:05–0:10 | The tuner, with the emitter, rail and tuner side by side |
| 0:10–0:15 | The hillside perimeter, day to night |
| 0:15–0:20 | Rails sealing a doorway, a shaft and a walkway |
| 0:20–0:24 | The Blocking tab |
| 0:24–0:28 | The Detection tab |
| 0:28–0:30 | Closing post model and title |

Times are rounded; the transitions overlap neighboring scenes by twelve frames.

## Assets and provenance

`npm run prepare-assets` resolves the current model parents and copies the original textures and captures into the ignored `public/` and `src/generated/` folders. It records every input hash in `../../docs/media/sources.json`. The captures come from `docs/publishing/gallery/` and are copied byte for byte.

The Three.js views are studio renders of the shipped model assets, using the models' own UVs, the animated energy and display strips at their `.mcmeta` frame rates, and the default cyan field tint from the mod. The gameplay stills show the mod's separate field renderer and real interface. Do not present the studio lighting as an in-game result or these stills as newly recorded footage.

Barlow Condensed and IBM Plex Mono load through the pinned Remotion font package. The initial preview or render needs network access for those fonts and the rendering browser.

Model or texture changes require regenerating the video, stills and icons; new in-game visuals or interface changes require matching new captures. The media project stays outside Gradle's source sets and the shipping JAR.

## Implementation

- `src/scenes/`: one editable React component per scene.
- `src/Showcase.tsx`: timing and transitions.
- `src/ModelStage.tsx`: model geometry, face UVs, tint, animated strips, lighting and deterministic turns.
- `src/Design.tsx`: typography, framing, the capture frame and the three-piece lineup.
- `src/ProjectIcon.tsx`: the square icon composition.
- `scripts/prepare-assets.mjs`: copies original inputs and records provenance.
- `scripts/render-stills.mjs`: renders the seven presentation images.
- `scripts/export-icons.py`: exports the platform icons from the rendered master.
- `scripts/write-manifest.mjs`: records output checksums and dimensions.

The model stage follows [Remotion's Three.js integration](https://www.remotion.dev/docs/three); all motion is driven by composition frames. Project-authored code and Field Emitters assets use the repository's [license](../../LICENSE). Remotion and the other dependencies retain their own licenses; see the locked packages and [Remotion's license](https://github.com/remotion-dev/remotion/blob/main/LICENSE.md).
