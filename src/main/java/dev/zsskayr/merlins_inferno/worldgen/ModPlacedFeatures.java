package dev.zsskayr.merlins_inferno.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * {@link ResourceKey} constants for this mod's {@code PlacedFeature}s (where/how often a
 * {@code ConfiguredFeature} scatters across the world). Actual data built in
 * {@code dev.zsskayr.merlins_inferno.datagen.worldgen.ModTreeProvider}.
 */
public final class ModPlacedFeatures {
    public static final ResourceKey<PlacedFeature> ASHWOOD_TREE_PLACED = key("ashwood_tree_placed");
    public static final ResourceKey<PlacedFeature> ROWANWOOD_TREE_PLACED = key("rowanwood_tree_placed");

    private ModPlacedFeatures() {
    }

    private static ResourceKey<PlacedFeature> key(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, name));
    }
}
