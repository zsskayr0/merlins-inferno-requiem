package dev.zsskayr.merlins_inferno.worldgen.structure;

import dev.zsskayr.merlins_inferno.registry.ModStructurePieceTypes;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumWorld;
import dev.zsskayr.merlins_inferno.worldgen.elysium.structure.WallGeode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Saved structure piece for the exact, chunk-clipped Geode Fort block layout. */
public final class GeodeFortPiece extends StructurePiece {
    private final ElysiumWorld.Site site;

    public GeodeFortPiece(ElysiumWorld.Site site) {
        super(ModStructurePieceTypes.GEODE_FORT.get(), 0, new BoundingBox(
                site.x() - 16, site.y() - 13, site.z() - 16,
                site.x() + 16, site.y() + 13, site.z() + 16));
        this.site = site;
    }

    public GeodeFortPiece(CompoundTag tag) {
        super(ModStructurePieceTypes.GEODE_FORT.get(), tag);
        this.site = new ElysiumWorld.Site("geode_fort", tag.getInt("SiteX"), tag.getInt("SiteY"),
                tag.getInt("SiteZ"), tag.getInt("SiteRot"), tag.getLong("SiteSalt"));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("SiteX", site.x());
        tag.putInt("SiteY", site.y());
        tag.putInt("SiteZ", site.z());
        tag.putInt("SiteRot", site.rot());
        tag.putLong("SiteSalt", site.salt());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
            RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
        WallGeode.build(level, chunkPos, site);
    }
}
