package dev.zsskayr.merlins_inferno.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * Demonite's hatred of "humankind": any tool/weapon in {@link ModTags.Items#DEMONITE_TOOLS} deals
 * 10% extra damage to mobs in {@link ModTags.EntityTypes#HUMANS} (villagers and their variants,
 * pillagers, witches, and the rest of the raid cast).
 * <p>
 * This is baked into the material itself - independent of, and stacks with, the "Bane of
 * Humanity" enchantment ({@code data/merlins_inferno/enchantment/bane_of_humanity.json}), which
 * every Demonite tool is crafted with at level 1 by default (see the {@code result.components} block in
 * {@code data/merlins_inferno/recipe/demonite_*.json}) and which grants a similar bonus to any
 * weapon a player chooses to enchant with it further.
 * <p>
 * Implemented as an event handler rather than as a data-driven enchantment effect because the
 * bonus needs to apply unconditionally to the material, not to an optional enchantment level.
 */
public final class DemoniteCombatHandler {

    private static final float HUMAN_DAMAGE_MULTIPLIER = 1.1F;

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!event.getEntity().getType().is(ModTags.EntityTypes.HUMANS)) {
            return;
        }

        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) {
            return;
        }

        ItemStack weapon = livingAttacker.getMainHandItem();
        if (weapon.is(ModTags.Items.DEMONITE_TOOLS)) {
            event.setAmount(event.getAmount() * HUMAN_DAMAGE_MULTIPLIER);
        }
    }
}
