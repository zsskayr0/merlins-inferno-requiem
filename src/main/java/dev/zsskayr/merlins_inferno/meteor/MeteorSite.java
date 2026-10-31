package dev.zsskayr.merlins_inferno.meteor;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GeodeBlockSettings;
import net.minecraft.world.level.levelgen.GeodeCrackSettings;
import net.minecraft.world.level.levelgen.GeodeLayerSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.GeodeConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootTable;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;

/**
 * Builds a fallen star the way Applied Energistics 2 builds its meteorites: a sealed geode buried just under the ground, a wide shallow
 * crater cut around its top, and a scatter of scorched fallout over the rim. The crater's kind follows the biome (plain, lava,
 * obsidian, water, snow or ice), and a star that lands in water leaves no crater at all, only the buried geode.
 * <p>
 * The geode is Lyrium-sized (same layers and spread as the Lyrium geodes), closed all round, with a Luminite lining, a middle layer
 * of Arganite flecked with Quartzite and a Smooth Basalt shell. The sky-drop chest sits in its hollow, so it must be dug into.
 */
public final class MeteorSite {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "chests/meteor"));
    /** Once the Third Circle has opened the stars carry more: a bigger haul and one more guard. */
    public static final ResourceKey<LootTable> LOOT_DEEP = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "chests/meteor_deep"));
    /** The chunks within this many blocks of the centre must be loaded before the site is built (the fallout reaches 30). */
    public static final int LOAD_RADIUS = 34;
    private static final int FALLOUT_RADIUS = 30;
    /** The fallout leaves everything this close to the centre alone: the geode and its hollow. */
    private static final int GEODE_REACH = 9;
    /** AE2's "meteorite size": it sets how deep the geode sits and how wide the crater opens. */
    private static final int SIZE = 5;
    private static final double CRATER = (SIZE * 2.0 + 5.0) * (SIZE * 2.0 + 5.0);

    private static GeodeConfiguration geode;

    /** The kinds of crater, with what fills the bowl's floor (null: no crater at all). */
    private enum Crater {
        NONE(null), NORMAL(Blocks.AIR), LAVA(Blocks.LAVA), OBSIDIAN(Blocks.OBSIDIAN), WATER(Blocks.WATER), SNOW(Blocks.SNOW_BLOCK), ICE(Blocks.ICE);

        final Block filler;

        Crater(Block filler) {
            this.filler = filler;
        }
    }

    /** A built site: the height of the geode's centre, and whether Templars were left to guard it. */
    public record Built(int y, boolean guarded) {
    }

    /** The chance a fallen star has the Cult of El at its crater. */
    public static final double GUARD_CHANCE = 0.5;

    private MeteorSite() {
    }

    /** True when every chunk the crater and its fallout touch is loaded. */
    public static boolean canBuild(ServerLevel level, int x, int z) {
        return level.hasChunksAt(new BlockPos(x - LOAD_RADIUS, level.getMinBuildHeight(), z - LOAD_RADIUS),
                new BlockPos(x + LOAD_RADIUS, level.getMinBuildHeight(), z + LOAD_RADIUS));
    }

    /** Builds the site around (x, z). */
    public static Built build(ServerLevel level, int x, int z, RandomSource random) {
        int yOffset = SIZE + 1;
        // Where the ground is, over the meteorite's footprint: its mean, pulled down towards the lowest point on rough ground.
        int scan = SIZE * 2;
        double sum = 0;
        double sumSquares = 0;
        int min = Integer.MAX_VALUE;
        int count = 0;
        for (int dx = -scan; dx <= scan; dx++) {
            for (int dz = -scan; dz <= scan; dz++) {
                int h = groundAt(level, x + dx, z + dz);
                sum += h;
                sumSquares += (double) h * h;
                min = Math.min(min, h);
                count++;
            }
        }
        double mean = sum / count;
        int centreY = (int) mean;
        if (sumSquares / count - mean * mean > 5.0) {
            centreY = (int) (centreY - (mean - min) * 0.75);
        }
        centreY = Math.max(level.getMinBuildHeight() + yOffset + 8, centreY - yOffset);
        BlockPos centre = new BlockPos(x, centreY, z);

        // A star in the water leaves no crater. Otherwise the biome picks the kind.
        boolean wet = level.getFluidState(new BlockPos(x, groundAt(level, x, z) + 1, z)).is(net.minecraft.tags.FluidTags.WATER);
        Crater crater = wet ? Crater.NONE : craterFor(level, centre, random);
        if (crater != Crater.NONE) {
            carveCrater(level, centre, crater);
        }
        BlockPos chest = placeGeode(level, centre, random);
        if (crater != Crater.NONE) {
            decay(level, centre, crater, random);
            if (lakeAround(level, centre)) {
                fillLake(level, centre, crater, random);
            }
        }

        boolean deep = ProgressionHelper.hasReached(level, ProgressionHelper.THIRD_CIRCLE);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random)), 2);
        RandomizableContainer.setBlockEntityLootTable(level, random, chest, deep ? LOOT_DEEP : LOOT);

        boolean guarded = crater != Crater.NONE && random.nextDouble() < GUARD_CHANCE;
        if (guarded) {
            for (int i = 0; i < (deep ? 3 : 2); i++) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double distance = 8 + random.nextDouble() * 5;
                int gx = x + (int) (Math.cos(angle) * distance);
                int gz = z + (int) (Math.sin(angle) * distance);
                BlockPos at = new BlockPos(gx, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, gx, gz), gz);
                Mob guard = ModEntityTypes.TEMPLAR.get().spawn(level, at, MobSpawnType.EVENT);
                if (guard != null) {
                    guard.setPersistenceRequired();
                }
            }
        }
        return new Built(centreY, guarded);
    }

    /** The top of the solid ground in a column, under water, snow cover, leaves and trunks. */
    private static int groundAt(ServerLevel level, int x, int z) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        while (cursor.getY() > level.getMinBuildHeight() + 8) {
            BlockState below = level.getBlockState(cursor.below());
            if (below.isSolid() && !below.is(BlockTags.LEAVES) && !below.is(BlockTags.LOGS) && !below.is(BlockTags.SNOW)) {
                break;
            }
            cursor.move(Direction.DOWN);
        }
        return cursor.getY();
    }

    /** AE2's choice of crater, by temperature and rain: most are plain, the rest depend on how hot or cold the biome is. */
    private static Crater craterFor(ServerLevel level, BlockPos pos, RandomSource random) {
        Biome biome = level.getBiome(pos).value();
        float temp = biome.getBaseTemperature();
        if (random.nextFloat() <= 0.5F) {
            return Crater.NORMAL;
        }
        boolean canSnow = biome.coldEnoughToSnow(pos);
        if (temp >= 1.0F) {
            boolean lava = random.nextFloat() > 0.5F;
            if (!biome.hasPrecipitation()) {
                return lava ? Crater.LAVA : Crater.NORMAL;
            }
            if (!canSnow) {
                Crater molten = random.nextFloat() > 0.75F ? Crater.OBSIDIAN : Crater.LAVA;
                return lava ? molten : Crater.NORMAL;
            }
        }
        if (temp < 1.0F && temp >= 0.2) {
            boolean lake = random.nextFloat() > 0.25F;
            boolean lava = random.nextFloat() > 0.8F;
            if (!biome.hasPrecipitation()) {
                return lava ? Crater.LAVA : Crater.NORMAL;
            }
            if (!canSnow) {
                Crater molten = random.nextFloat() > 0.75F ? Crater.OBSIDIAN : Crater.LAVA;
                return lava ? molten : lake ? Crater.WATER : Crater.NORMAL;
            }
            Crater water = lake ? Crater.WATER : Crater.NORMAL;
            return random.nextFloat() > 0.75F ? Crater.SNOW : water;
        }
        if (temp < 0.2) {
            boolean lake = random.nextFloat() > 0.25F;
            boolean lava = random.nextFloat() > 0.95F;
            boolean frozen = random.nextFloat() > 0.25F;
            if (!biome.hasPrecipitation()) {
                return lava ? Crater.LAVA : Crater.NORMAL;
            }
            if (!canSnow) {
                Crater water = frozen ? Crater.ICE : Crater.WATER;
                return lava ? Crater.LAVA : lake ? water : Crater.NORMAL;
            }
            return lava ? Crater.LAVA : lake ? Crater.SNOW : Crater.NORMAL;
        }
        return Crater.NORMAL;
    }

    /** The crater: a very flat cone (rising 0.02 a block squared) opened from the geode's flank up to the sky. */
    private static void carveCrater(ServerLevel level, BlockPos centre, Crater crater) {
        BlockState filler = crater.filler.defaultBlockState();
        double h = centre.getY() - SIZE + 1;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dx = -FALLOUT_RADIUS; dx <= FALLOUT_RADIUS; dx++) {
            for (int dz = -FALLOUT_RADIUS; dz <= FALLOUT_RADIUS; dz++) {
                double rim = h + (double) (dx * dx + dz * dz) * 0.02;
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, centre.getX() + dx, centre.getZ() + dz);
                int from = Math.max(centre.getY() - 5, (int) Math.floor(rim) + 1);
                for (int y = from; y <= top; y++) {
                    if (y <= rim) {
                        continue;
                    }
                    at.set(centre.getX() + dx, y, centre.getZ() + dz);
                    BlockState current = level.getBlockState(at);
                    if (crater != Crater.NORMAL && y < centre.getY() && current.isSolid()) {
                        put(level, at, filler);
                    } else {
                        put(level, at, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
    }

    /** Places the sealed geode with the vanilla geode feature and returns a hollow spot inside it for the chest. */
    private static BlockPos placeGeode(ServerLevel level, BlockPos centre, RandomSource random) {
        Feature.GEODE.place(new FeaturePlaceContext<>(Optional.empty(), level, level.getChunkSource().getGenerator(), random, centre, geodeConfig()));
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-3, -3, -3), centre.offset(3, 3, 3))) {
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).isSolid()) {
                double distance = pos.distSqr(centre);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = pos.immutable();
                }
            }
        }
        return best != null ? best : centre;
    }

    /**
     * Lyrium-sized and closed: the Lyrium geode's layers and spread, no crack, Luminite for the lining (a little Lyrium and Amethyst in
     * it), Arganite flecked with Quartzite in place of the calcite layer, and a Smooth Basalt shell. Nothing counts as an invalid block,
     * so it forms under water and lava too.
     */
    private static GeodeConfiguration geodeConfig() {
        if (geode == null) {
            BlockState arganite = ModBlocks.ARGANITE.get().defaultBlockState();
            BlockState quartzite = ModBlocks.QUARTZITE.get().defaultBlockState();
            geode = new GeodeConfiguration(
                    new GeodeBlockSettings(
                            BlockStateProvider.simple(Blocks.AIR),
                            BlockStateProvider.simple(ModBlocks.LUMINITE_BLOCK.get()),
                            BlockStateProvider.simple(ModBlocks.LYRIUM_BLOCK.get()),
                            new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder().add(arganite, 7).add(quartzite, 3).build()),
                            BlockStateProvider.simple(Blocks.SMOOTH_BASALT),
                            List.of(ModBlocks.LUMINITE_CLUSTER.get().defaultBlockState(), ModBlocks.LUMINITE_CLUSTER.get().defaultBlockState(),
                                    ModBlocks.LUMINITE_CLUSTER.get().defaultBlockState(), ModBlocks.LYRIUM_CLUSTER.get().defaultBlockState(),
                                    Blocks.AMETHYST_CLUSTER.defaultBlockState()),
                            BlockTags.FEATURES_CANNOT_REPLACE,
                            BlockTags.GEODE_INVALID_BLOCKS),
                    new GeodeLayerSettings(1.7, 2.2, 3.2, 4.2),
                    new GeodeCrackSettings(0.0, 2.0, 2),
                    0.35, 0.25, false,
                    UniformInt.of(4, 6), UniformInt.of(3, 4), UniformInt.of(1, 2),
                    -16, 16, 0.05, Integer.MAX_VALUE);
        }
        return geode;
    }

    /** The fallout: debris dropped over the crater's rim, and the rim's own ground scorched. */
    private static void decay(ServerLevel level, BlockPos centre, Crater crater, RandomSource random) {
        double budget = 0;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos up = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos down = new BlockPos.MutableBlockPos();
        for (int i = centre.getX() - FALLOUT_RADIUS; i <= centre.getX() + FALLOUT_RADIUS; i++) {
            for (int k = centre.getZ() - FALLOUT_RADIUS; k <= centre.getZ() + FALLOUT_RADIUS; k++) {
                for (int j = centre.getY() - 9; j < centre.getY() + FALLOUT_RADIUS; j++) {
                    if (centre.distSqr(new BlockPos(i, j, k)) <= GEODE_REACH * GEODE_REACH) {
                        continue;
                    }
                    at.set(i, j, k);
                    up.set(i, j + 1, k);
                    down.set(i, j - 1, k);
                    BlockState state = level.getBlockState(at);
                    if (state.canBeReplaced()) {
                        if (!level.isEmptyBlock(up)) {
                            level.setBlock(at, level.getBlockState(up), 2);
                        } else if (budget < 100.0 * CRATER) {
                            double dx = i - centre.getX();
                            double dy = j - centre.getY();
                            double dz = k - centre.getZ();
                            double dist = dx * dx + dy * dy + dz * dz;
                            BlockState under = level.getBlockState(down);
                            if (!under.canBeReplaced()) {
                                double height = CRATER * (random.nextDouble() * 0.6 + 0.2) - Math.abs(dist - CRATER * 1.7);
                                if (!under.isAir() && height > 0.0 && random.nextDouble() > 0.6) {
                                    budget++;
                                    put(level, at, scorched(random, crater));
                                }
                            }
                        }
                    } else if (level.isEmptyBlock(up) && random.nextDouble() > 0.4 && isNaturalGround(state)) {
                        double dx = i - centre.getX();
                        double dy = j - centre.getY();
                        double dz = k - centre.getZ();
                        if ((Math.abs(dx) > 1 || Math.abs(dy) > 1 || Math.abs(dz) > 1) && dx * dx + dy * dy + dz * dz < CRATER * 1.6) {
                            put(level, at, scorched(random, crater));
                        }
                    }
                }
            }
        }
    }

    /** Only plain ground is scorched; the geode, ores and structures are left as they are. */
    private static boolean isNaturalGround(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.SNOW_BLOCK);
    }

    private static BlockState scorched(RandomSource random, Crater crater) {
        int roll = random.nextInt(10);
        if (roll < 4) {
            return Blocks.SMOOTH_BASALT.defaultBlockState();
        }
        if (roll < 7) {
            return Blocks.BLACKSTONE.defaultBlockState();
        }
        if (roll == 7) {
            return crater == Crater.LAVA ? Blocks.MAGMA_BLOCK.defaultBlockState() : ModBlocks.ARGANITE.get().defaultBlockState();
        }
        return Blocks.COARSE_DIRT.defaultBlockState();
    }

    /** True when the crater's slope cuts below sea level next to open water: it then fills as a lake. */
    private static boolean lakeAround(ServerLevel level, BlockPos centre) {
        int maxY = level.getSeaLevel() - 1;
        double h = centre.getY() - SIZE + 1;
        for (int dx = -32; dx <= 32; dx++) {
            for (int dz = -32; dz <= 32; dz++) {
                double distance = dx * dx + dz * dz;
                if (maxY > h + distance * 0.0175 && maxY < h + distance * 0.02
                        && level.getHeight(Heightmap.Types.OCEAN_FLOOR, centre.getX() + dx, centre.getZ() + dz) < level.getSeaLevel()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Floods the crater up to sea level and lines its slope so the water stays in. */
    private static void fillLake(ServerLevel level, BlockPos centre, Crater crater, RandomSource random) {
        int maxY = level.getSeaLevel() - 1;
        double h = centre.getY() - SIZE + 1;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = centre.getX() - FALLOUT_RADIUS; x <= centre.getX() + FALLOUT_RADIUS; x++) {
            for (int z = centre.getZ() - FALLOUT_RADIUS; z <= centre.getZ() + FALLOUT_RADIUS; z++) {
                double distance = (x - centre.getX()) * (x - centre.getX()) + (z - centre.getZ()) * (z - centre.getZ());
                for (int y = centre.getY() - 5; y <= maxY; y++) {
                    at.set(x, y, z);
                    if (y > h + distance * 0.02) {
                        if (level.getBlockState(at).isAir()) {
                            level.setBlock(at, Blocks.WATER.defaultBlockState(), 2);
                            if (y == maxY) {
                                level.scheduleTick(at.immutable(), Fluids.WATER, 0);
                            }
                        }
                    } else if (maxY + (maxY - y) * 2 + 2 > h + distance * 0.02) {
                        lineSlope(level, at, crater, random);
                    }
                }
            }
        }
    }

    private static void lineSlope(ServerLevel level, BlockPos from, Crater crater, RandomSource random) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos().set(from);
        for (int i = 0; i < 20; i++) {
            BlockState state = level.getBlockState(at);
            if (state.isAir() || state.getFluidState().isEmpty() && (state.canBeReplaced() || state.is(BlockTags.REPLACEABLE))) {
                put(level, at, crater == Crater.LAVA && random.nextFloat() < 0.075F ? Blocks.MAGMA_BLOCK.defaultBlockState() : scorched(random, crater));
                at.move(Direction.DOWN);
            } else {
                return;
            }
        }
    }

    private static void put(ServerLevel level, BlockPos pos, BlockState state) {
        BlockState original = level.getBlockState(pos);
        if (!original.is(Blocks.BEDROCK) && original != state) {
            level.setBlock(pos, state, 2);
        }
    }
}
