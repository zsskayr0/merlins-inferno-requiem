package dev.zsskayr.merlins_inferno.gametest;

import dev.zsskayr.merlins_inferno.block.TiledPorcelainBlock;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tiled Porcelain pairs up from wherever the builder starts, in either direction. */
@GameTestHolder("merlins_inferno")
@PrefixGameTestTemplate(false)
public final class TiledPorcelainGameTests {
    private static void place(GameTestHelper helper, int x) {
        BlockPos pos = new BlockPos(x, 2, 2);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(ModBlocks.TILED_PORCELAIN.get());
        BlockPlaceContext context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(helper.absolutePos(pos).getCenter(), Direction.UP, helper.absolutePos(pos), false));
        BlockState state = ModBlocks.TILED_PORCELAIN.get().getStateForPlacement(context);
        helper.getLevel().setBlock(helper.absolutePos(pos), state, 3);
        ModBlocks.TILED_PORCELAIN.get().setPlacedBy(helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos), player, stack);
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
}
