package com.zerotheabsolute.fieldemitters.client;

import com.zerotheabsolute.fieldemitters.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = FieldEmitters.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FieldClient {
  @SubscribeEvent
  public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
    event.enqueueWork(() -> FieldControls.remoteData = RemoteScreen::receive);
    event.enqueueWork(
        () ->
            FieldControls.open =
                e -> net.minecraft.client.Minecraft.getInstance().setScreen(new ControlScreen(e)));
  }

  @SubscribeEvent
  public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
    e.registerBlockEntityRenderer(FieldEmitters.EMITTER_BE.get(), FieldRenderer::new);
  }

  @SubscribeEvent
  public static void blockColors(
      net.minecraftforge.client.event.RegisterColorHandlersEvent.Block event) {
    event.register(
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

  @SubscribeEvent
  public static void itemColors(
      net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
    event.register(
        (stack, index) -> {
          if (index != 0) return -1;
          var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
          var tag = data == null ? new net.minecraft.nbt.CompoundTag() : data.copyTag();
          return 0xFF000000 | (tag.contains("FieldColor") ? tag.getInt("FieldColor") : 0x52E5FF);
        },
        FieldEmitters.TUNER.get(),
        FieldEmitters.EMITTER_ITEM.get(),
        FieldEmitters.RAIL_ITEM.get());
  }
}
