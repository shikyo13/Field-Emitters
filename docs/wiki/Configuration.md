# Energy and configuration

## Energy

Every emitter and rail accepts Forge Energy on any side from cables, generators and energy cells made by other mods. Connected emitters pool their stored energy and draw it together, so one input can power a whole perimeter or rail chain.

| Setting | Default | Meaning |
| --- | --- | --- |
| `energyPerCellTick` | 2 | FE consumed per projected field block each tick while a field runs. |
| `emitterCapacity` | 100000 | FE stored by each emitter and rail. |
| `emitterTransferRate` | 10000 | Maximum FE per tick accepted from cables and generators. |

A link between two posts costs (distance − 1) × 5 × `energyPerCellTick` FE/t. A twenty-block side costs 190 FE/t and a 20 by 16 rectangle costs 680 FE/t with default settings. A rail span costs (distance + 1) × `energyPerCellTick` FE/t per strip.

A field starts when the pool holds enough energy for one tick and stops when it runs dry. The Power tab shows stored energy, demand and whether the field is running.

## Files

Start the game or server once to generate the configuration files, then close it before editing.

| File | Controls |
| --- | --- |
| `<world>/serverconfig/fieldemitters-server.toml` | Energy cost, capacity and transfer rate, plus the testing switch below. |
| `config/fieldemitters-client.toml` | Whether direction guides are shown while holding the tuner. |

In multiplayer the server's settings apply to everyone.

## Testing switch

`demoRedstonePower = true` lets a redstone signal supply unlimited energy. It exists for testing builds and showcase worlds and is off by default. The Power tab says when it is active.
