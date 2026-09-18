package com.zerotheabsolute.fieldemitters;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.*;

public final class FieldBlock extends BaseEntityBlock implements LiquidBlockContainer {
  /** Player mode in which listed players and badge holders are the ones allowed through. */
  private static final int PASSES_WHEN_LISTED = 2;

  public static final BooleanProperty LIT = BooleanProperty.create("lit");
  public static final BooleanProperty X_AXIS = BooleanProperty.create("x_axis");

  public FieldBlock(Properties p) {
    super(p);
    registerDefaultState(stateDefinition.any().setValue(X_AXIS, true).setValue(LIT, false));
  }

  protected MapCodec<? extends BaseEntityBlock> codec() {
    return simpleCodec(FieldBlock::new);
  }

  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
    b.add(X_AXIS, LIT);
  }

  /**
   * Field cells are not solid, so without this a fluid would treat a cell as free space, destroy it
   * and take its place. The rebuild only refills air, so the hole would be permanent. Returning
   * false stops water and lava at the field instead.
   */
  public boolean canPlaceLiquid(
      net.minecraft.world.entity.player.Player player,
      BlockGetter l,
      BlockPos p,
      BlockState s,
      net.minecraft.world.level.material.Fluid fluid) {
    return false;
  }

  public boolean placeLiquid(
      net.minecraft.world.level.LevelAccessor l,
      BlockPos p,
      BlockState s,
      net.minecraft.world.level.material.FluidState fluid) {
    return false;
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

  /**
   * A mount and everything riding it cross as one body. The field stops the group when it stops any
   * member, so a blocked player cannot ride through on an unblocked horse or boat. A rider who
   * holds passage rights carries the group through instead, so the owner and badge holders are not
   * stopped by their own mount.
   */
  public static boolean blocked(
      EmitterEntity e, ControlSettings settings, Entity entity, Direction movement) {
    return blocked(e, settings, entity, movement, null);
  }

  public static boolean blocked(
      EmitterEntity e,
      ControlSettings settings,
      Entity entity,
      Direction movement,
      Direction inward) {
    if (!entity.isVehicle() && !entity.isPassenger())
      return stops(e, settings, entity, movement, inward);
    var root = entity.getRootVehicle();
    var riders = new java.util.ArrayList<Entity>();
    riders.add(root);
    root.getIndirectPassengers().forEach(riders::add);
    // A checkpoint holds the group whatever else is true: the contraband is aboard either way.
    for (var rider : riders)
      if (FieldCheckpoint.blocks(e, settings, rider, movement, inward)) return true;
    boolean stopped = false;
    for (var rider : riders)
      if (settings.blocks(rider, e.owner, movement, inward)) {
        stopped = true;
        break;
      }
    if (!stopped) return false;
    var rule = settings.barrierRule(movement, inward);
    for (var rider : riders) if (permitted(rule, rider, e.owner)) return false;
    return true;
  }

  /**
   * Whether this rider may take the group through: the exempt owner, or a listed player or access
   * badge holder while the blocking filter lets listed players pass. Simply falling outside the
   * filter is not passage rights, so an ordinary player cannot ferry a blocked mob across.
   */
  private static boolean permitted(EntityFilter rule, Entity entity, java.util.UUID owner) {
    if (!(entity instanceof net.minecraft.world.entity.player.Player player)
        || player.isSpectator()) return false;
    if (rule.exemptOwner && owner != null && player.getUUID().equals(owner)) return true;
    if (rule.playerMode != PASSES_WHEN_LISTED) return false;
    return rule.playerList.containsKey(player.getUUID())
        || BadgeAccess.matches(player, owner, rule.accessGroups);
  }

  private static boolean stops(
      EmitterEntity e,
      ControlSettings settings,
      Entity entity,
      Direction movement,
      Direction inward) {
    return settings.blocks(entity, e.owner, movement, inward)
        || FieldCheckpoint.blocks(e, settings, entity, movement, inward);
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
      if (!blocked(e, settings, entity, movement, link.inward())) continue;
      return switch (link.normal()) {
        case X -> Block.box(6.5, 0, 0, 9.5, 16, 16);
        case Y -> Block.box(0, 15, 0, 16, 16, 16);
        case Z -> Block.box(0, 0, 6.5, 16, 16, 9.5);
      };
    }
    return Shapes.empty();
  }

  protected void tick(BlockState s, ServerLevel l, BlockPos p, RandomSource random) {
    if (!(l.getBlockEntity(p) instanceof FieldCell c)
        || !l.hasChunkAt(c.source)
        || !(l.getBlockEntity(c.source) instanceof EmitterEntity e)
        || !e.cells.contains(p)) l.removeBlock(p, false);
    else l.scheduleTick(p, this, 40);
  }
}
