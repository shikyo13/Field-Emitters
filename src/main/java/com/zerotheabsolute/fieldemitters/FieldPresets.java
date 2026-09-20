package com.zerotheabsolute.fieldemitters;

import com.google.gson.*;
import java.io.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;

/** A reload replaces the complete registry only after every definition validates. */
public class FieldPresets extends SimplePreparableReloadListener<Map<ResourceLocation, FieldPresets.Preset>> {
  private static final int MAX_JSON_CHARS = 65536;
  private static final int MAX_LIST_SIZE = 256;
  private static volatile Map<ResourceLocation, Preset> presets = Map.of();
  private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();

  public record Preset(CompoundTag controls, Integer color, Boolean enabled, boolean locked) {}

  public static Set<ResourceLocation> ids() { return presets.keySet(); }
  public static void clear() { presets = Map.of(); }
  public static Preset get(String id) {
    var key = ResourceLocation.tryParse(id);
    var preset = key == null ? null : presets.get(key);
    if (preset == null) throw new IllegalArgumentException("Unknown field preset: " + id);
    return preset;
  }

  @Override protected Map<ResourceLocation, Preset> prepare(ResourceManager resources, ProfilerFiller profiler) {
    var filters = read(resources, "fieldemitters/filters");
    var definitions = read(resources, "fieldemitters/presets");
    var result = new LinkedHashMap<ResourceLocation, Preset>();
    // Validate unused filters too, so a typo cannot sit undetected until a preset references it.
    filters.forEach((id, json) -> decodeFilter(json, Map.of(), id.toString()));
    definitions.forEach((id, json) -> {
      for (String key : json.keySet())
        if (!Set.of("controls", "color", "enabled", "locked").contains(key)) invalid(id + ": unknown property " + key);
      var controls = json.has("controls")
          ? decode(json.getAsJsonObject("controls"), new ControlSettings().save(), filters, id.toString())
          : new CompoundTag();
      var normalized = new ControlSettings().save();
      normalized.merge(controls);
      requirePreserved(controls, ControlSettings.load(normalized).save(), id.toString());
      Integer color = null;
      if (json.has("color")) {
        String value = json.get("color").getAsString();
        if (!value.matches("#[0-9a-fA-F]{6}")) invalid(id + ": color must be #RRGGBB");
        color = Integer.parseInt(value.substring(1), 16);
      }
      result.put(id, new Preset(controls, color, optionalBoolean(json, "enabled"), Boolean.TRUE.equals(optionalBoolean(json, "locked"))));
    });
    return Map.copyOf(result);
  }

  @Override protected void apply(Map<ResourceLocation, Preset> prepared, ResourceManager resources, ProfilerFiller profiler) {
    presets = prepared;
  }

  private static Map<ResourceLocation, JsonObject> read(ResourceManager resources, String folder) {
    var result = new LinkedHashMap<ResourceLocation, JsonObject>();
    resources.listResources(folder, id -> id.getPath().endsWith(".json")).forEach((path, resource) -> {
      String name = path.getPath().substring(folder.length() + 1, path.getPath().length() - 5);
      var id = ResourceLocation.fromNamespaceAndPath(path.getNamespace(), name);
      try (var reader = resource.openAsReader()) {
        var text = new StringBuilder();
        char[] buffer = new char[4096];
        int read;
        while ((read = reader.read(buffer)) >= 0) {
          text.append(buffer, 0, read);
          if (text.length() > MAX_JSON_CHARS) invalid(path + ": definition is too large");
        }
        result.put(id, JsonParser.parseString(text.toString()).getAsJsonObject());
      } catch (IOException | RuntimeException ex) {
        throw new IllegalArgumentException("Invalid field definition " + path + ": " + ex.getMessage(), ex);
      }
    });
    return result;
  }

  private static CompoundTag decode(JsonObject input, CompoundTag schema, Map<ResourceLocation, JsonObject> filters, String path) {
    var output = new CompoundTag();
    for (var entry : input.entrySet()) {
      String key = entry.getKey(), location = path + "." + key;
      Tag expected = schema.get(key);
      var value = entry.getValue();
      if (expected == null) invalid(location + ": unknown setting");
      if (expected instanceof CompoundTag compound) {
        if (compound.isEmpty() && key.endsWith("Directions")) {
          var directions = new CompoundTag();
          for (var direction : value.getAsJsonObject().entrySet()) {
            if (net.minecraft.core.Direction.byName(direction.getKey()) == null) invalid(location + ": unknown direction");
            directions.put(direction.getKey(), decodeFilter(direction.getValue(), filters, location));
          }
          output.put(key, directions);
        } else if (compound.contains("Groups")) output.put(key, decodeFilter(value, filters, location));
        else output.put(key, decode(value.getAsJsonObject(), compound, filters, location));
      } else if (expected instanceof ListTag) {
        if (!value.isJsonArray() || value.getAsJsonArray().size() > MAX_LIST_SIZE) invalid(location + ": expected a list of at most " + MAX_LIST_SIZE + " entries");
        var list = new ListTag();
        for (var item : value.getAsJsonArray()) {
          if (key.equals("PlayerList")) {
            var player = item.getAsJsonObject();
            if (!Set.of("Id", "Name").containsAll(player.keySet())) invalid(location + ": expected Id and optional Name");
            var tag = new CompoundTag();
            tag.putUUID("Id", java.util.UUID.fromString(player.get("Id").getAsString()));
            tag.putString("Name", player.has("Name") ? player.get("Name").getAsString() : "");
            list.add(tag); continue;
          }
          if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString()) invalid(location + ": expected strings");
          list.add(StringTag.valueOf(item.getAsString()));
        }
        output.put(key, list);
      } else if (expected instanceof StringTag) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) invalid(location + ": expected a string");
        output.putString(key, value.getAsString());
      } else if (expected instanceof ByteTag) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) invalid(location + ": expected true or false");
        output.putBoolean(key, value.getAsBoolean());
      } else if (expected instanceof NumericTag) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) invalid(location + ": expected a number");
        double number = value.getAsDouble();
        if (!Double.isFinite(number)) invalid(location + ": expected a finite number");
        if (expected instanceof FloatTag) output.putFloat(key, (float)number);
        else {
          if (number != Math.rint(number) || number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) invalid(location + ": expected an integer");
          output.putInt(key, (int)number);
        }
      } else invalid(location + ": unsupported setting");
    }
    return output;
  }

  private static CompoundTag decodeFilter(JsonElement value, Map<ResourceLocation, JsonObject> filters, String path) {
    JsonObject object;
    if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
      var id = ResourceLocation.tryParse(value.getAsString());
      object = id == null ? null : filters.get(id);
      if (object == null) throw new IllegalArgumentException(path + ": unknown filter " + value);
    } else object = value.getAsJsonObject();
    var result = decode(object, new EntityFilter().save(), Map.of(), path);
    var full = new EntityFilter().save(); full.merge(result);
    requirePreserved(result, EntityFilter.load(full).save(), path);
    return result;
  }

  private static void requirePreserved(CompoundTag requested, CompoundTag actual, String path) {
    for (String key : requested.getAllKeys()) {
      var wanted = requested.get(key); var found = actual.get(key);
      if (wanted instanceof CompoundTag compound && found instanceof CompoundTag other) requirePreserved(compound, other, path + "." + key);
      else if (!Objects.equals(wanted, found)) invalid(path + "." + key + ": invalid value or out of range");
    }
  }

  private static Boolean optionalBoolean(JsonObject json, String key) {
    if (!json.has(key)) return null;
    var value = json.get(key);
    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) invalid(key + ": expected true or false");
    return value.getAsBoolean();
  }

  public static String export(EmitterEntity emitter) {
    var json = new JsonObject();
    json.add("controls", toJson(emitter.controls.save()));
    json.addProperty("color", String.format("#%06X", emitter.color));
    json.addProperty("enabled", emitter.enabled);
    json.addProperty("locked", emitter.presetLocked);
    return JSON.toJson(json);
  }

  private static JsonElement toJson(Tag tag) {
    if (tag instanceof CompoundTag compound) {
      var object = new JsonObject();
      for (String key : compound.getAllKeys()) {
        if (key.equals("Id") && compound.hasUUID(key)) object.addProperty(key, compound.getUUID(key).toString());
        else object.add(key, toJson(compound.get(key)));
      }
      return object;
    }
    if (tag instanceof ListTag list) {
      var array = new JsonArray(); for (Tag item : list) array.add(toJson(item)); return array;
    }
    if (tag instanceof ByteTag value) return new JsonPrimitive(value.getAsByte() != 0);
    return NbtOps.INSTANCE.convertTo(com.mojang.serialization.JsonOps.INSTANCE, tag);
  }

  private static void invalid(String message) { throw new IllegalArgumentException(message); }
}
