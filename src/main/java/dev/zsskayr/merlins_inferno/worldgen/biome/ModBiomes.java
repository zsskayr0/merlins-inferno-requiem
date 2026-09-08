package dev.zsskayr.merlins_inferno.worldgen.biome;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * {@link ResourceKey} constants for the biomes this mod adds. Biomes are a dynamic (datapack)
 * registry, not a code-registered one, so unlike {@code ModItems}/{@code ModBlocks} there's no
 * {@code DeferredRegister} here - the actual {@link Biome} data is built in
 * {@code dev.zsskayr.merlins_inferno.datagen.worldgen.ModBiomeProvider} and only a stable key
 * (usable before the registry is populated) lives here.
 */
public final class ModBiomes {
    public static final ResourceKey<Biome> HALLOWED_GROVE = key("hallowed_grove");

    private ModBiomes() {
    }

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, name));
    }
}
