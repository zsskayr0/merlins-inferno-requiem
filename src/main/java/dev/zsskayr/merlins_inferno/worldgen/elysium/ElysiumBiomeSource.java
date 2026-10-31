package dev.zsskayr.merlins_inferno.worldgen.elysium;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumBiome;

/**
 * El'ysium's biomes come straight from the core ({@code ElysiumWorld.biomeAt}), the same function that shapes the
 * terrain, so a biome can never disagree with the island it labels. JSON: {@code {"type": "merlins_inferno:elysium"}}.
 */
public final class ElysiumBiomeSource extends BiomeSource {
    public static final MapCodec<ElysiumBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RegistryOps.<Biome, ElysiumBiomeSource>retrieveGetter(Registries.BIOME)
    ).apply(instance, ElysiumBiomeSource::new));

    /** Holders by {@link ElysiumBiome#ordinal()}. */
    private final List<Holder<Biome>> holders;

    public ElysiumBiomeSource(HolderGetter<Biome> biomes) {
        ElysiumBiome[] all = ElysiumBiome.values();
        List<Holder<Biome>> list = new ArrayList<>(all.length);
        for (ElysiumBiome b : all) {
            list.add(biomes.getOrThrow(key(b)));
        }
        this.holders = List.copyOf(list);
    }

    public static ResourceKey<Biome> key(ElysiumBiome biome) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, biome.id));
    }

    public Holder<Biome> holder(ElysiumBiome biome) {
        return holders.get(biome.ordinal());
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return holders.stream();
    }

    /** Quart coordinates in, the biome at the centre of that 4x4x4 cell out. */
    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        return holder(ElysiumSeed.world().biomeAt(QuartPos.toBlock(x) + 2, QuartPos.toBlock(y) + 2, QuartPos.toBlock(z) + 2));
    }
}
