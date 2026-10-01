package dev.zsskayr.merlins_inferno.block;

import com.mojang.serialization.MapCodec;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Connected cloth: blue binding and loose threads appear only at exposed edges. */
public class OrnatedCarpetBlock extends CarpetBlock {
    public static final MapCodec<OrnatedCarpetBlock> CODEC = simpleCodec(OrnatedCarpetBlock::new);

    private static final BooleanProperty[] DIAGONALS = {
        BooleanProperty.create("north_west"), BooleanProperty.create("north_east"),
        BooleanProperty.create("south_east"), BooleanProperty.create("south_west")
    };
    private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private static BooleanProperty diagonal(Direction a, Direction b) {
        int x = a.getStepX() + b.getStepX(), z = a.getStepZ() + b.getStepZ();
        return DIAGONALS[z < 0 ? (x < 0 ? 0 : 1) : (x > 0 ? 2 : 3)];
    }

    /** Shared by placement, updates and structures, including their not-yet-generated chunks. */
    public BlockState connectedState(BlockState state, BlockPos pos, Predicate<BlockPos> hasCarpet) {
        for (int i = 0; i < 4; i++) {
            Direction a = SIDES[i], b = SIDES[(i + 3) % 4];
            state = state.setValue(connection(a), joins(hasCarpet, pos.relative(a)))
                    .setValue(DIAGONALS[i], joins(hasCarpet, pos.relative(a).relative(b)));
        }
        return state;
    }

    private static boolean joins(Predicate<BlockPos> hasCarpet, BlockPos pos) {
        return hasCarpet.test(pos) || hasCarpet.test(pos.above()) || hasCarpet.test(pos.below());
    }

    private BlockState connections(BlockState state, BlockGetter level, BlockPos pos) {
        return connectedState(state, pos, p -> level.getBlockState(p).is(this));
    }

    // Direct neighbor updates do not reach the diagonally adjacent tile.
    private void notifyDiagonals(Level level, BlockPos pos) {
        for (int i = 0; i < 4; i++)
            level.neighborChanged(pos.relative(SIDES[i]).relative(SIDES[(i + 3) % 4]), this, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!oldState.is(this)) notifyDiagonals(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        super.onRemove(state, level, pos, newState, moving);
        if (!newState.is(this)) notifyDiagonals(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, level, pos, block, fromPos, moving);
        if (level.getBlockState(pos).is(this)) {
            BlockState updated = connections(state, level, pos);
            if (updated != state) level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        }
    }

    public OrnatedCarpetBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(BlockStateProperties.NORTH, false).setValue(BlockStateProperties.EAST, false)
                .setValue(BlockStateProperties.SOUTH, false).setValue(BlockStateProperties.WEST, false)
                .setValue(DIAGONALS[0], false).setValue(DIAGONALS[1], false)
                .setValue(DIAGONALS[2], false).setValue(DIAGONALS[3], false));
    }

    @Override
    public MapCodec<OrnatedCarpetBlock> codec() { return CODEC; }

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
        builder.add(DIAGONALS);
        builder.add(BlockStateProperties.NORTH, BlockStateProperties.EAST,
                BlockStateProperties.SOUTH, BlockStateProperties.WEST);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighbor, level, pos, neighborPos);
        return updated.is(this) && direction.getAxis().isHorizontal()
                ? connectedState(updated, pos, p -> p.equals(neighborPos) ? neighbor.is(this)
                        : level.getBlockState(p).is(this)) : updated;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState result = state;
        for (Direction direction : Direction.Plane.HORIZONTAL)
            result = result.setValue(connection(rotation.rotate(direction)), state.getValue(connection(direction)));
        for (int i = 0; i < 4; i++)
            result = result.setValue(diagonal(rotation.rotate(SIDES[i]), rotation.rotate(SIDES[(i + 3) % 4])), state.getValue(DIAGONALS[i]));
        return result;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = state;
        for (Direction direction : Direction.Plane.HORIZONTAL)
            result = result.setValue(connection(mirror.mirror(direction)), state.getValue(connection(direction)));
        for (int i = 0; i < 4; i++)
            result = result.setValue(diagonal(mirror.mirror(SIDES[i]), mirror.mirror(SIDES[(i + 3) % 4])), state.getValue(DIAGONALS[i]));
        return result;
    }
}
