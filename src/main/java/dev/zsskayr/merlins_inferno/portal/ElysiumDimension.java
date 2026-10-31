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

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumSeed;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.Column;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumWorld;

/**
 * El'ysium: the Angelical route's third Circle, a world of floating continents. Travel works like Oblivion's: same
 * coordinates, the nearest arrival portal is reused, and when there is none a Purified Obsidian frame is raised on the
 * nearest island with room for it (most coordinates are open sky, so the search walks outward until it finds land).
 */
public final class ElysiumDimension {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "elysium"));

    /** The Circle the way in opens at. Held at the second until the third exists, so the dimension can be reached and tested. */
    public static final int REQUIRED_CIRCLE = ProgressionHelper.SECOND_CIRCLE;

    private static final int REUSE_RADIUS = 128;
    /** An arrival is raised only this far from the coast, so it never hangs over the edge. */
    private static final int MIN_INLAND = 60;
    private static final int SEARCH_STEP = 24;
    private static final int SEARCH_RADIUS = 6000;
    /** Where an arrival goes if no land is found at all (a plinth in the sky, above the cloud sea). */
    private static final int FALLBACK_Y = 200;

    private ElysiumDimension() {
    }

    /** The block a player arrives on, beside the (found or freshly built) portal nearest to {@code target}. */
    public static BlockPos ensureArrival(ServerLevel level, BlockPos target) {
        ElysiumPortals data = ElysiumPortals.get(level);
        BlockPos best = null;
        double bestDist = (double) REUSE_RADIUS * REUSE_RADIUS;
        for (BlockPos portal : new java.util.ArrayList<>(data.all())) {
            level.getChunk(portal);
            if (!level.getBlockState(portal).is(ModBlocks.ELYSIUM_PORTAL.get())) {
                data.remove(portal);
                continue;
            }
            double dx = portal.getX() - target.getX(), dz = portal.getZ() - target.getZ();
            if (dx * dx + dz * dz <= bestDist) {
                bestDist = dx * dx + dz * dz;
                best = portal;
            }
        }
        if (best == null) {
            best = buildFrame(level, target, data);
        }
        return best.offset(1, 0, 2);
    }

    /** The nearest column to {@code target} that is dry island well inland, spiralling outward over the open sky. */
    private static BlockPos findLand(ServerLevel level, BlockPos target) {
        ElysiumWorld world = ElysiumSeed.world();
        Column c = new Column();
        for (int ring = 0; ring <= SEARCH_RADIUS / SEARCH_STEP; ring++) {
            int r = ring * SEARCH_STEP;
            int points = Math.max(1, ring * 8);
            for (int i = 0; i < points; i++) {
                double a = i * 2.0 * Math.PI / points;
                int x = target.getX() + (int) Math.round(r * Math.cos(a)), z = target.getZ() + (int) Math.round(r * Math.sin(a));
                world.sample(x, z, c);
                if (c.land && c.inland >= MIN_INLAND && !c.hasWater()) {
                    return new BlockPos(x, c.top + 1, z);
                }
            }
        }
        return new BlockPos(target.getX(), FALLBACK_Y, target.getZ());
    }

    /** A 4 x 5 Purified Obsidian frame facing north-south on a small plinth; returns its interior's lower block. */
    private static BlockPos buildFrame(ServerLevel level, BlockPos target, ElysiumPortals data) {
        BlockPos land = findLand(level, target);
        level.getChunk(land);
        int ground = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, land.getX(), land.getZ());
        if (ground <= level.getMinBuildHeight()) {
            ground = land.getY(); // no land found anywhere near: the plinth floats above the cloud sea
        }
        BlockPos origin = new BlockPos(land.getX() - 1, ground, land.getZ());
        BlockState frame = ModBlocks.PURIFIED_OBSIDIAN.get().defaultBlockState();
        for (int x = -1; x <= 4; x++) {
            for (int z = -1; z <= 4; z++) {
                BlockPos p = origin.offset(x, -1, z);
                level.setBlock(p, frame, Block.UPDATE_CLIENTS);
                for (int y = 0; y <= 5; y++) {
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
        BlockPos portal = origin.offset(1, 1, 0);
        ElysiumPortalShape.findEmptyPortalShape(level, portal, Direction.Axis.X).ifPresent(ElysiumPortalShape::createPortalBlocks);
        data.add(portal);
        return portal;
    }
}
