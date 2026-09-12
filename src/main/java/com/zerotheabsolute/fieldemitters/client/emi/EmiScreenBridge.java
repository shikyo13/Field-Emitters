package com.zerotheabsolute.fieldemitters.client.emi;

import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/** EMI 1.1.24's NeoForge hooks handle container screens only; forward this custom screen explicitly. */
public final class EmiScreenBridge {
  private EmiScreenBridge() {}
  public static void init(Screen screen) { EmiScreenManager.addWidgets(screen); }
  public static void render(GuiGraphics graphics,int x,int y,float delta) {
    graphics.pose().pushPose();
    var context=EmiDrawContext.wrap(graphics);
    EmiScreenManager.drawBackground(context,x,y,delta);
    EmiScreenManager.render(context,x,y,delta);
    EmiScreenManager.drawForeground(context,x,y,delta);
    graphics.pose().popPose();
  }
  public static boolean click(double x,double y,int button) {return EmiScreenManager.mouseClicked(x,y,button);}
  public static boolean release(double x,double y,int button) {return EmiScreenManager.mouseReleased(x,y,button);}
  public static boolean drag(double x,double y,int button,double dx,double dy) {return EmiScreenManager.mouseDragged(x,y,button,dx,dy);}
  public static boolean scroll(double x,double y,double amount) {return EmiScreenManager.mouseScrolled(x,y,amount);}
  public static boolean key(int key,int scan,int modifiers) {return EmiScreenManager.keyPressed(key,scan,modifiers);}
  public static boolean character(char value,int modifiers) {return EmiScreenManager.search.charTyped(value,modifiers);}
}
