package dev.zsskayr.merlins_inferno.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/**
 * Once per chunk: every lava block in the given biome chars the stone and flesh within {@code radius} blocks of it.
 * Runs over the chunk's own 16x16 columns, so it never reaches further than the generation region allows.
 */
public class LavaCharFeature extends Feature<LavaCharFeature.Config> {
    public record Config(BlockState state, ResourceLocation biome, int radius) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockState.CODEC.fieldOf("state").forGetter(Config::state),
                ResourceLocation.CODEC.fieldOf("biome").forGetter(Config::biome),
                Codec.intRange(1, 4).fieldOf("radius").forGetter(Config::radius)
        ).apply(instance, Config::new));
    }

    public LavaCharFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        Config config = context.config();
        ChunkPos chunk = new ChunkPos(context.origin());
        int minY = level.getMinBuildHeight() + 5, maxY = level.getMaxBuildHeight() - 2;
        int r = config.radius();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                for (int y = minY; y < maxY; y++) {
                    cursor.set(x, y, z);
                    if (!level.getFluidState(cursor).is(FluidTags.LAVA)) {
                        continue;
                    }
                    if (!level.getBiome(cursor).unwrapKey().map(k -> k.location().equals(config.biome())).orElse(false)) {
                        continue;
                    }
                    BlockPos lava = cursor.immutable();
                    for (int dx = -r; dx <= r; dx++) {
                        for (int dy = -r; dy <= r; dy++) {
                            for (int dz = -r; dz <= r; dz++) {
                                if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > r) {
                                    continue;
                                }
                                BlockPos pos = lava.offset(dx, dy, dz);
                                BlockState there = level.getBlockState(pos);
                                if (there.is(BlockTags.BASE_STONE_NETHER) || there.is(ModBlocks.FLESH_BLOCK.get())
                                        || there.is(ModBlocks.POROUS_FLESH.get())) {
                                    level.setBlock(pos, config.state(), 2);
                                    placed = true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return placed;
    }
}
