package com.zerotheabsolute.fieldemitters;

import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;

/** Compare-and-set edits over the finite settings schema. Lists are atomic; category and direction flags merge independently. */
public final class SettingsPatch {
  public static final int PROTOCOL = 3;
  public static final int MAX_PAYLOAD_CHARS = 262144;
  private static final List<String> DIRECTION_GROUPS = List.of("BarrierDirections", "SensorDirections", "DamageDirections", "Checkpoint.Directions");
  private static final Map<String, Property> PROPERTIES = properties();

  private SettingsPatch() {}

  public static CompoundTag snapshot(ControlSettings settings, int color, boolean enabled) {
    var tag = new CompoundTag();
    tag.put("Controls", settings.save());
    tag.putInt("Color", color);
    tag.putBoolean("Enabled", enabled);
    return tag;
  }

  public static ListTag between(CompoundTag before, CompoundTag after) {
    var changes = new ListTag();
    PROPERTIES.forEach((key, property) -> {
      var old = property.read(before);
      var value = property.read(after);
      if (Objects.equals(old, value)) return;
      var change = new CompoundTag();
      change.putString("Property", key);
      if (old != null) change.put("Before", old.copy());
      if (value != null) change.put("After", value.copy());
      changes.add(change);
    });
    return changes;
  }

  public record Result(CompoundTag settings, boolean conflict) {}

  public static Result apply(CompoundTag current, ListTag changes, boolean atomic) {
    if (changes.size() > PROPERTIES.size() || changes.toString().length() > MAX_PAYLOAD_CHARS)
      throw new IllegalArgumentException("Settings edit exceeds limits");
    var result = current.copy();
    var seen = new HashSet<String>();
    boolean conflict = false;
    for (var entry : changes) {
      if (!(entry instanceof CompoundTag change)) throw new IllegalArgumentException("Invalid edit");
      String key = change.getString("Property");
      var property = PROPERTIES.get(key);
      if (property == null || !seen.add(key)) throw new IllegalArgumentException("Unknown or repeated property");
      var before = change.get("Before");
      var after = change.get("After");
      if (!property.accepts(before) || !property.accepts(after))
        throw new IllegalArgumentException("Wrong property type");
      var value = property.read(current);
      if (Objects.equals(value, after)) continue;
      if (!Objects.equals(value, before)) { conflict = true; continue; }
      property.write(result, after);
    }
    for (String group : DIRECTION_GROUPS) {
      var parts = group.split("\\.");
      var rules = result.getCompound("Controls");
      for (String part : parts) rules = rules.getCompound(part);
      for (Direction direction : Direction.values())
        if (rules.contains(direction.getName(), Tag.TAG_COMPOUND) && rules.getCompound(direction.getName()).isEmpty()) rules.remove(direction.getName());
    }
    return new Result(atomic && conflict ? current.copy() : result, conflict);
  }

  private record Property(String path, int type, int bit, boolean optional) {
    Tag read(CompoundTag root) {
      var parts = path.split("\\.");
      for (int i = 0; i < parts.length - 1; i++) root = root.getCompound(parts[i]);
      var value = root.get(parts[parts.length - 1]);
      return bit == 0 || value == null ? value : ByteTag.valueOf((((NumericTag) value).getAsInt() & bit) != 0);
    }
    boolean accepts(Tag value) {
      return value == null ? optional : value.getId() == (bit == 0 ? type : Tag.TAG_BYTE);
    }
    void write(CompoundTag root, Tag value) {
      var parts = path.split("\\.");
      for (int i = 0; i < parts.length - 1; i++) {
        if (!root.contains(parts[i], Tag.TAG_COMPOUND)) root.put(parts[i], new CompoundTag());
        root = root.getCompound(parts[i]);
      }
      String key = parts[parts.length - 1];
      if (value == null) root.remove(key);
      else if (bit != 0) root.putInt(key, ((NumericTag) value).getAsByte() != 0 ? root.getInt(key) | bit : root.getInt(key) & ~bit);
      else root.put(key, value.copy());
    }
  }

  private static Map<String, Property> properties() {
    var properties = new LinkedHashMap<String, Property>();
    collect(properties, "", snapshot(new ControlSettings(), 0, false), false);
    for (String group : DIRECTION_GROUPS)
      for (Direction direction : Direction.values()) {
        String path = "Controls." + group + "." + direction.getName();
        collect(properties, path + ".", new EntityFilter().save(), true);
      }
    return Collections.unmodifiableMap(properties);
  }

  private static void collect(Map<String, Property> properties, String prefix, CompoundTag tag, boolean optional) {
    for (String key : new TreeSet<>(tag.getAllKeys())) {
      String path = prefix + key;
      var value = tag.get(key);
      if (value instanceof CompoundTag compound) { collect(properties, path + ".", compound, optional); continue; }
      int bits = key.equals("Groups") ? 5 : key.equals("Directions") ? 6 : key.equals("Crossings") ? 2 : 0;
      if (bits == 0) properties.put(path, new Property(path, value.getId(), 0, optional));
      else for (int bit = 0; bit < bits; bit++) properties.put(path + "#" + bit, new Property(path, value.getId(), 1 << bit, optional));
    }
  }
}
