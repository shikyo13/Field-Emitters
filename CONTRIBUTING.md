# Contributing to Field Emitters

Bug reports, translations, visual improvements and code contributions are welcome. Discuss larger changes in an [issue](https://github.com/shikyo13/Field-Emitters/issues) or on [Discord](https://discord.gg/NrdXnbWzGC) before starting.

For a bug, include the Minecraft version, NeoForge version, Field Emitters version, the modpack or power source involved, reproduction steps and the expected result. Remove private information from logs before sharing them.

Base contributions on `main`. Preserve the existing `fieldemitters` registry and save IDs so players can keep their worlds. Regenerate `src/generated/resources` with `./gradlew runData` when you change recipes, loot tables or tags, and commit the result. Test the behavior affected by your change and describe the result in the pull request. Build instructions are in the README.

The [license](LICENSE) permits private research and contributions, including clearly labeled source-only contribution forks. It does not permit publishing a separate mod, port, feature variant or fork binary without permission. Contributors retain ownership of their original work and grant the maintainers permission to include it in the official project under the project license. Only submit work you have the right to contribute.

## Repository scope

Keep mod source, runtime assets, build files, and documentation here. Keep testing, demo, recording, and publishing tooling in an external workspace.
