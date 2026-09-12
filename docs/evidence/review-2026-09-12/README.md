# Field Emitters 1.0.0 takeover review

Reviewed on September 12, 2026, at `3ec6e63076efbedc1fc9687e7ac1d7d6a0aad2a9`. Scope: Claude's changes after the imported 0.2.0 demo, plus gameplay regression and fresh impact captures. No production source, release asset, or CurseForge save was changed.

**Result: default gameplay passed the exercised scenarios, but two supported energy configurations fail.** The showcase tooling also has an ESLint configuration failure. These findings remain unfixed in the reviewed release.

## Confirmed findings

### Zero energy cost disables fields

`FieldConfig.java:23` accepts `energyPerCellTick = 0`. `FieldNetwork.java:178` requires `demand > 0` to activate a network, so zero-cost fields cannot run.

Reproduction: in the isolated QA world, establish a working three-strip rail doorway with demo redstone power enabled and a redstone block on its configured input side. Exit the world, set cost to zero, and reopen it. All rails retain valid links and are enabled, but report `Demand:0` and `Powered:0b`. The same doorway ran at the default cost. See [zero-cost.json](zero-cost.json).

The setting should either support free operation with valid projected links, or explicitly reject zero. Treating an accepted value as a silent off switch is misleading.

### Large energy buffers overflow the network total

`FieldNetwork.java:168-172` sums extractable energy into an `int`. Both capacity and transfer rate allow one billion FE, so three connected full buffers exceed the signed integer range.

Reproduction: exit the QA world, set capacity and transfer rate to `1000000000`, cost to `3`, and reopen. Use a straight chain of posts at x=50, 60, and 70, y=-49, z=10, without redstone power or a generator. Load each buffer with one billion FE using a test command. Two posts run, with demand 135 FE/t. Adding the third creates a valid second span, but all three turn off despite their nearly full buffers. Removing the third restores operation. Both the extraction limit and capacity must be raised to reproduce this case.

See [maximum-energy.json](maximum-energy.json) and [cache-after-removal.json](cache-after-removal.json). Use a wider accumulator or stop accumulating once demand is satisfied. This is a supported configuration edge case; it did not occur with default settings.

### Showcase lint and outdated roadmap

The showcase TypeScript check passes. `npx eslint src scripts` fails because Node globals (`Buffer`, `console`, `process`) are not declared for the `.mjs` scripts in the ESLint configuration. See [showcase-lint.log](showcase-lint.log). This does not establish a rendering failure; the existing exports passed media validation.

`ROADMAP.md:10` lists rail impact waves as future work, although they are already implemented and visible in the new capture.

## Validation performed

| Area | Result and evidence |
| --- | --- |
| Clean build and generated data | `clean build runData` passed on Java 21 / NeoForge 21.1.206. Generated data remained unchanged. [Build log](build.log). |
| Release reproducibility | Rebuilt JAR exactly matches the preserved published JAR: SHA-256 `029ba534a36c51c5cbe4d08769b4409fb156efba530ec3c46be149a6b9ff5fd9`. Metadata expands correctly; license and recipes are packaged; development controller and Mekanism are not bundled. |
| GitHub release and CI | Live GitHub API confirmed the public v1.0.0 release and matching asset digest. CI runs 34663054585 and 34656152923 succeeded. |
| Recipes | Crafted the emitter, four rails, and tuner through the normal crafting menu. Ingredients were consumed. Stained glass and stained panes exercised the common ingredient tags. [Crafting evidence](crafting.json). |
| Survival drops | Mining a middle post section with an iron pickaxe removed all five sections and yielded exactly one emitter. Mining a rail yielded one rail. [Post](survival-post.json), [rail](survival-rail.json). |
| Actual FE integration | A Mekanism creative energy cube supplied FE through the normal capability connection. A ten-block post span consumed 90 FE/t and its source held 99,910 FE while supplied. [FE input](fe-input.json). |
| Depletion | After removing input, 1,800 FE fell to 900 with the field on, then zero with the field off. Unfrozen wall-clock sampling; not an exact per-tick benchmark. [Timed depletion](fe-depletion-timed.json). |
| Nondefault cost and capacity | Cost 3 produced demand 135 instead of 90 for the same post span. Reloaded buffers accepted one billion FE. See maximum-energy evidence above. |
| Existing behavior | `/fielddemo verify` returned success (1), exercising masks, movement in both directions, owner exemption, power-off passage, projection and terrain, plus automation verification. `/fielddemo verifyrails` returned success (5) for the rail scenarios. Invocation script: [field-review-runtime.py](field-review-runtime.py). These are the project's existing in-game verifiers, not an independent exhaustive specification. |
| Network cache | Manager grouped six connected doorway rails into one field. Removing the third post from the energy-edge fixture restored the remaining pair after topology refresh. No removed member remained in that observed network. |
| Remote GUI | Opened manager using the tuner, selected a remote rail and visited all five control tabs. At 427×240 logical GUI size, all controls fit. Lighting changed on the server without a save action. [Tab states](gui-tabs.json), [instant apply](gui-autoapply.json), [Connections screenshot](gui-connections.png). |
| Dedicated server | Standalone Field Emitters server reached `Done` on localhost:25579 without client-class loading failure. Stopped with SIGTERM; the log confirms world saving. Gradle's final exit 143 is the deliberate termination, not a startup failure. [Server log](dedicated-server.log). |
| Gallery and showcase | All 17 local gallery/presentation hashes matched their records; showcase export hashes matched. Existing video is 1920×1080, 30 fps, 30 seconds, with no audio track. Poster and representative video frame inspected. [Gallery hashes](gallery-hashes.json), [artifact checks](artifact-checks.json). |

## Impact media

Fresh native gameplay captures show a zombie impact on a post field and a ripple crossing both seams of a three-strip rail doorway. The camera HUD is hidden. Each recorded frame advances one game tick; video assembly uses 20 fps. No synthetic frames or generated imagery. The clip is silent.

- [Combined MP4, 1708×960, 6.4 seconds](../../media/impact-captures/impact-showcase.mp4)
- [Looping GIF, 854×480](../../media/impact-captures/impact-showcase.gif)
- [Post impact screenshot](../../media/impact-captures/perimeter-impact-11.png)
- [Rail impact screenshot](../../media/impact-captures/doorway-impact-11.png)
- [Media hashes and provenance](../../media/impact-captures/manifest.json)

## Limits and cleanup

This does not establish two-client multiplayer behavior, compatibility with every mod, large-network performance, every GUI scale/window size, or live CurseForge/Modrinth approval status. The creative tab registration and item entries were reviewed in source; its inventory navigation was not separately exercised. Asset manifests establish local file integrity, not current third-party gallery contents.

The isolated `run-menu` client and temporary dedicated server were stopped. The original QA server configuration was restored. The user's CurseForge profile still contains the older demo JAR and was not upgraded by this test. Raw capture frames and discarded takes are retained in the ignored QA directory, outside the deliverable media folder. The earlier `fe-depletion.json` used Minecraft tick freeze, which does not halt this mod's energy event; use `fe-depletion-timed.json` for the valid depletion observation.
