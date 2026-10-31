package dev.zsskayr.merlins_inferno.worldgen.elysium;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumWorld;

/**
 * The world seed El'ysium is generated from. A {@code ChunkGenerator} never sees the seed in {@code fillFromNoise}
 * (only a {@code RandomState}), so it is captured here when the server is about to start, which happens before any
 * level (and so any chunk) is created, on dedicated and integrated servers alike. Loading another world in the same
 * game fires the event again with that world's seed; stopping the server forgets it.
 *
 * <p>{@code /reload} does not recreate levels nor change the seed, so nothing has to happen on reload: the generator
 * object of an already loaded level keeps asking this class, which keeps answering the same seed.
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID)
public final class ElysiumSeed {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile boolean known;
    private static volatile long seed;
    private static volatile boolean warned;

    private ElysiumSeed() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        set(event.getServer().getWorldData().worldGenOptions().seed());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        known = false;
        warned = false;
        ElysiumWorlds.clear();
    }

    public static void set(long worldSeed) {
        seed = worldSeed;
        known = true;
        LOGGER.debug("El'ysium seed set to {}", worldSeed);
    }

    /** The current world seed. Falls back to the running server (and, outside any server, to 0 with a warning). */
    public static long get() {
        if (!known) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                set(server.getWorldData().worldGenOptions().seed());
            } else {
                if (!warned) {
                    warned = true;
                    LOGGER.warn("El'ysium asked for the world seed with no server running; using 0");
                }
                return 0L;
            }
        }
        return seed;
    }

    /** The El'ysium of the current world. */
    public static ElysiumWorld world() {
        return ElysiumWorlds.get(get());
    }
}
