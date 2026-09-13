package com.zerotheabsolute.fieldemitters;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Server-authoritative contact damage. Adjacent tiles and overlapping fields cannot multiply a hit. */
public final class FieldDamage {
  private static final String NEXT_HIT = "fieldemitters:next_damage";
  private static final java.util.Map<ServerLevel, Budget> BUDGETS = new java.util.WeakHashMap<>();
  private static final class Budget { long tick; int effects; }
  private FieldDamage() {}

  public static void tick(ServerLevel level, EmitterEntity e, long now) {
    if (!e.powered) return;
    var space = FieldSpace.at(e);
    for (var link : e.links) {
      var settings = e.settings(link);
      if (!settings.damageEnabled || settings.damageAmount <= 0) continue;
      var p = e.getBlockPos();
      var bounds = link.box(p);
      if (!link.rail()) {
        int min = java.util.Arrays.stream(link.ground()).min().orElse(p.getY());
        int max = java.util.Arrays.stream(link.ground()).max().orElse(p.getY());
        bounds = new AABB(bounds.minX, min, bounds.minZ, bounds.maxX, max + 5, bounds.maxZ);
      }
      for (var entity : level.getEntities((Entity) null, bounds.inflate(.2),
          a -> a.isAlive() && !a.isSpectator())) {
        long due = entity.getPersistentData().getLong(NEXT_HIT);
        if (due > now && due - now <= 200) continue;
        var center = space.local(entity.getBoundingBox().getCenter());
        double u = (center.x - p.getX() - .5) * link.dx()
            + (center.y - p.getY() - .5) * link.dy()
            + (center.z - p.getZ() - .5) * link.dz();
        int i = (int) Math.floor(u + .5);
        if (i < (link.rail() ? 0 : 1) || i > (link.rail() ? link.length() : link.length() - 1)
            || now - e.transition < e.controls.linkFormationTicks(i)) continue;
        var cell = link.cell(p, i, 0);
        var slab = switch (link.normal()) {
          case X -> new AABB(p.getX()+.3, cell.getY(), cell.getZ(), p.getX()+.7, cell.getY()+link.height(), cell.getZ()+1);
          case Y -> new AABB(cell.getX(), link.origin(p).y-.2, cell.getZ(), cell.getX()+1, link.origin(p).y+.2, cell.getZ()+1);
          case Z -> new AABB(cell.getX(), cell.getY(), p.getZ()+.3, cell.getX()+1, cell.getY()+link.height(), p.getZ()+.7);
        };
        if (!slab.intersects(space.local(entity.getBoundingBox()))) continue;
        Direction direction = FieldContact.movement(e, link, entity, center);
        if (!settings.damages(entity, e.owner, direction)) continue;
        if (entity.hurt(level.damageSources().magic(), settings.damageAmount)) {
          entity.getPersistentData().putLong(NEXT_HIT, now + settings.damageInterval);
          boolean destroyed = !entity.isAlive() || (entity instanceof LivingEntity living && living.getHealth() <= 0);
          effects(level, e, entity, now, destroyed);
        }
      }
    }
  }

  public static void contact(ServerLevel level,EmitterEntity e,Entity entity,ControlSettings settings,Direction movement,long now) {
    long due=entity.getPersistentData().getLong(NEXT_HIT);
    if(due>now&&due-now<=200||!settings.damages(entity,e.owner,movement))return;
    if(entity.hurt(level.damageSources().magic(),settings.damageAmount)) {
      entity.getPersistentData().putLong(NEXT_HIT,now+settings.damageInterval);
      effects(level,e,entity,now,!entity.isAlive());
    }
  }

  private static void effects(ServerLevel level, EmitterEntity e, Entity entity, long now, boolean destroyed) {
    var budget = BUDGETS.computeIfAbsent(level, ignored -> new Budget());
    if (budget.tick != now) { budget.tick = now; budget.effects = 0; }
    // Cosmetic budget is dimension-wide; exceeding it never changes gameplay damage.
    if (budget.effects >= 8 || now - e.lastFizzle < 4) return;
    budget.effects++;
    e.lastFizzle = now;
    var center = entity.getBoundingBox().getCenter();
    FieldSounds.play(level, e, center, 3);
    if (!e.controls.fizzleEffects) return;
    if (destroyed && entity instanceof LivingEntity)
      net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(level,null,center.x,center.y,center.z,32,new FizzleNotice(entity.getId()));
    int c = e.controls.accentColor(e.color);
    var dust = new DustParticleOptions(new Vector3f((c>>16&255)/255f,(c>>8&255)/255f,(c&255)/255f), .65f);
    // Three small bands trace the body on a lethal hit. At most 24 normal-distance particles per burst.
    int bands = destroyed ? 3 : 1;
    for (int b = 0; b < bands; b++) {
      double y = destroyed ? entity.getY() + entity.getBbHeight() * (b + .5) / bands : center.y;
      level.sendParticles(dust, center.x, y, center.z, destroyed ? 8 : 5,
          Math.min(.8, entity.getBbWidth()*.45), .08, Math.min(.8, entity.getBbWidth()*.45), .02);
    }
  }
}
