# El'ysium core: notes from the terrain-core agent

Package `worldgen/elysium/core` (pure Java, no Minecraft). Tools: `tools/elysium/Preview.java`, `tools/elysium/Validator.java`.

## Build and run
```
javac -d /home/claude/work/corebuild $(find src/main/java/dev/zsskayr/merlins_inferno/worldgen/elysium/core tools/elysium -name '*.java')
java -Xmx4g -cp /home/claude/work/corebuild Validator 1 424242 -7310925581   # exits 1 on any violation
java -Djava.awt.headless=true -cp /home/claude/work/corebuild Preview 1 /home/claude/work/renders
```

## Algorithm
1. **Layout (stage A, `ElysiumWorld.buildA`)**: the plane is split into `SUPER` = 1900-block super cells. Each one holds either
   one LARGE/GIANT island (70% where the climate allows both layouts) or a 2x2 quad of SMALL/MEDIUM islands. Every island
   stays inside its (sub)cell with `CELL_MARGIN` = 30, so islands never overlap and the gap is at least 60. Climate is
   (temperature, humidity, ruggedness), very low-frequency fbm (scales 7000/5500/3600). Each biome has an anchor point in
   that climate space (`ANCHORS`). An island takes the nearest anchor among the biomes whose min/max size allows the island's
   size. A SMALL island falls back to MEDIUM when no small biome is near. GIANT is Lark Meadows only.
   Island index = packed (cell x 12 bits, cell z 12 bits, slot 7 bits). `ElysiumWorld.island(index)` / `plan(index)` decode it.
2. **Island plan (`IslandPlan`)**: outline = elliptical (aspect 0.72..1, random rotation), domain-warped, with radial
   angular noise. `inland(x,z)` is the scaled radial distance to the coast (> 0 on land). Relief per biome is in
   `relief()`: terraced hills, spire fields (sharp needles), peak cones with a ridged overlay (Argent up to y ~350,
   Frostcrest a lower ridge of 5–8 peaks), Stonecrown terrace levels 13 blocks apart made of 5x5 offset "basalt columns",
   and plateaus that Cascade and Mistfall cut into. Seas: Glass Sea is a cliff rim with a deep basin and islets.
   Azure Shallows is a rim ring with a 1–5 deep lagoon, reefs and sand bars. Coasts are vertical cliffs because the top
   never ramps down to the edge.
3. **Water model**: a column holds water when its top is below its water level W. W is the sea level (the whole of a sea
   island), a lake level (lake bowl), or a river terrace level (wet tube); otherwise there is none. Anywhere two W meet,
   a bank at least as high as the higher level is built into the heightfield: lake rims `max(level+1)`, river
   embankments `L+1 - 0.8*(e-2)`, sea rims. River terraces step down by 1 at a time. Spill columns are water columns
   with a void neighbour; they only appear at river or notch mouths, which taper to half-width 1.25 (2–3 blocks).
4. **Rivers (`River`)**: a slow-turning walk (4-block steps, turn rate <= 0.09 rad per step) from a head pool inland to
   12 blocks past the coast. VALLEY rivers take their level from the ground (head pool = ring minimum - 1, then down
   1 block at most every 2 vertices when the ground goes lower). CANYON (Cascade) and GORGE (Mistfall) follow a staircase
   profile (stepped falls, 1 block per vertex). NOTCH = the straight 2–3 wide spill channels through the sea rims (Azure 3–5,
   Glass Sea 1–2). Lookups use a 32-block bucket index of segments.
5. **Underside**: `thick = rim + (cone - rim) * t^0.72` with noise, ridged ribs, a central tip and stalactites (12-block
   cells). Cone depth = min(30 + 7.5% of the width, (baseY - 88) / 1.5), clamped to bottom >= 80. Hang length is 1..6
   (the palette picks the material).
6. **Spans (stage B)**: island pairs in this cell and in the +x / +z / diagonal cells. A pair with facing coasts at most 300 apart
   becomes a bridge (55%): VINE_CHAIN if Mossy Weald or Chainfall is involved, ROPE between meadow/steppe/pine islands,
   else STONE_ARCH. Pairs 300–1500 apart (50%) get two GIANT_CHAINs offset 9 blocks to each side, anchored 8–16 below
   the cliff top, with sag limited so they stay above y 105. Chainfall platforms are placed every ~210 blocks, alternating
   28–68 blocks to each side of the line, and joined by VINE_CHAIN hops <= 260 (80%). A pair is skipped when a third
   island blocks the line or the span crosses one already accepted in the cell. Deck y = top block of a dry anchor column
   3–18 blocks inland.
7. **Stage C**: per super cell, the list of every island and platform whose bbox touches it. `sample()` scans only that list.
8. **Springs and sites** are computed lazily per island (volatile cache).

9. **Biome regions**: a land island has 1 to 6 seeds (`planRegions`), as many as a slow noise and its size allow: its own biome plus its nearest climate neighbours, never a sea and never cold next to warm. A column belongs to the nearest seed after a domain warp, and the relief is a weighted mix of every region within `REGION_BLEND`, so borders are ramps. Sea islands keep one biome.
10. **Coast**: `coastFall` brings tall relief down to a low lip over 20 + 0.85 x height blocks, in rough ledges. Sea rims and platforms are exempt.
11. **Hanging chains**: `IslandPlan.computeChains`, drawn by `SpanBuilder.buildHanging` with the giant chain's links. Tied to one island only.
12. **Cloud layer**: `cloudTop` / `cloudTone` give a 2-block layer of `CloudSeaBlock` in every column. It is the only cloud sea: the old drawn sheets were dropped because shader packs discard them.

13. **Glass Sea**: comes in several scales. Ordinary cells pick it like any biome, at MEDIUM or LARGE. Colossal ones, 4200 wide (three large islands), come from a scatter: each `MEGA` x `MEGA` block of super cells offers one candidate, every row of blocks slides by its own hash, the candidate strays up to `SEA_JITTER` cells, and one closer than `SEA_MIN_CELLS` to a higher-priority neighbour is dropped (`seaOfBlock`). That averages about 14 km apart with no lines. A sea covers the 3x3 cells around its owner and its crossings reach `MEGA_REACH` cells, so `spansIn`, `buildB` and `buildC` also look at colossal owners that far away.

14. **Layout and shapes**: a super cell is open sky (`EMPTY_CELL_CHANCE`), one LARGE/GIANT island, or `splitCell`'s 2 to 4 uneven rectangles, each holding a SMALL/MEDIUM island that can stray along the free side of its rectangle or be left empty (`EMPTY_PART_CHANCE`). Each island draws an `IslandPlan.Shape` (round, oval, peanut, trefoil, kidney, crescent, teardrop) that scales the coast radius; it never exceeds the radius, so `extent` still bounds the island, and the underside code is untouched.

15. **Rock**: no strata. Every island is a calcite crust (`ElysiumPalette.CRUST`, wobbled by `patch`) over Arganite, with veins from ore features: andesite through Arganite, quartzite through both. The rock columns of `design-bible.md` are out of date.

16. **Spawn islets**: `ElysiumWorld.buildS` (one per super cell, stage S) puts a small Lark Meadows island (radius `SPAWN_ISLET_R`, no rivers, sites, springs or chains) beside each land island that has room, skipping seas and cold biomes, in a gap clear of other bodies and spans, and wholly inside the cell so cells need not see each other's islets. A flat `STONE_ARCH` joins each to its host, about 50 blocks long. They are planned from stages A and B and merged into `spansIn` and stage C. `nearestSpawnIslet` finds one for a block. Travel (`ElysiumDimension`) is Nether-like at `SCALE` = 70: a way in goes to the portal nearest its scaled coordinates, else to the nearest islet. A portal other than the one you arrived by opens at its coordinates x 70 outside, raising a frame if none is near. Key-lit portals are recorded in `ElysiumPortals`, so they are found again. Terrain already generated in a save stays as it is, so old worlds see only a floating plinth.

## Tunables
`EMPTY_CELL_CHANCE`, `EMPTY_PART_CHANCE`, `MEGA`, `SEA_WIDTH`, `SEA_JITTER`, `SEA_MIN_CELLS`, `REGION_BLEND`, `ISLET_LEVEL`, `SUPER`, `CELL_MARGIN`, `MAX_BRIDGE_GAP`, `MAX_CHAIN_GAP` (ElysiumWorld); the climate anchors; the single/quad (0.7)
and small (0.45) probabilities in `buildA`; per-biome constants in `IslandPlan.relief`, `planRivers`, `planLakes`;
`coneDepth` / `rim` in the IslandPlan constructor.

## Known limitations
- Gaps between islands range from 60 to about 900 blocks (single large islands in their 1900 cells leave wide gaps).
  GIANT islands are capped at about 1530 wide by the cell.
- There are many Chainfall platforms (about 5 per island); reduce `count` in `chainCrossing` if that is too busy.
- Span crossings are checked only within one super cell, so spans owned by different cells can occasionally cross.
- Springs and river mouths spill water off cliffs into the void; the MC side must handle where that water ends up (the world bottom).
- `slope` is computed only for the Argent, Frostcrest and Stonecrown biomes and the canyon/gorge walls; it is 0 elsewhere.
