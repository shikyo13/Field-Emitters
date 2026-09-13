package com.zerotheabsolute.fieldemitters.mixin;
import com.zerotheabsolute.fieldemitters.HardwareProtection;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Level.class)
abstract class HardwareMobMixin {
  @Inject(method="destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z", at=@At("HEAD"), cancellable=true)
  private void protectHardware(BlockPos pos, boolean drops, Entity source, int depth, CallbackInfoReturnable<Boolean> cir) {
    if(source instanceof Mob && HardwareProtection.protectedBlock(((Level)(Object)this).getBlockState(pos))) cir.setReturnValue(false);
  }
}
