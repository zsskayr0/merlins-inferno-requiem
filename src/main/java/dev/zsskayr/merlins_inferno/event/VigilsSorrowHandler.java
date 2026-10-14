package dev.zsskayr.merlins_inferno.event;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import dev.zsskayr.merlins_inferno.registry.ModEffects;
import dev.zsskayr.merlins_inferno.registry.ModItems;
import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * Vigil's Sorrow's traits (Angelical legendary sword). Every hit, player wielders only (see
 * {@link SanctifiedCombatHandler#canUseSanctified}):
 * <ul>
 *   <li><b>Sanctified II:</b> inflicted on the victim outright, whatever it is.</li>
 *   <li><b>Holy I, built in:</b> the effect of the Holy enchantment at level 1 without the enchantment - +3 vs
 *       {@link ModTags.EntityTypes#DEMON}, +2 vs the Undead, -1 vs {@link ModTags.EntityTypes#NEUTRAL_MOBS}, -2 vs
 *       {@link ModTags.EntityTypes#PASSIVE_MOBS}, +1 vs any other {@link Monster} (the numbers of
 *       {@code data/merlins_inferno/enchantment/holy.json} and {@link HolyCombatHandler}).</li>
 *   <li><b>Giant-slayer:</b> {@link #STRONGER_MULTIPLIER}x damage to anything with more maximum health than the wielder.</li>
 * </ul>
 */
public final class VigilsSorrowHandler {
    private static final int SANCTIFIED_DURATION = 200; // 10s, like Seraphium's on-hit application
    private static final int SANCTIFIED_AMPLIFIER = 1;  // level II
    private static final float STRONGER_MULTIPLIER = 1.5F;

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity source = event.getSource().getEntity();
        if (!(source instanceof LivingEntity attacker) || !attacker.getMainHandItem().is(ModItems.VIGILS_SORROW.get())
                || !SanctifiedCombatHandler.canUseSanctified(attacker)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        victim.addEffect(new MobEffectInstance(ModEffects.SANCTIFIED, SANCTIFIED_DURATION, SANCTIFIED_AMPLIFIER));

        float amount = event.getAmount();
        if (victim.getType().is(ModTags.EntityTypes.DEMON)) {
            amount += 3.0F;
        } else if (victim.getType().is(EntityTypeTags.UNDEAD)) {
            amount += 2.0F;
        } else if (victim.getType().is(ModTags.EntityTypes.NEUTRAL_MOBS)) {
            amount = Math.max(0.0F, amount - 1.0F);
        } else if (victim.getType().is(ModTags.EntityTypes.PASSIVE_MOBS)) {
            amount = Math.max(0.0F, amount - 2.0F);
        } else if (victim instanceof Monster) {
            amount += 1.0F;
        }
        if (victim.getMaxHealth() > attacker.getMaxHealth()) {
            amount *= STRONGER_MULTIPLIER;
        }
        event.setAmount(amount);
    }
}
