package dev.zsskayr.merlins_inferno.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;

import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/**
 * Black nylium grown over Blightstone instead of netherrack: the same plants and bonemeal, but when it is covered it
 * turns back into Blightstone (vanilla's {@link net.minecraft.world.level.block.NyliumBlock} always falls back to
 * netherrack).
 */
public class BlightstoneNyliumBlock extends BlackNyliumBlock {
    public BlightstoneNyliumBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canBeNylium(state, level, pos)) {
            level.setBlockAndUpdate(pos, ModBlocks.BLIGHTSTONE.get().defaultBlockState());
        }
    }

    /** Vanilla's rule: nylium survives while enough light can reach its top face. */
    private static boolean canBeNylium(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        int light = LightEngine.getLightBlockInto(level, state, pos, aboveState, above, Direction.UP,
                aboveState.getLightBlock(level, above));
        return light < level.getMaxLightLevel();
    }
}
