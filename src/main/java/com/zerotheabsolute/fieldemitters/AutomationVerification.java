package com.zerotheabsolute.fieldemitters;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

public final class AutomationVerification {
  public static void run(ServerLevel l, EmitterEntity e) {
    var saved = e.controls;
    long count = e.crossings;
    String last = e.lastDetection;
    int queued = e.queuedPulses, signal = e.outputSignal;
    var cow = EntityType.COW.create(l);
    var zombie = EntityType.ZOMBIE.create(l);
    var item = new ItemEntity(l, 0, 0, 0, new ItemStack(Items.GUNPOWDER, 3));
    try {
      var f = new EntityFilter();
      f.groups = 2;
      f.age = 1;
      cow.setAge(-24000);
      require(f.matches(cow, e.owner), "baby cow matches");
      cow.setAge(0);
      require(!f.matches(cow, e.owner), "adult cow excluded");
      f.inverted = true;
      require(f.matches(cow, e.owner), "allow-only-babies inversion blocks adult");
      f = new EntityFilter();
      f.groups = 8;
      f.itemType = "minecraft:gunpowder";
      require(f.matches(item, e.owner) && !f.matches(zombie, e.owner), "item ID filter");
      f.itemType = "#minecraft:logs";
      require(!f.matches(item, e.owner), "item tag rejects gunpowder");
      f = new EntityFilter();
      f.groups = 31;
      f.identity = cow.getUUID().toString();
      require(f.matches(cow, e.owner) && !f.matches(zombie, e.owner), "UUID match");
      var link = e.links.getFirst();
      int i = link.length() / 2;
      Vec3 center =
          new Vec3(
              e.getBlockPos().getX() + .5 + i * link.dx(),
              link.ground()[i] + .1,
              e.getBlockPos().getZ() + .5 + i * link.dz());
      Vec3 normal = Vec3.atLowerCornerOf(link.movement(true).getNormal());
      e.controls = new ControlSettings();
      e.controls.barrier.groups = 0;
      e.controls.sensor.groups = 8;
      e.controls.sensorMode = 1;
      e.controls.countItems = true;
      e.crossings = 0;
      e.queuedPulses = 0;
      e.outputSignal = 0;
      e.pulseUntil = 0;
      e.gapUntil = 0;
      e.passages.clear();
      item.setNoGravity(true);
      item.setPos(center.subtract(normal));
      l.addFreshEntity(item);
      long now = l.getGameTime();
      FieldSensor.tick(l, e, now);
      item.setPos(center);
      FieldSensor.tick(l, e, ++now);
      FieldSensor.tick(l, e, ++now);
      require(e.crossings == 0, "contact is not a crossing");
      item.setPos(center.add(normal));
      FieldSensor.tick(l, e, ++now);
      require(e.crossings == 3, "stack counts three items once");
      require(e.outputSignal == 15 && e.queuedPulses == 2, "first pulse starts, two queued");
      FieldSensor.tick(l, e, ++now);
      require(e.crossings == 3, "stationary entity does not recount");
      FieldSensor.tick(l, e, now += 4);
      require(e.outputSignal == 0, "pulse ends");
      FieldSensor.tick(l, e, ++now);
      require(e.outputSignal == 0, "pulse gap");
      FieldSensor.tick(l, e, ++now);
      require(e.outputSignal == 15, "next queued pulse");
      e.controls.sensor.directions = 1 << link.movement(true).ordinal();
      item.setPos(center.subtract(normal));
      FieldSensor.tick(l, e, ++now);
      require(e.crossings == 3, "reverse direction ignored");
      e.controls.sensorMode = 2;
      item.setPos(center);
      FieldSensor.tick(l, e, ++now);
      require(e.outputSignal == 15, "presence high");
      item.setPos(center.add(normal.scale(2)));
      FieldSensor.tick(l, e, ++now);
      require(e.outputSignal == 0, "presence clears");
    } finally {
      item.discard();
      cow.discard();
      zombie.discard();
      e.controls = saved;
      e.crossings = count;
      e.lastDetection = last;
      e.queuedPulses = queued;
      e.outputSignal = signal;
      e.pulseUntil = 0;
      e.gapUntil = 0;
      e.passages.clear();
      e.sync();
      l.updateNeighborsAt(e.getBlockPos(), e.getBlockState().getBlock());
    }
  }

  private static void require(boolean condition, String label) {
    if (!condition) throw new IllegalStateException(label);
  }
}
