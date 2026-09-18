package com.zerotheabsolute.fieldemitters.client;

import com.mojang.blaze3d.vertex.*;
import com.zerotheabsolute.fieldemitters.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.*;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Tuner overlay: arrows point in world movement directions, amber means that direction is enabled.
 */
public final class FieldGuide {
  public static BlockPos source = BlockPos.ZERO, target = BlockPos.ZERO;
  public static long until = 0;

  public static void render(EmitterEntity e, PoseStack pose, MultiBufferSource buffers) {
    var mc = Minecraft.getInstance();
    if (mc.player == null || !FieldConfig.SHOW_GUIDES.get()) return;
    boolean selected = e.getBlockPos().equals(source) && e.getLevel().getGameTime() < until;
    if (!selected && !mc.player.getMainHandItem().is(FieldEmitters.TUNER.get())) return;
    if (mc.player.distanceToSqr(Vec3.atCenterOf(e.getBlockPos())) > 1024) return;
    var v = buffers.getBuffer(FieldRenderType.ENERGY);
    var m = pose.last().pose();
    for (var link : e.links) {
      int i = link.length() / 2;
      var center =
          Vec3.atCenterOf(link.cell(e.getBlockPos(), i, 0))
              .subtract(Vec3.atLowerCornerOf(e.getBlockPos()));
      if (!link.rail()) center = center.add(0, 1, 0);
      var along = new Vec3(link.dx(), link.dy(), link.dz());
      var normal = Vec3.atLowerCornerOf(link.movement(true).getNormal());
      for (int sign : new int[] {-1, 1}) {
        var c = center.add(along.scale(sign * .25));
        var forward = normal.scale(sign);
        int color =
            e.settings(link).barrierCovers(link.movement(sign > 0), link.inward())
                ? 0xFFB750
                : 0x77FFBD;
        var a = c.subtract(forward.scale(.65));
        var b = c.add(forward.scale(.35));
        quad(
            v,
            m,
            a.add(along.scale(.025)),
            b.add(along.scale(.025)),
            b.subtract(along.scale(.025)),
            a.subtract(along.scale(.025)),
            color);
        quad(
            v,
            m,
            b.add(along.scale(.13)),
            c.add(forward.scale(.65)),
            c.add(forward.scale(.65)),
            b.subtract(along.scale(.13)),
            color);
      }
      if (selected && link.target().equals(target)) {
        var a = center.subtract(along.scale(.48));
        var b = center.add(along.scale(.48));
        quad(
            v,
            m,
            a.add(0, .05, 0),
            b.add(0, .05, 0),
            b.add(0, -.05, 0),
            a.add(0, -.05, 0),
            0xFFFFFF);
      }
    }
  }

  private static void quad(
      VertexConsumer v, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) {
    for (var p : new Vec3[] {a, b, c, d})
      v.addVertex(m, (float) p.x, (float) p.y, (float) p.z)
          .setColor(
              (color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, .85f)
          .setUv(.5f, .5f)
          .setOverlay(OverlayTexture.NO_OVERLAY)
          .setLight(LightTexture.FULL_BRIGHT)
          .setNormal(0, 1, 0);
  }
}
