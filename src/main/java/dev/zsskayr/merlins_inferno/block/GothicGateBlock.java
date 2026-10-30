package dev.zsskayr.merlins_inferno.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A door whose closed leaf stands in the middle of the block, so it lines up with Gothic Iron Bars on either side. */
public class GothicGateBlock extends DoorBlock {
    private static final VoxelShape CLOSED_X = Block.box(6.5, 0, 0, 9.5, 16, 16);
    private static final VoxelShape CLOSED_Z = Block.box(0, 0, 6.5, 16, 16, 9.5);

    public GothicGateBlock(BlockSetType type, Properties properties) {
        super(type, properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(OPEN)) {
            return super.getShape(state, level, pos, context);
        }
        Direction facing = state.getValue(FACING);
        return facing.getAxis() == Direction.Axis.X ? CLOSED_X : CLOSED_Z;
    }
}
