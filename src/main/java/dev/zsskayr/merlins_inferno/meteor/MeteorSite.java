package dev.zsskayr.merlins_inferno.meteor;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.compat.lootr.LootrCompat;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;

/**
 * Builds a fallen star: a perfectly round, sealed geode buried just under the ground, a smooth bowl of a crater opened around its top
 * (a little irregular at the rim, scorched towards the middle), debris and fires over the rim. The crater's kind follows the biome
 * (plain, lava, obsidian, water, snow or ice); a star that lands in water leaves no crater, only the buried geode, so the site is
 * moved to dry land first when there is any close by.
 * <p>
 * The geode's shell is Meteorite, then a middle layer of Arganite flecked with Quartzite, then a Luminite lining (a little Lyrium in
 * it) hollow inside, with Luminite, Lyrium and Amethyst clusters growing inwards. The sky-drop chest sits in the hollow, so it must be
 * dug into.
 */
public final class MeteorSite {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "chests/meteor"));
    /** Once the Third Circle has opened the stars carry more: a bigger haul and one more guard. */
    public static final ResourceKey<LootTable> LOOT_DEEP = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "chests/meteor_deep"));
    /** The chunks within this many blocks of the centre must be loaded before the site is built. */
    public static final int LOAD_RADIUS = 40;
    /** A star that would land in water is moved at most this far to dry land. */
    private static final int MAX_SHIFT = 10;
    /** The geode's layers: hollow, Luminite lining, Arganite/Quartzite middle and the Meteorite shell. Exact spheres, so it is always round. */
    private static final double HOLLOW = 3.1;
    private static final double LINING = 4.2;
    private static final double MIDDLE = 5.7;
    private static final double SHELL = 7.0;
    /** How far the geode's centre sits under the bowl's rim level, and how deep the bowl is, so the top of the geode stands in the bowl. */
    private static final int GEODE_SINK = 7;
    private static final double BOWL_DEPTH = 4.0;
    /** How far from the centre the cut reaches on average; the rim's waves push it out to about a quarter more (must stay inside {@link #LOAD_RADIUS}). */
    private static final int CUT_REACH = 26;

    /** A built site: where it ended up, the height of the geode's centre, and whether Templars were left to guard it. */
    public record Built(int x, int y, int z, boolean guarded) {
    }

    /** The chance a fallen star has the Cult of El at its crater. */
    public static final double GUARD_CHANCE = 0.5;

    /** The kinds of crater; the biome picks one, and a star in the water has none. */
    private enum Crater {
        NONE, NORMAL, LAVA, OBSIDIAN, WATER, SNOW, ICE
    }

    private MeteorSite() {
    }

    /** True when every chunk the crater and its debris touch is loaded. */
    public static boolean canBuild(ServerLevel level, int x, int z) {
        return level.hasChunksAt(new BlockPos(x - LOAD_RADIUS, level.getMinBuildHeight(), z - LOAD_RADIUS),
                new BlockPos(x + LOAD_RADIUS, level.getMinBuildHeight(), z + LOAD_RADIUS));
    }

    /** Builds the site around (x, z), or on dry land close to it. */
    public static Built build(ServerLevel level, int x, int z, RandomSource random) {
        int[] dry = dryNear(level, x, z);
        x = dry[0];
        z = dry[1];
        int baseY = footprintGround(level, x, z);
        int centreY = Math.max(level.getMinBuildHeight() + 12, baseY - GEODE_SINK);
        baseY = centreY + GEODE_SINK;
        BlockPos centre = new BlockPos(x, centreY, z);

        // A star in the water leaves no crater. Otherwise the biome picks the kind.
        Crater crater = wet(level, x, z) ? Crater.NONE : craterFor(level, centre, random);
        double radius = 12.0 + random.nextDouble() * 3.0;
        double[] phase = {random.nextDouble() * 6.3, random.nextDouble() * 6.3, random.nextDouble() * 6.3};
        List<BlockPos> fires = new ArrayList<>();
        if (crater != Crater.NONE) {
            carveBowl(level, centre, baseY, radius, phase, crater, random, fires);
        }
        BlockPos chest = placeGeode(level, centre, random);
        if (crater != Crater.NONE) {
            scatterDebris(level, centre, radius, phase, random, fires);
            light(level, fires, random);
        }

        boolean deep = ProgressionHelper.hasReached(level, ProgressionHelper.THIRD_CIRCLE);
        // Lootr's chest when it is installed (every player gets their own haul), the vanilla one otherwise.
        BlockState chestState = LootrCompat.rewardChest().orElse(Blocks.CHEST).defaultBlockState();
        if (chestState.hasProperty(ChestBlock.FACING)) {
            chestState = chestState.setValue(ChestBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }
        level.setBlock(chest, chestState, 2);
        RandomizableContainer.setBlockEntityLootTable(level, random, chest, deep ? LOOT_DEEP : LOOT);

        boolean guarded = crater != Crater.NONE && random.nextDouble() < GUARD_CHANCE;
        if (guarded) {
            for (int i = 0; i < (deep ? 3 : 2); i++) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double distance = radius * (0.6 + random.nextDouble() * 0.5);
                int gx = x + (int) (Math.cos(angle) * distance);
                int gz = z + (int) (Math.sin(angle) * distance);
                BlockPos at = new BlockPos(gx, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, gx, gz), gz);
                Mob guard = ModEntityTypes.TEMPLAR.get().spawn(level, at, MobSpawnType.EVENT);
                if (guard != null) {
                    guard.setPersistenceRequired();
                }
            }
        }
        return new Built(x, centreY, z, guarded);
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

    private static boolean wet(ServerLevel level, int x, int z) {
        return level.getFluidState(new BlockPos(x, groundAt(level, x, z) + 1, z)).is(FluidTags.WATER);
    }

    /** (x, z) itself when it is dry, otherwise the nearest dry spot within {@link #MAX_SHIFT} blocks (or (x, z) again if there is none). */
    private static int[] dryNear(ServerLevel level, int x, int z) {
        if (!wet(level, x, z)) {
            return new int[] {x, z};
        }
        for (int ring = 3; ring <= MAX_SHIFT; ring += 3) {
            for (int step = 0; step < 12; step++) {
                double angle = step * Math.PI / 6.0;
                int cx = x + (int) Math.round(Math.cos(angle) * ring);
                int cz = z + (int) Math.round(Math.sin(angle) * ring);
                if (!wet(level, cx, cz) && groundAt(level, cx, cz) >= level.getSeaLevel()) {
                    return new int[] {cx, cz};
                }
            }
        }
        return new int[] {x, z};
    }

    /** The ground level over the star's footprint: its mean, pulled down towards the lowest point on rough ground. */
    private static int footprintGround(ServerLevel level, int x, int z) {
        double sum = 0;
        double sumSquares = 0;
        int min = Integer.MAX_VALUE;
        int count = 0;
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                int h = groundAt(level, x + dx, z + dz);
                sum += h;
                sumSquares += (double) h * h;
                min = Math.min(min, h);
                count++;
            }
        }
        double mean = sum / count;
        int ground = (int) Math.round(mean);
        if (sumSquares / count - mean * mean > 5.0) {
            ground = (int) Math.round(ground - (mean - min) * 0.5);
        }
        return ground;
    }

    /** The biome picks the kind of crater: most are plain, the rest depend on how hot or cold it is and whether it rains. */
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

    /** The rim's radius towards an angle: a circle bent by a few low waves, so the crater is not a perfect ring. */
    private static double rimRadius(double radius, double[] phase, double angle) {
        return radius * (1.0 + 0.10 * Math.sin(2 * angle + phase[0]) + 0.07 * Math.sin(3 * angle + phase[1]) + 0.05 * Math.sin(5 * angle + phase[2]));
    }

    /**
     * The crater: a smooth bowl, deepest at the geode and rising as a parabola to the ground level at the rim. Past the rim the cut keeps
     * climbing in a long, gentle slope (never steeper than about 40 degrees, like AE2's cone) out to {@link #CUT_REACH} blocks, so on a
     * hillside the bowl melts into the slope instead of ending in a wall. Everything above the cut is cleared (trees too); the bowl
     * is scorched towards the middle and the slope keeps the ground cover it had.
     */
    private static void carveBowl(ServerLevel level, BlockPos centre, int baseY, double radius, double[] phase, Crater crater, RandomSource random, List<BlockPos> fires) {
        double liquidY = baseY - BOWL_DEPTH * 0.55;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int span = CUT_REACH * 5 / 4;
        for (int dx = -span; dx <= span; dx++) {
            for (int dz = -span; dz <= span; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                double rim = rimRadius(radius, phase, Math.atan2(dz, dx));
                if (distance > CUT_REACH * rim / radius) {
                    continue;
                }
                double u = distance / rim;
                double d = u * radius;
                double outside = d - radius;
                double bowl = u <= 1.0 ? baseY - BOWL_DEPTH * (1.0 - u * u) : baseY + 0.8 * outside + 0.008 * outside * outside;
                int floor = (int) Math.floor(bowl);
                int x = centre.getX() + dx;
                int z = centre.getZ() + dz;
                int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (ground - 1 <= floor) {
                    continue;
                }
                at.set(x, ground - 1, z);
                BlockState original = level.getBlockState(at);
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                for (int y = floor + 1; y < top; y++) {
                    at.set(x, y, z);
                    put(level, at, Blocks.AIR.defaultBlockState());
                }
                at.set(x, floor, z);
                BlockState surface = surfaceFor(crater, u, random);
                if (surface != null) {
                    put(level, at, surface);
                    if (surface.is(Blocks.MAGMA_BLOCK) || (surface.is(Blocks.BLACKSTONE) && random.nextInt(14) == 0)) {
                        fires.add(at.immutable());
                    }
                } else if (original.is(BlockTags.DIRT) || original.is(BlockTags.SAND)) {
                    put(level, at, original);
                }
                if ((crater == Crater.LAVA || crater == Crater.WATER) && floor < liquidY) {
                    BlockState liquid = crater == Crater.LAVA ? Blocks.LAVA.defaultBlockState() : Blocks.WATER.defaultBlockState();
                    for (int y = floor + 1; y <= (int) liquidY; y++) {
                        at.set(x, y, z);
                        put(level, at, liquid);
                    }
                }
            }
        }
    }

    /** The bowl's floor: scorched rock and magma at the middle, coarse dirt and gravel towards the rim, left alone beyond it. */
    private static BlockState surfaceFor(Crater crater, double u, RandomSource random) {
        if (u > 1.12) {
            return null;
        }
        int roll = random.nextInt(100);
        if (u < 0.7) {
            switch (crater) {
                case OBSIDIAN:
                    return Blocks.OBSIDIAN.defaultBlockState();
                case SNOW:
                    return Blocks.SNOW_BLOCK.defaultBlockState();
                case ICE:
                    return Blocks.PACKED_ICE.defaultBlockState();
                default:
                    break;
            }
        }
        if (u < 0.35) {
            if (roll < 12) {
                return ModBlocks.METEORITE.get().defaultBlockState();
            }
            if (roll < 12 + (crater == Crater.LAVA ? 28 : 16)) {
                return Blocks.MAGMA_BLOCK.defaultBlockState();
            }
            return roll < 62 ? Blocks.SMOOTH_BASALT.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
        }
        if (u < 0.8) {
            if (roll < 20) {
                return Blocks.SMOOTH_BASALT.defaultBlockState();
            }
            if (roll < 45) {
                return Blocks.BLACKSTONE.defaultBlockState();
            }
            return roll < 80 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
        }
        if (roll < 40) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (roll < 55) {
            return Blocks.BLACKSTONE.defaultBlockState();
        }
        return roll < 65 ? Blocks.GRAVEL.defaultBlockState() : null;
    }

    /** Places the round, sealed geode and returns the spot inside it for the chest. */
    private static BlockPos placeGeode(ServerLevel level, BlockPos centre, RandomSource random) {
        BlockState luminite = ModBlocks.LUMINITE_BLOCK.get().defaultBlockState();
        BlockState lyrium = ModBlocks.LYRIUM_BLOCK.get().defaultBlockState();
        BlockState arganite = ModBlocks.ARGANITE.get().defaultBlockState();
        BlockState quartzite = ModBlocks.QUARTZITE.get().defaultBlockState();
        BlockState meteorite = ModBlocks.METEORITE.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        int reach = (int) Math.ceil(SHELL);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dy = -reach; dy <= reach; dy++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    BlockState state;
                    if (distance <= HOLLOW) {
                        state = air;
                    } else if (distance <= LINING) {
                        state = random.nextInt(8) == 0 ? lyrium : luminite;
                    } else if (distance <= MIDDLE) {
                        state = random.nextInt(10) < 7 ? arganite : quartzite;
                    } else if (distance <= SHELL) {
                        state = meteorite;
                    } else {
                        continue;
                    }
                    put(level, centre.offset(dx, dy, dz), state);
                }
            }
        }
        BlockPos chest = centre.below(3);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos inside = centre.offset(dx, dy, dz);
                    if (inside.equals(chest) || !level.getBlockState(inside).isAir() || random.nextInt(5) != 0) {
                        continue;
                    }
                    Direction towards = null;
                    for (Direction side : Direction.values()) {
                        BlockState neighbour = level.getBlockState(inside.relative(side));
                        if (neighbour.is(ModBlocks.LUMINITE_BLOCK.get()) || neighbour.is(ModBlocks.LYRIUM_BLOCK.get())) {
                            towards = side;
                            break;
                        }
                    }
                    if (towards == null) {
                        continue;
                    }
                    int pick = random.nextInt(5);
                    Block crystal = pick < 3 ? ModBlocks.LUMINITE_CLUSTER.get() : pick == 3 ? ModBlocks.LYRIUM_CLUSTER.get() : Blocks.AMETHYST_CLUSTER;
                    level.setBlock(inside, crystal.defaultBlockState().setValue(AmethystClusterBlock.FACING, towards.getOpposite()), 2);
                }
            }
        }
        return chest;
    }

    /** Debris thrown over the rim: ground scorched or strewn with rock, thinning out with distance. */
    private static void scatterDebris(ServerLevel level, BlockPos centre, double radius, double[] phase, RandomSource random, List<BlockPos> fires) {
        int reach = (int) Math.ceil(radius * 1.2 * 1.7);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                double rim = rimRadius(radius, phase, Math.atan2(dz, dx));
                double u = Math.sqrt(dx * dx + dz * dz) / rim;
                if (u < 1.0 || u > 1.7) {
                    continue;
                }
                double chance = 0.30 * (1.7 - u) / 0.7;
                if (random.nextDouble() >= chance) {
                    continue;
                }
                int x = centre.getX() + dx;
                int z = centre.getZ() + dz;
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                at.set(x, top - 1, z);
                BlockState ground = level.getBlockState(at);
                if (!(ground.is(BlockTags.DIRT) || ground.is(BlockTags.SAND) || ground.is(BlockTags.BASE_STONE_OVERWORLD) || ground.is(Blocks.SNOW_BLOCK))) {
                    continue;
                }
                int roll = random.nextInt(10);
                BlockState scorched = roll < 4 ? Blocks.COARSE_DIRT.defaultBlockState() : roll < 7 ? Blocks.BLACKSTONE.defaultBlockState()
                        : roll < 9 ? Blocks.SMOOTH_BASALT.defaultBlockState() : ModBlocks.METEORITE.get().defaultBlockState();
                put(level, at, scorched);
                if (random.nextInt(18) == 0) {
                    fires.add(at.immutable());
                }
            }
        }
    }

    /** Sets the fires: a flame over every magma block (it never burns out, rain or not) and a few over the other scorched rock. */
    private static void light(ServerLevel level, List<BlockPos> fires, RandomSource random) {
        for (BlockPos below : fires) {
            BlockPos above = below.above();
            boolean magma = level.getBlockState(below).is(Blocks.MAGMA_BLOCK);
            if (level.getBlockState(above).isAir() && (magma || level.getBlockState(below).isSolid()) && (!magma || random.nextInt(3) != 0)) {
                level.setBlock(above, Blocks.FIRE.defaultBlockState(), 3);
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
