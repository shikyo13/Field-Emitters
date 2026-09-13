package com.zerotheabsolute.fieldemitters.client;

import com.zerotheabsolute.fieldemitters.*;

public final class FieldClient implements net.fabricmc.api.ClientModInitializer {
  private static void run(Runnable action) { action.run(); }
  @Override public void onInitializeClient() {
    net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback.EVENT.register(context ->
        context.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("zeromodscore", "energy_surface"), com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY,
            com.zeromods.core.client.EnergyRenderTypes::surfaceShader));
    com.zerotheabsolute.fieldemitters.network.NativeNetwork.initClient();
    renderers(); blockColors(); itemColors();
    run(() -> FizzleNotice.receive = FizzleDeaths::receive);
    run(() -> PlayerLookup.receive = result -> { PlayerListScreen.receive(result); ManagementScreen.lookup(result); });
    run(() -> ManagementPackets.receive = ManagementScreen::receive);
    run(() -> AccessPackets.receive = AccessScreen::receive);
    run(() -> FieldControls.remoteData = RemoteScreen::receive);
    run(
        () ->
            FieldControls.open =
                e -> {
                  var mc = net.minecraft.client.Minecraft.getInstance();
                  if (mc.player == null) return;
                  if (FieldControls.editable(e, mc.player)) mc.setScreen(new ControlScreen(e));
                  else mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                      "This field is private. Ask its owner for management access."), true);
                });
    net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(TunerKeys.OPEN);
    net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(TunerKeys::tick);
    net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> FizzleDeaths.tick());
  }

  public static void renderers() {
    net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(FieldEmitters.EMITTER_BE.get(), FieldRenderer::new);
  }

  public static void blockColors() {
    net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register(
        (state, level, pos, index) -> {
          if (index != 0 || level == null || pos == null) return -1;
          var base = EmitterBlock.base(pos, state);
          if (level.getBlockEntity(base) instanceof EmitterEntity emitter)
            return 0xFF000000 | emitter.color;
          return 0xFF52E5FF;
        },
        FieldEmitters.TOWER.get(),
        FieldEmitters.EMITTER.get(),
        FieldEmitters.RAIL.get());
  }

  public static void itemColors() {
    net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.ITEM.register(
        (stack, index) -> {
          if (index != 0) return -1;
          var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
          var tag = data == null ? new net.minecraft.nbt.CompoundTag() : data.copyTag();
          return 0xFF000000 | (tag.contains("FieldColor") ? tag.getInt("FieldColor") : 0x52E5FF);
        },
        FieldEmitters.TOWER_ITEM.get(),
        FieldEmitters.TUNER.get(),
        FieldEmitters.EMITTER_ITEM.get(),
        FieldEmitters.RAIL_ITEM.get());
  }
}
