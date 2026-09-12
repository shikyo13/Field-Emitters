package com.zerotheabsolute.fieldemitters;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class RailBlock extends BaseEntityBlock {
  public static final DirectionProperty FACING = BlockStateProperties.FACING;
  public static final BooleanProperty ACTIVE = EmitterBlock.ACTIVE, LIGHT = EmitterBlock.LIGHT;

  public RailBlock(Properties p) {
    super(p);
    registerDefaultState(
        stateDefinition
            .any()
            .setValue(FACING, Direction.NORTH)
            .setValue(ACTIVE, false)
            .setValue(LIGHT, false));
  }


  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
    b.add(FACING, ACTIVE, LIGHT);
  }

  public BlockState getStateForPlacement(BlockPlaceContext c) {
    return defaultBlockState().setValue(FACING, c.getClickedFace());
  }

  public RenderShape getRenderShape(BlockState s) {
    return RenderShape.MODEL;
  }

  public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    return switch (s.getValue(FACING)) {
      case NORTH -> Block.box(0, 0, 13, 16, 16, 16);
      case SOUTH -> Block.box(0, 0, 0, 16, 16, 3);
      case EAST -> Block.box(0, 0, 0, 3, 16, 16);
      case WEST -> Block.box(13, 0, 0, 16, 16, 16);
      case UP -> Block.box(0, 0, 0, 16, 3, 16);
      case DOWN -> Block.box(0, 13, 0, 16, 16, 16);
    };
  }

  public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    var hardware = getShape(s, l, p, c);
    if (c instanceof EntityCollisionContext ec
        && ec.getEntity() != null
        && l.getBlockEntity(p) instanceof EmitterEntity e) {
      var field = FieldBlock.collision(e, ec.getEntity(), p);
      // The far endpoint can own no links; find its loaded source through its network.
      for (var part : FieldNetwork.members(e))
        if (part != e && !part.isRemoved())
          field = Shapes.or(field, FieldBlock.collision(part, ec.getEntity(), p));
      return Shapes.or(hardware, field);
    }
    return hardware;
  }

  public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
    return new EmitterEntity(p, s);
  }

  public void setPlacedBy(Level l, BlockPos p, BlockState s, LivingEntity entity, ItemStack stack) {
    if (l.getBlockEntity(p) instanceof EmitterEntity e && entity != null) {
      e.owner = entity.getUUID();
      e.placedAt = l.getGameTime();
      e.setChanged();
    }
  }

  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level l, BlockState s, BlockEntityType<T> t) {
    return !l.isClientSide
        ? createTickerHelper(
            t, FieldEmitters.EMITTER_BE.get(), (world, pos, state, e) -> FieldNetwork.add(e))
        : null;
  }


  public InteractionResult use(
      BlockState s, Level l, BlockPos p, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
    if (!player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
    if (l.isClientSide && l.getBlockEntity(p) instanceof EmitterEntity e)
      FieldControls.open.accept(e);
    return InteractionResult.SUCCESS;
  }

  public boolean isSignalSource(BlockState s) {
    return true;
  }

  public int getSignal(BlockState s, BlockGetter l, BlockPos p, Direction side) {
    return l.getBlockEntity(p) instanceof EmitterEntity e
            && side == e.controls.outputFace.getOpposite()
        ? e.outputSignal
        : 0;
  }
}
