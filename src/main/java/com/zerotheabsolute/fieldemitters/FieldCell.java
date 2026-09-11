package com.zerotheabsolute.fieldemitters;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class FieldCell extends BlockEntity {
  public BlockPos source = BlockPos.ZERO;

  public FieldCell(BlockPos p, BlockState s) {
    super(FieldEmitters.CELL_BE.get(), p, s);
  }

  protected void saveAdditional(CompoundTag t, HolderLookup.Provider r) {
    super.saveAdditional(t, r);
    t.putLong("Source", source.asLong());
  }

  protected void loadAdditional(CompoundTag t, HolderLookup.Provider r) {
    super.loadAdditional(t, r);
    source = BlockPos.of(t.getLong("Source"));
  }

  public CompoundTag getUpdateTag(HolderLookup.Provider r) {
    return saveWithoutMetadata(r);
  }

  public ClientboundBlockEntityDataPacket getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }
}
