package dev.zsskayr.merlins_inferno.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import dev.zsskayr.merlins_inferno.Merlins_inferno;
import dev.zsskayr.merlins_inferno.item.DarkCovenItem;
import dev.zsskayr.merlins_inferno.registry.ModAttachments;

/**
 * No stage of the Dark Coven can be thrown away: it moves between inventory slots and chests, nothing more. A toss is put
 * back in the inventory (if it is full the throw is allowed, rather than losing the sword). Death does not drop it either:
 * it comes back on respawn.
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

    /** Takes the coven out of a player's death drops and remembers it for the respawn. */
    @SubscribeEvent
    public static void onDeathDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            event.getDrops().removeIf(drop -> {
                if (drop.getItem().getItem() instanceof DarkCovenItem) {
                    player.getData(ModAttachments.KEPT_COVENS).add(drop.getItem().copy());
                    return true;
                }
                return false;
            });
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            for (var stack : event.getOriginal().getData(ModAttachments.KEPT_COVENS)) {
                DarkCovenItem.setInvoked(stack, false);
                if (!event.getEntity().getInventory().add(stack)) {
                    event.getEntity().drop(stack, false);
                }
            }
        }
    }
}
