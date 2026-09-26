package com.zerotheabsolute.fieldemitters.client;

import com.zerotheabsolute.fieldemitters.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

/** Two visible exception lists shared by the filter tabs. */
final class FilterListPanel {
  static final int HEIGHT = 83, ROW_HEIGHT = 19, ROWS = 3, GAP = 6;
  private final EntityFilter filter;
  private final FilterPurpose purpose;
  private final int x, y, width;
  private final Consumer<Boolean> add;
  private final Runnable changed;
  private final int[] pages;

  FilterListPanel(EntityFilter filter, FilterPurpose purpose, int x, int y, int width,
      int[] pages, Consumer<Boolean> add, Runnable changed) {
    this.filter = filter; this.purpose = purpose; this.x = x; this.y = y;
    this.width = width; this.pages = pages; this.add = add; this.changed = changed;
  }
  int top() { return y; }
  int columnWidth() { return (width - GAP) / 2; }
  /** Blocking and damage lists are red where they stop or hurt; the other list is green. */
  boolean red(boolean exclude) {
    return exclude != (purpose == FilterPurpose.BLOCKING || purpose == FilterPurpose.DAMAGE);
  }
  /** The green list is always on the left. */
  int columnX(boolean exclude) { return x + (red(exclude) ? columnWidth() + GAP : 0); }
  private boolean[] leftToRight() { return red(false) ? new boolean[]{true, false} : new boolean[]{false, true}; }
  Set<FilterTarget> entries(boolean exclude) { return exclude ? filter.excluded : filter.included; }
  static String text(String key, Object... args) { return UiText.text("screen.fieldemitters.targets." + key, args); }
  String heading(boolean exclude) { return text(purpose.name().toLowerCase(Locale.ROOT) + (exclude ? ".exclude" : ".include")); }

  void widgets(Consumer<FieldButton> register) {
    for (boolean exclude : leftToRight()) {
      int col = exclude ? 1 : 0, cx = columnX(exclude), cw = columnWidth();
      int pageCount = Math.max(1, (entries(exclude).size() + ROWS - 1) / ROWS);
      pages[col] = Math.min(pages[col], pageCount - 1);
      var plus = new FieldButton(cx + cw - 20, y, 20, 18, Component.literal("+"), b -> add.accept(exclude), false);
      plus.setTooltip(Tooltip.create(Component.literal(text("add_to", heading(exclude))))); register.accept(plus);
      var list = new ArrayList<>(entries(exclude));
      for (int i = 0; i < ROWS && pages[col] * ROWS + i < list.size(); i++) {
        var target = list.get(pages[col] * ROWS + i);
        var remove = new FieldButton(cx + cw - 19, y + 21 + i * ROW_HEIGHT, 17, 17,
            Component.literal("×"), b -> { entries(exclude).remove(target); changed.run(); }, false);
        remove.setTooltip(Tooltip.create(Component.literal(text("remove", name(target))))); register.accept(remove);
      }
    }
  }

  Boolean sideAt(double mx, double my) {
    if (my < y + 20 || my >= y + HEIGHT) return null;
    for (boolean exclude : new boolean[]{false, true})
      if (mx >= columnX(exclude) && mx < columnX(exclude) + columnWidth()) return exclude;
    return null;
  }
  boolean scroll(double mx, double my, double delta) {
    Boolean side = sideAt(mx,my); if (side == null || delta == 0) return false;
    int col = side ? 1 : 0, max = Math.max(0, (entries(side).size()-1)/ROWS);
    pages[col] = Math.max(0, Math.min(max, pages[col] + (delta < 0 ? 1 : -1))); changed.run(); return true;
  }
  void render(GuiGraphics g, int mx, int my) {
    var font = Minecraft.getInstance().font;
    for (boolean exclude : leftToRight()) {
      int cx=columnX(exclude), cw=columnWidth(), col=exclude?1:0;
      int color=red(exclude)?0xFFE09A9A:0xFF8DD5A2;
      g.drawString(font, heading(exclude), cx+3,y+5,color,false);
      if(entries(exclude).size()>ROWS) {
        String count=(pages[col]+1)+"/"+Math.max(1,(entries(exclude).size()+ROWS-1)/ROWS);
        g.drawString(font,count,cx+cw-25-font.width(count),y+5,color,false);
      }
      g.fill(cx,y+20,cx+cw,y+HEIGHT,color);
      g.fill(cx+1,y+21,cx+cw-1,y+HEIGHT-1,0xFF122330);
      var list=new ArrayList<>(entries(exclude));
      if(list.isEmpty()) {
        var lines=font.split(Component.literal(text("drop")),cw-12);
        for(int i=0;i<lines.size();i++) g.drawString(font,lines.get(i),cx+6,y+31+i*11,0xFFADBED0,false);
      }
      for(int i=0;i<ROWS && pages[col]*ROWS+i<list.size();i++) {
        var target=list.get(pages[col]*ROWS+i);int ry=y+22+i*ROW_HEIGHT;
        g.renderItem(icon(target),cx+3,ry);
        g.drawString(font,font.plainSubstrByWidth(name(target),cw-44),cx+22,ry+4,0xFFE0F3FF,false);
      }
    }
  }
  void tooltip(GuiGraphics g,int mx,int my) {
    Boolean side=sideAt(mx,my);if(side==null)return;
    int cw=columnWidth(),cx=columnX(side);if(mx>=cx+cw-20)return;
    var list=new ArrayList<>(entries(side));int index=pages[side?1:0]*ROWS+(my-y-21)/ROW_HEIGHT;
    if(index>=0 && index<list.size()) {
      var target=list.get(index);
      g.renderTooltip(Minecraft.getInstance().font,Component.literal(name(target)+"\n"+target.id()),mx,my);
    }
  }
  static String name(FilterTarget target) {
    if(target.kind()==FilterTarget.Kind.INDIVIDUAL) {
      var type=ResourceLocation.tryParse(target.name());
      String label=type==null?target.name():BuiltInRegistries.ENTITY_TYPE.get(type).getDescription().getString();
      return text("individual_name",label,target.id().substring(0,Math.min(8,target.id().length())));
    }
    if(target.kind()==FilterTarget.Kind.PLAYER) return target.name().isEmpty()?target.id():target.name();
    if(target.kind()==FilterTarget.Kind.CARD_GROUP) return text("card_group",target.id());
    if(target.id().startsWith("#"))return target.id();
    var id=ResourceLocation.tryParse(target.id());if(id==null)return target.id();
    return target.kind()==FilterTarget.Kind.ITEM?BuiltInRegistries.ITEM.get(id).getDescription().getString()
        :BuiltInRegistries.ENTITY_TYPE.get(id).getDescription().getString();
  }
  static ItemStack icon(FilterTarget target) {
    if(target.kind()==FilterTarget.Kind.PLAYER)return new ItemStack(Items.PLAYER_HEAD);
    if(target.kind()==FilterTarget.Kind.INDIVIDUAL)return new ItemStack(Items.NAME_TAG);
    if(target.kind()==FilterTarget.Kind.CARD_GROUP || target.id().startsWith("#"))return new ItemStack(Items.NAME_TAG);
    var id=ResourceLocation.tryParse(target.id());if(id==null)return new ItemStack(Items.BARRIER);
    if(target.kind()==FilterTarget.Kind.ITEM)return new ItemStack(BuiltInRegistries.ITEM.get(id));
    var egg=SpawnEggItem.byId(BuiltInRegistries.ENTITY_TYPE.get(id));return new ItemStack(egg==null?Items.PAPER:egg);
  }
  static FilterTarget fromStack(ItemStack stack) {
    if(stack.getItem() instanceof SpawnEggItem egg)
      return new FilterTarget(FilterTarget.Kind.MOB,BuiltInRegistries.ENTITY_TYPE.getKey(egg.getType(stack)).toString(),"");
    return new FilterTarget(FilterTarget.Kind.ITEM,BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),"");
  }
}
