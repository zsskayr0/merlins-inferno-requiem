package dev.zsskayr.merlins_inferno.worldgen.elysium.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One river (or rim notch) of an island: a smooth polyline from its head pool to just past the coast, with a
 * per-vertex half width and an integer water level that only ever steps down by one block per vertex. Immutable once
 * {@link #finish} has built the bucket index, so it is shared freely between threads.
 *
 * <p>Vertices are {@link #STEP} blocks apart and the walk turns slowly, so the tube of influence has a unique nearest
 * point and two neighbouring columns never see levels that differ by more than one terrace.
 */
public final class River {
    /** How the banks are shaped. */
    public enum Kind { VALLEY, CANYON, GORGE, NOTCH }

    public static final double STEP = 4.0;
    private static final int BUCKET_SHIFT = 5;

    public final Kind kind;
    public final double[] xs, zs, s, w;
    public final int[] level;
    /** Arc length where the polyline leaves the island outline (taper ends here). */
    public final double sCoast;
    /** Max distance from the polyline (beyond the half width) where the banks still change the terrain. */
    public final double influence;
    private Map<Long, int[]> buckets;
    public int minX, minZ, maxX, maxZ;

    River(Kind kind, double[] xs, double[] zs, double[] s, double[] w, int[] level, double sCoast, double influence) {
        this.kind = kind;
        this.xs = xs;
        this.zs = zs;
        this.s = s;
        this.w = w;
        this.level = level;
        this.sCoast = sCoast;
        this.influence = influence;
    }

    public int size() {
        return xs.length;
    }

    /** Builds the bucket index of segments by their influence box. Call once before sharing. */
    void finish() {
        Map<Long, List<Integer>> tmp = new HashMap<>();
        minX = Integer.MAX_VALUE;
        minZ = Integer.MAX_VALUE;
        maxX = Integer.MIN_VALUE;
        maxZ = Integer.MIN_VALUE;
        for (int i = 0; i + 1 < xs.length; i++) {
            double r = Math.max(w[i], w[i + 1]) + influence + 2;
            int x0 = (int) Math.floor(Math.min(xs[i], xs[i + 1]) - r), x1 = (int) Math.ceil(Math.max(xs[i], xs[i + 1]) + r);
            int z0 = (int) Math.floor(Math.min(zs[i], zs[i + 1]) - r), z1 = (int) Math.ceil(Math.max(zs[i], zs[i + 1]) + r);
            minX = Math.min(minX, x0);
            minZ = Math.min(minZ, z0);
            maxX = Math.max(maxX, x1);
            maxZ = Math.max(maxZ, z1);
            for (int bx = x0 >> BUCKET_SHIFT; bx <= x1 >> BUCKET_SHIFT; bx++) {
                for (int bz = z0 >> BUCKET_SHIFT; bz <= z1 >> BUCKET_SHIFT; bz++) {
                    tmp.computeIfAbsent(key(bx, bz), k -> new ArrayList<>()).add(i);
                }
            }
        }
        Map<Long, int[]> out = new HashMap<>(tmp.size() * 2);
        for (Map.Entry<Long, List<Integer>> e : tmp.entrySet()) {
            out.put(e.getKey(), e.getValue().stream().mapToInt(Integer::intValue).toArray());
        }
        buckets = out;
    }

    private static long key(int bx, int bz) {
        return ((long) bx << 32) ^ (bz & 0xffffffffL);
    }

    /** Result of {@link #query}: e = distance beyond the half width (negative inside the water). */
    public static final class Hit {
        public double e = Double.POSITIVE_INFINITY, s, halfWidth;
        public int level;
        public River river;
    }

    /** Nearest point of this river to (x, z); updates {@code hit} only when closer (by e) than what it holds. */
    public void query(double x, double z, Hit hit) {
        if (x < minX || x > maxX || z < minZ || z > maxZ) {
            return;
        }
        int[] segs = buckets.get(key((int) Math.floor(x) >> BUCKET_SHIFT, (int) Math.floor(z) >> BUCKET_SHIFT));
        if (segs == null) {
            return;
        }
        double bestD2 = Double.POSITIVE_INFINITY, bestT = 0;
        int best = -1;
        for (int i : segs) {
            double ax = xs[i], az = zs[i], dx = xs[i + 1] - ax, dz = zs[i + 1] - az;
            double len2 = dx * dx + dz * dz;
            double t = len2 > 0 ? ((x - ax) * dx + (z - az) * dz) / len2 : 0;
            t = t < 0 ? 0 : (t > 1 ? 1 : t);
            double px = ax + dx * t - x, pz = az + dz * t - z;
            double d2 = px * px + pz * pz;
            if (d2 < bestD2) {
                bestD2 = d2;
                best = i;
                bestT = t;
            }
        }
        if (best < 0) {
            return;
        }
        double hw = w[best] + (w[best + 1] - w[best]) * bestT;
        double e = Math.sqrt(bestD2) - hw;
        if (e > influence || e >= hit.e) {
            return;
        }
        hit.e = e;
        hit.s = s[best] + (s[best + 1] - s[best]) * bestT;
        hit.halfWidth = hw;
        // terrace = level of the vertex nearest along the line, so steps sit midway between vertices
        hit.level = bestT < 0.5 ? level[best] : level[best + 1];
        hit.river = this;
    }

    /** Smallest distance between any vertices of two rivers (cheap overlap test at plan time). */
    static double minDistance(River a, River b) {
        double best = Double.POSITIVE_INFINITY;
        for (int i = 0; i < a.xs.length; i++) {
            for (int j = 0; j < b.xs.length; j++) {
                double dx = a.xs[i] - b.xs[j], dz = a.zs[i] - b.zs[j];
                best = Math.min(best, dx * dx + dz * dz);
            }
        }
        return Math.sqrt(best);
    }

    /** Smallest distance from a point to this river's vertices. */
    double distanceTo(double x, double z) {
        double best = Double.POSITIVE_INFINITY;
        for (int i = 0; i < xs.length; i++) {
            double dx = xs[i] - x, dz = zs[i] - z;
            best = Math.min(best, dx * dx + dz * dz);
        }
        return Math.sqrt(best);
    }
}
