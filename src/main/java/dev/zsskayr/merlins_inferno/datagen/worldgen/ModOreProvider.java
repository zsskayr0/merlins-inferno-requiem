package dev.zsskayr.merlins_inferno.datagen.worldgen;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.worldgen.ModConfiguredFeatures;
import dev.zsskayr.merlins_inferno.worldgen.ModPlacedFeatures;

/**
 * Demonite Debris ore generation - deliberately mirrors vanilla's own Ancient Debris setup 1:1
 * (same large/small vein split, same size/discard-on-air-exposure values, same height bands,
 * replacing {@link BlockTags#BASE_STONE_NETHER} exactly like Ancient Debris does), with one
 * change: an extra {@code countExtra} roll on both veins for a 25% chance of a second attempt per
 * chunk - "a little more common" without changing how it actually looks/behaves once found.
 * <p>
 * Actually placing this into the Nether happens separately, via a data-driven
 * {@code neoforge:add_features} biome modifier (see
 * {@code data/merlins_inferno/neoforge/biome_modifier/demonite_debris_ore.json}) targeting
 * {@code #minecraft:is_nether} - unlike the Overworld biome placement problem (see
 * {@code OverworldBiomeBuilderMixin}), adding a decoration feature to biomes that already exist
 * (rather than deciding which biome generates where) is fully data-driven, no Mixin needed.
 */
public final class ModOreProvider {
    private ModOreProvider() {
    }

    public static void bootstrapConfiguredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest baseStoneNether = new TagMatchTest(BlockTags.BASE_STONE_NETHER);

        context.register(ModConfiguredFeatures.DEMONITE_DEBRIS_ORE_LARGE, new ConfiguredFeature<>(Feature.SCATTERED_ORE,
                new OreConfiguration(baseStoneNether, ModBlocks.DEMONITE_DEBRIS.get().defaultBlockState(), 3, 1.0F)));
        context.register(ModConfiguredFeatures.DEMONITE_DEBRIS_ORE_SMALL, new ConfiguredFeature<>(Feature.SCATTERED_ORE,
                new OreConfiguration(baseStoneNether, ModBlocks.DEMONITE_DEBRIS.get().defaultBlockState(), 2, 1.0F)));
    }

    public static void bootstrapPlacedFeatures(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        context.register(ModPlacedFeatures.DEMONITE_DEBRIS_ORE_LARGE_PLACED,
                new PlacedFeature(configuredFeatures.getOrThrow(ModConfiguredFeatures.DEMONITE_DEBRIS_ORE_LARGE), java.util.List.of(
                        PlacementUtils.countExtra(1, 0.25F, 1), // vanilla's implicit 1/chunk, +25% chance of a 2nd
                        InSquarePlacement.spread(),
                        HeightRangePlacement.triangle(VerticalAnchor.absolute(8), VerticalAnchor.absolute(24)),
                        BiomeFilter.biome())));

        context.register(ModPlacedFeatures.DEMONITE_DEBRIS_ORE_SMALL_PLACED,
                new PlacedFeature(configuredFeatures.getOrThrow(ModConfiguredFeatures.DEMONITE_DEBRIS_ORE_SMALL), java.util.List.of(
                        PlacementUtils.countExtra(1, 0.25F, 1),
                        InSquarePlacement.spread(),
                        PlacementUtils.RANGE_8_8,
                        BiomeFilter.biome())));
    }
}
