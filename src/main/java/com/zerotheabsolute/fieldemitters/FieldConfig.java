package com.zerotheabsolute.fieldemitters;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FieldConfig {
  public static final ModConfigSpec SPEC;
  public static final ModConfigSpec CLIENT_SPEC;
  public static final ModConfigSpec.BooleanValue SHOW_GUIDES;
  public static final ModConfigSpec.IntValue ENERGY_PER_CELL;
  public static final ModConfigSpec.IntValue CAPACITY;
  public static final ModConfigSpec.IntValue TRANSFER;

  public static final int DEFAULT_ENERGY_PER_CELL = 2;
  public static final int DEFAULT_CAPACITY = 100000;
  public static final int DEFAULT_TRANSFER = 10000;

  static {
    var b = new ModConfigSpec.Builder();
    b.push("balance");
    ENERGY_PER_CELL =
        b.comment("FE consumed per projected field block each tick while a field is running.")
            .defineInRange("energyPerCellTick", DEFAULT_ENERGY_PER_CELL, 0, 10000);
    CAPACITY =
        b.comment("FE stored by each Field Emitter and Field Rail.")
            .defineInRange("emitterCapacity", DEFAULT_CAPACITY, 1000, 1000000000);
    TRANSFER =
        b.comment("Maximum FE per tick an emitter or rail accepts from cables and generators.")
            .defineInRange("emitterTransferRate", DEFAULT_TRANSFER, 1, 1000000000);
    b.pop();
    SPEC = b.build();
    var client = new ModConfigSpec.Builder();
    SHOW_GUIDES =
        client
            .comment("Show direction arrows while holding the tuner or previewing a connection.")
            .define("showDirectionGuides", true);
    CLIENT_SPEC = client.build();
  }

  public static int energyPerCell() {
    return SPEC.isLoaded() ? ENERGY_PER_CELL.get() : DEFAULT_ENERGY_PER_CELL;
  }

  public static int capacity() {
    return SPEC.isLoaded() ? CAPACITY.get() : DEFAULT_CAPACITY;
  }

  public static int transfer() {
    return SPEC.isLoaded() ? TRANSFER.get() : DEFAULT_TRANSFER;
  }
}
