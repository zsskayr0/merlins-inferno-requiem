package dev.zsskayr.merlins_inferno.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A slab of porcelain; see {@link PorcelainBlock}. */
public class PorcelainSlabBlock extends SlabBlock {
    public PorcelainSlabBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        PorcelainBlock.softFall(level, entity, fallDistance);
    }
}
