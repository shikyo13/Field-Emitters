# Troubleshooting

## Emitters will not connect

- Posts connect north, south, east or west to the nearest compatible post in range. The default maximum range is 20 blocks.
- Check the height of the terrain between posts. The field can follow changes of up to four blocks per step.
- Posts need five clear blocks of height; Projection Towers need seven.
- Posts connect to posts and rails to rails. Rails must face each other.
- Both emitters must be loaded and have compatible ownership.

## The field will not turn on

1. Read the status in **Overview** and check the stored energy. The network needs enough energy for one tick of operation.
2. Make sure the connected cable or generator is supplying energy.
3. Check **Field: On** and **Turn on** in Overview. A network set to redstone ON needs a signal; one set to redstone OFF needs all its inputs off.
4. Check **Read redstone from**. The sensor output side cannot also read the control signal.

Switched-off emitters stay connected. You do not need a separate energy or redstone input on every rail.

## A player or mob passes through

- Check **Blocking**, the selected travel direction and any custom filters for that direction or connection.
- **Block selected** stops matches. **Allow selected only** lets matches through and stops everything else.
- **Skip owner: Yes** lets the owner through, including through a bridge.
- Access cards can allow passage. A player specifically named in a blocking blacklist is still stopped.
- Check enabled mob, item and player lists; they replace the general filters for their own category.
- Wait for the formation animation to finish before using the field as a barrier.

Sensor settings control detection, not blocking. Damage and inventory checkpoints have separate settings too.

## The field crosses plants, doors or snow

Plants in the field's path are stored while it is active and restored when it switches off or is removed. The field also blocks at occupied positions such as doors, gates, carpets and snow layers. You should not need to clear these by hand. Report a reproducible gap with your Minecraft version, loader, mod version and the block arrangement.

## I cannot change settings

The owner, invited managers and server operators can manage a private network. The owner can add managers under **Access → Field management**, or allow public management. An access card grants passage, not management permission. A locked server preset can also prevent changes.

## The sensor does not send a pulse

Use **Sensor → Pulse on crossing** and check its filter. The whole entity must pass through; touching the field does not count. Use **On while touching** to detect contact instead.

Connect your circuit to **Output side**. For connected rails, use the rail at the displayed **Redstone output at** coordinates. Another crossing during a pulse extends it without turning the output off and on again.

## The field looks dark or invisible

In **Appearance**, check **Show forcefield**, **Animate field pattern** and **Light nearby blocks**. Hiding the field or disabling its light does not turn off its blocking, detection or damage.
