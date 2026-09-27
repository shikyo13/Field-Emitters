package com.zerotheabsolute.fieldemitters.client;

import net.minecraft.network.chat.Component;

/** Resolve presentation text on the client, in the player's selected language. */
final class UiText {
  private UiText() {}

  static String text(String key, Object... arguments) {
    return Component.translatable(key, arguments).getString();
  }

  static String direction(net.minecraft.core.Direction direction) {
    return text("direction.fieldemitters." + direction.getName());
  }

  static String crossing(com.zeromods.core.filter.CrossingSense sense) {
    return text("crossing.fieldemitters." + sense.name().toLowerCase(java.util.Locale.ROOT));
  }

  static String formation(com.zeromods.core.animation.SphereFormation formation) {
    return text("formation.fieldemitters." + formation.name().toLowerCase(java.util.Locale.ROOT));
  }

  static String formation(int formation) {
    return switch (formation) {
      case 0 -> text("formation.fieldemitters.sweep");
      case 1 -> text("formation.fieldemitters.dissolve");
      case 2 -> text("formation.fieldemitters.fade");
      case 4 -> text("formation.fieldemitters.scan_line");
      case 6 -> text("formation.fieldemitters.tracing_ribs");
      default -> formation(com.zeromods.core.animation.PlanarProjection.style(formation));
    };
  }

  /** A whole number grouped for the selected language, such as 92,700. */
  static String number(long value) {
    return java.text.NumberFormat.getIntegerInstance(locale()).format(value);
  }

  /** A dimension's name, or its ID for dimensions without one. */
  static String dimension(String id) {
    String key = "screen.fieldemitters.dimension." + id.replace(':', '.');
    return net.minecraft.client.resources.language.I18n.exists(key) ? text(key) : id;
  }

  private static java.util.Locale locale() {
    String language =
        net.minecraft.client.Minecraft.getInstance().getLanguageManager().getSelected();
    return java.util.Locale.forLanguageTag(language.replace('_', '-'));
  }

  static String seconds(int ticks) {
    var number = java.text.NumberFormat.getNumberInstance(locale());
    number.setMinimumFractionDigits(1);
    number.setMaximumFractionDigits(1);
    return text(
        "screen.fieldemitters.seconds",
        number.format(ticks / (double) net.minecraft.SharedConstants.TICKS_PER_SECOND));
  }
}
