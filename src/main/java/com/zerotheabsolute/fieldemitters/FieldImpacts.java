package com.zerotheabsolute.fieldemitters;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

public final class FieldImpacts {
  public static final int LIFETIME = 40;
  private static final int MAX_ACTIVE = 256;
  private static final double MERGE_DISTANCE_SQUARED = .35 * .35;
  private static final int POSITION_SCALE = 256;
  public record Wave(Vec3 position, long time) {}

  private FieldImpacts() {}

  static void add(EmitterEntity emitter, List<Vec3> positions, long now) {
    emitter.impactWaves.removeIf(wave -> now - wave.time() >= LIFETIME);
    for (var position : positions) {
      boolean duplicate = emitter.impactWaves.stream().anyMatch(
          wave -> wave.time() == now && wave.position().distanceToSqr(position) < MERGE_DISTANCE_SQUARED);
      if (!duplicate) emitter.impactWaves.add(new Wave(position, now));
    }
    if (emitter.impactWaves.size() > MAX_ACTIVE)
      emitter.impactWaves.subList(0, emitter.impactWaves.size() - MAX_ACTIVE).clear();
    emitter.impact = positions.get(0);
    emitter.impactTime = now;
  }

  static void save(EmitterEntity emitter, CompoundTag tag) {
    var origin = Vec3.atLowerCornerOf(emitter.getBlockPos());
    int[] packed = new int[emitter.impactWaves.size() * 4];
    int i = 0;
    for (var wave : emitter.impactWaves) {
      var local = wave.position().subtract(origin);
      packed[i++] = (int) Math.round(local.x * POSITION_SCALE);
      packed[i++] = (int) Math.round(local.y * POSITION_SCALE);
      packed[i++] = (int) Math.round(local.z * POSITION_SCALE);
      packed[i++] = (int) (wave.time() - emitter.impactTime);
    }
    tag.putIntArray("ImpactWaves", packed);
  }

  static void load(EmitterEntity emitter, CompoundTag tag) {
    emitter.impactWaves.clear();
    var origin = Vec3.atLowerCornerOf(emitter.getBlockPos());
    int[] packed = tag.getIntArray("ImpactWaves");
    for (int i = 0; i + 3 < packed.length && i < MAX_ACTIVE * 4; i += 4) {
      var position = origin.add(packed[i] / (double) POSITION_SCALE,
          packed[i + 1] / (double) POSITION_SCALE, packed[i + 2] / (double) POSITION_SCALE);
      emitter.impactWaves.add(new Wave(position, emitter.impactTime + packed[i + 3]));
    }
  }
}
