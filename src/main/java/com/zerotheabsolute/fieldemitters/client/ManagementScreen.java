package com.zerotheabsolute.fieldemitters.client;

import com.zerotheabsolute.fieldemitters.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import com.zerotheabsolute.fieldemitters.network.NativeNetwork;

final class ManagementScreen extends FittedScreen {
  private final ControlScreen parent;
  private final BlockPos pos;
  private int left, top, page, pending = -1;
  private boolean owner, publicManagement;
  private final List<UUID> managers = new ArrayList<>();
  private String query = "",
      notice = UiText.text("screen.fieldemitters.management.loading_permissions");
  private long lookupStarted;

  ManagementScreen(ControlScreen parent, BlockPos pos) {
    super(Component.literal(UiText.text("screen.fieldemitters.management.network_management")));
    this.parent = parent;
    this.pos = pos;
    request(-1, "");
  }

  private void request(int action, String id) {
    NativeNetwork.sendToServer(new ManagementPackets.Request(pos, action, id));
  }

  private FieldButton button(String text, int x, int y, int w, Runnable run) {
    return addRenderableWidget(
        new FieldButton(
            left + x,
            top + y,
            w,
            ScreenMetrics.BUTTON_HEIGHT,
            Component.literal(text),
            b -> run.run(),
            false));
  }

  protected void init() {
    fit(ScreenMetrics.PANEL_WIDTH, ScreenMetrics.PANEL_HEIGHT);
    left = (width - ScreenMetrics.PANEL_WIDTH) / 2;
    top = (height - ScreenMetrics.PANEL_HEIGHT) / 2;
    button(
                UiText.text(
                    "screen.fieldemitters.management.management",
                    (publicManagement
                        ? UiText.text("screen.fieldemitters.management.public_anyone_can_configure")
                        : UiText.text(
                            "screen.fieldemitters.management.private_owner_and_invited_managers"))),
                12,
                34,
                ScreenMetrics.CONTENT_WIDTH,
                () -> request(publicManagement ? 0 : 1, ""))
            .active =
        owner;
    var input =
        addRenderableWidget(
            new EditBox(
                font,
                left + 12,
                top + 100,
                278,
                18,
                Component.literal(
                    UiText.text(
                        "screen.fieldemitters.management.minecraft_account_name_or_uuid"))));
    input.setMaxLength(ScreenMetrics.UUID_LENGTH);
    input.setValue(query);
    input.setResponder(v -> query = v);
    input.active = owner;
    input.setTooltip(
        Tooltip.create(
            Component.literal(
                UiText.text(
                    "screen.fieldemitters.management.minecraft_account_name_or_uuid_server_nicknames_are"))));
    button(
                pending < 0
                    ? UiText.text("screen.fieldemitters.management.add_manager")
                    : UiText.text("screen.fieldemitters.management.looking_up"),
                296,
                100,
                96,
                () -> {
                  if (query.isBlank()) {
                    notice =
                        UiText.text(
                            "screen.fieldemitters.management.enter_a_minecraft_account_name_or_uuid");
                    return;
                  }
                  pending = PlayerLookupSequence.next();
                  lookupStarted = net.minecraft.Util.getMillis();
                  NativeNetwork.sendToServer(
                      new PlayerLookup.Request(pos, pending, query.strip()));
                  notice = UiText.text("screen.fieldemitters.management.resolving_player");
                  rebuildWidgets();
                })
            .active =
        owner && pending < 0 && managers.size() < ManagementAccess.MAX_MANAGERS;
    int pages =
        Math.max(1, (managers.size() + ScreenMetrics.PLAYER_ROWS - 1) / ScreenMetrics.PLAYER_ROWS);
    page = Math.min(page, pages - 1);
    for (int i = 0;
        i < ScreenMetrics.PLAYER_ROWS && page * ScreenMetrics.PLAYER_ROWS + i < managers.size();
        i++) {
      var id = managers.get(page * ScreenMetrics.PLAYER_ROWS + i);
      var info = minecraft.getConnection().getPlayerInfo(id);
      String name = info == null ? id.toString() : info.getProfile().getName();
      button(font.plainSubstrByWidth(name, 280), 12, 130 + i * 20, 292, () -> {})
          .setTooltip(Tooltip.create(Component.literal(id.toString())));
      button(
                  UiText.text("screen.fieldemitters.management.remove"),
                  310,
                  130 + i * 20,
                  82,
                  () -> request(3, id.toString()))
              .active =
          owner;
    }
    button(
                UiText.text("screen.fieldemitters.management.previous"),
                12,
                254,
                90,
                () -> {
                  page--;
                  rebuildWidgets();
                })
            .active =
        page > 0;
    button(
                UiText.text("screen.fieldemitters.management.next"),
                110,
                254,
                90,
                () -> {
                  page++;
                  rebuildWidgets();
                })
            .active =
        page + 1 < pages;
    button(UiText.text("screen.fieldemitters.access.back"), 310, 280, 82, this::onClose);
  }

  static void receive(ManagementPackets.State state) {
    if (Minecraft.getInstance().screen instanceof ManagementScreen s && s.pos.equals(state.pos())) {
      s.owner = state.owner();
      s.publicManagement = state.data().getBoolean("Public");
      s.managers.clear();
      for (var v : state.data().getList("Managers", 8))
        try {
          s.managers.add(UUID.fromString(v.getAsString()));
        } catch (IllegalArgumentException ignored) {
        }
      s.notice =
          s.owner
              ? UiText.text(
                  "screen.fieldemitters.management.only_you_and_server_operators_can_change_these")
              : UiText.text(
                  "screen.fieldemitters.management.only_the_owner_can_change_management_permissions");
      s.rebuildWidgets();
    }
  }

  static void lookup(PlayerLookup.Result r) {
    if (Minecraft.getInstance().screen instanceof ManagementScreen s
        && s.pending == r.requestId()) {
      s.pending = -1;
      if (!r.error().getString().isEmpty()) {
        s.notice = r.error().getString();
        s.rebuildWidgets();
        return;
      }
      s.request(2, r.uuid());
      s.query = "";
    }
  }

  public void tick() {
    if (!parent.canConfigure()) {
      minecraft.setScreen(null);
      return;
    }
    if (pending >= 0
        && net.minecraft.Util.getMillis() - lookupStarted > ScreenMetrics.LOOKUP_TIMEOUT_MILLIS) {
      pending = -1;
      notice = UiText.text("screen.fieldemitters.management.lookup_timed_out_try_again_or_enter_a");
      rebuildWidgets();
    }
  }

  public boolean isPauseScreen() {
    return false;
  }

  public void onClose() {
    minecraft.setScreen(parent);
  }

  public void renderBackground(GuiGraphics g) {}

  public void render(GuiGraphics g, int x, int y, float p) {
    beginFit(g);
    g.fill(
        left, top, left + ScreenMetrics.PANEL_WIDTH, top + ScreenMetrics.PANEL_HEIGHT, 0xFF0D1D2B);
    g.fill(left, top, left + ScreenMetrics.PANEL_WIDTH, top + 2, 0xFF53BBCB);
    g.drawString(
        font,
        UiText.text("screen.fieldemitters.management.network_management"),
        left + 12,
        top + 12,
        0xFFE0F3FF,
        false);
    g.drawWordWrap(
        font,
        Component.literal(
            UiText.text(
                "screen.fieldemitters.management.these_permissions_control_field_settings_not_passage_badges")),
        left + 12,
        top + 59,
        ScreenMetrics.CONTENT_WIDTH,
        0xFFADBED0);
    g.drawString(
        font,
        UiText.text("screen.fieldemitters.management.minecraft_account_name_or_uuid"),
        left + 12,
        top + 87,
        0xFFADBED0,
        false);
    if (managers.isEmpty())
      g.drawString(
          font,
          UiText.text("screen.fieldemitters.management.no_additional_managers"),
          left + 12,
          top + 140,
          0xFFADBED0,
          false);
    g.drawWordWrap(font, Component.literal(notice), left + 12, top + 280, 288, 0xFFADBED0);
    super.render(g, fitMouse(x), fitMouse(y), p);
    g.pose().popPose();
  }
}
