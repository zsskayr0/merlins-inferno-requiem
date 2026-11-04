package dev.zsskayr.merlins_inferno.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.entity.ExcaliburAwakening;

/** Gives back an Excalibur whose awakening was cut short (see ExcaliburAwakening#resume). */
@EventBusSubscriber(modid = Merlins_inferno.MODID)
public final class ExcaliburHandler {
    private ExcaliburHandler() {
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 10 == 0) {
            ExcaliburAwakening.resume(player);
        }
    }
}
