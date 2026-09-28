package dev.zsskayr.merlins_inferno.block;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/** Where this mod's Nether plants (and the vanilla Wither Rose, see the mixin) may stand. */
public final class NetherPlants {
    private NetherPlants() {
    }

    /** Everything crimson roots can stand on (nylium, soul soil), plus netherrack and Flesh. */
    public static boolean canSustain(BlockState state) {
        return state.is(BlockTags.NYLIUM) || state.is(Blocks.NETHERRACK) || state.is(Blocks.SOUL_SOIL)
                || state.is(ModBlocks.FLESH_BLOCK.get());
    }

    /** Wither Rose extension: black nylium and Flesh only, vanilla nylium stays as it was. */
    public static boolean canSustainWitherRose(BlockState state) {
        return state.is(ModBlocks.BLACK_NYLIUM.get()) || state.is(ModBlocks.FLESH_BLOCK.get());
    }
}
