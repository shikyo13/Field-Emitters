package com.zerotheabsolute.fieldemitters.data;

import com.zerotheabsolute.fieldemitters.FieldEmitters;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class FieldBlockTags extends BlockTagsProvider {
  public FieldBlockTags(
      PackOutput output,
      CompletableFuture<HolderLookup.Provider> registries,
      ExistingFileHelper existingFileHelper) {
    super(output, registries, FieldEmitters.ID, existingFileHelper);
  }

  @Override
  protected void addTags(HolderLookup.Provider provider) {
    tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .add(FieldEmitters.EMITTER.get(), FieldEmitters.RAIL.get());
  }
}
