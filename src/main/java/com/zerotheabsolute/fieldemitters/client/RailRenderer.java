package com.zerotheabsolute.fieldemitters.client;

import com.mojang.blaze3d.vertex.*;
import com.zerotheabsolute.fieldemitters.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class RailRenderer {
  public static void render(
      EmitterEntity e, float partial, PoseStack pose, MultiBufferSource buffers) {
    if (!e.controls.visible) return;
    float age = e.getLevel().getGameTime() + partial - e.transition;
    var v = buffers.getBuffer(FieldRenderType.ENERGY);
    var m = pose.last().pose();
    for (var l : e.links) {
      float end =
          e.powered
              ? Math.min(l.length() + .5f, age / 2)
              : -.5f + (l.length() + 1) * FieldShutdown.remaining(age);
      if (e.controls.formation != 0) end = l.length() + .5f;
      if (end <= -.5 || FieldPattern.progress(e, age) <= 0) continue;
      var along = new Vec3(l.dx(), l.dy(), l.dz());
      var normal =
          Vec3.atLowerCornerOf(
              Direction.fromAxisAndDirection(l.normal(), Direction.AxisDirection.POSITIVE)
                  .getNormal());
      var across = normal.cross(along);

      if (!FieldPattern.projected(e) && !joined(e, l, across.scale(-1)))
        line(
            v,
            m,
            along,
            across,
            -.5f,
            -.49f,
            end,
            -.49f,
            .012f,
            e.color,
            .75f * FieldPattern.progress(e, age));
      if (!FieldPattern.projected(e) && !joined(e, l, across))
        line(
            v,
            m,
            along,
            across,
            -.5f,
            .49f,
            end,
            .49f,
            .012f,
            e.color,
            .75f * FieldPattern.progress(e, age));
      // The same world-space lattice and clock are used by every coplanar strip.
      float time = e.controls.animation ? e.getLevel().getGameTime() + partial : 0;
      var origin = l.origin(e.getBlockPos()).subtract(Vec3.atLowerCornerOf(e.root));
      var uAxis = l.normal() == Direction.Axis.X ? new Vec3(0, 0, 1) : new Vec3(1, 0, 0);
      var vAxis = l.normal() == Direction.Axis.Y ? new Vec3(0, 0, 1) : new Vec3(0, 1, 0);
      float u0 = (float) origin.dot(uAxis), v0 = (float) origin.dot(vAxis);
      float au = (float) along.dot(uAxis), av = (float) along.dot(vAxis);
      float bu = (float) across.dot(uAxis), bv = (float) across.dot(vAxis);
      float left = u0 + Math.min(-.5f * au, end * au) - .5f * Math.abs(bu);
      float right = u0 + Math.max(-.5f * au, end * au) + .5f * Math.abs(bu);
      float bottom = v0 + Math.min(-.5f * av, end * av) - .5f * Math.abs(bv);
      float top = v0 + Math.max(-.5f * av, end * av) + .5f * Math.abs(bv);
      final float projectedEnd = end;
      FieldPattern.Stroke stroke =
          (x1, y1, x2, y2, w, color, alpha) ->
              clippedLine(
                  v,
                  m,
                  along,
                  across,
                  (x1 - u0) * au + (y1 - v0) * av,
                  (x1 - u0) * bu + (y1 - v0) * bv,
                  (x2 - u0) * au + (y2 - v0) * av,
                  (x2 - u0) * bu + (y2 - v0) * bv,
                  w,
                  color,
                  alpha,
                  -.5f,
                  projectedEnd,
                  -.5f,
                  .5f);
      if (FieldPattern.projected(e)) {
        var frame = projectionFrame(e, l, across, uAxis, vAxis);
        FieldPattern.project(
            left,
            right,
            bottom,
            top,
            time,
            FieldPattern.waves(e, l, uAxis, vAxis, partial),
            e.color,
            e.controls,
            FieldPattern.progress(e, age),
            frame,
            (x, y, X, Y, color, a, b, c, d) -> {
              vertex(
                  v,
                  m,
                  along,
                  across,
                  (x - u0) * au + (y - v0) * av,
                  (x - u0) * bu + (y - v0) * bv,
                  color,
                  a);
              vertex(
                  v,
                  m,
                  along,
                  across,
                  (X - u0) * au + (y - v0) * av,
                  (X - u0) * bu + (y - v0) * bv,
                  color,
                  b);
              vertex(
                  v,
                  m,
                  along,
                  across,
                  (X - u0) * au + (Y - v0) * av,
                  (X - u0) * bu + (Y - v0) * bv,
                  color,
                  c);
              vertex(
                  v,
                  m,
                  along,
                  across,
                  (x - u0) * au + (Y - v0) * av,
                  (x - u0) * bu + (Y - v0) * bv,
                  color,
                  d);
            },
            stroke);
      } else
        FieldPattern.render(
            left,
            right,
            bottom,
            top,
            time,
            FieldPattern.waves(e, l, uAxis, vAxis, partial),
            e.color,
            e.controls,
            FieldPattern.progress(e, age),
            stroke);
      if (!FieldPattern.projected(e))
        line(
            v,
            m,
            along,
            across,
            end,
            -.5f,
            end,
            .5f,
            .025f,
            e.color,
            .9f * FieldPattern.progress(e, age));
    }
  }

  private static com.zeromods.core.animation.PlanarProjection.Frame projectionFrame(
      EmitterEntity e, EmitterEntity.Link link, Vec3 across, Vec3 uAxis, Vec3 vAxis) {
    int low = 0, high = 0;
    while (joined(e, link, across.scale(low - 1))) low--;
    while (joined(e, link, across.scale(high + 1))) high++;
    Vec3 origin = link.origin(e.getBlockPos()).subtract(Vec3.atLowerCornerOf(e.root)),
        along = new Vec3(link.dx(), link.dy(), link.dz());
    Vec3 a = origin.add(along.scale(-.5)).add(across.scale(low - .5));
    Vec3 b = origin.add(along.scale(link.length() + .5)).add(across.scale(high + .5));
    Vec3 source = origin.add(along.scale(-.5)).add(across.scale((low + high) / 2.0));
    return new com.zeromods.core.animation.PlanarProjection.Frame(
        (float) Math.min(a.dot(uAxis), b.dot(uAxis)),
        (float) Math.max(a.dot(uAxis), b.dot(uAxis)),
        (float) Math.min(a.dot(vAxis), b.dot(vAxis)),
        (float) Math.max(a.dot(vAxis), b.dot(vAxis)),
        (float) source.dot(uAxis),
        (float) source.dot(vAxis),
        link.normal() == Direction.Axis.Y);
  }

  private static boolean joined(EmitterEntity e, EmitterEntity.Link link, Vec3 offset) {
    var delta =
        new net.minecraft.core.BlockPos(
            (int) Math.round(offset.x), (int) Math.round(offset.y), (int) Math.round(offset.z));
    var pos = e.getBlockPos().offset(delta);
    return e.getLevel().getBlockEntity(pos) instanceof EmitterEntity other
        && other.powered == e.powered
        && other.controls.visible
        && other.color == e.color
        && other.controls.pattern == e.controls.pattern
        && other.controls.formation == e.controls.formation
        && other.controls.animation == e.controls.animation
        && other.controls.accentColor(other.color) == e.controls.accentColor(e.color)
        && other.links.stream()
            .anyMatch(
                l -> l.target().equals(link.target().offset(delta)) && l.normal() == link.normal());
  }

  private static void clippedLine(
      VertexConsumer v,
      Matrix4f m,
      Vec3 a,
      Vec3 b,
      float x1,
      float y1,
      float x2,
      float y2,
      float w,
      int color,
      float alpha,
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
    if (hi > lo)
      line(v, m, a, b, x1 + dx * lo, y1 + dy * lo, x1 + dx * hi, y1 + dy * hi, w, color, alpha);
  }

  private static void panel(
      VertexConsumer v,
      Matrix4f m,
      Vec3 a,
      Vec3 b,
      float x,
      float y,
      float X,
      float Y,
      int c,
      float alpha) {
    vertex(v, m, a, b, x, y, c, alpha);
    vertex(v, m, a, b, X, y, c, alpha);
    vertex(v, m, a, b, X, Y, c, alpha);
    vertex(v, m, a, b, x, Y, c, alpha);
  }

  private static void line(
      VertexConsumer v,
      Matrix4f m,
      Vec3 a,
      Vec3 b,
      float x,
      float y,
      float X,
      float Y,
      float w,
      int c,
      float alpha) {
    double length = Math.hypot(X - x, Y - y);
    if (length == 0) return;
    float px = (float) (-(Y - y) / length * w), py = (float) ((X - x) / length * w);
    vertex(v, m, a, b, x + px, y + py, c, alpha);
    vertex(v, m, a, b, X + px, Y + py, c, alpha);
    vertex(v, m, a, b, X - px, Y - py, c, alpha);
    vertex(v, m, a, b, x - px, y - py, c, alpha);
  }

  private static void vertex(
      VertexConsumer v, Matrix4f m, Vec3 a, Vec3 b, float x, float y, int c, float alpha) {
    var p = a.scale(x).add(b.scale(y)).add(.5, a.y == 0 && b.y == 0 ? 1 : .5, .5);
    v.vertex(m, (float) p.x, (float) p.y, (float) p.z)
        .color((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f, alpha)
        .uv(.5f, .5f)
        .overlayCoords(OverlayTexture.NO_OVERLAY)
        .uv2(LightTexture.FULL_BRIGHT)
        .normal(0, 1, 0).endVertex();
  }
}
