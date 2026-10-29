package dev.zsskayr.merlins_inferno.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Tiled Porcelain reads as big 2x2 tiles, and where a tile starts is up to the builder, not the world's coordinates.
 * On each axis a block is either the first or the second half of its pair ({@code second_x/y/z}); a block set in the
 * world looks at its neighbours on that axis and completes a lone block next to it, so a row begun anywhere pairs up
 * from there on, however it was set (a player, a command, a paste that runs block updates). The model only joins two
 * blocks that are halves of the same pair on every axis they differ on (and share the other flags), see
 * {@code ConnectedBlockModel}. World generation and tools that write chunks directly skip this, leaving first halves.
 */
public class TiledPorcelainBlock extends PorcelainBlock {
    public static final BooleanProperty SECOND_X = BooleanProperty.create("second_x");
    public static final BooleanProperty SECOND_Y = BooleanProperty.create("second_y");
    public static final BooleanProperty SECOND_Z = BooleanProperty.create("second_z");
    private static final Direction.Axis[] AXES = Direction.Axis.values();

    public TiledPorcelainBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(SECOND_X, false).setValue(SECOND_Y, false).setValue(SECOND_Z, false));
    }

    public static BooleanProperty second(Direction.Axis axis) {
        return switch (axis) {
            case X -> SECOND_X;
            case Y -> SECOND_Y;
            case Z -> SECOND_Z;
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SECOND_X, SECOND_Y, SECOND_Z);
    }

    /**
     * Pairs the block up whenever it is set in the world, by whatever means: a player, a command, a paste. A lone neighbour
     * that was a second half where it should be a first (or the other way round) is flipped to pair with this block.
     */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide || movedByPiston || oldState.is(this)) {
            return;
        }
        BlockState paired = state;
        for (Direction.Axis axis : AXES) {
            paired = paired.setValue(second(axis), chooseSecond(level, pos, axis));
        }
        if (paired != state) {
            level.setBlock(pos, paired, Block.UPDATE_ALL);
        }
        for (Direction.Axis axis : AXES) {
            BooleanProperty property = second(axis);
            Direction toPartner = Direction.fromAxisAndDirection(axis, paired.getValue(property) ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
            BlockPos partnerPos = pos.relative(toPartner);
            BlockState partner = level.getBlockState(partnerPos);
            if (partner.is(this) && partner.getValue(property) == paired.getValue(property)) {
                level.setBlock(partnerPos, partner.setValue(property, !paired.getValue(property)), Block.UPDATE_ALL);
            }
        }
    }

    private boolean chooseSecond(Level level, BlockPos pos, Direction.Axis axis) {
        BooleanProperty property = second(axis);
        BlockPos before = pos.relative(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE));
        BlockPos after = pos.relative(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE));
        BlockState low = level.getBlockState(before);
        BlockState high = level.getBlockState(after);
        if (low.is(this) && isLone(level, before, low, property, axis, pos)) {
            return true;  // finish the lone block behind: it becomes the first half, this the second
        }
        if (high.is(this) && isLone(level, after, high, property, axis, pos)) {
            return false; // finish the lone block ahead
        }
        if (low.is(this)) {
            return false; // the block behind already has its partner, so this one starts the next pair
        }
        if (high.is(this)) {
            return true;  // the block ahead has its partner; stand alone as a second half
        }
        // nothing on this axis: line up with a tiled block beside, above or below, so a new row follows the one next to it
        for (Direction side : Direction.values()) {
            if (side.getAxis() != axis) {
                BlockState neighbour = level.getBlockState(pos.relative(side));
                if (neighbour.is(this)) {
                    return neighbour.getValue(property);
                }
            }
        }
        return false;
    }

    /** A block whose partner position on the axis holds no block that is its other half; {@code placing}, the block being set, does not count. */
    private boolean isLone(Level level, BlockPos pos, BlockState state, BooleanProperty property, Direction.Axis axis, BlockPos placing) {
        Direction toPartner = Direction.fromAxisAndDirection(axis, state.getValue(property) ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
        BlockPos partnerPos = pos.relative(toPartner);
        BlockState partner = level.getBlockState(partnerPos);
        return partnerPos.equals(placing) || !(partner.is(this) && partner.getValue(property) != state.getValue(property));
    }
}
