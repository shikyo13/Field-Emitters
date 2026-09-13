package com.zerotheabsolute.fieldemitters.client;

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
    super(Component.literal("INVENTORY CHECKPOINT"));
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
            380,
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
    fit(404, 306);
    left = (width - 404) / 2;
    top = (height - 306) / 2;
    button("Checkpoint: " + (s.enabled ? "On" : "Off"), 30, () -> s.enabled = !s.enabled);
    button(
        "Players moving: " + (direction == null ? "Any direction" : direction.getName()),
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
        "This direction: "
            + (direction == null
                ? "Shared rules"
                : s.directions.has(direction) ? "Custom rules" : "Shared rules"),
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
                    "Inspect this direction: "
                        + (direction == null || f.direction(direction) ? "Yes" : "No")),
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
            Component.literal("Skip owner: " + (f.exemptOwner ? "Yes" : "No")),
            b -> {
              var rule = editableRule();
              rule.exemptOwner = !rule.exemptOwner;
              apply.run();
              rebuildWidgets();
            },
            false));
    button(
        "Contraband item list",
        118,
        () -> minecraft.setScreen(new TypeListScreen(this, s.items, true, 4, apply)));
    button("Redstone alert: " + (s.detect ? "On" : "Off"), 140, () -> s.detect = !s.detect);
    button(
        "Deny passage while carrying contraband: " + (s.deny ? "Yes" : "No"),
        162,
        () -> s.deny = !s.deny);
    button(
        "Confiscate: "
            + new String[] {"Off", "Drop on entry side", "Send to adjacent storage"}[s.confiscate],
        184,
        () -> s.confiscate = (s.confiscate + 1) % 3);
    button(
        "Storage side: " + s.storageFace.getName() + " (relative to emitter)",
        206,
        () -> s.storageFace = Direction.values()[(s.storageFace.ordinal() + 1) % 6]);
    button(
        "Storage overflow: "
            + (s.dropOverflow ? "Drop on entry side" : "Hold player; keep remaining items"),
        228,
        () -> s.dropOverflow = !s.dropOverflow);
    button(
        "Player / badge filters",
        250,
        () -> {
          if (direction != null && !s.directions.has(direction))
            s.directions.set(direction, EntityFilter.load(s.players.save()));
          minecraft.setScreen(
              new PlayerListScreen(
                  this,
                  pos,
                  direction == null ? s.players : s.directions.resolve(direction, s.players),
                  4,
                  apply));
        });
    button("Back", 280, this::onClose);
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
    g.fill(left, top, left + 404, top + 306, 0xFF0D1D2B);
    g.fill(left, top, left + 404, top + 2, 0xFF53BBCB);
    g.drawString(font, "INVENTORY CHECKPOINT", left + 12, top + 12, 0xFFE0F3FF, false);
    super.render(g, fitMouse(x), fitMouse(y), p);
    g.pose().popPose();
  }
}
