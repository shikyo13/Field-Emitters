package com.zerotheabsolute.fieldemitters;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.*;

/**
 * Server-side inventory transfers preserve components and remove only successfully transferred
 * items.
 */
public final class FieldCheckpoint {
  private static final int CONTACT_GAP_TICKS = 4;
  private static final int PICKUP_DELAY_TICKS = 40;
  private static final int MAX_QUEUED_PULSES = 100000;

  public static boolean hasContraband(Player p, CheckpointSettings s) {
    for (int i = 0; i < p.getInventory().getContainerSize(); i++)
      if (s.matches(p.getInventory().getItem(i))) return true;
    return false;
  }

  public static boolean blocks(
      EmitterEntity e,
      ControlSettings settings,
      net.minecraft.world.entity.Entity entity,
      Direction movement) {
    if (!(entity instanceof Player p)) return false;
    var s = settings.checkpoint;
    if (!s.applies(p, e.owner, movement) || !hasContraband(p, s)) return false;
    // Storage checkpoints hold the player until every selected item has been transferred.
    return s.deny || s.confiscate == 2 && !s.dropOverflow;
  }

  public static void process(
      ServerLevel level,
      EmitterEntity e,
      ControlSettings settings,
      Player player,
      Direction movement,
      Vec3 entry,
      long now) {
    var s = settings.checkpoint;
    if (!s.applies(player, e.owner, movement) || !hasContraband(player, s)) return;
    // Stacked rails share one output and one contact record at their root.
    var output =
        e.isRail() && level.getBlockEntity(e.root) instanceof EmitterEntity root ? root : e;
    String key = "checkpoint:" + player.getUUID();
    var old = output.passages.get(key);
    boolean fresh = old == null || now - old.time() > CONTACT_GAP_TICKS;
    output.passages.put(key, new EmitterEntity.Passage(0, entry, now));
    if (fresh && s.detect) {
      output.crossings++;
      output.queuedPulses = Math.min(MAX_QUEUED_PULSES, output.queuedPulses + 1);
      output.lastDetection = "Contraband: " + player.getName().getString();
      output.sync();
    }
    if (s.confiscate == 0) return;
    IItemHandler storage = null;
    var storagePos = e.getBlockPos().relative(s.storageFace);
    if (s.confiscate == 2 && level.hasChunkAt(storagePos))
      storage =
          level.getCapability(
              Capabilities.ItemHandler.BLOCK, storagePos, s.storageFace.getOpposite());
    boolean changed = false;
    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
      var stack = player.getInventory().getItem(i);
      if (!s.matches(stack)) continue;
      if (storage != null) {
        var remainder = ItemHandlerHelper.insertItemStacked(storage, stack.copy(), false);
        int moved = stack.getCount() - remainder.getCount();
        if (moved > 0) {
          stack.shrink(moved);
          changed = true;
        }
      }
      if (!stack.isEmpty() && (s.confiscate == 1 || s.dropOverflow)) {
        var drop = new ItemEntity(level, entry.x, entry.y, entry.z, stack.copy());
        drop.setPickUpDelay(PICKUP_DELAY_TICKS);
        drop.setDeltaMovement(Vec3.ZERO);
        if (level.addFreshEntity(drop)) {
          stack.setCount(0);
          changed = true;
        }
      }
    }
    if (changed) {
      player.getInventory().setChanged();
      player.containerMenu.broadcastChanges();
    }
  }

  public static void tick(ServerLevel level, EmitterEntity e, long now) {
    if (!e.powered) return;
    for (var link : e.links) {
      var settings = e.settings(link);
      if (!settings.checkpoint.enabled) continue;
      var p = e.getBlockPos();
      for (var player :
          level.getEntitiesOfClass(Player.class, link.box(p).inflate(.8), a -> !a.isSpectator())) {
        var center = player.getBoundingBox().getCenter();
        double u =
            (center.x - p.getX() - .5) * link.dx()
                + (center.z - p.getZ() - .5) * link.dz()
                + (center.y - p.getY() - .5) * link.dy();
        int i = (int) Math.floor(u + .5);
        if (i < (link.rail() ? 0 : 1)
            || i > (link.rail() ? link.length() : link.length() - 1)
            || now - e.transition < e.controls.linkFormationTicks(i)) continue;
        var cell = link.cell(p, i, 0);
        if (!link.rail()
            && (player.getY() >= cell.getY() + 5 || player.getBoundingBox().maxY <= cell.getY()))
          continue;
        double side = link.normalCoordinate(center) - link.normalCoordinate(Vec3.atCenterOf(p));
        double reach =
            (link.normal() == Direction.Axis.Y ? player.getBbHeight() : player.getBbWidth()) * .5
                + .3;
        if (Math.abs(side) > reach) continue;
        var previous =
            center.add(
                player.xo - player.getX(), player.yo - player.getY(), player.zo - player.getZ());
        double previousSide =
            link.normalCoordinate(previous) - link.normalCoordinate(Vec3.atCenterOf(p));
        Direction movement =
            link.movement((Math.abs(previousSide) > .001 ? previousSide : side) < 0);
        var away = Vec3.atLowerCornerOf(movement.getOpposite().getNormal());
        var entry = player.position().add(away.scale(reach + .5));
        process(level, e, settings, player, movement, entry, now);
      }
    }
    e.passages
        .entrySet()
        .removeIf(a -> a.getKey().startsWith("checkpoint:") && now - a.getValue().time() > 20);
  }
}
