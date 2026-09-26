package dev.zsskayr.merlins_inferno.event;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import dev.zsskayr.merlins_inferno.registry.ModEffects;
import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * Sanctified's two combat traits, both keyed off the ATTACKER currently having the effect (not off
 * a weapon/item tag, unlike {@code DemonbloodCombatHandler} - Sanctified is a status the attacker
 * carries, so anything they hit with counts, bare fists included):
 * <ul>
 *     <li>bonus damage against {@link EntityTypeTags#UNDEAD}, matching the Smite enchantment's own
 *     scaling (+2.5 per level);</li>
 *     <li>contagion - a melee hit (not a projectile/indirect source) copies the attacker's current
 *     Sanctified instance onto the victim.</li>
 * </ul>
 * Both are player-only: a mob that merely got Sanctified (say, from a Seraphium weapon) must NOT
 * hand it back to the player who inflicted it - that made every such fight an endless ping-pong
 * until someone died. A mob type only takes part when explicitly listed in
 * {@link ModTags.EntityTypes#SANCTIFIED_CARRIERS} (empty by default).
 */
public final class SanctifiedCombatHandler {
    private static final float UNDEAD_DAMAGE_PER_LEVEL = 2.5F; // matches vanilla Smite

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker)) {
            return;
        }

        if (!canUseSanctified(attacker)) {
            return;
        }

        MobEffectInstance sanctified = attacker.getEffect(ModEffects.SANCTIFIED);
        if (sanctified == null) {
            return;
        }

        if (event.getEntity().getType().is(EntityTypeTags.UNDEAD)) {
            int level = sanctified.getAmplifier() + 1;
            event.setAmount(event.getAmount() + UNDEAD_DAMAGE_PER_LEVEL * level);
        }

        // Melee only: a projectile/indirect hit has a direct entity different from the causing one.
        if (event.getSource().getDirectEntity() == sourceEntity) {
            event.getEntity().addEffect(new MobEffectInstance(sanctified));
        }
    }

    /** Players always; mobs only when a data pack (or this mod) opts their type in. */
    public static boolean canUseSanctified(LivingEntity attacker) {
        return attacker instanceof Player || attacker.getType().is(ModTags.EntityTypes.SANCTIFIED_CARRIERS);
    }
}
