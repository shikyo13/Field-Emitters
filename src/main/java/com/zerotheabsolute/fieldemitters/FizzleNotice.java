package com.zerotheabsolute.fieldemitters;

import net.minecraft.network.FriendlyByteBuf;
import com.zerotheabsolute.fieldemitters.network.PacketCodec;
import com.zerotheabsolute.fieldemitters.network.FieldPayload;
import net.minecraft.resources.ResourceLocation;
import com.zerotheabsolute.fieldemitters.network.ForgeNetworkRegistrar;

/** Cosmetic death notice only; damage and loot remain entirely under Minecraft's server rules. */
public record FizzleNotice(int entityId) implements FieldPayload {
  public static java.util.function.IntConsumer receive = id -> {};
  public static final Type<FizzleNotice> TYPE = new Type<>(new ResourceLocation(FieldEmitters.ID,"fizzle"));
  public static final PacketCodec<FriendlyByteBuf,FizzleNotice> CODEC = PacketCodec.of(
      (b,p)->b.writeVarInt(p.entityId),b->new FizzleNotice(b.readVarInt()));
  public Type<? extends FieldPayload> type() { return TYPE; }
  public static void register(ForgeNetworkRegistrar event) {
    event.playToClient(FizzleNotice.class,CODEC,(p,c)->c.enqueueWork(()->receive.accept(p.entityId)));
  }
}
