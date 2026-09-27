package com.zerotheabsolute.fieldemitters.client;

import com.zeromods.core.client.filter.ExceptionListPanel;
import com.zeromods.core.client.filter.ExceptionListStyle;
import com.zeromods.core.client.filter.StandardTargets;
import com.zeromods.core.filter.FilterTarget;
import com.zeromods.core.ui.UiTheme;
import com.zerotheabsolute.fieldemitters.*;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

/** Field Emitters' wording, colors and card groups for the shared exception lists. */
final class FilterListPanel implements ExceptionListStyle {
  private static final UiTheme THEME = new UiTheme(0xFF0D1B26, 0xFF122330, 0xFF354D63,
      0xFFE0F3FF, 0xFFADBED0, 0xFF53BBCB, 0xFF8DD5A2, 0xFFE0C872, 0xFFE09A9A);
  private final FilterPurpose purpose;

  private FilterListPanel(FilterPurpose purpose) { this.purpose = purpose; }

  static ExceptionListPanel create(EntityFilter filter, FilterPurpose purpose, int x, int y,
      int width, int[] pages, Consumer<Boolean> add, Runnable changed) {
    return new ExceptionListPanel(filter.exceptions, new FilterListPanel(purpose), x, y, width,
        pages, add, changed);
  }

  static String text(String key, Object... args) {
    return UiText.text("screen.fieldemitters.targets." + key, args);
  }

  @Override public UiTheme theme() { return THEME; }

  @Override public String heading(boolean exclude) {
    return text(purpose.name().toLowerCase(Locale.ROOT) + (exclude ? ".exclude" : ".include"));
  }

  /** Blocking and damage lists deny where they stop or hurt; the other list is green. */
  @Override public boolean denies(boolean exclude) {
    return exclude != (purpose == FilterPurpose.BLOCKING || purpose == FilterPurpose.DAMAGE);
  }

  @Override public String name(FilterTarget target) { return displayName(target); }

  @Override public ItemStack icon(FilterTarget target) {
    var icon = StandardTargets.icon(target);
    return icon != null ? icon : new ItemStack(Items.NAME_TAG);
  }

  @Override public String emptyHint() { return text("drop"); }

  @Override public String addTooltip(boolean exclude) { return text("add_to", heading(exclude)); }

  @Override public String removeTooltip(FilterTarget target) {
    return text("remove", displayName(target));
  }

  @Override public AbstractButton button(int x, int y, int width, int height, String label,
      Runnable press) {
    return new FieldButton(x, y, width, height, Component.literal(label), b -> press.run(), false);
  }

  static String displayName(FilterTarget target) {
    if (target.kind().equals(FieldTargets.CARD_GROUP)) return text("card_group", target.id());
    String name = StandardTargets.name(target);
    if (name == null) return target.id();
    if (target.kind().equals(FilterTarget.INDIVIDUAL))
      return text("individual_name", name, target.id().substring(0, Math.min(8, target.id().length())));
    return name;
  }

  static FilterTarget fromStack(ItemStack stack) {
    if (stack.getItem() instanceof SpawnEggItem egg)
      return new FilterTarget(FilterTarget.MOB,
          BuiltInRegistries.ENTITY_TYPE.getKey(egg.getType(stack)).toString(), "");
    return new FilterTarget(FilterTarget.ITEM,
        BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), "");
  }
}
