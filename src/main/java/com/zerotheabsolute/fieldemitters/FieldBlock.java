package com.zerotheabsolute.fieldemitters;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.*;

public final class FieldBlock extends BaseEntityBlock {
  public static final BooleanProperty LIT = BooleanProperty.create("lit");
  public static final BooleanProperty X_AXIS = BooleanProperty.create("x_axis");

  public FieldBlock(Properties p) {
    super(p);
    registerDefaultState(stateDefinition.any().setValue(X_AXIS, true).setValue(LIT, false));
  }


  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
    b.add(X_AXIS, LIT);
  }

  public RenderShape getRenderShape(BlockState s) {
    return RenderShape.INVISIBLE;
  }

  public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
    return new FieldCell(p, s);
  }

  public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    return Shapes.empty();
  }

  public static boolean blocks(EmitterEntity e, Entity entity) {
    return e.controls.barrier.matches(entity, e.owner);
  }

  public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    if (!(c instanceof EntityCollisionContext ec)
        || !(l.getBlockEntity(p) instanceof FieldCell cell)
        || !(l.getBlockEntity(cell.source) instanceof EmitterEntity e)
        || !e.powered
        || ec.getEntity() == null) return Shapes.empty();
    return collision(e, ec.getEntity(), p, c instanceof FieldCollisionContext local
        ? local.center : ec.getEntity().getBoundingBox().getCenter());
  }

  public static VoxelShape collision(EmitterEntity e, Entity entity, BlockPos p) {
    return entity == null ? Shapes.empty() : collision(e, entity, p, entity.getBoundingBox().getCenter());
  }

  public static VoxelShape collision(EmitterEntity e, Entity entity, BlockPos p, net.minecraft.world.phys.Vec3 center) {
    if (!e.powered || entity == null) return Shapes.empty();
    if(e.isTower())return SphereField.collision(e,entity,p,center);
    for (var link : e.links) {
      var settings = e.settings(link);
      int index =
          (p.getX() - e.getBlockPos().getX()) * link.dx()
              + (p.getZ() - e.getBlockPos().getZ()) * link.dz()
              + (p.getY() - e.getBlockPos().getY()) * link.dy();
      if (index < (link.rail() ? 0 : 1)
          || index > (link.rail() ? link.length() : link.length() - 1)) continue;
      var expected = link.cell(e.getBlockPos(), index, 0);
      if (p.getX() != expected.getX()
          || p.getZ() != expected.getZ()
          || p.getY() < expected.getY()
          || p.getY() >= expected.getY() + link.height()) continue;
      if (e.getLevel().getGameTime() - e.transition < e.controls.linkFormationTicks(index)) continue;
      var movement = FieldContact.movement(e, link, entity, center);
      if (!settings.blocks(entity, e.owner, movement)
          && !FieldCheckpoint.blocks(e, settings, entity, movement)) continue;
      return switch (link.normal()) {
        case X -> Block.box(6.5, 0, 0, 9.5, 16, 16);
        case Y -> Block.box(0, 15, 0, 16, 16, 16);
        case Z -> Block.box(0, 0, 6.5, 16, 16, 9.5);
      };
    }
    return Shapes.empty();
  }

  public void tick(BlockState s, ServerLevel l, BlockPos p, RandomSource random) {
    if (!(l.getBlockEntity(p) instanceof FieldCell c)
        || !l.hasChunkAt(c.source)
        || !(l.getBlockEntity(c.source) instanceof EmitterEntity e)
        || !e.cells.contains(p)) l.removeBlock(p, false);
    else l.scheduleTick(p, this, 40);
  }
}
