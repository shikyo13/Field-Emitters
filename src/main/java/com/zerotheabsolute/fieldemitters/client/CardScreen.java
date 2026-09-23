package com.zerotheabsolute.fieldemitters.client;

import com.zeromods.core.client.FittedScreen;
import com.zerotheabsolute.fieldemitters.*;
import com.zerotheabsolute.fieldemitters.network.ForgePacketDistributor;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

final class CardScreen extends FittedScreen {
  private enum Page { GROUPS, ISSUE, REVOKE }
  private static final int GROUP_ROWS = 4;
  private final Screen parent;
  private final BlockPos pos;
  private final CardPassage cards;
  /** From the screen's emitter copy, which remote screens build from server data. */
  private final UUID owner;
  private final Runnable apply;
  private Page page = Page.GROUPS;
  private int left, top, groupPage;
  private String group = "staff", notice = "";
  private UUID bound;
  private boolean allowIssued = true, pending;
  private String pendingGroup;
  private boolean pendingAllow;
  private long sentAt;

  CardScreen(Screen parent, BlockPos pos, UUID owner, CardPassage cards, Runnable apply) {
    super(Component.translatable("screen.fieldemitters.cards.title"));
    this.parent = parent;
    this.pos = pos;
    this.owner = owner;
    this.cards = cards;
    this.apply = apply;
  }

  private static String text(String key, Object... args) {
    return UiText.text("screen.fieldemitters.cards." + key, args);
  }

  private FieldButton button(String label, int x, int y, int width, Runnable action) {
    return addRenderableWidget(new FieldButton(left + x, top + y, width,
        ScreenMetrics.BUTTON_HEIGHT, Component.literal(label), b -> action.run(), false));
  }

  private void wide(String label, int y, Runnable action) {
    button(label, 12, y, ScreenMetrics.CONTENT_WIDTH, action);
  }

  private void groupInput(int y) {
    var input = addRenderableWidget(new FieldEditBox(font, left + 12, top + y,
        ScreenMetrics.CONTENT_WIDTH, ScreenMetrics.BUTTON_HEIGHT, Component.literal(text("group"))));
    input.setMaxLength(32);
    input.setValue(group);
    input.setResponder(value -> group = value);
    input.setTooltip(Tooltip.create(Component.literal(text("group_help"))));
  }

  @Override
  protected void init() {
    fit(ScreenMetrics.PANEL_WIDTH, ScreenMetrics.PANEL_HEIGHT);
    left = (width - ScreenMetrics.PANEL_WIDTH) / 2;
    top = (height - ScreenMetrics.PANEL_HEIGHT) / 2;
    if (page == Page.GROUPS) {
      wide(text("toggle", text(cards.enabled ? "enabled" : "disabled")), 55, () -> {
        cards.enabled = !cards.enabled;
        changed();
      });
      if (cards.enabled) {
        var groups = new ArrayList<>(cards.groups);
        int pages = Math.max(1, (groups.size() + GROUP_ROWS - 1) / GROUP_ROWS);
        groupPage = Math.min(groupPage, pages - 1);
        for (int i = 0; i < GROUP_ROWS && groupPage * GROUP_ROWS + i < groups.size(); i++) {
          String name = groups.get(groupPage * GROUP_ROWS + i);
          button(name, 12, 91 + i * 20, 286, () -> { group = name; open(Page.ISSUE); });
          button(text("remove"), 304, 91 + i * 20, 88, () -> {
            cards.groups.remove(name);
            changed();
          });
        }
        button(text("previous"), 12, 175, 110, () -> { groupPage--; rebuildWidgets(); }).active = groupPage > 0;
        button(text("next"), 282, 175, 110, () -> { groupPage++; rebuildWidgets(); }).active = groupPage + 1 < pages;
        groupInput(199);
        wide(text("add"), 221, this::addGroup);
      }
      button(text("issue"), 12, 247, ScreenMetrics.HALF_CONTENT_WIDTH, () -> open(Page.ISSUE));
      button(text("revoke"), 205, 247, ScreenMetrics.HALF_CONTENT_WIDTH, () -> open(Page.REVOKE));
    } else {
      groupInput(79);
      if (page == Page.ISSUE) {
        wide(text("holder", holderName()), 110, this::cyclePlayer);
        wide(text("allow_group", text(allowIssued ? "yes" : "no")), 150, () -> {
          allowIssued = !allowIssued;
          rebuildWidgets();
        });
        button(text("issue_held"), 12, 197, ScreenMetrics.CONTENT_WIDTH, () -> send(0)).active =
            !pending && ownsField();
      } else {
        button(text("revoke_group"), 12, 155, ScreenMetrics.CONTENT_WIDTH, () -> send(1)).active = !pending;
      }
    }
    wide(text("back"), 280, this::onClose);
  }

  /** Cards are keyed to their issuer, and a field accepts only its owner's cards. */
  private boolean ownsField() {
    return minecraft.player != null && minecraft.player.getUUID().equals(owner);
  }

  private String holderName() {
    if (bound == null) return text("anyone");
    var info = minecraft.getConnection().getPlayerInfo(bound);
    return info == null ? bound.toString() : info.getProfile().getName();
  }

  private void cyclePlayer() {
    var ids = minecraft.getConnection().getOnlinePlayers().stream()
        .map(info -> info.getProfile().getId()).sorted().toList();
    int next = bound == null ? 0 : ids.indexOf(bound) + 1;
    bound = next < ids.size() ? ids.get(next) : null;
    rebuildWidgets();
  }

  private String validGroup() {
    String name = BadgeAccess.group(group);
    if (!BadgeAccess.validGroup(name)) { notice = text("invalid"); return null; }
    return name;
  }

  private void addGroup() {
    String name = validGroup();
    if (name == null) return;
    if (!cards.groups.contains(name) && cards.groups.size() >= EntityFilter.MAX_ACCESS_GROUPS) {
      notice = text("limit"); return;
    }
    cards.groups.add(name);
    changed();
  }

  private void changed() {
    apply.run();
    notice = text("saved");
    rebuildWidgets();
  }

  private void open(Page next) {
    page = next;
    notice = "";
    rebuildWidgets();
  }

  private void send(int action) {
    String name = validGroup();
    if (name == null || pending) return;
    if (action == 0 && allowIssued && !cards.groups.contains(name)
        && cards.groups.size() >= EntityFilter.MAX_ACCESS_GROUPS) {
      notice = text("limit"); return;
    }
    pendingGroup = name;
    pendingAllow = action == 0 && allowIssued;
    pending = true;
    sentAt = net.minecraft.Util.getMillis();
    ForgePacketDistributor.sendToServer(new AccessPackets.Request(pos, name,
        bound == null || action == 1 ? "" : bound.toString(), action));
    notice = text("waiting");
    rebuildWidgets();
  }

  static void receive(AccessPackets.Result result) {
    if (!(net.minecraft.client.Minecraft.getInstance().screen instanceof CardScreen screen)
        || !screen.pos.equals(result.pos())) return;
    screen.pending = false;
    boolean issued = result.message().getContents() instanceof TranslatableContents content
        && content.getKey().equals("message.fieldemitters.accesspackets.issued_badge");
    if (issued && screen.pendingAllow) {
      screen.cards.enabled = true;
      screen.cards.groups.add(screen.pendingGroup);
      screen.apply.run();
    }
    screen.notice = result.message().getString();
    if (issued) screen.notice = text(screen.cards.enabled && screen.cards.groups.contains(screen.pendingGroup)
        ? "issued_allowed" : "issued_not_allowed", screen.pendingGroup);
    screen.rebuildWidgets();
  }

  @Override public void tick() {
    if (pending && net.minecraft.Util.getMillis() - sentAt > ScreenMetrics.LOOKUP_TIMEOUT_MILLIS) {
      pending = false;
      notice = text("timeout");
      rebuildWidgets();
    }
  }

  @Override public void onClose() {
    if (pending) return;
    if (page != Page.GROUPS) open(Page.GROUPS);
    else minecraft.setScreen(parent);
  }
  @Override public boolean isPauseScreen() { return false; }
  @Override public void renderBackground(GuiGraphics g) {}

  private void label(GuiGraphics g, String value, int y) {
    drawLabel(g, value, left + 12, top + y, ScreenMetrics.CONTENT_WIDTH, 0xFFADBED0);
  }

  @Override public void render(GuiGraphics g, int x, int y, float partialTick) {
    beginFit(g);
    g.fill(left, top, left + ScreenMetrics.PANEL_WIDTH, top + ScreenMetrics.PANEL_HEIGHT, 0xFF0D1D2B);
    g.fill(left, top, left + ScreenMetrics.PANEL_WIDTH, top + 2, 0xFF53BBCB);
    label(g, text(page == Page.GROUPS ? "title" : page == Page.ISSUE ? "issue" : "revoke"), 12);
    label(g, text("scope"), 33);
    if (page == Page.GROUPS && cards.enabled) label(g, text(cards.groups.isEmpty() ? "empty" : "groups"), 79);
    if (page != Page.GROUPS) label(g, text("group"), 65);
    if (page == Page.ISSUE) label(g, text("online_help"), 133);
    if (page == Page.ISSUE && !ownsField()) label(g, text("owner_only"), 176);
    if (page == Page.REVOKE) drawParagraph(g, Component.literal(text("revoke_help")), left + 12, top + 108, ScreenMetrics.CONTENT_WIDTH, 38, 0xFFADBED0);
    drawParagraph(g, Component.literal(notice), left + 12, top + (page == Page.GROUPS ? 268 : 226), ScreenMetrics.CONTENT_WIDTH, page == Page.GROUPS ? 10 : 45, 0xFF8BE4DD);
    super.render(g, fitMouse(x), fitMouse(y), partialTick);
    g.pose().popPose();
  }
}
