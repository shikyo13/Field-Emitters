package com.zerotheabsolute.fieldemitters.mixin;
import com.zerotheabsolute.fieldemitters.EntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Entity.class)
abstract class EntityDataMixin implements EntityData {
  @Unique private CompoundTag fieldemitters$data = new CompoundTag();
  @Override public CompoundTag fieldData() { return fieldemitters$data; }
  @Inject(method="load", at=@At("TAIL"))
  private void readFieldData(CompoundTag tag, CallbackInfo ci) { fieldemitters$data = tag.getCompound("FieldEmittersData").copy(); }
  @Inject(method="saveWithoutId", at=@At("RETURN"))
  private void writeFieldData(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
    if (!fieldemitters$data.isEmpty()) cir.getReturnValue().put("FieldEmittersData", fieldemitters$data.copy());
  }
}
