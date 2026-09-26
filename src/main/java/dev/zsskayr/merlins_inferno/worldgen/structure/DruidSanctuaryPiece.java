package dev.zsskayr.merlins_inferno.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.DruidEntity;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModStructurePieceTypes;

/**
 * The Druid's sanctuary (design doc, 4.3): a small round clearing in the Hallowed Grove - a ring
 * of six Ashwood pillars, a plank-cross hearth with a lit campfire at the centre, a chest with rare loot (one of
 * the places the Druid's Touch enchantment can turn up) and, as a guaranteed spawn, a Druid that
 * stays near its home.
 * <p>
 * Built procedurally rather than pasted from an NBT template. Because {@code postProcess} runs
 * once per overlapping chunk, every "random" choice is derived from a stored seed and the block's
 * own coordinates ({@link #roll}) - never from the {@code RandomSource} passed in - so the
 * clearing looks the same whichever chunk places a given block. Nothing here is relative to a
 * rotation: the layout is radially symmetric.
 */
public class DruidSanctuaryPiece extends StructurePiece {
    public static final int RADIUS = 7;
    /** How far the box reaches below the floor (for the dirt fill) and above it (for clearing). */
    public static final int DEPTH_BELOW = 4;
    public static final int HEIGHT_ABOVE = 10;

    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "chests/druid_sanctuary"));

    private final long seed;
    private final BlockPos center; // the floor's centre block (top surface of the ground)
    private boolean spawnedDruid;

    public DruidSanctuaryPiece(BoundingBox box, long seed, BlockPos center) {
        super(ModStructurePieceTypes.DRUID_SANCTUARY.get(), 0, box);
        this.seed = seed;
        this.center = center;
    }

    public DruidSanctuaryPiece(CompoundTag tag) {
        super(ModStructurePieceTypes.DRUID_SANCTUARY.get(), tag);
        this.seed = tag.getLong("Seed");
        this.center = new BlockPos(tag.getInt("CX"), tag.getInt("CY"), tag.getInt("CZ"));
        this.spawnedDruid = tag.getBoolean("SpawnedDruid");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLong("Seed", this.seed);
        tag.putInt("CX", this.center.getX());
        tag.putInt("CY", this.center.getY());
        tag.putInt("CZ", this.center.getZ());
        tag.putBoolean("SpawnedDruid", this.spawnedDruid);
    }

    /** Deterministic 0..1 value for one column + salt, independent of which chunk is being generated. */
    private double roll(int x, int z, int salt) {
        long h = this.seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) * (1.0 / (1L << 53));
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
        int cx = this.center.getX();
        int cz = this.center.getZ();
        int floorY = this.center.getY();

        this.buildClearing(level, chunkBox, cx, cz, floorY);
        this.buildPillars(level, chunkBox, cx, cz, floorY);
        this.buildAltar(level, chunkBox, cx, cz, floorY);
        this.placeChest(level, chunkBox, random, cx + 3, floorY + 1, cz);
        this.spawnDruid(level, chunkBox, cx + 2, floorY + 1, cz + 2);
    }

    // ------------------------------------------------------------------------------------------

    private void set(WorldGenLevel level, BoundingBox chunkBox, int x, int y, int z, BlockState state) {
        BlockPos p = new BlockPos(x, y, z);
        if (chunkBox.isInside(p)) {
            level.setBlock(p, state, Block.UPDATE_CLIENTS);
        }
    }

    private void buildClearing(WorldGenLevel level, BoundingBox chunkBox, int cx, int cz, int floorY) {
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > RADIUS) {
                    continue;
                }
                int x = cx + dx;
                int z = cz + dz;
                if (!chunkBox.isInside(new BlockPos(x, floorY, z))) {
                    continue;
                }

                // Clear whatever stands above the floor (trees, uneven terrain) ...
                for (int y = floorY + 1; y <= floorY + HEIGHT_ABOVE; y++) {
                    if (!level.getBlockState(new BlockPos(x, y, z)).isAir()) {
                        level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
                // ... and build a flat floor with a mossy, mixed surface, filled down to solid ground.
                double surface = roll(x, z, 1);
                BlockState top = dist > RADIUS - 1 || surface < 0.55 ? Blocks.GRASS_BLOCK.defaultBlockState()
                        : surface < 0.75 ? Blocks.PODZOL.defaultBlockState()
                        : surface < 0.9 ? Blocks.MOSS_BLOCK.defaultBlockState()
                        : Blocks.COARSE_DIRT.defaultBlockState();
                level.setBlock(new BlockPos(x, floorY, z), top, Block.UPDATE_CLIENTS);
                for (int y = floorY - 1; y >= floorY - DEPTH_BELOW; y--) {
                    BlockPos below = new BlockPos(x, y, z);
                    BlockState existing = level.getBlockState(below);
                    if (existing.isAir() || !existing.getFluidState().isEmpty() || existing.canBeReplaced()) {
                        level.setBlock(below, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                    } else {
                        break;
                    }
                }
                // Undergrowth on the outer part of the clearing only - the middle stays open.
                if (dist > 4.5 && roll(x, z, 2) < 0.22) {
                    double kind = roll(x, z, 3);
                    BlockState plant = kind < 0.45 ? Blocks.SHORT_GRASS.defaultBlockState()
                            : kind < 0.7 ? Blocks.FERN.defaultBlockState()
                            : kind < 0.85 ? Blocks.LILY_OF_THE_VALLEY.defaultBlockState()
                            : Blocks.AZURE_BLUET.defaultBlockState();
                    if (top.is(Blocks.GRASS_BLOCK) || top.is(Blocks.PODZOL)) {
                        level.setBlock(new BlockPos(x, floorY + 1, z), plant, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    /**
     * Six pillars on a ring of radius 5: Ashwood logs (every other one stripped), each finished by
     * exactly ONE thing on top of the log, never a stack: a bare slab, a tuft of leaves, or a lit
     * candle - in that rotation around the ring.
     */
    private void buildPillars(WorldGenLevel level, BoundingBox chunkBox, int cx, int cz, int floorY) {
        for (int k = 0; k < 6; k++) {
            double angle = Math.toRadians(30.0 + k * 60.0);
            int px = cx + (int) Math.round(5.0 * Math.cos(angle));
            int pz = cz + (int) Math.round(5.0 * Math.sin(angle));
            int height = 3 + (k % 2);
            BlockState log = (k % 2 == 0 ? ModBlocks.ASHWOOD_LOG : ModBlocks.STRIPPED_ASHWOOD_LOG).get().defaultBlockState();
            for (int i = 1; i <= height; i++) {
                set(level, chunkBox, px, floorY + i, pz, log);
            }
            BlockState crown = switch (k % 3) {
                case 0 -> ModBlocks.ASHWOOD_SLAB.get().defaultBlockState();
                case 1 -> ModBlocks.ASHWOOD_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
                default -> Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true);
            };
            set(level, chunkBox, px, floorY + height + 1, pz, crown);
        }
    }

    /**
     * The hearth: a cross of planks (the centre and its four neighbours) with a slab on each of the
     * four corner cells, and a lit campfire on the middle of the cross.
     */
    private void buildAltar(WorldGenLevel level, BoundingBox chunkBox, int cx, int cz, int floorY) {
        BlockState planks = ModBlocks.ASHWOOD_PLANKS.get().defaultBlockState();
        BlockState slab = ModBlocks.ASHWOOD_SLAB.get().defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean corner = dx != 0 && dz != 0;
                set(level, chunkBox, cx + dx, floorY + 1, cz + dz, corner ? slab : planks);
            }
        }
        set(level, chunkBox, cx, floorY + 2, cz, Blocks.CAMPFIRE.defaultBlockState());
    }

    private void placeChest(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int x, int y, int z) {
        BlockPos p = new BlockPos(x, y, z);
        if (!chunkBox.isInside(p)) {
            return;
        }
        // Faces the altar (the chest sits east of it, so its front looks west).
        level.setBlock(p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST), Block.UPDATE_CLIENTS);
        RandomizableContainer.setBlockEntityLootTable(level, random, p, LOOT);
    }

    /** The guaranteed Druid - like a witch hut's witch: spawned once, persistent, tethered to the clearing. */
    private void spawnDruid(WorldGenLevel level, BoundingBox chunkBox, int x, int y, int z) {
        BlockPos p = new BlockPos(x, y, z);
        if (this.spawnedDruid || !chunkBox.isInside(p)) {
            return;
        }
        this.spawnedDruid = true;
        DruidEntity druid = ModEntityTypes.DRUID.get().create(level.getLevel());
        if (druid == null) {
            return;
        }
        druid.moveTo(x + 0.5, y, z + 0.5, (float) (roll(x, z, 9) * 360.0), 0.0F);
        druid.setPersistenceRequired();
        druid.restrictTo(this.center, RADIUS + 5);
        druid.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null);
        level.addFreshEntityWithPassengers(druid);
    }
}
