package dev.zsskayr.merlins_inferno.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeepingVinesPlantBlock;

import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/** The body of Black Vines: Weeping Vines' plant block, growing from {@link BlackVinesBlock}. */
public class BlackVinesPlantBlock extends WeepingVinesPlantBlock {
    public BlackVinesPlantBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected net.minecraft.world.level.block.GrowingPlantHeadBlock getHeadBlock() {
        return (net.minecraft.world.level.block.GrowingPlantHeadBlock) ModBlocks.BLACK_VINES.get();
    }
}
