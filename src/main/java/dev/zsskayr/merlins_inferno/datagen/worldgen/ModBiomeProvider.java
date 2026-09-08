package dev.zsskayr.merlins_inferno.datagen.worldgen;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import dev.zsskayr.merlins_inferno.worldgen.ModPlacedFeatures;
import dev.zsskayr.merlins_inferno.worldgen.biome.ModBiomes;

/**
 * Builds the actual {@link Biome} data for {@link ModBiomes#HALLOWED_GROVE}. Runs only during the
 * {@code data} run (datagen) - the output gets written to
 * {@code src/generated/resources/data/merlins_inferno/worldgen/biome/hallowed_grove.json}, which
 * is what the game actually loads at runtime (see {@code build.gradle}'s
 * {@code sourceSets.main.resources.srcDir 'src/generated/resources'}).
 * <p>
 * Structured the same way vanilla's {@code OverworldBiomes.oldGrowthTaiga} is (ancient/mossy
 * forest floor, fox/rabbit + the standard hostile set) since that's the closest vanilla analogue
 * to "old mystical grove". The turquoise palette comes entirely from {@code BiomeSpecialEffects}'
 * color overrides, not from picking an unusual temperature/downfall. Trees: see
 * {@code ModTreeProvider} for the actual shapes/placement.
 */
public final class ModBiomeProvider {
    private ModBiomeProvider() {
    }

    public static void bootstrap(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> worldCarvers = context.lookup(Registries.CONFIGURED_CARVER);

        context.register(ModBiomes.HALLOWED_GROVE, hallowedGrove(placedFeatures, worldCarvers));
    }

    private static Biome hallowedGrove(HolderGetter<PlacedFeature> placedFeatures, HolderGetter<ConfiguredWorldCarver<?>> worldCarvers) {
        MobSpawnSettings.Builder mobSpawns = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.farmAnimals(mobSpawns);
        mobSpawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.FOX, 8, 2, 4));
        mobSpawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3));
        BiomeDefaultFeatures.commonSpawns(mobSpawns);

        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(placedFeatures, worldCarvers);
        BiomeDefaultFeatures.addDefaultCarversAndLakes(generation);
        BiomeDefaultFeatures.addDefaultCrystalFormations(generation);
        BiomeDefaultFeatures.addDefaultMonsterRoom(generation);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(generation);
        BiomeDefaultFeatures.addDefaultSprings(generation);
        BiomeDefaultFeatures.addSurfaceFreezing(generation);
        BiomeDefaultFeatures.addMossyStoneBlock(generation);
        BiomeDefaultFeatures.addFerns(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addDefaultSoftDisks(generation);
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, placedFeatures.getOrThrow(ModPlacedFeatures.ASHWOOD_TREE_PLACED));
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, placedFeatures.getOrThrow(ModPlacedFeatures.ROWANWOOD_TREE_PLACED));
        BiomeDefaultFeatures.addDefaultFlowers(generation);
        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation);
        BiomeDefaultFeatures.addCommonBerryBushes(generation);

        BiomeSpecialEffects effects = new BiomeSpecialEffects.Builder()
                .waterColor(0x1F6F7A)
                .waterFogColor(0x0F3D45)
                .fogColor(0x8FD9CE)
                .skyColor(0x9FCFC7)
                .grassColorOverride(0x3E9E8D)
                .foliageColorOverride(0x2E8B7D)
                .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                .build();

        return new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                // NOTE: this temperature is unrelated to the noise/climate niche the Mixin
                // places the biome at (that only controls WHERE in the world it generates).
                // This one controls rain vs. snow/ice - anything below 0.15 freezes, which is
                // why an earlier 0.0F made it generate as a snowy biome. 0.6F keeps it solidly
                // temperate/rainy (matches vanilla Birch Forest), for the European/Celtic look.
                .temperature(0.6F)
                .downfall(0.8F) // humid, matches the temperate/humid niche the Mixin places this at
                .specialEffects(effects)
                .mobSpawnSettings(mobSpawns.build())
                .generationSettings(generation.build())
                .build();
    }
}
