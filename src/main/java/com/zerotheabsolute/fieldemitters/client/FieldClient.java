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
    event.enqueueWork(() -> FizzleNotice.receive = FizzleDeaths::receive);
    event.enqueueWork(() -> PlayerLookup.receive = result -> { PlayerListScreen.receive(result); ManagementScreen.lookup(result); });
    event.enqueueWork(() -> ManagementPackets.receive = ManagementScreen::receive);
    event.enqueueWork(() -> AccessPackets.receive = AccessScreen::receive);
    event.enqueueWork(() -> FieldControls.remoteData = RemoteScreen::receive);
    event.enqueueWork(
        () ->
            FieldControls.open =
                e -> {
                  var mc = net.minecraft.client.Minecraft.getInstance();
                  if (mc.player == null) return;
                  if (FieldControls.editable(e, mc.player)) mc.setScreen(new ControlScreen(e));
                  else mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                      "This field is private. Ask its owner for management access."), true);
                });
  }

  @SubscribeEvent
  public static void keys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) { event.register(TunerKeys.OPEN); }

  @SubscribeEvent
  public static void shaders(net.minecraftforge.client.event.RegisterShadersEvent event) throws java.io.IOException {
    event.registerShader(new net.minecraft.client.renderer.ShaderInstance(event.getResourceProvider(),
        new net.minecraft.resources.ResourceLocation("zeromodscore", "energy_surface"), com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY),
        com.zeromods.core.client.EnergyRenderTypes::surfaceShader);
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
        FieldEmitters.TOWER.get(),
        FieldEmitters.EMITTER.get(),
        FieldEmitters.RAIL.get());
  }

  @SubscribeEvent
  public static void itemColors(
      net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
    event.register(
        (stack, index) -> {
          if (index != 0) return -1;
          var data = stack.getTag();
          var tag = data == null ? new net.minecraft.nbt.CompoundTag() : data.copy();
          return 0xFF000000 | (tag.contains("FieldColor") ? tag.getInt("FieldColor") : 0x52E5FF);
        },
        FieldEmitters.TOWER_ITEM.get(),
        FieldEmitters.TUNER.get(),
        FieldEmitters.EMITTER_ITEM.get(),
        FieldEmitters.RAIL_ITEM.get());
  }
}
