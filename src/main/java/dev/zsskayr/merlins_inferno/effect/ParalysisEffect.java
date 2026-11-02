package dev.zsskayr.merlins_inferno.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * "Paralysis" - the Luminite weapons' star-struck hold. The bearer cannot walk, jump or swing, and whatever it does hit while held
 * does no damage (see {@code event.LuminiteCombatHandler}). It is short by design: the hold is a chance on a hit, with a cooldown
 * per victim, and bosses are held for a fraction of it.
 */
public class ParalysisEffect extends MobEffect {
    public ParalysisEffect(MobEffectCategory category, int color) {
        super(category, color);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "paralysis_still"),
                -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        this.addAttributeModifier(Attributes.JUMP_STRENGTH, ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "paralysis_rooted"),
                -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        this.addAttributeModifier(Attributes.ATTACK_SPEED, ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "paralysis_numb"),
                -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
