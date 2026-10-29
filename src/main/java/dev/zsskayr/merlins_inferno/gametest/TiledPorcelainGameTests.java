package dev.zsskayr.merlins_inferno.gametest;

import dev.zsskayr.merlins_inferno.block.TiledPorcelainBlock;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tiled Porcelain pairs up from wherever the builder starts, in either direction, however the blocks are set. */
@GameTestHolder("merlins_inferno")
@PrefixGameTestTemplate(false)
public final class TiledPorcelainGameTests {
    private static void place(GameTestHelper helper, int x) {
        helper.setBlock(new BlockPos(x, 2, 2), ModBlocks.TILED_PORCELAIN.get());
    }

    private static String row(GameTestHelper helper, int from, int to) {
        StringBuilder out = new StringBuilder();
        for (int x = from; x <= to; x++) {
            out.append(helper.getBlockState(new BlockPos(x, 2, 2)).getValue(TiledPorcelainBlock.SECOND_X) ? '2' : '1');
        }
        return out.toString();
    }

    @GameTest(template = "bench_empty")
    public static void rowStartedInTheMiddleMakesWholePairs(GameTestHelper helper) {
        for (int x : new int[] {3, 4, 5, 6, 2, 1}) {
            place(helper, x);
        }
        helper.assertTrue(row(helper, 1, 6).equals("121212"), "Three pairs from a middle start, got " + row(helper, 1, 6));
        helper.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void rowBuiltLeftwardMakesWholePairs(GameTestHelper helper) {
        for (int x : new int[] {6, 5, 4, 3, 2, 1}) {
            place(helper, x);
        }
        helper.assertTrue(row(helper, 1, 6).equals("121212"), "Three pairs building leftward, got " + row(helper, 1, 6));
        helper.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void filledFloorMakesTwoByTwoTiles(GameTestHelper helper) {
        for (int z = 1; z <= 4; z++) {
            for (int x = 1; x <= 4; x++) {
                helper.setBlock(new BlockPos(x, 2, z), ModBlocks.TILED_PORCELAIN.get());
            }
        }
        for (int z = 1; z <= 4; z++) {
            for (int x = 1; x <= 4; x++) {
                BlockState state = helper.getBlockState(new BlockPos(x, 2, z));
                helper.assertTrue(state.getValue(TiledPorcelainBlock.SECOND_X) == (x % 2 == 0), "x half at " + x + "," + z);
                helper.assertTrue(state.getValue(TiledPorcelainBlock.SECOND_Z) == (z % 2 == 0), "z half at " + x + "," + z);
            }
        }
        helper.succeed();
    }
}
