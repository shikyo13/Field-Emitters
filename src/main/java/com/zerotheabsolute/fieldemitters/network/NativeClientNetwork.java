package com.zerotheabsolute.fieldemitters.network;

import java.util.function.BiConsumer;
import net.minecraft.network.FriendlyByteBuf;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client receiver bindings kept out of dedicated-server class loading. */
@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
final class NativeClientNetwork {

  public static <T extends FieldPayload> void register(FieldPayload.Type<T> type, PacketCodec<FriendlyByteBuf,T> codec, BiConsumer<T,NativeNetwork.Context> handler) {
    ClientPlayNetworking.registerGlobalReceiver(type.id(), (client, network, buffer, sender) -> {
      T payload = codec.decode(buffer); handler.accept(payload, new NativeNetwork.Context(client.player,client));
    });
  }
  public static void send(FieldPayload payload) { ClientPlayNetworking.send(payload.type().id(), NativeNetwork.encode(payload)); }
}
