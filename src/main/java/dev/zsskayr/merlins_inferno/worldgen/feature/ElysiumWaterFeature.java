package dev.zsskayr.merlins_inferno.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumTerrain;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumTerrains;

/**
 * Fills El'ysium's basins up to the water line. The islands float in the void, so the generator's own sea cannot be used
 * (it would flood everything below it); the terrain keeps every shore above the water, so each basin is already closed.
 */
public class ElysiumWaterFeature extends Feature<NoneFeatureConfiguration> {
    public ElysiumWaterFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ElysiumTerrain terrain = ElysiumTerrains.forLevel(level.getLevel());
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        BlockState water = Blocks.WATER.defaultBlockState();
        ElysiumTerrain.Column[] cols = terrain.columns(x0 >> 4, z0 >> 4);
        ElysiumTerrain.Column column;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean any = false;
        double[] slash = new double[2];
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                // a trench cut into an island stays dry: no river down the middle of it
                if (terrain.nearestSlash(x0 + dx, z0 + dz, slash) <= 0.0 && !Double.isInfinite(slash[1])) {
                    continue;
                }
                column = cols[dz * 16 + dx];
                // only the wet lowlands hold water: a dry island that rides low is just an island
                double wet = column.weights[ElysiumTerrain.SEA] + column.weights[ElysiumTerrain.WEEPING];
                if (!column.land || column.top >= ElysiumTerrain.WATER_Y || wet < 0.3) {
                    continue;
                }
                for (int y = (int) column.top + 1; y <= ElysiumTerrain.WATER_Y; y++) {
                    pos.set(x0 + dx, y, z0 + dz);
                    if (level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, water, 2);
                        any = true;
                    }
                }
            }
        }
        return any;
    }
}
