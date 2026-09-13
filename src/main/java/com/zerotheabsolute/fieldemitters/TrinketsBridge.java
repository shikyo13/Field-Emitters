package com.zerotheabsolute.fieldemitters;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import dev.emi.trinkets.api.TrinketsApi;
public final class TrinketsBridge {
  private TrinketsBridge() {}
  public static List<ItemStack> equipped(Player player) {
    var result = new ArrayList<ItemStack>();
    TrinketsApi.getTrinketComponent(player).ifPresent(component ->
        component.getAllEquipped().forEach(entry -> result.add(entry.getB())));
    return result;
  }
}
