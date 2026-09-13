package com.zerotheabsolute.fieldemitters;

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

public final class TowerBlock extends BaseEntityBlock {
  public static final int HEIGHT = 7;
  public static final net.minecraft.world.level.block.state.properties.BooleanProperty ACTIVE =
      net.minecraft.world.level.block.state.properties.BooleanProperty.create("active");
  public static final net.minecraft.world.level.block.state.properties.BooleanProperty LIGHT =
      net.minecraft.world.level.block.state.properties.BooleanProperty.create("light");
  public static final IntegerProperty SECTION = IntegerProperty.create("section", 0, HEIGHT - 1);

  public TowerBlock(Properties p) {
    super(p);
    registerDefaultState(
        stateDefinition.any().setValue(SECTION, 0).setValue(ACTIVE, false).setValue(LIGHT, false));
  }

  @Override
  public boolean canEntityDestroy(
      BlockState state, BlockGetter level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
    return entity instanceof Player;
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
    for (int i = 0; i < HEIGHT; i++)
      if (c.getClickedPos().getY() + i >= c.getLevel().getMaxBuildHeight()
          || !c.getLevel().getBlockState(c.getClickedPos().above(i)).canBeReplaced(c)) return null;
    return defaultBlockState();
  }

  public void setPlacedBy(Level l, BlockPos p, BlockState s, LivingEntity entity, ItemStack stack) {
    if (!l.isClientSide) {
      for (int i = 1; i < HEIGHT; i++) l.setBlock(p.above(i), s.setValue(SECTION, i), 3);
      if (l.getBlockEntity(p) instanceof EmitterEntity e && entity != null) {
        e.owner = entity.getUUID();
        e.placedAt = l.getGameTime();
        e.setChanged();
      }
    }
  }

  public static BlockPos base(BlockPos p, BlockState s) {
    return p.below(s.getValue(SECTION));
  }


  public InteractionResult use(
      BlockState s, Level l, BlockPos p, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
    if (!player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
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

  public void onRemove(BlockState old, Level l, BlockPos p, BlockState next, boolean moving) {
    if (!old.is(next.getBlock()) && !l.isClientSide) {
      BlockPos b = base(p, old);
      for (int i = 0; i < HEIGHT; i++) {
        BlockPos part = b.above(i);
        if (!part.equals(p) && l.getBlockState(part).is(this)) l.removeBlock(part, false);
      }
    }
    super.onRemove(old, l, p, next, moving);
  }
}
