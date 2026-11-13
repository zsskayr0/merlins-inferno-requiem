package dev.zsskayr.merlins_inferno.worldgen.feature;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
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

    /** Blended share of soul biome a column needs to turn. */
    private static final float SHARE = 0.3F;

    private static boolean soulBiome(net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> key) {
        return key == Biomes.BASALT_DELTAS || key == Biomes.SOUL_SAND_VALLEY;
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
        // The Oblivion's biomes are 3D and come in 4x4 cells, so a per-cell choice stripes the cliffs and cuts lakes in
        // stair steps. Each 4x4 column gets the share of its height that is a soul biome, those shares are blended
        // between neighbouring columns (into the next chunks too, so edges meet), and a block column turns to Soul Lava
        // when the blend reaches a half. That gives soft, curved shores that line up across chunks.
        int qx0 = (x0 >> 2) - 1, qz0 = (z0 >> 2) - 1;
        float[][] share = new float[6][6];
        for (int i = 0; i < 6; i++) for (int j = 0; j < 6; j++) {
            int cells = 0, soul = 0;
            for (int y = minY + 2; y < maxY; y += 8) {
                cells++;
                if (level.getBiome(pos.set(((qx0 + i) << 2) + 2, y, ((qz0 + j) << 2) + 2)).is(SoulDeltaFeature::soulBiome)) soul++;
            }
            share[i][j] = cells == 0 ? 0 : (float) soul / cells;
        }
        for (int bx = 0; bx < 16; bx++) for (int bz = 0; bz < 16; bz++) {
            // Quart centres sit at q*4+2; find the four around this block column and blend.
            float fx = (x0 + bx - 2) / 4F - qx0, fz = (z0 + bz - 2) / 4F - qz0;
            int i = (int) Math.floor(fx), j = (int) Math.floor(fz);
            float tx = fx - i, tz = fz - j;
            float v = (share[i][j] * (1 - tx) + share[i + 1][j] * tx) * (1 - tz) + (share[i][j + 1] * (1 - tx) + share[i + 1][j + 1] * tx) * tz;
            if (v < SHARE) continue;
            for (int y = minY; y < maxY; y++) {
                pos.set(x0 + bx, y, z0 + bz);
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.LAVA)) {
                    BlockState next = soulLava.setValue(LiquidBlock.LEVEL, state.getValue(LiquidBlock.LEVEL));
                    level.setBlock(pos, next, 2);
                    if (!next.getFluidState().isSource()) level.scheduleTick(pos, next.getFluidState().getType(), 1);
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
