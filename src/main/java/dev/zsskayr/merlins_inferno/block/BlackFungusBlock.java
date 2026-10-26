package dev.zsskayr.merlins_inferno.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FungusBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/** The black fungus grows under bonemeal on either black nylium: the netherrack one and the Blightstone one. */
public class BlackFungusBlock extends FungusBlock {
    public BlackFungusBlock(ResourceKey<ConfiguredFeature<?, ?>> feature, Block requiredBlock, Properties properties) {
        super(feature, requiredBlock, properties);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        Block below = level.getBlockState(pos.below()).getBlock();
        return below == ModBlocks.BLACK_NYLIUM.get() || below == ModBlocks.BLIGHTSTONE_NYLIUM.get();
    }
}
