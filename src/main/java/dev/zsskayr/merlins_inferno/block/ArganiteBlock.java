package dev.zsskayr.merlins_inferno.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** The natural rock of the angelic realm: hard on the knees, landing on it hurts twice as much. */
public class ArganiteBlock extends Block {
    public static final float FALL_DAMAGE_MULTIPLIER = 2.0F;

    public ArganiteBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        entity.causeFallDamage(fallDistance, FALL_DAMAGE_MULTIPLIER, level.damageSources().fall());
    }
}
