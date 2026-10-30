package dev.zsskayr.merlins_inferno.worldgen.elysium;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where El'ysium's islands are tied together: a bridge across each short gap and a pair of giant chains holding the
 * two undersides. Spans are found, not placed by hand: every region cell throws a few rays from a random inland point,
 * and a ray that crosses open sky and lands on another island becomes a span. Pure function of the terrain, so every
 * chunk that a span passes through finds the same one.
 *
 * <p>There are two layers of the same search: a fine one for the many short crossings, and a coarse one whose rays go
 * on for up to two and a half kilometres, so that a few chains reach right across the void and show where the next
 * island lies.
 */
public final class ElysiumSpans {
    /** One crossing: shore to shore (A and B at ground level), plus the undersides the chains hang from. */
    public record Span(double ax, double ay, double az, double bx, double by, double bz, double aBottom, double bBottom, double length) {
        public boolean hasBridge() {
            return length <= BRIDGE_MAX_GAP && Math.abs(by - ay) <= 0.4 * length;
        }
    }

    public static final int BRIDGE_MAX_GAP = 300;
    private static final int MIN_GAP = 48;
    private static final double STEP = 12.0;
    /** A ray inside its own island strides this far at a time. */
    private static final double INLAND_STEP = 24.0;
    /** Beyond this much open sky a ray takes bigger strides: the far island is a continent, not a sliver. */
    private static final double FINE_GAP = 300.0;
    private static final double COARSE_STEP = 40.0;

    /** One search: how big its cells are, how many rays each throws, which gaps it accepts and how far apart survivors stay. */
    private record Layer(int cell, int rays, int minGap, int maxGap, double maxRay, double crowd, long salt) {
        int reach() {
            return (int) maxRay + maxGap + cell;
        }
    }

    private static final Layer SHORT = new Layer(512, 24, MIN_GAP, 560, 700.0, 170.0, 0L);
    private static final Layer LONG = new Layer(1024, 12, 560, 2000, 450.0, 900.0, 0x5DEECE66DL);
    /** The longest crossing there can be. */
    public static final int MAX_GAP = LONG.maxGap();

    private final ElysiumTerrain terrain;
    private final ConcurrentHashMap<Long, List<Span>> shortCells = new ConcurrentHashMap<>(), shortCandidates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, List<Span>> longCells = new ConcurrentHashMap<>(), longCandidates = new ConcurrentHashMap<>();

    public ElysiumSpans(ElysiumTerrain terrain) {
        this.terrain = terrain;
    }

    /** Every span that may touch the box (endpoints within reach of it), found cell by cell. */
    public List<Span> near(int minX, int minZ, int maxX, int maxZ) {
        List<Span> out = new ArrayList<>();
        collect(SHORT, shortCells, shortCandidates, minX, minZ, maxX, maxZ, out);
        collect(LONG, longCells, longCandidates, minX, minZ, maxX, maxZ, out);
        return out;
    }

    private void collect(Layer layer, ConcurrentHashMap<Long, List<Span>> cells, ConcurrentHashMap<Long, List<Span>> candidates,
                         int minX, int minZ, int maxX, int maxZ, List<Span> out) {
        // a span can start in a cell as far away as a full ray plus the gap, so look that far for the ones that end here
        int reach = layer.reach();
        int i0 = Math.floorDiv(minX - reach, layer.cell()), i1 = Math.floorDiv(maxX + reach, layer.cell());
        int j0 = Math.floorDiv(minZ - reach, layer.cell()), j1 = Math.floorDiv(maxZ + reach, layer.cell());
        for (int i = i0; i <= i1; i++) {
            for (int j = j0; j <= j1; j++) {
                for (Span s : cell(layer, cells, candidates, i, j)) {
                    double lo = Math.min(s.ax, s.bx) - 60, hi = Math.max(s.ax, s.bx) + 60;
                    double lo2 = Math.min(s.az, s.bz) - 60, hi2 = Math.max(s.az, s.bz) + 60;
                    if (hi >= minX && lo <= maxX && hi2 >= minZ && lo2 <= maxZ) {
                        out.add(s);
                    }
                }
            }
        }
    }

    private static long key(int i, int j) {
        return ((long) i << 32) ^ (j & 0xffffffffL);
    }

    /** The spans a cell keeps: its candidates that no better candidate in the 3x3 cells around it crowds out. */
    private List<Span> cell(Layer layer, ConcurrentHashMap<Long, List<Span>> cells, ConcurrentHashMap<Long, List<Span>> candidates, int i, int j) {
        if (cells.size() > 4096) {
            cells.clear();
        }
        return cells.computeIfAbsent(key(i, j), k -> {
            List<Span> mine = candidates(layer, candidates, i, j);
            List<Span> kept = new ArrayList<>();
            for (Span c : mine) {
                boolean crowded = false;
                for (int di = -1; di <= 1 && !crowded; di++) {
                    for (int dj = -1; dj <= 1 && !crowded; dj++) {
                        for (Span o : candidates(layer, candidates, i + di, j + dj)) {
                            if (o != c && priority(o) > priority(c) && crowds(o, c, layer.crowd())) {
                                crowded = true;
                                break;
                            }
                        }
                    }
                }
                if (!crowded) {
                    kept.add(c);
                }
            }
            return kept;
        });
    }

    /** Longer crossings win, with a random tiebreak, so the survivors are spread out and tend to be the long ones. */
    private static double priority(Span s) {
        long h = (long) s.ax() * 73856093L ^ (long) s.az() * 19349663L ^ (long) s.bx() * 83492791L ^ (long) s.bz() * 2654435761L;
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        double random = (h >>> 11) / (double) (1L << 53);
        return 0.55 * random + 0.45 * Math.min(s.length(), 2400.0) / 2400.0;
    }

    private static boolean crowds(Span a, Span b, double crowd) {
        double mx = (a.ax() + a.bx() - b.ax() - b.bx()) / 2.0, mz = (a.az() + a.bz() - b.az() - b.bz()) / 2.0;
        return Math.hypot(mx, mz) < crowd;
    }

    private List<Span> candidates(Layer layer, ConcurrentHashMap<Long, List<Span>> candidates, int i, int j) {
        if (candidates.size() > 4096) {
            candidates.clear();
        }
        return candidates.computeIfAbsent(key(i, j), k -> compute(layer, i, j));
    }

    private List<Span> compute(Layer layer, int i, int j) {
        final int CELL = layer.cell(), RAYS_PER_CELL = layer.rays(), MAX_GAP = layer.maxGap();
        SplittableRandom rng = new SplittableRandom(terrain.seed * 0x9E3779B97F4A7C15L + layer.salt() + i * 341873128712L + j * 132897987541L);
        List<Span> found = new ArrayList<>();
        ElysiumTerrain.Column c = new ElysiumTerrain.Column();
        ElysiumTerrain.Column d = new ElysiumTerrain.Column();
        for (int k = 0; k < RAYS_PER_CELL; k++) {
            double px = (i + rng.nextDouble()) * CELL, pz = (j + rng.nextDouble()) * CELL;
            double theta = rng.nextDouble() * 2.0 * Math.PI;
            double dx = Math.cos(theta), dz = Math.sin(theta);
            if (terrain.inlandAt((int) Math.floor(px), (int) Math.floor(pz)) < 40.0) {
                continue;
            }
            double t = 0.0;
            boolean left = false;
            while (t < layer.maxRay()) {
                t += INLAND_STEP;
                if (terrain.inlandAt((int) Math.floor(px + dx * t), (int) Math.floor(pz + dz * t)) <= 0.0) {
                    left = true;
                    break;
                }
            }
            if (!left) {
                continue;
            }
            // the shore lies between the last land sample and this one: walk in on it
            double shoreLo = t - INLAND_STEP, shoreHi = t;
            for (int n = 0; n < 5 && shoreHi - shoreLo > 1.5; n++) {
                double mid = (shoreLo + shoreHi) / 2.0;
                if (terrain.inlandAt((int) Math.floor(px + dx * mid), (int) Math.floor(pz + dz * mid)) > 0.0) {
                    shoreLo = mid;
                } else {
                    shoreHi = mid;
                }
            }
            double tA = shoreLo;
            t = shoreHi;
            double gapStart = t;
            boolean landed = false;
            while (t - gapStart < MAX_GAP) {
                double stride = t - gapStart < FINE_GAP ? STEP : COARSE_STEP;
                t += stride;
                if (terrain.inlandAt((int) Math.floor(px + dx * t), (int) Math.floor(pz + dz * t)) > 0.0) {
                    landed = true;
                    // a long stride overshoots the shore: walk back to it
                    double lo = t - stride, hi = t;
                    for (int n = 0; n < 6 && hi - lo > 1.5; n++) {
                        double mid = (lo + hi) / 2.0;
                        if (terrain.inlandAt((int) Math.floor(px + dx * mid), (int) Math.floor(pz + dz * mid)) > 0.0) {
                            hi = mid;
                        } else {
                            lo = mid;
                        }
                    }
                    t = hi;
                    break;
                }
            }
            double gap = t - tA;
            if (!landed || gap < layer.minGap()) {
                continue;
            }
            // the far shore must be real ground, not a sliver
            terrain.sample((int) Math.floor(px + dx * (t + 28)), (int) Math.floor(pz + dz * (t + 28)), d);
            if (!d.land || d.inland < 12.0) {
                continue;
            }
            double ax = px + dx * tA, az = pz + dz * tA, bx = px + dx * t, bz = pz + dz * t;
            ElysiumTerrain.Column a = new ElysiumTerrain.Column(), b = new ElysiumTerrain.Column();
            terrain.sample((int) Math.floor(ax), (int) Math.floor(az), a);
            terrain.sample((int) Math.floor(bx), (int) Math.floor(bz), b);
            if (!a.land || !b.land || a.top < ElysiumTerrain.WATER_Y + 3 || b.top < ElysiumTerrain.WATER_Y + 3 || Math.abs(a.top - b.top) > 140.0) {
                continue;
            }
            // the chains hang from the rock a little way in from each shore
            ElysiumTerrain.Column ua = new ElysiumTerrain.Column(), ub = new ElysiumTerrain.Column();
            terrain.sample((int) Math.floor(ax - dx * 14), (int) Math.floor(az - dz * 14), ua);
            terrain.sample((int) Math.floor(bx + dx * 14), (int) Math.floor(bz + dz * 14), ub);
            if (!ua.land || !ub.land) {
                continue;
            }
            found.add(new Span(ax, a.top, az, bx, b.top, bz, ua.bottom + 8.0, ub.bottom + 8.0, gap));
        }
        return found;
    }
}
