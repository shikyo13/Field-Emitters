package com.zerotheabsolute.fieldemitters.client;

final class ScreenMetrics {
  static final int FILTER_HEADER_HEIGHT = 44;
  static final int FILTER_FOOTER_HEIGHT = 40;
  static final int DAMAGE_EXTRA_HEIGHT = 49;
  static final int SENSOR_EXTRA_HEIGHT = 64;
  static final int PANEL_WIDTH = 404;
  static final int PANEL_HEIGHT = 306;
  static final int CONTENT_WIDTH = 380;
  static final int BUTTON_HEIGHT = 18;
  static final int ROW_HEIGHT = 20;
  static final int COMPACT_ROW_HEIGHT = 27;
  static final int CONTENT_INSET = 12;
  static final int COLUMN_GAP = 6;
  static final int HALF_CONTENT_WIDTH = (CONTENT_WIDTH - COLUMN_GAP) / 2;
  static final int DIRECTION_COLUMN_STEP = 64;
  static final int DIRECTION_BUTTON_WIDTH = 61;
  static final int PLAYER_ROWS = 6;
  static final int TYPE_SLOTS = 24;
  static final int DAMAGE_INTERVAL_STEP = 10;
  static final int DAMAGE_INTERVAL_CYCLE_MAX = 100;
  static final int UUID_LENGTH = 36;
  static final long TEXT_DEBOUNCE_MILLIS = 350;
  static final long LOOKUP_TIMEOUT_MILLIS = 10_000;

  private ScreenMetrics() {}
}
