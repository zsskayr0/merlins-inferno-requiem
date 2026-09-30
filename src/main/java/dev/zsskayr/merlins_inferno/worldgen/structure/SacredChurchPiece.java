package dev.zsskayr.merlins_inferno.worldgen.structure;

import java.util.Comparator;
import java.util.List;

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
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.blockentity.SacredAltarBlockEntity;
import dev.zsskayr.merlins_inferno.entity.SacredPriestEntity;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModStructurePieceTypes;

/**
 * The Sacred Church: a gothic cross-plan church (nave running west to east, transepts north and south, a raised
 * chancel to the east), pasted from the {@code sacred_church} template - a classic {@code .schematic} converted
 * once to a vanilla structure NBT (banners, signs and the original chest were dropped). The template's y = 0 layer
 * is the grass the church stands on, so {@link #templatePosition} is the ground block at its north-west corner.
 * <p>
 * The template's air is baked in for every column under the building, so pasting it also carves the hillside.
 * On top of the paste this piece adds what makes it the Sacred Church: the Sacred Altar where the template had an
 * enchanting table on its little platform, the {@link GreatBell} hung from the vault over the crossing, the
 * sacristy chest, Elias kneeling before the altar and a congregation of neutral cultists in the nave.
 * See {@link SacredChurchStructure} for the site rules.
 */
public class SacredChurchPiece extends TemplateStructurePiece {
    public static final ResourceLocation TEMPLATE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "sacred_church");

    /** How far under the church's y = 0 layer the foundation is filled down to reach solid ground. */
    public static final int DEPTH_BELOW = 10;

    // Local coordinates in the template.
    private static final int ALTAR_X = 41, ALTAR_Y = 3, ALTAR_Z = 22;
    // The Great Bell's heart, over the crossing; the chain runs from its crown up to the vault (the crossing's ceiling is y = 32).
    private static final int BELL_X = 36, BELL_Y = 9, BELL_Z = 22, VAULT_Y = 31;
    private static final int ELIAS_X = 39, ELIAS_Y = 3, ELIAS_Z = 22;
    private static final int NAVE_Y = 2; // the red carpet down the nave
    /** The Priest stands behind the altar (east of it), on the slab there, facing the nave. */
    private static final int PRIEST_X = 43, PRIEST_Y = 3, PRIEST_Z = 22;
    private static final double PRIEST_FEET_OFFSET = 0.5; // the bottom slab he stands on
    /** Yaw facing west (the nave): 0 is south, 90 west, 180 north, 270 east. */
    private static final float FACE_WEST = 90.0F;
    /** The pews: spruce stairs facing west (their backs to the nave, so a worshipper looks east at the altar) at floor level. */
    private static final int PEW_MIN_X = 14, PEW_MAX_X = 36;
    /** The middle of the nave: where the congregation roams around by day (see {@link ChurchCongregation}). */
    private static final int NAVE_CENTER_X = 25, NAVE_CENTER_Z = 22;
    /** Loot chests on the nave floor level (y = 2), tucked into nooks: {x, z}. All but the first are only placed by chance. */
    private static final int[][] CHESTS = {{34, 6}, {12, 13}, {12, 31}, {12, 17}, {12, 27}, {32, 38}, {40, 6}, {55, 33}, {59, 33}, {41, 12}, {41, 32}};
    /** Candidate nooks behind the east hall for the hidden vault chest (Pandora Box, see the hidden loot table); one is picked per church. */
    private static final int[][] HIDDEN_CHESTS = {{68, 19}, {68, 25}, {67, 17}, {67, 27}};
    /** Chance for each entry of {@link #CHESTS} after the first to actually exist. */
    private static final double CHEST_CHANCE = 0.7;
    /** The template's two eyed End portal frames, replaced by plain slabs. */
    private static final int[][] PORTAL_FRAMES = {{63, 4, 19}, {63, 4, 25}};

    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "chests/sacred_church"));
    private static final ResourceKey<LootTable> HIDDEN_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "chests/sacred_church_hidden"));

    private final long seed;
    private int spawnedMask; // bit 0 = the Sacred Priest, bits 1.. = cultists
    private List<BlockPos> pewSeats; // derived from the template, not saved

    public SacredChurchPiece(StructureTemplateManager templateManager, long seed, BlockPos pos) {
        super(ModStructurePieceTypes.SACRED_CHURCH.get(), 0, templateManager, TEMPLATE, TEMPLATE.toString(), makeSettings(), pos);
        this.seed = seed;
    }

    public SacredChurchPiece(StructureTemplateManager templateManager, CompoundTag tag) {
        super(ModStructurePieceTypes.SACRED_CHURCH.get(), tag, templateManager, location -> makeSettings());
        this.seed = tag.getLong("Seed");
        this.spawnedMask = tag.getInt("Spawned");
    }

    private static StructurePlaceSettings makeSettings() {
        // No ignore-air processor: the template's air is what clears the hillside.
        return new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(true);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putLong("Seed", this.seed);
        tag.putInt("Spawned", this.spawnedMask);
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
        // No data markers in this template.
    }

    /** The pew seats, front row first (nearest the altar), read from the pasted template's own stairs. */
    private List<BlockPos> pewSeats() {
        if (this.pewSeats == null) {
            int floor = this.templatePosition.getY() + NAVE_Y;
            this.pewSeats = this.template.filterBlocks(this.templatePosition, this.placeSettings, Blocks.SPRUCE_STAIRS).stream()
                    .filter(info -> info.state().getValue(StairBlock.FACING) == Direction.WEST && info.state().getValue(StairBlock.HALF) == Half.BOTTOM
                            && info.pos().getY() == floor
                            && info.pos().getX() >= this.templatePosition.getX() + PEW_MIN_X && info.pos().getX() <= this.templatePosition.getX() + PEW_MAX_X)
                    .map(info -> info.pos().immutable())
                    .sorted(Comparator.comparingInt((BlockPos p) -> -p.getX()).thenComparingInt(BlockPos::getZ))
                    .toList();
        }
        return this.pewSeats;
    }

    private BlockPos at(int lx, int ly, int lz) {
        return this.templatePosition.offset(lx, ly, lz);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
        this.foundation(level, chunkBox);
        super.postProcess(level, structureManager, generator, random, chunkBox, chunkPos, pos);
        this.chancel(level, chunkBox);
        this.chests(level, chunkBox, random);
        this.spawnInhabitants(level, chunkBox);
    }

    /** Fills the gaps under the template's ground layer, down to solid ground. */
    private void foundation(WorldGenLevel level, BoundingBox chunkBox) {
        int width = this.template.getSize().getX();
        int length = this.template.getSize().getZ();
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                if (!chunkBox.isInside(this.at(x, 0, z))) {
                    continue;
                }
                for (int y = -1; y >= -DEPTH_BELOW; y--) {
                    BlockPos p = this.at(x, y, z);
                    BlockState existing = level.getBlockState(p);
                    if (existing.isAir() || !existing.getFluidState().isEmpty() || existing.canBeReplaced()) {
                        level.setBlock(p, (y == -1 ? Blocks.STONE_BRICKS : Blocks.COBBLESTONE).defaultBlockState(), Block.UPDATE_CLIENTS);
                    } else {
                        break;
                    }
                }
            }
        }
    }

    private void set(WorldGenLevel level, BoundingBox chunkBox, int lx, int ly, int lz, BlockState state) {
        BlockPos p = this.at(lx, ly, lz);
        if (chunkBox.isInside(p)) {
            level.setBlock(p, state, Block.UPDATE_CLIENTS);
        }
    }

    /** The altar where the template's enchanting table stood, and the Great Bell over the crossing. */
    private void chancel(WorldGenLevel level, BoundingBox chunkBox) {
        this.set(level, chunkBox, ALTAR_X, ALTAR_Y, ALTAR_Z, ModBlocks.SACRED_ALTAR.get().defaultBlockState());
        BlockPos altarPos = this.at(ALTAR_X, ALTAR_Y, ALTAR_Z);
        BlockPos bellPos = this.at(BELL_X, BELL_Y, BELL_Z);
        GreatBell.build(level, chunkBox, bellPos);
        for (int y = BELL_Y + GreatBell.CHAIN_START; y <= VAULT_Y; y++) {
            this.set(level, chunkBox, BELL_X, y, BELL_Z, Blocks.CHAIN.defaultBlockState());
        }
        if (chunkBox.isInside(altarPos) && level.getBlockEntity(altarPos) instanceof SacredAltarBlockEntity altar) {
            altar.configure(bellPos, this.at(ELIAS_X, ELIAS_Y, ELIAS_Z), true);
            altar.setCongregation(this.at(NAVE_CENTER_X, NAVE_Y, NAVE_CENTER_Z), ChurchCongregation.baseSeats(this.pewSeats(), this.templatePosition),
                    ChurchCongregation.extraSeats(this.pewSeats(), this.templatePosition), level.getLevel().getGameTime());
        }
    }

    private void chests(WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
        for (int[] frame : PORTAL_FRAMES) {
            this.set(level, chunkBox, frame[0], frame[1], frame[2], Blocks.STONE_BRICK_SLAB.defaultBlockState());
        }
        for (int i = 0; i < CHESTS.length; i++) {
            if (i == 0 || this.roll(CHESTS[i][0], CHESTS[i][1], 300 + i) < CHEST_CHANCE) {
                this.placeChest(level, chunkBox, random, CHESTS[i][0], CHESTS[i][1], LOOT);
            }
        }
        int[] hidden = HIDDEN_CHESTS[(int) (this.roll(0, 0, 400) * HIDDEN_CHESTS.length)];
        this.placeChest(level, chunkBox, random, hidden[0], hidden[1], HIDDEN_LOOT);
    }

    /** Places a loot chest at floor level facing the first open side (away from the nook's walls). */
    private void placeChest(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int lx, int lz, ResourceKey<LootTable> table) {
        BlockPos chest = this.at(lx, NAVE_Y, lz);
        if (!chunkBox.isInside(chest)) {
            return;
        }
        Direction facing = Direction.NORTH;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(chest.relative(d)).isAir()) {
                facing = d;
                break;
            }
        }
        level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), Block.UPDATE_CLIENTS);
        RandomizableContainer.setBlockEntityLootTable(level, random, chest, table);
    }

    // ------------------------------------------------------------------------------------------

    private double roll(int x, int z, int salt) {
        long h = this.seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) * (1.0 / (1L << 53));
    }

    /**
     * The Sacred Priest and the congregation, each spawned exactly once, when the chunk holding its spot is generated.
     * Elias is not among them: he is woken at the altar once the Priest has fallen.
     */
    private void spawnInhabitants(WorldGenLevel level, BoundingBox chunkBox) {
        BlockPos priestPos = this.at(PRIEST_X, PRIEST_Y, PRIEST_Z);
        if ((this.spawnedMask & 1) == 0 && chunkBox.isInside(priestPos)) {
            this.spawnedMask |= 1;
            SacredPriestEntity priest = ModEntityTypes.SACRED_PRIEST.get().create(level.getLevel());
            if (priest != null) {
                Vec3 post = new Vec3(priestPos.getX() + 0.5, priestPos.getY() + PRIEST_FEET_OFFSET, priestPos.getZ() + 0.5);
                priest.moveTo(post.x, post.y, post.z, FACE_WEST, 0.0F); // behind the altar, facing the congregation
                priest.setHome(this.at(ALTAR_X, ALTAR_Y, ALTAR_Z));
                priest.setServicePost(post, FACE_WEST);
                priest.finalizeSpawn(level, level.getCurrentDifficultyAt(priestPos), MobSpawnType.STRUCTURE, null);
                level.addFreshEntityWithPassengers(priest);
            }
        }
        List<BlockPos> seats = ChurchCongregation.baseSeats(this.pewSeats(), this.templatePosition);
        for (int i = 0; i < seats.size(); i++) {
            int bit = 1 << (i + 1);
            BlockPos seat = seats.get(i);
            if ((this.spawnedMask & bit) != 0 || !chunkBox.isInside(seat)) {
                continue;
            }
            this.spawnedMask |= bit;
            ChurchCongregation.spawn(level, seat, this.at(NAVE_CENTER_X, NAVE_Y, NAVE_CENTER_Z));
        }
    }
}
