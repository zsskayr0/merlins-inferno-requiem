package dev.zsskayr.merlins_inferno.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;

/** Tells each player who joins the world's Circle (changes after that are broadcast by {@link ProgressionHelper}). */
public class CircleSyncHandler {
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ProgressionHelper.syncTo(player);
        }
    }
}
