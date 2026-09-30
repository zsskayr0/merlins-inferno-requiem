package dev.zsskayr.merlins_inferno.datagen.worldgen;

import java.util.List;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.GeodeBlockSettings;
import net.minecraft.world.level.levelgen.GeodeCrackSettings;
import net.minecraft.world.level.levelgen.GeodeLayerSettings;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.GeodeConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RandomOffsetPlacement;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.common.world.BiomeModifier;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.worldgen.ModConfiguredFeatures;
import dev.zsskayr.merlins_inferno.worldgen.ModPlacedFeatures;

/**
 * Where Raw Lyrium comes from in the world (design decision): it forms like amethyst - a geode
 * with a calcite/basalt shell, a Lyrium Block lining and Lyrium Clusters growing inward - and it
 * also turns up as scarce, rare single-block ores, the way emeralds do. Lyrium is the celestial
 * material ("high, sacred"), so both are added to mountain biomes only, above the usual cave
 * depths.
 * <p>
 * Everything here is data written by the {@code data} run; the biome modifiers (not the biome
 * JSONs) are what attach the features, so mod-added mountain biomes get them too.
 */
public final class ModLyriumProvider {
    private ModLyriumProvider() {
    }

    public static void bootstrapConfiguredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        // Emerald-style: tiny veins (size 2) in stone or deepslate.
        context.register(ModConfiguredFeatures.LYRIUM_ORE, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(List.of(
                OreConfiguration.target(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES), ModBlocks.LYRIUM_ORE.get().defaultBlockState()),
                OreConfiguration.target(new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES), ModBlocks.DEEPSLATE_LYRIUM_ORE.get().defaultBlockState())),
                2)));

        // Amethyst-style geode: the vanilla amethyst geode's own numbers, with Lyrium Block as the
        // lining and Lyrium Clusters as the crystals. Clusters may grow from any lining block
        // ("placementsRequireLayer0Alternate" off) since there's no budding variant to gate them.
        BlockStateProvider lining = BlockStateProvider.simple(ModBlocks.LYRIUM_BLOCK.get());
        context.register(ModConfiguredFeatures.LYRIUM_GEODE, new ConfiguredFeature<>(Feature.GEODE, new GeodeConfiguration(
                new GeodeBlockSettings(
                        BlockStateProvider.simple(Blocks.AIR),
                        lining,
                        lining,
                        BlockStateProvider.simple(Blocks.CALCITE),
                        BlockStateProvider.simple(Blocks.SMOOTH_BASALT),
                        List.of(ModBlocks.LYRIUM_CLUSTER.get().defaultBlockState()),
                        BlockTags.FEATURES_CANNOT_REPLACE,
                        BlockTags.GEODE_INVALID_BLOCKS),
                new GeodeLayerSettings(1.7, 2.2, 3.2, 4.2),
                new GeodeCrackSettings(0.95, 2.0, 2),
                0.35, 0.083, false,
                UniformInt.of(4, 6), UniformInt.of(3, 4), UniformInt.of(1, 2),
                -16, 16, 0.05, 1)));
    }

    public static void bootstrapPlacedFeatures(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);

        // "Scarce and rare": on average about two attempts every three chunks of mountain, each
        // a vein of at most 2 blocks - well under emerald's density.
        context.register(ModPlacedFeatures.LYRIUM_ORE_PLACED, new PlacedFeature(configured.getOrThrow(ModConfiguredFeatures.LYRIUM_ORE), List.of(
                CountPlacement.of(2),
                RarityFilter.onAverageOnceEvery(3),
                InSquarePlacement.spread(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(48), VerticalAnchor.absolute(300)),
                BiomeFilter.biome())));

        // One in 16 chunks of mountain (vanilla's amethyst geode is one in 24, but it can generate
        // anywhere underground - this one only in mountains, so it has to be denser to be found). The centre goes
        // 12-22 blocks under the terrain surface rather than at a fixed height. The geode is a sphere about 8 blocks
        // across and knows nothing about the terrain, so on a slope the ground beside it is lower than the ground
        // above it: 6-14 blocks down still left a dome of basalt standing on the hillside. Deep enough that even the
        // slope's low side covers it, it is found where mountain caves, ravines and cliffs crack it open.
        context.register(ModPlacedFeatures.LYRIUM_GEODE_PLACED, new PlacedFeature(configured.getOrThrow(ModConfiguredFeatures.LYRIUM_GEODE), List.of(
                RarityFilter.onAverageOnceEvery(16),
                InSquarePlacement.spread(),
                HeightmapPlacement.onHeightmap(Heightmap.Types.OCEAN_FLOOR_WG),
                // Two offsets: one RandomOffsetPlacement only takes -16..16, and the total here is -22..-12.
                RandomOffsetPlacement.vertical(UniformInt.of(-12, -6)),
                RandomOffsetPlacement.vertical(UniformInt.of(-10, -6)),
                BiomeFilter.biome())));
    }

    public static void bootstrapBiomeModifiers(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> placed = context.lookup(Registries.PLACED_FEATURE);

        context.register(modifierKey("add_lyrium_ore"), new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_MOUNTAIN),
                HolderSet.direct(placed.getOrThrow(ModPlacedFeatures.LYRIUM_ORE_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        context.register(modifierKey("add_lyrium_geode"), new BiomeModifiers.AddFeaturesBiomeModifier(
                biomes.getOrThrow(BiomeTags.IS_MOUNTAIN),
                HolderSet.direct(placed.getOrThrow(ModPlacedFeatures.LYRIUM_GEODE_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_DECORATION));
    }

    private static ResourceKey<BiomeModifier> modifierKey(String name) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, name));
    }
}
