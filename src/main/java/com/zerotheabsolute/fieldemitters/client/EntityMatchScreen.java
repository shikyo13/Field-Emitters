package com.zerotheabsolute.fieldemitters.client;

import com.zeromods.core.client.FittedScreen;
import com.zerotheabsolute.fieldemitters.EntityFilter;
import com.zerotheabsolute.fieldemitters.FieldControls;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class EntityMatchScreen extends FittedScreen {
  private static final int FIRST_FIELD_Y = 50;
  private static final int FIELD_STEP = 44;
  private static final int SAMPLE_Y = FIRST_FIELD_Y + 2 * FIELD_STEP;
  private static final int HELP_Y = SAMPLE_Y + ScreenMetrics.COMPACT_ROW_HEIGHT;
  private static final int HELP_HEIGHT = 64;
  private final Screen parent;
  private final EntityFilter filter;
  private final Runnable apply;
  private final Function<Boolean, String> sample;
  private int left, top;
  private long changedAt;
  private String status = "";

  EntityMatchScreen(Screen parent, EntityFilter filter, Runnable apply,
      Function<Boolean, String> sample) {
    super(Component.literal(UiText.text("screen.fieldemitters.pages.entity_rules")));
    this.parent = parent;
    this.filter = filter;
    this.apply = apply;
    this.sample = sample;
  }

  @Override
  protected void init() {
    fit(ScreenMetrics.PANEL_WIDTH, ScreenMetrics.PANEL_HEIGHT);
    left = (width - ScreenMetrics.PANEL_WIDTH) / 2;
    top = (height - ScreenMetrics.PANEL_HEIGHT) / 2;
    input(Control.SPECIFIC_MOB_PLAYER_UUID, "specific_mob_player_uuid", filter.identity,
        value -> filter.identity = value, FIRST_FIELD_Y);
    input(Control.CUSTOM_ENTITY_LABEL_TAG, "custom_entity_label_tag", filter.entityTag,
        value -> filter.entityTag = value, FIRST_FIELD_Y + FIELD_STEP);
    if (sample != null) {
      sampleButton(true, ScreenMetrics.CONTENT_INSET);
      sampleButton(false, ScreenMetrics.CONTENT_INSET + ScreenMetrics.HALF_CONTENT_WIDTH + ScreenMetrics.COLUMN_GAP);
    }
    addRenderableWidget(new FieldButton(left + ScreenMetrics.CONTENT_INSET,
        top + ScreenMetrics.PANEL_HEIGHT - ScreenMetrics.COMPACT_ROW_HEIGHT,
        ScreenMetrics.CONTENT_WIDTH, ScreenMetrics.BUTTON_HEIGHT,
        Component.literal(UiText.text("screen.fieldemitters.access.back")), b -> onClose(), false));
  }

  private void input(Control control, String key, String value, Consumer<String> save, int y) {
    var box = new FieldEditBox(font, left + ScreenMetrics.CONTENT_INSET, top + y,
        ScreenMetrics.CONTENT_WIDTH, ScreenMetrics.BUTTON_HEIGHT,
        Component.literal(UiText.text("screen.fieldemitters.control." + key)));
    box.setMaxLength(EntityFilter.MAX_TYPE_LENGTH);
    box.setValue(value);
    box.setTooltip(Tooltip.create(Component.literal(ControlHelp.field(control))));
    box.setResponder(text -> {
      save.accept(text.trim());
      changedAt = net.minecraft.Util.getMillis();
    });
    addRenderableWidget(box);
  }

  private void sampleButton(boolean individual, int x) {
    var control = individual ? Control.SAMPLE_INDIVIDUAL : Control.SAMPLE_TYPE;
    String label = UiText.text("screen.fieldemitters.control." + (individual ? "sample_individual" : "sample_type"));
    var button = addRenderableWidget(new FieldButton(left + x, top + SAMPLE_Y,
        ScreenMetrics.HALF_CONTENT_WIDTH, ScreenMetrics.BUTTON_HEIGHT, Component.literal(label),
        b -> {
          status = sample.apply(individual);
          apply.run();
          changedAt = 0;
          rebuildWidgets();
        }, false));
    button.setTooltip(Tooltip.create(Component.literal(ControlHelp.button(control, label, ControlTab.BLOCKING))));
  }

  @Override
  public void tick() {
    if (changedAt == 0 || net.minecraft.Util.getMillis() - changedAt < ScreenMetrics.TEXT_DEBOUNCE_MILLIS) return;
    changedAt = 0;
    Component error = FieldControls.validate(filter);
    if (error == null) {
      apply.run();
      status = UiText.text("screen.fieldemitters.control.changes_applied");
    } else status = error.getString();
  }

  @Override
  public void onClose() {
    apply.run();
    minecraft.setScreen(parent);
  }

  @Override
  public boolean isPauseScreen() { return false; }

  @Override
  public void renderBackground(GuiGraphics graphics, int x, int y, float partial) {}

  @Override
  public void render(GuiGraphics g, int x, int y, float partial) {
    beginFit(g);
    g.fill(left, top, left + ScreenMetrics.PANEL_WIDTH, top + ScreenMetrics.PANEL_HEIGHT, 0xFF0D1D2B);
    g.fill(left, top, left + ScreenMetrics.PANEL_WIDTH, top + 2, 0xFF53BBCB);
    drawLabel(g, getTitle().getString(), left + ScreenMetrics.CONTENT_INSET,
        top + ScreenMetrics.CONTENT_INSET, ScreenMetrics.CONTENT_WIDTH, 0xFFE0F3FF);
    drawLabel(g, UiText.text("screen.fieldemitters.control.specific_mob_player_uuid"),
        left + ScreenMetrics.CONTENT_INSET, top + FIRST_FIELD_Y - ScreenMetrics.CONTENT_INSET,
        ScreenMetrics.CONTENT_WIDTH, 0xFFADBED0);
    drawLabel(g, UiText.text("screen.fieldemitters.control.custom_entity_label_tag"),
        left + ScreenMetrics.CONTENT_INSET, top + FIRST_FIELD_Y + FIELD_STEP - ScreenMetrics.CONTENT_INSET,
        ScreenMetrics.CONTENT_WIDTH, 0xFFADBED0);
    drawParagraph(g, Component.literal(UiText.text(filter.categoryLists ? "screen.fieldemitters.targets.details_help" : "screen.fieldemitters.pages.entity_details_help")),
        left + ScreenMetrics.CONTENT_INSET, top + HELP_Y, ScreenMetrics.CONTENT_WIDTH,
        HELP_HEIGHT, 0xFFADBED0);
    drawLabel(g, status, left + ScreenMetrics.CONTENT_INSET,
        top + HELP_Y + HELP_HEIGHT + ScreenMetrics.COLUMN_GAP, ScreenMetrics.CONTENT_WIDTH, 0xFF8DE0CF);
    super.render(g, fitMouse(x), fitMouse(y), partial);
    g.pose().popPose();
  }
}
