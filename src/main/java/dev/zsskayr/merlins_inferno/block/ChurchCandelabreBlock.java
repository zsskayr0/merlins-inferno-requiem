package dev.zsskayr.merlins_inferno.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Church candelabre: bracketed to a wall (FACING points away from the wall) or hung by a chain from the ceiling. */
public class ChurchCandelabreBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty HANGING = BlockStateProperties.HANGING;

    private static final VoxelShape HANGING_SHAPE = Block.box(5, 0, 5, 11, 16, 11);
    private static final VoxelShape WALL_NORTH = Block.box(5, 2, 3, 11, 16, 16);
    private static final VoxelShape WALL_SOUTH = Block.box(5, 2, 0, 11, 16, 13);
    private static final VoxelShape WALL_WEST = Block.box(3, 2, 5, 16, 16, 11);
    private static final VoxelShape WALL_EAST = Block.box(0, 2, 5, 13, 16, 11);

    public ChurchCandelabreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HANGING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HANGING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction dir : context.getNearestLookingDirections()) {
            BlockState state;
            if (dir == Direction.UP) {
                state = defaultBlockState().setValue(HANGING, true);
            } else if (dir.getAxis().isHorizontal()) {
                state = defaultBlockState().setValue(HANGING, false).setValue(FACING, dir.getOpposite());
            } else {
                continue;
            }
            if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                return state;
            }
        }
        return null;
    }

    private static Direction attachedDirection(BlockState state) {
        return state.getValue(HANGING) ? Direction.UP : state.getValue(FACING).getOpposite();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction dir = attachedDirection(state);
        BlockPos support = pos.relative(dir);
        return level.getBlockState(support).isFaceSturdy(level, support, dir.getOpposite());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == attachedDirection(state) && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(HANGING)) {
            return HANGING_SHAPE;
        }
        return switch (state.getValue(FACING)) {
            case SOUTH -> WALL_SOUTH;
            case WEST -> WALL_WEST;
            case EAST -> WALL_EAST;
            default -> WALL_NORTH;
        };
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double y = pos.getY() + (state.getValue(HANGING) ? 0.4 : 0.55);
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        if (!state.getValue(HANGING)) {
            Direction f = state.getValue(FACING);
            x += f.getStepX() * 0.2;
            z += f.getStepZ() * 0.2;
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
