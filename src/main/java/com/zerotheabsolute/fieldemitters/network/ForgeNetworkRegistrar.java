package com.zerotheabsolute.fieldemitters.network;

import com.zerotheabsolute.fieldemitters.FieldEmitters;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.SimpleChannel;
import java.util.function.BiConsumer;

public final class ForgeNetworkRegistrar {
    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(FieldEmitters.ID, "main"))
            .networkProtocolVersion(1).simpleChannel();
    public <T extends CustomPacketPayload> ForgeNetworkRegistrar playToClient(Class<T> type, StreamCodec<FriendlyByteBuf, T> codec,
                                                            BiConsumer<T, ForgePayloadContext> handler) {
        register(type, codec, handler, NetworkDirection.PLAY_TO_CLIENT); return this;
    }
    public <T extends CustomPacketPayload> ForgeNetworkRegistrar playToServer(Class<T> type, StreamCodec<FriendlyByteBuf, T> codec,
                                                            BiConsumer<T, ForgePayloadContext> handler) {
        register(type, codec, handler, NetworkDirection.PLAY_TO_SERVER); return this;
    }
    private <T extends CustomPacketPayload> void register(Class<T> type, StreamCodec<FriendlyByteBuf, T> codec,
                                                         BiConsumer<T, ForgePayloadContext> handler,
                                                         NetworkDirection<RegistryFriendlyByteBuf> direction) {
        CHANNEL.messageBuilder(type, direction)
                .encoder((value, buffer) -> codec.encode(buffer, value)).decoder(codec::decode)
                .consumerNetworkThread((value, context) -> {
                    handler.accept(value, new ForgePayloadContext(context));
                    context.setPacketHandled(true);
                }).add();
    }
}
