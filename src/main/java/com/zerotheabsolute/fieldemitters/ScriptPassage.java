package com.zerotheabsolute.fieldemitters;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Short-lived server decisions are mirrored to clients for player collision prediction. */
final class ScriptPassage {
  private static final int REFRESH_TICKS = 5;
  private static final int LIFETIME_TICKS = 15;
  private static final double PLAYER_RANGE = 48;
  private ScriptPassage() {}

  static void tick(ServerLevel level, EmitterEntity emitter, long now) {
    if (now % REFRESH_TICKS != 0) return;
    var decisions = new CompoundTag();
    if (emitter.powered && FieldAutomation.interested("passage")) {
      var space = FieldSpace.at(emitter);
      for (var player : level.players()) {
        if (space.local(player.position()).distanceToSqr(Vec3.atCenterOf(emitter.getBlockPos())) > PLAYER_RANGE * PLAYER_RANGE) continue;
        int allow = 0, deny = 0;
        for (var direction : Direction.values()) {
          boolean blocked = emitter.controls.blocks(player, emitter.owner, direction)
              || FieldCheckpoint.blocks(emitter, emitter.controls, player, direction);
          var event = FieldAutomation.decide(emitter, player, direction, blocked);
          if (event != null && event.hasDecision()) {
            if (event.isBlocked()) deny |= 1 << direction.ordinal();
            else allow |= 1 << direction.ordinal();
          }
        }
        if ((allow | deny) == 0) continue;
        var decision = new CompoundTag();
        decision.putInt("Allow", allow); decision.putInt("Deny", deny);
        decision.putLong("Expires", now + LIFETIME_TICKS);
        decisions.put(player.getUUID().toString(), decision);
      }
    }
    if (!decisions.equals(emitter.scriptPassage)) { emitter.scriptPassage = decisions; emitter.sync(); }
  }

  static Boolean cached(EmitterEntity emitter, Entity entity, Direction direction) {
    if (direction == null || emitter.getLevel() == null) return null;
    var decision = emitter.scriptPassage.getCompound(entity.getUUID().toString());
    if (decision.getLong("Expires") <= emitter.getLevel().getGameTime()) return null;
    int bit = 1 << direction.ordinal();
    if ((decision.getInt("Deny") & bit) != 0) return true;
    if ((decision.getInt("Allow") & bit) != 0) return false;
    return null;
  }
}
