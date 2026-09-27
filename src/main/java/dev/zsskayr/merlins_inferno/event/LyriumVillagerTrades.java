package dev.zsskayr.merlins_inferno.event;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import dev.zsskayr.merlins_inferno.registry.ModEnchantments;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * Adds the Refined Lyrium trade to the vanilla Cleric profession, at Master level (5) - the only
 * villager-trade infrastructure in the mod so far. Two alternate offers, per design: 9x Impure
 * Lyrium OR 27x Diamond, either one for 1x Refined Lyrium.
 * <p>
 * Also the Cleric's one enchanted-book offer: an Apprentice-level (2) trade of Refined Lyrium for a
 * Holy book (see {@link ModEnchantments#HOLY} for why - trading with a Cleric or a Sacred Church's
 * chest loot are Holy's only two sources; it never appears at the enchanting table).
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

        ItemStack holyBook = createHolyBook(event.getRegistryAccess(), 1);
        if (!holyBook.isEmpty()) {
            event.getTrades().get(2).add(new BasicItemListing(
                    new ItemStack(ModItems.LYRIUM_REFINED.get(), 3),
                    holyBook,
                    2, 20, 0.05F));
        }
    }

    private static ItemStack createHolyBook(net.minecraft.core.RegistryAccess registryAccess, int level) {
        HolderLookup.RegistryLookup<Enchantment> enchantments = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> holy = enchantments.get(ModEnchantments.HOLY).orElse(null);
        return holy == null ? ItemStack.EMPTY : EnchantedBookItem.createForEnchantment(new EnchantmentInstance(holy, level));
    }
}
