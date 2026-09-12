package com.zerotheabsolute.fieldemitters.client;

import com.mojang.blaze3d.vertex.*;
import com.zerotheabsolute.fieldemitters.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

final class SphereRenderer {
  private static final int LONGITUDES = 64, LATITUDES = 24, FANS = 4;
  private static final float IDLE_ALPHA = .11f, SHUTDOWN_TICKS = 25;
  // Fixed pattern coordinates bound detail work even on the largest sphere.
  private static final float PATTERN_RADIUS = 8;
  private static final double DETAIL_OFFSET = .015;

  private SphereRenderer() {}

  static void render(EmitterEntity e, float partial, PoseStack pose, MultiBufferSource buffers) {
    if (!e.controls.visible) return;
    float age = e.getLevel().getGameTime() + partial - e.transition;
    float progress = e.powered ? Mth.clamp(age / SphereField.FORMATION_TICKS, 0, 1) : 1;
    float opacity = e.powered ? 1 : Mth.clamp(1 - age / SHUTDOWN_TICKS, 0, 1);
    if (opacity <= 0) return;
    var vertices = buffers.getBuffer(FieldRenderType.ENERGY);
    var matrix = pose.last().pose();
    int radius = e.controls.sphereRadius;
    double minLatitude = e.controls.dome ? 0 : -Math.PI / 2;
    double range = Math.PI / 2 - minLatitude;
    var hit = e.impact.subtract(SphereField.center(e)).normalize();
    double waveAge = (e.getLevel().getGameTime() + partial - e.impactTime) / 20.0;
    for (int longitude = 0; longitude < LONGITUDES; longitude++) {
      double azimuth = longitude * Math.PI * 2 / LONGITUDES;
      // Four fans each build one quadrant; the leading edge matches the projection sheets.
      double sector = (longitude % (LONGITUDES / FANS)) / (double) (LONGITUDES / FANS);
      if (sector > progress) continue;
      for (int latitude = 0; latitude < LATITUDES; latitude++) {
        double elevation = minLatitude + range * latitude / LATITUDES;
        Vec3 a = point(radius, azimuth, elevation),
            b = point(radius, azimuth + Math.PI * 2 / LONGITUDES, elevation),
            c = point(radius, azimuth + Math.PI * 2 / LONGITUDES, elevation + range / LATITUDES),
            d = point(radius, azimuth, elevation + range / LATITUDES);
        var normal = a.subtract(.5, 0, .5).normalize();
        double arc = Math.acos(Mth.clamp(normal.dot(hit), -1, 1)) * radius;
        float ripple =
            waveAge >= 0 && waveAge < 2
                ? (float) Math.exp(-Math.pow((arc - waveAge * 9) / .6, 2))
                    * (float) (1 - waveAge / 2)
                : 0;
        float alpha = ((e.controls.pattern == 3 ? 0 : IDLE_ALPHA) + ripple * .45f) * opacity;
        quad(vertices, matrix, a, b, c, d, e.color, alpha);
      }
    }
    renderPattern(e, vertices, matrix, partial, progress, opacity, minLatitude);
    if (e.powered && progress < 1 && e.controls.animation) {
      var tip = new Vec3(.5, TowerBlock.HEIGHT - .25, .5);
      for (int fan = 0; fan < FANS; fan++) {
        double angle = (fan + progress) * Math.PI * 2 / FANS;
        for (int band = 0; band < LATITUDES; band++) {
          double elevation = minLatitude + range * band / LATITUDES;
          var a = point(radius, angle, elevation);
          var b = point(radius, angle, elevation + range / LATITUDES);
          quad(vertices, matrix, tip, a, b, tip, e.color, .10f);
          if (band == 0 || band == LATITUDES / 2 || band == LATITUDES - 1)
            quad(vertices, matrix, tip, a, a.add(0, .08, 0), tip, 0xDFFFFF, .65f);
          // Soft trailing sheet gives the broad translucent laser fan in the reference.
          var trail = point(radius, angle - .035, elevation);
          quad(vertices, matrix, tip, a, trail, tip, e.color, .14f);
        }
      }
    }
  }

  private static void renderPattern(
      EmitterEntity e,
      VertexConsumer vertices,
      Matrix4f matrix,
      float partial,
      float progress,
      float opacity,
      double minLatitude) {
    float right = (float) (Math.PI * 2 * PATTERN_RADIUS);
    float bottom = (float) (minLatitude * PATTERN_RADIUS);
    float top = (float) (Math.PI / 2 * PATTERN_RADIUS);
    float time = e.controls.animation ? e.getLevel().getGameTime() + partial : 0;
    com.zeromods.core.animation.EnergySurface.render(
        0,
        right,
        bottom,
        top,
        time,
        -1000,
        0,
        0,
        e.color,
        e.controls.particleColor,
        e.controls.pattern,
        e.controls.formation,
        progress,
        (x1, y1, x2, y2, width, color, alpha) -> {
          // The curved shell above supplies the broad fill. Keep the detailed strokes and tiles.
          if (width > .2f) return;
          float middle = (x1 + x2) * .5f;
          float sector = middle / right * FANS;
          if (sector - Math.floor(sector) > progress || middle < 0 || middle > right) return;
          float dx = x2 - x1, dy = y2 - y1;
          float length = (float) Math.hypot(dx, dy);
          if (length < .0001f) return;
          float ox = -dy / length * width, oy = dx / length * width;
          double radius = e.controls.sphereRadius + DETAIL_OFFSET;
          quad(
              vertices,
              matrix,
              mapped(radius, x1 + ox, y1 + oy, right, bottom, top),
              mapped(radius, x2 + ox, y2 + oy, right, bottom, top),
              mapped(radius, x2 - ox, y2 - oy, right, bottom, top),
              mapped(radius, x1 - ox, y1 - oy, right, bottom, top),
              color,
              alpha * opacity);
        });
  }

  private static Vec3 mapped(
      double radius, float u, float v, float right, float bottom, float top) {
    return point(
        radius,
        Mth.clamp(u, 0, right) / PATTERN_RADIUS,
        Mth.clamp(v, bottom, top) / PATTERN_RADIUS);
  }

  private static Vec3 point(double radius, double azimuth, double elevation) {
    double horizontal = Math.cos(elevation) * radius;
    return new Vec3(
        .5 + Math.cos(azimuth) * horizontal,
        Math.sin(elevation) * radius,
        .5 + Math.sin(azimuth) * horizontal);
  }

  private static void quad(
      VertexConsumer v, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, float alpha) {
    vertex(v, matrix, a, color, alpha);
    vertex(v, matrix, b, color, alpha);
    vertex(v, matrix, c, color, alpha);
    vertex(v, matrix, d, color, alpha);
  }

  private static void vertex(VertexConsumer v, Matrix4f matrix, Vec3 p, int color, float alpha) {
    v.addVertex(matrix, (float) p.x, (float) p.y, (float) p.z)
        .setColor(
            (color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, alpha)
        .setUv(.5f, .5f)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(0xF000F0)
        .setNormal(0, 1, 0);
  }
}
