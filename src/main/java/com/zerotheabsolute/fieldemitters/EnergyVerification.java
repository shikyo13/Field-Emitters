package com.zerotheabsolute.fieldemitters;

/** Operator-only, reversible test of the loader's public energy connection. */
public final class EnergyVerification {
  public static int run(net.minecraft.commands.CommandSourceStack source) {
    var level = source.getLevel();
    var e = FieldNetwork.loaded(level).stream().min(java.util.Comparator.comparingDouble(be -> be.getBlockPos().distToCenterSqr(source.getPosition()))).orElseThrow(() -> new IllegalStateException("Stand near a loaded emitter"));
    int original = e.energy.getEnergyStored();
    int amount = Math.min(100, FieldConfig.transfer());
    try {
      e.energy.deserializeNBT(net.minecraft.nbt.IntTag.valueOf(0));
      var input = team.reborn.energy.api.EnergyStorage.SIDED.find(level, e.getBlockPos(), net.minecraft.core.Direction.NORTH);
      require(input != null, "Missing Fabric energy lookup");
      try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
        require(input.insert(amount, tx) == amount, "Fabric insert failed");
      }
      require(e.energy.getEnergyStored() == 0, "Aborted transaction retained energy");
      try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
        require(input.insert(amount, tx) == amount, "Fabric insert failed"); tx.commit();
      }
      require(e.energy.getEnergyStored() == amount, "Committed transaction lost energy");
      try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
        require(input.extract(amount, tx) == amount, "Fabric extract failed");
      }
      require(e.energy.getEnergyStored() == amount, "Aborted extraction lost energy");

      source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("PASS: native energy input and rollback/simulation"), false);
      return 1;
    } finally { e.energy.deserializeNBT(net.minecraft.nbt.IntTag.valueOf(original)); e.sync(); }
  }
  private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
