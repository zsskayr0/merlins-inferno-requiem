package dev.zsskayr.merlins_inferno.worldgen.structure;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import dev.zsskayr.merlins_inferno.registry.ModStructureTypes;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumChunkGenerator;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumWorld;

/**
 * The Giant Geode as a {@code Structure} (so {@code /locate structure merlins_inferno:giant_geode} finds it): a 31-block geode of
 * Amethyst, Lyrium and Luminite inside a shell of Purified Obsidian, sunk into an El'ysium island so only its crown shows. Rare - see the
 * structure set. A site needs an island broad and deep enough to bury it: the ground at the centre and four points around it must be
 * within {@link #MAX_UNEVENNESS} of each other and solid {@link #DEPTH} blocks down.
 */
public class GiantGeodeStructure extends Structure {
    public static final MapCodec<GiantGeodeStructure> CODEC = simpleCodec(GiantGeodeStructure::new);

    /** How much of the geode's height shows above the ground: the rest is buried. */
    private static final int CROWN = 10;
    private static final int REACH = 12;
    private static final int MAX_UNEVENNESS = 5;
    private static final int DEPTH = 18;

    public GiantGeodeStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        ChunkGenerator generator = context.chunkGenerator();
        if (!(generator instanceof ElysiumChunkGenerator)) {
            return Optional.empty();
        }
        StructureTemplateManager templateManager = context.structureTemplateManager();
        Vec3i size = templateManager.getOrCreate(GiantGeodePiece.TEMPLATE).getSize();

        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();
        int surface = height(context, x, z);
        for (int[] d : new int[][] {{REACH, 0}, {-REACH, 0}, {0, REACH}, {0, -REACH}}) {
            if (Math.abs(height(context, x + d[0], z + d[1]) - surface) > MAX_UNEVENNESS) {
                return Optional.empty();
            }
        }
        if (surface <= ElysiumWorld.LOWEST_SOLID) {
            return Optional.empty();
        }
        NoiseColumn column = generator.getBaseColumn(x, z, context.heightAccessor(), context.randomState());
        if (column.getBlock(surface - DEPTH).isAir()) {
            return Optional.empty();
        }

        BlockPos origin = new BlockPos(x - size.getX() / 2, surface - (size.getY() - CROWN), z - size.getZ() / 2);
        return Optional.of(new Structure.GenerationStub(origin.offset(size.getX() / 2, size.getY() / 2, size.getZ() / 2),
                builder -> builder.addPiece(new GiantGeodePiece(templateManager, origin))));
    }

    private static int height(Structure.GenerationContext context, int x, int z) {
        return context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.GIANT_GEODE.get();
    }
}
