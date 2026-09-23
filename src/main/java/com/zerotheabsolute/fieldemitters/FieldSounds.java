package com.zerotheabsolute.fieldemitters;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Short vanilla sound palettes, no looping hum. Every field sound passes the master switch. */
public final class FieldSounds {
  public static final int PALETTES = 4;

  private FieldSounds() {}

  private record Voice(SoundEvent sound, float volume, float pitch) {}

  public static void play(ServerLevel level, EmitterEntity e, Vec3 pos, int event) {
    var c = e.controls;
    if (!c.sounds || (event < 2 && !c.powerSounds)
        || (event == 2 && !c.impactSounds) || (event == 3 && !c.damageSounds)) return;
    var voice = voice(c.soundStyle, event);
    if (event != 3) pos = FieldSpace.at(e).world(pos);
    level.playSound(null, pos.x, pos.y, pos.z, voice.sound(), SoundSource.BLOCKS,
        voice.volume(), voice.pitch());
  }

  /**
   * Event 0 forms the field, 1 collapses it, 2 is an impact and 3 is damage. Volumes offset each
   * sample's own loudness so the palettes sound equally loud.
   */
  private static Voice voice(int style, int event) {
    return switch (style) {
      case 1 -> switch (event) { // Crystal
        case 0 -> new Voice(SoundEvents.BELL_RESONATE, .18f, 1.6f);
        case 1 -> new Voice(SoundEvents.BELL_RESONATE, .25f, 1f);
        case 2 -> new Voice(SoundEvents.AMETHYST_BLOCK_HIT, .4f, 1.6f);
        default -> new Voice(SoundEvents.AMETHYST_BLOCK_BREAK, .16f, 1.6f);
      };
      case 2 -> event == 3 // Electric
          ? new Voice(SoundEvents.GENERIC_BURN, .16f, 1.6f)
          : new Voice(SoundEvents.RESPAWN_ANCHOR_CHARGE, .2f, event == 1 ? .85f : 1.6f);
      case 3 -> switch (event) { // Conduit
        case 0 -> new Voice(SoundEvents.CONDUIT_ACTIVATE, .2f, 1f);
        case 1 -> new Voice(SoundEvents.CONDUIT_DEACTIVATE, .1f, 1f);
        case 2 -> new Voice(SoundEvents.AMETHYST_BLOCK_RESONATE, .5f, 1.6f);
        default -> new Voice(SoundEvents.CONDUIT_ATTACK_TARGET, .35f, .9f);
      };
      default -> switch (event) { // Soft sizzle
        case 0 -> new Voice(SoundEvents.BEACON_ACTIVATE, .2f, 1.6f);
        case 1 -> new Voice(SoundEvents.BEACON_DEACTIVATE, .2f, .85f);
        case 2 -> new Voice(SoundEvents.AMETHYST_BLOCK_RESONATE, .2f, 1.6f);
        default -> new Voice(SoundEvents.FIRE_EXTINGUISH, .16f, 1.6f);
      };
    };
  }
}
