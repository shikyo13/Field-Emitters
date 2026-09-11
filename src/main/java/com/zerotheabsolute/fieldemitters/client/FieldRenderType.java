package com.zerotheabsolute.fieldemitters.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;

final class FieldRenderType extends RenderStateShard {
  private FieldRenderType() {
    super("field_dummy", () -> {}, () -> {});
  }

  static final RenderType ENERGY =
      RenderType.create(
          "field_energy",
          DefaultVertexFormat.NEW_ENTITY,
          VertexFormat.Mode.QUADS,
          4096,
          false,
          true,
          RenderType.CompositeState.builder()
              .setShaderState(RENDERTYPE_EYES_SHADER)
              .setTextureState(
                  new TextureStateShard(
                      ResourceLocation.fromNamespaceAndPath(
                          "fieldemitters", "textures/misc/white.png"),
                      false,
                      false))
              .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
              .setDepthTestState(LEQUAL_DEPTH_TEST)
              .setCullState(NO_CULL)
              .setWriteMaskState(COLOR_WRITE)
              .createCompositeState(false));
}
