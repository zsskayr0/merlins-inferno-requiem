package dev.zsskayr.merlins_inferno.event;

import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * Adds the Refined Lyrium trade to the vanilla Cleric profession, at Master level (5) - the only
 * villager-trade infrastructure in the mod so far. Two alternate offers, per design: 9x Impure
 * Lyrium OR 27x Diamond, either one for 1x Refined Lyrium.
 */
public final class LyriumVillagerTrades {
    @SubscribeEvent
    public void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfession.CLERIC) {
            return;
        }

        event.getTrades().get(5).add(new BasicItemListing(
                new ItemStack(ModItems.LYRIUM_IMPURE.get(), 9),
                new ItemStack(ModItems.LYRIUM_REFINED.get()),
                4, 30, 0.05F));
        event.getTrades().get(5).add(new BasicItemListing(
                new ItemStack(Items.DIAMOND, 27),
                new ItemStack(ModItems.LYRIUM_REFINED.get()),
                4, 30, 0.05F));
    }
}
