package com.zerotheabsolute.fieldemitters;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class FieldNetwork {
  private static final Map<Level, Set<BlockPos>> KNOWN = new WeakHashMap<>();

  public static void add(EmitterEntity e) {
    KNOWN.computeIfAbsent(e.getLevel(), k -> new HashSet<>()).add(e.getBlockPos());
  }

  public static List<EmitterEntity> connected(EmitterEntity seed) {
    return connected(seed, false);
  }

  public static List<EmitterEntity> configurable(EmitterEntity seed) {
    return connected(seed, true);
  }

  private static List<EmitterEntity> connected(EmitterEntity seed, boolean includeDisabled) {
    if (seed.getLevel() == null) return List.of(seed);
    List<EmitterEntity> all = loaded(seed.getLevel());
    List<EmitterEntity> found = new ArrayList<>();
    Set<BlockPos> seen = new HashSet<>();
    ArrayDeque<EmitterEntity> q = new ArrayDeque<>();
    q.add(seed);
    while (!q.isEmpty()) {
      var e = q.remove();
      if (!seen.add(e.getBlockPos())) continue;
      found.add(e);
      q.addAll(neighbors(e, all, includeDisabled));
    }
    return found;
  }

  public static List<EmitterEntity> loaded(Level l) {
    List<EmitterEntity> result = new ArrayList<>();
    var set = KNOWN.get(l);
    if (set == null) return result;
    set.removeIf(p -> !l.hasChunkAt(p) || !(l.getBlockEntity(p) instanceof EmitterEntity));
    for (var p : set) if (l.getBlockEntity(p) instanceof EmitterEntity e) result.add(e);
    return result;
  }

  private static List<EmitterEntity> neighbors(EmitterEntity a, List<EmitterEntity> all) {
    return neighbors(a, all, false);
  }

  private static List<EmitterEntity> neighbors(
      EmitterEntity a, List<EmitterEntity> all, boolean includeDisabled) {
    if (a.isRail()) return railNeighbors(a, all, includeDisabled);
    List<EmitterEntity> found = new ArrayList<>();
    for (Direction d : Direction.Plane.HORIZONTAL) {
      EmitterEntity best = null;
      int distance = 21;
      for (var b : all) {
        if (b.isRail()) continue;
        int x = b.getBlockPos().getX() - a.getBlockPos().getX(),
            z = b.getBlockPos().getZ() - a.getBlockPos().getZ();
        int n = x * d.getStepX() + z * d.getStepZ();
        if (n > 0
            && n < distance
            && x == n * d.getStepX()
            && z == n * d.getStepZ()
            && Math.abs(b.getBlockPos().getY() - a.getBlockPos().getY()) <= 8) {
          best = b;
          distance = n;
        }
      }
      if (best != null && (includeDisabled || best.enabled && a.enabled) && trace(a, best) != null)
        found.add(best);
    }
    return found;
  }

  private static boolean ground(Level l, BlockPos p) {
    if (!l.hasChunkAt(p)) return false;
    var s = l.getBlockState(p);
    return !s.is(FieldEmitters.EMITTER.get())
        && !s.is(FieldEmitters.FIELD.get())
        && !(s.getBlock() instanceof LeavesBlock)
        && !s.getCollisionShape(l, p).isEmpty();
  }

  private static EmitterEntity.Link trace(EmitterEntity a, EmitterEntity b) {
    var p = a.getBlockPos();
    var t = b.getBlockPos();
    if (a.isRail()) {
      var face = a.getBlockState().getValue(RailBlock.FACING);
      int n =
          Math.abs(t.getX() - p.getX())
              + Math.abs(t.getY() - p.getY())
              + Math.abs(t.getZ() - p.getZ());
      if (b.getBlockState().getValue(RailBlock.FACING) != face.getOpposite()) return null;
      int[] heights = new int[n + 1];
      for (int i = 0; i <= n; i++) heights[i] = p.getY() + face.getStepY() * i;
      var normal = Direction.Axis.values()[a.controls.railNormal];
      if (normal == face.getAxis())
        normal = face.getAxis() == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z;
      return new EmitterEntity.Link(
          t, face.getStepX(), face.getStepZ(), heights, face.getStepY(), normal, true);
    }
    int dx = Integer.signum(t.getX() - p.getX()),
        dz = Integer.signum(t.getZ() - p.getZ()),
        n = Math.abs(t.getX() - p.getX()) + Math.abs(t.getZ() - p.getZ());
    int[] heights = new int[n + 1];
    heights[0] = p.getY();
    heights[n] = t.getY();
    int y = p.getY();
    for (int i = 1; i < n; i++) {
      boolean found = false;
      for (int offset = 4; offset >= -4; offset--) {
        var floor = new BlockPos(p.getX() + dx * i, y + offset - 1, p.getZ() + dz * i);
        if (ground(a.getLevel(), floor) && !ground(a.getLevel(), floor.above())) {
          y = floor.getY() + 1;
          found = true;
          break;
        }
      }
      if (!found) return null;
      heights[i] = y;
    }
    if (Math.abs(y - t.getY()) > 4) return null;
    return new EmitterEntity.Link(t, dx, dz, heights);
  }

  public static void tick(LevelTickEvent.Post event) {
    if (!(event.getLevel() instanceof ServerLevel l)) return;
    List<EmitterEntity> all = loaded(l);
    if (all.isEmpty()) return;
    long now = l.getGameTime();
    for (var e : all) {
      if (e.powered) {
        if (e.isRail()) railImpacts(l, e, now);
        else impacts(l, e, now);
      }
      FieldSensor.tick(l, e, now);
      if (now % 20 == 0) e.sync();
      if (e.powered && now - e.transition <= 42 && now % 2 == 0) updateLights(l, e, true);
    }
    if (now % 10 == 0)
      rebuild(
          l, all,
          now); // Energy consumption is per tick, topology work is bounded to twice per second.
    Set<BlockPos> seen = new HashSet<>();
    for (var seed : all) {
      if (seen.contains(seed.getBlockPos())) continue;
      var network = connected(seed);
      network.forEach(e -> seen.add(e.getBlockPos()));
      boolean redstone = network.stream().anyMatch(e -> e.enabled && input(l, e));
      int demand = network.stream().mapToInt(e -> e.demand).sum();
      int stored =
          network.stream()
              .filter(e -> e.enabled)
              .mapToInt(e -> e.energy.extractEnergy(Integer.MAX_VALUE, true))
              .sum();
      boolean demo = FieldConfig.DEMO_POWER.get() && redstone;
      boolean allowed =
          network.stream()
              .allMatch(
                  e -> e.controls.inputMode == 0 || (input(l, e) == (e.controls.inputMode == 1)));
      boolean on = demand > 0 && allowed && (demo || stored >= demand);
      if (on && !demo) {
        int remaining = demand;
        for (var e : network)
          if (e.enabled) {
            remaining -= e.energy.extractEnergy(remaining, false);
            if (remaining == 0) break;
          }
      }
      for (var e : network) {
        boolean active = on && e.enabled;
        if (active != e.powered) {
          e.powered = active;
          e.transition = now;
          e.sync();
          updateLights(l, e, active);
          l.playSound(
              null,
              e.getBlockPos(),
              active ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
              SoundSource.BLOCKS,
              .35f,
              active ? 1.6f : .8f);
        }
      }
    }
  }

  private static void rebuild(ServerLevel l, List<EmitterEntity> all, long now) {
    Map<BlockPos, Integer> rank = new HashMap<>();
    for (var seed : all) {
      if (rank.containsKey(seed.getBlockPos())) continue;
      var network = connected(seed);
      var source =
          network.stream()
              .filter(
                  e ->
                      e.enabled
                          && (l.hasNeighborSignal(e.getBlockPos())
                              || e.energy.getEnergyStored() > 0))
              .min(java.util.Comparator.comparingLong(e -> e.getBlockPos().asLong()))
              .orElseGet(
                  () ->
                      network.stream()
                          .filter(e -> e.getBlockPos().equals(seed.root))
                          .findFirst()
                          .orElse(seed));
      var queue = new ArrayDeque<EmitterEntity>();
      queue.add(source);
      rank.put(source.getBlockPos(), 0);
      while (!queue.isEmpty()) {
        var current = queue.remove();
        for (var next : neighbors(current, all))
          if (!rank.containsKey(next.getBlockPos())) {
            rank.put(next.getBlockPos(), rank.get(current.getBlockPos()) + 1);
            queue.add(next);
          }
      }
      for (var e : network) e.root = source.getBlockPos();
    }
    for (var e : all) {
      List<EmitterEntity.Link> links = new ArrayList<>();
      if (e.enabled)
        for (var other : neighbors(e, all)) {
          if (other.enabled
              && (rank.getOrDefault(e.getBlockPos(), 0) < rank.getOrDefault(other.getBlockPos(), 0)
                  || (rank.getOrDefault(e.getBlockPos(), 0)
                          .equals(rank.getOrDefault(other.getBlockPos(), 0))
                      && e.getBlockPos().asLong() < other.getBlockPos().asLong()))) {
            var link = trace(e, other);
            if (link != null) links.add(link);
          }
        }
      String before = signature(e.links);
      e.links = links;
      e.demand =
          links.stream()
              .mapToInt(link -> (link.length() + (link.rail() ? 1 : -1)) * link.height() * 2)
              .sum();
      Set<BlockPos> next = new HashSet<>();
      for (var link : links)
        for (int i = 1; i < link.length(); i++)
          for (int h = 0; h < link.height(); h++) {
            BlockPos p =
                new BlockPos(
                    e.getBlockPos().getX() + link.dx() * i,
                    link.ground()[i] + h,
                    e.getBlockPos().getZ() + link.dz() * i);
            if (!l.hasChunkAt(p)) continue;
            var state = l.getBlockState(p);
            if (state.isAir()) {
              l.setBlock(
                  p,
                  FieldEmitters.FIELD
                      .get()
                      .defaultBlockState()
                      .setValue(FieldBlock.X_AXIS, link.dx() != 0)
                      .setValue(FieldBlock.LIT, e.powered && e.controls.light),
                  3);
              if (l.getBlockEntity(p) instanceof FieldCell cell) {
                cell.source = e.getBlockPos();
                cell.setChanged();
                l.sendBlockUpdated(p, l.getBlockState(p), l.getBlockState(p), 3);
                l.scheduleTick(p, FieldEmitters.FIELD.get(), 40);
              }
            }
            if (l.getBlockEntity(p) instanceof FieldCell cell
                && cell.source.equals(e.getBlockPos())) next.add(p);
          }
      for (var old : e.cells)
        if (!next.contains(old)
            && l.hasChunkAt(old)
            && l.getBlockEntity(old) instanceof FieldCell cell
            && cell.source.equals(e.getBlockPos())) l.removeBlock(old, false);
      e.cells = next;
      updateLights(l, e, e.powered);
      if (!before.equals(signature(links))) e.sync();
    }
  }

  private static void updateLights(ServerLevel l, EmitterEntity e, boolean active) {
    if (e.isRail()) {
      var state = e.getBlockState();
      l.setBlock(
          e.getBlockPos(),
          state
              .setValue(RailBlock.ACTIVE, active)
              .setValue(RailBlock.LIGHT, active && e.controls.light),
          3);
    }
    for (int i = 0; i < 5; i++) {
      var p = e.getBlockPos().above(i);
      var state = l.getBlockState(p);
      if (state.is(FieldEmitters.EMITTER.get()))
        l.setBlock(
            p,
            state
                .setValue(EmitterBlock.ACTIVE, active)
                .setValue(EmitterBlock.LIGHT, active && e.controls.light),
            3);
    }
    for (var p : e.cells) {
      var state = l.getBlockState(p);
      if (state.is(FieldEmitters.FIELD.get())) {
        int distance =
            Math.abs(p.getX() - e.getBlockPos().getX())
                + Math.abs(p.getZ() - e.getBlockPos().getZ());
        boolean lit = active && e.controls.light && l.getGameTime() - e.transition >= distance * 2;
        l.setBlock(p, state.setValue(FieldBlock.LIT, lit), 3);
      }
    }
  }

  private static void railImpacts(ServerLevel level, EmitterEntity emitter, long now) {
    if (now - emitter.impactTime < 8) return;
    emitter.contacts.entrySet().removeIf(entry -> now - entry.getValue() > 60);
    var origin = net.minecraft.world.phys.Vec3.atCenterOf(emitter.getBlockPos());
    for (var link : emitter.links) {
      var bounds = link.box(emitter.getBlockPos());
      for (var entity :
          level.getEntities((net.minecraft.world.entity.Entity) null, bounds.inflate(.2))) {
        if (now - emitter.contacts.getOrDefault(entity.getUUID(), -1000L) < 25) continue;
        var center = entity.getBoundingBox().getCenter();
        double u =
            center
                .subtract(origin)
                .dot(new net.minecraft.world.phys.Vec3(link.dx(), link.dy(), link.dz()));
        int index = Math.max(0, Math.min(link.length(), (int) Math.floor(u + .5)));
        var cell = link.cell(emitter.getBlockPos(), index, 0);
        var shape = FieldBlock.collision(emitter, entity, cell);
        if (shape.isEmpty()
            || !shape.bounds().move(cell).inflate(.06).intersects(entity.getBoundingBox()))
          continue;
        var impact =
            new net.minecraft.world.phys.Vec3(
                Math.max(bounds.minX, Math.min(bounds.maxX, center.x)),
                Math.max(bounds.minY, Math.min(bounds.maxY, center.y)),
                Math.max(bounds.minZ, Math.min(bounds.maxZ, center.z)));
        impact =
            switch (link.normal()) {
              case X -> new net.minecraft.world.phys.Vec3(origin.x, impact.y, impact.z);
              case Y -> new net.minecraft.world.phys.Vec3(impact.x, origin.y, impact.z);
              case Z -> new net.minecraft.world.phys.Vec3(impact.x, impact.y, origin.z);
            };
        // One timestamp and hit location for the connected surface, not one hit per rail.
        for (var part : connected(emitter)) {
          if (!part.powered || !part.isRail()) continue;
          if (Math.abs(
                  link.normalCoordinate(
                          net.minecraft.world.phys.Vec3.atCenterOf(part.getBlockPos()))
                      - link.normalCoordinate(origin))
              > .01) continue;
          if (!part.links.isEmpty()
              && part.links.stream().noneMatch(other -> other.normal() == link.normal())) continue;
          part.impact = impact;
          part.impactTime = now;
          part.contacts.put(entity.getUUID(), now);
          part.sync();
        }
        level.playSound(
            null,
            impact.x,
            impact.y,
            impact.z,
            net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,
            net.minecraft.sounds.SoundSource.BLOCKS,
            .20f,
            1.7f);
        return;
      }
    }
  }

  private static void impacts(ServerLevel l, EmitterEntity e, long now) {
    if (now - e.impactTime < 8) return;
    e.contacts.entrySet().removeIf(entry -> now - entry.getValue() > 60);
    var p = e.getBlockPos();
    for (var link : e.links) {
      var t = link.target();
      var area =
          new net.minecraft.world.phys.AABB(
              Math.min(p.getX(), t.getX()) - .8,
              Math.min(p.getY(), t.getY()),
              Math.min(p.getZ(), t.getZ()) - .8,
              Math.max(p.getX(), t.getX()) + 1.8,
              Math.max(p.getY(), t.getY()) + 5,
              Math.max(p.getZ(), t.getZ()) + 1.8);
      for (var entity :
          l.getEntitiesOfClass(
              net.minecraft.world.entity.LivingEntity.class,
              area,
              entity -> FieldBlock.blocks(e, entity))) {
        double u =
            (entity.getX() - p.getX() - .5) * link.dx()
                + (entity.getZ() - p.getZ() - .5) * link.dz();
        int i = (int) Math.floor(u + .5);
        if (i <= 0 || i >= link.length() || now - e.transition < i * 2) continue;
        double normal =
            link.dx() != 0
                ? Math.abs(entity.getZ() - p.getZ() - .5)
                : Math.abs(entity.getX() - p.getX() - .5);
        if (normal > entity.getBbWidth() / 2 + .15
            || entity.getY() + entity.getBbHeight() < link.ground()[i]
            || entity.getY() > link.ground()[i] + 5
            || now - e.contacts.getOrDefault(entity.getUUID(), -1000L) < 25) continue;
        e.impact =
            new net.minecraft.world.phys.Vec3(
                p.getX() + .5 + u * link.dx(),
                Math.max(
                    link.ground()[i] + .15,
                    Math.min(link.ground()[i] + 4.85, entity.getY() + entity.getBbHeight() * .55)),
                p.getZ() + .5 + u * link.dz());
        e.impactTime = now;
        e.contacts.put(entity.getUUID(), now);
        e.sync();
        l.playSound(
            null,
            e.impact.x,
            e.impact.y,
            e.impact.z,
            net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,
            net.minecraft.sounds.SoundSource.BLOCKS,
            .20f,
            1.7f);
        return;
      }
    }
  }

  private static List<EmitterEntity> railNeighbors(
      EmitterEntity a, List<EmitterEntity> all, boolean includeDisabled) {
    var found = new ArrayList<EmitterEntity>();
    if (!includeDisabled && !a.enabled) return found;
    var face = a.getBlockState().getValue(RailBlock.FACING);
    EmitterEntity best = null;
    int distance = 21;
    for (var b : all) {
      if (!b.isRail() || !includeDisabled && !b.enabled || a == b) continue;
      var delta = b.getBlockPos().subtract(a.getBlockPos());
      int n =
          delta.getX() * face.getStepX()
              + delta.getY() * face.getStepY()
              + delta.getZ() * face.getStepZ();
      if (n == 0
          && a.getBlockPos().distManhattan(b.getBlockPos()) == 1
          && b.getBlockState().getValue(RailBlock.FACING) == face) found.add(b);
      if (n > 0
          && n < distance
          && delta.equals(
              new BlockPos(face.getStepX() * n, face.getStepY() * n, face.getStepZ() * n))
          && b.getBlockState().getValue(RailBlock.FACING) == face.getOpposite()) {
        best = b;
        distance = n;
      }
    }
    if (best != null) found.add(best);
    return found;
  }

  private static boolean input(ServerLevel l, EmitterEntity e) {
    var face = e.controls.inputFace;
    return l.getSignal(e.getBlockPos().relative(face), face) > 0;
  }

  private static String signature(List<EmitterEntity.Link> links) {
    StringBuilder s = new StringBuilder();
    for (var link : links) s.append(link.target()).append(Arrays.toString(link.ground()));
    return s.toString();
  }
}
