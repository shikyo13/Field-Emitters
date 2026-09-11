package com.zerotheabsolute.fieldemitters;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.*;

@Mod(FieldEmitters.ID)
public final class FieldEmitters {
  public static final String ID = "fieldemitters";
  public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
  public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
  public static final DeferredRegister<BlockEntityType<?>> TYPES =
      DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
  public static final DeferredBlock<EmitterBlock> EMITTER =
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
  public static final DeferredBlock<FieldBlock> FIELD =
      BLOCKS.register(
          "forcefield",
          () ->
              new FieldBlock(
                  BlockBehaviour.Properties.of()
                      .strength(-1, 3600000)
                      .noLootTable()
                      .noOcclusion()
                      .noTerrainParticles()
                      .lightLevel(s -> s.getValue(FieldBlock.LIT) ? 8 : 0)));
  public static final DeferredBlock<RailBlock> RAIL =
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
  public static final DeferredItem<BlockItem> RAIL_ITEM = ITEMS.registerSimpleBlockItem(RAIL);
  public static final DeferredItem<BlockItem> EMITTER_ITEM = ITEMS.registerSimpleBlockItem(EMITTER);
  public static final DeferredItem<TunerItem> TUNER =
      ITEMS.register("field_tuner", () -> new TunerItem(new Item.Properties().stacksTo(1)));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EmitterEntity>>
      EMITTER_BE =
          TYPES.register(
              "emitter",
              () ->
                  BlockEntityType.Builder.of(EmitterEntity::new, EMITTER.get(), RAIL.get())
                      .build(null));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FieldCell>> CELL_BE =
      TYPES.register(
          "field_cell", () -> BlockEntityType.Builder.of(FieldCell::new, FIELD.get()).build(null));

  public FieldEmitters(IEventBus bus, net.neoforged.fml.ModContainer container) {
    container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, FieldConfig.SPEC);
    container.registerConfig(
        net.neoforged.fml.config.ModConfig.Type.CLIENT, FieldConfig.CLIENT_SPEC);
    bus.addListener(FieldControls::register);
    BLOCKS.register(bus);
    ITEMS.register(bus);
    TYPES.register(bus);
    bus.addListener(
        (RegisterCapabilitiesEvent e) ->
            e.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK, EMITTER_BE.get(), (be, side) -> be.energy));
    bus.addListener(
        (net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e) -> {
          if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            e.accept(EMITTER_ITEM);
            e.accept(RAIL_ITEM);
            e.accept(TUNER);
          }
        });
    NeoForge.EVENT_BUS.addListener(FieldNetwork::tick);
    NeoForge.EVENT_BUS.addListener(DemoCommands::register);
  }
}
