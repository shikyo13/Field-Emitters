package com.zerotheabsolute.fieldemitters.client;

import com.zeromods.core.client.FittedScreen;

import com.zerotheabsolute.fieldemitters.*;
import com.zerotheabsolute.fieldemitters.client.emi.EmiScreenBridge;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

/** Ghost slots store type references only; inventory stacks never move or shrink. */
public final class TypeListScreen extends FittedScreen {
  private final net.minecraft.client.gui.screens.Screen parent;
  private final EntityFilter filter;
  private final boolean items;
  private final FilterPurpose purpose;
  private final Runnable apply;
  private int left, top, page;
  private final boolean emi = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("emi");
  private final int browserInset = emi || net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("jei") ? 24 : 0;
  private String query = "",
      status =
          UiText.text("screen.fieldemitters.typelist.drag_from_your_inventory_jei_or_emi_into");
  private long legacyChanged;
  private ItemStack dragging = ItemStack.EMPTY;

  TypeListScreen(
      net.minecraft.client.gui.screens.Screen parent,
      EntityFilter filter,
      boolean items,
      FilterPurpose purpose,
      Runnable apply) {
    super(
        Component.literal(
            items
                ? UiText.text("screen.fieldemitters.typelist.item_list")
                : UiText.text("screen.fieldemitters.typelist.mob_list")));
    this.parent = parent;
    this.filter = filter;
    this.items = items;
    this.purpose = purpose;
    this.apply = apply;
  }

  private Set<String> entries() {
    return items ? filter.itemList : filter.mobList;
  }

  private int mode() {
    return items ? filter.itemMode : filter.mobMode;
  }

  private String modeLabel() {
    if (mode() == 0) return UiText.text("screen.fieldemitters.typelist.use_general_filter");
    if (purpose == FilterPurpose.CHECKPOINT)
      return UiText.text(
          mode() == 1
              ? "screen.fieldemitters.typelist.contraband_listed_items"
              : "screen.fieldemitters.typelist.contraband_everything_except_listed_items");
    String action =
        purpose == FilterPurpose.BLOCKING
            ? (mode() == 1 ? "block" : "allow")
            : purpose == FilterPurpose.SENSOR ? "detect" : "damage";
    return UiText.text(
        "screen.fieldemitters.typelist.mode."
            + (items ? "items." : "mobs.")
            + action
            + (mode() == 1 ? ".listed" : ".unlisted"));
  }

  protected void init() {
    fit(ScreenMetrics.PANEL_WIDTH, ScreenMetrics.PANEL_HEIGHT, browserInset);
    left = (width - ScreenMetrics.PANEL_WIDTH) / 2;
    top = (height - ScreenMetrics.PANEL_HEIGHT) / 2;
    // Recipe browsers measure Screen.width/height in real GUI coordinates.
    width = minecraft.getWindow().getGuiScaledWidth();
    height = minecraft.getWindow().getGuiScaledHeight();
    button(
            modeLabel(),
            12,
            30,
            ScreenMetrics.CONTENT_WIDTH,
            () -> {
              if (items) filter.itemMode = (mode() + 1) % 3;
              else filter.mobMode = (mode() + 1) % 3;
              status = UiText.text("screen.fieldemitters.typelist.list_mode_updated");
              changed();
            })
        .setTooltip(
            Tooltip.create(
                Component.literal(
                    UiText.text(
                        "screen.fieldemitters.typelist.general_filter_preserves_the_existing_category_and_single"))));
    var legacy =
        new EditBox(
            font,
            left + 12,
            top + 76,
            ScreenMetrics.CONTENT_WIDTH,
            16,
            Component.literal(
                UiText.text("screen.fieldemitters.typelist.general_filter_single_type_or_tag")));
    legacy.setMaxLength(EntityFilter.MAX_TYPE_LENGTH);
    legacy.setValue(items ? filter.itemType : filter.entityType);
    legacy.active = mode() == 0;
    legacy.setTooltip(
        Tooltip.create(
            Component.literal(
                UiText.text(
                    "screen.fieldemitters.typelist.the_previous_single_type_restriction_only_used_in"))));
    legacy.setResponder(
        value -> {
          if (items) filter.itemType = value;
          else filter.entityType = value;
          legacyChanged = net.minecraft.Util.getMillis();
        });
    addRenderableWidget(legacy);
    var input =
        addRenderableWidget(
            new EditBox(
                font,
                left + 12,
                top + 106,
                294,
                18,
                Component.literal(
                    UiText.text("screen.fieldemitters.typelist.add_registry_id_or_tag"))));
    input.setMaxLength(EntityFilter.MAX_TYPE_LENGTH);
    input.setValue(query);
    input.setResponder(value -> query = value);
    input.setTooltip(
        Tooltip.create(
            Component.literal(
                items
                    ? UiText.text(
                        "screen.fieldemitters.typelist.item_id_minecraft_gunpowder_item_tag_minecraft_logs")
                    : UiText.text(
                        "screen.fieldemitters.typelist.mob_id_minecraft_creeper_entity_type_tag_minecraft"))));
    button(UiText.text("screen.fieldemitters.playerlist.add"), 312, 106, 80, () -> add(query));
    int pages =
        Math.max(1, (entries().size() + ScreenMetrics.TYPE_SLOTS - 1) / ScreenMetrics.TYPE_SLOTS);
    page = Math.min(page, pages - 1);
    button(
                UiText.text("screen.fieldemitters.management.previous"),
                312,
                134,
                80,
                () -> {
                  page--;
                  rebuildWidgets();
                })
            .active =
        page > 0;
    button(
                UiText.text("screen.fieldemitters.management.next"),
                312,
                156,
                80,
                () -> {
                  page++;
                  rebuildWidgets();
                })
            .active =
        page + 1 < pages;
    button(UiText.text("screen.fieldemitters.access.back"), 312, 284, 80, this::onClose);
    if (emi) EmiScreenBridge.init(this);
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

  private void add(String value) {
    value = value.trim();
    Component error = FieldControls.validateTypeEntry(value, items);
    if (error != null) {
      status = error.getString();
      return;
    }
    boolean tag = value.startsWith("#");
    value = (tag ? "#" : "") + ResourceLocation.parse(tag ? value.substring(1) : value);
    if (entries().contains(value)) {
      status = UiText.text("screen.fieldemitters.typelist.already_in_this_list");
      return;
    }
    if (entries().size() >= EntityFilter.MAX_TYPES) {
      status = UiText.text("screen.fieldemitters.typelist.the_list_is_full_64_entries");
      return;
    }
    entries().add(value);
    query = "";
    page = (entries().size() - 1) / 24;
    status =
        mode() == 0
            ? UiText.text(
                "screen.fieldemitters.typelist.added_choose_whitelist_or_blacklist_to_enable_the")
            : UiText.text("screen.fieldemitters.typelist.added_changes_applied");
    changed();
  }

  public boolean canAcceptDrop(ItemStack stack) {
    return !stack.isEmpty() && (items || stack.getItem() instanceof SpawnEggItem);
  }

  public void acceptDrop(ItemStack stack) {
    if (!canAcceptDrop(stack)) {
      status = UiText.text("screen.fieldemitters.typelist.use_a_spawn_egg_for_a_mob_or");
      return;
    }
    add(
        items
            ? BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()
            : BuiltInRegistries.ENTITY_TYPE
                .getKey(((SpawnEggItem) stack.getItem()).getType(stack))
                .toString());
  }

  private Rect2i physical(int x, int y, int w, int h) {
    // CanvasFit can truncate the logical height; derive the exact scale from its shared model.
    double s =
        com.zeromods.core.ui.CanvasFit.fit(
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight() - (browserInset),
                ScreenMetrics.PANEL_WIDTH,
                ScreenMetrics.PANEL_HEIGHT,
                4)
            .scale();
    return new Rect2i(
        (int) Math.ceil(x * s),
        (int) Math.ceil(y * s),
        (int) Math.floor(w * s),
        (int) Math.floor(h * s));
  }

  public Rect2i dropArea() {
    return physical(left + 12, top + 132, 288, 44);
  }

  public Rect2i panelArea() {
    return physical(left, top, ScreenMetrics.PANEL_WIDTH, ScreenMetrics.PANEL_HEIGHT);
  }

  private boolean inside(int x, int y, int bx, int by, int w, int h) {
    return x >= bx && x < bx + w && y >= by && y < by + h;
  }

  public boolean mouseClicked(double x, double y, int button) {
    if (emi && EmiScreenBridge.click(x, y, button)) return true;
    int mx = fitMouse((int) x), my = fitMouse((int) y);
    if (button == 0 && inside(mx, my, left + 12, top + 208, 162, 72)) {
      int slot = (my - top - 208) / 18 * 9 + (mx - left - 12) / 18;
      int inventorySlot = slot < 27 ? slot + 9 : slot - 27;
      dragging = minecraft.player.getInventory().getItem(inventorySlot).copy();
      return true;
    }
    if (button == 1 && inside(mx, my, left + 12, top + 132, 288, 44)) {
      int slot =
          (my - top - 132) / 22 * 12 + (mx - left - 12) / 24 + page * ScreenMetrics.TYPE_SLOTS;
      var list = new ArrayList<>(entries());
      if (slot < list.size()) {
        entries().remove(list.get(slot));
        status = UiText.text("screen.fieldemitters.typelist.removed_changes_applied");
        changed();
      }
      return true;
    }
    return super.mouseClicked(x, y, button);
  }

  public boolean mouseReleased(double x, double y, int button) {
    if (emi && EmiScreenBridge.release(x, y, button)) return true;
    if (button == 0 && !dragging.isEmpty()) {
      var stack = dragging;
      dragging = ItemStack.EMPTY;
      if (inside(fitMouse((int) x), fitMouse((int) y), left + 12, top + 132, 288, 44))
        acceptDrop(stack);
      return true;
    }
    return super.mouseReleased(x, y, button);
  }

  public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
    if (emi && EmiScreenBridge.drag(x, y, button, dx, dy)) return true;
    return super.mouseDragged(x, y, button, dx, dy);
  }

  public boolean mouseScrolled(double x, double y, double dx, double dy) {
    if (emi && EmiScreenBridge.scroll(x, y, dy)) return true;
    return super.mouseScrolled(x, y, dx, dy);
  }

  public boolean charTyped(char value, int modifiers) {
    if (emi && EmiScreenBridge.character(value, modifiers)) return true;
    return super.charTyped(value, modifiers);
  }

  public boolean keyPressed(int key, int scan, int modifiers) {
    if (emi && EmiScreenBridge.key(key, scan, modifiers)) return true;
    if (key == 257 || key == 335) {
      if (!query.isBlank()) add(query);
      else apply.run();
      return true;
    }
    return super.keyPressed(key, scan, modifiers);
  }

  public void tick() {
    super.tick();
    if (legacyChanged != 0 && net.minecraft.Util.getMillis() - legacyChanged > 350) {
      legacyChanged = 0;
      Component error = FieldControls.validate(filter);
      if (error == null) {
        apply.run();
        status = UiText.text("screen.fieldemitters.control.changes_applied");
      } else status = error.getString();
    }
  }

  public boolean isPauseScreen() {
    return false;
  }

  public void onClose() {
    apply.run();
    minecraft.setScreen(parent);
  }

  public void renderBackground(GuiGraphics g, int x, int y, float partial) {}

  private ItemStack icon(String value) {
    if (value.startsWith("#")) return new ItemStack(Items.NAME_TAG);
    var id = ResourceLocation.tryParse(value);
    if (id == null) return new ItemStack(Items.BARRIER);
    if (items) return new ItemStack(BuiltInRegistries.ITEM.get(id));
    var egg = SpawnEggItem.byId(BuiltInRegistries.ENTITY_TYPE.get(id));
    return new ItemStack(egg == null ? Items.PAPER : egg);
  }

  public void render(GuiGraphics g, int mx, int my, float partial) {
    int x = fitMouse(mx), y = fitMouse(my);
    beginFit(g);
    g.fill(
        left, top, left + ScreenMetrics.PANEL_WIDTH, top + ScreenMetrics.PANEL_HEIGHT, 0xFF0D1D2B);
    g.fill(left, top, left + ScreenMetrics.PANEL_WIDTH, top + 2, 0xFF53BBCB);
    drawLabel(g, UiText.text(
            "screen.fieldemitters.typelist.list",
            (items
                ? UiText.text("screen.fieldemitters.typelist.item_2")
                : UiText.text("screen.fieldemitters.typelist.mob_2")),
            purpose.title()), left + 12, top + 12, ScreenMetrics.CONTENT_WIDTH, 0xFFE0F3FF);
    drawLabel(g, items
            ? (purpose == FilterPurpose.CHECKPOINT
                ? UiText.text(
                    "screen.fieldemitters.typelist.matching_items_carried_by_selected_players_are_contraband")
                : UiText.text(
                    "screen.fieldemitters.typelist.dropped_items_only_players_and_mobs_keep_their"))
            : UiText.text("screen.fieldemitters.typelist.mobs_only_age_uuid_and_label_narrow_the"), left + 12, top + 54, ScreenMetrics.CONTENT_WIDTH, 0xFFADBED0);
    drawLabel(g, UiText.text("screen.fieldemitters.typelist.general_filter_single_type_or_tag"), left + 12, top + 66, ScreenMetrics.CONTENT_WIDTH, 0xFFADBED0);
    drawLabel(g, UiText.text(
            "screen.fieldemitters.typelist.add_id_or_tag",
            (items
                ? UiText.text("screen.fieldemitters.typelist.item")
                : UiText.text("screen.fieldemitters.typelist.mob"))), left + 12, top + 96, ScreenMetrics.CONTENT_WIDTH, 0xFFADBED0);
    var list = new ArrayList<>(entries());
    String hovered = null;
    for (int i = 0; i < 24; i++) {
      int sx = left + 12 + i % 12 * 24, sy = top + 132 + i / 12 * 22;
      g.fill(sx, sy, sx + 22, sy + 20, 0xFF356071);
      g.fill(sx + 1, sy + 1, sx + 21, sy + 19, 0xFF162F40);
      if (page * ScreenMetrics.TYPE_SLOTS + i < list.size()) {
        String value = list.get(page * ScreenMetrics.TYPE_SLOTS + i);
        g.renderItem(icon(value), sx + 3, sy + 2);
        if (inside(x, y, sx, sy, 22, 20))
          hovered = UiText.text("screen.fieldemitters.typelist.right_click_to_remove", value);
      }
    }
    drawLabel(g, UiText.text("screen.fieldemitters.typelist.page", (page + 1)), left + 12, top + 178, ScreenMetrics.CONTENT_WIDTH, 0xFFADBED0);
    drawLabel(g, status, left + 12, top + 190, ScreenMetrics.CONTENT_WIDTH, 0xFF8DE0CF);
    drawLabel(g, UiText.text("screen.fieldemitters.typelist.your_inventory_drag_a_copy_into_the_boxes"), left + 12, top + 199, ScreenMetrics.CONTENT_WIDTH, 0xFFADBED0);
    for (int i = 0; i < 36; i++) {
      int sx = left + 12 + i % 9 * 18, sy = top + 208 + i / 9 * 18;
      var stack = minecraft.player.getInventory().getItem(i < 27 ? i + 9 : i - 27);
      g.fill(sx, sy, sx + 17, sy + 17, 0xFF294455);
      g.renderItem(stack, sx + 1, sy + 1);
      g.renderItemDecorations(font, stack, sx + 1, sy + 1);
      if (!stack.isEmpty() && inside(x, y, sx, sy, 18, 18))
        hovered = stack.getHoverName().getString();
    }
    drawParagraph(g, Component.literal(
            items
                ? UiText.text(
                    "screen.fieldemitters.typelist.drag_items_from_your_inventory_jei_or_emi")
                : UiText.text(
                    "screen.fieldemitters.typelist.drag_spawn_eggs_from_your_inventory_jei_or")), left + 190, top + 213, 196, 63, 0xFFADBED0);
    drawLabel(g, UiText.text("screen.fieldemitters.typelist.right_click_a_filter_icon_to_remove_it"), left + 12, top + 289, ScreenMetrics.CONTENT_WIDTH, 0xFFADBED0);
    super.render(g, x, y, partial);
    if (!dragging.isEmpty()) g.renderItem(dragging, x - 8, y - 8);
    else if (hovered != null)
      g.renderComponentTooltip(
          font,
          Arrays.stream(hovered.split("\n"))
              .map(Component::literal)
              .map(c -> (Component) c)
              .toList(),
          x,
          y);
    g.pose().popPose();
    if (emi) EmiScreenBridge.render(g, mx, my, partial);
  }
}
