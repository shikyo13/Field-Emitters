package com.zerotheabsolute.fieldemitters;

import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

public final class TunerItem extends Item {
  public static final int[] COLORS = {0x52E5FF, 0xB08CFF, 0xFF668D, 0xFFB750, 0x77FFBD, 0xDCEFFF};

  public TunerItem(Properties p) {
    super(p);
  }

  public InteractionResult useOn(UseOnContext c) {
    var l = c.getLevel();
    var state = l.getBlockState(c.getClickedPos());
    if (!state.is(FieldEmitters.EMITTER.get()) && !state.is(FieldEmitters.RAIL.get()) && !state.is(FieldEmitters.TOWER.get()))
      return InteractionResult.PASS;
    if (l.isClientSide) {
      if (c.getPlayer() != null
          && !c.getPlayer().isShiftKeyDown()
          && l.getBlockEntity(EmitterBlock.base(c.getClickedPos(), state))
              instanceof EmitterEntity e) FieldControls.open.accept(e);
      return InteractionResult.SUCCESS;
    }
    var p = c.getPlayer();
    if (p == null) return InteractionResult.PASS;
    if (l.getBlockEntity(EmitterBlock.base(c.getClickedPos(), state)) instanceof EmitterEntity e) {
      if (!FieldControls.editable(e,p)) {
        p.displayClientMessage(
            Component.literal("This perimeter belongs to another player."), true);
        return InteractionResult.FAIL;
      }
      if (p.isShiftKeyDown()) {
        int index = 0;
        for (int i = 0; i < COLORS.length; i++) if (COLORS[i] == e.color) index = i;
        int color = COLORS[(index + 1) % COLORS.length];
        for (var part : FieldNetwork.connected(e)) {
          if (!FieldControls.editable(part,p)) continue;
          part.color = color;
          part.sync();
        }
        p.displayClientMessage(
            Component.literal("Field color: #" + String.format("%06X", color)), true);
      } else {
        // The control screen sends an explicit, validated settings update.

      }
      var item = c.getItemInHand();
      var saved = item.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
      var tag = saved == null ? new net.minecraft.nbt.CompoundTag() : saved.copyTag();
      tag.putInt("FieldColor", e.color);
      tag.putInt("FieldTargets", e.mask);
      item.set(
          net.minecraft.core.component.DataComponents.CUSTOM_DATA,
          net.minecraft.world.item.component.CustomData.of(tag));
    }
    return InteractionResult.SUCCESS;
  }

  @Override
  public InteractionResult interactLivingEntity(
      ItemStack stack,
      net.minecraft.world.entity.player.Player player,
      net.minecraft.world.entity.LivingEntity entity,
      InteractionHand hand) {
    if (!player.isShiftKeyDown()) return InteractionResult.PASS;
    if (!player.level().isClientSide) sample(stack, player, entity);
    return InteractionResult.SUCCESS;
  }

  @Override
  public InteractionResultHolder<ItemStack> use(
      net.minecraft.world.level.Level level,
      net.minecraft.world.entity.player.Player player,
      InteractionHand hand) {
    var stack = player.getItemInHand(hand);
    if (!player.isShiftKeyDown()) {
      if (!level.isClientSide) remoteOpen(player);
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    if (!level.isClientSide) sample(stack, player, player);
    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
  }

  private static void remoteOpen(net.minecraft.world.entity.player.Player player) {
    // Shared request handler keeps list access identical to the menu's Refresh button.
    FieldControls.openRemote(player);
  }

  private void sample(
      ItemStack stack,
      net.minecraft.world.entity.player.Player player,
      net.minecraft.world.entity.Entity entity) {
    var old = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
    var tag = old == null ? new net.minecraft.nbt.CompoundTag() : old.copyTag();
    tag.putString("SampleUUID", entity.getUUID().toString());
    tag.putString(
        "SampleType",
        net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
            .getKey(entity.getType())
            .toString());
    stack.set(
        net.minecraft.core.component.DataComponents.CUSTOM_DATA,
        net.minecraft.world.item.component.CustomData.of(tag));
    player.displayClientMessage(
        Component.literal(
            "Sampled " + entity.getName().getString() + ". Apply identity or type in controls."),
        true);
  }

  @Override
  public void appendHoverText(
      ItemStack stack,
      Item.TooltipContext context,
      java.util.List<Component> lines,
      TooltipFlag flags) {
    lines.add(
        Component.literal("Use in air: remote manager; on emitter: controls")
            .withStyle(net.minecraft.ChatFormatting.GRAY));
    lines.add(
        Component.literal("Sneak-use emitter: color; mob / air: sample")
            .withStyle(net.minecraft.ChatFormatting.GRAY));
    var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
    if (data != null && data.copyTag().contains("FieldTargets")) {
      lines.add(
          Component.literal("Last tuned: " + targets(data.copyTag().getInt("FieldTargets")))
              .withStyle(net.minecraft.ChatFormatting.AQUA));
    }
  }

  public static String targets(int mask) {
    if (mask == 0) return "nothing (visual only)";
    java.util.List<String> a = new java.util.ArrayList<>();
    if ((mask & 1) != 0) a.add("hostiles");
    if ((mask & 2) != 0) a.add("peaceful mobs");
    if ((mask & 4) != 0) a.add("other players");
    return String.join(", ", a);
  }
}
