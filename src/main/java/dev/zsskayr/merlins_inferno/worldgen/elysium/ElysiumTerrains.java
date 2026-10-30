package dev.zsskayr.merlins_inferno.worldgen.elysium;

import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * Finds the {@link ElysiumTerrain} of a world. The seed comes from the {@code elysium} noise: the router's copy of it and
 * the one {@code RandomState} hands to features are the same {@link NormalNoise}, so terrain, biomes, water and
 * bridges all agree without anything having to read the world seed directly.
 */
public final class ElysiumTerrains {
    public static final ResourceKey<NormalNoise.NoiseParameters> NOISE_KEY = ResourceKey.create(Registries.NOISE,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "elysium_seed"));

    private static final ConcurrentHashMap<Long, ElysiumTerrain> CACHE = new ConcurrentHashMap<>();
    private static final ElysiumTerrain FALLBACK = new ElysiumTerrain(0L);

    private ElysiumTerrains() {
    }

    public static ElysiumTerrain forNoise(NormalNoise noise) {
        if (noise == null) {
            return FALLBACK;
        }
        long seed = Double.doubleToLongBits(noise.getValue(0.5, 0.25, 0.75)) * 31L + Double.doubleToLongBits(noise.getValue(17.5, 3.25, -9.75));
        if (CACHE.size() > 8) {
            CACHE.clear();
        }
        return CACHE.computeIfAbsent(seed, ElysiumTerrain::new);
    }

    private static final ConcurrentHashMap<Long, ElysiumSpans> SPANS = new ConcurrentHashMap<>();

    /** The spans (bridges and chains) of a terrain, found once and shared by every chunk. */
    public static ElysiumSpans spans(ElysiumTerrain terrain) {
        if (SPANS.size() > 8) {
            SPANS.clear();
        }
        return SPANS.computeIfAbsent(terrain.seed, k -> new ElysiumSpans(terrain));
    }

    public static ElysiumTerrain forLevel(ServerLevel level) {
        return forNoise(level.getChunkSource().randomState().getOrCreateNoise(NOISE_KEY));
    }
}
