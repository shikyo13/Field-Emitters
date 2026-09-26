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

  private enum Page {
    MAIN,
    PLAYERS,
    RESPONSE
  }

  private Page page = Page.MAIN;

  CheckpointScreen(
      Screen parent, net.minecraft.core.BlockPos pos, CheckpointSettings s, Runnable apply) {
    super(Component.literal(UiText.text("screen.fieldemitters.checkpoint.inventory_checkpoint")));
    this.parent = parent;
    this.pos = pos;
    this.s = s;
    this.apply = apply;
  }

  private String text(String key) {
    return UiText.text("screen.fieldemitters.pages." + key);
  }

  private void open(Page next) {
    page = next;
    rebuildWidgets();
  }

  private void menu(String title, String summary, int y, Page next) {
    var entry =
        addRenderableWidget(
            new FieldButton(
                left + 12,
                top + y,
                ScreenMetrics.CONTENT_WIDTH,
                ScreenMetrics.MENU_HEIGHT,
                Component.literal(title),
                b -> open(next),
                false));
    entry.detail(summary);
    entry.setTooltip(
        net.minecraft.client.gui.components.Tooltip.create(
            Component.literal(title + "\n" + summary)));
  }

  private FieldButton button(String text, int y, Runnable action) {
    return button(text, y, action, null);
  }

  private FieldButton button(String text, int y, Runnable action, String helpKey) {
    var button =
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
    String help = helpKey == null ? text : text(helpKey);
    button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(help)));
    return button;
  }

  protected void init() {
    fit(ScreenMetrics.PANEL_WIDTH, ScreenMetrics.PANEL_HEIGHT);
    left = (width - ScreenMetrics.PANEL_WIDTH) / 2;
    top = (height - ScreenMetrics.PANEL_HEIGHT) / 2;
    if (page == Page.MAIN) {
      button(
          UiText.text(
              "screen.fieldemitters.checkpoint.checkpoint",
              (s.enabled
                  ? UiText.text("screen.fieldemitters.control.on")
                  : UiText.text("screen.fieldemitters.control.off"))),
          30,
          () -> s.enabled = !s.enabled);
      button(
          UiText.text("screen.fieldemitters.checkpoint.contraband_item_list"),
          60,
          () ->
              minecraft.setScreen(
                  new TypeListScreen(this, s.items, true, FilterPurpose.CHECKPOINT, apply)));
      menu(
          text("checkpoint_players"),
          UiText.text(
              "screen.fieldemitters.control.skip_owner",
              UiText.text(
                  "screen.fieldemitters.control." + (s.players.exemptOwner ? "yes" : "no"))),
          96,
          Page.PLAYERS);
      menu(
          text("checkpoint_response"),
          UiText.text(
              "screen.fieldemitters.checkpoint.confiscate",
              UiText.text(
                  s.confiscate == 0
                      ? "screen.fieldemitters.control.off"
                      : s.confiscate == 1
                          ? "screen.fieldemitters.checkpoint.drop_on_entry_side"
                          : "screen.fieldemitters.checkpoint.send_to_adjacent_storage")),
          138,
          Page.RESPONSE);
    } else if (page == Page.PLAYERS) {
      button(
          UiText.text(
              "screen.fieldemitters.control.rules_for",
              (direction == null
                  ? UiText.text("screen.fieldemitters.control.all_directions")
                  : UiText.direction(direction))),
          52,
          () ->
              direction =
                  direction == null
                      ? Direction.DOWN
                      : direction == Direction.EAST
                          ? null
                          : Direction.values()[direction.ordinal() + 1],
          "checkpoint_rule_help");
      var f = direction == null ? s.players : s.directions.resolve(direction, s.players);
      button(
                  UiText.text(
                      "screen.fieldemitters.control.rule_source",
                      (direction == null
                          ? UiText.text("screen.fieldemitters.control.shared")
                          : s.directions.has(direction)
                              ? UiText.text("screen.fieldemitters.control.custom")
                              : UiText.text("screen.fieldemitters.control.shared"))),
                  74,
                  () -> {
                    if (direction != null) {
                      if (s.directions.has(direction)) s.directions.inherit(direction);
                      else s.directions.set(direction, EntityFilter.load(s.players.save()));
                    }
                  },
                  "checkpoint_rule_help")
              .active =
          direction != null;
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
      inspect.active = direction != null && s.directions.has(direction);
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
                      false))
              .active =
          direction == null || s.directions.has(direction);
      button(
                  UiText.text("screen.fieldemitters.checkpoint.player_badge_filters"),
                  132,
                  () -> {
                    minecraft.setScreen(
                        new PlayerListScreen(
                            this,
                            pos,
                            direction == null
                                ? s.players
                                : s.directions.resolve(direction, s.players),
                            FilterPurpose.CHECKPOINT,
                            apply));
                  })
              .active =
          direction == null || s.directions.has(direction);
    } else {
      button(
          UiText.text(
              "screen.fieldemitters.checkpoint.redstone_alert",
              (s.detect
                  ? UiText.text("screen.fieldemitters.control.on")
                  : UiText.text("screen.fieldemitters.control.off"))),
          52,
          () -> s.detect = !s.detect,
          "checkpoint_response_help");
      button(
          UiText.text(
              "screen.fieldemitters.checkpoint.deny_passage_while_carrying_contraband",
              (s.deny
                  ? UiText.text("screen.fieldemitters.control.yes")
                  : UiText.text("screen.fieldemitters.control.no"))),
          74,
          () -> s.deny = !s.deny,
          "checkpoint_response_help");
      button(
          UiText.text(
              "screen.fieldemitters.checkpoint.confiscate",
              new String[] {
                    UiText.text("screen.fieldemitters.control.off"),
                    UiText.text("screen.fieldemitters.checkpoint.drop_on_entry_side"),
                    UiText.text("screen.fieldemitters.checkpoint.send_to_adjacent_storage")
                  }
                  [s.confiscate]),
          96,
          () -> s.confiscate = (s.confiscate + 1) % 3,
          "checkpoint_confiscate_help");
      if (s.confiscate == 2) {
        button(
            UiText.text(
                "screen.fieldemitters.checkpoint.storage_side_relative_to_emitter",
                UiText.direction(s.storageFace)),
            140,
            () -> s.storageFace = Direction.values()[(s.storageFace.ordinal() + 1) % 6],
            "checkpoint_storage_help");
        button(
            UiText.text(
                "screen.fieldemitters.checkpoint.storage_overflow",
                (s.dropOverflow
                    ? UiText.text("screen.fieldemitters.checkpoint.drop_on_entry_side")
                    : UiText.text(
                        "screen.fieldemitters.checkpoint.hold_player_keep_remaining_items"))),
            162,
            () -> s.dropOverflow = !s.dropOverflow,
            "checkpoint_storage_help");
      }
    }
    button(
        UiText.text("screen.fieldemitters.access.back"),
        280,
        () -> {
          if (page == Page.MAIN) onClose();
          else open(Page.MAIN);
        });
  }

  private EntityFilter editableRule() {
    if (direction == null) return s.players;
    return s.directions.overrides().get(direction);
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
        page == Page.MAIN
            ? UiText.text("screen.fieldemitters.checkpoint.inventory_checkpoint")
            : text(page == Page.PLAYERS ? "checkpoint_players" : "checkpoint_response"),
        left + 12,
        top + 12,
        0xFFE0F3FF,
        false);
    super.render(g, fitMouse(x), fitMouse(y), p);
    g.pose().popPose();
  }
}
