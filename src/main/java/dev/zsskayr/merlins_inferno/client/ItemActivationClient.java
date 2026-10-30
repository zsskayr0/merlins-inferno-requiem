package dev.zsskayr.merlins_inferno.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/** Client end of {@code ItemActivationPayload}: the Totem of Undying's pop-up, with another item in it. */
public final class ItemActivationClient {
    private ItemActivationClient() {
    }

    public static void show(ItemStack stack) {
        Minecraft.getInstance().gameRenderer.displayItemActivation(stack);
    }
}
