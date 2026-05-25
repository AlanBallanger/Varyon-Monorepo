# Comet_Raids_Redux

Source code: `https://github.com/FPSTordah/Comet-Raids-Redux`
Support Discord: `https://discord.gg/r5MBWdzWWW`

Ever wanted random events to spice up your Hytale gameplay? This mod adds falling comets that crash into your world, bringing waves of enemies to fight. Use the comet stone (interaction or break) to start the encounter - survive all waves and claim your rewards.

`Comet_Raids_Redux` is the actively maintained continuation of the original Comet Raids project.

Current release: **v4.0.0**

This mod is built for players who want a raid-like experience and server owners who want a customizable reward system. You can create custom themes, define multi-wave encounters, override loot tables per theme, and tweak every aspect of spawning and combat.

## Features

- **5 Comet Tiers** - Common, Rare, Epic, Legendary, and Mythic. Higher tiers = tougher fights, better loot.
- **Themed Waves** - Skeletons, goblins, spiders, trorks, outlanders, undead hordes... each comet picks a random theme (or you can force one).
- **Multi-Wave Combat** - Enemies spawn in waves. Clear one, the next begins. Rewards drop after the final wave.
- **Timed Reward Chest** - Final rewards spawn in a per-comet chest instance. 
- **Map Markers** - Comets show up on your map so you can track them down.
- **Zone + Tier Loot Model** - Rewards combine zone identity pools, current tier pools, and lower-tier inheritance weights.
- **Fully Configurable** - Spawn rates, enemy counts, loot tables, zone distributions, and scaling multipliers are all configurable.

## Reward Chest Behavior

When a comet encounter is cleared, the reward chest system works like this:

- **Comet stone blocks** (`Comet_Stone_*`): the block is replaced by the reward chest on the same spot.
- **Other registered comet blocks** (if you extend the mod): the chest may be placed in front; when the chest expires or is broken, the mod can remove the linked block too.
- Each comet creates its own chest instance with loot generated for that comet's tier/theme.

## Quick Start

### Requirements

- Endgame & QoL mod (`Config:Endgame&QoL`) for Tier 5/Mythic content (optional)

Tier 5/Mythic is the Endgame integration tier (Prisma/Onyxium and related Endgame materials), so it requires Endgame & QoL to be active.
If Endgame & QoL is missing, the mod still loads and Tier 5/Mythic is automatically disabled.

### Permissions (LuckPerms)

- `/comet spawn` -> `hytale.command.comet.spawn`
- `/comet test` -> `hytale.command.comet.test`
- `/comet zone` -> `hytale.command.comet.zone`
- `/comet destroyall` -> `hytale.command.comet.destroyall`
- `/comet reload` -> `hytale.command.comet.reload`

Recommended admin setup in LuckPerms:
- Grant all comet command nodes above to your admin group, or grant wildcard `hytale.command.comet.*` if your permissions setup supports wildcards.

### Build

- **JDK 21+** sur la machine
- **`libs/HytaleServer.jar`** (dossier `libs/` gitignoré)

```bat
gradlew build
```

→ **`build/libs/Varyon_Comet-4.0.0.jar`**

Config minimale : `settings.gradle.kts` (nom du projet), `build.gradle.kts` (Java + dépendances + chemins sources). Le **Gradle wrapper** (`gradlew`, `gradlew.bat`, `gradle/wrapper/`) sert seulement à lancer Gradle sans l’installer.

### Deploy

Copie `Varyon_Comet-4.0.0.jar` depuis `build/libs/` vers les mods du serveur.

## Comet Ownership

By default, each comet is "owned" by the player it spawned for. Only that player can see the map marker and trigger the encounter by breaking the comet block. Other players can't interact with it.

If you want **any player** to be able to trigger **any comet** (useful for multiplayer servers), set `"globalComets": true` in the config. When enabled:
- All players see all comet markers on the map
- Any player can break and trigger any comet

## When Do Comets Spawn?

Comets spawn naturally based on these default settings:
- **Spawn interval**: Every 2-5 minutes (120-300 seconds)
- **Spawn chance**: 40% chance each time the interval triggers
- **Spawn distance**: 30-50 blocks away from a player
- **Despawn**: Unclaimed common-tier comets despawn after 30 minutes

The tier of comet that spawns depends on `zoneSpawnChances` (`0..3` keys in config), including Mythic probabilities.

## Commands

### Main Commands

| Command | Description |
|---------|-------------|
| `/comet spawn` | Spawns a Common-tier comet near you |
| `/comet spawn --tier Rare` | Spawns a specific tier (Common, Rare, Epic, Legendary, Mythic) |
| `/comet spawn --theme Skeleton` | Spawns with a specific theme |
| `/comet spawn --tier Legendary --theme Void` | Combine tier and theme |
| `/comet spawn --onme true` | Spawns comet directly above you (for testing) |
| `/comet test` | Simulates automatic zone-based comet spawn for your location |
| `/comet zone` | Shows your current zone and comet tier distribution |
| `/comet destroyall` | Removes all active comet blocks in the world |
| `/comet reload` | Reloads the config from file |

### Spawn Command Examples

```
/comet spawn
/comet spawn --tier Legendary
/comet spawn --tier Epic --theme Trork
/comet spawn --theme Undead
/comet spawn --tier Rare --theme Spider
/comet spawn --onme true --tier Legendary
```

### More Fixed Spawn Examples

These are still valid as JSON examples, but command-based editing is currently disabled:

```json
{
  "spawns": [
    {
      "x": 100,
      "y": 64,
      "z": 200,
      "name": "Town Square",
      "enabled": true,
      "cooldownSeconds": 300,
      "tier": "Epic",
      "theme": "Trork Warband"
    }
  ]
}
```

### Available Themes

> Tier 1 = Common, Tier 2 = Rare, Tier 3 = Epic, Tier 4 = Legendary, Tier 5 = Mythic

- `Skeleton` - Skeleton Horde (Tier 1-2)
- `Goblin` - Goblin Gang (Tier 1-2)
- `Spider` - Spider Swarm (Tier 1-2)
- `Trork` - Trork Warband (Tier 1-3)
- `Skeleton_Sand` - Sand Skeleton Legion (Tier 1-3)
- `Sabertooth` - Sabertooth Pack (Tier 1-3)
- `Void` - Voidspawn (Tier 1-3)
- `Outlander` - Outlander Cult (Tier 2-5)
- `Leopard` - Snow Leopard Pride (Tier 2-5)
- `Skeleton_Burnt` - Burnt Legion (Tier 3-5)
- `Ice` - Legendary Ice (Tier 3-5)
- `Lava` - Legendary Lava (Tier 3-5)
- `Earth` - Legendary Earth (Tier 3-5)
- `Undead` / `Undead_Legendary` - Undead variants (Tier 1-5 depending on theme)
- `Zombie` - Zombie Aberration (Tier 3-5)
- `Frostbound_Pack` - Frostbound Pack (Tier 2-5)
- `Ashen_Vanguard` - Ashen Vanguard (Tier 3-5)
- `Dune_Stalkers` - Dune Stalkers (Tier 2-4)
- `Void_Reavers` - Void Reavers (Tier 2-4)
- `Plague_Horde` - Plague Horde (Tier 2-5)
- `Trork_Siege` - Trork Siege (Tier 2-4)
- `Ember_Hunters` - Ember Hunters (Tier 3-5)
- `Crystal_Marauders` - Crystal Marauders (Tier 3-5)
- `Wraithborn_Legion` - Wraithborn Legion (Tier 3-5)
- `Predator_Clan` - Predator Clan (Tier 1-4)

## Configuration

Settings are split across two files (all generated on first run if missing):

- `config.json` - main configuration: spawn behavior, zone chances, tier settings, reward tables, inheritance, WorldProtect rules, messages
- `cometMobs.json` - top-level **`cometMobs`** object: definitions per id (monster groups, optional per-theme **rewardOverride**)

### Message placeholders

Chat and banner messages in `config.json` under `"messages"` support placeholders. Full list and message keys are in the in-game Voile doc (Comet Raids Redux guide). Quick reference:

| Placeholder | Description |
|-------------|-------------|
| `%tier%` | Comet tier (Common, Rare, Mythic, etc.) |
| `%x%`, `%y%`, `%z%` | Comet block coordinates |
| `%currentWave%`, `%totalWaves%` | Wave progress |
| `%theme%` | Theme display name |
| `%bossStatus%` | Alive / Defeated |
| `%killed%`, `%total%` | Mobs killed / total this wave |
| `%time%` | Remaining time |

Message keys: `msgCometFallingTitle`, `msgCometFallingSubtitle`, `msgCometFallingChatCoords`, `msgWaveBossTitle`, `msgWaveBossTitleNoCount`, `msgWaveBossSubtitle`, `msgWaveTitle`, `msgWaveTitleNoCount`, `msgWaveSubtitle`, `msgWaveFailedTitle`, `msgWaveFailedSubtitle`, `msgWaveCompleteTitle`, `msgWaveCompleteSubtitle`, `msgWaveCompleteChatHeaderPrefix`, `msgWaveCompleteChatHeader`, `msgWaveCompleteChatItemPrefix`.

## Packaging As One JAR

Gradle produit un seul JAR : `build/libs/Varyon_Comet-4.0.0.jar` (`gradlew clean build`).

Le JAR contient notamment `Common/`, `Server/`, `manifest.json`.

Config files (`config.json`, `cometMobs.json`) are generated in the plugin directory on first run if missing.

Importer le projet comme projet Gradle dans l’IDE.

### Main Sections

**spawnSettings** - Controls natural comet spawning
```json
"spawnSettings": {
  "naturalSpawnsEnabled": true, // Set to false to disable random spawns (comets only via commands)
  "minDelaySeconds": 120,      // Minimum time between spawn attempts
  "maxDelaySeconds": 300,      // Maximum time between spawn attempts
  "spawnChance": 0.4,          // 40% chance to spawn when timer triggers
  "despawnTimeMinutes": 30.0,  // How long uncommon comets last before despawning
  "minSpawnDistance": 30,      // Minimum blocks from player
  "maxSpawnDistance": 50,      // Maximum blocks from player
  "disabledWorlds": [],        // World names where comet raids are disabled (case-insensitive)
  "globalComets": false,       // If true, any player can trigger any comet
  "disableWaveMobLoot": true  // If true, mobs spawned in waves drop no loot (default true)
}
```

> **Tip:** To disable random spawns, set `"naturalSpawnsEnabled": false`; use `/comet spawn` to spawn comets manually.

**claimProtect** - Generic claim-plugin spawn blocking
```json
"claimProtect": {
  "enabled": true,                     // If true, block natural comet spawns inside configured claim systems
  "autoDetectProviders": false,        // If true, auto-detect installed supported providers and ignore the providers list
  "providers": ["WorldProtect", "HyperFactions", "Hyfaction", "SimpleClaims", "WiFlowsClaims", "UltimateFaction", "ElbaphFactions"] // Case-insensitive provider names
}
```

Supported providers right now:
- `HyperFactions`
- `WorldProtect`
- `Hyfaction`
- `SimpleClaims`
- `WiFlowsClaims`
- `UltimateFaction`
- `ElbaphFactions`

**zoneSpawnChances** - Tier distribution per zone
```json
"zoneSpawnChances": {
  "0": { "tier1": 0.8, "tier2": 0.1, "tier3": 0.07, "tier4": 0.03, "tier5": 0.0 },
  "1": { "tier1": 0.6, "tier2": 0.2, "tier3": 0.13, "tier4": 0.07, "tier5": 0.0 }
}
```

**tierSettings** - Per-tier combat settings
```json
"tierSettings": {
  "1": {
    "timeoutSeconds": 90,    // How long before wave times out
    "minRadius": 3.0,        // Min spawn radius for enemies
    "maxRadius": 5.0         // Max spawn radius for enemies
  }
}
```

**rewardSettings** - Loot drops per tier
```json
"rewardSettings": {
  "1": {
    "drops": [
      {
        "id": "Ingredient_Bar_Copper",
        "minCount": 5,
        "maxCount": 7,
        "chance": 100,
        "displayName": "Copper Ingots"
      }
    ]
  }
}
```

**zoneBaseLootPools** - Base material identity per zone

**tierInheritanceWeights** - Lower-tier inclusion chances per active tier

Per-theme **rewardOverride** entries live in `cometMobs.json` when you want custom loot for a theme.

### Startup Validation

On load, the mod runs lightweight validation for:

- `config.json`

Validation emits actionable logs with this prefix:

- `[ConfigValidation][config.json]`

Errors do not hard-stop startup, but indicate config issues you should fix.

### Boot-Time JSON Sync

On startup, the mod now automatically:
- Creates missing `config.json` and `cometMobs.json`
- Merges/synchronizes missing config keys into existing files using current schema defaults

This keeps older config files forward-compatible after updates without manual key-by-key edits.

### Schema Notes (Tooling-Safe)

Some config objects intentionally include pseudo-comment keys for human guidance.
These keys are supported by the mod parser and should be treated as metadata by tools:

- Any key starting with `_` is metadata/comment-only (for example `_comment_multiwave`).
- Under `cometMobs`, non-theme entries should use `_` prefix so validators and editors can ignore them.
- Theme definitions should remain object values (`"theme_id": { ... }`), while pseudo-comments can be strings.

### Theme Configuration

Theme definitions (mobs, bosses, waves) go in `cometMobs.json`. Comets always place the tier `Comet_Stone_*` block. Per-theme reward overrides are optional entries in the same file (a **rewardOverride** object keyed by tier number).

Use `"naturalSpawn": false` on themes in `cometMobs.json` to prevent them from spawning naturally.

### Creating Custom Themes

Want to make your own encounters? Edit `cometMobs.json` (monster groups, optional **rewardOverride** per theme). You can:

- Define multiple waves with different enemy compositions
- Configure boss waves separately from normal waves
- Override the default loot table with custom rewards per tier
- Use `"naturalSpawn": false` to prevent a theme from spawning naturally while you test it

Optional HP/damage/scale/speed multipliers can be set per mob or boss entry in `cometMobs` (inline stat blocks on those entries only).

The config is fully JSON - just copy an existing theme, rename it, and start tweaking.

> **Tip:** To restrict a theme to command-spawn only (e.g. boss arenas), set `"naturalSpawn": false` on that theme so it never appears in random natural spawns.

## Version Notes

- `v4.0.0`: Reward chest after waves; configurable wave mob loot (`disableWaveMobLoot`); timed chest cleanup; multi-wave themes; loot overrides per theme.
- `v3.3.0`: Added configurable comet chat and banner messages with placeholders, plus an in-game Voile documentation page for configuration help.
- `v3.2.4`: Generic claim protection integration.
- `v3.2.3`: Improved wave timer reliability, fixed stuck wave progression in edge cases, and improved kill-count tracking compatibility with mob/combat overhaul mods.
- `manifest.json` controls runtime compatibility (`ServerVersion`).
- `pom.xml` controls compile dependency (`hytale.server.version`).
- Keep them aligned when updating to a new server release.

## Attribution

Special thanks to Frog for the original Comet Raids groundwork and for allowing continued development with free rein.

- Original repository: https://github.com/FrogCsLoL/Comet-Raids

## Usage & Distribution

This project is being maintained under MIT permission from the original owner.

## Credits

Original mod created by **Frog**.

Current continuation and maintenance: **Tordah**.

Ty to Pferd for balancing this Mod.


Some parts of this mod were made with AI assistance - mainly coding help and upscaling some visual assets like the mod icon.

---

Have fun getting obliterated by Legendary comets.
