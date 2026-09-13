package com.zerotheabsolute.fieldemitters;

import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;

/** Inventory right-click copies no data: actual badge stacks move into or out of the holder. */
public final class BadgeHolderItem extends Item {
  public static final int CAPACITY = 16;

  public BadgeHolderItem(Properties p) {
    super(p);
  }

  public static List<ItemStack> contents(ItemStack holder) {
    return new ArrayList<>(
        holder.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).stream()
            .filter(s -> !s.isEmpty())
            .map(ItemStack::copy)
            .toList());
  }

  private static void save(ItemStack holder, List<ItemStack> list) {
    holder.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
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
      access.set(list.removeLast());
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
      var badge = list.removeLast();
      var remainder = slot.safeInsert(badge);
      if (!remainder.isEmpty()) list.add(remainder);
      save(holder, list);
    }
    return true;
  }

  @Override
  public void appendHoverText(
      ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flags) {
    lines.add(
        Component.literal(
            "Right-click with a badge to store it; with an empty cursor to remove it."));
    lines.add(Component.literal("Holds 16 badges. Works anywhere in your inventory."));
    for (var badge : contents(stack))
      lines.add(badge.getHoverName().copy().withStyle(net.minecraft.ChatFormatting.AQUA));
  }
}
