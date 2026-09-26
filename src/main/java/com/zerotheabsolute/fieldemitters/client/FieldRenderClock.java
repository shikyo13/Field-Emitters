package com.zerotheabsolute.fieldemitters.client;

import com.zerotheabsolute.fieldemitters.EmitterEntity;
import java.util.IdentityHashMap;
import java.util.Map;

/** Detached previews use their lesson clock without changing the world's clock. */
final class FieldRenderClock {
  private static final Map<EmitterEntity, Float> PREVIEWS = new IdentityHashMap<>();
  private FieldRenderClock() {}

  static float time(EmitterEntity emitter, float partial) {
    Float preview = PREVIEWS.get(emitter);
    return preview != null ? preview : emitter.getLevel().getGameTime() + partial;
  }

  static boolean preview(EmitterEntity emitter) { return PREVIEWS.containsKey(emitter); }

  static void preview(EmitterEntity emitter, float ticks, Runnable draw) {
    Float previous = PREVIEWS.put(emitter, ticks);
    try { draw.run(); }
    finally {
      if (previous == null) PREVIEWS.remove(emitter);
      else PREVIEWS.put(emitter, previous);
    }
  }
}
