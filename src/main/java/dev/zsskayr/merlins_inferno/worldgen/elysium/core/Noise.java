package dev.zsskayr.merlins_inferno.worldgen.elysium.core;

import java.util.SplittableRandom;

/**
 * Seeded 2D gradient noise plus the integer hashes the core uses for every random decision. Immutable after
 * construction, so one instance is shared by all chunk-gen threads. Per-island variation comes from coordinate
 * offsets (see {@link IslandPlan}), not from separate permutation tables.
 */
public final class Noise {
    private final int[] perm = new int[512];

    public Noise(long seed) {
        SplittableRandom r = new SplittableRandom(seed);
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) {
            p[i] = i;
        }
        for (int i = 255; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int t = p[i];
            p[i] = p[j];
            p[j] = t;
        }
        for (int i = 0; i < 512; i++) {
            perm[i] = p[i & 255];
        }
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private static double grad(int h, double x, double y) {
        switch (h & 7) {
            case 0: return x + y;
            case 1: return -x + y;
            case 2: return x - y;
            case 3: return -x - y;
            case 4: return x;
            case 5: return -x;
            case 6: return y;
            default: return -y;
        }
    }

    /** Single octave, roughly -1..1 (stretched so it reaches the ends occasionally). */
    public double noise(double x, double y) {
        double fx = Math.floor(x), fy = Math.floor(y);
        int xi = (int) fx & 255, yi = (int) fy & 255;
        double xf = x - fx, yf = y - fy;
        double u = fade(xf), v = fade(yf);
        int a = perm[xi] + yi, b = perm[xi + 1] + yi;
        int aa = perm[a], ab = perm[a + 1], ba = perm[b], bb = perm[b + 1];
        double x1 = grad(aa, xf, yf) + u * (grad(ba, xf - 1, yf) - grad(aa, xf, yf));
        double x2 = grad(ab, xf, yf - 1) + u * (grad(bb, xf - 1, yf - 1) - grad(ab, xf, yf - 1));
        return 1.4 * (x1 + v * (x2 - x1));
    }

    /** Octave sum normalised to about -1..1, clamped. */
    public double fbm(double x, double y, int octaves) {
        double amp = 1.0, sum = 0.0, norm = 0.0;
        for (int i = 0; i < octaves; i++) {
            sum += amp * noise(x, y);
            norm += amp;
            x = x * 2.03 + 17.1;
            y = y * 2.03 - 9.7;
            amp *= 0.5;
        }
        double v = sum / norm;
        return v < -1 ? -1 : (v > 1 ? 1 : v);
    }

    /** Ridged noise in 0..1, 1 on the ridge lines. */
    public double ridged(double x, double y, int octaves) {
        double amp = 1.0, sum = 0.0, norm = 0.0;
        for (int i = 0; i < octaves; i++) {
            double n = 1.0 - Math.abs(noise(x, y));
            sum += amp * n * n;
            norm += amp;
            x = x * 2.03 + 31.3;
            y = y * 2.03 + 5.1;
            amp *= 0.5;
        }
        return Math.min(1.0, sum / norm);
    }

    // ------------------------------------------------------------------ hashes

    public static long mix(long h) {
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h;
    }

    public static long hash(long seed, long a, long b) {
        return mix(seed ^ mix(a * 0x9E3779B97F4A7C15L + b * 0xC2B2AE3D27D4EB4FL + 0x165667B19E3779F9L));
    }

    public static long hash(long seed, long a, long b, long c) {
        return mix(hash(seed, a, b) ^ (c * 0xD6E8FEB86659FD93L));
    }

    /** Uniform [0,1) from a hash. */
    public static double unit(long h) {
        return (h >>> 11) * 0x1.0p-53;
    }

    public static double smoothstep(double e0, double e1, double x) {
        double t = (x - e0) / (e1 - e0);
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        return t * t * (3 - 2 * t);
    }

    public static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
