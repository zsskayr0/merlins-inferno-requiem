package dev.zsskayr.merlins_inferno.gametest;

import dev.zsskayr.merlins_inferno.block.GateBlock;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("merlins_inferno")
@PrefixGameTestTemplate(false)
public final class GateGameTests {
    private static boolean passes(GameTestHelper helper, BlockState ghost, BlockPos pos, Entity entity) {
        return ghost.getCollisionShape(helper.getLevel(), pos, CollisionContext.of(entity)).isEmpty();
    }

    private static BlockState ghost(GateBlock gate) {
        return gate.defaultBlockState().setValue(GateBlock.VANISHED, true);
    }

    /** Who walks through which ghost gate: player (standing and sneaking), passive, neutral, hostile, and a dropped item. */
    @GameTest(template = "bench_empty")
    public static void whoPasses(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(4, 2, 4));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Player sneaker = helper.makeMockPlayer(GameType.SURVIVAL);
        sneaker.setShiftKeyDown(true);
        Entity cow = helper.spawnWithNoFreeWill(EntityType.COW, 1, 2, 1);
        Entity wolf = helper.spawnWithNoFreeWill(EntityType.WOLF, 2, 2, 1);
        Entity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 3, 2, 1);
        Entity item = new ItemEntity(helper.getLevel(), pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.STICK));

        //                       player sneaker cow    wolf   zombie item
        check(helper, pos, ModBlocks.RUNIC_GATE.get(), new Entity[]{player, sneaker, cow, wolf, zombie, item},
                new boolean[]{true, true, true, true, true, true});
        check(helper, pos, ModBlocks.MUNDANE_GATE.get(), new Entity[]{player, sneaker, cow, wolf, zombie, item},
                new boolean[]{true, false, false, false, false, true});
        check(helper, pos, ModBlocks.DEMONIC_GATE.get(), new Entity[]{player, sneaker, cow, wolf, zombie, item},
                new boolean[]{true, true, true, false, false, true});
        check(helper, pos, ModBlocks.ANGEL_GATE.get(), new Entity[]{player, sneaker, cow, wolf, zombie, item},
                new boolean[]{true, true, false, false, true, true});
        check(helper, pos, ModBlocks.FAERIC_GATE.get(), new Entity[]{player, sneaker, cow, wolf, zombie, item},
                new boolean[]{false, false, true, true, true, true});
        helper.succeed();
    }

    private static void check(GameTestHelper helper, BlockPos pos, GateBlock gate, Entity[] who, boolean[] expected) {
        for (int i = 0; i < who.length; i++) {
            helper.assertTrue(passes(helper, ghost(gate), pos, who[i]) == expected[i],
                    gate.kind() + " gate, entity " + i + " (" + who[i].getType().getDescriptionId() + ") should "
                            + (expected[i] ? "pass" : "be blocked"));
            helper.assertTrue(!passes(helper, gate.defaultBlockState(), pos, who[i]), gate.kind() + " gate is solid when not a ghost");
        }
    }

    /** A click ghosts the whole connected row, and the row turns solid again later. */
    @GameTest(template = "bench_empty", timeoutTicks = 300)
    public static void rippleAndReturn(GameTestHelper helper) {
        var level = helper.getLevel();
        GateBlock gate = ModBlocks.RUNIC_GATE.get();
        BlockPos first = helper.absolutePos(new BlockPos(2, 2, 4));
        for (int i = 0; i < 4; i++) {
            level.setBlock(first.east(i), gate.defaultBlockState(), 3);
        }
        BlockPos other = first.east(5);
        level.setBlock(other, ModBlocks.MUNDANE_GATE.get().defaultBlockState(), 3);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        level.getBlockState(first).useWithoutItem(level, player,
                new BlockHitResult(Vec3.atCenterOf(first), Direction.UP, first, false));
        helper.runAfterDelay(50, () -> {
            for (int i = 0; i < 4; i++) {
                helper.assertTrue(level.getBlockState(first.east(i)).getValue(GateBlock.VANISHED), "Gate " + i + " ghosted by the ripple");
            }
            helper.assertTrue(!level.getBlockState(other).getValue(GateBlock.VANISHED), "A gate of another kind is not rippled");
        });
        helper.runAfterDelay(230, () -> {
            for (int i = 0; i < 4; i++) {
                BlockState s = level.getBlockState(first.east(i));
                helper.assertTrue(!s.getValue(GateBlock.VANISHED) && !s.getValue(GateBlock.ACTIVE) && s.getValue(GateBlock.FADE) == 0,
                        "Gate " + i + " turns solid again");
            }
            helper.succeed();
        });
    }

    /** A gate does not turn solid on top of a creature standing inside it. */
    @GameTest(template = "bench_empty", timeoutTicks = 260)
    public static void waitsForOccupant(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 0, 4));
        level.setBlock(pos, ModBlocks.RUNIC_GATE.get().defaultBlockState(), 3);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        level.getBlockState(pos).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        // the cow walks in once the gate is a ghost (before that, a solid gate would push it out)
        Entity[] cow = new Entity[1];
        helper.runAfterDelay(20, () -> cow[0] = helper.spawnWithNoFreeWill(EntityType.COW, 4, 0, 4));
        helper.runAfterDelay(140, () -> {
            helper.assertTrue(level.getBlockState(pos).getValue(GateBlock.VANISHED), "Still a ghost while the cow stands in it");
            cow[0].discard();
        });
        helper.runAfterDelay(170, () -> {
            helper.assertTrue(!level.getBlockState(pos).getValue(GateBlock.VANISHED), "Solid once the cow is gone");
            helper.succeed();
        });
    }
}
