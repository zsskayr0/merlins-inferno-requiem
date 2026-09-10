package dev.zsskayr.merlins_inferno.datagen.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.registry.ModFeatures;
import dev.zsskayr.merlins_inferno.worldgen.ModConfiguredFeatures;
import dev.zsskayr.merlins_inferno.worldgen.ModPlacedFeatures;
import dev.zsskayr.merlins_inferno.worldgen.feature.StructurePasteConfiguration;

/**
 * Ashwood's natural-generation shape and placement for the Hallowed Grove - a structure-paste
 * feature (see {@code StructurePasteFeature}) rather than a procedural trunk/foliage placer,
 * ported from a friend's structure-based tree pack (spruce shapes, retextured to Ashwood via
 * block swap).
 * <p>
 * Rowanwood (the biome's other tree) is NOT here - it's a real {@code Structure} instead of a
 * decoration Feature, so it's findable with {@code /locate}. See {@code RowanwoodTreeStructure}
 * and {@code ModStructureProvider}.
 */
public final class ModTreeProvider {
    private ModTreeProvider() {
    }

    public static void bootstrapConfiguredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        context.register(ModConfiguredFeatures.ASHWOOD_TREE,
                new ConfiguredFeature<>(ModFeatures.STRUCTURE_PASTE.get(),
                        new StructurePasteConfiguration(ashwoodStructurePool(), ashwoodBlockSwaps())));
    }

    public static void bootstrapPlacedFeatures(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        // 6 was Flower Forest's own density (one of vanilla's densest) - way too tight for a
        // walkable "vibe wood" grove, and it's what caused the wall-to-wall touching canopies.
        // 2 is closer to a normal, walkable forest.
        context.register(ModPlacedFeatures.ASHWOOD_TREE_PLACED,
                new PlacedFeature(configuredFeatures.getOrThrow(ModConfiguredFeatures.ASHWOOD_TREE),
                        treePlacement(PlacementUtils.countExtra(2, 0.1F, 1))));
    }

    /** Mirrors vanilla's standard "scatter a tree across the chunk" placement chain. */
    private static List<PlacementModifier> treePlacement(PlacementModifier countOrRarity) {
        return List.of(
                countOrRarity,
                InSquarePlacement.spread(),
                SurfaceWaterDepthFilter.forMaxDepth(0),
                PlacementUtils.HEIGHTMAP_OCEAN_FLOOR,
                BiomeFilter.biome());
    }

    /**
     * The 36 Ashwood tree shapes (3 letters x 3 sizes x 4 baked rotations) under
     * {@code data/merlins_inferno/structure/ashwood_tree/}, ported from a friend's
     * structure-based tree pack. The NBT files themselves are untouched vanilla
     * spruce_log/spruce_wood/spruce_leaves - see {@link #ashwoodBlockSwaps()} for how they end up
     * as Ashwood in-world.
     */
    private static List<ResourceLocation> ashwoodStructurePool() {
        List<ResourceLocation> structures = new ArrayList<>(36);
        for (String letter : new String[] {"c", "d", "e"}) {
            for (int size = 1; size <= 3; size++) {
                for (String rotationSuffix : new String[] {"", "_r1", "_r2", "_r3"}) {
                    structures.add(ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID,
                            "ashwood_tree/" + letter + size + rotationSuffix));
                }
            }
        }
        return structures;
    }

    /**
     * Retextures the ported spruce structures to Ashwood at paste time (see
     * {@code BlockPaletteSwapProcessor}) instead of needing 36 hand-edited NBT files. Works
     * cleanly because Ashwood's log/wood/leaves share the exact same block-state properties
     * (axis; distance/persistent) as their vanilla counterparts.
     */
    private static Map<Block, Block> ashwoodBlockSwaps() {
        return Map.of(
                Blocks.SPRUCE_LOG, ModBlocks.ASHWOOD_LOG.get(),
                Blocks.SPRUCE_WOOD, ModBlocks.ASHWOOD_WOOD.get(),
                Blocks.SPRUCE_LEAVES, ModBlocks.ASHWOOD_LEAVES.get());
    }
}
