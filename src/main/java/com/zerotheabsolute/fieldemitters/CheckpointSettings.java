package com.zerotheabsolute.fieldemitters;

import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class CheckpointSettings {
  public boolean enabled, detect, deny;

  /** 0 keep items; 1 drop on entry side; 2 insert into adjacent inventory. */
  public int confiscate;

  public boolean dropOverflow;
  public Direction storageFace = Direction.DOWN;
  public EntityFilter players = new EntityFilter(), items = new EntityFilter();
  public final com.zeromods.core.filter.DirectionalRules<Direction, EntityFilter> directions =
      new com.zeromods.core.filter.DirectionalRules<>();

  public CheckpointSettings() {
    players.groups = 4;
    players.exemptOwner = true;
    items.groups = 8;
    items.itemMode = 1;
  }

  public boolean applies(Player player, UUID owner, Direction movement) {
    var rule = directions.resolve(movement, players);
    return enabled && rule.direction(movement) && rule.matches(player, owner);
  }

  public boolean matches(ItemStack stack) {
    if (stack.isEmpty()) return false;
    java.util.function.Predicate<String> match =
        value -> {
          boolean tag = value.startsWith("#");
          var id =
              net.minecraft.resources.ResourceLocation.tryParse(tag ? value.substring(1) : value);
          if (id == null) return false;
          return tag
              ? stack.is(
                  net.minecraft.tags.TagKey.create(
                      net.minecraft.core.registries.Registries.ITEM, id))
              : net.minecraft.core.registries.BuiltInRegistries.ITEM
                  .getKey(stack.getItem())
                  .equals(id);
        };
    if (items.itemMode == 0) return !items.itemType.isBlank() && match.test(items.itemType);
    boolean listed = items.itemList.stream().anyMatch(match);
    return items.itemMode == 1 ? listed : !listed;
  }

  public CompoundTag save() {
    var t = new CompoundTag();
    t.putBoolean("Enabled", enabled);
    t.putBoolean("Detect", detect);
    t.putBoolean("Deny", deny);
    t.putInt("Confiscate", confiscate);
    t.putBoolean("DropOverflow", dropOverflow);
    t.putInt("StorageFace", storageFace.ordinal());
    t.put("Players", players.save());
    t.put("Items", items.save());
    var rules = new CompoundTag();
    directions.overrides().forEach((d, f) -> rules.put(d.getName(), f.save()));
    t.put("Directions", rules);
    return t;
  }

  public static CheckpointSettings load(CompoundTag t) {
    var s = new CheckpointSettings();
    s.enabled = t.getBoolean("Enabled");
    s.detect = t.getBoolean("Detect");
    s.deny = t.getBoolean("Deny");
    s.confiscate = Math.max(0, Math.min(2, t.getInt("Confiscate")));
    s.dropOverflow = t.getBoolean("DropOverflow");
    s.storageFace = Direction.from3DDataValue(Math.max(0, Math.min(5, t.getInt("StorageFace"))));
    if (t.contains("Players")) s.players = EntityFilter.load(t.getCompound("Players"));
    if (t.contains("Items")) s.items = EntityFilter.load(t.getCompound("Items"));
    var rules = t.getCompound("Directions");
    for (var d : Direction.values())
      if (rules.contains(d.getName()))
        s.directions.set(d, EntityFilter.load(rules.getCompound(d.getName())));
    return s;
  }
}
