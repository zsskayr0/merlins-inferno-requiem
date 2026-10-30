package dev.zsskayr.merlins_inferno.gametest;

import dev.zsskayr.merlins_inferno.event.MarkPickupHandler;
import dev.zsskayr.merlins_inferno.mark.MarkHelper;
import dev.zsskayr.merlins_inferno.registry.ModAttachments;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("merlins_inferno")
@PrefixGameTestTemplate(false)
public final class MarkGameTests {
    @GameTest(template = "bench_empty")
    public static void rightClickBindsTheMarkAndConsumesIt(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack mark = new ItemStack(ModItems.MARK_OF_CAIN.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, mark);
        h.assertTrue(!MarkHelper.hasMark(player), "Nobody starts marked");
        ModItems.MARK_OF_CAIN.get().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(MarkHelper.hasMark(player, ModItems.MARK_OF_CAIN.get()), "Right click binds the Mark of Cain");
        h.assertTrue(mark.isEmpty(), "The item is taken in: the player only feels the effects");
        h.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void aMarkedPlayerRefusesAnotherMark(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        MarkHelper.equip(player, ModItems.MARK_OF_CAIN.get());
        ItemStack second = new ItemStack(ModItems.MARK_OF_CAIN.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, second);
        ModItems.MARK_OF_CAIN.get().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(!second.isEmpty(), "A second Mark is not used up");
        h.assertTrue(!MarkHelper.equip(player, ModItems.MARK_OF_CAIN.get()), "A player bears at most one Mark");
        h.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void clearingFreesThePlayer(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        MarkHelper.equip(player, ModItems.MARK_OF_CAIN.get());
        MarkHelper.clear(player);
        h.assertTrue(!MarkHelper.hasMark(player), "A cure removes the Mark");
        h.assertTrue(player.getData(ModAttachments.EQUIPPED_MARK).isEmpty(), "and its state");
        h.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void pickingUpFromTheGroundSometimesBindsTheMark(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var handler = new MarkPickupHandler();
        int bound = 0;
        for (int i = 0; i < 200; i++) {
            player.setData(ModAttachments.EQUIPPED_MARK, "");
            var item = new ItemEntity(h.getLevel(), player.getX(), player.getY(), player.getZ(), new ItemStack(ModItems.MARK_OF_CAIN.get()));
            handler.onPickup(new ItemEntityPickupEvent.Pre(player, item));
            if (MarkHelper.hasMark(player)) {
                bound++;
                h.assertTrue(item.getItem().isEmpty(), "A Mark that bound itself is not left on the ground");
            }
        }
        h.assertTrue(bound > 40 && bound < 120, "About 40% of pickups bind the Mark (" + bound + " of 200)");
        MarkHelper.equip(player, ModItems.MARK_OF_CAIN.get());
        for (int i = 0; i < 50; i++) {
            var item = new ItemEntity(h.getLevel(), player.getX(), player.getY(), player.getZ(), new ItemStack(ModItems.MARK_OF_CAIN.get()));
            handler.onPickup(new ItemEntityPickupEvent.Pre(player, item));
            h.assertTrue(!item.getItem().isEmpty(), "A marked player picks it up as an ordinary item");
        }
        h.succeed();
    }
}
