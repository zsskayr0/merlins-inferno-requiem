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
    public static final ResourceKey<PlacedFeature> CASCADE_PINK_TREE_PLACED = key("cascade_pink_tree_placed");
    public static final ResourceKey<PlacedFeature> CASCADE_BLUE_SPRUCE_PLACED = key("cascade_blue_spruce_placed");
    public static final ResourceKey<PlacedFeature> LYRIUM_GEODE_PLACED = key("lyrium_geode_placed");
    public static final ResourceKey<PlacedFeature> LYRIUM_GEODE_SELECTOR_NORMAL = key("lyrium_geode_selector_normal");
    public static final ResourceKey<PlacedFeature> LYRIUM_GEODE_SELECTOR_RED = key("lyrium_geode_selector_red");
    public static final ResourceKey<PlacedFeature> OBLIVION_RED_LYRIUM_GEODE = key("oblivion_red_lyrium_geode");
    public static final ResourceKey<PlacedFeature> OBLIVION_GIANT_RED_LYRIUM_GEODE = key("oblivion_giant_red_lyrium_geode");
    public static final ResourceKey<PlacedFeature> SNOWY_ICE_SPIKES_FROSTCREST = key("snowy_ice_spikes_frostcrest");
    public static final ResourceKey<PlacedFeature> SNOWY_ICE_SPIKES_RIME = key("snowy_ice_spikes_rime");
    public static final ResourceKey<PlacedFeature> LYRIUM_SPIKES_FROSTCREST = key("lyrium_spikes_frostcrest");
    public static final ResourceKey<PlacedFeature> LYRIUM_SPIKES_RIME = key("lyrium_spikes_rime");
    public static final ResourceKey<PlacedFeature> GIANT_LYRIUM_SPIKE_FROSTCREST = key("giant_lyrium_spike_frostcrest");
    public static final ResourceKey<PlacedFeature> GIANT_LYRIUM_SPIKE_RIME = key("giant_lyrium_spike_rime");


    private ModPlacedFeatures() {
    }

    private static ResourceKey<PlacedFeature> key(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, name));
    }
}
