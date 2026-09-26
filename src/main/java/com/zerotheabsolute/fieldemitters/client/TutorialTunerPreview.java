package com.zerotheabsolute.fieldemitters.client;

import com.zerotheabsolute.fieldemitters.EmitterEntity;
import com.zerotheabsolute.fieldemitters.FieldEmitters;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Renders the real tuner widgets against a detached emitter; no input is forwarded. */
public final class TutorialTunerPreview {
  private static final double TAB_CLICK_SECONDS = 1.0;
  private static final double ZOOM_SECONDS = .6;
  private static final double CONTROL_HOLD_SECONDS = 2.5;
  private static final float WIDTH = 480, HEIGHT = 180;
  private ControlScreen screen;
  private String chapter;
  private int windowWidth, windowHeight;

  public static double minimumSeconds() {
    return TAB_CLICK_SECONDS + ZOOM_SECONDS + CONTROL_HOLD_SECONDS;
  }

  public void render(GuiGraphics graphics, String lesson, double seconds) {
    var minecraft = Minecraft.getInstance();
    int width = minecraft.getWindow().getGuiScaledWidth();
    int height = minecraft.getWindow().getGuiScaledHeight();
    if (screen == null || !lesson.equals(chapter) || width != windowWidth || height != windowHeight) {
      var block = lesson.equals("towers") ? FieldEmitters.TOWER.get()
          : lesson.equals("rails") ? FieldEmitters.RAIL.get() : FieldEmitters.EMITTER.get();
      var emitter = new EmitterEntity(BlockPos.ZERO, block.defaultBlockState());
      emitter.setLevel(minecraft.level);
      screen = ControlScreen.tutorialPreview(emitter);
      screen.init(minecraft, width, height);
      chapter = lesson;
      windowWidth = width;
      windowHeight = height;
    }
    var page = page(lesson);
    boolean opened = seconds >= TAB_CLICK_SECONDS;
    screen.tutorialTab(opened ? page : ControlTab.OVERVIEW);
    AbstractWidget target = opened ? screen.tutorialControl(control(lesson)) : null;
    if (target == null) {
      String label = Component.translatable(page.translationKey()).getString();
      target = screen.children().stream().filter(AbstractWidget.class::isInstance)
          .map(AbstractWidget.class::cast).filter(widget -> widget.getMessage().getString().equals(label))
          .findFirst().orElseThrow();
    }
    int[] bounds = screen.tutorialBounds();
    float fit = (float) width / screen.width;
    float overviewScale = Math.min((WIDTH - 12) / bounds[2], (HEIGHT - 8) / bounds[3]);
    float progress = (float) Math.max(0, Math.min(1, (seconds - TAB_CLICK_SECONDS) / ZOOM_SECONDS));
    progress = progress * progress * (3 - 2 * progress);
    float zoom = overviewScale + (.90f - overviewScale) * progress;
    float tx = target.getX() + target.getWidth() / 2f;
    float ty = target.getY() + target.getHeight() / 2f;
    float cx = bounds[0] + bounds[2] / 2f, cy = bounds[1] + bounds[3] / 2f;
    cy += (ty - cy) * progress;
    graphics.pose().pushPose();
    try {
      graphics.pose().translate(WIDTH / 2, HEIGHT / 2, 500);
      graphics.pose().scale(zoom / fit, zoom / fit, 1);
      graphics.pose().translate(-cx * fit, -cy * fit, 0);
      screen.render(graphics, -10000, -10000, 0);
      graphics.pose().scale(fit, fit, 1);
      int x = target.getX(), y = target.getY(), w = target.getWidth(), h = target.getHeight();
      graphics.fill(x - 2, y - 2, x + w + 2, y, 0xFF00D9ED);
      graphics.fill(x - 2, y + h, x + w + 2, y + h + 2, 0xFF00D9ED);
      graphics.fill(x - 2, y, x, y + h, 0xFF00D9ED);
      graphics.fill(x + w, y, x + w + 2, y + h, 0xFF00D9ED);
      float approach = (float) Math.min(1, seconds / .8);
      int cursorX = (int) (target.getX() + target.getWidth() - 13 + 26 * (1 - approach));
      int cursorY = (int) (ty + 18 * (1 - approach));
      for (int line = 0; line < 8; line++)
        graphics.fill(cursorX, cursorY + line, cursorX + 1 + line / 2, cursorY + line + 1, 0xFFFFFFFF);
      graphics.flush();
    } finally { graphics.pose().popPose(); }
  }

  private static ControlTab page(String chapter) {
    return switch (chapter) {
      case "rails", "towers" -> ControlTab.CONNECTIONS;
      case "blocking", "directions", "age", "items" -> ControlTab.BLOCKING;
      case "sensor" -> ControlTab.SENSOR;
      case "damage" -> ControlTab.DAMAGE;
      case "cards", "checkpoint" -> ControlTab.ACCESS;
      case "appearance", "formations" -> ControlTab.APPEARANCE;
      default -> ControlTab.OVERVIEW;
    };
  }

  private static Control control(String chapter) {
    return switch (chapter) {
      case "rails" -> Control.BRIDGE_PRESET;
      case "towers" -> Control.SHAPE;
      case "blocking" -> Control.HOSTILE;
      case "directions" -> Control.RULES_FOR;
      case "age" -> Control.AGE;
      case "items" -> Control.ITEM_LIST;
      case "sensor" -> Control.SIGNAL;
      case "damage" -> Control.DAMAGE;
      case "cards" -> Control.ISSUE_AND_REVOKE_BADGES;
      case "checkpoint" -> Control.INVENTORY_CHECKPOINT;
      case "appearance" -> Control.PATTERN;
      case "formations" -> Control.FORMATION;
      default -> Control.TURN_ON;
    };
  }
}
