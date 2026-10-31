package dev.zsskayr.merlins_inferno.worldgen.elysium.core;

/**
 * Which material goes where, per biome. Pure (no Minecraft types). OWNED BY THE ART DIRECTOR; the core calls these
 * methods from {@link ElysiumWorld#material}. Signatures are the contract; bodies are art.
 *
 * <p>{@code hash} is a deterministic per-block value in [0,1) the core passes in (for dithering/patches); {@code patch}
 * is a smooth low-frequency noise in [-1,1] at that column (for big patches without square edges).
 *
 * <p>Art rules encoded here (see docs/elysium/design-bible.md):
 * <ul>
 *   <li>Steep columns ({@link Column#slope}) lose their soil: cliffs read as rock walls with visible strata.</li>
 *   <li>Strata are bands by ABSOLUTE y, wobbled a few blocks by {@code patch} so they undulate along a cliff.</li>
 *   <li>Patches use {@code patch} thresholds, dithered only in a thin border band; column-level decisions use
 *       {@link #colHash} (stable for the whole column) so a column never flips grass/stone vertically.</li>
 *   <li>Gravity blocks (SAND, GRAVEL) only where at least 2 solid blocks lie below inside the column.</li>
 * </ul>
 * Slope is assumed to be "blocks of rise per block of run" (max neighbour height difference). Tune
 * {@link #ROCKY}/{@link #STEEP} if the core uses another scale.
 */
public final class ElysiumPalette {
    private ElysiumPalette() {
    }

    // ---------------------------------------------------------------- tuning

    /** Below this slope soil always stays. */
    static final double ROCKY = 0.9;
    /** Above this slope the skin is always bare rock. Between ROCKY and STEEP: dithered per column. */
    static final double STEEP = 1.6;
    /** Argent Peaks snow line (absolute y); matches vanilla freeze_top_layer with biome temperature 0.37. */
    static final int ARGENT_SNOW_LINE = 256;
    /** Frostcrest: below this y, valley floors may show grass (for spruces). */
    static final int FROSTCREST_VALLEY_Y = 205;

    // ---------------------------------------------------------------- strata

    /** A periodic stack of bands by absolute y. Immutable, shared, thread-safe. */
    static final class Strata {
        final Mat[] lut;
        final int wobble;

        Strata(int wobble, Object... bands) {
            int period = 0;
            for (int i = 1; i < bands.length; i += 2) period += (Integer) bands[i];
            lut = new Mat[period];
            int k = 0;
            for (int i = 0; i < bands.length; i += 2) {
                Mat m = (Mat) bands[i];
                int t = (Integer) bands[i + 1];
                for (int j = 0; j < t; j++) lut[k++] = m;
            }
            this.wobble = wobble;
        }

        Mat at(int y, double patch) {
            int yy = y + (int) Math.round(patch * wobble);
            return lut[Math.floorMod(yy, lut.length)];
        }
    }

    static final Strata S_LARK = new Strata(3,
            Mat.ARGANITE, 11, Mat.STONE, 4, Mat.ARGANITE, 7, Mat.DIORITE, 1, Mat.ARGANITE, 3, Mat.ANDESITE, 3);
    static final Strata S_STEPPE = new Strata(3,
            Mat.ARGANITE, 8, Mat.STONE, 4, Mat.LIGHT_GRAY_TERRACOTTA, 2, Mat.ARGANITE, 6, Mat.TERRACOTTA, 1, Mat.STONE, 3);
    static final Strata S_HIGHPINE = new Strata(3,
            Mat.STONE, 9, Mat.ANDESITE, 4, Mat.STONE, 5, Mat.TUFF, 2, Mat.ARGANITE, 6);
    static final Strata S_MOSSY = new Strata(3,
            Mat.CALCITE, 3, Mat.STONE, 6, Mat.DIORITE, 2, Mat.STONE, 5, Mat.CALCITE, 1);
    static final Strata S_ARGENT = new Strata(6,
            Mat.ARGANITE, 9, Mat.QUARTZITE, 2, Mat.ARGANITE, 6, Mat.CALCITE, 1, Mat.ARGANITE, 4, Mat.DIORITE, 1);
    static final Strata S_STONECROWN = new Strata(2,
            Mat.STONE, 8, Mat.ANDESITE, 4, Mat.STONE, 5, Mat.TUFF, 2, Mat.STONE, 3, Mat.ANDESITE, 3);
    static final Strata S_CASCADE = new Strata(3,
            Mat.STONE, 5, Mat.ORANGE_TERRACOTTA, 2, Mat.BROWN_TERRACOTTA, 2, Mat.STONE, 3, Mat.TERRACOTTA, 2,
            Mat.COARSE_DIRT, 1, Mat.STONE, 4, Mat.ORANGE_TERRACOTTA, 1, Mat.ANDESITE, 2);
    static final Strata S_MISTFALL = new Strata(3,
            Mat.STONE, 7, Mat.TUFF, 3, Mat.ANDESITE, 3, Mat.STONE, 5, Mat.TUFF, 2);
    static final Strata S_GLASS = new Strata(3,
            Mat.CALCITE, 3, Mat.ARGANITE, 9, Mat.DIORITE, 2, Mat.ARGANITE, 6, Mat.QUARTZITE, 1);
    static final Strata S_AZURE = new Strata(2,
            Mat.SANDSTONE, 5, Mat.ARGANITE, 8, Mat.CALCITE, 2, Mat.ARGANITE, 5);
    static final Strata S_RIME = new Strata(3,
            Mat.STONE, 7, Mat.DIORITE, 2, Mat.ARGANITE, 9, Mat.ANDESITE, 3);
    static final Strata S_FROSTCREST = new Strata(4,
            Mat.ANDESITE, 5, Mat.STONE, 7, Mat.DIORITE, 2, Mat.ARGANITE, 8, Mat.TUFF, 2);
    static final Strata S_CHAINFALL = new Strata(3,
            Mat.ARGANITE, 9, Mat.TUFF, 2, Mat.ANDESITE, 4, Mat.ARGANITE, 6, Mat.TUFF, 1, Mat.ANDESITE, 2);
    static final Strata S_UNDER = new Strata(4,
            Mat.ARGANITE, 12, Mat.TUFF, 2, Mat.ARGANITE, 8, Mat.ANDESITE, 1, Mat.ARGANITE, 5, Mat.ANDESITE, 2);
    /** The bulk of every island, under its biome's crust of rock. */
    static final Strata S_BODY = new Strata(4,
            Mat.ARGANITE, 13, Mat.STONE, 2, Mat.ARGANITE, 9, Mat.ANDESITE, 1, Mat.ARGANITE, 6, Mat.CALCITE, 1);

    static Strata strata(ElysiumBiome b) {
        return switch (b) {
            case LARK_MEADOWS -> S_LARK;
            case WINDLESS_STEPPE -> S_STEPPE;
            case HIGHPINE_FOREST -> S_HIGHPINE;
            case MOSSY_WEALD -> S_MOSSY;
            case ARGENT_PEAKS -> S_ARGENT;
            case STONECROWN_CLIFFS -> S_STONECROWN;
            case CASCADE_VALE -> S_CASCADE;
            case MISTFALL_GORGE -> S_MISTFALL;
            case GLASS_SEA -> S_GLASS;
            case AZURE_SHALLOWS -> S_AZURE;
            case RIME_EXPANSE -> S_RIME;
            case FROSTCREST_RANGE -> S_FROSTCREST;
            case CHAINFALL -> S_CHAINFALL;
            case UNDERHANG -> S_UNDER;
        };
    }

    // ---------------------------------------------------------------- helpers

    /** Stable per-column value in [0,1) (does not change with y). */
    static double colHash(Column c, int salt) {
        long h = c.x * 0x9E3779B97F4A7C15L ^ c.z * 0xC2B2AE3D27D4EB4FL ^ salt * 0x165667B19E3779F9L;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 32;
        return (h >>> 11) * 0x1.0p-53;
    }

    /** Soft threshold: true when v > t, with a dithered border band of half-width w (no straight edges). */
    static boolean over(double v, double t, double w, double dither) {
        if (v >= t + w) return true;
        if (v <= t - w) return false;
        return dither < (v - (t - w)) / (2 * w);
    }

    /** Is this column's skin bare rock? Per-biome slope thresholds, dithered per column. */
    static boolean bare(ElysiumBiome b, Column c) {
        double lo = ROCKY, hi = STEEP;
        switch (b) {
            case STONECROWN_CLIFFS, ARGENT_PEAKS, CHAINFALL -> { lo = 0.6; hi = 1.15; }
            case CASCADE_VALE, MISTFALL_GORGE, FROSTCREST_RANGE -> { lo = 0.75; hi = 1.35; }
            case LARK_MEADOWS, WINDLESS_STEPPE, RIME_EXPANSE -> { lo = 1.1; hi = 1.9; }
            default -> { }
        }
        if (c.slope >= hi) return true;
        if (c.slope <= lo) return false;
        return colHash(c, 11) < (c.slope - lo) / (hi - lo);
    }

    /** At least two solid blocks of this column lie below y: gravity blocks are safe here. */
    static boolean held(Column c, int y) {
        return y - 2 >= c.bottom;
    }

    static int snowLine(Column c, double patch) {
        return ARGENT_SNOW_LINE + (int) Math.round(patch * 6) + (int) Math.round((colHash(c, 3) - 0.5) * 8);
    }

    // ---------------------------------------------------------------- surface

    /** Block {@code depth} blocks below the column top (0 = the top block) within the surface skin (depth < c.surfaceDepth). */
    public static Mat surface(ElysiumBiome biome, Column c, int y, int depth, double hash, double patch) {
        double ch = colHash(c, 1);
        if (bare(biome, c)) {
            return bareSkin(biome, c, y, depth, hash, patch);
        }
        switch (biome) {
            case LARK_MEADOWS: {
                if (depth == 0) return Mat.GRASS_BLOCK;
                return Mat.DIRT;
            }
            case WINDLESS_STEPPE: {
                if (depth == 0) {
                    if (over(patch, 0.45, 0.08, ch)) return Mat.COARSE_DIRT;
                    if (patch < -0.7 && ch < 0.25 && held(c, y)) return Mat.GRAVEL; // loose stony spots
                    return Mat.GRASS_BLOCK;
                }
                return depth >= 2 && patch > 0.2 ? Mat.COARSE_DIRT : Mat.DIRT;
            }
            case HIGHPINE_FOREST: {
                if (depth == 0) {
                    if (over(patch, 0.25, 0.08, ch)) return Mat.PODZOL;
                    if (patch < -0.65) return Mat.COARSE_DIRT;
                    return Mat.GRASS_BLOCK;
                }
                return Mat.DIRT;
            }
            case MOSSY_WEALD: {
                if (depth == 0) {
                    return over(patch, 0.4, 0.08, ch) ? Mat.MOSS_BLOCK : Mat.GRASS_BLOCK;
                }
                return Mat.DIRT;
            }
            case ARGENT_PEAKS: {
                int line = snowLine(c, patch);
                if (y >= line) {
                    if (depth == 0 && c.slope < 0.5 && y > line + 12 && over(patch, 0.55, 0.06, ch)) return Mat.POWDER_SNOW;
                    if (depth <= 1) return Mat.SNOW_BLOCK;
                    return stone(biome, c, y, hash, patch);
                }
                if (depth == 0) {
                    if (c.slope > 0.5 && patch < -0.3 && held(c, y)) return Mat.GRAVEL; // scree on rocky slopes
                    if (y > line - 14 && ch < 0.35) return Mat.STONE;                  // thinning soil under the snow
                    return Mat.GRASS_BLOCK;
                }
                return depth == 1 ? Mat.DIRT : stone(biome, c, y, hash, patch);
            }
            case STONECROWN_CLIFFS: {
                if (depth == 0) return over(patch, 0.55, 0.08, ch) ? Mat.COARSE_DIRT : Mat.GRASS_BLOCK;
                return depth <= 2 ? Mat.DIRT : stone(biome, c, y, hash, patch);
            }
            case CASCADE_VALE: {
                if (depth == 0) return over(patch, 0.6, 0.08, ch) ? Mat.COARSE_DIRT : Mat.GRASS_BLOCK;
                return depth >= 2 && patch < 0 ? Mat.COARSE_DIRT : Mat.DIRT;
            }
            case MISTFALL_GORGE: {
                if (depth == 0) {
                    if (over(patch, 0.2, 0.08, ch)) return Mat.MOSS_BLOCK;
                    if (patch < -0.6) return Mat.ROOTED_DIRT;
                    return Mat.GRASS_BLOCK;
                }
                return Mat.DIRT;
            }
            case GLASS_SEA: {
                if (depth == 0) {
                    if (c.inland < 6 || !over(patch, -0.55, 0.08, ch)) return Mat.CALCITE; // pale rims and outcrops
                    return Mat.GRASS_BLOCK;
                }
                return c.inland < 6 ? Mat.CALCITE : Mat.DIRT;
            }
            case AZURE_SHALLOWS: {
                boolean sandy = c.inland < 40 || c.riverDist < 12 || over(patch, 0.35, 0.08, ch);
                if (sandy) {
                    if (depth <= 2 && held(c, y)) return Mat.SAND;
                    return Mat.SANDSTONE;
                }
                return depth == 0 ? Mat.GRASS_BLOCK : (depth <= 2 ? Mat.DIRT : Mat.SANDSTONE);
            }
            case RIME_EXPANSE: {
                // freeze_top_layer lays snow on everything; grass patches let sparse spruces root.
                if (depth == 0) return over(patch, -0.25, 0.08, ch) ? Mat.SNOW_BLOCK : Mat.GRASS_BLOCK;
                if (depth == 1 && patch > -0.25) return Mat.SNOW_BLOCK;
                return Mat.DIRT;
            }
            case FROSTCREST_RANGE: {
                if (depth == 0) {
                    if (over(patch, 0.75, 0.05, ch)) return Mat.BLUE_ICE;
                    if (over(patch, 0.5, 0.06, ch)) return Mat.PACKED_ICE;
                    if (y < FROSTCREST_VALLEY_Y && !over(patch, -0.3, 0.08, ch)) return Mat.GRASS_BLOCK;
                    return Mat.SNOW_BLOCK;
                }
                if (depth == 1) return patch > 0.5 ? Mat.PACKED_ICE : (y < FROSTCREST_VALLEY_Y && patch < -0.3 ? Mat.DIRT : Mat.SNOW_BLOCK);
                return stone(biome, c, y, hash, patch);
            }
            case CHAINFALL: {
                if (depth == 0) {
                    if (over(patch, 0.2, 0.1, ch)) return Mat.GRASS_BLOCK;
                    if (patch < -0.4) return Mat.TUFF;
                    return patch < 0 ? Mat.ANDESITE : Mat.ARGANITE;
                }
                return patch > 0.2 && depth == 1 ? Mat.DIRT : stone(biome, c, y, hash, patch);
            }
            case UNDERHANG:
            default:
                return stone(biome, c, y, hash, patch);
        }
    }

    /** Skin of a steep column: rock strata, with biome dressing (moss walls, ice streaks, terracotta). */
    static Mat bareSkin(ElysiumBiome biome, Column c, int y, int depth, double hash, double patch) {
        double ch = colHash(c, 2);
        switch (biome) {
            case MOSSY_WEALD:
                if (over(patch, 0.4, 0.1, ch)) return Mat.MOSS_BLOCK;
                if (patch > 0.15 && hash < 0.35) return Mat.MOSSY_COBBLESTONE;
                break;
            case MISTFALL_GORGE:
                if (over(patch, 0.3, 0.1, ch)) return Mat.MOSS_BLOCK;   // green walls
                break;
            case FROSTCREST_RANGE:
                if (over(patch, 0.6, 0.08, ch)) return Mat.PACKED_ICE;  // frozen falls on cliffs
                break;
            case HIGHPINE_FOREST:
                if (depth == 0 && over(patch, 0.5, 0.1, ch)) return Mat.MOSS_BLOCK;
                break;
            default:
                break;
        }
        return stone(biome, c, y, hash, patch);
    }

    // ---------------------------------------------------------------- body

    /** Body of the island under the skin: strata by absolute y, so cliffs read as layers. */
    public static Mat stone(ElysiumBiome biome, Column c, int y, double hash, double patch) {
        int below = c.top - y;
        // Soft soil/rock border: 0..2 extra blocks of dirt under the skin on gentle, soil biomes.
        if (below >= c.surfaceDepth && below < c.surfaceDepth + 2 && hasSoil(biome) && !bare(biome, c)) {
            if (hash < 0.55 - 0.25 * (below - c.surfaceDepth)) {
                return biome == ElysiumBiome.CASCADE_VALE || biome == ElysiumBiome.WINDLESS_STEPPE ? Mat.COARSE_DIRT : Mat.DIRT;
            }
        }
        if (biome == ElysiumBiome.MOSSY_WEALD && below < 10 && patch > 0.55 && hash < 0.3) {
            return Mat.MOSSY_COBBLESTONE;
        }
        return below >= crust(biome, patch) ? S_BODY.at(y, patch) : strata(biome).at(y, patch);
    }

    /** How deep a biome's own rock goes before the Arganite body starts; the pale biomes are mostly Arganite already. */
    static int crust(ElysiumBiome biome, double patch) {
        int base = switch (biome) {
            case CASCADE_VALE -> 18; // the terracotta bands are the point of that biome
            case STONECROWN_CLIFFS -> 11;
            case ARGENT_PEAKS, GLASS_SEA, AZURE_SHALLOWS, CHAINFALL, UNDERHANG -> 1000;
            default -> 6;
        };
        return base + (int) Math.round(2.5 * (patch + 1));
    }

    static boolean hasSoil(ElysiumBiome b) {
        return switch (b) {
            case LARK_MEADOWS, WINDLESS_STEPPE, HIGHPINE_FOREST, MOSSY_WEALD, CASCADE_VALE, MISTFALL_GORGE, RIME_EXPANSE -> true;
            default -> false;
        };
    }

    // ---------------------------------------------------------------- water

    /** Floor of rivers, lakes and seas, {@code depth} below the bed top. Never SAND/GRAVEL over air (core guarantees solid below). */
    public static Mat waterBed(ElysiumBiome biome, Column c, int y, int depth, double hash, double patch) {
        double ch = colHash(c, 4);
        boolean safe = held(c, y);
        switch (biome) {
            case GLASS_SEA:
                if (depth <= 2) {
                    if (depth == 0 && !over(patch, -0.5, 0.08, ch)) return Mat.CLAY;
                    if (depth == 0 && over(patch, 0.55, 0.08, ch)) return Mat.DIORITE;
                    return Mat.CALCITE;   // white bed = turquoise translucent water
                }
                break;
            case AZURE_SHALLOWS:
                if (depth <= 1 && safe) return Mat.SAND;
                if (depth <= 4) return Mat.SANDSTONE;
                break;
            case MOSSY_WEALD:
                if (depth == 0) return over(patch, 0.0, 0.1, ch) ? Mat.CLAY : (safe ? Mat.GRAVEL : Mat.DIRT);
                if (depth <= 2) return Mat.DIRT;
                break;
            case MISTFALL_GORGE:
                if (depth == 0) return over(patch, 0.0, 0.1, ch) ? Mat.MUD : Mat.CLAY;
                if (depth <= 2) return Mat.DIRT;
                break;
            case HIGHPINE_FOREST:
                if (depth == 0) return over(patch, 0.1, 0.1, ch) && safe ? Mat.GRAVEL : Mat.COARSE_DIRT;
                if (depth <= 2) return Mat.DIRT;
                break;
            case RIME_EXPANSE:
            case FROSTCREST_RANGE:
                if (depth == 0) return over(patch, 0.3, 0.1, ch) ? Mat.CLAY : (safe ? Mat.GRAVEL : Mat.STONE);
                if (depth == 1) return Mat.DIRT;
                break;
            case ARGENT_PEAKS:
                if (depth == 0) return over(patch, 0.0, 0.1, ch) ? Mat.CALCITE : (safe ? Mat.GRAVEL : Mat.ARGANITE);
                break;
            case CHAINFALL:
            case UNDERHANG:
                if (depth == 0) return safe && patch > 0 ? Mat.GRAVEL : Mat.TUFF;
                break;
            case CASCADE_VALE:
                if (depth == 0) {
                    if (!over(patch, -0.4, 0.08, ch)) return Mat.CLAY;
                    return safe ? Mat.GRAVEL : Mat.COARSE_DIRT;   // river gravel bars
                }
                if (depth <= 2) return Mat.COARSE_DIRT;
                break;
            default: // LARK_MEADOWS, WINDLESS_STEPPE, STONECROWN_CLIFFS
                if (depth == 0) {
                    if (over(patch, 0.45, 0.08, ch)) return Mat.CLAY;
                    return safe ? Mat.GRAVEL : Mat.DIRT;
                }
                if (depth <= 2) return Mat.DIRT;
                break;
        }
        return stone(biome, c, y, hash, patch);
    }

    /** Banks: columns within a few blocks of water whose top is at most 2 above the water. */
    public static Mat shore(ElysiumBiome biome, Column c, int y, int depth, double hash, double patch) {
        double ch = colHash(c, 5);
        boolean safe = held(c, y);
        switch (biome) {
            case GLASS_SEA:
                return depth <= 3 ? Mat.CALCITE : stone(biome, c, y, hash, patch);     // calcite beaches
            case AZURE_SHALLOWS:
                if (depth <= 2 && safe) return Mat.SAND;
                return Mat.SANDSTONE;
            case RIME_EXPANSE:
            case FROSTCREST_RANGE:
                if (depth == 0) return over(patch, 0.0, 0.1, ch) ? Mat.SNOW_BLOCK : (safe ? Mat.GRAVEL : Mat.STONE);
                return depth <= 2 ? Mat.DIRT : stone(biome, c, y, hash, patch);
            case MOSSY_WEALD:
                if (depth == 0) return over(patch, 0.1, 0.1, ch) ? Mat.MOSS_BLOCK : (safe ? Mat.GRAVEL : Mat.DIRT);
                return depth <= 2 ? Mat.DIRT : stone(biome, c, y, hash, patch);
            case MISTFALL_GORGE:
                if (depth == 0) return over(patch, 0.0, 0.1, ch) ? Mat.MOSS_BLOCK : Mat.ROOTED_DIRT;
                return depth <= 2 ? Mat.DIRT : stone(biome, c, y, hash, patch);
            case HIGHPINE_FOREST:
                if (depth == 0) return over(patch, 0.2, 0.1, ch) ? Mat.PODZOL : Mat.COARSE_DIRT;
                return depth <= 2 ? Mat.DIRT : stone(biome, c, y, hash, patch);
            case CASCADE_VALE:
                if (depth == 0) return over(patch, 0.2, 0.1, ch) ? Mat.GRASS_BLOCK : (safe ? Mat.GRAVEL : Mat.COARSE_DIRT);
                return depth <= 2 ? Mat.COARSE_DIRT : stone(biome, c, y, hash, patch);
            case ARGENT_PEAKS:
            case STONECROWN_CLIFFS:
            case CHAINFALL:
                if (depth == 0) return over(patch, 0.0, 0.1, ch) ? Mat.GRASS_BLOCK : (safe ? Mat.GRAVEL : Mat.STONE);
                return depth == 1 ? Mat.DIRT : stone(biome, c, y, hash, patch);
            case WINDLESS_STEPPE:
                if (depth == 0 && c.riverDist < 2.5 && safe) return Mat.SAND;
                if (depth == 0) return over(patch, 0.3, 0.1, ch) ? Mat.COARSE_DIRT : Mat.GRASS_BLOCK;
                return depth <= 2 ? Mat.DIRT : stone(biome, c, y, hash, patch);
            case UNDERHANG:
                return stone(biome, c, y, hash, patch);
            default: // LARK_MEADOWS
                if (depth == 0 && c.riverDist < 3 && safe) return over(patch, 0.0, 0.1, ch) ? Mat.GRAVEL : Mat.SAND; // pebble beach
                if (depth == 0) return over(patch, 0.6, 0.08, ch) ? Mat.CLAY : Mat.GRASS_BLOCK;
                return depth <= 2 ? Mat.DIRT : stone(biome, c, y, hash, patch);
        }
    }

    // ---------------------------------------------------------------- underside

    /** The underside cone and its stalactites; {@code fromBottom} = y - c.bottom. */
    public static Mat underside(ElysiumBiome biome, Column c, int y, int fromBottom, double hash, double patch) {
        // Stalactite tips go darker (andesite/tuff) so the underside reads shadowy but never black.
        if (fromBottom < 5) {
            double t = (5 - fromBottom) / 5.0;             // 1 at the tip
            if (hash < 0.45 * t) return hash < 0.15 * t ? Mat.TUFF : Mat.ANDESITE;
        }
        // Blobby (not vertical-stripe) accent patches: mix the column patch with a slow y wave.
        double blob = patch * 0.7 + 0.3 * Math.sin(y * 0.45 + patch * 3.0);
        switch (biome) {
            case RIME_EXPANSE:
            case FROSTCREST_RANGE:
                if (fromBottom < 8 && patch > 0.4) return Mat.PACKED_ICE;  // ice icicles
                break;
            case MOSSY_WEALD:
            case MISTFALL_GORGE:
            case CASCADE_VALE:
                if (blob > 0.5 && fromBottom > 3) return hash < 0.5 ? Mat.MOSS_BLOCK : Mat.MOSSY_COBBLESTONE;
                if (blob < -0.55 && fromBottom > 6) return Mat.ROOTED_DIRT;
                break;
            case GLASS_SEA:
            case AZURE_SHALLOWS:
            case ARGENT_PEAKS:
                if (blob > 0.5 && fromBottom > 2) return Mat.CALCITE;
                break;
            default:
                break;
        }
        return S_UNDER.at(y, patch);
    }

    /** Hanging dressing under the underside (roots, vines). Return AIR for none; the core picks the length. */
    public static Mat hang(ElysiumBiome biome, Column c, double hash, double patch) {
        double cluster = 0.5 + 0.5 * patch; // 0..1, clusters dressing instead of uniform noise
        if (hash < 0.012) return Mat.GLOW_LICHEN_CEIL; // rare light points everywhere
        double p;
        boolean vines;
        switch (biome) {
            case MOSSY_WEALD, MISTFALL_GORGE -> { p = 0.20; vines = patch < 0.1; }
            case CASCADE_VALE, HIGHPINE_FOREST -> { p = 0.14; vines = patch < -0.2; }
            case LARK_MEADOWS, WINDLESS_STEPPE, STONECROWN_CLIFFS -> { p = 0.07; vines = false; }
            case GLASS_SEA, AZURE_SHALLOWS -> { p = 0.06; vines = true; }
            case CHAINFALL -> { p = 0.09; vines = true; }
            case UNDERHANG -> { p = 0.12; vines = patch < 0; }
            default -> { p = 0.02; vines = false; } // snow + Argent: bare icy/rocky undersides
        }
        if (hash < 0.012 + p * cluster * 2) return vines ? Mat.CAVE_VINES : Mat.HANGING_ROOTS;
        return Mat.AIR;
    }
}
