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

    // No Rowanwood entry here anymore - it's a real Structure now (see RowanwoodTreeStructure),
    // not a decoration Feature. See ModStructures instead.

    // Demonite Debris ore veins - see ModOreProvider.
    public static final ResourceKey<ConfiguredFeature<?, ?>> DEMONITE_DEBRIS_ORE_LARGE = key("demonite_debris_ore_large");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DEMONITE_DEBRIS_ORE_SMALL = key("demonite_debris_ore_small");

    private ModConfiguredFeatures() {
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> key(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, name));
    }
}
