package com.zerotheabsolute.fieldemitters;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Exercises Minecraft's actual entity movement against the placed demo, on the server thread. */
public final class DemoVerification {
  public static int run(CommandSourceStack source) {
    var l = source.getLevel();
    var at = BlockPos.containing(source.getPosition());
    EmitterEntity e = null;
    for (int x = -8; x <= 8; x++)
      for (int z = -8; z <= 8; z++)
        for (int y = -5; y <= 5; y++)
          if (l.getBlockEntity(at.offset(x, y, z)) instanceof EmitterEntity candidate
              && candidate.powered
              && !candidate.links.isEmpty()) e = candidate;
    if (e == null) {
      source.sendFailure(Component.literal("Stand near a powered emitter with an outgoing link."));
      return 0;
    }
    var link = e.links.getFirst();
    int index = link.length() / 2;
    var p = e.getBlockPos();
    double x = p.getX() + .5 + index * link.dx(),
        z = p.getZ() + .5 + index * link.dz(),
        y = link.ground()[index] + .05;
    double nx = link.dx() == 0 ? 1 : 0, nz = link.dz() == 0 ? 1 : 0;
    var zombie = EntityType.ZOMBIE.create(l);
    var cow = EntityType.COW.create(l);
    var owner =
        FakePlayerFactory.get(
            l,
            new GameProfile(
                UUID.fromString("56f3e2a2-9270-4b13-8822-303bef87c5ac"), "FieldTestOwner"));
    var other =
        FakePlayerFactory.get(
            l,
            new GameProfile(
                UUID.fromString("642cae2b-cd05-45eb-88dd-7788aa732915"), "FieldTestGuest"));
    int oldMask = e.mask;
    var oldControls = e.controls;
    e.controls = new ControlSettings();
    UUID oldOwner = e.owner;
    boolean powered = e.powered;
    long transition = e.transition;
    try {
      e.owner = owner.getUUID();
      e.transition = l.getGameTime() - 100;
      for (int mask = 0; mask < 8; mask++) {
        e.mask = mask;
        e.controls.barrier.groups = mask;
        for (int direction : new int[] {-1, 1}) {
          movement(
              zombie,
              x,
              y,
              z,
              nx * direction,
              nz * direction,
              (mask & 1) != 0,
              "hostile mask " + mask);
          movement(
              cow,
              x,
              y,
              z,
              nx * direction,
              nz * direction,
              (mask & 2) != 0,
              "peaceful mask " + mask);
          movement(
              other,
              x,
              y,
              z,
              nx * direction,
              nz * direction,
              (mask & 4) != 0,
              "guest mask " + mask);
          movement(owner, x, y, z, nx * direction, nz * direction, false, "owner mask " + mask);
        }
      }
      e.powered = false;
      e.mask = 7;
      e.controls.barrier.groups = 7;
      movement(zombie, x, y, z, nx, nz, false, "power off");
      e.powered = true;
      e.transition = l.getGameTime();
      movement(zombie, x, y, z, nx, nz, false, "projection not arrived");
      e.transition = l.getGameTime() - 100;
      e.mask = 1;
      e.controls.barrier.groups = 1;
      for (var segment : e.links)
        for (int i = 1; i < segment.length(); i++) {
          var cell =
              new BlockPos(
                  p.getX() + segment.dx() * i, segment.ground()[i], p.getZ() + segment.dz() * i);
          if (!(l.getBlockEntity(cell) instanceof FieldCell))
            throw new IllegalStateException("Missing terrain cell " + cell);
          double cx = cell.getX() + .5, cz = cell.getZ() + .5;
          movement(
              zombie,
              cx,
              cell.getY() + .02,
              cz,
              segment.dx() == 0 ? 1 : 0,
              segment.dz() == 0 ? 1 : 0,
              true,
              "terrain column " + i);
        }
      AutomationVerification.run(l, e);
      source.sendSuccess(
          () ->
              Component.literal(
                  "PASS: actual server movement for all 8 target combinations, both directions,"
                      + " owner exemption, power-off release, projection timing, and every terrain"
                      + " column. Automation: baby/adult, inversion, UUID, item filters, completed"
                      + " crossings, queued pulses, direction and presence."),
          false);
      return 1;
    } catch (Exception failure) {
      source.sendFailure(Component.literal("FAIL: " + failure.getMessage()));
      throw new IllegalStateException(failure);
    } finally {
      e.mask = oldMask;
      e.controls = oldControls;
      e.owner = oldOwner;
      e.powered = powered;
      e.transition = transition;
      e.sync();
      zombie.discard();
      cow.discard();
    }
  }

  private static void movement(
      Entity entity,
      double x,
      double y,
      double z,
      double nx,
      double nz,
      boolean expectedBlocked,
      String label) {
    entity.noPhysics = false;
    entity.setDeltaMovement(Vec3.ZERO);
    entity.setPos(x - nx, y, z - nz);
    entity.setOnGround(false);
    entity.move(MoverType.SELF, new Vec3(nx * 2, 0, nz * 2));
    double traveled = (entity.getX() - (x - nx)) * nx + (entity.getZ() - (z - nz)) * nz;
    boolean blocked = traveled < 1.5;
    if (blocked != expectedBlocked)
      throw new IllegalStateException(
          label + " expected blocked=" + expectedBlocked + " traveled=" + traveled);
  }
}
