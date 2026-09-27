package dev.zsskayr.merlins_inferno.event;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import dev.zsskayr.merlins_inferno.registry.ModAttachments;
import dev.zsskayr.merlins_inferno.registry.ModItems;

/**
 * The Pandora Box never leaves its owner, not even to death: it isn't dropped like a normal death-drop would
 * (whether or not keepInventory is on), and comes back on respawn.
 */
public class PandoraHandler {
    /** Cancels the Box's own drop (everything else still falls normally) and remembers to give it back. */
    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player player)) {
            return;
        }
        event.getDrops().removeIf(entity -> {
            if (entity.getItem().is(ModItems.PANDORA_BOX.get())) {
                player.setData(ModAttachments.RETURN_PANDORA_BOX, true);
                return true;
            }
            return false;
        });
    }

    /** On respawn (never on a dimension change), hands a fresh Pandora Box back if the old one was owed. */
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        net.minecraft.world.entity.player.Player original = event.getOriginal();
        if (original.getData(ModAttachments.RETURN_PANDORA_BOX)) {
            event.getEntity().getInventory().add(new ItemStack(ModItems.PANDORA_BOX.get()));
            event.getEntity().setData(ModAttachments.RETURN_PANDORA_BOX, false);
        }
    }
}
