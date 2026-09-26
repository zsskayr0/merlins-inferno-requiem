package dev.zsskayr.merlins_inferno.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import dev.zsskayr.merlins_inferno.registry.ModParticles;

/**
 * The geode's lining: blazing bright, and - when it is the world's own - sanctifying to undead
 * and demons nearby ({@link LyriumRadiance}).
 * <p>
 * {@link #PLACED} is what makes "the world's own" checkable: worldgen sets the default state
 * ({@code placed=false}), while anything placed through an item (a player, a dispenser, a
 * Silk-Touched block put back down) goes through {@link #getStateForPlacement} and is flagged
 * {@code placed=true}. A placed block still glows but neither radiates nor random-ticks at all, so
 * a player can't fence a base with the geode's curse. (Pistons can't carry a natural one off
 * either: the block is immovable, see {@code ModBlocks}.)
 */
public class LyriumBlock extends Block {
    public static final BooleanProperty PLACED = BooleanProperty.create("placed");

    public LyriumBlock(Properties properties) {
        super(properties);
        // BooleanProperty's default would be its first value (true) - natural is the default here.
        this.registerDefaultState(this.stateDefinition.any().setValue(PLACED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PLACED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(PLACED, true);
    }

    /** Placed blocks never enter the random-tick list at all. */
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
        if (!state.getValue(PLACED) && random.nextInt(24) == 0) {
            level.addParticle(ModParticles.SANCTIFIED_TYPE, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
        }
    }
}
