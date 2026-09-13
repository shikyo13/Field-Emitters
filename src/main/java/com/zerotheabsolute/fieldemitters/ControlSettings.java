package com.zerotheabsolute.fieldemitters;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

public final class ControlSettings {
  public com.zeromods.core.animation.SphereFormation projection = com.zeromods.core.animation.SphereFormation.LASER_CURTAIN;
  public int sphereRadius = SphereField.DEFAULT_RADIUS;
  public boolean dome = true;
  public CheckpointSettings checkpoint = new CheckpointSettings();
  public EntityFilter barrier = new EntityFilter(), sensor = new EntityFilter(), damage = new EntityFilter();
  public final com.zeromods.core.filter.DirectionalRules<Direction,EntityFilter> barrierDirections = new com.zeromods.core.filter.DirectionalRules<>();
  public final com.zeromods.core.filter.DirectionalRules<Direction,EntityFilter> sensorDirections = new com.zeromods.core.filter.DirectionalRules<>();
  public final com.zeromods.core.filter.DirectionalRules<Direction,EntityFilter> damageDirections = new com.zeromods.core.filter.DirectionalRules<>();
  public boolean damageEnabled = false, fizzleEffects = true;
  public float damageAmount = 2;
  public int damageInterval = 20;
  public int pattern = 0, formation = 0, particleColor = 0xFF55DD;
  public boolean sounds = true, powerSounds = true, impactSounds = true, damageSounds = true;
  public int soundStyle = 0;
  public EntityFilter damage(Direction movement) { return damageDirections.resolve(movement, damage); }
  public boolean damages(net.minecraft.world.entity.Entity entity, java.util.UUID owner, Direction movement) {
    var rule = damage(movement);
    return damageEnabled && damageAmount > 0 && rule.direction(movement) && rule.matches(entity, owner);
  }
  public EntityFilter barrier(Direction movement) { return barrierDirections.resolve(movement,barrier); }
  public EntityFilter sensor(Direction movement) { return sensorDirections.resolve(movement,sensor); }
  public boolean blocks(net.minecraft.world.entity.Entity entity, java.util.UUID owner, Direction movement) {
    var rule=barrier(movement);return rule.direction(movement) && rule.matches(entity,owner);
  }
  public boolean detects(net.minecraft.world.entity.Entity entity, java.util.UUID owner, Direction movement) {
    var rule=sensor(movement);return rule.direction(movement) && rule.matches(entity,owner);
  }
  public java.util.List<EntityFilter> filters() {
    var result=new java.util.ArrayList<EntityFilter>();result.add(barrier);result.add(sensor);result.add(damage);
    result.addAll(barrierDirections.overrides().values());result.addAll(sensorDirections.overrides().values());result.addAll(damageDirections.overrides().values());result.add(checkpoint.players);result.add(checkpoint.items);result.addAll(checkpoint.directions.overrides().values());return result;
  }
  public boolean light = true, visible = true, animation = true, countItems = false;
  public int railNormal = 2;
  public int sensorMode = 0, pulseTicks = 4, inputMode = 0;
  public Direction outputFace = Direction.DOWN, inputFace = Direction.UP;

  public ControlSettings() {
    barrier.exemptOwner = true;
    damage.exemptOwner = true;
    sensor.groups = 31;
  }

  public CompoundTag save() {
    var t = new CompoundTag();
    t.putString("Projection", projection.name());
    t.putInt("SphereRadius",sphereRadius); t.putBoolean("Dome",dome);
    t.put("Checkpoint",checkpoint.save());
    t.put("Barrier", barrier.save());
    t.put("Sensor", sensor.save());
    t.put("Damage", damage.save());
    var damageRules = new CompoundTag();
    damageDirections.overrides().forEach((direction,rule)->damageRules.put(direction.getName(),rule.save()));
    t.put("DamageDirections", damageRules);
    t.putBoolean("DamageEnabled", damageEnabled);
    t.putFloat("DamageAmount", damageAmount);
    t.putInt("DamageInterval", damageInterval);
    t.putBoolean("FizzleEffects", fizzleEffects);
    t.putInt("Pattern", pattern); t.putInt("Formation", formation); t.putInt("ParticleColor", particleColor);
    t.putBoolean("Sounds", sounds); t.putBoolean("PowerSounds", powerSounds);
    t.putBoolean("ImpactSounds", impactSounds); t.putBoolean("DamageSounds", damageSounds);
    t.putInt("SoundStyle", soundStyle);
    var blocking=new CompoundTag();var detection=new CompoundTag();
    barrierDirections.overrides().forEach((direction,rule)->blocking.put(direction.getName(),rule.save()));
    sensorDirections.overrides().forEach((direction,rule)->detection.put(direction.getName(),rule.save()));
    t.put("BarrierDirections",blocking);t.put("SensorDirections",detection);
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
    s.projection = com.zeromods.core.animation.SphereFormation.fromId(t.getString("Projection"));
    s.sphereRadius=t.contains("SphereRadius")?Math.max(SphereField.MIN_RADIUS,Math.min(SphereField.MAX_RADIUS,t.getInt("SphereRadius"))):SphereField.DEFAULT_RADIUS;
    s.dome=!t.contains("Dome")||t.getBoolean("Dome");
    if(t.contains("Checkpoint"))s.checkpoint=CheckpointSettings.load(t.getCompound("Checkpoint"));
    s.barrier = EntityFilter.load(t.getCompound("Barrier"));
    s.sensor = EntityFilter.load(t.getCompound("Sensor"));
    if (t.contains("Damage", 10)) s.damage = EntityFilter.load(t.getCompound("Damage"));
    var damageRules = t.getCompound("DamageDirections");
    for (var direction : Direction.values())
      if (damageRules.contains(direction.getName(),10))
        s.damageDirections.set(direction, EntityFilter.load(damageRules.getCompound(direction.getName())));
    s.damageEnabled = t.getBoolean("DamageEnabled");
    float amount = t.contains("DamageAmount") ? t.getFloat("DamageAmount") : 2;
    s.damageAmount = Float.isFinite(amount) ? Math.max(0, Math.min(1000, amount)) : 0;
    s.damageInterval = t.contains("DamageInterval") ? Math.max(10, Math.min(200, t.getInt("DamageInterval"))) : 20;
    s.fizzleEffects = !t.contains("FizzleEffects") || t.getBoolean("FizzleEffects");
    s.pattern = Math.max(0, Math.min(3, t.getInt("Pattern")));
    s.formation = Math.max(0, Math.min(2, t.getInt("Formation")));
    s.particleColor = t.contains("ParticleColor") ? t.getInt("ParticleColor") & 0xFFFFFF : 0xFF55DD;
    s.sounds = !t.contains("Sounds") || t.getBoolean("Sounds");
    s.powerSounds = !t.contains("PowerSounds") || t.getBoolean("PowerSounds");
    s.impactSounds = !t.contains("ImpactSounds") || t.getBoolean("ImpactSounds");
    s.damageSounds = !t.contains("DamageSounds") || t.getBoolean("DamageSounds");
    s.soundStyle = Math.max(0, Math.min(2, t.getInt("SoundStyle")));
    var blocking=t.getCompound("BarrierDirections");var detection=t.getCompound("SensorDirections");
    for(var direction:Direction.values()) {
      if(blocking.contains(direction.getName(),10))s.barrierDirections.set(direction,EntityFilter.load(blocking.getCompound(direction.getName())));
      if(detection.contains(direction.getName(),10))s.sensorDirections.set(direction,EntityFilter.load(detection.getCompound(direction.getName())));
    }
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
