package com.zerotheabsolute.fieldemitters;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Field positions a cell cannot fill because something already stands there: an open gate or
 * door, a sign, carpet, snow, water. Movement is clipped against the field plane at these
 * positions, so the field has no holes while the block itself is left alone. The server finds
 * them while rebuilding and sends them with the emitter, so a player's own movement is stopped
 * without the server having to pull them back.
 */
public final class FieldGaps {
  private static final Map<Level, Map<BlockPos, Entry>> GAPS =
      Collections.synchronizedMap(new WeakHashMap<>());

  /** An emitter's gaps, their bounds for a quick distance check, and whether they move with Sable. */
  private record Entry(Set<BlockPos> positions, AABB bounds, boolean moving) {}

  private FieldGaps() {}

  /** Whether a field position needs gap collision: not this emitter's cell, and not already solid. */
  static boolean open(Level level, BlockPos pos, BlockState state, EmitterEntity e) {
    if (level.getBlockEntity(pos) instanceof FieldCell cell && cell.source.equals(e.getBlockPos())) return false;
    return !state.isCollisionShapeFullBlock(level, pos);
  }

  /** A colliding block that leaves open space inside its own block, like a carpet or slab. */
  static boolean partial(Level level, BlockPos pos) {
    var state = level.getBlockState(pos);
    return !state.is(FieldEmitters.EMITTER.get())
        && !state.getCollisionShape(level, pos).isEmpty()
        && !state.isCollisionShapeFullBlock(level, pos);
  }

  /** Server side: record a rebuild's gaps and tell clients when they change. */
  static void update(EmitterEntity e, Set<BlockPos> gaps) {
    if (gaps.equals(e.gaps)) return;
    set(e, gaps);
    e.sync();
  }

  static void set(EmitterEntity e, Set<BlockPos> gaps) {
    e.gaps = Set.copyOf(gaps);
    var level = e.getLevel();
    if (level == null) return;
    var map = GAPS.computeIfAbsent(level, k -> new ConcurrentHashMap<>());
    if (e.gaps.isEmpty()) {
      map.remove(e.getBlockPos());
      return;
    }
    AABB bounds = null;
    for (var p : e.gaps) bounds = bounds == null ? new AABB(p) : bounds.minmax(new AABB(p));
    map.put(e.getBlockPos().immutable(), new Entry(e.gaps, bounds, !FieldSpace.at(e).isWorld()));
  }

  static void remove(EmitterEntity e) {
    var level = e.getLevel();
    var map = level == null ? null : GAPS.get(level);
    if (map != null) map.remove(e.getBlockPos());
  }

  static void save(EmitterEntity e, CompoundTag tag) {
    if (!e.gaps.isEmpty()) tag.putLongArray("Gaps", e.gaps.stream().mapToLong(BlockPos::asLong).toArray());
  }

  static Set<BlockPos> read(CompoundTag tag) {
    var result = new HashSet<BlockPos>();
    for (long p : tag.getLongArray("Gaps")) result.add(BlockPos.of(p));
    return result;
  }

  /** Clips movement at every emitter's gaps; returns at once when no field has any. */
  static Vec3 limit(Entity entity, Vec3 movement) {
    var map = GAPS.get(entity.level());
    if (map == null || map.isEmpty()) return movement;
    var near = entity.getBoundingBox().expandTowards(movement).inflate(1);
    Vec3 result = movement;
    for (var entry : map.entrySet()) {
      var gaps = entry.getValue();
      if (!gaps.moving() && !gaps.bounds().intersects(near)) continue;
      if (!entity.level().hasChunkAt(entry.getKey())
          || !(entity.level().getBlockEntity(entry.getKey()) instanceof EmitterEntity emitter)
          || !emitter.powered || emitter.isRemoved()) continue;
      result = PhasingCollision.clipAgainst(emitter, entity, result, gaps.positions());
    }
    return result;
  }
}
