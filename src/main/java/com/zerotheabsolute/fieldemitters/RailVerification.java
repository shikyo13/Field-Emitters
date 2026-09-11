package com.zerotheabsolute.fieldemitters;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;

public final class RailVerification {
  public static int run(CommandSourceStack source) {
    var l = source.getLevel();
    var at = BlockPos.containing(source.getPosition());
    int verified = 0;
    var mob = EntityType.ZOMBIE.create(l);
    mob.setBaby(true);
    try {
      for (var p : BlockPos.betweenClosed(at.offset(-12, -8, -12), at.offset(12, 8, 12))) {
        if (!(l.getBlockEntity(p) instanceof EmitterEntity e)
            || !e.isRail()
            || e.links.isEmpty()
            || !e.powered) continue;
        var saved = e.controls;
        var overrides = new java.util.HashMap<>(e.overrides);
        long transition = e.transition;
        try {
          e.controls = new ControlSettings();
          e.overrides.clear();
          e.transition = l.getGameTime() - 100;
          for (var link : e.links) {
            int i = link.length() / 2;
            var cell = link.cell(e.getBlockPos(), i, 0);
            for (int direction : new int[] {-1, 1}) {
              var n = Vec3.atLowerCornerOf(link.movement(direction > 0).getNormal());
              var center = Vec3.atCenterOf(cell).add(0, -mob.getBbHeight() / 2, 0);
              for (boolean blocked : new boolean[] {true, false}) {
                e.controls.barrier.directions =
                    blocked ? 63 : 1 << link.movement(direction < 0).ordinal();
                mob.setPos(center.subtract(n));
                mob.setDeltaMovement(Vec3.ZERO);
                mob.setOnGround(false);
                mob.move(MoverType.SELF, n.scale(2));
                double travel = mob.position().subtract(center.subtract(n)).dot(n);
                if ((travel < 1.5) != blocked)
                  throw new IllegalStateException(
                      "rail "
                          + p
                          + " normal "
                          + link.normal()
                          + " direction "
                          + direction
                          + " travel="
                          + travel);
              }
            }
            verified++;
          }
        } finally {
          e.controls = saved;
          e.overrides.clear();
          e.overrides.putAll(overrides);
          e.transition = transition;
          e.sync();
        }
      }
      if (verified == 0) throw new IllegalStateException("No powered rail links nearby");
      int total = verified;
      source.sendSuccess(
          () ->
              Component.literal(
                  "PASS: "
                      + total
                      + " rail spans, baby mob movement, both directions, one-way release."),
          false);
      return verified;
    } finally {
      mob.discard();
    }
  }
}
