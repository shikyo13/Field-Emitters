package com.zerotheabsolute.fieldemitters.network;

import java.util.function.BiConsumer;
import net.minecraft.network.FriendlyByteBuf;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
/** Client receiver bindings kept out of dedicated-server class loading. */
@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
final class NativeClientNetwork {

  public static <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type, StreamCodec<FriendlyByteBuf,T> codec, BiConsumer<T,NativeNetwork.Context> handler) {
    ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) -> handler.accept(payload, new NativeNetwork.Context(context.player(),context.client())));
  }
  public static void send(CustomPacketPayload payload) { ClientPlayNetworking.send(payload); }
}
