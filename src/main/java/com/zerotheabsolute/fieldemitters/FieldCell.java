package com.zerotheabsolute.fieldemitters;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class FieldCell extends BlockEntity {
  public BlockPos source = BlockPos.ZERO;
  /** The plant this cell displaced, put back when the cell is removed. */
  FieldVegetation.Parked parked;

  public FieldCell(BlockPos p, BlockState s) {
    super(FieldEmitters.CELL_BE.get(), p, s);
  }

  protected void saveAdditional(CompoundTag t, HolderLookup.Provider r) {
    super.saveAdditional(t, r);
    t.putLong("Source", source.asLong());
    var offset = source.subtract(worldPosition);
    t.putIntArray("SourceOffset", new int[] {offset.getX(), offset.getY(), offset.getZ()});
    if (parked != null) t.put("Parked", parked.save());
  }

  protected void loadAdditional(CompoundTag t, HolderLookup.Provider r) {
    super.loadAdditional(t, r);
    int[] offset = t.getIntArray("SourceOffset");
    source = offset.length == 3 ? worldPosition.offset(offset[0], offset[1], offset[2])
        : BlockPos.of(t.getLong("Source"));
    parked = t.contains("Parked", 10) ? FieldVegetation.Parked.load(t.getCompound("Parked")) : null;
  }

  public CompoundTag getUpdateTag(HolderLookup.Provider r) {
    return saveWithoutMetadata(r);
  }

  public ClientboundBlockEntityDataPacket getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }
}
