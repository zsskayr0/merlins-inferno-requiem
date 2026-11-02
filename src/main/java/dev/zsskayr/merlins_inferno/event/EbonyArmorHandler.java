package dev.zsskayr.merlins_inferno.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * Ebony armor's set bonus: wearing all four pieces keeps Fire Resistance on you (the original refreshed it every tick; here every
 * {@value #CHECK_INTERVAL} ticks, with a duration that outlasts the gap), and withers whoever strikes you in melee (see
 * {@link EbonyCombatHandler}). The price is its weight: {@value #WEIGHT_PENALTY} of your walking speed - Luminite is the light set.
 */
public class EbonyArmorHandler {
    private static final int CHECK_INTERVAL = 20;
    private static final int DURATION = 60;
    /** Walking speed lost to the weight of a full set, as a fraction. */
    public static final double WEIGHT_PENALTY = 0.06;
    private static final ResourceLocation WEIGHT_ID = ResourceLocation.fromNamespaceAndPath(Merlins_inferno.MODID, "ebony_weight");

    public static boolean wearsFullSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.EBONY_HELMET.get())
                && entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.EBONY_CHESTPLATE.get())
                && entity.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.EBONY_LEGGINGS.get())
                && entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.EBONY_BOOTS.get());
    }

    /** Applies (or clears) the set's effects for this instant. */
    public static void refresh(Player player) {
        boolean fullSet = wearsFullSet(player);
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            if (fullSet) {
                speed.addOrUpdateTransientModifier(new AttributeModifier(WEIGHT_ID, -WEIGHT_PENALTY, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else {
                speed.removeModifier(WEIGHT_ID);
            }
        }
        if (fullSet) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, DURATION, 0, true, false, true));
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide() && player.tickCount % CHECK_INTERVAL == 0) {
            refresh(player);
        }
    }
}
