package com.zerotheabsolute.fieldemitters;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;

/** Categories are ORed; optional constraints are ANDed. Inversion applies last. */
public final class EntityFilter {
  public static final int MAX_TYPE_LENGTH = 128;
  public static final int MAX_ACCESS_GROUPS = 64;
  public int groups = 1, age = 0, directions = 63;

  /** Whether the selection below is read as world directions or as crossings of an enclosure. */
  public com.zeromods.core.filter.DirectionFrame frame =
      com.zeromods.core.filter.DirectionFrame.WORLD;

  /** Selected crossing senses while the relative frame is in use. */
  public int crossings = com.zeromods.core.filter.CrossingSense.ALL;

  /** 0 uses general filters; 1 matches listed players; 2 matches unlisted players. */
  public int playerMode = 0;

  public final java.util.LinkedHashSet<String> accessGroups = new java.util.LinkedHashSet<>();
  public final java.util.LinkedHashMap<UUID, String> playerList = new java.util.LinkedHashMap<>();
  public static final int MAX_PLAYERS = 64;

  /** 0 follows the general filter; 1 matches listed types; 2 matches unlisted types. */
  public int mobMode = 0, itemMode = 0;

  public static final int MAX_TYPES = 64;
  public final java.util.LinkedHashSet<String> mobList = new java.util.LinkedHashSet<>(),
      itemList = new java.util.LinkedHashSet<>();
  public boolean inverted = false, exemptOwner = false;
  public String entityType = "", itemType = "", identity = "", entityTag = "";

  public boolean matches(Entity entity, UUID owner) {
    if (entity == null) return false;
    if (entity instanceof net.minecraft.world.entity.player.Player && playerMode != 0) {
      if (entity.isSpectator() || exemptOwner && entity.getUUID().equals(owner)) return false;
      return com.zeromods.core.filter.PlayerListMode.values()[Math.max(0, Math.min(2, playerMode))]
          .matches(
              playerList.containsKey(entity.getUUID())
                  || BadgeAccess.matches(
                      (net.minecraft.world.entity.player.Player) entity, owner, accessGroups));
    }
    var subject = new com.zeromods.core.neoforge.MinecraftEntitySubject(entity);
    boolean mob =
        entity instanceof LivingEntity
            && !(entity instanceof net.minecraft.world.entity.player.Player);
    boolean item = entity instanceof net.minecraft.world.entity.item.ItemEntity;
    int mode = mob ? mobMode : item ? itemMode : 0;
    if (mode != 0) {
      if (entity.isSpectator() || exemptOwner && entity.getUUID().equals(owner)) return false;
      boolean details =
          !mob
              || (age == 0 || subject.baby() == (age == 1))
                  && (identity.isEmpty() || entity.getUUID().toString().equalsIgnoreCase(identity))
                  && (entityTag.isEmpty() || subject.scoreboardTag(entityTag));
      return com.zeromods.core.filter.TypeList.matches(
          mode, mob ? mobList : itemList, mob ? subject::entityType : subject::itemType, details);
    }
    var selection =
        new com.zeromods.core.filter.EntitySelection(
            groups,
            com.zeromods.core.filter.EntitySelection.Age.values()[Math.max(0, Math.min(2, age))],
            inverted,
            exemptOwner,
            identity,
            entityTag,
            entityType,
            itemType);
    return selection.matches(new com.zeromods.core.neoforge.MinecraftEntitySubject(entity), owner);
  }

  public boolean direction(net.minecraft.core.Direction movement) {
    return (directions & (1 << movement.ordinal())) != 0;
  }

  /**
   * Whether this rule covers a crossing, given which way the span faces into the enclosure. In the
   * relative frame one setting reads correctly on every side of a perimeter, because each span
   * translates the crossing into inward or outward for itself. Vertical crossings and spans with no
   * enclosure fall back to world directions.
   */
  public boolean direction(net.minecraft.core.Direction movement, net.minecraft.core.Direction inward) {
    if (!relative(inward) || movement.getAxis() == net.minecraft.core.Direction.Axis.Y)
      return direction(movement);
    var sense =
        movement == inward
            ? com.zeromods.core.filter.CrossingSense.INWARD
            : com.zeromods.core.filter.CrossingSense.OUTWARD;
    return sense.selected(crossings);
  }

  /** Whether this rule is written relative to an enclosure that the span actually has. */
  public boolean relative(net.minecraft.core.Direction inward) {
    return frame == com.zeromods.core.filter.DirectionFrame.RELATIVE && inward != null;
  }

  public CompoundTag save() {
    var t = new CompoundTag();
    t.putInt("Groups", groups);
    t.putInt("PlayerMode", playerMode);
    saveTypes(t, "AccessGroups", accessGroups);
    var players = new net.minecraft.nbt.ListTag();
    playerList.forEach(
        (id, name) -> {
          var entry = new CompoundTag();
          entry.putUUID("Id", id);
          entry.putString("Name", name);
          players.add(entry);
        });
    t.put("PlayerList", players);
    t.putInt("MobMode", mobMode);
    t.putInt("ItemMode", itemMode);
    saveTypes(t, "MobList", mobList);
    saveTypes(t, "ItemList", itemList);
    t.putInt("Age", age);
    t.putInt("Directions", directions);
    t.putInt("DirectionFrame", frame.id());
    t.putInt("Crossings", crossings);
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
    f.playerMode = Math.max(0, Math.min(2, t.getInt("PlayerMode")));
    loadTypes(t, "AccessGroups", f.accessGroups);
    var players = t.getList("PlayerList", 10);
    for (int i = 0; i < Math.min(MAX_PLAYERS, players.size()); i++) {
      var entry = players.getCompound(i);
      if (!entry.hasUUID("Id")) continue;
      String name = entry.getString("Name");
      f.playerList.put(entry.getUUID("Id"), name.matches("[A-Za-z0-9_]{1,16}") ? name : "");
    }
    f.mobMode = Math.max(0, Math.min(2, t.getInt("MobMode")));
    f.itemMode = Math.max(0, Math.min(2, t.getInt("ItemMode")));
    loadTypes(t, "MobList", f.mobList);
    loadTypes(t, "ItemList", f.itemList);
    f.age = Math.max(0, Math.min(2, t.getInt("Age")));
    f.directions = t.contains("Directions") ? t.getInt("Directions") & 63 : 63;
    f.frame = com.zeromods.core.filter.DirectionFrame.byId(t.getInt("DirectionFrame"));
    f.crossings =
        t.contains("Crossings")
            ? com.zeromods.core.filter.CrossingSense.sanitize(t.getInt("Crossings"))
            : com.zeromods.core.filter.CrossingSense.ALL;
    f.inverted = t.getBoolean("Invert");
    f.exemptOwner = t.getBoolean("OwnerExempt");
    f.entityType = bounded(t.getString("Entity"));
    f.itemType = bounded(t.getString("Item"));
    f.identity = bounded(t.getString("Identity"));
    f.entityTag = bounded(t.getString("Tag"));
    return f;
  }

  private static void saveTypes(CompoundTag tag, String key, java.util.Set<String> values) {
    var list = new net.minecraft.nbt.ListTag();
    values.forEach(value -> list.add(net.minecraft.nbt.StringTag.valueOf(value)));
    tag.put(key, list);
  }

  private static void loadTypes(CompoundTag tag, String key, java.util.Set<String> values) {
    var list = tag.getList(key, 8);
    for (int i = 0; i < Math.min(MAX_TYPES, list.size()); i++)
      values.add(bounded(list.getString(i)));
  }

  private static String bounded(String s) {
    return s.substring(0, Math.min(s.length(), MAX_TYPE_LENGTH)).trim();
  }
}
