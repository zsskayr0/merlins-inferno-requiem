package dev.zsskayr.merlins_inferno.event;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import dev.zsskayr.merlins_inferno.registry.ModEffects;

/**
 * The server side of the Druidic Trance: heavy damage resistance and the "munchies" (food gives double saturation and a
 * regeneration proportional to its nutrition). See {@code effect.DruidicTranceEffect} for the slowness and hunger drain
 * and {@code client.EdenweedSight} for The Sight, which is purely client-side so it only shows for the entranced player.
 */
public class EdenweedHandler {
    /** Damage taken while entranced: 80% less. */
    private static final float DAMAGE_MULTIPLIER = 0.2F;
    private static final int REGEN_TICKS_PER_NUTRITION = 40;

    @SubscribeEvent
    public void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!event.getEntity().hasEffect(ModEffects.DRUIDIC_TRANCE) || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        event.setAmount(event.getAmount() * DAMAGE_MULTIPLIER);
    }

    @SubscribeEvent
    public void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide || !player.hasEffect(ModEffects.DRUIDIC_TRANCE)) {
            return;
        }
        FoodProperties food = event.getItem().getFoodProperties(player);
        if (food == null) {
            return;
        }
        // Vanilla already granted nutrition * saturation * 2; grant the same again, capped like vanilla does.
        FoodData data = player.getFoodData();
        float extra = food.nutrition() * food.saturation() * 2.0F;
        data.setSaturation(Math.min(data.getSaturationLevel() + extra, data.getFoodLevel()));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, food.nutrition() * REGEN_TICKS_PER_NUTRITION, 0));
    }
}
