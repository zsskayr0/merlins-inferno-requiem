package dev.zsskayr.merlins_inferno.worldgen.soul;

import dev.zsskayr.merlins_inferno.worldgen.elysium.core.Noise;

/**
 * El'ysium's IslandPlan silhouette adapted to small volcanic reservoirs: warped coast, noisy lobes,
 * inverted cone and a central tip. The lake has a complete rim instead of El'ysium's river outlets.
 * Pure geometry, with all Y values relative to the lake, shared with offline containment tests.
 */
public final class SoulReservoirShape {
    private final int radiusX, radiusZ, depth, thickness;
    private final long seed;
    private final Noise noise;
    public enum Material { OUTSIDE, ROCK, MAGMA, LAVA, AIR }

    public record Column(int bottom, int floor, int top, boolean pool, double radius, double angle) {}

    public SoulReservoirShape(int radiusX, int radiusZ, int depth, int thickness, long seed) {
        if (radiusX < 5 || radiusZ < 5 || depth < 2 || thickness < depth + 4) {
            throw new IllegalArgumentException("Reservoir needs a solid shell below its lake");
        }
        this.radiusX = radiusX;
        this.radiusZ = radiusZ;
        this.depth = depth;
        this.thickness = thickness;
        this.seed = seed;
        this.noise = new Noise(seed);
    }

    public int radiusX() { return radiusX; }
    public int radiusZ() { return radiusZ; }
    public int depth() { return depth; }
    public int thickness() { return thickness; }
    public long seed() { return seed; }
    public int extentX() { return (int) Math.ceil(radiusX * 1.35) + 2; }
    public int extentZ() { return (int) Math.ceil(radiusZ * 1.35) + 2; }
    public int minY() { return -(int) Math.ceil(thickness * 1.25) - 8; }
    public int maxY() { return 6; }

    public Column column(int x, int z) {
        // Same domain warp and low-order coast lobes as IslandPlan.inland, rescaled to this island.
        double nx = (double) x / radiusX + 0.035 * noise.noise(x / 24.0, z / 24.0);
        double nz = (double) z / radiusZ + 0.035 * noise.noise(x / 24.0 + 91.7, z / 24.0 - 13.3);
        double angle = Math.atan2(nz, nx);
        double cu = Math.cos(angle), cv = Math.sin(angle);
        double lobes = 0.65 * noise.noise(cu * 1.6, cv * 1.6)
                + 0.5 * noise.noise(cu * 3.4 - 7, cv * 3.4 + 3);
        double edge = 1 + 0.17 * Noise.clamp(lobes, -1, 1)
                + 0.045 * noise.noise(cu * 5.3 + 40, cv * 5.3 - 21);
        double r = Math.sqrt(nx * nx + nz * nz) / edge;
        if (r > 1) return null;
        // IslandPlan's steep rim, tapering mountain underside and extra central tip.
        double t = Noise.clamp((1 - r) / 0.85, 0, 1);
        double thick = 5 + 0.6 * (thickness - 5) * Math.pow(t, 0.75)
                + 0.4 * (thickness - 5) * Math.pow(t, 1.8);
        thick *= 1 + 0.10 * noise.noise(x / 27.0, z / 27.0);
        thick += 3 * t * noise.ridged(x / 9.0, z / 9.0, 2);
        if (r < 0.28) thick += 0.12 * thickness * Math.pow(1 - r / 0.28, 2);
        int bottom = -(int) Math.ceil(thick);
        boolean pool = r < 0.66;
        int floor = -(2 + (int) Math.round(depth * Math.max(0, 1 - Math.pow(r / 0.66, 2))));
        bottom = Math.min(bottom, floor - 3);
        // The complete ring is above the fluid surface; no source is placed on an exposed ledge.
        int top = pool ? 0 : 1 + (int) Math.round(4 * Math.sin(Math.PI * (r - 0.66) / 0.34));
        return new Column(bottom, floor, top, pool, r, angle);
    }

    public Material material(Column c, int y) {
        if (c == null || y < c.bottom() || y > maxY()) return Material.OUTSIDE;
        if (c.pool() && y > c.floor() && y <= 0) return Material.LAVA;
        if (y > (c.pool() ? c.floor() : c.top())) return Material.AIR;
        double phase = (seed & 65535L) * (Math.PI * 2.0 / 65536.0);
        // Narrow luminous seams reveal the reservoir from below without opening fluid outlets.
        boolean seam = Math.abs(Math.sin(c.angle() * 7 + phase + y * 0.12)) < 0.10;
        boolean skin = y <= c.bottom() + 1 || (!c.pool() && y >= c.top() - 1);
        return seam && skin ? Material.MAGMA : Material.ROCK;
    }
}
