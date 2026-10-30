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
    /** Slashes: one may lie in each cell of this size; the longest reaches well under a cell, so the 3x3 around a column is enough. */
    private static final int SLASH_CELL = 900;
    private static final double SLASH_CHANCE = 0.28;
    /** Crossings: now and then two long slashes strike the same point, in an X that ties the island in four. */
    private static final int CROSS_CELL = 2200;
    private static final double CROSS_CHANCE = 0.3;
    /** The share of slashes that stop short of the underside: a trench with a floor, not a gap. */
    private static final double SHALLOW_SHARE = 0.4;
    /** How far the wall zone of a slash reaches into the rock, past which the strata and alcoves are gone. */
    public static final double WALL_ZONE = 30.0;

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

    private final Perlin warpA, warpB, isl1, isl2, isl3, altitude, temp, humid, rel1, rel2, rel3, under, pool, dune, ridge, islet, hole;

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
        hole = new Perlin(r.nextLong());
    }

    /** Blocks inland from the coast (zero or less over open sky): the island field with the slashes cut out, much cheaper than a full {@link #sample}. */
    public double inlandAt(int x, int z) {
        return Math.min(baseInlandAt(x, z), slashDist(x, z));
    }

    private static long mix(long h) {
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h;
    }

    private static double unit(long h) {
        return (h >>> 11) / (double) (1L << 53);
    }

    /** Blocks from the nearest slash that goes right through its island (negative inside), huge far from any. */
    private double slashDist(int x, int z) {
        return scan(x, z, 0, null);
    }

    /**
     * Blocks from the nearest slash of either kind, negative inside it. {@code out[1]} gets that slash's depth below the
     * rim, or infinity if it goes clean through.
     */
    public double nearestSlash(int x, int z, double[] out) {
        return scan(x, z, 2, out);
    }

    /** How deep below the rim a column inside a shallow slash lies. */
    public static double shallowDrop(double depth, double noise) {
        return depth * (0.95 + 0.05 * noise);
    }

    public double shallowNoise(int x, int z) {
        return hole.noise(x / 26.0 + 3.0, z / 26.0);
    }

    /**
     * The slashes: blade-shaped cuts, long and slightly bowed, that strike an island as if something had hit it. Most go
     * clean through (the walls stay as high as the rock around them and run all the way down the underside); the others
     * are trenches that stop a few dozen blocks down. {@code mode}: 0 through only, 1 shallow only, 2 both.
     */
    private double scan(int x, int z, int mode, double[] out) {
        int ci = Math.floorDiv(x, SLASH_CELL), cj = Math.floorDiv(z, SLASH_CELL);
        double best = 1.0e9;
        for (int i = ci - 1; i <= ci + 1; i++) {
            for (int j = cj - 1; j <= cj + 1; j++) {
                long h = mix(seed ^ (i * 0x9E3779B97F4A7C15L) ^ (j * 0xC2B2AE3D27D4EB4FL));
                if (unit(h) > SLASH_CHANCE) {
                    continue;
                }
                double cx = (i + unit(h = mix(h + 1))) * SLASH_CELL, cz = (j + unit(h = mix(h + 1))) * SLASH_CELL;
                double ang = unit(h = mix(h + 1)) * Math.PI;
                double half = 300.0 + 400.0 * unit(h = mix(h + 1));
                double wide = 18.0 + 24.0 * unit(h = mix(h + 1));
                double bend = (unit(h = mix(h + 1)) * 2.0 - 1.0) * 0.14 * half;
                boolean shallow = unit(h = mix(h + 1)) < SHALLOW_SHARE;
                double depth = 22.0 + 48.0 * unit(h = mix(h + 1));
                if (mode == 0 && shallow || mode == 1 && !shallow) {
                    continue;
                }
                best = blade(x, z, cx, cz, ang, half, wide, bend, shallow ? depth : Double.POSITIVE_INFINITY, i, j, out, best);
            }
        }
        if (mode == 1) {
            return best;
        }
        int ki = Math.floorDiv(x, CROSS_CELL), kj = Math.floorDiv(z, CROSS_CELL);
        for (int i = ki - 1; i <= ki + 1; i++) {
            for (int j = kj - 1; j <= kj + 1; j++) {
                long h = mix(seed * 31L ^ (i * 0xA24BAED4963EE407L) ^ (j * 0x9FB21C651E98DF25L));
                if (unit(h) > CROSS_CHANCE) {
                    continue;
                }
                double cx = (i + 0.2 + 0.6 * unit(h = mix(h + 1))) * CROSS_CELL, cz = (j + 0.2 + 0.6 * unit(h = mix(h + 1))) * CROSS_CELL;
                double ang = unit(h = mix(h + 1)) * Math.PI;
                double gap = 0.6 + 0.9 * unit(h = mix(h + 1));
                for (int k = 0; k < 2; k++) {
                    double half = 450.0 + 350.0 * unit(h = mix(h + 1));
                    double wide = 20.0 + 18.0 * unit(h = mix(h + 1));
                    double bend = (unit(h = mix(h + 1)) * 2.0 - 1.0) * 0.10 * half;
                    best = blade(x, z, cx, cz, ang + k * gap, half, wide, bend, Double.POSITIVE_INFINITY, i + 1000 * (k + 1), j, out, best);
                }
            }
        }
        return best;
    }

    /** Distance from (x, z) to one blade, folded into {@code best}. */
    private double blade(int x, int z, double cx, double cz, double ang, double half, double wide, double bend, double depth,
                         int saltI, int saltJ, double[] out, double best) {
        double dx = x - cx, dz = z - cz;
        if (dx * dx + dz * dz > (1.25 * half + wide) * (1.25 * half + wide)) {
            return best;
        }
        double ca = Math.cos(ang), sa = Math.sin(ang);
        double s = dx * ca + dz * sa;
        if (Math.abs(s) >= half) {
            return best;
        }
        double t = s / half;
        double w = -dx * sa + dz * ca - bend * t * t;
        // worn edges: slow swells along the cut, ragged bites and spurs a few blocks across
        double width = wide * Math.pow(1.0 - t * t, 0.6) * (1.0 + 0.16 * hole.noise(s / 70.0 + saltI * 7.3, saltJ * 3.1 + 0.5))
                + 4.5 * hole.noise(x / 11.0 + 40.0, z / 11.0) + 2.0 * hole.noise(x / 4.5, z / 4.5 + 17.0);
        double dist = Math.abs(w) - width;
        if (dist < best) {
            if (out != null) {
                out[0] = dist;
                out[1] = depth;
            }
            return dist;
        }
        return best;
    }

    /** The strata, ledges and marks of a slash wall at one column; see {@link #wall}. */
    public final class Wall {
        private final double top, floor, shift, vary, alcove, cy;

        private Wall(int x, int z, double top, double floor) {
            this.top = top;
            this.floor = floor;
            shift = 14.0 * hole.noise(x / 70.0 + 5.0, z / 70.0);
            vary = 0.5 + 0.5 * hole.noise(x / 30.0, z / 30.0 + 9.0);
            alcove = smooth(0.05, 0.3, hole.noise(x / 45.0 + 30.0, z / 45.0));
            cy = top - 40.0 - 25.0 * hole.noise(x / 110.0, z / 110.0 + 3.0);
        }

        /** Which rock layer height {@code y} lies in (0..7): decides how far it stands back and what it is made of. */
        public int band(int y) {
            int k = (int) Math.floor((y + shift) / 6.0);
            return (int) (unit(mix(seed + k * 0x9E3779B97F4A7C15L)) * 8.0);
        }

        private static final double[] STANDBACK = {0, 1, 3, 5, 7, 10, 0, 3};

        /** How far the wall has worn back at height {@code y}: a few blocks by layer, a deep balcony where an alcove cuts in. */
        public double recess(int y) {
            double gate = smooth(6.0, 16.0, top - y) * smooth(floor + 2.0, floor + 10.0, y);
            if (gate <= 0.0) {
                return 0.0;
            }
            double r = STANDBACK[band(y)] * (0.6 + 0.4 * vary);
            double u = 6.5 - Math.abs(y - cy);
            if (alcove > 0.0 && u > 0.0) {
                r += 14.0 * alcove * Math.min(1.0, u / 1.5);
            }
            return r * gate;
        }
    }

    /** {@code floor} is the lowest block the wall may stand back at (use a very low number for a slash that goes through). */
    public Wall wall(int x, int z, double top, double floor) {
        return new Wall(x, z, top, floor);
    }

    /** How far below the rim the glowing strike line runs along a wall, and whether it is lit here. */
    public int markDepth(int x, int z) {
        return 26 + (int) (10.0 * hole.noise(x / 130.0 + 70.0, z / 130.0));
    }

    public boolean markLit(int x, int z) {
        return hole.noise(x / 25.0 + 12.0, z / 25.0 + 88.0) > -0.35;
    }

    /** Thin dark fissures running down a wall. */
    public boolean crack(int x, int y, int z) {
        return Math.abs(hole.noise((x * 0.6 + z * 0.8) / 16.0 + 55.0, y / 24.0)) < 0.035;
    }

    public double baseInlandAt(int x, int z) {
        double wx = x + 260.0 * warpA.fbm(x / 650.0, z / 650.0, 2);
        double wz = z + 260.0 * warpB.fbm(x / 650.0, z / 650.0, 2);
        double field = 0.78 * isl1.fbm(wx / 2000.0, wz / 2000.0, 2) + 0.17 * isl2.fbm(wx / 800.0, wz / 800.0, 2) + 0.05 * isl3.noise(wx / 260.0, wz / 260.0);
        return (field - LAND_THRESHOLD) * FIELD_SCALE;
    }

    /** Fills {@code c} for the column ({@code x}, {@code z}); cheap enough to call per column, cached by the callers. */
    public void sample(int x, int z, Column c) {
        c.x = x;
        c.z = z;
        double base = baseInlandAt(x, z);
        c.inland = Math.min(base, slashDist(x, z));
        c.temperature = climate(temp, x, z);
        c.humidity = climate(humid, x, z);
        if (base <= 0.0) {
            c.land = false;
            return;
        }
        // inside a slash the column is open sky, but its top and bottom are still worked out: they say where the rock
        // around it stands
        c.land = c.inland > 0.0;
        weights(c.temperature, c.humidity, c.weights);

        // shape the island as if the slash were not there: its walls then stand as tall and as deep as the rock around them
        double ed = base;
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
            double r = 1.0 - Math.abs(ridge.fbm(x / 210.0, z / 210.0, 4)) * 1.7;
            double massif = smooth(40.0, 420.0, ed);
            rel += w[PEAKS] * (4.0 + 125.0 * massif * Math.pow(Math.max(0.0, r), 2.9) + 8.0 * w2);
        }
        if (w[WEEPING] > 0.005) {
            double lakes = smooth(0.18, 0.46, pool.fbm(x / 210.0 + 91.0, z / 210.0, 3));
            rel += w[WEEPING] * (8.0 + 18.0 * w1 + 5.0 * w2 - 22.0 * lakes * smooth(60.0, 160.0, ed));
        }

        double alt = 30.0 * altitude.fbm(x / 2600.0, z / 2600.0, 2);
        double interior = softCeiling(BASE_Y + alt + rel * RELIEF);
        // every coast rises a little above the water, so a basin is always closed in by its own shore
        double shore = WATER_Y + 5.0;
        c.top = Math.min(MAX_Y - 6.0, Math.floor(lerp(shore, Math.max(interior, shore), smooth(0.0, 90.0, ed))));

        double mountain = w[PEAKS];
        double tmax = 135.0 + 55.0 * mountain;
        // the rim drops away almost sheer in the first few dozen blocks, then the underside tapers like a cone
        double cliff = Math.sqrt(smooth(0.0, 30.0, ed));
        double thick = 8.0 + tmax * (0.6 * cliff + 0.4 * Math.pow(smooth(0.0, 650.0, ed), 0.7));
        double rough = 40.0 * (0.5 + 0.5 * under.fbm(x / 110.0, z / 110.0, 3)) * smooth(30.0, 300.0, ed);
        c.bottom = Math.max(MIN_Y + 3.0, Math.ceil(c.top - thick - rough));
        if (c.land) {
            double[] shallow = new double[2];
            if (scan(x, z, 1, shallow) <= 0.0) {
                c.top = Math.max(c.bottom + 8.0, c.top - Math.floor(shallowDrop(shallow[1], shallowNoise(x, z))));
            }
        }
    }

    /** Eases the tallest summits into the ceiling instead of slicing them flat against it. */
    private static double softCeiling(double y) {
        double knee = MAX_Y - 40.0;
        return y <= knee ? y : knee + 28.0 * Math.tanh((y - knee) / 28.0);
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
