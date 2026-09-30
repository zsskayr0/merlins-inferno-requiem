package dev.zsskayr.merlins_inferno.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * "Druidic Trance" - the Edenweed high. Couch-lock: slower, weaker and clumsy at mining, the "munchies" drain the bearer's
 * hunger fast. The rest of the trance (heavy damage resistance, doubled food saturation with regeneration, and The Sight)
 * lives in {@code event.EdenweedHandler} and {@code client.EdenweedSight}.
 */
public class DruidicTranceEffect extends MobEffect {
    /** Extra exhaustion per tick: roughly three times what vanilla's Hunger I costs. */
    private static final float HUNGER_EXHAUSTION = 0.015F;

    public DruidicTranceEffect(MobEffectCategory category, int color) {
        super(category, color);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "druidic_trance_slow"),
                -0.35, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        this.addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "druidic_trance_clumsy"),
                -0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        this.addAttributeModifier(Attributes.ATTACK_DAMAGE, ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "druidic_trance_weak"),
                -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Player player) {
            player.causeFoodExhaustion(HUNGER_EXHAUSTION);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
