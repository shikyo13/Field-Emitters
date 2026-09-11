# Release 1.0.0 verification

Build: `./gradlew build` and `./gradlew runData` on JDK 21 with NeoForge 21.1.206. Runtime checks ran in the development client with the Automation Demo world, Mekanism 10.7.17 for energy input, and the Minecraft-Control addon for input. The addon is not part of the mod.

| Check | Result |
| --- | --- |
| `/fielddemo verify` beside the perimeter | Passed: server-side movement for every category mask, owner exemption, power-off release, projection timing, terrain columns and the automation checks. |
| `/fielddemo verifyrails` beside the rail spans | Passed for 5 spans: baby-mob collision and one-way release in both directions. |
| Survival drops | Breaking the middle section of a placed emitter with an iron pickaxe removed all five sections and dropped one Field Emitter. Breaking a placed rail dropped one Field Rail. |
| Recipes | Crafted one Field Emitter, one Field Tuner and four Field Rails in a crafting table from the documented patterns. |
| Forge Energy | A Mekanism creative energy cube filled an emitter to 99,910 FE and started the field at 90 FE/t for a ten-block link. Removing the cube drained the buffer at 90 FE/t and the field stopped when it ran out. |
| Interface | Every control tab and the Field Manager opened and rendered at GUI scale 4 in a 1708 by 960 window. |

Screenshots in `docs/publishing/gallery` are unedited captures from this session.

Release JAR `field-emitters-1.0.0.jar` SHA-256: `029ba534a36c51c5cbe4d08769b4409fb156efba530ec3c46be149a6b9ff5fd9`.
