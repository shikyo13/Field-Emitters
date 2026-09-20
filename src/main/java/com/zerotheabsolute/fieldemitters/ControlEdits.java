package com.zerotheabsolute.fieldemitters;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Server transaction boundary for editor changes. */
public final class ControlEdits {
  public static final int REPLY_KIND = 3;
  private static final int MAX_SESSIONS = 32;
  private static final Map<ServerPlayer, LinkedHashMap<UUID, Long>> REQUESTS = new WeakHashMap<>();

  private ControlEdits() {}

  public static CompoundTag scope(EmitterEntity emitter, BlockPos target, boolean link) {
    var scope = emitter.editScope.copy();
    scope.putLong("Source", emitter.getBlockPos().asLong());
    scope.putLong("Target", target.asLong());
    scope.putBoolean("Link", link);
    scope.putLong("TargetPlacedAt", link ? scope.getCompound("Links").getLong(Long.toString(target.asLong())) : emitter.placedAt);
    scope.remove("Links");
    return scope;
  }

  public static CompoundTag describe(EmitterEntity emitter) {
    var tag = new CompoundTag();
    tag.putInt("Protocol", SettingsPatch.PROTOCOL);
    tag.putLong("PlacedAt", emitter.placedAt);
    if (emitter.managedNetwork != null) tag.putUUID("Network", emitter.managedNetwork.id());
    var links = new CompoundTag();
    for (var link : emitter.links)
      if (emitter.getLevel() != null && emitter.getLevel().hasChunkAt(link.target())
          && emitter.getLevel().getBlockEntity(link.target()) instanceof EmitterEntity other)
        links.putLong(Long.toString(link.target().asLong()), other.placedAt);
    tag.put("Links", links);
    return tag;
  }

  public static void handle(FieldControls.Update request, ServerPlayer player) {
    var edit = request.settings();
    if (edit == null || edit.getInt("Protocol") != SettingsPatch.PROTOCOL) {
      FieldControls.reply(player, 2, new CompoundTag(), message("update_required"));
      return;
    }
    if (!edit.hasUUID("Session") || !edit.contains("Request") || edit.toString().length() > SettingsPatch.MAX_PAYLOAD_CHARS) return;
    var sessions = REQUESTS.computeIfAbsent(player, key -> new LinkedHashMap<>());
    var session = edit.getUUID("Session");
    long sequence = edit.getLong("Request");
    if (sequence <= sessions.getOrDefault(session, 0L)) return;
    sessions.put(session, sequence);
    if (sessions.size() > MAX_SESSIONS) sessions.remove(sessions.keySet().iterator().next());
    var level = player.serverLevel();
    if (!level.hasChunkAt(request.pos()) || !(level.getBlockEntity(request.pos()) instanceof EmitterEntity seed)
        || !FieldControls.hasTuner(player) && player.distanceToSqr(request.pos().getX() + .5, request.pos().getY() + .5, request.pos().getZ() + .5) > 144
        || !FieldControls.editable(seed, player)) {
      respond(player, edit, null, request, "unavailable");
      return;
    }
    ManagedFields.refresh(level, FieldNetwork.loaded(level));
    seed.editScope = describe(seed);
    EmitterEntity other = null;
    if (request.linkOnly()) {
      if (seed.links.stream().noneMatch(link -> link.target().equals(request.target()))
          || !level.hasChunkAt(request.target())
          || !(level.getBlockEntity(request.target()) instanceof EmitterEntity endpoint)
          || !FieldControls.editable(endpoint, player)) {
        respond(player, edit, null, request, "unavailable");
        return;
      }
      other = endpoint;
    }
    if (!scope(seed, request.target(), request.linkOnly()).equals(edit.getCompound("Scope"))) {
      respond(player, edit, seed, request, "scope_changed");
      return;
    }
    if (edit.getBoolean("Poll")) { respond(player, edit, seed, request, "applied"); return; }
    boolean override = seed.overrides.containsKey(request.target());
    var settings = request.linkOnly() ? seed.overrides.getOrDefault(request.target(), seed.controls) : seed.controls;
    var current = SettingsPatch.snapshot(settings, seed.color, seed.enabled);
    if (request.linkOnly() && override != edit.getBoolean("Override")) {
      respond(player, edit, seed, request, "conflict");
      return;
    }
    if (request.inherit()) {
      if (!request.linkOnly() || !current.equals(edit.getCompound("Expected"))) {
        respond(player, edit, seed, request, "conflict");
        return;
      }
      if (override) {
        seed.overrides.remove(request.target());
        other.overrides.remove(request.pos());
        seed.passages.clear(); other.passages.clear();
        seed.sync(); other.sync();
      }
      respond(player, edit, seed, request, "applied");
      return;
    }
    try {
      var result = SettingsPatch.apply(current, edit.getList("Changes", 10), true);
      if (result.conflict()) { respond(player, edit, seed, request, "conflict"); return; }
      var merged = result.settings();
      var validated = ControlSettings.load(merged.getCompound("Controls"));
      if (!validated.save().equals(merged.getCompound("Controls")) || FieldControls.validate(validated) != null
          || (merged.getInt("Color") & 0xffffff) != merged.getInt("Color")) {
        respond(player, edit, seed, request, "invalid");
        return;
      }
      if (request.linkOnly()) {
        if (merged.getInt("Color") != seed.color || merged.getBoolean("Enabled") != seed.enabled || request.reset()) {
          respond(player, edit, seed, request, "invalid"); return;
        }
        if (!current.equals(merged)) {
          seed.overrides.put(request.target(), validated);
          other.overrides.put(request.pos(), ControlSettings.load(validated.save()));
          seed.passages.clear(); other.passages.clear();
          seed.sync(); other.sync();
        }
      } else if (!current.equals(merged) || request.reset()) {
        for (var emitter : FieldNetwork.configurable(seed)) {
          if (!FieldControls.editable(emitter, player)) continue;
          if (emitter.isTower() && (emitter.controls.sphereRadius != validated.sphereRadius || emitter.controls.dome != validated.dome))
            emitter.transition = level.getGameTime();
          emitter.controls = ControlSettings.load(validated.save());
          emitter.color = merged.getInt("Color");
          emitter.enabled = merged.getBoolean("Enabled");
          emitter.mask = emitter.controls.barrier.groups;
          emitter.adoptPending = false;
          emitter.passages.clear();
          level.updateNeighborsAt(emitter.getBlockPos(), emitter.getBlockState().getBlock());
          if (request.reset()) {
            emitter.crossings = 0;
            emitter.lastDetection = Component.translatable("message.fieldemitters.detection.none");
          }
        }
        NetworkSettings.edited(level, seed);
      }
      respond(player, edit, seed, request, "applied");
    } catch (IllegalArgumentException ex) {
      respond(player, edit, seed, request, "invalid");
    }
  }

  private static void respond(ServerPlayer player, CompoundTag edit, EmitterEntity seed, FieldControls.Update request, String status) {
    var response = new CompoundTag();
    response.putUUID("Session", edit.getUUID("Session"));
    response.putLong("Request", edit.getLong("Request"));
    response.putString("Status", status);
    if (seed != null) {
      response.put("EmitterScope", describe(seed));
      response.put("SharedControls", seed.controls.save());
      response.put("Scope", scope(seed, request.target(), request.linkOnly()));
      response.putBoolean("Override", seed.overrides.containsKey(request.target()));
      response.put("Snapshot", SettingsPatch.snapshot(request.linkOnly() ? seed.overrides.getOrDefault(request.target(), seed.controls) : seed.controls, seed.color, seed.enabled));
      response.putLong("Revision", seed.settingsRevision);
    }
    FieldControls.reply(player, REPLY_KIND, response, message(status));
  }

  private static Component message(String status) {
    return Component.translatable("message.fieldemitters.edit." + status);
  }
}
