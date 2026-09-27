package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * {@link ResourceKey} constants for this mod's data-driven enchantments (definitions live under
 * {@code data/merlins_inferno/enchantment/}) - enchantments are a dynamic/datapack registry, not
 * one a mod registers into directly the way items/blocks are, so (like {@code bane_of_humanity}
 * and {@code evil} before it) there's no {@code DeferredRegister} here, just keys to look the
 * resolved {@link Holder} up by wherever Java code needs to check for one.
 */
public final class ModEnchantments {
    public static final ResourceKey<Enchantment> BANE_OF_HUMANITY = ResourceKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "bane_of_humanity"));
    public static final ResourceKey<Enchantment> EVIL = ResourceKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "evil"));
    /** Evil's Angelical mirror - see {@code event.HolyCombatHandler} for the actual percentage effects (this
     * enchantment's own JSON, like {@code druids_touch}, carries no vanilla effect - the bonus is per-target-type
     * percentage, which vanilla's additive enchantment effect system can't express). */
    public static final ResourceKey<Enchantment> HOLY = ResourceKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "holy"));
    /** See {@code data/merlins_inferno/enchantment/druids_touch.json} for the anvil-cost/loot-source design; the actual per-tool effects are split across block/loot data (see the Rowanwood log loot table) and {@code event.DruidsTouchHandler}. */
    public static final ResourceKey<Enchantment> DRUIDS_TOUCH = ResourceKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "druids_touch"));

    private ModEnchantments() {
    }

    /** Resolves and checks {@link #DRUIDS_TOUCH}'s level on a stack in one call - 0 if absent. */
    public static int getDruidsTouchLevel(Level level, ItemStack stack) {
        return getLevel(level, stack, DRUIDS_TOUCH);
    }

    /** Resolves and checks {@link #HOLY}'s level on a stack in one call - 0 if absent. */
    public static int getHolyLevel(Level level, ItemStack stack) {
        return getLevel(level, stack, HOLY);
    }

    private static int getLevel(Level level, ItemStack stack, ResourceKey<Enchantment> key) {
        HolderLookup.RegistryLookup<Enchantment> registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> holder = registry.get(key).orElse(null);
        return holder == null ? 0 : stack.getEnchantmentLevel(holder);
    }
}
