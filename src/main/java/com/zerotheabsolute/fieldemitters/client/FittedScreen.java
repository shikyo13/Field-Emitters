package com.zerotheabsolute.fieldemitters.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

abstract class FittedScreen extends com.zeromods.core.client.FittedScreen {
  private record Overflow(Component text, int x, int y, int width, int height) {}
  private final List<Overflow> overflow = new ArrayList<>();

  protected FittedScreen(Component title) { super(title); }

  @Override
  protected void beginFit(GuiGraphics graphics) {
    overflow.clear();
    super.beginFit(graphics);
  }

  protected void drawLabel(GuiGraphics graphics, String text, int x, int y, int width, int color) {
    String visible = text;
    if (font.width(text) > width) {
      visible = font.plainSubstrByWidth(text, Math.max(0, width - font.width("…"))) + "…";
      overflow.add(new Overflow(Component.literal(text), x, y, width, font.lineHeight));
    }
    graphics.drawString(font, visible, x, y, color, false);
  }

  protected void drawParagraph(GuiGraphics graphics, Component text, int x, int y,
      int width, int height, int color) {
    var lines = font.split(text, width);
    int limit = Math.max(1, height / font.lineHeight);
    boolean clipped = lines.size() > limit;
    for (int i = 0; i < Math.min(limit, lines.size()); i++) {
      if (clipped && i == limit - 1) {
        // Leave space for an ellipsis without cutting a multibyte glyph in half.
        var line = new StringBuilder();
        lines.get(i).accept((index, style, codepoint) -> { line.appendCodePoint(codepoint); return true; });
        String visible = font.plainSubstrByWidth(line.toString(), Math.max(0, width - font.width("…"))) + "…";
        graphics.drawString(font, visible, x, y + i * font.lineHeight, color, false);
      } else graphics.drawString(font, lines.get(i), x, y + i * font.lineHeight, color, false);
    }
    if (clipped) overflow.add(new Overflow(text, x, y, width, Math.min(height, limit * font.lineHeight)));
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    super.render(graphics, mouseX, mouseY, partialTick);
    for (var label : overflow) {
      if (mouseX >= label.x && mouseX < label.x + label.width
          && mouseY >= label.y && mouseY < label.y + label.height) {
        graphics.renderTooltip(font, label.text, mouseX, mouseY);
        break;
      }
    }
  }
}
