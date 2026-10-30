package dev.zsskayr.merlins_inferno.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumTerrain;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumTerrains;

/**
 * El'ysium's rivers. They are one network over the whole sky (see {@link ElysiumTerrain#riverAt}): where a line crosses
 * an island it is dug in as a valley, a bowl of a bed deepest in the middle, a gravel waterline, banks that rise gently
 * and keep the ground's own skin, with reeds and lily pads along it. Where the island ends the water simply pours off
 * the edge, and the same river comes up again on the next one.
 *
 * <p>Each column works from its own coordinates, and the level of the water is taken from the ground around the nearest
 * point of the river's line, so a river holds one level across its width and steps down along its length.
 */
public class ElysiumRiverFeature extends Feature<NoneFeatureConfiguration> {
    private static final double BANK = 24.0;

    public ElysiumRiverFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    private static double rand(int x, int y, int z, int salt) {
        long h = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L ^ salt * 0x27D4EB2F165667C5L;
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return (h >>> 11) / (double) (1L << 53);
    }

    /**
     * The river bed, after Dynamic Waters' bank styles: fast narrow stretches run over rapids of gravel and cobble, slow
     * wide ones silt up into sand and clay, and the material changes in patches a few blocks across rather than block by block.
     */
    private static BlockState bed(int x, int z, double half) {
        double patch = rand(Math.floorDiv(x, 5), 0, Math.floorDiv(z, 5), 74);
        double roll = rand(x, 3, z, 75);
        boolean rapids = half < 6.0 ? patch < 0.75 : patch < 0.3;
        if (rapids) {
            return (roll < 0.55 ? Blocks.GRAVEL : roll < 0.8 ? Blocks.COBBLESTONE : roll < 0.9 ? Blocks.STONE : Blocks.MOSSY_COBBLESTONE).defaultBlockState();
        }
        return (roll < 0.5 ? Blocks.SAND : roll < 0.75 ? Blocks.CLAY : Blocks.GRAVEL).defaultBlockState();
    }

    /** Bridges, chains and lanterns stand over the water and are never washed away. */
    private static boolean kept(BlockState s) {
        return s.is(ModBlocks.ARGANITE_BRICKS.get()) || s.is(ModBlocks.ARGANITE_BRICKS_WALL.get()) || s.is(Blocks.LANTERN)
                || s.is(Blocks.IRON_BLOCK) || s.is(Blocks.CHAIN);
    }

    /** What the vegetation left standing where the river now runs: trunks, leaves, plants, and the dirt under them. */
    private static boolean growth(BlockState s) {
        return s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.is(BlockTags.REPLACEABLE) || s.is(BlockTags.FLOWERS)
                || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.PODZOL) || s.is(Blocks.MOSS_BLOCK);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ElysiumTerrain terrain = ElysiumTerrains.forLevel(level.getLevel());
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        ElysiumTerrain.Column[] cols = terrain.columns(x0 >> 4, z0 >> 4);
        ElysiumTerrain.Column col;
        ElysiumTerrain.Column at = new ElysiumTerrain.Column();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        boolean any = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                col = cols[dz * 16 + dx];
                if (!col.land || col.weights[ElysiumTerrain.PEAKS] > 0.5 || col.weights[ElysiumTerrain.SEA] > 0.6) {
                    continue;
                }
                ElysiumTerrain.River river = terrain.riverAt(x, z, BANK);
                if (river == null) {
                    continue;
                }
                double d = river.dist(), half = river.half();
                if (d > half + BANK) {
                    continue;
                }
                // the level of the water: the ground around the nearest point of the line, a couple of blocks down
                double sum = 0.0;
                int n = 0;
                int px = (int) Math.round(river.px()), pz = (int) Math.round(river.pz());
                for (int k = 0; k < 5; k++) {
                    int sx = px + (k == 1 ? 12 : k == 2 ? -12 : 0), sz = pz + (k == 3 ? 12 : k == 4 ? -12 : 0);
                    terrain.sample(sx, sz, at);
                    if (at.land) {
                        sum += at.top;
                        n++;
                    }
                }
                int top = (int) col.top;
                int level0 = (int) Math.floor((n > 0 ? sum / n : top) - 2.0);
                // where the ground has fallen away from the river's level the water stops, and spills if it can
                if (top < level0 - 1) {
                    continue;
                }
                BlockState surface = level.getBlockState(pos.set(x, top, z));
                BlockState under = level.getBlockState(pos.set(x, top - 1, z));
                if (d <= half) {
                    double t = d / half;
                    double depth = Math.max(1.0, Math.min(8.0, 0.8 + 0.5 * half));
                    int bed = (int) Math.floor(level0 - depth * (1.0 - t * t));
                    for (int y = bed; y <= Math.max(top, level0); y++) {
                        if (!kept(level.getBlockState(pos.set(x, y, z)))) {
                            level.setBlock(pos, y <= level0 ? water : air, 2);
                        }
                    }
                    for (int y = Math.max(top, level0) + 1; y <= Math.min(ElysiumTerrain.MAX_Y - 1, Math.max(top, level0) + 24); y++) {
                        if (growth(level.getBlockState(pos.set(x, y, z)))) {
                            level.setBlock(pos, y <= level0 ? water : air, 2);
                        }
                    }
                    BlockState bedBlock = bed(x, z, half);
                    level.setBlock(pos.set(x, bed - 1, z), bedBlock, 2);
                    if (t > 0.7) {
                        // the shallows at the edge: sand bars and gravel
                        level.setBlock(pos.set(x, bed, z), rand(x, 4, z, 76) < 0.5 ? Blocks.SAND.defaultBlockState() : Blocks.GRAVEL.defaultBlockState(), 2);
                    }
                    double plant = rand(x, 1, z, 72);
                    if (plant < 0.03) {
                        level.setBlock(pos.set(x, level0 + 1, z), Blocks.LILY_PAD.defaultBlockState(), 2);
                    } else if (plant < 0.11 && bed < level0 - 1) {
                        level.setBlock(pos.set(x, bed, z), Blocks.SEAGRASS.defaultBlockState(), 2);
                    }
                    // the edge of a river that runs off an island, or into a cut: set it flowing
                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        if (level.getBlockState(pos.set(x + dir.getStepX(), level0, z + dir.getStepZ())).isAir()) {
                            level.scheduleTick(new BlockPos(x, level0, z), Fluids.WATER, 0);
                            break;
                        }
                    }
                    any = true;
                } else {
                    // the bank rises gently from the water and keeps the ground's own skin
                    int target = (int) Math.floor(level0 + 1 + (d - half) * 0.35);
                    if (top > target) {
                        for (int y = target + 1; y <= top; y++) {
                            level.setBlock(pos.set(x, y, z), air, 2);
                        }
                        // the shore keeps the ground's own skin for the most part, with patches of gravel and sand
                        double shoreRoll = d < half + 2.5 ? rand(Math.floorDiv(x, 4), 5, Math.floorDiv(z, 4), 73) : 0.0;
                        boolean bare = shoreRoll > 0.55;
                        level.setBlock(pos.set(x, target, z), bare ? (shoreRoll > 0.8 ? Blocks.GRAVEL : Blocks.SAND).defaultBlockState() : surface, 2);
                        level.setBlock(pos.set(x, target - 1, z), bare ? Blocks.GRAVEL.defaultBlockState() : under, 2);
                        any = true;
                    } else if (top < level0 + 1 && d < half + 3.0) {
                        // a low bank: raise it just over the water, so the river holds
                        for (int y = top + 1; y <= level0 + 1; y++) {
                            level.setBlock(pos.set(x, y, z), y == level0 + 1 ? surface : under, 2);
                        }
                        any = true;
                    }
                }
            }
        }
        return any;
    }
}
