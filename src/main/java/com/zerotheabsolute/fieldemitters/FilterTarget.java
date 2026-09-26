package com.zerotheabsolute.fieldemitters;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

/** A named exception to a filter's category selection. */
public record FilterTarget(Kind kind, String id, String name) {
  public enum Kind { MOB, ITEM, PLAYER, CARD_GROUP, INDIVIDUAL }

  public boolean matches(Entity entity, UUID owner) {
    var subject = new com.zeromods.core.neoforge.MinecraftEntitySubject(entity);
    return switch (kind) {
      case MOB -> subject.entityType(id);
      case ITEM -> entity instanceof ItemEntity && subject.itemType(id);
      case INDIVIDUAL -> entity.getUUID().toString().equalsIgnoreCase(id);
      case PLAYER -> entity instanceof Player && entity.getUUID().toString().equalsIgnoreCase(id);
      case CARD_GROUP -> entity instanceof Player player && BadgeAccess.matches(player, owner, java.util.Set.of(id));
    };
  }

  public CompoundTag save() {
    var tag = new CompoundTag();
    tag.putString("Kind", kind.name()); tag.putString("Id", id); tag.putString("Name", name);
    return tag;
  }

  public static FilterTarget load(CompoundTag tag) {
    try {
      String id = tag.getString("Id"), name = tag.getString("Name");
      Kind kind = Kind.valueOf(tag.getString("Kind"));
      if (id.length() > EntityFilter.MAX_TYPE_LENGTH || name.length() > (kind == Kind.INDIVIDUAL ? EntityFilter.MAX_TYPE_LENGTH : 16)) return null;
      return new FilterTarget(kind, id, name);
    } catch (IllegalArgumentException e) { return null; }
  }

  public net.minecraft.network.chat.Component validate() {
    if (kind == Kind.MOB && id.equals("minecraft:player")) return null;
    if (kind == Kind.MOB || kind == Kind.ITEM) return FieldControls.validateTypeEntry(id, kind == Kind.ITEM);
    if (kind == Kind.CARD_GROUP && BadgeAccess.validGroup(id)) return null;
    if (kind == Kind.INDIVIDUAL) {
      try {
        UUID.fromString(id);
        return name.equals("minecraft:player") ? null : FieldControls.validateTypeEntry(name, false);
      } catch (IllegalArgumentException ignored) {}
    }
    if (kind == Kind.PLAYER) {
      try {
        UUID.fromString(id);
        if (name.isEmpty() || name.matches("[A-Za-z0-9_]{1,16}")) return null;
      } catch (IllegalArgumentException ignored) {}
    }
    return net.minecraft.network.chat.Component.translatable("message.fieldemitters.fieldcontrols.invalid_id_or_tag", id);
  }
}
