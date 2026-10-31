# El'ysium rework — integration notes (MC side)

Owner: integration agent. Everything here is written against the fixed core signatures in
`worldgen/elysium/core/{ElysiumWorld,Column,Mat,ElysiumBiome}.java`.

## How it fits together

```
ServerAboutToStartEvent ─► ElysiumSeed.set(worldGenOptions().seed())      (fallback: ServerLifecycleHooks.getCurrentServer())
                           ElysiumSeed.world() ─► ElysiumWorlds.get(seed) (one ElysiumWorld per seed, shared by all threads)

dimension/elysium.json ─► ElysiumChunkGenerator (codec "merlins_inferno:elysium", field biome_source)
                            createBiomes   : chunk.fillBiomesFromNoise(quart -> world.biomeAt(4q+2, ...))
                            fillFromNoise  : 256x world.sample(); blocks written straight into LevelChunkSections;
                                             OCEAN_FLOOR_WG / WORLD_SURFACE_WG updated; flowing water marked for post-processing
                            applyBiomeDecoration : vanilla biome features, then ElysiumDecorator
                          ElysiumBiomeSource (codec "merlins_inferno:elysium", RegistryOps.retrieveGetter(BIOME))
ElysiumDecorator ─► SpanBuilder.build / SiteBuilder.build (structures agent), Springs, sparse underside glow lichen
client: ElysiumSkyEffects (effects "merlins_inferno:elysium") ─► ElysiumCloudSea (3 drifting sheets at y 46/56/64)
```

### Chunk fill rules (ElysiumChunkGenerator.stateAt — also used by getBaseColumn/getBaseHeight)
- arch span `archBottom..archTop` -> `world.material(c, y)` (checked first, also over open sky)
- `!land` -> air
- `bottom..top` -> `world.material(c, y)` via `ElysiumBlocks.state(mat)` (a Mat.AIR material leaves air)
- `top+1..waterTop` -> water source; ice on the top block when `frozen`
- below `bottom` (`hangLength` blocks): `CAVE_VINES` = plant blocks with a fully grown (AGE 25) head at the tip, 12% with
  glow berries; `HANGING_ROOTS` and `GLOW_LICHEN_CEIL` = one block only; `AIR/WATER/MOSS_CARPET/SNOW_LAYER` = nothing
  (cannot hang); any other Mat = solid drip (stalactite) for the whole length.
- Water marked with `chunk.markPosForPostprocessing` (LevelChunk ticks those fluids when the chunk goes live): every block
  of a `spill` column, every block with a horizontal neighbour that is neither solid nor water per the core's shape, and the
  surface block where a neighbour column's `waterTop` differs. Neighbours across the chunk border are sampled lazily.
- `getSeaLevel()` = 0 (nothing uses it), `getMinY()` = 0, `getGenDepth()` = 384, carvers/surface = no-ops,
  `spawnOriginalMobs` = same as NoiseBasedChunkGenerator.

### Seed
`ElysiumSeed` captures the seed on `ServerAboutToStartEvent` (HIGHEST priority; fires before levels are created on both
dedicated and integrated servers), forgets it on `ServerStoppedEvent` (so a second single-player world gets its own seed),
and if asked before the event it reads `ServerLifecycleHooks.getCurrentServer()`. `/reload` does not recreate levels or
change the seed, so nothing is needed there. Outside any server it returns 0 and logs a warning once.

## Hooks called (structures agent provides these, exact signatures)
```java
dev.zsskayr.merlins_inferno.worldgen.elysium.structure.SpanBuilder.build(
        net.minecraft.world.level.WorldGenLevel level, net.minecraft.world.level.ChunkPos chunk,
        dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumWorld world,
        java.util.List<dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumWorld.Span> spans)
// called once per chunk with world.spansIn(chunkMinX, chunkMinZ, chunkMaxX, chunkMaxZ) when non-empty

dev.zsskayr.merlins_inferno.worldgen.elysium.structure.SiteBuilder.build(
        WorldGenLevel level, ChunkPos chunk, ElysiumWorld world, ElysiumWorld.Site site)
// called for each site of world.sitesIn(chunk rectangle); may write within ±16 of the chunk
```
Order inside `ElysiumDecorator.decorate`: spans, sites, springs, lichen. A RuntimeException thrown by a builder is caught
and logged once (worldgen keeps going).

## What other agents must provide
- **core**: bodies of `sample`, `material` (including inside the arch span), `biomeAt`, `spansIn`, `springsIn`, `sitesIn`.
  `biomeAt` is called 1536x per chunk (every quart cell of 384 blocks) — keep it cheap / cached per column.
  If a new `Mat` is added, add it to `ElysiumBlocks.map` (unmapped Mats fall back to stone with an error log).
- **art director**: all 14 biome JSONs (`ElysiumBiomeSource` resolves every `ElysiumBiome.id` eagerly with `getOrThrow`;
  a missing one fails world load), `assets/merlins_inferno/textures/environment/cloud_sea.png` (tiling, 256x256;
  must NOT be clamped — no `"clamp": true` mcmeta — UVs run past 1 and rely on GL_REPEAT).
  Note for biome temperatures: vanilla lowers temperature above y 80 by `(y-80)*0.05/40` (≈0.15 at y 200), which decides
  snow vs rain and water freezing on the islands.
- **structures**: `SiteBuilder` (only `SpanBuilder` exists at the time of writing).

## Files
Added
- `src/main/java/dev/zsskayr/merlins_inferno/worldgen/elysium/ElysiumChunkGenerator.java`
- `src/main/java/dev/zsskayr/merlins_inferno/worldgen/elysium/ElysiumBiomeSource.java`
- `src/main/java/dev/zsskayr/merlins_inferno/worldgen/elysium/ElysiumBlocks.java`
- `src/main/java/dev/zsskayr/merlins_inferno/worldgen/elysium/ElysiumDecorator.java`
- `src/main/java/dev/zsskayr/merlins_inferno/worldgen/elysium/ElysiumSeed.java`
- `src/main/java/dev/zsskayr/merlins_inferno/worldgen/elysium/ElysiumWorlds.java`
- `src/main/java/dev/zsskayr/merlins_inferno/registry/ModChunkGenerators.java` (CHUNK_GENERATOR + BIOME_SOURCE registers)
- `src/main/java/dev/zsskayr/merlins_inferno/client/elysium/ElysiumSkyEffects.java`
- `src/main/java/dev/zsskayr/merlins_inferno/client/elysium/ElysiumCloudSea.java`
- `src/main/java/dev/zsskayr/merlins_inferno/client/elysium/ElysiumClientEvents.java`
- `docs/elysium/integration-notes.md`

Changed
- `Merlins_inferno.java` — `ModDensityFunctions.register` -> `ModChunkGenerators.register`
- `registry/ModFeatures.java` — removed ELYSIUM_WATER/SPANS/EROSION/GROUNDWATER/RIVERS
- `portal/ElysiumDimension.java` — arrival search on the core (`land && inland >= 60 && !hasWater()`, arrival at top+1);
  if no land at all, the plinth is built at y 200 instead of y 0
- `gametest/ElysiumGameTests.java` — runs ElysiumChunkGenerator on bare ProtoChunks, checks each column's highest block
  equals the core's (≤0.5% mismatch) and chunk biomes equal `biomeAt` (0 mismatch); dumps `build/elysium-test/top.png`
- `data/merlins_inferno/dimension/elysium.json` — custom generator + biome source
- `data/merlins_inferno/dimension_type/elysium.json` — height/logical_height 384, effects `merlins_inferno:elysium`
- `assets/merlins_inferno/lang/en_us.json`, `pt_br.json` — removed the 6 old biome names

Deleted
- `registry/ModDensityFunctions.java`
- `worldgen/elysium/ElysiumField.java`, `ElysiumTerrain.java`, `ElysiumTerrains.java`, `ElysiumSpans.java`
- `worldgen/feature/ElysiumErosionFeature.java`, `ElysiumGroundwaterFeature.java`, `ElysiumRiverFeature.java`,
  `ElysiumSpansFeature.java`, `ElysiumWaterFeature.java`
- `data/merlins_inferno/worldgen/configured_feature/elysium_{erosion,groundwater,rivers,spans,water}.json`
- `data/merlins_inferno/worldgen/placed_feature/elysium_{erosion,groundwater,rivers,spans,water}.json`
- `data/merlins_inferno/worldgen/noise_settings/elysium.json`, `data/merlins_inferno/worldgen/noise/elysium_seed.json`
- `data/merlins_inferno/worldgen/biome/{silent_fields,silverwood,silver_sea,salt_waste,mute_peaks,weeping_plains}.json`

Kept: `placed_feature/elysium_ore_*.json`, `elysium_lyrium_geode.json` (+ their configured features). No tags, advancements,
Patchouli pages or datagen providers referenced the removed biomes/features (grepped).

## Compile check done here
`javac` of all new/changed Java files + core + structure package, against the NeoForge 21.1.249 jar, the old mod classes,
and hand-written stubs of DFU (`MapCodec`, `Codec`, `RecordCodecBuilder`, `Products`, kinds), slf4j/LogUtils, the event
bus annotations, FML `EventBusSubscriber`, dist markers, JOML `Matrix4f`, Gson: **0 errors**. `Merlins_inferno.java` and
`ModFeatures.java` were not compiled (one-line change / pure deletion).

## Verify on Windows
1. `gradlew build` — especially the two `RecordCodecBuilder.mapCodec(...)` codecs (checked only against DFU stubs) and the
   `DeferredRegister<MapCodec<? extends ChunkGenerator/BiomeSource>>` holders in `ModChunkGenerators`.
2. `ElysiumClientEvents` and `ElysiumSeed` use `@EventBusSubscriber(modid = ...)` without `bus`, mixing a mod-bus event
   (`RegisterDimensionSpecialEffectsEvent`) with a game-bus one (`RenderLevelStageEvent`) like `ModEntityRenderers` already
   does; confirm the sky effects are registered (F3 / no vanilla clouds in El'ysium).
3. Seed: create a world, enter El'ysium, leave to title, create a second world with another seed: islands must differ
   and match `/seed`. Also on a dedicated server. (Log at debug: "El'ysium seed set to ...".)
4. **Old saves**: the dimension height changed 256 -> 384 and the generator changed. Delete
   `<world>/dimensions/merlins_inferno/elysium/` in test worlds before entering.
5. Cloud sea: correct placement (top sheet at y 64, centred on the camera, not rotated/offset), drift, edge fade, no
   z-fighting with islands; with Graphics = Fabulous; with Clouds = OFF (drawn from `RenderLevelStageEvent.AFTER_WEATHER`);
   texture tiling (repeat). Sodium/Embeddium/Iris may bypass `renderClouds` — check if those are in the modpack.
6. Sky: looking down from an island edge shows the cloud sea then bright fog colour, not black. (Vanilla only draws the
   dark lower disc below y 63, i.e. when falling under the clouds.)
7. Water: river terraces turn into short flowing slopes, `spill` lips into narrow waterfalls, springs flow; no water walls,
   no runaway floods at chunk borders. Frozen lakes keep ice on top.
8. Underhang: cave vines (body/head) survive block updates and do not keep growing; hanging roots stay; lichen sparse.
9. `/locate biome merlins_inferno:<id>` works (uses the biome source); F3 shows the "El'ysium island ..." lines.
10. Gametest `elysiumGeneratesFloatingIslands` (`gradlew runGameTestServer`) passes once the core bodies exist.
11. Feature-order cycle check across the 14 new biome JSONs (crash on world load if broken).
12. Water mobs: with sea level 0, vanilla cod/salmon/squid/dolphin spawn rules (which test y near sea level) will not
    spawn in island lakes. Decide whether that is wanted.
