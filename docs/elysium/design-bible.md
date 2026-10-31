# El'ysium — design bible (art director)

Source of truth for look & feel. Data lives in `core/ElysiumPalette.java` (blocks), `worldgen/biome/<id>.json`
(colours, vegetation, mobs) and `textures/environment/cloud_sea*.png`. Hex colours below are the values in the JSONs.

## 1. Mood
- **High, diffuse noon light.** Fixed time 6000. Pale blue sky, never deep saturated blue; fog is a light haze
  (`#CFE3F7`-ish) so distant islands fade into the sky, not into grey.
- **A bottomless sky without a dark abyss.** A dense white cloud sea at y 64 replaces the void. Islands hang well above
  it (surfaces y 170–230). The only shadow in the world is under the islands (Underhang).
- **Mist and light shafts** in forests and gorges (Mistfall Gorge: greyer fog + faint white_ash drift; Mossy Weald:
  green-grey haze). Shaders do the shafts; we supply the dense canopy and haze colour.
- **Contrast is the story:** dark, wet, overgrown stone ruins (stone/tuff/mossy cobble, vines, moss) vs. bright white
  architecture (porcelain, polished/tiled porcelain, quartz, quartzite, calcite, arganite bricks) standing in clean air
  above the clouds. Never mix the two in one build; a white building may sit *on* an overgrown ruin, not be one.
- **What to avoid** (bug shots): sand as a default surface, flat uniform light-sand slabs, water standing as walls or
  curtains, lone pillars of debris, perfectly straight patch edges, salt-and-pepper noise on surfaces.

## 2. Materials grammar (all biomes)
- Base stone is **arganite** (pale silvery). Strata are bands by **absolute y**, 1–12 blocks thick, wobbled ±2–6 by the
  smooth `patch` noise so they undulate along a cliff. Dominant band thick, accent bands 1–2 blocks.
- **Steep = rock.** Above a per-biome slope threshold the soil skin disappears (dithered per column, not per block), so
  cliffs read as rock walls with strata. Rocky biomes (Stonecrown, Argent, Chainfall) go bare earlier.
- Soil biomes get 0–2 extra dithered dirt blocks under the skin so the soil/rock line is ragged, not ruled.
- Patches: `patch` thresholds with a thin dithered border (no square edges, no salt-and-pepper).
- Gravity blocks (sand, gravel) only with ≥2 solid blocks below inside the column. Sand appears **only** in Azure
  Shallows (depth 0–2 over sandstone) — nowhere else.

## 3. Biomes
Layer notation: top / sub / stone strata (bottom→top order of the repeating band) — accents — vegetation.

| Biome | Top / sub | Stone strata | Accents | Vegetation |
|---|---|---|---|---|
| Lark Meadows | grass / dirt | arganite 11, stone 4, arganite 7, diorite 1, arganite 3, andesite 3 | clay spots on banks, gravel/clay beds | meadow + plains flowers (yellow/white), sunflowers, tall grass, isolated oaks |
| Windless Steppe | grass + coarse-dirt patches, rare gravel / dirt+coarse | arganite 8, stone 4, light-grey terracotta 2, arganite 6, terracotta 1, stone 3 | mossy boulders (forest_rock) | tall dry grass, savanna grass, dead bushes |
| Highpine Forest | grass + podzol patches, coarse dirt / dirt | stone 9, andesite 4, stone 5, tuff 2, arganite 6 | moss tops on steep ledges, boulders | old-growth spruce, large ferns, taiga grass, berries |
| Mossy Weald | moss / podzol / rooted dirt patches over grass | stone 8, andesite 3, tuff 2, stone 5, andesite 2 | moss + mossy cobble on cliffs, mud/clay pond beds | dark oaks (dense canopy), ferns, mushrooms, vines, lily pads |
| Argent Peaks | grass → (thin soil) → snow block, powder snow pockets above ~y 268 | arganite 9, quartzite 2, arganite 6, calcite 1, arganite 4, diorite 1 (wobble 6 = veins) | gravel scree on rocky slopes, snow line y≈256 ±7 dithered | grove spruces on low slopes only (they cannot root on snow) |
| Stonecrown Cliffs | grass + coarse dirt / dirt 2 | stone 8, andesite 4, stone 5, tuff 2, stone 3, andesite 3 | vertical bare columns (low slope threshold), vines | sparse oaks, grass, flowers |
| Cascade Vale | grass + coarse dirt / dirt, coarse dirt | stone 5, orange terracotta 2, brown terracotta 2, stone 3, terracotta 2, coarse dirt 1, stone 4, orange terracotta 1, andesite 2 | gravel bars + clay in river | birch & oak, forest grass, flowers, sugar cane, vines |
| Mistfall Gorge | moss / grass / rooted dirt | stone 7, tuff 3, andesite 3, stone 5, tuff 2 | moss-covered walls (steep + patch), mud/clay bed | swamp oaks with vines, ferns, jungle grass, lily pads |
| Glass Sea | calcite rims (inland<6) + grass | calcite 3, arganite 9, diorite 2, arganite 6, quartzite 1 | calcite beaches, calcite/clay/diorite sea bed (white bed = turquoise water) | seagrass, sea pickles, sparse oaks on islets |
| Azure Shallows | sand (coast, rivers, patches) over sandstone; grass inland | sandstone 5, arganite 8, calcite 2, arganite 5 | sand beds over sandstone | warm-ocean corals, seagrass, sea pickles, sugar cane |
| Rime Expanse | snow block / grass patches (both under freeze snow) | stone 7, diorite 2, arganite 9, andesite 3 | ice spikes (crystals), ice patches, frozen lakes | sparse spruces only on the grass patches |
| Frostcrest Range | snow; packed ice / blue ice patches; grass in valleys < y 205 | andesite 5, stone 7, diorite 2, arganite 8, tuff 2 | packed-ice streaks on cliffs (frozen falls) | grove spruces in valleys |
| Chainfall | grass patches / bare arganite, andesite, tuff | arganite 9, tuff 2, andesite 4, arganite 6, tuff 1, andesite 2 | vines on cliffs/chains | sparse grass |
| Underhang | — | arganite 12, tuff 2, arganite 8, deepslate 1, arganite 5, andesite 2 | tuff/deepslate stalactite tips; moss/rooted dirt (forest islands), packed ice (snow islands), calcite (sea/peak islands) | hanging roots, cave vines (glow berries), glow lichen; sparse vanilla glow lichen / vines |

### Colours (biome JSON)
| Biome | sky | fog | water / water fog | grass / foliage | temp | music |
|---|---|---|---|---|---|---|
| Lark Meadows | 84BDF5 | CFE3F7 | 3F9FE4 / 0B3A5C | 7FD24A / 5FBF3A (vivid) | 0.8 | meadow |
| Windless Steppe | 93C2EC | DCE4E6 | 4A9FCB / 0E3A55 | B5C25A / A0AE48 (yellowish) | 0.9 | — |
| Highpine Forest | 7FB2E8 | C4D6E4 | 3D7FC0 / 082F4A | 4F8F3E / 3A6B30 (dark) | 0.45 | old_growth_taiga |
| Mossy Weald | 86B4DA | B9CDBF (green haze) | 3C7F7A / 0A2E2A | 5C9E3C / 4E8A2E | 0.7 | forest |
| Argent Peaks | 8CC6FA | E4EEF8 | 3F8FE0 / 0B3557 | 86B87A / 5E8C5A (cool) | 0.37 | jagged_peaks |
| Stonecrown Cliffs | 86BCF2 | D2E2F0 | 3F9FE4 / 0B3A5C | 77C04A / 5AA33A | 0.6 | stony_peaks |
| Cascade Vale | 8ABEEA | D4E0E8 | 2FA7C9 / 093B4D | 7CBF48 / 64A83C | 0.7 | flower_forest |
| Mistfall Gorge | 9DB4C8 (overcast) | A9B6BF (dense grey) | 3A8C9A / 0C2C33 | 5FA33F / 4F9435 | 0.65 | lush_caves |
| Glass Sea | 7EC4F5 | D0EAF5 | 3FE0D8 turquoise / 1E8C9A | 7DC75A / 5FB040 | 0.8 | — |
| Azure Shallows | 78C2F8 | D6EEF8 | 43D5EE / 1FA3C2 | 8BCB5A / 6CC043 | 0.95 | — |
| Rime Expanse | A9CCEB | E6EEF5 | 3D6FD6 / 050533 | 80B497 / 60A17B | 0.0 (snow) | snowy_slopes |
| Frostcrest Range | 9CC8F2 | E0EAF4 | 3D57D6 / 050533 | 7FAE8F / 5E9873 | 0.0 (snow) | frozen_peaks |
| Chainfall | 8BB9E3 | C9D3DC | 3F8FD0 / 0B3557 | 8CB060 / 6E9A48 | 0.6 | — |
| Underhang | 7D8C9A | 6E7680 (dark, not black) | 3A6F8F / 0A1E2A | 6A9050 / 587F40 | 0.6 | dripstone_caves |

**Temperature note:** vanilla lowers biome temperature by 0.00125 per block above y 80 and `freeze_top_layer` snows
where it drops below 0.15. Non-snow biomes are warm enough to stay green up to ~y 280+; Argent Peaks is 0.37 so vanilla
snow starts at ≈ y 256, matching the palette snow line. Do not lower these without re-checking the snow line.

## 4. Cloud sea
- Client-rendered stacked layers, top at y 64. `cloud_sea.png`: 256² seamless, ~94 % opaque soft white → light
  blue-grey billows lit from one side, ~2 % small rounded gaps (gaps show the next layer, never the void).
  `cloud_sea_puffs.png`: ~23 % coverage of soft puffs for the topmost layer, drifting slower/faster than the base.
- Tint toward the fog colour with distance; vertex colour slightly warmer on top layer. No vanilla clouds at all.
- Generator: `/home/claude/work/gen_cloud_sea.py` (FFT-filtered noise, periodic by construction).

## 5. Underside (Underhang)
- Cone/stalactite base proportional to island width. Arganite body, darker tuff/deepslate at the tips; per-island
  accents (moss under forests, packed-ice icicles under snow islands, calcite under seas/peaks).
- Hang dressing in clusters (driven by `patch`): hanging roots under grassland, cave vines (glow berries) under
  forests/seas/Chainfall, rare glow-lichen points everywhere (≈1 %), almost nothing under snow/Argent.
- The only shadowy place: readable silhouettes, glow berries and lichen as the light source. No hostile spawns.

## 6. Waterfalls
- Narrow (1–3 wide) falls from notched river lips or cliff springs; many thin falls beat one wide one (refs 4, 16, 18).
- Falls end in the cloud sea (no splash basin needed); mid-cliff springs on Stonecrown/Cascade.

## 7. Bridges & chains (structures agent)
- **Stone arch** (refs 6, 18): stone bricks / mossy stone bricks / cracked + tuff bricks, deep arches with short
  stalactite drips under each pier, overgrown with vines & moss carpet. Belongs to the *dark ruin* family.
- **White span** (ref 3): arganite bricks + polished porcelain deck, quartz/porcelain balusters, curved colonnade.
- **Rope** (refs 1, 16): spruce/dark-oak plank deck, fence/chain rails, sagging, few lanterns.
- **Vine chain** (ref 1): iron chains with vines and glow-berry cave vines hanging, sag 6–15.
- **Giant chain** (Chainfall): oversized links in iron/dark-grey (smooth basalt or polished deepslate + chain blocks),
  moss and hanging vines on the lower half.

## 8. Reference images (what each teaches) — /home/claude/refs
- `14ee07…` pixel-art spire castle over glowing cloud sea — silhouette: spire + stalactite base, dark mass on bright clouds.
- `151571…` jungle islands tied by chains/rope bridges, hanging vines, haze — vine-chain bridges, mist depth.
- `2015-03-27…` one island: spruce ring, twin snowy peaks, lake, ONE narrow edge waterfall — Highpine/Argent prototype.
- `305a8a…` curved white colonnade over a cloud sea — white architecture; how opaque the cloud sea must be.
- `4771fb…` columnar rock spires, many thin waterfalls, stalactite undersides — Stonecrown Cliffs.
- `48687a…` grass cliff edge above a sea of blocky clouds — cloud-sea density; grass-over-stone rim.
- `5f98c7…` dark stone aqueduct with dripping stalactites, overcast — stone-arch bridge, dark ruin family.
- `6ae4ed…` ruined colonnade on an islet, vines, brown/orange underside — overgrown ruins, warm earth underside.
- `6e991a…` snowy jagged peaks dropping into turquoise water — Frostcrest/Glass Sea colour contrast, sheer walls.
- `7ec7ef…` lush islands with round canopy trees, falls into ponds — Mossy Weald / Cascade, stacked heights.
- `8f5cfc…` terraced green valley with a big misty waterfall — Cascade Vale terraces, Mistfall mist.
- `992d94…` overgrown industrial ruins with rails/cranes and vines — "industrial ruins with vines".
- `9b60d6…` narrow gorge, turquoise pool, orange/brown strata, dense green walls — Mistfall Gorge / Cascade strata.
- `c54e5e…` ice-blue palace with mirror floor and chandeliers — mirror-floor hall (white/ice architecture).
- `c670a4…` overcast valley, castle ruin, lake, mossy steps — dark overgrown ruin palette.
- `c75228…` rolling meadow, spruce groups, pond, yellow flowers, dramatic sky — Lark Meadows / Highpine edge.
- `c75466…` white observatory on a snowy island, light shafts, thin falls, rope bridges — white arch + light shafts.
- `d65c12…` giant blue ice crystal on a snow plain — Rime Expanse ice crystals.
- `download (1).png` dark castle city, arched bridges, dozens of thin falls — bridge network, thin waterfalls.
- `f88a73…` tall-grass plain, spruces, grey stone cliffs with grass ledges — Windless Steppe / Stonecrown ledges.
- `fbe2d8…` white snowy spired castle — white architecture palette (quartz/porcelain/calcite).
