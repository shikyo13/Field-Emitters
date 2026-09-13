# Plasma textures and independent formation

- Surface Pattern and Formation were already independent. Expanded the save/load game test to cover every tower projection, every planar formation, and all four patterns together.
- Flat plasma contours now use domain-warped world coordinates, a brighter core and layered glow. Selected planar projection presets also receive an interpolated translucent glow mesh, clipped by the selected formation. No entity particles are spawned.
- Plasma dissolve samples its reveal mesh every quarter block for smoother organic edges; other formation meshes retain their existing spacing.
- Core `check` and Field Emitters `build runCoreGameTestServer` passed. Logs: `/tmp/core-plasma-final.log`, `/tmp/fe-plasma-final-tests.log`.
- Local comparison: `docs/media/plasma-textures/index.html`. Four texture choices use Formation 8, purple field and magenta accent, on wall and bridge rails. Captured from the isolated `run-core-ui` world, 30 fps, silent, without HUD. These are development previews, not a published showcase.
- Glow mesh work is bounded by surface area; this is not a measured FPS benchmark. Minecraft rendering is not the same shader/bloom pipeline as the browser concept.
