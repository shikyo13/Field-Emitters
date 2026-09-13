package com.zerotheabsolute.fieldemitters.client;

import com.zerotheabsolute.fieldemitters.*;
import com.zerotheabsolute.fieldemitters.client.emi.EmiScreenBridge;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

/** Ghost slots store type references only; inventory stacks never move or shrink. */
public final class TypeListScreen extends FittedScreen {
  private final net.minecraft.client.gui.screens.Screen parent;
  private final EntityFilter filter;
  private final boolean items;
  private final int tab;
  private final Runnable apply;
  private int left, top, page;
  private final boolean emi = net.neoforged.fml.ModList.get().isLoaded("emi");
  private final int browserInset = emi || net.neoforged.fml.ModList.get().isLoaded("jei") ? 24 : 0;
  private String query = "", status = "Drag from your inventory, JEI or EMI into a filter box.";
  private long legacyChanged;
  private ItemStack dragging = ItemStack.EMPTY;

  TypeListScreen(net.minecraft.client.gui.screens.Screen parent, EntityFilter filter, boolean items, int tab, Runnable apply) {
    super(Component.literal(items ? "ITEM LIST" : "MOB LIST"));
    this.parent=parent;this.filter=filter;this.items=items;this.tab=tab;this.apply=apply;
  }
  private Set<String> entries() { return items ? filter.itemList : filter.mobList; }
  private int mode() { return items ? filter.itemMode : filter.mobMode; }
  private String modeLabel() {
    String noun=items?"items":"mobs";
    if(mode()==0)return "Use general filter";
    if(tab==4)return mode()==1?"Contraband: listed items":"Contraband: everything except listed items";
    if(tab==1)return mode()==1?"Blacklist: block listed "+noun:"Whitelist: allow listed "+noun+" only";
    return (mode()==1?"Whitelist: ":"Blacklist: ")+(tab==2?"detect ":"damage ")+(mode()==1?"listed ":"unlisted ")+noun;
  }
  protected void init() {
    fit(404,306,browserInset);left=(width-404)/2;top=(height-306)/2;
    // Recipe browsers measure Screen.width/height in real GUI coordinates.
    width=minecraft.getWindow().getGuiScaledWidth();height=minecraft.getWindow().getGuiScaledHeight();
    button(modeLabel(),12,30,380,() -> {
      if(items)filter.itemMode=(mode()+1)%3;else filter.mobMode=(mode()+1)%3;
      status="List mode updated.";changed();
    }).setTooltip(Tooltip.create(Component.literal("General filter preserves the existing category and single-type rule. List modes override categories, single-type rules and general inversion for this kind of entity only. Direction and Skip owner still apply. Mob age, UUID and custom label narrow the listed selection before whitelist/blacklist is applied.")));
    var legacy=new EditBox(font,left+12,top+76,380,16,Component.literal("General filter: single type or #tag"));
    legacy.setMaxLength(128);legacy.setValue(items?filter.itemType:filter.entityType);legacy.active=mode()==0;
    legacy.setTooltip(Tooltip.create(Component.literal("The previous single-type restriction. Only used in General filter mode; blank means any type. Changes apply after a short typing pause.")));
    legacy.setResponder(value -> {if(items)filter.itemType=value;else filter.entityType=value;legacyChanged=net.minecraft.Util.getMillis();});addRenderableWidget(legacy);
    var input=addRenderableWidget(new EditBox(font,left+12,top+106,294,18,Component.literal("Add registry ID or #tag")));
    input.setMaxLength(128);input.setValue(query);input.setResponder(value -> query=value);
    input.setTooltip(Tooltip.create(Component.literal(items?"Item ID: minecraft:gunpowder. Item tag: #minecraft:logs. For checkpoints, matches carried inventory items; otherwise matches dropped item entities. Item count and components are ignored.":"Mob ID: minecraft:creeper. Entity-type tag: #minecraft:skeletons. Drag a spawn egg to add its mob type. This is not a /tag label. Players have their own list.")));
    button("Add",312,106,80,() -> add(query));
    int pages=Math.max(1,(entries().size()+23)/24);page=Math.min(page,pages-1);
    button("Previous",312,134,80,() -> {page--;rebuildWidgets();}).active=page>0;
    button("Next",312,156,80,() -> {page++;rebuildWidgets();}).active=page+1<pages;
    button("Back",312,284,80,this::onClose);
    if(emi)EmiScreenBridge.init(this);
  }
  private FieldButton button(String text,int x,int y,int w,Runnable action) {
    return addRenderableWidget(new FieldButton(left+x,top+y,w,18,Component.literal(text),b->action.run(),false));
  }
  private void changed() {apply.run();rebuildWidgets();}
  private void add(String value) {
    value=value.trim();
    String error=FieldControls.validateTypeEntry(value,items);
    if(error!=null){status=error;return;}
    boolean tag=value.startsWith("#");value=(tag?"#":"")+ResourceLocation.parse(tag?value.substring(1):value);
    if(entries().contains(value)){status="Already in this list.";return;}
    if(entries().size()>=EntityFilter.MAX_TYPES){status="The list is full (64 entries).";return;}
    entries().add(value);query="";page=(entries().size()-1)/24;
    status=mode()==0?"Added. Choose whitelist or blacklist to enable the list.":"Added. Changes applied.";changed();
  }
  public boolean canAcceptDrop(ItemStack stack) {return !stack.isEmpty()&&(items||stack.getItem() instanceof SpawnEggItem);}
  public void acceptDrop(ItemStack stack) {
    if(!canAcceptDrop(stack)){status="Use a spawn egg for a mob, or type its ID above.";return;}
    add(items?BuiltInRegistries.ITEM.getKey(stack.getItem()).toString():BuiltInRegistries.ENTITY_TYPE.getKey(((SpawnEggItem)stack.getItem()).getType(stack)).toString());
  }
  private Rect2i physical(int x,int y,int w,int h) {
    // CanvasFit can truncate the logical height; derive the exact scale from its shared model.
    double s=com.zeromods.core.ui.CanvasFit.fit(minecraft.getWindow().getGuiScaledWidth(),minecraft.getWindow().getGuiScaledHeight()-(browserInset),404,306,4).scale();
    return new Rect2i((int)Math.ceil(x*s),(int)Math.ceil(y*s),(int)Math.floor(w*s),(int)Math.floor(h*s));
  }
  public Rect2i dropArea() {return physical(left+12,top+132,288,44);}
  public Rect2i panelArea() {return physical(left,top,404,306);}
  private boolean inside(int x,int y,int bx,int by,int w,int h){return x>=bx&&x<bx+w&&y>=by&&y<by+h;}
  public boolean mouseClicked(double x,double y,int button) {
    if(emi && EmiScreenBridge.click(x,y,button))return true;
    int mx=fitMouse((int)x),my=fitMouse((int)y);
    if(button==0&&inside(mx,my,left+12,top+208,162,72)) {
      int slot=(my-top-208)/18*9+(mx-left-12)/18;
      int inventorySlot=slot<27?slot+9:slot-27;
      dragging=minecraft.player.getInventory().getItem(inventorySlot).copy();return true;
    }
    if(button==1&&inside(mx,my,left+12,top+132,288,44)) {
      int slot=(my-top-132)/22*12+(mx-left-12)/24+page*24;
      var list=new ArrayList<>(entries());
      if(slot<list.size()){entries().remove(list.get(slot));status="Removed. Changes applied.";changed();}return true;
    }
    return super.mouseClicked(x,y,button);
  }
  public boolean mouseReleased(double x,double y,int button) {
    if(emi && EmiScreenBridge.release(x,y,button))return true;
    if(button==0&&!dragging.isEmpty()) {
      var stack=dragging;dragging=ItemStack.EMPTY;
      if(inside(fitMouse((int)x),fitMouse((int)y),left+12,top+132,288,44))acceptDrop(stack);
      return true;
    }
    return super.mouseReleased(x,y,button);
  }
  public boolean mouseDragged(double x,double y,int button,double dx,double dy) {
    if(emi && EmiScreenBridge.drag(x,y,button,dx,dy))return true;
    return super.mouseDragged(x,y,button,dx,dy);
  }
  public boolean mouseScrolled(double x,double y,double dx,double dy) {
    if(emi && EmiScreenBridge.scroll(x,y,dy))return true;
    return super.mouseScrolled(x,y,dx,dy);
  }
  public boolean charTyped(char value,int modifiers) {
    if(emi && EmiScreenBridge.character(value,modifiers))return true;
    return super.charTyped(value,modifiers);
  }
  public boolean keyPressed(int key,int scan,int modifiers) {
    if(emi && EmiScreenBridge.key(key,scan,modifiers))return true;
    if(key==257 || key==335) {if(!query.isBlank())add(query);else apply.run();return true;}
    return super.keyPressed(key,scan,modifiers);
  }
  public void tick() {
    super.tick();
    if(legacyChanged!=0 && net.minecraft.Util.getMillis()-legacyChanged>350) {
      legacyChanged=0;String error=FieldControls.validate(filter);
      if(error==null){apply.run();status="Changes applied.";}else status=error;
    }
  }
  public boolean isPauseScreen(){return false;}
  public void onClose(){apply.run();minecraft.setScreen(parent);}
  public void renderBackground(GuiGraphics g,int x,int y,float partial){}
  private ItemStack icon(String value) {
    if(value.startsWith("#"))return new ItemStack(Items.NAME_TAG);
    var id=ResourceLocation.tryParse(value);if(id==null)return new ItemStack(Items.BARRIER);
    if(items)return new ItemStack(BuiltInRegistries.ITEM.get(id));
    var egg=SpawnEggItem.byId(BuiltInRegistries.ENTITY_TYPE.get(id));return new ItemStack(egg==null?Items.PAPER:egg);
  }
  public void render(GuiGraphics g,int mx,int my,float partial) {
    int x=fitMouse(mx),y=fitMouse(my);beginFit(g);
    g.fill(left,top,left+404,top+306,0xFF0D1D2B);g.fill(left,top,left+404,top+2,0xFF53BBCB);
    g.drawString(font,(items?"ITEM":"MOB")+" LIST / "+new String[]{"","BLOCK","DETECT","DAMAGE","CONTRABAND"}[tab],left+12,top+12,0xFFE0F3FF,false);
    g.drawString(font,items?(tab==4?"Matching items carried by selected players are contraband.":"Dropped items only; players and mobs keep their own rules."):"Mobs only; age, UUID and label narrow the listed selection.",left+12,top+54,0xFFADBED0,false);
    g.drawString(font,"General filter: single type or #tag",left+12,top+66,0xFFADBED0,false);
    g.drawString(font,"Add "+(items?"item":"mob")+" ID or #tag",left+12,top+96,0xFFADBED0,false);
    var list=new ArrayList<>(entries());String hovered=null;
    for(int i=0;i<24;i++) {
      int sx=left+12+i%12*24,sy=top+132+i/12*22;
      g.fill(sx,sy,sx+22,sy+20,0xFF356071);g.fill(sx+1,sy+1,sx+21,sy+19,0xFF162F40);
      if(page*24+i<list.size()) {
        String value=list.get(page*24+i);g.renderItem(icon(value),sx+3,sy+2);
        if(inside(x,y,sx,sy,22,20))hovered=value+"\nRight-click to remove";
      }
    }
    g.drawString(font,"Page "+(page+1),left+12,top+178,0xFFADBED0,false);
    g.drawString(font,font.plainSubstrByWidth(status,380),left+12,top+190,0xFF8DE0CF,false);
    g.drawString(font,"Your inventory (drag a copy into the boxes)",left+12,top+199,0xFFADBED0,false);
    for(int i=0;i<36;i++) {
      int sx=left+12+i%9*18,sy=top+208+i/9*18;
      var stack=minecraft.player.getInventory().getItem(i<27?i+9:i-27);
      g.fill(sx,sy,sx+17,sy+17,0xFF294455);g.renderItem(stack,sx+1,sy+1);g.renderItemDecorations(font,stack,sx+1,sy+1);
      if(!stack.isEmpty()&&inside(x,y,sx,sy,18,18))hovered=stack.getHoverName().getString();
    }
    g.drawWordWrap(font,Component.literal(items?"Drag items from your inventory, JEI or EMI.\n\nStacks stay in your inventory.":"Drag spawn eggs from your inventory, JEI or EMI.\n\nOr type a mob ID or tag above."),left+190,top+213,196,0xFFADBED0);
    g.drawString(font,"Right-click a filter icon to remove it.",left+12,top+289,0xFFADBED0,false);
    super.render(g,x,y,partial);
    if(!dragging.isEmpty())g.renderItem(dragging,x-8,y-8);
    else if(hovered!=null)g.renderComponentTooltip(font,Arrays.stream(hovered.split("\n")).map(Component::literal).map(c -> (Component)c).toList(),x,y);
    g.pose().popPose();
    if(emi)EmiScreenBridge.render(g,mx,my,partial);
  }
}
