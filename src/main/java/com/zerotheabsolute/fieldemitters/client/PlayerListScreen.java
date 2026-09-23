package com.zerotheabsolute.fieldemitters.client;

import com.zeromods.core.client.FittedScreen;

import com.zerotheabsolute.fieldemitters.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import com.zerotheabsolute.fieldemitters.network.ForgePacketDistributor;

final class PlayerListScreen extends FittedScreen {
  private final net.minecraft.client.gui.screens.Screen parent;
  private final BlockPos pos;
  private final EntityFilter filter;
  private final FilterPurpose purpose;
  private final Runnable apply;
  private int left, top, page, pending = -1;
  private long started;
  private String query = "",
      status = UiText.text("screen.fieldemitters.playerlist.changes_apply_immediately");
  private EditBox input;

  PlayerListScreen(
      net.minecraft.client.gui.screens.Screen parent,
      BlockPos pos,
      EntityFilter filter,
      FilterPurpose purpose,
      Runnable apply) {
    super(Component.literal(UiText.text("screen.fieldemitters.playerlist.player_list_2")));
    this.parent = parent;
    this.pos = pos;
    this.filter = filter;
    this.purpose = purpose;
    this.apply = apply;
  }

  private String mode() {
    if (filter.playerMode == 0)
      return UiText.text("screen.fieldemitters.playerlist.players_use_the_general_filter");
    String key =
        switch (purpose) {
          case BLOCKING -> filter.playerMode == 1 ? "block_listed" : "allow_listed";
          case SENSOR -> filter.playerMode == 1 ? "detect_listed" : "detect_unlisted";
          case CHECKPOINT -> filter.playerMode == 1 ? "inspect_listed" : "inspect_unlisted";
          default -> filter.playerMode == 1 ? "damage_listed" : "damage_unlisted";
        };
    return UiText.text("screen.fieldemitters.playerlist.mode." + key);
  }

  protected void init() {
    fit(ScreenMetrics.PANEL_WIDTH, ScreenMetrics.PANEL_HEIGHT);
    left = (width - ScreenMetrics.PANEL_WIDTH) / 2;
    top = (height - ScreenMetrics.PANEL_HEIGHT) / 2;
    button(
        mode(),
        12,
        32,
        ScreenMetrics.CONTENT_WIDTH,
        () -> {
          filter.playerMode = (filter.playerMode + 1) % 3;
          status = UiText.text("screen.fieldemitters.playerlist.player_list_mode_updated");
          changed();
        });
    button(
        UiText.text("screen.fieldemitters.playerlist.access_groups_badges"),
        12,
        80,
        ScreenMetrics.CONTENT_WIDTH,
        () -> minecraft.setScreen(new AccessScreen(this, pos, filter, apply)));
    input =
        addRenderableWidget(
            new FieldEditBox(
                font,
                left + 12,
                top + 110,
                214,
                18,
                Component.literal(
                    UiText.text(
                        "screen.fieldemitters.management.minecraft_account_name_or_uuid"))));
    input.setMaxLength(ScreenMetrics.UUID_LENGTH);
    input.setValue(query);
    input.setResponder(s -> query = s);
    input.setTooltip(
        Tooltip.create(
            Component.literal(
                UiText.text(
                    "screen.fieldemitters.playerlist.use_a_minecraft_account_name_not_a_server"))));
    var add =
        button(
            pending < 0
                ? UiText.text("screen.fieldemitters.playerlist.add")
                : UiText.text("screen.fieldemitters.management.looking_up"),
            232,
            110,
            78,
            this::lookup);
    add.active = pending < 0 && filter.playerList.size() < EntityFilter.MAX_PLAYERS;
    var self =
        button(
            UiText.text("screen.fieldemitters.playerlist.add_myself"),
            316,
            110,
            76,
            () -> {
              var p = minecraft.player;
              add(p.getUUID(), p.getGameProfile().getName());
            });
    self.active = filter.playerList.size() < EntityFilter.MAX_PLAYERS;
    var entries = new ArrayList<>(filter.playerList.entrySet());
    int pages = Math.max(1, (entries.size() + 4) / 5);
    page = Math.min(page, pages - 1);
    for (int i = 0; i < 5 && page * 5 + i < entries.size(); i++) {
      var entry = entries.get(page * 5 + i);
      int y = 145 + i * 18;
      var row =
          button(
              entry.getValue().isEmpty() ? entry.getKey().toString() : entry.getValue(),
              12,
              y,
              304,
              () -> {});
      row.setTooltip(Tooltip.create(Component.literal(entry.getKey().toString())));
      button(
          UiText.text("screen.fieldemitters.management.remove"),
          322,
          y,
          70,
          () -> {
            filter.playerList.remove(entry.getKey());
            status = UiText.text("screen.fieldemitters.playerlist.player_removed");
            changed();
          });
    }
    var prev =
        button(
            UiText.text("screen.fieldemitters.management.previous"),
            12,
            241,
            84,
            () -> {
              page--;
              rebuildWidgets();
            });
    prev.active = page > 0;
    var next =
        button(
            UiText.text("screen.fieldemitters.management.next"),
            308,
            241,
            84,
            () -> {
              page++;
              rebuildWidgets();
            });
    next.active = page + 1 < pages;
    button(UiText.text("screen.fieldemitters.access.back"), 308, 281, 84, this::onClose);
  }

  private FieldButton button(String text, int x, int y, int w, Runnable action) {
    return addRenderableWidget(
        new FieldButton(
            left + x,
            top + y,
            w,
            ScreenMetrics.BUTTON_HEIGHT,
            Component.literal(text),
            b -> action.run(),
            false));
  }

  private void changed() {
    apply.run();
    rebuildWidgets();
  }

  private void lookup() {
    if (query.isBlank()) {
      status =
          UiText.text("screen.fieldemitters.management.enter_a_minecraft_account_name_or_uuid");
      return;
    }
    pending = PlayerLookupSequence.next();
    started = net.minecraft.Util.getMillis();
    status = UiText.text("screen.fieldemitters.management.resolving_player");
    ForgePacketDistributor.sendToServer(new PlayerLookup.Request(pos, pending, query.trim()));
    rebuildWidgets();
  }

  private void add(UUID id, String name) {
    if (!filter.playerList.containsKey(id)
        && filter.playerList.size() >= EntityFilter.MAX_PLAYERS) {
      status = UiText.text("screen.fieldemitters.playerlist.the_list_is_full_64_players");
      return;
    }
    boolean existed = filter.playerList.containsKey(id);
    filter.playerList.put(id, name);
    query = "";
    status =
        existed
            ? UiText.text("screen.fieldemitters.playerlist.player_already_listed_name_refreshed")
            : UiText.text(
                "screen.fieldemitters.playerlist.player_added_choose_the_list_mode_above_to");
    page = (filter.playerList.size() - 1) / 5;
    changed();
  }

  public static void receive(PlayerLookup.Result result) {
    if (!(Minecraft.getInstance().screen instanceof PlayerListScreen screen)
        || screen.pending != result.requestId()) return;
    screen.pending = -1;
    if (!result.error().getString().isEmpty()) {
      screen.status = result.error().getString();
      screen.rebuildWidgets();
      return;
    }
    try {
      screen.add(UUID.fromString(result.uuid()), result.name());
    } catch (IllegalArgumentException e) {
      screen.status =
          UiText.text("screen.fieldemitters.playerlist.the_lookup_returned_an_invalid_uuid");
      screen.rebuildWidgets();
    }
  }

  public void tick() {
    super.tick();
    if (pending >= 0
        && net.minecraft.Util.getMillis() - started > ScreenMetrics.LOOKUP_TIMEOUT_MILLIS) {
      pending = -1;
      status = UiText.text("screen.fieldemitters.management.lookup_timed_out_try_again_or_enter_a");
      rebuildWidgets();
    }
  }

  public boolean isPauseScreen() {
    return false;
  }

  public void onClose() {
    minecraft.setScreen(parent);
  }

  public void renderBackground(GuiGraphics g, int x, int y, float partial) {}

  public void render(GuiGraphics g, int mx, int my, float partial) {
    beginFit(g);
    g.fill(
        left, top, left + ScreenMetrics.PANEL_WIDTH, top + ScreenMetrics.PANEL_HEIGHT, 0xFF0D1D2B);
    g.fill(left, top, left + ScreenMetrics.PANEL_WIDTH, top + 2, 0xFF53BBCB);
    drawLabel(g, UiText.text("screen.fieldemitters.playerlist.player_list", purpose.title()), left + 12, top + 12, ScreenMetrics.CONTENT_WIDTH, 0xFFE0F3FF);
    drawParagraph(g, Component.literal(
            UiText.text(
                "screen.fieldemitters.playerlist.list_modes_replace_the_general_filter_for_players")), left + 12, top + 58, ScreenMetrics.CONTENT_WIDTH, 36, 0xFFADBED0);
    drawLabel(g, UiText.text("screen.fieldemitters.management.minecraft_account_name_or_uuid"), left + 12, top + 98, ScreenMetrics.CONTENT_WIDTH, 0xFFADBED0);
    drawLabel(g, UiText.text("screen.fieldemitters.playerlist.saved_players"), left + 12, top + 133, ScreenMetrics.CONTENT_WIDTH, 0xFFADBED0);
    if (filter.playerList.isEmpty())
      drawLabel(g, UiText.text("screen.fieldemitters.playerlist.no_players_added_yet"), left + 12, top + 153, ScreenMetrics.CONTENT_WIDTH, 0xFFADBED0);
    g.drawCenteredString(
        font,
        UiText.text(
            "screen.fieldemitters.playerlist.page",
            (page + 1),
            Math.max(1, (filter.playerList.size() + 4) / 5)),
        left + 202,
        top + 246,
        0xFFADBED0);
    drawParagraph(g, Component.literal(status), left + 12, top + 265, 280, 36, 0xFFADBED0);
    super.render(g, fitMouse(mx), fitMouse(my), partial);
    g.pose().popPose();
  }
}
