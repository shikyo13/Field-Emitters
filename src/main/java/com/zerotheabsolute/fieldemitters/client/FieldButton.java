package com.zerotheabsolute.fieldemitters.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Shared controls for emitter settings and remote field management. */
final class FieldButton extends Button {
  private final boolean selected;
  private final Integer swatch;
  private String subtitle;
  private Tooltip hoverTooltip;

  FieldButton(int x, int y, int w, int h, Component title, OnPress action, boolean selected) {
    this(x, y, w, h, title, action, selected, null);
  }

  FieldButton(
      int x,
      int y,
      int w,
      int h,
      Component title,
      OnPress action,
      boolean selected,
      Integer swatch) {
    super(x, y, w, h, title, action, DEFAULT_NARRATION);
    this.selected = selected;
    this.swatch = swatch;
    setTooltip(net.minecraft.client.gui.components.Tooltip.create(title));
  }

  FieldButton detail(String text) {
    subtitle = text;
    return this;
  }

  @Override
  public void setTooltip(Tooltip tooltip) {
    hoverTooltip = tooltip;
    super.setTooltip(isHovered() ? tooltip : null);
  }

  @Override
  protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
    super.setTooltip(isHovered() ? hoverTooltip : null);
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
    if (swatch != null)
      g.fill(
          getX() + 2,
          getY() + height - 4,
          getX() + width - 2,
          getY() + height - 2,
          0xFF000000 | swatch);
    var font = net.minecraft.client.Minecraft.getInstance().font;
    int textColor = !active ? 0xFF708395 : selected ? 0xFFA0F5FF : 0xFFD8E6F3;
    int padding = subtitle == null ? 8 : 16;
    String fullText = getMessage().getString();
    String text = fullText;
    if (font.width(fullText) > width - padding) {
      String ellipsis = "…";
      text =
          font.plainSubstrByWidth(fullText, Math.max(0, width - padding - font.width(ellipsis)))
              + ellipsis;
    }
    g.drawString(
        font,
        text,
        getX() + (subtitle == null ? (width - font.width(text)) / 2 : 8),
        getY() + (subtitle == null ? (height - font.lineHeight) / 2 : 4),
        textColor,
        false);
    if (subtitle != null)
      g.drawString(
          font,
          font.plainSubstrByWidth(subtitle, width - 16),
          getX() + 8,
          getY() + 16,
          active ? 0xFFA9BECF : 0xFF708395,
          false);
  }
}
