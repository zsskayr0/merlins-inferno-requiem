# El'ysium structures (structures agent)

Package `dev.zsskayr.merlins_inferno.worldgen.elysium.structure`. Procedural, code-only (no NBT templates, no jigsaw,
no biome JSON). The chunk generator calls two entry points after vanilla biome decoration:

```java
SpanBuilder.build(WorldGenLevel level, ChunkPos chunk, ElysiumWorld world, List<ElysiumWorld.Span> spans)
SiteBuilder.build(WorldGenLevel level, ChunkPos chunk, ElysiumWorld world, ElysiumWorld.Site site)
```

## Files
| File | Role |
|---|---|
| `SpanBuilder` | public entry: bridges (STONE_ARCH, ROPE, VINE_CHAIN) and GIANT_CHAIN, drawn one chunk at a time |
| `SiteBuilder` | public entry: dispatch on `site.kind()`; unknown kinds do nothing |
| `Ruins` | `overgrown_ruin`, `mossy_castle`, `overgrown_works` |
| `WhiteArchitecture` | `white_colonnade`, `spire_chapel` |
| `Brush` | clipped world access: `set`, `put` (only into air/plants), `shaped` (set + mark for post-processing), `clear` (never next to water), `ground`, `foundation`, `carpet`, `wallVines`, `hangingVines`, `chest` |
| `SiteCtx` | a site's local frame (u forward, v right) over a Brush: `plinth`, `footing` (stilts), `line`, `noise`... |
| `Frame` | rotation: `rot` 0..3 = `Direction.from2DDataValue` (S, W, N, E) is "forward" (+u) |
| `Shapes` | pure geometry: `catenary`, `hump`, `LineFrame` (along/across a segment) |
| `Rng` | SplitMix64: sequential stream (sites) and stateless position hashes (spans) |
| `Pal` | every block state used, in one place |

## Contract details
- **Spans**: each chunk draws only its own 16x16 columns. Every block is a pure function of the span and the column
  (`Rng.unit(salt(span), x, k, z)`), so chunks join seamlessly in any order. A span's salt is a hash of its geometry.
  - Anchors `(ax, ay, az)` / `(bx, by, bz)` are the **deck block** at each coast (walking surface = y + 1).
    The deck runs on `SpanBuilder.ABUT` = 6 blocks into each island as a flat abutment on a foundation (max 10 deep),
    with 3 blocks of headroom carved through terrain (never next to water; sand/gravel left hanging is turned to
    sandstone/stone).
  - `width` (walkway) defaults: STONE_ARCH 5, ROPE 3, VINE_CHAIN 3. Rails are outside the walkway.
  - `sag`: ROPE / VINE_CHAIN / GIANT_CHAIN hang in a catenary (`Shapes.catenary`, k = 2) of that depth; STONE_ARCH
    ignores sag and rises instead (hump = min(6, 1.5 + 0.035 len)). Sags are clamped so nothing reaches y 80.
  - GIANT_CHAIN anchors are under the cliffs; the chain is extended 6 blocks into each island (only air is filled).
- **Sites**: built once, from the chunk containing (x, z). Writes are clipped to chunk +-16 and a 28-block disc around
  the origin. Everything is designed within ~16-19 blocks of the origin, so a site near a chunk edge only loses a sliver
  at worst (see risks). The ground is read per column from `WORLD_SURFACE_WG`, skipping plants, leaves and logs; a
  column whose first thing below the surface is water counts as no ground.
  - Floors never float: a floor column is built only if the ground is at most 6 below it (4 for ruins); otherwise that
    floor block is left out (ruins: the floor is broken there; colonnade: the coast simply ends).
  - Buildings (chapel) use `footing`: solid quartzite fill down to 6 deep; on wall lines and a 3x3 grid, pillars
    continue down to the ground (up to 32) so a chapel on a slope stands on stilts.
  - Loose pieces (fallen columns, slabs, rubble, rails) stand on the ground of their own column.
- Plants: vines are only placed with a face against a block that can hold them (vanilla `isAcceptableNeighbour`), or as
  hanging strands under a sturdy-bottomed block (first block UP + one side, the rest the same side: the vanilla "vine
  above holds this face" rule). Moss carpets only on a sturdy top face. No water is ever placed.
- Walls, fences, iron bars, panes and stairs are written with `Brush.shaped`, which marks them for post-processing:
  when the chunk becomes full the game runs `updateShape` on them, so connections are right even across chunk borders.

## What they look like
- **STONE_ARCH**: 5-wide Arganite-brick walkway with half-step slabs, raised Arganite curbs with Arganite-brick wall
  railings and a lantern every 16 blocks; a gentle hump. Under it a solid barrel arch with an elliptical intrados
  (springing up to 18 below the deck at the cliffs), alternating Arganite / brick voussoirs, a keystone at the crown,
  vines hanging from the faces.
- **ROPE**: 3-wide spruce slab deck (some oak, a few missing planks) in a catenary; stripped-spruce log stringers with
  spruce fences, taller posts every 6, log posts with lanterns at both ends; log crossbeams underneath; vine drapes.
- **VINE_CHAIN**: 3-wide mossy stone-brick / mossy cobble / Arganite slab deck in a catenary with moss carpets;
  polished-blackstone stringers, vertical chain hangers every 3 blocks and horizontal chain handrails; long vines.
- **GIANT_CHAIN**: 9-block diamond links, 2 thick, alternating planes, overlapping by a quarter so each threads the
  next; polished blackstone (every 7th link polished blackstone bricks); moss carpets on top, vines trailing below.
- **overgrown_ruin** (r 6-9): cracked/mossy stone-brick floor with holes, ring of 7-10 columns (chiseled base;
  ~35% whole with capitals and lintels between neighbours, the rest snapped with drums lying in the grass), a fallen
  slab lintel, ivy, moss carpets; 50% a chest with `minecraft:chests/ruined_portal`.
- **mossy_castle**: keep corner: two 2-thick walls (12-16 and 10-14 long, 10-16 high) meeting at a round tower stump
  (7-11 high, cobble floor, a door into the yard); merlons on the outer row, window slits, a doorway; walls crumble
  raggedly towards their ends; ivy on both faces, moss, azalea; three rubble cones.
- **overgrown_works**: rusted copper frame 9x7 (oxidized/weathered cut copper posts and beams, a broken top beam with
  its slabs on the ground, iron-bar bracing, copper grate patches, hanging chains), a crane mast 6 above the frame with a
  9-12 jib, iron-bar tie, counterweight and a long hook chain; a 17-block rail line on a tuff/andesite bed following
  the terrain (ascending rails on 1-block steps, rusted gaps); vines and moss everywhere.
- **white_colonnade**: on the coast, facing `rot` (outward). 9-15 columns (polished porcelain base, quartz pillar
  shaft, chiseled quartz capital, 7 tall) on an arc of radius 10-13 sweeping +-55..75 degrees, convex side to the
  clouds; smooth-quartz architrave + polished-porcelain slab cornice; tiled porcelain floor with polished rings, raised
  one block on calcite, a ring of polished porcelain steps outside; hanging lanterns between columns; end-rod pedestal.
- **spire_chapel**: 15x9 hall facing `rot`: polished porcelain walls, quartz pilasters, tall white glass windows,
  gabled porcelain-stair roof with a quartz ridge, arched door with steps; 3-5 spires (3x3 calcite shafts with porcelain
  bands and slits, quartz-stair taper, quartz pillar, end-rod tip; front pair taller; a central spire on the ridge
  for 3 or 5); mirror floor = white glass aisle over sea lanterns, polished porcelain field, tiled border; quartz
  clusters, chained lanterns, quartz altar with a lyrium cluster and two end rods.

## Block list
Arganite, Arganite bricks (+ slab, stairs*, wall), Arganite slab; stone bricks / mossy / cracked / chiseled, mossy &
plain stone-brick slabs (stairs*, mossy wall*), cobblestone, mossy cobblestone (+ slabs), stone, sandstone (gravity
fix only), tuff bricks, polished andesite; polished blackstone, polished blackstone bricks; oxidized/weathered copper,
oxidized/weathered cut copper (+ slabs), oxidized copper grate, iron bars, iron block, rail, chain; spruce/oak slabs,
spruce fence, stripped spruce log; porcelain stairs, polished porcelain (+ slab, stairs), tiled porcelain, quartzite,
quartz cluster, lyrium cluster; quartz pillar, quartz bricks, chiseled quartz, smooth quartz, quartz slab & stairs,
calcite, white stained glass (+ pane), sea lantern, end rod; lantern, moss block, moss carpet, vine, azalea and
flowering azalea leaves (persistent), chest. (* declared in `Pal`, currently unused.)

## Open risks / notes
- Site writes into neighbouring chunks happen before those chunks run their own vanilla decoration, so a vanilla tree
  can later grow into a ruin (same as vanilla structures). Trees standing in a footprint are cut where we clear.
- Clip: a site whose origin sits at a chunk edge can lose the outermost few blocks of the colonnade arc ends / crane
  tip (they reach ~16-19 from the origin; the guaranteed-safe radius is 16).
- Lyrium cluster on the chapel altar is placed with `placed=false` (natural), so it random-ticks its radiance.
- Bridges use the anchor y as the deck block; if the core means "walking surface" instead, decks sit one block high
  (one-line change in `Deck.surface`).
- `blocksMotion()` is deprecated in 1.21.1 (warning only; vanilla still uses it).
