package com.zerotheabsolute.fieldemitters;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Keep the entry side until the entire body has cleared the field. */
final class FieldContact {
  private static final int CONTACT_GAP_TICKS = 2;
  private static final double PLANAR_HALF_THICKNESS = .2;

  private record Key(UUID entity, BlockPos target) {}

  private record Entry(boolean negative, long tick) {}

  static final class Contacts {
    final Map<Key, Entry> entries = new HashMap<>();
    long cleanedAt = Long.MIN_VALUE;
  }

  private FieldContact() {}

  static Direction movement(EmitterEntity emitter, EmitterEntity.Link link, Entity entity) {
    return movement(emitter, link, entity, FieldSpace.at(emitter).local(entity.getBoundingBox().getCenter()));
  }

  static Direction movement(EmitterEntity emitter, EmitterEntity.Link link, Entity entity, Vec3 center) {
    double side =
        link.normalCoordinate(center)
            - link.normalCoordinate(link.origin(emitter.getBlockPos()));
    double extent =
        (link.normal() == Direction.Axis.Y ? entity.getBbHeight() : entity.getBbWidth()) / 2;
    return link.movement(
        negative(emitter, link.target(), entity, side, extent + PLANAR_HALF_THICKNESS));
  }

  static boolean negative(
      EmitterEntity emitter, BlockPos target, Entity entity, double side, double margin) {
    long now = emitter.getLevel().getGameTime();
    var contacts = emitter.contactDirections;
    if (contacts.cleanedAt != now) {
      contacts
          .entries
          .values()
          .removeIf(entry -> now < entry.tick || now - entry.tick > CONTACT_GAP_TICKS);
      contacts.cleanedAt = now;
    }
    var key = new Key(entity.getUUID(), target);
    var old = contacts.entries.get(key);
    boolean negative = Math.abs(side) > margin || old == null ? side < 0 : old.negative;
    contacts.entries.put(key, new Entry(negative, now));
    return negative;
  }
}
