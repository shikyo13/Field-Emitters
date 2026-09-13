package com.zerotheabsolute.fieldemitters.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zerotheabsolute.fieldemitters.EmitterEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Contours are sampled in 3D so they meet at the seam and both poles. */
final class SpherePlasma {
  private static final float LEVEL = .5f;

  private SpherePlasma() {}

  static void triangle(
      EmitterEntity e,
      VertexConsumer v,
      Matrix4f m,
      Vec3 a,
      Vec3 b,
      Vec3 c,
      float time,
      float progress,
      float opacity,
      double minimum) {
    float an = noise(a, time), bn = noise(b, time), cn = noise(c, time);
    if ((an < LEVEL) == (bn < LEVEL) && (bn < LEVEL) == (cn < LEVEL)) return;
    if ((an < LEVEL) == (bn < LEVEL)) {
      Vec3 swap = a;
      a = c;
      c = b;
      b = swap;
      float n = an;
      an = cn;
      cn = bn;
      bn = n;
    } else if ((an < LEVEL) == (cn < LEVEL)) {
      Vec3 swap = a;
      a = b;
      b = c;
      c = swap;
      float n = an;
      an = bn;
      bn = cn;
      cn = n;
    }
    Vec3 start = a.lerp(b, (LEVEL - an) / (bn - an)), end = a.lerp(c, (LEVEL - an) / (cn - an));
    Vec3 normal = start.add(end).scale(.5).subtract(.5, 0, .5).normalize();
    float mask =
        SphereRenderer.coverage(
            e,
            Math.atan2(normal.z, normal.x),
            Math.asin(Mth.clamp(normal.y, -1, 1)),
            minimum,
            progress);
    if (mask <= 0) return;
    start = raised(start);
    end = raised(end);
    SphereRenderer.ray(v, m, start, end, e.controls.accentColor(e.color), .065f, .08f * opacity * mask);
    SphereRenderer.ray(v, m, start, end, e.controls.accentColor(e.color), .014f, .5f * opacity * mask);
  }

  private static float noise(Vec3 p, float time) {
    return SphereRenderer.shellNoise(p.subtract(.5, 0, .5).normalize(), time);
  }

  private static Vec3 raised(Vec3 p) {
    return p.add(p.subtract(.5, 0, .5).normalize().scale(.025));
  }
}
