package dev.zsskayr.merlins_inferno.registry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * Central registry for every {@code Block} the mod adds.
 * <p>
 * Remember: registering a block here does NOT give it an inventory item.
 * Pair each block with a {@code BlockItem} registration in {@link ModItems}
 * (e.g. via {@code ModItems.ITEMS.registerSimpleBlockItem(...)}) if the player
 * should be able to hold/place it.
 */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Merlins_inferno.MODID);

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
