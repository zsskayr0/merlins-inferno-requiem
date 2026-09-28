package dev.zsskayr.merlins_inferno.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A two-block-high plant (tall crimson roots, tall black grass) that stands on the same ground as crimson roots. */
public class NetherDoublePlantBlock extends DoublePlantBlock {
    public NetherDoublePlantBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return NetherPlants.canSustain(state) || super.mayPlaceOn(state, level, pos);
    }
}
