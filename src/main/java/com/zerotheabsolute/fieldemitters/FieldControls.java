package com.zerotheabsolute.fieldemitters;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class FieldControls {
  public static Consumer<EmitterEntity> open = e -> {};
  public static Consumer<RemoteData> remoteData = data -> {};

  public static boolean hasTuner(net.minecraft.world.entity.player.Player player) {
    return BadgeAccess.carried(player).stream()
        .anyMatch(stack -> stack.is(FieldEmitters.TUNER.get()));
  }

  public record RemoteRequest(boolean list, BlockPos pos) implements CustomPacketPayload {
    public static final Type<RemoteRequest> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(FieldEmitters.ID, "remote_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoteRequest> CODEC =
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
    public static final StreamCodec<RegistryFriendlyByteBuf, Rename> CODEC =
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

  public record RemoteData(int kind, CompoundTag data, Component message)
      implements CustomPacketPayload {
    public static final Type<RemoteData> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(FieldEmitters.ID, "remote_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoteData> CODEC =
        StreamCodec.of(
            (b, p) -> {
              b.writeInt(p.kind);
              b.writeNbt(p.data);
              net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.encode(b, p.message);
            },
            b ->
                new RemoteData(
                    b.readInt(),
                    b.readNbt(),
                    net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.decode(b)));

    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  static void reply(
      net.minecraft.world.entity.player.Player player,
      int kind,
      CompoundTag data,
      Component message) {
    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
        (net.minecraft.server.level.ServerPlayer) player, new RemoteData(kind, data, message));
  }

  public static void openRemote(net.minecraft.world.entity.player.Player player) {
    remote(new RemoteRequest(true, BlockPos.ZERO), player);
  }

  private static void remote(
      RemoteRequest request, net.minecraft.world.entity.player.Player player) {
    if (!hasTuner(player)) {
      reply(
          player,
          2,
          new CompoundTag(),
          Component.translatable(
              "message.fieldemitters.fieldcontrols.carry_or_equip_a_field_tuner_to_manage"));
      return;
    }
    var level = player.level();
    if (request.list) {
      ManagedFields.refresh(
          (net.minecraft.server.level.ServerLevel) level, FieldNetwork.loaded(level));
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
        entry.putLong(
            "Origin",
            (anchor.managedNetwork == null
                    ? anchor.getBlockPos()
                    : anchor.managedNetwork.anchor().orElse(anchor.getBlockPos()))
                .asLong());
        entry.putString(
            "Name",
            anchor.managedNetwork != null ? anchor.managedNetwork.name() : anchor.fieldName);
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
      reply(
          player,
          0,
          data,
          Component.translatable(
              "message.fieldemitters.fieldcontrols.connected_loaded_emitters_form_one_field_select_a"));
    } else {
      if (!level.hasChunkAt(request.pos)
          || !(level.getBlockEntity(request.pos) instanceof EmitterEntity e)
          || !editable(e, player)) {
        reply(
            player,
            2,
            new CompoundTag(),
            Component.translatable(
                "message.fieldemitters.fieldcontrols.emitter_unavailable_or_access_denied_refresh_the_list"));
        return;
      }
      var data = new CompoundTag();
      data.putLong("Pos", e.getBlockPos().asLong());
      data.put("State", net.minecraft.nbt.NbtUtils.writeBlockState(e.getBlockState()));
      data.put("Emitter", e.getUpdateTag(level.registryAccess()));
      reply(player, 1, data, Component.empty());
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
      boolean linkOnly,
      boolean inherit)
      implements CustomPacketPayload {
    public static final Type<Update> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(FieldEmitters.ID, "controls"));
    public static final StreamCodec<RegistryFriendlyByteBuf, Update> CODEC =
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
              b.writeBoolean(p.inherit);
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
                    b.readBoolean(),
                    b.readBoolean()));

    public Type<? extends CustomPacketPayload> type() {
      return TYPE;
    }
  }

  public static void register(RegisterPayloadHandlersEvent event) {
    event
        .registrar("3")
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
                            Component.translatable(
                                "message.fieldemitters.fieldcontrols.cannot_rename_carry_or_equip_a_tuner_and"));
                        return;
                      }
                      String name = p.name.strip();
                      if (name.chars().anyMatch(ch -> Character.isISOControl(ch) || ch == 167)) {
                        reply(
                            player,
                            2,
                            new CompoundTag(),
                            Component.translatable(
                                "message.fieldemitters.fieldcontrols.use_plain_text_for_the_field_name"));
                        return;
                      }
                      for (var e : FieldNetwork.configurable(seed))
                        if (editable(e, player)) {
                          ManagedFields.rename(
                              (net.minecraft.server.level.ServerLevel) level, e, name);
                          e.fieldName = name;
                          e.sync();
                        }
                      reply(
                          player,
                          2,
                          new CompoundTag(),
                          Component.translatable(
                              "message.fieldemitters.fieldcontrols.field_name_updated"));
                    }));
    event
        .registrar("3")
        .playToServer(
            RemoteRequest.TYPE,
            RemoteRequest.CODEC,
            (p, c) -> c.enqueueWork(() -> remote(p, c.player())))
        .playToClient(
            RemoteData.TYPE, RemoteData.CODEC, (p, c) -> c.enqueueWork(() -> remoteData.accept(p)));
    event
        .registrar("3")
        .playToServer(
            Update.TYPE,
            Update.CODEC,
            (p, context) ->
                context.enqueueWork(
                    () -> {
                      ControlEdits.handle(p, (net.minecraft.server.level.ServerPlayer) context.player());
                    }));
  }

  public static Component validate(ControlSettings settings) {
    if (!Float.isFinite(settings.damageAmount)
        || settings.damageAmount < 0
        || settings.damageAmount > ControlSettings.MAX_DAMAGE)
      return Component.translatable(
          "message.fieldemitters.fieldcontrols.damage_must_be_a_number_from_0_to");
    for (var filter : settings.filters()) {
      Component error = validate(filter);
      if (error != null) return error;
    }
    return null;
  }

  public static Component validate(EntityFilter f) {
    if (f.included.size() > EntityFilter.MAX_TYPES || f.excluded.size() > EntityFilter.MAX_TYPES)
      return Component.translatable("message.fieldemitters.fieldcontrols.mob_and_item_lists_support_at_most_64");
    for (var target : java.util.stream.Stream.concat(f.included.stream(), f.excluded.stream()).toList()) {
      Component error = target.validate();
      if (error != null) return error;
    }
    if (f.mobMode < 0
        || f.mobMode > 2
        || f.itemMode < 0
        || f.itemMode > 2
        || f.mobList.size() > EntityFilter.MAX_TYPES
        || f.itemList.size() > EntityFilter.MAX_TYPES)
      return Component.translatable(
          "message.fieldemitters.fieldcontrols.mob_and_item_lists_support_at_most_64");
    for (String value : f.mobList) {
      Component error = validateTypeEntry(value, false);
      if (error != null) return error;
    }
    for (String value : f.itemList) {
      Component error = validateTypeEntry(value, true);
      if (error != null) return error;
    }
    if (f.accessGroups.size() > EntityFilter.MAX_ACCESS_GROUPS
        || f.accessGroups.stream().anyMatch(g -> !BadgeAccess.validGroup(g)))
      return Component.translatable(
          "message.fieldemitters.fieldcontrols.use_up_to_64_group_names_lowercase_letters");
    if (f.playerMode < 0 || f.playerMode > 2 || f.playerList.size() > EntityFilter.MAX_PLAYERS)
      return Component.translatable(
          "message.fieldemitters.fieldcontrols.player_lists_support_at_most_64_entries");
    if (f.playerList.values().stream()
        .anyMatch(name -> !name.isEmpty() && !name.matches("[A-Za-z0-9_]{1,16}")))
      return Component.translatable(
          "message.fieldemitters.fieldcontrols.player_names_must_be_minecraft_account_names_not");
    for (String value : java.util.List.of(f.entityType, f.itemType))
      if (!value.isEmpty()
          && net.minecraft.resources.ResourceLocation.tryParse(
                  value.startsWith("#") ? value.substring(1) : value)
              == null)
        return Component.translatable(
            "message.fieldemitters.fieldcontrols.invalid_id_or_tag", value);
    if (!f.identity.isEmpty())
      try {
        java.util.UUID.fromString(f.identity);
      } catch (IllegalArgumentException ex) {
        return Component.translatable(
            "message.fieldemitters.fieldcontrols.invalid_individual_uuid");
      }
    if (!f.entityType.isEmpty()
        && !f.entityType.startsWith("#")
        && !net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(
            net.minecraft.resources.ResourceLocation.parse(f.entityType)))
      return Component.translatable(
          "message.fieldemitters.fieldcontrols.unknown_entity_id", f.entityType);
    if (!f.itemType.isEmpty()
        && !f.itemType.startsWith("#")
        && !net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(
            net.minecraft.resources.ResourceLocation.parse(f.itemType)))
      return Component.translatable(
          "message.fieldemitters.fieldcontrols.unknown_item_id", f.itemType);
    return null;
  }

  public static Component validateTypeEntry(String value, boolean item) {
    boolean tag = value.startsWith("#");
    var id = net.minecraft.resources.ResourceLocation.tryParse(tag ? value.substring(1) : value);
    if (value.length() > EntityFilter.MAX_TYPE_LENGTH || id == null)
      return Component.translatable(
          "message.fieldemitters.fieldcontrols.use_a_registry_id_or_tag_such_as");
    if (!tag
        && !(item
            ? net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id)
            : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(id)))
      return Component.translatable(
          item
              ? "message.fieldemitters.fieldcontrols.unknown_item_id"
              : "message.fieldemitters.fieldcontrols.unknown_entity_id",
          value);
    if (!tag && !item && (id.toString().equals("minecraft:player")))
      return Component.translatable(
          "message.fieldemitters.fieldcontrols.use_the_player_list_for_players_this_list");
    return null;
  }

  public static boolean editable(EmitterEntity e, net.minecraft.world.entity.player.Player player) {
    return ManagementAccess.editable(e, player);
  }
}
