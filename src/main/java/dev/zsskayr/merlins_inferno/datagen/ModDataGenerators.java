package dev.zsskayr.merlins_inferno.datagen;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.datagen.client.ModBlockStateProvider;
import dev.zsskayr.merlins_inferno.datagen.loot.ModBlockLootProvider;
import dev.zsskayr.merlins_inferno.datagen.worldgen.ModBiomeProvider;
import dev.zsskayr.merlins_inferno.datagen.worldgen.ModTreeProvider;

/**
 * Wires up every data generator this mod has. Hooked to {@link GatherDataEvent} from the main
 * mod class; running it (the {@code data} Gradle run) writes the generated JSON under
 * {@code src/generated/resources/}, which the normal {@code main} run then loads like any other
 * resource (see {@code build.gradle}'s extra {@code sourceSets.main.resources} dir).
 */
public final class ModDataGenerators {
    private ModDataGenerators() {
    }

    public static void gatherData(GatherDataEvent event) {
        PackOutput packOutput = event.getGenerator().getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // Order mirrors vanilla's own (VanillaRegistries.BUILDER): configured features before
        // placed features before biomes, since biomes reference placed features which reference
        // configured features. Cross-registry lookups are lazy either way, but this keeps it
        // consistent with the vanilla convention.
        RegistrySetBuilder dynamicRegistries = new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, ModTreeProvider::bootstrapConfiguredFeatures)
                .add(Registries.PLACED_FEATURE, ModTreeProvider::bootstrapPlacedFeatures)
                .add(Registries.BIOME, ModBiomeProvider::bootstrap);

        event.getGenerator().addProvider(event.includeServer(),
                new DatapackBuiltinEntriesProvider(packOutput, lookupProvider, dynamicRegistries, Set.of(Merlins_inferno.MODID)));

        event.getGenerator().addProvider(event.includeServer(),
                new LootTableProvider(packOutput, Set.of(),
                        List.of(new LootTableProvider.SubProviderEntry(ModBlockLootProvider::new, LootContextParamSets.BLOCK)),
                        lookupProvider));

        event.getGenerator().addProvider(event.includeClient(),
                new ModBlockStateProvider(packOutput, event.getExistingFileHelper()));
    }
}
