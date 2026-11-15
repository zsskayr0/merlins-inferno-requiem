package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/** Brewing-stand intermediate potions. The Elixir of Warding itself is a custom item (see WardingBrewing). */
public final class ModPotions {
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, Merlins_inferno.MODID);

    /** Water Bottle + Refined Lyrium. Effectless, like Mundane/Thick; only step two uses it. */
    public static final DeferredHolder<Potion, Potion> LYRIUM_INFUSED = POTIONS.register("lyrium_infused", () -> new Potion());

    private ModPotions() {
    }

    public static void register(IEventBus modEventBus) {
        POTIONS.register(modEventBus);
    }
}
