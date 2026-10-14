package dev.zsskayr.merlins_inferno.worldgen.feature;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * A giant chain hanging from a ceiling: diamond-shaped links in alternating planes, each link sharing its bottom
 * block with the top of the next. The origin is the air block right under the ceiling it hangs from. One material is
 * drawn per chain from a weighted palette, so a chain is never a patchwork.
 */
public class GiantChainFeature extends Feature<GiantChainFeature.Config> {
    public record Entry(BlockState state, int weight) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockState.CODEC.fieldOf("state").forGetter(Entry::state),
                Codec.intRange(1, 1000).fieldOf("weight").forGetter(Entry::weight)
        ).apply(instance, Entry::new));
    }

    public record Config(List<Entry> palette, int minLinks, int maxLinks, int minScale, int maxScale)
            implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Entry.CODEC.listOf().fieldOf("palette").forGetter(Config::palette),
                Codec.intRange(1, 40).fieldOf("min_links").forGetter(Config::minLinks),
                Codec.intRange(1, 40).fieldOf("max_links").forGetter(Config::maxLinks),
                Codec.intRange(1, 3).fieldOf("min_scale").forGetter(Config::minScale),
                Codec.intRange(1, 3).fieldOf("max_scale").forGetter(Config::maxScale)
        ).apply(instance, Config::new));
    }

    public GiantChainFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        Config config = context.config();
        if (!level.getBlockState(origin).isAir()) {
            return false;
        }
        BlockPos ceiling = origin.above();
        if (!level.getBlockState(ceiling).isFaceSturdy(level, ceiling, Direction.DOWN)) {
            return false;
        }
        BlockState material = pick(config, random);
        int scale = config.minScale() + random.nextInt(Math.max(1, config.maxScale() - config.minScale() + 1));
        int links = config.minLinks() + random.nextInt(Math.max(1, config.maxLinks() - config.minLinks() + 1));
        boolean alongX = random.nextBoolean();
        boolean placed = false;
        BlockPos top = origin;
        for (int link = 0; link < links; link++) {
            Direction side = alongX ? Direction.EAST : Direction.SOUTH;
            // the diamond: top (0,0), then widening to +-scale at half height and closing at the bottom (0,-2*scale)
            for (int step = 0; step <= scale * 2; step++) {
                int width = step <= scale ? step : scale * 2 - step;
                for (int sign = -1; sign <= 1; sign += 2) {
                    if (width == 0 && sign == 1) {
                        continue;
                    }
                    BlockPos pos = top.below(step).relative(side, sign * width);
                    if (level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, material, 2);
                        placed = true;
                    }
                }
            }
            top = top.below(scale * 2);
            alongX = !alongX;
            if (top.getY() <= level.getMinBuildHeight() + 6) {
                break;
            }
        }
        return placed;
    }

    private static BlockState pick(Config config, RandomSource random) {
        int total = 0;
        for (Entry entry : config.palette()) {
            total += entry.weight();
        }
        int roll = random.nextInt(Math.max(1, total));
        for (Entry entry : config.palette()) {
            roll -= entry.weight();
            if (roll < 0) {
                return entry.state();
            }
        }
        return config.palette().get(0).state();
    }
}
