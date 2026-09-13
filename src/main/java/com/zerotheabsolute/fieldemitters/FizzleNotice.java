package com.zerotheabsolute.fieldemitters;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.zerotheabsolute.fieldemitters.network.ForgeNetworkRegistrar;

/** Cosmetic death notice only; damage and loot remain entirely under Minecraft's server rules. */
public record FizzleNotice(int entityId) implements CustomPacketPayload {
  public static java.util.function.IntConsumer receive = id -> {};
  public static final Type<FizzleNotice> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FieldEmitters.ID,"fizzle"));
  public static final StreamCodec<FriendlyByteBuf,FizzleNotice> CODEC = StreamCodec.of(
      (b,p)->b.writeVarInt(p.entityId),b->new FizzleNotice(b.readVarInt()));
  public Type<? extends CustomPacketPayload> type() { return TYPE; }
  public static void register(ForgeNetworkRegistrar event) {
    event.playToClient(FizzleNotice.class,CODEC,(p,c)->c.enqueueWork(()->receive.accept(p.entityId)));
  }
}
