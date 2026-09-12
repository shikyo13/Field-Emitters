package com.zerotheabsolute.fieldemitters;

/** Operator-only, reversible test of the loader's public energy connection. */
public final class EnergyVerification {
  public static int run(net.minecraft.commands.CommandSourceStack source) {
    var level = source.getLevel();
    var e = FieldNetwork.loaded(level).stream().min(java.util.Comparator.comparingDouble(be -> be.getBlockPos().distToCenterSqr(source.getPosition()))).orElseThrow(() -> new IllegalStateException("Stand near a loaded emitter"));
    int original = e.energy.getEnergyStored();
    int amount = Math.min(100, FieldConfig.transfer());
    try {
      e.energy.deserializeNBT(level.registryAccess(), net.minecraft.nbt.IntTag.valueOf(0));
      var input = e.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY, net.minecraft.core.Direction.NORTH).orElseThrow(() -> new IllegalStateException("Missing FE capability"));
      require(input.receiveEnergy(amount, true) == amount && e.energy.getEnergyStored() == 0, "FE simulation mutated storage");
      require(input.receiveEnergy(amount, false) == amount && e.energy.getEnergyStored() == amount, "FE input failed");
      require(input.extractEnergy(amount, true) == amount && e.energy.getEnergyStored() == amount, "FE extraction simulation mutated storage");

      source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("PASS: native energy input and rollback/simulation"), false);
      return 1;
    } finally { e.energy.deserializeNBT(level.registryAccess(), net.minecraft.nbt.IntTag.valueOf(original)); e.sync(); }
  }
  private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
