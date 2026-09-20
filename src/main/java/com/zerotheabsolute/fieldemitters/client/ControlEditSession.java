package com.zerotheabsolute.fieldemitters.client;

import com.zerotheabsolute.fieldemitters.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/** One outstanding transaction per editor; replies remain routable while a child screen is open. */
final class ControlEditSession {
  private static final Map<UUID, ControlEditSession> PENDING = new HashMap<>();
  private final UUID id = UUID.randomUUID();
  private final EmitterEntity emitter;
  private final Consumer<FieldControls.Update> send;
  private final Consumer<FieldControls.RemoteData> receive;
  private CompoundTag baseline, scope, sent;
  private BlockPos target;
  private boolean link, override, polling;
  private static final long REPLY_TIMEOUT_NANOS = java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
  private long sequence, outstanding, sentAt;
  private boolean timedOut;

  ControlEditSession(EmitterEntity emitter, Consumer<FieldControls.Update> send, Consumer<FieldControls.RemoteData> receive) {
    PENDING.values().removeIf(session -> session.emitter.getLevel() != emitter.getLevel());
    this.emitter = emitter;
    this.send = send;
    this.receive = receive;
  }

  static void receive(FieldControls.RemoteData reply) {
    var tag = reply.data();
    if (!tag.hasUUID("Session")) return;
    var session = PENDING.get(tag.getUUID("Session"));
    if (session != null && session.outstanding == tag.getLong("Request")) {
      PENDING.remove(session.id);
      session.receive.accept(reply);
    }
  }

  boolean busy() {
    if (outstanding != 0 && System.nanoTime() - sentAt > REPLY_TIMEOUT_NANOS) {
      PENDING.remove(id); outstanding = 0; timedOut = true;
    }
    return outstanding != 0;
  }
  boolean timedOut() { busy(); return timedOut; }
  boolean supported() { return scope != null && scope.getInt("Protocol") == SettingsPatch.PROTOCOL; }

  void select(BlockPos target, boolean link, CompoundTag snapshot) {
    if (busy()) throw new IllegalStateException("Cannot retarget an outstanding edit");
    this.target = target;
    this.link = link;
    baseline = snapshot.copy();
    scope = ControlEdits.scope(emitter, target, link);
    override = link && emitter.overrides.containsKey(target);
  }

  boolean submit(CompoundTag desired, boolean reset, boolean inherit, boolean poll) {
    if (busy() || !supported() || timedOut && !poll) return false;
    var changes = SettingsPatch.between(baseline, desired);
    if (!poll && !reset && !inherit && changes.isEmpty()) return false;
    var edit = new CompoundTag();
    edit.putInt("Protocol", SettingsPatch.PROTOCOL);
    edit.putUUID("Session", id);
    edit.putLong("Request", ++sequence);
    edit.put("Scope", scope.copy());
    edit.putBoolean("Override", override);
    edit.putBoolean("Poll", poll);
    edit.put("Changes", poll ? new net.minecraft.nbt.ListTag() : changes);
    if (inherit) edit.put("Expected", baseline.copy());
    outstanding = sequence;
    sentAt = System.nanoTime();
    sent = (poll ? baseline : desired).copy();
    polling = poll;
    PENDING.put(id, this);
    send.accept(new FieldControls.Update(emitter.getBlockPos(), edit, 0, false, true, reset, target, link, inherit));
    return true;
  }

  record Reconciled(CompoundTag draft, boolean refresh, boolean conflict, boolean poll) {}

  Reconciled acknowledge(FieldControls.RemoteData reply, CompoundTag draft) {
    outstanding = 0;
    timedOut = false;
    var data = reply.data();
    if (!data.contains("Snapshot", 10)) return new Reconciled(draft, false, true, polling);
    boolean scopeChanged = !scope.equals(data.getCompound("Scope"));
    var pending = SettingsPatch.between(sent, draft);
    baseline = data.getCompound("Snapshot").copy();
    scope = data.getCompound("Scope").copy();
    override = data.getBoolean("Override");
    emitter.editScope = data.getCompound("EmitterScope").copy();
    if (data.getLong("Revision") >= emitter.settingsRevision) {
      emitter.settingsRevision = data.getLong("Revision");
      emitter.controls = ControlSettings.load(data.getCompound("SharedControls"));
      emitter.color = baseline.getInt("Color");
      emitter.enabled = baseline.getBoolean("Enabled");
      if (link) {
        if (override) emitter.overrides.put(target, ControlSettings.load(baseline.getCompound("Controls")));
        else emitter.overrides.remove(target);
      }
    }
    var rebased = SettingsPatch.apply(baseline, pending, false);
    boolean conflict = !data.getString("Status").equals("applied") || rebased.conflict();
    return new Reconciled(scopeChanged ? baseline.copy() : rebased.settings(), true, conflict, polling);
  }
}
