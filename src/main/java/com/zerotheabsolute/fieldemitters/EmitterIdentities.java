package com.zerotheabsolute.fieldemitters;

import com.zeromods.core.network.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;

/** Positions can be reused or moved by assembly mods; an emitter's identity cannot. */
final class EmitterIdentities {
  private static final String KEY = "fieldemitters:emitter_ids";
  private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

  private EmitterIdentities() {}

  private record Node(EmitterEntity emitter) implements NodeIdentities.Node<BlockPos> {
    public BlockPos position() { return emitter.getBlockPos(); }
    public UUID identity() { return emitter.emitterId; }
    public void identity(UUID replacement) { emitter.emitterId = replacement; emitter.setChanged(); }
    public UUID owner() { return emitter.owner; }
    public boolean replacement() { return emitter.adoptPending; }
  }

  private static final NodeIdentities.Store<BlockPos> STORE = new NodeIdentities.Store<>() {
    public Map<BlockPos, UUID> read(ManagedNetwork<BlockPos> network) { return EmitterIdentities.read(network); }
    public void write(ManagedNetwork<BlockPos> network, Map<BlockPos, UUID> ids) { EmitterIdentities.write(network, ids); }
  };

  static void reconcile(NetworkDirectory<BlockPos> directory, List<EmitterEntity> loaded) {
    NodeIdentities.reconcile(directory, loaded.stream().map(Node::new).toList(), STORE);
  }

  static void remember(List<EmitterEntity> loaded) {
    var updates = new HashMap<ManagedNetwork<BlockPos>, Map<BlockPos, UUID>>();
    for (var emitter : loaded) {
      if (emitter.managedNetwork == null) continue;
      var ids = updates.computeIfAbsent(emitter.managedNetwork, EmitterIdentities::read);
      if (!emitter.emitterId.equals(ids.put(emitter.getBlockPos(), emitter.emitterId)))
        emitter.setChanged();
    }
    updates.forEach((network, ids) -> {
      ids.keySet().retainAll(network.nodes());
      write(network, ids);
    });
  }

  private static Map<BlockPos, UUID> read(ManagedNetwork<BlockPos> network) {
    var ids = new HashMap<BlockPos, UUID>();
    network.property(KEY).ifPresent(value -> {
      try {
        var entries = TagParser.parseTag(value).getList("Members", Tag.TAG_COMPOUND);
        for (var tag : entries) {
          var entry = (CompoundTag) tag;
          if (entry.hasUUID("Id")) ids.put(BlockPos.of(entry.getLong("Position")), entry.getUUID("Id"));
        }
      } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) {
        LOGGER.warn("Cannot read emitter identities for field network {}", network.id(), ex);
      }
    });
    return ids;
  }

  private static void write(ManagedNetwork<BlockPos> network, Map<BlockPos, UUID> ids) {
    var members = new ListTag();
    ids.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(member -> {
      var tag = new CompoundTag();
      tag.putLong("Position", member.getKey().asLong());
      tag.putUUID("Id", member.getValue());
      members.add(tag);
    });
    var tag = new CompoundTag();
    tag.put("Members", members);
    network.property(KEY, tag.toString());
  }
}
