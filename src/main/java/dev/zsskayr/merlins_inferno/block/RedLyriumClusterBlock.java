package dev.zsskayr.merlins_inferno.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class RedLyriumClusterBlock extends AmethystClusterBlock {
    public RedLyriumClusterBlock(float height, float offset, Properties properties) {
        super(height, offset, properties);
        registerDefaultState(defaultBlockState().setValue(LyriumBlock.PLACED, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LyriumBlock.PLACED);
    }

    @Nullable @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(LyriumBlock.PLACED, true);
    }

    @Override protected boolean isRandomlyTicking(BlockState state) {
        return !state.getValue(LyriumBlock.PLACED);
    }

    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LyriumBlock.PLACED)) RedLyriumRadiance.radiate(level, pos);
    }
}
