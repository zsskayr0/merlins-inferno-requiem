package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.menu.HellForgeMenu;
import dev.zsskayr.merlins_inferno.menu.PandoraBoxMenu;

/** Central registry for every {@code MenuType} this mod adds. */
public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, Merlins_inferno.MODID);

    // No extra data needed on menu-open - the client just builds a blank container/data (see
    // HellForgeMenu's client-side constructor) and gets it filled in via the normal slot/data sync,
    // same as vanilla's own furnace menu.
    public static final DeferredHolder<MenuType<?>, MenuType<HellForgeMenu>> HELL_FORGE = MENU_TYPES.register("hell_forge",
            () -> new MenuType<>(HellForgeMenu::new, FeatureFlags.VANILLA_SET));

    // Same story as Hell Forge's: no extra open data, the client's blank container/data gets synced right after.
    public static final DeferredHolder<MenuType<?>, MenuType<PandoraBoxMenu>> PANDORA_BOX = MENU_TYPES.register("pandora_box",
            () -> new MenuType<>(PandoraBoxMenu::new, FeatureFlags.VANILLA_SET));

    private ModMenuTypes() {
    }

    public static void register(IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
    }
}
