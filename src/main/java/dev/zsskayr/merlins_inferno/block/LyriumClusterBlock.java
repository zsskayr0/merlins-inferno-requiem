package dev.zsskayr.merlins_inferno.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import dev.zsskayr.merlins_inferno.registry.ModParticles;

/**
 * The geode's crystals. Same shape and drops behaviour as an amethyst cluster, but blazing bright
 * and - only while natural - sanctifying to undead and demons nearby ({@link LyriumRadiance}).
 * See {@link LyriumBlock} for the {@code placed} flag. Unlike amethyst there is no budding block:
 * clusters never regrow, so a geode is a finite deposit.
 */
public class LyriumClusterBlock extends AmethystClusterBlock {
    public static final BooleanProperty PLACED = LyriumBlock.PLACED;

    public LyriumClusterBlock(float height, float aabbOffset, Properties properties) {
        super(height, aabbOffset, properties);
        this.registerDefaultState(this.defaultBlockState().setValue(PLACED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PLACED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(PLACED, true);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !state.getValue(PLACED);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(PLACED)) {
            LyriumRadiance.radiate(level, pos);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(PLACED) && random.nextInt(16) == 0) {
            level.addParticle(ModParticles.SANCTIFIED_TYPE, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
        }
    }
}
