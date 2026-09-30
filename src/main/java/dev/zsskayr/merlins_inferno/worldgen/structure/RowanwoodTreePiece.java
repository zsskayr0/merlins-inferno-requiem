package dev.zsskayr.merlins_inferno.worldgen.structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import dev.zsskayr.merlins_inferno.block.EdenweedBushBlock;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
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
    /** Trunk logs up to this many blocks above the tree's base count as "roots". */
    private static final int BASE_HEIGHT = 2;
    /** How far from a root log a shrub may grow. */
    private static final int SPREAD = 3;
    /** Shrubs per tree, at most. */
    private static final int BUSHES = 7;
    private static final int SCAN_UP = 6;
    private static final int SCAN_DOWN = 8;

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

    /**
     * After the tree is pasted, scatters Edenweed shrubs on the ground around its trunk's base - "at the roots". The spots
     * are picked from the base logs of the template with a random seeded on the piece position, so every chunk the tree
     * spans agrees on them and each only plants the ones inside its own box.
     */
    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
            BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        super.postProcess(level, structureManager, generator, random, box, chunkPos, pos);

        Set<BlockPos> roots = new LinkedHashSet<>();
        for (StructureTemplate.StructureBlockInfo info : this.template.filterBlocks(this.templatePosition, makeSettings(), ModBlocks.ROWANWOOD_LOG.get())) {
            if (info.pos().getY() - this.templatePosition.getY() <= BASE_HEIGHT) {
                for (int dx = -SPREAD; dx <= SPREAD; dx++) {
                    for (int dz = -SPREAD; dz <= SPREAD; dz++) {
                        roots.add(new BlockPos(info.pos().getX() + dx, this.templatePosition.getY(), info.pos().getZ() + dz));
                    }
                }
            }
        }
        if (roots.isEmpty()) {
            return;
        }
        List<BlockPos> spots = new ArrayList<>(roots);
        Collections.shuffle(spots, new java.util.Random(this.templatePosition.asLong()));

        int planted = 0;
        for (BlockPos spot : spots) {
            if (planted >= BUSHES || !box.isInside(spot)) {
                continue;
            }
            for (int y = spot.getY() + SCAN_UP; y >= spot.getY() - SCAN_DOWN; y--) {
                BlockPos at = new BlockPos(spot.getX(), y, spot.getZ());
                if (level.getBlockState(at.below()).is(BlockTags.DIRT) && level.getBlockState(at).isAir() && level.getBlockState(at.above()).isAir()) {
                    level.setBlock(at, ModBlocks.EDENWEED_BUSH.get().defaultBlockState()
                            .setValue(EdenweedBushBlock.AGE, 2 + random.nextInt(2)), Block.UPDATE_CLIENTS);
                    planted++;
                    break;
                }
            }
        }
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
        // No data markers in this structure.
    }
}
