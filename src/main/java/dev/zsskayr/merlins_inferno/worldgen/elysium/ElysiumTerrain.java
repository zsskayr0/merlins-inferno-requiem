package dev.zsskayr.merlins_inferno.worldgen.elysium;

import java.util.SplittableRandom;

/**
 * The shape of El'ysium, as a pure function of the world seed and a column: floating, continent-sized islands cut out of
 * thresholded noise (so their outlines are as irregular as any Minecraft landmass), whose relief follows a smooth
 * temperature/humidity climate. Biomes are chosen from the same climate by the vanilla {@code multi_noise} source, so a
 * desert is never next to a sea: the climate has to walk through plains and marsh to get there.
 *
 * <p>No Minecraft types in here on purpose: {@code tools/ElysiumPreview.java} renders it without starting the game.
 */
public final class ElysiumTerrain {
    public static final int BASE_Y = 150;
    /** Water in every basin (the Silver Sea, marsh pools, weeping lakes) stands at this height. */
    public static final int WATER_Y = 138;
    public static final int MIN_Y = 0;
    public static final int MAX_Y = 256;
    /** Every relief amplitude below is scaled by this, so the tallest peaks stop just short of the world's ceiling (the islands span y 0..250). */
    private static final double RELIEF = 0.7;

    /** Biome climate anchors (temperature, humidity): the terrain blends by these, {@code multi_noise} picks by them. */
    public static final String[] BIOMES = {"silent_fields", "silverwood", "pale_marsh", "silver_sea", "salt_waste", "mute_peaks", "weeping_plains"};
    public static final double[][] CLIMATE = {
            {0.15, -0.30}, {-0.05, 0.30}, {0.75, 0.45}, {0.20, 0.75}, {0.80, -0.60}, {-0.75, -0.15}, {-0.60, 0.75}};
    public static final int PLAINS = 0, FOREST = 1, MARSH = 2, SEA = 3, DESERT = 4, PEAKS = 5, WEEPING = 6;

    /** Blocks of "inland" per unit of the island field; tuned so ed reads roughly as blocks from the coast. */
    private static final double FIELD_SCALE = 1000.0;
    private static final double LAND_THRESHOLD = 0.07;

    /** What one column looks like: solid from {@code bottom} to {@code top} inclusive when {@code land}. */
    public static final class Column {
        public boolean land;
        public double top, bottom;
        /** Blocks inland from the coast (0 at the shore). */
        public double inland;
        public double temperature, humidity;
        public final double[] weights = new double[BIOMES.length];
        public int x = Integer.MIN_VALUE, z = Integer.MIN_VALUE;
    }

    private final Perlin warpA, warpB, isl1, isl2, isl3, altitude, temp, humid, rel1, rel2, rel3, under, pool, dune, ridge, islet;

    public final long seed;

    public ElysiumTerrain(long seed) {
        this.seed = seed;
        SplittableRandom r = new SplittableRandom(seed);
        warpA = new Perlin(r.nextLong());
        warpB = new Perlin(r.nextLong());
        isl1 = new Perlin(r.nextLong());
        isl2 = new Perlin(r.nextLong());
        isl3 = new Perlin(r.nextLong());
        altitude = new Perlin(r.nextLong());
        temp = new Perlin(r.nextLong());
        humid = new Perlin(r.nextLong());
        rel1 = new Perlin(r.nextLong());
        rel2 = new Perlin(r.nextLong());
        rel3 = new Perlin(r.nextLong());
        under = new Perlin(r.nextLong());
        pool = new Perlin(r.nextLong());
        dune = new Perlin(r.nextLong());
        ridge = new Perlin(r.nextLong());
        islet = new Perlin(r.nextLong());
    }

    /** Fills {@code c} for the column ({@code x}, {@code z}); cheap enough to call per column, cached by the callers. */
    public void sample(int x, int z, Column c) {
        c.x = x;
        c.z = z;
        double wx = x + 260.0 * warpA.fbm(x / 650.0, z / 650.0, 2);
        double wz = z + 260.0 * warpB.fbm(x / 650.0, z / 650.0, 2);
        double field = 0.78 * isl1.fbm(wx / 2000.0, wz / 2000.0, 2) + 0.17 * isl2.fbm(wx / 800.0, wz / 800.0, 2) + 0.05 * isl3.noise(wx / 260.0, wz / 260.0);
        c.inland = (field - LAND_THRESHOLD) * FIELD_SCALE;
        c.temperature = climate(temp, x, z);
        c.humidity = climate(humid, x, z);
        if (c.inland <= 0.0) {
            c.land = false;
            return;
        }
        c.land = true;
        weights(c.temperature, c.humidity, c.weights);

        double ed = c.inland;
        double w1 = rel1.fbm(x / 380.0, z / 380.0, 3);
        double w2 = rel2.fbm(x / 130.0, z / 130.0, 3);
        double rel = 0.0;
        double[] w = c.weights;
        if (w[PLAINS] > 0.005) {
            rel += w[PLAINS] * (6.0 + 12.0 * w1 + 5.0 * w2);
        }
        if (w[FOREST] > 0.005) {
            rel += w[FOREST] * (14.0 + 34.0 * w1 + 9.0 * w2);
        }
        if (w[MARSH] > 0.005) {
            double lowland = 2.0 + 3.0 * w2;
            double pools = smooth(0.12, 0.40, pool.fbm(x / 170.0, z / 170.0, 3));
            rel += w[MARSH] * (lowland - 17.0 * pools * smooth(40.0, 120.0, ed));
        }
        if (w[SEA] > 0.005) {
            double basin = -50.0 * smooth(50.0, 300.0, ed);
            double bumps = 55.0 * Math.max(0.0, islet.fbm(x / 150.0, z / 150.0, 2) - 0.12) * 4.0 * smooth(120.0, 300.0, ed);
            rel += w[SEA] * (basin + bumps + 3.0 * w2);
        }
        if (w[DESERT] > 0.005) {
            double d = 1.0 - Math.abs(dune.noise(x / 210.0 + 0.3 * z / 210.0, z / 330.0)) * 1.4;
            rel += w[DESERT] * (12.0 + 30.0 * Math.max(0.0, d) + 6.0 * w2);
        }
        if (w[PEAKS] > 0.005) {
            double r = 1.0 - Math.abs(ridge.fbm(x / 320.0, z / 320.0, 4)) * 1.7;
            double massif = smooth(40.0, 420.0, ed);
            rel += w[PEAKS] * (24.0 + 105.0 * massif * Math.pow(Math.max(0.0, r), 1.5) + 8.0 * w2);
        }
        if (w[WEEPING] > 0.005) {
            double lakes = smooth(0.18, 0.46, pool.fbm(x / 210.0 + 91.0, z / 210.0, 3));
            rel += w[WEEPING] * (8.0 + 18.0 * w1 + 5.0 * w2 - 22.0 * lakes * smooth(60.0, 160.0, ed));
        }

        double alt = 30.0 * altitude.fbm(x / 2600.0, z / 2600.0, 2);
        double interior = BASE_Y + alt + rel * RELIEF;
        // every coast rises a little above the water, so a basin is always closed in by its own shore
        double shore = WATER_Y + 5.0;
        c.top = Math.min(MAX_Y - 6.0, Math.floor(lerp(shore, Math.max(interior, shore), smooth(0.0, 90.0, ed))));

        double mountain = w[PEAKS];
        double tmax = 135.0 + 55.0 * mountain;
        double thick = 8.0 + tmax * Math.pow(smooth(0.0, 650.0, ed), 0.7);
        double rough = 40.0 * (0.5 + 0.5 * under.fbm(x / 110.0, z / 110.0, 3)) * smooth(30.0, 300.0, ed);
        c.bottom = Math.max(MIN_Y + 3.0, Math.ceil(c.top - thick - rough));
    }

    /** Density for the noise generator: positive inside an island, with a soft gradient across its skin. */
    public static double density(Column c, int y) {
        if (!c.land) {
            return -1.0;
        }
        double d = Math.min(c.top + 0.5 - y, y - c.bottom + 0.5);
        return Math.max(-1.0, Math.min(1.0, d / 5.0));
    }

    public double temperatureAt(int x, int z) {
        return climate(temp, x, z);
    }

    public double humidityAt(int x, int z) {
        return climate(humid, x, z);
    }

    private static double climate(Perlin p, int x, int z) {
        double v = 2.0 * p.fbm(x / 3400.0, z / 3400.0, 2);
        return Math.max(-1.0, Math.min(1.0, v));
    }

    /** Soft biome weights: the closer a climate anchor, the more its relief counts. */
    public static void weights(double t, double h, double[] out) {
        double sum = 0.0;
        for (int i = 0; i < CLIMATE.length; i++) {
            double dt = t - CLIMATE[i][0], dh = h - CLIMATE[i][1];
            out[i] = Math.exp(-(dt * dt + dh * dh) / (2.0 * 0.30 * 0.30));
            sum += out[i];
        }
        for (int i = 0; i < out.length; i++) {
            out[i] /= sum;
        }
    }

    public static int dominantBiome(double t, double h) {
        int best = 0;
        double bd = Double.MAX_VALUE;
        for (int i = 0; i < CLIMATE.length; i++) {
            double dt = t - CLIMATE[i][0], dh = h - CLIMATE[i][1];
            double d = dt * dt + dh * dh;
            if (d < bd) {
                bd = d;
                best = i;
            }
        }
        return best;
    }

    private static double smooth(double a, double b, double x) {
        double t = Math.max(0.0, Math.min(1.0, (x - a) / (b - a)));
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** Classic gradient noise with its own permutation table, so the whole thing depends on the seed alone. */
    static final class Perlin {
        private final int[] perm = new int[512];

        Perlin(long seed) {
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

        /** Roughly -1..1. */
        double noise(double x, double y) {
            int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
            double xf = x - xi, yf = y - yi;
            xi &= 255;
            yi &= 255;
            double u = fade(xf), v = fade(yf);
            int aa = perm[perm[xi] + yi], ab = perm[perm[xi] + yi + 1];
            int ba = perm[perm[xi + 1] + yi], bb = perm[perm[xi + 1] + yi + 1];
            double x1 = lerp(grad(aa, xf, yf), grad(ba, xf - 1, yf), u);
            double x2 = lerp(grad(ab, xf, yf - 1), grad(bb, xf - 1, yf - 1), u);
            return lerp(x1, x2, v);
        }

        /** Octaves normalised to about -1..1 (Perlin rarely strays past +-0.7, so it is stretched). */
        double fbm(double x, double y, int octaves) {
            double amp = 1.0, sum = 0.0, norm = 0.0;
            for (int i = 0; i < octaves; i++) {
                sum += amp * noise(x, y);
                norm += amp;
                x *= 2.0;
                y *= 2.0;
                amp *= 0.5;
            }
            return Math.max(-1.0, Math.min(1.0, 1.35 * sum / norm));
        }
    }
}
