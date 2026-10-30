package dev.zsskayr.merlins_inferno.worldgen.feature;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumSpans;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumTerrain;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumTerrains;

/**
 * Draws El'ysium's spans (see {@link ElysiumSpans}) into the chunk being decorated: an arched Arganite bridge with
 * railings and lanterns across each short gap, and under every span two enormous chains of diamond links in alternating
 * planes, hanging in a curve from one island's underside to the other's. Each chunk draws only its own columns and
 * works every shape out from the span alone, so the pieces meet exactly across chunk borders.
 */
public class ElysiumSpansFeature extends Feature<NoneFeatureConfiguration> {
    private static final int LINK = 4;
    private static final double LINK_SPACING = 2.0 * LINK;
    private static final double CHAIN_OFFSET = 9.0;

    public ElysiumSpansFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ElysiumTerrain terrain = ElysiumTerrains.forLevel(level.getLevel());
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        List<ElysiumSpans.Span> spans = ElysiumTerrains.spans(terrain).near(x0, z0, x0 + 15, z0 + 15);
        boolean any = false;
        for (ElysiumSpans.Span span : spans) {
            if (span.hasBridge()) {
                any |= bridge(level, span, x0, z0);
            }
            any |= chain(level, span, -CHAIN_OFFSET, x0, z0);
            any |= chain(level, span, CHAIN_OFFSET, x0, z0);
        }
        return any;
    }

    // ---- bridge ----

    /** Where a column sits on a span's bridge: {along, across} in blocks from A and the centre line, or null. */
    private static double[] frame(ElysiumSpans.Span s, int x, int z) {
        double dx = s.bx() - s.ax(), dz = s.bz() - s.az(), len = Math.hypot(dx, dz);
        double ux = dx / len, uz = dz / len;
        double rx = x + 0.5 - s.ax(), rz = z + 0.5 - s.az();
        double along = rx * ux + rz * uz, across = -rx * uz + rz * ux;
        return new double[] {along, across, len};
    }

    private static int deckY(ElysiumSpans.Span s, double along, double len) {
        double f = along / len;
        return (int) Math.round(s.ay() + (s.by() - s.ay()) * f + (4.0 + len * 0.05) * Math.sin(Math.PI * f));
    }

    /** 0 off the bridge, 1 deck, 2 railing line. */
    private static int kind(ElysiumSpans.Span s, int x, int z) {
        double[] f = frame(s, x, z);
        if (f[0] < 0.0 || f[0] > f[2]) {
            return 0;
        }
        double a = Math.abs(f[1]);
        return a > 2.5 ? 0 : a > 1.5 ? 2 : 1;
    }

    private static boolean bridge(WorldGenLevel level, ElysiumSpans.Span s, int x0, int z0) {
        BlockState bricks = ModBlocks.ARGANITE_BRICKS.get().defaultBlockState();
        BlockState wall = ModBlocks.ARGANITE_BRICKS_WALL.get().defaultBlockState();
        BlockState lantern = Blocks.LANTERN.defaultBlockState();
        boolean any = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                int kind = kind(s, x, z);
                if (kind == 0) {
                    continue;
                }
                double[] f = frame(s, x, z);
                int y = deckY(s, f[0], f[2]);
                any |= put(level, pos.set(x, y, z), bricks);
                if (Math.abs(f[1]) <= 1.5) {
                    // the arch's belly: a thicker spine under the middle of the deck
                    any |= put(level, pos.set(x, y - 1, z), bricks);
                    if (Math.abs(f[1]) <= 0.5) {
                        any |= put(level, pos.set(x, y - 2, z), bricks);
                    }
                }
                if (kind == 2) {
                    BlockState state = wall;
                    state = state.setValue(WallBlock.NORTH_WALL, rail(s, x, z - 1, y) ? WallSide.LOW : WallSide.NONE)
                            .setValue(WallBlock.SOUTH_WALL, rail(s, x, z + 1, y) ? WallSide.LOW : WallSide.NONE)
                            .setValue(WallBlock.WEST_WALL, rail(s, x - 1, z, y) ? WallSide.LOW : WallSide.NONE)
                            .setValue(WallBlock.EAST_WALL, rail(s, x + 1, z, y) ? WallSide.LOW : WallSide.NONE);
                    any |= put(level, pos.set(x, y + 1, z), state);
                    // a lantern on the railing every dozen blocks
                    if (((int) Math.floor(f[0] / 12.0)) != ((int) Math.floor((f[0] - 1.0) / 12.0)) && f[0] > 2.0 && f[0] < f[2] - 2.0) {
                        any |= put(level, pos.set(x, y + 2, z), lantern);
                    }
                }
            }
        }
        return any;
    }

    private static boolean rail(ElysiumSpans.Span s, int x, int z, int y) {
        if (kind(s, x, z) != 2) {
            return false;
        }
        double[] f = frame(s, x, z);
        return Math.abs(deckY(s, f[0], f[2]) - y) <= 1;
    }

    // ---- chains ----

    private static boolean chain(WorldGenLevel level, ElysiumSpans.Span s, double offset, int x0, int z0) {
        double dx = s.bx() - s.ax(), dz = s.bz() - s.az(), len = Math.hypot(dx, dz);
        double ux = dx / len, uz = dz / len;
        double ox = -uz * offset, oz = ux * offset;
        double ax = s.ax() - ux * 14 + ox, az = s.az() - uz * 14 + oz, ay = s.aBottom();
        double bx = s.bx() + ux * 14 + ox, bz = s.bz() + uz * 14 + oz, by = s.bBottom();
        // a long crossing would sag out of the bottom of the world: keep the lowest link a few blocks above it
        double sag = Math.min(0.26 * len + 22.0, Math.max(4.0, (ay + by) / 2.0 - 8.0));
        int n = Math.max(24, (int) (len / 3.0));
        double[][] pts = new double[n + 1][3];
        double[] arc = new double[n + 1];
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            pts[i][0] = ax + (bx - ax) * t;
            pts[i][1] = ay + (by - ay) * t - sag * 4.0 * t * (1.0 - t);
            pts[i][2] = az + (bz - az) * t;
            if (i > 0) {
                arc[i] = arc[i - 1] + Math.sqrt(sq(pts[i][0] - pts[i - 1][0]) + sq(pts[i][1] - pts[i - 1][1]) + sq(pts[i][2] - pts[i - 1][2]));
            }
        }
        BlockState material = Blocks.IRON_BLOCK.defaultBlockState();
        boolean any = false;
        int seg = 0;
        int links = (int) (arc[n] / LINK_SPACING);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int k = 0; k < links; k++) {
            double at = (k + 0.5) * LINK_SPACING;
            while (seg < n - 1 && arc[seg + 1] < at) {
                seg++;
            }
            double f = (at - arc[seg]) / Math.max(1e-6, arc[seg + 1] - arc[seg]);
            double cx = pts[seg][0] + (pts[seg + 1][0] - pts[seg][0]) * f;
            double cy = pts[seg][1] + (pts[seg + 1][1] - pts[seg][1]) * f;
            double cz = pts[seg][2] + (pts[seg + 1][2] - pts[seg][2]) * f;
            if (cx < x0 - 12 || cx > x0 + 28 || cz < z0 - 12 || cz > z0 + 28) {
                continue;
            }
            double tx = pts[seg + 1][0] - pts[seg][0], ty = pts[seg + 1][1] - pts[seg][1], tz = pts[seg + 1][2] - pts[seg][2];
            double tl = Math.sqrt(tx * tx + ty * ty + tz * tz);
            tx /= tl;
            ty /= tl;
            tz /= tl;
            // horizontal side-vector h, and v = t x h: alternate links lie in (t, h) and (t, v)
            double hx = -tz, hz = tx, hl = Math.hypot(hx, hz);
            hx /= hl;
            hz /= hl;
            double vx = ty * hz, vy = tz * hx - tx * hz, vz = -ty * hx;
            double nx, ny, nz, bx2, by2, bz2;
            if ((k & 1) == 0) {
                nx = hx;
                ny = 0.0;
                nz = hz;
                bx2 = vx;
                by2 = vy;
                bz2 = vz;
            } else {
                nx = vx;
                ny = vy;
                nz = vz;
                bx2 = hx;
                by2 = 0.0;
                bz2 = hz;
            }
            int steps = LINK * 5;
            for (int edge = 0; edge < 4; edge++) {
                // diamond corners in (along, across): (S,0) (0,S) (-S,0) (0,-S)
                double[][] corner = {{LINK, 0}, {0, LINK}, {-LINK, 0}, {0, -LINK}};
                double[] p = corner[edge], q = corner[(edge + 1) % 4];
                for (int i = 0; i < steps; i++) {
                    double u = i / (double) steps;
                    double sAlong = p[0] + (q[0] - p[0]) * u, sAcross = p[1] + (q[1] - p[1]) * u;
                    for (int thick = 0; thick < 2; thick++) {
                        double wx = cx + tx * sAlong + nx * sAcross + bx2 * thick;
                        double wy = cy + ty * sAlong + ny * sAcross + by2 * thick;
                        double wz = cz + tz * sAlong + nz * sAcross + bz2 * thick;
                        int ix = (int) Math.floor(wx), iy = (int) Math.floor(wy), iz = (int) Math.floor(wz);
                        if (ix >= x0 && ix < x0 + 16 && iz >= z0 && iz < z0 + 16) {
                            any |= put(level, pos.set(ix, iy, iz), material);
                        }
                    }
                }
            }
        }
        return any;
    }

    private static double sq(double v) {
        return v * v;
    }

    private static boolean put(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
            return false;
        }
        BlockState current = level.getBlockState(pos);
        if (current.isAir() || current.canBeReplaced()) {
            level.setBlock(pos, state, 2);
            return true;
        }
        return false;
    }
}
