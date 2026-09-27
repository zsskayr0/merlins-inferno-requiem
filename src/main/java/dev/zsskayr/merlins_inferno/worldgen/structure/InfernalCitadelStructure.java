package dev.zsskayr.merlins_inferno.worldgen.structure;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import dev.zsskayr.merlins_inferno.registry.ModStructureTypes;

/**
 * Andras' Citadel: a gothic-demonic tower rising out of the Nether's lava sea. Unlike this mod's other
 * structures, it doesn't sample terrain at all - like a vanilla Bastion Remnant, it always generates at the
 * same fixed height ({@link InfernalCitadelPiece#BASE_Y}), floating clear of whatever netherrack or lava is
 * actually there, with its own support pillars plunging down to meet it. One {@link InfernalCitadelPiece} per
 * chunk it starts in - restricted to the Nether by the settings' biome tag (see {@code ModStructureProvider}).
 */
public class InfernalCitadelStructure extends Structure {
    public static final MapCodec<InfernalCitadelStructure> CODEC = simpleCodec(InfernalCitadelStructure::new);

    public InfernalCitadelStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        BlockPos center = new BlockPos(chunkPos.getMiddleBlockX(), InfernalCitadelPiece.BASE_Y, chunkPos.getMiddleBlockZ());
        long seed = context.random().nextLong();
        return Optional.of(new Structure.GenerationStub(center, builder -> builder.addPiece(new InfernalCitadelPiece(seed, center))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructureTypes.INFERNAL_CITADEL.get();
    }
}
