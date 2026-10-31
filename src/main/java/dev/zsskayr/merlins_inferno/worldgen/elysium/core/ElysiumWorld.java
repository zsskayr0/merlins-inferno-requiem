package dev.zsskayr.merlins_inferno.worldgen.elysium.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The whole of El'ysium as a pure function of the seed. NO Minecraft types in this package (tools compile it with plain javac).
 * Thread-safe: every method may be called concurrently from many chunk-gen threads. Cache per-island data lazily
 * (ConcurrentHashMap keyed by island cell), never per-call global state.
 *
 * THIS FILE IS THE CONTRACT between the core (terrain-core agent) and the Minecraft side (integration agent).
 * Signatures here must not change without the tech lead.
 *
 * <h2>Layout</h2>
 * The plane is cut into {@link #SUPER}-block super cells. Each super cell holds nothing, one LARGE/GIANT island, or two to
 * four SMALL/MEDIUM islands in uneven parts of the cell, placed loosely so the cell grid does not show; every island stays
 * inside its part with a 30-block margin so islands can never overlap and the cloud-sea gap is at least 60. The climate (temperature, humidity, ruggedness; very low frequency)
 * at the cell picks the layout and each island's biome among the biomes that allow its size. Spans (bridges, giant
 * chains) and Chainfall platforms between neighbouring islands are computed per super cell in a second stage; a third
 * stage lists every body that touches a cell so {@link #sample} only scans a handful of candidates.
 * The exception is the Glass Sea at its largest: colossal islands scattered about 14 km apart (see {@link #seaOfBlock}), each
 * owned by one cell and covering the 3x3 cells around it, which stay otherwise empty. Elsewhere, cells whose own climate is
 * near enough pick ordinary MEDIUM or LARGE Glass Seas like any other biome.
 * See docs/elysium/core-notes.md.
 */
public final class ElysiumWorld {
    public static final int MIN_Y = 0;
    public static final int HEIGHT = 384;            // dimension height: y 0..383
    public static final int CLOUD_SEA_Y = 64;        // top of the rendered cloud sea (client)
    public static final int LOWEST_SOLID = 80;       // nothing solid below this (keeps the cloud sea clear)
    public static final int HIGHEST_SOLID = 370;

    /** Super cell size in blocks (one large island or four small/medium ones). */
    public static final int SUPER = 1900;
    /** Minimum distance kept between an island's outline reach and its cell border (gap >= 2x this). */
    public static final int CELL_MARGIN = 30;
    /** Bridges for facing coasts up to this gap; giant chains + Chainfall platforms above it, up to {@link #MAX_CHAIN_GAP}. */
    public static final int MAX_BRIDGE_GAP = 300;
    public static final int MAX_CHAIN_GAP = 1500;
    /** Side, in super cells, of the blocks that each offer one candidate colossal Glass Sea: about 14 km, once the rows are staggered. */
    public static final int MEGA = 7;
    /** How far, in super cells, a sea's owner cell strays from its block's centre. Its 3x3 cells always stay inside the block. */
    private static final int SEA_JITTER = 2;
    /** Two seas closer than this many super cells (about 10 km) cannot both stand: the one with the higher priority does. */
    private static final double SEA_MIN_CELLS = 5.5;
    /** Share of super cells left as open sky. */
    private static final double EMPTY_CELL_CHANCE = 0.1;
    /** Share of the parts of a split super cell left as open sky. */
    private static final double EMPTY_PART_CHANCE = 0.12;
    /** A colossal island is three large ones across. */
    private static final double SEA_WIDTH = 4200;
    /** Farthest, in super cells, a colossal island's crossings can reach from the cell that owns them. */
    private static final int MEGA_REACH = 3;

    public final long seed;
    private final Noise nz;
    private final ConcurrentHashMap<Long, CellA> cellsA = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CellB> cellsB = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CellC> cellsC = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, int[]> seaBlocks = new ConcurrentHashMap<>();
    private final ThreadLocal<BiomeCache> biomeColumn = ThreadLocal.withInitial(BiomeCache::new);

    /** Last column sampled by {@link #biomeAt} on this thread (the biome source asks many y per column). */
    private static final class BiomeCache {
        final Column column = new Column();
        boolean valid;
    }

    public ElysiumWorld(long seed) {
        this.seed = seed;
        this.nz = new Noise(seed ^ 0x3E1F5A11L);
    }

    // ======================================================================== contract

    /** Fill {@code out} for column (x, z). Must be fast: called 256x per chunk. */
    public void sample(int x, int z, Column out) {
        out.x = x;
        out.z = z;
        CellC cc = cellC(Math.floorDiv(x, SUPER), Math.floorDiv(z, SUPER));
        double px = x + 0.5, pz = z + 0.5;
        for (IslandPlan p : cc.plans) {
            Island is = p.island;
            if (x < is.minX() || x > is.maxX() || z < is.minZ() || z > is.maxZ()) {
                continue;
            }
            double in = p.inland(px, pz);
            if (in > 0) {
                p.fill(x, z, in, out);
                return;
            }
        }
        out.land = false;
        out.top = Integer.MIN_VALUE;
        out.bottom = Integer.MIN_VALUE;
        out.waterTop = Integer.MIN_VALUE;
        out.frozen = false;
        out.spill = false;
        out.shore = false;
        out.archBottom = 0;
        out.archTop = -1;
        out.hangMat = Mat.AIR;
        out.hangLength = 0;
        out.island = -1;
        out.inland = 0;
        out.slope = 0;
        out.riverDist = Double.POSITIVE_INFINITY;
        out.baseY = 0;
        out.patch = 0;
        out.biome = nearestBiome(cc, px, pz);
    }

    /** Solid material at height y of a sampled column (bottom <= y <= top, or inside the arch span). */
    public Mat material(Column c, int y) {
        double hash = Noise.unit(Noise.hash(seed, c.x, y, c.z));
        Mat m;
        boolean lowest;
        if (c.hasArch() && y >= c.archBottom && y <= c.archTop) {
            int depth = c.archTop - y;
            m = depth < 2 ? ElysiumPalette.surface(c.biome, c, y, depth, hash, c.patch)
                    : ElysiumPalette.stone(c.biome, c, y, hash, c.patch);
            lowest = y == c.archBottom;
        } else {
            int depth = c.top - y;
            if (c.hasWater() && depth < 3) {
                m = ElysiumPalette.waterBed(c.biome, c, y, depth, hash, c.patch);
            } else if (c.shore && depth < Math.max(2, c.surfaceDepth)) {
                m = ElysiumPalette.shore(c.biome, c, y, depth, hash, c.patch);
            } else if (depth < c.surfaceDepth) {
                m = ElysiumPalette.surface(c.biome, c, y, depth, hash, c.patch);
            } else if (y < c.baseY - 10) {
                m = ElysiumPalette.underside(c.biome, c, y, y - c.bottom, hash, c.patch);
            } else {
                m = ElysiumPalette.stone(c.biome, c, y, hash, c.patch);
            }
            lowest = y == c.bottom;
        }
        if (m == null || m == Mat.AIR || m == Mat.WATER) {
            m = Mat.ARGANITE;
        }
        if (lowest) {
            m = supported(m);
        }
        return m;
    }

    /** Substitute for a material that would sit on air (rule 4: gravity blocks never over air). */
    public static Mat supported(Mat m) {
        return switch (m) {
            case SAND -> Mat.SANDSTONE;
            case GRAVEL -> Mat.ARGANITE;
            case SNOW_LAYER, POWDER_SNOW -> Mat.SNOW_BLOCK;
            case MOSS_CARPET, HANGING_ROOTS, CAVE_VINES, GLOW_LICHEN_CEIL -> Mat.DIRT;
            default -> m;
        };
    }

    /** True for materials that fall when unsupported. */
    public static boolean isGravity(Mat m) {
        return m == Mat.SAND || m == Mat.GRAVEL;
    }

    /** Biome at block (x, y, z). Underside/interior below the surface layer => UNDERHANG; else the column's island biome. */
    public ElysiumBiome biomeAt(int x, int y, int z) {
        BiomeCache bc = biomeColumn.get();
        Column c = bc.column;
        if (!bc.valid || c.x != x || c.z != z) {
            sample(x, z, c);
            bc.valid = true;
        }
        if (!c.land) {
            return c.biome;
        }
        if (y <= c.top - 12 && y >= c.bottom - 8) {
            return ElysiumBiome.UNDERHANG;
        }
        return c.biome;
    }

    /** Islands whose bounding box touches the block rectangle [minX..maxX]x[minZ..maxZ]. */
    public List<Island> islandsIn(int minX, int minZ, int maxX, int maxZ) {
        List<Island> out = new ArrayList<>();
        for (IslandPlan p : plansIn(minX, minZ, maxX, maxZ)) {
            out.add(p.island);
        }
        return out;
    }

    /** Spans (bridges, chains) whose bounding box touches the rectangle. */
    public List<Span> spansIn(int minX, int minZ, int maxX, int maxZ) {
        List<Span> out = new ArrayList<>();
        int cx0 = Math.floorDiv(minX, SUPER), cx1 = Math.floorDiv(maxX, SUPER);
        int cz0 = Math.floorDiv(minZ, SUPER), cz1 = Math.floorDiv(maxZ, SUPER);
        for (int sx = cx0 - MEGA_REACH; sx <= cx1 + MEGA_REACH; sx++) {
            for (int sz = cz0 - MEGA_REACH; sz <= cz1 + MEGA_REACH; sz++) {
                // a normal cell's crossings stay within one cell of it; only a colossal island's reach farther
                boolean near = sx >= cx0 - 1 && sx <= cx1 + 1 && sz >= cz0 - 1 && sz <= cz1 + 1;
                if (!near && !isMegaCentre(sx, sz)) {
                    continue;
                }
                for (Span s : cellB(sx, sz).spans) {
                    int pad = s.width() + 2;
                    if (Math.max(s.ax(), s.bx()) + pad >= minX && Math.min(s.ax(), s.bx()) - pad <= maxX
                            && Math.max(s.az(), s.bz()) + pad >= minZ && Math.min(s.az(), s.bz()) - pad <= maxZ) {
                        out.add(s);
                    }
                }
            }
        }
        return out;
    }

    /** Springs / waterfall sources whose position lies inside the rectangle. */
    public List<Spring> springsIn(int minX, int minZ, int maxX, int maxZ) {
        List<Spring> out = new ArrayList<>();
        for (IslandPlan p : plansIn(minX, minZ, maxX, maxZ)) {
            for (Spring s : p.springs()) {
                if (s.x() >= minX && s.x() <= maxX && s.z() >= minZ && s.z() <= maxZ) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    /** Loose chains hanging from islands that may reach into the rectangle. */
    public List<HangingChain> hangingChainsIn(int minX, int minZ, int maxX, int maxZ) {
        List<HangingChain> out = new ArrayList<>();
        int pad = HangingChain.MAX_RUN + 12;
        for (IslandPlan p : plansIn(minX - pad, minZ - pad, maxX + pad, maxZ + pad)) {
            for (HangingChain h : p.chains()) {
                if (h.x() + pad >= minX && h.x() - pad <= maxX && h.z() + pad >= minZ && h.z() - pad <= maxZ) {
                    out.add(h);
                }
            }
        }
        return out;
    }

    /** Points of interest for structures (ruins, white architecture) inside the rectangle. */
    public List<Site> sitesIn(int minX, int minZ, int maxX, int maxZ) {
        List<Site> out = new ArrayList<>();
        for (IslandPlan p : plansIn(minX, minZ, maxX, maxZ)) {
            for (Site s : p.sites()) {
                if (s.x() >= minX && s.x() <= maxX && s.z() >= minZ && s.z() <= maxZ) {
                    out.add(s);
                }
            }
        }
        return out;
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

    /**
     * A giant chain tied to one island and to nothing else, hanging in the air. (x, y, z) is the anchor; (outX, outZ) the
     * unit direction it leaves the coast in and {@code run} how far out it swings (0 = straight down from the underside);
     * {@code drop} how far the free end falls below the anchor.
     */
    public record HangingChain(int x, int y, int z, double outX, double outZ, int run, int drop, long salt) {
        public static final int MAX_RUN = 20;
    }

    /** A water source on a cliff face or river mouth: place a WATER source at (x,y,z); the game makes the fall. */
    public record Spring(int x, int y, int z) {}

    /** A structure site. kind ids are owned by the structures agent (e.g. "overgrown_ruin", "white_colonnade", "spire_chapel"). y = surface. rot 0..3. */
    public record Site(String kind, int x, int y, int z, int rot, long salt) {}

    // ======================================================================== extra public helpers

    /** The island (or Chainfall platform) with this {@link Column#island} index, or null. */
    public Island island(int index) {
        IslandPlan p = plan(index);
        return p == null ? null : p.island;
    }

    /** The full plan of an island (rivers, lakes...) for tools; null if unknown. */
    public IslandPlan plan(int index) {
        if (index < 0) {
            return null;
        }
        int sx = (index << 1) >> 20, sz = (index << 13) >> 20, slot = index & 0x7F;
        if (slot < 4) {
            for (IslandPlan p : cellA(sx, sz).plans) {
                if (p.island.index() == index) {
                    return p;
                }
            }
        } else {
            for (IslandPlan p : cellB(sx, sz).platforms) {
                if (p.island.index() == index) {
                    return p;
                }
            }
        }
        return null;
    }

    /** Rivers (and rim notches) of an island, for tools and the decorator. */
    public List<River> riversOf(int index) {
        IslandPlan p = plan(index);
        return p == null ? List.of() : List.copyOf(p.rivers);
    }

    /** Island plans (islands and platforms) whose bbox touches the rectangle. */
    public List<IslandPlan> plansIn(int minX, int minZ, int maxX, int maxZ) {
        List<IslandPlan> out = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        int sx0 = Math.floorDiv(minX, SUPER), sx1 = Math.floorDiv(maxX, SUPER);
        int sz0 = Math.floorDiv(minZ, SUPER), sz1 = Math.floorDiv(maxZ, SUPER);
        for (int sx = sx0; sx <= sx1; sx++) {
            for (int sz = sz0; sz <= sz1; sz++) {
                for (IslandPlan p : cellC(sx, sz).plans) {
                    Island is = p.island;
                    if (is.maxX() >= minX && is.minX() <= maxX && is.maxZ() >= minZ && is.minZ() <= maxZ
                            && seen.add(is.index())) {
                        out.add(p);
                    }
                }
            }
        }
        return out;
    }

    /** Highest block of the physical cloud layer in a column (see {@code CloudSeaBlock}); the layer is 2 blocks thick. */
    public int cloudTop(int x, int z) {
        return CLOUD_SEA_Y - 1 + (int) Math.round(2.2 * nz.fbm(x / 44.0 + 919.0, z / 44.0 - 271.0, 2));
    }

    /** Cloud block tint 0..2 (dark to bright) for a column of the layer: slow patches, brighter on the crests. */
    public int cloudTone(int x, int z, int top) {
        double v = nz.fbm(x / 21.0 - 53.0, z / 21.0 + 777.0, 2) + (top - CLOUD_SEA_Y + 1) * 0.3;
        return v < -0.25 ? 0 : (v < 0.25 ? 1 : 2);
    }

    /** Climate at a block: {temperature, humidity, ruggedness}, each about -1..1. */
    public double[] climate(double x, double z) {
        double t = Noise.clamp(1.5 * nz.fbm(x / 7000.0 + 311.0, z / 7000.0 - 97.0, 2), -1, 1);
        double m = Noise.clamp(1.5 * nz.fbm(x / 5500.0 - 733.0, z / 5500.0 + 401.0, 2), -1, 1);
        double r = Noise.clamp(1.5 * nz.fbm(x / 3600.0 + 1201.0, z / 3600.0 + 59.0, 2), -1, 1);
        return new double[] {t, m, r};
    }

    // ======================================================================== stage A: islands

    private static long key(int sx, int sz) {
        return ((long) sx << 32) ^ (sz & 0xffffffffL);
    }

    static int packIndex(int sx, int sz, int slot) {
        return ((sx & 0xFFF) << 19) | ((sz & 0xFFF) << 7) | (slot & 0x7F);
    }

    private static final class CellA {
        final List<IslandPlan> plans = new ArrayList<>();
    }

    private static final double[][] ANCHORS = new double[ElysiumBiome.values().length][];

    static {
        anchor(ElysiumBiome.LARK_MEADOWS, 0.15, 0.0, -0.5);
        anchor(ElysiumBiome.WINDLESS_STEPPE, 0.6, -0.6, -0.4);
        anchor(ElysiumBiome.HIGHPINE_FOREST, -0.35, 0.35, 0.1);
        anchor(ElysiumBiome.MOSSY_WEALD, 0.15, 0.65, -0.1);
        anchor(ElysiumBiome.ARGENT_PEAKS, -0.4, -0.3, 0.85);
        anchor(ElysiumBiome.STONECROWN_CLIFFS, 0.15, -0.35, 0.7);
        anchor(ElysiumBiome.CASCADE_VALE, 0.45, -0.1, 0.55);
        anchor(ElysiumBiome.MISTFALL_GORGE, 0.2, 0.55, 0.65);
        anchor(ElysiumBiome.GLASS_SEA, 0.3, 0.3, 0.05);
        anchor(ElysiumBiome.AZURE_SHALLOWS, 0.8, 0.55, -0.5);
        anchor(ElysiumBiome.RIME_EXPANSE, -0.85, -0.1, -0.5);
        anchor(ElysiumBiome.FROSTCREST_RANGE, -0.75, 0.25, 0.6);
    }

    private static void anchor(ElysiumBiome b, double t, double m, double r) {
        ANCHORS[b.ordinal()] = new double[] {t, m, r};
    }

    private static double climateDist(double[] c, ElysiumBiome b) {
        double[] a = ANCHORS[b.ordinal()];
        double dt = c[0] - a[0], dm = c[1] - a[1], dr = c[2] - a[2];
        return Math.sqrt(dt * dt * 1.3 + dm * dm + dr * dr * 0.8);
    }

    private static boolean allows(ElysiumBiome b, ElysiumBiome.Size s) {
        return s.ordinal() >= b.minSize.ordinal() && s.ordinal() <= b.maxSize.ordinal();
    }

    /**
     * The biomes an island of {@code own} biome at this climate can share its land with, nearest first: never a sea (those
     * are a basin held by a rim), and never a cold biome next to a warm one. Seas themselves keep to one biome.
     */
    private static ElysiumBiome[] neighbours(double[] c, ElysiumBiome own) {
        if (own == ElysiumBiome.GLASS_SEA || own == ElysiumBiome.AZURE_SHALLOWS) {
            return new ElysiumBiome[0];
        }
        boolean cold = own == ElysiumBiome.RIME_EXPANSE || own == ElysiumBiome.FROSTCREST_RANGE;
        List<ElysiumBiome> out = new ArrayList<>();
        for (ElysiumBiome b : ElysiumBiome.values()) {
            if (b == own || ANCHORS[b.ordinal()] == null || b == ElysiumBiome.GLASS_SEA || b == ElysiumBiome.AZURE_SHALLOWS) {
                continue;
            }
            boolean coldB = b == ElysiumBiome.RIME_EXPANSE || b == ElysiumBiome.FROSTCREST_RANGE;
            boolean either = b == ElysiumBiome.HIGHPINE_FOREST || b == ElysiumBiome.ARGENT_PEAKS;
            if (coldB == cold || either) {
                out.add(b);
            }
        }
        out.sort(java.util.Comparator.comparingDouble(b -> climateDist(c, b)));
        return out.subList(0, Math.min(3, out.size())).toArray(new ElysiumBiome[0]);
    }

    /** Nearest surface biome to the climate allowing size s (null s = any). */
    private static ElysiumBiome pick(double[] c, ElysiumBiome.Size s) {
        ElysiumBiome best = null;
        double bd = Double.POSITIVE_INFINITY;
        for (ElysiumBiome b : ElysiumBiome.values()) {
            if (ANCHORS[b.ordinal()] == null || (s != null && !allows(b, s))) {
                continue;
            }
            double d = climateDist(c, b);
            if (d < bd) {
                bd = d;
                best = b;
            }
        }
        return best;
    }

    private CellA cellA(int sx, int sz) {
        long k = key(sx, sz);
        CellA a = cellsA.get(k);
        if (a == null) {
            a = buildA(sx, sz);
            CellA prev = cellsA.putIfAbsent(k, a);
            if (prev != null) {
                a = prev;
            }
        }
        return a;
    }

    /** Each row of blocks slides sideways by its own amount, so the seas never line up in columns. */
    private int seaRowShift(int mz) {
        return (int) (Noise.unit(Noise.hash(seed, mz, 0, 0x5EC)) * MEGA);
    }

    private int seaBlockX(int sx, int mz) {
        return Math.floorDiv(sx - seaRowShift(mz), MEGA);
    }

    /** The cell block (mx, mz) would put a sea in: {cell x, cell z}. */
    private int[] seaCandidate(int mx, int mz) {
        long h = Noise.hash(seed, mx, mz, 0x5EA);
        int jx = (int) (Noise.unit(Noise.mix(h + 1)) * (2 * SEA_JITTER + 1)) - SEA_JITTER;
        int jz = (int) (Noise.unit(Noise.mix(h + 2)) * (2 * SEA_JITTER + 1)) - SEA_JITTER;
        return new int[] {mx * MEGA + seaRowShift(mz) + MEGA / 2 + jx, mz * MEGA + MEGA / 2 + jz};
    }

    private double seaPriority(int mx, int mz) {
        return Noise.unit(Noise.mix(Noise.hash(seed, mx, mz, 0x5EA) + 3));
    }

    /**
     * The owner cell of the Glass Sea in block (mx, mz), or an empty array when it has none. A candidate stands unless a
     * neighbouring candidate with a higher priority sits too close, which every chunk can work out alone. Footprints stay
     * inside their blocks, so a cell only ever has its own block's sea to think about.
     */
    private int[] seaOfBlock(int mx, int mz) {
        return seaBlocks.computeIfAbsent(key(mx, mz), k -> {
            int[] c = seaCandidate(mx, mz);
            double priority = seaPriority(mx, mz);
            for (int row = mz - 1; row <= mz + 1; row++) {
                int base = seaBlockX(c[0], row);
                for (int m = base - 1; m <= base + 1; m++) {
                    if (m == mx && row == mz) {
                        continue;
                    }
                    int[] o = seaCandidate(m, row);
                    if (Math.hypot(o[0] - c[0], o[1] - c[1]) < SEA_MIN_CELLS && seaPriority(m, row) > priority) {
                        return new int[0];
                    }
                }
            }
            return c;
        });
    }

    private boolean isMegaCentre(int sx, int sz) {
        int mz = Math.floorDiv(sz, MEGA);
        int[] sea = seaOfBlock(seaBlockX(sx, mz), mz);
        return sea.length > 0 && sea[0] == sx && sea[1] == sz;
    }

    private CellA buildA(int sx, int sz) {
        CellA cell = new CellA();
        SplittableRandom r = new SplittableRandom(Noise.hash(seed, sx, sz, 0xCE11));
        int mz = Math.floorDiv(sz, MEGA);
        int[] sea = seaOfBlock(seaBlockX(sx, mz), mz);
        if (sea.length > 0 && Math.abs(sx - sea[0]) <= 1 && Math.abs(sz - sea[1]) <= 1) {
            if (sx == sea[0] && sz == sea[1]) {
                addColossalSea(cell, r, sx, sz);
            }
            return cell;
        }
        if (r.nextDouble() < EMPTY_CELL_CHANCE) {
            return cell;
        }
        double ccx = (sx + 0.5) * SUPER, ccz = (sz + 0.5) * SUPER;
        double[] clim = climate(ccx, ccz);
        ElysiumBiome nearest = pick(clim, null);
        boolean single;
        if (nearest.minSize.ordinal() >= ElysiumBiome.Size.LARGE.ordinal()) {
            single = true;
        } else if (nearest.maxSize.ordinal() <= ElysiumBiome.Size.MEDIUM.ordinal()) {
            single = false;
        } else {
            single = r.nextDouble() < 0.7;
        }
        if (single) {
            ElysiumBiome.Size size = nearest == ElysiumBiome.LARK_MEADOWS && r.nextDouble() < 0.4
                    ? ElysiumBiome.Size.GIANT : ElysiumBiome.Size.LARGE;
            // the narrower it is, the farther it can stray from the middle of its cell
            double width = size == ElysiumBiome.Size.GIANT ? 1600 + 400 * r.nextDouble() : 1000 + 500 * r.nextDouble();
            addIsland(cell, r, sx, sz, 0, ccx, ccz, SUPER / 2.0, SUPER / 2.0, width, size);
        } else {
            List<double[]> parts = splitCell(r, sx * SUPER, sz * SUPER);
            for (int i = 0; i < parts.size(); i++) {
                if (r.nextDouble() < EMPTY_PART_CHANCE) {
                    continue;
                }
                double[] p = parts.get(i);
                ElysiumBiome.Size size = r.nextDouble() < 0.45 ? ElysiumBiome.Size.SMALL : ElysiumBiome.Size.MEDIUM;
                double width = size == ElysiumBiome.Size.SMALL ? 320 + 240 * r.nextDouble() : 600 + 400 * r.nextDouble();
                addIsland(cell, r, sx, sz, i, p[0] + p[2] / 2, p[1] + p[3] / 2, p[2] / 2, p[3] / 2, width, size);
            }
        }
        return cell;
    }

    /** Cuts a super cell into 2 to 4 uneven rectangles {x, z, width, depth}, always splitting the largest along its long side. */
    private static List<double[]> splitCell(SplittableRandom r, double x, double z) {
        List<double[]> parts = new ArrayList<>();
        parts.add(new double[] {x, z, SUPER, SUPER});
        int n = 2 + r.nextInt(3);
        while (parts.size() < n) {
            int big = 0;
            for (int i = 1; i < parts.size(); i++) {
                if (parts.get(i)[2] * parts.get(i)[3] > parts.get(big)[2] * parts.get(big)[3]) {
                    big = i;
                }
            }
            double[] p = parts.remove(big);
            double cut = 0.36 + 0.28 * r.nextDouble();
            if (p[2] >= p[3]) {
                parts.add(new double[] {p[0], p[1], p[2] * cut, p[3]});
                parts.add(new double[] {p[0] + p[2] * cut, p[1], p[2] * (1 - cut), p[3]});
            } else {
                parts.add(new double[] {p[0], p[1], p[2], p[3] * cut});
                parts.add(new double[] {p[0], p[1] + p[3] * cut, p[2], p[3] * (1 - cut)});
            }
        }
        return parts;
    }

    /** The Glass Sea: one island inside the 3x3 super cells around (sx, sz). */
    private void addColossalSea(CellA cell, SplittableRandom r, int sx, int sz) {
        double half = 1.5 * SUPER, qx = (sx + 0.5) * SUPER, qz = (sz + 0.5) * SUPER;
        double R = SEA_WIDTH / 2;
        double slack = Math.max(0, half - CELL_MARGIN - IslandPlan.extent(R));
        int icx = (int) Math.floor(qx + (r.nextDouble() * 2 - 1) * slack), icz = (int) Math.floor(qz + (r.nextDouble() * 2 - 1) * slack);
        int ext = (int) Math.ceil(IslandPlan.extent(R));
        int baseY = 190 + r.nextInt(16);
        Island is = new Island(packIndex(sx, sz, 0), icx, icz, (int) Math.round(2 * R), ElysiumBiome.Size.COLOSSAL, ElysiumBiome.GLASS_SEA,
                baseY, icx - ext, icz - ext, icx + ext, icz + ext);
        cell.plans.add(new IslandPlan(is, R, nz, seed, new ElysiumBiome[0]));
    }

    private void addIsland(CellA cell, SplittableRandom r, int sx, int sz, int slot, double qx, double qz, double halfX, double halfZ,
                           double width, ElysiumBiome.Size size) {
        double half = Math.min(halfX, halfZ);
        double maxR = (half - CELL_MARGIN - 2) / (1.062 + IslandPlan.amp(width / 2) + 0.035);
        if (maxR <= 760 && width / 2 > 760) {
            maxR = (half - CELL_MARGIN - 2) / (1.092 + IslandPlan.amp(800));
        }
        double R = Math.min(width / 2, maxR);
        double reach = CELL_MARGIN + IslandPlan.extent(R);
        double cx = qx + (r.nextDouble() * 2 - 1) * Math.max(0, halfX - reach), cz = qz + (r.nextDouble() * 2 - 1) * Math.max(0, halfZ - reach);
        double[] clim = climate(cx, cz);
        ElysiumBiome biome = pick(clim, size);
        if (size == ElysiumBiome.Size.SMALL) {
            ElysiumBiome any = pick(clim, null);
            if (climateDist(clim, biome) > climateDist(clim, any) + 0.45) {
                size = ElysiumBiome.Size.MEDIUM;
                biome = pick(clim, size);
                R = Math.min(Math.max(R, 280), maxR);
            }
        }
        if (size == ElysiumBiome.Size.GIANT && biome != ElysiumBiome.LARK_MEADOWS) {
            size = ElysiumBiome.Size.LARGE;
            R = Math.min(R, (half - CELL_MARGIN - 2) / (1.092 + IslandPlan.amp(0)));
        }
        int w = (int) Math.round(2 * R);
        ElysiumBiome.Size actual = ElysiumBiome.Size.SMALL;
        for (ElysiumBiome.Size s : ElysiumBiome.Size.values()) {
            if (s != ElysiumBiome.Size.PLATFORM && w >= s.minWidth) {
                actual = s;
            }
        }
        double sizeF = Noise.clamp((w - 300) / 1500.0, 0, 1);
        int baseY = (int) Math.round(Noise.clamp(166 + 22 * r.nextDouble() + 26 * sizeF, 165, 215));
        if (biome == ElysiumBiome.GLASS_SEA || biome == ElysiumBiome.AZURE_SHALLOWS) {
            baseY = Math.max(baseY, 180);
        }
        int ext = (int) Math.ceil(IslandPlan.extent(R));
        int icx = (int) Math.floor(cx), icz = (int) Math.floor(cz);
        Island is = new Island(packIndex(sx, sz, slot), icx, icz, w, actual, biome, baseY,
                icx - ext, icz - ext, icx + ext, icz + ext);
        cell.plans.add(new IslandPlan(is, R, nz, seed, neighbours(clim, biome)));
    }

    // ======================================================================== stage B: spans & platforms

    private static final class CellB {
        final List<Span> spans = new ArrayList<>();
        final List<IslandPlan> platforms = new ArrayList<>();
    }

    private CellB cellB(int sx, int sz) {
        long k = key(sx, sz);
        CellB b = cellsB.get(k);
        if (b == null) {
            b = buildB(sx, sz);
            CellB prev = cellsB.putIfAbsent(k, b);
            if (prev != null) {
                b = prev;
            }
        }
        return b;
    }

    /** Coast point of {@code p} along the ray from its centre towards (tx, tz): last land point. */
    private static double[] coastToward(IslandPlan p, double tx, double tz) {
        double dx = tx - p.cx, dz = tz - p.cz, len = Math.sqrt(dx * dx + dz * dz);
        dx /= len;
        dz /= len;
        double last = 0;
        double ext = IslandPlan.extent(p.R);
        for (double d = 0; d <= ext; d += 1) {
            if (p.inland(p.cx + dx * d, p.cz + dz * d) > 0) {
                last = d;
            }
        }
        return new double[] {p.cx + dx * last, p.cz + dz * last, dx, dz};
    }

    /** Deck anchor: a dry land column a few blocks inside the coast at c (dir points outward). */
    private static int[] deckAnchor(IslandPlan p, double[] c, Column col) {
        double px = -c[3], pz = c[2];
        for (int k = 0; k < 7; k++) {
            double off = (k == 0 ? 0 : ((k & 1) == 1 ? 1 : -1) * 5 * ((k + 1) / 2));
            for (double back = 3; back < 20; back += 3) {
                int x = (int) Math.floor(c[0] - c[2] * back + px * off), z = (int) Math.floor(c[1] - c[3] * back + pz * off);
                double in = p.inland(x + 0.5, z + 0.5);
                if (in <= 0) {
                    continue;
                }
                p.fill(x, z, in, col);
                if (!col.hasWater() && !col.hasArch() && col.riverDist > 3) {
                    return new int[] {x, col.top, z, col.bottom};
                }
            }
        }
        return null;
    }

    private static boolean segmentsCross(double ax, double az, double bx, double bz, double cx, double cz, double dx, double dz) {
        double d1 = cross(cx, cz, dx, dz, ax, az), d2 = cross(cx, cz, dx, dz, bx, bz);
        double d3 = cross(ax, az, bx, bz, cx, cz), d4 = cross(ax, az, bx, bz, dx, dz);
        return d1 * d2 < 0 && d3 * d4 < 0;
    }

    private static double cross(double ax, double az, double bx, double bz, double px, double pz) {
        return (bx - ax) * (pz - az) - (bz - az) * (px - ax);
    }

    private static boolean walkable(ElysiumBiome b) {
        return b == ElysiumBiome.LARK_MEADOWS || b == ElysiumBiome.WINDLESS_STEPPE || b == ElysiumBiome.HIGHPINE_FOREST;
    }

    private CellB buildB(int sx, int sz) {
        CellB cell = new CellB();
        List<IslandPlan> around = new ArrayList<>();
        boolean colossal = isMegaCentre(sx, sz);
        for (int dx = -MEGA_REACH; dx <= MEGA_REACH; dx++) {
            for (int dz = -MEGA_REACH; dz <= MEGA_REACH; dz++) {
                // crossings may pass near anything in the usual window, and near any colossal island farther off
                if (colossal || (dx >= -1 && dx <= 2 && dz >= -2 && dz <= 2) || isMegaCentre(sx + dx, sz + dz)) {
                    around.addAll(cellA(sx + dx, sz + dz).plans);
                }
            }
        }
        List<double[]> segs = new ArrayList<>();
        int[][] nbr = {{0, 0}, {1, 0}, {0, 1}, {1, 1}, {1, -1}};
        if (colossal) {
            // the cells it covers are empty, so a colossal island's neighbours are the cells around them
            List<int[]> ring = new ArrayList<>();
            for (int dx = -MEGA_REACH; dx <= MEGA_REACH; dx++) {
                for (int dz = -MEGA_REACH; dz <= MEGA_REACH; dz++) {
                    if (Math.abs(dx) > 1 || Math.abs(dz) > 1) {
                        ring.add(new int[] {dx, dz});
                    }
                }
            }
            nbr = ring.toArray(new int[0][]);
        }
        int slot = 4;
        Column col = new Column();
        List<IslandPlan> mine = cellA(sx, sz).plans;
        for (IslandPlan a : mine) {
            for (int[] nd : nbr) {
                for (IslandPlan b : cellA(sx + nd[0], sz + nd[1]).plans) {
                    if (nd[0] == 0 && nd[1] == 0 && b.island.index() <= a.island.index()) {
                        continue;
                    }
                    if (b.island.size() == ElysiumBiome.Size.COLOSSAL && b.island.index() <= a.island.index()) {
                        continue; // two seas side by side: either one's turn would do
                    }
                    double cdx = b.cx - a.cx, cdz = b.cz - a.cz;
                    double cd = Math.sqrt(cdx * cdx + cdz * cdz);
                    if (cd - IslandPlan.extent(a.R) - IslandPlan.extent(b.R) > MAX_CHAIN_GAP) {
                        continue;
                    }
                    double[] A = coastToward(a, b.cx, b.cz), B = coastToward(b, a.cx, a.cz);
                    double gx = B[0] - A[0], gz = B[1] - A[1];
                    double gap = Math.sqrt(gx * gx + gz * gz);
                    if (gap < 20 || gap > MAX_CHAIN_GAP) {
                        continue;
                    }
                    double u = Noise.unit(Noise.hash(seed, a.island.index(), b.island.index(), 0x5BA9));
                    boolean bridge = gap <= MAX_BRIDGE_GAP;
                    if (u >= (bridge ? 0.55 : 0.5)) {
                        continue;
                    }
                    // blocked by a third island?
                    boolean blocked = false;
                    for (double t = 0.02; t < 0.98 && !blocked; t += Math.min(0.1, 30 / gap)) {
                        double qx = A[0] + gx * t, qz = A[1] + gz * t;
                        for (IslandPlan o : around) {
                            if (o != a && o != b && o.inland(qx, qz) > -40) {
                                blocked = true;
                                break;
                            }
                        }
                    }
                    for (double[] s : segs) {
                        if (!blocked && segmentsCross(A[0], A[1], B[0], B[1], s[0], s[1], s[2], s[3])) {
                            blocked = true;
                        }
                    }
                    if (blocked) {
                        continue;
                    }
                    if (bridge) {
                        Span sp = bridgeBetween(a, b, A, B, col);
                        if (sp != null) {
                            cell.spans.add(sp);
                            segs.add(new double[] {A[0], A[1], B[0], B[1]});
                        }
                    } else {
                        slot = chainCrossing(cell, a, b, A, B, gap, around, col, sx, sz, slot);
                        segs.add(new double[] {A[0], A[1], B[0], B[1]});
                    }
                }
            }
        }
        return cell;
    }

    private Span bridgeBetween(IslandPlan a, IslandPlan b, double[] A, double[] B, Column col) {
        int[] da = deckAnchor(a, A, col), db = deckAnchor(b, B, col);
        if (da == null || db == null) {
            return null;
        }
        double gap = Math.hypot(db[0] - da[0], db[2] - da[2]);
        Span.Kind kind;
        if (a.biome == ElysiumBiome.MOSSY_WEALD || b.biome == ElysiumBiome.MOSSY_WEALD
                || a.biome == ElysiumBiome.CHAINFALL || b.biome == ElysiumBiome.CHAINFALL) {
            kind = Span.Kind.VINE_CHAIN;
        } else if (walkable(a.biome) && walkable(b.biome)) {
            kind = Span.Kind.ROPE;
        } else {
            kind = Span.Kind.STONE_ARCH;
        }
        double sag = switch (kind) {
            case STONE_ARCH -> 0;
            case ROPE -> 0.05 * gap;
            default -> 0.06 * gap;
        };
        int width = kind == Span.Kind.STONE_ARCH ? 5 : 3;
        return new Span(kind, da[0], da[1], da[2], db[0], db[1], db[2], sag, width);
    }

    private int chainCrossing(CellB cell, IslandPlan a, IslandPlan b, double[] A, double[] B, double gap,
                              List<IslandPlan> around, Column col, int sx, int sz, int slot) {
        double ux = (B[0] - A[0]) / gap, uz = (B[1] - A[1]) / gap, px = -uz, pz = ux;
        int[] da = deckAnchor(a, A, col), db = deckAnchor(b, B, col);
        if (da == null || db == null) {
            return slot;
        }
        SplittableRandom r = new SplittableRandom(Noise.hash(seed, a.island.index(), b.island.index(), 0xC4A1));
        // two giant chains, anchored in the cliff faces below the coast tops
        for (int side = -1; side <= 1; side += 2) {
            int[] ca = chainAnchor(a, A[0] + px * side * 9, A[1] + pz * side * 9, ux, uz, r, col);
            int[] cb = chainAnchor(b, B[0] + px * side * 9, B[1] + pz * side * 9, -ux, -uz, r, col);
            if (ca == null || cb == null) {
                continue;
            }
            double sag = Math.min(gap * 0.12, Math.min(ca[1], cb[1]) - LOWEST_SOLID - 25);
            cell.spans.add(new Span(Span.Kind.GIANT_CHAIN, ca[0], ca[1], ca[2], cb[0], cb[1], cb[2], Math.max(0, sag), 3));
        }
        // Chainfall platforms strung along the crossing, linked by vine bridges
        int count = gap < 450 ? 0 : Math.min(3, Math.max(1, (int) (gap / 400)));
        List<IslandPlan> chain = new ArrayList<>();
        for (int k = 0; k < count && slot < 127; k++) {
            double t = (k + 0.5 + (r.nextDouble() - 0.5) * 0.3) / count;
            double lat = ((k & 1) == 0 ? 1 : -1) * (28 + 40 * r.nextDouble());
            double Rp = 20 + 60 * r.nextDouble() * r.nextDouble();
            double qx = A[0] + (B[0] - A[0]) * t + px * lat, qz = A[1] + (B[1] - A[1]) * t + pz * lat;
            double ext = IslandPlan.extent(Rp);
            boolean ok = true;
            for (IslandPlan o : around) {
                if (o.inland(qx, qz) > -(ext + 45)) {
                    ok = false;
                    break;
                }
            }
            for (IslandPlan o : cell.platforms) {
                if (Math.hypot(o.cx - qx, o.cz - qz) < IslandPlan.extent(o.R) + ext + 30) {
                    ok = false;
                }
            }
            if (!ok) {
                continue;
            }
            int baseY = (int) Math.round(Noise.clamp(Noise.lerp(da[1], db[1], t) - 8 - 22 * r.nextDouble(), 150, 215));
            int icx = (int) Math.floor(qx), icz = (int) Math.floor(qz), e = (int) Math.ceil(ext);
            Island is = new Island(packIndex(sx, sz, slot++), icx, icz, (int) Math.round(2 * Rp),
                    ElysiumBiome.Size.PLATFORM, ElysiumBiome.CHAINFALL, baseY, icx - e, icz - e, icx + e, icz + e);
            IslandPlan p = new IslandPlan(is, Rp, nz, seed, new ElysiumBiome[0]);
            cell.platforms.add(p);
            chain.add(p);
        }
        // hop bridges a -> p1 -> ... -> b where hops are short
        List<IslandPlan> path = new ArrayList<>();
        path.add(a);
        path.addAll(chain);
        path.add(b);
        for (int i = 0; i + 1 < path.size(); i++) {
            IslandPlan p = path.get(i), q = path.get(i + 1);
            double[] P = coastToward(p, q.cx, q.cz), Q = coastToward(q, p.cx, p.cz);
            double hop = Math.hypot(Q[0] - P[0], Q[1] - P[1]);
            if (hop > 260 || r.nextDouble() > 0.8) {
                continue;
            }
            Span sp = bridgeBetween(p, q, P, Q, col);
            if (sp != null) {
                cell.spans.add(new Span(Span.Kind.VINE_CHAIN, sp.ax(), sp.ay(), sp.az(), sp.bx(), sp.by(), sp.bz(),
                        0.06 * hop, 3));
            }
        }
        return slot;
    }

    /** Anchor for a giant chain: on the cliff face 8..16 below the coast top, column at the coast. */
    private static int[] chainAnchor(IslandPlan p, double x0, double z0, double ux, double uz, SplittableRandom r, Column col) {
        // find the coast column along u near (x0,z0)
        for (double back = -30; back < 30; back += 1) {
            double x = x0 - ux * back, z = z0 - uz * back;
            double in = p.inland(x, z);
            if (in > 0 && in < 3) {
                int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
                double in2 = p.inland(ix + 0.5, iz + 0.5);
                if (in2 <= 0) {
                    continue;
                }
                p.fill(ix, iz, in2, col);
                int y = col.top - 8 - r.nextInt(9);
                y = Math.max(y, col.bottom + 2);
                if (y > col.top) {
                    return null;
                }
                return new int[] {ix, y, iz};
            }
        }
        return null;
    }

    // ======================================================================== stage C: per-cell query lists

    private static final class CellC {
        IslandPlan[] plans;
    }

    private CellC cellC(int sx, int sz) {
        long k = key(sx, sz);
        CellC c = cellsC.get(k);
        if (c == null) {
            c = buildC(sx, sz);
            CellC prev = cellsC.putIfAbsent(k, c);
            if (prev != null) {
                c = prev;
            }
        }
        return c;
    }

    private CellC buildC(int sx, int sz) {
        int x0 = sx * SUPER - 1, z0 = sz * SUPER - 1, x1 = x0 + SUPER + 2, z1 = z0 + SUPER + 2;
        List<IslandPlan> out = new ArrayList<>();
        for (int dx = -MEGA_REACH; dx <= MEGA_REACH; dx++) {
            for (int dz = -MEGA_REACH; dz <= MEGA_REACH; dz++) {
                // a colossal island strings its platforms well past the usual one cell
                if ((Math.abs(dx) > 1 || Math.abs(dz) > 1) && !isMegaCentre(sx + dx, sz + dz)) {
                    continue;
                }
                List<IslandPlan> all = new ArrayList<>(cellA(sx + dx, sz + dz).plans);
                all.addAll(cellB(sx + dx, sz + dz).platforms);
                for (IslandPlan p : all) {
                    Island is = p.island;
                    if (is.maxX() >= x0 && is.minX() <= x1 && is.maxZ() >= z0 && is.minZ() <= z1) {
                        out.add(p);
                    }
                }
            }
        }
        // islands first (platforms never overlap them, but keep a stable order)
        out.sort((p, q) -> Integer.compare(p.island.index(), q.island.index()));
        CellC c = new CellC();
        c.plans = out.toArray(new IslandPlan[0]);
        return c;
    }

    private static ElysiumBiome nearestBiome(CellC cc, double x, double z) {
        ElysiumBiome best = ElysiumBiome.LARK_MEADOWS;
        double bd = Double.POSITIVE_INFINITY;
        for (IslandPlan p : cc.plans) {
            if (p.platform) {
                continue;
            }
            double d = Math.hypot(p.cx - x, p.cz - z) - p.R;
            if (d < bd) {
                bd = d;
                best = p.biome;
            }
        }
        return best;
    }
}
