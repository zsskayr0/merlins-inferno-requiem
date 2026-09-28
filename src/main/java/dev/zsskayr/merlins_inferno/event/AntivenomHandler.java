package dev.zsskayr.merlins_inferno.event;

import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import dev.zsskayr.merlins_inferno.registry.ModEffects;

/** While Antivenom is active the bearer cannot be Poisoned - the counter to Ostara's petals, like a blindfold against a gorgon. */
public final class AntivenomHandler {
    @SubscribeEvent
    public void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().is(MobEffects.POISON) && event.getEntity().hasEffect(ModEffects.ANTIVENOM)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }
}
