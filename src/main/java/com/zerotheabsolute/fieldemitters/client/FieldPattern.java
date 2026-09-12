package com.zerotheabsolute.fieldemitters.client;
import com.zeromods.core.animation.HexFieldPattern;
/** Mod preset; Core owns the seamless pattern and impact animation. */
final class FieldPattern {
  interface Stroke extends HexFieldPattern.Stroke {}
  static float progress(com.zerotheabsolute.fieldemitters.EmitterEntity e, float age) {
    return e.controls.formation == 0 ? 1 : Math.max(0, Math.min(1, e.powered ? age / 40 : 1-age/40));
  }
  static void render(float left, float right, float bottom, float top, float time,
      float impactAge, float impactU, float impactV, int color, com.zerotheabsolute.fieldemitters.ControlSettings settings, float progress, Stroke stroke) {
    com.zeromods.core.animation.EnergySurface.render(left, right, bottom, top, time, impactAge, impactU, impactV, color, settings.particleColor, settings.pattern, settings.formation, progress, stroke);
  }
}
