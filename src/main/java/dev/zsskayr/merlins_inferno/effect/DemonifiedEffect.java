package dev.zsskayr.merlins_inferno.effect;

import dev.zsskayr.merlins_inferno.block.RedLyriumRadiance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Red Lyrium's damaging corruption, with a stronger burn against humans. */
public class DemonifiedEffect extends MobEffect {
    public DemonifiedEffect() { super(MobEffectCategory.HARMFUL, 0xB42131); }

    @Override public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        boolean hardcore = entity.level() instanceof ServerLevel level && level.getServer().isHardcore();
        float damage = RedLyriumRadiance.isHuman(entity) ? 2.0F : 1.0F;
        if (hardcore) damage = Math.min(damage, entity.getHealth() - 1.0F);
        if (damage > 0.0F) {
            entity.hurt(entity.damageSources().magic(), damage);
        }
        return true;
    }

    @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = 25 >> amplifier;
        return interval <= 0 || duration % interval == 0;
    }
}
