package dev.zsskayr.merlins_inferno.worldgen.elysium.core;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Everything one island (or Chainfall platform) needs to answer {@link #fill} for any of its columns: outline,
 * relief, rivers, lakes, peaks, underside cone, springs and structure sites. Built once per island and cached by
 * {@link ElysiumWorld}; immutable afterwards except for the lazily computed spring/site lists (published through
 * volatile fields, recomputation is deterministic so a race only wastes work).
 *
 * <h2>Water model</h2>
 * A column holds water when its top is below the column's water level W. W is: the sea level everywhere on a sea
 * island (Glass Sea, Azure Shallows), the lake level inside a lake's bowl, the river terrace level inside a river's
 * wet tube, otherwise none. Every place where two different W meet is guaranteed a solid bank at least as high as the
 * higher level (lake rims, river banks, sea rims), except river terrace steps (levels differ by exactly 1) and the
 * coast lip of rivers/notches, which is flagged {@link Column#spill}.
 */
public final class IslandPlan {
    static final int NONE = Integer.MIN_VALUE;

    public final ElysiumWorld.Island island;
    public final ElysiumBiome biome;
    public final boolean platform;
    final Noise nz;
    final long salt;
    final double cx, cz, R, aspect, cosA, sinA, warp;
    final double ox, oz;
    public final int baseY;
    final double coneDepth, rim;
    public final int seaLevel;
    final List<River> rivers = new ArrayList<>();
    final List<Lake> lakes = new ArrayList<>();
    final List<Peak> peaks = new ArrayList<>();
    /** Chainfall natural arch (platforms only), null when none. */
    Arch arch;
    private volatile List<ElysiumWorld.Spring> springs;
    private volatile List<ElysiumWorld.Site> sites;

    record Lake(double x, double z, double r, int level) {}

    record Peak(double x, double z, double r, double h) {}

    record Arch(double x0, double z0, double x1, double z1, double height, double sag) {}

    IslandPlan(ElysiumWorld.Island island, double R, Noise nz, long seed) {
        this.island = island;
        this.biome = island.biome();
        this.platform = biome == ElysiumBiome.CHAINFALL;
        this.nz = nz;
        this.salt = Noise.hash(seed, island.index(), 0x15A1);
        SplittableRandom r = new SplittableRandom(salt);
        this.cx = island.cx() + 0.5;
        this.cz = island.cz() + 0.5;
        this.R = R;
        this.aspect = platform ? 0.75 + 0.25 * r.nextDouble() : 0.72 + 0.28 * r.nextDouble();
        double ang = r.nextDouble() * Math.PI;
        this.cosA = Math.cos(ang);
        this.sinA = Math.sin(ang);
        this.warp = 0.035 * R;
        this.ox = r.nextDouble() * 50000 - 25000;
        this.oz = r.nextDouble() * 50000 - 25000;
        this.baseY = island.baseY();
        // cone: ~30 + 7.5% of the width deep, clamped so stalactites still stay above LOWEST_SOLID
        double room = (baseY - ElysiumWorld.LOWEST_SOLID - 8) / 1.5; // leave room for the central tip and stalactites
        this.coneDepth = platform ? Math.min(room, 22 + 0.35 * R) : Math.min(room, 30 + 0.075 * island.width());
        this.rim = platform ? 7 + 4 * r.nextDouble() : 11 + 6 * r.nextDouble();
        this.seaLevel = switch (biome) {
            case GLASS_SEA -> baseY + 6;
            case AZURE_SHALLOWS -> baseY + 5;
            default -> NONE;
        };
        planPeaks(r);
        if (platform) {
            if (r.nextDouble() < 0.55 && R > 26) {
                double a = r.nextDouble() * Math.PI * 2, d = 0.55 * R * aspect;
                double h = 13 + 10 * r.nextDouble();
                arch = new Arch(cx + Math.cos(a) * d, cz + Math.sin(a) * d, cx - Math.cos(a) * d, cz - Math.sin(a) * d,
                        h, h * (0.25 + 0.15 * r.nextDouble()));
            }
        } else {
            planRivers(r);
            planLakes(r);
        }
    }

    // =================================================================== outline

    /** Signed distance-ish from the coast: > 0 on land (blocks inland), <= 0 over the void. */
    public double inland(double x, double z) {
        double px = x - cx, pz = z - cz;
        double s = 1.0 / Math.max(60.0, 0.35 * R);
        px += warp * nz.noise((x + ox) * s, (z + oz) * s);
        pz += warp * nz.noise((x - oz) * s + 91.7, (z + ox) * s - 13.3);
        double u = px * cosA + pz * sinA;
        double v = (-px * sinA + pz * cosA) / aspect;
        double d = Math.sqrt(u * u + v * v);
        if (d < 1e-6) {
            return R;
        }
        double cu = u / d, cv = v / d;
        double edge = R * (1.0 + 0.11 * nz.noise(cu * 1.6 + ox * 1e-3, cv * 1.6 + oz * 1e-3)
                + 0.045 * nz.noise(cu * 5.3 + 40.0, cv * 5.3 - 21.0)
                + (platform ? 0.08 * nz.noise(cu * 3.1 - 7, cv * 3.1 + 3) : 0.012 * nz.noise(cu * 23.0 + 9, cv * 23.0 + 1)));
        double f = Math.sqrt(cu * cu + cv * cv * aspect * aspect);
        return (edge - d) * f;
    }

    /** Max horizontal reach from the centre (for bounding boxes and spacing). */
    static double extent(double R) {
        return 1.21 * R + 2;
    }

    /** Typical inland depth of the centre (minor semi-axis). */
    double coreInland() {
        return R * aspect;
    }

    // =================================================================== relief

    private double n(double x, double z, double scale, int oct) {
        return nz.fbm((x + ox) / scale, (z + oz) / scale, oct);
    }

    /** Spire field: sparse sharp rock needles. Returns height added at (x,z). */
    private double spires(double x, double z, int cell, double prob, double rMin, double rMax, double hMin, double hMax) {
        int gx = Math.floorDiv((int) Math.floor(x), cell), gz = Math.floorDiv((int) Math.floor(z), cell);
        long h = Noise.hash(salt, gx, gz, 0x5B1);
        if (Noise.unit(h) >= prob) {
            return 0;
        }
        double rr = rMin + (rMax - rMin) * Noise.unit(Noise.mix(h + 1));
        double margin = 2.2 * rr;
        double sx = gx * cell + margin + (cell - 2 * margin) * Noise.unit(Noise.mix(h + 2));
        double sz = gz * cell + margin + (cell - 2 * margin) * Noise.unit(Noise.mix(h + 3));
        double dx = x - sx, dz = z - sz;
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d >= 2 * rr) {
            return 0;
        }
        double hh = hMin + (hMax - hMin) * Noise.unit(Noise.mix(h + 4));
        double v = 0.22 * hh * (1 - d / (2 * rr));
        if (d < rr) {
            double k = 1 - d / rr;
            v += hh * Math.sqrt(k) * (0.85 + 0.15 * nz.noise(x * 0.31, z * 0.31));
        }
        return v;
    }

    private static double terrace(double h, double step, double sharp) {
        double f = Math.floor(h / step);
        double t = h / step - f;
        double k = Noise.smoothstep(1 - sharp, 1, t);
        return (f + k) * step;
    }

    /** Base terrain height (absolute y, before rivers/lakes) at (x,z). */
    public double relief(double x, double z, double inland) {
        double h;
        switch (biome) {
            case LARK_MEADOWS -> {
                h = 7 + 3.6 * n(x, z, 170, 3) + 1.4 * n(x, z, 45, 2);
                h = terrace(h, 3, 0.35) + spires(x, z, 220, 0.12, 3, 5, 9, 20);
            }
            case WINDLESS_STEPPE -> h = 3 + 1.3 * n(x, z, 300, 2) + 0.5 * n(x, z, 40, 2) + spires(x, z, 150, 0.18, 1.5, 3, 3, 7);
            case HIGHPINE_FOREST -> {
                h = 17 + 8.5 * n(x, z, 210, 3) + 3 * n(x, z, 55, 2);
                h = terrace(h, 4, 0.3) + spires(x, z, 130, 0.25, 3, 6, 14, 34);
            }
            case MOSSY_WEALD -> h = 6 + 2.6 * n(x, z, 140, 3) + 1.1 * n(x, z, 35, 2) + spires(x, z, 170, 0.15, 3, 6, 6, 14);
            case ARGENT_PEAKS, FROSTCREST_RANGE -> {
                boolean argent = biome == ElysiumBiome.ARGENT_PEAKS;
                double base = argent ? 16 + 7 * n(x, z, 110, 3) : 12 + 6 * n(x, z, 120, 3);
                double best = 0, sum = 0;
                for (Peak p : peaks) {
                    double dx = x - p.x, dz = z - p.z;
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d < p.r) {
                        double k = 1 - d / p.r;
                        double v = p.h * Math.pow(k, argent ? 1.35 : 1.6);
                        sum += v;
                        best = Math.max(best, v);
                    }
                }
                double m = best + 0.3 * (sum - best);
                double rough = nz.ridged((x + ox) / 38.0, (z + oz) / 38.0, 3);
                h = base + m * (0.82 + 0.25 * rough) + spires(x, z, 120, argent ? 0.35 : 0.22, 3, 6, 15, 40);
            }
            case STONECROWN_CLIFFS -> {
                double v = n(x, z, 230, 3);
                int rowZ = Math.floorDiv((int) Math.floor(z), 5);
                int colX = Math.floorDiv((int) Math.floor(x) + ((rowZ & 1) != 0 ? 2 : 0), 5);
                double colHash = Noise.unit(Noise.hash(salt, colX, rowZ, 0xC01));
                double lv = Math.floor(Noise.clamp((v + 0.85) * 2.7 + 0.45 * colHash, 0, 5));
                h = 12 + 13 * lv + 2 * colHash + spires(x, z, 95, 0.32, 3, 6, 18, 42);
            }
            case CASCADE_VALE -> h = 50 + 6 * n(x, z, 200, 3) + 2 * n(x, z, 40, 2) + spires(x, z, 110, 0.25, 3, 6, 12, 30);
            case MISTFALL_GORGE -> h = 44 + 6 * n(x, z, 180, 3) + 1.5 * n(x, z, 40, 2) + spires(x, z, 120, 0.25, 3, 5, 10, 26);
            case GLASS_SEA -> {
                double ringW = 16 + 9 * n(x, z, 120, 2);
                double rimH = 13 + 4 * n(x, z, 90, 2);
                double floor = -12 - 22 * Noise.smoothstep(ringW, ringW + 140, inland) + 5 * n(x, z, 70, 2);
                h = Noise.lerp(rimH, floor, Noise.smoothstep(ringW, ringW + 45, inland));
                double iv = n(x + 333, z - 777, 95, 3);
                if (iv > 0.38) {
                    h = Math.max(h, 6 + (iv - 0.38) * 42);
                }
                h += spires(x, z, 120, 0.3, 4, 8, 10, 30);
            }
            case AZURE_SHALLOWS -> {
                double ringW = 14 + 6 * n(x, z, 90, 2);
                double rimH = 9 + 2.5 * n(x, z, 60, 2);
                double floor = 1.2 + 1.3 * n(x, z, 60, 2);
                double reef = n(x + 99, z + 55, 22, 2);
                if (reef > 0.35) {
                    floor = Math.max(floor, 2.5 + (reef - 0.35) * 5); // reef crests 1..2 below the surface
                    floor = Math.min(floor, 4);
                }
                double bar = n(x - 410, z + 230, 110, 3);
                if (bar > 0.45) {
                    floor = Math.max(floor, 5 + (bar - 0.45) * 14);
                }
                h = Noise.lerp(rimH, floor, Noise.smoothstep(ringW, ringW + 12, inland));
                h += spires(x, z, 150, 0.15, 3, 6, 8, 22);
            }
            case RIME_EXPANSE -> h = 3 + 1.6 * n(x, z, 260, 3) + 0.6 * n(x, z, 40, 2);
            case CHAINFALL -> h = 3 + 2.2 * n(x, z, 28, 2);
            default -> h = 6;
        }
        return baseY + h;
    }

    // =================================================================== column

    private static final ThreadLocal<River.Hit> HIT = ThreadLocal.withInitial(River.Hit::new);

    /** Fill the column at (x, z), which must be inside this island's outline (inland > 0 given). */
    void fill(int x, int z, double inland, Column c) {
        double px = x + 0.5, pz = z + 0.5;
        c.land = true;
        c.island = island.index();
        c.biome = biome;
        c.inland = inland;
        c.baseY = baseY;
        c.spill = false;
        c.frozen = biome == ElysiumBiome.RIME_EXPANSE || biome == ElysiumBiome.FROSTCREST_RANGE;
        c.shore = false;
        c.archBottom = 0;
        c.archTop = -1;
        c.patch = nz.noise((px + ox) / 48.0, (pz + oz) / 48.0);
        c.surfaceDepth = 4;
        c.slope = 0;
        c.riverDist = Double.POSITIVE_INFINITY;

        double h = relief(px, pz, inland);
        double top = h;
        int w = seaLevel;

        // ---- rivers / notches
        if (!rivers.isEmpty()) {
            River.Hit hit = HIT.get();
            hit.e = Double.POSITIVE_INFINITY;
            hit.river = null;
            for (River rv : rivers) {
                rv.query(px, pz, hit);
            }
            if (hit.river != null) {
                double e = hit.e;
                int L = hit.level;
                c.riverDist = Math.max(0, e);
                if (e < 0) {
                    double depth = hit.river.kind == River.Kind.NOTCH ? 2 : 1 + Math.min(2.0, -e * 0.6);
                    top = L - Math.round(depth);
                    w = L;
                } else {
                    double bank;
                    switch (hit.river.kind) {
                        case CANYON -> {
                            double ee = e + 2.5 * nz.noise((px + ox) / 17.0, (pz + oz) / 17.0);
                            bank = ee < 3 ? L + 1 : L + 1 + 8 * Math.ceil((ee - 3) / 6.0);
                            if (ee >= 3 && bank < h) {
                                c.slope = 3;
                            }
                        }
                        case GORGE -> {
                            double ee = e + 3.0 * nz.noise((px + ox) / 21.0, (pz + oz) / 21.0) + 1.5;
                            bank = ee < 2 ? L + 1 : L + 1 + (ee - 2) * 6.5;
                            if (ee >= 2 && bank < h) {
                                c.slope = 4;
                            }
                        }
                        case NOTCH -> bank = L + 1 + e * 1.6;
                        default -> bank = e < 1.5 ? L + 1 : L + 1 + (e - 1.5) * 0.85;
                    }
                    top = Math.min(top, bank);
                    // embankment: never lower than the water next to it; dike slopes away where ground dips
                    double embank = L + 1 - Math.max(0, e - 2) * 0.8;
                    top = Math.max(top, embank);
                    if (e < 3 && top <= L + 2) {
                        c.shore = true;
                    }
                }
            }
        }

        // ---- lakes
        for (int i = 0, n = lakes.size(); i < n; i++) {
            Lake lk = lakes.get(i);
            double dx = px - lk.x, dz = pz - lk.z;
            double rr = lk.r * 1.2 + 8;
            if (dx > rr || dx < -rr || dz > rr || dz < -rr) {
                continue;
            }
            double rn = lk.r * (1 + 0.18 * nz.noise((px + ox) / 13.0, (pz + oz) / 13.0));
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < rn) {
                double k = d / rn;
                double depth = 1 + (lk.r > 14 ? 5 : 3) * (1 - k * k);
                top = Math.min(top, lk.level - Math.round(depth));
                w = lk.level;
            } else if (d < rn + 7) {
                double bank = lk.level + 1 + (d - rn) * 1.1;
                top = Math.min(top, bank);
                top = Math.max(top, lk.level + 1);
                if (top <= lk.level + 2) {
                    c.shore = true;
                }
            }
        }

        // ---- platform arch
        if (arch != null) {
            double pillar = archPillar(px, pz);
            if (pillar > top) {
                top = pillar;
            }
            applyArch(px, pz, c, top);
            if (c.hasArch() && c.archBottom <= Math.floor(top) + 1) {
                top = Math.max(top, c.archTop);
                c.archBottom = 0;
                c.archTop = -1;
            }
        }

        int itop = (int) Math.floor(top);
        if (itop > ElysiumWorld.HIGHEST_SOLID) {
            itop = ElysiumWorld.HIGHEST_SOLID;
        }
        c.top = itop;
        if (w != NONE && itop < w) {
            c.waterTop = w;
            if (seaLevel != NONE && itop >= w - 2 && c.riverDist > 0) {
                c.shore = true;
            }
            c.spill = inland < 5 && touchesVoid(px, pz);
        } else {
            c.waterTop = Integer.MIN_VALUE;
            if (seaLevel != NONE && itop <= seaLevel + 2) {
                c.shore = true;
            }
        }

        // ---- slope (mountain biomes only; others leave the cheap 0)
        if (c.slope == 0 && (biome == ElysiumBiome.ARGENT_PEAKS || biome == ElysiumBiome.FROSTCREST_RANGE
                || biome == ElysiumBiome.STONECROWN_CLIFFS)) {
            double gx = relief(px + 1, pz, inland) - relief(px - 1, pz, inland);
            double gz = relief(px, pz + 1, inland) - relief(px, pz - 1, inland);
            c.slope = 0.5 * Math.sqrt(gx * gx + gz * gz);
        }
        if (c.slope > 3) {
            c.surfaceDepth = 1;
        } else if (c.slope > 1.6) {
            c.surfaceDepth = 2;
        }

        // ---- underside: inverted cone + ribs + stalactites
        double t = Noise.clamp(inland / (0.85 * coreInland()), 0, 1);
        double thick = rim + (coneDepth - rim) * Math.pow(t, 0.72);
        thick *= 1 + 0.10 * nz.noise((px + ox) / 110.0, (pz + oz) / 110.0);
        thick += 5 * t * nz.ridged((px + oz) / 26.0, (pz - ox) / 26.0, 2);
        double dcx = px - cx, dcz = pz - cz;
        double dc = Math.sqrt(dcx * dcx + dcz * dcz);
        double tipR = 0.28 * coreInland();
        if (dc < tipR) {
            double k = 1 - dc / tipR;
            thick += 0.45 * coneDepth * k * k;
        }
        thick += stalactite(x, z, t);
        int bottom = (int) Math.floor(baseY - thick);
        bottom = Math.max(bottom, ElysiumWorld.LOWEST_SOLID);
        bottom = Math.min(bottom, itop - 3);
        c.bottom = bottom;

        long hh = Noise.hash(salt, x, z, 0x4A6);
        Mat hang = ElysiumPalette.hang(biome, c, Noise.unit(hh), c.patch);
        int len = 1 + (int) ((hh >>> 3) % 6);
        if (hang == null || hang == Mat.AIR || bottom - len < ElysiumWorld.LOWEST_SOLID) {
            c.hangMat = Mat.AIR;
            c.hangLength = 0;
        } else {
            c.hangMat = hang;
            c.hangLength = len;
        }
    }

    private double stalactite(int x, int z, double t) {
        final int cell = 12;
        int gx = Math.floorDiv(x, cell), gz = Math.floorDiv(z, cell);
        long h = Noise.hash(salt, gx, gz, 0x57A);
        if (Noise.unit(h) > 0.25 + 0.4 * t) {
            return 0;
        }
        double r = 2 + 3.5 * Noise.unit(Noise.mix(h + 1));
        double sx = gx * cell + r + (cell - 2 * r) * Noise.unit(Noise.mix(h + 2));
        double sz = gz * cell + r + (cell - 2 * r) * Noise.unit(Noise.mix(h + 3));
        double dx = x + 0.5 - sx, dz = z + 0.5 - sz;
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d >= r) {
            return 0;
        }
        double u = Noise.unit(Noise.mix(h + 4));
        double len = (5 + 34 * u * u) * (0.35 + t) * Math.max(0.5, coneDepth / 90.0);
        return len * Math.pow(1 - d / r, 1.6);
    }

    /** True when one of the 4 horizontal neighbours is outside the outline. */
    boolean touchesVoid(double px, double pz) {
        return inland(px + 1, pz) <= 0 || inland(px - 1, pz) <= 0 || inland(px, pz + 1) <= 0 || inland(px, pz - 1) <= 0;
    }

    private double archPillar(double px, double pz) {
        double best = Double.NEGATIVE_INFINITY;
        double[][] ends = {{arch.x0, arch.z0}, {arch.x1, arch.z1}};
        for (double[] e : ends) {
            double dx = px - e[0], dz = pz - e[1];
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < 4.5) {
                best = Math.max(best, baseY + 3 + arch.height + 1.5 - d * 0.6);
            }
        }
        return best;
    }

    private void applyArch(double px, double pz, Column c, double top) {
        double dx = arch.x1 - arch.x0, dz = arch.z1 - arch.z0;
        double len2 = dx * dx + dz * dz;
        double t = ((px - arch.x0) * dx + (pz - arch.z0) * dz) / len2;
        if (t <= 0 || t >= 1) {
            return;
        }
        double qx = arch.x0 + dx * t - px, qz = arch.z0 + dz * t - pz;
        if (qx * qx + qz * qz > 2.3 * 2.3) {
            return;
        }
        double bow = 4 * t * (1 - t);
        int at = (int) Math.floor(baseY + 3 + arch.height - arch.sag * bow);
        int ab = at - 2 - (int) Math.round((1 - bow) * 3);
        if (at <= Math.floor(top) + 1) {
            return;
        }
        c.archTop = at;
        c.archBottom = Math.max(ab, (int) Math.floor(top) + 2);
    }

    // =================================================================== planning

    private void planPeaks(SplittableRandom r) {
        if (biome == ElysiumBiome.ARGENT_PEAKS) {
            int n = 3 + (int) (R / 150) + r.nextInt(2);
            double maxH = ElysiumWorld.HIGHEST_SOLID - 12 - baseY - 30;
            for (int i = 0; i < n; i++) {
                double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * 0.62 * coreInland();
                double h = maxH * (i == 0 ? 1.0 : 0.45 + 0.45 * r.nextDouble());
                double rad = (0.32 + 0.25 * r.nextDouble()) * R * (i == 0 ? 1.1 : 1.0);
                peaks.add(new Peak(cx + Math.cos(a) * d, cz + Math.sin(a) * d, rad, h));
            }
        } else if (biome == ElysiumBiome.FROSTCREST_RANGE) {
            // a range: peaks strung along a ridge axis through the island
            double a = r.nextDouble() * Math.PI;
            int n = 5 + r.nextInt(4);
            for (int i = 0; i < n; i++) {
                double f = (i / (double) (n - 1)) * 1.2 - 0.6;
                double side = (r.nextDouble() - 0.5) * 0.3 * R;
                double x = cx + Math.cos(a) * f * R * aspect - Math.sin(a) * side;
                double z = cz + Math.sin(a) * f * R * aspect + Math.cos(a) * side;
                double h = 55 + 40 * r.nextDouble();
                double rad = (0.22 + 0.14 * r.nextDouble()) * R;
                peaks.add(new Peak(x, z, rad, h));
            }
        }
    }

    private void planRivers(SplittableRandom r) {
        double chance;
        int count = 1;
        River.Kind kind = River.Kind.VALLEY;
        switch (biome) {
            case CASCADE_VALE -> {
                chance = 1;
                kind = River.Kind.CANYON;
                count = island.width() > 900 ? 2 : 1;
            }
            case MISTFALL_GORGE -> {
                chance = 1;
                kind = River.Kind.GORGE;
            }
            case LARK_MEADOWS -> {
                chance = 0.55;
                count = island.width() > 1300 ? 2 : 1;
            }
            case HIGHPINE_FOREST, MOSSY_WEALD -> chance = 0.45;
            case STONECROWN_CLIFFS -> chance = 0.35;
            case AZURE_SHALLOWS -> {
                for (int i = 0, n = 3 + r.nextInt(3); i < n; i++) {
                    addNotch(r, (i + 0.3 * r.nextDouble()) / n * Math.PI * 2);
                }
                return;
            }
            case GLASS_SEA -> {
                for (int i = 0, n = 1 + r.nextInt(2); i < n; i++) {
                    addNotch(r, (i + 0.3 * r.nextDouble()) / n * Math.PI * 2);
                }
                return;
            }
            default -> chance = 0;
        }
        if (r.nextDouble() >= chance) {
            return;
        }
        double phi = r.nextDouble() * Math.PI * 2;
        for (int i = 0; i < count; i++) {
            double dir = phi + i * (Math.PI * (0.75 + 0.5 * r.nextDouble()));
            River rv = buildRiver(r, kind, dir);
            if (rv == null) {
                continue;
            }
            boolean ok = true;
            for (River o : rivers) {
                if (River.minDistance(o, rv) < o.influence + rv.influence + o.w[0] + rv.w[0] + 12) {
                    ok = false;
                }
            }
            if (ok) {
                rivers.add(rv);
            }
        }
    }

    /** Coast-facing unit direction from the centre in world space, given an angle. */
    private River buildRiver(SplittableRandom r, River.Kind kind, double dir) {
        double headBack = switch (kind) {
            case CANYON -> 0.35;
            case GORGE -> 0.45;
            default -> 0.05 + 0.3 * r.nextDouble();
        };
        double ci = coreInland();
        double hx = cx - Math.cos(dir) * headBack * ci, hz = cz - Math.sin(dir) * headBack * ci;
        if (inland(hx, hz) < 60) {
            return null;
        }
        List<double[]> pts = new ArrayList<>();
        double a = dir, x = hx, z = hz;
        double turnSeed = r.nextDouble() * 1000;
        double sCoast = -1, s = 0;
        pts.add(new double[] {x, z});
        for (int i = 0; i < 3000; i++) {
            double turn = 0.11 * nz.noise(turnSeed + i * 0.045, 7.7) + 0.035 * angleDiff(dir, a);
            a += Noise.clamp(turn, -0.09, 0.09);
            x += Math.cos(a) * River.STEP;
            z += Math.sin(a) * River.STEP;
            s += River.STEP;
            pts.add(new double[] {x, z});
            double in = inland(x, z);
            if (sCoast < 0 && in <= 0) {
                sCoast = s;
            }
            if (sCoast >= 0 && s > sCoast + 12) {
                break;
            }
        }
        if (sCoast < 0 || sCoast < 80) {
            return null;
        }
        int n = pts.size();
        double[] xs = new double[n], zs = new double[n], ss = new double[n], ws = new double[n];
        int[] lv = new int[n];
        double baseW = switch (kind) {
            case CANYON -> 3 + 1.5 * r.nextDouble();
            case GORGE -> 1.8 + 0.6 * r.nextDouble();
            default -> 2.2 + 1.6 * r.nextDouble();
        };
        double pool = kind == River.Kind.GORGE ? 4.5 : 6.5 + 2 * r.nextDouble();
        for (int i = 0; i < n; i++) {
            xs[i] = pts.get(i)[0];
            zs[i] = pts.get(i)[1];
            ss[i] = i * River.STEP;
            double wv = baseW * (1 + 0.25 * nz.noise(turnSeed + i * 0.03, 33.1));
            wv = Math.max(wv, pool - Math.max(0, ss[i] - 6) * 0.35);
            double taper = Noise.smoothstep(sCoast - 26, sCoast - 4, ss[i]);
            ws[i] = Noise.lerp(wv, 1.25, taper);
        }
        double influence = switch (kind) {
            case CANYON -> 34;
            case GORGE -> 22;
            default -> 36;
        };
        // ---- levels
        int minLevel = (int) Math.ceil(baseY - rim + 6);
        int poolEnd = (int) Math.ceil((pool + 8) / River.STEP);
        if (kind == River.Kind.VALLEY) {
            double minRing = Double.POSITIVE_INFINITY;
            for (int k = 0; k < 12; k++) {
                double aa = k * Math.PI / 6;
                double rx = xs[0] + Math.cos(aa) * (pool + 3), rz = zs[0] + Math.sin(aa) * (pool + 3);
                minRing = Math.min(minRing, relief(rx, rz, inland(rx, rz)));
            }
            int L = (int) Math.floor(minRing) - 1;
            int lastDrop = -100;
            for (int i = 0; i < n; i++) {
                if (i > poolEnd) {
                    double hx2 = relief(xs[i], zs[i], inland(xs[i], zs[i]));
                    int target = (int) Math.floor(hx2) - 2;
                    if (target < L && i - lastDrop >= 2 && L > minLevel) {
                        L--;
                        lastDrop = i;
                    }
                }
                lv[i] = L;
            }
        } else {
            double relStart = kind == River.Kind.CANYON ? 22 + 6 * r.nextDouble() : 6 + 3 * r.nextDouble();
            double relEnd = kind == River.Kind.CANYON ? 0 : -3;
            int steps = 5 + r.nextInt(4);
            int L = (int) Math.round(baseY + relStart);
            int lastDrop = -100;
            for (int i = 0; i < n; i++) {
                double u = Noise.clamp(ss[i] / sCoast, 0, 1);
                double f = u * steps;
                double stair = (Math.floor(f) + Noise.smoothstep(0.55, 1.0, f - Math.floor(f))) / steps;
                int target = (int) Math.round(baseY + Noise.lerp(relStart, relEnd, stair));
                if (i > poolEnd && target < L && i - lastDrop >= 1 && L > minLevel) {
                    L--;
                    lastDrop = i;
                }
                lv[i] = L;
            }
        }
        River rv = new River(kind, xs, zs, ss, ws, lv, sCoast, influence);
        rv.finish();
        return rv;
    }

    private void addNotch(SplittableRandom r, double ang) {
        // walk outward from well inside the rim until we leave the outline
        double dx = Math.cos(ang), dz = Math.sin(ang);
        double far = extent(R);
        double start = -1;
        for (double d = 0; d < far; d += 2) {
            double in = inland(cx + dx * d, cz + dz * d);
            if (in < 40 && start < 0) {
                start = d;
            }
            if (in <= 0) {
                List<double[]> pts = new ArrayList<>();
                for (double q = start; q <= d + 12; q += River.STEP) {
                    pts.add(new double[] {cx + dx * q, cz + dz * q});
                }
                int n = pts.size();
                if (n < 3) {
                    return;
                }
                double[] xs = new double[n], zs = new double[n], ss = new double[n], ws = new double[n];
                int[] lv = new int[n];
                for (int i = 0; i < n; i++) {
                    xs[i] = pts.get(i)[0];
                    zs[i] = pts.get(i)[1];
                    ss[i] = i * River.STEP;
                    ws[i] = 1.25 + 0.3 * r.nextDouble() * 0;
                    lv[i] = seaLevel;
                }
                River rv = new River(River.Kind.NOTCH, xs, zs, ss, ws, lv, d - start, 8);
                rv.finish();
                rivers.add(rv);
                return;
            }
        }
    }

    private static double angleDiff(double target, double a) {
        double d = target - a;
        while (d > Math.PI) {
            d -= 2 * Math.PI;
        }
        while (d < -Math.PI) {
            d += 2 * Math.PI;
        }
        return d;
    }

    private void planLakes(SplittableRandom r) {
        int n;
        double rMin, rMax;
        switch (biome) {
            case MOSSY_WEALD -> { n = 6 + r.nextInt(9); rMin = 5; rMax = 13; }
            case RIME_EXPANSE -> { n = 4 + r.nextInt(7); rMin = 14; rMax = 42; }
            case FROSTCREST_RANGE -> { n = 2 + r.nextInt(3); rMin = 12; rMax = 26; }
            case LARK_MEADOWS -> { n = r.nextInt(3); rMin = 8; rMax = 18; }
            case HIGHPINE_FOREST -> { n = r.nextInt(2); rMin = 6; rMax = 12; }
            case WINDLESS_STEPPE -> { n = r.nextInt(2); rMin = 6; rMax = 12; }
            default -> { return; }
        }
        for (int attempt = 0; attempt < n * 8 && lakes.size() < n; attempt++) {
            double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * 0.9 * R;
            double x = cx + Math.cos(a) * d, z = cz + Math.sin(a) * d;
            double rr = rMin + (rMax - rMin) * r.nextDouble();
            double in = inland(x, z);
            if (in < rr * 1.2 + 40) {
                continue;
            }
            boolean ok = true;
            for (Lake o : lakes) {
                double ddx = o.x - x, ddz = o.z - z;
                if (Math.sqrt(ddx * ddx + ddz * ddz) < (o.r + rr) * 1.2 + 20) {
                    ok = false;
                    break;
                }
            }
            for (River rv : rivers) {
                if (!ok || rv.distanceTo(x, z) < rr * 1.2 + rv.influence + 14) {
                    ok = false;
                }
            }
            for (Peak p : peaks) {
                double ddx = p.x - x, ddz = p.z - z;
                if (biome == ElysiumBiome.FROSTCREST_RANGE && Math.sqrt(ddx * ddx + ddz * ddz) < p.r * 0.75) {
                    ok = false;
                }
            }
            if (!ok) {
                continue;
            }
            double minRing = Double.POSITIVE_INFINITY, centre = relief(x, z, in);
            for (int k = 0; k < 16; k++) {
                double aa = k * Math.PI / 8;
                double rx = x + Math.cos(aa) * (rr * 1.25 + 3), rz = z + Math.sin(aa) * (rr * 1.25 + 3);
                minRing = Math.min(minRing, relief(rx, rz, inland(rx, rz)));
            }
            int level = (int) Math.floor(minRing) - 1;
            if (centre - level > 14) {
                continue; // on a steep slope: would be a pit, skip
            }
            lakes.add(new Lake(x, z, rr, level));
        }
    }

    // =================================================================== springs & sites

    List<ElysiumWorld.Spring> springs() {
        List<ElysiumWorld.Spring> s = springs;
        if (s == null) {
            s = computeSprings();
            springs = s;
        }
        return s;
    }

    private List<ElysiumWorld.Spring> computeSprings() {
        SplittableRandom r = new SplittableRandom(salt ^ 0x5915);
        int coastal, interior = 0;
        switch (biome) {
            case STONECROWN_CLIFFS -> { coastal = 10; interior = 22; }
            case AZURE_SHALLOWS -> coastal = 7;
            case CASCADE_VALE, MISTFALL_GORGE -> { coastal = 4; interior = 8; }
            case CHAINFALL -> coastal = r.nextInt(2);
            case RIME_EXPANSE, FROSTCREST_RANGE -> coastal = 1;
            default -> coastal = 2 + r.nextInt(2);
        }
        List<ElysiumWorld.Spring> out = new ArrayList<>();
        Column c = new Column();
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int k = 0; k < coastal; k++) {
            double a = (k + r.nextDouble() * 0.8) / Math.max(1, coastal) * Math.PI * 2;
            double dx = Math.cos(a), dz = Math.sin(a);
            double last = -1;
            for (double d = 0; d < extent(R) + 4; d += 1) {
                if (inland(cx + dx * d, cz + dz * d) > 0) {
                    last = d;
                }
            }
            if (last < 0) {
                continue;
            }
            int x = (int) Math.floor(cx + dx * last), z = (int) Math.floor(cz + dz * last);
            double in = inland(x + 0.5, z + 0.5);
            if (in <= 0) {
                continue;
            }
            fill(x, z, in, c);
            if (c.hasWater() || c.top - c.bottom < 8) {
                continue;
            }
            boolean open = false;
            for (int[] dd : dirs) {
                if (inland(x + dd[0] + 0.5, z + dd[1] + 0.5) <= 0) {
                    open = true;
                }
            }
            if (!open) {
                continue;
            }
            int y = c.top - 2 - r.nextInt(Math.max(1, Math.min(6, c.top - c.bottom - 6)));
            out.add(new ElysiumWorld.Spring(x, y, z));
        }
        Column nb = new Column();
        for (int k = 0, tries = 0; k < interior && tries < interior * 12; tries++) {
            double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * R;
            int x = (int) Math.floor(cx + Math.cos(a) * d), z = (int) Math.floor(cz + Math.sin(a) * d);
            double in = inland(x + 0.5, z + 0.5);
            if (in < 6) {
                continue;
            }
            fill(x, z, in, c);
            if (c.hasWater()) {
                continue;
            }
            for (int[] dd : dirs) {
                double in2 = inland(x + dd[0] + 0.5, z + dd[1] + 0.5);
                if (in2 <= 0) {
                    continue;
                }
                fill(x + dd[0], z + dd[1], in2, nb);
                int y = c.top - 1;
                if (nb.top <= y - 5 && !nb.hasArch() && (!nb.hasWater() || nb.waterTop < y) && y > c.bottom + 2) {
                    out.add(new ElysiumWorld.Spring(x, y - r.nextInt(Math.min(4, c.top - nb.top - 3)), z));
                    k++;
                    break;
                }
            }
        }
        return List.copyOf(out);
    }

    List<ElysiumWorld.Site> sites() {
        List<ElysiumWorld.Site> s = sites;
        if (s == null) {
            s = computeSites();
            sites = s;
        }
        return s;
    }

    private static boolean temperate(ElysiumBiome b) {
        return switch (b) {
            case LARK_MEADOWS, WINDLESS_STEPPE, HIGHPINE_FOREST, MOSSY_WEALD, STONECROWN_CLIFFS, CASCADE_VALE,
                 MISTFALL_GORGE, GLASS_SEA, AZURE_SHALLOWS -> true;
            default -> false;
        };
    }

    private List<ElysiumWorld.Site> computeSites() {
        List<ElysiumWorld.Site> out = new ArrayList<>();
        if (platform) {
            return out;
        }
        SplittableRandom r = new SplittableRandom(salt ^ 0x517E);
        Column c = new Column(), q = new Column();
        final int cell = 300;
        int gx0 = Math.floorDiv(island.minX(), cell), gx1 = Math.floorDiv(island.maxX(), cell);
        int gz0 = Math.floorDiv(island.minZ(), cell), gz1 = Math.floorDiv(island.maxZ(), cell);
        for (int gx = gx0; gx <= gx1; gx++) {
            for (int gz = gz0; gz <= gz1; gz++) {
                long h = Noise.hash(salt, gx, gz, 0x5173);
                double u = Noise.unit(h);
                String kind = null;
                switch (biome) {
                    case MOSSY_WEALD, HIGHPINE_FOREST -> kind = u < 0.10 ? "mossy_castle" : (u < 0.85 ? "overgrown_ruin" : null);
                    case WINDLESS_STEPPE, CASCADE_VALE -> kind = u < 0.10 ? "overgrown_works" : (u < 0.85 ? "overgrown_ruin" : null);
                    case LARK_MEADOWS, ARGENT_PEAKS -> kind = u < 0.07 ? "spire_chapel"
                            : (u < 0.85 && biome != ElysiumBiome.ARGENT_PEAKS ? "overgrown_ruin" : null);
                    case GLASS_SEA -> kind = u < 0.10 ? "spire_chapel" : (u < 0.6 ? "overgrown_ruin" : null);
                    default -> kind = temperate(biome) && u < 0.85 ? "overgrown_ruin" : null;
                }
                if (kind == null) {
                    continue;
                }
                boolean islet = biome == ElysiumBiome.GLASS_SEA && kind.equals("spire_chapel");
                for (int tries = 0; tries < 10; tries++) {
                    int x = gx * cell + 20 + r.nextInt(cell - 40), z = gz * cell + 20 + r.nextInt(cell - 40);
                    double in = inland(x + 0.5, z + 0.5);
                    if (in < 36) {
                        continue;
                    }
                    fill(x, z, in, c);
                    if (c.hasWater() || c.hasArch() || c.top > ElysiumWorld.HIGHEST_SOLID - 40) {
                        continue;
                    }
                    if (!islet && c.riverDist < 24) {
                        continue;
                    }
                    if (seaLevel != NONE && c.top < seaLevel + 2) {
                        continue;
                    }
                    if (!flatDry(x, z, c.top, islet ? 6 : 24, islet, q)) {
                        continue;
                    }
                    out.add(new ElysiumWorld.Site(kind, x, c.top, z, r.nextInt(4), Noise.mix(h + tries)));
                    break;
                }
            }
        }
        // colonnade on the coast, facing outward
        if (temperate(biome) && Noise.unit(Noise.mix(salt + 77)) < 0.3) {
            for (int tries = 0; tries < 8; tries++) {
                double a = r.nextDouble() * Math.PI * 2, dx = Math.cos(a), dz = Math.sin(a);
                double last = -1;
                for (double d = 0; d < extent(R) + 4; d += 1) {
                    if (inland(cx + dx * d, cz + dz * d) > 6) {
                        last = d;
                    }
                }
                if (last < 0) {
                    continue;
                }
                int x = (int) Math.floor(cx + dx * last), z = (int) Math.floor(cz + dz * last);
                double in = inland(x + 0.5, z + 0.5);
                if (in <= 0) {
                    continue;
                }
                fill(x, z, in, c);
                if (c.hasWater() || c.riverDist < 16 || !flatDry(x, z, c.top, 5, false, q)) {
                    continue;
                }
                // rot: 0 faces +Z (south), 1 -X (west), 2 -Z (north), 3 +X (east)
                int rot = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? 3 : 1) : (dz > 0 ? 0 : 2);
                out.add(new ElysiumWorld.Site("white_colonnade", x, c.top, z, rot, Noise.mix(salt + tries)));
                break;
            }
        }
        return List.copyOf(out);
    }

    private boolean flatDry(int x, int z, int top, int radius, boolean allowWaterFar, Column q) {
        int lo = top, hi = top;
        int[][] near = {{6, 0}, {-6, 0}, {0, 6}, {0, -6}, {4, 4}, {-4, -4}, {4, -4}, {-4, 4}};
        for (int[] o : near) {
            double in = inland(x + o[0] + 0.5, z + o[1] + 0.5);
            if (in <= 0) {
                return false;
            }
            fill(x + o[0], z + o[1], in, q);
            if (q.hasWater() || q.hasArch()) {
                return false;
            }
            lo = Math.min(lo, q.top);
            hi = Math.max(hi, q.top);
        }
        if (hi - lo > 3) {
            return false;
        }
        if (!allowWaterFar) {
            for (int k = 0; k < 8; k++) {
                double a = k * Math.PI / 4;
                int xx = x + (int) Math.round(Math.cos(a) * radius), zz = z + (int) Math.round(Math.sin(a) * radius);
                double in = inland(xx + 0.5, zz + 0.5);
                if (in <= 0) {
                    continue;
                }
                fill(xx, zz, in, q);
                if (q.hasWater()) {
                    return false;
                }
            }
        }
        return true;
    }
}
