package com.zerotheabsolute.fieldemitters;
import com.zeromods.core.neoforge.NetworkSavedData;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
@GameTestHolder("fieldemitters")
@PrefixGameTestTemplate(false)
public final class CoreIntegrationGameTests {
    @GameTest(template="core_empty")
    public static void legacyFieldMigratesAndCoreDiskCodecPreservesIdentity(GameTestHelper helper) {
        var level=helper.getLevel();
        var owner=UUID.randomUUID();
        var a=place(helper,new BlockPos(2,1,2),owner,10);
        var b=place(helper,new BlockPos(6,1,2),owner,20);
        a.fieldName="Test fence";
        ManagedFields.refresh(level,FieldNetwork.loaded(level));
        helper.assertTrue(a.managedNetwork != null && a.managedNetwork==b.managedNetwork,"Connected emitters did not share Core network");
        var id=a.managedNetwork.id();
        helper.assertValueEqual(a.managedNetwork.name(),"Test fence","Legacy name");
        ManagedFields.rename(level,b,"Workshop");
        var data=NetworkSavedData.get(level,"fieldemitters_core_networks");
        var tag=data.save(new CompoundTag(),level.registryAccess());
        var restored=NetworkSavedData.load(tag,level.registryAccess());
        var snapshot=restored.directory().get(id).orElseThrow().snapshot();
        helper.assertValueEqual(snapshot.name(),"Workshop","Core name persisted");
        helper.assertValueEqual(snapshot.owner(),owner,"Owner persisted");
        helper.assertTrue(snapshot.nodes().containsAll(List.of(a.getBlockPos(),b.getBlockPos())),"Nodes persisted");
        helper.assertValueEqual(snapshot.anchor(),a.getBlockPos(),"Oldest emitter is anchor");
        helper.assertTrue(restored.directory().loadedNodes(id).isEmpty(),"Availability must not persist as loaded");
        helper.succeed();
    }
    @GameTest(template="core_empty")
    public static void villagersAgeAndDirectionsKeepIndependentBehavior(GameTestHelper helper) {
        var villager=helper.spawn(EntityType.VILLAGER,new BlockPos(2,1,2));villager.setAge(-100);
        var filter=new EntityFilter();filter.groups=2;filter.entityType="minecraft:villager";filter.age=1;
        filter.directions=1<<Direction.NORTH.ordinal();
        helper.assertTrue(filter.matches(villager,null),"Baby villager should match");
        helper.assertTrue(filter.direction(Direction.NORTH) && !filter.direction(Direction.SOUTH),"One-way blocking directions");
        villager.setAge(0);helper.assertTrue(!filter.matches(villager,null),"Adult must not match baby filter");
        filter.inverted=true;filter.exemptOwner=true;
        helper.assertTrue(!filter.matches(villager,villager.getUUID()),"Owner exemption must remain outside inversion");
        var restored=EntityFilter.load(filter.save());
        helper.assertTrue(restored.directions==filter.directions && restored.exemptOwner && restored.inverted,"Legacy filter codec");
        helper.succeed();
    }
    @GameTest(template="core_empty")
    public static void independentDirectionalFiltersControlCollisionAndCounting(GameTestHelper helper) {
        var level=helper.getLevel();var owner=UUID.randomUUID();
        var a=place(helper,new BlockPos(2,1,2),owner,10);var b=place(helper,new BlockPos(6,1,2),owner,20);
        var pos=a.getBlockPos();int y=pos.getY();
        a.links=List.of(new EmitterEntity.Link(b.getBlockPos(),1,0,new int[]{y,y,y,y,y}));
        a.powered=true;a.transition=level.getGameTime()-100;a.controls.sensorMode=1;
        var south=new EntityFilter();south.groups=2;south.entityType="minecraft:villager";south.inverted=true;
        var north=new EntityFilter();north.groups=2;north.entityType="minecraft:cow";north.inverted=true;
        a.controls.barrierDirections.set(Direction.SOUTH,south);a.controls.barrierDirections.set(Direction.NORTH,north);
        var detectSouth=EntityFilter.load(south.save());detectSouth.inverted=false;
        var detectNorth=EntityFilter.load(north.save());detectNorth.inverted=false;
        a.controls.sensorDirections.set(Direction.SOUTH,detectSouth);a.controls.sensorDirections.set(Direction.NORTH,detectNorth);
        a.controls=ControlSettings.load(a.controls.save());
        helper.assertTrue(a.controls.barrierDirections.has(Direction.NORTH) && a.controls.sensorDirections.has(Direction.SOUTH),"Directional overrides must persist");
        var villager=helper.spawn(EntityType.VILLAGER,new BlockPos(4,1,2));villager.setNoAi(true);
        var cell=pos.offset(2,0,0);
        villager.setPos(cell.getX()+.5,y,pos.getZ()+.3);
        helper.assertTrue(FieldBlock.collision(a,villager,cell).isEmpty(),"Villager must pass north to south");
        villager.setPos(cell.getX()+.5,y,pos.getZ()+.7);
        helper.assertTrue(!FieldBlock.collision(a,villager,cell).isEmpty(),"Villager must be blocked south to north");
        long now=level.getGameTime();
        villager.setPos(cell.getX()+.5,y,pos.getZ()-.3);FieldSensor.tick(level,a,now);
        villager.setPos(cell.getX()+.5,y,pos.getZ()+.5);FieldSensor.tick(level,a,now+1);
        villager.setPos(cell.getX()+.5,y,pos.getZ()+1.3);FieldSensor.tick(level,a,now+2);
        helper.assertTrue(a.crossings==1,"Southbound villager should be counted once");
        villager.setPos(cell.getX()+.5,y,pos.getZ()+.5);FieldSensor.tick(level,a,now+3);
        villager.setPos(cell.getX()+.5,y,pos.getZ()-.3);FieldSensor.tick(level,a,now+4);
        helper.assertTrue(a.crossings==1,"Northbound villager must not count with cow-only rule");
        var cow=helper.spawn(EntityType.COW,new BlockPos(4,1,3));cow.setNoAi(true);
        cow.setPos(cell.getX()+.5,y,pos.getZ()+1.3);FieldSensor.tick(level,a,now+5);
        cow.setPos(cell.getX()+.5,y,pos.getZ()+.5);FieldSensor.tick(level,a,now+6);
        cow.setPos(cell.getX()+.5,y,pos.getZ()-.3);FieldSensor.tick(level,a,now+7);
        helper.assertTrue(a.crossings==2,"Northbound cow should use independent detection rule");
        a.controls.barrierDirections.inherit(Direction.NORTH);
        helper.assertTrue(a.controls.barrier(Direction.NORTH)==a.controls.barrier,"Removing override restores shared fallback");
        helper.succeed();
    }

    @GameTest(template="core_empty")
    public static void directionalDamageIsIndependentAndCannotStackAtSeams(GameTestHelper helper) {
        var level=helper.getLevel();var a=place(helper,new BlockPos(2,1,2),UUID.randomUUID(),10);
        var p=a.getBlockPos();int y=p.getY();
        var link=new EmitterEntity.Link(p.east(4),1,0,new int[]{y,y,y,y,y});
        a.links=List.of(link,link);a.powered=true;a.transition=level.getGameTime()-100;
        a.controls.barrier.groups=0;a.controls.damageEnabled=true;a.controls.damageAmount=4;
        a.controls.damage.groups=0;a.controls.sounds=false;a.controls.fizzleEffects=false;
        var rule=new EntityFilter();rule.groups=2;rule.entityType="minecraft:villager";
        a.controls.damageDirections.set(Direction.SOUTH,rule);
        var villager=helper.spawn(EntityType.VILLAGER,new BlockPos(4,1,2));villager.setNoAi(true);
        villager.setPos(p.getX()+2.5,y,p.getZ()+.3);
        villager.xo=villager.getX();villager.yo=villager.getY();villager.zo=villager.getZ();
        float health=villager.getHealth();long now=level.getGameTime();
        FieldDamage.tick(level,a,now);
        helper.assertTrue(villager.getHealth()==health-4,"Damage must happen once despite overlapping links");
        villager.invulnerableTime=0;FieldDamage.tick(level,a,now+1);
        helper.assertTrue(villager.getHealth()==health-4,"Shared field cooldown must prevent a second hit");
        villager.setPos(p.getX()+2.5,y,p.getZ()+.7);villager.zo=villager.getZ();
        FieldDamage.tick(level,a,now+21);
        helper.assertTrue(villager.getHealth()==health-4,"Opposite direction must retain its own no-damage filter");
        a.controls=ControlSettings.load(a.controls.save());
        helper.assertTrue(a.controls.damageDirections.has(Direction.SOUTH) && a.controls.damageAmount==4 && !a.controls.sounds,"Damage/sound rules persist");
        helper.assertTrue(FieldBlock.collision(a,villager,p.east(2)).isEmpty(),"Damage must not add blocking");
        var invalid=new CompoundTag();invalid.putFloat("DamageAmount",Float.NaN);
        helper.assertTrue(ControlSettings.load(invalid).damageAmount==0,"Nonfinite damage fails safe");
        helper.assertTrue(!ControlSettings.load(new CompoundTag()).damageEnabled,"Legacy saves start with damage off");
        helper.succeed();
    }

    @GameTest(template="core_empty")
    public static void horizontalRailsSupportWalkingAndRespectUpDownFilters(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2,3,2),FieldEmitters.RAIL.get().defaultBlockState().setValue(RailBlock.FACING,Direction.EAST));
        helper.setBlock(new BlockPos(6,3,2),FieldEmitters.RAIL.get().defaultBlockState().setValue(RailBlock.FACING,Direction.WEST));
        var a=(EmitterEntity)helper.getBlockEntity(new BlockPos(2,3,2));
        var b=(EmitterEntity)helper.getBlockEntity(new BlockPos(6,3,2));
        var p=a.getBlockPos();int y=p.getY();
        a.links=List.of(new EmitterEntity.Link(b.getBlockPos(),1,0,new int[]{y,y,y,y,y},0,Direction.Axis.Y,true));
        a.powered=true;a.transition=helper.getLevel().getGameTime()-100;
        a.controls.barrier.groups=31;a.controls.barrier.exemptOwner=false;
        a.controls.barrier.directions=1<<Direction.DOWN.ordinal();
        a.network=List.of(a,b);b.network=List.of(a,b);
        for(int i=1;i<4;i++) {
            helper.setBlock(new BlockPos(2+i,3,2),FieldEmitters.FIELD.get());
            var cell=(FieldCell)helper.getBlockEntity(new BlockPos(2+i,3,2));cell.source=p;
            a.cells.add(cell.getBlockPos());
        }
        var villager=helper.spawn(EntityType.VILLAGER,new BlockPos(4,5,2));villager.setNoAi(true);
        villager.setPos(p.getX()+1.5,y+1,p.getZ()+.5);
        villager.move(net.minecraft.world.entity.MoverType.SELF,new net.minecraft.world.phys.Vec3(0,-2,0));
        helper.assertTrue(Math.abs(villager.getY()-(y+.5))<.001,"Horizontal bridge must support feet on the rendered surface");
        villager.move(net.minecraft.world.entity.MoverType.SELF,new net.minecraft.world.phys.Vec3(2,0,0));
        villager.move(net.minecraft.world.entity.MoverType.SELF,new net.minecraft.world.phys.Vec3(0,-.3,0));
        helper.assertTrue(Math.abs(villager.getY()-(y+.5))<.001,"Walking across field cells must not fall through seams");
        villager.setPos(p.getX()+2.5,y-2,p.getZ()+.5);
        helper.assertTrue(FieldBlock.collision(a,villager,p.east(2)).isEmpty(),"Upward passage should be allowed");
        a.powered=false;villager.setPos(p.getX()+2.5,y+1,p.getZ()+.5);
        helper.assertTrue(FieldBlock.collision(a,villager,p.east(2)).isEmpty(),"Unpowered bridge must have no field collision");
        helper.succeed();
    }

    @GameTest(template="core_empty")
    public static void damageHandlesItemsPlayersAndOwnerExemption(GameTestHelper helper) {
        var level=helper.getLevel();var a=place(helper,new BlockPos(2,1,2),UUID.randomUUID(),10);
        var p=a.getBlockPos();int y=p.getY();
        a.links=List.of(new EmitterEntity.Link(p.east(4),1,0,new int[]{y,y,y,y,y}));
        a.powered=true;a.transition=level.getGameTime()-100;a.controls.damageEnabled=true;
        a.controls.damageAmount=10;a.controls.damage.groups=8;a.controls.sounds=false;a.controls.fizzleEffects=false;
        var item=new net.minecraft.world.entity.item.ItemEntity(level,p.getX()+2.5,y+.3,p.getZ()+.5,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GUNPOWDER));
        level.addFreshEntity(item);FieldDamage.tick(level,a,level.getGameTime());
        helper.assertTrue(!item.isAlive(),"Damageable dropped items should fizzle using their normal damage handler");
        var player=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(p.getX()+2.5,y,p.getZ()+.3);level.addFreshEntity(player);
        player.xo=player.getX();player.yo=player.getY();player.zo=player.getZ();
        a.owner=player.getUUID();a.controls.damage.groups=4;a.controls.damageAmount=2;
        float health=player.getHealth();FieldDamage.tick(level,a,level.getGameTime());
        helper.assertTrue(player.getHealth()==health,"Skip owner must prevent damage");
        a.controls.damage.exemptOwner=false;FieldDamage.tick(level,a,level.getGameTime());
        helper.assertTrue(player.getHealth()==health-2,"Selected survival players should take configured damage");
        player.discard();helper.succeed();
    }
    private static EmitterEntity place(GameTestHelper helper,BlockPos pos,UUID owner,long placed) {
        helper.setBlock(pos,FieldEmitters.EMITTER.get());
        var emitter=(EmitterEntity)helper.getBlockEntity(pos);emitter.owner=owner;emitter.placedAt=placed;
        FieldNetwork.add(emitter);return emitter;
    }
    @GameTest(template="core_empty")
    public static void playerListsStayPlayerOnlyAndPersistPerDirection(GameTestHelper helper) {
        var a=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var b=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        a.setUUID(UUID.randomUUID());b.setUUID(UUID.randomUUID());
        var pig=helper.spawn(EntityType.PIG,new BlockPos(3,1,3));
        var f=new EntityFilter();f.groups=2;f.playerMode=2;f.playerList.put(a.getUUID(),"Alice");
        helper.assertTrue(!f.matches(a,null)&&f.matches(b,null),"Whitelist must allow listed players and block unlisted players");
        helper.assertTrue(f.matches(pig,null),"Player whitelist must retain selected mob blocking");
        f.groups=0;
        helper.assertTrue(!f.matches(pig,null),"Player whitelist must not block unselected mobs");
        f.inverted=true;
        helper.assertTrue(!f.matches(a,null)&&f.matches(b,null),"Player list must not be reversed by general filter inversion");
        f.playerMode=1;
        helper.assertTrue(f.matches(a,null)&&!f.matches(b,null),"Blacklist must match listed players only");
        f.exemptOwner=true;
        helper.assertTrue(!f.matches(a,a.getUUID()),"Owner exemption must override the player list");
        f.exemptOwner=false;
        var settings=new ControlSettings();settings.barrierDirections.set(Direction.NORTH,f);
        settings.damageDirections.set(Direction.SOUTH,EntityFilter.load(f.save()));settings.damageEnabled=true;
        var copy=ControlSettings.load(settings.save());
        helper.assertTrue(copy.blocks(a,null,Direction.NORTH)&&!copy.blocks(a,null,Direction.SOUTH),"Directional list must not affect the opposite direction");
        helper.assertTrue(copy.damages(a,null,Direction.SOUTH)&&!copy.damages(b,null,Direction.SOUTH),"Damage must use its own directional list");
        helper.assertTrue(copy.barrier(Direction.NORTH).playerList.get(a.getUUID()).equals("Alice"),"Name and UUID must round-trip");
        var old=EntityFilter.load(new CompoundTag());
        helper.assertTrue(old.playerMode==0&&old.playerList.isEmpty(),"Legacy filters must keep their old behavior");
        f.playerList.clear();f.playerMode=2;
        helper.assertTrue(f.matches(a,null)&&f.matches(b,null),"Empty whitelist must block all players");
        f.playerMode=1;
        helper.assertTrue(!f.matches(a,null)&&!f.matches(b,null),"Empty blacklist must block no players");
        var overflow=f.save();var entries=new net.minecraft.nbt.ListTag();
        for(int n=0;n<70;n++){var entry=new CompoundTag();entry.putUUID("Id",UUID.randomUUID());entry.putString("Name","Player"+n);entries.add(entry);}
        overflow.put("PlayerList",entries);
        helper.assertTrue(EntityFilter.load(overflow).playerList.size()==64,"Stored list must be bounded");
        helper.succeed();
    }
    @GameTest(template="core_empty")
    public static void mobAndItemListsAreIndependentAndDirectional(GameTestHelper helper) {
        var pig=helper.spawn(EntityType.PIG,new BlockPos(2,1,2));pig.setBaby(true);
        var cow=helper.spawn(EntityType.COW,new BlockPos(4,1,2));
        var player=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var powder=new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(),0,0,0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GUNPOWDER));
        var log=new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(),0,0,0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_LOG));
        var f=new EntityFilter();f.groups=0;f.mobMode=1;f.mobList.add("minecraft:pig");f.age=1;
        f.itemMode=1;f.itemList.add("minecraft:gunpowder");f.itemList.add("#minecraft:logs");
        helper.assertTrue(f.matches(pig,null)&&!f.matches(cow,null),"Mob blacklist selects listed babies only");
        helper.assertTrue(f.matches(powder,null)&&f.matches(log,null),"Item IDs and tags OR together and ignore mob age");
        helper.assertTrue(!f.matches(player,null),"Mob and item modes cannot change player filtering");
        pig.setBaby(false);helper.assertTrue(!f.matches(pig,null),"Adult listed mob does not match baby selection");
        f.mobMode=2;
        helper.assertTrue(f.matches(pig,null)&&f.matches(cow,null),"Baby whitelist blocks adults and unlisted mobs");
        pig.setBaby(true);helper.assertTrue(!f.matches(pig,null),"Baby whitelist allows listed babies");
        f.inverted=true;helper.assertTrue(!f.matches(pig,null)&&f.matches(powder,null),"General inversion must not flip list modes");
        f.itemMode=2;helper.assertTrue(!f.matches(powder,null)&&!f.matches(log,null),"Item whitelist allows both ID and tag entries");
        f.exemptOwner=true;helper.assertTrue(!f.matches(cow,cow.getUUID()),"Owner exemption wins over list modes");
        var settings=new ControlSettings();settings.barrierDirections.set(Direction.NORTH,f);
        var south=EntityFilter.load(f.save());south.mobMode=1;south.itemMode=1;
        settings.barrierDirections.set(Direction.SOUTH,south);
        settings.sensorDirections.set(Direction.NORTH,south);settings.damageDirections.set(Direction.SOUTH,south);settings.damageEnabled=true;
        var copy=ControlSettings.load(settings.save());
        helper.assertTrue(!copy.blocks(pig,null,Direction.NORTH)&&copy.blocks(pig,null,Direction.SOUTH),"Independent directional mob lists persist");
        helper.assertTrue(copy.sensor(Direction.NORTH).matches(powder,null)&&copy.damages(powder,null,Direction.SOUTH),"Detection and damage use their own lists");
        helper.assertTrue(copy.barrier(Direction.NORTH).itemList.equals(f.itemList),"Item entries retain tags through NBT");
        f.mobList.clear();f.itemList.clear();
        helper.assertTrue(f.matches(pig,null)&&f.matches(powder,null),"Empty whitelist blocks all of its own kind");
        f.mobMode=1;f.itemMode=1;
        helper.assertTrue(!f.matches(pig,null)&&!f.matches(powder,null),"Empty blacklist blocks none");
        var legacy=EntityFilter.load(new CompoundTag());
        helper.assertTrue(legacy.mobMode==0&&legacy.itemMode==0,"Existing worlds retain general filter mode");
        f.itemList.add("missing:no_such_item");helper.assertTrue(FieldControls.validate(f)!=null,"Unknown item IDs rejected");
        f.itemList.clear();f.mobList.add("#not a tag");helper.assertTrue(FieldControls.validate(f)!=null,"Invalid tags rejected");
        var oversized=new CompoundTag();var list=new net.minecraft.nbt.ListTag();
        for(int i=0;i<70;i++)list.add(net.minecraft.nbt.StringTag.valueOf("#test:tag_"+i));
        oversized.put("ItemList",list);helper.assertTrue(EntityFilter.load(oversized).itemList.size()==64,"Stored list size is bounded");
        helper.succeed();
    }
}
