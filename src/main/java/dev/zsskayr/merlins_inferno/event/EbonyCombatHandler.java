package dev.zsskayr.merlins_inferno.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * Ebony's corruption, on both ends of a melee hit:
 * <ul>
 *   <li><b>The weapons</b> ({@link ModTags.Items#EBONY_TOOLS}) wither what they cut and give back a sliver of the blow as health
 *       ({@link #LIFESTEAL} of the damage, at most {@link #LIFESTEAL_CAP} of the attacker's maximum health per blow).</li>
 *   <li><b>The full set</b> withers whoever strikes its wearer in melee (arrows and spells pass).</li>
 * </ul>
 * Mobs immune to Wither shrug the effect off by themselves.
 */
public final class EbonyCombatHandler {
    private static final int WITHER_TICKS = 60;
    private static final float LIFESTEAL = 0.05F;
    /** The most one blow can give back, as a fraction of the attacker's maximum health (10%: one heart at 20 HP, scaling with modded health). */
    private static final float LIFESTEAL_CAP = 0.10F;

    @SubscribeEvent
    public void onHit(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        boolean melee = source.getDirectEntity() == attacker;
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        if (CombatWeapon.of(source).is(ModTags.Items.EBONY_TOOLS)) {
            victim.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_TICKS, 0), attacker);
            attacker.heal(Math.min(event.getNewDamage() * LIFESTEAL, attacker.getMaxHealth() * LIFESTEAL_CAP));
        }
        if (melee && EbonyArmorHandler.wearsFullSet(victim)) {
            attacker.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_TICKS, 0), victim);
        }
    }
}
