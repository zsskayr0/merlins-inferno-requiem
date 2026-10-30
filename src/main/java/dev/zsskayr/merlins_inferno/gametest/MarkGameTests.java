package dev.zsskayr.merlins_inferno.gametest;

import dev.zsskayr.merlins_inferno.event.MarkOfCainHandler;
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
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
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

    @GameTest(template = "bench_empty")
    public static void penaltyTiersStartAtTheDeadlineAndRiseEveryFiveMinutes(GameTestHelper h) {
        h.assertTrue(MarkOfCainHandler.penaltyTier(99, 100) == 0, "On time, no penalty");
        h.assertTrue(MarkOfCainHandler.penaltyTier(100, 100) == 1, "First tier at the deadline");
        h.assertTrue(MarkOfCainHandler.penaltyTier(100 + 5999, 100) == 1, "Still the first tier just short of 5 minutes");
        h.assertTrue(MarkOfCainHandler.penaltyTier(100 + 6000, 100) == 2, "Second tier after 5 minutes");
        h.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void anOverdueMarkTakesHealthAndHungerUntilTheNextKill(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var handler = new MarkOfCainHandler();
        MarkHelper.equip(player, ModItems.MARK_OF_CAIN.get());
        handler.tickSecond(player);
        h.assertTrue(player.getData(ModAttachments.CAIN_KILL_DEADLINE) > h.getLevel().getGameTime(), "The first tick starts the 20 minutes");
        h.assertTrue(player.getMaxHealth() == 20.0F, "No penalty while on time");

        player.setData(ModAttachments.CAIN_KILL_DEADLINE, h.getLevel().getGameTime() - 1);
        handler.tickSecond(player);
        h.assertTrue(player.getMaxHealth() == 16.0F, "Late: 2 hearts of maximum health are gone");
        h.assertTrue(player.getFoodData().getFoodLevel() == 0, "and the hunger bar is empty");

        var cow = h.spawn(net.minecraft.world.entity.EntityType.COW, 1, 2, 1);
        handler.onDeath(new LivingDeathEvent(cow, h.getLevel().damageSources().playerAttack(player)));
        h.assertTrue(player.getMaxHealth() == 20.0F, "A passive kill pays the debt");
        h.assertTrue(player.getData(ModAttachments.CAIN_KILL_DEADLINE) > h.getLevel().getGameTime(), "and restarts the timer");
        h.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void murderStrengthWeaknessAndImmunities(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var handler = new MarkOfCainHandler();
        var zombie = h.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, 1, 2, 1);
        var hit = incoming(player, h.getLevel().damageSources().mobAttack(zombie), 10.0F);
        handler.onIncomingDamage(hit);
        h.assertTrue(hit.getAmount() == 10.0F, "Unmarked, a blow is just a blow");

        MarkHelper.equip(player, ModItems.MARK_OF_CAIN.get());
        hit = incoming(player, h.getLevel().damageSources().mobAttack(zombie), 10.0F);
        handler.onIncomingDamage(hit);
        h.assertTrue(Math.abs(hit.getAmount() - 11.5F) < 0.01F, "The bearer takes 15% more");
        var dealt = incoming(zombie, h.getLevel().damageSources().playerAttack(player), 10.0F);
        handler.onIncomingDamage(dealt);
        h.assertTrue(Math.abs(dealt.getAmount() - 13.0F) < 0.01F, "and deals 30% more");

        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 200));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 200));
        handler.tickSecond(player);
        h.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.WITHER), "The Wither is left alone: the Dark Coven lives on it");
        h.assertTrue(!player.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "but Poison does not stay on the bearer");
        var wither = incoming(player, h.getLevel().damageSources().wither(), 1.0F);
        handler.onIncomingDamage(wither);
        h.assertTrue(wither.isCanceled(), "and the Wither does no harm");
        h.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void markOfTheMurderReturnsTheBlowSevenfoldOnce(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var handler = new MarkOfCainHandler();
        MarkHelper.equip(player, ModItems.MARK_OF_CAIN.get());
        player.setData(ModAttachments.CAIN_MURDER_ARMED, true);
        var zombie = h.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, 1, 2, 1);
        float before = zombie.getHealth();
        handler.onIncomingDamage(incoming(player, h.getLevel().damageSources().mobAttack(zombie), 1.0F));
        h.assertTrue(!player.getData(ModAttachments.CAIN_MURDER_ARMED), "The Murder is spent");
        h.assertTrue(before - zombie.getHealth() > 7.0F, "The attacker took 7x the blow taken (lost " + (before - zombie.getHealth()) + ")");
        float after = zombie.getHealth();
        handler.onIncomingDamage(incoming(player, h.getLevel().damageSources().mobAttack(zombie), 1.0F));
        h.assertTrue(zombie.getHealth() == after, "Not returned again while it is on cooldown");
        h.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void theScrollOfSalvationFreesAMarkedPlayerOnly(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack scroll = new ItemStack(ModItems.SCROLL_OF_SALVATION.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, scroll);
        ModItems.SCROLL_OF_SALVATION.get().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(!scroll.isEmpty(), "Without a Mark the scroll is not wasted");
        MarkHelper.equip(player, ModItems.MARK_OF_CAIN.get());
        ModItems.SCROLL_OF_SALVATION.get().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        h.assertTrue(!MarkHelper.hasMark(player), "Reading it frees the bearer");
        h.assertTrue(scroll.isEmpty(), "and it is used up");
        h.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void withCuriosTheMarkSitsInItsSlotAndLeavesWithTheCure(GameTestHelper h) {
        if (!net.neoforged.fml.ModList.get().isLoaded("curios")) {
            h.succeed();
            return;
        }
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        h.assertTrue(MarkHelper.bind(player, ModItems.MARK_OF_CAIN.get()), "A free player takes the Mark in");
        h.assertTrue(MarkHelper.hasMark(player, ModItems.MARK_OF_CAIN.get()), "and bears it");
        h.assertTrue(dev.zsskayr.merlins_inferno.compat.OptionalCurios.equipped(player).stream().anyMatch(stack -> stack.getItem() == ModItems.MARK_OF_CAIN.get()),
                "The Mark is visible in the Curios mark slot");
        MarkHelper.clear(player);
        h.assertTrue(dev.zsskayr.merlins_inferno.compat.OptionalCurios.equipped(player).stream().noneMatch(stack -> stack.getItem() instanceof dev.zsskayr.merlins_inferno.item.MarkItem),
                "The cure empties the slot");
        h.assertTrue(!MarkHelper.hasMark(player), "and frees the player");
        h.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void foodFillsHalfAndKillsFeed(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var handler = new MarkOfCainHandler();
        MarkHelper.equip(player, ModItems.MARK_OF_CAIN.get());
        player.getFoodData().setFoodLevel(8);
        handler.tickSecond(player);
        player.getFoodData().setFoodLevel(14);
        handler.tickSecond(player);
        h.assertTrue(player.getFoodData().getFoodLevel() == 11, "Food that fills 6 shanks fills 3 (got " + player.getFoodData().getFoodLevel() + ")");
        var chicken = h.spawn(net.minecraft.world.entity.EntityType.CHICKEN, 1, 2, 1);
        handler.onDeath(new LivingDeathEvent(chicken, h.getLevel().damageSources().playerAttack(player)));
        h.assertTrue(player.getFoodData().getFoodLevel() == 13, "A chicken is a sweet berry's worth: 2 (got " + player.getFoodData().getFoodLevel() + ")");
        handler.tickSecond(player);
        h.assertTrue(player.getFoodData().getFoodLevel() == 13, "and what a kill gave is not halved");
        var zombie = h.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, 1, 2, 1);
        handler.onDeath(new LivingDeathEvent(zombie, h.getLevel().damageSources().playerAttack(player)));
        h.assertTrue(player.getFoodData().getFoodLevel() == 14, "A hostile kill gives half of that: 1 (got " + player.getFoodData().getFoodLevel() + ")");
        h.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void theDarkCovenStaysInvokedUnderTheMark(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var handler = new MarkOfCainHandler();
        MarkHelper.equip(player, ModItems.MARK_OF_CAIN.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DARK_COVEN_I.get()));
        ModItems.DARK_COVEN_I.get().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
        handler.tickSecond(player);
        player.getMainHandItem().inventoryTick(h.getLevel(), player, 0, true);
        h.assertTrue(dev.zsskayr.merlins_inferno.item.DarkCovenItem.isInvoked(player.getMainHandItem()), "The coven is not dismissed: the Wither it lives on is still there");
        h.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST), "and its boons hold");
        h.succeed();
    }

    private static LivingIncomingDamageEvent incoming(net.minecraft.world.entity.LivingEntity victim, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return new LivingIncomingDamageEvent(victim, new net.neoforged.neoforge.common.damagesource.DamageContainer(source, amount));
    }
}
