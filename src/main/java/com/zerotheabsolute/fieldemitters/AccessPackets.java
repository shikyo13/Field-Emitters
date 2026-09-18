package com.zerotheabsolute.fieldemitters;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import com.zerotheabsolute.fieldemitters.network.PacketCodec;
import com.zerotheabsolute.fieldemitters.network.FieldPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

public final class AccessPackets {
  public static java.util.function.Consumer<Result> receive = result -> {};

  public record Result(BlockPos pos, Component message) implements FieldPayload {
    public static final Type<Result> TYPE =
        new Type<>(new ResourceLocation(FieldEmitters.ID, "badge_result"));
    public static final PacketCodec<FriendlyByteBuf, Result> CODEC =
        PacketCodec.of(
            (b, p) -> {
              b.writeBlockPos(p.pos);
              b.writeComponent(p.message);
            },
            b ->
                new Result(
                    b.readBlockPos(),
                    b.readComponent()));

    public Type<? extends FieldPayload> type() {
      return TYPE;
    }
  }

  private static void reply(
      net.minecraft.world.entity.player.Player player, BlockPos pos, Component message) {
    com.zerotheabsolute.fieldemitters.network.ForgePacketDistributor.sendToPlayer(
        (net.minecraft.server.level.ServerPlayer) player, new Result(pos, message));
  }

  public record Request(BlockPos pos, String group, String player, int action)
      implements FieldPayload {
    public static final Type<Request> TYPE =
        new Type<>(new ResourceLocation(FieldEmitters.ID, "badge_action"));
    public static final PacketCodec<FriendlyByteBuf, Request> CODEC =
        PacketCodec.of(
            (b, p) -> {
              b.writeBlockPos(p.pos);
              b.writeUtf(p.group, 32);
              b.writeUtf(p.player, 36);
              b.writeVarInt(p.action);
            },
            b -> new Request(b.readBlockPos(), b.readUtf(32), b.readUtf(36), b.readVarInt()));

    public Type<? extends FieldPayload> type() {
      return TYPE;
    }
  }

  public record Grants(net.minecraft.nbt.CompoundTag data) implements FieldPayload {
    public static final Type<Grants> TYPE =
        new Type<>(new ResourceLocation(FieldEmitters.ID, "badge_grants"));
    public static final PacketCodec<FriendlyByteBuf, Grants> CODEC =
        PacketCodec.of((b, p) -> b.writeNbt(p.data), b -> new Grants(b.readNbt()));

    public Type<? extends FieldPayload> type() {
      return TYPE;
    }
  }

  private static final java.util.Map<
          net.minecraft.server.level.ServerPlayer, net.minecraft.nbt.CompoundTag>
      sent = new java.util.WeakHashMap<>();

  public static void tick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
    if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
    if (!(event.player instanceof net.minecraft.server.level.ServerPlayer player)
        || player.tickCount % 5 != 0) return;
    var data = BadgeAccess.grants(player, player.serverLevel());
    if (!data.equals(sent.get(player))) {
      sent.put(player, data.copy());
      com.zerotheabsolute.fieldemitters.network.ForgePacketDistributor.sendToPlayer(player, new Grants(data));
    }
  }

  public static void register(
      com.zerotheabsolute.fieldemitters.network.ForgeNetworkRegistrar event) {
    event
        
        .playToClient(Result.class, Result.CODEC, (p, c) -> c.enqueueWork(() -> receive.accept(p)))
        .playToClient(
            Grants.class,
            Grants.CODEC,
            (p, c) ->
                c.enqueueWork(
                    () -> {
                      BadgeAccess.clientGrants.clear();
                      if (p.data == null) return;
                      for (var key : p.data.getAllKeys())
                        try {
                          var groups = new java.util.HashSet<String>();
                          for (var value : p.data.getList(key, 8)) groups.add(value.getAsString());
                          BadgeAccess.clientGrants.put(java.util.UUID.fromString(key), groups);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }));
    event
        
        .playToServer(
            Request.class,
            Request.CODEC,
            (p, c) ->
                c.enqueueWork(
                    () -> {
                      var player = c.player();
                      var level = (ServerLevel) player.level();
                      if (!level.hasChunkAt(p.pos)
                          || !(level.getBlockEntity(p.pos) instanceof EmitterEntity e)
                          || !FieldControls.editable(e, player)
                          || (!FieldControls.hasTuner(player)
                              && player.distanceToSqr(
                                      net.minecraft.world.phys.Vec3.atCenterOf(p.pos))
                                  > 144)) return;
                      String group = BadgeAccess.group(p.group);
                      if (!BadgeAccess.validGroup(group)) return;
                      java.util.UUID bound = null;
                      if (!p.player.isBlank())
                        try {
                          bound = java.util.UUID.fromString(p.player);
                        } catch (IllegalArgumentException ex) {
                          reply(
                              player,
                              p.pos,
                              Component.translatable(
                                  "message.fieldemitters.accesspackets.enter_a_player_uuid_or_leave_blank_for"));
                          return;
                        }
                      // An administrator editing somebody else's field still issues only their own
                      // credentials.
                      var issuer = player.getUUID();
                      var data = BadgeAccess.get(level);
                      if (p.action == 1) {
                        int count = data.revoke(issuer, group);
                        reply(
                            player,
                            p.pos,
                            Component.translatable(
                                "message.fieldemitters.accesspackets.revoked_badges_for",
                                count,
                                group));
                        return;
                      }
                      if (p.action != 0) return;
                      var stack =
                          player.getMainHandItem().is(FieldEmitters.BADGE.get())
                              ? player.getMainHandItem()
                              : player.getOffhandItem();
                      if (!stack.is(FieldEmitters.BADGE.get())) {
                        reply(
                            player,
                            p.pos,
                            Component.translatable(
                                "message.fieldemitters.accesspackets.hold_an_access_badge_in_either_hand_to"));
                        return;
                      }
                      data.issue(stack, issuer, group, bound);
                      player.getInventory().setChanged();
                      player.containerMenu.broadcastChanges();
                      reply(
                          player,
                          p.pos,
                          Component.translatable(
                              "message.fieldemitters.accesspackets.issued_badge",
                              group,
                              (bound == null
                                  ? Component.translatable(
                                      "message.fieldemitters.accesspackets.transferable")
                                  : Component.translatable(
                                      "message.fieldemitters.accesspackets.for", bound))));
                    }));
  }
}
