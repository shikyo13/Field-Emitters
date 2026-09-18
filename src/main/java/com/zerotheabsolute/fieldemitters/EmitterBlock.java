package com.zerotheabsolute.fieldemitters;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class EmitterBlock extends BaseEntityBlock {
  public static final net.minecraft.world.level.block.state.properties.BooleanProperty ACTIVE =
      net.minecraft.world.level.block.state.properties.BooleanProperty.create("active");
  public static final net.minecraft.world.level.block.state.properties.BooleanProperty LIGHT =
      net.minecraft.world.level.block.state.properties.BooleanProperty.create("light");
  public static final IntegerProperty SECTION = IntegerProperty.create("section", 0, 4);

  public EmitterBlock(Properties p) {
    super(p);
    registerDefaultState(
        stateDefinition.any().setValue(SECTION, 0).setValue(ACTIVE, false).setValue(LIGHT, false));
  }


  protected MapCodec<? extends BaseEntityBlock> codec() {
    return simpleCodec(EmitterBlock::new);
  }

  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
    b.add(SECTION, ACTIVE, LIGHT);
  }

  public RenderShape getRenderShape(BlockState s) {
    return RenderShape.MODEL;
  }

  public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    return s.getValue(SECTION) == 0
        ? Block.box(1, 0, 1, 15, 16, 15)
        : Block.box(4, 0, 4, 12, 16, 12);
  }

  public BlockState getStateForPlacement(BlockPlaceContext c) {
    for (int i = 0; i < 5; i++)
      if (c.getClickedPos().getY() + i >= c.getLevel().getMaxBuildHeight()
          || !c.getLevel().getBlockState(c.getClickedPos().above(i)).canBeReplaced(c)) return null;
    return defaultBlockState();
  }

  public void setPlacedBy(Level l, BlockPos p, BlockState s, LivingEntity entity, ItemStack stack) {
    if (!l.isClientSide) {
      for (int i = 1; i < 5; i++) l.setBlock(p.above(i), s.setValue(SECTION, i), 3);
      if (l.getBlockEntity(p) instanceof EmitterEntity e && entity != null) {
        e.owner = entity.getUUID();
        e.placedAt = l.getGameTime();
        e.adoptPending = true;
        e.setChanged();
      }
    }
  }

  public static BlockPos base(BlockPos p, BlockState s) {
    if(s.is(FieldEmitters.TOWER.get()))return TowerBlock.base(p,s);
    return s.hasProperty(SECTION) ? p.below(s.getValue(SECTION)) : p;
  }

  @Override
  protected net.minecraft.world.ItemInteractionResult useItemOn(
      ItemStack stack,
      BlockState state,
      Level level,
      BlockPos pos,
      Player player,
      net.minecraft.world.InteractionHand hand,
      BlockHitResult hit) {
    // Nonempty hands must reach the item's own interaction (tuner or block placement).
    return stack.isEmpty()
        ? net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        : net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
  }

  protected InteractionResult useWithoutItem(
      BlockState s, Level l, BlockPos p, Player player, BlockHitResult hit) {
    if (l.isClientSide && l.getBlockEntity(base(p, s)) instanceof EmitterEntity e)
      FieldControls.open.accept(e);
    return InteractionResult.SUCCESS;
  }

  public boolean isSignalSource(BlockState state) {
    return state.getValue(SECTION) == 0;
  }

  public int getSignal(BlockState s, BlockGetter l, BlockPos p, net.minecraft.core.Direction side) {
    return l.getBlockEntity(p) instanceof EmitterEntity e
            && side == e.controls.outputFace.getOpposite()
        ? e.outputSignal
        : 0;
  }

  public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
    return s.getValue(SECTION) == 0 ? new EmitterEntity(p, s) : null;
  }

  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level l, BlockState s, BlockEntityType<T> type) {
    return !l.isClientSide
        ? createTickerHelper(
            type, FieldEmitters.EMITTER_BE.get(), (world, pos, state, be) -> FieldNetwork.add(be))
        : null;
  }

  protected void onRemove(BlockState old, Level l, BlockPos p, BlockState next, boolean moving) {
    if (!old.is(next.getBlock()) && !l.isClientSide) {
      BlockPos b = base(p, old);
      for (int i = 0; i < 5; i++) {
        BlockPos part = b.above(i);
        if (!part.equals(p) && l.getBlockState(part).is(this)) l.removeBlock(part, false);
      }
    }
    super.onRemove(old, l, p, next, moving);
  }
}
