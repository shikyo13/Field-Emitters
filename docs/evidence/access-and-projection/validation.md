# Access and projection development validation

Date: 2026-09-12. Source checkpoint: Field Emitters `41568e1`, ZeroMods Core `d6f1c92`.

`build runCoreGameTestServer` passed on Java 21 / NeoForge 1.21.1. Tests cover issuer isolation, player-bound credentials, revocation, holder transfers, private/public management and manager removal, item components and full-storage overflow, checkpoint pulse modes, directional settings, curved collision contact, formation timing, energy consumption above the cable transfer limit, and hardware mining/blast properties. ZeroMods Core's `check :neoforge-1.21.1:build` also passed.

Live checks used the isolated **ZeroMods Core QA** world and Curios 9.5.1:

- With no tuner in ordinary inventory, the tuner equipped in Curios opened the remote manager using the configured K key. K is a QA profile choice, not the shipped default.
- Issuing a held badge produced its server credential and an inline GUI confirmation.
- Adding a UUID manager displayed the row; removing it removed the membership. The field remained private.
- A sphere checkpoint moved exactly 11 diamonds named `Checkpoint test` into its adjacent chest. The chest NBT retained the count and custom name. After transfer, forward movement carried the player through the boundary.
- A creeper explosion destroyed nearby dirt while the post and rail remained in place. Tower resistance uses the same protection hooks and is covered by the hardware test; it was not separately subjected to a live explosion.
- A radius-12 dome formed with sweeping projection fans. A radius-8 full sphere formed around an elevated tower and created lower-hemisphere field cells.
- The new menus fit at GUI scale 4. Manager entry has a visible label, and help wraps above the footer.

The GIF is a sampled, silent local preview, approximately 10 fps. It is not a release showcase or a frame-time benchmark. One idle runtime sample with the dome and nearby sphere loaded showed 55 fps with a 60 fps cap and a 0.7 ms integrated-server tick on an Apple M4 Max. This is not a claim about large servers or maximum-radius stress performance.

Runtime projection checks used the explicit redstone demo-power setting. The profile's original configuration was restored after the run. Normal FE consumption is covered by the energy tests; no external generator/cable mod was exercised in this pass.

Not yet established: a real multi-client multiplayer session, maximum-radius stress profiling, or feature parity on the other Minecraft/loader versions. Nothing in this development pass was uploaded to mod hosts or installed into a CurseForge profile.
