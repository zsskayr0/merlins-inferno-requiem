package dev.zsskayr.merlins_inferno.datagen.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModFeatures;
import dev.zsskayr.merlins_inferno.worldgen.ModConfiguredFeatures;
import dev.zsskayr.merlins_inferno.worldgen.ModPlacedFeatures;
import dev.zsskayr.merlins_inferno.worldgen.feature.StructurePasteConfiguration;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter;

/** Imported snow/ice and Lyrium crystal forms for El'ysium's two frozen biomes. */
public final class ModFrozenSpikeProvider {
    private ModFrozenSpikeProvider() {}

    public static void bootstrapConfiguredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        List<ResourceLocation> ice = new ArrayList<>(14);
        for (int i = 1; i <= 14; i++) ice.add(template("snowy_ice_" + twoDigits(i)));
        List<ResourceLocation> lyrium = new ArrayList<>(19);
        for (int i = 1; i <= 12; i++) lyrium.add(template("lyrium_west_" + twoDigits(i)));
        for (int i = 2; i <= 8; i++) lyrium.add(template("lyrium_east_" + twoDigits(i)));
        context.register(ModConfiguredFeatures.SNOWY_ICE_SPIKES,
                new ConfiguredFeature<>(ModFeatures.STRUCTURE_PASTE.get(),
                        new StructurePasteConfiguration(ice, Map.of(), 4)));
        context.register(ModConfiguredFeatures.LYRIUM_SPIKES,
                new ConfiguredFeature<>(ModFeatures.STRUCTURE_PASTE.get(),
                        new StructurePasteConfiguration(lyrium, Map.of(), 4)));
        context.register(ModConfiguredFeatures.GIANT_LYRIUM_SPIKE,
                new ConfiguredFeature<>(ModFeatures.STRUCTURE_PASTE.get(),
                        new StructurePasteConfiguration(List.of(template("lyrium_east_01")), Map.of(), 3)));
    }

    public static void bootstrapPlacedFeatures(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);
        register(context, configured, ModPlacedFeatures.SNOWY_ICE_SPIKES_FROSTCREST,
                ModConfiguredFeatures.SNOWY_ICE_SPIKES, 9);
        register(context, configured, ModPlacedFeatures.SNOWY_ICE_SPIKES_RIME,
                ModConfiguredFeatures.SNOWY_ICE_SPIKES, 5);
        register(context, configured, ModPlacedFeatures.LYRIUM_SPIKES_FROSTCREST,
                ModConfiguredFeatures.LYRIUM_SPIKES, 24);
        register(context, configured, ModPlacedFeatures.LYRIUM_SPIKES_RIME,
                ModConfiguredFeatures.LYRIUM_SPIKES, 28);
        register(context, configured, ModPlacedFeatures.GIANT_LYRIUM_SPIKE_FROSTCREST,
                ModConfiguredFeatures.GIANT_LYRIUM_SPIKE, 140);
        register(context, configured, ModPlacedFeatures.GIANT_LYRIUM_SPIKE_RIME,
                ModConfiguredFeatures.GIANT_LYRIUM_SPIKE, 180);
    }

    private static void register(BootstrapContext<PlacedFeature> context,
            HolderGetter<ConfiguredFeature<?, ?>> configured,
            net.minecraft.resources.ResourceKey<PlacedFeature> placed,
            net.minecraft.resources.ResourceKey<ConfiguredFeature<?, ?>> feature, int rarity) {
        context.register(placed, new PlacedFeature(configured.getOrThrow(feature), List.of(
                RarityFilter.onAverageOnceEvery(rarity), InSquarePlacement.spread(),
                SurfaceWaterDepthFilter.forMaxDepth(0), PlacementUtils.HEIGHTMAP_OCEAN_FLOOR,
                BiomeFilter.biome())));
    }

    private static ResourceLocation template(String name) {
        return ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "frozen_spike/" + name);
    }

    private static String twoDigits(int value) {
        return value < 10 ? "0" + value : Integer.toString(value);
    }
}
