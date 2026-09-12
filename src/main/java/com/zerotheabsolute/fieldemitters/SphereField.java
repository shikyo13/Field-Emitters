package com.zerotheabsolute.fieldemitters;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class SphereField {
  public static final int MIN_RADIUS = 8, MAX_RADIUS = 24, DEFAULT_RADIUS = 12;
  public static final int FORMATION_TICKS = 80;
  private static final double SHELL_THICKNESS = .18;
  private static final int COLLISION_RESOLUTION = 4;
  private static final Map<String, List<BlockPos>> SHELLS = new HashMap<>();
  private static final Map<String, Map<BlockPos, VoxelShape>> SHAPES = new HashMap<>();

  private SphereField() {}

  public static Vec3 center(EmitterEntity e) {
    return Vec3.atLowerCornerOf(e.getBlockPos()).add(.5, 0, .5);
  }

  public static boolean formed(EmitterEntity e) {
    return e.powered && e.getLevel().getGameTime() - e.transition >= FORMATION_TICKS;
  }

  private static String key(EmitterEntity e) {
    return e.controls.sphereRadius + ":" + e.controls.dome;
  }

  public static List<BlockPos> shell(int radius, boolean dome) {
    return SHELLS.computeIfAbsent(
        radius + ":" + dome,
        ignored -> {
          var result = new ArrayList<BlockPos>();
          for (int x = -radius; x <= radius; x++)
            for (int y = dome ? 0 : -radius; y < radius; y++)
              for (int z = -radius; z <= radius; z++) {
                double distance = Math.sqrt(x * x + (y + .5) * (y + .5) + z * z);
                if (Math.abs(distance - radius) < 1) result.add(new BlockPos(x, y, z));
              }
          return List.copyOf(result);
        });
  }

  public static boolean fitsHeight(EmitterEntity e) {
    int radius = e.controls.sphereRadius;
    return e.getBlockPos().getY() + radius <= e.getLevel().getMaxBuildHeight()
        && (e.controls.dome || e.getBlockPos().getY() - radius >= e.getLevel().getMinBuildHeight());
  }

  public static boolean loaded(EmitterEntity e) {
    var level = e.getLevel();
    int radius = e.controls.sphereRadius;
    if (!fitsHeight(e)) return false;
    for (int x = -radius; x <= radius; x += 16)
      for (int z = -radius; z <= radius; z += 16)
        if (!level.hasChunkAt(e.getBlockPos().offset(x, 0, z))) return false;
    for (int x = -radius; x <= radius; x += 16)
      if (!level.hasChunkAt(e.getBlockPos().offset(x, 0, radius))) return false;
    for (int z = -radius; z <= radius; z += 16)
      if (!level.hasChunkAt(e.getBlockPos().offset(radius, 0, z))) return false;
    return level.hasChunkAt(e.getBlockPos().offset(radius, 0, radius));
  }

  public static void rebuild(ServerLevel level, EmitterEntity e) {
    var next = new HashSet<BlockPos>();
    if (e.enabled && loaded(e))
      for (var offset : shell(e.controls.sphereRadius, e.controls.dome)) {
        var p = e.getBlockPos().offset(offset);
        var state = level.getBlockState(p);
        if (state.isAir()) {
          level.setBlock(p, FieldEmitters.FIELD.get().defaultBlockState(), 3);
          if (level.getBlockEntity(p) instanceof FieldCell cell) {
            cell.source = e.getBlockPos();
            cell.setChanged();
            level.sendBlockUpdated(p, level.getBlockState(p), level.getBlockState(p), 3);
            level.scheduleTick(p, FieldEmitters.FIELD.get(), 40);
          }
        }
        if (level.getBlockEntity(p) instanceof FieldCell cell
            && cell.source.equals(e.getBlockPos())) next.add(p);
      }
    for (var p : e.cells)
      if (!next.contains(p)
          && level.hasChunkAt(p)
          && level.getBlockEntity(p) instanceof FieldCell cell
          && cell.source.equals(e.getBlockPos())) level.removeBlock(p, false);
    e.cells = next;
    long area =
        Math.round(
            (e.controls.dome ? 2 : 4)
                * Math.PI
                * e.controls.sphereRadius
                * e.controls.sphereRadius);
    e.demand = (int) Math.min(Integer.MAX_VALUE, area * FieldConfig.energyPerCell());
  }

  public static Direction movement(EmitterEntity e, Entity entity) {
    var point = entity.getBoundingBox().getCenter();
    var previous =
        point.add(entity.xo - entity.getX(), entity.yo - entity.getY(), entity.zo - entity.getZ());
    var radial = point.subtract(center(e));
    var travel =
        previous.distanceTo(center(e)) >= e.controls.sphereRadius ? radial.scale(-1) : radial;
    return Direction.getNearest(travel.x, travel.y, travel.z);
  }

  public static VoxelShape collision(EmitterEntity e, Entity entity, BlockPos p) {
    if (!formed(e) || e.controls.dome && p.getY() < e.getBlockPos().getY()) return Shapes.empty();
    var direction = movement(e, entity);
    if (!e.controls.blocks(entity, e.owner, direction)
        && !FieldCheckpoint.blocks(e, e.controls, entity, direction)) return Shapes.empty();
    var offset = p.subtract(e.getBlockPos());
    return SHAPES
        .computeIfAbsent(key(e), ignored -> new HashMap<>())
        .computeIfAbsent(
            offset,
            local -> {
              var shape = Shapes.empty();
              final int resolution = COLLISION_RESOLUTION;
              double step = 1.0 / resolution;
              for (int x = 0; x < resolution; x++)
                for (int y = 0; y < resolution; y++)
                  for (int z = 0; z < resolution; z++) {
                    double px = local.getX() + (x + .5) * step - .5,
                        py = local.getY() + (y + .5) * step,
                        pz = local.getZ() + (z + .5) * step - .5;
                    if (Math.abs(Math.sqrt(px * px + py * py + pz * pz) - e.controls.sphereRadius)
                        <= SHELL_THICKNESS)
                      shape =
                          Shapes.or(
                              shape,
                              Shapes.box(
                                  x * step,
                                  y * step,
                                  z * step,
                                  (x + 1) * step,
                                  (y + 1) * step,
                                  (z + 1) * step));
                  }
              return shape;
            });
  }

  static double contactMargin(Entity entity, Vec3 normal) {
    var bounds = entity.getBoundingBox();
    double extent =
        (Math.abs(normal.x) * bounds.getXsize()
                + Math.abs(normal.y) * bounds.getYsize()
                + Math.abs(normal.z) * bounds.getZsize())
            * .5;
    // Voxel collision rounds the mathematical shell out to quarter-block cells.
    return extent + SHELL_THICKNESS + 1.0 / COLLISION_RESOLUTION;
  }

  public static void tick(ServerLevel level, EmitterEntity e, long now) {
    if (!formed(e)) {
      e.spherePassages.clear();
      return;
    }
    var center = center(e);
    int radius = e.controls.sphereRadius;
    var bounds = new AABB(center, center).inflate(radius + 2);
    for (var entity :
        level.getEntities((Entity) null, bounds, a -> a.isAlive() && !a.isSpectator())) {
      var point = entity.getBoundingBox().getCenter();
      if (e.controls.dome && point.y < center.y) continue;
      double side = point.distanceTo(center) - radius;
      double margin = contactMargin(entity, point.subtract(center).normalize());
      int sign = side < -margin ? -1 : side > margin ? 1 : 0;
      var old = e.spherePassages.get(entity.getUUID());
      var direction = movement(e, entity);
      if (sign != 0) {
        if (e.controls.sensorMode != 0
            && old != null
            && old.side() != sign
            && now - old.time() <= 2
            && e.controls.detects(entity, e.owner, direction)) {
          e.crossings++;
          if (e.controls.sensorMode == 1) e.queuedPulses = Math.min(100000, e.queuedPulses + 1);
          e.lastDetection =
              entity.getName().getString() + " → " + (sign < 0 ? "inside" : "outside");
          e.sync();
        }
        e.spherePassages.put(entity.getUUID(), new EmitterEntity.Passage(sign, point, now));
      } else {
        if (old != null)
          e.spherePassages.put(
              entity.getUUID(), new EmitterEntity.Passage(old.side(), old.position(), now));
        if (now - e.impactTime >= 8) {
          e.impact = center.add(point.subtract(center).normalize().scale(radius));
          e.impactTime = now;
          e.sync();
          FieldSounds.play(level, e, e.impact, 2);
        }
        if (e.controls.sensorMode == 2 && e.controls.detects(entity, e.owner, direction))
          e.spherePresent = true;
        FieldDamage.contact(level, e, entity, e.controls, direction, now);
        if (entity instanceof Player player) {
          var outward = point.subtract(center).normalize();
          var entry = player.position().add(outward.scale((side >= 0 ? 1 : -1) * (margin + .5)));
          FieldCheckpoint.process(level, e, e.controls, player, direction, entry, now);
        }
      }
    }
    e.spherePassages.entrySet().removeIf(entry -> now - entry.getValue().time() > 2);
  }
}
