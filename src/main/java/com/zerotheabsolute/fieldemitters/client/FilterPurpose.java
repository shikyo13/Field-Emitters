package com.zerotheabsolute.fieldemitters.client;

/** Filter editors share presentation, but retain independent gameplay rules. */
enum FilterPurpose {
  BLOCKING,
  SENSOR,
  DAMAGE,
  CHECKPOINT;

  String title() {
    return UiText.text(
        "screen.fieldemitters.filter_purpose." + name().toLowerCase(java.util.Locale.ROOT));
  }
}
