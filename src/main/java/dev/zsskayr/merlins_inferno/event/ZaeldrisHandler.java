package dev.zsskayr.merlins_inferno.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.item.ZaeldrisItem;
import dev.zsskayr.merlins_inferno.network.ZaeldrisPosePayload;
import dev.zsskayr.merlins_inferno.registry.ModEffects;

/** Zaeldris' barrage rhythm, and God Touch's "no healing" half. */
@EventBusSubscriber(modid = Merlins_inferno.MODID)
public final class ZaeldrisHandler {
    private ZaeldrisHandler() {}

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ZaeldrisItem.tickBarrage(player);
        }
    }

    /** Someone who comes into view mid-barrage still sees the trident pose. */
    @SubscribeEvent
    static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer watcher
                && ZaeldrisItem.isBarraging(target.getUUID())) {
            PacketDistributor.sendToPlayer(watcher, new ZaeldrisPosePayload(target.getId(), true));
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ZaeldrisItem.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    static void onHeal(LivingHealEvent event) {
        if (event.getEntity().hasEffect(ModEffects.GOD_TOUCH)) {
            event.setCanceled(true);
        }
    }
}
