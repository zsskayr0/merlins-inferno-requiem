package dev.zsskayr.merlins_inferno.block;

import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Iron bars with spear tips: only the topmost bar of a column carries the points, so stacked bars read as one tall railing. */
public class GothicIronBarsBlock extends IronBarsBlock {
    public static final BooleanProperty CAPPED = BooleanProperty.create("capped");

    public GothicIronBarsBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(CAPPED, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CAPPED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        BlockState placed = state.setValue(CAPPED, !isBars(context.getLevel().getBlockState(context.getClickedPos().above())));
        for (Direction side : Direction.Plane.HORIZONTAL) {
            placed = joinGate(placed, side, context.getLevel().getBlockState(context.getClickedPos().relative(side)));
        }
        return placed;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighbor, level, pos, neighborPos);
        if (direction == Direction.UP) {
            return updated.setValue(CAPPED, !isBars(neighbor));
        }
        return direction.getAxis().isHorizontal() ? joinGate(updated, direction, neighbor) : updated;
    }

    /** The bars reach out to a Gothic Gate (or Great Gothic Gate) whose leaf runs along them. */
    private static BlockState joinGate(BlockState state, Direction side, BlockState neighbor) {
        boolean gate = neighbor.getBlock() instanceof GothicGateBlock || neighbor.is(ModBlocks.GOTHIC_GREAT_GATE.get());
        if (gate && neighbor.getValue(GothicGateBlock.FACING).getAxis() != side.getAxis()) {
            return state.setValue(PROPERTY_BY_DIRECTION.get(side), true);
        }
        return state;
    }

    private static boolean isBars(BlockState state) {
        return state.getBlock() instanceof GothicIronBarsBlock;
    }
}
