package dev.zsskayr.merlins_inferno.worldgen.elysium;

import java.util.List;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.Fluids;
import org.slf4j.Logger;

import dev.zsskayr.merlins_inferno.worldgen.elysium.core.Column;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumWorld;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.Mat;
import dev.zsskayr.merlins_inferno.worldgen.elysium.structure.SiteBuilder;
import dev.zsskayr.merlins_inferno.worldgen.elysium.structure.SpanBuilder;

/**
 * What the chunk generator adds after vanilla biome decoration (the FEATURES step, so it may write into the 3x3 chunks
 * around this one): built spans (bridges, chains), loose hanging chains, structure sites, springs, and a few glow lichens under the islands
 * so the Underhang is dark but not black. Nothing here goes into biome JSON (see the feature-order rule).
 */
public final class ElysiumDecorator {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Columns per chunk, on average, that get a glow lichen under the island (where the core hung nothing). */
    private static final double LICHEN_CHANCE = 6.0 / 256.0;

    private static volatile boolean spanFailureLogged, siteFailureLogged;

    private ElysiumDecorator() {
    }

    public static void decorate(WorldGenLevel level, ChunkAccess chunk, ElysiumWorld world) {
        ChunkPos chunkPos = chunk.getPos();
        int minX = chunkPos.getMinBlockX(), minZ = chunkPos.getMinBlockZ();
        int maxX = chunkPos.getMaxBlockX(), maxZ = chunkPos.getMaxBlockZ();

        List<ElysiumWorld.Span> spans = world.spansIn(minX, minZ, maxX, maxZ);
        if (!spans.isEmpty()) {
            try {
                SpanBuilder.build(level, chunkPos, world, spans);
            } catch (RuntimeException e) {
                if (!spanFailureLogged) {
                    spanFailureLogged = true;
                    LOGGER.error("El'ysium span builder failed in chunk {} (further failures are not logged)", chunkPos, e);
                }
            }
        }

        try {
            SpanBuilder.buildHanging(level, chunkPos, world.hangingChainsIn(minX, minZ, maxX, maxZ));
        } catch (RuntimeException e) {
            if (!spanFailureLogged) {
                spanFailureLogged = true;
                LOGGER.error("El'ysium hanging chains failed in chunk {} (further failures are not logged)", chunkPos, e);
            }
        }

        for (ElysiumWorld.Site site : world.sitesIn(minX, minZ, maxX, maxZ)) {
            try {
                SiteBuilder.build(level, chunkPos, world, site);
            } catch (RuntimeException e) {
                if (!siteFailureLogged) {
                    siteFailureLogged = true;
                    LOGGER.error("El'ysium site builder failed for {} in chunk {} (further failures are not logged)", site, chunkPos, e);
                }
            }
        }

        placeSprings(level, world, minX, minZ, maxX, maxZ);
        placeLichen(level, world, minX, minZ);
    }

    /** Each spring is a water source the game ticks right away, so the fall forms as soon as the chunk is live. */
    private static void placeSprings(WorldGenLevel level, ElysiumWorld world, int minX, int minZ, int maxX, int maxZ) {
        BlockState water = ElysiumBlocks.water();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (ElysiumWorld.Spring spring : world.springsIn(minX, minZ, maxX, maxZ)) {
            pos.set(spring.x(), spring.y(), spring.z());
            if (level.isOutsideBuildHeight(pos) || !level.ensureCanWrite(pos)) {
                continue;
            }
            level.setBlock(pos, water, Block.UPDATE_CLIENTS);
            level.scheduleTick(pos.immutable(), Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
    }

    /** A sparse scatter of glow lichen on the underside rock, where the core hung no roots or vines. */
    private static void placeLichen(WorldGenLevel level, ElysiumWorld world, int minX, int minZ) {
        BlockState lichen = ElysiumBlocks.state(Mat.GLOW_LICHEN_CEIL);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Column c = null;
        for (int dz = 0; dz < 16; dz++) {
            for (int dx = 0; dx < 16; dx++) {
                int x = minX + dx, z = minZ + dz;
                if (ElysiumChunkGenerator.hash01(world.seed ^ 0x51C4E2L, x, 0, z) >= LICHEN_CHANCE) {
                    continue;
                }
                if (c == null) {
                    c = new Column();
                }
                world.sample(x, z, c);
                if (!c.land || c.hangLength > 0) {
                    continue;
                }
                pos.set(x, c.bottom - 1, z);
                if (level.isOutsideBuildHeight(pos) || !level.getBlockState(pos).isAir()) {
                    continue;
                }
                BlockPos above = pos.above();
                if (!level.getBlockState(above).isFaceSturdy(level, above, Direction.DOWN)) {
                    continue;
                }
                level.setBlock(pos, lichen, Block.UPDATE_CLIENTS);
            }
        }
    }
}
