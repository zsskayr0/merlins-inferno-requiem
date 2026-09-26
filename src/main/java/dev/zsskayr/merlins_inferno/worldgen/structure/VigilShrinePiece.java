package dev.zsskayr.merlins_inferno.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
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

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.blockentity.VigilAltarBlockEntity;
import dev.zsskayr.merlins_inferno.entity.PenitentEntity;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModStructurePieceTypes;

/**
 * The Vigil Shrine: a gothic cross-plan church (nave running west to east, transepts north and south, a raised
 * chancel to the east), pasted from the {@code vigil_church} template - a classic {@code .schematic} converted
 * once to a vanilla structure NBT (banners, signs and the original chest were dropped). The template's y = 0 layer
 * is the grass the church stands on, so {@link #templatePosition} is the ground block at its north-west corner.
 * <p>
 * The template's air is baked in for every column under the building, so pasting it also carves the hillside.
 * On top of the paste this piece adds what makes it the Vigil Shrine: the Vigil Altar where the template had an
 * enchanting table on its little platform, the {@link GreatBell} hung from the vault over the crossing, the
 * sacristy chest, the Penitent kneeling before the altar and a congregation of neutral worshippers in the nave.
 * See {@link VigilShrineStructure} for the site rules.
 */
public class VigilShrinePiece extends TemplateStructurePiece {
    public static final ResourceLocation TEMPLATE = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "vigil_church");

    /** How far under the church's y = 0 layer the foundation is filled down to reach solid ground. */
    public static final int DEPTH_BELOW = 10;

    // Local coordinates in the template.
    private static final int ALTAR_X = 41, ALTAR_Y = 3, ALTAR_Z = 22;
    // The Great Bell's heart, over the crossing; the chain runs from its crown up to the vault (the crossing's ceiling is y = 32).
    private static final int BELL_X = 36, BELL_Y = 9, BELL_Z = 22, VAULT_Y = 31;
    private static final int PENITENT_X = 39, PENITENT_Y = 3, PENITENT_Z = 22;
    private static final int NAVE_Y = 2; // the red carpet down the nave
    private static final int[][] WORSHIPPERS = {{18, 21}, {22, 21}, {26, 21}, {30, 21}, {18, 23}, {22, 23}, {26, 23}, {30, 23}};
    private static final int[] CHEST = {34, 2, 6};

    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "chests/vigil_shrine"));

    private final long seed;
    private int spawnedMask; // bit 0 = Penitent, bits 1.. = worshippers

    public VigilShrinePiece(StructureTemplateManager templateManager, long seed, BlockPos pos) {
        super(ModStructurePieceTypes.VIGIL_SHRINE.get(), 0, templateManager, TEMPLATE, TEMPLATE.toString(), makeSettings(), pos);
        this.seed = seed;
    }

    public VigilShrinePiece(StructureTemplateManager templateManager, CompoundTag tag) {
        super(ModStructurePieceTypes.VIGIL_SHRINE.get(), tag, templateManager, location -> makeSettings());
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

    private BlockPos at(int lx, int ly, int lz) {
        return this.templatePosition.offset(lx, ly, lz);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
        this.foundation(level, chunkBox);
        super.postProcess(level, structureManager, generator, random, chunkBox, chunkPos, pos);
        this.chancel(level, chunkBox);
        this.chest(level, chunkBox, random);
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
        this.set(level, chunkBox, ALTAR_X, ALTAR_Y, ALTAR_Z, ModBlocks.VIGIL_ALTAR.get().defaultBlockState());
        BlockPos altarPos = this.at(ALTAR_X, ALTAR_Y, ALTAR_Z);
        BlockPos bellPos = this.at(BELL_X, BELL_Y, BELL_Z);
        GreatBell.build(level, chunkBox, bellPos);
        for (int y = BELL_Y + GreatBell.CHAIN_START; y <= VAULT_Y; y++) {
            this.set(level, chunkBox, BELL_X, y, BELL_Z, Blocks.CHAIN.defaultBlockState());
        }
        if (chunkBox.isInside(altarPos) && level.getBlockEntity(altarPos) instanceof VigilAltarBlockEntity altar) {
            altar.configure(bellPos, this.at(PENITENT_X, PENITENT_Y, PENITENT_Z));
        }
    }

    private void chest(WorldGenLevel level, BoundingBox chunkBox, RandomSource random) {
        BlockPos chest = this.at(CHEST[0], CHEST[1], CHEST[2]);
        if (chunkBox.isInside(chest)) {
            level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST), Block.UPDATE_CLIENTS);
            RandomizableContainer.setBlockEntityLootTable(level, random, chest, LOOT);
        }
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

    /** The Penitent and the congregation, each spawned exactly once, when the chunk holding its spot is generated. */
    private void spawnInhabitants(WorldGenLevel level, BoundingBox chunkBox) {
        BlockPos penitentPos = this.at(PENITENT_X, PENITENT_Y, PENITENT_Z);
        if ((this.spawnedMask & 1) == 0 && chunkBox.isInside(penitentPos)) {
            this.spawnedMask |= 1;
            PenitentEntity penitent = ModEntityTypes.PENITENT.get().create(level.getLevel());
            if (penitent != null) {
                penitent.moveTo(penitentPos.getX() + 0.5, penitentPos.getY(), penitentPos.getZ() + 0.5, 270.0F, 0.0F); // kneeling east, toward the altar
                penitent.setHome(this.at(ALTAR_X, ALTAR_Y, ALTAR_Z));
                penitent.finalizeSpawn(level, level.getCurrentDifficultyAt(penitentPos), MobSpawnType.STRUCTURE, null);
                level.addFreshEntityWithPassengers(penitent);
            }
        }
        for (int i = 0; i < WORSHIPPERS.length; i++) {
            int bit = 1 << (i + 1);
            BlockPos p = this.at(WORSHIPPERS[i][0], NAVE_Y, WORSHIPPERS[i][1]);
            if ((this.spawnedMask & bit) != 0 || !chunkBox.isInside(p)) {
                continue;
            }
            this.spawnedMask |= bit;
            Mob worshipper = ModEntityTypes.WORSHIPPER.get().create(level.getLevel());
            if (worshipper != null) {
                worshipper.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, (float) (this.roll(p.getX(), p.getZ(), 90) * 360.0), 0.0F);
                worshipper.setPersistenceRequired();
                worshipper.restrictTo(this.at(25, NAVE_Y, 22), 18);
                worshipper.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null);
                level.addFreshEntityWithPassengers(worshipper);
            }
        }
    }
}
