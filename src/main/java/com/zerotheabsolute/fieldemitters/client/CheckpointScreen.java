package com.zerotheabsolute.fieldemitters.client;

import com.zeromods.core.client.FittedScreen;

import com.zerotheabsolute.fieldemitters.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

final class CheckpointScreen extends FittedScreen {
  private final Screen parent;
  private final net.minecraft.core.BlockPos pos;
  private final CheckpointSettings s;
  private final Runnable apply;
  private int left, top;
  private Direction direction;

  CheckpointScreen(
      Screen parent, net.minecraft.core.BlockPos pos, CheckpointSettings s, Runnable apply) {
    super(Component.literal(UiText.text("screen.fieldemitters.checkpoint.inventory_checkpoint")));
    this.parent = parent;
    this.pos = pos;
    this.s = s;
    this.apply = apply;
  }

  private void button(String text, int y, Runnable action) {
    addRenderableWidget(
        new FieldButton(
            left + 12,
            top + y,
            ScreenMetrics.CONTENT_WIDTH,
            18,
            Component.literal(text),
            b -> {
              action.run();
              apply.run();
              rebuildWidgets();
            },
            false));
  }

  protected void init() {
    fit(ScreenMetrics.PANEL_WIDTH, ScreenMetrics.PANEL_HEIGHT);
    left = (width - ScreenMetrics.PANEL_WIDTH) / 2;
    top = (height - ScreenMetrics.PANEL_HEIGHT) / 2;
    button(
        UiText.text(
            "screen.fieldemitters.checkpoint.checkpoint",
            (s.enabled
                ? UiText.text("screen.fieldemitters.control.on")
                : UiText.text("screen.fieldemitters.control.off"))),
        30,
        () -> s.enabled = !s.enabled);
    button(
        UiText.text(
            "screen.fieldemitters.checkpoint.players_moving",
            (direction == null
                ? UiText.text("screen.fieldemitters.checkpoint.any_direction")
                : UiText.direction(direction))),
        52,
        () ->
            direction =
                direction == null
                    ? Direction.DOWN
                    : direction == Direction.EAST
                        ? null
                        : Direction.values()[direction.ordinal() + 1]);
    var f = direction == null ? s.players : s.directions.resolve(direction, s.players);
    button(
        UiText.text(
            "screen.fieldemitters.checkpoint.this_direction",
            (direction == null
                ? UiText.text("screen.fieldemitters.checkpoint.shared_rules")
                : s.directions.has(direction)
                    ? UiText.text("screen.fieldemitters.checkpoint.custom_rules")
                    : UiText.text("screen.fieldemitters.checkpoint.shared_rules"))),
        74,
        () -> {
          if (direction != null) {
            if (s.directions.has(direction)) s.directions.inherit(direction);
            else s.directions.set(direction, EntityFilter.load(s.players.save()));
          }
        });
    var inspect =
        addRenderableWidget(
            new FieldButton(
                left + 12,
                top + 96,
                187,
                18,
                Component.literal(
                    UiText.text(
                        "screen.fieldemitters.checkpoint.inspect_this_direction",
                        (direction == null || f.direction(direction)
                            ? UiText.text("screen.fieldemitters.control.yes")
                            : UiText.text("screen.fieldemitters.control.no")))),
                b -> {
                  var rule = editableRule();
                  rule.directions ^= 1 << direction.ordinal();
                  apply.run();
                  rebuildWidgets();
                },
                false));
    inspect.active = direction != null;
    addRenderableWidget(
        new FieldButton(
            left + 205,
            top + 96,
            187,
            18,
            Component.literal(
                UiText.text(
                    "screen.fieldemitters.control.skip_owner",
                    (f.exemptOwner
                        ? UiText.text("screen.fieldemitters.control.yes")
                        : UiText.text("screen.fieldemitters.control.no")))),
            b -> {
              var rule = editableRule();
              rule.exemptOwner = !rule.exemptOwner;
              apply.run();
              rebuildWidgets();
            },
            false));
    button(
        UiText.text("screen.fieldemitters.checkpoint.contraband_item_list"),
        118,
        () ->
            minecraft.setScreen(
                new TypeListScreen(this, s.items, true, FilterPurpose.CHECKPOINT, apply)));
    button(
        UiText.text(
            "screen.fieldemitters.checkpoint.redstone_alert",
            (s.detect
                ? UiText.text("screen.fieldemitters.control.on")
                : UiText.text("screen.fieldemitters.control.off"))),
        140,
        () -> s.detect = !s.detect);
    button(
        UiText.text(
            "screen.fieldemitters.checkpoint.deny_passage_while_carrying_contraband",
            (s.deny
                ? UiText.text("screen.fieldemitters.control.yes")
                : UiText.text("screen.fieldemitters.control.no"))),
        162,
        () -> s.deny = !s.deny);
    button(
        UiText.text(
            "screen.fieldemitters.checkpoint.confiscate",
            new String[] {
                  UiText.text("screen.fieldemitters.control.off"),
                  UiText.text("screen.fieldemitters.checkpoint.drop_on_entry_side"),
                  UiText.text("screen.fieldemitters.checkpoint.send_to_adjacent_storage")
                }
                [s.confiscate]),
        184,
        () -> s.confiscate = (s.confiscate + 1) % 3);
    button(
        UiText.text(
            "screen.fieldemitters.checkpoint.storage_side_relative_to_emitter",
            UiText.direction(s.storageFace)),
        206,
        () -> s.storageFace = Direction.values()[(s.storageFace.ordinal() + 1) % 6]);
    button(
        UiText.text(
            "screen.fieldemitters.checkpoint.storage_overflow",
            (s.dropOverflow
                ? UiText.text("screen.fieldemitters.checkpoint.drop_on_entry_side")
                : UiText.text("screen.fieldemitters.checkpoint.hold_player_keep_remaining_items"))),
        228,
        () -> s.dropOverflow = !s.dropOverflow);
    button(
        UiText.text("screen.fieldemitters.checkpoint.player_badge_filters"),
        250,
        () -> {
          if (direction != null && !s.directions.has(direction))
            s.directions.set(direction, EntityFilter.load(s.players.save()));
          minecraft.setScreen(
              new PlayerListScreen(
                  this,
                  pos,
                  direction == null ? s.players : s.directions.resolve(direction, s.players),
                  FilterPurpose.CHECKPOINT,
                  apply));
        });
    button(UiText.text("screen.fieldemitters.access.back"), 280, this::onClose);
  }

  private EntityFilter editableRule() {
    if (direction == null) return s.players;
    if (!s.directions.has(direction))
      s.directions.set(direction, EntityFilter.load(s.players.save()));
    return s.directions.resolve(direction, s.players);
  }

  public void onClose() {
    apply.run();
    minecraft.setScreen(parent);
  }

  public boolean isPauseScreen() {
    return false;
  }

  public void renderBackground(GuiGraphics g) {}

  public void render(GuiGraphics g, int x, int y, float p) {
    beginFit(g);
    g.fill(
        left, top, left + ScreenMetrics.PANEL_WIDTH, top + ScreenMetrics.PANEL_HEIGHT, 0xFF0D1D2B);
    g.fill(left, top, left + ScreenMetrics.PANEL_WIDTH, top + 2, 0xFF53BBCB);
    g.drawString(
        font,
        UiText.text("screen.fieldemitters.checkpoint.inventory_checkpoint"),
        left + 12,
        top + 12,
        0xFFE0F3FF,
        false);
    super.render(g, fitMouse(x), fitMouse(y), p);
    g.pose().popPose();
  }
}
