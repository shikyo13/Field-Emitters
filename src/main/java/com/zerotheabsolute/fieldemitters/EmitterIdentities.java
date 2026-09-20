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

  private record Member(ManagedNetwork<BlockPos> network, BlockPos position) {}

  private record Move(Member previous, BlockPos destination, UUID identity) {}

  static void reconcile(NetworkDirectory<BlockPos> directory, List<EmitterEntity> loaded) {
    var identities = new HashMap<ManagedNetwork<BlockPos>, Map<BlockPos, UUID>>();
    var previous = new HashMap<UUID, Member>();
    for (var snapshot : directory.snapshot()) {
      var network = directory.get(snapshot.id()).orElseThrow();
      var ids = read(network);
      ids.keySet().retainAll(network.nodes());
      identities.put(network, ids);
      ids.forEach((pos, id) -> previous.putIfAbsent(id, new Member(network, pos)));
    }
    var moves = new ArrayList<Move>();
    var claimed = new HashSet<UUID>();
    var ordered = new ArrayList<>(loaded);
    // A copied block must not steal the original member's network identity.
    ordered.sort(Comparator.comparing(e -> {
      var old = previous.get(e.emitterId);
      return old == null || !old.position.equals(e.getBlockPos());
    }));
    for (var emitter : ordered) {
      if (!claimed.add(emitter.emitterId)) {
        emitter.emitterId = UUID.randomUUID();
        claimed.add(emitter.emitterId);
        emitter.setChanged();
      }
      var pos = emitter.getBlockPos();
      var old = previous.get(emitter.emitterId);
      for (var entry : identities.entrySet()) {
        var network = entry.getKey();
        var known = entry.getValue().get(pos);
        if (network.nodes().contains(pos)
            && (known != null && !known.equals(emitter.emitterId)
                || known == null && emitter.adoptPending)) {
          network.removeNode(pos);
          entry.getValue().remove(pos);
        }
      }
      if (old != null && !old.position.equals(pos)
          && Objects.equals(old.network.owner(), emitter.owner)) {
        moves.add(new Move(old, pos, emitter.emitterId));
      }
    }
    for (var move : moves) {
      move.previous.network.removeNode(move.previous.position);
      identities.get(move.previous.network).remove(move.previous.position);
      directory.setLoaded(move.previous.position, false);
    }
    for (var move : moves) {
      move.previous.network.addNode(move.destination);
      identities.get(move.previous.network).put(move.destination, move.identity);
    }
    for (var entry : identities.entrySet()) {
      if (entry.getKey().nodes().isEmpty()) directory.remove(entry.getKey().id());
      else write(entry.getKey(), entry.getValue());
    }
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
