package dev.zsskayr.merlins_inferno.worldgen.elysium.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;

import dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumWorld;

/**
 * The one door through which the structure builders touch the world. A brush owns a clip box (x/z rectangle, plus an
 * optional radius around an origin, plus y in [{@link ElysiumWorld#LOWEST_SOLID}, build height)) and silently drops
 * every write outside it, so callers can draw whole shapes and let the brush cut out the part this chunk may touch.
 *
 * <p>Write flavours:
 * <ul>
 *   <li>{@link #set} overwrites (masonry, floors);</li>
 *   <li>{@link #put} only fills air / replaceable plants (decoration that must not eat terrain);</li>
 *   <li>{@link #shaped} = set + mark for post-processing, for blocks whose look depends on neighbours (walls, fences,
 *       bars, panes, stairs): when the chunk becomes full the game runs {@code updateShape} on them, so connections
 *       across chunk borders come out right;</li>
 *   <li>{@link #clear} carves to air, but never next to water (rule: every water block stays held).</li>
 * </ul>
 * All writes use flag 2 (no neighbour updates during generation) like vanilla features.
 */
final class Brush {
    /** Returned by {@link #ground} when a column has no usable ground (void, water). */
    static final int NONE = Integer.MIN_VALUE;

    final WorldGenLevel level;
    final int minX, minZ, maxX, maxZ;
    final int minY, maxY;
    private final int ox, oz;
    private final long radiusSq;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    private Brush(WorldGenLevel level, int minX, int minZ, int maxX, int maxZ, int ox, int oz, int radius) {
        this.level = level;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.minY = Math.max(ElysiumWorld.LOWEST_SOLID, level.getMinBuildHeight());
        this.maxY = level.getMaxBuildHeight() - 1;
        this.ox = ox;
        this.oz = oz;
        this.radiusSq = radius <= 0 ? Long.MAX_VALUE : (long) radius * radius;
    }

    /** Clip to exactly one chunk (spans: each chunk draws only its own columns). */
    static Brush chunk(WorldGenLevel level, ChunkPos chunk) {
        return new Brush(level, chunk.getMinBlockX(), chunk.getMinBlockZ(), chunk.getMaxBlockX(), chunk.getMaxBlockZ(), 0, 0, 0);
    }

    /** Clip to the chunk grown by {@code margin} blocks and to a disc of {@code radius} around (x, z) (sites). */
    static Brush site(WorldGenLevel level, ChunkPos chunk, int margin, int x, int z, int radius) {
        return new Brush(level, chunk.getMinBlockX() - margin, chunk.getMinBlockZ() - margin,
                chunk.getMaxBlockX() + margin, chunk.getMaxBlockZ() + margin, x, z, radius);
    }

    boolean insideXZ(int x, int z) {
        if (x < minX || x > maxX || z < minZ || z > maxZ) {
            return false;
        }
        long dx = x - ox, dz = z - oz;
        return radiusSq == Long.MAX_VALUE || dx * dx + dz * dz <= radiusSq;
    }

    boolean inside(int x, int y, int z) {
        return y >= minY && y <= maxY && insideXZ(x, z);
    }

    BlockState get(int x, int y, int z) {
        return level.getBlockState(pos.set(x, y, z));
    }

    /** Overwrite. */
    boolean set(int x, int y, int z, BlockState state) {
        if (!inside(x, y, z)) {
            return false;
        }
        return level.setBlock(pos.set(x, y, z), state, 2);
    }

    /** Overwrite and mark for post-processing (shape fix-up of walls, fences, bars, panes, stairs). */
    boolean shaped(int x, int y, int z, BlockState state) {
        if (!set(x, y, z, state)) {
            return false;
        }
        pos.set(x, y, z);
        level.getChunk(pos).markPosForPostprocessing(pos);
        return true;
    }

    /** True if the block here may be overwritten by decoration: air or a replaceable plant, never a fluid. */
    boolean free(int x, int y, int z) {
        BlockState s = get(x, y, z);
        return s.isAir() || (s.canBeReplaced() && s.getFluidState().isEmpty());
    }

    /** Place only into air / replaceable plants. */
    boolean put(int x, int y, int z, BlockState state) {
        if (!inside(x, y, z) || !free(x, y, z)) {
            return false;
        }
        return level.setBlock(pos.set(x, y, z), state, 2);
    }

    /** Solid for building purposes: blocks movement, holds no fluid, is not foliage. */
    boolean solid(int x, int y, int z) {
        BlockState s = get(x, y, z);
        return s.blocksMotion() && s.getFluidState().isEmpty() && !s.is(BlockTags.LEAVES);
    }

    /**
     * Carve to air. Refused next to water (so no water block loses its hold) and on fluids. A gravity block left
     * hanging right above the cut is turned to stone/sandstone so nothing falls.
     */
    boolean clear(int x, int y, int z) {
        if (!inside(x, y, z)) {
            return false;
        }
        BlockState s = get(x, y, z);
        if (s.isAir()) {
            return false;
        }
        if (!s.getFluidState().isEmpty() || wet(x, y + 1, z) || wet(x + 1, y, z) || wet(x - 1, y, z)
                || wet(x, y, z + 1) || wet(x, y, z - 1)) {
            return false;
        }
        level.setBlock(pos.set(x, y, z), Pal.AIR, 2);
        BlockState above = get(x, y + 1, z);
        if (above.getBlock() instanceof FallingBlock && inside(x, y + 1, z)) {
            level.setBlock(pos.set(x, y + 1, z), above.is(BlockTags.SAND) ? Pal.SANDSTONE : Pal.STONE, 2);
        }
        return true;
    }

    private boolean wet(int x, int y, int z) {
        return !get(x, y, z).getFluidState().isEmpty();
    }

    /**
     * Y of the top solid ground block of the column (skipping plants, leaves and logs), or {@link #NONE} if the column
     * is open sky or the first thing under the surface is water. Starts at the WORLD_SURFACE_WG heightmap.
     */
    int ground(int x, int z) {
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        for (int y = top, n = 0; y >= minY && n < 64; y--, n++) {
            BlockState s = get(x, y, z);
            if (!s.getFluidState().isEmpty()) {
                return NONE;
            }
            if (s.isAir() || !s.blocksMotion() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS)) {
                continue;
            }
            return y;
        }
        return NONE;
    }

    /**
     * Fill from {@code yTop} downwards with {@code state} through air/plants until solid ground, at most
     * {@code maxDepth} blocks. Returns true if the fill reached ground (the column is supported).
     */
    boolean foundation(int x, int yTop, int z, BlockState state, int maxDepth) {
        for (int y = yTop; y > yTop - maxDepth; y--) {
            if (y < minY) {
                return false;
            }
            if (solid(x, y, z)) {
                return true;
            }
            set(x, y, z, state);
        }
        return solid(x, yTop - maxDepth, z);
    }

    /** Vertical run of {@code state} at (x, z) from {@code yBottom} to {@code yTop} inclusive. */
    void column(int x, int z, int yBottom, int yTop, BlockState state) {
        for (int y = yBottom; y <= yTop; y++) {
            set(x, y, z, state);
        }
    }

    /** Moss (or any) carpet: only into free space resting on a sturdy top face. */
    boolean carpet(int x, int y, int z, BlockState carpet) {
        if (!inside(x, y, z) || !free(x, y, z)) {
            return false;
        }
        BlockPos below = new BlockPos(x, y - 1, z);
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return false;
        }
        return level.setBlock(pos.set(x, y, z), carpet, 2);
    }

    /**
     * A strand of vines growing down a wall from (x, y, z): each block clings to every horizontal neighbour that can hold
     * a vine (vanilla rule, so the vines survive updates). Stops at the first block that has no support or is not free.
     */
    int wallVines(int x, int y, int z, int length) {
        int placed = 0;
        for (int i = 0; i < length; i++) {
            int yy = y - i;
            if (!inside(x, yy, z) || !free(x, yy, z) || !get(x, yy, z).isAir()) {
                break;
            }
            BlockState vine = Pal.VINE;
            boolean any = false;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = new BlockPos(x + d.getStepX(), yy, z + d.getStepZ());
                if (VineBlock.isAcceptableNeighbour(level, n, d)) {
                    vine = vine.setValue(VineBlock.getPropertyForFace(d), true);
                    any = true;
                }
            }
            if (!any) {
                break;
            }
            level.setBlock(pos.set(x, yy, z), vine, 2);
            placed++;
        }
        return placed;
    }

    /**
     * A strand of vines hanging free under a ceiling block at y + 1. The first block clings UP; every block also carries
     * the same side face so the vanilla "vine above holds this side" rule keeps the strand together. Requires a ceiling
     * whose underside can hold a vine.
     */
    int hangingVines(int x, int y, int z, int length, Direction side) {
        BlockPos ceiling = new BlockPos(x, y + 1, z);
        // vanilla convention: (neighbour position, direction from the vine towards that neighbour)
        if (!VineBlock.isAcceptableNeighbour(level, ceiling, Direction.UP)) {
            return 0;
        }
        int placed = 0;
        for (int i = 0; i < length; i++) {
            int yy = y - i;
            if (!inside(x, yy, z) || !get(x, yy, z).isAir()) {
                break;
            }
            BlockState vine = Pal.VINE.setValue(VineBlock.getPropertyForFace(side), true);
            if (i == 0) {
                vine = vine.setValue(VineBlock.UP, true);
            }
            level.setBlock(pos.set(x, yy, z), vine, 2);
            placed++;
        }
        return placed;
    }

    /** A chest with a vanilla loot table, facing {@code facing}, on solid ground only. */
    boolean chest(int x, int y, int z, Direction facing, ResourceKey<LootTable> loot, long salt) {
        if (!inside(x, y, z) || !solid(x, y - 1, z)) {
            return false;
        }
        BlockPos at = new BlockPos(x, y, z);
        if (!level.setBlock(at, Pal.CHEST.setValue(ChestBlock.FACING, facing), 2)) {
            return false;
        }
        RandomizableContainer.setBlockEntityLootTable(level, RandomSource.create(salt), at, loot);
        return true;
    }
}
