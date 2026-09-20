package com.zerotheabsolute.fieldemitters;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class FieldScriptApi {
  public FieldHandle get(ServerLevel level, int x, int y, int z) {
    return new FieldHandle(level, new BlockPos(x, y, z));
  }
  public java.util.List<String> presets() {
    return FieldPresets.ids().stream().map(Object::toString).sorted().toList();
  }
}
