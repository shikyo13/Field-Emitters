package com.zerotheabsolute.fieldemitters.client;

import com.zerotheabsolute.fieldemitters.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

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
      notice = "Changes to accepted groups apply automatically.";
  private long due;

  AccessScreen(Screen parent, BlockPos pos, EntityFilter filter, Runnable apply) {
    super(Component.literal("ACCESS BADGES"));
    this.parent = parent;
    this.pos = pos;
    this.filter = filter;
    this.apply = apply;
    groups = filter == null ? "" : String.join(", ", filter.accessGroups);
  }

  private void button(String text, int y, Runnable run) {
    addRenderableWidget(
        new FieldButton(
            left + 12, top + y, 380, 18, Component.literal(text), b -> run.run(), false));
  }

  private void input(
      String label, String value, int y, int max, java.util.function.Consumer<String> change) {
    var w =
        addRenderableWidget(
            new EditBox(font, left + 12, top + y, 380, 18, Component.literal(label)));
    w.setMaxLength(max);
    w.setValue(value);
    w.setResponder(change);
    w.setTooltip(Tooltip.create(Component.literal(label)));
  }

  protected void init() {
    fit(404, 306);
    left = (width - 404) / 2;
    top = (height - 306) / 2;
    if (filter != null)
      input(
          "Accepted groups, separated by commas. Uses the player list mode; only this field owner's"
              + " badges count.",
          groups,
          48,
          1024,
          v -> {
            groups = v;
            due = net.minecraft.Util.getMillis() + 350;
          });
    input(
        "Group to issue or revoke: lowercase letters, numbers, spaces, underscores and hyphens",
        group,
        111,
        32,
        v -> group = v);
    input(
        "Optional player UUID. Blank means anyone carrying the badge can use it.",
        player,
        150,
        36,
        v -> player = v);
    button("Issue badge held in either hand", 176, () -> send(0));
    button("Revoke my badges in this group", 198, () -> send(1));
    button("Back", 280, this::onClose);
  }

  private void send(int action) {
    String name = BadgeAccess.group(group);
    if (!BadgeAccess.validGroup(name)) {
      notice = "Enter a valid group name first.";
      return;
    }
    PacketDistributor.sendToServer(new AccessPackets.Request(pos, name, player.strip(), action));
    notice = "Waiting for server…";
  }

  static void receive(AccessPackets.Result result) {
    if (net.minecraft.client.Minecraft.getInstance().screen instanceof AccessScreen screen
        && screen.pos.equals(result.pos())) screen.notice = result.message();
  }

  private void save() {
    due = 0;
    if (filter == null) return;
    var next = new java.util.LinkedHashSet<String>();
    for (var s : groups.split(",")) {
      if (s.isBlank()) continue;
      var name = BadgeAccess.group(s);
      if (!BadgeAccess.validGroup(name)) {
        notice = "Invalid group name; previous groups retained.";
        return;
      }
      next.add(name);
    }
    if (next.size() > 64) {
      notice = "Maximum 64 groups.";
      return;
    }
    filter.accessGroups.clear();
    filter.accessGroups.addAll(next);
    apply.run();
    notice = "Accepted groups updated. Enable a player list mode to use them.";
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
    g.fill(left, top, left + 404, top + 306, 0xFF0D1D2B);
    g.fill(left, top, left + 404, top + 2, 0xFF53BBCB);
    g.drawString(font, "ACCESS BADGES", left + 12, top + 12, 0xFFE0F3FF, false);
    g.drawString(
        font,
        filter == null
            ? "Issue badges for fields you own"
            : "Accepted groups (player list OR badge group)",
        left + 12,
        top + 34,
        0xFFADBED0,
        false);
    g.drawString(font, "Badge group", left + 12, top + 98, 0xFFADBED0, false);
    g.drawString(font, "Bind to player UUID (optional)", left + 12, top + 137, 0xFFADBED0, false);
    g.drawWordWrap(
        font,
        Component.literal(
            "A badge holder stores 16 badges. Right-click it with a badge in your inventory to"
                + " insert; empty cursor to remove. Equip the holder or tuner in Curios, or carry"
                + " them normally."),
        left + 12,
        top + 224,
        380,
        0xFFADBED0);
    g.drawString(
        font, font.plainSubstrByWidth(notice, 380), left + 12, top + 267, 0xFFADBED0, false);
    super.render(g, fitMouse(x), fitMouse(y), p);
    g.pose().popPose();
  }
}
