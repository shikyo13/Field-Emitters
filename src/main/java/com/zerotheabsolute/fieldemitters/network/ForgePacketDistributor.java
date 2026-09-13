package com.zerotheabsolute.fieldemitters.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.PacketDistributor;

public final class ForgePacketDistributor {
    private ForgePacketDistributor() {}
    public static void sendToPlayersNear(ServerLevel level, ServerPlayer excluded, double x, double y, double z, double radius, FieldPayload payload) {
      for (var player : level.players()) if(player != excluded && player.distanceToSqr(x,y,z) <= radius*radius) sendToPlayer(player,payload);
    }
    public static void sendToServer(FieldPayload payload) {
        ForgeNetworkRegistrar.CHANNEL.sendToServer(payload);
    }
    public static void sendToPlayer(ServerPlayer player, FieldPayload payload) {
        ForgeNetworkRegistrar.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }
    public static void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos position, FieldPayload payload) {
        var chunk = level.getChunkSource().getChunkNow(position.x, position.z);
        if (chunk != null) {
            ForgeNetworkRegistrar.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), payload);
        }
    }
}
