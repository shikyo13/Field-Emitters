package com.zerotheabsolute.fieldemitters;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import com.zerotheabsolute.fieldemitters.network.ForgePacketDistributor;
import com.zerotheabsolute.fieldemitters.network.ForgeNetworkRegistrar;

/** Resolves names on the server; profile-service requests never block the game thread. */
public final class PlayerLookup {
  public static Consumer<Result> receive = result -> {};
  private PlayerLookup() {}
  public record Request(BlockPos pos, int requestId, String query) implements CustomPacketPayload {
    public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FieldEmitters.ID,"player_lookup"));
    public static final StreamCodec<FriendlyByteBuf,Request> CODEC = StreamCodec.of(
        (b,p) -> { b.writeBlockPos(p.pos); b.writeInt(p.requestId); b.writeUtf(p.query,36); },
        b -> new Request(b.readBlockPos(),b.readInt(),b.readUtf(36)));
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
  }
  public record Result(int requestId, String uuid, String name, String error) implements CustomPacketPayload {
    public static final Type<Result> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FieldEmitters.ID,"player_lookup_result"));
    public static final StreamCodec<FriendlyByteBuf,Result> CODEC = StreamCodec.of(
        (b,p) -> { b.writeInt(p.requestId); b.writeUtf(p.uuid,36); b.writeUtf(p.name,16); b.writeUtf(p.error,128); },
        b -> new Result(b.readInt(),b.readUtf(36),b.readUtf(16),b.readUtf(128)));
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
  }
  public static void register(ForgeNetworkRegistrar event) {
    event
        .playToServer(Request.class,Request.CODEC,(p,c) -> c.enqueueWork(() -> lookup(p,(ServerPlayer)c.player())))
        .playToClient(Result.class,Result.CODEC,(p,c) -> c.enqueueWork(() -> receive.accept(p)));
  }
  private static boolean allowed(Request r, ServerPlayer p) {
    return p.level().hasChunkAt(r.pos) && p.level().getBlockEntity(r.pos) instanceof EmitterEntity e
        && FieldControls.editable(e,p) && (FieldControls.hasTuner(p) || p.distanceToSqr(r.pos.getX()+.5,r.pos.getY()+.5,r.pos.getZ()+.5)<=144);
  }
  private static void reply(Request r, ServerPlayer p, UUID id, String name, String error) {
    ForgePacketDistributor.sendToPlayer(p,new Result(r.requestId,id==null?"":id.toString(),name,error));
  }
  private static void lookup(Request r, ServerPlayer p) {
    if (!allowed(r,p)) { reply(r,p,null,"","Stay near an emitter you can edit, or hold a tuner."); return; }
    long now=p.server.getTickCount();
    var data=p.getPersistentData();
    long due=data.getLong("fieldemitters:lookup_after");
    if (due>now && due-now<=20) { reply(r,p,null,"","Please wait a second before another lookup."); return; }
    data.putLong("fieldemitters:lookup_after",now+20);
    String query=r.query.trim();
    try {
      UUID id=UUID.fromString(query);
      if (!id.toString().equalsIgnoreCase(query)) throw new IllegalArgumentException();
      var online=p.server.getPlayerList().getPlayer(id);
      reply(r,p,id,online==null?"":online.getGameProfile().getName(),""); return;
    } catch (IllegalArgumentException ignored) {}
    if (!query.matches("[A-Za-z0-9_]{1,16}")) { reply(r,p,null,"","Enter a Minecraft account name or a full UUID."); return; }
    var online=p.server.getPlayerList().getPlayerByName(query);
    if (online!=null) { reply(r,p,online.getUUID(),online.getGameProfile().getName(),""); return; }
    var cache=p.server.getProfileCache();
    if (cache==null) { reply(r,p,null,"","Name lookup unavailable. Enter the player's UUID instead."); return; }
    try {
      cache.getAsync(query).whenComplete((profile,error) -> p.server.execute(() -> {
        if (p.hasDisconnected() || !allowed(r,p)) return;
        if (error!=null || profile==null || profile.isEmpty()) reply(r,p,null,"","Player not found. Check the account name or enter their UUID.");
        else reply(r,p,profile.get().getId(),profile.get().getName(),"");
      }));
    } catch (IllegalStateException ex) { reply(r,p,null,"","Name lookup unavailable. Enter the player's UUID instead."); }
  }
}
