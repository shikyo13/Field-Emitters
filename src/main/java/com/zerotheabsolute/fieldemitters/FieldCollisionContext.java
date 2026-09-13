package com.zerotheabsolute.fieldemitters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.EntityCollisionContext;

/** Entity identity stays unchanged while a moving structure supplies local contact coordinates. */
public final class FieldCollisionContext extends EntityCollisionContext {
  public final Vec3 center;
  public FieldCollisionContext(Entity entity, Vec3 center) {
    super(entity);
    this.center = center;
  }
}
