package com.zerotheabsolute.fieldemitters.client;

import java.util.LinkedHashMap;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;

/** Replace the ordinary falling corpse with the bounded body-shaped fizzle burst. */
public final class FizzleDeaths {
  private record Death(UUID uuid,long time) {}
  private static final LinkedHashMap<Integer,Death> DEATHS=new LinkedHashMap<>();
  private static net.minecraft.client.multiplayer.ClientLevel world;
  public static void receive(int id) {
    var mc=Minecraft.getInstance();
    if (mc.level==null || mc.options.particles().get()==ParticleStatus.MINIMAL) return;
    if (world!=mc.level) { DEATHS.clear();world=mc.level; }
    var entity=world.getEntity(id);
    if (entity==null) return;
    DEATHS.entrySet().removeIf(e->world.getGameTime()-e.getValue().time()>40);
    while(DEATHS.size()>=32) DEATHS.remove(DEATHS.keySet().iterator().next());
    DEATHS.put(id,new Death(entity.getUUID(),world.getGameTime()));
  }
  public static void tick() {
    var level=Minecraft.getInstance().level;
    if(world!=level) { DEATHS.clear();world=level; }
    if(world!=null && !DEATHS.isEmpty()) DEATHS.entrySet().removeIf(e->world.getGameTime()-e.getValue().time()>40);
  }
  public static boolean hidden(net.minecraft.world.entity.LivingEntity entity) {
    var death=DEATHS.get(entity.getId());
    if (entity.level()!=world || death==null || !death.uuid().equals(entity.getUUID())) return false;
    if (world.getGameTime()-death.time()>40) { DEATHS.remove(entity.getId());return false; }
    return entity.isDeadOrDying();
  }
}
