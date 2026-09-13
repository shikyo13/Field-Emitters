package com.zerotheabsolute.fieldemitters.client;

/** Player-facing help shared by mouse hover and keyboard focus. */
final class ControlHelp {
  private static final String FILTER =
      "Selected means any checked category that also meets EVERY filled-in detail below. Blank"
          + " details add no restriction. Enabled player, mob and item lists use their own modes instead. ";

  static String field(String label) {
    return switch (label) {
      case "Entity type or #group" ->
          "Optional: enter one entity type, such as minecraft:creeper. Or enter an existing"
              + " entity-type tag, such as #yourpack:guards, to match a group defined by a mod or"
              + " datapack. The # is required for groups; typing a name does not create a group."
              + " Keep its category checked above. Leave blank to accept any type in the selected"
              + " categories.";
      case "Dropped item or #group" ->
          "Optional: enter minecraft:gunpowder to match dropped gunpowder, or #minecraft:logs to"
              + " match dropped logs. Check Drops above and use Any age. This checks items on the"
              + " ground, not a mob's or player's inventory. One ID or existing item tag per box;"
              + " blank means any dropped item.";
      case "Specific mob / player (UUID)" ->
          "Optional: match one specific entity using its unique UUID. Easier: sneak-right-click a"
              + " living mob with the tuner, or sneak-right-click air to sample yourself. Then"
              + " click Sample individual. An enabled Player list replaces this restriction for players.";
      case "Custom entity label (/tag)" ->
          "Optional: match a label assigned to an entity with Minecraft's /tag command. For"
              + " example, /tag @s add resident gives you the label resident; enter resident here,"
              + " without #. This is different from an entity-type or item group. Leave blank to"
              + " ignore custom labels.";
      case "Damage per hit (HP; 2 = 1 heart)" ->
          "Enter 0 to 1000 health points per successful hit. 2 HP equals one heart. Damage is off by default. Vanilla invulnerability, damage immunity and protection mods still apply. Nonliving entities respond only if Minecraft gives them damage behavior; this never forcibly deletes an entity.";
      case "Particle color (6-digit hex)" ->
          "Color of drifting pixels, impact rings and fizzle bursts, independent of the field color. Enter six hex digits, such as FF33CC for magenta.";
      case "Custom color (6-digit hex code)" ->
          "Enter a six-digit color code, for example 52E5FF for cyan. You can also choose a color"
              + " preset above. Valid color changes apply automatically.";
      default -> "Leave blank for no additional restriction. Valid changes apply automatically.";
    };
  }

  static String button(String raw, int tab) {
    String text = raw.replaceFirst("^[✓○] ", "");
    if (text.startsWith("Mob list")) return "Manage up to 64 mob IDs or entity-type tags using whitelist/blacklist modes. Drag spawn eggs into the boxes or type IDs. Age, individual UUID and custom label narrow the listed selection. Players and dropped items have separate lists. Rules belong to the selected travel direction.";
    if (text.startsWith("Item list")) return "Manage up to 64 dropped-item IDs or item tags. Drag item icons from your inventory, JEI or EMI into the boxes without consuming anything. Whitelist allows listed drops; blacklist blocks listed drops. Detection and damage have their own lists. Item lists ignore mob age and general inversion.";
    if (text.startsWith("Player list")) return "Manage up to 64 players by Minecraft account name or UUID. Choose a whitelist or blacklist for blocking, or listed/unlisted players for detection and damage. A list changes players only, respects this direction and Skip owner, and replaces the other filter details for players.";
    if (text.equals("Sample individual")) return "Copy the individual UUID sampled with your tuner into this filter. An enabled Player list takes priority for players.";
    if (text.equals("Sample type")) return "Copy the entity type sampled with your tuner into this filter. An enabled Player list takes priority for players.";
    if (text.startsWith("Damage:")) return "Enable contact damage using this tab's own filter. Blocking and detection remain independent. Damage starts off; enabling it can hurt selected players, mobs and damageable nonliving entities.";
    if (text.startsWith("Damage selected") || text.startsWith("Damage unselected")) return FILTER + "Choose whether matching entities or everything outside the selection takes damage. Skip owner always takes priority.";
    if (text.startsWith("Hit interval:")) return "Minimum time between successful field hits on one entity, shared across touching tiles and overlapping fields so seams cannot multiply damage. Vanilla damage cooldowns still apply.";
    if (text.startsWith("Fizzle particles:")) return "Short colored bursts on damage and a larger body-shaped burst on a lethal hit. Effects are capped per tick and emitter. This changes particles, not damage or sounds.";
    if (text.startsWith("Pattern:")) return "Choose hex lattice, a clean translucent glow, drifting colored pixels, or plasma. All styles retain impact ripples. Visuals do not change filters or collision.";
    if (text.startsWith("Projection:")) return "Choose how the tower builds its hollow shell: opposing laser curtains, a rising ring, assembling hex cells, meridian arcs, a crown seed, or plasma dissolve. Works with every pattern. All take four seconds; collision starts only when complete. This choice does not change filters or power use.";
    if (text.startsWith("Formation:")) return "Choose the power-on and power-off appearance: a traveling sweep, a sheet forming/dissolving in patches, or a gentle fade. The field still activates progressively along the link.";
    if (text.startsWith("Purple /")) return "Apply a translucent purple field with drifting magenta pixels. Field and particle colors can also be edited separately.";
    if (text.startsWith("All field sounds:")) return "Master switch for this emitter's activation, deactivation, impact and fizzle sounds. Minecraft's Blocks volume also controls these effects. No looping background hum.";
    if (text.startsWith("Power on / off sounds:") || text.startsWith("Impact sounds:") || text.startsWith("Damage sounds:")) return "Toggle this sound category independently. The master All field sounds switch must also be on.";
    if (text.startsWith("Sound palette:")) return "Choose short vanilla sound effects: Soft sizzle, Crystal or Electric. No new music or continuous sound loop is added.";
    if (text.startsWith("Rules for:")) return "Choose the travel direction to edit. North → South means entering from the north and leaving to the south. Both directions edits the shared fallback; custom directional rules take priority. Blocking, detection and damage are independent.";
    if (text.startsWith("Rule source:")) return "Shared uses the Both directions filter. Click to create a Custom copy for this travel direction, then edit it below. Click again to remove the override and use Shared. Changing shared rules does not change custom copies.";
    if (text.equals("Hostile"))
      return FILTER
          + "Hostile selects entities Minecraft classifies as monsters, such as zombies and"
          + " creepers.";
    if (text.equals("Passive"))
      return FILTER
          + "Passive selects living entities that are not players or monsters, such as cows, sheep"
          + " and villagers. Neutral mobs can belong here even when angry.";
    if (text.equals("Players"))
      return FILTER
          + "Players selects player characters. Skip owner controls whether this emitter's owner"
          + " can match.";
    if (text.equals("Drops"))
      return FILTER
          + "Drops selects loose item entities, such as gunpowder on the ground. Use the Dropped"
          + " item box for a specific item. Use Any age.";
    if (text.equals("Nonliving"))
      return FILTER
          + "Nonliving selects the remaining entities, such as arrows, boats, minecarts and XP"
          + " orbs. Dropped items have their own Drops category. Use Any age.";
    if (text.startsWith("Block selected"))
      return FILTER
          + "Matching entities are blocked. Everything else may pass. Click to switch to allowing"
          + " only the selection.";
    if (text.startsWith("Allow selected"))
      return FILTER
          + "Only matching entities may pass; everything else is blocked. Example: check Passive"
          + " and Babies only to allow baby passive mobs through. Skip owner: Yes always lets the"
          + " owner pass.";
    if (text.startsWith("Detect selected"))
      return FILTER
          + "Matching entities trigger detection. Blocking is configured separately in Blocking.";
    if (text.startsWith("Detect unselected"))
      return FILTER
          + "Entities outside that selection trigger detection. Skip owner: Yes always excludes the"
          + " owner. This does not change what the field blocks.";
    if (text.startsWith("Age:"))
      return "Click to cycle: Any age, Babies only, Adults only. Babies are living entities"
          + " Minecraft marks as babies; Adults means living entities that are not babies."
          + " Dropped items and other nonliving entities need Any age.";
    if (text.startsWith("Skip owner:") && tab == 3) return "Yes always exempts the emitter owner from damage, including inverted filters. No includes the owner when they match.";
    if (text.startsWith("Skip owner:"))
      return tab == 1
          ? "Yes always allows the player who placed this emitter through, even with Allow selected"
              + " only. No applies the normal blocking rules to the owner too."
          : "Yes never detects the player who placed this emitter. No includes that player when"
              + " they match the detection rules.";
    if (tab == 3 && (text.startsWith("To ") || text.equals("Upward") || text.equals("Downward"))) return "Checked: damage matching entities approaching in this world direction. Downward applies from above, including entities standing on a horizontal bridge. Unchecked: no damage in this direction.";
    if (text.startsWith("To ") || text.equals("Upward") || text.equals("Downward"))
      return (tab == 1
              ? "Checked: apply blocking in this movement direction. Unchecked: allow passage in"
                  + " this direction. "
              : "Checked: detect crossings in this movement direction. Unchecked: ignore them. ")
          + "To South means traveling from north to south; Upward means moving from below to above."
          + " These are world directions, not the direction you are looking. Entity filters still"
          + " apply.";
    if (text.startsWith("Signal:"))
      return "Click to cycle. Off: no detection output. Pulse on crossing: send one redstone pulse"
          + " when a matching entity completely passes through. On while touching: keep"
          + " redstone on while a matching entity overlaps the field. Touching alone does"
          + " not count as a crossing.";
    if (text.startsWith("Items:"))
      return "Choose how dropped item stacks affect crossing counts and queued pulses. Count each"
          + " stack: a stack of 64 gives one count/pulse. Count each item: it gives 64"
          + " counts/pulses, sent one at a time. Mobs and players always count once.";
    if (text.startsWith("Match this sampled"))
      return "Copy the individual saved in your tuner. Sneak-right-click a living mob to sample it,"
          + " or sneak-right-click air to sample yourself. This replaces the current filter"
          + " details with that individual's UUID. Changes apply immediately.";
    if (text.startsWith("Match the sampled"))
      return "Copy the entity type saved in your tuner, for example all cows rather than one"
          + " particular cow. First sneak-right-click a living mob, or air for players. This"
          + " replaces the current filter details. Changes apply immediately.";
    if (text.startsWith("Field:"))
      return "On allows this emitter to operate when it has energy and satisfies its redstone"
          + " condition. Off stops its fields. Change settings for determines whether this"
          + " affects just this emitter or your connected emitters.";
    if (text.startsWith("Turn on:"))
      return "Choose when the field operates: whenever energy is available, only while its redstone"
          + " input is on, or only while that input is off. Normal operation always requires"
          + " FE energy. Only the explicitly enabled demo test mode can use redstone as"
          + " power.";
    if (text.startsWith("Read redstone from:"))
      return "Choose the side that reads your enable/disable signal. Top and Bottom refer to the"
          + " emitter block; North, South, East and West are world directions. The output"
          + " side is excluded to avoid using the same face for input and output.";
    if (text.startsWith("Reset crossing"))
      return "Immediately clear the recorded crossing count, recent detection and waiting pulses.";
    if (text.startsWith("Light nearby blocks:"))
      return "Yes adds real Minecraft block light. No keeps the surroundings dark for mob farms."
          + " The field can still look bright and block entities when world lighting is"
          + " off.";
    if (text.startsWith("Show forcefield:"))
      return "No hides the forcefield graphics only. Blocking, detection and damage continue while powered."
          + " Use Field: Off on the Power tab to stop operation.";
    if (text.startsWith("Animate field pattern:"))
      return "Toggle the moving pattern on the field surface. This does not disable"
          + " projection/retraction or hardware animations.";
    if (text.startsWith("Direction guides:"))
      return "Show with tuner displays movement arrows while holding the tuner or briefly"
          + " previewing a connection. Amber means blocking is enabled in that direction;"
          + " green means it is disabled. Filters still determine which entities are"
          + " blocked. Hidden removes these guides. This preference is saved for your client"
          + " only.";
    if (text.startsWith("Editing:"))
      return "Choose default emitter rules, or select one outgoing field for different Blocking,"
          + " Detection and Damage filters. A field-specific rule takes priority over defaults. All three filters can differ for one field; energy, color and redstone output"
          + " remain emitter settings. Changes apply automatically; switching loads"
          + " that field's saved settings.";
    if (text.startsWith("Change settings for:"))
      return "This emitter only changes the emitter you opened. My connected emitters copies these"
          + " settings to the connected emitters you are allowed to edit. Existing"
          + " field-specific rules remain in place. When editing one field, each change updates"
          + " only that field's filters at both endpoints.";
    if (text.startsWith("Send redstone signal from:"))
      return "Choose where to connect redstone dust, a lamp or a counter. The detector sends its"
          + " output through this side of the source emitter. A rail chain shares one"
          + " detector output at the coordinates shown below. Input and output must use"
          + " different sides.";
    if (text.startsWith("Signal pulse length:"))
      return "Choose how long each detection pulse stays on. There is a 0.1-second off gap between"
          + " queued pulses so counters see separate events. Times assume Minecraft's normal"
          + " 20 ticks per second. Pulse mode must be enabled on Detection.";
    if (text.startsWith("Bridge preset:")) return "Set horizontal mode, block all entities moving downward (including the owner), allow upward passage and disable damage. Replaces blocking direction overrides. Use on connected rails to build a walkable bridge.";
    if (text.startsWith("Field shape:"))
      return "Choose the orientation of the flat field between these rails. Wall facing North /"
          + " South is crossed north-to-south; Wall facing East / West is crossed"
          + " east-to-west. Horizontal floor / ceiling is crossed upward or downward. Only"
          + " orientations compatible with the rail placement are offered.";
    if (text.startsWith("#"))
      return "Immediately use this color preset for the emitter(s) selected in" + " Connections.";
    return text;
  }
}
