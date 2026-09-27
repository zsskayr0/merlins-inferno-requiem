package dev.zsskayr.merlins_inferno.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * "Mark of Debt" - Andras' brand on those who owe him. Pure marker: the extra damage the bearer takes is
 * applied by {@code event.MarkOfDebtHandler}.
 */
public class MarkOfDebtEffect extends MobEffect {
    public MarkOfDebtEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
