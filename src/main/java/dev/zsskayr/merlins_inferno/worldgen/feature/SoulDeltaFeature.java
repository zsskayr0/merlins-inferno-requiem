package dev.zsskayr.merlins_inferno.worldgen.feature;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.registry.ModFluids;
import dev.zsskayr.merlins_inferno.portal.OblivionDimension;

/**
 * Runs last in a chunk of the Oblivion: every lava block and magma block standing in a basalt
 * deltas biome becomes Soul Lava and Soul Magma. Only the chunk it is placed in is touched.
 */
public class SoulDeltaFeature extends Feature<NoneFeatureConfiguration> {
    public SoulDeltaFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        if (level.getLevel().dimension() != OblivionDimension.LEVEL) return false;
        int x0 = context.origin().getX() & ~15, z0 = context.origin().getZ() & ~15;
        int minY = level.getMinBuildHeight(), maxY = level.getMaxBuildHeight();
        BlockState soulLava = ModFluids.SOUL_LAVA_BLOCK.get().defaultBlockState();
        BlockState soulMagma = ModBlocks.SOUL_MAGMA.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean changed = false;
        // Biomes are stored per 4x4x4 cell, so the lookup is done once per cell.
        for (int cx = 0; cx < 16; cx += 4) for (int cz = 0; cz < 16; cz += 4) for (int cy = minY; cy < maxY; cy += 4) {
            Holder<Biome> biome = level.getBiome(pos.set(x0 + cx + 2, cy + 2, z0 + cz + 2));
            if (!biome.is(Biomes.BASALT_DELTAS)) continue;
            for (int dx = 0; dx < 4; dx++) for (int dz = 0; dz < 4; dz++) for (int dy = 0; dy < 4 && cy + dy < maxY; dy++) {
                pos.set(x0 + cx + dx, cy + dy, z0 + cz + dz);
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.LAVA)) {
                    BlockState next = soulLava.setValue(LiquidBlock.LEVEL, state.getValue(LiquidBlock.LEVEL));
                    level.setBlock(pos, next, 2);
                    if (!next.getFluidState().isSource()) {
                        level.scheduleTick(pos, next.getFluidState().getType(), 1);
                    }
                    changed = true;
                } else if (state.is(Blocks.MAGMA_BLOCK)) {
                    level.setBlock(pos, soulMagma, 2);
                    changed = true;
                }
            }
        }
        return changed;
    }
}
