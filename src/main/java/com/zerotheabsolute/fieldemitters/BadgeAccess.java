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
    return data;
  }

  public CompoundTag save(CompoundTag tag) {
    var list = new ListTag();
    issued.values().forEach(e -> list.add(e.copy()));
    tag.put("Issued", list);
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
      if (old != null && old.getUUID("Issuer").equals(issuer)) issued.remove(old.getUUID("Id"));
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
    issued
        .values()
        .removeIf(t -> t.getUUID("Issuer").equals(issuer) && t.getString("Group").equals(group));
    if (before != issued.size()) setDirty();
    return before - issued.size();
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
    if (net.minecraftforge.fml.ModList.get().isLoaded("curios"))
      result.addAll(CuriosBridge.equipped(player));
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
