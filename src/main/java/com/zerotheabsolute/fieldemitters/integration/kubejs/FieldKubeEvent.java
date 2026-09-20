package com.zerotheabsolute.fieldemitters.integration.kubejs;

import com.zerotheabsolute.fieldemitters.*;
import net.minecraft.world.entity.Entity;

public final class FieldKubeEvent implements dev.latvian.mods.kubejs.event.KubeEvent {
  private final FieldAutomation.Event event;
  public FieldKubeEvent(FieldAutomation.Event event) { this.event = event; }
  public FieldHandle getField() { return event.field; }
  public Entity getEntity() { return event.entity; }
  public String getDirection() { return event.direction; }
  public int getCount() { return event.count; }
  public boolean isPowered() { return event.field.isPowered(); }
  public boolean isBlocked() { return event.isBlocked(); }
  public void allow() { event.allow(); }
  public void deny() { event.deny(); }
}
