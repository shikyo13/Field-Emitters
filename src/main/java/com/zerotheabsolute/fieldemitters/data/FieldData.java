package com.zerotheabsolute.fieldemitters.data;

import com.zerotheabsolute.fieldemitters.FieldEmitters;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.data.event.GatherDataEvent;

/** Entry point for the data run: recipes, loot tables and tags land in src/generated/resources. */
@EventBusSubscriber(modid = FieldEmitters.ID, bus = EventBusSubscriber.Bus.MOD)
public final class FieldData {
  @SubscribeEvent
  public static void gather(GatherDataEvent event) {
    var generator = event.getGenerator();
    var output = generator.getPackOutput();
    var registries = event.getLookupProvider();
    generator.addProvider(event.includeServer(), new FieldRecipes(output, registries));
    generator.addProvider(event.includeServer(), new FieldLoot(output, registries));
    generator.addProvider(
        event.includeServer(),
        new FieldBlockTags(output, registries, event.getExistingFileHelper()));
  }
}
