package dev.zsskayr.merlins_inferno.worldgen.structure;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import dev.zsskayr.merlins_inferno.registry.ModStructurePieceTypes;

/**
 * Invisible companion of the Ancient Battlefield: a wider box around the island whose only job is to strip the
 * Nether's fungus trees (stems, wart blocks, shroomlight, vines) from the air above the island's surface level.
 * The dome cut by {@link AncientBattlefieldPiece} removes whatever stands inside it, and would leave the caps of the
 * trees it cut hanging in mid-air just outside its edge; this piece reaches well past that edge, so nothing of a tree
 * that touched the dome survives. It only ever removes tree blocks, never terrain.
 */
public class AncientBattlefieldScrubPiece extends StructurePiece {
    /** How far from the island's centre (blocks) trees are stripped: the dome's radius plus a margin. */
    public static final int RADIUS = 56;

    private static final Set<Block> TREE_BLOCKS = Set.of(
            Blocks.CRIMSON_STEM, Blocks.STRIPPED_CRIMSON_STEM, Blocks.CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_HYPHAE,
            Blocks.WARPED_STEM, Blocks.STRIPPED_WARPED_STEM, Blocks.WARPED_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE,
            Blocks.NETHER_WART_BLOCK, Blocks.WARPED_WART_BLOCK, Blocks.SHROOMLIGHT,
            Blocks.WEEPING_VINES, Blocks.WEEPING_VINES_PLANT, Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT);

    private final int centerX;
    private final int centerZ;
    private final int baseY;

    public AncientBattlefieldScrubPiece(int centerX, int centerZ, int baseY, int maxY) {
        super(ModStructurePieceTypes.ANCIENT_BATTLEFIELD_SCRUB.get(), 1,
                new BoundingBox(centerX - RADIUS, baseY, centerZ - RADIUS, centerX + RADIUS, maxY, centerZ + RADIUS));
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.baseY = baseY;
    }

    public AncientBattlefieldScrubPiece(CompoundTag tag) {
        super(ModStructurePieceTypes.ANCIENT_BATTLEFIELD_SCRUB.get(), tag);
        this.centerX = tag.getInt("CX");
        this.centerZ = tag.getInt("CZ");
        this.baseY = tag.getInt("BaseY");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CX", this.centerX);
        tag.putInt("CZ", this.centerZ);
        tag.putInt("BaseY", this.baseY);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
        int minX = Math.max(chunkBox.minX(), this.centerX - RADIUS);
        int maxX = Math.min(chunkBox.maxX(), this.centerX + RADIUS);
        int minZ = Math.max(chunkBox.minZ(), this.centerZ - RADIUS);
        int maxZ = Math.min(chunkBox.maxZ(), this.centerZ + RADIUS);
        int maxY = Math.min(chunkBox.maxY(), this.boundingBox.maxY());
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                long dx = x - this.centerX;
                long dz = z - this.centerZ;
                if (dx * dx + dz * dz > (long) RADIUS * RADIUS) {
                    continue;
                }
                for (int y = this.baseY; y <= maxY; y++) {
                    p.set(x, y, z);
                    if (TREE_BLOCKS.contains(level.getBlockState(p).getBlock())) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }
}
