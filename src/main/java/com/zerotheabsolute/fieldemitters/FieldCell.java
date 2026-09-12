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

  protected void saveAdditional(CompoundTag t) {
    super.saveAdditional(t);
    t.putLong("Source", source.asLong());
  }

  public void load(CompoundTag t) {
    super.load(t);
    source = BlockPos.of(t.getLong("Source"));
  }

  public CompoundTag getUpdateTag() {
    return saveWithoutMetadata();
  }

  public ClientboundBlockEntityDataPacket getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }
}
