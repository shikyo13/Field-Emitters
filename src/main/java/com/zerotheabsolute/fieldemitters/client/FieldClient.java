package com.zerotheabsolute.fieldemitters.client;

import com.zerotheabsolute.fieldemitters.*;

public final class FieldClient implements net.fabricmc.api.ClientModInitializer {
  private static void run(Runnable action) { action.run(); }
  @Override public void onInitializeClient() {
    com.zerotheabsolute.fieldemitters.network.NativeNetwork.initClient();
    renderers(); blockColors(); itemColors();
    run(() -> FieldControls.remoteData = RemoteScreen::receive);
    run(
        () ->
            FieldControls.open =
                e -> net.minecraft.client.Minecraft.getInstance().setScreen(new ControlScreen(e)));
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
        FieldEmitters.EMITTER.get(),
        FieldEmitters.RAIL.get());
  }

  public static void itemColors() {
    net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.ITEM.register(
        (stack, index) -> {
          if (index != 0) return -1;
          var data = stack.getTag();
          var tag = data == null ? new net.minecraft.nbt.CompoundTag() : data.copy();
          return 0xFF000000 | (tag.contains("FieldColor") ? tag.getInt("FieldColor") : 0x52E5FF);
        },
        FieldEmitters.TUNER.get(),
        FieldEmitters.EMITTER_ITEM.get(),
        FieldEmitters.RAIL_ITEM.get());
  }
}
