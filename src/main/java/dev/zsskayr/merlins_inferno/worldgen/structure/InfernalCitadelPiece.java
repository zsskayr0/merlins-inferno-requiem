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
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.AndrasEntity;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModStructurePieceTypes;

/**
 * Andras' Citadel: a gothic-demonic keep that floats over the Nether's lava sea on its own support pillars,
 * rather than sitting on the terrain - see {@link InfernalCitadelStructure} for why. Five stacked halls
 * connected by a single ladder shaft (a plainer stand-in for the spiral stairwell a Twilight Forest Lich
 * Tower climbs through - a hand-built {@code .nbt} template, the way the Sacred Church is, would do that
 * justice properly; this is what a procedurally-built piece can manage without one):
 * <ul>
 *     <li><b>Entrance Hall</b> (ground floor): breached south wall as the doorway, two roamers on guard.</li>
 *     <li><b>Guard Hall I/II</b>: a colonnade of pillars, arrow-slit windows, two roamers each.</li>
 *     <li><b>Vault</b>: the loot room - eight chests around the walls, all drawn from vanilla's own Bastion
 *     Remnant loot tables (per design: "the same loot as a Bastion"), one roamer standing guard.</li>
 *     <li><b>Andras' Hall</b>: the boss room, taller than the rest - Andras himself, and a {@link ModBlocks#CORRUPTED_OBSIDIAN}
 *     portal frame in the back wall (decorative only for now - see the block's own javadoc).</li>
 * </ul>
 * Imps and Starveds are the roamers throughout. Built procedurally, so every "random" choice (which wall
 * course gets a cracked brick, which chest gets which loot table) is derived from a stored seed and each
 * block's own coordinates ({@link #roll}), never from the {@code RandomSource} {@code postProcess} is
 * handed - {@code postProcess} runs once per overlapping chunk, and a real per-call random would make the
 * citadel look different depending on which chunk order the game happened to generate it in.
 */
public class InfernalCitadelPiece extends StructurePiece {
    /** Fixed height every citadel generates at, regardless of terrain - see the structure's own javadoc. */
    public static final int BASE_Y = 55;
    public static final int RADIUS = 8; // half-width; footprint is (2*RADIUS+1) blocks square
    /** How far the corner support pillars reach below the base, toward (and usually into) the lava sea. */
    public static final int SUPPORT_DEPTH = 40;
    /** Tallest point above the base: five halls, the roof and the corner turret spikes, plus slack. */
    public static final int HEIGHT_ABOVE = 40;

    private static final int[] HALL_HEIGHT = {6, 5, 5, 5, 8}; // Entrance, Guard I, Guard II, Vault, Andras' Hall
    private static final int ENTRANCE = 0, GUARD_1 = 1, GUARD_2 = 2, VAULT = 3, BOSS_HALL = 4;

    private static final int SHAFT_DX = RADIUS - 2; // the ladder shaft sits off-centre so the boss spawn/portal can use the middle

    private static final BlockState WALL = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    private static final BlockState WALL_CRACKED = Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    private static final BlockState TRIM = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
    private static final BlockState BAND = Blocks.RED_NETHER_BRICKS.defaultBlockState();
    private static final BlockState FLOOR = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
    private static final BlockState PILLAR = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    private static final BlockState SUPPORT = Blocks.BASALT.defaultBlockState();
    private static final BlockState WINDOW = Blocks.NETHER_BRICK_FENCE.defaultBlockState();
    private static final BlockState LANTERN = Blocks.SOUL_LANTERN.defaultBlockState();
    private static final BlockState ROOF_STAIRS = Blocks.RED_NETHER_BRICK_STAIRS.defaultBlockState();
    private static final BlockState LADDER_NORTH = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH);
    private static final BlockState PORTAL_FRAME = ModBlocks.CORRUPTED_OBSIDIAN.get().defaultBlockState();

    /** {dx, dz, hall, 0 = Imp / 1 = Starved} - Andras himself is tracked separately (see {@link #spawnAndras}). */
    private static final int[][] ROAMERS = {
            {-3, 3, ENTRANCE, 0}, {3, -3, ENTRANCE, 1},
            {-3, 3, GUARD_1, 0}, {3, -3, GUARD_1, 1},
            {-3, 3, GUARD_2, 0}, {3, -3, GUARD_2, 1},
            {0, 0, VAULT, 1},
    };

    /** {dx, dz} around the Vault's inner walls; the loot table rotates every three chests. */
    private static final int[][] CHESTS = {
            {RADIUS - 2, 0}, {-(RADIUS - 2), 0}, {0, RADIUS - 2}, {0, -(RADIUS - 2)},
            {RADIUS - 3, RADIUS - 3}, {-(RADIUS - 3), RADIUS - 3}, {RADIUS - 3, -(RADIUS - 3)}, {-(RADIUS - 3), -(RADIUS - 3)},
    };
    private static final ResourceKey<LootTable>[] BASTION_LOOT = bastionLootTables();

    private final long seed;
    private final BlockPos base; // BASE_Y, at the citadel's centre column
    private int spawnedMask; // bits 0..6 = ROAMERS, bit 7 = Andras

    public InfernalCitadelPiece(long seed, BlockPos base) {
        super(ModStructurePieceTypes.INFERNAL_CITADEL.get(), 0,
                new BoundingBox(base.getX() - RADIUS - 3, base.getY() - SUPPORT_DEPTH, base.getZ() - RADIUS - 3,
                        base.getX() + RADIUS + 3, base.getY() + HEIGHT_ABOVE, base.getZ() + RADIUS + 3));
        this.seed = seed;
        this.base = base;
    }

    public InfernalCitadelPiece(CompoundTag tag) {
        super(ModStructurePieceTypes.INFERNAL_CITADEL.get(), tag);
        this.seed = tag.getLong("Seed");
        this.base = new BlockPos(tag.getInt("BX"), tag.getInt("BY"), tag.getInt("BZ"));
        this.spawnedMask = tag.getInt("Spawned");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLong("Seed", this.seed);
        tag.putInt("BX", this.base.getX());
        tag.putInt("BY", this.base.getY());
        tag.putInt("BZ", this.base.getZ());
        tag.putInt("Spawned", this.spawnedMask);
    }

    @SuppressWarnings("unchecked")
    private static ResourceKey<LootTable>[] bastionLootTables() {
        return new ResourceKey[] {
                ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("chests/bastion_treasure")),
                ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("chests/bastion_other")),
                ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("chests/bastion_bridge")),
        };
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

    private int floorY(int hall) {
        int y = this.base.getY() + 1;
        for (int i = 0; i < hall; i++) {
            y += HALL_HEIGHT[i];
        }
        return y;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pos) {
        int cx = this.base.getX();
        int cz = this.base.getZ();

        this.buildSupports(level, chunkBox, cx, cz);
        for (int hall = 0; hall <= BOSS_HALL; hall++) {
            this.buildHall(level, chunkBox, cx, cz, hall);
        }
        this.buildRoof(level, chunkBox, cx, cz);
        this.buildPortalFrame(level, chunkBox, cx, cz);
        this.placeChests(level, chunkBox, random, cx, cz);
        this.spawnRoamers(level, chunkBox, cx, cz);
        this.spawnAndras(level, chunkBox, cx, cz);
    }

    // ------------------------------------------------------------------------------------------
    // Block-placement helpers
    // ------------------------------------------------------------------------------------------

    private void set(WorldGenLevel level, BoundingBox chunkBox, int x, int y, int z, BlockState state) {
        BlockPos p = new BlockPos(x, y, z);
        if (chunkBox.isInside(p)) {
            level.setBlock(p, state, Block.UPDATE_CLIENTS);
        }
    }

    private void fillBox(WorldGenLevel level, BoundingBox chunkBox, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    this.set(level, chunkBox, x, y, z, state);
                }
            }
        }
    }

    /** The four vertical wall faces of a box (no floor/ceiling) - used for one hall's walls, one Y layer at a time. */
    private void wallRing(WorldGenLevel level, BoundingBox chunkBox, int cx, int cz, int y, int radius, BlockState state) {
        for (int x = cx - radius; x <= cx + radius; x++) {
            this.set(level, chunkBox, x, y, cz - radius, state);
            this.set(level, chunkBox, x, y, cz + radius, state);
        }
        for (int z = cz - radius; z <= cz + radius; z++) {
            this.set(level, chunkBox, cx - radius, y, z, state);
            this.set(level, chunkBox, cx + radius, y, z, state);
        }
    }

    // ------------------------------------------------------------------------------------------
    // The citadel itself
    // ------------------------------------------------------------------------------------------

    /** Four corner legs of Basalt, dropping from the base toward the lava sea - what actually "holds it up". */
    private void buildSupports(WorldGenLevel level, BoundingBox chunkBox, int cx, int cz) {
        int inset = RADIUS - 1;
        int[][] corners = {{-inset, -inset}, {-inset, inset}, {inset, -inset}, {inset, inset}};
        for (int[] corner : corners) {
            this.fillBox(level, chunkBox, cx + corner[0], this.base.getY() - SUPPORT_DEPTH, cz + corner[1],
                    cx + corner[0] + 1, this.base.getY(), cz + corner[1] + 1, SUPPORT);
        }
    }

    private void buildHall(WorldGenLevel level, BoundingBox chunkBox, int cx, int cz, int hall) {
        int floorY = this.floorY(hall);
        int height = HALL_HEIGHT[hall];
        int ceilingY = floorY + height;

        // Floor: solid on the ground hall, punched with the shaft's hole everywhere above it.
        this.fillBox(level, chunkBox, cx - RADIUS, floorY, cz - RADIUS, cx + RADIUS, floorY, cz + RADIUS, FLOOR);
        if (hall > 0) {
            this.fillBox(level, chunkBox, cx + SHAFT_DX - 1, floorY, cz - 1, cx + SHAFT_DX + 1, floorY, cz + 1, Blocks.AIR.defaultBlockState());
        }

        // Walls, one Y layer at a time so window slits/trim bands can override a single course.
        for (int y = floorY; y < ceilingY; y++) {
            boolean isTrimCourse = y == floorY || y == ceilingY - 1;
            boolean isWindowCourse = !isTrimCourse && y == floorY + height / 2;
            for (int x = cx - RADIUS; x <= cx + RADIUS; x++) {
                for (int side = -1; side <= 1; side += 2) {
                    int z = cz + side * RADIUS;
                    this.wallBlock(level, chunkBox, x, y, z, cx, cz, hall, isTrimCourse, isWindowCourse);
                }
            }
            for (int z = cz - RADIUS + 1; z <= cz + RADIUS - 1; z++) {
                for (int side = -1; side <= 1; side += 2) {
                    int x = cx + side * RADIUS;
                    this.wallBlock(level, chunkBox, x, y, z, cx, cz, hall, isTrimCourse, isWindowCourse);
                }
            }
        }

        if (hall == ENTRANCE) {
            // The doorway: breach the south wall, three wide, floor to near the ceiling.
            this.fillBox(level, chunkBox, cx - 1, floorY + 1, cz + RADIUS, cx + 1, floorY + height - 2, cz + RADIUS,
                    Blocks.AIR.defaultBlockState());
        }
        if (hall == GUARD_1 || hall == GUARD_2) {
            this.buildColonnade(level, chunkBox, cx, cz, floorY, height);
        }
        if (hall == BOSS_HALL) {
            this.fillBox(level, chunkBox, cx - 3, floorY + 1, cz - 2, cx + 3, floorY + 1, cz + 2, Blocks.AIR.defaultBlockState());
            this.set(level, chunkBox, cx - RADIUS + 2, floorY + 1, cz - RADIUS + 2, LANTERN);
            this.set(level, chunkBox, cx + RADIUS - 2, floorY + 1, cz - RADIUS + 2, LANTERN);
            this.set(level, chunkBox, cx - RADIUS + 2, floorY + 1, cz + RADIUS - 2, LANTERN);
            this.set(level, chunkBox, cx + RADIUS - 2, floorY + 1, cz + RADIUS - 2, LANTERN);
        }

        // The shaft's ladder, spanning this hall's full height, and the lanterns marking each landing.
        if (hall < BOSS_HALL) {
            for (int y = floorY + 1; y < ceilingY; y++) {
                this.set(level, chunkBox, cx + SHAFT_DX, y, cz - 1, LADDER_NORTH);
            }
        }
        this.set(level, chunkBox, cx - RADIUS + 1, floorY + 1, cz - RADIUS + 1, LANTERN);
        this.set(level, chunkBox, cx + RADIUS - 1, floorY + 1, cz + RADIUS - 1, LANTERN);
    }

    /**
     * One wall block: mostly {@link #WALL}, with trim/window/cracked-brick variety layered on top. The Entrance
     * Hall's doorway is carved separately, after every hall's walls are up (see {@link #buildHall}) - this
     * places the wall solid there too, since the carve overwrites exactly the range that should be open and
     * deliberately leaves the threshold/lintel trim courses this method already placed alone.
     */
    private void wallBlock(WorldGenLevel level, BoundingBox chunkBox, int x, int y, int z, int cx, int cz, int hall,
            boolean isTrimCourse, boolean isWindowCourse) {
        boolean onMidX = x == cx;
        boolean onMidZ = z == cz;
        BlockState state;
        if (hall == BOSS_HALL && isTrimCourse) {
            state = BAND;
        } else if (isWindowCourse && (onMidX || onMidZ)) {
            state = WINDOW;
        } else if (isTrimCourse) {
            state = TRIM;
        } else {
            state = this.roll(x, z, 5 + hall) < 0.08 ? WALL_CRACKED : WALL;
        }
        this.set(level, chunkBox, x, y, z, state);
    }

    /** Four interior pillars, one per quadrant, capped with a trim block. */
    private void buildColonnade(WorldGenLevel level, BoundingBox chunkBox, int cx, int cz, int floorY, int height) {
        int offset = RADIUS - 3;
        int[][] posts = {{-offset, -offset}, {-offset, offset}, {offset, -offset}, {offset, offset}};
        for (int[] post : posts) {
            this.fillBox(level, chunkBox, cx + post[0], floorY + 1, cz + post[1], cx + post[0], floorY + height - 2, cz + post[1], PILLAR);
            this.set(level, chunkBox, cx + post[0], floorY + height - 1, cz + post[1], TRIM);
        }
    }

    /** A stepped pyramid over Andras' Hall, plus four corner turret spikes for the tower's silhouette. */
    private void buildRoof(WorldGenLevel level, BoundingBox chunkBox, int cx, int cz) {
        int roofY = this.floorY(BOSS_HALL) + HALL_HEIGHT[BOSS_HALL];
        for (int radius = RADIUS; radius >= 0; radius -= 2) {
            int y = roofY + (RADIUS - radius) / 2;
            this.wallRing(level, chunkBox, cx, cz, y, radius, ROOF_STAIRS);
            this.fillBox(level, chunkBox, cx - radius, y, cz - radius, cx + radius, y, cz + radius, ROOF_STAIRS);
        }
        this.set(level, chunkBox, cx, roofY + RADIUS / 2 + 1, cz, ROOF_STAIRS);

        int[][] corners = {{-RADIUS, -RADIUS}, {-RADIUS, RADIUS}, {RADIUS, -RADIUS}, {RADIUS, RADIUS}};
        int entranceRoofY = this.floorY(ENTRANCE) + HALL_HEIGHT[ENTRANCE];
        for (int[] corner : corners) {
            this.fillBox(level, chunkBox, cx + corner[0], entranceRoofY, cz + corner[1], cx + corner[0], roofY + 2, cz + corner[1], PILLAR);
            this.set(level, chunkBox, cx + corner[0], roofY + 3, cz + corner[1], TRIM);
        }
    }

    /** The (decorative, for now) portal frame in Andras' Hall's back wall - see the block's own javadoc. */
    private void buildPortalFrame(WorldGenLevel level, BoundingBox chunkBox, int cx, int cz) {
        int floorY = this.floorY(BOSS_HALL);
        int z = cz - RADIUS;
        this.fillBox(level, chunkBox, cx - 2, floorY + 1, z, cx + 2, floorY + 5, z, PORTAL_FRAME);
        this.fillBox(level, chunkBox, cx - 1, floorY + 2, z, cx + 1, floorY + 4, z, Blocks.AIR.defaultBlockState());
    }

    // ------------------------------------------------------------------------------------------
    // Loot and inhabitants
    // ------------------------------------------------------------------------------------------

    private void placeChests(WorldGenLevel level, BoundingBox chunkBox, RandomSource random, int cx, int cz) {
        int floorY = this.floorY(VAULT) + 1;
        for (int i = 0; i < CHESTS.length; i++) {
            BlockPos p = new BlockPos(cx + CHESTS[i][0], floorY, cz + CHESTS[i][1]);
            if (!chunkBox.isInside(p)) {
                continue;
            }
            Direction facing = Direction.getNearest(-CHESTS[i][0], 0, -CHESTS[i][1]);
            level.setBlock(p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), Block.UPDATE_CLIENTS);
            RandomizableContainer.setBlockEntityLootTable(level, random, p, BASTION_LOOT[i % BASTION_LOOT.length]);
        }
    }

    /** Imps and Starveds on guard through the halls - each spawned once, tethered to the whole citadel. */
    private void spawnRoamers(WorldGenLevel level, BoundingBox chunkBox, int cx, int cz) {
        for (int i = 0; i < ROAMERS.length; i++) {
            int bit = 1 << i;
            if ((this.spawnedMask & bit) != 0) {
                continue;
            }
            int[] roamer = ROAMERS[i];
            BlockPos p = new BlockPos(cx + roamer[0], this.floorY(roamer[2]) + 1, cz + roamer[1]);
            if (!chunkBox.isInside(p)) {
                continue;
            }
            this.spawnedMask |= bit;
            Mob mob;
            if (roamer[3] == 0) {
                mob = ModEntityTypes.IMP.get().create(level.getLevel());
            } else {
                mob = ModEntityTypes.STARVED.get().create(level.getLevel());
            }
            if (mob == null) {
                continue;
            }
            mob.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, (float) (this.roll(p.getX(), p.getZ(), 11) * 360.0), 0.0F);
            mob.setPersistenceRequired();
            mob.restrictTo(this.base, RADIUS + 4);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null);
            level.addFreshEntityWithPassengers(mob);
        }
    }

    /** Andras himself, spawned once at the centre of his hall - guaranteed, unlike his rare roaming spawn elsewhere. */
    private void spawnAndras(WorldGenLevel level, BoundingBox chunkBox, int cx, int cz) {
        int bit = 1 << ROAMERS.length;
        BlockPos p = new BlockPos(cx, this.floorY(BOSS_HALL) + 1, cz);
        if ((this.spawnedMask & bit) != 0 || !chunkBox.isInside(p)) {
            return;
        }
        this.spawnedMask |= bit;
        AndrasEntity andras = ModEntityTypes.ANDRAS.get().create(level.getLevel());
        if (andras == null) {
            return;
        }
        andras.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 180.0F, 0.0F);
        andras.setPersistenceRequired();
        andras.restrictTo(this.base, RADIUS + 2);
        andras.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null);
        level.addFreshEntityWithPassengers(andras);
    }
}
