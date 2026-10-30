package dev.zsskayr.merlins_inferno.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumTerrain;
import dev.zsskayr.merlins_inferno.worldgen.elysium.ElysiumTerrains;

/**
 * Wears down the slashes (see {@link ElysiumTerrain}) that cut through El'ysium's islands: loose rubble and bare ground
 * along the rim, patches of crumbled stone in the walls, and vines hanging down the cut. Every column decides from its
 * own coordinates, so chunks agree where they meet.
 */
public class ElysiumErosionFeature extends Feature<NoneFeatureConfiguration> {
    private static final double RIM = 14.0;

    public ElysiumErosionFeature() {
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

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ElysiumTerrain terrain = ElysiumTerrains.forLevel(level.getLevel());
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        ElysiumTerrain.Column column = new ElysiumTerrain.Column();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                double d = terrain.slashDistAt(x, z);
                if (d <= 0.0 || d > RIM) {
                    continue;
                }
                terrain.sample(x, z, column);
                if (!column.land) {
                    continue;
                }
                int top = (int) column.top;
                double near = 1.0 - d / RIM;

                // the lip: bare ground and loose stones, thickest right at the edge
                if (rand(x, 0, z, 1) < 0.65 * near * near) {
                    Block ground = switch ((int) (rand(x, 1, z, 2) * 4)) {
                        case 0 -> Blocks.GRAVEL;
                        case 1 -> Blocks.COARSE_DIRT;
                        case 2 -> Blocks.ANDESITE;
                        default -> Blocks.COBBLESTONE;
                    };
                    pos.set(x, top, z);
                    level.setBlock(pos, ground.defaultBlockState(), 2);
                    any = true;
                    if (rand(x, 2, z, 3) < 0.30) {
                        pos.set(x, top + 1, z);
                        if (level.getBlockState(pos).isAir()) {
                            Block rock = rand(x, 3, z, 4) < 0.5 ? Blocks.COBBLESTONE : ModBlocks.ARGANITE.get();
                            level.setBlock(pos, rock.defaultBlockState(), 2);
                        }
                    }
                }

                if (d > 2.5) {
                    continue;
                }
                // the wall: crumbled patches in blobs of a few blocks, down the whole face
                for (int y = (int) column.bottom; y <= top; y++) {
                    double blob = rand(x >> 1, y >> 2, z >> 1, 5);
                    if (blob > 0.22) {
                        continue;
                    }
                    pos.set(x, y, z);
                    if (level.getBlockState(pos).isAir()) {
                        continue;
                    }
                    Block patch = blob < 0.11 ? Blocks.ANDESITE : blob < 0.17 ? Blocks.COBBLESTONE : Blocks.GRAVEL;
                    level.setBlock(pos, patch.defaultBlockState(), 2);
                    any = true;
                }

                // vines hanging over the edge
                if (d > 1.5) {
                    continue;
                }
                for (Direction face : Direction.Plane.HORIZONTAL) {
                    int vx = dx + face.getStepX(), vz = dz + face.getStepZ();
                    if (vx < 0 || vx > 15 || vz < 0 || vz > 15 || rand(x, face.ordinal(), z, 6) > 0.22) {
                        continue;
                    }
                    int len = 3 + (int) (rand(x, 9, z, 7) * 12);
                    BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(face.getOpposite()), true);
                    for (int y = top; y > top - len; y--) {
                        pos.set(x + face.getStepX(), y, z + face.getStepZ());
                        if (!level.getBlockState(pos).isAir()) {
                            break;
                        }
                        level.setBlock(pos, vine, 2);
                        any = true;
                    }
                }
            }
        }
        return any;
    }
}
