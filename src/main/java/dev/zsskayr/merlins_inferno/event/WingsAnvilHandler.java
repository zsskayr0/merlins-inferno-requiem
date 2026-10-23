package dev.zsskayr.merlins_inferno.event;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.item.WingsItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

/** Wings that cannot be mended cannot be mended by combining two of them in an anvil either (vanilla lets any two damaged copies merge). */
@EventBusSubscriber(modid = Merlins_inferno.MODID)
public final class WingsAnvilHandler {
    private WingsAnvilHandler() {}
    @SubscribeEvent static void onAnvil(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        if (left.getItem() instanceof WingsItem && !left.isRepairable() && event.getRight().is(left.getItem())) event.setCanceled(true);
    }
}
