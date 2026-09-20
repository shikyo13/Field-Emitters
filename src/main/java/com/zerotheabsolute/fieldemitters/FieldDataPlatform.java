package com.zerotheabsolute.fieldemitters;
public final class FieldDataPlatform {
  private FieldDataPlatform() {}
  public static void register() {
    net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.server.packs.PackType.SERVER_DATA)
        .registerReloadListener(new Reload());
    net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> FieldCommands.register(dispatcher));
    net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> FieldPresets.clear());
  }
  private static final class Reload extends FieldPresets implements net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener {
    @Override public net.minecraft.resources.ResourceLocation getFabricId() { return new net.minecraft.resources.ResourceLocation("fieldemitters", "presets"); }
  }
}
