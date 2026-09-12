package com.zerotheabsolute.fieldemitters.energy;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

/** Native transactional Fabric energy storage with bounded internal withdrawals. */
public class EnergyStorage extends SnapshotParticipant<Integer> implements team.reborn.energy.api.EnergyStorage {
  private int amount;
  private final int capacity, receive, extract;
  public EnergyStorage(int capacity, int receive, int extract) {
    this.capacity = capacity; this.receive = receive; this.extract = extract;
  }
  public int getEnergyStored() { return amount; }
  public int getMaxEnergyStored() { return capacity; }
  public int receiveEnergy(int value, boolean simulate) {
    int accepted = Math.max(0, Math.min(value, Math.min(receive, capacity - amount)));
    if (!simulate) amount += accepted;
    return accepted;
  }
  public int extractEnergy(int value, boolean simulate) {
    int taken = Math.max(0, Math.min(value, Math.min(extract, amount)));
    if (!simulate) amount -= taken;
    return taken;
  }
  public void deserializeNBT(net.minecraft.nbt.IntTag tag) {
    amount = Math.max(0, Math.min(capacity, tag.getAsInt()));
  }
  @Override public long insert(long maxAmount, TransactionContext transaction) {
    if (maxAmount < 0) throw new IllegalArgumentException("Negative energy insertion");
    int accepted = receiveEnergy((int)Math.min(Integer.MAX_VALUE, maxAmount), true);
    if (accepted > 0) { updateSnapshots(transaction); amount += accepted; }
    return accepted;
  }
  @Override public long extract(long maxAmount, TransactionContext transaction) {
    if (maxAmount < 0) throw new IllegalArgumentException("Negative energy extraction");
    int taken = extractEnergy((int)Math.min(Integer.MAX_VALUE, maxAmount), true);
    if (taken > 0) { updateSnapshots(transaction); amount -= taken; }
    return taken;
  }
  @Override public long getAmount() { return amount; }
  @Override public long getCapacity() { return capacity; }
  @Override protected Integer createSnapshot() { return amount; }
  @Override protected void readSnapshot(Integer value) { amount = value; }
}
