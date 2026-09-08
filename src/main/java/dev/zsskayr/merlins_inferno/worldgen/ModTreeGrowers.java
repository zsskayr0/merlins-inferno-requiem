package dev.zsskayr.merlins_inferno.worldgen;

import java.util.Optional;

import net.minecraft.world.level.block.grower.TreeGrower;

/**
 * {@link TreeGrower}s for the saplings this mod adds - what a sapling actually grows into on
 * bonemeal/random tick, referencing the {@code ConfiguredFeature} keys in
 * {@link ModConfiguredFeatures}. {@code TreeGrower}'s constructor self-registers into its own
 * internal by-name map (same as vanilla's {@code TreeGrower.OAK} etc.), so just constructing
 * these is enough.
 */
public final class ModTreeGrowers {
    public static final TreeGrower ASHWOOD = new TreeGrower("ashwood", Optional.empty(), Optional.of(ModConfiguredFeatures.ASHWOOD_TREE), Optional.empty());
    public static final TreeGrower ROWANWOOD = new TreeGrower("rowanwood", Optional.empty(), Optional.of(ModConfiguredFeatures.ROWANWOOD_TREE), Optional.empty());

    private ModTreeGrowers() {
    }
}
