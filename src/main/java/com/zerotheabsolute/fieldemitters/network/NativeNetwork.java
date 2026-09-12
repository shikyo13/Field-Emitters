package com.zerotheabsolute.fieldemitters.network;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

public final class NativeNetwork {
  private static final List<Runnable> CLIENT = new ArrayList<>();
  private static final Map<ResourceLocation, PacketCodec<FriendlyByteBuf, ?>> CODECS = new HashMap<>();
  public record Context(Player player, Executor executor) {
    public void enqueueWork(Runnable action) { executor.execute(action); }
  }
  public <T extends FieldPayload> NativeNetwork playToServer(FieldPayload.Type<T> type,
      PacketCodec<FriendlyByteBuf, T> codec, BiConsumer<T, Context> handler) {
    CODECS.put(type.id(), codec);
    ServerPlayNetworking.registerGlobalReceiver(type.id(), (server, player, network, buffer, sender) -> {
      T payload = codec.decode(buffer);
      handler.accept(payload, new Context(player, server));
    });
    return this;
  }
  public <T extends FieldPayload> NativeNetwork playToClient(FieldPayload.Type<T> type,
      PacketCodec<FriendlyByteBuf, T> codec, BiConsumer<T, Context> handler) {
    CODECS.put(type.id(), codec);
    CLIENT.add(() -> NativeClientNetwork.register(type, codec, handler));
    return this;
  }
  @SuppressWarnings("unchecked")
  static <T extends FieldPayload> FriendlyByteBuf encode(T payload) {
    var codec = (PacketCodec<FriendlyByteBuf, T>)Objects.requireNonNull(CODECS.get(payload.type().id()));
    var buffer = PacketByteBufs.create(); codec.encode(buffer, payload); return buffer;
  }
  public static void initClient() { CLIENT.forEach(Runnable::run); CLIENT.clear(); }
  public static void sendToServer(FieldPayload payload) {
    NativeClientNetwork.send(payload);
  }
  public static void sendToPlayer(ServerPlayer player, FieldPayload payload) { ServerPlayNetworking.send(player, payload.type().id(), encode(payload)); }
}
