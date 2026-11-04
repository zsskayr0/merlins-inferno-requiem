package dev.zsskayr.merlins_inferno.worldgen.structure;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import dev.zsskayr.merlins_inferno.registry.ModStructureTypes;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumChunkGenerator;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumSeed;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/** A vanilla-locatable structure whose piece uses the planned cliff geode from El'ysium's terrain core. */
public final class GeodeFortStructure extends Structure {
    public static final MapCodec<GeodeFortStructure> CODEC = simpleCodec(GeodeFortStructure::new);

    public GeodeFortStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!(context.chunkGenerator() instanceof ElysiumChunkGenerator)) {
            return Optional.empty();
        }

        // The structure set has one candidate at the origin of each 4x4-chunk region.
        // The island planner chooses the actual cliff position within that region.
        ChunkPos candidate = context.chunkPos();
        int minX = candidate.getMinBlockX();
        int minZ = candidate.getMinBlockZ();
        int maxX = minX + 63;
        int maxZ = minZ + 63;
        for (ElysiumWorld.Site site : ElysiumSeed.world().geodeFortsIn(minX, minZ, maxX, maxZ)) {
            if (site.x() >= minX && site.x() <= maxX && site.z() >= minZ && site.z() <= maxZ) {
                BlockPos center = new BlockPos(site.x(), site.y(), site.z());
                return Optional.of(new GenerationStub(center, builder -> builder.addPiece(new GeodeFortPiece(site))));
            }
        }
        return Optional.empty();
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.GEODE_FORT.get();
    }
}
