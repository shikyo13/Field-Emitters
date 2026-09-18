package com.zerotheabsolute.fieldemitters.client;

import com.zerotheabsolute.fieldemitters.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import com.zerotheabsolute.fieldemitters.network.ForgePacketDistributor;

/** Changes are sent immediately; text is validated after a short typing pause. */
public final class ControlScreen extends FittedScreen {
  private final EmitterEntity emitter;
  private ControlSettings draft;
  private int color, left, top, row;
  private ControlTab tab = ControlTab.OVERVIEW;
  private int selectedLink = -1;
  private Direction filterDirection;
  private boolean filterEditable = true;
  private boolean enabled, reset = false;
  private boolean showGuides = FieldConfig.SHOW_GUIDES.get();
  private String notice = UiText.text("screen.fieldemitters.control.changes_apply_automatically");
  private long textDue;
  private String lastSent = "";
  private static final int CONTENT_WIDTH = 404, NAV_WIDTH = 106, PANEL_HEIGHT = 306;

  public ControlScreen(EmitterEntity e) {
    super(
        Component.literal(
            UiText.text(
                "screen.fieldemitters.control.field_emitter",
                e.getBlockPos().toShortString(),
                net.neoforged.fml.ModList.get()
                    .getModContainerById(com.zerotheabsolute.fieldemitters.FieldEmitters.ID)
                    .map(c -> c.getModInfo().getVersion().toString())
                    .orElse(""))));
    emitter = e;
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

  private int panelHeight() {
    return PANEL_HEIGHT + (tab == ControlTab.APPEARANCE && draft.customAccent ? 57 : 0);
  }

  protected void init() {
    fit(CONTENT_WIDTH + NAV_WIDTH, panelHeight());
    if (selectedLink >= emitter.links.size()) selectedLink = -1;
    left = (width - CONTENT_WIDTH - NAV_WIDTH) / 2 + NAV_WIDTH;
    top = (height - panelHeight()) / 2;
    row = top + 44;
    filterEditable = true;
    for (int i = 0; i < ControlTab.values().length; i++) {
      final int index = i;
      addRenderableWidget(
          new FieldButton(
              left - NAV_WIDTH + 8,
              top + 44 + i * 27,
              NAV_WIDTH - 16,
              23,
              Component.translatable(ControlTab.values()[i].translationKey()),
              b -> {
                applyPending();
                tab = ControlTab.values()[index];
                rebuildWidgets();
              },
              tab.ordinal() == index));
    }
    if (tab == ControlTab.OVERVIEW) overviewControls();
    if (tab.hasFilter()) filterControls();
    if (tab == ControlTab.APPEARANCE) appearanceControls();
    if (tab == ControlTab.SOUNDS) soundsControls();
    if (tab == ControlTab.CONNECTIONS) connectionsControls();
    if (tab == ControlTab.ACCESS) accessControls();
    addRenderableWidget(
            new FieldButton(
                left + 242,
                top + panelHeight() - 23,
                72,
                18,
                Component.literal(UiText.text("screen.fieldemitters.control.fields")),
                b -> {
                  applyPending();
                  RemoteScreen.openManager();
                },
                false))
        .setTooltip(
            Tooltip.create(
                Component.literal(
                    UiText.text(
                        "screen.fieldemitters.control.manage_loaded_emitters_remotely_hold_a_field_tuner"))));
    addRenderableWidget(
            new FieldButton(
                left + 320,
                top + panelHeight() - 23,
                72,
                18,
                Component.literal(UiText.text("screen.fieldemitters.control.close")),
                b -> onClose(),
                false))
        .setTooltip(
            Tooltip.create(
                Component.literal(
                    UiText.text(
                        "screen.fieldemitters.control.close_this_menu_valid_changes_have_already_been"))));
  }

  private void overviewControls() {
    row += 56;
    button(
        Control.FIELD,
        UiText.text(
            "screen.fieldemitters.control.field",
            (enabled
                ? UiText.text("screen.fieldemitters.control.on")
                : UiText.text("screen.fieldemitters.control.off"))),
        () -> {
          enabled = !enabled;
          redraw();
        });
    button(
        Control.TURN_ON,
        UiText.text(
            "screen.fieldemitters.control.turn_on",
            new String[] {
                  UiText.text("screen.fieldemitters.control.whenever_energy_is_available"),
                  UiText.text("screen.fieldemitters.control.when_redstone_is_on"),
                  UiText.text("screen.fieldemitters.control.when_redstone_is_off")
                }
                [draft.inputMode]),
        () -> {
          draft.inputMode = (draft.inputMode + 1) % 3;
          redraw();
        });
    button(
        Control.READ_REDSTONE_FROM,
        UiText.text(
            "screen.fieldemitters.control.read_redstone_from",
            draft.inputAny
                ? UiText.text("screen.fieldemitters.control.any_side")
                : sideName(draft.inputFace)),
        () -> {
          if (draft.inputAny) {
            draft.inputAny = false;
            draft.inputFace = Direction.DOWN == draft.outputFace ? Direction.UP : Direction.DOWN;
          } else {
            var next = nextFace(draft.inputFace, draft.outputFace);
            if (next.ordinal() <= draft.inputFace.ordinal()) draft.inputAny = true;
            else draft.inputFace = next;
          }
          redraw();
        });
  }

  private void filterControls() {
    var shared =
        tab == ControlTab.BLOCKING
            ? draft.barrier
            : tab == ControlTab.SENSOR ? draft.sensor : draft.damage;
    var rules =
        tab == ControlTab.BLOCKING
            ? draft.barrierDirections
            : tab == ControlTab.SENSOR ? draft.sensorDirections : draft.damageDirections;
    small(
        Control.RULES_FOR,
        UiText.text(
            "screen.fieldemitters.control.rules_for",
            (filterDirection == null
                ? UiText.text("screen.fieldemitters.control.all_directions")
                : travelName(filterDirection))),
        left + 12,
        row,
        220,
        () -> {
          applyPending();
          filterDirection =
              filterDirection == null
                  ? Direction.DOWN
                  : filterDirection == Direction.EAST
                      ? null
                      : Direction.values()[filterDirection.ordinal() + 1];
          rebuildWidgets();
        });
    small(
        Control.RULE_SOURCE,
        UiText.text(
            "screen.fieldemitters.control.rule_source",
            (filterDirection == null
                ? UiText.text("screen.fieldemitters.control.shared")
                : rules.has(filterDirection)
                    ? UiText.text("screen.fieldemitters.control.custom")
                    : UiText.text("screen.fieldemitters.control.shared"))),
        left + 237,
        row,
        155,
        () -> {
          if (filterDirection == null) return;
          applyPending();
          if (rules.has(filterDirection)) rules.inherit(filterDirection);
          else rules.set(filterDirection, EntityFilter.load(shared.save()));
          redraw();
        });
    row += ScreenMetrics.ROW_HEIGHT;
    filterEditable = filterDirection == null || rules.has(filterDirection);
    var f = filterDirection == null ? shared : rules.resolve(filterDirection, shared);
    if (tab == ControlTab.SENSOR) {
      small(
          Control.SIGNAL,
          UiText.text(
              "screen.fieldemitters.control.signal",
              new String[] {
                    UiText.text("screen.fieldemitters.control.off"),
                    UiText.text("screen.fieldemitters.control.pulse_on_crossing"),
                    UiText.text("screen.fieldemitters.control.on_while_touching")
                  }
                  [draft.sensorMode]),
          left + 12,
          row,
          185,
          () -> {
            draft.sensorMode = (draft.sensorMode + 1) % 3;
            redraw();
          });
      small(
          Control.ITEMS,
          UiText.text(
              "screen.fieldemitters.control.items",
              (draft.countItems
                  ? UiText.text("screen.fieldemitters.control.count_each_item")
                  : UiText.text("screen.fieldemitters.control.count_each_stack"))),
          left + 207,
          row,
          185,
          () -> {
            draft.countItems = !draft.countItems;
            redraw();
          });
      row += ScreenMetrics.ROW_HEIGHT;
    }
    if (tab == ControlTab.DAMAGE) {
      small(
          Control.DAMAGE,
          UiText.text(
              "screen.fieldemitters.control.damage",
              (draft.damageEnabled
                  ? UiText.text("screen.fieldemitters.control.on")
                  : UiText.text("screen.fieldemitters.control.off"))),
          left + 12,
          row,
          185,
          () -> {
            draft.damageEnabled = !draft.damageEnabled;
            redraw();
          });
      small(
          Control.HIT_INTERVAL,
          UiText.text(
              "screen.fieldemitters.control.hit_interval", UiText.seconds(draft.damageInterval)),
          left + 207,
          row,
          185,
          () -> {
            draft.damageInterval =
                draft.damageInterval >= ScreenMetrics.DAMAGE_INTERVAL_CYCLE_MAX
                    ? ControlSettings.MIN_DAMAGE_INTERVAL
                    : draft.damageInterval + ScreenMetrics.DAMAGE_INTERVAL_STEP;
            redraw();
          });
      row += ScreenMetrics.ROW_HEIGHT;
      field(
          Control.DAMAGE_PER_HIT_HP_2_1_HEART,
          UiText.text("screen.fieldemitters.control.damage_per_hit_hp_2_1_heart"),
          Float.toString(draft.damageAmount),
          v -> {
            try {
              draft.damageAmount = Float.parseFloat(v);
            } catch (NumberFormatException ex) {
              draft.damageAmount = Float.NaN;
            }
          },
          left + 12,
          row,
          185);
      small(
          Control.FIZZLE_PARTICLES,
          UiText.text(
              "screen.fieldemitters.control.fizzle_particles",
              (draft.fizzleEffects
                  ? UiText.text("screen.fieldemitters.control.on")
                  : UiText.text("screen.fieldemitters.control.off"))),
          left + 207,
          row + 8,
          185,
          () -> {
            draft.fizzleEffects = !draft.fizzleEffects;
            redraw();
          });
      row += 29;
    }
    Control[] categoryControls = {
      Control.HOSTILE, Control.PASSIVE, Control.PLAYERS, Control.DROPS, Control.NONLIVING
    };
    String[] groups = {
      UiText.text("screen.fieldemitters.control.hostile"),
      UiText.text("screen.fieldemitters.control.passive"),
      UiText.text("screen.fieldemitters.control.players"),
      UiText.text("screen.fieldemitters.control.drops"),
      UiText.text("screen.fieldemitters.control.nonliving")
    };
    for (int i = 0; i < categoryControls.length; i++) {
      final int bit = 1 << i;
      small(
          categoryControls[i],
          ((f.groups & bit) != 0 ? "✓ " : "○ ") + groups[i],
          left + 12 + i * 77,
          row,
          74,
          () -> {
            f.groups ^= bit;
            redraw();
          },
          (f.groups & bit) != 0);
    }
    row += ScreenMetrics.ROW_HEIGHT;
    small(
        tab == ControlTab.BLOCKING
            ? (f.inverted ? Control.ALLOW_SELECTED : Control.BLOCK_SELECTED)
            : tab == ControlTab.SENSOR
                ? (f.inverted ? Control.DETECT_UNSELECTED : Control.DETECT_SELECTED)
                : Control.DAMAGE_SELECTED,
        (tab == ControlTab.BLOCKING
            ? (f.inverted
                ? UiText.text("screen.fieldemitters.control.allow_selected_only")
                : UiText.text("screen.fieldemitters.control.block_selected"))
            : tab == ControlTab.SENSOR
                ? (f.inverted
                    ? UiText.text("screen.fieldemitters.control.detect_unselected")
                    : UiText.text("screen.fieldemitters.control.detect_selected"))
                : (f.inverted
                    ? UiText.text("screen.fieldemitters.control.damage_unselected")
                    : UiText.text("screen.fieldemitters.control.damage_selected"))),
        left + 12,
        row,
        126,
        () -> {
          f.inverted = !f.inverted;
          redraw();
        });
    small(
        Control.AGE,
        UiText.text(
            "screen.fieldemitters.control.age",
            new String[] {
                  UiText.text("screen.fieldemitters.control.any_age"),
                  UiText.text("screen.fieldemitters.control.babies_only"),
                  UiText.text("screen.fieldemitters.control.adults_only")
                }
                [f.age]),
        left + 142,
        row,
        126,
        () -> {
          f.age = (f.age + 1) % 3;
          redraw();
        });
    small(
        Control.SKIP_OWNER,
        UiText.text(
            "screen.fieldemitters.control.skip_owner",
            (f.exemptOwner
                ? UiText.text("screen.fieldemitters.control.yes")
                : UiText.text("screen.fieldemitters.control.no"))),
        left + 272,
        row,
        126,
        () -> {
          f.exemptOwner = !f.exemptOwner;
          redraw();
        });
    row += ScreenMetrics.ROW_HEIGHT;
    small(
        Control.MOB_LIST,
        UiText.text("screen.fieldemitters.control.mob_list"),
        left + 12,
        row,
        185,
        () -> {
          applyPending();
          minecraft.setScreen(
              new TypeListScreen(this, f, false, tab.purpose(), this::applyChanges));
        });
    small(
        Control.ITEM_LIST,
        UiText.text("screen.fieldemitters.control.item_list"),
        left + 207,
        row,
        185,
        () -> {
          applyPending();
          minecraft.setScreen(new TypeListScreen(this, f, true, tab.purpose(), this::applyChanges));
        });
    row += 27;
    field(
        Control.SPECIFIC_MOB_PLAYER_UUID,
        UiText.text("screen.fieldemitters.control.specific_mob_player_uuid"),
        f.identity,
        v -> f.identity = v,
        left + 12,
        row,
        185);
    field(
        Control.CUSTOM_ENTITY_LABEL_TAG,
        UiText.text("screen.fieldemitters.control.custom_entity_label_tag"),
        f.entityTag,
        v -> f.entityTag = v,
        left + 207,
        row,
        185);
    row += 27;
    if (filterDirection != null) {
      small(
          Control.MOVEMENT,
          movementName(filterDirection),
          left + 12,
          row,
          ScreenMetrics.CONTENT_WIDTH,
          () -> {
            f.directions ^= 1 << filterDirection.ordinal();
            redraw();
          },
          f.direction(filterDirection));
    } else {
      for (int i = 0; i < 6; i++) {
        final var d = Direction.values()[i];
        small(
            Control.MOVEMENT,
            movementName(d),
            left + 12 + i * 64,
            row,
            61,
            () -> {
              f.directions ^= 1 << d.ordinal();
              redraw();
            },
            f.direction(d));
      }
    }
    row += ScreenMetrics.ROW_HEIGHT;
    small(
        Control.SAMPLE_INDIVIDUAL,
        UiText.text("screen.fieldemitters.control.sample_individual"),
        left + 12,
        row,
        124,
        () -> {
          sample(f, true);
          redraw();
        });
    small(
        Control.SAMPLE_TYPE,
        UiText.text("screen.fieldemitters.control.sample_type"),
        left + 140,
        row,
        124,
        () -> {
          sample(f, false);
          redraw();
        });
    small(
        Control.PLAYER_LIST,
        UiText.text("screen.fieldemitters.control.player_list"),
        left + 268,
        row,
        124,
        () -> {
          applyPending();
          minecraft.setScreen(
              new PlayerListScreen(
                  this, emitter.getBlockPos(), f, tab.purpose(), this::applyChanges));
        });
    if (tab == ControlTab.SENSOR) {
      row += 24;
      small(
          Control.OUTPUT_SIDE,
          UiText.text("screen.fieldemitters.control.output_side", sideName(draft.outputFace)),
          left + 12,
          row,
          185,
          () -> {
            draft.outputFace = nextFace(draft.outputFace, draft.inputFace);
            redraw();
          });
      small(
          Control.PULSE_LENGTH,
          UiText.text(
              "screen.fieldemitters.control.pulse_length", UiText.seconds(draft.pulseTicks)),
          left + 207,
          row,
          185,
          () -> {
            draft.pulseTicks = draft.pulseTicks >= 20 ? 2 : draft.pulseTicks + 2;
            redraw();
          });
      row += ScreenMetrics.ROW_HEIGHT;
      button(
          Control.RESET_CROSSING,
          UiText.text("screen.fieldemitters.control.reset_crossing_count_now"),
          () -> {
            reset = true;
            applyChanges();
          });
    }
  }

  private void appearanceControls() {
    colorPresets(
        UiText.text("screen.fieldemitters.control.field_color_presets"), color, c -> color = c);
    field(
        Control.CUSTOM_COLOR_6_DIGIT_HEX_CODE,
        UiText.text("screen.fieldemitters.control.custom_color_6_digit_hex_code"),
        String.format("%06X", color),
        v -> {
          try {
            if (!v.matches("#?[0-9a-fA-F]{6}")) throw new NumberFormatException();
            color = Integer.parseInt(v.replace("#", ""), 16) & 0xffffff;
          } catch (NumberFormatException ex) {
            notice =
                UiText.text("screen.fieldemitters.control.invalid_color_previous_color_retained");
          }
        },
        left + 12,
        row,
        ScreenMetrics.CONTENT_WIDTH);
    row += 29;
    button(
        Control.EFFECT_COLOR,
        UiText.text(
            "screen.fieldemitters.control.effect_color",
            (draft.customAccent
                ? UiText.text("screen.fieldemitters.control.custom_accent")
                : UiText.text("screen.fieldemitters.control.matches_field"))),
        () -> {
          draft.customAccent = !draft.customAccent;
          redraw();
        });
    if (draft.customAccent) {
      colorPresets(
          UiText.text("screen.fieldemitters.control.effect_color_presets"),
          draft.particleColor,
          c -> draft.particleColor = c);
      field(
          Control.EFFECT_COLOR_6_DIGIT_HEX,
          UiText.text("screen.fieldemitters.control.effect_color_6_digit_hex"),
          String.format("%06X", draft.particleColor),
          v -> {
            if (v.matches("#?[0-9a-fA-F]{6}"))
              draft.particleColor = Integer.parseInt(v.replace("#", ""), 16);
            else
              notice =
                  UiText.text(
                      "screen.fieldemitters.control.invalid_effect_color_previous_color_retained");
          },
          left + 12,
          row,
          ScreenMetrics.CONTENT_WIDTH);
      row += 29;
    }
    button(
        Control.PRESET_PURPLE_FIELD_MAGENTA_EFFECTS,
        UiText.text("screen.fieldemitters.control.preset_purple_field_magenta_effects"),
        () -> {
          color = 0x9933FF;
          draft.particleColor = 0xFF33CC;
          draft.customAccent = true;
          draft.pattern = 2;
          redraw();
        });
    button(
        Control.PATTERN,
        UiText.text(
            "screen.fieldemitters.control.pattern",
            new String[] {
                  UiText.text("screen.fieldemitters.control.hex_lattice"),
                  UiText.text("screen.fieldemitters.control.smooth_glow"),
                  UiText.text("screen.fieldemitters.control.drifting_pixels"),
                  UiText.text("screen.fieldemitters.control.plasma")
                }
                [draft.pattern]),
        () -> {
          draft.pattern = (draft.pattern + 1) % 4;
          redraw();
        });
    if (emitter.isTower())
      button(
          Control.PROJECTION,
          UiText.text(
              "screen.fieldemitters.control.projection", UiText.formation(draft.projection)),
          () -> {
            draft.projection = draft.projection.next();
            redraw();
          });
    else
      button(
          Control.FORMATION,
          UiText.text("screen.fieldemitters.control.formation", UiText.formation(draft.formation)),
          () -> {
            draft.formation =
                (draft.formation + 1) % com.zeromods.core.animation.PlanarProjection.PRESET_COUNT;
            redraw();
          });
    button(
        Control.LIGHT_NEARBY_BLOCKS,
        UiText.text(
            "screen.fieldemitters.control.light_nearby_blocks",
            (draft.light
                ? UiText.text("screen.fieldemitters.control.yes")
                : UiText.text("screen.fieldemitters.control.no_for_dark_mob_farms"))),
        () -> {
          draft.light = !draft.light;
          redraw();
        });
    button(
        Control.SHOW_FORCEFIELD,
        UiText.text(
            "screen.fieldemitters.control.show_forcefield",
            (draft.visible
                ? UiText.text("screen.fieldemitters.control.yes")
                : UiText.text("screen.fieldemitters.control.no_still_works"))),
        () -> {
          draft.visible = !draft.visible;
          redraw();
        });
    button(
        Control.ANIMATE_FIELD_PATTERN,
        UiText.text(
            "screen.fieldemitters.control.animate_field_pattern",
            (draft.animation
                ? UiText.text("screen.fieldemitters.control.yes")
                : UiText.text("screen.fieldemitters.control.no"))),
        () -> {
          draft.animation = !draft.animation;
          redraw();
        });
    button(
        Control.DIRECTION_GUIDES,
        UiText.text(
            "screen.fieldemitters.control.direction_guides",
            (showGuides
                ? UiText.text("screen.fieldemitters.control.show_with_tuner")
                : UiText.text("screen.fieldemitters.control.hidden"))),
        () -> {
          showGuides = !showGuides;
          FieldConfig.SHOW_GUIDES.set(showGuides);
          FieldConfig.SHOW_GUIDES.save();
          rebuildWidgets();
        });
  }

  private void soundsControls() {
    button(
        Control.ALL_FIELD_SOUNDS,
        UiText.text(
            "screen.fieldemitters.control.all_field_sounds",
            (draft.sounds
                ? UiText.text("screen.fieldemitters.control.on")
                : UiText.text("screen.fieldemitters.control.off"))),
        () -> {
          draft.sounds = !draft.sounds;
          redraw();
        });
    button(
        Control.POWER_ON_OFF_SOUNDS,
        UiText.text(
            "screen.fieldemitters.control.power_on_off_sounds",
            (draft.powerSounds
                ? UiText.text("screen.fieldemitters.control.on")
                : UiText.text("screen.fieldemitters.control.off"))),
        () -> {
          draft.powerSounds = !draft.powerSounds;
          redraw();
        });
    button(
        Control.IMPACT_SOUNDS,
        UiText.text(
            "screen.fieldemitters.control.impact_sounds",
            (draft.impactSounds
                ? UiText.text("screen.fieldemitters.control.on")
                : UiText.text("screen.fieldemitters.control.off"))),
        () -> {
          draft.impactSounds = !draft.impactSounds;
          redraw();
        });
    button(
        Control.DAMAGE_SOUNDS,
        UiText.text(
            "screen.fieldemitters.control.damage_sounds",
            (draft.damageSounds
                ? UiText.text("screen.fieldemitters.control.on")
                : UiText.text("screen.fieldemitters.control.off"))),
        () -> {
          draft.damageSounds = !draft.damageSounds;
          redraw();
        });
    button(
        Control.SOUND_PALETTE,
        UiText.text(
            "screen.fieldemitters.control.sound_palette",
            new String[] {
                  UiText.text("screen.fieldemitters.control.soft_sizzle"),
                  UiText.text("screen.fieldemitters.control.crystal"),
                  UiText.text("screen.fieldemitters.control.electric")
                }
                [draft.soundStyle]),
        () -> {
          draft.soundStyle = (draft.soundStyle + 1) % 3;
          redraw();
        });
  }

  private void connectionsControls() {
    if (emitter.isTower()) {
      button(
          Control.SHAPE,
          UiText.text(
              "screen.fieldemitters.control.shape",
              (draft.dome
                  ? UiText.text("screen.fieldemitters.control.dome")
                  : UiText.text("screen.fieldemitters.control.full_sphere"))),
          () -> {
            draft.dome = !draft.dome;
            redraw();
          });
      button(
          Control.RADIUS,
          UiText.text("screen.fieldemitters.control.radius_blocks", draft.sphereRadius),
          () -> {
            draft.sphereRadius =
                draft.sphereRadius >= SphereField.MAX_RADIUS
                    ? SphereField.MIN_RADIUS
                    : draft.sphereRadius + 2;
            redraw();
          });
    }
    button(
        Control.EDITING,
        UiText.text(
            "screen.fieldemitters.control.editing",
            (selectedLink < 0
                ? UiText.text("screen.fieldemitters.control.default_rules_for_this_emitter")
                : UiText.text(
                    "screen.fieldemitters.control.only_the_field_to",
                    emitter.links.get(selectedLink).target().toShortString()))),
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
    if (emitter.isRail())
      button(
          Control.FIELD_SHAPE,
          UiText.text("screen.fieldemitters.control.field_shape", planeName()),
          () -> {
            cyclePlane();
            redraw();
          });
    if (emitter.isRail()
        && emitter.getBlockState().getValue(RailBlock.FACING).getAxis() != Direction.Axis.Y)
      button(
          Control.BRIDGE_PRESET,
          UiText.text("screen.fieldemitters.control.bridge_preset_safe_walking_from_above"),
          () -> {
            draft.railNormal = Direction.Axis.Y.ordinal();
            draft.barrier = new EntityFilter();
            draft.barrier.groups = 31;
            draft.barrier.exemptOwner = false;
            draft.barrier.directions = 1 << Direction.DOWN.ordinal();
            for (var direction : Direction.values()) draft.barrierDirections.inherit(direction);
            draft.damageEnabled = false;
            redraw();
          });
    row += 8;
  }

  private void accessControls() {
    labels.add(
        new Label(
            UiText.text("screen.fieldemitters.control.who_can_change_network_settings"),
            left + 12,
            row));
    row += 18;
    button(
        Control.MANAGERS_AND_PUBLIC_ACCESS,
        UiText.text("screen.fieldemitters.control.managers_and_public_access"),
        () -> {
          applyPending();
          minecraft.setScreen(new ManagementScreen(this, emitter.getBlockPos()));
        });
    row += 24;
    labels.add(
        new Label(
            UiText.text("screen.fieldemitters.control.who_can_pass_using_an_access_badge"),
            left + 12,
            row));
    row += 18;
    button(
        Control.ISSUE_AND_REVOKE_BADGES,
        UiText.text("screen.fieldemitters.control.issue_and_revoke_badges"),
        () -> {
          applyPending();
          minecraft.setScreen(
              new AccessScreen(this, emitter.getBlockPos(), null, this::applyChanges));
        });
    row += 24;
    labels.add(
        new Label(
            UiText.text("screen.fieldemitters.control.inspect_items_carried_through_the_field"),
            left + 12,
            row));
    row += 18;
    button(
        Control.INVENTORY_CHECKPOINT,
        UiText.text("screen.fieldemitters.control.inventory_checkpoint"),
        () -> {
          applyPending();
          minecraft.setScreen(
              new CheckpointScreen(
                  this, emitter.getBlockPos(), draft.checkpoint, this::applyChanges));
        });
  }

  @Override
  public void tick() {
    if (!canConfigure()) {
      textDue = 0;
      minecraft.setScreen(null);
      return;
    }
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

  boolean canConfigure() {
    return minecraft.player != null
        && !emitter.isRemoved()
        && FieldControls.editable(emitter, minecraft.player);
  }

  private void applyChanges() {
    textDue = 0;
    if (!canConfigure()) return;
    Component error = FieldControls.validate(draft);
    if (error != null) {
      notice = error.getString();
      return;
    }
    if (selectedLink >= emitter.links.size()) {
      notice = UiText.text("screen.fieldemitters.control.field_disconnected_reopen_settings");
      return;
    }
    String signature = draft.save().toString() + color + enabled + selectedLink;
    if (!reset && signature.equals(lastSent)) return;
    var target =
        selectedLink < 0 ? emitter.getBlockPos() : emitter.links.get(selectedLink).target();
    ForgePacketDistributor.sendToServer(
        new FieldControls.Update(
            emitter.getBlockPos(),
            draft.save(),
            color,
            enabled,
            true,
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
    notice = UiText.text("screen.fieldemitters.control.applying_changes");
  }

  public void acknowledge(Component message) {
    notice = message.getString();
    if (!(message.getContents()
            instanceof net.minecraft.network.chat.contents.TranslatableContents translated
        && translated.getKey().equals("message.fieldemitters.fieldcontrols.changes_applied")))
      lastSent = "";
  }

  private static String sideName(Direction d) {
    return UiText.direction(d);
  }

  private static String travelName(Direction direction) {
    return switch (direction) {
      case NORTH -> UiText.text("screen.fieldemitters.control.south_north");
      case SOUTH -> UiText.text("screen.fieldemitters.control.north_south");
      case EAST -> UiText.text("screen.fieldemitters.control.west_east");
      case WEST -> UiText.text("screen.fieldemitters.control.east_west");
      case UP -> UiText.text("screen.fieldemitters.control.below_above");
      case DOWN -> UiText.text("screen.fieldemitters.control.above_below");
    };
  }

  private static String movementName(Direction d) {
    return d == Direction.UP
        ? UiText.text("screen.fieldemitters.control.upward")
        : d == Direction.DOWN
            ? UiText.text("screen.fieldemitters.control.downward")
            : UiText.text("screen.fieldemitters.control.to", sideName(d));
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
      case X -> UiText.text("screen.fieldemitters.control.wall_facing_east_west");
      case Z -> UiText.text("screen.fieldemitters.control.wall_facing_north_south");
      case Y -> UiText.text("screen.fieldemitters.control.horizontal_floor_ceiling");
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
    String[] names = {
      UiText.text("screen.fieldemitters.control.cyan"),
      UiText.text("screen.fieldemitters.control.aqua"),
      UiText.text("screen.fieldemitters.control.blue"),
      UiText.text("screen.fieldemitters.control.purple"),
      UiText.text("screen.fieldemitters.control.pink"),
      UiText.text("screen.fieldemitters.control.red"),
      UiText.text("screen.fieldemitters.control.orange"),
      UiText.text("screen.fieldemitters.control.white")
    };
    for (int i = 0; i < colors.length; i++) {
      final int value = colors[i];
      var button =
          new FieldButton(
              left + 12 + i * 48,
              row + 9,
              44,
              18,
              Component.literal(names[i]),
              b -> {
                choose.accept(value);
                redraw();
              },
              selected == value,
              value);
      button.active = selectedLink < 0;
      button.setTooltip(
          Tooltip.create(
              Component.literal(
                  selectedLink >= 0
                      ? UiText.text(
                          "screen.fieldemitters.control.select_emitter_defaults_on_links_to_change_colors")
                      : UiText.text(
                          "screen.fieldemitters.control.applies_immediately_keeps_your_other_visual_settings",
                          names[i],
                          label))));
      addRenderableWidget(button);
    }
    row += 28;
  }

  private void button(Control control, String text, Runnable action) {
    small(control, text, left + 12, row, ScreenMetrics.CONTENT_WIDTH, action);
    row += ScreenMetrics.ROW_HEIGHT;
  }

  private static boolean sensorSetting(Control control) {
    return control == Control.SIGNAL
        || control == Control.ITEMS
        || control == Control.OUTPUT_SIDE
        || control == Control.PULSE_LENGTH
        || control == Control.RESET_CROSSING;
  }

  private void small(Control control, String text, int x, int y, int w, Runnable action) {
    small(control, text, x, y, w, action, false);
  }

  private void small(
      Control control, String text, int x, int y, int w, Runnable action, boolean selected) {
    var button =
        new FieldButton(
            x,
            y,
            w,
            ScreenMetrics.BUTTON_HEIGHT,
            Component.literal(text),
            b -> action.run(),
            selected);
    String help = text + "\n\n" + ControlHelp.button(control, text, tab);
    boolean fieldOnly =
        selectedLink >= 0
            && control != Control.EDITING
            && control != Control.DIRECTION_GUIDES
            && (tab == ControlTab.OVERVIEW
                || tab == ControlTab.APPEARANCE
                || tab == ControlTab.SOUNDS
                || tab == ControlTab.CONNECTIONS
                || tab == ControlTab.DAMAGE && control == Control.FIZZLE_PARTICLES
                || tab == ControlTab.ACCESS && control == Control.INVENTORY_CHECKPOINT
                || tab == ControlTab.SENSOR && sensorSetting(control));
    boolean inherited =
        !filterEditable
            && (tab.hasFilter())
            && !sensorSetting(control)
            && control != Control.DAMAGE
            && control != Control.HIT_INTERVAL
            && control != Control.FIZZLE_PARTICLES;
    button.active = !fieldOnly && !inherited;
    button.setTooltip(
        Tooltip.create(
            Component.literal(
                inherited
                    ? UiText.text(
                        "screen.fieldemitters.control.this_direction_uses_the_shared_filter_choose_rule")
                    : fieldOnly
                        ? UiText.text(
                            "screen.fieldemitters.control.this_setting_belongs_to_the_emitter_in_connections")
                        : help)));
    addRenderableWidget(button);
  }

  private final List<Label> labels = new ArrayList<>();

  private record Label(String text, int x, int y, int width) {
    Label(String text, int x, int y) {
      this(text, x, y, ScreenMetrics.CONTENT_WIDTH);
    }
  }

  private void field(
      Control control,
      String label,
      String value,
      java.util.function.Consumer<String> save,
      int x,
      int y,
      int w) {
    labels.add(new Label(label, x, y, w));
    var box = new EditBox(font, x, y + 9, w, 16, Component.literal(label));
    box.setMaxLength(EntityFilter.MAX_TYPE_LENGTH);
    box.setValue(value);
    box.setTooltip(Tooltip.create(Component.literal(ControlHelp.field(control))));
    if (!filterEditable && (tab.hasFilter()) && control != Control.DAMAGE_PER_HIT_HP_2_1_HEART) {
      box.setEditable(false);
      box.setTooltip(
          Tooltip.create(
              Component.literal(
                  UiText.text(
                      "screen.fieldemitters.control.choose_rule_source_custom_to_edit_this_direction"))));
    }
    if (selectedLink >= 0 && tab == ControlTab.APPEARANCE) {
      box.setEditable(false);
      box.setTooltip(
          Tooltip.create(
              Component.literal(
                  UiText.text(
                      "screen.fieldemitters.control.appearance_belongs_to_the_emitter_select_default_rules"))));
    }
    addRenderableWidget(box);
    box.setResponder(
        v -> {
          save.accept(v.trim());
          textDue = System.currentTimeMillis() + ScreenMetrics.TEXT_DEBOUNCE_MILLIS;
          notice =
              UiText.text("screen.fieldemitters.control.typing_valid_changes_apply_automatically");
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
          notice =
              UiText.text(
                  "screen.fieldemitters.control.sample_uuid_now_narrows_the_mob_list_selection");
        } else if (f.mobList.contains(t.getString("SampleType"))
            || f.mobList.size() < EntityFilter.MAX_TYPES) {
          f.mobList.add(t.getString("SampleType"));
          notice = UiText.text("screen.fieldemitters.control.sample_type_added_to_mob_list");
        } else notice = UiText.text("screen.fieldemitters.control.mob_list_is_full_64_entries");
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
      notice = UiText.text("screen.fieldemitters.control.sample_selected");
      return;
    }
    notice = UiText.text("screen.fieldemitters.control.first_sneak_use_a_mob_or_air_for");
  }

  private String fitLabel(Label label) {
    if (font.width(label.text) <= label.width) return label.text;
    String ellipsis = "…";
    return font.plainSubstrByWidth(label.text, Math.max(0, label.width - font.width(ellipsis)))
        + ellipsis;
  }

  public void render(GuiGraphics g, int mx, int my, float partial) {
    beginFit(g);
    mx = fitMouse(mx);
    my = fitMouse(my);
    g.fill(0, 0, width, height, 0xB0101723);
    g.fill(left - NAV_WIDTH, top, left + CONTENT_WIDTH, top + panelHeight(), 0xFF111D2C);
    g.fill(left - NAV_WIDTH + 4, top + 40, left - 4, top + panelHeight() - 28, 0xFF0E1926);
    g.drawString(
        font,
        UiText.text("screen.fieldemitters.control.controls"),
        left - NAV_WIDTH + 12,
        top + 26,
        0x92A9BE,
        false);
    g.fill(left + 1, top + 2, left + 403, top + 20, 0xFF1B2D3E);
    g.fill(left + 8, top + 40, left + 396, top + panelHeight() - 28, 0xFF0E1926);
    g.fill(left + 8, top + panelHeight() - 26, left + 396, top + panelHeight() - 25, 0xFF354D63);
    g.fill(left - NAV_WIDTH, top, left + 404, top + 2, 0xFF000000 | color);
    g.drawString(
        font,
        selectedLink < 0
            ? title
            : Component.literal(
                UiText.text("screen.fieldemitters.control.editing_one_field_block_detect_damage")),
        left + 12,
        top + 7,
        0xDBF8FF,
        false);
    String scope =
        tab == ControlTab.ACCESS
            ? UiText.text(
                "screen.fieldemitters.control.badge_access_does_not_grant_management_access")
            : selectedLink >= 0
                ? UiText.text(
                    "screen.fieldemitters.control.this_connection_only",
                    emitter.links.get(selectedLink).target().toShortString())
                : UiText.text(
                    "screen.fieldemitters.control.changes_affect_your_connected_emitters");
    g.drawString(font, scope, left + 12, top + 26, 0x92A9BE, false);
    if (tab == ControlTab.OVERVIEW) {
      String status =
          emitter.powered
              ? UiText.text("screen.fieldemitters.control.field_running")
              : !emitter.enabled
                  ? UiText.text("screen.fieldemitters.control.field_switched_off")
                  : emitter.energy.getEnergyStored() == 0
                      ? UiText.text("screen.fieldemitters.control.no_energy_connect_fe")
                      : emitter.energy.getEnergyStored() < emitter.demand
                          ? UiText.text(
                              "screen.fieldemitters.control.not_enough_energy_needs_fe_t",
                              emitter.demand)
                          : emitter.controls.inputMode != 0
                              ? UiText.text("screen.fieldemitters.control.waiting_for_redstone_input")
                              : UiText.text("screen.fieldemitters.control.starting");
      if (emitter.isTower() && emitter.enabled) {
        if (!SphereField.fitsHeight(emitter))
          status =
              UiText.text(
                  "screen.fieldemitters.control.outside_world_height_move_tower_or_reduce_radius");
        else if (emitter.powered && !SphereField.formed(emitter))
          status = UiText.text("screen.fieldemitters.control.projecting_field");
      }
      g.drawString(
          font,
          font.plainSubstrByWidth(status, ScreenMetrics.CONTENT_WIDTH),
          left + 12,
          top + 44,
          0x77FFBD,
          false);
      g.drawString(
          font,
          UiText.text(
              "screen.fieldemitters.control.energy_stored_fe_used_fe_t",
              emitter.energy.getEnergyStored(),
              emitter.demand),
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
          UiText.text(
              "screen.fieldemitters.control.crossings_signals_waiting",
              (emitter.isRail() ? UiText.text("screen.fieldemitters.control.rail_chain") : ""),
              detector.crossings,
              detector.queuedPulses),
          left + 12,
          top + 68,
          0xCAD6E5,
          false);
      g.drawString(
          font,
          font.plainSubstrByWidth(
              UiText.text("screen.fieldemitters.control.last_detected", detector.lastDetection),
              ScreenMetrics.CONTENT_WIDTH),
          left + 12,
          top + 80,
          0xCAD6E5,
          false);
    }
    if (tab == ControlTab.CONNECTIONS) {
      int y = row;
      if (emitter.isRail()) {
        g.drawString(
            font,
            UiText.text(
                "screen.fieldemitters.control.connect_redstone_at", emitter.root.toShortString()),
            left + 12,
            y,
            0x77FFBD,
            false);
        y += 14;
      }
      for (var link : emitter.links) {
        g.drawString(
            font,
            UiText.text(
                "screen.fieldemitters.control.field_to_blocks",
                link.target().toShortString(),
                link.length()),
            left + 12,
            y,
            0xCAD6E5,
            false);
        y += 14;
        if (y > top + 192) break;
      }
    }
    for (var l : labels) g.drawString(font, fitLabel(l), l.x, l.y, 0x92A9BE, false);

    g.drawString(
        font,
        font.plainSubstrByWidth(notice, 218),
        left + 12,
        top + panelHeight() - 18,
        0x92A9BE,
        false);
    super.render(g, mx, my, partial);
    for (var label : labels) {
      if (font.width(label.text) > label.width
          && mx >= label.x
          && mx < label.x + label.width
          && my >= label.y
          && my < label.y + font.lineHeight) {
        g.renderTooltip(font, Component.literal(label.text), mx, my);
        break;
      }
    }
    g.pose().popPose();
  }
}
