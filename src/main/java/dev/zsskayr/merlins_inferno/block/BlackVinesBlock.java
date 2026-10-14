package dev.zsskayr.merlins_inferno.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeepingVinesBlock;

import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/** The growing tip of Black Vines: Weeping Vines' behaviour (hangs down, grows with bonemeal) with its own body block. */
public class BlackVinesBlock extends WeepingVinesBlock {
    public BlackVinesBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected Block getBodyBlock() {
        return ModBlocks.BLACK_VINES_PLANT.get();
    }
}
