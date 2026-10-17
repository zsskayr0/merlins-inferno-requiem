package dev.zsskayr.merlins_inferno.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * The Haunted Forest tree: a thick, twisting trunk that flares into roots at its foot, a few long branches near the
 * top, and a wide canopy of leaves from which weeping strands hang. Written for this mod; the look follows the
 * "lament" trees of Oh The Biomes You'll Go (used with the author's permission).
 */
public class HauntedTreeFeature extends Feature<HauntedTreeFeature.Config> {
    public record Config(BlockState log, BlockState leaves, BlockState decor, float decorChance, int minHeight, int maxHeight) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockState.CODEC.fieldOf("log").forGetter(Config::log),
                BlockState.CODEC.fieldOf("leaves").forGetter(Config::leaves),
                BlockState.CODEC.fieldOf("decor").forGetter(Config::decor),
                Codec.floatRange(0.0F, 1.0F).fieldOf("decor_chance").forGetter(Config::decorChance),
                Codec.intRange(6, 40).fieldOf("min_height").forGetter(Config::minHeight),
                Codec.intRange(6, 40).fieldOf("max_height").forGetter(Config::maxHeight)
        ).apply(instance, Config::new));
    }

    public HauntedTreeFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        Config config = context.config();
        BlockPos groundPos = origin.below();
        if (!level.getBlockState(groundPos).isFaceSturdy(level, groundPos, Direction.UP)
                || !level.getBlockState(origin).canBeReplaced()) {
            return false;
        }
        int height = config.minHeight() + random.nextInt(Math.max(1, config.maxHeight() - config.minHeight() + 1));
        height = Math.min(height, level.getMaxBuildHeight() - origin.getY() - 10);
        if (height < config.minHeight()) {
            return false;
        }
        // clear room for the trunk
        for (int y = 0; y < Math.min(height, 6); y++) {
            if (!level.getBlockState(origin.above(y)).canBeReplaced()) {
                return false;
            }
        }
        BlockState upright = config.log();
        // roots: the foot splays out and down in four to six directions
        int roots = 4 + random.nextInt(3);
        for (int i = 0; i < roots; i++) {
            double angle = Math.PI * 2 * i / roots + random.nextDouble() * 0.5;
            int length = 3 + random.nextInt(3);
            for (int step = 1; step <= length; step++) {
                BlockPos pos = origin.offset((int) Math.round(Math.cos(angle) * step), 1 - step / 2, (int) Math.round(Math.sin(angle) * step));
                putLog(level, origin, pos, upright, Direction.Axis.Y);
                for (int d = 1; d <= 3 && level.getBlockState(pos.below(d)).canBeReplaced(); d++) {
                    putLog(level, origin, pos.below(d), upright, Direction.Axis.Y);
                }
            }
        }
        // trunk: 2x2 for the lower third, then a single column that drifts sideways now and then
        BlockPos pos = origin;
        int thick = height / 3;
        for (int y = 0; y < height; y++) {
            putLog(level, origin, pos, upright, Direction.Axis.Y);
            if (y < thick) {
                putLog(level, origin, pos.east(), upright, Direction.Axis.Y);
                putLog(level, origin, pos.south(), upright, Direction.Axis.Y);
                putLog(level, origin, pos.east().south(), upright, Direction.Axis.Y);
            }
            if (y > 4 && random.nextInt(4) == 0) {
                pos = pos.relative(Direction.Plane.HORIZONTAL.getRandomDirection(random));
                putLog(level, origin, pos, upright, Direction.Axis.Y);
            }
            pos = pos.above();
        }
        BlockPos top = pos;
        shelves(level, origin, height, random);
        // branches from the upper third, each ending in a canopy lobe
        int branches = 3 + random.nextInt(3);
        for (int i = 0; i < branches; i++) {
            Direction out = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos cursor = top.below(random.nextInt(Math.max(1, height / 4)));
            int run = 4 + random.nextInt(5);
            for (int step = 0; step < run; step++) {
                cursor = cursor.relative(out);
                if (step % 2 == 1) {
                    cursor = cursor.above();
                }
                if (random.nextInt(3) == 0) {
                    cursor = cursor.relative(out.getClockWise());
                }
                putLog(level, origin, cursor, upright, out.getAxis());
            }
            lobe(level, origin, cursor, config, random, 3 + random.nextInt(2));
        }
        lobe(level, origin, top, config, random, 4);
        return true;
    }

    /** Bracket fungi cling to the trunk's flanks, on the lower half of the tree. */
    private static void shelves(WorldGenLevel level, BlockPos origin, int height, RandomSource random) {
        BlockState shroom = dev.zsskayr.merlins_inferno.registry.ModBlocks.HAUNTED_SHELF_SHROOM.get().defaultBlockState();
        int attempts = 6 + random.nextInt(6);
        for (int i = 0; i < attempts; i++) {
            BlockPos pos = origin.offset(random.nextInt(5) - 2, 1 + random.nextInt(Math.max(1, height * 2 / 3)), random.nextInt(5) - 2);
            Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockState placed = shroom.setValue(dev.zsskayr.merlins_inferno.block.ShelfShroomBlock.FACING, facing);
            if (level.getBlockState(pos).isAir() && placed.canSurvive(level, pos)
                    && level.getBlockState(pos.relative(facing.getOpposite())).is(dev.zsskayr.merlins_inferno.registry.ModBlocks.HAUNTED_LOG.get())) {
                level.setBlock(pos, placed, 2);
            }
        }
    }

    private static void lobe(WorldGenLevel level, BlockPos origin, BlockPos centre, Config config, RandomSource random, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double d = (dx * dx + dz * dz) / (double) (radius * radius) + dy * dy / 5.0;
                    if (d > 1.0 || (d > 0.7 && random.nextInt(3) == 0)) {
                        continue;
                    }
                    BlockPos pos = centre.offset(dx, dy, dz);
                    if (inRegion(origin, pos) && level.getBlockState(pos).canBeReplaced()) {
                        // the underside of the canopy carries the glowing fruit
                        boolean underside = dy <= -1 && level.getBlockState(pos.below()).isAir();
                        level.setBlock(pos, underside && random.nextFloat() < config.decorChance() ? config.decor() : config.leaves(), 2);
                    }
                }
            }
        }
        // weeping strands from the underside
        for (int i = 0; i < radius * 3; i++) {
            BlockPos start = centre.offset(random.nextInt(radius * 2 + 1) - radius, -2, random.nextInt(radius * 2 + 1) - radius);
            int length = 2 + random.nextInt(6);
            for (int y = 0; y < length; y++) {
                BlockPos pos = start.below(y);
                if (!inRegion(origin, pos) || !level.getBlockState(pos).canBeReplaced()) {
                    break;
                }
                boolean tip = y == length - 1 && random.nextFloat() < config.decorChance() * 2;
                level.setBlock(pos, tip ? config.decor() : config.leaves(), 2);
            }
        }
    }

    /** Worldgen may only touch the origin chunk and its neighbours; anything further out stalls chunk generation. */
    private static boolean inRegion(BlockPos origin, BlockPos pos) {
        int cx = origin.getX() >> 4;
        int cz = origin.getZ() >> 4;
        int px = pos.getX() >> 4;
        int pz = pos.getZ() >> 4;
        return Math.abs(px - cx) <= 1 && Math.abs(pz - cz) <= 1;
    }

    private static void putLog(WorldGenLevel level, BlockPos origin, BlockPos pos, BlockState log, Direction.Axis axis) {
        if (inRegion(origin, pos) && level.getBlockState(pos).canBeReplaced()) {
            level.setBlock(pos, log.hasProperty(RotatedPillarBlock.AXIS) ? log.setValue(RotatedPillarBlock.AXIS, axis) : log, 2);
        }
    }
}
