package com.zerotheabsolute.fieldemitters.client;
import com.zeromods.core.client.EnergyRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
final class FieldRenderType {
  static final RenderType ENERGY = EnergyRenderTypes.translucent("field_energy",
      ResourceLocation.fromNamespaceAndPath("fieldemitters", "textures/misc/white.png"));
}
