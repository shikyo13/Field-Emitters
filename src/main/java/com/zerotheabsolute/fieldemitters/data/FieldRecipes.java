package com.zerotheabsolute.fieldemitters.data;

import com.zerotheabsolute.fieldemitters.FieldEmitters;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.Tags;

public final class FieldRecipes extends RecipeProvider {
  public FieldRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
    super(output, registries);
  }

  @Override
  protected void buildRecipes(RecipeOutput output) {
    // Field Emitter: copper contacts and an amethyst resonator over a glass-shielded redstone core.
    ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, FieldEmitters.EMITTER.get())
        .pattern("CAC")
        .pattern("GRG")
        .pattern("III")
        .define('C', Items.COPPER_INGOT)
        .define('A', Items.AMETHYST_SHARD)
        .define('G', Tags.Items.GLASS_BLOCKS)
        .define('R', Items.REDSTONE_BLOCK)
        .define('I', Items.IRON_INGOT)
        .unlockedBy("has_amethyst_shard", has(Items.AMETHYST_SHARD))
        .save(output);

    // Field Rail: a strip of four from one resonator, cheaper per block than an emitter post.
    ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, FieldEmitters.RAIL.get(), 4)
        .pattern("ICI")
        .pattern("ARA")
        .pattern("ICI")
        .define('I', Items.IRON_INGOT)
        .define('C', Items.COPPER_INGOT)
        .define('A', Items.AMETHYST_SHARD)
        .define('R', Items.REDSTONE)
        .unlockedBy("has_amethyst_shard", has(Items.AMETHYST_SHARD))
        .save(output);

    // Field Tuner: handheld probe with an amethyst tip and a glass readout.
    ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, FieldEmitters.TUNER.get())
        .pattern(" A ")
        .pattern("CGC")
        .pattern("IRI")
        .define('A', Items.AMETHYST_SHARD)
        .define('C', Items.COPPER_INGOT)
        .define('G', Tags.Items.GLASS_PANES)
        .define('I', Items.IRON_INGOT)
        .define('R', Items.REDSTONE)
        .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT))
        .save(output);
  }
}
