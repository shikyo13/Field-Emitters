package com.zerotheabsolute.fieldemitters.client;

import com.zeromods.core.client.FittedScreen;
import com.zerotheabsolute.fieldemitters.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Sample, manual entry and online player selection for either exception list. */
final class TargetEntryScreen extends FittedScreen {
  private static final int PANEL_WIDTH=404, PANEL_HEIGHT=326, SAMPLE_OFFSET=76;
  private static final FilterTarget.Kind[] MANUAL_KINDS={FilterTarget.Kind.MOB,FilterTarget.Kind.ITEM,FilterTarget.Kind.PLAYER,FilterTarget.Kind.CARD_GROUP};
  private FilterTarget sampledType, sampledIndividual;
  private final ControlScreen parent;
  private final EntityFilter filter;
  private final boolean exclude;
  private final BlockPos pos;
  private final Runnable apply;
  private FilterTarget.Kind kind=FilterTarget.Kind.MOB;
  private String query="",status="";
  private int left,top,pending=-1;
  private long started;
  TargetEntryScreen(ControlScreen parent,EntityFilter filter,boolean exclude,BlockPos pos,Runnable apply,String title) {
    super(Component.literal(title));this.parent=parent;this.filter=filter;this.exclude=exclude;this.pos=pos;this.apply=apply;
  }
  private String text(String key,Object...args){return FilterListPanel.text(key,args);}
  protected void init(){
    fit(PANEL_WIDTH,PANEL_HEIGHT);left=(width-PANEL_WIDTH)/2;top=(height-PANEL_HEIGHT)/2;
    readSample();
    var typeButton=addRenderableWidget(new FieldButton(left+12,top+65,186,20,Component.literal(text("sample_type")),b->add(sampledType),false));
    typeButton.active=sampledType!=null;
    typeButton.setTooltip(Tooltip.create(Component.literal(text("sample_type_help"))));
    var individualButton=addRenderableWidget(new FieldButton(left+204,top+65,188,20,Component.literal(text("sample_individual")),b->add(sampledIndividual),false));
    individualButton.active=sampledIndividual!=null;
    individualButton.setTooltip(Tooltip.create(Component.literal(text("sample_individual_help"))));
    for(var k:MANUAL_KINDS) {
      var b=new FieldButton(left+12+k.ordinal()*96,top+SAMPLE_OFFSET+34,92,20,Component.literal(text("kind."+k.name().toLowerCase(Locale.ROOT))),
          button->{kind=k;query="";rebuildWidgets();},kind==k);addRenderableWidget(b);
    }
    var input=new FieldEditBox(font,left+12,top+SAMPLE_OFFSET+79,288,20,Component.literal(text("entry")));
    input.setMaxLength(EntityFilter.MAX_TYPE_LENGTH);input.setValue(query);input.setResponder(s->query=s);addRenderableWidget(input);
    addRenderableWidget(new FieldButton(left+306,top+SAMPLE_OFFSET+79,86,20,Component.literal(text("add")),b->submit(),false)).active=pending<0;
    if(kind==FilterTarget.Kind.PLAYER && minecraft.getConnection()!=null){
      int i=0;
      for(var info:minecraft.getConnection().getOnlinePlayers()){
        var profile=info.getProfile();int index=i++;
        if(index>=8)break;
        addRenderableWidget(new FieldButton(left+12+(index%2)*192,top+SAMPLE_OFFSET+123+(index/2)*23,186,20,Component.literal(profile.getName()),
            b->add(new FilterTarget(kind,profile.getId().toString(),profile.getName())),false));
      }
    }
    addRenderableWidget(new FieldButton(left+308,top+SAMPLE_OFFSET+221,84,20,Component.literal(UiText.text("screen.fieldemitters.access.back")),b->onClose(),false));
  }
  private void readSample(){
    sampledType=null;sampledIndividual=null;
    if(minecraft.player==null)return;
    var stacks=new ArrayList<net.minecraft.world.item.ItemStack>();
    stacks.add(minecraft.player.getMainHandItem());
    stacks.add(minecraft.player.getOffhandItem());
    stacks.addAll(minecraft.player.getInventory().items);
    for(var stack:stacks){
      if(!stack.is(FieldEmitters.TUNER.get()))continue;
      var data=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
      if(data==null)continue;
      var tag=data.copyTag();
      var type=new FilterTarget(FilterTarget.Kind.MOB,tag.getString("SampleType"),"");
      var individual=new FilterTarget(FilterTarget.Kind.INDIVIDUAL,tag.getString("SampleUUID"),tag.getString("SampleType"));
      if(type.validate()!=null||individual.validate()!=null)continue;
      sampledType=type;
      sampledIndividual=type.id().equals("minecraft:player")?new FilterTarget(FilterTarget.Kind.PLAYER,individual.id(),""):individual;
      return;
    }
  }
  private void submit(){
    if(kind==FilterTarget.Kind.PLAYER){
      if(query.isBlank())return;
      pending=PlayerLookupSequence.next();started=net.minecraft.Util.getMillis();
      PacketDistributor.sendToServer(new PlayerLookup.Request(pos,pending,query.trim()));rebuildWidgets();return;
    }
    add(new FilterTarget(kind,query.trim(),""));
  }
  private void add(FilterTarget target){
    Component error=target.validate();if(error!=null){status=error.getString();return;}
    var entries=exclude?filter.excluded:filter.included;
    if(entries.size()>=EntityFilter.MAX_TYPES && entries.stream().noneMatch(t->t.kind()==target.kind()&&t.id().equals(target.id()))){status=text("full");return;}
    filter.addTarget(target,exclude);apply.run();onClose();
  }
  static boolean receive(PlayerLookup.Result result){
    if(!(Minecraft.getInstance().screen instanceof TargetEntryScreen s)||s.pending!=result.requestId())return false;
    s.pending=-1;
    if(!result.error().getString().isEmpty()){s.status=result.error().getString();s.rebuildWidgets();}
    else s.add(new FilterTarget(FilterTarget.Kind.PLAYER,result.uuid(),result.name()));
    return true;
  }
  public void tick(){if(pending>=0&&net.minecraft.Util.getMillis()-started>ScreenMetrics.LOOKUP_TIMEOUT_MILLIS){pending=-1;status=text("lookup_timeout");rebuildWidgets();}}
  public void onClose(){minecraft.setScreen(parent);}
  public boolean isPauseScreen(){return false;}
  public void renderBackground(GuiGraphics g,int x,int y,float p){}
  public void render(GuiGraphics g,int mx,int my,float p){
    beginFit(g);int x=fitMouse(mx),y=fitMouse(my);
    g.fill(left,top,left+PANEL_WIDTH,top+PANEL_HEIGHT,0xFF0D1D2B);g.fill(left,top,left+PANEL_WIDTH,top+2,0xFF53BBCB);
    g.drawString(font,title,left+12,top+12,0xFFE0F3FF,false);
    String sampleLabel=sampledType==null?text("sample_empty"):text("sample_stored",FilterListPanel.name(sampledType),sampledIndividual.id().substring(0,8));
    g.drawWordWrap(font,Component.literal(sampleLabel),left+12,top+33,380,0xFFADBED0);
    g.drawString(font,text("entry."+kind.name().toLowerCase(Locale.ROOT)),left+12,top+SAMPLE_OFFSET+65,0xFFADBED0,false);
    if(kind==FilterTarget.Kind.PLAYER)g.drawString(font,text("online"),left+12,top+SAMPLE_OFFSET+109,0xFFADBED0,false);
    else g.drawWordWrap(font,Component.literal(text("manual_help")),left+12,top+SAMPLE_OFFSET+115,380,0xFFADBED0);
    g.drawWordWrap(font,Component.literal(status),left+12,top+SAMPLE_OFFSET+204,288,0xFFE0AD8D);
    super.render(g,x,y,p);g.pose().popPose();
  }
}
