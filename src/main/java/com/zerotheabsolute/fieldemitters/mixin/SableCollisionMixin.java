package com.zerotheabsolute.fieldemitters.mixin;

import com.zerotheabsolute.fieldemitters.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision", remap = false)
abstract class SableCollisionMixin {
  @Unique private static java.lang.reflect.Method fieldemitters$inverse;

  @Inject(method = "getSubLevelEntityCollisionShape", at = @At("HEAD"), cancellable = true)
  private static void fieldemitters$entityShape(Entity entity, Vector3dc center,
      @Coerce Object pose, BlockState state, @Coerce Object getter, BlockPos pos,
      @Coerce Object scratch, CallbackInfoReturnable<VoxelShape> result) {
    if (!(state.getBlock() instanceof FieldBlock) && !(state.getBlock() instanceof RailBlock)) return;
    try {
      if (fieldemitters$inverse == null)
        fieldemitters$inverse = Class.forName("dev.ryanhcode.sable.companion.math.Pose3dc")
            .getMethod("transformPositionInverse", Vec3.class);
      var local = (Vec3) fieldemitters$inverse.invoke(pose, new Vec3(center.x(), center.y(), center.z()));
      result.setReturnValue(state.getCollisionShape((BlockGetter) getter, pos,
          new FieldCollisionContext(entity, local)));
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException("Unsupported Sable coordinate API", exception);
    }
  }
}
