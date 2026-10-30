package dev.zsskayr.merlins_inferno.worldgen.feature;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

import dev.zsskayr.merlins_inferno.block.LyriumBlock;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumSpans;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumTerrain;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumTerrains;

/**
 * Everything that makes a slash (see {@link ElysiumTerrain}) look struck and then weathered: the rock layers that stand
 * back by different amounts, with balconies under deep alcoves; rubble and bare ground on the rim; vines and springs
 * over the edge; caves lined with quartzite and quartz or Lyrium; a glowing line where the blow landed; fissures; rock
 * left floating in the gap; and lower bridges that walk into the wall through arched doorways.
 *
 * <p>Every column and every site decides from its own coordinates alone, so chunks agree where they meet. Nothing
 * writes more than a chunk away from the one being decorated.
 */
public class ElysiumErosionFeature extends Feature<NoneFeatureConfiguration> {
    private static final double RIM = 14.0;
    private static final double INF = Double.POSITIVE_INFINITY;

    public ElysiumErosionFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    private static double rand(int x, int y, int z, int salt) {
        long h = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L ^ salt * 0x27D4EB2F165667C5L;
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return (h >>> 11) / (double) (1L << 53);
    }

    /** Per-chunk working state. */
    private static final class Ctx {
        final WorldGenLevel level;
        final ElysiumTerrain terrain;
        final int cx, cz;
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean any;

        Ctx(WorldGenLevel level, ElysiumTerrain terrain, int x0, int z0) {
            this.level = level;
            this.terrain = terrain;
            this.cx = x0 >> 4;
            this.cz = z0 >> 4;
        }

        /** Only this chunk and its eight neighbours may be touched. */
        boolean reach(int x, int z) {
            return Math.abs((x >> 4) - cx) <= 1 && Math.abs((z >> 4) - cz) <= 1;
        }

        BlockState get(int x, int y, int z) {
            if (y < 0 || y >= ElysiumTerrain.MAX_Y || !reach(x, z)) {
                return Blocks.AIR.defaultBlockState();
            }
            return level.getBlockState(pos.set(x, y, z));
        }

        boolean set(int x, int y, int z, BlockState state) {
            if (y < 0 || y >= ElysiumTerrain.MAX_Y || !reach(x, z)) {
                return false;
            }
            level.setBlock(pos.set(x, y, z), state, 2);
            any = true;
            return true;
        }
    }

    /** Bridges, chains and lanterns are never worn away. */
    private static boolean kept(BlockState s) {
        return s.is(ModBlocks.ARGANITE_BRICKS.get()) || s.is(ModBlocks.ARGANITE_BRICKS_WALL.get()) || s.is(Blocks.LANTERN) || s.is(Blocks.IRON_BLOCK);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ElysiumTerrain terrain = ElysiumTerrains.forLevel(level.getLevel());
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        Ctx c = new Ctx(level, terrain, x0, z0);
        ElysiumTerrain.Column[] cols = terrain.columns(x0 >> 4, z0 >> 4);
        ElysiumTerrain.Column column;
        double[] near = new double[2];
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                double d = terrain.nearestSlash(x, z, near);
                if (d <= 0.0 || d > ElysiumTerrain.WALL_ZONE) {
                    continue;
                }
                column = cols[dz * 16 + dx];
                if (!column.land) {
                    continue;
                }
                double depth = near[1];
                lip(c, column, x, z, d);
                wall(c, column, x, z, d, depth);
                if (d <= 1.6) {
                    edge(c, column, x, z, d);
                    cave(c, column, x, z, depth);
                }
            }
        }
        debris(c, x0, z0);
        lowerBridges(c, x0, z0);
        return c.any;
    }

    // ---- the rim ----

    /** Bare ground and loose stones along the lip, thickest at the edge. */
    private static void lip(Ctx c, ElysiumTerrain.Column column, int x, int z, double d) {
        if (d > RIM) {
            return;
        }
        int top = (int) column.top;
        double near = 1.0 - d / RIM;
        if (rand(x, 0, z, 1) >= 0.65 * near * near) {
            return;
        }
        Block ground = switch ((int) (rand(x, 1, z, 2) * 4)) {
            case 0 -> Blocks.GRAVEL;
            case 1 -> Blocks.COARSE_DIRT;
            case 2 -> Blocks.ANDESITE;
            default -> Blocks.COBBLESTONE;
        };
        c.set(x, top, z, ground.defaultBlockState());
        if (rand(x, 2, z, 3) < 0.30 && c.get(x, top + 1, z).isAir()) {
            Block rock = rand(x, 3, z, 4) < 0.5 ? Blocks.COBBLESTONE : ModBlocks.ARGANITE.get();
            c.set(x, top + 1, z, rock.defaultBlockState());
        }
    }

    /** Vines hanging over the edge. */
    private static void edge(Ctx c, ElysiumTerrain.Column column, int x, int z, double d) {
        int top = (int) column.top;
        for (Direction face : Direction.Plane.HORIZONTAL) {
            if (rand(x, face.ordinal(), z, 6) > 0.22) {
                continue;
            }
            int len = 3 + (int) (rand(x, 9, z, 7) * 12);
            BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(face.getOpposite()), true);
            for (int y = top; y > top - len; y--) {
                if (!c.get(x + face.getStepX(), y, z + face.getStepZ()).isAir()) {
                    break;
                }
                c.set(x + face.getStepX(), y, z + face.getStepZ(), vine);
            }
        }
    }

    // ---- the wall ----

    private static void wall(Ctx c, ElysiumTerrain.Column column, int x, int z, double d, double depth) {
        int top = (int) column.top;
        boolean through = depth == INF;
        int low = through ? (int) column.bottom : (int) Math.max(column.bottom, Math.floor(top - depth));
        // the highest a trench floor can be: nothing is carved below it
        double floorLimit = through ? -1.0e9 : top - 0.8 * depth;
        ElysiumTerrain.Wall wall = c.terrain.wall(x, z, top, floorLimit);
        if (d > wall.reach()) {
            return;
        }
        // far from the wall only an alcove can still reach: look at its heights alone
        int yHi = top - 1;
        if (d > wall.strataReach()) {
            low = Math.max(low, wall.alcoveLow());
            yHi = Math.min(yHi, wall.alcoveHigh());
        }
        int markDepth = c.terrain.markDepth(x, z);
        boolean lit = c.terrain.markLit(x, z);
        BlockState arganite = ModBlocks.ARGANITE.get().defaultBlockState();
        double r = wall.recess(low);
        BlockState plant = null;
        for (int y = low; y <= yHi; y++) {
            double next = wall.recess(y + 1);
            BlockState st = c.get(x, y, z);
            if (r > d && !kept(st)) {
                if (!st.isAir()) {
                    c.set(x, y, z, Blocks.AIR.defaultBlockState());
                }
                if (plant != null) {
                    if (column.humidity > 0.1 && rand(x, y, z, 23) < 0.03) {
                        // a spring on the ledge: it spills over the edge as a small waterfall
                        c.set(x, y, z, Blocks.WATER.defaultBlockState());
                        c.level.scheduleTick(new BlockPos(x, y, z), Fluids.WATER, 0);
                    } else {
                        c.set(x, y, z, plant);
                    }
                    plant = null;
                }
            } else {
                plant = null;
                if (st.is(arganite.getBlock())) {
                    if (next > d && y + 1 < top) {
                        // the top of a ledge: moss and grass, with a plant growing out of it
                        double roll = rand(x, y, z, 21);
                        c.set(x, y, z, (roll < 0.55 ? Blocks.GRASS_BLOCK : roll < 0.8 ? Blocks.MOSS_BLOCK : Blocks.COARSE_DIRT).defaultBlockState());
                        double p = rand(x, y, z, 22);
                        plant = p < 0.45 ? Blocks.SHORT_GRASS.defaultBlockState()
                                : p < 0.55 ? Blocks.FERN.defaultBlockState()
                                : p < 0.59 ? Blocks.AZALEA.defaultBlockState()
                                : p < 0.62 ? Blocks.FLOWERING_AZALEA.defaultBlockState() : null;
                    } else if (d - r < 1.2) {
                        BlockState face = faceBlock(c.terrain, wall, x, y, z, top - y == markDepth && lit);
                        if (face != null) {
                            c.set(x, y, z, face);
                        }
                    }
                }
            }
            r = next;
        }
    }

    /** What a freshly exposed face of the wall is made of, or null to leave it Arganite. */
    private static BlockState faceBlock(ElysiumTerrain terrain, ElysiumTerrain.Wall wall, int x, int y, int z, boolean mark) {
        if (mark) {
            // a thin quartzite line, with a lantern set in it every nine blocks
            return ((x + z) % 9 == 0 ? Blocks.SEA_LANTERN : ModBlocks.QUARTZITE.get()).defaultBlockState();
        }
        double blob = rand(x >> 1, y >> 2, z >> 1, 5);
        if (blob < 0.07) {
            return (blob < 0.035 ? Blocks.ANDESITE : blob < 0.055 ? Blocks.COBBLESTONE : Blocks.GRAVEL).defaultBlockState();
        }
        if (terrain.crack(x, y, z)) {
            return Blocks.SMOOTH_BASALT.defaultBlockState();
        }
        return switch (wall.band(y)) {
            case 2 -> Blocks.CALCITE.defaultBlockState();
            case 5 -> Blocks.DIORITE.defaultBlockState();
            default -> null;
        };
    }

    // ---- caves ----

    /** A pocket cut into the wall, lined with quartzite and set with quartz or Lyrium crystals. */
    private static void cave(Ctx c, ElysiumTerrain.Column column, int x, int z, double depth) {
        if (rand(x, 0, z, 11) >= 0.006) {
            return;
        }
        double[] tmp = new double[2];
        double gx = c.terrain.nearestSlash(x + 3, z, tmp) - c.terrain.nearestSlash(x - 3, z, tmp);
        double gz = c.terrain.nearestSlash(x, z + 3, tmp) - c.terrain.nearestSlash(x, z - 3, tmp);
        double len = Math.hypot(gx, gz);
        if (len < 1.0e-3) {
            return;
        }
        double ux = gx / len, uz = gz / len;
        int top = (int) column.top;
        int lo = depth == INF ? (int) column.bottom + 14 : (int) (top - depth) + 10;
        int hi = top - 18;
        if (hi <= lo) {
            return;
        }
        int cy = lo + (int) (rand(x, 1, z, 12) * (hi - lo));
        double ccx = x + ux * 5.5, ccz = z + uz * 5.5;
        double r = 3.0 + 1.6 * rand(x, 2, z, 13);
        boolean lyrium = rand(x, 3, z, 14) < 0.35;
        BlockState shell = ModBlocks.QUARTZITE.get().defaultBlockState();
        BlockState lyriumShell = ModBlocks.LYRIUM_BLOCK.get().defaultBlockState().setValue(LyriumBlock.PLACED, false);
        BlockState air = Blocks.AIR.defaultBlockState();
        int ir = (int) Math.ceil(r + 2.5);
        int mx = (int) Math.round(ccx), mz = (int) Math.round(ccz);
        for (int dx = -ir; dx <= ir; dx++) {
            for (int dz = -ir; dz <= ir; dz++) {
                for (int dy = -ir; dy <= ir; dy++) {
                    double q = (dx * dx + dz * dz) / (r * r) + dy * dy / (0.72 * r * 0.72 * r);
                    double outer = 1.0 + 1.5 / r;
                    if (q > outer * outer) {
                        continue;
                    }
                    carveOrLine(c, mx + dx, cy + dy, mz + dz, q <= 1.0, shell, lyrium && rand(mx + dx, cy + dy, mz + dz, 15) < 0.3 ? lyriumShell : shell);
                }
            }
        }
        // the mouth: a short tunnel from the pocket out to the face of the wall
        for (int t = 0; t <= 13; t++) {
            int tx = (int) Math.round(ccx - ux * t), tz = (int) Math.round(ccz - uz * t);
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    for (int dy = -3; dy <= 3; dy++) {
                        double q = dx * dx + dz * dz + dy * dy;
                        if (q <= 9.0) {
                            carveOrLine(c, tx + dx, cy + dy, tz + dz, q <= 4.5, shell, shell);
                        }
                    }
                }
            }
        }
        // crystals growing off the floor and the ceiling
        BlockState up = cluster(lyrium, Direction.UP), down = cluster(lyrium, Direction.DOWN);
        for (int dx = -ir; dx <= ir; dx++) {
            for (int dz = -ir; dz <= ir; dz++) {
                for (int dy = -ir; dy <= ir; dy++) {
                    int px = mx + dx, py = cy + dy, pz = mz + dz;
                    double q = (dx * dx + dz * dz) / (r * r) + dy * dy / (0.72 * r * 0.72 * r);
                    if (q > 0.9 || !c.get(px, py, pz).isAir()) {
                        continue;
                    }
                    if (isShell(c.get(px, py - 1, pz)) && rand(px, py, pz, 16) < 0.16) {
                        c.set(px, py, pz, up);
                    } else if (isShell(c.get(px, py + 1, pz)) && rand(px, py, pz, 17) < 0.12) {
                        c.set(px, py, pz, down);
                    }
                }
            }
        }
    }

    private static boolean isShell(BlockState s) {
        return s.is(ModBlocks.QUARTZITE.get()) || s.is(ModBlocks.LYRIUM_BLOCK.get());
    }

    private static BlockState cluster(boolean lyrium, Direction facing) {
        if (lyrium) {
            return ModBlocks.LYRIUM_CLUSTER.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, facing)
                    .setValue(dev.zsskayr.merlins_inferno.block.LyriumClusterBlock.PLACED, false);
        }
        return ModBlocks.QUARTZ_CLUSTER.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, facing);
    }

    private static void carveOrLine(Ctx c, int x, int y, int z, boolean inside, BlockState air, BlockState lining) {
        BlockState st = c.get(x, y, z);
        if (st.isAir() || kept(st) || isShell(st) || st.getFluidState().isSource()) {
            return;
        }
        c.set(x, y, z, inside ? Blocks.AIR.defaultBlockState() : lining);
    }

    // ---- rock left floating in the gap ----

    private static void debris(Ctx c, int x0, int z0) {
        final int cell = 64;
        List<ElysiumSpans.Span> spans = ElysiumTerrains.spans(c.terrain).near(x0 - 16, z0 - 16, x0 + 31, z0 + 31);
        ElysiumTerrain.Column col = new ElysiumTerrain.Column();
        for (int ci = Math.floorDiv(x0, cell); ci <= Math.floorDiv(x0 + 15, cell); ci++) {
            for (int cj = Math.floorDiv(z0, cell); cj <= Math.floorDiv(z0 + 15, cell); cj++) {
                if (rand(ci, 0, cj, 31) > 0.5) {
                    continue;
                }
                int sx = ci * cell + (int) (rand(ci, 1, cj, 32) * cell), sz = cj * cell + (int) (rand(ci, 2, cj, 33) * cell);
                if (sx < x0 || sx > x0 + 15 || sz < z0 || sz > z0 + 15) {
                    continue;
                }
                double base = c.terrain.baseInlandAt(sx, sz);
                // an islet in a slash is small; one out in the open sky between the islands can be a good deal bigger
                boolean gap = base <= 0.0;
                double pick = rand(ci, 3, cj, 34);
                double r = gap ? 4.0 + 9.0 * pick * pick : 3.0 + 5.0 * pick;
                if (gap && (base < -380.0 || rand(ci, 5, cj, 36) > 0.6)) {
                    continue;
                }
                c.terrain.sample(sx, sz, col);
                if (col.land || col.inland > -(r + 6.0)) {
                    continue;
                }
                int ri = (int) Math.ceil(r + 3.0);
                if (c.terrain.inlandAt(sx + ri, sz) > -2.0 || c.terrain.inlandAt(sx - ri, sz) > -2.0
                        || c.terrain.inlandAt(sx, sz + ri) > -2.0 || c.terrain.inlandAt(sx, sz - ri) > -2.0) {
                    continue;
                }
                if (nearSpan(spans, sx, sz, r + 14.0)) {
                    continue;
                }
                int cy;
                if (gap) {
                    cy = (int) (c.terrain.skyLevel(sx, sz) - 75.0 + 125.0 * rand(ci, 4, cj, 35));
                } else {
                    double span = Math.min(60.0, (col.top - col.bottom) * 0.5);
                    cy = (int) (col.top - 22.0 - rand(ci, 4, cj, 35) * span);
                    if (cy - r * 1.4 < col.bottom + 6) {
                        continue;
                    }
                }
                if (cy < 24 || cy > ElysiumTerrain.MAX_Y - 24) {
                    continue;
                }
                fragment(c, sx, cy, sz, r, rand(ci, 6, cj, 37));
            }
        }
    }

    private static boolean nearSpan(List<ElysiumSpans.Span> spans, int x, int z, double within) {
        for (ElysiumSpans.Span s : spans) {
            double dx = s.bx() - s.ax(), dz = s.bz() - s.az();
            double len2 = dx * dx + dz * dz;
            double t = len2 == 0 ? 0 : Math.max(0.0, Math.min(1.0, ((x - s.ax()) * dx + (z - s.az()) * dz) / len2));
            if (Math.hypot(x - (s.ax() + dx * t), z - (s.az() + dz * t)) < within) {
                return true;
            }
        }
        return false;
    }

    /** A chunk of the missing slice: flat grassy top, a long root of rock hanging beneath. */
    private static void fragment(Ctx c, int cx, int cy, int cz, double r, double style) {
        // some are squat plates, some deep cones; a few are bare rock
        double drop = 0.9 + 1.0 * style;
        boolean bare = style > 0.82;
        int ry = (int) Math.ceil(r * drop) + 1;
        int rh = (int) Math.ceil(r) + 1;
        BlockState rock = ModBlocks.ARGANITE.get().defaultBlockState();
        for (int dx = -rh; dx <= rh; dx++) {
            for (int dz = -rh; dz <= rh; dz++) {
                double wobble = 0.85 + 0.3 * rand(cx + dx, 0, cz + dz, 41);
                int topY = Integer.MIN_VALUE;
                for (int dy = ry; dy >= -ry; dy--) {
                    double yy = dy >= 0 ? dy / (0.35 * r) : dy / (drop * r);
                    double f = (dx * dx + dz * dz) / (r * r * wobble) + yy * yy;
                    if (f > 1.0) {
                        continue;
                    }
                    int x = cx + dx, y = cy + dy, z = cz + dz;
                    if (!c.get(x, y, z).isAir()) {
                        continue;
                    }
                    c.set(x, y, z, rock);
                    topY = Math.max(topY, y);
                }
                if (topY != Integer.MIN_VALUE && !bare) {
                    c.set(cx + dx, topY, cz + dz, Blocks.GRASS_BLOCK.defaultBlockState());
                    double p = rand(cx + dx, topY, cz + dz, 42);
                    if (p < 0.35 && c.get(cx + dx, topY + 1, cz + dz).isAir()) {
                        c.set(cx + dx, topY + 1, cz + dz, (p < 0.02 ? Blocks.AZALEA : Blocks.SHORT_GRASS).defaultBlockState());
                    }
                }
            }
        }
    }

    // ---- lower bridges ----

    /** Where a column sits on a span: {along, across, length}. */
    private static double[] frame(ElysiumSpans.Span s, int x, int z) {
        double dx = s.bx() - s.ax(), dz = s.bz() - s.az(), len = Math.hypot(dx, dz);
        double ux = dx / len, uz = dz / len;
        double rx = x + 0.5 - s.ax(), rz = z + 0.5 - s.az();
        return new double[] {rx * ux + rz * uz, -rx * uz + rz * ux, len};
    }

    /**
     * Under some of the short crossings, a plain bridge at a lower level that goes through an arched doorway into the wall
     * at each end, or a stub of one that stops half way, broken.
     */
    private static void lowerBridges(Ctx c, int x0, int z0) {
        BlockState bricks = ModBlocks.ARGANITE_BRICKS.get().defaultBlockState();
        BlockState post = ModBlocks.ARGANITE_BRICKS_WALL.get().defaultBlockState();
        BlockState lantern = Blocks.LANTERN.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (ElysiumSpans.Span s : ElysiumTerrains.spans(c.terrain).near(x0 - 20, z0 - 20, x0 + 35, z0 + 35)) {
            if (s.length() > 150.0) {
                continue;
            }
            int sa = (int) s.ax(), sb = (int) s.bz();
            double roll = rand(sa, (int) s.az(), sb, 51);
            if (roll > 0.6) {
                continue;
            }
            int y0 = (int) (Math.min(s.ay(), s.by()) - 30.0 - 14.0 * rand(sa, 1, sb, 52));
            if (y0 < Math.max(s.aBottom(), s.bBottom()) + 14.0) {
                continue;
            }
            boolean broken = roll > 0.36;
            boolean fromA = rand(sa, 2, sb, 53) < 0.5;
            double stub = 0.3 + 0.25 * rand(sa, 3, sb, 54);
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    int x = x0 + dx, z = z0 + dz;
                    double[] f = frame(s, x, z);
                    double along = f[0], across = Math.abs(f[1]), len = f[2];
                    if (across > 1.6 || along < -18.0 || along > len + 18.0) {
                        continue;
                    }
                    if (broken) {
                        double reach = fromA ? along : len - along;
                        if (reach > len * stub + 14.0 * (rand(x, 0, z, 55) - 0.5) || reach > len * stub + 4.0 && rand(x, 1, z, 56) < 0.3) {
                            continue;
                        }
                    }
                    boolean inRock = along < 1.0 || along > len - 1.0;
                    if (c.get(x, y0, z).isAir() || !inRock) {
                        c.set(x, y0, z, bricks);
                    }
                    for (int yy = y0 + 1; yy <= y0 + 3; yy++) {
                        // the doorway: three high, and rounded at the corners
                        boolean corner = yy == y0 + 3 && across > 0.8;
                        BlockState st = c.get(x, yy, z);
                        if (inRock && !st.isAir() && !kept(st) && !corner) {
                            c.set(x, yy, z, air);
                        }
                    }
                    if (across > 0.8 && !inRock && ((int) Math.floor(along)) % 4 == 0) {
                        c.set(x, y0 + 1, z, post);
                        if (((int) Math.floor(along)) % 12 == 0) {
                            c.set(x, y0 + 2, z, lantern);
                        }
                    }
                }
            }
        }
    }
}
