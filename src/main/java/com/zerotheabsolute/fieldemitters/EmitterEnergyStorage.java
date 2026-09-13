package com.zerotheabsolute.fieldemitters;

import com.zerotheabsolute.fieldemitters.energy.EnergyStorage;

/** Cable transfer limits do not limit the field's consumption of already stored energy. */
public final class EmitterEnergyStorage extends EnergyStorage {
  private final Runnable changed;

  EmitterEnergyStorage(int capacity, int transfer, Runnable changed) {
    super(capacity, transfer, transfer);
    this.changed = changed;
  }

  @Override
  public int receiveEnergy(int amount, boolean simulate) {
    int received = super.receiveEnergy(amount, simulate);
    if (received > 0 && !simulate) changed.run();
    return received;
  }

  @Override
  public int extractEnergy(int amount, boolean simulate) {
    int extracted = super.extractEnergy(amount, simulate);
    if (extracted > 0 && !simulate) changed.run();
    return extracted;
  }

  @Override protected void onFinalCommit() { changed.run(); }

  int consume(int amount) {
    int used = Math.min(amount, Math.max(0, amount));
    amount -= used;
    if (used > 0) changed.run();
    return used;
  }
}
