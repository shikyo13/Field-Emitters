package com.zerotheabsolute.fieldemitters.mixin;

import com.zerotheabsolute.fieldemitters.PhasingCollision;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class PhasingCollisionMixin {
  @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
  private Vec3 fieldemitters$limitPhasing(Vec3 movement) {
    return PhasingCollision.limit((Entity) (Object) this, movement);
  }
}
