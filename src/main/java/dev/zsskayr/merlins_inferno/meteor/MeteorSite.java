package dev.zsskayr.merlins_inferno.meteor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.core.registries.Registries;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;

/**
 * Builds a fallen star: a scorched bowl in the ground, a core of Luminite Ore and a few Lyrium clusters at its heart, and a chest
 * of sky-drop loot at its edge, watched over by a pair of Templars.
 */
public final class MeteorSite {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "chests/meteor"));
    /** Once the Third Circle has opened the stars carry more: a bigger haul and one more guard. */
    public static final ResourceKey<LootTable> LOOT_DEEP = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "chests/meteor_deep"));
    /** Horizontal radius of the bowl; the chunks within this and a margin must be loaded before it is built. */
    public static final int RADIUS = 6;
    private static final int DEPTH = 4;
    private static final int HEIGHT = 7;

    private MeteorSite() {
    }

    /** True when every chunk the crater touches is loaded. */
    public static boolean canBuild(ServerLevel level, int x, int z) {
        return level.hasChunksAt(new BlockPos(x - RADIUS - 2, level.getMinBuildHeight(), z - RADIUS - 2),
                new BlockPos(x + RADIUS + 2, level.getMinBuildHeight(), z + RADIUS + 2));
    }

    /** Builds the site around (x, z) and returns the height of its floor at the centre. */
    public static int build(ServerLevel level, int x, int z, RandomSource random) {
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, ground, z);
        // The heightmap sits above water too: come down to the bed so the crater is dug into the ground, not the sea.
        while (cursor.getY() > level.getMinBuildHeight() + 8 && !level.getBlockState(cursor.below()).isSolid()) {
            cursor.move(Direction.DOWN);
        }
        int surface = cursor.getY();

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > RADIUS + 0.5) {
                    continue;
                }
                int depth = (int) Math.round(DEPTH * (1.0 - distance / (RADIUS + 1.0)));
                int floor = surface - depth;
                for (int y = floor; y < surface + HEIGHT; y++) {
                    level.setBlock(new BlockPos(x + dx, y, z + dz), Blocks.AIR.defaultBlockState(), 2);
                }
                // The scorched floor: the rim is only singed, the bowl itself is glass-hard rock.
                level.setBlock(new BlockPos(x + dx, floor - 1, z + dz), scorched(random, distance), 2);
                if (distance > RADIUS - 1.5) {
                    level.setBlock(new BlockPos(x + dx, floor, z + dz), scorched(random, distance), 2);
                }
            }
        }

        int floorY = surface - DEPTH;
        // The core: a small cluster of ore, half of it Luminite, with Lyrium growing out of the rock around it.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (Math.abs(dx) + Math.abs(dz) == 2 && random.nextBoolean()) {
                    continue;
                }
                BlockState core = random.nextInt(3) == 0 ? Blocks.BLACKSTONE.defaultBlockState() : ModBlocks.LUMINITE_ORE.get().defaultBlockState();
                level.setBlock(new BlockPos(x + dx, floorY, z + dz), core, 2);
            }
        }
        level.setBlock(new BlockPos(x, floorY + 1, z), ModBlocks.LUMINITE_ORE.get().defaultBlockState(), 2);
        int clusters = 2 + random.nextInt(3);
        for (int i = 0; i < clusters; i++) {
            BlockPos at = new BlockPos(x + random.nextInt(5) - 2, floorY + 1, z + random.nextInt(5) - 2);
            if (level.getBlockState(at).isAir() && level.getBlockState(at.below()).isSolid()) {
                level.setBlock(at, ModBlocks.LYRIUM_CLUSTER.get().defaultBlockState(), 2);
            }
        }
        level.setBlock(new BlockPos(x + 2, floorY, z), ModBlocks.LYRIUM_BLOCK.get().defaultBlockState(), 2);

        boolean deep = ProgressionHelper.hasReached(level, ProgressionHelper.THIRD_CIRCLE);
        // The sky drop: a chest on the lip of the bowl.
        BlockPos chest = new BlockPos(x - 3, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x - 3, z), z);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random)), 2);
        RandomizableContainer.setBlockEntityLootTable(level, random, chest, deep ? LOOT_DEEP : LOOT);

        for (int i = 0; i < (deep ? 3 : 2); i++) {
            Mob guard = ModEntityTypes.TEMPLAR.get().spawn(level, chest.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2), MobSpawnType.EVENT);
            if (guard != null) {
                guard.setPersistenceRequired();
            }
        }
        return floorY + 1;
    }

    private static BlockState scorched(RandomSource random, double distance) {
        int roll = random.nextInt(10);
        if (roll < 4) {
            return Blocks.SMOOTH_BASALT.defaultBlockState();
        }
        if (roll < 7) {
            return Blocks.BLACKSTONE.defaultBlockState();
        }
        return distance < 3.5 && roll == 7 ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
    }
}
