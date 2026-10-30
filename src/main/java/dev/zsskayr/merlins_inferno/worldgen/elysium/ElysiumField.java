package dev.zsskayr.merlins_inferno.worldgen.elysium;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * A density function over {@link ElysiumTerrain}: the final terrain density, or one of the two climate axes the
 * {@code multi_noise} biome source reads. Carries the {@code elysium_seed} noise only to learn the world's seed.
 */
public final class ElysiumField implements DensityFunction.SimpleFunction {
    public enum Kind implements StringRepresentable {
        DENSITY("density"), TEMPERATURE("temperature"), HUMIDITY("humidity");

        public static final com.mojang.serialization.Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        private final String name;

        Kind(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final MapCodec<ElysiumField> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            DensityFunction.NoiseHolder.CODEC.fieldOf("noise").forGetter(f -> f.noise),
            Kind.CODEC.fieldOf("kind").forGetter(f -> f.kind)).apply(i, ElysiumField::new));
    public static final KeyDispatchDataCodec<ElysiumField> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    /** The column last sampled on this thread, shared by every field of the same terrain: the interpolator walks y first. */
    private static final class Memo {
        ElysiumTerrain terrain;
        final ElysiumTerrain.Column column = new ElysiumTerrain.Column();
    }

    private static final ThreadLocal<Memo> MEMO = ThreadLocal.withInitial(Memo::new);

    private final DensityFunction.NoiseHolder noise;
    private final Kind kind;
    private final ElysiumTerrain terrain;

    public ElysiumField(DensityFunction.NoiseHolder noise, Kind kind) {
        this.noise = noise;
        this.kind = kind;
        this.terrain = noise.noise() == null ? null : ElysiumTerrains.forNoise(noise.noise());
    }

    @Override
    public double compute(DensityFunction.FunctionContext ctx) {
        ElysiumTerrain t = terrain != null ? terrain : ElysiumTerrains.forNoise(null);
        int x = ctx.blockX(), z = ctx.blockZ();
        switch (kind) {
            case TEMPERATURE:
                return t.temperatureAt(x, z);
            case HUMIDITY:
                return t.humidityAt(x, z);
            default:
                Memo m = MEMO.get();
                if (m.terrain != t || m.column.x != x || m.column.z != z) {
                    m.terrain = t;
                    t.sample(x, z, m.column);
                }
                return ElysiumTerrain.density(m.column, ctx.blockY());
        }
    }

    @Override
    public DensityFunction mapAll(DensityFunction.Visitor visitor) {
        return visitor.apply(new ElysiumField(visitor.visitNoise(noise), kind));
    }

    @Override
    public double minValue() {
        return -1.0;
    }

    @Override
    public double maxValue() {
        return 1.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
