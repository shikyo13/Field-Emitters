package com.zerotheabsolute.fieldemitters.client;

import com.mojang.blaze3d.vertex.*;
import com.zerotheabsolute.fieldemitters.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

final class SphereRenderer {
  private static final int LONGITUDES = 96, LATITUDES = 48;
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
    float time = e.controls.animation ? (e.getLevel().getGameTime() + partial) / 20 : 0;
    double waveAge = (e.getLevel().getGameTime() + partial - e.impactTime) / 20.0;
    for (int longitude = 0; longitude < LONGITUDES; longitude++) {
      double azimuth = longitude * Math.PI * 2 / LONGITUDES;
      for (int latitude = 0; latitude < LATITUDES; latitude++) {
        double elevation = minLatitude + range * latitude / LATITUDES;
        Vec3 a = point(radius, azimuth, elevation),
            b = point(radius, azimuth + Math.PI * 2 / LONGITUDES, elevation),
            c = point(radius, azimuth + Math.PI * 2 / LONGITUDES, elevation + range / LATITUDES),
            d = point(radius, azimuth, elevation + range / LATITUDES);
        shellVertex(e, vertices, matrix, a, hit, waveAge, progress, opacity, minLatitude);
        shellVertex(e, vertices, matrix, b, hit, waveAge, progress, opacity, minLatitude);
        shellVertex(e, vertices, matrix, c, hit, waveAge, progress, opacity, minLatitude);
        shellVertex(e, vertices, matrix, d, hit, waveAge, progress, opacity, minLatitude);
        if (e.controls.pattern == 3) {
          SpherePlasma.triangle(e, vertices, matrix, a, b, c, time, progress, opacity, minLatitude);
          SpherePlasma.triangle(e, vertices, matrix, a, c, d, time, progress, opacity, minLatitude);
        }
      }
    }
    renderPattern(e, vertices, matrix, partial, progress, opacity, minLatitude);
    if (e.powered && progress < 1 && e.controls.animation)
      SphereProjection.render(
          e.controls.projection, vertices, matrix, radius, minLatitude, progress, e.color);
  }

  private static void shellVertex(
      EmitterEntity e,
      VertexConsumer vertices,
      Matrix4f matrix,
      Vec3 point,
      Vec3 hit,
      double waveAge,
      float progress,
      float opacity,
      double minimum) {
    if (progress >= 1 && (waveAge < 0 || waveAge >= 2)) {
      vertex(vertices, matrix, point, e.color, IDLE_ALPHA * opacity);
      return;
    }
    Vec3 normal = point.subtract(.5, 0, .5).normalize();
    double arc = Math.acos(Mth.clamp(normal.dot(hit), -1, 1)) * e.controls.sphereRadius;
    float ripple =
        waveAge >= 0 && waveAge < 2
            ? (float) Math.exp(-Math.pow((arc - waveAge * 9) / .6, 2)) * (float) (1 - waveAge / 2)
            : 0;
    double azimuth = Math.atan2(normal.z, normal.x);
    double height = (Math.asin(Mth.clamp(normal.y, -1, 1)) - minimum) / (Math.PI / 2 - minimum);
    float noise = shellNoise(normal, 0);
    float reveal =
        e.controls.animation
            ? e.controls.projection.coverage(azimuth, height, noise, progress)
            : progress;
    float edge =
        e.controls.animation ? e.controls.projection.edge(azimuth, height, noise, progress) : 0;
    vertex(
        vertices,
        matrix,
        point,
        e.color,
        ((IDLE_ALPHA * reveal) + edge * .38f + ripple * .45f) * opacity);
  }

  static float shellNoise(Vec3 normal, float seconds) {
    return com.zeromods.core.animation.PlasmaSurface.noise(
        normal.x * 3, normal.y * 3, normal.z * 3, seconds);
  }

  static void ray(
      VertexConsumer vertices,
      Matrix4f matrix,
      Vec3 start,
      Vec3 end,
      int color,
      float width,
      float alpha) {
    var direction = end.subtract(start).normalize();
    var across = direction.cross(new Vec3(0, 1, 0));
    if (across.lengthSqr() < .001) across = direction.cross(new Vec3(1, 0, 0));
    across = across.normalize().scale(width);
    var other = direction.cross(across).normalize().scale(width);
    quad(
        vertices,
        matrix,
        start.subtract(across),
        end.subtract(across),
        end.add(across),
        start.add(across),
        color,
        alpha);
    quad(
        vertices,
        matrix,
        start.subtract(other),
        end.subtract(other),
        end.add(other),
        start.add(other),
        color,
        alpha);
  }

  static float coverage(
      EmitterEntity e, double azimuth, double elevation, double minimum, float progress) {
    if (progress >= 1) return 1;
    if (progress <= 0) return 0;
    if (!e.controls.animation) return progress;
    Vec3 normal = point(1, azimuth, elevation).subtract(.5, 0, .5);
    return e.controls.projection.coverage(
        azimuth, (elevation - minimum) / (Math.PI / 2 - minimum), shellNoise(normal, 0), progress);
  }

  private static void renderPattern(
      EmitterEntity e,
      VertexConsumer vertices,
      Matrix4f matrix,
      float partial,
      float progress,
      float opacity,
      double minLatitude) {
    if (e.controls.pattern == 3) return;
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
        0,
        1,
        (x1, y1, x2, y2, width, color, alpha) -> {
          // The curved shell above supplies the broad fill. Keep the detailed strokes and tiles.
          if (width > .2f) return;
          float middle = (x1 + x2) * .5f;
          if (middle < 0 || middle > right) return;
          float reveal =
              coverage(
                  e,
                  middle / PATTERN_RADIUS,
                  (y1 + y2) * .5 / PATTERN_RADIUS,
                  minLatitude,
                  progress);
          if (reveal <= 0) return;
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
              alpha * opacity * reveal);
        });
  }

  private static Vec3 mapped(
      double radius, float u, float v, float right, float bottom, float top) {
    return point(
        radius,
        Mth.clamp(u, 0, right) / PATTERN_RADIUS,
        Mth.clamp(v, bottom, top) / PATTERN_RADIUS);
  }

  static Vec3 point(double radius, double azimuth, double elevation) {
    double horizontal = Math.cos(elevation) * radius;
    return new Vec3(
        .5 + Math.cos(azimuth) * horizontal,
        Math.sin(elevation) * radius,
        .5 + Math.sin(azimuth) * horizontal);
  }

  static void quad(
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
