package com.zerotheabsolute.fieldemitters;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Trusted server automation. Commands enforce operator access before obtaining this handle. */
public final class FieldHandle {
  private final ServerLevel level;
  private final BlockPos position;
  private final java.util.UUID identity;
  FieldHandle(ServerLevel level, BlockPos position) {
    this.level = level; this.position = position.immutable();
    if (!level.getServer().isSameThread() || !level.hasChunkAt(position)
        || !(level.getBlockEntity(position) instanceof EmitterEntity emitter))
      throw new IllegalArgumentException("Expected a loaded emitter on the server thread");
    identity = emitter.emitterId;
  }

  private EmitterEntity emitter() {
    if (!level.getServer().isSameThread()) throw new IllegalStateException("Field operations require the server thread");
    if (!level.hasChunkAt(position) || !(level.getBlockEntity(position) instanceof EmitterEntity emitter))
      throw new IllegalArgumentException("No loaded field emitter at " + position.toShortString());
    if (!identity.equals(emitter.emitterId)) throw new IllegalStateException("The emitter was replaced; get a new field handle");
    return emitter;
  }
  public ServerLevel getLevel() { return level; }
  public BlockPos getPosition() { return position; }
  public boolean isEnabled() { return emitter().enabled; }
  public boolean isPowered() { return emitter().powered; }
  private EmitterEntity detector() {
    var emitter = emitter();
    return emitter.isRail() && level.hasChunkAt(emitter.root)
        && level.getBlockEntity(emitter.root) instanceof EmitterEntity root ? root : emitter;
  }
  public long getCrossings() { return detector().crossings; }
  public int getSignal() { return detector().outputSignal; }
  public String getStatus() { return emitter().operationStatus; }
  public String getPreset() { return emitter().presetId; }
  public boolean isLocked() { return emitter().presetLocked; }
  public String exportPreset() { return FieldPresets.export(emitter()); }

  public void setEnabled(boolean enabled) {
    var emitter = emitter(); emitter.enabled = enabled; NetworkSettings.edited(level, emitter);
  }
  public void unlock() {
    var emitter = emitter(); emitter.presetLocked = false; NetworkSettings.edited(level, emitter);
  }
  public void applyPreset(String id) {
    var preset = FieldPresets.get(id);
    var emitter = emitter();
    ManagedFields.refresh(level, FieldNetwork.loaded(level));
    var controls = emitter.controls.save(); controls.merge(preset.controls().copy());
    var updated = ControlSettings.load(controls);
    if (updated.sphereRadius != emitter.controls.sphereRadius || updated.dome != emitter.controls.dome)
      emitter.transition = level.getGameTime();
    emitter.controls = updated;
    emitter.mask = updated.barrier.groups;
    emitter.adoptPending = false;
    emitter.passages.clear();
    if (preset.color() != null) emitter.color = preset.color();
    if (preset.enabled() != null) emitter.enabled = preset.enabled();
    emitter.presetId = id;
    emitter.presetLocked = preset.locked();
    // A locked preset must not be bypassed by an older per-connection exception.
    if (preset.locked()) for (var member : FieldNetwork.configurable(emitter)) {
      member.overrides.clear(); member.sync();
    }
    NetworkSettings.edited(level, emitter);
  }
}
