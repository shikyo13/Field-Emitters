package com.zerotheabsolute.fieldemitters;

import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/** Loader-independent server events; optional scripting adapters install the listener. */
public final class FieldAutomation {
  private static Consumer<Event> listener;
  private static java.util.function.Predicate<String> interested = type -> false;
  private static final ThreadLocal<Boolean> DISPATCHING = ThreadLocal.withInitial(() -> false);
  private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

  private FieldAutomation() {}
  static boolean interested(String type) { return listener != null && interested.test(type); }
  public static void listen(java.util.function.Predicate<String> accepts, Consumer<Event> callback) { interested = accepts; listener = callback; }

  public static final class Event {
    public final String type;
    public final FieldHandle field;
    public final Entity entity;
    public final String direction;
    public final int count;
    private Boolean blocked;
    private boolean decided;
    boolean hasDecision() { return decided; }

    private Event(String type, EmitterEntity emitter, Entity entity, Direction direction, int count, Boolean blocked) {
      this.type = type;
      this.field = new FieldHandle((ServerLevel)emitter.getLevel(), emitter.getBlockPos());
      this.entity = entity;
      this.direction = direction == null ? "" : direction.getName();
      this.count = count;
      this.blocked = blocked;
    }
    public boolean isBlocked() { return Boolean.TRUE.equals(blocked); }
    public void allow() { decide(false); }
    public void deny() { decide(true); }
    private void decide(boolean value) {
      if (!type.equals("passage")) throw new IllegalStateException("Only passage events can change access");
      blocked = value; decided = true;
    }
  }

  public static void emit(String type, EmitterEntity emitter, Entity entity, Direction direction, int count) {
    if (listener != null && interested.test(type) && emitter.getLevel() instanceof ServerLevel)
      dispatch(new Event(type, emitter, entity, direction, count, null));
  }

  public static boolean passage(EmitterEntity emitter, Entity entity, Direction direction, boolean blocked) {
    Boolean cached = ScriptPassage.cached(emitter, entity, direction);
    if (cached != null) return cached;
    var event = decide(emitter, entity, direction, blocked);
    return event == null ? blocked : event.isBlocked();
  }

  static Event decide(EmitterEntity emitter, Entity entity, Direction direction, boolean blocked) {
    if (!interested("passage") || !(emitter.getLevel() instanceof ServerLevel)) return null;
    var event = new Event("passage", emitter, entity, direction, 0, blocked);
    dispatch(event);
    return event;
  }

  private static void dispatch(Event event) {
    if (DISPATCHING.get()) return;
    DISPATCHING.set(true);
    try { listener.accept(event); }
    catch (RuntimeException ex) { LOGGER.error("Field script event {} failed", event.type, ex); }
    finally { DISPATCHING.set(false); }
  }
}
