package com.zerotheabsolute.fieldemitters;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;

public final class FieldCommands {
  private static final int OPERATOR_LEVEL = 2;
  private static final DynamicCommandExceptionType ERROR = new DynamicCommandExceptionType(
      detail -> Component.translatable("command.fieldemitters.error", detail));
  private FieldCommands() {}

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(Commands.literal("fieldemitters").requires(source -> source.hasPermission(OPERATOR_LEVEL))
        .then(Commands.literal("presets").executes(context -> {
          context.getSource().sendSuccess(() -> Component.translatable("command.fieldemitters.presets", String.join(", ", new FieldScriptApi().presets())), false);
          return FieldPresets.ids().size();
        }))
        .then(Commands.literal("apply").then(Commands.argument("pos", BlockPosArgument.blockPos())
            .then(Commands.argument("preset", ResourceLocationArgument.id())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(new FieldScriptApi().presets(), builder))
                .executes(context -> run(context, handle -> handle.applyPreset(ResourceLocationArgument.getId(context, "preset").toString()))))))
        .then(Commands.literal("enable").then(Commands.argument("pos", BlockPosArgument.blockPos())
            .then(Commands.argument("enabled", BoolArgumentType.bool())
                .executes(context -> run(context, handle -> handle.setEnabled(BoolArgumentType.getBool(context, "enabled")))))))
        .then(Commands.literal("unlock").then(Commands.argument("pos", BlockPosArgument.blockPos())
            .executes(context -> run(context, FieldHandle::unlock))))
        .then(Commands.literal("status").then(Commands.argument("pos", BlockPosArgument.blockPos())
            .executes(context -> query(context, false))))
        .then(Commands.literal("export").then(Commands.argument("pos", BlockPosArgument.blockPos())
            .executes(context -> query(context, true)))));
  }

  private static FieldHandle handle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return new FieldHandle(context.getSource().getLevel(), BlockPosArgument.getLoadedBlockPos(context, "pos"));
  }
  private static int run(CommandContext<CommandSourceStack> context, java.util.function.Consumer<FieldHandle> operation) throws CommandSyntaxException {
    try {
      operation.accept(handle(context));
      context.getSource().sendSuccess(() -> Component.translatable("command.fieldemitters.updated"), true);
      return 1;
    } catch (IllegalArgumentException | IllegalStateException ex) { throw error(ex); }
  }
  private static int query(CommandContext<CommandSourceStack> context, boolean export) throws CommandSyntaxException {
    try {
      var field = handle(context);
      var output = export ? Component.literal(field.exportPreset()) : Component.translatable("command.fieldemitters.status",
          field.statusText(), field.getCrossings(), field.getSignal(),
          field.getPreset().isEmpty() ? Component.translatable("message.fieldemitters.detection.none") : field.getPreset(),
          Component.translatable("screen.fieldemitters.control." + (field.isLocked() ? "yes" : "no")));
      context.getSource().sendSuccess(() -> output, false);
      return export ? 1 : field.getSignal();
    } catch (IllegalArgumentException | IllegalStateException ex) { throw error(ex); }
  }
  private static CommandSyntaxException error(RuntimeException failure) {
    if (failure instanceof FieldOperationException known) return ERROR.create(known.text());
    com.mojang.logging.LogUtils.getLogger().warn("Field command failed", failure);
    return ERROR.create(Component.translatable("command.fieldemitters.error.unexpected"));
  }

}
