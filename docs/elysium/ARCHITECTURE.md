# El'ysium rework — architecture contract (tech lead)

Branch: `elysium-rework`. Minecraft 1.21.1, NeoForge 21.1.249, Java 21, Mojang mappings (+ Parchment).
Mod id `merlins_inferno`, base package `dev.zsskayr.merlins_inferno`.

## Why the rework
The old El'ysium used the vanilla `minecraft:noise` generator (density function + surface rules + multi_noise biomes)
and then patched rivers, basin water, slashes and bridges in as features. Every visible bug in the handoff comes from
that split: features cleared up to an *analytic* top while the real surface (surface rules, dunes) was higher -> floating
sand, lone pillars; river level and basin level (fixed y 138) disagreed -> water walls; the biome chosen by multi_noise
disagreed with terrain weights -> deserts labelled Silver Sea; vanilla clouds at y~192 cut through islands.

## The one rule
**Everything placed by the chunk generator comes from one pure function** `core.ElysiumWorld` (seed, x, z) ->
`Column`. Terrain, water, materials, and biome are decided together, so they cannot disagree. The Minecraft layer only
copies what the core says into the chunk. The core has no Minecraft imports and is unit-tested and rendered offline.

```
core/ (pure Java, no MC)          worldgen/elysium/ (MC side)
  ElysiumWorld  ── sample() ──►     ElysiumChunkGenerator  (custom ChunkGenerator, codec "merlins_inferno:elysium")
  Column, Mat, ElysiumBiome ──►     ElysiumBiomeSource     (custom BiomeSource, codec "merlins_inferno:elysium")
  Island/Span/Spring/Site  ──►      ElysiumDecorator       (runs after vanilla biome decoration: springs, spans, underhang, sites)
                                    ElysiumBlocks          (Mat -> BlockState)
                                    ElysiumSeed            (world seed captured on ServerAboutToStartEvent)
client/                             ElysiumSkyEffects      (DimensionSpecialEffects: cloud sea at y 64, no vanilla clouds)
```

## World frame
- Dimension: `min_y 0`, `height 384`, `logical_height 384`, `fixed_time 6000`, skylight, no ceiling, effects `merlins_inferno:elysium`.
- Cloud sea: a *client-rendered* dense layer, top at y 64 (`ElysiumWorld.CLOUD_SEA_Y`). Nothing solid below y 80.
- Island surfaces mostly y 170–230. Mountains reach y ~360 (`HIGHEST_SOLID 370`).
- Islands: min 300x300. Size classes in `ElysiumBiome.Size`: SMALL 300–500, MEDIUM 500–900, LARGE 900–1500, GIANT 1500+ (very rare, "continents").
  Islands are explicit objects (center, width, biome, baseY) placed on a jittered grid, never overlapping, with
  cloud-sea gaps between them (60 to ~600 blocks).
- One island = one biome (12 surface biomes). Biomes are chosen per island from a low-frequency climate so neighbours
  make sense (snow next to peaks/forest, never snow next to Azure Shallows), constrained by the biome's allowed sizes.
- CHAINFALL: platforms (40–160 wide) strung along the long crossings where the giant chains hang; open cliffs, natural arches.
- UNDERHANG: 3D biome for everything under an island's surface skin (underside cone). Arganite stalactites, hanging roots, little light.
- Underside: cone/stalactite base proportional to width (bigger island = deeper cone), vegetation only on top or hanging.

## Water rules (the validator enforces these)
1. Water exists only where the core says (`Column.waterTop > top`). No feature ever places water except `Spring`s.
2. Every water block is held: each horizontal neighbour at the same y is solid or water, unless the column is `spill`
   (an intended waterfall lip). River terraces may step down by **1 block** between neighbours; the MC side marks those
   water blocks for post-processing so the game turns the step into a short flowing slope (no vertical water walls).
3. Waterfalls off island edges are **narrow** (1–3 blocks): a river is tapered to a notch at the lip, or a `Spring`
   on a cliff face. Never a full-width curtain.
4. Gravity blocks (SAND, GRAVEL) never have air directly below them; sand banks sit on sandstone/dirt.
5. Nothing floats except intended arches (`Column.arch*`) and built spans.

## Ownership (each agent edits ONLY its files; nobody commits — the tech lead commits)
| Agent | Owns |
|---|---|
| terrain-core | `worldgen/elysium/core/**` (except the public signatures in `ElysiumWorld.java`, `Column.java`), `tools/elysium/**` (preview, validator, renders), `docs/elysium/core-notes.md` |
| integration | `worldgen/elysium/*.java` (new MC-side classes), `client/elysium/**`, registration edits (`registry/*`, main mod class, client setup), `data/merlins_inferno/dimension/elysium.json`, `dimension_type/elysium.json`, removal of the old pipeline, `portal/ElysiumDimension.java`, `gametest/ElysiumGameTests.java` |
| art-director | `docs/elysium/design-bible.md`, `data/merlins_inferno/worldgen/biome/<12+2 new>.json`, `core/ElysiumPalette.java` (per-biome Mat layers, pure), `assets/merlins_inferno/textures/environment/cloud_sea.png`, lang entries for biomes |
| structures | `worldgen/elysium/structure/**` (procedural builders: ruins, white architecture, bridges/chains drawing), `docs/elysium/structures.md` |

Shared paths: `src/main/java/dev/zsskayr/merlins_inferno/` = `$J`; `src/main/resources/` = `$R`.

## Verification available
- Cloud workspace has JDK 21 but **no network to Maven/Mojang**: Gradle cannot run here. The core is compiled & tested with plain `javac`.
- MC API reference: `/home/claude/work/lib/neoforge-21.1.249.jar` (Minecraft + NeoForge classes, Mojang names) — use `javap -cp` to check every signature you call. Old compiled mod classes: `/home/claude/work/oldclasses/classes/java/main`.
- Vanilla worldgen data (placed/configured features, biomes): `/home/claude/work/vanilla/data/minecraft/worldgen/`. Vanilla textures: `/home/claude/work/vanilla/assets/minecraft/textures/`.
- The final `gradlew build` + in-game test runs on the owner's Windows PC.

## Feature-order rule (crash risk)
All biomes of one dimension must list shared placed features in the same relative order per step (else "Feature order
cycle" and chunk gen hangs). Never list the same feature twice in one step. Our own decorations do NOT go in biome JSON:
the chunk generator runs `ElysiumDecorator` after vanilla decoration.
