package com.zerotheabsolute.fieldemitters.integration.kubejs;

import com.zerotheabsolute.fieldemitters.*;
import dev.latvian.mods.kubejs.event.*;
import java.util.Map;

public final class FieldKubePlugin implements dev.latvian.mods.kubejs.plugin.KubeJSPlugin {
  public static final EventGroup GROUP = EventGroup.of("FieldEmitterEvents");
  private static final Map<String, EventHandler> EVENTS = Map.of(
      "crossing", GROUP.server("crossing", () -> FieldKubeEvent.class),
      "contraband", GROUP.server("contraband", () -> FieldKubeEvent.class),
      "powerChanged", GROUP.server("powerChanged", () -> FieldKubeEvent.class),
      "passage", GROUP.server("passage", () -> FieldKubeEvent.class));

  @Override public void init() {
    FieldAutomation.listen(type -> EVENTS.get(type).hasListeners(), event -> {
      var handler = EVENTS.get(event.type);
      if (handler.hasListeners()) handler.post(new FieldKubeEvent(event));
    });
  }
  @Override public void registerEvents(dev.latvian.mods.kubejs.event.EventGroupRegistry registry) { registry.register(GROUP); }
  @Override public void registerBindings(dev.latvian.mods.kubejs.script.BindingRegistry bindings) {
    if (bindings.type() == dev.latvian.mods.kubejs.script.ScriptType.SERVER) bindings.add("FieldEmitters", new FieldScriptApi());
  }
}
