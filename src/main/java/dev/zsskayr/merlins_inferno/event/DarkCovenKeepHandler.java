package dev.zsskayr.merlins_inferno.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.item.DarkCovenItem;

/**
 * No stage of the Dark Coven can be thrown away: it moves between inventory slots and chests, nothing more. A toss is put
 * back in the inventory (if it is full the throw is allowed, rather than losing the sword).
 */
@EventBusSubscriber(modid = Merlins_inferno.MODID)
public final class DarkCovenKeepHandler {
    private DarkCovenKeepHandler() {
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        var stack = event.getEntity().getItem();
        if (stack.getItem() instanceof DarkCovenItem && !event.getPlayer().level().isClientSide
                && event.getPlayer().getInventory().add(stack)) {
            event.setCanceled(true);
        }
    }
}
