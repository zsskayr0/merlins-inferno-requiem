package dev.zsskayr.merlins_inferno.worldgen.elysium.core;

import java.util.List;

/**
 * The whole of El'ysium as a pure function of the seed. NO Minecraft types in this package (tools compile it with plain javac).
 * Thread-safe: every method may be called concurrently from many chunk-gen threads. Cache per-island data lazily
 * (ConcurrentHashMap keyed by island cell), never per-call global state.
 *
 * THIS FILE IS THE CONTRACT between the core (terrain-core agent) and the Minecraft side (integration agent).
 * Signatures here must not change without the tech lead.
 */
public final class ElysiumWorld {
    public static final int MIN_Y = 0;
    public static final int HEIGHT = 384;            // dimension height: y 0..383
    public static final int CLOUD_SEA_Y = 64;        // top of the rendered cloud sea (client)
    public static final int LOWEST_SOLID = 80;       // nothing solid below this (keeps the cloud sea clear)
    public static final int HIGHEST_SOLID = 370;

    public final long seed;

    public ElysiumWorld(long seed) {
        this.seed = seed;
        // TODO(core)
    }

    /** Fill {@code out} for column (x, z). Must be fast: called 256x per chunk. */
    public void sample(int x, int z, Column out) {
        throw new UnsupportedOperationException("TODO(core)");
    }

    /** Solid material at height y of a sampled column (bottom <= y <= top, or inside the arch span). */
    public Mat material(Column c, int y) {
        throw new UnsupportedOperationException("TODO(core)");
    }

    /** Biome at block (x, y, z). Underside/interior below the surface layer => UNDERHANG; else the column's island biome. */
    public ElysiumBiome biomeAt(int x, int y, int z) {
        throw new UnsupportedOperationException("TODO(core)");
    }

    /** Islands whose bounding box touches the block rectangle [minX..maxX]x[minZ..maxZ]. */
    public List<Island> islandsIn(int minX, int minZ, int maxX, int maxZ) {
        throw new UnsupportedOperationException("TODO(core)");
    }

    /** Spans (bridges, chains) whose bounding box touches the rectangle. */
    public List<Span> spansIn(int minX, int minZ, int maxX, int maxZ) {
        throw new UnsupportedOperationException("TODO(core)");
    }

    /** Springs / waterfall sources whose position lies inside the rectangle. */
    public List<Spring> springsIn(int minX, int minZ, int maxX, int maxZ) {
        throw new UnsupportedOperationException("TODO(core)");
    }

    /** Points of interest for structures (ruins, white architecture) inside the rectangle. */
    public List<Site> sitesIn(int minX, int minZ, int maxX, int maxZ) {
        throw new UnsupportedOperationException("TODO(core)");
    }

    /** One floating island. */
    public record Island(int index, int cx, int cz, int width, ElysiumBiome.Size size, ElysiumBiome biome,
                         int baseY, int minX, int minZ, int maxX, int maxZ) {}

    /**
     * A crossing between two islands. kind: STONE_ARCH / ROPE / VINE_CHAIN are walkable bridges (deck from a to b);
     * GIANT_CHAIN hangs below a long crossing. Points are absolute block coordinates of the two anchors (deck level for
     * bridges, anchor under the cliffs for chains). sag = how far the middle drops below the straight line.
     */
    public record Span(Kind kind, int ax, int ay, int az, int bx, int by, int bz, double sag, int width) {
        public enum Kind { STONE_ARCH, ROPE, VINE_CHAIN, GIANT_CHAIN }
    }

    /** A water source on a cliff face or river mouth: place a WATER source at (x,y,z); the game makes the fall. */
    public record Spring(int x, int y, int z) {}

    /** A structure site. kind ids are owned by the structures agent (e.g. "overgrown_ruin", "white_colonnade", "spire_chapel"). y = surface. rot 0..3. */
    public record Site(String kind, int x, int y, int z, int rot, long salt) {}
}
