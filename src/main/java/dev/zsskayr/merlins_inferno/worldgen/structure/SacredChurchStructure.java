package dev.zsskayr.merlins_inferno.worldgen.structure;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import dev.zsskayr.merlins_inferno.registry.ModStructureTypes;

/**
 * The Sacred Church as a {@code Structure} (so {@code /locate structure merlins_inferno:sacred_church} finds it):
 * one {@link SacredChurchPiece} centred on its start chunk. Restricted to mountain biomes by the settings' biome set
 * (see {@code ModStructureProvider}).
 * <p>
 * The church is 73x45, so a site is judged on twelve terrain samples over its footprint: rejected if any column is
 * under water or the ground varies by more than {@link #MAX_UNEVENNESS} (the piece fills under the church but does
 * not cut cliffs). The floor is the average of the samples.
 */
public class SacredChurchStructure extends Structure {
    public static final MapCodec<SacredChurchStructure> CODEC = simpleCodec(SacredChurchStructure::new);

    private static final int MAX_UNEVENNESS = 9;

    public SacredChurchStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        StructureTemplateManager templateManager = context.structureTemplateManager();
        Vec3i size = templateManager.getOrCreate(SacredChurchPiece.TEMPLATE).getSize();

        ChunkPos chunkPos = context.chunkPos();
        int ox = chunkPos.getMiddleBlockX() - size.getX() / 2;
        int oz = chunkPos.getMiddleBlockZ() - size.getZ() / 2;
        ChunkGenerator generator = context.chunkGenerator();

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int sum = 0;
        int count = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 3; j++) {
                int x = ox + (size.getX() - 1) * i / 3;
                int z = oz + (size.getZ() - 1) * j / 2;
                int surface = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                int ground = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
                if (surface != ground) {
                    return Optional.empty(); // water over this column
                }
                min = Math.min(min, surface);
                max = Math.max(max, surface);
                sum += surface;
                count++;
            }
        }
        if (max - min > MAX_UNEVENNESS) {
            return Optional.empty();
        }

        // getFirstOccupiedHeight is the first FREE block above the ground, so the ground's top block is one below.
        int floorY = Math.round(sum / (float) count) - 1;
        BlockPos origin = new BlockPos(ox, floorY, oz);
        long seed = context.random().nextLong();
        return Optional.of(new Structure.GenerationStub(origin.offset(size.getX() / 2, 0, size.getZ() / 2),
                builder -> builder.addPiece(new SacredChurchPiece(templateManager, seed, origin))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.SACRED_CHURCH.get();
    }
}
