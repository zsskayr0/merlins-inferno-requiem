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

import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;
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
 * to "old mystical grove". The subtle cool tint comes entirely from {@code BiomeSpecialEffects}'
 * color overrides, not from picking an unusual temperature/downfall - kept mild so grass/fog still
 * read as a natural temperate forest rather than an artificial color. Trees: see
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
        // Default is 0.1: only one chunk in ten rolls its creatures at generation, which left loose Druids
        // (weight 15 of ~55) practically never seen. 0.5 makes wandering Druids a real part of the grove.
        MobSpawnSettings.Builder mobSpawns = new MobSpawnSettings.Builder().creatureGenerationProbability(0.5F);
        BiomeDefaultFeatures.farmAnimals(mobSpawns);
        mobSpawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.FOX, 8, 2, 4));
        mobSpawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3));
        // Solitary on purpose (max group size 1) - a lone guardian spirit reads as more special
        // than a pack of them. This is also the ONLY biome that lists it, which is the entire
        // mechanism keeping it exclusive to the Hallowed Grove (see ModSpawnPlacements - the
        // placement rule itself has no biome restriction).
        // Weight bumped from 3 to 15 (on par with sheep, the heaviest entry here) - CREATURE-
        // category mobs mostly spawn once per chunk at generation time and don't despawn, so with
        // farm animals + fox + rabbit already filling the area's spawn cap first, a weight of 3 out
        // of ~55 meant the Druid essentially never actually won the roll in practice (confirmed:
        // never once seen across several fresh test worlds) even though it was wired up correctly.
        mobSpawns.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(ModEntityTypes.DRUID.get(), 15, 1, 1));
        BiomeDefaultFeatures.commonSpawns(mobSpawns);
        // The night miniboss. Weight 5 against the ~500 of the vanilla monster set makes it a rare
        // roll; the spawn rule (night, this biome, none within 128 blocks) and the entity's own
        // dawn/leave-the-biome despawn do the rest - see DullahanEntity.
        mobSpawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ModEntityTypes.DULLAHAN.get(), 5, 1, 1));
        // Ostara, the Spring Deity: the Circle 1 boss. Even rarer; the rule (daytime, solitary) lives on the entity.
        mobSpawns.addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ModEntityTypes.OSTARA.get(), 1, 1, 1));

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
        // Rowanwood is no longer a decoration Feature - it's a real Structure now (see
        // RowanwoodTreeStructure), so it doesn't get added to a biome's feature list at all. Its
        // own StructureSettings' biome HolderSet (see ModStructureProvider) is what restricts it
        // to Hallowed Grove.
        BiomeDefaultFeatures.addDefaultFlowers(generation);
        // Ground cover - forgetting this left the biome with no grass patches at all. MUST come
        // after addDefaultFlowers: vanilla's OverworldBiomes.forest() does flowers-then-grass for
        // birch forest, and the two must agree on relative order for shared placed features or
        // the game throws "Feature order cycle found" against old_growth_birch_forest at load.
        BiomeDefaultFeatures.addForestGrass(generation);
        BiomeDefaultFeatures.addDefaultMushrooms(generation);
        BiomeDefaultFeatures.addDefaultExtraVegetation(generation);
        BiomeDefaultFeatures.addCommonBerryBushes(generation);

        // Toned down from an earlier neon-turquoise pass (0x8FD9CE fog, 0x3E9E8D grass, etc.) that
        // read as artificial. Keeps a faint cool/"hallowed" tint but stays close to natural
        // temperate-forest colors (vanilla Birch Forest-ish) so fog and grass don't look painted on.
        // Fog/sky/water nudged a bit further toward blue (still desaturated, not neon) per
        // follow-up feedback - grass/foliage stay green since blue-tinted grass reads as fake.
        BiomeSpecialEffects effects = new BiomeSpecialEffects.Builder()
                .waterColor(0x35687A)
                .waterFogColor(0x17303F)
                .fogColor(0xC2D3DC)
                .skyColor(0x7CAFC4)
                .grassColorOverride(0x52A575)
                .foliageColorOverride(0x5C9C5E)
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
