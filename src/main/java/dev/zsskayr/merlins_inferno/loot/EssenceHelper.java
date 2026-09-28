package dev.zsskayr.merlins_inferno.loot;

import net.minecraft.world.item.ItemStack;

import dev.zsskayr.merlins_inferno.registry.ModItems;

public final class EssenceHelper {
    private EssenceHelper() {
    }

    public static boolean isEssence(ItemStack stack) {
        return stack.is(ModItems.MUNDANE_ESSENCE.get()) || stack.is(ModItems.FAE_ESSENCE.get())
                || stack.is(ModItems.INFERNAL_ESSENCE.get()) || stack.is(ModItems.OTHERWORLD_ESSENCE.get())
                || stack.is(ModItems.CELESTIAL_ESSENCE.get());
    }
}
