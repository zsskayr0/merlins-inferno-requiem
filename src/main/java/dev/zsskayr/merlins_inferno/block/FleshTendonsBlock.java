package dev.zsskayr.merlins_inferno.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A thin strand of flesh that hangs from a ceiling or rises from the floor (Carnal Heap). Not solid; it breaks when
 * nothing solid holds it from above or below, or from the tendon next to it in its column.
 */
public class FleshTendonsBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);

    public FleshTendonsBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return holds(level, pos, Direction.UP) || holds(level, pos, Direction.DOWN);
    }

    private static boolean holds(LevelReader level, BlockPos pos, Direction dir) {
        BlockPos other = pos.relative(dir);
        BlockState state = level.getBlockState(other);
        return state.is(dev.zsskayr.merlins_inferno.registry.ModBlocks.FLESH_TENDONS.get())
                || state.isFaceSturdy(level, other, dir.getOpposite());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
            net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return canSurvive(state, level, pos) ? state : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }
}
