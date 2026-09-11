package com.zerotheabsolute.fieldemitters.client;

import com.zerotheabsolute.fieldemitters.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-backed field directory, then the emitters in a selected field. */
public final class RemoteScreen extends FittedScreen {
  private final CompoundTag data;
  private CompoundTag selected;
  private int page, left, top;
  private String status;
  private long renameDue;
  private EditBox nameBox;

  public RemoteScreen(CompoundTag data, String status) {
    super(Component.literal("FIELD MANAGER"));
    this.data = data;
    this.status = status;
  }

  public static void openManager() {
    PacketDistributor.sendToServer(new FieldControls.RemoteRequest(true, BlockPos.ZERO));
  }

  public static void receive(FieldControls.RemoteData reply) {
    var mc = Minecraft.getInstance();
    if (mc.level == null) return;
    if (reply.kind() == 0) mc.setScreen(new RemoteScreen(reply.data(), reply.message()));
    else if (reply.kind() == 1) {
      var tag = reply.data();
      var pos = BlockPos.of(tag.getLong("Pos"));
      var state =
          net.minecraft.nbt.NbtUtils.readBlockState(
              mc.level.holderLookup(net.minecraft.core.registries.Registries.BLOCK),
              tag.getCompound("State"));
      var emitter = new EmitterEntity(pos, state);
      emitter.loadWithComponents(tag.getCompound("Emitter"), mc.level.registryAccess());
      emitter.setLevel(mc.level);
      mc.setScreen(new ControlScreen(emitter));
    } else if (mc.screen instanceof ControlScreen screen) screen.acknowledge(reply.message());
    else if (mc.screen instanceof RemoteScreen screen) screen.status = reply.message();
    else if (mc.player != null)
      mc.player.displayClientMessage(Component.literal(reply.message()), true);
  }

  private void rename() {
    if (renameDue == 0 || selected == null || nameBox == null) return;
    renameDue = 0;
    String name = nameBox.getValue().strip();
    selected.putString("Name", name);
    PacketDistributor.sendToServer(
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

  @Override
  protected void init() {
    fit(404, 224);
    left = (width - 404) / 2;
    top = (height - 224) / 2;
    var entries = selected == null ? data.getList("Entries", 10) : selected.getList("Members", 10);
    int pages = Math.max(1, (entries.size() + 4) / 5);
    page = Math.min(page, pages - 1);
    if (selected != null) {
      nameBox = new EditBox(font, left + 48, top + 39, 344, 18, Component.literal("Field name"));
      nameBox.setMaxLength(32);
      nameBox.setValue(selected.getString("Name"));
      nameBox.setTooltip(
          Tooltip.create(
              Component.literal(
                  "Name this entire connected field. Changes apply automatically. Blank restores"
                      + " its location-based name. The location uses the oldest loaded emitter;"
                      + " existing demo emitters use a coordinate fallback.")));
      nameBox.setResponder(
          value -> {
            selected.putString("Name", value);
            renameDue = System.currentTimeMillis() + 350;
          });
      addRenderableWidget(nameBox);
    }
    for (int i = page * 5; i < Math.min(entries.size(), page * 5 + 5); i++) {
      var e = entries.getCompound(i);
      var pos = BlockPos.of(e.getLong("Pos"));
      String label =
          selected == null
              ? e.getString("Name")
                  + " • "
                  + e.getList("Members", 10).size()
                  + " emitters • "
                  + e.getInt("Running")
                  + " running"
              : (e.getBoolean("Rail") ? "Surface rail" : "Emitter")
                  + " at "
                  + pos.toShortString()
                  + " • "
                  + (e.getBoolean("Powered") ? "Running" : "Idle");
      var button =
          addRenderableWidget(
              Button.builder(
                      Component.literal(label),
                      b -> {
                        if (selected == null) {
                          selected = e;
                          page = 0;
                          status = "Edit the name, or open an emitter below.";
                          rebuildWidgets();
                        } else {
                          rename();
                          PacketDistributor.sendToServer(
                              new FieldControls.RemoteRequest(false, pos));
                        }
                      })
                  .bounds(left + 12, top + 65 + (i % 5) * 22, 380, 20)
                  .build());
      button.setTooltip(
          Tooltip.create(
              Component.literal(
                  selected == null
                      ? "Anchor: "
                          + pos.toShortString()
                          + ". Includes connected loaded emitters even when switched off. Click to"
                          + " rename or manage individual emitters."
                      : "Open this emitter's controls. Connections selects one span or your entire"
                            + " connected field.")));
    }
    var prev =
        addRenderableWidget(
            Button.builder(
                    Component.literal("Previous"),
                    b -> {
                      rename();
                      page--;
                      rebuildWidgets();
                    })
                .bounds(left + 12, top + 201, 65, 18)
                .build());
    prev.active = page > 0;
    var next =
        addRenderableWidget(
            Button.builder(
                    Component.literal("Next"),
                    b -> {
                      rename();
                      page++;
                      rebuildWidgets();
                    })
                .bounds(left + 82, top + 201, 48, 18)
                .build());
    next.active = page + 1 < pages;
    addRenderableWidget(
        Button.builder(
                Component.literal(selected == null ? "Refresh" : "All fields"),
                b -> {
                  rename();
                  openManager();
                })
            .bounds(left + 242, top + 201, 72, 18)
            .build());
    addRenderableWidget(
        Button.builder(Component.literal("Close"), b -> onClose())
            .bounds(left + 320, top + 201, 72, 18)
            .build());
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void renderBackground(GuiGraphics g, int x, int y, float p) {}

  @Override
  public void render(GuiGraphics g, int x, int y, float p) {
    beginFit(g);
    x = fitMouse(x);
    y = fitMouse(y);
    g.fill(0, 0, width, height, 0xB0101723);
    g.fill(left, top, left + 404, top + 224, 0xFF111D2C);
    g.fill(left, top, left + 404, top + 2, 0xFF52E5FF);
    g.drawString(font, title, left + 12, top + 10, 0xDBF8FF, false);
    g.drawString(
        font,
        selected == null
            ? "Loaded in " + data.getString("Dimension")
            : "Field location: " + BlockPos.of(selected.getLong("Pos")).toShortString(),
        left + 12,
        top + 26,
        0x92A9BE,
        false);
    g.drawString(
        font,
        selected == null ? "One connected chain = one field. Hold your tuner." : "Name:",
        left + 12,
        top + 43,
        0x92A9BE,
        false);
    if (selected == null && data.getList("Entries", 10).isEmpty())
      g.drawString(
          font, "No loaded fields available to you here.", left + 12, top + 90, 0xDBF8FF, false);
    g.drawString(font, font.plainSubstrByWidth(status, 380), left + 12, top + 188, 0x92A9BE, false);
    g.drawString(font, "Page " + (page + 1), left + 153, top + 206, 0x92A9BE, false);
    super.render(g, x, y, p);
    g.pose().popPose();
  }
}
