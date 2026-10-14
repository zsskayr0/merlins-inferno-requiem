package dev.zsskayr.merlins_inferno.worldgen.feature;

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

import dev.zsskayr.merlins_inferno.registry.ModBlocks;

/**
 * A half-ellipse arch standing in a vertical plane, its legs run down to the ground. Used for the Bone Marsh's giant
 * ribs (bone blocks) and the Weeping Ruins' broken gateways (blackstone bricks, with Black Vines hanging under them).
 */
public class ArchFeature extends Feature<ArchFeature.Config> {
    public record Config(BlockState state, BlockState altState, float altChance, int minWidth, int maxWidth, int minHeight,
            int maxHeight, int thickness, float vineChance) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockState.CODEC.fieldOf("state").forGetter(Config::state),
                BlockState.CODEC.fieldOf("alt_state").forGetter(Config::altState),
                Codec.floatRange(0.0F, 1.0F).fieldOf("alt_chance").forGetter(Config::altChance),
                Codec.intRange(3, 40).fieldOf("min_width").forGetter(Config::minWidth),
                Codec.intRange(3, 40).fieldOf("max_width").forGetter(Config::maxWidth),
                Codec.intRange(3, 40).fieldOf("min_height").forGetter(Config::minHeight),
                Codec.intRange(3, 40).fieldOf("max_height").forGetter(Config::maxHeight),
                Codec.intRange(1, 3).fieldOf("thickness").forGetter(Config::thickness),
                Codec.floatRange(0.0F, 1.0F).fieldOf("vine_chance").forGetter(Config::vineChance)
        ).apply(instance, Config::new));
    }

    public ArchFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        Config config = context.config();
        if (level.getBlockState(origin.below()).isAir() || !level.getBlockState(origin).canBeReplaced()) {
            return false;
        }
        int width = config.minWidth() + random.nextInt(Math.max(1, config.maxWidth() - config.minWidth() + 1));
        int height = config.minHeight() + random.nextInt(Math.max(1, config.maxHeight() - config.minHeight() + 1));
        Direction along = random.nextBoolean() ? Direction.EAST : Direction.SOUTH;
        Direction across = along.getClockWise();
        double a = width / 2.0, b = height;
        int steps = width * 3;
        for (int i = 0; i <= steps; i++) {
            double angle = Math.PI * i / steps;
            double x = Math.cos(angle) * a, y = Math.sin(angle) * b;
            for (int t = 0; t < config.thickness(); t++) {
                BlockPos top = origin.relative(along, (int) Math.round(x)).relative(across, t).above((int) Math.round(y));
                put(level, top, config, random);
                if (i == 0 || i == steps) {
                    for (int d = 1; d < height + 2 && level.getBlockState(top.below(d)).isAir(); d++) {
                        put(level, top.below(d), config, random);
                    }
                } else if (config.vineChance() > 0 && random.nextFloat() < config.vineChance()
                        && level.getBlockState(top.below()).isAir()) {
                    hangVines(level, top.below(), random);
                }
            }
        }
        // the legs: from the arch's ends down to the ground
        for (int end = 0; end < 2; end++) {
            for (int t = 0; t < config.thickness(); t++) {
                BlockPos base = origin.relative(along, end == 0 ? (int) Math.round(a) : -(int) Math.round(a)).relative(across, t);
                for (int d = 0; d < 12 && (level.getBlockState(base.below(d)).isAir() || level.getBlockState(base.below(d)).canBeReplaced()); d++) {
                    put(level, base.below(d), config, random);
                }
            }
        }
        return true;
    }

    private static void put(WorldGenLevel level, BlockPos pos, Config config, RandomSource random) {
        if (level.getBlockState(pos).canBeReplaced()) {
            level.setBlock(pos, random.nextFloat() < config.altChance() ? config.altState() : config.state(), 2);
        }
    }

    private static void hangVines(WorldGenLevel level, BlockPos start, RandomSource random) {
        int length = 1 + random.nextInt(4);
        for (int i = 0; i < length; i++) {
            BlockPos pos = start.below(i);
            if (!level.getBlockState(pos).isAir()) {
                return;
            }
            boolean last = i == length - 1 || !level.getBlockState(pos.below()).isAir();
            level.setBlock(pos, (last ? ModBlocks.BLACK_VINES.get() : ModBlocks.BLACK_VINES_PLANT.get()).defaultBlockState(), 2);
            if (last) {
                return;
            }
        }
    }
}
