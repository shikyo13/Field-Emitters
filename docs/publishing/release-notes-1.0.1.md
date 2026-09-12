# Field Emitters 1.0.1

- Added Forge and Fabric builds for Minecraft 1.21.1 and 1.20.1, alongside NeoForge 1.21.1.
- Fixed zero FE cost disabling connected forcefields. Zero cost now allows free operation; unlinked emitters remain idle.
- Fixed large energy buffers overflowing the network total and shutting down powered fields.
- Fixed the demo builder's redstone input face so its lever powers the demonstration.

Install the file matching your Minecraft version and loader on both client and server. Fabric requires Fabric API and Forge Config API Port; Team Reborn Energy is bundled. Java 21 is required for 1.21.1; Java 17 for 1.20.1.
