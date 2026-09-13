package com.zerotheabsolute.fieldemitters.client.jei;

import com.zerotheabsolute.fieldemitters.client.TypeListScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.*;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

/** Optional JEI integration; ghost ingredients never transfer inventory contents. */
@JeiPlugin
public final class FieldJeiPlugin implements IModPlugin {
  public ResourceLocation getPluginUid(){return new ResourceLocation("fieldemitters","filters");}
  public void registerGuiHandlers(IGuiHandlerRegistration registration) {
    if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("emi"))return;
    registration.addGuiScreenHandler(TypeListScreen.class, screen -> {
      if(screen.width <= 0 || screen.height <= 0)return null;
      var area=screen.panelArea();var window=net.minecraft.client.Minecraft.getInstance().getWindow();
      return new Properties(TypeListScreen.class,area.getX(),area.getY(),area.getWidth(),area.getHeight(),window.getGuiScaledWidth(),window.getGuiScaledHeight());
    });
    registration.addGhostIngredientHandler(TypeListScreen.class,new IGhostIngredientHandler<TypeListScreen>() {
      public <I> List<Target<I>> getTargetsTyped(TypeListScreen screen,ITypedIngredient<I> ingredient,boolean doStart) {
        var stack=ingredient.getItemStack();
        if(stack.isEmpty()||!screen.canAcceptDrop(stack.get()))return List.of();
        var copy=stack.get().copy();
        return List.of(new Target<I>() {
          public Rect2i getArea(){return screen.dropArea();}
          public void accept(I ignored){screen.acceptDrop(copy);}
        });
      }
      public void onComplete(){}
    });
  }
  private record Properties(Class<? extends Screen> screenClass,int guiLeft,int guiTop,int guiXSize,int guiYSize,int screenWidth,int screenHeight) implements IGuiProperties { public Class<? extends Screen> getScreenClass() { return screenClass; } public int getGuiLeft() { return guiLeft; } public int getGuiTop() { return guiTop; } public int getGuiXSize() { return guiXSize; } public int getGuiYSize() { return guiYSize; } public int getScreenWidth() { return screenWidth; } public int getScreenHeight() { return screenHeight; } }
}
