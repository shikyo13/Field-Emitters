package com.zerotheabsolute.fieldemitters;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FieldConfig {
  public static final ModConfigSpec SPEC;
  public static final ModConfigSpec CLIENT_SPEC;
  public static final ModConfigSpec.BooleanValue SHOW_GUIDES;
  public static final ModConfigSpec.BooleanValue DEMO_POWER;

  static {
    var b = new ModConfigSpec.Builder();
    DEMO_POWER =
        b.comment(
                "Testing only: redstone input supplies unlimited energy. Normal worlds require FE.")
            .define("demoRedstonePower", false);
    SPEC = b.build();
    var client = new ModConfigSpec.Builder();
    SHOW_GUIDES =
        client
            .comment("Show direction arrows while holding the tuner or previewing a connection.")
            .define("showDirectionGuides", true);
    CLIENT_SPEC = client.build();
  }
}
