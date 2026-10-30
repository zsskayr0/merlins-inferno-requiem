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
 */
public final class ElysiumSpans {
    /** One crossing: shore to shore (A and B at ground level), plus the undersides the chains hang from. */
    public record Span(double ax, double ay, double az, double bx, double by, double bz, double aBottom, double bBottom, double length) {
        public boolean hasBridge() {
            return length <= BRIDGE_MAX_GAP;
        }
    }

    public static final int CELL = 512;
    public static final int MAX_GAP = 420;
    public static final int BRIDGE_MAX_GAP = 300;
    private static final int MIN_GAP = 48;
    private static final int RAYS_PER_CELL = 8;
    private static final double STEP = 12.0;

    private final ElysiumTerrain terrain;
    private final ConcurrentHashMap<Long, List<Span>> cells = new ConcurrentHashMap<>();

    public ElysiumSpans(ElysiumTerrain terrain) {
        this.terrain = terrain;
    }

    /** Every span that may touch the box (endpoints within reach of it), found cell by cell. */
    public List<Span> near(int minX, int minZ, int maxX, int maxZ) {
        int reach = MAX_GAP + 80;
        int i0 = Math.floorDiv(minX - reach, CELL), i1 = Math.floorDiv(maxX + reach, CELL);
        int j0 = Math.floorDiv(minZ - reach, CELL), j1 = Math.floorDiv(maxZ + reach, CELL);
        List<Span> out = new ArrayList<>();
        for (int i = i0; i <= i1; i++) {
            for (int j = j0; j <= j1; j++) {
                for (Span s : cell(i, j)) {
                    double lo = Math.min(s.ax, s.bx) - 60, hi = Math.max(s.ax, s.bx) + 60;
                    double lo2 = Math.min(s.az, s.bz) - 60, hi2 = Math.max(s.az, s.bz) + 60;
                    if (hi >= minX && lo <= maxX && hi2 >= minZ && lo2 <= maxZ) {
                        out.add(s);
                    }
                }
            }
        }
        return out;
    }

    private List<Span> cell(int i, int j) {
        if (cells.size() > 4096) {
            cells.clear();
        }
        return cells.computeIfAbsent(((long) i << 32) ^ (j & 0xffffffffL), k -> compute(i, j));
    }

    private List<Span> compute(int i, int j) {
        SplittableRandom rng = new SplittableRandom(terrain.seed * 0x9E3779B97F4A7C15L + i * 341873128712L + j * 132897987541L);
        List<Span> found = new ArrayList<>();
        ElysiumTerrain.Column c = new ElysiumTerrain.Column();
        ElysiumTerrain.Column d = new ElysiumTerrain.Column();
        for (int k = 0; k < RAYS_PER_CELL; k++) {
            double px = (i + rng.nextDouble()) * CELL, pz = (j + rng.nextDouble()) * CELL;
            double theta = rng.nextDouble() * 2.0 * Math.PI;
            double dx = Math.cos(theta), dz = Math.sin(theta);
            terrain.sample((int) Math.floor(px), (int) Math.floor(pz), c);
            if (!c.land || c.inland < 40.0) {
                continue;
            }
            double t = 0.0;
            boolean left = false;
            while (t < 700.0) {
                t += STEP;
                terrain.sample((int) Math.floor(px + dx * t), (int) Math.floor(pz + dz * t), c);
                if (!c.land) {
                    left = true;
                    break;
                }
            }
            if (!left) {
                continue;
            }
            double tA = t - STEP;
            double gapStart = t;
            boolean landed = false;
            while (t - gapStart < MAX_GAP) {
                t += STEP;
                terrain.sample((int) Math.floor(px + dx * t), (int) Math.floor(pz + dz * t), c);
                if (c.land) {
                    landed = true;
                    break;
                }
            }
            double gap = t - tA;
            if (!landed || gap < MIN_GAP) {
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
            if (!a.land || !b.land || a.top < ElysiumTerrain.WATER_Y + 3 || b.top < ElysiumTerrain.WATER_Y + 3 || Math.abs(a.top - b.top) > 70.0) {
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
