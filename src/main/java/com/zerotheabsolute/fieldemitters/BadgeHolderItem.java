package com.zerotheabsolute.fieldemitters;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

/** Inventory right-click copies no data: actual badge stacks move into or out of the holder. */
public final class BadgeHolderItem extends Item {
  public static final int CAPACITY = 16;

  public BadgeHolderItem(Properties p) {
    super(p);
  }

  public static List<ItemStack> contents(ItemStack holder) {
    var result = new ArrayList<ItemStack>();
    var tags = holder.getOrCreateTag().getList("Badges", net.minecraft.nbt.Tag.TAG_COMPOUND);
    for (int i=0;i<tags.size();i++) {
      var stack = ItemStack.of(tags.getCompound(i));
      if (!stack.isEmpty()) result.add(stack);
    }
    return result;
  }

  private static void save(ItemStack holder, List<ItemStack> list) {
    var tags = new net.minecraft.nbt.ListTag();
    for (var stack : list) tags.add(stack.save(new net.minecraft.nbt.CompoundTag()));
    holder.getOrCreateTag().put("Badges", tags);
  }

  @Override
  public boolean overrideOtherStackedOnMe(
      ItemStack holder,
      ItemStack cursor,
      Slot slot,
      ClickAction action,
      Player player,
      SlotAccess access) {
    if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
    var list = contents(holder);
    if (cursor.is(FieldEmitters.BADGE.get()) && list.size() < CAPACITY) {
      list.add(cursor.split(1));
      save(holder, list);
      return true;
    }
    if (cursor.isEmpty() && !list.isEmpty()) {
      access.set(list.remove(list.size()-1));
      save(holder, list);
      return true;
    }
    return true;
  }

  @Override
  public boolean overrideStackedOnOther(
      ItemStack holder, Slot slot, ClickAction action, Player player) {
    if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
    var list = contents(holder);
    var item = slot.getItem();
    if (item.is(FieldEmitters.BADGE.get()) && list.size() < CAPACITY) {
      list.add(slot.safeTake(1, 1, player));
      save(holder, list);
    } else if (item.isEmpty() && !list.isEmpty()) {
      var badge = list.remove(list.size()-1);
      var remainder = slot.safeInsert(badge);
      if (!remainder.isEmpty()) list.add(remainder);
      save(holder, list);
    }
    return true;
  }

  @Override
  public void appendHoverText(
      ItemStack stack, net.minecraft.world.level.Level context, List<Component> lines, TooltipFlag flags) {
    lines.add(
        Component.literal(
            "Right-click with a badge to store it; with an empty cursor to remove it."));
    lines.add(Component.literal("Holds 16 badges. Works in your inventory or Curios slot."));
    for (var badge : contents(stack))
      lines.add(badge.getHoverName().copy().withStyle(net.minecraft.ChatFormatting.AQUA));
  }
}
