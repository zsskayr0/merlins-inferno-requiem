package dev.zsskayr.merlins_inferno.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.block.ElysiumPortalBlock;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumSeed;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.Column;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumWorld;

/**
 * El'ysium: the Angelical route's third Circle, a world of floating continents. It sits at {@value #SCALE} to 1 with the world
 * outside, and travel works like the Nether's: a portal leads to the portal nearest its scaled coordinates, so going out by a
 * portal lit far along El'ysium and back in by the one that opens outside returns you to that same spot. A way in with no
 * portal near its target raises a Purified Obsidian frame on the nearest spawn islet ({@link ElysiumWorld#nearestSpawnIslet}).
 * In a world with no islet near, the search walks outward for land instead.
 */
public final class ElysiumDimension {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "elysium"));

    /** The Circle the way in opens at. Held at the second until the third exists, so the dimension can be reached and tested. */
    public static final int REQUIRED_CIRCLE = ProgressionHelper.SECOND_CIRCLE;

    /** One block walked in El'ysium is this many outside. */
    public static final int SCALE = 70;

    private static final int REUSE_RADIUS = 128;
    /** The frame an arrival raises stands at the middle of its islet, this close at most. */
    private static final int ISLET_RADIUS = 16;
    /** Portals stay inside the vanilla world border, whatever El'ysium's coordinates scale up to. */
    private static final int WORLD_LIMIT = 29_999_000;
    /** An arrival is raised only this far from the coast, so it never hangs over the edge. */
    private static final int MIN_INLAND = 60;
    private static final int SEARCH_STEP = 24;
    private static final int SEARCH_RADIUS = 6000;
    /** Where an arrival goes if no land is found at all (a plinth in the sky, above the cloud sea). */
    private static final int FALLBACK_Y = 200;

    private ElysiumDimension() {
    }

    /** The block a player arrives on in El'ysium, coming from {@code from} outside: beside the portal nearest its scaled coordinates. */
    public static BlockPos ensureArrival(ServerLevel level, BlockPos from) {
        ElysiumPortals data = ElysiumPortals.get(level);
        int x = Math.floorDiv(from.getX(), SCALE), z = Math.floorDiv(from.getZ(), SCALE);
        BlockPos portal = nearestPortal(level, data, x, z, REUSE_RADIUS);
        if (portal == null) {
            ElysiumWorld.SpawnIslet islet = ElysiumSeed.world().nearestSpawnIslet(x, z);
            if (islet != null) {
                x = islet.island().cx();
                z = islet.island().cz();
                portal = nearestPortal(level, data, x, z, ISLET_RADIUS);
            }
            if (portal == null) {
                portal = buildFrame(level, findLand(new BlockPos(x, 0, z), islet != null));
            }
        }
        return standBeside(level, portal);
    }

    /** The block a player arrives on outside, leaving by the El'ysium portal at {@code from}: beside the portal nearest its scaled coordinates. */
    public static BlockPos ensureExit(ServerLevel level, BlockPos from) {
        int x = scaledUp(from.getX()), z = scaledUp(from.getZ());
        BlockPos portal = nearestPortal(level, ElysiumPortals.get(level), x, z, REUSE_RADIUS);
        if (portal == null) {
            level.getChunk(new BlockPos(x, 0, z));
            portal = buildFrame(level, new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z));
        }
        return standBeside(level, portal);
    }

    private static int scaledUp(int coordinate) {
        return (int) Math.max(-WORLD_LIMIT, Math.min(WORLD_LIMIT, (long) coordinate * SCALE));
    }

    /** The nearest live portal within {@code radius}; one that has been broken is forgotten. */
    private static BlockPos nearestPortal(ServerLevel level, ElysiumPortals data, int x, int z, int radius) {
        BlockPos best = null;
        double bestDist = (double) radius * radius;
        for (BlockPos portal : new java.util.ArrayList<>(data.all())) {
            double dx = portal.getX() - x, dz = portal.getZ() - z;
            if (dx * dx + dz * dz > bestDist) {
                continue;
            }
            level.getChunk(portal);
            if (!level.getBlockState(portal).is(ModBlocks.ELYSIUM_PORTAL.get())) {
                data.remove(portal);
                continue;
            }
            bestDist = dx * dx + dz * dz;
            best = portal;
        }
        return best;
    }

    /** Where to stand in front of a portal, which the player may have built anywhere; inside the film itself if nowhere else has room. */
    private static BlockPos standBeside(ServerLevel level, BlockPos portal) {
        boolean alongX = level.getBlockState(portal).getValue(ElysiumPortalBlock.AXIS) == Direction.Axis.X;
        for (int side : new int[] {2, -2}) {
            for (int dy : new int[] {0, -1, 1, -2}) {
                BlockPos p = portal.offset(alongX ? 0 : side, dy, alongX ? side : 0);
                if (level.getBlockState(p.below()).isSolid() && level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()) {
                    return p;
                }
            }
        }
        return portal;
    }

    /** {@code target} if it is dry land (an islet is smaller than the usual margin), else the nearest column of dry island well inland, spiralling outward over the open sky. */
    private static BlockPos findLand(BlockPos target, boolean islet) {
        ElysiumWorld world = ElysiumSeed.world();
        Column c = new Column();
        for (int ring = 0; ring <= SEARCH_RADIUS / SEARCH_STEP; ring++) {
            int r = ring * SEARCH_STEP;
            int points = Math.max(1, ring * 8);
            for (int i = 0; i < points; i++) {
                double a = i * 2.0 * Math.PI / points;
                int x = target.getX() + (int) Math.round(r * Math.cos(a)), z = target.getZ() + (int) Math.round(r * Math.sin(a));
                world.sample(x, z, c);
                if (c.land && !c.hasWater() && (c.inland >= MIN_INLAND || (islet && r == 0))) {
                    return new BlockPos(x, c.top + 1, z);
                }
            }
        }
        return new BlockPos(target.getX(), FALLBACK_Y, target.getZ());
    }

    /** A 4 x 5 Purified Obsidian frame facing north-south on a small plinth at {@code land}; returns the portal block it records. */
    private static BlockPos buildFrame(ServerLevel level, BlockPos land) {
        level.getChunk(land);
        BlockPos origin = land.west();
        BlockState frame = ModBlocks.PURIFIED_OBSIDIAN.get().defaultBlockState();
        for (int x = -1; x <= 4; x++) {
            for (int z = -1; z <= 4; z++) {
                BlockPos p = origin.offset(x, -1, z);
                level.setBlock(p, frame, Block.UPDATE_CLIENTS);
                for (int y = 0; y <= 12; y++) {
                    level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        for (int x = 0; x <= 3; x++) {
            for (int y = 0; y <= 4; y++) {
                boolean edge = x == 0 || x == 3 || y == 0 || y == 4;
                if (edge) {
                    level.setBlock(origin.offset(x, y, 0), frame, Block.UPDATE_CLIENTS);
                }
            }
        }
        BlockPos portal = origin.offset(2, 1, 0); // the corner the shape records, so later arrivals stand on the same spot
        ElysiumPortalShape.findEmptyPortalShape(level, portal, Direction.Axis.X).ifPresent(ElysiumPortalShape::createPortalBlocks);
        return portal;
    }
}
