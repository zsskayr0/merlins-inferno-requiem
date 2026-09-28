package dev.zsskayr.merlins_inferno.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A flower that, unlike vanilla ones, grows on nylium, netherrack, soul soil and Flesh as well as dirt. */
public class NetherFlowerBlock extends FlowerBlock {
    public NetherFlowerBlock(Holder<MobEffect> effect, float effectSeconds, Properties properties) {
        super(effect, effectSeconds, properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return NetherPlants.canSustain(state) || super.mayPlaceOn(state, level, pos);
    }
}
