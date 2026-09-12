package com.zerotheabsolute.fieldemitters.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Keeps the complete control canvas visible, with input mapped to the same canvas. */
abstract class FittedScreen extends Screen {
  private double fitScale = 1;

  protected FittedScreen(Component title) {
    super(title);
  }

  protected void fit(int panelWidth, int panelHeight) {
    int screenWidth = minecraft.getWindow().getGuiScaledWidth();
    int screenHeight = minecraft.getWindow().getGuiScaledHeight();
    fitScale =
        Math.min(1, Math.min((screenWidth - 8.0) / panelWidth, (screenHeight - 8.0) / panelHeight));
    fitScale = Math.max(.1, fitScale);
    width = (int) (screenWidth / fitScale);
    height = (int) (screenHeight / fitScale);
  }

  protected int fitMouse(int coordinate) {
    return (int) (coordinate / fitScale);
  }

  protected void beginFit(GuiGraphics graphics) {
    graphics.pose().pushPose();
    graphics.pose().scale((float) fitScale, (float) fitScale, 1);
  }

  @Override
  public boolean mouseClicked(double x, double y, int b) {
    return super.mouseClicked(x / fitScale, y / fitScale, b);
  }

  @Override
  public boolean mouseReleased(double x, double y, int b) {
    return super.mouseReleased(x / fitScale, y / fitScale, b);
  }

  @Override
  public boolean mouseDragged(double x, double y, int b, double dx, double dy) {
    return super.mouseDragged(x / fitScale, y / fitScale, b, dx / fitScale, dy / fitScale);
  }

  @Override
  public boolean mouseScrolled(double x, double y, double dy) {
    return super.mouseScrolled(x / fitScale, y / fitScale, dy);
  }

  @Override
  public void mouseMoved(double x, double y) {
    super.mouseMoved(x / fitScale, y / fitScale);
  }
}
