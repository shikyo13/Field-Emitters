package com.zerotheabsolute.fieldemitters.client;

import com.mojang.blaze3d.vertex.*;
import com.zerotheabsolute.fieldemitters.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

final class SphereRenderer {
  private static final int LONGITUDES = 64, LATITUDES = 24, BEAMS = 4;
  private static final float IDLE_ALPHA = .11f, SHUTDOWN_TICKS = 25;
  // Fixed pattern coordinates bound detail work even on the largest sphere.
  private static final float PATTERN_RADIUS = 8;
  private static final double DETAIL_OFFSET = .015;
  private static final int TRAIL_SAMPLES = 6;
  private static final float TRAIL_SECONDS = .09f;
  private static final float BEAM_WIDTH = .045f;
  private static final double TAU = Math.PI * 2;

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
        float reveal = progress * progress;
        float alpha =
            ((e.controls.pattern == 3 ? 0 : IDLE_ALPHA * reveal) + ripple * .45f) * opacity;
        quad(vertices, matrix, a, b, c, d, e.color, alpha);
      }
    }
    renderPattern(e, vertices, matrix, partial, progress, opacity, minLatitude);
    if (e.powered && progress < 1 && e.controls.animation)
      renderProjectors(vertices, matrix, radius, minLatitude, age / 20, progress, e.color);
  }

  /** Accelerating mirror sweeps: each beam has its own phase, direction and elevation. */
  private static Vec3 scanTarget(double radius, double minimum, float seconds, int beam) {
    double t = Math.max(0, seconds);
    double phase = TAU * (.18 * t + .38 * t * t);
    double offset = beam * TAU / BEAMS;
    double direction = (beam & 1) == 0 ? 1 : -1;
    double azimuth =
        offset
            + direction * phase * (1 + beam * .17)
            + .55 * Math.sin(phase * (1.3 + beam * .19) + offset);
    double height =
        .5
            + .43 * Math.sin(phase * (.61 + beam * .13) + offset)
            + .06 * Math.sin(phase * 2.3 - offset);
    return point(radius, azimuth, minimum + (Math.PI / 2 - minimum) * height);
  }

  private static void renderProjectors(
      VertexConsumer vertices,
      Matrix4f matrix,
      int radius,
      double minimum,
      float seconds,
      float progress,
      int color) {
    var tip = new Vec3(.5, TowerBlock.HEIGHT - .25, .5);
    float fade = Mth.clamp((1 - progress) * 8, 0, 1);
    for (int beam = 0; beam < BEAMS; beam++) {
      var target = scanTarget(radius, minimum, seconds, beam);
      for (int sample = 1; sample <= TRAIL_SAMPLES; sample++) {
        float delay = TRAIL_SECONDS * sample / TRAIL_SAMPLES;
        var previous = scanTarget(radius, minimum, seconds - delay, beam);
        float strength = 1 - sample / (float) (TRAIL_SAMPLES + 1);
        // A short curved trail gives the moving ray a soft laser-projector fan.
        quad(vertices, matrix, tip, target, previous, tip, color, .10f * strength * fade);
        ray(vertices, matrix, tip, previous, color, BEAM_WIDTH * 2, .06f * strength * fade);
        target = previous;
      }
      target = scanTarget(radius, minimum, seconds, beam);
      ray(vertices, matrix, tip, target, color, BEAM_WIDTH * 4, .08f * fade);
      ray(vertices, matrix, tip, target, 0xE8FFFF, BEAM_WIDTH, .8f * fade);
      var normal = target.subtract(.5, 0, .5).normalize();
      var tangent = normal.cross(new Vec3(0, 1, 0));
      if (tangent.lengthSqr() < .001) tangent = normal.cross(new Vec3(1, 0, 0));
      tangent = tangent.normalize().scale(.14);
      var up = normal.cross(tangent).normalize().scale(.14);
      var spot = target.add(normal.scale(DETAIL_OFFSET));
      quad(
          vertices,
          matrix,
          spot.subtract(tangent).subtract(up),
          spot.add(tangent).subtract(up),
          spot.add(tangent).add(up),
          spot.subtract(tangent).add(up),
          0xE8FFFF,
          .8f * fade);
    }
  }

  private static void ray(
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

  private static float coverage(double azimuth, double elevation, double minimum, float progress) {
    if (progress >= 1) return 1;
    int column = (int) Math.floor(azimuth / TAU * LONGITUDES);
    int row = (int) Math.floor((elevation - minimum) / (Math.PI / 2 - minimum) * LATITUDES);
    // Stable tile ordering lets repeated scans build up a persistent hologram, without a frame
    // cache.
    double seed = Math.sin(column * 12.9898 + row * 78.233) * 43758.5453;
    seed -= Math.floor(seed);
    float reveal = Mth.clamp((progress - (float) seed * .85f) / .15f, 0, 1);
    return reveal * reveal * (3 - 2 * reveal);
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
          if (middle < 0 || middle > right) return;
          float reveal =
              coverage(
                  middle / PATTERN_RADIUS, (y1 + y2) * .5 / PATTERN_RADIUS, minLatitude, progress);
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
