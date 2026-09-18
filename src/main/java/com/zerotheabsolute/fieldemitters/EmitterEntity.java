package com.zerotheabsolute.fieldemitters;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class EmitterEntity extends BlockEntity {
  public final Map<UUID, Passage> spherePassages = new HashMap<>();
  public boolean spherePresent;

  public boolean isTower() {
    return getBlockState().is(FieldEmitters.TOWER.get());
  }

  public ControlSettings controls = new ControlSettings();
  public final Map<BlockPos, ControlSettings> overrides = new HashMap<>();

  public ControlSettings settings(Link link) {
    return overrides.getOrDefault(link.target(), controls);
  }

  public long crossings = 0;
  public int queuedPulses = 0, outputSignal = 0;
  public long pulseUntil = 0, gapUntil = 0;
  public net.minecraft.network.chat.Component lastDetection =
      net.minecraft.network.chat.Component.translatable("message.fieldemitters.detection.none");
  final FieldContact.Contacts contactDirections = new FieldContact.Contacts();
  public final Map<String, Passage> passages = new HashMap<>();

  public record Passage(int side, net.minecraft.world.phys.Vec3 position, long time) {}

  public int color = 0x52E5FF, mask = 1;
  public boolean enabled = true, powered = false;
  public UUID owner;
  public boolean managementPublic;
  public final Set<UUID> managerIds = new HashSet<>();
  public long placedAt = Long.MAX_VALUE;
  public String fieldName = "";
  public long transition = 0;
  public int demand = 0;
  private int lastSentColor = -1;
  public BlockPos root = BlockPos.ZERO;
  public net.minecraft.world.phys.Vec3 impact = net.minecraft.world.phys.Vec3.ZERO;
  public long impactTime = -1000;
  public final List<FieldImpacts.Wave> impactWaves = new ArrayList<>();
  public long lastFizzle = -1000;
  public final Map<UUID, Long> contacts = new HashMap<>();
  public List<Link> links = new ArrayList<>();

  /** Connected emitters as of the last topology rebuild; refreshed by FieldNetwork. */
  public List<EmitterEntity> network;

  public com.zeromods.core.network.ManagedNetwork<BlockPos> managedNetwork;
  public Set<BlockPos> cells = new HashSet<>();
  public final EmitterEnergyStorage energy =
      new EmitterEnergyStorage(FieldConfig.capacity(), FieldConfig.transfer(), this::setChanged);

  public EmitterEntity(BlockPos p, BlockState s) {
    super(FieldEmitters.EMITTER_BE.get(), p, s);
  }

  public boolean isRail() {
    return getBlockState().is(FieldEmitters.RAIL.get());
  }

  public net.minecraft.world.phys.AABB renderBounds() {
    if (isTower())
      return new net.minecraft.world.phys.AABB(worldPosition).inflate(controls.sphereRadius + 1);
    var box = new net.minecraft.world.phys.AABB(worldPosition);
    int low = worldPosition.getY(), high = worldPosition.getY() + 5;
    for (var link : links) {
      box = box.minmax(link.box(worldPosition));
      for (int y : link.ground()) {
        low = Math.min(low, y);
        high = Math.max(high, y + link.height());
      }
    }
    return new net.minecraft.world.phys.AABB(box.minX, low, box.minZ, box.maxX, high, box.maxZ)
        .inflate(1);
  }

  public record Link(
      BlockPos target,
      int dx,
      int dz,
      int[] ground,
      int dy,
      net.minecraft.core.Direction.Axis normal,
      boolean rail) {
    public Link(BlockPos target, int dx, int dz, int[] ground) {
      this(target, dx, dz, ground, 0, dx != 0 ? Direction.Axis.Z : Direction.Axis.X, false);
    }

    public BlockPos cell(BlockPos source, int i, int h) {
      return new BlockPos(source.getX() + dx * i, ground[i] + h, source.getZ() + dz * i);
    }

    public int height() {
      return rail ? 1 : 5;
    }

    public net.minecraft.world.phys.AABB box(BlockPos source) {
      var a = net.minecraft.world.phys.Vec3.atLowerCornerOf(source);
      var b = net.minecraft.world.phys.Vec3.atLowerCornerOf(target);
      return new net.minecraft.world.phys.AABB(
          Math.min(a.x, b.x),
          Math.min(a.y, b.y),
          Math.min(a.z, b.z),
          Math.max(a.x, b.x) + 1,
          Math.max(a.y, b.y) + height(),
          Math.max(a.z, b.z) + 1);
    }

    public net.minecraft.world.phys.Vec3 origin(BlockPos source) {
      return net.minecraft.world.phys.Vec3.atCenterOf(source)
          .add(0, rail && normal == Direction.Axis.Y ? .5 : 0, 0);
    }

    public double normalCoordinate(net.minecraft.world.phys.Vec3 v) {
      return normal.choose(v.x, v.y, v.z);
    }

    public net.minecraft.core.Direction movement(boolean positive) {
      return Direction.fromAxisAndDirection(
          normal, positive ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
    }

    public int length() {
      return ground.length - 1;
    }
  }

  public void sync() {
    setChanged();
    if (level != null) {
      level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
      if (lastSentColor != color) {
        lastSentColor = color;
        for (int i = 1; i < (isTower() ? TowerBlock.HEIGHT : 5); i++) {
          var part = worldPosition.above(i);
          var state = level.getBlockState(part);
          if (state.is(getBlockState().getBlock())) level.sendBlockUpdated(part, state, state, 3);
        }
      }
    }
  }

  protected void saveAdditional(CompoundTag t) {
    super.saveAdditional(t);
    t.put("Management", ManagementAccess.snapshot(this));
    t.putLong("PlacedAt", placedAt);
    t.putString("FieldName", fieldName);
    t.put("Controls", controls.save());
    var overrideTags = new ListTag();
    overrides.forEach(
        (pos, settings) -> {
          var entry = new CompoundTag();
          entry.putLong("Target", pos.asLong());
          var offset = pos.subtract(worldPosition);
          entry.putIntArray(
              "TargetOffset", new int[] {offset.getX(), offset.getY(), offset.getZ()});
          entry.put("Settings", settings.save());
          overrideTags.add(entry);
        });
    t.put("Overrides", overrideTags);
    t.putLong("Crossings", crossings);
    t.putString(
        "LastDetectionText",
        net.minecraft.network.chat.Component.Serializer.toJson(lastDetection));
    t.putInt("QueuedPulses", queuedPulses);
    t.putInt("OutputSignal", outputSignal);
    t.putLong("ImpactTime", impactTime);
    FieldImpacts.save(this, t);
    t.putDouble("ImpactX", impact.x);
    t.putDouble("ImpactY", impact.y);
    t.putDouble("ImpactZ", impact.z);
    t.putLong("Root", root.asLong());
    var rootOffset = root.subtract(worldPosition);
    t.putIntArray(
        "RootOffset", new int[] {rootOffset.getX(), rootOffset.getY(), rootOffset.getZ()});
    t.putInt("Color", color);
    t.putInt("Mask", mask);
    t.putBoolean("Enabled", enabled);
    t.putBoolean("Powered", powered);
    t.putLong("Transition", transition);
    t.putInt("Demand", demand);
    t.putInt("Energy", energy.getEnergyStored());
    if (owner != null) t.putUUID("Owner", owner);
    ListTag a = new ListTag();
    for (Link link : links) {
      CompoundTag n = new CompoundTag();
      n.putLong("Target", link.target.asLong());
      var targetOffset = link.target.subtract(worldPosition);
      n.putIntArray(
          "TargetOffset",
          new int[] {targetOffset.getX(), targetOffset.getY(), targetOffset.getZ()});
      n.putIntArray(
          "GroundOffset",
          java.util.Arrays.stream(link.ground).map(y -> y - worldPosition.getY()).toArray());
      n.putInt("DX", link.dx);
      n.putInt("DZ", link.dz);
      n.putInt("DY", link.dy);
      n.putBoolean("Rail", link.rail);
      n.putString("Normal", link.normal.getName());
      n.putIntArray("Ground", link.ground);
      a.add(n);
    }
    t.put("Links", a);
  }

  public void load(CompoundTag t) {
    var management = t.getCompound("Management");
    managementPublic = management.getBoolean("Public");
    managerIds.clear();
    for (var value : management.getList("Managers", 8))
      try {
        managerIds.add(UUID.fromString(value.getAsString()));
      } catch (IllegalArgumentException ignored) {
      }
    int previousColor = color;
    super.load(t);
    impactTime = t.contains("ImpactTime") ? t.getLong("ImpactTime") : -1000;
    impact =
        new net.minecraft.world.phys.Vec3(
            t.getDouble("ImpactX"), t.getDouble("ImpactY"), t.getDouble("ImpactZ"));
    FieldImpacts.load(this, t);
    int[] rootOffset = t.getIntArray("RootOffset");
    root =
        rootOffset.length == 3
            ? worldPosition.offset(rootOffset[0], rootOffset[1], rootOffset[2])
            : BlockPos.of(t.getLong("Root"));
    color = t.contains("Color") ? t.getInt("Color") : 0x52E5FF;
    mask = t.contains("Mask") ? t.getInt("Mask") : 1;
    if (t.contains("Controls")) controls = ControlSettings.load(t.getCompound("Controls"));
    else {
      controls = new ControlSettings();
      controls.barrier.groups = mask;
    }
    overrides.clear();
    for (var tag : t.getList("Overrides", Tag.TAG_COMPOUND)) {
      var entry = (CompoundTag) tag;
      if (overrides.size() < 64)
        overrides.put(
            relativePosition(entry, "TargetOffset", "Target"),
            ControlSettings.load(entry.getCompound("Settings")));
    }
    crossings = t.getLong("Crossings");
    lastDetection =
        net.minecraft.network.chat.Component.translatable("message.fieldemitters.detection.none");
    if (t.contains("LastDetectionText")) {
      try {
        var restored =
            net.minecraft.network.chat.Component.Serializer.fromJson(
                t.getString("LastDetectionText"));
        if (restored != null) lastDetection = restored;
      } catch (com.google.gson.JsonParseException ignored) {
        // An invalid saved message must not prevent the emitter from loading.
      }
    } else if (t.contains("LastDetection") && !t.getString("LastDetection").equals("None")) {
      lastDetection = net.minecraft.network.chat.Component.literal(t.getString("LastDetection"));
    }
    queuedPulses = Math.max(0, Math.min(100000, t.getInt("QueuedPulses")));
    outputSignal = t.getInt("OutputSignal");
    enabled = !t.contains("Enabled") || t.getBoolean("Enabled");
    powered = t.getBoolean("Powered");
    transition = t.getLong("Transition");
    demand = t.getInt("Demand");
    energy.deserializeNBT(IntTag.valueOf(Math.max(0, Math.min(energy.getMaxEnergyStored(), t.getInt("Energy")))));
    placedAt = t.contains("PlacedAt") ? t.getLong("PlacedAt") : Long.MAX_VALUE;
    fieldName = t.getString("FieldName");
    owner = t.hasUUID("Owner") ? t.getUUID("Owner") : null;
    links = new ArrayList<>();
    for (Tag tag : t.getList("Links", Tag.TAG_COMPOUND)) {
      CompoundTag n = (CompoundTag) tag;
      int[] ground =
          n.contains("GroundOffset")
              ? java.util.Arrays.stream(n.getIntArray("GroundOffset"))
                  .map(y -> y + worldPosition.getY())
                  .toArray()
              : n.getIntArray("Ground");
      if (ground.length > 1 && ground.length <= 21)
        links.add(
            new Link(
                relativePosition(n, "TargetOffset", "Target"),
                n.getInt("DX"),
                n.getInt("DZ"),
                ground,
                n.getInt("DY"),
                n.contains("Normal")
                    ? Direction.Axis.byName(n.getString("Normal"))
                    : n.getInt("DX") != 0 ? Direction.Axis.Z : Direction.Axis.X,
                n.getBoolean("Rail")));
    }
    // State-identical server block packets do not dirty the client's baked tint cache.
    // Rebuild all five sections after the authoritative color has actually been loaded.
    if (level != null && level.isClientSide && previousColor != color) {
      for (int i = 0; i < 5; i++) {
        var part = worldPosition.above(i);
        var state = level.getBlockState(part);
        level.sendBlockUpdated(part, state, state, 3);
      }
    }
  }

  private BlockPos relativePosition(CompoundTag tag, String offsetKey, String legacyKey) {
    int[] offset = tag.getIntArray(offsetKey);
    return offset.length == 3
        ? worldPosition.offset(offset[0], offset[1], offset[2])
        : BlockPos.of(tag.getLong(legacyKey));
  }

  public CompoundTag getUpdateTag() {
    return saveWithoutMetadata();
  }

  public ClientboundBlockEntityDataPacket getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }
}
