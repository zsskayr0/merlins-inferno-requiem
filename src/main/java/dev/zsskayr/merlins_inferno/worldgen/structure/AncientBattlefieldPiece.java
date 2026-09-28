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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

    /** The dome of air cut over the island: centred on the island's middle (template-local), radius and height in blocks. */
    private static final int DOME_CENTER_X = 40, DOME_CENTER_Z = 40, DOME_RADIUS = 38, DOME_HEIGHT = 56;

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
        this.clearDome(level, chunkBox);
        super.postProcess(level, structureManager, generator, random, chunkBox, chunkPos, pos);
        this.spawnAndras(level, chunkBox);
    }

    /**
     * Cuts a hard-edged dome of air over the island (the template is pasted right after, over the top of it), so the
     * Nether's own terrain, fungus trees and ceiling can never swallow the giant sword: everything inside an
     * ellipsoid centred on the island's middle, from its surface up, is removed. It has to reach ~42 layers above the
     * surface at the sword, which hangs 19 blocks off-centre, hence the tall vertical radius.
     */
    private void clearDome(WorldGenLevel level, BoundingBox chunkBox) {
        int cx = this.templatePosition.getX() + DOME_CENTER_X;
        int cz = this.templatePosition.getZ() + DOME_CENTER_Z;
        int baseY = this.templatePosition.getY() + SURFACE_Y + 1;
        int minX = Math.max(chunkBox.minX(), cx - DOME_RADIUS);
        int maxX = Math.min(chunkBox.maxX(), cx + DOME_RADIUS);
        int minZ = Math.max(chunkBox.minZ(), cz - DOME_RADIUS);
        int maxZ = Math.min(chunkBox.maxZ(), cz + DOME_RADIUS);
        int topY = Math.min(chunkBox.maxY(), baseY + DOME_HEIGHT);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double horizontal = ((double) (x - cx) * (x - cx) + (double) (z - cz) * (z - cz)) / ((double) DOME_RADIUS * DOME_RADIUS);
                if (horizontal >= 1.0) {
                    continue;
                }
                int height = (int) Math.floor(DOME_HEIGHT * Math.sqrt(1.0 - horizontal));
                for (int y = baseY; y <= Math.min(topY, baseY + height); y++) {
                    p.set(x, y, z);
                    if (!level.getBlockState(p).isAir()) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
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
