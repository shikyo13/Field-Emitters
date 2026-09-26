package com.zerotheabsolute.fieldemitters.client;

import com.mojang.blaze3d.vertex.*;
import com.zerotheabsolute.fieldemitters.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

final class SphereRenderer {
  private static final int LONGITUDES = 96, LATITUDES = 48;
  private static final float IDLE_ALPHA = .11f;
  // Fixed pattern coordinates bound detail work even on the largest sphere.
  private static final float PATTERN_RADIUS = 8;
  private static final double DETAIL_OFFSET = .015;

  private record MeshKey(int radius, double minimum) {}
  private static final Map<MeshKey, Vec3[][]> MESHES = new HashMap<>();
  private record Ripple(Vec3 normal, double radius, float fade, double minDot, double maxDot) {}

  private static List<Ripple> ripples(EmitterEntity emitter, float partial) {
    var result = new ArrayList<Ripple>();
    double now = FieldRenderClock.time(emitter, partial);
    var center = SphereField.center(emitter);
    for (var wave : emitter.impactWaves) {
      double seconds = (now - wave.time()) / 20.0;
      if (seconds < 0 || seconds >= 2) continue;
      double radius = seconds * 9;
      double low = Math.max(0, (radius - 1.8) / emitter.controls.sphereRadius);
      double high = Math.min(Math.PI, (radius + 1.8) / emitter.controls.sphereRadius);
      result.add(new Ripple(wave.position().subtract(center).normalize(), radius,
          (float) (1 - seconds / 2), Math.cos(high), Math.cos(low)));
    }
    return result;
  }

  private SphereRenderer() {}

  static void render(EmitterEntity e, float partial, PoseStack pose, MultiBufferSource buffers) {
    if (e.controls.pattern == 3) {
      PlasmaDomeRenderer.render(e, partial, pose, buffers);
      return;
    }
    if (!e.controls.visible) return;
    float age = FieldRenderClock.time(e, partial) - e.transition;
    float progress = e.powered ? Mth.clamp(age / SphereField.FORMATION_TICKS, 0, 1) : 1;
    float opacity = e.powered ? 1 : FieldShutdown.remaining(age);
    if (opacity <= 0) return;
    var vertices = buffers.getBuffer(FieldRenderType.ENERGY);
    var matrix = pose.last().pose();
    int radius = e.controls.sphereRadius;
    double depth = Math.min(SphereField.DOME_DEPTH,
        Math.max(0, e.getBlockPos().getY() - e.getLevel().getMinBuildHeight()));
    double minLatitude = e.controls.dome ? Math.asin(-depth / radius) : -Math.PI / 2;
    double range = Math.PI / 2 - minLatitude;
    var waves = (e.controls.pattern == 0 || e.controls.pattern == 2) ? List.<Ripple>of() : ripples(e, partial);
    float time = e.controls.animation ? (FieldRenderClock.time(e, partial)) / 20 : 0;
    var mesh = MESHES.computeIfAbsent(new MeshKey(radius, minLatitude), key -> {
      var points = new Vec3[LONGITUDES + 1][LATITUDES + 1];
      for (int x = 0; x <= LONGITUDES; x++)
        for (int y = 0; y <= LATITUDES; y++)
          points[x][y] = point(radius, x * Math.PI * 2 / LONGITUDES, minLatitude + range * y / LATITUDES);
      return points;
    });
    var alpha = new float[LONGITUDES + 1][LATITUDES + 1];
    var colors = new int[LONGITUDES + 1][LATITUDES + 1];
    for (int x = 0; x <= LONGITUDES; x++)
      for (int y = 0; y <= LATITUDES; y++) {
        alpha[x][y] = shellAlpha(e, mesh[x][y], waves, progress, opacity, minLatitude);
        colors[x][y] = e.color;
        if (e.controls.customAccent && e.controls.pattern == 1 && !waves.isEmpty()) {
          float base = shellAlpha(e, mesh[x][y], List.of(), progress, opacity, minLatitude);
          float weight = (alpha[x][y] - base) / Math.max(.001f, alpha[x][y]);
          colors[x][y] = com.zeromods.core.animation.EnergyColors.mix(e.color, e.controls.accentColor(e.color), weight);
        }
      }
    for (int longitude = 0; longitude < LONGITUDES; longitude++) {
      for (int latitude = 0; latitude < LATITUDES; latitude++) {
        Vec3 a = mesh[longitude][latitude], b = mesh[longitude + 1][latitude],
            c = mesh[longitude + 1][latitude + 1], d = mesh[longitude][latitude + 1];
        vertex(vertices, matrix, a, colors[longitude][latitude], alpha[longitude][latitude]);
        vertex(vertices, matrix, b, colors[longitude + 1][latitude], alpha[longitude + 1][latitude]);
        vertex(vertices, matrix, c, colors[longitude + 1][latitude + 1], alpha[longitude + 1][latitude + 1]);
        vertex(vertices, matrix, d, colors[longitude][latitude + 1], alpha[longitude][latitude + 1]);
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

  private static float shellAlpha(EmitterEntity e, Vec3 point, List<Ripple> waves,
      float progress, float opacity, double minimum) {
    if (progress >= 1 && waves.isEmpty()) return IDLE_ALPHA * opacity;
    Vec3 normal = point.subtract(.5, 0, .5).scale(1.0 / e.controls.sphereRadius);
    float ripple = 0;
    for (var wave : waves) {
      double dot = normal.dot(wave.normal());
      if (dot < wave.minDot() || dot > wave.maxDot()) continue;
      double arc = Math.acos(Mth.clamp(dot, -1, 1)) * e.controls.sphereRadius;
      double band = (arc - wave.radius()) / .6;
      ripple = Math.max(ripple, (float) Math.exp(-band * band) * wave.fade());
    }
    if (progress >= 1) return (IDLE_ALPHA + ripple * .45f) * opacity;
    double azimuth = Math.atan2(normal.z, normal.x);
    double height = (Math.asin(Mth.clamp(normal.y, -1, 1)) - minimum) / (Math.PI / 2 - minimum);
    float noise = shellNoise(normal, 0);
    float reveal =
        e.controls.animation
            ? e.controls.projection.coverage(azimuth, height, noise, progress)
            : progress;
    float edge =
        e.controls.animation ? e.controls.projection.edge(azimuth, height, noise, progress) : 0;
    return ((IDLE_ALPHA * reveal) + edge * .38f + ripple * .45f) * opacity;
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
    float time = e.controls.animation ? FieldRenderClock.time(e, partial) : 0;
    com.zeromods.core.animation.HexFieldPattern.Stroke stroke =
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
        };
    if (e.controls.pattern == 0)
      com.zeromods.core.animation.HexFieldPattern.render(0, right, bottom, top, time,
          new SphereSurfaceRipples(e, partial, PATTERN_RADIUS), e.color, e.controls.accentColor(e.color),
          com.zeromods.core.animation.HexFieldPattern.Style.forAccent(e.controls.accentColor(e.color)), stroke);
    else if (e.controls.pattern == 2)
      com.zeromods.core.animation.EnergySurface.render(0, right, bottom, top, time,
          new SphereSurfaceRipples(e, partial, PATTERN_RADIUS), e.color, e.controls.accentColor(e.color),
          e.controls.pattern, 0, 1, false, stroke);
    else
      com.zeromods.core.animation.EnergySurface.render(0, right, bottom, top, time,
          -1000, 0, 0, e.color, e.controls.accentColor(e.color), e.controls.pattern, 0, 1, stroke);
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
