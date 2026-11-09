package dev.zsskayr.merlins_inferno.mark;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.compat.OptionalCurios;
import dev.zsskayr.merlins_inferno.registry.ModEffects;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Everything that keeps Red Lyrium's corruption off a body that would otherwise take it: the Warded effect (Elixir of
 * Warding), a Warding Amulet carried anywhere in the inventory or worn in a Curios slot, or any armour piece with the
 * Warding enchantment.
 */
public final class DemonifiedWard {
    public static final ResourceKey<Enchantment> WARDING = ResourceKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "warding"));

    private DemonifiedWard() {}

    public static boolean isWarded(LivingEntity entity) {
        if (entity.hasEffect(ModEffects.WARDED)) return true;
        Holder<Enchantment> warding = entity.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(WARDING).orElse(null);
        if (warding != null) {
            for (ItemStack armour : entity.getArmorSlots()) {
                if (EnchantmentHelper.getItemEnchantmentLevel(warding, armour) > 0) return true;
            }
        }
        if (entity instanceof Player player && player.getInventory().contains(ModItems.WARDING_AMULET.get().getDefaultInstance())) {
            return true;
        }
        for (ItemStack stack : OptionalCurios.equipped(entity)) {
            if (stack.is(ModItems.WARDING_AMULET.get())) return true;
        }
        return false;
    }
}
