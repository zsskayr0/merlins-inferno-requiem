package dev.zsskayr.merlins_inferno.event;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.item.WingsItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Keeps a wings boost pulling the player every tick for as long as it lasts. */
@EventBusSubscriber(modid = Merlins_inferno.MODID)
public final class WingsBoostHandler {
    private WingsBoostHandler() {}
    @SubscribeEvent static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide) return;
        WingsItem.tickBoost(event.getEntity());
        WingsItem.tickGroundRecharge(event.getEntity());
    }
}
