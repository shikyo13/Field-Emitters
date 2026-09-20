package com.zerotheabsolute.fieldemitters.client;

import com.zerotheabsolute.fieldemitters.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import com.zerotheabsolute.fieldemitters.network.NativeNetwork;

/** Server-backed field directory, then the emitters in a selected field. */
public final class RemoteScreen extends FittedScreen {
  private static final int ROWS_PER_PAGE = 4, ROW_HEIGHT = 30;
  private final CompoundTag data;
  private CompoundTag selected;
  private int page, left, top;
  private String status;
  private long renameDue;
  private EditBox nameBox;

  public RemoteScreen(CompoundTag data, String status) {
    super(Component.literal(UiText.text("screen.fieldemitters.remote.field_manager")));
    this.data = data;
    this.status = status;
  }

  public static void openManager() {
    NativeNetwork.sendToServer(new FieldControls.RemoteRequest(true, BlockPos.ZERO));
  }

  public static void receive(FieldControls.RemoteData reply) {
    var mc = Minecraft.getInstance();
    if (mc.level == null) return;
    if (reply.kind() == ControlEdits.REPLY_KIND) { ControlEditSession.receive(reply); return; }
    if (reply.kind() == 0)
      mc.setScreen(new RemoteScreen(reply.data(), reply.message().getString()));
    else if (reply.kind() == 1) {
      var tag = reply.data();
      var pos = BlockPos.of(tag.getLong("Pos"));
      var state =
          net.minecraft.nbt.NbtUtils.readBlockState(
              mc.level.holderLookup(net.minecraft.core.registries.Registries.BLOCK),
              tag.getCompound("State"));
      var emitter = new EmitterEntity(pos, state);
      emitter.load(tag.getCompound("Emitter"));
      emitter.setLevel(mc.level);
      mc.setScreen(new ControlScreen(emitter));
    } else if (mc.screen instanceof ControlScreen screen) screen.acknowledge(reply.message());
    else if (mc.screen instanceof RemoteScreen screen) screen.status = reply.message().getString();
    else if (mc.player != null) mc.player.displayClientMessage(reply.message(), true);
  }

  private void rename() {
    if (renameDue == 0 || selected == null || nameBox == null) return;
    renameDue = 0;
    String name = nameBox.getValue().strip();
    selected.putString("Name", name);
    NativeNetwork.sendToServer(
        new FieldControls.Rename(BlockPos.of(selected.getLong("Pos")), name));
  }

  @Override
  public void tick() {
    if (renameDue != 0 && System.currentTimeMillis() >= renameDue) rename();
  }

  @Override
  public void onClose() {
    rename();
    super.onClose();
  }

  private static String fieldName(CompoundTag field) {
    String name = field.getString("Name");
    return name.isBlank()
        ? UiText.text(
            "screen.fieldemitters.remote.default_name",
            BlockPos.of(field.getLong(field.contains("Origin") ? "Origin" : "Pos")).toShortString())
        : name;
  }

  @Override
  protected void init() {
    fit(404, 224);
    left = (width - 404) / 2;
    top = (height - 224) / 2;
    var entries = selected == null ? data.getList("Entries", 10) : selected.getList("Members", 10);
    int pages = Math.max(1, (entries.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
    page = Math.min(page, pages - 1);
    if (selected != null) {
      nameBox =
          new EditBox(
              font,
              left + 48,
              top + 39,
              344,
              18,
              Component.literal(UiText.text("screen.fieldemitters.remote.field_name")));
      nameBox.setMaxLength(32);
      nameBox.setValue(selected.getString("Name"));
      nameBox.setTooltip(
          Tooltip.create(
              Component.literal(
                  UiText.text(
                      "screen.fieldemitters.remote.name_this_entire_connected_field_changes_apply_automatically"))));
      nameBox.setResponder(
          value -> {
            selected.putString("Name", value);
            renameDue = System.currentTimeMillis() + ScreenMetrics.TEXT_DEBOUNCE_MILLIS;
          });
      addRenderableWidget(nameBox);
    }
    for (int i = page * ROWS_PER_PAGE;
        i < Math.min(entries.size(), (page + 1) * ROWS_PER_PAGE);
        i++) {
      var e = entries.getCompound(i);
      var pos = BlockPos.of(e.getLong("Pos"));
      String label =
          selected == null
              ? fieldName(e)
              : UiText.text(
                  "screen.fieldemitters.remote.at",
                  (e.getBoolean("Rail")
                      ? UiText.text("screen.fieldemitters.remote.surface_rail")
                      : UiText.text("block.fieldemitters.field_emitter")),
                  pos.toShortString());
      String detail =
          selected == null
              ? UiText.text(
                  "screen.fieldemitters.remote.emitters_running",
                  e.getList("Members", 10).size(),
                  e.getInt("Running"),
                  BlockPos.of(e.getLong(e.contains("Origin") ? "Origin" : "Pos")).toShortString())
              : e.getBoolean("Powered")
                  ? UiText.text("screen.fieldemitters.control.field_running")
                  : UiText.text("screen.fieldemitters.remote.idle_open_to_view_power_and_settings");
      var button =
          addRenderableWidget(
              new FieldButton(
                      left + 12,
                      top + 65 + (i % ROWS_PER_PAGE) * ROW_HEIGHT,
                      ScreenMetrics.CONTENT_WIDTH,
                      ROW_HEIGHT - 2,
                      Component.literal(label),
                      b -> {
                        if (selected == null) {
                          selected = e;
                          page = 0;
                          status =
                              UiText.text(
                                  "screen.fieldemitters.remote.edit_the_name_or_open_an_emitter_below");
                          rebuildWidgets();
                        } else {
                          rename();
                          NativeNetwork.sendToServer(
                              new FieldControls.RemoteRequest(false, pos));
                        }
                      },
                      false)
                  .detail(detail));
      button.setTooltip(
          Tooltip.create(
              Component.literal(
                  selected == null
                      ? UiText.text(
                          "screen.fieldemitters.remote.anchor_includes_connected_loaded_emitters_even_when_switched",
                          pos.toShortString())
                      : UiText.text(
                          "screen.fieldemitters.remote.open_this_emitter_s_controls_connections_selects_one"))));
    }
    var prev =
        addRenderableWidget(
            new FieldButton(
                left + 12,
                top + 201,
                65,
                18,
                Component.literal(UiText.text("screen.fieldemitters.management.previous")),
                b -> {
                  rename();
                  page--;
                  rebuildWidgets();
                },
                false));
    prev.active = page > 0;
    var next =
        addRenderableWidget(
            new FieldButton(
                left + 82,
                top + 201,
                48,
                18,
                Component.literal(UiText.text("screen.fieldemitters.management.next")),
                b -> {
                  rename();
                  page++;
                  rebuildWidgets();
                },
                false));
    next.active = page + 1 < pages;
    addRenderableWidget(
        new FieldButton(
            left + 242,
            top + 201,
            72,
            18,
            Component.literal(
                selected == null
                    ? UiText.text("screen.fieldemitters.remote.refresh")
                    : UiText.text("screen.fieldemitters.remote.all_fields")),
            b -> {
              rename();
              openManager();
            },
            false));
    addRenderableWidget(
        new FieldButton(
            left + 320,
            top + 201,
            72,
            18,
            Component.literal(UiText.text("screen.fieldemitters.control.close")),
            b -> onClose(),
            false));
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void renderBackground(GuiGraphics g) {}

  @Override
  public void render(GuiGraphics g, int x, int y, float p) {
    beginFit(g);
    x = fitMouse(x);
    y = fitMouse(y);
    g.fill(0, 0, width, height, 0xB0101723);
    g.fill(left, top, left + 404, top + 224, 0xFF111D2C);
    g.fill(left + 1, top + 2, left + 403, top + 20, 0xFF1B2D3E);
    g.fill(left + 8, top + 40, left + 396, top + 196, 0xFF0E1926);
    g.fill(left + 8, top + 198, left + 396, top + 199, 0xFF354D63);
    g.fill(left, top, left + 404, top + 2, 0xFF52E5FF);
    g.drawString(font, title, left + 12, top + 10, 0xDBF8FF, false);
    g.drawString(
        font,
        selected == null
            ? UiText.text("screen.fieldemitters.remote.loaded_in", data.getString("Dimension"))
            : UiText.text(
                "screen.fieldemitters.remote.field_location",
                BlockPos.of(selected.getLong(selected.contains("Origin") ? "Origin" : "Pos"))
                    .toShortString()),
        left + 12,
        top + 26,
        0x92A9BE,
        false);
    g.drawString(
        font,
        selected == null
            ? UiText.text("screen.fieldemitters.remote.choose_a_field_to_rename_it_or_manage")
            : UiText.text("screen.fieldemitters.remote.name"),
        left + 12,
        top + 43,
        0x92A9BE,
        false);
    if (selected == null && data.getList("Entries", 10).isEmpty())
      g.drawString(
          font,
          UiText.text("screen.fieldemitters.remote.no_loaded_fields_available_to_you_here"),
          left + 12,
          top + 90,
          0xDBF8FF,
          false);
    g.drawString(
        font,
        font.plainSubstrByWidth(status, ScreenMetrics.CONTENT_WIDTH),
        left + 12,
        top + 188,
        0x92A9BE,
        false);
    g.drawString(
        font,
        UiText.text("screen.fieldemitters.typelist.page", (page + 1)),
        left + 153,
        top + 206,
        0x92A9BE,
        false);
    super.render(g, x, y, p);
    g.pose().popPose();
  }
}
