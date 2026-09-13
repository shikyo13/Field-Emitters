package com.zerotheabsolute.fieldemitters;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import com.zerotheabsolute.fieldemitters.network.PacketCodec;
import com.zerotheabsolute.fieldemitters.network.FieldPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import com.zerotheabsolute.fieldemitters.network.ForgePacketDistributor;

public final class ManagementPackets {
  public static java.util.function.Consumer<State> receive = p -> {};

  public record Request(BlockPos pos, int action, String member) implements FieldPayload {
    public static final Type<Request> TYPE =
        new Type<>(new ResourceLocation(FieldEmitters.ID, "management"));
    public static final PacketCodec<FriendlyByteBuf, Request> CODEC =
        PacketCodec.of(
            (b, p) -> {
              b.writeBlockPos(p.pos);
              b.writeVarInt(p.action);
              b.writeUtf(p.member, 36);
            },
            b -> new Request(b.readBlockPos(), b.readVarInt(), b.readUtf(36)));

    public Type<? extends FieldPayload> type() {
      return TYPE;
    }
  }

  public record State(BlockPos pos, CompoundTag data, boolean owner)
      implements FieldPayload {
    public static final Type<State> TYPE =
        new Type<>(new ResourceLocation(FieldEmitters.ID, "management_state"));
    public static final PacketCodec<FriendlyByteBuf, State> CODEC =
        PacketCodec.of(
            (b, p) -> {
              b.writeBlockPos(p.pos);
              b.writeNbt(p.data);
              b.writeBoolean(p.owner);
            },
            b -> new State(b.readBlockPos(), b.readNbt(), b.readBoolean()));

    public Type<? extends FieldPayload> type() {
      return TYPE;
    }
  }

  public static void register(
      com.zerotheabsolute.fieldemitters.network.ForgeNetworkRegistrar event) {
    event
        
        .playToClient(State.class, State.CODEC, (p, c) -> c.enqueueWork(() -> receive.accept(p)))
        .playToServer(
            Request.class,
            Request.CODEC,
            (p, c) ->
                c.enqueueWork(
                    () -> {
                      var player = c.player();
                      var level = (ServerLevel) player.level();
                      if (!level.hasChunkAt(p.pos)
                          || !(level.getBlockEntity(p.pos) instanceof EmitterEntity e)) return;
                      ManagedFields.refresh(level, FieldNetwork.loaded(level));
                      if (!FieldControls.editable(e, player)
                          || (!FieldControls.hasTuner(player)
                              && player.distanceToSqr(
                                      net.minecraft.world.phys.Vec3.atCenterOf(p.pos))
                                  > 144)) return;
                      java.util.UUID member = null;
                      if (!p.member.isBlank())
                        try {
                          member = java.util.UUID.fromString(p.member);
                        } catch (IllegalArgumentException ex) {
                          return;
                        }
                      if (p.action >= 0)
                        ManagementAccess.update(level, e, player, p.action, member);
                      ForgePacketDistributor.sendToPlayer(
                          (net.minecraft.server.level.ServerPlayer) player,
                          new State(
                              p.pos,
                              ManagementAccess.snapshot(e),
                              ManagementAccess.owner(e, player)));
                    }));
  }
}
