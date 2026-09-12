package com.zerotheabsolute.fieldemitters.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Shared controls for emitter settings and remote field management. */
final class FieldButton extends Button {
  private final boolean selected;

  FieldButton(int x, int y, int w, int h, Component title, OnPress action, boolean selected) {
    super(x, y, w, h, title, action, DEFAULT_NARRATION);
    this.selected = selected;
  }

  @Override
  protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
    int border =
        !active
            ? 0xFF243441
            : isHoveredOrFocused() ? 0xFF87ECFF : selected ? 0xFF53BBCB : 0xFF354D63;
    g.fill(getX(), getY(), getX() + width, getY() + height, border);
    g.fill(
        getX() + 1,
        getY() + 1,
        getX() + width - 1,
        getY() + height - 1,
        selected ? 0xFF214755 : isHoveredOrFocused() ? 0xFF263F53 : 0xFF1B2D3E);
    var font = net.minecraft.client.Minecraft.getInstance().font;
    String fullText = getMessage().getString();
    // Direction labels need the whole button width; their selected state has a bright fill.
    if (width == 61
        && (fullText.contains("To ")
            || fullText.contains("Upward")
            || fullText.contains("Downward"))) fullText = fullText.replaceFirst("^[✓○] ", "");
    String text = font.plainSubstrByWidth(fullText, width - 8);
    g.drawString(
        font,
        text,
        getX() + (width - font.width(text)) / 2,
        getY() + (height - 8) / 2,
        !active ? 0xFF708395 : selected ? 0xFFA0F5FF : 0xFFD8E6F3,
        false);
  }
}
