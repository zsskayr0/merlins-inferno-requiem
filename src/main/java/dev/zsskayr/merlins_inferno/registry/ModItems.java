package dev.zsskayr.merlins_inferno.registry;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.zsskayr.merlins_inferno.Merlins_inferno;

/**
 * Central registry for every {@code Item} the mod adds (including {@code BlockItem}s,
 * which stay here rather than in {@link ModBlocks} since items are what the player
 * actually interacts with in an inventory).
 * <p>
 * Add new items as {@code public static final} fields below, then call
 * {@link #register(net.neoforged.bus.api.IEventBus)} once from the main mod class.
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Merlins_inferno.MODID);

    // Starter tome handed out for the future onboarding/tutorial flow. Plain item for now -
    // no reading/GUI behavior yet, just something to register end-to-end and show in-world.
    public static final DeferredItem<Item> GRIMMORIUM = ITEMS.registerSimpleItem("grimmorium", new Item.Properties());

    private ModItems() {
    }

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
