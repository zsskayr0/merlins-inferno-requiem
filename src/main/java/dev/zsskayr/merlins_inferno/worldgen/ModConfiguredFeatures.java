package dev.zsskayr.merlins_inferno.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * {@link ResourceKey} constants for this mod's {@code ConfiguredFeature}s (tree shapes). Actual
 * data built in {@code dev.zsskayr.merlins_inferno.datagen.worldgen.ModTreeProvider}.
 */
public final class ModConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> ASHWOOD_TREE = key("ashwood_tree");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CASCADE_PINK_TREE = key("cascade_pink_tree");
    public static final ResourceKey<ConfiguredFeature<?, ?>> CASCADE_BLUE_SPRUCE = key("cascade_blue_spruce");
    public static final ResourceKey<ConfiguredFeature<?, ?>> LYRIUM_GEODE = key("lyrium_geode");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SNOWY_ICE_SPIKES = key("snowy_ice_spikes");
    public static final ResourceKey<ConfiguredFeature<?, ?>> LYRIUM_SPIKES = key("lyrium_spikes");
    public static final ResourceKey<ConfiguredFeature<?, ?>> GIANT_LYRIUM_SPIKE = key("giant_lyrium_spike");

    // No Rowanwood entry here anymore - it's a real Structure now (see RowanwoodTreeStructure),
    // not a decoration Feature. See ModStructures instead.


    private ModConfiguredFeatures() {
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> key(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, name));
    }
}
