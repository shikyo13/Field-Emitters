# Planar formation presets

Validated locally with NeoForge 1.21.1.

- Core checks pass for all six presets: monotonic reveal, every edge complete at the end, matching alpha on shared strip boundaries, finite and bounded guides, and positive/negative source directions on vertical and horizontal frames.
- Field Emitters build and required GameTests pass. New presets persist and defer collision until tick 80 in all three normal planes, including vertical rail links. Legacy activation timing remains unchanged.
- Live menu cycling confirmed all nine choices apply to server settings immediately (`menu.json`). The scaled menu fits its viewport (`menu.png`).
- A survival player walked from x=401.5 to x=407.834 on a completed bridge, remaining on the ground at y=-55.5 throughout (`bridge-walk.json`).
- Recorded all six presets on terrain-following posts, side-mounted wall rails, horizontal bridge rails, and vertically linked floor/ceiling rails. MP4 previews are actual in-game captures, 30 fps, silent, without HUD overlays. Capture stages and cameras are in `scenes.json`.
- Startup footage predates two final shutdown refinements: adjacent inactive rails keep a shared frame, and unpowered rail members do not continue sensor monitoring. The final build and GameTests include these refinements.
- The QA scenes remain in the isolated development world. Temporary testing configuration was restored; no CurseForge installation or public release was changed.
