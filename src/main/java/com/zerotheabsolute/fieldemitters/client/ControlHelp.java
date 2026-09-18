package com.zerotheabsolute.fieldemitters.client;

/** Player-facing help shared by mouse hover and keyboard focus. */
final class ControlHelp {
  private static String filterHelp() {
    return UiText.text(
        "screen.fieldemitters.controlhelp.selected_means_any_checked_category_that_also_meets");
  }

  static String field(Control control) {
    return switch (control) {
      case ENTITY_TYPE_OR_GROUP ->
          UiText.text(
              "screen.fieldemitters.controlhelp.optional_enter_one_entity_type_such_as_minecraft");
      case DROPPED_ITEM_OR_GROUP ->
          UiText.text(
              "screen.fieldemitters.controlhelp.optional_enter_minecraft_gunpowder_to_match_dropped_gunpowder");
      case SPECIFIC_MOB_PLAYER_UUID ->
          UiText.text(
              "screen.fieldemitters.controlhelp.optional_match_one_specific_entity_using_its_unique");
      case CUSTOM_ENTITY_LABEL_TAG ->
          UiText.text(
              "screen.fieldemitters.controlhelp.optional_match_a_label_assigned_to_an_entity");
      case DAMAGE_PER_HIT_HP_2_1_HEART ->
          UiText.text(
              "screen.fieldemitters.controlhelp.enter_0_to_1000_health_points_per_successful");
      case EFFECT_COLOR_6_DIGIT_HEX ->
          UiText.text(
              "screen.fieldemitters.controlhelp.custom_color_for_impact_effects_drifting_pixels_plasma");
      case CUSTOM_COLOR_6_DIGIT_HEX_CODE ->
          UiText.text("screen.fieldemitters.controlhelp.enter_a_six_digit_color_code_for_example");
      default ->
          UiText.text(
              "screen.fieldemitters.controlhelp.leave_blank_for_no_additional_restriction_valid_changes");
    };
  }

  static String button(Control control, String text, ControlTab tab) {
    if (control == Control.EFFECT_COLOR)
      return UiText.text(
          "screen.fieldemitters.controlhelp.matches_field_keeps_impacts_pixels_plasma_veins_and");
    if (control == Control.PRESET_PURPLE_FIELD_MAGENTA_EFFECTS)
      return UiText.text(
          "screen.fieldemitters.controlhelp.sets_a_purple_field_with_drifting_pixels_and");
    if (control == Control.MANAGERS_AND_PUBLIC_ACCESS)
      return UiText.text(
          "screen.fieldemitters.controlhelp.choose_who_can_change_this_connected_network_s");
    if (control == Control.ISSUE_AND_REVOKE_BADGES)
      return UiText.text(
          "screen.fieldemitters.controlhelp.issue_badges_for_access_groups_and_revoke_them");
    if (control == Control.INVENTORY_CHECKPOINT)
      return UiText.text(
          "screen.fieldemitters.controlhelp.inspect_carried_inventory_using_a_separate_player_and");
    if (control == Control.OUTPUT_SIDE)
      return UiText.text(
          "screen.fieldemitters.controlhelp.emitter_face_that_sends_the_sensor_s_redstone");
    if (control == Control.PULSE_LENGTH)
      return UiText.text(
          "screen.fieldemitters.controlhelp.how_long_each_crossing_pulse_stays_on_a");
    if (control == Control.MOB_LIST)
      return UiText.text("screen.fieldemitters.controlhelp.manage_up_to_64_mob_ids_or_entity");
    if (control == Control.ITEM_LIST)
      return UiText.text("screen.fieldemitters.controlhelp.manage_up_to_64_dropped_item_ids_or");
    if (control == Control.PLAYER_LIST)
      return UiText.text(
          "screen.fieldemitters.controlhelp.manage_up_to_64_players_by_minecraft_account");
    if (control == Control.SAMPLE_INDIVIDUAL)
      return UiText.text(
          "screen.fieldemitters.controlhelp.copy_the_individual_uuid_sampled_with_your_tuner");
    if (control == Control.SAMPLE_TYPE)
      return UiText.text(
          "screen.fieldemitters.controlhelp.copy_the_entity_type_sampled_with_your_tuner");
    if (control == Control.DAMAGE)
      return UiText.text(
          "screen.fieldemitters.controlhelp.enable_contact_damage_using_this_tab_s_own");
    if (control == Control.DAMAGE_SELECTED || control == Control.DAMAGE_UNSELECTED)
      return UiText.text(
          "screen.fieldemitters.controlhelp.choose_whether_matching_entities_or_everything_outside_the",
          filterHelp());
    if (control == Control.HIT_INTERVAL)
      return UiText.text(
          "screen.fieldemitters.controlhelp.minimum_time_between_successful_field_hits_on_one");
    if (control == Control.FIZZLE_PARTICLES)
      return UiText.text(
          "screen.fieldemitters.controlhelp.short_colored_bursts_on_damage_and_a_larger");
    if (control == Control.PATTERN)
      return UiText.text(
          "screen.fieldemitters.controlhelp.choose_hex_lattice_a_clean_translucent_glow_drifting");
    if (control == Control.PROJECTION)
      return UiText.text(
          "screen.fieldemitters.controlhelp.choose_how_the_tower_builds_its_hollow_shell");
    if (control == Control.FORMATION)
      return UiText.text(
          "screen.fieldemitters.controlhelp.cycle_sweep_dissolve_fade_or_the_six_projection");
    if (control == Control.PURPLE)
      return UiText.text(
          "screen.fieldemitters.controlhelp.apply_a_translucent_purple_field_with_drifting_magenta");
    if (control == Control.ALL_FIELD_SOUNDS)
      return UiText.text(
          "screen.fieldemitters.controlhelp.master_switch_for_this_emitter_s_activation_deactivation");
    if (control == Control.POWER_ON_OFF_SOUNDS
        || control == Control.IMPACT_SOUNDS
        || control == Control.DAMAGE_SOUNDS)
      return UiText.text(
          "screen.fieldemitters.controlhelp.toggle_this_sound_category_independently_the_master_all");
    if (control == Control.SOUND_PALETTE)
      return UiText.text(
          "screen.fieldemitters.controlhelp.choose_short_vanilla_sound_effects_soft_sizzle_crystal");
    if (control == Control.RULES_FOR)
      return UiText.text(
          "screen.fieldemitters.controlhelp.choose_the_travel_direction_to_edit_north_south");
    if (control == Control.RULE_SOURCE)
      return UiText.text(
          "screen.fieldemitters.controlhelp.shared_uses_the_all_directions_filter_click_to");
    if (control == Control.HOSTILE)
      return UiText.text(
          "screen.fieldemitters.controlhelp.hostile_selects_entities_minecraft_classifies_as_monsters_such",
          filterHelp());
    if (control == Control.PASSIVE)
      return UiText.text(
          "screen.fieldemitters.controlhelp.passive_selects_living_entities_that_are_not_players",
          filterHelp());
    if (control == Control.PLAYERS)
      return UiText.text(
          "screen.fieldemitters.controlhelp.players_selects_player_characters_skip_owner_controls_whether",
          filterHelp());
    if (control == Control.DROPS)
      return UiText.text(
          "screen.fieldemitters.controlhelp.drops_selects_loose_item_entities_such_as_gunpowder",
          filterHelp());
    if (control == Control.NONLIVING)
      return UiText.text(
          "screen.fieldemitters.controlhelp.nonliving_selects_the_remaining_entities_such_as_arrows",
          filterHelp());
    if (control == Control.BLOCK_SELECTED)
      return UiText.text(
          "screen.fieldemitters.controlhelp.matching_entities_are_blocked_everything_else_may_pass",
          filterHelp());
    if (control == Control.ALLOW_SELECTED)
      return UiText.text(
          "screen.fieldemitters.controlhelp.only_matching_entities_may_pass_everything_else_is",
          filterHelp());
    if (control == Control.DETECT_SELECTED)
      return UiText.text(
          "screen.fieldemitters.controlhelp.matching_entities_trigger_detection_blocking_is_configured_separately",
          filterHelp());
    if (control == Control.DETECT_UNSELECTED)
      return UiText.text(
          "screen.fieldemitters.controlhelp.entities_outside_that_selection_trigger_detection_skip_owner",
          filterHelp());
    if (control == Control.AGE)
      return UiText.text(
          "screen.fieldemitters.controlhelp.click_to_cycle_any_age_babies_only_adults");
    if (control == Control.SKIP_OWNER && tab == ControlTab.DAMAGE)
      return UiText.text(
          "screen.fieldemitters.controlhelp.yes_always_exempts_the_emitter_owner_from_damage");
    if (control == Control.SKIP_OWNER)
      return tab == ControlTab.BLOCKING
          ? UiText.text(
              "screen.fieldemitters.controlhelp.yes_always_allows_the_player_who_placed_this")
          : UiText.text(
              "screen.fieldemitters.controlhelp.yes_never_detects_the_player_who_placed_this");
    if (tab == ControlTab.DAMAGE && (control == Control.MOVEMENT))
      return UiText.text(
          "screen.fieldemitters.controlhelp.checked_damage_matching_entities_approaching_in_this_world");
    if (control == Control.MOVEMENT)
      return UiText.text(
          "screen.fieldemitters.controlhelp.to_south_means_traveling_from_north_to_south",
          (tab == ControlTab.BLOCKING
              ? UiText.text(
                  "screen.fieldemitters.controlhelp.checked_apply_blocking_in_this_movement_direction_unchecked")
              : UiText.text(
                  "screen.fieldemitters.controlhelp.checked_detect_crossings_in_this_movement_direction_unchecked")));
    if (control == Control.SIGNAL)
      return UiText.text(
          "screen.fieldemitters.controlhelp.click_to_cycle_off_no_detection_output_pulse");
    if (control == Control.ITEMS)
      return UiText.text(
          "screen.fieldemitters.controlhelp.choose_how_dropped_item_stacks_affect_crossing_counts");
    if (control == Control.MATCH_THIS_SAMPLED)
      return UiText.text(
          "screen.fieldemitters.controlhelp.copy_the_individual_saved_in_your_tuner_sneak");
    if (control == Control.MATCH_THE_SAMPLED)
      return UiText.text(
          "screen.fieldemitters.controlhelp.copy_the_entity_type_saved_in_your_tuner");
    if (control == Control.FIELD)
      return UiText.text(
          "screen.fieldemitters.controlhelp.on_allows_this_emitter_to_operate_when_it");
    if (control == Control.TURN_ON)
      return UiText.text(
          "screen.fieldemitters.controlhelp.choose_when_the_field_operates_whenever_energy_is");
    if (control == Control.READ_REDSTONE_FROM)
      return UiText.text(
          "screen.fieldemitters.controlhelp.choose_the_side_that_reads_your_enable_disable");
    if (control == Control.RESET_CROSSING)
      return UiText.text(
          "screen.fieldemitters.controlhelp.immediately_clear_the_recorded_crossing_count_recent_detection");
    if (control == Control.LIGHT_NEARBY_BLOCKS)
      return UiText.text(
          "screen.fieldemitters.controlhelp.yes_adds_real_minecraft_block_light_no_keeps");
    if (control == Control.SHOW_FORCEFIELD)
      return UiText.text(
          "screen.fieldemitters.controlhelp.no_hides_the_forcefield_graphics_only_blocking_detection");
    if (control == Control.ANIMATE_FIELD_PATTERN)
      return UiText.text(
          "screen.fieldemitters.controlhelp.toggle_the_moving_pattern_on_the_field_surface");
    if (control == Control.DIRECTION_GUIDES)
      return UiText.text(
          "screen.fieldemitters.controlhelp.show_with_tuner_displays_movement_arrows_while_holding");
    if (control == Control.EDITING)
      return UiText.text(
          "screen.fieldemitters.controlhelp.choose_default_emitter_rules_or_select_one_outgoing");
    if (control == Control.SEND_REDSTONE_SIGNAL_FROM)
      return UiText.text(
          "screen.fieldemitters.controlhelp.choose_where_to_connect_redstone_dust_a_lamp");
    if (control == Control.SIGNAL_PULSE_LENGTH)
      return UiText.text(
          "screen.fieldemitters.controlhelp.choose_how_long_each_detection_pulse_stays_on");
    if (control == Control.BRIDGE_PRESET)
      return UiText.text(
          "screen.fieldemitters.controlhelp.set_horizontal_mode_block_all_entities_moving_downward");
    if (control == Control.FIELD_SHAPE)
      return UiText.text(
          "screen.fieldemitters.controlhelp.choose_the_orientation_of_the_flat_field_between");
    if (control == Control.COLOR_PRESET)
      return UiText.text(
          "screen.fieldemitters.controlhelp.immediately_use_this_color_preset_for_the_emitter");
    return text;
  }
}
