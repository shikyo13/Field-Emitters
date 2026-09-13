package com.zerotheabsolute.fieldemitters.client;

import com.zerotheabsolute.fieldemitters.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Changes are sent immediately; text is validated after a short typing pause. */
public final class ControlScreen extends FittedScreen {
  private final EmitterEntity emitter;
  private ControlSettings draft;
  private int color, tab = 0, left, top, row;
  private int selectedLink = -1;
  private Direction filterDirection;
  private boolean filterEditable = true;
  private boolean enabled, network = false, reset = false;
  private boolean showGuides = FieldConfig.SHOW_GUIDES.get();
  private String notice = "Changes apply automatically.";
  private long textDue;
  private String lastSent = "";
  private static final String[] TABS = {
    "Power", "Block", "Detect", "Damage", "Visuals", "Sounds", "Links"
  };

  public ControlScreen(EmitterEntity e) {
    super(Component.literal("FIELD EMITTER / " + e.getBlockPos().toShortString()));
    emitter = e;
    network = e.isRail();
    draft = ControlSettings.load(e.controls.save());
    color = e.color;
    enabled = e.enabled;
  }

  @Override
  public void renderBackground(GuiGraphics g, int mx, int my, float partial) {}

  public boolean isPauseScreen() {
    return false;
  }

  private void redraw() {
    applyChanges();
    rebuildWidgets();
  }

  protected void init() {
    fit(404, 306);
    if (selectedLink >= emitter.links.size()) selectedLink = -1;
    left = (width - 404) / 2;
    top = (height - 306) / 2;
    row = top + 44;
    filterEditable = true;
    for (int i = 0; i < TABS.length; i++) {
      final int index = i;
      addRenderableWidget(
          new FieldButton(
              left + i * 58,
              top + 20,
              56,
              18,
              Component.literal(TABS[i]),
              b -> {
                applyPending();
                tab = index;
                rebuildWidgets();
              },
              tab == index));
    }
    if (tab == 0) {
      row += 56;
      button(
          "Field: " + (enabled ? "On" : "Off"),
          () -> {
            enabled = !enabled;
            redraw();
          });
      button(
          "Turn on: "
              + new String[] {
                    "Whenever energy is available", "When redstone is ON", "When redstone is OFF"
                  }
                  [draft.inputMode],
          () -> {
            draft.inputMode = (draft.inputMode + 1) % 3;
            redraw();
          });
      button(
          "Read redstone from: " + sideName(draft.inputFace),
          () -> {
            draft.inputFace = nextFace(draft.inputFace, draft.outputFace);
            redraw();
          });
      button("Network management access", () -> { applyPending(); minecraft.setScreen(new ManagementScreen(this,emitter.getBlockPos())); });
      button("Access badges", () -> { applyPending(); minecraft.setScreen(new AccessScreen(this,emitter.getBlockPos(),null,this::applyChanges)); });
      button("Inventory checkpoint", () -> { applyPending(); minecraft.setScreen(new CheckpointScreen(this,emitter.getBlockPos(),draft.checkpoint,this::applyChanges)); });
      button(
          "Reset crossing count now",
          () -> {
            reset = true;
            applyChanges();
          });
    }
    if (tab >= 1 && tab <= 3) {
      var shared = tab == 1 ? draft.barrier : tab == 2 ? draft.sensor : draft.damage;
      var rules = tab == 1 ? draft.barrierDirections : tab == 2 ? draft.sensorDirections : draft.damageDirections;
      small("Rules for: " + (filterDirection == null ? "Both directions" : travelName(filterDirection)),
          left + 12,row,220,() -> {
            applyPending();
            filterDirection = filterDirection == null ? Direction.DOWN : filterDirection == Direction.EAST ? null : Direction.values()[filterDirection.ordinal()+1];
            rebuildWidgets();
          });
      small("Rule source: " + (filterDirection == null ? "Shared" : rules.has(filterDirection) ? "Custom" : "Shared"),
          left+237,row,155,() -> {
            if(filterDirection==null)return;
            applyPending();
            if(rules.has(filterDirection)) rules.inherit(filterDirection);
            else rules.set(filterDirection,EntityFilter.load(shared.save()));
            redraw();
          });
      row+=20;
      filterEditable = filterDirection == null || rules.has(filterDirection);
      var f = filterDirection == null ? shared : rules.resolve(filterDirection,shared);
      if (tab == 2) {
        small(
            "Signal: "
                + new String[] {"Off", "Pulse on crossing", "On while touching"}[draft.sensorMode],
            left + 12,
            row,
            185,
            () -> {
              draft.sensorMode = (draft.sensorMode + 1) % 3;
              redraw();
            });
        small(
            "Items: " + (draft.countItems ? "Count each item" : "Count each stack"),
            left + 207,
            row,
            185,
            () -> {
              draft.countItems = !draft.countItems;
              redraw();
            });
        row += 20;
      }
      if (tab == 3) {
        small("Damage: " + (draft.damageEnabled ? "On" : "Off"), left+12,row,185,() -> {
          draft.damageEnabled = !draft.damageEnabled; redraw();
        });
        small("Hit interval: " + String.format(Locale.ROOT,"%.1f seconds",draft.damageInterval/20f),left+207,row,185,() -> {
          draft.damageInterval = draft.damageInterval >= 100 ? 10 : draft.damageInterval + 10; redraw();
        });
        row += 20;
        field("Damage per hit (HP; 2 = 1 heart)",Float.toString(draft.damageAmount),v -> {
          try { draft.damageAmount=Float.parseFloat(v); }
          catch(NumberFormatException ex) { draft.damageAmount=Float.NaN; }
        },left+12,row,185);
        small("Fizzle particles: " + (draft.fizzleEffects ? "On" : "Off"),left+207,row+8,185,() -> {
          draft.fizzleEffects=!draft.fizzleEffects;redraw();
        });
        row += 29;
      }
      String[] groups = {"Hostile", "Passive", "Players", "Drops", "Nonliving"};
      for (int i = 0; i < 5; i++) {
        final int bit = 1 << i;
        small(
            ((f.groups & bit) != 0 ? "✓ " : "○ ") + groups[i],
            left + 12 + i * 77,
            row,
            74,
            () -> {
              f.groups ^= bit;
              redraw();
            });
      }
      row += 20;
      small(
          (tab == 1
              ? (f.inverted ? "Allow selected only" : "Block selected")
              : tab == 2 ? (f.inverted ? "Detect unselected" : "Detect selected")
              : (f.inverted ? "Damage unselected" : "Damage selected")),
          left + 12,
          row,
          126,
          () -> {
            f.inverted = !f.inverted;
            redraw();
          });
      small(
          "Age: " + new String[] {"Any age", "Babies only", "Adults only"}[f.age],
          left + 142,
          row,
          126,
          () -> {
            f.age = (f.age + 1) % 3;
            redraw();
          });
      small(
          "Skip owner: " + (f.exemptOwner ? "Yes" : "No"),
          left + 272,
          row,
          126,
          () -> {
            f.exemptOwner = !f.exemptOwner;
            redraw();
          });
      row += 20;
      small("Mob list", left + 12, row, 185, () -> {
        applyPending(); minecraft.setScreen(new TypeListScreen(this, f, false, tab, this::applyChanges));
      });
      small("Item list", left + 207, row, 185, () -> {
        applyPending(); minecraft.setScreen(new TypeListScreen(this, f, true, tab, this::applyChanges));
      });
      row += 27;
      field("Specific mob / player (UUID)", f.identity, v -> f.identity = v, left + 12, row, 185);
      field("Custom entity label (/tag)", f.entityTag, v -> f.entityTag = v, left + 207, row, 185);
      row += 27;
      if(filterDirection != null) {
        small((f.direction(filterDirection) ? "✓ " : "○ ") + movementName(filterDirection),left+12,row,380,() -> {
          f.directions ^= 1 << filterDirection.ordinal();redraw();
        });
      } else {
      for (int i = 0; i < 6; i++) {
        final var d = Direction.values()[i];
        small(
            (f.direction(d) ? "✓ " : "○ ") + movementName(d),
            left + 12 + i * 64,
            row,
            61,
            () -> {
              f.directions ^= 1 << d.ordinal();
              redraw();
            });
      }
      }
      row += 20;
      small("Sample individual", left + 12, row, 124, () -> { sample(f, true); redraw(); });
      small("Sample type", left + 140, row, 124, () -> { sample(f, false); redraw(); });
      small("Player list", left + 268, row, 124, () -> {
        applyPending();
        minecraft.setScreen(new PlayerListScreen(this, emitter.getBlockPos(), f, tab, this::applyChanges));
      });
    }
    if (tab == 4) {
      colorPresets("Field color presets", color, c -> color = c);
      field(
          "Custom color (6-digit hex code)",
          String.format("%06X", color),
          v -> {
            try {
              if (!v.matches("#?[0-9a-fA-F]{6}")) throw new NumberFormatException();
              color = Integer.parseInt(v.replace("#", ""), 16) & 0xffffff;
            } catch (NumberFormatException ex) {
              notice = "Invalid color; previous color retained.";
            }
          },
          left + 12,
          row,
          380);
      row += 29;
      colorPresets("Particle color presets", draft.particleColor, c -> draft.particleColor = c);
      field("Particle color (6-digit hex)",String.format("%06X",draft.particleColor),v -> {
        if (v.matches("#?[0-9a-fA-F]{6}")) draft.particleColor=Integer.parseInt(v.replace("#",""),16);
        else notice="Invalid particle color; previous color retained.";
      },left+12,row,185);
      small("Purple / magenta preset",left+207,row+8,185,() -> {
        color=0x9933FF;draft.particleColor=0xFF33CC;draft.pattern=2;redraw();
      });
      row+=29;
      button("Pattern: " + new String[]{"Hex lattice","Smooth glow","Drifting pixels","Plasma"}[draft.pattern],() -> {
        draft.pattern=(draft.pattern+1)%4;redraw();
      });
      if (emitter.isTower()) button("Projection: " + draft.projection.label(), () -> {
        draft.projection = draft.projection.next(); redraw();
      });
      else button("Formation: " + new String[]{"Sweep","Dissolve / reform","Fade"}[draft.formation],() -> {
        draft.formation=(draft.formation+1)%3;redraw();
      });
      button(
          "Light nearby blocks: " + (draft.light ? "Yes" : "No (for dark mob farms)"),
          () -> {
            draft.light = !draft.light;
            redraw();
          });
      button(
          "Show forcefield: " + (draft.visible ? "Yes" : "No (still works)"),
          () -> {
            draft.visible = !draft.visible;
            redraw();
          });
      button(
          "Animate field pattern: " + (draft.animation ? "Yes" : "No"),
          () -> {
            draft.animation = !draft.animation;
            redraw();
          });
      button(
          "Direction guides: " + (showGuides ? "Show with tuner" : "Hidden"),
          () -> {
            showGuides = !showGuides;
            FieldConfig.SHOW_GUIDES.set(showGuides);
            FieldConfig.SHOW_GUIDES.save();
            rebuildWidgets();
          });

    }
    if (tab == 5) {
      button("All field sounds: " + (draft.sounds ? "On" : "Off"),() -> { draft.sounds=!draft.sounds;redraw(); });
      button("Power on / off sounds: " + (draft.powerSounds ? "On" : "Off"),() -> { draft.powerSounds=!draft.powerSounds;redraw(); });
      button("Impact sounds: " + (draft.impactSounds ? "On" : "Off"),() -> { draft.impactSounds=!draft.impactSounds;redraw(); });
      button("Damage sounds: " + (draft.damageSounds ? "On" : "Off"),() -> { draft.damageSounds=!draft.damageSounds;redraw(); });
      button("Sound palette: " + new String[]{"Soft sizzle","Crystal","Electric"}[draft.soundStyle],() -> { draft.soundStyle=(draft.soundStyle+1)%3;redraw(); });
    }
    if (tab == 6) {
      if(emitter.isTower()){
        button("Shape: "+(draft.dome?"Dome":"Full sphere"),()->{draft.dome=!draft.dome;redraw();});
        button("Radius: "+draft.sphereRadius+" blocks",()->{draft.sphereRadius=draft.sphereRadius>=SphereField.MAX_RADIUS?SphereField.MIN_RADIUS:draft.sphereRadius+2;redraw();});
      }
      button(
          "Editing: "
              + (selectedLink < 0
                  ? "Default rules for this emitter"
                  : "Only the field to "
                      + emitter.links.get(selectedLink).target().toShortString()),
          () -> {
            applyPending();
            selectedLink++;
            if (selectedLink >= emitter.links.size()) selectedLink = -1;
            if (selectedLink >= 0) {
              FieldGuide.source = emitter.getBlockPos();
              FieldGuide.target = emitter.links.get(selectedLink).target();
              FieldGuide.until = emitter.getLevel().getGameTime() + 200;
            }
            draft =
                ControlSettings.load(
                    (selectedLink < 0
                            ? emitter.controls
                            : emitter.settings(emitter.links.get(selectedLink)))
                        .save());
            rebuildWidgets();
          });
      button(
          "Change settings for: "
              + (selectedLink >= 0
                  ? "This field only"
                  : network ? "My connected emitters" : "This emitter only"),
          () -> {
            applyPending();
            network = !network;
            rebuildWidgets();
          });
      button(
          "Send redstone signal from: " + sideName(draft.outputFace),
          () -> {
            draft.outputFace = nextFace(draft.outputFace, draft.inputFace);
            redraw();
          });
      button(
          "Signal pulse length: "
              + String.format(Locale.ROOT, "%.1f seconds", draft.pulseTicks / 20.0),
          () -> {
            draft.pulseTicks = draft.pulseTicks >= 20 ? 2 : draft.pulseTicks + 2;
            redraw();
          });
      if (emitter.isRail())
        button(
            "Field shape: " + planeName(),
            () -> {
              cyclePlane();
              redraw();
            });
      if (emitter.isRail() && emitter.getBlockState().getValue(RailBlock.FACING).getAxis() != Direction.Axis.Y)
        button("Bridge preset: safe walking from above", () -> {
          draft.railNormal=Direction.Axis.Y.ordinal();
          draft.barrier=new EntityFilter();draft.barrier.groups=31;draft.barrier.exemptOwner=false;
          draft.barrier.directions=1<<Direction.DOWN.ordinal();
          for(var direction:Direction.values())draft.barrierDirections.inherit(direction);
          draft.damageEnabled=false;redraw();
        });
      row += 8;
    }
    addRenderableWidget(
            new FieldButton(
                left + 242,
                top + 283,
                72,
                18,
                Component.literal("Fields"),
                b -> {
                  applyPending();
                  RemoteScreen.openManager();
                },
                false))
        .setTooltip(
            Tooltip.create(
                Component.literal("Manage loaded emitters remotely. Hold a Field Tuner.")));
    addRenderableWidget(
            new FieldButton(
                left + 320, top + 283, 72, 18, Component.literal("Close"), b -> onClose(), false))
        .setTooltip(
            Tooltip.create(
                Component.literal("Close this menu. Valid changes have already been applied.")));
  }

  @Override
  public void tick() {
    if (textDue != 0 && System.currentTimeMillis() >= textDue) applyPending();
  }

  private void applyPending() {
    if (textDue != 0) {
      applyChanges();
    }
  }

  @Override
  public void onClose() {
    applyPending();
    super.onClose();
  }

  private void applyChanges() {
    textDue = 0;
    String error = FieldControls.validate(draft);
    if (error != null) {
      notice = error;
      return;
    }
    if (selectedLink >= emitter.links.size()) {
      notice = "Field disconnected. Reopen settings.";
      return;
    }
    String signature = draft.save().toString() + color + enabled + network + selectedLink;
    if (!reset && signature.equals(lastSent)) return;
    var target =
        selectedLink < 0 ? emitter.getBlockPos() : emitter.links.get(selectedLink).target();
    PacketDistributor.sendToServer(
        new FieldControls.Update(
            emitter.getBlockPos(),
            draft.save(),
            color,
            enabled,
            network,
            reset,
            target,
            selectedLink >= 0));
    if (selectedLink < 0) {
      emitter.controls = ControlSettings.load(draft.save());
      emitter.color = color;
      emitter.enabled = enabled;
    } else emitter.overrides.put(target, ControlSettings.load(draft.save()));
    lastSent = signature;
    reset = false;
    notice = "Applying changes…";
  }

  public void acknowledge(String message) {
    notice = message;
    if (!message.equals("Changes applied.")) lastSent = "";
  }

  private static String sideName(Direction d) {
    return switch (d) {
      case UP -> "Top";
      case DOWN -> "Bottom";
      default -> d.getName().substring(0, 1).toUpperCase(Locale.ROOT) + d.getName().substring(1);
    };
  }

  private static String travelName(Direction direction) {
    return switch(direction) {
      case NORTH -> "South → North";case SOUTH -> "North → South";
      case EAST -> "West → East";case WEST -> "East → West";
      case UP -> "Below → Above";case DOWN -> "Above → Below";
    };
  }

  private static String movementName(Direction d) {
    return d == Direction.UP ? "Upward" : d == Direction.DOWN ? "Downward" : "To " + sideName(d);
  }

  private Direction.Axis effectivePlane() {
    var span = emitter.getBlockState().getValue(RailBlock.FACING).getAxis();
    var normal = Direction.Axis.values()[draft.railNormal];
    return normal == span
        ? (span == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z)
        : normal;
  }

  private String planeName() {
    return switch (effectivePlane()) {
      case X -> "Wall facing East / West";
      case Z -> "Wall facing North / South";
      case Y -> "Horizontal floor / ceiling";
    };
  }

  private void cyclePlane() {
    var span = emitter.getBlockState().getValue(RailBlock.FACING).getAxis();
    var current = effectivePlane();
    for (var axis : Direction.Axis.values())
      if (axis != span && axis != current) {
        draft.railNormal = axis.ordinal();
        break;
      }
  }

  private Direction nextFace(Direction from, Direction excluded) {
    var d = Direction.values()[(from.ordinal() + 1) % 6];
    return d == excluded ? Direction.values()[(d.ordinal() + 1) % 6] : d;
  }

  private void colorPresets(String label, int selected, java.util.function.IntConsumer choose) {
    labels.add(new Label(label, left + 12, row));
    int[] colors = {0x00FFFF, 0x00CED1, 0x4169E1, 0x9B59B6, 0xFF69B4, 0xFF4444, 0xFF8C00, 0xFFFFFF};
    String[] names = {"Cyan", "Aqua", "Blue", "Purple", "Pink", "Red", "Orange", "White"};
    for (int i = 0; i < colors.length; i++) {
      final int value = colors[i];
      var button = new FieldButton(left + 12 + i * 48, row + 9, 44, 18,
          Component.literal(names[i]), b -> { choose.accept(value); redraw(); }, selected == value, value);
      button.active = selectedLink < 0;
      button.setTooltip(Tooltip.create(Component.literal(selectedLink >= 0
          ? "Select emitter defaults on Links to change colors."
          : names[i] + " — " + label + ". Applies immediately; keeps your other visual settings.")));
      addRenderableWidget(button);
    }
    row += 28;
  }

  private void button(String text, Runnable action) {
    small(text, left + 12, row, 380, action);
    row += 20;
  }

  private void small(String text, int x, int y, int w, Runnable action) {
    var button =
        new FieldButton(
            x, y, w, 18, Component.literal(text), b -> action.run(), text.startsWith("✓"));
    String help = ControlHelp.button(text, tab);
    boolean fieldOnly =
        selectedLink >= 0
            && !text.startsWith("Editing:")
            && !text.startsWith("Direction guides:")
            && (tab == 0
                || tab == 4
                || tab == 5
                || tab == 6
                || tab == 3 && text.startsWith("Fizzle particles:")
                || tab == 2 && (text.startsWith("Signal:") || text.startsWith("Items:")));
    boolean inherited = !filterEditable && (tab >= 1 && tab <= 3)
        && !text.startsWith("Signal:") && !text.startsWith("Items:") && !text.startsWith("Damage:")
        && !text.startsWith("Hit interval:") && !text.startsWith("Fizzle particles:");
    button.active = !fieldOnly && !inherited;
    button.setTooltip(
        Tooltip.create(
            Component.literal(
                inherited ? "This direction uses the shared filter. Choose Rule source: Custom to give it separate rules." : fieldOnly
                    ? "This setting belongs to the emitter. In Connections, select default rules to"
                        + " change it. Blocking, detection and damage rules can differ for one"
                        + " field."
                    : help)));
    addRenderableWidget(button);
  }

  private final List<Label> labels = new ArrayList<>();

  private record Label(String text, int x, int y) {}

  private void field(
      String label, String value, java.util.function.Consumer<String> save, int x, int y, int w) {
    labels.add(new Label(label, x, y));
    var box = new EditBox(font, x, y + 9, w, 16, Component.literal(label));
    box.setMaxLength(128);
    box.setValue(value);
    box.setTooltip(Tooltip.create(Component.literal(ControlHelp.field(label))));
    if (!filterEditable && (tab >= 1 && tab <= 3) && !label.startsWith("Damage per hit")) {
      box.setEditable(false);
      box.setTooltip(Tooltip.create(Component.literal("Choose Rule source: Custom to edit this direction independently.")));
    }
    if (selectedLink >= 0 && tab == 4) {
      box.setEditable(false);
      box.setTooltip(
          Tooltip.create(
              Component.literal(
                  "Appearance belongs to the emitter. Select default rules in Connections to change"
                      + " it.")));
    }
    addRenderableWidget(box);
    box.setResponder(
        v -> {
          save.accept(v.trim());
          textDue = System.currentTimeMillis() + 350;
          notice = "Typing… valid changes apply automatically.";
        });
  }

  @Override
  public void rebuildWidgets() {
    labels.clear();
    super.rebuildWidgets();
  }

  private void sample(EntityFilter f, boolean individual) {
    for (var stack : minecraft.player.getInventory().items) {
      if (!stack.is(FieldEmitters.TUNER.get())) continue;
      var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
      if (data == null) continue;
      var t = data.copyTag();
      if (!t.contains("SampleUUID")) continue;
      if (f.mobMode != 0 && !t.getString("SampleType").equals("minecraft:player")) {
        if (individual) {
          f.identity = t.getString("SampleUUID");
          notice = "Sample UUID now narrows the mob list selection.";
        } else if (f.mobList.contains(t.getString("SampleType")) || f.mobList.size() < EntityFilter.MAX_TYPES) {
          f.mobList.add(t.getString("SampleType")); notice = "Sample type added to mob list.";
        } else notice = "Mob list is full (64 entries).";
        return;
      }
      f.groups = 31;
      f.exemptOwner = false;
      f.age = 0;
      f.itemType = "";
      f.entityTag = "";
      f.inverted = false;
      if (individual) {
        f.identity = t.getString("SampleUUID");
        f.entityType = "";
      } else {
        f.entityType = t.getString("SampleType");
        f.identity = "";
      }
      notice = "Sample selected.";
      return;
    }
    notice = "First sneak-use a mob, or air for yourself.";
  }

  public void render(GuiGraphics g, int mx, int my, float partial) {
    beginFit(g);
    mx = fitMouse(mx);
    my = fitMouse(my);
    g.fill(0, 0, width, height, 0xB0101723);
    g.fill(left, top, left + 404, top + 306, 0xFF111D2C);
    g.fill(left + 1, top + 2, left + 403, top + 20, 0xFF1B2D3E);
    g.fill(left + 8, top + 40, left + 396, top + 278, 0xFF0E1926);
    g.fill(left + 8, top + 280, left + 396, top + 281, 0xFF354D63);
    g.fill(left, top, left + 404, top + 2, 0xFF000000 | color);
    g.drawString(
        font,
        selectedLink < 0 ? title : Component.literal("EDITING ONE FIELD: BLOCK / DETECT / DAMAGE"),
        left + 12,
        top + 7,
        0xDBF8FF,
        false);
    if (tab == 0) {
      String status =
          emitter.powered
              ? "Field running"
              : emitter.enabled ? "Waiting for energy or redstone input" : "Field switched off";
      if (emitter.isTower() && emitter.enabled) {
        if (!SphereField.fitsHeight(emitter)) status = "Outside world height: move tower or reduce radius";
        else if (emitter.powered && !SphereField.formed(emitter)) status = "Projecting field…";
      }
      if (FieldConfig.DEMO_POWER.get()) status += " (demo power enabled)";
      g.drawString(
          font, font.plainSubstrByWidth(status, 380), left + 12, top + 44, 0x77FFBD, false);
      g.drawString(
          font,
          "Energy stored: "
              + emitter.energy.getEnergyStored()
              + " FE   Used: "
              + emitter.demand
              + " FE/t",
          left + 12,
          top + 56,
          0xCAD6E5,
          false);
      int energyWidth =
          (int)
              (380L
                  * emitter.energy.getEnergyStored()
                  / Math.max(1, emitter.energy.getMaxEnergyStored()));
      g.fill(left + 12, top + 93, left + 392, top + 96, 0xFF263F53);
      g.fill(left + 12, top + 93, left + 12 + energyWidth, top + 96, 0xFF52E5FF);
      EmitterEntity detector = emitter;
      if (emitter.isRail()
          && emitter.getLevel().getBlockEntity(emitter.root) instanceof EmitterEntity source)
        detector = source;
      g.drawString(
          font,
          (emitter.isRail() ? "Rail chain: " : "")
              + detector.crossings
              + " crossings   "
              + detector.queuedPulses
              + " signals waiting",
          left + 12,
          top + 68,
          0xCAD6E5,
          false);
      g.drawString(
          font,
          font.plainSubstrByWidth("Last detected: " + detector.lastDetection, 380),
          left + 12,
          top + 80,
          0xCAD6E5,
          false);
    }
    if (tab == 6) {
      int y = row;
      if (emitter.isRail()) {
        g.drawString(
            font,
            "Connect redstone at: " + emitter.root.toShortString(),
            left + 12,
            y,
            0x77FFBD,
            false);
        y += 14;
      }
      for (var link : emitter.links) {
        g.drawString(
            font,
            "Field to " + link.target().toShortString() + " (" + link.length() + " blocks)",
            left + 12,
            y,
            0xCAD6E5,
            false);
        y += 14;
        if (y > top + 192) break;
      }
    }
    for (var l : labels) g.drawString(font, l.text, l.x, l.y, 0x92A9BE, false);

    g.drawString(font, font.plainSubstrByWidth(notice, 218), left + 12, top + 288, 0x92A9BE, false);
    super.render(g, mx, my, partial);
    g.pose().popPose();
  }
}
