package com.zerotheabsolute.fieldemitters.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

final class FieldEditBox extends EditBox {
  private Tooltip hoverTooltip;

  FieldEditBox(Font font, int x, int y, int width, int height, Component label) {
    super(font, x, y, width, height, label);
  }

  @Override
  public void setTooltip(Tooltip tooltip) {
    hoverTooltip = tooltip;
    super.setTooltip(isHovered() ? tooltip : null);
  }

  @Override
  public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    super.setTooltip(isHovered() ? hoverTooltip : null);
    super.renderWidget(graphics, mouseX, mouseY, partialTick);
  }
}
