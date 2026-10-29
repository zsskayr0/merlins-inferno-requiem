package dev.zsskayr.merlins_inferno.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import dev.zsskayr.merlins_inferno.attachment.ProgressionHelper;

/** Reads the world's Circle when the server starts and tells each player who joins the world's Circle (changes after that are broadcast by {@link ProgressionHelper}). */
public class CircleSyncHandler {
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        ProgressionHelper.onServerStarted(event.getServer());
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ProgressionHelper.syncTo(player);
        }
    }
}
