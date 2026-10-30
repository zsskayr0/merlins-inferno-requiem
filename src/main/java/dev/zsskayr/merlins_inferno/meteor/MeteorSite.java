package dev.zsskayr.merlins_inferno.meteor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
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
 * Builds a fallen star: a scorched bowl in the ground, a small geode of Luminite, Amethyst and Lyrium crystals at its heart, and a chest
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
    /** Radii of the heart geode: its hollow, the lining around it and the basalt shell outside. */
    private static final double GEODE_CAVITY = 2.2;
    private static final double GEODE_LINING = 3.1;
    private static final double GEODE_SHELL = 3.9;

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
        // The core: a small geode, the star's own heart - a basalt shell lined with amethyst and a little Lyrium, hollow inside, its
        // crystals Luminite always and Amethyst and Lyrium sometimes. Its top is left open so it can be reached.
        List<BlockPos> lining = new ArrayList<>();
        BlockPos centre = new BlockPos(x, floorY + 2, z);
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    BlockPos at = centre.offset(dx, dy, dz);
                    if (distance <= GEODE_CAVITY) {
                        level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
                    } else if (dy >= 2 && dx * dx + dz * dz < 3) {
                        level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
                    } else if (distance <= GEODE_LINING) {
                        level.setBlock(at, random.nextInt(8) == 0 ? ModBlocks.LYRIUM_BLOCK.get().defaultBlockState() : Blocks.AMETHYST_BLOCK.defaultBlockState(), 2);
                        lining.add(at);
                    } else if (distance <= GEODE_SHELL) {
                        level.setBlock(at, Blocks.SMOOTH_BASALT.defaultBlockState(), 2);
                    }
                }
            }
        }
        List<BlockPos[]> faces = new ArrayList<>();
        for (BlockPos block : lining) {
            for (Direction side : Direction.values()) {
                BlockPos inside = block.relative(side);
                if (level.getBlockState(inside).isAir() && Math.sqrt(inside.distSqr(centre)) <= GEODE_CAVITY) {
                    faces.add(new BlockPos[] {inside, block});
                }
            }
        }
        Collections.shuffle(faces, new java.util.Random(random.nextLong()));
        int luminite = 3 + random.nextInt(3);
        int amethyst = random.nextInt(4);
        int lyrium = random.nextInt(3);
        for (BlockPos[] face : faces) {
            Block crystal;
            if (luminite > 0) {
                crystal = ModBlocks.LUMINITE_CLUSTER.get();
                luminite--;
            } else if (amethyst > 0) {
                crystal = Blocks.AMETHYST_CLUSTER;
                amethyst--;
            } else if (lyrium > 0) {
                crystal = ModBlocks.LYRIUM_CLUSTER.get();
                lyrium--;
            } else {
                break;
            }
            Direction outward = Direction.getNearest(face[0].getX() - face[1].getX(), face[0].getY() - face[1].getY(), face[0].getZ() - face[1].getZ());
            level.setBlock(face[0], crystal.defaultBlockState().setValue(AmethystClusterBlock.FACING, outward), 2);
        }

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
