package com.zerotheabsolute.fieldemitters.client;

import java.util.concurrent.atomic.AtomicInteger;

/** All player lookup screens share one sequence so stale replies cannot match another screen. */
final class PlayerLookupSequence {
  private static final AtomicInteger NEXT = new AtomicInteger();

  private PlayerLookupSequence() {}

  static int next() {
    return NEXT.updateAndGet(value -> value == Integer.MAX_VALUE ? 0 : value + 1);
  }
}
