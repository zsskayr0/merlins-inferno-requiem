package dev.zsskayr.merlins_inferno.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.AndrasEntity;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModStructurePieceTypes;

/**
 * The Ancient Battlefield, Andras' lair: a floating island of netherrack pasted from the hand-built
 * {@code ancient_battlefield} template (converted from the author's WorldEdit schematic by
 * {@code tools/schem_to_structure.py}, which also wires every chest to its loot table). What the template holds:
 * <ul>
 *     <li>the battlefield itself - rubble piles with 10 chests ({@code andras_ruins}) and four Starved spawners,
 *     an Imp spawner in the middle, and a ritual circle of flesh and compressed netherrack;</li>
 *     <li>the glass-roofed dome at the circle's centre, with the giant sword hanging over it. The sword's core is
 *     Corrupted Obsidian: it is the portal Andras' key opens (decorative for now - see the block's javadoc);</li>
 *     <li>one chest behind the sword ({@code andras_sword_chest}), and under the dome a sealed skull chamber with
 *     eight double chests ({@code andras_vault}) around a pit of lava.</li>
 * </ul>
 * The template's y = 0 is the underside of the island; its top surface is {@link #SURFACE_Y} above that, and only
 * the top six layers of its rim show above the lava it floats in. The
 * template carries only the air that sits inside the island's own volume (chambers, gaps), so pasting it leaves
 * the surrounding Nether alone. On top of the paste this piece adds Andras himself, standing on the dome under the
 * sword - a template can't hold entities, and he must be there exactly once, so it is tracked in {@link #spawned}.
 */
public class AncientBattlefieldPiece extends TemplateStructurePiece {
    public static final ResourceLocation TEMPLATE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "ancient_battlefield");

    /** Template-local Y of the island's top surface (the battlefield floor). */
    public static final int SURFACE_Y = 25;
    /**
     * Template-local Y of the last lava block of the sea the island sits in: everything up to here is submerged, so
     * the {@code SURFACE_Y - LAVA_TOP_LOCAL_Y} = 6 layers above it are the exposed sides. The structure places the
     * template so this layer lines up with the world's actual lava sea (see {@link AncientBattlefieldStructure}).
     */
    public static final int LAVA_TOP_LOCAL_Y = 19;

    /** Where Andras stands, template-local: on the dome's glass roof, straight under the sword. */
    private static final int ANDRAS_X = 40, ANDRAS_Y = 26, ANDRAS_Z = 21;
    /** How far he may wander from his spot - the dome and the circle around it. */
    private static final int ANDRAS_LEASH = 24;

    private boolean spawned;

    public AncientBattlefieldPiece(StructureTemplateManager templateManager, BlockPos pos) {
        super(ModStructurePieceTypes.ANCIENT_BATTLEFIELD.get(), 0, templateManager, TEMPLATE, TEMPLATE.toString(), makeSettings(), pos);
    }

    public AncientBattlefieldPiece(StructureTemplateManager templateManager, CompoundTag tag) {
        super(ModStructurePieceTypes.ANCIENT_BATTLEFIELD.get(), tag, templateManager, location -> makeSettings());
        this.spawned = tag.getBoolean("AndrasSpawned");
    }

    private static StructurePlaceSettings makeSettings() {
        // No ignore-air processor: the template's interior air is what carves the chambers out of the terrain.
        return new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(true);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putBoolean("AndrasSpawned", this.spawned);
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
        // No data markers in this template.
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
        super.postProcess(level, structureManager, generator, random, chunkBox, chunkPos, pos);
        this.logPlacement(level, chunkBox, chunkPos);
        this.spawnAndras(level, chunkBox);
    }

    /** TEMPORARY diagnostic: how many of the template's nether bricks inside this chunk really ended up in the world. */
    private void logPlacement(WorldGenLevel level, BoundingBox chunkBox, ChunkPos chunkPos) {
        try {
            java.util.List<net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo> bricks =
                    this.template.filterBlocks(this.templatePosition, this.placeSettings, net.minecraft.world.level.block.Blocks.NETHER_BRICKS);
            int expected = 0;
            int found = 0;
            net.minecraft.world.level.block.state.BlockState sample = null;
            for (var info : bricks) {
                if (chunkBox.isInside(info.pos())) {
                    expected++;
                    net.minecraft.world.level.block.state.BlockState state = level.getBlockState(info.pos());
                    if (state.is(net.minecraft.world.level.block.Blocks.NETHER_BRICKS)) {
                        found++;
                    } else if (sample == null) {
                        sample = state;
                    }
                }
            }
            Merlins_inferno.LOGGER.info("[battlefield-diag] chunk {} templatePos {} pieceBB {} chunkBox {} templateSize {} allBricksInfos {} expectedInChunk {} foundInWorld {} sampleWorldState {}",
                    chunkPos, this.templatePosition, this.boundingBox, chunkBox, this.template.getSize(), bricks.size(), expected, found, sample);
        } catch (RuntimeException e) {
            Merlins_inferno.LOGGER.error("[battlefield-diag] failed", e);
        }
    }

    /** Andras himself, spawned once, and the only place he ever appears naturally. */
    private void spawnAndras(WorldGenLevel level, BoundingBox chunkBox) {
        BlockPos p = this.templatePosition.offset(ANDRAS_X, ANDRAS_Y, ANDRAS_Z);
        if (this.spawned || !chunkBox.isInside(p)) {
            return;
        }
        this.spawned = true;
        AndrasEntity andras = ModEntityTypes.ANDRAS.get().create(level.getLevel());
        if (andras == null) {
            return;
        }
        andras.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 180.0F, 0.0F);
        andras.setPersistenceRequired();
        andras.restrictTo(p, ANDRAS_LEASH);
        andras.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null);
        level.addFreshEntityWithPassengers(andras);
    }
}
