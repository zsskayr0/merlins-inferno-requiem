package dev.zsskayr.merlins_inferno.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import dev.zsskayr.merlins_inferno.block.LyriumClusterBlock;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumTerrain;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumTerrains;

/**
 * El'ysium's water table: under the larger islands the rock is hollowed into one flooded cavern after another, a few
 * blocks of air over a lake that can run for hundreds of blocks, with pillars left standing, dripstone on the roof and
 * quartz or Lyrium crystals glowing from it. Each column decides from its own coordinates, so the lakes meet exactly
 * across chunk borders.
 */
public class ElysiumGroundwaterFeature extends Feature<NoneFeatureConfiguration> {
    public ElysiumGroundwaterFeature() {
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

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ElysiumTerrain terrain = ElysiumTerrains.forLevel(level.getLevel());
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        ElysiumTerrain.Column[] cols = terrain.columns(x0 >> 4, z0 >> 4);
        ElysiumTerrain.Column col;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState gravel = Blocks.GRAVEL.defaultBlockState();
        BlockState clay = Blocks.CLAY.defaultBlockState();
        boolean any = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                col = cols[dz * 16 + dx];
                if (!col.land) {
                    continue;
                }
                ElysiumTerrain.Aquifer a = terrain.aquifer(x, z, col.top, col.bottom, col.inland);
                if (a == null) {
                    continue;
                }
                for (int y = a.floor(); y <= a.ceil(); y++) {
                    level.setBlock(pos.set(x, y, z), y <= ElysiumTerrain.WATER_TABLE ? water : air, 2);
                }
                // the lake bed, and what hangs from the roof and grows from it
                level.setBlock(pos.set(x, a.floor() - 1, z), rand(x, 0, z, 61) < 0.5 ? gravel : clay, 2);
                double roll = rand(x, 1, z, 62);
                BlockPos.MutableBlockPos roof = pos.set(x, a.ceil(), z);
                if (roll < 0.035) {
                    level.setBlock(roof, ModBlocks.QUARTZ_CLUSTER.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.DOWN), 2);
                } else if (roll < 0.06) {
                    level.setBlock(roof, ModBlocks.LYRIUM_CLUSTER.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.DOWN)
                            .setValue(LyriumClusterBlock.PLACED, false), 2);
                } else if (roll < 0.14) {
                    int len = 1 + (int) (rand(x, 2, z, 63) * 3);
                    for (int i = 0; i < len; i++) {
                        DripstoneThickness t = i == len - 1 ? DripstoneThickness.TIP : i == len - 2 ? DripstoneThickness.FRUSTUM : DripstoneThickness.MIDDLE;
                        level.setBlock(pos.set(x, a.ceil() - i, z), Blocks.POINTED_DRIPSTONE.defaultBlockState()
                                .setValue(BlockStateProperties.VERTICAL_DIRECTION, Direction.DOWN).setValue(BlockStateProperties.DRIPSTONE_THICKNESS, t), 2);
                    }
                }
                any = true;
            }
        }
        return any;
    }
}
