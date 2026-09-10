package dev.zsskayr.merlins_inferno.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import dev.zsskayr.merlins_inferno.registry.ModStructurePieceTypes;

/**
 * Pastes one of the 4 pre-rotated Rowanwood tree NBT files (see {@code RowanwoodTreeStructure}) -
 * {@link TemplateStructurePiece}'s default {@code postProcess} already does the actual placement,
 * so there's nothing to override here beyond the two constructors it needs (one for first-time
 * generation, one to reconstruct the piece from saved NBT on chunk reload) and the no-op data
 * marker hook (this structure has no data marker or jigsaw blocks in it).
 * <p>
 * Rotation is NOT applied here (always {@link Rotation#NONE}) - like Ashwood, each orientation is
 * its own baked file, chosen by {@code RowanwoodTreeStructure} before this piece is even
 * constructed, so the resource location alone (persisted as the "Template" tag by the base class)
 * is enough to reconstruct this piece identically after a chunk reload.
 */
public class RowanwoodTreePiece extends TemplateStructurePiece {
    public RowanwoodTreePiece(StructureTemplateManager structureTemplateManager, ResourceLocation location, BlockPos pos) {
        super(ModStructurePieceTypes.ROWANWOOD_TREE.get(), 0, structureTemplateManager, location, location.toString(), makeSettings(), pos);
    }

    public RowanwoodTreePiece(StructureTemplateManager structureTemplateManager, CompoundTag tag) {
        super(ModStructurePieceTypes.ROWANWOOD_TREE.get(), tag, structureTemplateManager, location -> makeSettings());
    }

    private static StructurePlaceSettings makeSettings() {
        return new StructurePlaceSettings()
                .setRotation(Rotation.NONE)
                .setMirror(Mirror.NONE)
                .setIgnoreEntities(true)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
        // No data markers in this structure.
    }
}
