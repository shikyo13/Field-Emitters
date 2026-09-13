package com.zerotheabsolute.fieldemitters;

import com.zeromods.core.forge.NetworkSavedData;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public final class ManagementAccess {
  public static final int MAX_MANAGERS = 64;
  private static final String PUBLIC = "fieldemitters:public_management";
  private static final String STORE = "fieldemitters_core_networks";

  private ManagementAccess() {}

  public static boolean owner(EmitterEntity e, Player p) {
    return p.hasPermissions(2) || p.getUUID().equals(e.owner);
  }

  public static boolean editable(EmitterEntity e, Player p) {
    if (owner(e, p)) return true;
    if (e.getLevel() != null && e.getLevel().isClientSide)
      return e.managementPublic || e.managerIds.contains(p.getUUID());
    var network = e.managedNetwork;
    if (network == null) return false;
    return network.canConfigure(p.getUUID())
        || network.property(PUBLIC).map(Boolean::parseBoolean).orElse(false);
  }

  public static CompoundTag snapshot(EmitterEntity e) {
    var tag = new CompoundTag();
    var network = e.managedNetwork;
    tag.putBoolean(
        "Public",
        network != null && network.property(PUBLIC).map(Boolean::parseBoolean).orElse(false));
    var list = new ListTag();
    if (network != null)
      network.memberIds().forEach(id -> list.add(StringTag.valueOf(id.toString())));
    tag.put("Managers", list);
    return tag;
  }

  public static void update(
      ServerLevel level, EmitterEntity e, Player actor, int action, UUID member) {
    if (!owner(e, actor)) return;
    ManagedFields.refresh(level, FieldNetwork.loaded(level));
    var network = e.managedNetwork;
    if (network == null) return;
    switch (action) {
      case 0 -> network.property(PUBLIC, "false");
      case 1 -> network.property(PUBLIC, "true");
      case 2 -> {
        if (member != null && network.memberIds().size() < MAX_MANAGERS) network.addMember(member);
      }
      case 3 -> {
        if (member != null) network.removeMember(member);
      }
      default -> {
        return;
      }
    }
    NetworkSavedData.get(level, STORE).setDirty();
    for (var part : FieldNetwork.loaded(level)) if (part.managedNetwork == network) part.sync();
  }
}
