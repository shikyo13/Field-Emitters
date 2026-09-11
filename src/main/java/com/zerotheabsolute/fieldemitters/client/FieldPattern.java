package com.zerotheabsolute.fieldemitters.client;

/** World-anchored hex lattice and impact rings shared by posts and surface rails. */
final class FieldPattern {
  @FunctionalInterface
  interface Stroke {
    void draw(float x1, float y1, float x2, float y2, float width, int color, float alpha);
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
      Stroke stroke) {
    boolean hit = impactAge >= 0 && impactAge < 32;
    float radius = impactAge * .115f, fade = 1 - impactAge / 32;
    for (int col = (int) Math.floor(left / .45f) - 1; col <= Math.ceil(right / .45f) + 1; col++)
      for (int row = (int) Math.floor(bottom / .5196f) - 1;
          row <= Math.ceil(top / .5196f) + 1;
          row++) {
        float cx = col * .45f, cy = row * .5196f + Math.floorMod(col, 2) * .2598f;
        float scan =
            (float) Math.pow(Math.max(0, Math.cos(cx * .45f - time * .055f + cy * .32f)), 18);
        float distance = (float) Math.hypot(cx - impactU, cy - impactV);
        float wave = hit ? (float) Math.exp(-Math.pow((distance - radius) / .26f, 2)) * fade : 0;
        for (int k = 0; k < 6; k++) {
          double a = k * Math.PI / 3, b = (k + 1) * Math.PI / 3;
          stroke.draw(
              cx + (float) Math.cos(a) * .30f,
              cy + (float) Math.sin(a) * .30f,
              cx + (float) Math.cos(b) * .30f,
              cy + (float) Math.sin(b) * .30f,
              .008f + wave * .012f,
              wave > .3 ? 0xD0F7FF : color,
              .085f + .075f * scan + .72f * wave);
        }
        if (wave > .1f)
          for (int k = 0; k < 3; k++) {
            double a = k * Math.PI / 3, b = (k + 3) * Math.PI / 3;
            stroke.draw(
                cx + (float) Math.cos(a) * .27f,
                cy + (float) Math.sin(a) * .27f,
                cx + (float) Math.cos(b) * .27f,
                cy + (float) Math.sin(b) * .27f,
                .028f,
                color,
                wave * .13f);
          }
      }
    if (hit)
      for (int k = 0; k < 96; k++) {
        double a = k * Math.PI * 2 / 96, b = (k + 1) * Math.PI * 2 / 96;
        float x1 = impactU + (float) Math.cos(a) * radius,
            y1 = impactV + (float) Math.sin(a) * radius;
        float x2 = impactU + (float) Math.cos(b) * radius,
            y2 = impactV + (float) Math.sin(b) * radius;
        stroke.draw(x1, y1, x2, y2, .14f, color, fade * .12f);
        stroke.draw(x1, y1, x2, y2, .020f, color, fade * .60f);
      }
  }
}
