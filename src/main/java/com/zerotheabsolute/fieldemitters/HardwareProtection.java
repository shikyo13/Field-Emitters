package com.zerotheabsolute.fieldemitters;

import net.minecraft.world.level.block.state.BlockState;

/** Explosion resistance is independent of the existing mining hardness. */
public final class HardwareProtection {
  public static final float BLAST_RESISTANCE = 3_600_000;

  private HardwareProtection() {}

  public static boolean protectedBlock(BlockState state) {
    return state.is(FieldEmitters.EMITTER.get())
        || state.is(FieldEmitters.RAIL.get())
        || state.is(FieldEmitters.TOWER.get());
  }

}
