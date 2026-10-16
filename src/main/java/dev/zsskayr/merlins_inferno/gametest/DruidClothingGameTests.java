package dev.zsskayr.merlins_inferno.gametest;

import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("merlins_inferno")
@PrefixGameTestTemplate(false)
public final class DruidClothingGameTests {
    @GameTest(template="bench_empty") public static void nativeArmorKeepsCustomClothes(GameTestHelper h) {
        var druid=ModEntityTypes.DRUID.get().create(h.getLevel());
        h.assertTrue(druid.usesNativeClothing(EquipmentSlot.CHEST),"Empty slot shows authored clothing");
        druid.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.LEATHER_CHESTPLATE));
        h.assertTrue(druid.usesNativeClothing(EquipmentSlot.CHEST),"Existing leather-equipped druids show custom clothing");
        druid.setItemSlot(EquipmentSlot.CHEST,new ItemStack(ModItems.ROWANWOOD_CHESTPLATE.get()));
        h.assertTrue(druid.usesNativeClothing(EquipmentSlot.CHEST),"Rowanwood uses authored druid armor");
        druid.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));
        h.assertTrue(!druid.usesNativeClothing(EquipmentSlot.CHEST),"Other equipped armor may still render normally");h.succeed();
    }
}
