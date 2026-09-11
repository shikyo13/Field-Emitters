package com.zerotheabsolute.fieldemitters.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.zerotheabsolute.fieldemitters.EmitterEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** Mechanical iris vanes open smoothly around the exposed projector lens. */
final class HardwareRenderer {
  private static final ResourceLocation MATERIAL =
      ResourceLocation.fromNamespaceAndPath("fieldemitters", "textures/block/titanium.png");

  static void render(
      EmitterEntity e,
      float time,
      float charge,
      PoseStack pose,
      MultiBufferSource buffers,
      int light) {
    // Finish this entire material pass before FieldRenderer requests its energy consumer.
    var v = buffers.getBuffer(RenderType.entityCutoutNoCull(MATERIAL));
    float open = charge * charge * (3 - 2 * charge);
    for (int side = 0; side < 4; side++) {
      pose.pushPose();
      pose.translate(.5, 4.55, .5);
      pose.mulPose(Axis.YP.rotationDegrees(side * 90));
      pose.translate(0, 0, .13 + open * .09);
      pose.mulPose(Axis.XP.rotationDegrees(-open * 24));
      cuboid(v, pose.last().pose(), -.135f, 0, 0, .135f, .055f, .22f, light);
      pose.popPose();
    }
  }

  private static void cuboid(
      VertexConsumer v,
      Matrix4f m,
      float a,
      float b,
      float c,
      float x,
      float y,
      float z,
      int light) {
    face(v, m, a, y, c, x, y, c, x, y, z, a, y, z, 0, 1, 0, light);
    face(v, m, a, b, z, x, b, z, x, b, c, a, b, c, 0, -1, 0, light);
    face(v, m, a, b, c, x, b, c, x, y, c, a, y, c, 0, 0, -1, light);
    face(v, m, x, b, z, a, b, z, a, y, z, x, y, z, 0, 0, 1, light);
    face(v, m, x, b, c, x, b, z, x, y, z, x, y, c, 1, 0, 0, light);
    face(v, m, a, b, z, a, b, c, a, y, c, a, y, z, -1, 0, 0, light);
  }

  private static void face(
      VertexConsumer v,
      Matrix4f m,
      float a,
      float b,
      float c,
      float d,
      float e,
      float f,
      float g,
      float h,
      float i,
      float j,
      float k,
      float l,
      float nx,
      float ny,
      float nz,
      int light) {
    vertex(v, m, a, b, c, 0, 1, nx, ny, nz, light);
    vertex(v, m, d, e, f, 1, 1, nx, ny, nz, light);
    vertex(v, m, g, h, i, 1, 0, nx, ny, nz, light);
    vertex(v, m, j, k, l, 0, 0, nx, ny, nz, light);
  }

  private static void vertex(
      VertexConsumer v,
      Matrix4f m,
      float x,
      float y,
      float z,
      float u,
      float w,
      float nx,
      float ny,
      float nz,
      int light) {
    v.addVertex(m, x, y, z)
        .setColor(255, 255, 255, 255)
        .setUv(u, w)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(light)
        .setNormal(nx, ny, nz);
  }
}
