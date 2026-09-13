package com.zerotheabsolute.fieldemitters.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zeromods.core.animation.SphereFormation;
import com.zerotheabsolute.fieldemitters.TowerBlock;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Construction guides have no world entities or collision. */
final class SphereProjection {
  private static final int ARC_SEGMENTS = 64, FAN_RAYS = 12;
  private static final float CORE_WIDTH = .024f, HALO_WIDTH = .10f;
  private static final double SURFACE_OFFSET = .025;
  private static final int CORE_COLOR = 0xE0FFFF;

  private SphereProjection() {}

  static void render(
      SphereFormation style,
      VertexConsumer v,
      Matrix4f matrix,
      int radius,
      double minimum,
      float progress,
      int color) {
    double range = Math.PI / 2 - minimum;
    double front = style.front(progress);
    float fade = Math.min(1, (1 - progress) * 12);
    switch (style) {
      case LASER_CURTAIN -> {
        if (front >= 1) return;
        Vec3 tip = new Vec3(.5, TowerBlock.HEIGHT - .25, .5);
        for (int sign = -1; sign <= 1; sign += 2) {
          double azimuth = sign * front * Math.PI;
          Vec3 previous = SphereRenderer.point(radius, azimuth, minimum);
          for (int i = 0; i <= FAN_RAYS; i++) {
            Vec3 target = SphereRenderer.point(radius, azimuth, minimum + range * i / FAN_RAYS);
            SphereRenderer.quad(v, matrix, tip, previous, target, tip, color, .035f * fade);
            SphereRenderer.ray(v, matrix, tip, target, color, CORE_WIDTH * .5f, .30f * fade);
            previous = target;
          }
          meridian(v, matrix, radius, azimuth, minimum, Math.PI / 2, color, fade);
        }
      }
      case RISING_RING -> ring(v, matrix, radius, minimum + range * front, color, fade);
      case PROJECTED_SEED -> {
        if (progress < .27f) {
          Vec3 tip = new Vec3(.5, TowerBlock.HEIGHT - .25, .5);
          Vec3 crown = SphereRenderer.point(radius, 0, Math.PI / 2);
          float strength = Math.min(1, progress / .08f) * Math.min(1, (.27f - progress) / .08f);
          glow(v, matrix, tip, crown, color, strength);
        }
        if (progress > .13f) ring(v, matrix, radius, Math.PI / 2 - range * front, color, fade);
      }
      case MERIDIAN_SWEEP -> {
        double end = minimum + range * Math.min(1, progress / .65);
        for (int i = 0; i < SphereFormation.MERIDIANS; i++)
          meridian(
              v,
              matrix,
              radius,
              i * SphereFormation.TAU / SphereFormation.MERIDIANS,
              minimum,
              end,
              color,
              fade);
      }
      case HEX_ASSEMBLY, PLASMA_DISSOLVE -> {}
    }
  }

  private static void ring(
      VertexConsumer v, Matrix4f m, int radius, double latitude, int color, float alpha) {
    for (int i = 0; i < ARC_SEGMENTS; i++) {
      Vec3 a =
          SphereRenderer.point(
              radius + SURFACE_OFFSET, i * SphereFormation.TAU / ARC_SEGMENTS, latitude);
      Vec3 b =
          SphereRenderer.point(
              radius + SURFACE_OFFSET, (i + 1) * SphereFormation.TAU / ARC_SEGMENTS, latitude);
      glow(v, m, a, b, color, alpha);
    }
  }

  private static void meridian(
      VertexConsumer v,
      Matrix4f m,
      int radius,
      double azimuth,
      double start,
      double end,
      int color,
      float alpha) {
    for (int i = 0; i < ARC_SEGMENTS / 2; i++) {
      Vec3 a =
          SphereRenderer.point(
              radius + SURFACE_OFFSET, azimuth, start + (end - start) * i * 2 / ARC_SEGMENTS);
      Vec3 b =
          SphereRenderer.point(
              radius + SURFACE_OFFSET, azimuth, start + (end - start) * (i + 1) * 2 / ARC_SEGMENTS);
      glow(v, m, a, b, color, alpha);
    }
  }

  private static void glow(VertexConsumer v, Matrix4f m, Vec3 a, Vec3 b, int color, float alpha) {
    SphereRenderer.ray(v, m, a, b, color, HALO_WIDTH, .12f * alpha);
    SphereRenderer.ray(v, m, a, b, CORE_COLOR, CORE_WIDTH, .75f * alpha);
  }
}
