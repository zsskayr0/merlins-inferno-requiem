package dev.zsskayr.merlins_inferno.effect;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * "God Touch" - Zaeldris' brand. The bearer loses {@value #DAMAGE_PER_SECOND} health a second, through armor, and cannot heal
 * at all while it lasts (see {@code event.ZaeldrisHandler}). The spears refresh it many times a second, so the damage is paced
 * by the bearer's own clock instead of the effect's remaining duration, which would keep restarting.
 */
public class GodTouchEffect extends MobEffect {
    public static final float DAMAGE_PER_SECOND = 25.0F;
    /** Every spear that lodges or bursts lends this much, five seconds. */
    public static final int DURATION_TICKS = 100;
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "god_touch"));

    public GodTouchEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        if (!livingEntity.level().isClientSide && livingEntity.tickCount % 20 == 0) {
            // Whoever's spear lent it last (they set the bearer's last attacker every hit) owns the kill, loot and experience.
            LivingEntity attacker = livingEntity.getLastHurtByMob();
            livingEntity.hurt(new DamageSource(livingEntity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(DAMAGE_TYPE), attacker), DAMAGE_PER_SECOND);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
