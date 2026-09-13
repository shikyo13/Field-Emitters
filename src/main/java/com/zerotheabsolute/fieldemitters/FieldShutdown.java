package com.zerotheabsolute.fieldemitters;

public final class FieldShutdown {
  public static final int DURATION_TICKS = 40;

  private FieldShutdown() {}

  public static float remaining(float age) {
    float progress = Math.max(0, Math.min(1, age / DURATION_TICKS));
    return 1 - progress * progress * (3 - 2 * progress);
  }
}
