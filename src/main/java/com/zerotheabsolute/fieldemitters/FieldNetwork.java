package com.zerotheabsolute.fieldemitters;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraftforge.event.TickEvent;

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

  /**
   * Connected emitters from the last rebuild, at most half a second old. Collision and sensor
   * queries run many times per tick, so they read this cache instead of searching again.
   */
  public static List<EmitterEntity> members(EmitterEntity seed) {
    var cached = seed.network;
    if (cached != null && cached.contains(seed)) return cached;
    return connected(seed);
  }

  private static List<EmitterEntity> connected(EmitterEntity seed, boolean includeDisabled) {
    if (seed.getLevel() == null) return List.of(seed);
    List<EmitterEntity> all = loaded(seed.getLevel());
    return com.zeromods.core.network.NetworkTraversal.connected(seed,
        e -> e.getBlockPos(), e -> neighbors(e, all, includeDisabled));
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
    if (a.isTower()) return java.util.List.of();
    if (a.isRail()) return railNeighbors(a, all, includeDisabled);
    List<EmitterEntity> found = new ArrayList<>();
    for (Direction d : Direction.Plane.HORIZONTAL) {
      var best = nearest(a, all, d);
      // Links are mutual: both posts must choose each other and the ground must trace both ways.
      // Otherwise stacked or offset posts could produce one-directional links, and a group
      // would look different depending on which post it was traversed from.
      if (best != null
          && (includeDisabled || best.enabled && a.enabled)
          && nearest(best, all, d.getOpposite()) == a
          && trace(a, best) != null
          && trace(best, a) != null) found.add(best);
    }
    return found;
  }

  /** The closest post in one direction, preferring the closest height when posts are stacked. */
  private static EmitterEntity nearest(EmitterEntity a, List<EmitterEntity> all, Direction d) {
    EmitterEntity best = null;
    int distance = 21, rise = Integer.MAX_VALUE;
    for (var b : all) {
      if (b == a || b.isTower() || b.isRail() || !Objects.equals(a.owner, b.owner)) continue;
      int x = b.getBlockPos().getX() - a.getBlockPos().getX(),
          z = b.getBlockPos().getZ() - a.getBlockPos().getZ(),
          dy = Math.abs(b.getBlockPos().getY() - a.getBlockPos().getY());
      int n = x * d.getStepX() + z * d.getStepZ();
      if (n <= 0 || x != n * d.getStepX() || z != n * d.getStepZ() || dy > 8) continue;
      if (n < distance || (n == distance && dy < rise)) {
        best = b;
        distance = n;
        rise = dy;
      }
    }
    return best;
  }

  /**
   * The facing of this span that points into the enclosed area, found by stepping half a block off
   * the middle of the span and testing that point against the ring. A ray test rather than an
   * averaged centre, so it stays correct where the ring bends back on itself.
   */
  private static Direction inward(EmitterEntity e, EmitterEntity.Link link, GroupGeometry group) {
    double midX = (e.getBlockPos().getX() + link.target().getX()) / 2.0 + .5;
    double midZ = (e.getBlockPos().getZ() + link.target().getZ()) / 2.0 + .5;
    if (link.normal() == Direction.Axis.X)
      return enclosed(group, midX + PROBE_OFFSET, midZ) ? Direction.EAST : Direction.WEST;
    return enclosed(group, midX, midZ + PROBE_OFFSET) ? Direction.SOUTH : Direction.NORTH;
  }

  /** Whether a point lies inside the ring, by counting the spans a ray along positive X crosses. */
  private static boolean enclosed(GroupGeometry group, double x, double z) {
    boolean inside = false;
    for (var span : group.crossings())
      if (span[0] > x && z >= span[1] && z < span[2]) inside = !inside;
    return inside;
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
          t, face.getStepX(), face.getStepZ(), heights, face.getStepY(), normal, true, null);
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

  public static void tick(TickEvent.LevelTickEvent event) {
    if (event.phase != TickEvent.Phase.END) return;
    if (!(event.level instanceof ServerLevel l)) return;
    List<EmitterEntity> all = loaded(l);
    if (all.isEmpty()) {
      if (l.getGameTime() % 10 == 0) ManagedFields.refresh(l, all);
      return;
    }
    long now = l.getGameTime();
    for (var e : all) {
      e.impactWaves.removeIf(wave -> now - wave.time() >= FieldImpacts.LIFETIME);
      e.spherePresent=false;
      if(e.isTower())SphereField.tick(l,e,now);
      if (e.powered && !e.isTower()) {
        if (e.isRail()) railImpacts(l, e, now);
        else impacts(l, e, now);
      }
      FieldSensor.tick(l, e, now);
      FieldDamage.tick(l, e, now);
      FieldCheckpoint.tick(l,e,now);
      if (now % 20 == 0) e.sync();
      if (e.powered && now - e.transition <= com.zeromods.core.animation.PlanarProjection.DURATION_TICKS + 2 && now % 2 == 0) updateLights(l, e, true);
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
      long demand = network.stream().mapToLong(e -> e.demand).sum();
      long stored =
          network.stream()
              .filter(e -> e.enabled)
              .mapToLong(e -> e.energy.getEnergyStored())
              .sum();
      boolean allowed =
          network.stream()
              .allMatch(
                  e -> e.controls.inputMode == 0 || (input(l, e) == (e.controls.inputMode == 1)));
      // A zero-cost field still needs a span; a lone emitter must stay idle.
      boolean hasField =
          network.stream()
              .anyMatch(
                  e ->
                      e.enabled
                          && (e.isTower() ? SphereField.loaded(e) : e.links.stream().anyMatch(link -> link.rail() || link.length() > 1)));
      boolean on = hasField && allowed && stored >= demand;
      if (on) {
        long remaining = demand;
        for (var e : network)
          if (e.enabled) {
            remaining -=
                e.energy.consume((int) Math.min(remaining, Integer.MAX_VALUE));
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
          FieldSounds.play(l, e, net.minecraft.world.phys.Vec3.atCenterOf(e.getBlockPos()), active ? 0 : 1);
        }
      }
    }
  }

  /**
   * A group of connected emitters and rails shares one set of settings. Whenever a member has been
   * placed since the last rebuild, every member is brought in line with the member that has been
   * configured, or with the oldest member when none has. A new post joining a perimeter takes the
   * perimeter's rules, and a post bridging two configured groups leaves one group with one set of
   * rules, so it never matters which block of a group a player opens.
   */
  private static void adopt(ServerLevel l, List<EmitterEntity> network) {
    var members = network.stream().filter(e -> !e.isTower()).toList();
    if (members.size() < 2 || members.stream().noneMatch(e -> e.adoptPending)) return;
    var template =
        members.stream()
            .filter(e -> !e.adoptPending)
            .min(java.util.Comparator.comparingLong(e -> e.placedAt))
            .orElseGet(
                () ->
                    members.stream()
                        .min(java.util.Comparator.comparingLong(e -> e.placedAt))
                        .orElseThrow());
    String settings = template.controls.save().toString();
    for (var e : members) {
      e.adoptPending = false;
      if (e == template) continue;
      if (e.color == template.color
          && e.enabled == template.enabled
          && e.controls.save().toString().equals(settings)) continue;
      e.controls = ControlSettings.load(template.controls.save());
      e.color = template.color;
      e.enabled = template.enabled;
      e.mask = e.controls.barrier.groups;
      e.passages.clear();
      l.updateNeighborsAt(e.getBlockPos(), e.getBlockState().getBlock());
      e.sync();
    }
  }

  /** Axis-aligned spans need four posts to close a ring, and every post must continue it. */
  private static final int MIN_ENCLOSING_POSTS = 4, MIN_ENCLOSING_NEIGHBOURS = 2;

  /** Half a block to either side of a span, far enough to be clear of it and of any corner. */
  private static final double PROBE_OFFSET = .5;

  /**
   * The group's spans that can cross a ray travelling along positive X, each as {x, zFrom, zTo}.
   * Spans that run along X are parallel to the ray and cannot cross it, so they are not kept.
   */
  private record GroupGeometry(List<double[]> crossings, boolean closed) {}

  private static void rebuild(ServerLevel l, List<EmitterEntity> all, long now) {
    ManagedFields.refresh(l, all);
    // One adjacency pass per rebuild, shared by the enclosure test and the link pass below.
    Map<BlockPos, List<EmitterEntity>> adjacency = new HashMap<>(all.size());
    for (var e : all) adjacency.put(e.getBlockPos(), neighbors(e, all));
    Map<BlockPos, GroupGeometry> geometry = new HashMap<>(all.size());
    // The root is the lowest position in the group, never the post that happens to hold energy or
    // a redstone signal. Detection output, checkpoint storage and the formation origin therefore
    // stay where they were put and only move when the player changes or breaks the field.
    Set<BlockPos> grouped = new HashSet<>();
    for (var seed : all) {
      if (grouped.contains(seed.getBlockPos())) continue;
      var network = connected(seed);
      for (var member : network) member.network = network;
      var source =
          network.stream()
              .min(java.util.Comparator.comparingLong(e -> e.getBlockPos().asLong()))
              .orElse(seed);
      // Posts that enclose an area give every span an inside, which is what lets one relative
      // rule read correctly on all four sides of a perimeter. A line of posts encloses nothing.
      var posts = network.stream().filter(x -> !x.isRail() && !x.isTower()).toList();
      var crossings = new ArrayList<double[]>();
      for (var post : posts)
        for (var other : adjacency.getOrDefault(post.getBlockPos(), List.of())) {
          var from = post.getBlockPos();
          var to = other.getBlockPos();
          // Each span once, and only those running along Z, which are the ones a ray can cross.
          if (other.isRail() || other.isTower() || from.asLong() >= to.asLong()) continue;
          if (from.getX() != to.getX()) continue;
          crossings.add(
              new double[] {
                from.getX() + .5,
                Math.min(from.getZ(), to.getZ()) + .5,
                Math.max(from.getZ(), to.getZ()) + .5
              });
        }
      var group =
          new GroupGeometry(
              crossings,
              posts.size() >= MIN_ENCLOSING_POSTS
                  && posts.stream()
                      .allMatch(
                          x ->
                              adjacency.getOrDefault(x.getBlockPos(), List.of()).size()
                                  >= MIN_ENCLOSING_NEIGHBOURS));
      for (var e : network) {
        e.root = source.getBlockPos();
        geometry.put(e.getBlockPos(), group);
        grouped.add(e.getBlockPos());
      }
      adopt(l, network);
    }
    for (var e : all) {
      if(e.isTower()){SphereField.rebuild(l,e);updateLights(l,e,e.powered);continue;}
      List<EmitterEntity.Link> links = new ArrayList<>();
      if (e.enabled)
        for (var other : adjacency.getOrDefault(e.getBlockPos(), List.of())) {
          // The lower position always owns the link, so its detection output and checkpoint
          // storage never migrate to the other end.
          if (other.enabled && e.getBlockPos().asLong() < other.getBlockPos().asLong()) {
            var link = trace(e, other);
            if (link != null) links.add(link);
          }
        }
      // Keep the outgoing geometry until its shutdown has reached zero.
      if (!e.enabled && (e.powered || now - e.transition < FieldShutdown.DURATION_TICKS))
        links = new ArrayList<>(e.links);
      // A rule kept for a partner that has been removed would silently return if a post were
      // rebuilt in the same spot. Unloaded partners are left alone.
      e.overrides
          .keySet()
          .removeIf(t -> l.hasChunkAt(t) && !(l.getBlockEntity(t) instanceof EmitterEntity));
      var group = geometry.get(e.getBlockPos());
      if (group != null && group.closed() && !e.isRail())
        links.replaceAll(link -> link.withInward(inward(e, link, group)));
      String before = signature(e.links);
      e.links = links;
      e.demand =
          links.stream()
              .mapToInt(
                  link ->
                      (link.length() + (link.rail() ? 1 : -1))
                          * link.height()
                          * FieldConfig.energyPerCell())
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
                      .setValue(FieldBlock.LIT, e.powered && e.controls.light && l.getGameTime()-e.transition >= e.controls.linkFormationTicks(i)),
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
    if(e.isTower()){for(int i=0;i<TowerBlock.HEIGHT;i++){var p=e.getBlockPos().above(i);var state=l.getBlockState(p);if(state.is(FieldEmitters.TOWER.get()))l.setBlock(p,state.setValue(TowerBlock.ACTIVE,active).setValue(TowerBlock.LIGHT,active&&e.controls.light),3);}}
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
                + Math.abs(p.getZ() - e.getBlockPos().getZ())
                + (e.isRail() ? Math.abs(p.getY() - e.getBlockPos().getY()) : 0);
        boolean lit = active && e.controls.light && l.getGameTime() - e.transition >= (e.isTower()?SphereField.FORMATION_TICKS:e.controls.linkFormationTicks(distance));
        l.setBlock(p, state.setValue(FieldBlock.LIT, lit), 3);
      }
    }
  }

  private static void railImpacts(ServerLevel level, EmitterEntity emitter, long now) {
    if (now - emitter.impactTime < ImpactSelection.EFFECT_INTERVAL) return;
    record Hit(net.minecraft.world.phys.Vec3 position, Direction.Axis normal) {}
    var selection = new ImpactSelection<Hit>(emitter.contacts, now);
    var origin = net.minecraft.world.phys.Vec3.atCenterOf(emitter.getBlockPos());
    for (var source : members(emitter)) {
      if (source.isRemoved() || !source.powered || !source.isRail()) continue;
      var space = FieldSpace.at(source);
      var sourceOrigin = net.minecraft.world.phys.Vec3.atCenterOf(source.getBlockPos());
      for (var link : source.links) {
        if (emitter.links.stream().noneMatch(other -> other.normal() == link.normal())
            || Math.abs(sourceOrigin.get(link.normal()) - origin.get(link.normal())) > .01)
          continue;
        var bounds = link.box(source.getBlockPos());
        for (var entity :
            level.getEntities((net.minecraft.world.entity.Entity) null, bounds.inflate(.2))) {
          var center = space.local(entity.getBoundingBox().getCenter());
          double u =
              center
                  .subtract(sourceOrigin)
                  .dot(new net.minecraft.world.phys.Vec3(link.dx(), link.dy(), link.dz()));
          int index = Math.max(0, Math.min(link.length(), (int) Math.floor(u + .5)));
          var cell = link.cell(source.getBlockPos(), index, 0);
          var shape = FieldBlock.collision(source, entity, cell, center);
          if (shape.isEmpty()
              || !shape.bounds().move(cell).inflate(.06).intersects(space.local(entity.getBoundingBox())))
            continue;
          var impact =
              new net.minecraft.world.phys.Vec3(
                  Math.max(bounds.minX, Math.min(bounds.maxX, center.x)),
                  Math.max(bounds.minY, Math.min(bounds.maxY, center.y)),
                  Math.max(bounds.minZ, Math.min(bounds.maxZ, center.z)));
          impact =
              switch (link.normal()) {
                case X -> new net.minecraft.world.phys.Vec3(origin.x, impact.y, impact.z);
                case Y -> new net.minecraft.world.phys.Vec3(impact.x, link.origin(emitter.getBlockPos()).y, impact.z);
                case Z -> new net.minecraft.world.phys.Vec3(impact.x, impact.y, origin.z);
              };
          selection.consider(entity.getUUID(), new Hit(impact, link.normal()));
        }
      }
    }
    var hits = selection.finish();
    if (hits.isEmpty()) return;
    // Publish one batch per connected plane, keeping waves continuous across rail seams.
    for (var part : members(emitter)) {
      if (part.isRemoved() || !part.powered || !part.isRail()) continue;
      var partOrigin = net.minecraft.world.phys.Vec3.atCenterOf(part.getBlockPos());
      var positions = new ArrayList<net.minecraft.world.phys.Vec3>();
      for (var choice : hits) {
        var hit = choice.value();
        if (Math.abs(partOrigin.get(hit.normal()) - origin.get(hit.normal())) > .01) continue;
        if (!part.links.isEmpty()
            && part.links.stream().noneMatch(other -> other.normal() == hit.normal())) continue;
        positions.add(hit.position());
        part.contacts.put(choice.id(), now);
      }
      if (positions.isEmpty()) continue;
      FieldImpacts.add(part, positions, now);
      part.sync();
    }
    FieldSounds.play(level, emitter, hits.get(0).value().position(), 2);
  }

  private static void impacts(ServerLevel l, EmitterEntity e, long now) {
    if (now - e.impactTime < ImpactSelection.EFFECT_INTERVAL) return;
    var selection = new ImpactSelection<net.minecraft.world.phys.Vec3>(e.contacts, now);
    var p = e.getBlockPos();
    var space = FieldSpace.at(e);
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
              entity -> true)) {
        var feet = space.local(entity.position());
        double u =
            (feet.x - p.getX() - .5) * link.dx()
                + (feet.z - p.getZ() - .5) * link.dz();
        int i = (int) Math.floor(u + .5);
        if (i <= 0 || i >= link.length() || now - e.transition < e.controls.linkFormationTicks(i)) continue;
        double normal =
            link.dx() != 0
                ? Math.abs(feet.z - p.getZ() - .5)
                : Math.abs(feet.x - p.getX() - .5);
        if (FieldBlock.collision(e, entity, link.cell(p, i, 0), space.local(entity.getBoundingBox().getCenter())).isEmpty()) continue;
        if (normal > entity.getBbWidth() / 2 + .15
            || feet.y + entity.getBbHeight() < link.ground()[i]
            || feet.y > link.ground()[i] + 5) continue;
        var impact =
            new net.minecraft.world.phys.Vec3(
                p.getX() + .5 + u * link.dx(),
                Math.max(
                    link.ground()[i] + .15,
                    Math.min(link.ground()[i] + 4.85, feet.y + entity.getBbHeight() * .55)),
                p.getZ() + .5 + u * link.dz());
        selection.consider(entity.getUUID(), impact);
      }
    }
    var hits = selection.finish();
    if (hits.isEmpty()) return;
    FieldImpacts.add(e, hits.stream().map(ImpactSelection.Choice::value).toList(), now);
    e.sync();
    FieldSounds.play(l, e, e.impact, 2);
  }

  private static List<EmitterEntity> railNeighbors(
      EmitterEntity a, List<EmitterEntity> all, boolean includeDisabled) {
    var found = new ArrayList<EmitterEntity>();
    if (!includeDisabled && !a.enabled) return found;
    var face = a.getBlockState().getValue(RailBlock.FACING);
    for (var b : all) {
      if (!Objects.equals(a.owner, b.owner) || !b.isRail() || !includeDisabled && !b.enabled || a == b)
        continue;
      var delta = b.getBlockPos().subtract(a.getBlockPos());
      int n =
          delta.getX() * face.getStepX()
              + delta.getY() * face.getStepY()
              + delta.getZ() * face.getStepZ();
      if (n == 0
          && a.getBlockPos().distManhattan(b.getBlockPos()) == 1
          && b.getBlockState().getValue(RailBlock.FACING) == face) found.add(b);
    }
    // Facing rails link only when they choose each other. Without this, three rails in a line can
    // give one rail a span that reaches past its neighbour, so the same group looks different
    // depending on which rail it is traversed from and its power can oscillate.
    var best = nearestOpposing(a, all, includeDisabled);
    if (best != null && nearestOpposing(best, all, includeDisabled) == a) found.add(best);
    return found;
  }

  /** The closest rail facing this one head on, within the twenty block span. */
  private static EmitterEntity nearestOpposing(
      EmitterEntity a, List<EmitterEntity> all, boolean includeDisabled) {
    var face = a.getBlockState().getValue(RailBlock.FACING);
    EmitterEntity best = null;
    int distance = 21;
    for (var b : all) {
      if (!Objects.equals(a.owner, b.owner) || !b.isRail() || !includeDisabled && !b.enabled || a == b)
        continue;
      var delta = b.getBlockPos().subtract(a.getBlockPos());
      int n =
          delta.getX() * face.getStepX()
              + delta.getY() * face.getStepY()
              + delta.getZ() * face.getStepZ();
      if (n > 0
          && n < distance
          && delta.equals(
              new BlockPos(face.getStepX() * n, face.getStepY() * n, face.getStepZ() * n))
          && b.getBlockState().getValue(RailBlock.FACING) == face.getOpposite()) {
        best = b;
        distance = n;
      }
    }
    return best;
  }

  private static boolean input(ServerLevel l, EmitterEntity e) {
    if (!e.controls.inputAny) {
      var face = e.controls.inputFace;
      return l.getSignal(e.getBlockPos().relative(face), face) > 0;
    }
    for (var face : net.minecraft.core.Direction.values())
      if (face != e.controls.outputFace && l.getSignal(e.getBlockPos().relative(face), face) > 0)
        return true;
    return false;
  }

  private static String signature(List<EmitterEntity.Link> links) {
    StringBuilder s = new StringBuilder();
    for (var link : links)
      s.append(link.target()).append(Arrays.toString(link.ground())).append(link.inward());
    return s.toString();
  }
}
