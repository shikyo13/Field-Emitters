package com.zerotheabsolute.fieldemitters.client;

import com.zerotheabsolute.fieldemitters.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

final class ManagementScreen extends FittedScreen {
  private final ControlScreen parent;
  private final BlockPos pos;
  private int left, top, page, pending = -1;
  private boolean owner, publicManagement;
  private final List<UUID> managers = new ArrayList<>();
  private String query = "", notice = "Loading permissions…";
  private static int requests = 100000;
  private long lookupStarted;

  ManagementScreen(ControlScreen parent, BlockPos pos) {
    super(Component.literal("NETWORK MANAGEMENT"));
    this.parent = parent;
    this.pos = pos;
    request(-1, "");
  }

  private void request(int action, String id) {
    PacketDistributor.sendToServer(new ManagementPackets.Request(pos, action, id));
  }

  private FieldButton button(String text, int x, int y, int w, Runnable run) {
    return addRenderableWidget(
        new FieldButton(left + x, top + y, w, 18, Component.literal(text), b -> run.run(), false));
  }

  protected void init() {
    fit(404, 306);
    left = (width - 404) / 2;
    top = (height - 306) / 2;
    button(
                "Management: "
                    + (publicManagement
                        ? "Public — anyone can configure"
                        : "Private — owner and invited managers"),
                12,
                34,
                380,
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
                Component.literal("Minecraft account name or UUID")));
    input.setMaxLength(36);
    input.setValue(query);
    input.setResponder(v -> query = v);
    input.active = owner;
    input.setTooltip(
        Tooltip.create(
            Component.literal(
                "Minecraft account name or UUID; server nicknames are not supported.")));
    button(
                pending < 0 ? "Add manager" : "Looking up…",
                296,
                100,
                96,
                () -> {
                  if (query.isBlank()) {
                    notice = "Enter a Minecraft account name or UUID.";
                    return;
                  }
                  pending = ++requests;
                  lookupStarted = net.minecraft.Util.getMillis();
                  PacketDistributor.sendToServer(
                      new PlayerLookup.Request(pos, pending, query.strip()));
                  notice = "Resolving player…";
                  rebuildWidgets();
                })
            .active =
        owner && pending < 0 && managers.size() < ManagementAccess.MAX_MANAGERS;
    int pages = Math.max(1, (managers.size() + 5) / 6);
    page = Math.min(page, pages - 1);
    for (int i = 0; i < 6 && page * 6 + i < managers.size(); i++) {
      var id = managers.get(page * 6 + i);
      var info = minecraft.getConnection().getPlayerInfo(id);
      String name = info == null ? id.toString() : info.getProfile().getName();
      button(font.plainSubstrByWidth(name, 280), 12, 130 + i * 20, 292, () -> {})
          .setTooltip(Tooltip.create(Component.literal(id.toString())));
      button("Remove", 310, 130 + i * 20, 82, () -> request(3, id.toString())).active = owner;
    }
    button(
                "Previous",
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
                "Next",
                110,
                254,
                90,
                () -> {
                  page++;
                  rebuildWidgets();
                })
            .active =
        page + 1 < pages;
    button("Back", 310, 280, 82, this::onClose);
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
              ? "Only you and server operators can change these permissions."
              : "Only the owner can change management permissions.";
      s.rebuildWidgets();
    }
  }

  static void lookup(PlayerLookup.Result r) {
    if (Minecraft.getInstance().screen instanceof ManagementScreen s
        && s.pending == r.requestId()) {
      s.pending = -1;
      if (!r.error().isEmpty()) {
        s.notice = r.error();
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
    if (pending >= 0 && net.minecraft.Util.getMillis() - lookupStarted > 10000) {
      pending = -1;
      notice = "Lookup timed out. Try again or enter a UUID.";
      rebuildWidgets();
    }
  }

  public boolean isPauseScreen() {
    return false;
  }

  public void onClose() {
    minecraft.setScreen(parent);
  }

  public void renderBackground(GuiGraphics g, int x, int y, float p) {}

  public void render(GuiGraphics g, int x, int y, float p) {
    beginFit(g);
    g.fill(left, top, left + 404, top + 306, 0xFF0D1D2B);
    g.fill(left, top, left + 404, top + 2, 0xFF53BBCB);
    g.drawString(font, "NETWORK MANAGEMENT", left + 12, top + 12, 0xFFE0F3FF, false);
    g.drawWordWrap(
        font,
        Component.literal(
            "These permissions control field settings, not passage. Badges never grant management"
                + " rights. Public mode lets any player change field settings."),
        left + 12,
        top + 59,
        380,
        0xFFADBED0);
    g.drawString(font, "Minecraft account name or UUID", left + 12, top + 87, 0xFFADBED0, false);
    if (managers.isEmpty())
      g.drawString(font, "No additional managers.", left + 12, top + 140, 0xFFADBED0, false);
    g.drawWordWrap(font, Component.literal(notice), left + 12, top + 280, 288, 0xFFADBED0);
    super.render(g, fitMouse(x), fitMouse(y), p);
    g.pose().popPose();
  }
}
