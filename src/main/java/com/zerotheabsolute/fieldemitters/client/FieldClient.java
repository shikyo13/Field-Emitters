package com.zerotheabsolute.fieldemitters.client;

import com.zerotheabsolute.fieldemitters.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = FieldEmitters.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FieldClient {
  @SubscribeEvent
  public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
    event.enqueueWork(() -> FizzleNotice.receive = FizzleDeaths::receive);
    event.enqueueWork(() -> PlayerLookup.receive = result -> { PlayerListScreen.receive(result); ManagementScreen.lookup(result); });
    event.enqueueWork(() -> ManagementPackets.receive = ManagementScreen::receive);
    event.enqueueWork(() -> AccessPackets.receive = AccessScreen::receive);
    event.enqueueWork(() -> FieldControls.remoteData = RemoteScreen::receive);
    event.enqueueWork(
        () ->
            FieldControls.open =
                e -> net.minecraft.client.Minecraft.getInstance().setScreen(new ControlScreen(e)));
  }

  @SubscribeEvent
  public static void keys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) { event.register(TunerKeys.OPEN); }

  @SubscribeEvent
  public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
    e.registerBlockEntityRenderer(FieldEmitters.EMITTER_BE.get(), FieldRenderer::new);
  }

  @SubscribeEvent
  public static void blockColors(
      net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event) {
    event.register(
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

  @SubscribeEvent
  public static void itemColors(
      net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
    event.register(
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
