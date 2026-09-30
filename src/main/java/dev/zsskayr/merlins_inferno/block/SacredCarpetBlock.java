package dev.zsskayr.merlins_inferno.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Connected cloth: blue binding and loose threads appear only at exposed edges. */
public class SacredCarpetBlock extends CarpetBlock {
    public static final MapCodec<SacredCarpetBlock> CODEC = simpleCodec(SacredCarpetBlock::new);

    public SacredCarpetBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(BlockStateProperties.NORTH, false).setValue(BlockStateProperties.EAST, false)
                .setValue(BlockStateProperties.SOUTH, false).setValue(BlockStateProperties.WEST, false));
    }

    @Override
    public MapCodec<SacredCarpetBlock> codec() { return CODEC; }

    private static BooleanProperty connection(Direction direction) {
        return switch (direction) {
            case NORTH -> BlockStateProperties.NORTH;
            case EAST -> BlockStateProperties.EAST;
            case SOUTH -> BlockStateProperties.SOUTH;
            case WEST -> BlockStateProperties.WEST;
            default -> throw new IllegalArgumentException("Horizontal direction required");
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.NORTH, BlockStateProperties.EAST,
                BlockStateProperties.SOUTH, BlockStateProperties.WEST);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            state = state.setValue(connection(direction),
                    context.getLevel().getBlockState(context.getClickedPos().relative(direction)).is(this));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighbor, level, pos, neighborPos);
        return updated.is(this) && direction.getAxis().isHorizontal()
                ? updated.setValue(connection(direction), neighbor.is(this)) : updated;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState result = state;
        for (Direction direction : Direction.Plane.HORIZONTAL)
            result = result.setValue(connection(rotation.rotate(direction)), state.getValue(connection(direction)));
        return result;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = state;
        for (Direction direction : Direction.Plane.HORIZONTAL)
            result = result.setValue(connection(mirror.mirror(direction)), state.getValue(connection(direction)));
        return result;
    }
}
