package com.zerotheabsolute.fieldemitters;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.zerotheabsolute.fieldemitters.network.NativeNetwork;

public final class FieldControls {
  public static Consumer<EmitterEntity> open = e -> {};
  public static Consumer<RemoteData> remoteData = data -> {};

  public static boolean hasTuner(net.minecraft.world.entity.player.Player player) {
    return BadgeAccess.carried(player).stream().anyMatch(stack -> stack.is(FieldEmitters.TUNER.get()));
  }

  public record RemoteRequest(boolean list, BlockPos pos) implements CustomPacketPayload {
    public static final Type<RemoteRequest> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(FieldEmitters.ID, "remote_request"));
    public static final StreamCodec<FriendlyByteBuf, RemoteRequest> CODEC =
        StreamCodec.of(
            (b, p) -> {
              b.writeBoolean(p.list);
              b.writeBlockPos(p.pos);
            },
            b -> new RemoteRequest(b.readBoolean(), b.readBlockPos()));

    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record Rename(BlockPos pos, String name) implements CustomPacketPayload {
    public static final Type<Rename> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(FieldEmitters.ID, "rename"));
    public static final StreamCodec<FriendlyByteBuf, Rename> CODEC =
        StreamCodec.of(
            (b, p) -> {
              b.writeBlockPos(p.pos);
              b.writeUtf(p.name, 32);
            },
            b -> new Rename(b.readBlockPos(), b.readUtf(32)));

    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public record RemoteData(int kind, CompoundTag data, String message)
      implements CustomPacketPayload {
    public static final Type<RemoteData> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(FieldEmitters.ID, "remote_data"));
    public static final StreamCodec<FriendlyByteBuf, RemoteData> CODEC =
        StreamCodec.of(
            (b, p) -> {
              b.writeInt(p.kind);
              b.writeNbt(p.data);
              b.writeUtf(p.message, 256);
            },
            b -> new RemoteData(b.readInt(), b.readNbt(), b.readUtf(256)));

    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  private static void reply(
      net.minecraft.world.entity.player.Player player, int kind, CompoundTag data, String message) {
    com.zerotheabsolute.fieldemitters.network.NativeNetwork.sendToPlayer(
        (net.minecraft.server.level.ServerPlayer) player, new RemoteData(kind, data, message));
  }

  public static void openRemote(net.minecraft.world.entity.player.Player player) {
    remote(new RemoteRequest(true, BlockPos.ZERO), player);
  }

  private static void remote(
      RemoteRequest request, net.minecraft.world.entity.player.Player player) {
    if (!hasTuner(player)) {
      reply(player, 2, new CompoundTag(), "Carry or equip a Field Tuner to manage emitters remotely.");
      return;
    }
    var level = player.level();
    if (request.list) {
      ManagedFields.refresh((net.minecraft.server.level.ServerLevel) level, FieldNetwork.loaded(level));
      var data = new CompoundTag();
      var entries = new net.minecraft.nbt.ListTag();
      var all =
          FieldNetwork.loaded(level).stream()
              .filter(e -> editable(e, player))
              .sorted(
                  java.util.Comparator.comparingDouble(
                      e -> e.getBlockPos().distToCenterSqr(player.position())))
              .toList();
      var seen = new java.util.HashSet<BlockPos>();
      for (var seed : all) {
        if (seen.contains(seed.getBlockPos())) continue;
        var members =
            FieldNetwork.configurable(seed).stream()
                .filter(e -> editable(e, player))
                .sorted(
                    java.util.Comparator.comparingLong((EmitterEntity e) -> e.placedAt)
                        .thenComparingLong(e -> e.getBlockPos().asLong()))
                .toList();
        var anchor = members.getFirst();
        var entry = new CompoundTag();
        if (anchor.managedNetwork != null) entry.putUUID("NetworkId", anchor.managedNetwork.id());
        entry.putLong("Pos", anchor.getBlockPos().asLong());
        entry.putLong("Origin", (anchor.managedNetwork == null ? anchor.getBlockPos() : anchor.managedNetwork.anchor().orElse(anchor.getBlockPos())).asLong());
        entry.putString(
            "Name",
            anchor.managedNetwork != null ? anchor.managedNetwork.name() :
                anchor.fieldName.isBlank() ? "Field at " + anchor.getBlockPos().toShortString() : anchor.fieldName);
        var parts = new net.minecraft.nbt.ListTag();
        int running = 0;
        for (var e : members) {
          seen.add(e.getBlockPos());
          var part = new CompoundTag();
          part.putLong("Pos", e.getBlockPos().asLong());
          part.putBoolean("Rail", e.isRail());
          part.putBoolean("Powered", e.powered);
          parts.add(part);
          if (e.powered) running++;
        }
        entry.put("Members", parts);
        entry.putInt("Running", running);
        entries.add(entry);
        if (entries.size() >= 256) break;
      }
      data.put("Entries", entries);
      data.putString("Dimension", level.dimension().location().toString());
      reply(player, 0, data, "Connected loaded emitters form one field. Select a field.");
    } else {
      if (!level.hasChunkAt(request.pos)
          || !(level.getBlockEntity(request.pos) instanceof EmitterEntity e)
          || !editable(e, player)) {
        reply(
            player,
            2,
            new CompoundTag(),
            "Emitter unavailable or access denied. Refresh the list.");
        return;
      }
      var data = new CompoundTag();
      data.putLong("Pos", e.getBlockPos().asLong());
      data.put("State", net.minecraft.nbt.NbtUtils.writeBlockState(e.getBlockState()));
      data.put("Emitter", e.getUpdateTag(level.registryAccess()));
      reply(player, 1, data, "");
    }
  }

  public record Update(
      BlockPos pos,
      CompoundTag settings,
      int color,
      boolean enabled,
      boolean network,
      boolean reset,
      BlockPos target,
      boolean linkOnly)
      implements CustomPacketPayload {
    public static final Type<Update> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(FieldEmitters.ID, "controls"));
    public static final StreamCodec<FriendlyByteBuf, Update> CODEC =
        StreamCodec.of(
            (b, p) -> {
              b.writeBlockPos(p.pos);
              b.writeNbt(p.settings);
              b.writeInt(p.color);
              b.writeBoolean(p.enabled);
              b.writeBoolean(p.network);
              b.writeBoolean(p.reset);
              b.writeBlockPos(p.target);
              b.writeBoolean(p.linkOnly);
            },
            b ->
                new Update(
                    b.readBlockPos(),
                    b.readNbt(),
                    b.readInt(),
                    b.readBoolean(),
                    b.readBoolean(),
                    b.readBoolean(),
                    b.readBlockPos(),
                    b.readBoolean()));

    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public static void register(NativeNetwork event) {
    event
        
        .playToServer(
            Rename.TYPE,
            Rename.CODEC,
            (p, c) ->
                c.enqueueWork(
                    () -> {
                      var player = c.player();
                      var level = player.level();
                      if (!hasTuner(player)
                          || !level.hasChunkAt(p.pos)
                          || !(level.getBlockEntity(p.pos) instanceof EmitterEntity seed)
                          || !editable(seed, player)) {
                        reply(
                            player,
                            2,
                            new CompoundTag(),
                            "Cannot rename: carry or equip a tuner and use an accessible loaded field.");
                        return;
                      }
                      String name = p.name.strip();
                      if (name.chars().anyMatch(ch -> Character.isISOControl(ch) || ch == 167)) {
                        reply(player, 2, new CompoundTag(), "Use plain text for the field name.");
                        return;
                      }
                      for (var e : FieldNetwork.configurable(seed))
                        if (editable(e, player)) {
                          ManagedFields.rename((net.minecraft.server.level.ServerLevel) level, e, name);
                          e.fieldName = name;
                          e.sync();
                        }
                      reply(player, 2, new CompoundTag(), "Field name updated.");
                    }));
    event
        
        .playToServer(
            RemoteRequest.TYPE,
            RemoteRequest.CODEC,
            (p, c) -> c.enqueueWork(() -> remote(p, c.player())))
        .playToClient(
            RemoteData.TYPE, RemoteData.CODEC, (p, c) -> c.enqueueWork(() -> remoteData.accept(p)));
    event
        
        .playToServer(
            Update.TYPE,
            Update.CODEC,
            (p, context) ->
                context.enqueueWork(
                    () -> {
                      var player = context.player();
                      var level = player.level();
                      if (p.settings == null
                          || !level.hasChunkAt(p.pos)
                          || !hasTuner(player)
                              && player.distanceToSqr(
                                      p.pos.getX() + .5, p.pos.getY() + .5, p.pos.getZ() + .5)
                                  > 144
                          || !(level.getBlockEntity(p.pos) instanceof EmitterEntity seed)) {
                        reply(
                            player,
                            2,
                            new CompoundTag(),
                            "Emitter unavailable. Stay nearby or carry or equip a Field Tuner.");
                        return;
                      }
                      if (!editable(seed, player)) {
                        reply(
                            player,
                            2,
                            new CompoundTag(),
                            "Access denied. Ask the field owner for management access.");
                        return;
                      }
                      var validated = ControlSettings.load(p.settings);
                      String error = validate(validated);
                      if (error != null) {
                        reply(player, 2, new CompoundTag(), error);
                        return;
                      }
                      if (p.linkOnly) {
                        if (seed.links.stream().noneMatch(link -> link.target().equals(p.target)))
                          return;
                        if (!level.hasChunkAt(p.target)
                            || !(level.getBlockEntity(p.target) instanceof EmitterEntity other)
                            || !editable(other, player)) return;
                        seed.overrides.put(p.target, validated);
                        other.overrides.put(p.pos, ControlSettings.load(p.settings));
                        seed.passages.clear();
                        other.passages.clear();
                        seed.sync();
                        other.sync();
                        reply(player, 2, new CompoundTag(), "Changes applied.");
                        return;
                      }
                      for (var e :
                          p.network ? FieldNetwork.configurable(seed) : java.util.List.of(seed)) {
                        if (!editable(e, player)) continue;
                        if(e.isTower() && (e.controls.sphereRadius!=validated.sphereRadius || e.controls.dome!=validated.dome)) e.transition=level.getGameTime();
                        e.controls = ControlSettings.load(p.settings);
                        e.color = p.color & 0xffffff;
                        e.enabled = p.enabled;
                        e.mask = e.controls.barrier.groups;
                        e.passages.clear();
                        if (p.reset) {
                          e.crossings = 0;
                          e.queuedPulses = 0;
                          e.lastDetection = "None";
                        }
                        level.updateNeighborsAt(e.getBlockPos(), e.getBlockState().getBlock());
                        e.sync();
                      }
                      reply(player, 2, new CompoundTag(), "Changes applied.");
                    }));
  }

  public static String validate(ControlSettings settings) {
    if (!Float.isFinite(settings.damageAmount) || settings.damageAmount < 0 || settings.damageAmount > 1000)
      return "Damage must be a number from 0 to 1000 HP (2 HP = 1 heart).";
    for(var filter:settings.filters()) { String error=validate(filter);if(error!=null)return error; }
    return null;
  }

  public static String validate(EntityFilter f) {
    if (f.mobMode < 0 || f.mobMode > 2 || f.itemMode < 0 || f.itemMode > 2
        || f.mobList.size() > EntityFilter.MAX_TYPES || f.itemList.size() > EntityFilter.MAX_TYPES)
      return "Mob and item lists support at most 64 entries each.";
    for (String value : f.mobList) { String error = validateTypeEntry(value, false); if (error != null) return error; }
    for (String value : f.itemList) { String error = validateTypeEntry(value, true); if (error != null) return error; }
    if(f.accessGroups.size()>64 || f.accessGroups.stream().anyMatch(g->!BadgeAccess.validGroup(g))) return "Use up to 64 group names: lowercase letters, numbers, spaces, underscores or hyphens.";
    if (f.playerMode < 0 || f.playerMode > 2 || f.playerList.size() > EntityFilter.MAX_PLAYERS)
      return "Player lists support at most 64 entries.";
    if (f.playerList.values().stream().anyMatch(name -> !name.isEmpty() && !name.matches("[A-Za-z0-9_]{1,16}")))
      return "Player names must be Minecraft account names, not display nicknames.";
    for (String value : java.util.List.of(f.entityType, f.itemType))
      if (!value.isEmpty()
          && net.minecraft.resources.ResourceLocation.tryParse(
                  value.startsWith("#") ? value.substring(1) : value)
              == null) return "Invalid ID or tag: " + value;
    if (!f.identity.isEmpty())
      try {
        java.util.UUID.fromString(f.identity);
      } catch (IllegalArgumentException ex) {
        return "Invalid individual UUID.";
      }
    if (!f.entityType.isEmpty()
        && !f.entityType.startsWith("#")
        && !net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(
            net.minecraft.resources.ResourceLocation.parse(f.entityType)))
      return "Unknown entity ID: " + f.entityType;
    if (!f.itemType.isEmpty()
        && !f.itemType.startsWith("#")
        && !net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(
            net.minecraft.resources.ResourceLocation.parse(f.itemType)))
      return "Unknown item ID: " + f.itemType;
    return null;
  }

  public static String validateTypeEntry(String value, boolean item) {
    boolean tag = value.startsWith("#");
    var id = net.minecraft.resources.ResourceLocation.tryParse(tag ? value.substring(1) : value);
    if (value.length() > 128 || id == null) return "Use a registry ID or #tag, such as minecraft:pig or #minecraft:logs.";
    if (!tag && !(item ? net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id)
        : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(id)))
      return "Unknown " + (item ? "item" : "entity") + " ID: " + value;
    if (!tag && !item && (id.toString().equals("minecraft:player")))
      return "Use the player list for players. This list accepts mob types and entity-type tags.";
    return null;
  }

  public static boolean editable(EmitterEntity e, net.minecraft.world.entity.player.Player player) {
    return ManagementAccess.editable(e,player);
  }
}
