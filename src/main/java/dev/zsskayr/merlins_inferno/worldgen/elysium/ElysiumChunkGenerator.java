package dev.zsskayr.merlins_inferno.worldgen.elysium;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;

import dev.zsskayr.merlins_inferno.worldgen.elysium.core.Column;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumWorld;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.Mat;

/**
 * El'ysium's chunk generator. It copies what the pure core ({@link ElysiumWorld}) says about each column into the
 * chunk and does nothing else: no noise router, no surface rules, no aquifers, no carvers. JSON:
 * {@code {"type": "merlins_inferno:elysium", "biome_source": {"type": "merlins_inferno:elysium"}}}.
 *
 * <p>Column layout (see {@link Column}): hang dressing under {@code bottom}, solid {@code bottom..top}, water
 * {@code top+1..waterTop} (ice on top when frozen), and an optional floating arch span.
 */
public final class ElysiumChunkGenerator extends ChunkGenerator {
    public static final MapCodec<ElysiumChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
    ).apply(instance, ElysiumChunkGenerator::new));

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    /** Share of cave-vine blocks that carry glow berries (a little light under the islands, not much). */
    private static final double VINE_BERRY_CHANCE = 0.12;

    public ElysiumChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    // ------------------------------------------------------------------------------------------------ biomes

    @Override
    public CompletableFuture<ChunkAccess> createBiomes(RandomState randomState, Blender blender, StructureManager structureManager,
                                                       ChunkAccess chunk) {
        BiomeResolver resolver;
        if (biomeSource instanceof ElysiumBiomeSource source) {
            ElysiumWorld world = ElysiumSeed.world();
            resolver = (x, y, z, sampler) -> source.holder(
                    world.biomeAt(QuartPos.toBlock(x) + 2, QuartPos.toBlock(y) + 2, QuartPos.toBlock(z) + 2));
        } else {
            resolver = biomeSource;
        }
        chunk.fillBiomesFromNoise(resolver, randomState.sampler());
        return CompletableFuture.completedFuture(chunk);
    }

    // ------------------------------------------------------------------------------------------------ terrain

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager,
                                                        ChunkAccess chunk) {
        ElysiumWorld world = ElysiumSeed.world();
        ChunkPos chunkPos = chunk.getPos();
        int x0 = chunkPos.getMinBlockX(), z0 = chunkPos.getMinBlockZ();
        int minY = chunk.getMinBuildHeight(), maxY = chunk.getMaxBuildHeight() - 1;

        Neighbourhood cols = new Neighbourhood(world, x0, z0);
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        LevelChunkSection[] sections = chunk.getSections();
        for (LevelChunkSection section : sections) {
            section.acquire();
        }
        try {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int dz = 0; dz < 16; dz++) {
                for (int dx = 0; dx < 16; dx++) {
                    Column c = cols.get(dx, dz);
                    int lo = lowest(c), hi = highest(c);
                    if (hi == Integer.MIN_VALUE) {
                        continue;
                    }
                    lo = Math.max(lo, minY);
                    hi = Math.min(hi, maxY);
                    int wx = x0 + dx, wz = z0 + dz;
                    for (int y = lo; y <= hi; y++) {
                        BlockState state = stateAt(world, c, wx, y, wz);
                        if (state.isAir()) {
                            continue;
                        }
                        LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
                        section.setBlockState(dx, y & 15, dz, state, false);
                        oceanFloor.update(dx, y, dz, state);
                        worldSurface.update(dx, y, dz, state);
                        if (isWaterBlock(c, y) && needsFlow(cols, c, dx, dz, y)) {
                            chunk.markPosForPostprocessing(pos.set(wx, y, wz));
                        }
                    }
                }
            }
        } finally {
            for (LevelChunkSection section : sections) {
                section.release();
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    /** True for the liquid (not ice) blocks of a column's water. */
    private static boolean isWaterBlock(Column c, int y) {
        return c.land && c.hasWater() && y > c.top && y <= c.waterTop && !(c.frozen && y == c.waterTop)
                && !(c.hasArch() && y >= c.archBottom && y <= c.archTop);
    }

    /**
     * Water the game must tick once the chunk is loaded: spill columns (narrow waterfall lips), blocks with an open side
     * (the 1-block terrace steps become short flowing slopes), and the surface where the neighbour's water level differs.
     */
    private static boolean needsFlow(Neighbourhood cols, Column c, int dx, int dz, int y) {
        if (c.spill) {
            return true;
        }
        for (int i = 0; i < 4; i++) {
            Column n = cols.get(dx + NX[i], dz + NZ[i]);
            if (!occupied(n, y)) {
                return true;
            }
            if (y == c.waterTop && n.land && n.hasWater() && n.waterTop != c.waterTop) {
                return true;
            }
        }
        return false;
    }

    private static final int[] NX = {1, -1, 0, 0};
    private static final int[] NZ = {0, 0, 1, -1};

    /** Solid or water at height y according to the core (the shape, not the material: a carved AIR material counts as solid). */
    private static boolean occupied(Column n, int y) {
        if (n.hasArch() && y >= n.archBottom && y <= n.archTop) {
            return true;
        }
        if (!n.land) {
            return false;
        }
        return (y >= n.bottom && y <= n.top) || (n.hasWater() && y > n.top && y <= n.waterTop);
    }

    /** Lowest y with anything in the column (hang dressing included), or MAX_VALUE if empty. */
    private static int lowest(Column c) {
        int lo = Integer.MAX_VALUE;
        if (c.land) {
            lo = c.bottom - Math.max(0, c.hangLength);
        }
        if (c.hasArch()) {
            lo = Math.min(lo, c.archBottom);
        }
        return lo;
    }

    /** Highest y with anything in the column, or MIN_VALUE if empty. */
    private static int highest(Column c) {
        int hi = Integer.MIN_VALUE;
        if (c.land) {
            hi = c.hasWater() ? Math.max(c.top, c.waterTop) : c.top;
        }
        if (c.hasArch()) {
            hi = Math.max(hi, c.archTop);
        }
        return hi;
    }

    /** The block the core puts at (x, y, z) of an already sampled column. Used by the chunk fill and the base column alike. */
    static BlockState stateAt(ElysiumWorld world, Column c, int x, int y, int z) {
        if (c.hasArch() && y >= c.archBottom && y <= c.archTop) {
            return ElysiumBlocks.state(world.material(c, y));
        }
        if (!c.land) {
            return AIR;
        }
        if (y >= c.bottom && y <= c.top) {
            return ElysiumBlocks.state(world.material(c, y));
        }
        if (y > c.top) {
            if (c.hasWater() && y <= c.waterTop) {
                return c.frozen && y == c.waterTop ? ElysiumBlocks.state(Mat.ICE) : ElysiumBlocks.water();
            }
            return AIR;
        }
        int below = c.bottom - y; // 1 = directly under the island
        if (c.hangLength > 0 && below >= 1 && below <= c.hangLength) {
            return hangState(world, c, x, y, z, below);
        }
        return AIR;
    }

    /** Underside dressing. Roots and lichen are a single block under the rock; vines hang the whole length with a tip. */
    private static BlockState hangState(ElysiumWorld world, Column c, int x, int y, int z, int below) {
        Mat m = c.hangMat;
        switch (m) {
            case AIR:
            case WATER:
            case MOSS_CARPET:
            case SNOW_LAYER:
                return AIR; // nothing that can hang
            case HANGING_ROOTS:
            case GLOW_LICHEN_CEIL:
                return below == 1 ? ElysiumBlocks.state(m) : AIR;
            case CAVE_VINES:
                return ElysiumBlocks.caveVine(below == c.hangLength, hash01(world.seed, x, y, z) < VINE_BERRY_CHANCE);
            default:
                return ElysiumBlocks.state(m); // solid drip (stalactite rock)
        }
    }

    /** Deterministic value in [0,1) for a block. */
    static double hash01(long seed, int x, int y, int z) {
        long h = seed ^ 0x9E3779B97F4A7C15L;
        h ^= x * 0xC2B2AE3D27D4EB4FL;
        h = Long.rotateLeft(h, 31) * 0x165667B19E3779F9L;
        h ^= y * 0x27D4EB2F165667C5L;
        h = Long.rotateLeft(h, 29) * 0x9E3779B97F4A7C15L;
        h ^= z * 0x94D049BB133111EBL;
        h ^= h >>> 32;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h >>> 11) * 0x1.0p-53;
    }

    /** The 16x16 columns of a chunk plus a 1-column ring around it (sampled lazily, only when water needs its neighbours). */
    private static final class Neighbourhood {
        private final ElysiumWorld world;
        private final int x0, z0;
        private final Column[] cols = new Column[18 * 18];

        Neighbourhood(ElysiumWorld world, int x0, int z0) {
            this.world = world;
            this.x0 = x0;
            this.z0 = z0;
            for (int dz = 0; dz < 16; dz++) {
                for (int dx = 0; dx < 16; dx++) {
                    get(dx, dz);
                }
            }
        }

        Column get(int dx, int dz) {
            int i = (dz + 1) * 18 + (dx + 1);
            Column c = cols[i];
            if (c == null) {
                c = new Column();
                world.sample(x0 + dx, z0 + dz, c);
                cols[i] = c;
            }
            return c;
        }
    }

    // ------------------------------------------------------------------------------------------------ queries

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        ElysiumWorld world = ElysiumSeed.world();
        Column c = new Column();
        world.sample(x, z, c);
        int hi = highest(c);
        if (hi == Integer.MIN_VALUE) {
            return level.getMinBuildHeight();
        }
        Predicate<BlockState> opaque = type.isOpaque();
        int lo = Math.max(lowest(c), level.getMinBuildHeight());
        for (int y = Math.min(hi, level.getMaxBuildHeight() - 1); y >= lo; y--) {
            if (opaque.test(stateAt(world, c, x, y, z))) {
                return y + 1;
            }
        }
        return level.getMinBuildHeight();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        ElysiumWorld world = ElysiumSeed.world();
        Column c = new Column();
        world.sample(x, z, c);
        int minY = level.getMinBuildHeight();
        BlockState[] states = new BlockState[level.getHeight()];
        for (int i = 0; i < states.length; i++) {
            states[i] = stateAt(world, c, x, minY + i, z);
        }
        return new NoiseColumn(minY, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        ElysiumWorld world = ElysiumSeed.world();
        Column c = new Column();
        world.sample(pos.getX(), pos.getZ(), c);
        info.add(String.format(Locale.ROOT, "El'ysium island %d %s inland %.0f", c.island, c.biome.id, c.inland));
        info.add(String.format(Locale.ROOT, "El'ysium column land=%s top=%d bottom=%d water=%s biome@y=%s", c.land,
                c.land ? c.top : 0, c.land ? c.bottom : 0, c.hasWater() ? Integer.toString(c.waterTop) : "-",
                world.biomeAt(pos.getX(), pos.getY(), pos.getZ()).id));
    }

    // ------------------------------------------------------------------------------------------------ decoration

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        super.applyBiomeDecoration(level, chunk, structureManager);
        ElysiumDecorator.decorate(level, chunk, ElysiumSeed.world());
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
        // the core already chose every surface block
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager,
                             StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        // no carvers: caves would cut through the thin undersides
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        ChunkPos chunkPos = region.getCenter();
        Holder<Biome> biome = region.getBiome(chunkPos.getWorldPosition().atY(region.getMaxBuildHeight() - 1));
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
        random.setDecorationSeed(region.getSeed(), chunkPos.getMinBlockX(), chunkPos.getMinBlockZ());
        NaturalSpawner.spawnMobsForChunkGeneration(region, biome, chunkPos, random);
    }

    // ------------------------------------------------------------------------------------------------ frame

    @Override
    public int getGenDepth() {
        return ElysiumWorld.HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        // Nothing in El'ysium uses a sea level; 0 keeps vanilla's sea-level logic (drowned, squid bands) out of the islands.
        return ElysiumWorld.MIN_Y;
    }

    @Override
    public int getMinY() {
        return ElysiumWorld.MIN_Y;
    }
}
