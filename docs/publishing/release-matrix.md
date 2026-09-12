# Field Emitters release targets

The user requested parity with Flux Pylons on September 12, 2026. The local Flux Pylons README and port build properties establish this target matrix:

| Minecraft | Loader | Java | Field Emitters status |
| --- | --- | --- | --- |
| 1.21.1 | NeoForge 21.1.206+ (21.1 series) | 21 | 1.0.0 released; 1.0.1 energy fixes validated locally, not published |
| 1.20.1 | Forge 47.4.10+ (47 series) | 17 | Port required |
| 1.21.1 | Fabric Loader 0.16.14+ | 21 | Port required |
| 1.20.1 | Fabric Loader 0.16.14+ | 17 | Port required |
| 1.21.1 | Forge 52.1.0+ (52 series) | 21 | Port required |

Finish and validate the current fixes first. Then port in the order above, keeping registry IDs and save data compatible. Fabric requires a native energy adapter and explicit runtime dependency metadata; a renamed NeoForge JAR is not a port.

Before publishing each file, establish a clean build, client and dedicated-server startup, crafting and drops, actual loader-compatible energy input, both energy regression cases, blocking and sensor behavior, remote manager behavior, and connected impact visuals. Validate the GUI on the port, including a small window. Record the Minecraft version, loader, Java version, artifact hash and test evidence for each release file. Confirm platform metadata and downloadable artifact hashes after publishing.

Do not label any unbuilt or untested variant supported. Existing 1.0.0 assets remain immutable; publish fixes under a new patch version. The four port rows are planned work, not completed releases.
