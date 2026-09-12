package com.zerotheabsolute.fieldemitters;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class FieldEmitters implements net.fabricmc.api.ModInitializer {
  public static final String ID = "fieldemitters";
  public static final Registration<Block> BLOCKS = new Registration<>(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
  public static final Registration<Item> ITEMS = new Registration<>(net.minecraft.core.registries.BuiltInRegistries.ITEM);
  public static final Registration<BlockEntityType<?>> TYPES = new Registration<>(net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE);
  public static final java.util.function.Supplier<EmitterBlock> EMITTER =
      BLOCKS.register(
          "field_emitter",
          () ->
              new EmitterBlock(
                  BlockBehaviour.Properties.of()
                      .strength(3.5f)
                      .requiresCorrectToolForDrops()
                      .sound(SoundType.NETHERITE_BLOCK)
                      .noOcclusion()
                      .lightLevel(s -> s.getValue(EmitterBlock.LIGHT) ? 12 : 0)));
  public static final java.util.function.Supplier<FieldBlock> FIELD =
      BLOCKS.register(
          "forcefield",
          () ->
              new FieldBlock(
                  BlockBehaviour.Properties.of()
                      .strength(-1, 3600000)
                      .noLootTable()
                      .noOcclusion()
                      
                      .lightLevel(s -> s.getValue(FieldBlock.LIT) ? 8 : 0)));
  public static final java.util.function.Supplier<RailBlock> RAIL =
      BLOCKS.register(
          "field_rail",
          () ->
              new RailBlock(
                  BlockBehaviour.Properties.of()
                      .strength(3.5f)
                      .requiresCorrectToolForDrops()
                      .sound(SoundType.NETHERITE_BLOCK)
                      .noOcclusion()
                      .lightLevel(s -> s.getValue(RailBlock.LIGHT) ? 8 : 0)));
  public static final java.util.function.Supplier<BlockItem> RAIL_ITEM = ITEMS.register("field_rail", () -> new BlockItem(RAIL.get(), new Item.Properties()));
  public static final java.util.function.Supplier<BlockItem> EMITTER_ITEM = ITEMS.register("field_emitter", () -> new BlockItem(EMITTER.get(), new Item.Properties()));
  public static final java.util.function.Supplier<TunerItem> TUNER =
      ITEMS.register("field_tuner", () -> new TunerItem(new Item.Properties().stacksTo(1)));
  public static final java.util.function.Supplier<BlockEntityType<EmitterEntity>>
      EMITTER_BE =
          TYPES.register(
              "emitter",
              () ->
                  BlockEntityType.Builder.of(EmitterEntity::new, EMITTER.get(), RAIL.get())
                      .build(null));
  public static final java.util.function.Supplier<BlockEntityType<FieldCell>> CELL_BE =
      TYPES.register(
          "field_cell", () -> BlockEntityType.Builder.of(FieldCell::new, FIELD.get()).build(null));
  public static final Registration<CreativeModeTab> TABS = new Registration<>(net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB);
  public static final java.util.function.Supplier<CreativeModeTab> TAB =
      TABS.register(
          "field_emitters",
          () ->
              net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder()
                  .title(net.minecraft.network.chat.Component.translatable("itemGroup." + ID))
                  .icon(() -> new ItemStack(EMITTER_ITEM.get()))
                  .displayItems(
                      (parameters, output) -> {
                        output.accept(EMITTER_ITEM.get());
                        output.accept(RAIL_ITEM.get());
                        output.accept(TUNER.get());
                      })
                  .build());


  @Override public void onInitialize() {
    BLOCKS.register(); ITEMS.register(); TYPES.register(); TABS.register();
    fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry.INSTANCE.register(ID, net.minecraftforge.fml.config.ModConfig.Type.SERVER, FieldConfig.SPEC);
    fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry.INSTANCE.register(ID, net.minecraftforge.fml.config.ModConfig.Type.CLIENT, FieldConfig.CLIENT_SPEC);
    team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity((be, side) -> be.energy, EMITTER_BE.get());
    FieldControls.register(new com.zerotheabsolute.fieldemitters.network.NativeNetwork());
    net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_WORLD_TICK.register(FieldNetwork::tick);
    net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> DemoCommands.register(dispatcher));
  }
  private static final class Registration<T> {
    private final net.minecraft.core.Registry<T> registry;
    private final java.util.List<Runnable> entries = new java.util.ArrayList<>();
    Registration(net.minecraft.core.Registry<T> registry) { this.registry = registry; }
    <V extends T> java.util.function.Supplier<V> register(String id, java.util.function.Supplier<V> factory) {
      class Entry implements java.util.function.Supplier<V> { V value; public V get() { return java.util.Objects.requireNonNull(value, id + " not registered"); } }
      var entry = new Entry();
      entries.add(() -> entry.value = net.minecraft.core.Registry.register(registry, new net.minecraft.resources.ResourceLocation(ID, id), factory.get()));
      return entry;
    }
    void register() { entries.forEach(Runnable::run); }
  }
}
