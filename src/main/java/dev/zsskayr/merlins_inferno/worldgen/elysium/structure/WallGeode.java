package dev.zsskayr.merlins_inferno.worldgen.elysium.structure;

import dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumWorld;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** A cut-open geode embedded in an island cliff, with a small two-level refuge inside. */
public final class WallGeode {
    private WallGeode() {}

    // The visible rim was transcribed row by row from docs/elysium/wall_geode_reference.jpg.
    // The screenshot uses a 24-pixel block grid, x=17+24*column, y=24*row.
    // Rows 2..28: calcite at each outer edge, then the irregular amethyst band.
    private static final int[] LEFT = {
            11, 10, 8, 7, 6, 5, 4, 3, 3, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 4, 4, 5, 6, 7, 8, 10, 12
    };
    private static final int[] RIGHT = {
            17, 18, 20, 21, 22, 23, 24, 25, 25, 26, 26, 26, 26, 26, 26, 26, 25, 25, 25, 24, 24, 23, 22, 21, 20, 18, 16
    };
    private static final int[] BAND = {
            3, 2, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 4, 4, 4, 4, 4, 3, 3, 3, 4, 3, 2
    };

    public static void build(WorldGenLevel level, ChunkPos chunk, ElysiumWorld.Site site) {
        Brush brush = Brush.chunk(level, chunk);
        Frame frame = new Frame(site.x(), site.z(), site.rot());
        int cy = site.y();

        for (int row = 2; row <= 28; row++) {
            int y = cy + 15 - row;
            int left = LEFT[row - 2], right = RIGHT[row - 2];
            for (int col = left; col <= right; col++) {
                int v = col - 14;
                int edge = Math.min(col - left, right - col);
                for (int depth = 2; depth >= -8; depth--) {
                    if (depth == -8) {
                        write(brush, frame, depth, y, v, edge == 0 ? Blocks.CALCITE.defaultBlockState()
                                : amethyst(site.salt(), col, row, depth));
                    } else if (edge == 0) {
                        write(brush, frame, depth, y, v, Blocks.CALCITE.defaultBlockState());
                    } else if (edge <= BAND[row - 2]) {
                        write(brush, frame, depth, y, v, amethyst(site.salt(), col, row, depth));
                    } else {
                        carve(brush, frame, depth, y, v);
                    }
                }
            }
        }

        // A little stepped amethyst threshold protrudes below the open face.
        for (int v = -4; v <= 4; v++) {
            for (int depth = 2; depth <= 4; depth++) {
                write(brush, frame, depth, cy - 11 - (depth - 2) / 2, v, Blocks.AMETHYST_BLOCK.defaultBlockState());
            }
        }

        buildRefuge(brush, frame, cy, site.salt());
        hangLantern(brush, frame, -4, cy + 7, -4);
        hangLantern(brush, frame, -5, cy + 6, 2);
        hangLantern(brush, frame, -3, cy + 8, 5);

        // Crystal positions follow the clusters visible around the reference opening.
        int[][] buds = {{5, 9}, {5, 17}, {7, 6}, {8, 22}, {10, 5}, {11, 23},
                {13, 4}, {14, 23}, {16, 5}, {18, 22}, {21, 6}, {23, 20}, {25, 10}, {25, 18}};
        for (int[] bud : buds) {
            int row = bud[0], col = bud[1];
            int v = col - 14, y = cy + 15 - row;
            int edge = Math.min(col - LEFT[row - 2], RIGHT[row - 2] - col);
            if (edge >= 1 && edge <= 2) {
                write(brush, frame, 3, y, v, Blocks.AMETHYST_CLUSTER.defaultBlockState()
                        .setValue(AmethystClusterBlock.FACING, frame.fwd()));
            }
        }
        for (int v : new int[] {-5, -1, 3, 6}) {
            write(brush, frame, 3, cy - 10, v, Blocks.AMETHYST_CLUSTER.defaultBlockState()
                    .setValue(AmethystClusterBlock.FACING, Direction.UP));
        }
    }

    private static BlockState amethyst(long salt, int col, int row, int depth) {
        return Rng.unit(salt, col, row, depth) < 0.12
                ? Blocks.BUDDING_AMETHYST.defaultBlockState() : Blocks.AMETHYST_BLOCK.defaultBlockState();
    }

    private static void buildRefuge(Brush brush, Frame frame, int cy, long salt) {
        // Recessed rooms: the outer facade fills the lower middle of the geode, as in the reference.
        for (int depth = -1; depth >= -6; depth--) {
            for (int v = -6; v <= 6; v++) {
                write(brush, frame, depth, cy - 9, v, masonry(salt, depth, cy - 9, v));
                write(brush, frame, depth, cy + 3, v, masonry(salt, depth, cy + 3, v));
                if (depth <= -3 && v >= 1 && v <= 5) {
                    write(brush, frame, depth, cy - 3, v, Blocks.STONE_BRICK_SLAB.defaultBlockState());
                }
                if (v == -6 || v == 6 || depth == -6) {
                    for (int y = cy - 8; y <= cy + 2; y++) {
                        write(brush, frame, depth, y, v, masonry(salt, depth, y, v));
                    }
                }
            }
        }
        for (int v = -6; v <= 6; v++) {
            for (int y = cy - 8; y <= cy + 2; y++) {
                boolean doorway = (v == -1 || v == 0) && y >= cy - 8 && y <= cy - 7;
                boolean upperWindow = (v == -4 || v == -1 || v == 3) && y >= cy && y <= cy + 1;
                boolean lowerWindow = (v == -4 || v == 2) && y >= cy - 6 && y <= cy - 5;
                boolean balconyOpening = (v == 4 || v == 5) && y >= cy - 2 && y <= cy - 1;
                if (doorway || balconyOpening) {
                    carve(brush, frame, -1, y, v);
                } else if (upperWindow || lowerWindow) {
                    write(brush, frame, -1, y, v, Blocks.IRON_BARS.defaultBlockState());
                } else {
                    write(brush, frame, -1, y, v, masonry(salt, -1, y, v));
                }
            }
        }
        door(brush, frame, -1, cy - 8, -1, DoorHingeSide.LEFT);
        door(brush, frame, -1, cy - 8, 0, DoorHingeSide.RIGHT);

        // A small accessible room and upper gallery; the rear cave stays amethyst.
        for (int y = cy - 8; y <= cy + 2; y++) {
            for (int depth = -2; depth >= -5; depth--) {
                for (int v = -5; v <= 5; v++) {
                    if (y != cy - 3 || v < 1) carve(brush, frame, depth, y, v);
                }
            }
        }
        brush.chest(frame.x(-5, 4), cy - 8, frame.z(-5, 4), frame.fwd(), BuiltInLootTables.SIMPLE_DUNGEON, salt);
        write(brush, frame, -4, cy - 8, -4, Blocks.CRAFTING_TABLE.defaultBlockState());
        write(brush, frame, -4, cy - 8, -3, Blocks.BARREL.defaultBlockState());
    }

    private static BlockState masonry(long salt, int depth, int y, int v) {
        double r = Rng.unit(salt ^ 0x57A7E, depth, y, v);
        if (r < 0.69) return Blocks.STONE_BRICKS.defaultBlockState();
        if (r < 0.85) return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        if (r < 0.95) return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        return Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
    }

    private static void door(Brush brush, Frame frame, int depth, int y, int v, DoorHingeSide hinge) {
        BlockState lower = Blocks.DARK_OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, frame.fwd()).setValue(DoorBlock.HINGE, hinge)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        write(brush, frame, depth, y, v, lower);
        write(brush, frame, depth, y + 1, v, lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private static void hangLantern(Brush brush, Frame frame, int depth, int y, int v) {
        write(brush, frame, depth, y + 2, v, Blocks.AMETHYST_BLOCK.defaultBlockState());
        write(brush, frame, depth, y + 1, v, Pal.CHAIN_Y);
        write(brush, frame, depth, y, v, Pal.LANTERN_HANGING);
    }

    private static void write(Brush brush, Frame frame, int depth, int y, int v, BlockState state) {
        int x = frame.x(depth, v), z = frame.z(depth, v);
        if (brush.inside(x, y, z) && brush.get(x, y, z).getFluidState().isEmpty()) {
            brush.set(x, y, z, state);
        }
    }

    private static void carve(Brush brush, Frame frame, int depth, int y, int v) {
        brush.clear(frame.x(depth, v), y, frame.z(depth, v));
    }
}
