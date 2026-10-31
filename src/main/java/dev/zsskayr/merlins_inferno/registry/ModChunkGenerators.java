package dev.zsskayr.merlins_inferno.registry;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumBiomeSource;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumChunkGenerator;

/** Chunk generator and biome source types of this mod (El'ysium's own generator, {@code merlins_inferno:elysium}). */
public final class ModChunkGenerators {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, Merlins_inferno.MODID);
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, Merlins_inferno.MODID);

    public static final DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<ElysiumChunkGenerator>> ELYSIUM_GENERATOR =
            CHUNK_GENERATORS.register("elysium", () -> ElysiumChunkGenerator.CODEC);
    public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<ElysiumBiomeSource>> ELYSIUM_BIOME_SOURCE =
            BIOME_SOURCES.register("elysium", () -> ElysiumBiomeSource.CODEC);

    private ModChunkGenerators() {
    }

    public static void register(IEventBus bus) {
        CHUNK_GENERATORS.register(bus);
        BIOME_SOURCES.register(bus);
    }
}
