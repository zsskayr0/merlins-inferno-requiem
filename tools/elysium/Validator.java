import dev.zsskayr.merlins_inferno.worldgen.elysium.core.Column;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumBiome;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumWorld;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.Mat;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Voxel-level validator for the El'ysium core. For each seed it picks one whole island of every biome near the origin
 * (plus a few Chainfall platforms and the 512x512 square at the origin), samples every column exactly as the chunk
 * generator will (solid bottom..top via material(), water top+1..waterTop, arch, hang) and checks the architecture's
 * rules: held water, water steps <= 1, gravity blocks over air, floating fragments (6-connectivity on column
 * intervals), height bounds, spring placement, narrow spills, determinism across threads, and per-column timing.
 *
 * <p>Usage: {@code java -cp corebuild:tools Validator [seed...]}. Exit code 1 on any violation.
 */
public final class Validator {
    private static final Map<String, Long> VIOL = new LinkedHashMap<>();
    private static final Map<String, String> EXAMPLE = new HashMap<>();
    private static final Map<String, Long> STAT = new LinkedHashMap<>();

    private static void viol(String k, String ex) {
        VIOL.merge(k, 1L, Long::sum);
        EXAMPLE.putIfAbsent(k, ex);
    }

    private static void stat(String k, long v) {
        STAT.merge(k, v, Long::sum);
    }

    public static void main(String[] args) throws Exception {
        long[] seeds = args.length == 0 ? new long[] {1L, 424242L, -7310925581L} : new long[args.length];
        for (int i = 0; i < args.length; i++) {
            seeds[i] = Long.parseLong(args[i]);
        }
        boolean any = false;
        for (long seed : seeds) {
            VIOL.clear();
            EXAMPLE.clear();
            STAT.clear();
            any |= validate(seed);
        }
        System.exit(any ? 1 : 0);
    }

    private static boolean validate(long seed) throws Exception {
        System.out.println("=== seed " + seed + " ===");
        // ---- timing on a cold world (includes plan building), then warm
        ElysiumWorld tw = new ElysiumWorld(seed);
        Column tc = new Column();
        int chunks = 0;
        long t0 = System.nanoTime();
        for (int cx = -40; cx < 40; cx += 3) {
            for (int cz = -40; cz < 40; cz += 3) {
                for (int i = 0; i < 256; i++) {
                    tw.sample(cx * 16 + (i & 15), cz * 16 + (i >> 4), tc);
                }
                chunks++;
            }
        }
        long cold = System.nanoTime() - t0;
        t0 = System.nanoTime();
        for (int rep = 0; rep < 3; rep++) {
            for (int cx = -40; cx < 40; cx += 3) {
                for (int cz = -40; cz < 40; cz += 3) {
                    for (int i = 0; i < 256; i++) {
                        tw.sample(cx * 16 + (i & 15), cz * 16 + (i >> 4), tc);
                    }
                }
            }
        }
        long warm = (System.nanoTime() - t0) / 3;
        System.out.printf("timing: cold %.2f us/column (incl. plan build), warm %.2f us/column over %d chunks%n",
                cold / 1000.0 / (chunks * 256.0), warm / 1000.0 / (chunks * 256.0), chunks);

        ElysiumWorld world = new ElysiumWorld(seed);
        List<ElysiumWorld.Island> all = world.islandsIn(-9000, -9000, 9000, 9000);
        Map<ElysiumBiome, ElysiumWorld.Island> pick = new EnumMap<>(ElysiumBiome.class);
        List<ElysiumWorld.Island> platforms = new ArrayList<>();
        all.sort((a, b) -> Long.compare((long) a.cx() * a.cx() + (long) a.cz() * a.cz(), (long) b.cx() * b.cx() + (long) b.cz() * b.cz()));
        for (ElysiumWorld.Island is : all) {
            if (is.biome() == ElysiumBiome.CHAINFALL) {
                if (platforms.size() < 8) {
                    platforms.add(is);
                }
            } else {
                pick.putIfAbsent(is.biome(), is);
            }
        }
        if (pick.size() < 12) {
            List<ElysiumWorld.Island> more = world.islandsIn(-30000, -30000, 30000, 30000);
            more.sort((a, b) -> Long.compare((long) a.cx() * a.cx() + (long) a.cz() * a.cz(), (long) b.cx() * b.cx() + (long) b.cz() * b.cz()));
            for (ElysiumWorld.Island is : more) {
                if (is.biome() != ElysiumBiome.CHAINFALL) {
                    pick.putIfAbsent(is.biome(), is);
                }
            }
        }
        System.out.println("biomes covered: " + pick.size() + "/12, platforms: " + platforms.size());
        // the nearest Glass Sea may be an ordinary one: check the biggest colossal one as well
        world.islandsIn(-30000, -30000, 30000, 30000).stream().filter(is -> is.size() == ElysiumBiome.Size.COLOSSAL).max(java.util.Comparator.comparingInt(ElysiumWorld.Island::width))
                .ifPresent(platforms::add);
        List<int[]> regions = new ArrayList<>();
        List<Integer> targets = new ArrayList<>();
        for (ElysiumWorld.Island is : pick.values()) {
            regions.add(new int[] {is.minX() - 2, is.minZ() - 2, is.maxX() + 2, is.maxZ() + 2});
            targets.add(is.index());
        }
        for (ElysiumWorld.Island is : platforms) {
            regions.add(new int[] {is.minX() - 2, is.minZ() - 2, is.maxX() + 2, is.maxZ() + 2});
            targets.add(is.index());
        }
        regions.add(new int[] {-256, -256, 255, 255});
        targets.add(-1);
        long cols = 0;
        for (int i = 0; i < regions.size(); i++) {
            cols += checkRegion(world, regions.get(i), targets.get(i));
        }
        stat("columns checked", cols);

        // ---- springs
        for (int[] rg : regions) {
            for (ElysiumWorld.Spring s : world.springsIn(rg[0], rg[1], rg[2], rg[3])) {
                checkSpring(world, s);
            }
        }
        // ---- spans sanity
        int spanCount = 0;
        for (ElysiumWorld.Span s : world.spansIn(-9000, -9000, 9000, 9000)) {
            spanCount++;
            stat("spans " + s.kind(), 1);
            if (s.ay() < ElysiumWorld.LOWEST_SOLID || s.by() < ElysiumWorld.LOWEST_SOLID || s.ay() > ElysiumWorld.HIGHEST_SOLID) {
                viol("span anchor out of bounds", s.toString());
            }
            if (s.ay() - s.sag() < ElysiumWorld.LOWEST_SOLID && s.kind() == ElysiumWorld.Span.Kind.GIANT_CHAIN) {
                viol("chain sags below LOWEST_SOLID", s.toString());
            }
        }
        for (ElysiumWorld.HangingChain h : world.hangingChainsIn(-9000, -9000, 9000, 9000)) {
            stat(h.run() == 0 ? "hanging chains under" : "hanging chains side", 1);
            if (h.y() - h.drop() < ElysiumWorld.LOWEST_SOLID) {
                viol("hanging chain below LOWEST_SOLID", h.toString());
            }
        }
        stat("sites", world.sitesIn(-9000, -9000, 9000, 9000).size());
        // ---- biome adjacency (Rime next to Azure)
        for (ElysiumWorld.Island a : all) {
            for (ElysiumWorld.Island b : all) {
                if (a.biome() == ElysiumBiome.RIME_EXPANSE && b.biome() == ElysiumBiome.AZURE_SHALLOWS
                        && Math.hypot(a.cx() - b.cx(), a.cz() - b.cz()) < ElysiumWorld.SUPER * 1.6) {
                    viol("Rime Expanse next to Azure Shallows", a.index() + "/" + b.index());
                }
            }
        }
        // ---- determinism from 4 threads on a shared world + a fresh world
        checkDeterminism(seed, regions);

        STAT.forEach((k, v) -> System.out.println("  " + k + ": " + v));
        if (VIOL.isEmpty()) {
            System.out.println("RESULT: OK (0 violations)");
            return false;
        }
        long total = 0;
        for (Map.Entry<String, Long> e : VIOL.entrySet()) {
            System.out.println("  VIOLATION " + e.getKey() + ": " + e.getValue() + "   e.g. " + EXAMPLE.get(e.getKey()));
            total += e.getValue();
        }
        System.out.println("RESULT: FAIL (" + total + " violations)");
        return true;
    }

    // ------------------------------------------------------------------------------------------------ region

    private static long checkRegion(ElysiumWorld world, int[] rg, int target) {
        int x0 = rg[0], z0 = rg[1], w = rg[2] - rg[0] + 1, h = rg[3] - rg[1] + 1;
        int n = w * h;
        int[] top = new int[n], bot = new int[n], wt = new int[n], ab = new int[n], at = new int[n], isl = new int[n];
        boolean[] spill = new boolean[n], land = new boolean[n];
        Column c = new Column();
        long tSample = 0;
        for (int i = 0; i < n; i++) {
            int x = x0 + i % w, z = z0 + i / w;
            long t = System.nanoTime();
            world.sample(x, z, c);
            tSample += System.nanoTime() - t;
            land[i] = c.land;
            top[i] = c.top;
            bot[i] = c.bottom;
            wt[i] = c.hasWater() ? c.waterTop : Integer.MIN_VALUE;
            ab[i] = c.hasArch() ? c.archBottom : Integer.MIN_VALUE;
            at[i] = c.hasArch() ? c.archTop : Integer.MIN_VALUE;
            isl[i] = c.island;
            spill[i] = c.spill;
            if (c.land) {
                stat("land columns", 1);
                if (c.bottom > c.top) {
                    viol("bottom > top", x + "," + z);
                }
                if (c.bottom < ElysiumWorld.LOWEST_SOLID) {
                    viol("solid below LOWEST_SOLID", x + "," + z + " bottom " + c.bottom);
                }
                if (c.top > ElysiumWorld.HIGHEST_SOLID || (c.hasArch() && c.archTop > ElysiumWorld.HIGHEST_SOLID)) {
                    viol("solid above HIGHEST_SOLID", x + "," + z);
                }
                if (c.hasWater() && c.waterTop > ElysiumWorld.HIGHEST_SOLID) {
                    viol("water above HIGHEST_SOLID", x + "," + z);
                }
                if (c.hangLength > 0 && c.bottom - c.hangLength < ElysiumWorld.LOWEST_SOLID) {
                    viol("hang below LOWEST_SOLID", x + "," + z);
                }
                if (c.hasArch() && c.archBottom <= c.top + 1) {
                    viol("arch touching/inside column", x + "," + z);
                }
                if (c.hasArch() && c.hasWater() && c.archBottom <= c.waterTop) {
                    viol("arch inside water", x + "," + z);
                }
                // materials: every block, gravity over air
                for (int y = c.bottom; y <= c.top; y = (y < c.bottom + 2 || y >= c.top - 4) ? y + 1 : Math.max(y + 1, c.top - 4)) {
                    Mat m = world.material(c, y);
                    if (m == null || m == Mat.AIR || m == Mat.WATER) {
                        viol("material AIR/WATER/null inside solid", x + "," + y + "," + z);
                    } else if (ElysiumWorld.isGravity(m) && y == c.bottom) {
                        viol("gravity block over air", x + "," + y + "," + z + " " + m);
                    }
                }
                if (c.hasArch()) {
                    for (int y = c.archBottom; y <= c.archTop; y++) {
                        Mat m = world.material(c, y);
                        if (ElysiumWorld.isGravity(m) && y == c.archBottom) {
                            viol("gravity block over air (arch)", x + "," + y + "," + z + " " + m);
                        }
                    }
                }
                if (c.hasWater()) {
                    stat("water columns", 1);
                    if (c.spill) {
                        stat("spill columns", 1);
                    }
                }
            } else if (c.hasWater()) {
                viol("water over void", x + "," + z);
            }
        }
        stat("sample ns total", tSample);

        // ---- water holding
        int[][] d4 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int i = 0; i < n; i++) {
            if (wt[i] == Integer.MIN_VALUE) {
                continue;
            }
            int x = i % w, z = i / w;
            for (int[] d : d4) {
                int nx = x + d[0], nz = z + d[1];
                int ntop, nbot, nwt, nab, nat;
                if (nx < 0 || nz < 0 || nx >= w || nz >= h) {
                    world.sample(x0 + nx, z0 + nz, c);
                    ntop = c.land ? c.top : Integer.MIN_VALUE;
                    nbot = c.land ? c.bottom : Integer.MAX_VALUE;
                    nwt = c.hasWater() ? c.waterTop : Integer.MIN_VALUE;
                    nab = c.hasArch() ? c.archBottom : Integer.MIN_VALUE;
                    nat = c.hasArch() ? c.archTop : Integer.MIN_VALUE;
                } else {
                    int j = nz * w + nx;
                    ntop = land[j] ? top[j] : Integer.MIN_VALUE;
                    nbot = land[j] ? bot[j] : Integer.MAX_VALUE;
                    nwt = wt[j];
                    nab = ab[j];
                    nat = at[j];
                }
                if (nwt != Integer.MIN_VALUE) {
                    int step = Math.abs(nwt - wt[i]);
                    if (step > 1) {
                        viol("water-to-water step > 1", (x0 + x) + "," + (z0 + z) + " " + wt[i] + " vs " + nwt);
                    } else if (step == 1 && wt[i] > nwt) {
                        stat("water terrace steps (1 block)", 1);
                    }
                }
                for (int y = top[i] + 1; y <= wt[i]; y++) {
                    boolean solid = y >= nbot && y <= ntop || (nab != Integer.MIN_VALUE && y >= nab && y <= nat);
                    boolean water = nwt != Integer.MIN_VALUE && y > ntop && y <= nwt;
                    if (solid || water) {
                        continue;
                    }
                    if (nwt != Integer.MIN_VALUE && y == wt[i] && nwt == y - 1) {
                        continue; // terrace step, MC side turns it into a flowing slope
                    }
                    if (spill[i]) {
                        continue;
                    }
                    viol("unheld water", (x0 + x) + "," + y + "," + (z0 + z) + " neighbour " + (x0 + nx) + "," + (z0 + nz)
                            + " ntop=" + ntop + " nwt=" + nwt);
                }
            }
        }
        // ---- spill cluster sizes (narrow waterfalls)
        boolean[] seen = new boolean[n];
        int maxCluster = 0;
        for (int i = 0; i < n; i++) {
            if (!spill[i] || wt[i] == Integer.MIN_VALUE || seen[i]) {
                continue;
            }
            int size = 0;
            ArrayList<Integer> stack = new ArrayList<>();
            stack.add(i);
            seen[i] = true;
            while (!stack.isEmpty()) {
                int k = stack.remove(stack.size() - 1);
                size++;
                int x = k % w, z = k / w;
                for (int[] d : d4) {
                    int nx = x + d[0], nz = z + d[1];
                    if (nx < 0 || nz < 0 || nx >= w || nz >= h) {
                        continue;
                    }
                    int j = nz * w + nx;
                    if (!seen[j] && spill[j] && wt[j] != Integer.MIN_VALUE) {
                        seen[j] = true;
                        stack.add(j);
                    }
                }
            }
            stat("spill clusters", 1);
            maxCluster = Math.max(maxCluster, size);
            if (size > 10) {
                viol("wide spill (curtain) cluster > 10 columns", (x0 + i % w) + "," + (z0 + i / w) + " size " + size);
            }
        }
        STAT.merge("max spill cluster", (long) maxCluster, Math::max);

        // ---- connectivity of column intervals (main + arch nodes)
        int[] parent = new int[2 * n];
        for (int i = 0; i < 2 * n; i++) {
            parent[i] = i;
        }
        for (int i = 0; i < n; i++) {
            if (!land[i]) {
                continue;
            }
            int x = i % w, z = i / w;
            int[][] fw = {{1, 0}, {0, 1}};
            for (int[] d : fw) {
                int nx = x + d[0], nz = z + d[1];
                if (nx >= w || nz >= h) {
                    continue;
                }
                int j = nz * w + nx;
                if (!land[j]) {
                    continue;
                }
                if (overlap(bot[i], top[i], bot[j], top[j])) {
                    union(parent, i, j);
                }
                if (ab[j] != Integer.MIN_VALUE && overlap(bot[i], top[i], ab[j], at[j])) {
                    union(parent, i, n + j);
                }
                if (ab[i] != Integer.MIN_VALUE && overlap(ab[i], at[i], bot[j], top[j])) {
                    union(parent, n + i, j);
                }
                if (ab[i] != Integer.MIN_VALUE && ab[j] != Integer.MIN_VALUE && overlap(ab[i], at[i], ab[j], at[j])) {
                    union(parent, n + i, n + j);
                }
            }
        }
        Map<Integer, Map<Integer, Integer>> compsPerIsland = new HashMap<>();
        for (int i = 0; i < n; i++) {
            if (!land[i]) {
                continue;
            }
            compsPerIsland.computeIfAbsent(isl[i], k -> new HashMap<>()).merge(find(parent, i), 1, Integer::sum);
            if (ab[i] != Integer.MIN_VALUE) {
                compsPerIsland.get(isl[i]).merge(find(parent, n + i), 1, Integer::sum);
            }
        }
        for (Map.Entry<Integer, Map<Integer, Integer>> e : compsPerIsland.entrySet()) {
            if (target != -1 && e.getKey() != target) {
                continue;
            }
            Map<Integer, Integer> comps = e.getValue();
            if (comps.size() > 1) {
                int biggest = comps.values().stream().max(Integer::compare).orElse(0);
                for (Map.Entry<Integer, Integer> ce : comps.entrySet()) {
                    if (ce.getValue() != biggest) {
                        int root = ce.getKey();
                        int cx = root % n % w + x0, cz = root % n / w + z0;
                        viol("floating fragment (not 6-connected to island body)",
                                "island " + e.getKey() + " fragment of " + ce.getValue() + " cols near " + cx + "," + cz);
                    }
                }
            }
        }
        return n;
    }

    private static boolean overlap(int a0, int a1, int b0, int b1) {
        return a0 <= b1 && b0 <= a1;
    }

    private static int find(int[] p, int i) {
        while (p[i] != i) {
            p[i] = p[p[i]];
            i = p[i];
        }
        return i;
    }

    private static void union(int[] p, int a, int b) {
        int ra = find(p, a), rb = find(p, b);
        if (ra != rb) {
            p[ra] = rb;
        }
    }

    // ------------------------------------------------------------------------------------------------ springs

    private static void checkSpring(ElysiumWorld world, ElysiumWorld.Spring s) {
        stat("springs", 1);
        Column c = new Column(), nb = new Column();
        world.sample(s.x(), s.z(), c);
        if (!c.land || s.y() < c.bottom || s.y() > c.top) {
            viol("spring not inside solid column", s.toString());
            return;
        }
        boolean open = false;
        int[][] d4 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : d4) {
            world.sample(s.x() + d[0], s.z() + d[1], nb);
            boolean solid = nb.land && s.y() >= nb.bottom && s.y() <= nb.top
                    || nb.hasArch() && s.y() >= nb.archBottom && s.y() <= nb.archTop;
            boolean water = nb.hasWater() && s.y() > nb.top && s.y() <= nb.waterTop;
            if (!solid && !water) {
                open = true;
            }
        }
        if (!open) {
            viol("spring not exposed to air", s.toString());
        }
    }

    // ------------------------------------------------------------------------------------------------ determinism

    private static void checkDeterminism(long seed, List<int[]> regions) throws Exception {
        ElysiumWorld shared = new ElysiumWorld(seed);
        ElysiumWorld fresh = new ElysiumWorld(seed);
        java.util.SplittableRandom r = new java.util.SplittableRandom(seed);
        int m = 4000;
        int[] xs = new int[m], zs = new int[m];
        for (int i = 0; i < m; i++) {
            int[] rg = regions.get(r.nextInt(regions.size()));
            xs[i] = rg[0] + r.nextInt(rg[2] - rg[0] + 1);
            zs[i] = rg[1] + r.nextInt(rg[3] - rg[1] + 1);
        }
        ExecutorService ex = Executors.newFixedThreadPool(4);
        List<Future<String[]>> fs = new ArrayList<>();
        for (int t = 0; t < 4; t++) {
            final int off = t;
            fs.add(ex.submit(() -> {
                String[] out = new String[m];
                Column c = new Column();
                for (int k = 0; k < m; k++) {
                    int i = (k + off * 997) % m;
                    shared.sample(xs[i], zs[i], c);
                    out[i] = sig(shared, c);
                }
                return out;
            }));
        }
        String[] ref = new String[m];
        Column c = new Column();
        for (int i = 0; i < m; i++) {
            fresh.sample(xs[i], zs[i], c);
            ref[i] = sig(fresh, c);
        }
        for (Future<String[]> f : fs) {
            String[] got = f.get();
            for (int i = 0; i < m; i++) {
                if (!got[i].equals(ref[i])) {
                    viol("non-deterministic column", xs[i] + "," + zs[i]);
                }
            }
        }
        ex.shutdown();
        stat("determinism samples x4 threads", m);
        if (!shared.springsIn(-5000, -5000, 5000, 5000).equals(fresh.springsIn(-5000, -5000, 5000, 5000))
                || !shared.spansIn(-5000, -5000, 5000, 5000).equals(fresh.spansIn(-5000, -5000, 5000, 5000))
                || !shared.sitesIn(-5000, -5000, 5000, 5000).equals(fresh.sitesIn(-5000, -5000, 5000, 5000))) {
            viol("non-deterministic springs/spans/sites", "");
        }
    }

    private static String sig(ElysiumWorld w, Column c) {
        StringBuilder b = new StringBuilder();
        b.append(c.land).append(c.top).append(',').append(c.bottom).append(',').append(c.waterTop).append(',')
                .append(c.archBottom).append(',').append(c.archTop).append(c.spill).append(c.frozen).append(c.biome)
                .append(c.island).append(c.hangMat).append(c.hangLength).append(c.shore);
        if (c.land) {
            b.append(w.material(c, c.top)).append(w.material(c, c.bottom)).append(w.material(c, (c.top + c.bottom) / 2));
        }
        return b.toString();
    }
}
