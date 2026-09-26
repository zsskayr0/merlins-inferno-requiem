package dev.zsskayr.merlins_inferno.worldgen.structure;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import dev.zsskayr.merlins_inferno.registry.ModStructureTypes;

/**
 * The Druid's sanctuary as a real {@code Structure} (so {@code /locate structure} finds it), one
 * {@link DruidSanctuaryPiece} on the centre of its start chunk. Restricted to Hallowed Grove by the
 * settings' biome set (see {@code ModStructureProvider}); spacing and the exclusion zone around
 * Rowanwood trees live in its structure set.
 * <p>
 * The floor is levelled to the average of five terrain samples, and a spot is rejected (this
 * chunk simply gets no sanctuary) if it's under water or if the ground varies by more than
 * {@link #MAX_UNEVENNESS} - the clearing's own floor fill and clearing can absorb small slopes
 * but not cliffs.
 */
public class DruidSanctuaryStructure extends Structure {
    public static final MapCodec<DruidSanctuaryStructure> CODEC = simpleCodec(DruidSanctuaryStructure::new);

    private static final int SAMPLE_OFFSET = 6;
    private static final int MAX_UNEVENNESS = 5;
    private static final int[][] SAMPLES = {{0, 0}, {SAMPLE_OFFSET, 0}, {-SAMPLE_OFFSET, 0}, {0, SAMPLE_OFFSET}, {0, -SAMPLE_OFFSET}};

    public DruidSanctuaryStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int cx = chunkPos.getMiddleBlockX();
        int cz = chunkPos.getMiddleBlockZ();
        ChunkGenerator generator = context.chunkGenerator();

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int sum = 0;
        for (int[] offset : SAMPLES) {
            int x = cx + offset[0];
            int z = cz + offset[1];
            int surface = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
            int ground = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
            if (surface != ground) {
                return Optional.empty(); // water over this column
            }
            min = Math.min(min, surface);
            max = Math.max(max, surface);
            sum += surface;
        }
        if (max - min > MAX_UNEVENNESS) {
            return Optional.empty();
        }

        // getFirstOccupiedHeight is the first FREE block above the ground, so the ground's top block is one below.
        int floorY = Math.round(sum / (float) SAMPLES.length) - 1;
        BlockPos center = new BlockPos(cx, floorY, cz);
        BoundingBox box = new BoundingBox(cx - DruidSanctuaryPiece.RADIUS, floorY - DruidSanctuaryPiece.DEPTH_BELOW, cz - DruidSanctuaryPiece.RADIUS,
                cx + DruidSanctuaryPiece.RADIUS, floorY + DruidSanctuaryPiece.HEIGHT_ABOVE, cz + DruidSanctuaryPiece.RADIUS);
        long seed = context.random().nextLong();
        return Optional.of(new Structure.GenerationStub(center, builder -> builder.addPiece(new DruidSanctuaryPiece(box, seed, center))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.DRUID_SANCTUARY.get();
    }
}
