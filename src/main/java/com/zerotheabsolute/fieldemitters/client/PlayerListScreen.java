package com.zerotheabsolute.fieldemitters.client;

import com.zerotheabsolute.fieldemitters.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

final class PlayerListScreen extends FittedScreen {
  private static final AtomicInteger REQUESTS = new AtomicInteger();
  private final net.minecraft.client.gui.screens.Screen parent;
  private final BlockPos pos;
  private final EntityFilter filter;
  private final int tab;
  private final Runnable apply;
  private int left, top, page, pending=-1;
  private long started;
  private String query="", status="Changes apply immediately.";
  private EditBox input;
  PlayerListScreen(net.minecraft.client.gui.screens.Screen parent, BlockPos pos, EntityFilter filter, int tab, Runnable apply) {
    super(Component.literal("PLAYER LIST"));this.parent=parent;this.pos=pos;this.filter=filter;this.tab=tab;this.apply=apply;
  }
  private String mode() {
    if (filter.playerMode==0) return "Players: use the general filter";
    if(tab==1) return filter.playerMode==1?"Blacklist: block listed players":"Whitelist: allow listed players only";
    String action=tab==2?"Detect":tab==4?"Inspect":"Damage";
    return action+(filter.playerMode==1?" listed players only":" players except those listed");
  }
  protected void init() {
    fit(404,306);left=(width-404)/2;top=(height-306)/2;
    button(mode(),12,32,380,() -> { filter.playerMode=(filter.playerMode+1)%3;status="Player list mode updated.";changed(); });
    button("Access groups / badges",12,80,380,() -> minecraft.setScreen(new AccessScreen(this,pos,filter,apply)));
    input=addRenderableWidget(new EditBox(font,left+12,top+110,214,18,Component.literal("Minecraft account name or UUID")));
    input.setMaxLength(36);input.setValue(query);input.setResponder(s -> query=s);
    input.setTooltip(Tooltip.create(Component.literal("Use a Minecraft account name, not a server nickname. Names resolve to UUIDs; UUIDs remain valid after a name change. Offline-mode identities depend on the server.")));
    var add=button(pending<0?"Add":"Looking up…",232,110,78,this::lookup);add.active=pending<0&&filter.playerList.size()<EntityFilter.MAX_PLAYERS;
    var self=button("Add myself",316,110,76,() -> { var p=minecraft.player;add(p.getUUID(),p.getGameProfile().getName()); });self.active=filter.playerList.size()<EntityFilter.MAX_PLAYERS;
    var entries=new ArrayList<>(filter.playerList.entrySet());
    int pages=Math.max(1,(entries.size()+4)/5);page=Math.min(page,pages-1);
    for(int i=0;i<5 && page*5+i<entries.size();i++) {
      var entry=entries.get(page*5+i);int y=145+i*18;
      var row=button(entry.getValue().isEmpty()?entry.getKey().toString():entry.getValue(),12,y,304,() -> {});
      row.setTooltip(Tooltip.create(Component.literal(entry.getKey().toString())));
      button("Remove",322,y,70,() -> { filter.playerList.remove(entry.getKey());status="Player removed.";changed(); });
    }
    var prev=button("Previous",12,241,84,() -> {page--;rebuildWidgets();});prev.active=page>0;
    var next=button("Next",308,241,84,() -> {page++;rebuildWidgets();});next.active=page+1<pages;
    button("Back",308,281,84,this::onClose);
  }
  private FieldButton button(String text,int x,int y,int w,Runnable action) {
    return addRenderableWidget(new FieldButton(left+x,top+y,w,18,Component.literal(text),b->action.run(),false));
  }
  private void changed() { apply.run();rebuildWidgets(); }
  private void lookup() {
    if(query.isBlank()) {status="Enter a Minecraft account name or UUID.";return;}
    pending=REQUESTS.incrementAndGet();started=net.minecraft.Util.getMillis();status="Resolving player…";
    PacketDistributor.sendToServer(new PlayerLookup.Request(pos,pending,query.trim()));rebuildWidgets();
  }
  private void add(UUID id,String name) {
    if(!filter.playerList.containsKey(id)&&filter.playerList.size()>=EntityFilter.MAX_PLAYERS) {status="The list is full (64 players).";return;}
    boolean existed=filter.playerList.containsKey(id);
    filter.playerList.put(id,name);query="";status=existed?"Player already listed; name refreshed.":"Player added. Choose the list mode above to enable it.";
    page=(filter.playerList.size()-1)/5;changed();
  }
  public static void receive(PlayerLookup.Result result) {
    if(!(Minecraft.getInstance().screen instanceof PlayerListScreen screen)||screen.pending!=result.requestId())return;
    screen.pending=-1;
    if(!result.error().isEmpty()) {screen.status=result.error();screen.rebuildWidgets();return;}
    try {screen.add(UUID.fromString(result.uuid()),result.name());}
    catch(IllegalArgumentException e) {screen.status="The lookup returned an invalid UUID.";screen.rebuildWidgets();}
  }
  public void tick() {
    super.tick();
    if(pending>=0&&net.minecraft.Util.getMillis()-started>10000) {pending=-1;status="Lookup timed out. Try again or enter a UUID.";rebuildWidgets();}
  }
  public boolean isPauseScreen() {return false;}
  public void onClose() {minecraft.setScreen(parent);}
  public void renderBackground(GuiGraphics g,int x,int y,float partial) {}
  public void render(GuiGraphics g,int mx,int my,float partial) {
    beginFit(g);
    g.fill(left,top,left+404,top+306,0xFF0D1D2B);g.fill(left,top,left+404,top+2,0xFF53BBCB);
    g.drawString(font,"PLAYER LIST / "+new String[]{"","BLOCK","DETECT","DAMAGE","CHECKPOINT"}[tab],left+12,top+12,0xFFE0F3FF,false);
    g.drawWordWrap(font,Component.literal("List modes replace the general filter for players only. Mob and item rules stay as configured. Movement directions and Skip owner still apply."),left+12,top+58,380,0xFFADBED0);
    g.drawString(font,"Minecraft account name or UUID",left+12,top+98,0xFFADBED0,false);
    g.drawString(font,"Saved players",left+12,top+133,0xFFADBED0,false);
    if(filter.playerList.isEmpty())g.drawString(font,"No players added yet.",left+12,top+153,0xFFADBED0,false);
    g.drawCenteredString(font,"Page "+(page+1)+" / "+Math.max(1,(filter.playerList.size()+4)/5),left+202,top+246,0xFFADBED0);
    g.drawWordWrap(font,Component.literal(status),left+12,top+265,280,0xFFADBED0);
    super.render(g,fitMouse(mx),fitMouse(my),partial);g.pose().popPose();
  }
}
