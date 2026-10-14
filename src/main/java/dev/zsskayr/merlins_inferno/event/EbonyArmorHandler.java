package dev.zsskayr.merlins_inferno.event;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * Ebony armor's set bonus, from the original mod: wearing all four pieces keeps Fire Resistance IV on you (the original
 * refreshed it every tick; here every {@value #CHECK_INTERVAL} ticks, with a duration that outlasts the gap).
 */
public class EbonyArmorHandler {
    private static final int CHECK_INTERVAL = 20;
    private static final int DURATION = 60;
    private static final int AMPLIFIER = 3;

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % CHECK_INTERVAL != 0) {
            return;
        }
        boolean fullSet = player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.EBONY_HELMET.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.EBONY_CHESTPLATE.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.EBONY_LEGGINGS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.EBONY_BOOTS.get());
        if (fullSet) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, DURATION, AMPLIFIER, true, false, true));
        }
    }
}
