package com.zerotheabsolute.fieldemitters.client;

import com.zerotheabsolute.fieldemitters.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import com.zerotheabsolute.fieldemitters.network.NativeNetwork;

public final class TunerKeys {
  public static final KeyMapping OPEN =
      new KeyMapping(
          "key.fieldemitters.tuner",
          org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN,
          "key.categories.fieldemitters");

  public static void tick(net.minecraft.client.Minecraft event) {
    var mc = Minecraft.getInstance();
    while (OPEN.consumeClick()) {
      if (mc.player == null
          || mc.level == null
          || mc.screen != null
          || !FieldControls.hasTuner(mc.player)) continue;
      BlockPos pos = null;
      if (mc.hitResult instanceof BlockHitResult hit) {
        var base = EmitterBlock.base(hit.getBlockPos(), mc.level.getBlockState(hit.getBlockPos()));
        if (mc.level.getBlockEntity(base) instanceof EmitterEntity) pos = base;
      }
      NativeNetwork.sendToServer(
          new FieldControls.RemoteRequest(pos == null, pos == null ? BlockPos.ZERO : pos));
    }
  }
}
