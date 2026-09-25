package dev.zsskayr.merlins_inferno.event;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import dev.zsskayr.merlins_inferno.registry.ModEffects;
import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * Seraphium's own intrinsic combat traits - baked into the tool tag
 * ({@link ModTags.Items#SERAPHIUM_TOOLS}), unlike {@code SanctifiedCombatHandler}'s contagion
 * (which only fires while the attacker personally has the effect, from carrying Raw Lyrium).
 * Any hit with a Seraphium weapon:
 * <ul>
 *     <li>inflicts Sanctified on the victim outright - level 2 against Undead, level 1 against
 *     everything else;</li>
 *     <li>deals 50% damage to {@link ModTags.EntityTypes#PASSIVE_MOBS};</li>
 *     <li>deals 75% damage to {@link ModTags.EntityTypes#NEUTRAL_MOBS};</li>
 *     <li>deals 150% damage to {@link ModTags.EntityTypes#DEMON}s (takes priority over the next rule);</li>
 *     <li>deals increased damage to aggressive mobs - anything that's a {@link Monster} and NOT
 *     already claimed by {@link ModTags.EntityTypes#NEUTRAL_MOBS} (several vanilla "neutral" mobs,
 *     like Enderman/Spider/Piglin, are themselves {@code Monster} subclasses - the neutral tag
 *     takes priority so they stay 75%, not double-counted as "aggressive" too).</li>
 * </ul>
 */
public final class SeraphiumCombatHandler {
    private static final int SANCTIFIED_DURATION = 200; // 10s - a flat on-hit application, not tick-refreshed
    private static final float PASSIVE_DAMAGE_MULTIPLIER = 0.5F;
    private static final float NEUTRAL_DAMAGE_MULTIPLIER = 0.75F;
    private static final float AGGRESSIVE_DAMAGE_MULTIPLIER = 1.3F;
    private static final float DEMON_DAMAGE_MULTIPLIER = 1.5F; // provisional - tune with playtesting

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker)) {
            return;
        }

        ItemStack weapon = attacker.getMainHandItem();
        if (!weapon.is(ModTags.Items.SERAPHIUM_TOOLS)) {
            return;
        }

        LivingEntity victim = event.getEntity();
        boolean isUndead = victim.getType().is(EntityTypeTags.UNDEAD);
        int amplifier = isUndead ? 1 : 0; // level 2 vs Undead, level 1 vs everything else
        victim.addEffect(new MobEffectInstance(ModEffects.SANCTIFIED, SANCTIFIED_DURATION, amplifier));

        if (victim.getType().is(ModTags.EntityTypes.DEMON)) {
            // Demons take priority over the generic "aggressive" bonus below (they're Monsters too).
            event.setAmount(event.getAmount() * DEMON_DAMAGE_MULTIPLIER);
        } else if (victim.getType().is(ModTags.EntityTypes.PASSIVE_MOBS)) {
            event.setAmount(event.getAmount() * PASSIVE_DAMAGE_MULTIPLIER);
        } else if (victim.getType().is(ModTags.EntityTypes.NEUTRAL_MOBS)) {
            event.setAmount(event.getAmount() * NEUTRAL_DAMAGE_MULTIPLIER);
        } else if (victim instanceof Monster) {
            event.setAmount(event.getAmount() * AGGRESSIVE_DAMAGE_MULTIPLIER);
        }
    }
}
