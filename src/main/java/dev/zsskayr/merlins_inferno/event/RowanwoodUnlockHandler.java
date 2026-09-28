package dev.zsskayr.merlins_inferno.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import dev.zsskayr.merlins_inferno.registry.ModAttachments;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * Remembers that a player has obtained their first Rowanwood Scrap (picked up or crafted), which opens the
 * Druid's tool and enchantment trades. Scrap that arrives some other way (a chest, a trade) is caught when the
 * player opens a Druid's trades - see {@code DruidEntity#hasRowanwoodUnlocked}.
 */
public final class RowanwoodUnlockHandler {
    @SubscribeEvent
    public void onPickup(ItemEntityPickupEvent.Post event) {
        unlockIfScrap(event.getPlayer(), event.getOriginalStack());
    }

    @SubscribeEvent
    public void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        unlockIfScrap(event.getEntity(), event.getCrafting());
    }

    private static void unlockIfScrap(Player player, ItemStack stack) {
        if (stack.is(ModItems.ROWANWOOD_SCRAP.get())) {
            player.setData(ModAttachments.ROWANWOOD_UNLOCKED, true);
        }
    }
}
