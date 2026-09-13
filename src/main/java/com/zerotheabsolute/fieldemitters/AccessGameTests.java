package com.zerotheabsolute.fieldemitters;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("fieldemitters")
@PrefixGameTestTemplate(false)
public final class AccessGameTests {
  private static EmitterEntity emitter(GameTestHelper helper, UUID owner) {
    helper.setBlock(new BlockPos(2, 1, 2), FieldEmitters.EMITTER.get());
    var e = (EmitterEntity) helper.getBlockEntity(new BlockPos(2, 1, 2));
    e.owner = owner;
    e.placedAt = 10;
    FieldNetwork.add(e);
    return e;
  }

  @GameTest(template = "core_empty")
  public static void planarProjectionsWaitForTheSurface(GameTestHelper h) {
    var emitter = emitter(h, UUID.randomUUID());
    var p = emitter.getBlockPos();
    var player = h.makeMockPlayer(GameType.SURVIVAL);
    emitter.controls.barrier.groups = 4;
    emitter.controls.barrier.exemptOwner = false;
    emitter.powered = true;
    for (int mode = 3; mode < com.zeromods.core.animation.PlanarProjection.PRESET_COUNT; mode++) {
      emitter.controls.formation = mode;
      h.assertTrue(
          ControlSettings.load(emitter.controls.save()).formation == mode,
          "Planar preset must persist");
      for (int geometry = 0; geometry < 4; geometry++) {
        int dx = geometry == 1 ? 0 : 1, dz = geometry == 1 ? 1 : 0, dy = geometry == 3 ? 1 : 0;
        if (dy != 0) dx = 0;
        int[] ground = new int[5];
        for (int j = 0; j < ground.length; j++) ground[j] = p.getY() + dy * j;
        var normal =
            geometry == 0 ? Direction.Axis.Y : geometry == 1 ? Direction.Axis.X : Direction.Axis.Z;
        var link =
            new EmitterEntity.Link(
                p.offset(dx * 4, dy * 4, dz * 4), dx, dz, ground, dy, normal, true);
        emitter.links = java.util.List.of(link);
        var cell = link.cell(p, 2, 0);
        player.setPos(cell.getX() + .5, cell.getY() + .5, cell.getZ() + .5);
        emitter.transition = h.getLevel().getGameTime() - 79;
        h.assertTrue(
            FieldBlock.collision(emitter, player, cell).isEmpty(),
            "No invisible wall or bridge before formation finishes");
        emitter.transition--;
        h.assertTrue(
            !FieldBlock.collision(emitter, player, cell).isEmpty(),
            "Completed surface must block selected players in every plane");
      }
    }
    for (int legacy = 0; legacy < 3; legacy++) {
      emitter.controls.formation = legacy;
      h.assertTrue(
          emitter.controls.linkFormationTicks(4) == 8, "Legacy activation timing stays unchanged");
    }
    h.succeed();
  }

  @GameTest(template = "core_empty")
  public static void projectionChoicesPersistIndependently(GameTestHelper h) {
    for (var projection : com.zeromods.core.animation.SphereFormation.values()) {
      for (int pattern = 0; pattern < 4; pattern++) {
        var settings = new ControlSettings();
        settings.projection = projection;
        settings.pattern = pattern;
        settings.formation = 2;
        var copy = ControlSettings.load(settings.save());
        h.assertTrue(
            copy.projection == projection && copy.pattern == pattern && copy.formation == 2,
            "Tower projection, surface, and linked-field formation must persist independently");
      }
    }
    var legacy = new ControlSettings().save();
    legacy.remove("Projection");
    h.assertTrue(
        ControlSettings.load(legacy).projection
            == com.zeromods.core.animation.SphereFormation.LASER_CURTAIN,
        "Existing emitters must receive a valid default projection");
    legacy.putString("Projection", "unknown");
    h.assertTrue(
        ControlSettings.load(legacy).projection
            == com.zeromods.core.animation.SphereFormation.LASER_CURTAIN,
        "Unknown projection identifiers must safely default");
    h.succeed();
  }

  @GameTest(template = "core_empty")
  public static void largeSpheresConsumeTheirFullEnergyCost(GameTestHelper h) {
    var energy = new EmitterEnergyStorage(100000, 10000, () -> {});
    for (int i = 0; i < 10; i++) energy.receiveEnergy(10000, false);
    int cost = (int) Math.round(4 * Math.PI * SphereField.MAX_RADIUS * SphereField.MAX_RADIUS) * 2;
    h.assertTrue(cost > 10000, "Largest sphere exercises demand above cable transfer limit");
    h.assertTrue(
        energy.consume(cost) == cost && energy.getEnergyStored() == 100000 - cost,
        "Field must spend its full cost from storage");
    h.assertTrue(
        energy.extractEnergy(Integer.MAX_VALUE, true) == 10000,
        "External extraction must still respect the transfer limit");
    h.succeed();
  }

  @GameTest(template = "core_empty")
  public static void badgeIssuersBindingRevocationAndHolder(GameTestHelper h) {
    var p = h.makeMockPlayer(GameType.SURVIVAL);
    p.setUUID(UUID.randomUUID());
    var issuer = UUID.randomUUID();
    var other = UUID.randomUUID();
    var badge = new ItemStack(FieldEmitters.BADGE.get());
    var data = BadgeAccess.get(h.getLevel());
    data.issue(badge, issuer, "staff", p.getUUID());
    h.assertTrue(
        data.accepts(badge, issuer, p.getUUID(), Set.of("staff")),
        "Issued badge must grant its owner's group");
    h.assertTrue(
        !data.accepts(badge, other, p.getUUID(), Set.of("staff")),
        "Identical group names from another owner must not grant access");
    h.assertTrue(
        !data.accepts(badge, issuer, UUID.randomUUID(), Set.of("staff")),
        "Player-bound badges must reject another holder");
    var restored =
        BadgeAccess.load(
            data.save(new CompoundTag(), h.getLevel().registryAccess()),
            h.getLevel().registryAccess());
    h.assertTrue(
        restored.accepts(badge, issuer, p.getUUID(), Set.of("staff")),
        "Credentials must survive saving");
    var holder = new ItemStack(FieldEmitters.BADGE_HOLDER.get());
    var inventory = new SimpleContainer(holder);
    var slot = new Slot(inventory, 0, 0, 0);
    var cursor = new SimpleContainer(badge);
    FieldEmitters.BADGE_HOLDER
        .get()
        .overrideOtherStackedOnMe(
            holder, badge, slot, ClickAction.SECONDARY, p, SlotAccess.forContainer(cursor, 0));
    h.assertTrue(
        badge.isEmpty() && BadgeHolderItem.contents(holder).size() == 1,
        "Insertion must transfer one actual badge");
    p.getInventory().setItem(0, holder);
    h.assertTrue(
        BadgeAccess.matches(p, issuer, Set.of("staff")),
        "Holder in inventory must supply its badges");
    data.revoke(issuer, "staff");
    h.assertTrue(
        !BadgeAccess.matches(p, issuer, Set.of("staff")),
        "Revocation must invalidate badges inside holders");
    FieldEmitters.BADGE_HOLDER
        .get()
        .overrideOtherStackedOnMe(
            holder,
            ItemStack.EMPTY,
            slot,
            ClickAction.SECONDARY,
            p,
            SlotAccess.forContainer(cursor, 0));
    h.assertTrue(
        BadgeHolderItem.contents(holder).isEmpty()
            && cursor.getItem(0).is(FieldEmitters.BADGE.get()),
        "Removing badge must not duplicate or lose it");
    h.succeed();
  }

  @GameTest(template = "core_empty")
  public static void managementMembershipNeverComesFromBadges(GameTestHelper h) {
    var owner = h.makeMockPlayer(GameType.SURVIVAL);
    owner.setUUID(UUID.randomUUID());
    var manager = h.makeMockPlayer(GameType.SURVIVAL);
    manager.setUUID(UUID.randomUUID());
    var stranger = h.makeMockPlayer(GameType.SURVIVAL);
    stranger.setUUID(UUID.randomUUID());
    var e = emitter(h, owner.getUUID());
    ManagedFields.refresh(h.getLevel(), FieldNetwork.loaded(h.getLevel()));
    h.assertTrue(
        FieldControls.editable(e, owner) && !FieldControls.editable(e, manager),
        "New network must be owner-only");
    var badge = new ItemStack(FieldEmitters.BADGE.get());
    BadgeAccess.get(h.getLevel()).issue(badge, owner.getUUID(), "staff", null);
    manager.getInventory().setItem(0, badge);
    h.assertTrue(!FieldControls.editable(e, manager), "Passage badges must not grant management");
    ManagementAccess.update(h.getLevel(), e, owner, 2, manager.getUUID());
    h.assertTrue(FieldControls.editable(e, manager), "Invited manager should configure network");
    ManagementAccess.update(h.getLevel(), e, manager, 1, null);
    h.assertTrue(!FieldControls.editable(e, stranger), "Manager must not make network public");
    ManagementAccess.update(h.getLevel(), e, manager, 2, stranger.getUUID());
    h.assertTrue(!FieldControls.editable(e, stranger), "Manager must not invite managers");
    ManagementAccess.update(h.getLevel(), e, owner, 1, null);
    h.assertTrue(
        FieldControls.editable(e, stranger),
        "Explicit public management should permit configuration");
    ManagementAccess.update(h.getLevel(), e, owner, 0, null);
    ManagementAccess.update(h.getLevel(), e, owner, 3, manager.getUUID());
    h.assertTrue(
        !FieldControls.editable(e, manager) && !FieldControls.editable(e, stranger),
        "Revoked access must stop immediately");
    h.succeed();
  }

  @GameTest(template = "core_empty")
  public static void confiscationPreservesComponentsAndHandlesFullStorage(GameTestHelper h) {
    var player = h.makeMockPlayer(GameType.SURVIVAL);
    player.setUUID(UUID.randomUUID());
    var e = emitter(h, UUID.randomUUID());
    var s = e.controls.checkpoint;
    s.enabled = true;
    s.players.exemptOwner = false;
    s.items.itemList.add("minecraft:diamond");
    s.confiscate = 2;
    s.storageFace = Direction.EAST;
    var diamonds = new ItemStack(Items.DIAMOND, 13);
    diamonds.set(DataComponents.CUSTOM_NAME, Component.literal("Evidence"));
    player.getInventory().setItem(0, diamonds);
    player.getInventory().setItem(1, new ItemStack(Items.APPLE, 3));
    var entry = Vec3.atCenterOf(e.getBlockPos().north(2));
    FieldCheckpoint.process(h.getLevel(), e, e.controls, player, Direction.SOUTH, entry, 1);
    h.assertTrue(
        diamonds.getCount() == 13 && FieldCheckpoint.blocks(e, e.controls, player, Direction.SOUTH),
        "Missing storage must keep items and hold player");
    h.getLevel().setBlock(e.getBlockPos().east(), Blocks.CHEST.defaultBlockState(), 3);
    var chest =
        (net.minecraft.world.level.block.entity.ChestBlockEntity)
            h.getLevel().getBlockEntity(e.getBlockPos().east());
    FieldCheckpoint.process(h.getLevel(), e, e.controls, player, Direction.SOUTH, entry, 2);
    h.assertTrue(
        diamonds.isEmpty() && chest.getItem(0).getCount() == 13,
        "Storage must receive exactly the confiscated stack");
    h.assertTrue(
        chest.getItem(0).getHoverName().getString().equals("Evidence")
            && player.getInventory().getItem(1).getCount() == 3,
        "Custom components and unrelated items must survive");
    for (int i = 0; i < chest.getContainerSize(); i++)
      chest.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
    player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
    s.dropOverflow = true;
    FieldCheckpoint.process(h.getLevel(), e, e.controls, player, Direction.SOUTH, entry, 3);
    var drops =
        h.getLevel()
            .getEntitiesOfClass(
                ItemEntity.class, new net.minecraft.world.phys.AABB(entry, entry).inflate(1));
    h.assertTrue(
        player.getInventory().getItem(0).isEmpty()
            && drops.stream()
                    .filter(i -> i.getItem().is(Items.DIAMOND))
                    .mapToInt(i -> i.getItem().getCount())
                    .sum()
                == 7,
        "Overflow drops must preserve exact item count");
    h.succeed();
  }

  @GameTest(template = "core_empty")
  public static void checkpointAlertsWorkWithEverySensorMode(GameTestHelper h) {
    var player = h.makeMockPlayer(GameType.SURVIVAL);
    player.setUUID(UUID.randomUUID());
    var e = emitter(h, UUID.randomUUID());
    e.powered = true;
    e.controls.checkpoint.enabled = true;
    e.controls.checkpoint.detect = true;
    e.controls.checkpoint.items.itemList.add("minecraft:diamond");
    player.getInventory().setItem(0, new ItemStack(Items.DIAMOND));
    var entry = Vec3.atCenterOf(e.getBlockPos());
    for (int mode = 0; mode <= 2; mode++) {
      e.controls.sensorMode = mode;
      e.passages.clear();
      e.queuedPulses = 0;
      e.pulseUntil = 0;
      e.gapUntil = 0;
      e.outputSignal = 0;
      FieldCheckpoint.process(h.getLevel(), e, e.controls, player, Direction.SOUTH, entry, 100);
      FieldSensor.tick(h.getLevel(), e, 100);
      h.assertTrue(e.outputSignal == 15, "Checkpoint alert must pulse in sensor mode " + mode);
      FieldCheckpoint.process(h.getLevel(), e, e.controls, player, Direction.SOUTH, entry, 101);
      h.assertTrue(e.queuedPulses == 0, "Continuous contact must not queue repeated alerts");
      FieldSensor.tick(h.getLevel(), e, 100 + e.controls.pulseTicks);
      h.assertTrue(e.outputSignal == 0, "Checkpoint pulse must expire in sensor mode " + mode);
    }
    h.succeed();
  }

  @GameTest(template = "core_empty")
  public static void checkpointRulesAndSphereFormation(GameTestHelper h) {
    var p = h.makeMockPlayer(GameType.SURVIVAL);
    p.setUUID(UUID.randomUUID());
    var e = emitter(h, UUID.randomUUID());
    var s = e.controls.checkpoint;
    s.enabled = true;
    s.deny = true;
    s.items.itemList.add("minecraft:diamond");
    s.players.directions = 1 << Direction.SOUTH.ordinal();
    p.getInventory().setItem(0, new ItemStack(Items.DIAMOND));
    h.assertTrue(
        FieldCheckpoint.blocks(e, e.controls, p, Direction.SOUTH)
            && !FieldCheckpoint.blocks(e, e.controls, p, Direction.NORTH),
        "Checkpoint directions must be independent");
    var copy = ControlSettings.load(e.controls.save());
    h.assertTrue(
        copy.checkpoint.items.itemList.contains("minecraft:diamond") && copy.checkpoint.deny,
        "Checkpoint options must persist");
    h.setBlock(new BlockPos(6, 1, 6), FieldEmitters.TOWER.get());
    var tower = (EmitterEntity) h.getBlockEntity(new BlockPos(6, 1, 6));
    tower.powered = true;
    tower.controls.barrier.groups = 4;
    tower.controls.barrier.exemptOwner = false;
    tower.controls.sphereRadius = 8;
    p.setPos(
        tower.getBlockPos().getX() + 8.6,
        tower.getBlockPos().getY(),
        tower.getBlockPos().getZ() + .5);
    p.xo = p.getX();
    p.yo = p.getY();
    p.zo = p.getZ();
    tower.transition = h.getLevel().getGameTime();
    var cell = tower.getBlockPos().east(8);
    h.assertTrue(
        SphereField.collision(tower, p, cell).isEmpty(),
        "Projection must not collide before formation completes");
    tower.transition -= SphereField.FORMATION_TICKS;
    h.assertTrue(
        !SphereField.collision(tower, p, cell).isEmpty(),
        "Formed sphere must block selected players");
    for (var interior :
        java.util.List.of(
            new BlockPos(2, 1, 2),
            new BlockPos(-3, 2, 1),
            new BlockPos(0, 4, 0),
            new BlockPos(0, 0, 0))) {
      h.assertTrue(
          SphereField.collision(tower, p, tower.getBlockPos().offset(interior)).isEmpty(),
          "Dome interior and floor must have no field collision");
    }
    h.assertTrue(
        SphereField.collision(tower, p, tower.getBlockPos().below(8)).isEmpty(),
        "Dome must not create a lower hemisphere or a floor cap");
    h.assertTrue(
        SphereField.shell(8, true).stream()
            .allMatch(
                offset ->
                    Math.sqrt(
                            offset.getX() * offset.getX()
                                + Math.pow(offset.getY() + .5, 2)
                                + offset.getZ() * offset.getZ())
                        > 6),
        "Field cells must be confined to the outer shell");
    h.assertTrue(
        SphereField.contactMargin(p, new Vec3(0, 0, 1)) > .55,
        "Scanner must reach a player stopped against the outer quarter-block collision cell");
    h.assertTrue(
        SphereField.contactMargin(p, new Vec3(0, 1, 0))
            > SphereField.contactMargin(p, new Vec3(0, 0, 1)),
        "Contact near the pole must account for player height");
    tower.powered = false;
    h.assertTrue(
        SphereField.collision(tower, p, cell).isEmpty(),
        "Power loss removes collision immediately");
    h.assertTrue(
        SphereField.shell(8, true).stream().allMatch(v -> v.getY() >= 0)
            && SphereField.shell(8, false).stream().anyMatch(v -> v.getY() < 0),
        "Dome and sphere must have distinct lower geometry");
    h.succeed();
  }

  @GameTest(template = "core_empty")
  public static void emitterHardwareKeepsMiningHardnessAndRejectsMobDestruction(GameTestHelper h) {
    var creeper = h.spawn(EntityType.CREEPER, new BlockPos(6, 1, 6));
    for (var block :
        List.of(FieldEmitters.EMITTER.get(), FieldEmitters.RAIL.get(), FieldEmitters.TOWER.get())) {
      var state = block.defaultBlockState();
      h.assertTrue(
          state.getDestroySpeed(h.getLevel(), h.absolutePos(BlockPos.ZERO)) == 3.5f,
          "Blast resistance must not increase mining hardness");
      h.assertTrue(
          block.getExplosionResistance() >= HardwareProtection.BLAST_RESISTANCE,
          "Hardware needs blast resistance");
      h.assertTrue(
          !state.canEntityDestroy(h.getLevel(), h.absolutePos(BlockPos.ZERO), creeper),
          "Mobs must not destroy hardware");
    }
    h.succeed();
  }
}
