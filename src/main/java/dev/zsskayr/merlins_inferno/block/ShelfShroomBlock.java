package dev.zsskayr.merlins_inferno.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A bracket fungus that clings to the side of a trunk. {@code FACING} is the way it grows out from the wall. */
public class ShelfShroomBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ShelfShroomBlock> CODEC = simpleCodec(ShelfShroomBlock::new);
    private static final VoxelShape[] SHAPES = new VoxelShape[4];

    static {
        SHAPES[Direction.NORTH.get2DDataValue()] = Block.box(1.0, 1.0, 8.0, 15.0, 12.0, 16.0);
        SHAPES[Direction.SOUTH.get2DDataValue()] = Block.box(1.0, 1.0, 0.0, 15.0, 12.0, 8.0);
        SHAPES[Direction.EAST.get2DDataValue()] = Block.box(0.0, 1.0, 1.0, 8.0, 12.0, 15.0);
        SHAPES[Direction.WEST.get2DDataValue()] = Block.box(8.0, 1.0, 1.0, 16.0, 12.0, 15.0);
    }

    public ShelfShroomBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get2DDataValue()];
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos wall = pos.relative(state.getValue(FACING).getOpposite());
        return level.getBlockState(wall).isFaceSturdy(level, wall, state.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
            BlockPos pos, BlockPos neighborPos) {
        return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction dir : context.getNearestLookingDirections()) {
            if (dir.getAxis().isHorizontal()) {
                BlockState state = defaultBlockState().setValue(FACING, dir.getOpposite());
                if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                    return state;
                }
            }
        }
        return null;
    }
}
