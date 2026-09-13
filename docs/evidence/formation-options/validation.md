# Formation options

Implemented and reviewed on NeoForge 1.21.1 with ZeroMods Core 13029e1.

- Core contract checks passed: bounded and monotonic shell reveals, stable identifiers, longitude continuity, direction of rising/crown reveals, and clipped plasma contours.
- Field Emitters build and required GameTests passed. Projection and texture combinations round-trip through settings, including defaults for existing saves. Hollow-shell collision and formation delay remain covered.
- Live Visuals menu cycled through all six projections while retaining Pattern: Plasma; server NBT confirmed each change immediately (`menu-roundtrip.json`). The menu fits the scaled QA window (`menu.png`).
- After restarting Minecraft and reopening the QA world, Plasma dissolve and Pattern: Plasma remained selected (`reloaded.json`).
- All six formation previews were recorded from the running Minecraft client at 30 fps, without HUD or audio. The fixed tessellation bounds sphere work independently of radius; this is not a many-field performance benchmark.
- Saved previews were recorded before an equivalent idle fast path was added to skip construction calculations after the field forms. The final build was relaunched for the persistence check.
- QA testing used temporary redstone power. The original FE configuration and tower presentation were restored afterward. No CurseForge profiles or public releases were changed.
