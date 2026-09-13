package com.zerotheabsolute.fieldemitters.mixin;
import com.zerotheabsolute.fieldemitters.HardwareProtection;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Explosion.class)
abstract class HardwareExplosionMixin {
  @Shadow @Final private Level level;
  @Shadow @Final private ObjectArrayList<BlockPos> toBlow;
  @Inject(method="finalizeExplosion", at=@At("HEAD"))
  private void protectHardware(boolean particles, CallbackInfo ci) {
    toBlow.removeIf(pos -> HardwareProtection.protectedBlock(level.getBlockState(pos)));
  }
}
