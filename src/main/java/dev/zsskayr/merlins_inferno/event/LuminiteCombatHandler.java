package dev.zsskayr.merlins_inferno.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import dev.zsskayr.merlins_inferno.registry.ModEffects;
import dev.zsskayr.merlins_inferno.registry.ModTags;

/**
 * The Luminite weapons' star-struck touch (see {@link ModTags.Items#LUMINITE_TOOLS}): a melee hit leaves the victim
 * <b>Glowing</b>, and has a {@link #HOLD_CHANCE} to <b>Paralyse</b> it for {@link #HOLD_TICKS} ticks ({@link #BOSS_HOLD_TICKS}
 * for a boss) - at most once per {@link #HOLD_COOLDOWN} ticks per victim, so it is a moment to strike, never a stun-lock. A
 * paralysed attacker deals no damage at all.
 */
public final class LuminiteCombatHandler {
    private static final int GLOW_TICKS = 100;
    private static final float HOLD_CHANCE = 0.25F;
    private static final int HOLD_TICKS = 25;
    private static final int BOSS_HOLD_TICKS = 8;
    private static final long HOLD_COOLDOWN = 160L;
    private static final String READY_KEY = "merlins_inferno_paralysis_ready";

    @SubscribeEvent
    public void onHit(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof LivingEntity attacker)
                || !CombatWeapon.of(source).is(ModTags.Items.LUMINITE_TOOLS)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS, 0, false, false), attacker);

        long now = victim.level().getGameTime();
        CompoundTag data = victim.getPersistentData();
        if (now >= data.getLong(READY_KEY) && victim.getRandom().nextFloat() < HOLD_CHANCE) {
            int ticks = victim.getType().is(ModTags.EntityTypes.REWARD_CHEST_BOSSES) ? BOSS_HOLD_TICKS : HOLD_TICKS;
            if (victim.addEffect(new MobEffectInstance(ModEffects.PARALYSIS, ticks, 0, false, true), attacker)) {
                data.putLong(READY_KEY, now + HOLD_COOLDOWN);
            }
        }
    }

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker.hasEffect(ModEffects.PARALYSIS)) {
            event.setCanceled(true);
        }
    }
}
