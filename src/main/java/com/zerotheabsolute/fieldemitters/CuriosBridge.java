package com.zerotheabsolute.fieldemitters;

import java.util.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Loaded only when Curios is present. Cosmetic slots never grant access. */
public final class CuriosBridge {
  public static void register() {
    var curio = new ICurioItem() {};
    CuriosApi.registerCurio(FieldEmitters.TUNER.get(), curio);
    CuriosApi.registerCurio(FieldEmitters.BADGE.get(), curio);
    CuriosApi.registerCurio(FieldEmitters.BADGE_HOLDER.get(), curio);
  }

  public static List<ItemStack> equipped(Player player) {
    var result = new ArrayList<ItemStack>();
    CuriosApi.getCuriosInventory(player)
        .ifPresent(
            inv ->
                inv.getCurios()
                    .values()
                    .forEach(
                        slots -> {
                          var stacks = slots.getStacks();
                          for (int i = 0; i < stacks.getSlots(); i++)
                            result.add(stacks.getStackInSlot(i));
                        }));
    return result;
  }
}
