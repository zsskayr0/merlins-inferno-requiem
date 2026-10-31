package dev.zsskayr.merlins_inferno.worldgen.elysium;

import java.util.concurrent.ConcurrentHashMap;

import dev.zsskayr.merlins_inferno.worldgen.elysium.core.ElysiumWorld;

/** One {@link ElysiumWorld} per seed, shared by every thread (the core is thread-safe and caches its islands lazily). */
public final class ElysiumWorlds {
    private static final ConcurrentHashMap<Long, ElysiumWorld> CACHE = new ConcurrentHashMap<>();

    private ElysiumWorlds() {
    }

    public static ElysiumWorld get(long seed) {
        ElysiumWorld world = CACHE.get(seed);
        if (world != null) {
            return world;
        }
        if (CACHE.size() >= 4) {
            CACHE.clear();
        }
        return CACHE.computeIfAbsent(seed, ElysiumWorld::new);
    }

    static void clear() {
        CACHE.clear();
    }
}
