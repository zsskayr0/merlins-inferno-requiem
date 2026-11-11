package dev.zsskayr.merlins_inferno.event;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.BasicItemListing;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import dev.zsskayr.merlins_inferno.registry.ModEnchantments;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * Adds the Refined Lyrium trade to the vanilla Cleric profession, at Master level (5) - the only
 * villager-trade infrastructure in the mod so far. Two alternate offers, per design: 6x Impure
 * Lyrium OR 10x Diamond, either one for 1x Refined Lyrium.
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
                new ItemStack(ModItems.LYRIUM_IMPURE.get(), 6),
                new ItemStack(ModItems.LYRIUM_REFINED.get()),
                4, 30, 0.05F));
        event.getTrades().get(5).add(new BasicItemListing(
                new ItemStack(Items.DIAMOND, 10),
                new ItemStack(ModItems.LYRIUM_REFINED.get()),
                4, 30, 0.05F));

        // The cure for a Mark: dear, and a single use (see item.ScrollOfSplitterItem).
        event.getTrades().get(5).add(new BasicItemListing(
                new ItemStack(Items.EMERALD, 64),
                new ItemStack(ModItems.BOOK_OF_CONTRACTS.get()),
                new ItemStack(ModItems.SCROLL_OF_SPLITTER.get()),
                1, 30, 0.05F));

        ItemStack holyBook = createHolyBook(event.getRegistryAccess(), 1);
        if (!holyBook.isEmpty()) {
            event.getTrades().get(2).add(new BasicItemListing(
                    new ItemStack(ModItems.LYRIUM_REFINED.get(), 3),
                    holyBook,
                    2, 20, 0.05F));
        }
    }

    /** The Sacred Priest's top level. */
    public static final int PRIEST_MAX_LEVEL = 5;
    /** The dearest the Priest ever asks in diamonds - 14 is 98 Gold Coins, one stack of 99 (his top wares cost Refined Lyrium instead). */
    public static final int MAX_PRICE = 14;
    /** Numismatic Overhaul Gold Coins per diamond. */
    public static final int COINS_PER_DIAMOND = 7;

    /**
     * One level of the Sacred Priest's stock. He levels like a villager: every trade earns experience, and each level
     * opens a new tier. Everything is priced in diamonds - or Gold Coins at {@link #COINS_PER_DIAMOND} per diamond
     * when Numismatic Overhaul is installed - except the top tier, which is priced in Refined Lyrium, and the Scroll
     * of Splitter, which always costs 20 Refined Lyrium. Holy climbs I to III over the levels. Better than the
     * Cleric's wares, and dearer.
     */
    public static java.util.List<MerchantOffer> priestTier(net.minecraft.core.RegistryAccess registryAccess, int tier) {
        java.util.List<MerchantOffer> list = new java.util.ArrayList<>();
        int xp = tier * 5;
        switch (tier) {
            case 1 -> {
                list.add(offer(3, new ItemStack(Items.EXPERIENCE_BOTTLE, 16), 2, xp));
                addIfPresent(list, offer(4, createHolyBook(registryAccess, 1), 2, xp));
                addIfPresent(list, offer(6, holySword(registryAccess, 1, Items.DIAMOND_SWORD), 1, xp));
            }
            case 2 -> {
                addIfPresent(list, offer(7, createHolyBook(registryAccess, 2), 2, xp));
                addIfPresent(list, offer(9, holySword(registryAccess, 2, Items.DIAMOND_SWORD), 1, xp));
                // Cheaper than buying the Refined Lyrium outright: bring Raw Lyrium and a little money.
                list.add(new MerchantOffer(cost(4), java.util.Optional.of(new ItemCost(ModItems.LYRIUM_RAW.get(), 3)),
                        new ItemStack(ModItems.LYRIUM_REFINED.get()), 4, xp, 0.05F));
            }
            case 3 -> {
                addIfPresent(list, offer(11, createHolyBook(registryAccess, 3), 1, xp));
                addIfPresent(list, offer(MAX_PRICE, holySword(registryAccess, 3, Items.DIAMOND_SWORD), 1, xp));
                list.add(offer(8, new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), 1, xp));
                list.add(offer(MAX_PRICE, new ItemStack(ModItems.LYRIUM_REFINED.get()), 4, xp));
            }
            case 4 -> {
                list.add(offer(11, new ItemStack(Items.TOTEM_OF_UNDYING), 1, xp));
                list.add(offer(MAX_PRICE, new ItemStack(Items.BEACON), 1, xp));
            }
            case 5 -> {
                // The best of his wares, paid in Refined Lyrium alone.
                ItemStack blade = holySword(registryAccess, 3, Items.MACE);
                if (!blade.isEmpty()) {
                    list.add(new MerchantOffer(new ItemCost(ModItems.LYRIUM_REFINED.get(), 15), blade, 1, 0, 0.05F));
                }
                list.add(new MerchantOffer(new ItemCost(ModItems.LYRIUM_REFINED.get(), 12),
                        new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 4), 1, 0, 0.05F));
                list.add(new MerchantOffer(new ItemCost(ModItems.LYRIUM_REFINED.get(), 20),
                        new ItemStack(ModItems.SCROLL_OF_SPLITTER.get()), 1, 0, 0.05F));
            }
            default -> { }
        }
        return list;
    }

    private static void addIfPresent(java.util.List<MerchantOffer> list, MerchantOffer offer) {
        if (!offer.getResult().isEmpty()) {
            list.add(offer);
        }
    }

    /** {@code diamonds} diamonds, or the same worth in Numismatic Overhaul Gold Coins (one stack, up to 99). */
    private static ItemCost cost(int diamonds) {
        var coin = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("numismaticoverhaul", "gold_coin"));
        return coin.isPresent() ? new ItemCost(coin.get(), Math.min(99, diamonds * COINS_PER_DIAMOND))
                : new ItemCost(Items.DIAMOND, diamonds);
    }

    /** An offer costing {@code diamonds} diamonds - or Gold Coins when Numismatic Overhaul is present (up to two stacks). */
    private static MerchantOffer offer(int diamonds, ItemStack result, int maxUses, int xp) {
        var coin = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("numismaticoverhaul", "gold_coin"));
        if (coin.isEmpty()) {
            return new MerchantOffer(new ItemCost(Items.DIAMOND, diamonds), result, maxUses, xp, 0.05F);
        }
        int coins = diamonds * COINS_PER_DIAMOND;
        int first = Math.min(coins, 99);
        java.util.Optional<ItemCost> second = coins > first
                ? java.util.Optional.of(new ItemCost(coin.get(), coins - first)) : java.util.Optional.empty();
        return new MerchantOffer(new ItemCost(coin.get(), first), second, result, maxUses, xp, 0.05F);
    }

    private static ItemStack holySword(net.minecraft.core.RegistryAccess registryAccess, int level, net.minecraft.world.item.Item base) {
        Holder<Enchantment> holy = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).get(ModEnchantments.HOLY).orElse(null);
        if (holy == null) {
            return ItemStack.EMPTY;
        }
        ItemStack sword = new ItemStack(base);
        sword.enchant(holy, level);
        return sword;
    }

    private static ItemStack createHolyBook(net.minecraft.core.RegistryAccess registryAccess, int level) {
        HolderLookup.RegistryLookup<Enchantment> enchantments = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> holy = enchantments.get(ModEnchantments.HOLY).orElse(null);
        return holy == null ? ItemStack.EMPTY : EnchantedBookItem.createForEnchantment(new EnchantmentInstance(holy, level));
    }
}
