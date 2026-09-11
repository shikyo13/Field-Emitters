package com.zerotheabsolute.fieldemitters;

import java.util.UUID;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/** Categories are ORed; optional constraints are ANDed. Inversion applies last. */
public final class EntityFilter {
  public int groups = 1, age = 0, directions = 63;
  public boolean inverted = false, exemptOwner = false;
  public String entityType = "", itemType = "", identity = "", entityTag = "";

  public boolean matches(Entity entity, UUID owner) {
    if (entity == null || entity.isSpectator()) return false;
    if (exemptOwner && entity.getUUID().equals(owner)) return false;
    int category =
        entity instanceof Player
            ? 4
            : entity instanceof Enemy
                ? 1
                : entity instanceof LivingEntity ? 2 : entity instanceof ItemEntity ? 8 : 16;
    boolean match = (groups & category) != 0;
    if (age != 0) match &= entity instanceof LivingEntity living && (living.isBaby() == (age == 1));
    if (!identity.isEmpty()) match &= entity.getUUID().toString().equalsIgnoreCase(identity);
    if (!entityTag.isEmpty()) match &= entity.getTags().contains(entityTag);
    if (!entityType.isEmpty()) {
      var id =
          ResourceLocation.tryParse(
              entityType.startsWith("#") ? entityType.substring(1) : entityType);
      match &=
          id != null
              && (entityType.startsWith("#")
                  ? entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, id))
                  : BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(id));
    }
    if (!itemType.isEmpty()) {
      var id =
          ResourceLocation.tryParse(itemType.startsWith("#") ? itemType.substring(1) : itemType);
      match &=
          entity instanceof ItemEntity item
              && id != null
              && (itemType.startsWith("#")
                  ? item.getItem().is(TagKey.create(Registries.ITEM, id))
                  : BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).equals(id));
    }
    return inverted != match;
  }

  public boolean direction(net.minecraft.core.Direction movement) {
    return (directions & (1 << movement.ordinal())) != 0;
  }

  public CompoundTag save() {
    var t = new CompoundTag();
    t.putInt("Groups", groups);
    t.putInt("Age", age);
    t.putInt("Directions", directions);
    t.putBoolean("Invert", inverted);
    t.putBoolean("OwnerExempt", exemptOwner);
    t.putString("Entity", entityType);
    t.putString("Item", itemType);
    t.putString("Identity", identity);
    t.putString("Tag", entityTag);
    return t;
  }

  public static EntityFilter load(CompoundTag t) {
    var f = new EntityFilter();
    f.groups = t.getInt("Groups") & 31;
    f.age = Math.max(0, Math.min(2, t.getInt("Age")));
    f.directions = t.contains("Directions") ? t.getInt("Directions") & 63 : 63;
    f.inverted = t.getBoolean("Invert");
    f.exemptOwner = t.getBoolean("OwnerExempt");
    f.entityType = bounded(t.getString("Entity"));
    f.itemType = bounded(t.getString("Item"));
    f.identity = bounded(t.getString("Identity"));
    f.entityTag = bounded(t.getString("Tag"));
    return f;
  }

  private static String bounded(String s) {
    return s.substring(0, Math.min(s.length(), 128)).trim();
  }
}
