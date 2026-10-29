package dev.zsskayr.merlins_inferno.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Porcelain floors are forgiving: landing on any porcelain block, stair or slab only does a fifth of the fall damage. */
public class PorcelainBlock extends Block {
    public PorcelainBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        PorcelainBlock.softFall(level, entity, fallDistance);
    }

    static void softFall(Level level, Entity entity, float fallDistance) {
        entity.causeFallDamage(fallDistance, 0.2F, level.damageSources().fall());
    }
}
