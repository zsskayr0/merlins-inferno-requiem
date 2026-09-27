package dev.zsskayr.merlins_inferno.event;

import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import dev.zsskayr.merlins_inferno.registry.ModEffects;

/** Whoever bears Andras' Mark of Debt takes 25% more damage per level (I = +25%, II = +50%...). */
public class MarkOfDebtHandler {
    private static final float EXTRA_PER_LEVEL = 0.25F;

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        MobEffectInstance mark = event.getEntity().getEffect(ModEffects.MARK_OF_DEBT);
        if (mark != null) {
            event.setAmount(event.getAmount() * (1.0F + EXTRA_PER_LEVEL * (mark.getAmplifier() + 1)));
        }
    }
}
