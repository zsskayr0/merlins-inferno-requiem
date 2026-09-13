package dev.zsskayr.merlins_inferno.effect;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * "Sanctified" - Raw Lyrium's touch. See {@code event.SanctifiedTickHandler} for how a player
 * actually gets this (level scales with where the item sits: inventory, hand, or how long it's
 * been held) and {@code event.SanctifiedCombatHandler} for its combat effects (bonus damage
 * against Undead, and spreading to whoever the holder hits in melee).
 * <p>
 * This class only owns the damage-over-time tick, which is deliberately NOT capped at 1 HP the
 * way vanilla's Poison is - per project decision, Sanctified can kill outside Hardcore. Inside a
 * Hardcore world it falls back to Poison's own "never below 1 HP" guard, the one place the two
 * behave the same.
 */
public class SanctifiedEffect extends MobEffect {
    public SanctifiedEffect(MobEffectCategory category, int color, ParticleOptions particle) {
        super(category, color, particle);
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        boolean hardcore = livingEntity.level() instanceof ServerLevel serverLevel && serverLevel.getServer().isHardcore();
        if (!hardcore || livingEntity.getHealth() > 1.0F) {
            livingEntity.hurt(livingEntity.damageSources().magic(), 1.0F);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Same cadence curve as vanilla Poison - ticks faster at higher amplifiers.
        int interval = 25 >> amplifier;
        return interval > 0 ? duration % interval == 0 : true;
    }
}
