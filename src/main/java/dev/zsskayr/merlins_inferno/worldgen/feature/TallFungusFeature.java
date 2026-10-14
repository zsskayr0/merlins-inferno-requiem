package dev.zsskayr.merlins_inferno.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * A huge fungus like the Nether's, but much taller: a 1x1 (sometimes 2x2) stem of {@code min_height} to
 * {@code max_height} blocks with a wide, flattened cap of hat blocks, shroomlights under it and weeping vines hanging
 * from its underside. Grows only from the {@code minecraft:nylium} tag.
 */
public class TallFungusFeature extends Feature<TallFungusFeature.Config> {
    public record Config(BlockState stem, BlockState hat, BlockState decor, int minHeight, int maxHeight) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockState.CODEC.fieldOf("stem_state").forGetter(Config::stem),
                BlockState.CODEC.fieldOf("hat_state").forGetter(Config::hat),
                BlockState.CODEC.fieldOf("decor_state").forGetter(Config::decor),
                Codec.intRange(4, 64).fieldOf("min_height").forGetter(Config::minHeight),
                Codec.intRange(4, 64).fieldOf("max_height").forGetter(Config::maxHeight)
        ).apply(instance, Config::new));
    }

    public TallFungusFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        Config config = context.config();
        if (!level.getBlockState(origin.below()).is(BlockTags.NYLIUM)) {
            return false;
        }
        int height = config.minHeight() + random.nextInt(Math.max(1, config.maxHeight() - config.minHeight() + 1));
        int size = height >= 14 && random.nextFloat() < 0.5F ? 2 : 1;
        // every cell of the stem needs solid ground: on a rift edge a 2x2 stem would hang in the air
        if (size == 2 && !supported(level, origin, 2)) {
            size = 1;
        }
        int radius = size == 2 ? 5 : 4;
        height = Math.min(height, level.getMaxBuildHeight() - origin.getY() - 4);
        for (int y = 0; y < height; y++) {
            for (int dx = 0; dx < size; dx++) {
                for (int dz = 0; dz < size; dz++) {
                    if (!level.getBlockState(origin.offset(dx, y, dz)).canBeReplaced()) {
                        height = Math.min(height, y);
                    }
                }
            }
        }
        if (height < config.minHeight()) {
            return false;
        }
        for (int y = 0; y < height; y++) {
            for (int dx = 0; dx < size; dx++) {
                for (int dz = 0; dz < size; dz++) {
                    level.setBlock(origin.offset(dx, y, dz), config.stem(), 3);
                }
            }
        }
        // Cap layers around the top: radius per layer from 3 under the top to 2 over it.
        int[] radii = { radius - 2, radius - 1, radius, radius, radius - 1, radius - 3 };
        double centre = (size - 1) / 2.0;
        for (int layer = 0; layer < radii.length; layer++) {
            int y = origin.getY() + height - 3 + layer;
            int r = radii[layer];
            for (int dx = -r; dx <= r + size - 1; dx++) {
                for (int dz = -r; dz <= r + size - 1; dz++) {
                    double ox = dx - centre, oz = dz - centre;
                    double dist = ox * ox + oz * oz;
                    if (dist > r * r + r * 0.5 || (dist > (r - 1) * (r - 1) && random.nextFloat() < 0.15F)) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(origin.getX() + dx, y, origin.getZ() + dz);
                    if (!level.getBlockState(pos).canBeReplaced()) {
                        continue;
                    }
                    boolean underside = layer == 0;
                    level.setBlock(pos, underside && random.nextFloat() < 0.12F ? config.decor() : config.hat(), 3);
                    if (underside && random.nextFloat() < 0.18F) {
                        hangVines(level, pos.below(), random);
                    }
                }
            }
        }
        return true;
    }

    private static boolean supported(WorldGenLevel level, BlockPos origin, int size) {
        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                BlockPos below = origin.offset(dx, -1, dz);
                if (!level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void hangVines(WorldGenLevel level, BlockPos start, RandomSource random) {
        int length = 2 + random.nextInt(7);
        for (int i = 0; i < length; i++) {
            BlockPos pos = start.below(i);
            if (!level.getBlockState(pos).isAir()) {
                return;
            }
            boolean last = i == length - 1 || !level.getBlockState(pos.below()).isAir();
            level.setBlock(pos, (last ? dev.zsskayr.merlins_inferno.registry.ModBlocks.BLACK_VINES.get() : dev.zsskayr.merlins_inferno.registry.ModBlocks.BLACK_VINES_PLANT.get()).defaultBlockState(), 2);
            if (last) {
                return;
            }
        }
    }
}
