package com.zerotheabsolute.fieldemitters;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

public final class ControlSettings {
  public EntityFilter barrier = new EntityFilter(), sensor = new EntityFilter();
  public boolean light = true, visible = true, animation = true, countItems = false;
  public int railNormal = 2;
  public int sensorMode = 0, pulseTicks = 4, inputMode = 0;
  public Direction outputFace = Direction.DOWN, inputFace = Direction.UP;

  public ControlSettings() {
    barrier.exemptOwner = true;
    sensor.groups = 31;
  }

  public CompoundTag save() {
    var t = new CompoundTag();
    t.put("Barrier", barrier.save());
    t.put("Sensor", sensor.save());
    t.putBoolean("Light", light);
    t.putBoolean("Visible", visible);
    t.putBoolean("Animation", animation);
    t.putInt("RailNormal", railNormal);
    t.putBoolean("CountItems", countItems);
    t.putInt("SensorMode", sensorMode);
    t.putInt("PulseTicks", pulseTicks);
    t.putInt("InputMode", inputMode);
    t.putInt("OutputFace", outputFace.ordinal());
    t.putInt("InputFace", inputFace.ordinal());
    return t;
  }

  public static ControlSettings load(CompoundTag t) {
    var s = new ControlSettings();
    s.barrier = EntityFilter.load(t.getCompound("Barrier"));
    s.sensor = EntityFilter.load(t.getCompound("Sensor"));
    s.light = t.getBoolean("Light");
    s.visible = t.getBoolean("Visible");
    s.animation = t.getBoolean("Animation");
    s.railNormal = Math.max(0, Math.min(2, t.getInt("RailNormal")));
    s.countItems = t.getBoolean("CountItems");
    s.sensorMode = Math.max(0, Math.min(2, t.getInt("SensorMode")));
    s.pulseTicks = Math.max(2, Math.min(40, t.getInt("PulseTicks")));
    s.inputMode = Math.max(0, Math.min(2, t.getInt("InputMode")));
    s.outputFace = Direction.from3DDataValue(Math.max(0, Math.min(5, t.getInt("OutputFace"))));
    s.inputFace = Direction.from3DDataValue(Math.max(0, Math.min(5, t.getInt("InputFace"))));
    if (s.inputFace == s.outputFace) s.inputFace = s.outputFace.getOpposite();
    return s;
  }
}
