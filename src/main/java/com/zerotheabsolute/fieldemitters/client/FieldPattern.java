package com.zerotheabsolute.fieldemitters.client;

import com.zeromods.core.animation.HexFieldPattern;

/** Mod preset; Core owns the seamless pattern and impact animation. */
final class FieldPattern {
  interface Stroke extends HexFieldPattern.Stroke {}

  static float progress(com.zerotheabsolute.fieldemitters.EmitterEntity e, float age) {
    if (projected(e))
      return Math.max(
          0,
          Math.min(
              1,
              e.powered
                  ? age / com.zeromods.core.animation.PlanarProjection.DURATION_TICKS
                  : 1 - age / 25));
    return e.controls.formation == 0
        ? 1
        : Math.max(0, Math.min(1, e.powered ? age / 40 : 1 - age / 40));
  }

  static boolean projected(com.zerotheabsolute.fieldemitters.EmitterEntity e) {
    return com.zeromods.core.animation.PlanarProjection.selected(e.controls.formation);
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
      com.zerotheabsolute.fieldemitters.ControlSettings settings,
      float progress,
      com.zeromods.core.animation.PlanarProjection.Frame frame,
      com.zeromods.core.animation.PlanarProjection.Patch patch,
      Stroke stroke) {
    var style = com.zeromods.core.animation.PlanarProjection.style(settings.formation);
    float reveal = settings.animation ? progress : 1, opacity = settings.animation ? 1 : progress;
    com.zeromods.core.animation.PlanarProjection.fill(
        style, frame, left, right, bottom, top, reveal, opacity, color, patch);
    com.zeromods.core.animation.EnergySurface.render(
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
              com.zeromods.core.animation.PlanarProjection.coverage(
                  style, frame, (x1 + x2) / 2, (y1 + y2) / 2, reveal);
          if (mask > 0) stroke.draw(x1, y1, x2, y2, w, c, a * mask * opacity);
        });
    if (settings.animation)
      com.zeromods.core.animation.PlanarProjection.guides(style, frame, progress, color, stroke);
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
      com.zerotheabsolute.fieldemitters.ControlSettings settings,
      float progress,
      Stroke stroke) {
    com.zeromods.core.animation.EnergySurface.render(
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
