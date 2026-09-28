package dev.zsskayr.merlins_inferno.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.NyliumBlock;
import net.minecraft.world.level.block.state.BlockState;

import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/**
 * Black nylium: crimson/warped nylium's counterpart. Everything nylium does in vanilla (turning back into
 * netherrack when covered, taking part in the {@code minecraft:nylium} tag so roots and fungi grow on it) comes
 * from {@link NyliumBlock}; only the bonemeal vegetation is this mod's own.
 */
public class BlackNyliumBlock extends NyliumBlock {
    public BlackNyliumBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        for (int i = 0; i < 32; i++) {
            BlockPos target = pos.offset(random.nextInt(7) - 3, 1, random.nextInt(7) - 3);
            if (!level.getBlockState(target.below()).is(this) || !level.getBlockState(target).isAir()) {
                continue;
            }
            float roll = random.nextFloat();
            if (roll < 0.10F) {
                level.setBlock(target, ModBlocks.LUST_FLOWER.get().defaultBlockState(), Block.UPDATE_ALL);
            } else if (roll < 0.35F) {
                level.setBlock(target, Blocks.CRIMSON_ROOTS.defaultBlockState(), Block.UPDATE_ALL);
            } else if (level.getBlockState(target.above()).isAir()) {
                Block tall = roll < 0.50F ? ModBlocks.TALL_CRIMSON_ROOTS.get() : ModBlocks.TALL_BLACK_GRASS.get();
                DoublePlantBlock.placeAt(level, tall.defaultBlockState(), target, Block.UPDATE_ALL);
            }
        }
    }
}
