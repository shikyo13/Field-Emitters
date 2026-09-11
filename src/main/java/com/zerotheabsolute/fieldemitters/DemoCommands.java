package com.zerotheabsolute.fieldemitters;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class DemoCommands {
  public static void register(RegisterCommandsEvent event) {
    event
        .getDispatcher()
        .register(
            Commands.literal("fielddemo")
                .requires(s -> s.hasPermission(2))
                .then(
                    Commands.literal("verifyrails")
                        .executes(ctx -> RailVerification.run(ctx.getSource())))
                .then(
                    Commands.literal("verify")
                        .executes(ctx -> DemoVerification.run(ctx.getSource())))
                .then(
                    Commands.literal("build")
                        .executes(
                            ctx -> {
                              var s = ctx.getSource();
                              var l = s.getLevel();
                              var origin = BlockPos.containing(s.getPosition()).offset(0, 0, 7);
                              if (origin.getY() < l.getMinBuildHeight() + 1
                                  || origin.getY() > l.getMaxBuildHeight() - 12) {
                                s.sendFailure(Component.literal("Insufficient build height."));
                                return 0;
                              }
                              // Refuse to overwrite a build: the demo uses only open air above a
                              // surface.
                              for (int x = -3; x <= 23; x++)
                                for (int z = -3; z <= 19; z++)
                                  for (int y = 0; y <= 10; y++) {
                                    var p = origin.offset(x, y, z);
                                    if (!l.hasChunkAt(p) || !l.getBlockState(p).isAir()) {
                                      s.sendFailure(
                                          Component.literal(
                                              "Demo needs clear air: fly at least 12 blocks above"
                                                  + " the ground, then retry."));
                                      return 0;
                                    }
                                  }
                              for (int x = -3; x <= 23; x++)
                                for (int z = -3; z <= 19; z++) {
                                  int h = Math.max(0, Math.min(4, (x - 4) / 3));
                                  for (int y = 0; y <= h; y++)
                                    l.setBlock(
                                        origin.offset(x, y, z),
                                        (y == h ? Blocks.SMOOTH_STONE : Blocks.DEEPSLATE_TILES)
                                            .defaultBlockState(),
                                        3);
                                  if (z == -3 || z == 19 || x == -3 || x == 23)
                                    l.setBlock(
                                        origin.offset(x, h, z),
                                        Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                                        3);
                                }
                              for (int[] v : new int[][] {{0, 0}, {20, 0}, {0, 16}, {20, 16}}) {
                                int h = Math.max(0, Math.min(4, (v[0] - 4) / 3));
                                var p = origin.offset(v[0], h + 1, v[1]);
                                for (int i = 0; i < 5; i++)
                                  l.setBlock(
                                      p.above(i),
                                      FieldEmitters.EMITTER
                                          .get()
                                          .defaultBlockState()
                                          .setValue(EmitterBlock.SECTION, i),
                                      3);
                                if (l.getBlockEntity(p) instanceof EmitterEntity e
                                    && s.getEntity() != null) e.owner = s.getEntity().getUUID();
                              }
                              l.setBlock(
                                  origin.offset(-1, 1, 0),
                                  Blocks.LEVER
                                      .defaultBlockState()
                                      .setValue(
                                          net.minecraft.world.level.block.LeverBlock.FACE,
                                          net.minecraft.world.level.block.state.properties
                                              .AttachFace.FLOOR)
                                      .setValue(
                                          net.minecraft.world.level.block.LeverBlock.POWERED, true),
                                  3);
                              if (s.getEntity()
                                  instanceof net.minecraft.server.level.ServerPlayer player) {
                                player
                                    .getInventory()
                                    .add(
                                        new net.minecraft.world.item.ItemStack(
                                            FieldEmitters.EMITTER_ITEM.get(), 8));
                                player
                                    .getInventory()
                                    .add(
                                        new net.minecraft.world.item.ItemStack(
                                            FieldEmitters.TUNER.get()));
                              }
                              s.sendSuccess(
                                  () ->
                                      Component.literal(
                                          "Field demo built: four emitters, 20-block hillside span."
                                              + " Toggle the lever at "
                                              + origin.offset(-1, 1, 0)
                                              + " to retract. Use a tuner for targets; sneak-use"
                                              + " for color."),
                                  false);
                              return 1;
                            }))
                .then(
                    Commands.literal("color")
                        .then(
                            Commands.argument("rgb", IntegerArgumentType.integer(0, 0xffffff))
                                .executes(
                                    ctx ->
                                        configure(
                                            ctx.getSource(),
                                            IntegerArgumentType.getInteger(ctx, "rgb"),
                                            true))))
                .then(
                    Commands.literal("targets")
                        .then(
                            Commands.argument("mask", IntegerArgumentType.integer(0, 7))
                                .executes(
                                    ctx ->
                                        configure(
                                            ctx.getSource(),
                                            IntegerArgumentType.getInteger(ctx, "mask"),
                                            false)))));
  }

  private static int configure(
      net.minecraft.commands.CommandSourceStack s, int value, boolean color) {
    var l = s.getLevel();
    var at = BlockPos.containing(s.getPosition());
    EmitterEntity nearest = null;
    double distance = 64 * 64;
    for (int x = -8; x <= 8; x++)
      for (int z = -8; z <= 8; z++)
        for (int y = -5; y <= 5; y++)
          if (l.getBlockEntity(at.offset(x, y, z)) instanceof EmitterEntity e) {
            double d = e.getBlockPos().distSqr(at);
            if (d < distance) {
              distance = d;
              nearest = e;
            }
          }
    if (nearest == null) {
      s.sendFailure(Component.literal("Stand within eight blocks of an emitter."));
      return 0;
    }
    for (var e : FieldNetwork.connected(nearest)) {
      if (color) e.color = value;
      else {
        e.mask = value;
        e.controls.barrier.groups = value;
      }
      e.sync();
    }
    s.sendSuccess(
        () ->
            Component.literal(
                color ? "Perimeter color updated." : "Blocks: " + TunerItem.targets(value)),
        false);
    return 1;
  }
}
