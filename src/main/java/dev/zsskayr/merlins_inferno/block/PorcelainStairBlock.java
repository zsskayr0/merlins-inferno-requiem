package dev.zsskayr.merlins_inferno.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Stairs of porcelain; see {@link PorcelainBlock}. */
public class PorcelainStairBlock extends StairBlock {
    public PorcelainStairBlock(BlockState base, Properties properties) {
        super(base, properties);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        PorcelainBlock.softFall(level, entity, fallDistance);
    }
}
