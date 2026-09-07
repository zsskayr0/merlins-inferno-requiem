package dev.zsskayr.merlins_inferno;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The mod's COMMON config. Define new {@code ModConfigSpec.*Value} fields with {@link #BUILDER},
 * cache their resolved values as public static fields, and refresh those fields in {@link #onLoad}
 * (fired on both initial load and whenever the config file is edited/reloaded).
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
    }
}
