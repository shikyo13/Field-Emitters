package com.zerotheabsolute.fieldemitters.network;

import net.minecraft.resources.ResourceLocation;

public interface FieldPayload {
    Type<? extends FieldPayload> type();
    record Type<T extends FieldPayload>(ResourceLocation id) {}
}
