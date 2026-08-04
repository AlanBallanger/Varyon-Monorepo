# EasyHunger

A survival mod for Hytale that adds **Hunger** and **Thirst** systems, requiring players to manage their food and water intake to survive.

## Features

### Hunger System
- **Hunger Bar**: Visual HUD showing current hunger level
- **Hunger Depletion**: Decreases over time, influenced by stamina usage
- **Food Consumption**: Eat food items to restore hunger
- **Starvation**: Take damage when hunger reaches zero
- **Status Effects**: Visual/audio effects when hungry or starving

### Thirst System (NEW)
- **Thirst Bar**: Separate HUD for thirst level
- **Thirst Depletion**: Decreases over time, faster when sprinting
- **Water Consumption**: Drink water to restore thirst
- **Dehydration**: Take damage when thirst reaches zero
- **Toggle Option**: Can be disabled via config (`ThirstEnabled: false`)

### Additional Features
- **Safe Zones**: Hunger/thirst paused in protected areas (SimpleClaims integration)
- **Dynamic Food Values**: Configure individual food restoration amounts
- **Configurable Max Values**: Adjust MaxHunger and MaxThirst
- **Block Break Cost**: Lose hunger when breaking blocks
- **Jump Cost**: Lose hunger when jumping
- **HUD Positioning**: Multiple position options

## Configuration

Config file: `mods/Haas_EasyHunger/EasyHungerConfig.json`

### Hunger Settings
| Option | Default | Description |
|--------|---------|-------------|
| `MaxHunger` | 50 | Maximum hunger level |
| `StarvationTickRate` | 2.0 | Seconds between hunger ticks |
| `StarvationPerTick` | 0.04 | Hunger lost per tick |
| `StarvationStaminaModifier` | 0.175 | Extra decay when using stamina |
| `HungryThreshold` | 20.0 | Level for "hungry" status effects |
| `StarvationDamage` | 5.0 | Damage when starving |
| `BlockBreakHungerCost` | 0.005 | Hunger lost breaking blocks |
| `JumpHungerCost` | 0.01 | Hunger lost jumping |

### Thirst Settings
| Option | Default | Description |
|--------|---------|-------------|
| `ThirstEnabled` | true | Enable/disable thirst system |
| `MaxThirst` | 50 | Maximum thirst level |
| `ThirstDecayRate` | 0.05 | Thirst lost per tick |
| `SprintThirstMultiplier` | 1.5 | Extra decay when sprinting |
| `ThirstyThreshold` | 20.0 | Level for dehydration effects |
| `ThirstDamage` | 5.0 | Damage when dehydrated |

### Food Values
Configure individual food restoration amounts in the `FoodValues` map.

## Commands
- `/sethunger <player> <amount>` - Set player hunger level
- `/setthirst <player> <amount>` - Set player thirst level

## Compatibility
- Works with [MultipleHUD](https://www.curseforge.com/hytale/mods/multiplehud)
- Compatible with SimpleClaims for safe zone detection
- Single-player and multiplayer support

## Credits & Attribution

This mod is based on **Hungry** by [Aex12](https://www.curseforge.com/hytale/mods/hungry), licensed under AGPLv3.

### Original Hungry Mod Contributors
- [trouble-dev](https://github.com/trouble-dev): UI guides and reference plugins
- [Darkhax](https://github.com/Darkhax): Spellbook mod reference
- [oskarscot](https://github.com/oskarscot): Hytale ECS Basics guide
- [Buuz135](https://github.com/Buuz135): MultipleHUD mod
- [ItsNeil17](https://github.com/ItsNeil17): HytaleModding guides
- [Santoniche](https://opengameart.org/users/santoniche): Chicken leg icon
- The HytaleModding community

### EasyHunger Additions by Haas
- Complete Thirst system
- Safe zone integration
- Dynamic food values config
- Extended configuration options
- Performance optimizations
- Block break and jump hunger costs

## License

This project is licensed under the **GNU Affero General Public License v3.0 (AGPLv3)**.

See [LICENSE](LICENSE) for details.
