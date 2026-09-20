package com.zerotheabsolute.fieldemitters;
public final class FieldDataPlatform {
  private FieldDataPlatform() {}
  public static void register() {
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.AddReloadListenerEvent event) -> event.addListener(new FieldPresets()));
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.RegisterCommandsEvent event) -> FieldCommands.register(event.getDispatcher()));
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.server.ServerStoppedEvent event) -> FieldPresets.clear());
  }
}
