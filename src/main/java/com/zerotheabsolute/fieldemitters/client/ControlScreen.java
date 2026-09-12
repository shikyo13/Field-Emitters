package com.zerotheabsolute.fieldemitters.client;

import com.zerotheabsolute.fieldemitters.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import com.zerotheabsolute.fieldemitters.network.NativeNetwork;

/** Changes are sent immediately; text is validated after a short typing pause. */
public final class ControlScreen extends FittedScreen {
  private final EmitterEntity emitter;
  private ControlSettings draft;
  private int color, tab = 0, left, top, row;
  private int selectedLink = -1;
  private boolean enabled, network = false, reset = false;
  private boolean showGuides = FieldConfig.SHOW_GUIDES.get();
  private String notice = "Changes apply automatically.";
  private long textDue;
  private String lastSent = "";
  private static final String[] TABS = {
    "Power", "Blocking", "Detection", "Appearance", "Connections"
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
    fit(404, 224);
    if (selectedLink >= emitter.links.size()) selectedLink = -1;
    left = (width - 404) / 2;
    top = (height - 224) / 2;
    row = top + 44;
    for (int i = 0; i < TABS.length; i++) {
      final int index = i;
      addRenderableWidget(
          new ControlButton(
              left + i * 81,
              top + 20,
              79,
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
      button(
          "Reset crossing count now",
          () -> {
            reset = true;
            applyChanges();
          });
    }
    if (tab == 1 || tab == 2) {
      var f = tab == 1 ? draft.barrier : draft.sensor;
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
              : (f.inverted ? "Detect unselected" : "Detect selected")),
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
      field("Entity type or #group", f.entityType, v -> f.entityType = v, left + 12, row, 185);
      field("Dropped item or #group", f.itemType, v -> f.itemType = v, left + 207, row, 185);
      row += 27;
      field("Specific mob / player (UUID)", f.identity, v -> f.identity = v, left + 12, row, 185);
      field("Custom entity label (/tag)", f.entityTag, v -> f.entityTag = v, left + 207, row, 185);
      row += 27;
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
      row += 20;
      small(
          "Match this sampled individual",
          left + 12,
          row,
          185,
          () -> {
            sample(f, true);
            redraw();
          });
      small(
          "Match the sampled entity type",
          left + 207,
          row,
          185,
          () -> {
            sample(f, false);
            redraw();
          });
    }
    if (tab == 3) {
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
      row += 32;
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
      for (int i = 0; i < TunerItem.COLORS.length; i++) {
        final int c = TunerItem.COLORS[i];
        small(
            "#" + String.format("%06X", c),
            left + 12 + i * 64,
            row,
            61,
            () -> {
              color = c;
              applyChanges();
              rebuildWidgets();
            });
      }
    }
    if (tab == 4) {
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
      row += 8;
    }
    addRenderableWidget(
            Button.builder(
                    Component.literal("Emitters"),
                    b -> {
                      applyPending();
                      RemoteScreen.openManager();
                    })
                .bounds(left + 242, top + 201, 72, 18)
                .build())
        .setTooltip(
            Tooltip.create(
                Component.literal("Manage loaded emitters remotely. Hold a Field Tuner.")));
    addRenderableWidget(
            Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(left + 320, top + 201, 72, 18)
                .build())
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
    String error = FieldControls.validate(draft.barrier);
    if (error == null) error = FieldControls.validate(draft.sensor);
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
    NativeNetwork.sendToServer(
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

  private void button(String text, Runnable action) {
    small(text, left + 12, row, 380, action);
    row += 20;
  }

  private void small(String text, int x, int y, int w, Runnable action) {
    var button =
        new ControlButton(
            x, y, w, 18, Component.literal(text), b -> action.run(), text.startsWith("✓"));
    String help = ControlHelp.button(text, tab);
    boolean fieldOnly =
        selectedLink >= 0
            && !text.startsWith("Editing:")
            && !text.startsWith("Direction guides:")
            && (tab == 0
                || tab == 3
                || tab == 4
                || tab == 2 && (text.startsWith("Signal:") || text.startsWith("Items:")));
    button.active = !fieldOnly;
    button.setTooltip(
        Tooltip.create(
            Component.literal(
                fieldOnly
                    ? "This setting belongs to the emitter. In Connections, select default rules to"
                        + " change it. Only blocking and detection filters can differ for one"
                        + " field."
                    : help)));
    addRenderableWidget(button);
  }

  private static final class ControlButton extends Button {
    private final boolean selected;

    ControlButton(int x, int y, int w, int h, Component title, OnPress action, boolean selected) {
      super(x, y, w, h, title, action, DEFAULT_NARRATION);
      this.selected = selected;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
      int border =
          !active
              ? 0xFF243441
              : isHoveredOrFocused() ? 0xFF87ECFF : selected ? 0xFF53BBCB : 0xFF354D63;
      g.fill(getX(), getY(), getX() + width, getY() + height, border);
      g.fill(
          getX() + 1,
          getY() + 1,
          getX() + width - 1,
          getY() + height - 1,
          selected ? 0xFF214755 : isHoveredOrFocused() ? 0xFF263F53 : 0xFF1B2D3E);
      var font = net.minecraft.client.Minecraft.getInstance().font;
      String fullText = getMessage().getString();
      // Direction labels need the whole button width; their selected state has a bright fill.
      if (width == 61
          && (fullText.contains("To ")
              || fullText.contains("Upward")
              || fullText.contains("Downward"))) fullText = fullText.replaceFirst("^[✓○] ", "");
      String text = font.plainSubstrByWidth(fullText, width - 8);
      g.drawString(
          font,
          text,
          getX() + (width - font.width(text)) / 2,
          getY() + (height - 8) / 2,
          !active ? 0xFF708395 : selected ? 0xFFA0F5FF : 0xFFD8E6F3,
          false);
    }
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
    if (selectedLink >= 0 && tab == 3) {
      box.setEditable(false);
      box.setTooltip(
          Tooltip.create(
              Component.literal(
                  "Color belongs to the emitter. Select default rules in Connections to change"
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
    g.fill(left, top, left + 404, top + 224, 0xFF111D2C);
    g.fill(left, top, left + 404, top + 2, 0xFF000000 | color);
    g.drawString(
        font,
        selectedLink < 0 ? title : Component.literal("EDITING ONE FIELD: BLOCKING & DETECTION"),
        left + 12,
        top + 7,
        0xDBF8FF,
        false);
    if (tab == 0) {
      String status =
          emitter.powered
              ? "Field running"
              : emitter.enabled ? "Waiting for energy or redstone input" : "Field switched off";
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
    if (tab == 4) {
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

    g.drawString(font, font.plainSubstrByWidth(notice, 218), left + 12, top + 206, 0x92A9BE, false);
    super.render(g, mx, my, partial);
    g.pose().popPose();
  }
}
