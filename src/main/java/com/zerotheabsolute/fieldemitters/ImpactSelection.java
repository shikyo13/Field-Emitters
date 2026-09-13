package com.zerotheabsolute.fieldemitters;

import java.util.*;

/** Batches contacts in age order so overload cannot starve later entities. */
final class ImpactSelection<T> {
  static final int EFFECT_INTERVAL = 8;
  private static final int CONTACT_INTERVAL = 25;
  private static final int MAX_BATCH = 128;
  record Choice<T>(UUID id, T value) {}
  private final Map<UUID, Long> history;
  private final Map<UUID, T> present = new LinkedHashMap<>();
  private final long now;

  ImpactSelection(Map<UUID, Long> history, long now) {
    this.history = history;
    this.now = now;
  }

  void consider(UUID id, T candidate) {
    present.putIfAbsent(id, candidate);
  }

  List<Choice<T>> finish() {
    history.keySet().retainAll(present.keySet());
    var eligible = new ArrayList<UUID>();
    for (var id : present.keySet())
      if (!history.containsKey(id) || now - history.get(id) >= CONTACT_INTERVAL) eligible.add(id);
    eligible.sort(Comparator.comparingLong(id -> history.getOrDefault(id, Long.MIN_VALUE)));
    var result = new ArrayList<Choice<T>>();
    for (var id : eligible) {
      if (result.size() == MAX_BATCH) break;
      result.add(new Choice<>(id, present.get(id)));
      history.put(id, now);
    }
    return result;
  }
}
