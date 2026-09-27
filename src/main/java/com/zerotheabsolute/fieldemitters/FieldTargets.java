package com.zerotheabsolute.fieldemitters;

import com.zeromods.core.filter.FilterTarget;
import com.zeromods.core.filter.TargetMatcher;
import com.zeromods.core.neoforge.MinecraftEntitySubject;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Field Emitters' exception kinds: Core's standard kinds plus access card groups. */
public final class FieldTargets {
  /** An access card group; matches players carrying a card for it. */
  public static final String CARD_GROUP = "CARD_GROUP";

  private static final Set<String> KINDS = Set.of(FilterTarget.MOB, FilterTarget.ITEM,
      FilterTarget.PLAYER, FilterTarget.INDIVIDUAL, CARD_GROUP);
  private static final int MAX_PLAYER_NAME = 16;

  public static final TargetMatcher MATCHER = (target, subject, owner) ->
      target.kind().equals(CARD_GROUP)
          ? subject instanceof MinecraftEntitySubject entity && entity.entity() instanceof Player player
              && BadgeAccess.matches(player, owner, Set.of(target.id()))
          : TargetMatcher.STANDARD.matches(target, subject, owner);

  private FieldTargets() {}

  /** Whether a loaded entry is a kind this mod understands, with a name of the right length. */
  public static boolean known(FilterTarget target) {
    return KINDS.contains(target.kind())
        && (target.kind().equals(FilterTarget.INDIVIDUAL) || target.name().length() <= MAX_PLAYER_NAME);
  }

  public static Component validate(FilterTarget target) {
    String kind = target.kind(), id = target.id(), name = target.name();
    if (kind.equals(FilterTarget.MOB) && id.equals("minecraft:player")) return null;
    if (kind.equals(FilterTarget.MOB) || kind.equals(FilterTarget.ITEM))
      return FieldControls.validateTypeEntry(id, kind.equals(FilterTarget.ITEM));
    if (kind.equals(CARD_GROUP) && BadgeAccess.validGroup(id)) return null;
    if (kind.equals(FilterTarget.INDIVIDUAL)) {
      try {
        UUID.fromString(id);
        return name.equals("minecraft:player") ? null : FieldControls.validateTypeEntry(name, false);
      } catch (IllegalArgumentException ignored) {}
    }
    if (kind.equals(FilterTarget.PLAYER)) {
      try {
        UUID.fromString(id);
        if (name.isEmpty() || name.matches("[A-Za-z0-9_]{1,16}")) return null;
      } catch (IllegalArgumentException ignored) {}
    }
    return Component.translatable("message.fieldemitters.fieldcontrols.invalid_id_or_tag", id);
  }
}
