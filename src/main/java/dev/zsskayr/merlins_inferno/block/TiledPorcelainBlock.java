package dev.zsskayr.merlins_inferno.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Tiled Porcelain reads as big 2x2 tiles, and where a tile starts is up to the builder, not the world's coordinates.
 * On each axis a block is either the first or the second half of its pair ({@code second_x/y/z}); a placed block looks
 * at its neighbours on that axis and completes a lone block next to it, so a row begun anywhere pairs up from there on.
 * The model only joins two blocks that are halves of the same pair on every axis they differ on (and share the other
 * flags), see {@code ConnectedBlockModel}. Blocks placed without a player (commands, structures) are all first halves.
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

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState();
        for (Direction.Axis axis : AXES) {
            state = state.setValue(second(axis), chooseSecond(context.getLevel(), context.getClickedPos(), axis));
        }
        return state;
    }

    /** After placing, a lone neighbour that was a second half where it should be a first (or the other way round) is flipped to pair with this block. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        for (Direction.Axis axis : AXES) {
            BooleanProperty property = second(axis);
            Direction toPartner = Direction.fromAxisAndDirection(axis, state.getValue(property) ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
            BlockPos partnerPos = pos.relative(toPartner);
            BlockState partner = level.getBlockState(partnerPos);
            if (partner.is(this) && partner.getValue(property) == state.getValue(property)) {
                level.setBlock(partnerPos, partner.setValue(property, !state.getValue(property)), Block.UPDATE_ALL);
            }
        }
    }

    private boolean chooseSecond(Level level, BlockPos pos, Direction.Axis axis) {
        BooleanProperty property = second(axis);
        BlockPos before = pos.relative(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE));
        BlockPos after = pos.relative(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE));
        BlockState low = level.getBlockState(before);
        BlockState high = level.getBlockState(after);
        if (low.is(this) && isLone(level, before, low, property, axis)) {
            return true;  // finish the lone block behind: it becomes the first half, this the second
        }
        if (high.is(this) && isLone(level, after, high, property, axis)) {
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

    /** A block whose partner position on the axis holds no block that is its other half. */
    private boolean isLone(Level level, BlockPos pos, BlockState state, BooleanProperty property, Direction.Axis axis) {
        Direction toPartner = Direction.fromAxisAndDirection(axis, state.getValue(property) ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
        BlockState partner = level.getBlockState(pos.relative(toPartner));
        return !(partner.is(this) && partner.getValue(property) != state.getValue(property));
    }
}
