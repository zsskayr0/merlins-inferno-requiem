# Merlin's Inferno: Requiem

> Survive the inferno. Conquer the unknown.

A NeoForge mod for Minecraft 1.21.1 by Zsskayr. *(Work in progress — `0.0.1-alpha`.)*

## Features

- **Hell Forge** – a multiblock forge with its own GUI for crafting infernal gear.
- **Lyrium** – glowing ore/cluster blocks and villager trades.
- **Rowanwood & Ashwood** – new woods with their own combat and woodworking mechanics.
- **Sanctified** – a status effect and progression system with particles and combat bonuses.
- **Demonblood & Druid's Touch** – special combat and interaction handlers.
- **Mobs** – Imp, Druid, and the **Dullahan** boss (with its steed and boss bar), using GeckoLib models.
- **Patchouli guide book** – in-game documentation (soft dependency).
- **Languages** – English (`en_us`) and Brazilian Portuguese (`pt_br`).

## Requirements

| Dependency | Version |
|------------|---------|
| Minecraft  | 1.21.1  |
| NeoForge   | 21.1.249+ |
| GeckoLib   | required |
| Patchouli  | optional (guide book) |
| Java       | 21 |

## Building

```bash
./gradlew build
```

The jar is written to `build/libs/`.

## Development

```bash
./gradlew runClient    # launch the game client
./gradlew runServer    # launch a dedicated server
./gradlew runData      # run data generators
```

## License

All Rights Reserved.
