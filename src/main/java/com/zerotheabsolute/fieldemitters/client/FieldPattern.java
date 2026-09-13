package com.zerotheabsolute.fieldemitters.client;

import com.zeromods.core.animation.EnergySurface;
import com.zeromods.core.animation.HexFieldPattern;
import com.zeromods.core.animation.PlanarProjection;
import com.zeromods.core.animation.PlasmaSurface;
import com.zerotheabsolute.fieldemitters.ControlSettings;
import com.zerotheabsolute.fieldemitters.EmitterEntity;

/** Mod preset; Core owns the seamless pattern and impact animation. */
final class FieldPattern {
  interface Stroke extends HexFieldPattern.Stroke {}

  static float progress(EmitterEntity e, float age) {
    if (projected(e))
      return Math.max(
          0, Math.min(1, e.powered ? age / PlanarProjection.DURATION_TICKS : 1 - age / 25));
    return e.controls.formation == 0
        ? 1
        : Math.max(0, Math.min(1, e.powered ? age / 40 : 1 - age / 40));
  }

  static boolean projected(EmitterEntity e) {
    return PlanarProjection.selected(e.controls.formation);
  }

  static void project(
      float left,
      float right,
      float bottom,
      float top,
      float time,
      float impactAge,
      float hitU,
      float hitV,
      int color,
      ControlSettings settings,
      float progress,
      PlanarProjection.Frame frame,
      PlanarProjection.Patch patch,
      Stroke stroke) {
    var style = PlanarProjection.style(settings.formation);
    float reveal = settings.animation ? progress : 1, opacity = settings.animation ? 1 : progress;
    PlanarProjection.fill(style, frame, left, right, bottom, top, reveal, opacity, color, patch);
    if (settings.pattern == 3)
      PlasmaSurface.glow(
          left,
          right,
          bottom,
          top,
          time,
          settings.particleColor,
          (a, c, b, d, tint, ac, bc, bd, ad) ->
              patch.draw(
                  a,
                  c,
                  b,
                  d,
                  tint,
                  ac * PlanarProjection.coverage(style, frame, a, c, reveal) * opacity,
                  bc * PlanarProjection.coverage(style, frame, b, c, reveal) * opacity,
                  bd * PlanarProjection.coverage(style, frame, b, d, reveal) * opacity,
                  ad * PlanarProjection.coverage(style, frame, a, d, reveal) * opacity));
    EnergySurface.render(
        left,
        right,
        bottom,
        top,
        time,
        impactAge,
        hitU,
        hitV,
        color,
        settings.particleColor,
        settings.pattern,
        0,
        1,
        false,
        (x1, y1, x2, y2, w, c, a) -> {
          float mask =
              PlanarProjection.coverage(style, frame, (x1 + x2) / 2, (y1 + y2) / 2, reveal);
          if (mask > 0) stroke.draw(x1, y1, x2, y2, w, c, a * mask * opacity);
        });
    if (settings.animation) PlanarProjection.guides(style, frame, progress, color, stroke);
  }

  static void render(
      float left,
      float right,
      float bottom,
      float top,
      float time,
      float impactAge,
      float impactU,
      float impactV,
      int color,
      ControlSettings settings,
      float progress,
      Stroke stroke) {
    EnergySurface.render(
        left,
        right,
        bottom,
        top,
        time,
        impactAge,
        impactU,
        impactV,
        color,
        settings.particleColor,
        settings.pattern,
        settings.formation,
        progress,
        stroke);
  }
}
