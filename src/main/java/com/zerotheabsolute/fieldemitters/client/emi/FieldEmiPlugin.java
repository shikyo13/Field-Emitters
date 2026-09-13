package com.zerotheabsolute.fieldemitters.client.emi;

import com.zerotheabsolute.fieldemitters.client.TypeListScreen;
import dev.emi.emi.api.*;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

@EmiEntrypoint
public final class FieldEmiPlugin implements EmiPlugin {
  public void register(EmiRegistry registry) {
    registry.addScreenBoundsProvider(TypeListScreen.class, screen -> {
      if(screen.width<=0 || screen.height<=0)return Bounds.EMPTY;
      var area=screen.panelArea();
      return new Bounds(area.getX(),area.getY(),area.getWidth(),area.getHeight());
    });
    registry.addDragDropHandler(TypeListScreen.class,new EmiDragDropHandler<TypeListScreen>() {
      private ItemStack stack(TypeListScreen screen, EmiIngredient ingredient) {
        for(var candidate:ingredient.getEmiStacks()) {
          var stack=candidate.getItemStack();
          if(screen.canAcceptDrop(stack))return stack.copy();
        }
        return ItemStack.EMPTY;
      }
      public boolean dropStack(TypeListScreen screen,EmiIngredient ingredient,int x,int y) {
        var stack=stack(screen,ingredient);var area=screen.dropArea();
        if(stack.isEmpty() || !area.contains(x,y))return false;
        screen.acceptDrop(stack);return true;
      }
      public void render(TypeListScreen screen,EmiIngredient ingredient,GuiGraphics graphics,int x,int y,float delta) {
        if(stack(screen,ingredient).isEmpty())return;
        var area=screen.dropArea();
        graphics.fill(area.getX(),area.getY(),area.getX()+area.getWidth(),area.getY()+area.getHeight(),0x5553BBCB);
      }
    });
  }
}
