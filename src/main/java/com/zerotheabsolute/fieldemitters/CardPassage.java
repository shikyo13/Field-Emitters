package com.zerotheabsolute.fieldemitters;

import java.util.LinkedHashSet;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Passage credentials do not exempt their holders from damage or checkpoints. */
public final class CardPassage {
  public boolean enabled;
  public final LinkedHashSet<String> groups = new LinkedHashSet<>();

  public boolean allows(Entity entity, UUID owner) {
    return enabled && entity instanceof Player player && BadgeAccess.matches(player, owner, groups);
  }

  public CompoundTag save() {
    var tag = new CompoundTag();
    tag.putBoolean("Enabled", enabled);
    var list = new ListTag();
    groups.forEach(group -> list.add(StringTag.valueOf(group)));
    tag.put("AllowedGroups", list);
    return tag;
  }

  public void copyFrom(CardPassage source) {
    enabled = source.enabled;
    groups.clear();
    groups.addAll(source.groups);
  }

  public static CardPassage load(CompoundTag tag) {
    var result = new CardPassage();
    result.enabled = tag.getBoolean("Enabled");
    for (var value : tag.getList("AllowedGroups", 8)) {
      var group = BadgeAccess.group(value.getAsString());
      if (BadgeAccess.validGroup(group) && result.groups.size() < EntityFilter.MAX_ACCESS_GROUPS)
        result.groups.add(group);
    }
    return result;
  }
}
