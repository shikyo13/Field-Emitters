package com.zerotheabsolute.fieldemitters;

import com.zeromods.core.network.ManagedNetwork;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;

/** Saved group settings survive unloaded members; block copies are used by rendering and collision. */
final class NetworkSettings {
  private static final String KEY = "fieldemitters:settings";
  private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

  private NetworkSettings() {}

  static CompoundTag snapshot(EmitterEntity e) {
    var tag = new CompoundTag();
    tag.put("Controls", e.controls.save());
    tag.putInt("Color", e.color);
    tag.putString("Preset", e.presetId);
    tag.putBoolean("PresetLocked", e.presetLocked);
    tag.putBoolean("Enabled", e.enabled);
    tag.putLong("Revision", e.settingsRevision);
    tag.putLong("Origin", e.getBlockPos().asLong());
    return tag;
  }

  static Map<BlockPos, CompoundTag> saved(List<ManagedNetwork.Snapshot<BlockPos>> networks) {
    var result = new HashMap<BlockPos, CompoundTag>();
    for (var network : networks) {
      var value = network.properties().get(KEY);
      if (value == null) continue;
      try {
        var tag = TagParser.parseTag(value);
        if (!tag.contains("Controls", Tag.TAG_COMPOUND)) continue;
        // Copies saved by earlier versions lack newer settings. Normalise them, or they never
        // equal a member's settings and are re-applied on every rebuild.
        tag.put("Controls", ControlSettings.load(tag.getCompound("Controls")).save());
        for (var pos : network.nodes()) result.put(pos, tag);
      } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) {
        LOGGER.warn("Cannot read settings for field network {}", network.id(), ex);
      }
    }
    return result;
  }

  static void reconcile(List<EmitterEntity> loaded, Map<BlockPos, CompoundTag> saved) {
    var groups = new HashMap<ManagedNetwork<BlockPos>, List<EmitterEntity>>();
    for (var e : loaded)
      if (e.managedNetwork != null)
        groups.computeIfAbsent(e.managedNetwork, key -> new ArrayList<>()).add(e);
    for (var entry : groups.entrySet()) {
      var members = entry.getValue();
      if (members.size() == 1 && members.get(0).adoptPending
          && !saved.containsKey(members.get(0).getBlockPos())) continue;
      // Legacy saves have no revision. Prefer an established member, then placement order.
      var template = members.stream().min(Comparator
          .comparing((EmitterEntity e) -> e.adoptPending)
          .thenComparingLong(e -> e.placedAt)
          .thenComparingLong(e -> e.getBlockPos().asLong())).orElseThrow();
      var chosen = snapshot(template);
      for (var e : members) {
        var prior = saved.get(e.getBlockPos());
        if (prior != null && (prior.getLong("Revision") >= chosen.getLong("Revision")))
          chosen = newer(chosen, prior);
        if (!e.adoptPending && e.settingsRevision > chosen.getLong("Revision"))
          chosen = snapshot(e);
      }
      if (chosen.getLong("Revision") == 0) chosen.putLong("Revision", 1);
      entry.getKey().property(KEY, chosen.toString());
      for (var e : members) apply(e, chosen);
    }
  }

  private static CompoundTag newer(CompoundTag a, CompoundTag b) {
    int revision = Long.compare(a.getLong("Revision"), b.getLong("Revision"));
    if (revision != 0) return revision > 0 ? a : b;
    return a.getLong("Origin") <= b.getLong("Origin") ? a : b;
  }

  private static void apply(EmitterEntity e, CompoundTag tag) {
    boolean changed = !e.presetId.equals(tag.getString("Preset")) || e.presetLocked != tag.getBoolean("PresetLocked") || e.adoptPending || e.settingsRevision != tag.getLong("Revision")
        || e.color != tag.getInt("Color") || e.enabled != tag.getBoolean("Enabled")
        || !e.controls.save().equals(tag.getCompound("Controls"));
    if (!changed) return;
    e.presetId = tag.getString("Preset");
    e.presetLocked = tag.getBoolean("PresetLocked");
    if (e.presetLocked) e.overrides.clear();
    e.adoptPending = false;
    e.settingsRevision = tag.getLong("Revision");
    e.controls = ControlSettings.load(tag.getCompound("Controls"));
    e.color = tag.getInt("Color");
    e.enabled = tag.getBoolean("Enabled");
    e.mask = e.controls.barrier.groups;
    e.passages.clear();
    e.getLevel().updateNeighborsAt(e.getBlockPos(), e.getBlockState().getBlock());
    e.sync();
  }

  /** Called after an authorized edit; update the durable copy before another chunk can load. */
  static void edited(ServerLevel level, EmitterEntity seed) {
    var parts = FieldNetwork.configurable(seed);
    long revision = Math.max(level.getGameTime(),
        parts.stream().mapToLong(e -> e.settingsRevision).max().orElse(0) + 1);
    seed.settingsRevision = revision;
    var tag = snapshot(seed);
    if (seed.managedNetwork != null) seed.managedNetwork.property(KEY, tag.toString());
    for (var e : parts) apply(e, tag);
    seed.sync();
    ManagedFields.dirty(level);
  }
}
