package dev.zsskayr.merlins_inferno.datagen.worldgen;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.SpruceFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter;

import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.worldgen.ModConfiguredFeatures;
import dev.zsskayr.merlins_inferno.worldgen.ModPlacedFeatures;

/**
 * Tree shapes and their natural-generation placement for the Hallowed Grove.
 * <p>
 * Both trees use the Taiga/Spruce shape family (tall trunk, tapering conical foliage) rather than
 * a round oak-style blob - Ashwood at a smaller, "normal tree" scale, Rowanwood scaled up further
 * so it reads as "physically bigger", a landmark and not just another tree. Conical canopies also
 * overlap far less than round ones at the same spacing, which is why Ashwood switched to this
 * shape too: the earlier blob canopy at forest-like density produced solid, touching treetops
 * with no gaps (looked like one continuous mass, not a walkable grove) - the fix was both a
 * smaller/narrower canopy shape and a lower placement density (see {@link #bootstrapPlacedFeatures}).
 * <p>
 * <b>Rowanwood placement is an approximation:</b> the design doc calls for "50% chance to exist
 * per generated instance of the biome", which is a per-biome-patch concept placement modifiers
 * don't have a native equivalent for (they resolve per-chunk, independently, with no memory of
 * neighboring chunks). {@link RarityFilter#onAverageOnceEvery} below is a per-chunk stand-in
 * tuned to feel sparse/landmark-like; getting the exact "half of patches have one" behavior would
 * need a custom {@code Feature} that reasons about the whole biome region. Revisit after
 * playtesting shows whether this reads as too common/rare.
 */
public final class ModTreeProvider {
    private ModTreeProvider() {
    }

    public static void bootstrapConfiguredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        context.register(ModConfiguredFeatures.ASHWOOD_TREE, new ConfiguredFeature<>(Feature.TREE,
                new TreeConfiguration.TreeConfigurationBuilder(
                        BlockStateProvider.simple(ModBlocks.ASHWOOD_LOG.get()),
                        new StraightTrunkPlacer(5, 2, 1), // vanilla Spruce's own trunk - shorter than Rowanwood's
                        BlockStateProvider.simple(ModBlocks.ASHWOOD_LEAVES.get()),
                        new SpruceFoliagePlacer(UniformInt.of(1, 2), UniformInt.of(0, 2), UniformInt.of(1, 2)), // narrower cone than Rowanwood's
                        new TwoLayersFeatureSize(1, 0, 1))
                        .ignoreVines()
                        .build()));

        context.register(ModConfiguredFeatures.ROWANWOOD_TREE, new ConfiguredFeature<>(Feature.TREE,
                new TreeConfiguration.TreeConfigurationBuilder(
                        BlockStateProvider.simple(ModBlocks.ROWANWOOD_LOG.get()),
                        new StraightTrunkPlacer(8, 3, 2), // taller than vanilla Spruce's (5,2,1) - a landmark, not just a tree
                        BlockStateProvider.simple(ModBlocks.ROWANWOOD_LEAVES.get()),
                        new SpruceFoliagePlacer(UniformInt.of(2, 4), UniformInt.of(0, 2), UniformInt.of(1, 2)),
                        new TwoLayersFeatureSize(2, 0, 2))
                        .ignoreVines()
                        .build()));
    }

    public static void bootstrapPlacedFeatures(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        // 6 was Flower Forest's own density (one of vanilla's densest) - way too tight for a
        // walkable "vibe wood" grove, and it's what caused the wall-to-wall touching canopies.
        // 2 is closer to a normal, walkable forest.
        context.register(ModPlacedFeatures.ASHWOOD_TREE_PLACED,
                new PlacedFeature(configuredFeatures.getOrThrow(ModConfiguredFeatures.ASHWOOD_TREE),
                        treePlacement(PlacementUtils.countExtra(2, 0.1F, 1))));

        context.register(ModPlacedFeatures.ROWANWOOD_TREE_PLACED,
                new PlacedFeature(configuredFeatures.getOrThrow(ModConfiguredFeatures.ROWANWOOD_TREE),
                        treePlacement(RarityFilter.onAverageOnceEvery(32))));
    }

    /** Mirrors vanilla's standard "scatter a tree across the chunk" placement chain. */
    private static java.util.List<PlacementModifier> treePlacement(PlacementModifier countOrRarity) {
        return java.util.List.of(
                countOrRarity,
                InSquarePlacement.spread(),
                SurfaceWaterDepthFilter.forMaxDepth(0),
                PlacementUtils.HEIGHTMAP_OCEAN_FLOOR,
                BiomeFilter.biome());
    }
}
