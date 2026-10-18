package com.zerotheabsolute.fieldemitters;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.saveddata.SavedData;

/** Badge credentials are issued and revoked by the server, scoped to their issuing owner. */
public final class BadgeAccess extends SavedData {
  public static final Map<UUID, Set<String>> clientGrants = new HashMap<>();
  private final Map<UUID, CompoundTag> issued = new HashMap<>();
  private static final int PORTABLE_VERSION = 1;
  static final String PORTABLE_KEY = "CardCredentials";
  private UUID worldId = UUID.randomUUID();
  private final Set<UUID> retired = new HashSet<>();
  private final Map<UUID, Set<String>> revokedImports = new HashMap<>();

  public BadgeAccess() {
    // Persist the registry identity even if the first operation only exports a structure.
    setDirty();
  }

  public static BadgeAccess get(ServerLevel level) {
    return level
        .getServer()
        .overworld()
        .getDataStorage()
        .computeIfAbsent(
            BadgeAccess::load, BadgeAccess::new, "fieldemitters_badges");
  }

  public static BadgeAccess load(CompoundTag tag) {
    var data = new BadgeAccess();
    for (var value : tag.getList("Issued", 10)) {
      var entry = (CompoundTag) value;
      if (entry.hasUUID("Id") && entry.hasUUID("Issuer"))
        data.issued.put(entry.getUUID("Id"), entry.copy());
    }
    if (tag.hasUUID("WorldId")) data.worldId = tag.getUUID("WorldId");
    for (var value : tag.getList("Retired", Tag.TAG_COMPOUND)) {
      var entry = (CompoundTag) value;
      if (entry.hasUUID("Id")) data.retired.add(entry.getUUID("Id"));
    }
    for (var value : tag.getList("RevokedImports", Tag.TAG_COMPOUND)) {
      var entry = (CompoundTag) value;
      if (entry.hasUUID("Issuer") && validGroup(entry.getString("Group")))
        data.revokedImports.computeIfAbsent(entry.getUUID("Issuer"), id -> new HashSet<>())
            .add(entry.getString("Group"));
    }
    data.retired.forEach(data.issued::remove);
    return data;
  }

  public CompoundTag save(CompoundTag tag) {
    var list = new ListTag();
    issued.values().forEach(e -> list.add(e.copy()));
    tag.put("Issued", list);
    tag.putUUID("WorldId", worldId);
    var retiredTags = new ListTag();
    retired.forEach(id -> {
      var entry = new CompoundTag();
      entry.putUUID("Id", id);
      retiredTags.add(entry);
    });
    tag.put("Retired", retiredTags);
    var revokedTags = new ListTag();
    revokedImports.forEach((issuer, groups) -> groups.forEach(group -> {
      var entry = new CompoundTag();
      entry.putUUID("Issuer", issuer);
      entry.putString("Group", group);
      revokedTags.add(entry);
    }));
    tag.put("RevokedImports", revokedTags);
    return tag;
  }

  public static String group(String value) {
    return value.strip().toLowerCase(Locale.ROOT);
  }

  public static boolean validGroup(String value) {
    return value.matches("[a-z0-9][a-z0-9 _-]{0,31}");
  }

  public void issue(ItemStack stack, UUID issuer, String group, UUID player) {
    var previous = stack.getTag();
    if (previous != null && previous.copy().hasUUID("BadgeId")) {
      var old = issued.get(previous.copy().getUUID("BadgeId"));
      if (old != null && old.getUUID("Issuer").equals(issuer)) {
        issued.remove(old.getUUID("Id"));
        retired.add(old.getUUID("Id"));
      }
    }
    var tag = new CompoundTag();
    var id = UUID.randomUUID();
    tag.putUUID("Id", id);
    tag.putUUID("Issuer", issuer);
    tag.putString("Group", group);
    if (player != null) tag.putUUID("Player", player);
    issued.put(id, tag);
    setDirty();
    var display = tag.copy();
    display.putUUID("BadgeId", id);
    stack.setTag(display);
    stack.setHoverName(
        net.minecraft.network.chat.Component.translatable(
            "item.fieldemitters.access_badge.issued", group));
  }

  public int revoke(UUID issuer, String group) {
    int before = issued.size();
    issued.values().removeIf(t -> {
      boolean matches = t.getUUID("Issuer").equals(issuer) && t.getString("Group").equals(group);
      if (matches) retired.add(t.getUUID("Id"));
      return matches;
    });
    // Also reject older foreign snapshots that have not been loaded here yet.
    revokedImports.computeIfAbsent(issuer, id -> new HashSet<>()).add(group);
    setDirty();
    return before - issued.size();
  }

  /** Records travel with a saved structure, never with its client update packet. */
  public CompoundTag exportCards(EmitterEntity emitter) {
    var result = new CompoundTag();
    if (emitter.owner == null) return result;
    var groups = cardGroups(emitter);
    if (groups.isEmpty()) return result;
    var entries = new ListTag();
    for (var credential : issued.values())
      if (credential.getUUID("Issuer").equals(emitter.owner)
          && groups.contains(credential.getString("Group"))) entries.add(credential.copy());
    if (entries.isEmpty()) return result;
    result.putInt("Version", PORTABLE_VERSION);
    result.putUUID("WorldId", worldId);
    result.put("Issued", entries);
    return result;
  }

  /** An existing world's registry and revocations always win over a pasted snapshot. */
  public void importCards(EmitterEntity emitter, CompoundTag snapshot) {
    if (emitter.owner == null || snapshot.getInt("Version") != PORTABLE_VERSION
        || !snapshot.hasUUID("WorldId") || worldId.equals(snapshot.getUUID("WorldId"))) return;
    var groups = cardGroups(emitter);
    var revoked = revokedImports.getOrDefault(emitter.owner, Set.of());
    boolean changed = false;
    for (var value : snapshot.getList("Issued", Tag.TAG_COMPOUND)) {
      var entry = (CompoundTag) value;
      if (!entry.hasUUID("Id") || !entry.hasUUID("Issuer")
          || !entry.getUUID("Issuer").equals(emitter.owner)
          || !validGroup(entry.getString("Group"))
          || !groups.contains(entry.getString("Group"))
          || revoked.contains(entry.getString("Group"))
          || entry.contains("Player") && !entry.hasUUID("Player")) continue;
      var id = entry.getUUID("Id");
      if (!retired.contains(id) && !issued.containsKey(id)) {
        var credential = new CompoundTag();
        credential.putUUID("Id", id);
        credential.putUUID("Issuer", emitter.owner);
        credential.putString("Group", entry.getString("Group"));
        if (entry.hasUUID("Player")) credential.putUUID("Player", entry.getUUID("Player"));
        issued.put(id, credential);
        changed = true;
      }
    }
    if (changed) setDirty();
  }

  private static Set<String> cardGroups(EmitterEntity emitter) {
    var result = new HashSet<String>();
    var settings = new ArrayList<ControlSettings>();
    settings.add(emitter.controls);
    settings.addAll(emitter.overrides.values());
    for (var control : settings) {
      result.addAll(control.cards.groups);
      for (var filter : control.filters()) {
        result.addAll(filter.accessGroups);
        for (var target : filter.included)
          if (target.kind().equals(FieldTargets.CARD_GROUP)) result.add(target.id());
        for (var target : filter.excluded)
          if (target.kind().equals(FieldTargets.CARD_GROUP)) result.add(target.id());
      }
    }
    return result;
  }

  public boolean accepts(ItemStack stack, UUID issuer, UUID player, Set<String> groups) {
    if (!stack.is(FieldEmitters.BADGE.get())) return false;
    var data = stack.getTag();
    if (data == null) return false;
    var tag = data.copy();
    if (!tag.hasUUID("BadgeId")) return false;
    var credential = issued.get(tag.getUUID("BadgeId"));
    return credential != null
        && credential.getUUID("Issuer").equals(issuer)
        && groups.contains(credential.getString("Group"))
        && (!credential.hasUUID("Player") || credential.getUUID("Player").equals(player));
  }

  public static List<ItemStack> carried(Player player) {
    var result = new ArrayList<ItemStack>();
    for (int i = 0; i < player.getInventory().getContainerSize(); i++)
      result.add(player.getInventory().getItem(i));
    if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("trinkets"))
      result.addAll(TrinketsBridge.equipped(player));
    return result;
  }

  public static CompoundTag grants(Player player, ServerLevel level) {
    var result = new CompoundTag();
    var data = get(level);
    var stacks = new ArrayList<ItemStack>();
    for (var stack : carried(player)) {
      stacks.add(stack);
      if (stack.is(FieldEmitters.BADGE_HOLDER.get()))
        stacks.addAll(BadgeHolderItem.contents(stack));
    }
    for (var stack : stacks) {
      if (!stack.is(FieldEmitters.BADGE.get())) continue;
      var custom = stack.getTag();
      if (custom == null) continue;
      var tag = custom.copy();
      if (!tag.hasUUID("BadgeId")) continue;
      var credential = data.issued.get(tag.getUUID("BadgeId"));
      if (credential == null
          || credential.hasUUID("Player") && !credential.getUUID("Player").equals(player.getUUID()))
        continue;
      String issuer = credential.getUUID("Issuer").toString();
      var list = result.getList(issuer, 8);
      var group = StringTag.valueOf(credential.getString("Group"));
      if (!list.contains(group)) list.add(group);
      result.put(issuer, list);
    }
    return result;
  }

  public static boolean matches(Player player, UUID owner, Set<String> groups) {
    if (owner == null || groups.isEmpty()) return false;
    if (player.level().isClientSide)
      return groups.stream().anyMatch(clientGrants.getOrDefault(owner, Set.of())::contains);
    if (!(player.level() instanceof ServerLevel level)) return false;
    var data = get(level);
    for (var stack : carried(player)) {
      if (data.accepts(stack, owner, player.getUUID(), groups)) return true;
      if (stack.is(FieldEmitters.BADGE_HOLDER.get()))
        for (var badge : BadgeHolderItem.contents(stack))
          if (data.accepts(badge, owner, player.getUUID(), groups)) return true;
    }
    return false;
  }
}
