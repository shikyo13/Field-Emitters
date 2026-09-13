package com.zerotheabsolute.fieldemitters;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Short vanilla sound palettes, no looping hum. Every field sound passes the master switch. */
public final class FieldSounds {
  private FieldSounds() {}

  public static void play(ServerLevel level, EmitterEntity e, Vec3 pos, int event) {
    var c = e.controls;
    if (!c.sounds || (event < 2 && !c.powerSounds)
        || (event == 2 && !c.impactSounds) || (event == 3 && !c.damageSounds)) return;
    var sound = switch (c.soundStyle) {
      case 1 -> event == 3 ? SoundEvents.AMETHYST_BLOCK_BREAK : SoundEvents.AMETHYST_BLOCK_CHIME;
      case 2 -> event == 3 ? SoundEvents.GENERIC_BURN : SoundEvents.RESPAWN_ANCHOR_CHARGE;
      default -> event == 3 ? SoundEvents.FIRE_EXTINGUISH
          : event == 2 ? SoundEvents.AMETHYST_BLOCK_RESONATE
          : event == 0 ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE;
    };
    if (event != 3) pos = FieldSpace.at(e).world(pos);
    level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.BLOCKS,
        event == 3 ? .16f : .20f, event == 1 ? .85f : 1.6f);
  }
}
