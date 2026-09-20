package com.zerotheabsolute.fieldemitters;

import com.zeromods.core.fabric.NetworkSavedData;
import com.zeromods.core.network.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Legacy emitter fields migrate into the Core model the first time their chunks are observed. */
public final class ManagedFields {
  private static final String STORE = "fieldemitters_core_networks";

  public static void refresh(ServerLevel level, List<EmitterEntity> loaded) {
    var data = NetworkSavedData.get(level, STORE);
    var directory = data.directory();
    var before = directory.snapshot();
    EmitterIdentities.reconcile(directory, loaded);
    var savedSettings = NetworkSettings.saved(directory.snapshot());
    var components = new ArrayList<PhysicalNetworkReconciler.Component<BlockPos>>();
    var seen = new HashSet<BlockPos>();
    var ordered =
        loaded.stream()
            .sorted(
                Comparator.comparingLong((EmitterEntity e) -> e.placedAt)
                    .thenComparingLong(e -> e.getBlockPos().asLong()))
            .toList();
    for (var seed : ordered) {
      if (seen.contains(seed.getBlockPos())) continue;
      var parts =
          FieldNetwork.configurable(seed).stream()
              .filter(e -> Objects.equals(e.owner, seed.owner))
              .filter(e -> !seen.contains(e.getBlockPos()))
              .sorted(
                  Comparator.comparingLong((EmitterEntity e) -> e.placedAt)
                      .thenComparingLong(e -> e.getBlockPos().asLong()))
              .toList();
      if (parts.isEmpty()) continue;
      var positions = parts.stream().map(EmitterEntity::getBlockPos).toList();
      seen.addAll(positions);
      var first = parts.getFirst();
      components.add(
          new PhysicalNetworkReconciler.Component<>(seed.owner, first.fieldName, positions));
    }
    var observed = new HashSet<BlockPos>(seen);
    // A loaded chunk can contain emitters that have not received their first tick yet.
    // Only a missing emitter is evidence of removal; an unregistered one is still unobserved.
    before.forEach(n -> n.nodes().stream()
        .filter(level::hasChunkAt)
        .filter(pos -> !(level.getBlockEntity(pos) instanceof EmitterEntity))
        .forEach(observed::add));
    var assignments =
        new PhysicalNetworkReconciler<>(directory, "fieldemitters:field")
            .reconcile(components, observed);
    for (var emitter : loaded)
      emitter.managedNetwork = directory.get(assignments.get(emitter.getBlockPos())).orElse(null);
    EmitterIdentities.remember(loaded);
    NetworkSettings.reconcile(loaded, savedSettings);
    for (var emitter : loaded) {
      var scope = ControlEdits.describe(emitter);
      if (!scope.equals(emitter.editScope)) { emitter.editScope = scope; emitter.sync(); }
    }
    if (!before.equals(directory.snapshot())) data.setDirty();
  }

  static void dirty(ServerLevel level) {
    NetworkSavedData.get(level, STORE).setDirty();
  }

  public static void rename(ServerLevel level, EmitterEntity emitter, String name) {
    if (emitter.managedNetwork == null) refresh(level, FieldNetwork.loaded(level));
    if (emitter.managedNetwork != null) {
      emitter.managedNetwork.rename(name);
      NetworkSavedData.get(level, STORE).setDirty();
    }
  }
}
