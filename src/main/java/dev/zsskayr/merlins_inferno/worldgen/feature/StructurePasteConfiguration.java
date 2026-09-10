package dev.zsskayr.merlins_inferno.worldgen.feature;

import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Config for {@link StructurePasteFeature}: a pool of NBT structure templates to pick one from at
 * random each time the feature runs. Every rotation of a given tree is its own separate structure
 * file (baked in ahead of time by whoever authored them) rather than one file rotated at runtime,
 * so "pick a random structure from this list" is the entire variation story - no Rotation/Mirror
 * handling needed here.
 * <p>
 * {@code blockSwaps} optionally remaps blocks in the pasted structure (see
 * {@link BlockPaletteSwapProcessor}) - e.g. the Ashwood tree structures are untouched vanilla
 * spruce NBT files, retextured to Ashwood's own log/wood/leaves at paste time instead of needing
 * hand-edited copies of every file.
 */
public record StructurePasteConfiguration(List<ResourceLocation> structures, Map<Block, Block> blockSwaps) implements FeatureConfiguration {
    public static final Codec<StructurePasteConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.listOf().fieldOf("structures").forGetter(StructurePasteConfiguration::structures),
            Codec.unboundedMap(BuiltInRegistries.BLOCK.byNameCodec(), BuiltInRegistries.BLOCK.byNameCodec())
                    .optionalFieldOf("block_swaps", Map.of())
                    .forGetter(StructurePasteConfiguration::blockSwaps)
    ).apply(instance, StructurePasteConfiguration::new));

    public StructurePasteConfiguration(List<ResourceLocation> structures) {
        this(structures, Map.of());
    }
}
