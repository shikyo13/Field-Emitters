package com.zerotheabsolute.fieldemitters;
public final class FieldDataPlatform {
  private FieldDataPlatform() {}
  public static void register() {
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.AddReloadListenerEvent event) -> event.addListener(new FieldPresets()));
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent event) -> FieldCommands.register(event.getDispatcher()));
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event) -> FieldPresets.clear());
  }
}
