package dev.zsskayr.merlins_inferno.event;

import dev.zsskayr.merlins_inferno.block.RedLyriumRadiance;
import dev.zsskayr.merlins_inferno.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Mirror Sanctified's melee contagion, with the damage bonus aimed at humans. */
public final class DemonifiedCombatHandler {
    @SubscribeEvent public void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity source = event.getSource().getEntity();
        if (!(source instanceof Player player)) return;
        MobEffectInstance effect = player.getEffect(ModEffects.DEMONIFIED);
        if (effect == null) return;
        if (RedLyriumRadiance.isHuman(event.getEntity())) {
            event.setAmount(event.getAmount() + 2.5F * (effect.getAmplifier() + 1));
        }
        if (event.getSource().getDirectEntity() == source) {
            event.getEntity().addEffect(new MobEffectInstance(effect));
        }
    }
}
