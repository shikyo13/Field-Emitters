package com.zerotheabsolute.fieldemitters.mixin;
import com.zerotheabsolute.fieldemitters.EmitterEntity;
import java.util.List;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
/**
 * MC-112730: vanilla lists a block entity that renders off screen both with its section and in the
 * global list, so the field draws twice while the emitter's section is visible. Forge and NeoForge
 * patch this; emitters stay in the global list only.
 */
@Mixin(SectionCompiler.class)
abstract class EmitterSectionMixin {
  @Redirect(method = "handleBlockEntity", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0))
  private boolean globalOnly(List<Object> sectionList, Object blockEntity) {
    return !(blockEntity instanceof EmitterEntity) && sectionList.add(blockEntity);
  }
}
