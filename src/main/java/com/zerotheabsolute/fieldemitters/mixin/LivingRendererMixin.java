package com.zerotheabsolute.fieldemitters.mixin;
import com.zerotheabsolute.fieldemitters.client.FizzleDeaths;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntityRenderer.class)
abstract class LivingRendererMixin {
  @Inject(method="render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at=@At("HEAD"), cancellable=true)
  private void hideFizzle(LivingEntity entity, float yaw, float delta, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
    if(FizzleDeaths.hidden(entity)) ci.cancel();
  }
}
