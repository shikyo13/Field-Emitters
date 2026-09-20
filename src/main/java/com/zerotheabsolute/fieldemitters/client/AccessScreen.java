package com.zerotheabsolute.fieldemitters.client;

import com.zerotheabsolute.fieldemitters.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import com.zerotheabsolute.fieldemitters.network.NativeNetwork;

/** Credential issuance is separate from the field's list of accepted groups. */
final class AccessScreen extends FittedScreen {
  private final Screen parent;
  private final BlockPos pos;
  private final EntityFilter filter;
  private final Runnable apply;
  private int left, top;
  private String groups,
      group = "staff",
      player = "",
      notice =
          UiText.text("screen.fieldemitters.access.changes_to_accepted_groups_apply_automatically");
  private long due;

  AccessScreen(Screen parent, BlockPos pos, EntityFilter filter, Runnable apply) {
    super(Component.literal(UiText.text("screen.fieldemitters.access.access_badges")));
    this.parent = parent;
    this.pos = pos;
    this.filter = filter;
    this.apply = apply;
    groups = filter == null ? "" : String.join(", ", filter.accessGroups);
  }

  private void button(String text, int y, Runnable run) {
    addRenderableWidget(
        new FieldButton(
            left + 12,
            top + y,
            ScreenMetrics.CONTENT_WIDTH,
            18,
            Component.literal(text),
            b -> run.run(),
            false));
  }

  private void input(
      String label, String value, int y, int max, java.util.function.Consumer<String> change) {
    var w =
        addRenderableWidget(
            new EditBox(
                font,
                left + 12,
                top + y,
                ScreenMetrics.CONTENT_WIDTH,
                18,
                Component.literal(label)));
    w.setMaxLength(max);
    w.setValue(value);
    w.setResponder(change);
    w.setTooltip(Tooltip.create(Component.literal(label)));
  }

  protected void init() {
    fit(ScreenMetrics.PANEL_WIDTH, ScreenMetrics.PANEL_HEIGHT);
    left = (width - ScreenMetrics.PANEL_WIDTH) / 2;
    top = (height - ScreenMetrics.PANEL_HEIGHT) / 2;
    if (filter != null)
      input(
          UiText.text(
              "screen.fieldemitters.access.accepted_groups_separated_by_commas_uses_the_player"),
          groups,
          48,
          1024,
          v -> {
            groups = v;
            due = net.minecraft.Util.getMillis() + ScreenMetrics.TEXT_DEBOUNCE_MILLIS;
          });
    input(
        UiText.text(
            "screen.fieldemitters.access.group_to_issue_or_revoke_lowercase_letters_numbers"),
        group,
        111,
        32,
        v -> group = v);
    input(
        UiText.text(
            "screen.fieldemitters.access.optional_player_uuid_blank_means_anyone_carrying_the"),
        player,
        150,
        36,
        v -> player = v);
    button(
        UiText.text("screen.fieldemitters.access.issue_badge_held_in_either_hand"),
        176,
        () -> send(0));
    button(
        UiText.text("screen.fieldemitters.access.revoke_my_badges_in_this_group"),
        198,
        () -> send(1));
    button(UiText.text("screen.fieldemitters.access.back"), 280, this::onClose);
  }

  private void send(int action) {
    String name = BadgeAccess.group(group);
    if (!BadgeAccess.validGroup(name)) {
      notice = UiText.text("screen.fieldemitters.access.enter_a_valid_group_name_first");
      return;
    }
    NativeNetwork.sendToServer(new AccessPackets.Request(pos, name, player.strip(), action));
    notice = UiText.text("screen.fieldemitters.access.waiting_for_server");
  }

  static void receive(AccessPackets.Result result) {
    if (net.minecraft.client.Minecraft.getInstance().screen instanceof AccessScreen screen
        && screen.pos.equals(result.pos())) screen.notice = result.message().getString();
  }

  private void save() {
    due = 0;
    if (filter == null) return;
    var next = new java.util.LinkedHashSet<String>();
    for (var s : groups.split(",")) {
      if (s.isBlank()) continue;
      var name = BadgeAccess.group(s);
      if (!BadgeAccess.validGroup(name)) {
        notice =
            UiText.text("screen.fieldemitters.access.invalid_group_name_previous_groups_retained");
        return;
      }
      next.add(name);
    }
    if (next.size() > 64) {
      notice = UiText.text("screen.fieldemitters.access.maximum_64_groups");
      return;
    }
    filter.accessGroups.clear();
    filter.accessGroups.addAll(next);
    apply.run();
    notice =
        UiText.text(
            "screen.fieldemitters.access.accepted_groups_updated_enable_a_player_list_mode");
  }

  public void tick() {
    if (due > 0 && net.minecraft.Util.getMillis() >= due) save();
  }

  public void onClose() {
    if (due > 0) save();
    minecraft.setScreen(parent);
  }

  public boolean isPauseScreen() {
    return false;
  }

  public void renderBackground(GuiGraphics g, int x, int y, float p) {}

  public void render(GuiGraphics g, int x, int y, float p) {
    beginFit(g);
    g.fill(
        left, top, left + ScreenMetrics.PANEL_WIDTH, top + ScreenMetrics.PANEL_HEIGHT, 0xFF0D1D2B);
    g.fill(left, top, left + ScreenMetrics.PANEL_WIDTH, top + 2, 0xFF53BBCB);
    drawLabel(g, UiText.text("screen.fieldemitters.access.access_badges"), left + 12, top + 12, ScreenMetrics.CONTENT_WIDTH, 0xFFE0F3FF);
    drawLabel(g, filter == null
            ? UiText.text("screen.fieldemitters.access.issue_badges_for_fields_you_own")
            : UiText.text("screen.fieldemitters.access.accepted_groups_player_list_or_badge_group"), left + 12, top + 34, ScreenMetrics.CONTENT_WIDTH, 0xFFADBED0);
    drawLabel(g, UiText.text("screen.fieldemitters.access.badge_group"), left + 12, top + 98, ScreenMetrics.CONTENT_WIDTH, 0xFFADBED0);
    drawLabel(g, UiText.text("screen.fieldemitters.access.bind_to_player_uuid_optional"), left + 12, top + 137, ScreenMetrics.CONTENT_WIDTH, 0xFFADBED0);
    drawParagraph(g, Component.literal(
            UiText.text("screen.fieldemitters.access.a_badge_holder_stores_16_badges_right_click")), left + 12, top + 224, ScreenMetrics.CONTENT_WIDTH, 39, 0xFFADBED0);
    drawLabel(g, notice, left + 12, top + 267, ScreenMetrics.CONTENT_WIDTH, 0xFFADBED0);
    super.render(g, fitMouse(x), fitMouse(y), p);
    g.pose().popPose();
  }
}
