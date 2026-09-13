package com.zerotheabsolute.fieldemitters;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.*;

@Mod(FieldEmitters.ID)
public final class FieldEmitters {
  public static final String ID = "fieldemitters";
  public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ID);
  public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ID);
  public static final DeferredRegister<BlockEntityType<?>> TYPES =
      DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
  public static final RegistryObject<EmitterBlock> EMITTER =
      BLOCKS.register(
          "field_emitter",
          () ->
              new EmitterBlock(
                  BlockBehaviour.Properties.of()
                      .strength(3.5f, HardwareProtection.BLAST_RESISTANCE)
                      .requiresCorrectToolForDrops()
                      .sound(SoundType.NETHERITE_BLOCK)
                      .noOcclusion()
                      .lightLevel(s -> s.getValue(EmitterBlock.LIGHT) ? 12 : 0)));
  public static final RegistryObject<TowerBlock> TOWER = BLOCKS.register("projection_tower", () -> new TowerBlock(BlockBehaviour.Properties.of().strength(3.5f,HardwareProtection.BLAST_RESISTANCE).requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK).noOcclusion().lightLevel(s -> s.getValue(TowerBlock.LIGHT) ? 12 : 0)));
  public static final RegistryObject<BlockItem> TOWER_ITEM = ITEMS.register("projection_tower", () -> new BlockItem(TOWER.get(), new Item.Properties()));
  public static final RegistryObject<FieldBlock> FIELD =
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
  public static final RegistryObject<RailBlock> RAIL =
      BLOCKS.register(
          "field_rail",
          () ->
              new RailBlock(
                  BlockBehaviour.Properties.of()
                      .strength(3.5f, HardwareProtection.BLAST_RESISTANCE)
                      .requiresCorrectToolForDrops()
                      .sound(SoundType.NETHERITE_BLOCK)
                      .noOcclusion()
                      .lightLevel(s -> s.getValue(RailBlock.LIGHT) ? 8 : 0)));
  public static final RegistryObject<BlockItem> RAIL_ITEM = ITEMS.register("field_rail", () -> new BlockItem(RAIL.get(), new Item.Properties()));
  public static final RegistryObject<BlockItem> EMITTER_ITEM = ITEMS.register("field_emitter", () -> new BlockItem(EMITTER.get(), new Item.Properties()));
  public static final RegistryObject<Item> BADGE = ITEMS.register("access_badge", () -> new Item(new Item.Properties().stacksTo(1)));
  public static final RegistryObject<BadgeHolderItem> BADGE_HOLDER = ITEMS.register("badge_holder", () -> new BadgeHolderItem(new Item.Properties().stacksTo(1)));
  public static final RegistryObject<TunerItem> TUNER =
      ITEMS.register("field_tuner", () -> new TunerItem(new Item.Properties().stacksTo(1)));
  public static final RegistryObject<BlockEntityType<EmitterEntity>>
      EMITTER_BE =
          TYPES.register(
              "emitter",
              () ->
                  BlockEntityType.Builder.of(EmitterEntity::new, EMITTER.get(), RAIL.get(), TOWER.get())
                      .build(null));
  public static final RegistryObject<BlockEntityType<FieldCell>> CELL_BE =
      TYPES.register(
          "field_cell", () -> BlockEntityType.Builder.of(FieldCell::new, FIELD.get()).build(null));
  public static final DeferredRegister<CreativeModeTab> TABS =
      DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
  public static final RegistryObject<CreativeModeTab> TAB =
      TABS.register(
          "field_emitters",
          () ->
              CreativeModeTab.builder()
                  .title(net.minecraft.network.chat.Component.translatable("itemGroup." + ID))
                  .icon(() -> new ItemStack(EMITTER_ITEM.get()))
                  .displayItems(
                      (parameters, output) -> {
                        output.accept(EMITTER_ITEM.get());
                        output.accept(RAIL_ITEM.get());
                        output.accept(TUNER.get());
                        output.accept(TOWER_ITEM.get());
                        output.accept(BADGE.get()); output.accept(BADGE_HOLDER.get());
                      })
                  .build());

  public FieldEmitters() {
    IEventBus bus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
    var container = net.minecraftforge.fml.ModLoadingContext.get();
    container.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER, FieldConfig.SPEC);
    container.registerConfig(
        net.minecraftforge.fml.config.ModConfig.Type.CLIENT, FieldConfig.CLIENT_SPEC);
    var network = new com.zerotheabsolute.fieldemitters.network.ForgeNetworkRegistrar();
    FieldControls.register(network);
    FizzleNotice.register(network);
    PlayerLookup.register(network);
    AccessPackets.register(network);
    ManagementPackets.register(network);

    BLOCKS.register(bus);
    ITEMS.register(bus);
    TYPES.register(bus);
    TABS.register(bus);

    bus.addListener(
        (net.minecraftforge.event.BuildCreativeModeTabContentsEvent e) -> {
          if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            e.accept(EMITTER_ITEM);
            e.accept(RAIL_ITEM);
            e.accept(TUNER);
          }
        });
    MinecraftForge.EVENT_BUS.addListener(FieldNetwork::tick);
    MinecraftForge.EVENT_BUS.addListener(HardwareProtection::explosion);
    MinecraftForge.EVENT_BUS.addListener(AccessPackets::tick);
    MinecraftForge.EVENT_BUS.addListener(HardwareProtection::mobBreak);
  }
}
