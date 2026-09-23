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
  public static final int DOME_DEPTH = 4;
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
            for (int y = dome ? -DOME_DEPTH : -radius; y < radius; y++)
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
    var gaps = new HashSet<BlockPos>();
    int parkedPlants = 0;
    if (e.enabled && loaded(e)
        && (e.powered || level.getGameTime() - e.transition < FieldShutdown.DURATION_TICKS))
      for (var offset : shell(e.controls.sphereRadius, e.controls.dome)) {
        var p = e.getBlockPos().offset(offset);
        if (p.getY() < level.getMinBuildHeight()) continue;
        var state = level.getBlockState(p);
        FieldVegetation.Parked parked = null;
        if (e.powered && FieldVegetation.canPark(level, p, state)) {
          parked = FieldVegetation.lift(level, p, state,
              parkedPlants < FieldVegetation.EFFECTS_PER_REBUILD);
          if (parkedPlants++ == 0)
            FieldSounds.play(level, e, FieldSpace.at(e).world(Vec3.atCenterOf(p)), 3);
          state = level.getBlockState(p);
        }
        if (state.isAir()) {
          level.setBlock(p, FieldEmitters.FIELD.get().defaultBlockState(),
              parked == null ? 3 : FieldVegetation.QUIET);
          if (level.getBlockEntity(p) instanceof FieldCell cell) {
            cell.source = e.getBlockPos();
            cell.parked = parked;
            cell.setChanged();
            level.sendBlockUpdated(p, level.getBlockState(p), level.getBlockState(p), 3);
            level.scheduleTick(p, FieldEmitters.FIELD.get(), 40);
          }
        }
        if (level.getBlockEntity(p) instanceof FieldCell cell
            && cell.source.equals(e.getBlockPos())) next.add(p);
        else if (FieldGaps.open(level, p, level.getBlockState(p), e)) gaps.add(p);
      }
    var released = new ArrayList<BlockPos>();
    for (var p : e.cells)
      if (!next.contains(p)
          && level.hasChunkAt(p)
          && level.getBlockEntity(p) instanceof FieldCell cell
          && cell.source.equals(e.getBlockPos())) released.add(p);
    FieldVegetation.release(level, released);
    e.cells = next;
    FieldGaps.update(e, gaps);
    long area =
        Math.round(
            (e.controls.dome ? 2 : 4)
                * Math.PI
                * e.controls.sphereRadius
                * e.controls.sphereRadius);
    if (e.controls.dome) area += Math.round(2 * Math.PI * e.controls.sphereRadius * DOME_DEPTH);
    e.demand = (int) Math.min(Integer.MAX_VALUE, area * FieldConfig.energyPerCell());
  }

  public static Direction movement(EmitterEntity e, Entity entity) {
    return movement(e, entity, FieldSpace.at(e).local(entity.getBoundingBox().getCenter()));
  }

  private static Direction movement(EmitterEntity e, Entity entity, Vec3 point) {
    var radial = point.subtract(center(e));
    boolean inside =
        FieldContact.negative(
            e,
            e.getBlockPos(),
            entity,
            radial.length() - e.controls.sphereRadius,
            contactMargin(entity, radial.normalize()));
    var travel = inside ? radial : radial.scale(-1);
    return Direction.getNearest(travel.x, travel.y, travel.z);
  }

  public static VoxelShape collision(EmitterEntity e, Entity entity, BlockPos p, Vec3 center) {
    if (!formed(e) || e.controls.dome && p.getY() < e.getBlockPos().getY() - DOME_DEPTH)
      return Shapes.empty();
    var direction = movement(e, entity, center);
    if (!FieldBlock.blocked(e, e.controls, entity, direction)) return Shapes.empty();
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
    var space = FieldSpace.at(e);
    int radius = e.controls.sphereRadius;
    var bounds = new AABB(center, center).inflate(radius + 2);
    var impacts =
        now - e.impactTime >= ImpactSelection.EFFECT_INTERVAL
            ? new ImpactSelection<Vec3>(e.contacts, now)
            : null;
    for (var entity :
        level.getEntities((Entity) null, bounds, a -> a.isAlive() && !a.isSpectator())) {
      var point = space.local(entity.getBoundingBox().getCenter());
      if (e.controls.dome && point.y < center.y - DOME_DEPTH) continue;
      double side = point.distanceTo(center) - radius;
      double margin = contactMargin(entity, point.subtract(center).normalize());
      int sign = side < -margin ? -1 : side > margin ? 1 : 0;
      var old = e.spherePassages.get(entity.getUUID());
      var direction = movement(e, entity, point);
      if (sign != 0) {
        if (e.controls.sensorMode != 0
            && old != null
            && old.side() != sign
            && now - old.time() <= 2
            && e.controls.detects(entity, e.owner, direction)) {
          e.crossings++;
          FieldAutomation.emit("crossing", e, entity, direction, 1);
          if (e.controls.sensorMode == 1)
            FieldSensor.pulse(e, now);
          e.lastDetection =
              net.minecraft.network.chat.Component.translatable(
                  "message.fieldemitters.detection.crossing",
                  entity.getName(),
                  net.minecraft.network.chat.Component.translatable(
                      sign < 0
                          ? "direction.fieldemitters.inside"
                          : "direction.fieldemitters.outside"));
          e.sync();
        }
        e.spherePassages.put(entity.getUUID(), new EmitterEntity.Passage(sign, point, now));
      } else {
        if (old != null)
          e.spherePassages.put(
              entity.getUUID(), new EmitterEntity.Passage(old.side(), old.position(), now));
        if (impacts != null)
          impacts.consider(
              entity.getUUID(), center.add(point.subtract(center).normalize().scale(radius)));
        if (e.controls.sensorMode == 2 && e.controls.detects(entity, e.owner, direction))
          e.spherePresent = true;
        FieldDamage.contact(level, e, entity, e.controls, direction, now);
        if (entity instanceof Player player) {
          var outward = point.subtract(center).normalize();
          var entry =
              space.world(
                  space
                      .local(player.position())
                      .add(outward.scale((side >= 0 ? 1 : -1) * (margin + .5))));
          FieldCheckpoint.process(level, e, e.controls, player, direction, entry, now);
        }
      }
    }
    if (impacts != null) {
      var hits = impacts.finish();
      if (!hits.isEmpty()) {
        FieldImpacts.add(e, hits.stream().map(ImpactSelection.Choice::value).toList(), now);
        e.sync();
        FieldSounds.play(level, e, e.impact, 2);
      }
    }
    e.spherePassages.entrySet().removeIf(entry -> now - entry.getValue().time() > 2);
  }
}
