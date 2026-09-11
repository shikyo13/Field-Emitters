package com.zerotheabsolute.fieldemitters;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;

/** A passage completes only after the entire entity clears the far side of the plane. */
public final class FieldSensor {
  public static void tick(ServerLevel level, EmitterEntity e, long now) {
    boolean present = false;
    boolean monitoring = !e.isRail() || e.root.equals(e.getBlockPos());
    var detected = new java.util.HashSet<String>();
    if (e.powered && monitoring && e.controls.sensorMode != 0) {
      for (var origin : e.isRail() ? FieldNetwork.connected(e) : java.util.List.of(e)) {
        var p = origin.getBlockPos();
        for (var link : origin.links) {
          var settings = origin.settings(link);
          var t = link.target();
          var area = link.box(p).inflate(3);
          for (var entity :
              level.getEntities(
                  (net.minecraft.world.entity.Entity) null,
                  area,
                  a -> settings.sensor.matches(a, e.owner))) {
            var position =
                entity
                    .position()
                    .add(0, link.normal() == Direction.Axis.Y ? entity.getBbHeight() / 2 : 0, 0);
            double u =
                (entity.getX() - p.getX() - .5) * link.dx()
                    + (entity.getZ() - p.getZ() - .5) * link.dz()
                    + (entity.getY() - p.getY() - .5) * link.dy();
            int i = (int) Math.floor(u + .5);
            if (i < (link.rail() ? 0 : 1)
                || i > (link.rail() ? link.length() : link.length() - 1)
                || now - e.transition < i * 2) continue;
            if (!entity.getBoundingBox().intersects(link.box(p))) {
              // Keep observations on both sides, but only within the projected tile's other axes.
              var box = entity.getBoundingBox();
              var tile = link.box(p);
              if (link.normal() != Direction.Axis.X
                      && (box.maxX <= tile.minX || box.minX >= tile.maxX)
                  || link.normal() != Direction.Axis.Y
                      && (box.maxY <= tile.minY || box.minY >= tile.maxY)
                  || link.normal() != Direction.Axis.Z
                      && (box.maxZ <= tile.minZ || box.minZ >= tile.maxZ)) continue;
            }
            if (!link.rail()
                && (entity.getY() >= link.ground()[i] + 5
                    || entity.getY() + entity.getBbHeight() <= link.ground()[i])) continue;
            double normal =
                link.normalCoordinate(position)
                    - link.normalCoordinate(net.minecraft.world.phys.Vec3.atCenterOf(p));
            double margin =
                (link.normal() == Direction.Axis.Y ? entity.getBbHeight() : entity.getBbWidth()) / 2
                    + .1;
            int side = normal < -margin ? -1 : normal > margin ? 1 : 0;

            String key = link.target().asLong() + ":" + entity.getUUID();
            var old = e.passages.get(key);
            present |=
                side == 0
                    && (settings.sensor.directions == 63
                        || old != null && settings.sensor.direction(link.movement(old.side() < 0)));
            if (side == 0) {
              if (old != null)
                e.passages.put(key, new EmitterEntity.Passage(old.side(), old.position(), now));
              continue;
            }
            if (old != null && old.side() != side && now - old.time() <= 2) {
              var direction = link.movement(side > 0);
              // Reject shortcuts around an end or across a terrain discontinuity.
              double previousNormal =
                  link.normalCoordinate(old.position())
                      - link.normalCoordinate(net.minecraft.world.phys.Vec3.atCenterOf(p));
              double fraction = -previousNormal / (normal - previousNormal);
              var intersection = old.position().lerp(position, fraction);
              double crossingU =
                  (intersection.x - p.getX() - .5) * link.dx()
                      + (intersection.z - p.getZ() - .5) * link.dz()
                      + (intersection.y - p.getY() - .5) * link.dy();
              int ci = (int) Math.floor(crossingU + .5);
              if (ci >= (link.rail() ? 0 : 1)
                  && ci <= (link.rail() ? link.length() : link.length() - 1)
                  && (link.rail()
                      || intersection.y < link.ground()[ci] + 5
                          && intersection.y + entity.getBbHeight() > link.ground()[ci])
                  && settings.sensor.direction(direction)
                  && detected.add(
                      entity.getUUID()
                          + ":"
                          + link.normal()
                          + ":"
                          + link.normalCoordinate(net.minecraft.world.phys.Vec3.atCenterOf(p))
                          + ":"
                          + direction)) {
                int count =
                    e.controls.countItems && entity instanceof ItemEntity item
                        ? item.getItem().getCount()
                        : 1;
                e.crossings += count;
                if (e.controls.sensorMode == 1)
                  e.queuedPulses = Math.min(100000, e.queuedPulses + count);
                e.lastDetection = entity.getName().getString() + " → " + direction.getName();
                e.sync();
              }
            }
            e.passages.put(key, new EmitterEntity.Passage(side, position, now));
          }
        }
      }
      e.passages.entrySet().removeIf(a -> now - a.getValue().time() > 2);
    } else {
      e.passages.clear();
      e.queuedPulses = 0;
      e.pulseUntil = 0;
    }
    int signal = 0;
    if (e.powered && monitoring && e.controls.sensorMode == 2) signal = present ? 15 : 0;
    if (e.powered && monitoring && e.controls.sensorMode == 1) {
      if (now < e.pulseUntil) signal = 15;
      else if (e.outputSignal != 0) e.gapUntil = now + 2;
      else if (e.queuedPulses > 0 && now >= e.gapUntil) {
        e.queuedPulses--;
        e.pulseUntil = now + e.controls.pulseTicks;
        signal = 15;
      }
    }
    if (signal != e.outputSignal) {
      e.outputSignal = signal;
      level.updateNeighborsAt(e.getBlockPos(), e.getBlockState().getBlock());
      e.sync();
    }
  }
}
