package dev.zsskayr.merlins_inferno.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import dev.zsskayr.merlins_inferno.registry.ModItems;
import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * The Debt Collector's damage traits (the fire it sets is in the item itself):
 * <ul>
 *   <li><b>Evil I, built in:</b> the effect of the Evil enchantment at level 1 without the enchantment - +3 damage to
 *       {@link ModTags.EntityTypes#PASSIVE_MOBS}, +1.5 to {@link ModTags.EntityTypes#NEUTRAL_MOBS} (the numbers of
 *       {@code data/merlins_inferno/enchantment/evil.json}).</li>
 *   <li><b>Bully:</b> {@link #WEAKER_MULTIPLIER}x damage to anything with less maximum health than the wielder.</li>
 * </ul>
 */
public final class DebtCollectorHandler {
    private static final float EVIL_PASSIVE_BONUS = 3.0F;
    private static final float EVIL_NEUTRAL_BONUS = 1.5F;
    private static final float WEAKER_MULTIPLIER = 1.5F;

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity source = event.getSource().getEntity();
        if (!(source instanceof LivingEntity attacker) || !attacker.getMainHandItem().is(ModItems.DEBT_COLLECTOR.get())) {
            return;
        }
        LivingEntity target = event.getEntity();
        float amount = event.getAmount();
        if (target.getType().is(ModTags.EntityTypes.PASSIVE_MOBS)) {
            amount += EVIL_PASSIVE_BONUS;
        } else if (target.getType().is(ModTags.EntityTypes.NEUTRAL_MOBS)) {
            amount += EVIL_NEUTRAL_BONUS;
        }
        if (target.getMaxHealth() < attacker.getMaxHealth()) {
            amount *= WEAKER_MULTIPLIER;
        }
        event.setAmount(amount);
    }
}
