package com.zerotheabsolute.fieldemitters.client;

enum ControlTab {
  OVERVIEW,
  BLOCKING,
  SENSOR,
  DAMAGE,
  APPEARANCE,
  SOUNDS,
  CONNECTIONS,
  ACCESS;

  boolean hasFilter() {
    return this == BLOCKING || this == SENSOR || this == DAMAGE;
  }

  FilterPurpose purpose() {
    return switch (this) {
      case BLOCKING -> FilterPurpose.BLOCKING;
      case SENSOR -> FilterPurpose.SENSOR;
      case DAMAGE -> FilterPurpose.DAMAGE;
      default -> throw new IllegalStateException("This tab has no entity filter: " + this);
    };
  }

  String translationKey() {
    return "screen.fieldemitters.tab." + name().toLowerCase(java.util.Locale.ROOT);
  }
}
