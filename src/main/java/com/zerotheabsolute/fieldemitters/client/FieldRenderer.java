package com.zerotheabsolute.fieldemitters.client;

import com.mojang.blaze3d.vertex.*;
import com.zerotheabsolute.fieldemitters.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.*;
import org.joml.Matrix4f;

/** One depth-tested alpha pass: no consumer survives a buffer switch, no additive whiteout. */
public final class FieldRenderer implements BlockEntityRenderer<EmitterEntity> {
  public FieldRenderer(BlockEntityRendererProvider.Context c) {}

  public boolean shouldRenderOffScreen(EmitterEntity e) {
    return true;
  }

  public int getViewDistance() {
    return 128;
  }

  public AABB getRenderBoundingBox(EmitterEntity e) {
    return new AABB(e.getBlockPos())
        .inflate(
            e.isTower() ? SphereField.MAX_RADIUS + 1 : 21,
            e.isTower() ? SphereField.MAX_RADIUS + 1 : 14,
            e.isTower() ? SphereField.MAX_RADIUS + 1 : 21);
  }

  public void render(
      EmitterEntity e,
      float partial,
      PoseStack pose,
      MultiBufferSource buffers,
      int light,
      int overlay) {
    if (e.getLevel() == null) return;
    if (e.isTower()) {
      float time = e.getLevel().getGameTime() + partial;
      float age = time - e.transition;
      float charge = e.powered ? Mth.clamp(age / 12, 0, 1) : Mth.clamp(1 - age / 30, 0, 1);
      HardwareRenderer.render(e, time, charge, pose, buffers, light);
      SphereRenderer.render(e, partial, pose, buffers);
      return;
    }
    if (e.isRail()) {
      FieldGuide.render(e, pose, buffers);
      RailRenderer.render(e, partial, pose, buffers);
      return;
    }
    float time = e.getLevel().getGameTime() + partial;
    float age = time - e.transition;
    float charge = e.powered ? Mth.clamp(age / 12, 0, 1) : Mth.clamp(1 - age / 30, 0, 1);
    HardwareRenderer.render(e, time, charge, pose, buffers, light);
    FieldGuide.render(e, pose, buffers);
    var v = buffers.getBuffer(FieldRenderType.ENERGY);
    var m = pose.last().pose();
    int color = e.color;
    // An exposed lens, segmented calibration orbit and rising charge packets animate the hardware.
    for (int side = 0; side < 4; side++) {
      pose.pushPose();
      pose.translate(.5, 0, .5);
      pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(side * 90));
      var railMatrix = pose.last().pose();
      for (int section = 1; section < 4; section++) {
        float local = Mth.clamp(charge * 4 - section + .8f, 0, 1);
        float y = section + .10f + Mth.frac(time * .018f - section * .21f) * .74f;
        quad(
            v,
            railMatrix,
            -.055f,
            y,
            -.253f,
            .055f,
            y,
            -.253f,
            .055f,
            y + .10f,
            -.253f,
            -.055f,
            y + .10f,
            -.253f,
            color,
            local * .64f);
        quad(
            v,
            railMatrix,
            -.11f,
            y - .025f,
            -.254f,
            .11f,
            y - .025f,
            -.254f,
            .11f,
            y + .125f,
            -.254f,
            -.11f,
            y + .125f,
            -.254f,
            color,
            local * .10f);
      }
      pose.popPose();
    }
    float lensY = 4.68f + charge * .08f + Mth.sin(time * .035f) * .018f * charge;
    for (int ring = 0; ring < 2; ring++)
      for (int i = 0; i < 64; i++) {
        if (i % 16 >= 11) continue;
        double a = i * Math.PI * 2 / 64 + time * .009 * (ring == 0 ? 1 : -1), b = a + .073;
        ring(
            v,
            m,
            a,
            b,
            .22f + ring * .08f,
            lensY + (ring == 0 ? -.04f : .04f),
            color,
            charge * .65f);
      }
    pose.pushPose();
    pose.translate(.5, lensY, .5);
    pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(time * .8f));
    var lensMatrix = pose.last().pose();
    for (int side = 0; side < 4; side++) {
      double a = side * Math.PI / 2, b = (side + 1) * Math.PI / 2;
      float x1 = (float) Math.cos(a) * .125f,
          z1 = (float) Math.sin(a) * .125f,
          x2 = (float) Math.cos(b) * .125f,
          z2 = (float) Math.sin(b) * .125f;
      quad(v, lensMatrix, 0, .21f, 0, x1, 0, z1, x2, 0, z2, 0, .21f, 0, 0xC9F8FF, charge * .68f);
      quad(v, lensMatrix, 0, -.21f, 0, x2, 0, z2, x1, 0, z1, 0, -.21f, 0, color, charge * .65f);
    }
    pose.popPose();
    if (!e.controls.visible) return;
    if (!e.controls.animation) time = 0;
    for (var link : e.links) {
      float extent =
          e.powered ? Math.min(link.length(), age / 2) : Math.max(0, link.length() - age / 2);
      if (e.controls.formation != 0) extent = link.length();
      if (extent <= .5f || FieldPattern.progress(e, age) <= 0) continue;
      for (int i = 0; i <= link.length(); i++) {
        float from = Math.max(0, i - .5f), to = Math.min(Math.min(link.length(), i + .5f), extent);
        if (to <= from) continue;
        float floor = link.ground()[i] - e.getBlockPos().getY() + .035f, top = floor + 4.93f;

        if (FieldPattern.projected(e)) {
          projection(e, link, v, m, from, to, floor, top, time, partial, age);
          continue;
        }
        strip(v, m, link, from, top, to, top, .22f, color, .075f * FieldPattern.progress(e, age));
        strip(v, m, link, from, top, to, top, .075f, color, .22f * FieldPattern.progress(e, age));
        strip(v, m, link, from, top, to, top, .015f, color, .72f * FieldPattern.progress(e, age));
        strip(
            v, m, link, from, floor, to, floor, .15f, color, .12f * FieldPattern.progress(e, age));
        strip(
            v, m, link, from, floor, to, floor, .012f, color, .60f * FieldPattern.progress(e, age));
        if (i > 0 && i < link.length() && link.ground()[i] != link.ground()[i - 1])
          strip(
              v,
              m,
              link,
              from,
              Math.min(floor, link.ground()[i - 1] - e.getBlockPos().getY() + .035f) + 4.93f,
              from,
              top,
              .014f,
              color,
              .60f);
        float hitAge = (float) (e.getLevel().getGameTime() - e.impactTime) + partial;
        float originU =
            link.dx() != 0 ? e.getBlockPos().getX() + .5f : e.getBlockPos().getZ() + .5f;
        float direction = link.dx() != 0 ? link.dx() : link.dz();
        float worldLeft = originU + Math.min(from * direction, to * direction);
        float worldRight = originU + Math.max(from * direction, to * direction);
        float originY = e.getBlockPos().getY();
        boolean hit =
            hitAge >= 0
                && hitAge < 32
                && Math.abs(
                        link.normalCoordinate(e.impact)
                            - link.normalCoordinate(Vec3.atCenterOf(e.getBlockPos())))
                    < .15;
        FieldPattern.render(
            worldLeft,
            worldRight,
            floor + originY,
            top + originY,
            time,
            hit ? hitAge : -1,
            (float) (link.dx() != 0 ? e.impact.x : e.impact.z),
            (float) e.impact.y,
            color,
            e.controls,
            FieldPattern.progress(e, age),
            (x1, y1, x2, y2, w, c, alpha) ->
                clippedStrip(
                    v,
                    m,
                    link,
                    (x1 - originU) * direction,
                    y1 - originY,
                    (x2 - originU) * direction,
                    y2 - originY,
                    w,
                    c,
                    alpha,
                    from,
                    to,
                    floor,
                    top));
        if (e.controls.pattern == 0) {
          float sweep = floor + (time * .018f % 4.93f);
          strip(
              v,
              m,
              link,
              from,
              sweep,
              to,
              sweep,
              .016f,
              color,
              .11f * FieldPattern.progress(e, age));
        }
        if (extent < link.length() && extent >= from && extent <= to) {
          strip(v, m, link, extent, floor, extent, top, .14f, color, .18f);
          strip(v, m, link, extent, floor, extent, top, .025f, 0xE4FCFF, .90f);
        }
      }
    }
  }

  private static void projection(
      EmitterEntity e,
      EmitterEntity.Link link,
      VertexConsumer v,
      Matrix4f m,
      float from,
      float to,
      float floor,
      float top,
      float time,
      float partial,
      float age) {
    float originU = link.dx() != 0 ? e.getBlockPos().getX() + .5f : e.getBlockPos().getZ() + .5f;
    float direction = link.dx() != 0 ? link.dx() : link.dz(), originY = e.getBlockPos().getY();
    float left = originU + Math.min(from * direction, to * direction),
        right = originU + Math.max(from * direction, to * direction);
    var frame =
        new com.zeromods.core.animation.PlanarProjection.Frame(
            Math.min(originU, originU + link.length() * direction),
            Math.max(originU, originU + link.length() * direction),
            floor + originY,
            top + originY,
            originU,
            top + originY - .25f,
            false);
    float hitAge = (float) (e.getLevel().getGameTime() - e.impactTime) + partial;
    boolean hit =
        Math.abs(
                link.normalCoordinate(e.impact)
                    - link.normalCoordinate(Vec3.atCenterOf(e.getBlockPos())))
            < .15;
    FieldPattern.project(
        left,
        right,
        floor + originY,
        top + originY,
        time,
        hit ? hitAge : -1,
        (float) (link.dx() != 0 ? e.impact.x : e.impact.z),
        (float) e.impact.y,
        e.color,
        e.controls,
        FieldPattern.progress(e, age),
        frame,
        (x, y, X, Y, c, a, b, d, f) -> {
          projectionVertex(v, m, link, (x - originU) * direction, y - originY, c, a);
          projectionVertex(v, m, link, (X - originU) * direction, y - originY, c, b);
          projectionVertex(v, m, link, (X - originU) * direction, Y - originY, c, d);
          projectionVertex(v, m, link, (x - originU) * direction, Y - originY, c, f);
        },
        (x, y, X, Y, w, c, a) ->
            clippedStrip(
                v,
                m,
                link,
                (x - originU) * direction,
                y - originY,
                (X - originU) * direction,
                Y - originY,
                w,
                c,
                a,
                from,
                to,
                floor,
                top));
  }

  private static void projectionVertex(
      VertexConsumer v,
      Matrix4f m,
      EmitterEntity.Link link,
      float x,
      float y,
      int color,
      float alpha) {
    vertex(v, m, .5f + x * link.dx(), y, .5f + x * link.dz(), color, alpha);
  }

  private static void clippedStrip(
      VertexConsumer v,
      Matrix4f m,
      EmitterEntity.Link l,
      float x1,
      float y1,
      float x2,
      float y2,
      float w,
      int c,
      float a,
      float left,
      float right,
      float bottom,
      float top) {
    float dx = x2 - x1, dy = y2 - y1, lo = 0, hi = 1;
    if (Math.abs(dx) < .00001) {
      if (x1 < left || x1 > right) return;
    } else {
      float t1 = (left - x1) / dx, t2 = (right - x1) / dx;
      lo = Math.max(lo, Math.min(t1, t2));
      hi = Math.min(hi, Math.max(t1, t2));
    }
    if (Math.abs(dy) < .00001) {
      if (y1 < bottom || y1 > top) return;
    } else {
      float t1 = (bottom - y1) / dy, t2 = (top - y1) / dy;
      lo = Math.max(lo, Math.min(t1, t2));
      hi = Math.min(hi, Math.max(t1, t2));
    }
    if (hi > lo) strip(v, m, l, x1 + dx * lo, y1 + dy * lo, x1 + dx * hi, y1 + dy * hi, w, c, a);
  }

  private static void ring(
      VertexConsumer v, Matrix4f m, double a, double b, float r, float y, int c, float alpha) {
    float w = .017f;
    quad(
        v,
        m,
        .5f + (float) Math.cos(a) * (r - w),
        y,
        .5f + (float) Math.sin(a) * (r - w),
        .5f + (float) Math.cos(b) * (r - w),
        y,
        .5f + (float) Math.sin(b) * (r - w),
        .5f + (float) Math.cos(b) * (r + w),
        y,
        .5f + (float) Math.sin(b) * (r + w),
        .5f + (float) Math.cos(a) * (r + w),
        y,
        .5f + (float) Math.sin(a) * (r + w),
        c,
        alpha);
  }

  private static void panel(
      VertexConsumer v,
      Matrix4f m,
      EmitterEntity.Link l,
      float x1,
      float y1,
      float x2,
      float y2,
      int c,
      float a) {
    plane(v, m, l, x1, y1, x2, y1, x2, y2, x1, y2, c, a);
  }

  private static void strip(
      VertexConsumer v,
      Matrix4f m,
      EmitterEntity.Link l,
      float x1,
      float y1,
      float x2,
      float y2,
      float w,
      int c,
      float a) {
    float dx = x2 - x1, dy = y2 - y1, len = (float) Math.sqrt(dx * dx + dy * dy);
    if (len < .0001) return;
    float px = -dy / len * w, py = dx / len * w;
    plane(v, m, l, x1 + px, y1 + py, x2 + px, y2 + py, x2 - px, y2 - py, x1 - px, y1 - py, c, a);
  }

  private static void plane(
      VertexConsumer v,
      Matrix4f m,
      EmitterEntity.Link l,
      float x1,
      float y1,
      float x2,
      float y2,
      float x3,
      float y3,
      float x4,
      float y4,
      int c,
      float a) {
    quad(
        v,
        m,
        .5f + x1 * l.dx(),
        y1,
        .5f + x1 * l.dz(),
        .5f + x2 * l.dx(),
        y2,
        .5f + x2 * l.dz(),
        .5f + x3 * l.dx(),
        y3,
        .5f + x3 * l.dz(),
        .5f + x4 * l.dx(),
        y4,
        .5f + x4 * l.dz(),
        c,
        a);
  }

  private static void quad(
      VertexConsumer v,
      Matrix4f m,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float x3,
      float y3,
      float z3,
      float x4,
      float y4,
      float z4,
      int c,
      float a) {
    vertex(v, m, x1, y1, z1, c, a);
    vertex(v, m, x2, y2, z2, c, a);
    vertex(v, m, x3, y3, z3, c, a);
    vertex(v, m, x4, y4, z4, c, a);
  }

  private static void vertex(
      VertexConsumer v, Matrix4f m, float x, float y, float z, int c, float a) {
    v.addVertex(m, x, y, z)
        .setColor((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f, a)
        .setUv(.5f, .5f)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(0xF000F0)
        .setNormal(0, 1, 0);
  }
}
