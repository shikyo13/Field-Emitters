package com.zerotheabsolute.fieldemitters.data;

import com.zerotheabsolute.fieldemitters.FieldEmitters;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public final class FieldLoot extends LootTableProvider {
  public FieldLoot(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
    super(output, Set.of(), List.of(new SubProviderEntry(Blocks::new, LootContextParamSets.BLOCK)), registries);
  }

  /**
   * Every emitter section drops the emitter item. Breaking one section removes the other four
   * without drops, so a post always yields exactly one item no matter which section is mined.
   */
  public static final class Blocks extends BlockLootSubProvider {
    Blocks(HolderLookup.Provider registries) {
      super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
      dropSelf(FieldEmitters.EMITTER.get());
      dropSelf(FieldEmitters.RAIL.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
      return List.of(FieldEmitters.EMITTER.get(), FieldEmitters.RAIL.get());
    }
  }
}
