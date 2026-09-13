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

final class PlasmaDomeRenderer {
  private static final int LONGITUDES = 128, LATITUDES = 64;

  private record MeshKey(int radius, double minimum) {}
  private static final Map<MeshKey, Vec3[][]> MESHES = new HashMap<>();
  private record Ripple(Vec3 normal, double radius, float fade, double minDot, double maxDot, double width) {}

  private static List<Ripple> ripples(EmitterEntity emitter, float partial) {
    var result = new ArrayList<Ripple>();
    double now = emitter.getLevel().getGameTime() + partial;
    var center = SphereField.center(emitter);
    for (var wave : emitter.impactWaves) {
      double seconds = (now - wave.time()) / 20.0;
      double lifetime = emitter.controls.pattern == 2 ? 1.25 : 2;
      if (seconds < 0 || seconds >= lifetime) continue;
      double speed = switch (emitter.controls.pattern) { case 0 -> 6.5; case 2 -> 5; case 3 -> 7; default -> 9; };
      double width = emitter.controls.pattern == 3 ? 1.0 : .6;
      double radius = seconds * speed;
      double low = Math.max(0, (radius - width * 3) / emitter.controls.sphereRadius);
      double high = Math.min(Math.PI, (radius + width * 3) / emitter.controls.sphereRadius);
      result.add(new Ripple(wave.position().subtract(center).normalize(), radius,
          (float) (1 - seconds / lifetime), Math.cos(high), Math.cos(low), width));
    }
    return result;
  }

  private PlasmaDomeRenderer() {}

  static void render(EmitterEntity e, float partial, PoseStack pose, MultiBufferSource buffers) {
    if (!e.controls.visible) return;
    float age = e.getLevel().getGameTime() + partial - e.transition;
    float progress = e.powered ? Mth.clamp(age / SphereField.FORMATION_TICKS, 0, 1) : 1;
    float opacity = e.powered ? 1 : FieldShutdown.remaining(age);
    if (opacity <= 0) return;
    var vertices = buffers.getBuffer(com.zeromods.core.client.EnergyRenderTypes.SURFACE);
    var matrix = pose.last().pose();
    int radius = e.controls.sphereRadius;
    double depth = Math.min(SphereField.DOME_DEPTH,
        Math.max(0, e.getBlockPos().getY() - e.getLevel().getMinBuildHeight()));
    double minimum = e.controls.dome ? Math.asin(-depth / radius) : -Math.PI / 2;
    double range = Math.PI / 2 - minimum;
    var waves = ripples(e, partial);
    var mesh = MESHES.computeIfAbsent(new MeshKey(radius, minimum), key -> {
      var points = new Vec3[LONGITUDES + 1][LATITUDES + 1];
      for (int x = 0; x <= LONGITUDES; x++)
        for (int y = 0; y <= LATITUDES; y++)
          points[x][y] = point(radius, x * Math.PI * 2 / LONGITUDES, minimum + range * y / LATITUDES);
      return points;
    });
    var glow = new float[LONGITUDES + 1][LATITUDES + 1];
    for (int x = 0; x <= LONGITUDES; x++)
      for (int y = 0; y <= LATITUDES; y++)
        glow[x][y] = impact(mesh[x][y].subtract(.5, 0, .5).scale(1.0 / radius), waves, radius);
    for (int x = 0; x < LONGITUDES; x++)
      for (int y = 0; y < LATITUDES; y++) {
        surfaceVertex(e, vertices, matrix, mesh[x][y], x, y, minimum, range, progress, opacity, glow[x][y]);
        surfaceVertex(e, vertices, matrix, mesh[x+1][y], x+1, y, minimum, range, progress, opacity, glow[x+1][y]);
        surfaceVertex(e, vertices, matrix, mesh[x+1][y+1], x+1, y+1, minimum, range, progress, opacity, glow[x+1][y+1]);
        surfaceVertex(e, vertices, matrix, mesh[x][y+1], x, y+1, minimum, range, progress, opacity, glow[x][y+1]);
      }
    if (e.powered && progress < 1 && e.controls.animation)
      SphereProjection.render(e.controls.projection, buffers.getBuffer(FieldRenderType.ENERGY),
          matrix, radius, minimum, progress, e.color);
  }

  private static float impact(Vec3 normal, List<Ripple> waves, int radius) {
    float strongest = 0, total = 0;
    for (var wave : waves) {
      double dot = normal.dot(wave.normal());
      if (dot < wave.minDot() || dot > wave.maxDot()) continue;
      double arc = Math.acos(Mth.clamp(dot, -1, 1)) * radius;
      double band = (arc - wave.radius()) / wave.width();
      float crest = (float) Math.exp(-band * band) * wave.fade();
      strongest = Math.max(strongest, crest);
      total += crest;
    }
    return strongest + (1 - strongest) * (float) (1 - Math.exp(-(total - strongest)));
  }

  private static void surfaceVertex(EmitterEntity e, VertexConsumer v, Matrix4f matrix,
      Vec3 p, int x, int y, double minimum, double range, float progress, float opacity, float impact) {
    int flags = e.controls.pattern | 4 | (e.controls.dome ? 0 : 8) | (e.controls.animation ? 0 : 16);
    int formation = e.controls.projection.ordinal();
    int accent = e.controls.accentColor(e.color);
    v.addVertex(matrix, (float)p.x, (float)p.y, (float)p.z)
        .setColor((e.color >> 16 & 255)/255f, (e.color >> 8 & 255)/255f, (e.color & 255)/255f, opacity)
        .setUv((float)(x * Math.PI * 2 / LONGITUDES), (float)(minimum + range * y / LATITUDES))
        .setOverlay((Math.round(impact * 32767) << 16) | formation)
        .setLight((Math.round((e.controls.animation ? progress : 1) * 32767) << 16) | flags)
        .setNormal((accent >> 16 & 255)/255f, (accent >> 8 & 255)/255f, (accent & 255)/255f);
  }

  private static Vec3 point(double radius, double azimuth, double elevation) {
    double horizontal = Math.cos(elevation) * radius;
    return new Vec3(.5 + Math.cos(azimuth) * horizontal, Math.sin(elevation) * radius,
        .5 + Math.sin(azimuth) * horizontal);
  }
}
