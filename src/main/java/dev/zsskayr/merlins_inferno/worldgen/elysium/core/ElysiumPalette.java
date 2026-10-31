package dev.zsskayr.merlins_inferno.worldgen.elysium.core;

/**
 * Which material goes where, per biome. Pure (no Minecraft types). OWNED BY THE ART DIRECTOR; the core calls these
 * methods from {@link ElysiumWorld#material}. Signatures are the contract; bodies are art.
 *
 * <p>{@code hash} is a deterministic per-block value in [0,1) the core passes in (for dithering/patches); {@code patch}
 * is a smooth low-frequency noise in [-1,1] at that column (for big patches without square edges).
 */
public final class ElysiumPalette {
    private ElysiumPalette() {
    }

    /** Block {@code depth} blocks below the column top (0 = the top block) within the surface skin (depth < c.surfaceDepth). */
    public static Mat surface(ElysiumBiome biome, Column c, int y, int depth, double hash, double patch) {
        if (depth == 0) {
            return Mat.GRASS_BLOCK;
        }
        return Mat.DIRT;
    }

    /** Body of the island under the skin: strata by absolute y, so cliffs read as layers. */
    public static Mat stone(ElysiumBiome biome, Column c, int y, double hash, double patch) {
        return Mat.ARGANITE;
    }

    /** Floor of rivers, lakes and seas, {@code depth} below the bed top. Never SAND/GRAVEL over air (core guarantees solid below). */
    public static Mat waterBed(ElysiumBiome biome, Column c, int y, int depth, double hash, double patch) {
        return depth == 0 ? Mat.GRAVEL : Mat.DIRT;
    }

    /** Banks: columns within a few blocks of water whose top is at most 2 above the water. */
    public static Mat shore(ElysiumBiome biome, Column c, int y, int depth, double hash, double patch) {
        return depth == 0 ? Mat.GRASS_BLOCK : Mat.DIRT;
    }

    /** The underside cone and its stalactites; {@code fromBottom} = y - c.bottom. */
    public static Mat underside(ElysiumBiome biome, Column c, int y, int fromBottom, double hash, double patch) {
        return Mat.ARGANITE;
    }

    /** Hanging dressing under the underside (roots, vines). Return AIR for none; the core picks the length. */
    public static Mat hang(ElysiumBiome biome, Column c, double hash, double patch) {
        return hash < 0.08 ? Mat.HANGING_ROOTS : Mat.AIR;
    }
}
