package dev.zsskayr.merlins_inferno.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * A tall, tapering spire that rises from the ground to a point, leaning a little as it climbs. {@code accent_state}
 * shows through the stone here and there (the Ebony Spires' veins of ebony debris).
 */
public class SpireFeature extends Feature<SpireFeature.Config> {
    public record Config(BlockState state, BlockState accent, int minHeight, int maxHeight, int baseRadius, float accentChance)
            implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockState.CODEC.fieldOf("state").forGetter(Config::state),
                BlockState.CODEC.fieldOf("accent_state").forGetter(Config::accent),
                Codec.intRange(4, 96).fieldOf("min_height").forGetter(Config::minHeight),
                Codec.intRange(4, 96).fieldOf("max_height").forGetter(Config::maxHeight),
                Codec.intRange(1, 8).fieldOf("base_radius").forGetter(Config::baseRadius),
                Codec.floatRange(0.0F, 1.0F).fieldOf("accent_chance").forGetter(Config::accentChance)
        ).apply(instance, Config::new));
    }

    public SpireFeature(Codec<Config> codec) {
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
        int height = config.minHeight() + random.nextInt(Math.max(1, config.maxHeight() - config.minHeight() + 1));
        height = Math.min(height, level.getMaxBuildHeight() - origin.getY() - 2);
        if (height < config.minHeight()) {
            return false;
        }
        // the whole footprint must stand on ground: at least 7 of 9 sample points (centre + ring) over solid blocks
        int grounded = 0;
        int ring = Math.max(1, config.baseRadius() - 1);
        for (int sx = -1; sx <= 1; sx++) {
            for (int sz = -1; sz <= 1; sz++) {
                BlockPos below = origin.offset(sx * ring, -1, sz * ring);
                if (level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) {
                    grounded++;
                }
            }
        }
        if (grounded < 7) {
            return false;
        }
        double cx = origin.getX(), cz = origin.getZ();
        double leanX = (random.nextDouble() - 0.5) * 0.35, leanZ = (random.nextDouble() - 0.5) * 0.35;
        for (int y = -2; y < height; y++) {
            double t = Math.max(0.0, (double) y / height);
            double radius = config.baseRadius() * Math.pow(1.0 - t, 1.25) + 0.4;
            int reach = (int) Math.ceil(radius);
            int centreX = (int) Math.round(cx + leanX * Math.max(0, y)), centreZ = (int) Math.round(cz + leanZ * Math.max(0, y));
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    if (dx * dx + dz * dz > radius * radius) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(centreX + dx, origin.getY() + y, centreZ + dz);
                    if (y < 0 ? level.getBlockState(pos).isAir() : !level.getBlockState(pos).canBeReplaced()) {
                        continue;
                    }
                    // The accent stays buried: only blocks at least two deep inside the spire's skin can carry it,
                    // and never the top of the spire.
                    boolean buried = y >= 0 && y < height - 3 && dx * dx + dz * dz <= (radius - 1.6) * (radius - 1.6);
                    boolean accent = buried && random.nextFloat() < config.accentChance();
                    level.setBlock(pos, accent ? config.accent() : config.state(), 2);
                }
            }
        }
        return true;
    }
}
