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

  public static void explosion(net.minecraftforge.event.level.ExplosionEvent.Detonate event) {
    event.getAffectedBlocks().removeIf(p -> protectedBlock(event.getLevel().getBlockState(p)));
  }

  public static void mobBreak(
      net.minecraftforge.event.entity.living.LivingDestroyBlockEvent event) {
    if (protectedBlock(event.getState())) event.setCanceled(true);
  }
}
