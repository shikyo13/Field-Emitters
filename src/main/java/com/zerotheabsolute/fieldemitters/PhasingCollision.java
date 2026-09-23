package com.zerotheabsolute.fieldemitters;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Field collision that block shapes cannot provide: every field position for entities whose
 * movement skips block collision, and gap positions (see {@link FieldGaps}) for everyone else.
 */
public final class PhasingCollision {
  private PhasingCollision() {}

  public static Vec3 limit(Entity entity, Vec3 movement) {
    if (entity.isSpectator() || movement.lengthSqr() == 0) return movement;
    if (!entity.noPhysics) return FieldGaps.limit(entity, movement);
    Vec3 result = movement;
    for (var emitter : FieldNetwork.loaded(entity.level())) {
      if (!emitter.powered || emitter.isRemoved()) continue;
      result = clipAgainst(emitter, entity, result, emitter.cells);
      result = clipAgainst(emitter, entity, result, emitter.gaps);
      // Rail endpoints contain hardware instead of a field cell. Only the field stops phasing.
      var ends = new java.util.HashSet<BlockPos>();
      for (var link : emitter.links)
        if (link.rail()) {
          ends.add(emitter.getBlockPos());
          ends.add(link.target());
        }
      if (!ends.isEmpty()) result = clipAgainst(emitter, entity, result, ends);
    }
    return result;
  }

  /** Clips movement against one emitter's field plane at the given positions. */
  static Vec3 clipAgainst(EmitterEntity emitter, Entity entity, Vec3 movement, java.util.Set<BlockPos> positions) {
    if (positions.isEmpty()) return movement;
    var space = FieldSpace.at(emitter);
    var box = space.local(entity.getBoundingBox());
    var origin = space.local(entity.position());
    var localMovement = space.local(entity.position().add(movement)).subtract(origin);
    var swept = box.expandTowards(localMovement);
    if (!emitter.renderBounds().intersects(swept)) return movement;
    var shapes = new ArrayList<VoxelShape>();
    int minX = Mth.floor(swept.minX), minY = Mth.floor(swept.minY), minZ = Mth.floor(swept.minZ);
    int maxX = Mth.floor(swept.maxX), maxY = Mth.floor(swept.maxY), maxZ = Mth.floor(swept.maxZ);
    double volume = ((double) maxX - minX + 1) * ((double) maxY - minY + 1) * ((double) maxZ - minZ + 1);
    // Inspect nearby positions for normal movement; large moves remain bounded by the field's size.
    if (volume < positions.size()) {
      for (var cell : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ))
        if (positions.contains(cell)) add(emitter, entity, cell, box, swept, shapes);
    } else {
      for (var cell : positions) add(emitter, entity, cell, box, swept, shapes);
    }
    if (shapes.isEmpty()) return movement;
    var clipped = clip(box, localMovement, shapes);
    return clipped.equals(localMovement) ? movement : space.world(origin.add(clipped)).subtract(entity.position());
  }

  private static void add(EmitterEntity emitter, Entity entity, BlockPos cell, AABB box,
      AABB swept, ArrayList<VoxelShape> shapes) {
    if (!new AABB(cell).intersects(swept)) return;
    var shape = FieldBlock.collision(emitter, entity, cell, box.getCenter());
    if (!shape.isEmpty()) shapes.add(shape.move(cell.getX(), cell.getY(), cell.getZ()));
  }

  private static Vec3 clip(AABB box, Vec3 movement, ArrayList<VoxelShape> shapes) {
    double y = Shapes.collide(Direction.Axis.Y, box, shapes, movement.y);
    box = box.move(0, y, 0);
    boolean zFirst = Math.abs(movement.x) < Math.abs(movement.z);
    var first = zFirst ? Direction.Axis.Z : Direction.Axis.X;
    var second = zFirst ? Direction.Axis.X : Direction.Axis.Z;
    double a = Shapes.collide(first, box, shapes, zFirst ? movement.z : movement.x);
    box = box.move(zFirst ? 0 : a, 0, zFirst ? a : 0);
    double b = Shapes.collide(second, box, shapes, zFirst ? movement.x : movement.z);
    return new Vec3(zFirst ? b : a, y, zFirst ? a : b);
  }
}
