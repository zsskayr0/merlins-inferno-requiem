package dev.zsskayr.merlins_inferno.gametest;

import dev.zsskayr.merlins_inferno.item.WingsItem;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("merlins_inferno")
@PrefixGameTestTemplate(false)
public final class WingsGameTests {
    private static Player wearing(GameTestHelper h, ItemStack wings) {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.CHEST, wings);
        return player;
    }

    @GameTest(template = "bench_empty")
    public static void boostPushesGlidingPlayerOnceThenCoolsDown(GameTestHelper h) {
        Player player = wearing(h, new ItemStack(ModItems.SERAPH_WINGS.get()));
        player.setXRot(0);
        player.setYRot(0);
        player.startFallFlying();
        WingsItem.tryBoost(player);
        h.assertTrue(player.getDeltaMovement().z > 0.5, "The boost pushes the player along their look direction at once");
        for (int i = 0; i < 40; i++) WingsItem.tickBoost(player);
        h.assertTrue(player.getDeltaMovement().z > 1.6, "...and keeps pulling like a firework, ending above its 1.5 speed");
        h.assertTrue(player.getData(dev.zsskayr.merlins_inferno.registry.ModAttachments.WINGS_BOOST_TICKS) == 0, "...then stops");
        h.assertTrue(player.getCooldowns().isOnCooldown(ModItems.SERAPH_WINGS.get()), "The boost starts the wings' cooldown");
        player.setDeltaMovement(Vec3.ZERO);
        WingsItem.tryBoost(player);
        h.assertTrue(player.getDeltaMovement().equals(Vec3.ZERO), "No second boost while on cooldown");
        h.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void boostNeedsGlidingAndWings(GameTestHelper h) {
        Player wearer = wearing(h, new ItemStack(ModItems.FALLEN_WINGS.get()));
        WingsItem.tryBoost(wearer);
        h.assertTrue(wearer.getDeltaMovement().equals(Vec3.ZERO), "Wings do not boost a player who is not gliding");
        Player bare = h.makeMockPlayer(GameType.SURVIVAL);
        bare.startFallFlying();
        WingsItem.tryBoost(bare);
        h.assertTrue(bare.getDeltaMovement().equals(Vec3.ZERO), "A gliding player without wings is not boosted");
        h.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void onlyFallenWingsGiveArmor(GameTestHelper h) {
        Player seraph = wearing(h, new ItemStack(ModItems.SERAPH_WINGS.get()));
        Player fallen = wearing(h, new ItemStack(ModItems.FALLEN_WINGS.get()));
        seraph.tick();
        fallen.tick();
        h.assertTrue(seraph.getArmorValue() == 0, "Seraph Wings give no armour");
        h.assertTrue(fallen.getArmorValue() == 6, "Fallen Wings give an iron chestplate's armour");
        h.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void curiosBackWingsGlideAndBoost(GameTestHelper h) throws ReflectiveOperationException {
        if (!net.neoforged.fml.ModList.get().isLoaded("curios")) { h.succeed(); return; }
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        var handler = Class.forName("top.theillusivec4.curios.api.CuriosApi")
                .getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class).invoke(null, player);
        ItemStack wings = new ItemStack(ModItems.SERAPH_WINGS.get());
        Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler")
                .getMethod("setEquippedCurio", String.class, int.class, ItemStack.class)
                .invoke(((java.util.Optional<?>) handler).orElseThrow(), "back", 0, wings);
        h.assertTrue(WingsItem.find(player) == wings, "Wings in the Curios back slot are found");
        player.setOnGround(false);
        h.assertTrue(player.tryToStartFallFlying(), "Wings in the Curios back slot start a glide");
        player.setXRot(0);
        player.setYRot(0);
        WingsItem.tryBoost(player);
        h.assertTrue(player.getDeltaMovement().z > 0.5, "...and boost it");
        h.succeed();
    }

    private static int wearAfterSeconds(GameTestHelper h, ItemStack stack, int seconds) {
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        WingsItem wings = (WingsItem) stack.getItem();
        for (int tick = 0; tick < seconds * 20; tick++) wings.elytraFlightTick(stack, player, tick);
        return stack.getDamageValue();
    }

    @GameTest(template = "bench_empty")
    public static void durabilityIsMultiplesOfTheElytraAndWearsPerWorld(GameTestHelper h) {
        h.assertTrue(new ItemStack(ModItems.TATTERED_WINGS.get()).getMaxDamage() == 432, "Tattered Wings last as long as an elytra");
        h.assertTrue(new ItemStack(ModItems.SERAPH_WINGS.get()).getMaxDamage() == 864, "Seraph Wings last twice as long");
        h.assertTrue(new ItemStack(ModItems.FALLEN_WINGS.get()).getMaxDamage() == 1296, "Fallen Wings last three times as long");
        h.assertTrue(wearAfterSeconds(h, new ItemStack(ModItems.TATTERED_WINGS.get()), 4) == 4, "Tattered Wings lose one a second at home");
        h.assertTrue(wearAfterSeconds(h, new ItemStack(ModItems.SERAPH_WINGS.get()), 4) == 4, "Seraph Wings lose one a second at home");
        h.assertTrue(wearAfterSeconds(h, new ItemStack(ModItems.FALLEN_WINGS.get()), 4) == 6, "Fallen Wings lose one and a half a second at home");
        h.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void onlyTheRouteWingsCanBeMended(GameTestHelper h) {
        ItemStack tattered = new ItemStack(ModItems.TATTERED_WINGS.get());
        ItemStack seraph = new ItemStack(ModItems.SERAPH_WINGS.get());
        ItemStack fallen = new ItemStack(ModItems.FALLEN_WINGS.get());
        h.assertTrue(!tattered.isRepairable(), "Tattered Wings cannot be mended");
        h.assertTrue(seraph.isRepairable() && seraph.getItem().isValidRepairItem(seraph, new ItemStack(ModItems.SERAPHIUM_BAR.get())), "Seraph Wings are mended with a Seraphium Bar");
        h.assertTrue(fallen.isRepairable() && fallen.getItem().isValidRepairItem(fallen, new ItemStack(ModItems.EBONY_BAR.get())), "Fallen Wings are mended with an Ebony Bar");
        h.assertTrue(!seraph.getItem().isValidRepairItem(seraph, new ItemStack(ModItems.EBONY_BAR.get())), "...and not with another route's bar");
        h.succeed();
    }

    @GameTest(template = "bench_empty")
    public static void foreignHeavyArmourSlowsTheBoost(GameTestHelper h) {
        Player player = wearing(h, new ItemStack(ModItems.SERAPH_WINGS.get()));
        WingsItem wings = (WingsItem) player.getItemBySlot(EquipmentSlot.CHEST).getItem();
        double bare = wings.boostSpeed(player);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
        h.assertTrue(wings.boostSpeed(player) == bare, "Iron is not heavy");
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.SERAPHIUM_HELMET.get()));
        h.assertTrue(wings.boostSpeed(player) == bare, "Armour of the wings' own route is never heavy");
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.DIAMOND_HELMET));
        h.assertTrue(wings.boostSpeed(player) < bare, "Armour of another route stronger than iron slows the boost");
        h.succeed();
    }
}
