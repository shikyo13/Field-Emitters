package com.zerotheabsolute.fieldemitters;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * A field cell that forms where a plant stands parks the plant instead of breaking it, and puts it
 * back when the cell is removed, provided its original space and support remain available.
 */
final class FieldVegetation {
  static final int EFFECTS_PER_REBUILD = 4;
  private static final int SPARK_COUNT = 4;
  /** Without shape updates neither half of a plant breaks, and nothing resting on it drops. */
  static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

  /** A parked plant, with the other half of a two-block plant {@code partnerDy} blocks away. */
  record Parked(BlockState state, BlockState partner, int partnerDy, Map<Integer, BlockState> column) {
    CompoundTag save() {
      var tag = new CompoundTag();
      tag.put("State", NbtUtils.writeBlockState(state));
      if (partner != null) {
        tag.put("Partner", NbtUtils.writeBlockState(partner));
        tag.putInt("PartnerDy", partnerDy);
      }
      var segments = new ListTag();
      column.forEach((offset, block) -> {
        var segment = new CompoundTag();
        segment.putInt("Dy", offset);
        segment.put("State", NbtUtils.writeBlockState(block));
        segments.add(segment);
      });
      if (!segments.isEmpty()) tag.put("Column", segments);
      return tag;
    }

    static Parked load(CompoundTag tag) {
      var blocks = BuiltInRegistries.BLOCK.asLookup();
      var state = NbtUtils.readBlockState(blocks, tag.getCompound("State"));
      if (state.isAir()) return null;
      var partner = tag.contains("Partner") ? NbtUtils.readBlockState(blocks, tag.getCompound("Partner")) : null;
      var column = new TreeMap<Integer, BlockState>();
      var segments = tag.getList("Column", Tag.TAG_COMPOUND);
      for (int i = 0; i < segments.size(); i++) {
        var segment = segments.getCompound(i);
        var block = NbtUtils.readBlockState(blocks, segment.getCompound("State"));
        if (segment.getInt("Dy") > 0 && block.is(Blocks.SUGAR_CANE))
          column.put(segment.getInt("Dy"), block);
      }
      return new Parked(state, partner == null || partner.isAir() ? null : partner,
          tag.getInt("PartnerDy"), Map.copyOf(column));
    }
  }

  private FieldVegetation() {}

  static boolean canPark(ServerLevel level, BlockPos pos, BlockState state) {
    return !state.isAir()
        && !state.hasBlockEntity()
        && state.getFluidState().isEmpty()
        && state.getCollisionShape(level, pos).isEmpty()
        && (state.getBlock() instanceof BushBlock
            || state.is(Blocks.VINE)
            || state.is(Blocks.GLOW_LICHEN)
            || state.is(Blocks.HANGING_ROOTS)
            || state.is(Blocks.SUGAR_CANE));
  }

  /** Lifts the plant, and both halves of a two-block plant, so a field cell can take its place. */
  static Parked lift(ServerLevel level, BlockPos pos, BlockState state, boolean effects) {
    BlockState partner = null;
    int dy = 0;
    if (state.getBlock() instanceof DoublePlantBlock) {
      dy = state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER ? 1 : -1;
      var other = level.getBlockState(pos.above(dy));
      if (other.is(state.getBlock())) {
        partner = other;
        level.setBlock(pos.above(dy), Blocks.AIR.defaultBlockState(), QUIET);
      }
    }
    var column = new TreeMap<Integer, BlockState>();
    if (state.is(Blocks.SUGAR_CANE)) {
      for (int offset = 1; pos.getY() + offset < level.getMaxBuildHeight(); offset++) {
        var above = pos.above(offset);
        var segment = level.getBlockState(above);
        if (!segment.is(Blocks.SUGAR_CANE)) break;
        column.put(offset, segment);
        level.setBlock(above, Blocks.AIR.defaultBlockState(), QUIET);
      }
    }
    level.setBlock(pos, Blocks.AIR.defaultBlockState(), QUIET);
    if (effects)
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
          pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5,
          SPARK_COUNT, .15, .25, .15, .02);
    return new Parked(state, partner, dy, Map.copyOf(column));
  }

  /**
   * Removes field cells together and returns the plants they parked. Restoring the whole set before
   * checking survival lets plants that rest on each other (both halves, cane columns, hanging vines)
   * come back intact. A plant that can no longer stand where it was drops as if broken.
   */
  static void release(ServerLevel level, Collection<BlockPos> cells) {
    if (cells.isEmpty()) return;
    var batch = new HashSet<>(cells);
    var restored = new ArrayList<BlockPos>();
    for (var pos : batch) {
      if (!(level.getBlockEntity(pos) instanceof FieldCell cell) || cell.parked == null) continue;
      var parked = cell.parked;
      var other = parked.partner() == null ? null : pos.above(parked.partnerDy());
      if (other != null
          && !level.getBlockState(other).isAir()
          && !(batch.contains(other)
              && level.getBlockEntity(other) instanceof FieldCell partnerCell
              && partnerCell.parked == null)) {
        // Something else now stands where the other half was.
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        Block.dropResources(parked.state(), level, pos);
        Block.dropResources(parked.partner(), level, pos);
        continue;
      }
      level.setBlock(pos, parked.state(), QUIET);
      restored.add(pos);
      if (other != null) {
        level.setBlock(other, parked.partner(), QUIET);
        restored.add(other);
      }
      for (var segment : new TreeMap<>(parked.column()).entrySet()) {
        var target = pos.above(segment.getKey());
        if (level.getBlockState(target).isAir()
            || batch.contains(target) && level.getBlockEntity(target) instanceof FieldCell remaining
                && remaining.parked == null) {
          level.setBlock(target, segment.getValue(), QUIET);
          restored.add(target);
        } else {
          Block.dropResources(segment.getValue(), level, pos);
        }
      }
    }
    for (var pos : batch)
      if (level.getBlockEntity(pos) instanceof FieldCell) level.removeBlock(pos, false);
    for (var pos : restored) {
      var state = level.getBlockState(pos);
      if (state.isAir()) continue;
      if (!state.canSurvive(level, pos)) level.destroyBlock(pos, true);
      else level.blockUpdated(pos, state.getBlock());
    }
  }
}
