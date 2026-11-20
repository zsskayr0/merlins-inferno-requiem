package dev.zsskayr.merlins_inferno.worldgen.soul;

/** Taut ground tether with alternating diamond links, following El'ysium's giant-chain construction. */
public record SoulAnchorChain(int ax, int ay, int az, int bx, int by, int bz) {
    public static final int REACH = 4;
    @FunctionalInterface public interface VoxelConsumer { void accept(int x, int y, int z); }

    public void forEachBlock(VoxelConsumer out) {
        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1) return;
        double tx = dx / length, ty = dy / length, tz = dz / length;
        double horizontal = Math.hypot(tx, tz);
        double hx = horizontal < 0.01 ? 1 : -tz / horizontal;
        double hz = horizontal < 0.01 ? 0 : tx / horizontal;
        double vx = ty * hz, vy = tz * hx - tx * hz, vz = -ty * hx;
        int links = Math.max(1, (int) Math.ceil(length / 6));
        double spacing = length / links;
        // Every link shares its end with the next. The last one reaches the buried ground anchor exactly.
        double[][] corners = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}};
        for (int link = 0; link < links; link++) {
            double at = (link + 0.5) * spacing;
            double nx = (link & 1) == 0 ? hx : vx;
            double ny = (link & 1) == 0 ? 0 : vy;
            double nz = (link & 1) == 0 ? hz : vz;
            for (int edge = 0; edge < 4; edge++) {
                double[] a = corners[edge], b = corners[(edge + 1) % 4];
                for (int sample = 0; sample <= 24; sample++) {
                    double f = sample / 24.0;
                    double along = at + (a[0] + (b[0] - a[0]) * f) * spacing / 2;
                    double across = (a[1] + (b[1] - a[1]) * f) * 2.5;
                    out.accept((int) Math.floor(ax + 0.5 + tx * along + nx * across),
                            (int) Math.floor(ay + 0.5 + ty * along + ny * across),
                            (int) Math.floor(az + 0.5 + tz * along + nz * across));
                }
            }
        }
    }
}
