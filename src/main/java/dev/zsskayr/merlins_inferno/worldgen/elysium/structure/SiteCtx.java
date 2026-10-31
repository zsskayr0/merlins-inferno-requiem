package dev.zsskayr.merlins_inferno.worldgen.elysium.structure;

import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

import dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumWorld;

/**
 * Everything a site builder needs, in the site's local frame: (u, v) horizontal (see {@link Frame}), y absolute.
 * Wraps a {@link Brush} clipped to the decorating chunk +-{@link SiteBuilder#MARGIN} and to
 * {@link SiteBuilder#RADIUS} around the origin, a sequential {@link Rng} seeded from the site salt, and the ground
 * level {@link #floorY} the structure is built against.
 */
final class SiteCtx {
    final Brush brush;
    final Frame frame;
    final Rng rng;
    final ElysiumWorld.Site site;
    /** Y of the ground block under the origin (falls back to the site's y if the origin column is unusable). */
    final int floorY;

    SiteCtx(WorldGenLevel level, ChunkPos chunk, ElysiumWorld.Site site) {
        this.site = site;
        this.brush = Brush.site(level, chunk, SiteBuilder.MARGIN, site.x(), site.z(), SiteBuilder.RADIUS);
        this.frame = new Frame(site.x(), site.z(), site.rot());
        this.rng = new Rng(site.salt());
        int g = brush.ground(site.x(), site.z());
        this.floorY = g == Brush.NONE ? site.y() : g;
    }

    int x(int u, int v) {
        return frame.x(u, v);
    }

    int z(int u, int v) {
        return frame.z(u, v);
    }

    /** Ground block y at local (u, v), or {@link Brush#NONE}. */
    int ground(int u, int v) {
        return brush.ground(x(u, v), z(u, v));
    }

    boolean set(int u, int y, int v, BlockState state) {
        return brush.set(x(u, v), y, z(u, v), state);
    }

    boolean shaped(int u, int y, int v, BlockState state) {
        return brush.shaped(x(u, v), y, z(u, v), state);
    }

    boolean put(int u, int y, int v, BlockState state) {
        return brush.put(x(u, v), y, z(u, v), state);
    }

    boolean clear(int u, int y, int v) {
        return brush.clear(x(u, v), y, z(u, v));
    }

    BlockState get(int u, int y, int v) {
        return brush.get(x(u, v), y, z(u, v));
    }

    boolean free(int u, int y, int v) {
        return brush.free(x(u, v), y, z(u, v));
    }

    boolean solid(int u, int y, int v) {
        return brush.solid(x(u, v), y, z(u, v));
    }

    boolean carpet(int u, int y, int v, BlockState carpet) {
        return brush.carpet(x(u, v), y, z(u, v), carpet);
    }

    int wallVines(int u, int y, int v, int length) {
        return brush.wallVines(x(u, v), y, z(u, v), length);
    }

    int hangingVines(int u, int y, int v, int length) {
        return brush.hangingVines(x(u, v), y, z(u, v), length, Direction.from2DDataValue(rng.nextInt(4)));
    }

    /** Clear everything from y0 to y1 (inclusive) at (u, v). */
    void clearRange(int u, int v, int y0, int y1) {
        for (int y = y0; y <= y1; y++) {
            clear(u, y, v);
        }
    }

    /** Clear only plants/air-replaceable blocks from y0 to y1 at (u, v) (keeps terrain and trees). */
    void clearPlants(int u, int v, int y0, int y1) {
        for (int y = y0; y <= y1; y++) {
            BlockState s = get(u, y, v);
            if (!s.isAir() && free(u, y, v)) {
                clear(u, y, v);
            }
        }
    }

    /**
     * Support a floor block at {@code topY}: fill {@code fill} from topY - 1 down to the ground. Returns false (and
     * places nothing) when the column has no ground or the ground is more than {@code maxDepth} below topY - so no
     * floor ever floats. Terrain above topY is not touched here.
     */
    boolean plinth(int u, int v, int topY, BlockState fill, int maxDepth) {
        int g = ground(u, v);
        if (g == Brush.NONE || g < topY - maxDepth) {
            return false;
        }
        for (int y = topY - 1; y > g; y--) {
            set(u, y, v, fill);
        }
        return true;
    }

    /**
     * Like {@link #plinth} for buildings: a solid fill up to {@code maxDepth}, and on the {@code pillar} cells a stone
     * pillar carried all the way down to the ground (up to 32 blocks) so a building on a slope stands on stilts.
     * Returns true if this column is supported.
     */
    boolean footing(int u, int v, int topY, BlockState fill, BlockState pillarState, int maxDepth, boolean pillar) {
        int g = ground(u, v);
        if (g == Brush.NONE) {
            return false;
        }
        if (g >= topY - maxDepth) {
            for (int y = topY - 1; y > g; y--) {
                set(u, y, v, fill);
            }
            return true;
        }
        if (!pillar || g < topY - 32) {
            return false;
        }
        for (int y = topY - 1; y > g; y--) {
            set(u, y, v, y >= topY - maxDepth ? fill : pillarState);
        }
        return true;
    }

    /** World direction nearest to the local vector (du, dv). */
    Direction worldDir(double du, double dv) {
        Direction f = frame.forward, r = frame.right;
        double wx = f.getStepX() * du + r.getStepX() * dv;
        double wz = f.getStepZ() * du + r.getStepZ() * dv;
        if (Math.abs(wx) >= Math.abs(wz)) {
            return wx >= 0 ? Direction.EAST : Direction.WEST;
        }
        return wz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /** Per-position hash in [0,1) independent of the call order (for textures: which brick is mossy...). */
    double noise(int u, int y, int v) {
        return Rng.unit(site.salt(), u, y, v);
    }

    /** Draws a straight line of {@code state} between two local points (inclusive), one block per unit step. */
    void line(int u0, int y0, int v0, int u1, int y1, int v1, BlockState state, boolean shapedBlock) {
        int n = Math.max(Math.abs(u1 - u0), Math.max(Math.abs(y1 - y0), Math.abs(v1 - v0)));
        for (int i = 0; i <= n; i++) {
            double t = n == 0 ? 0.0 : i / (double) n;
            int u = (int) Math.round(Shapes.lerp(u0, u1, t));
            int y = (int) Math.round(Shapes.lerp(y0, y1, t));
            int v = (int) Math.round(Shapes.lerp(v0, v1, t));
            if (shapedBlock) {
                shaped(u, y, v, state);
            } else {
                set(u, y, v, state);
            }
        }
    }
}
