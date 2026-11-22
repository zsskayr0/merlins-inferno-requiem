package dev.zsskayr.merlins_inferno.event;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import dev.zsskayr.merlins_inferno.registry.ModEnchantments;
import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * Applies the Evil / Holy levels of {@link InnateEnchantments} to a melee hit and names them in the tooltip. If the stack also carries the real
 * enchantment (an anvil book), only the difference is added, so the two never stack on top of each other.
 */
public final class InnateEnchantmentHandler {
    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity source = event.getSource().getEntity();
        if (!(source instanceof LivingEntity attacker)) {
            return;
        }
        ItemStack weapon = CombatWeapon.of(event.getSource());
        if (weapon.isEmpty()) {
            return;
        }
        int evil = InnateEnchantments.evilLevel(weapon);
        int holy = InnateEnchantments.holyLevel(weapon);
        if (evil == 0 && holy == 0) {
            return;
        }
        LivingEntity victim = event.getEntity();
        // Bonuses are fractions of the blow. The real enchantment (an anvil book) is already in the amount as the same kind of factor, so only
        // the ratio between the innate bonus and it is applied.
        float factor = 1.0F;
        if (evil > 0) {
            int real = ModEnchantments.getEvilLevel(attacker.level(), weapon);
            float innate = weapon.is(ModTags.Items.EBONY_TOOLS) ? InnateEnchantments.ebonyBonus(victim) : InnateEnchantments.evilBonus(victim, evil);
            float present = InnateEnchantments.evilBonus(victim, real);
            if (innate > present) {
                factor *= (1.0F + innate) / (1.0F + present);
            }
        }
        if (holy > 0) {
            int real = ModEnchantments.getHolyLevel(attacker.level(), weapon);
            if (holy > real) {
                factor *= (1.0F + InnateEnchantments.holyBonus(victim, holy)) / (1.0F + InnateEnchantments.holyBonus(victim, real));
            }
        }
        if (factor != 1.0F) {
            event.setAmount(event.getAmount() * factor);
        }
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        int evil = InnateEnchantments.evilLevel(stack);
        if (evil > 0) {
            event.getToolTip().add(Component.translatable("tooltip.merlins_inferno.innate_evil", Component.translatable("enchantment.level." + evil))
                    .withStyle(net.minecraft.ChatFormatting.DARK_RED));
        }
        int holy = InnateEnchantments.holyLevel(stack);
        if (holy > 0) {
            event.getToolTip().add(Component.translatable("tooltip.merlins_inferno.innate_holy", Component.translatable("enchantment.level." + holy))
                    .withStyle(net.minecraft.ChatFormatting.YELLOW));
        }
    }
}
