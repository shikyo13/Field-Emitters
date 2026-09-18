package com.zerotheabsolute.fieldemitters.network;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class NativeNetwork {
  private static final List<Runnable> CLIENT = new ArrayList<>();
  public record Context(Player player, Executor executor) {
    public void enqueueWork(Runnable action) { executor.execute(action); }
  }
  public <T extends CustomPacketPayload> NativeNetwork playToServer(CustomPacketPayload.Type<T> type,
      StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, Context> handler) {
    PayloadTypeRegistry.playC2S().register(type, codec);
    ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) -> handler.accept(payload, new Context(context.player(), context.server())));
    return this;
  }
  public <T extends CustomPacketPayload> NativeNetwork playToClient(CustomPacketPayload.Type<T> type,
      StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, Context> handler) {
    PayloadTypeRegistry.playS2C().register(type, codec);
    CLIENT.add(() -> NativeClientNetwork.register(type, codec, handler));
    return this;
  }
  public static void initClient() { CLIENT.forEach(Runnable::run); CLIENT.clear(); }
  public static void sendToPlayersNear(net.minecraft.server.level.ServerLevel level, ServerPlayer excluded, double x, double y, double z, double radius, CustomPacketPayload payload) {
    for (var player : level.players()) if(player != excluded && player.distanceToSqr(x,y,z) <= radius*radius) sendToPlayer(player,payload);
  }
  public static void sendToServer(CustomPacketPayload payload) {
    NativeClientNetwork.send(payload);
  }
  public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) { ServerPlayNetworking.send(player, payload); }
}
