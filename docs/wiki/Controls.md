# Controls and the tuner

Right-click an emitter with an empty hand or the Field Tuner to open its controls. Settings save as you change them. Text fields save after a short pause in typing. Hover over a control for help.

## Tabs

- **Overview:** turn the network on or off, check its energy and status, and choose when it responds to redstone.
- **Blocking:** choose what the field stops, including travel directions, categories and lists.
- **Sensor:** choose what triggers detection, the signal type, output side and pulse length. You can also reset the crossing counter here.
- **Damage:** turn contact damage on or off, choose what it affects, and set damage per hit, hit interval and particles.
- **Appearance:** choose colors, patterns and formation animations. This page also controls field visibility, nearby lighting and direction guides.
- **Sounds:** choose a sound set or turn off individual sound types.
- **Connections:** edit network defaults or one connection. For rails, choose the field shape or apply **Bridge mode**. For towers, choose the shape and radius.
- **Access:** open Field management, Access cards or Inventory checkpoint.

## Filter details

Blocking, Sensor and Damage keep the category buttons and mob, item and player lists together. Each list shows its current mode and entry count. When a list replaces category matching, the affected category buttons are disabled.

Open **Travel directions** to restrict movement or edit a particular direction. Choosing a direction only changes the view; choose **Custom filter** to give it separate rules. **Age** is on the main filter page. In Mob list or Item list, **Entity details** opens UUID and scoreboard-label restrictions shared by the target categories. Mob list also provides the sampling buttons. Returning from these screens keeps the restrictions active.

In Sensor, **Redstone & counting** contains the output side, pulse length and crossing counter. Pulse length appears only in pulse mode. In Damage, **Damage settings** contains the amount and hit interval.

Appearance keeps color presets, pattern and formation on the main page. **Custom colors** contains the hex fields and separate effect colors; **Display options** contains visibility, lighting and direction guides.

Inventory checkpoint separates **Players & directions** from **When contraband is found**. Storage controls appear when confiscation is set to send items to adjacent storage.

## Networks and individual connections

Connected emitters share settings. A newly linked emitter inherits the network's settings, even when the field is off.

In **Connections**, select **Network defaults** to edit the whole network. Select a particular connection to give it different Blocking, Sensor or Damage filters. **Use network defaults** removes that connection's custom filters. Appearance, power and redstone settings apply to the network.

Settings survive world reloads. If two networks reconnect, the most recently edited settings are used. Connection-specific filters remain attached to their connections.

## The Field Tuner

- **Right-click an emitter** to open its controls.
- **Right-click the air** to open the Field Manager. Select a loaded field to rename it or open its controls remotely.
- **Sneak-right-click a mob or player** to sample it, or sneak-right-click the air to sample yourself. In Mob list → Entity details, use **Sample individual** for that entity or **Sample type** for its type.
- **Sneak-right-click an emitter** to cycle its color.

Set an **Open Field Tuner** key in Minecraft's Controls menu to open it from your inventory. Curios equipment slots are not supported on this build.

While holding the tuner, arrows show the blocked and allowed travel directions. Amber means blocking is enabled; green means it is disabled. The filters still decide which entities are affected. Turn the arrows off with **Direction guides** under Appearance → Display options.

## Editing together

Managers can edit different settings at the same time. If someone changes the same setting while you are editing it, the tuner refreshes it and asks you to retry. A player, mob or item list is saved as one setting, so two edits to the same list cannot be combined automatically.
