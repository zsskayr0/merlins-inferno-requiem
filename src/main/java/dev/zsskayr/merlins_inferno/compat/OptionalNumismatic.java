package dev.zsskayr.merlins_inferno.compat;

import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.fml.ModList;

/**
 * Optional Numismatic Overhaul bridge: with it loaded, the Druid's emerald prices are charged in its coins instead,
 * since its villagers stop dealing in emeralds. The coins are plain items, so no API is needed, only their ids.
 */
public final class OptionalNumismatic {
    private static final String MOD_ID = "numismaticoverhaul";
    /** Bronze coins per emerald. The mod itself uses 125; the Druid's early stock is cheap plants, so this is lower. */
    private static final int BRONZE_PER_EMERALD = 50;
    private static final int BRONZE_PER_SILVER = 100;

    private OptionalNumismatic() {}

    public static boolean active() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /** The offer with its emerald cost or emerald payout turned into coins; unchanged when the mod is absent. */
    public static MerchantOffer convert(ItemCost cost, ItemStack result, int maxUses, int xp, float priceMultiplier) {
        if (active()) {
            Item bronze = coin("bronze_coin");
            Item silver = coin("silver_coin");
            if (bronze != null && silver != null) {
                if (cost.itemStack().is(Items.EMERALD)) {
                    int total = cost.count() * BRONZE_PER_EMERALD;
                    int silvers = total / BRONZE_PER_SILVER;
                    int bronzes = total % BRONZE_PER_SILVER;
                    ItemCost first = silvers > 0 ? new ItemCost(silver, silvers) : new ItemCost(bronze, bronzes);
                    Optional<ItemCost> second = silvers > 0 && bronzes > 0 ? Optional.of(new ItemCost(bronze, bronzes)) : Optional.empty();
                    return new MerchantOffer(first, second, result, 0, maxUses, xp, priceMultiplier);
                }
                if (result.is(Items.EMERALD)) {
                    return new MerchantOffer(cost, new ItemStack(bronze, result.getCount() * BRONZE_PER_EMERALD), maxUses, xp, priceMultiplier);
                }
            }
        }
        return new MerchantOffer(cost, result, maxUses, xp, priceMultiplier);
    }

    private static Item coin(String path) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(MOD_ID, path)).orElse(null);
    }
}
